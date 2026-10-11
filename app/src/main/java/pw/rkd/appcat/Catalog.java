package pw.rkd.appcat;

import android.content.*;
import android.content.pm.*;
import org.json.*;
import java.util.*;

/** Package identity and stable category IDs survive rescans, renames and reinstalls. */
public final class Catalog {
    private static final Object ASSIGN_LOCK=new Object();
    public static final String UNASSIGNED = "";
    public record Category(String id, String name, String icon) {
        public Category(String id,String name){this(id,name,CategoryIcons.defaultIcon(id));}
    }
    public record App(String pkg, String name, String category, String source, long revision) {}
    private final Context context;
    private final android.content.SharedPreferences prefs;
    public Catalog(Context c) { context=c.getApplicationContext(); prefs=context.getSharedPreferences("catalog",0); init(); }
    private synchronized void init() {
        if(prefs.contains("categories")) return;
        JSONArray a=new JSONArray();
        String[] names={"Games","Music & audio","Video","Photography","Social","News","Maps & travel","Productivity","Accessibility","Communication","Tools","Shopping","Finance","Health & fitness","Education","Food & drink","Other"};
        for(int i=0;i<names.length;i++) a.put(json("id","c"+i,"name",names[i]));
        prefs.edit().putString("categories",a.toString()).apply();
    }
    static JSONObject json(String... kv) { JSONObject j=new JSONObject(); try { for(int i=0;i<kv.length;i+=2) j.put(kv[i],kv[i+1]); } catch(JSONException e) { throw new IllegalStateException(e); } return j; }
    public synchronized List<Category> categories() {
        List<Category> result=new ArrayList<>();
        try { JSONArray a=new JSONArray(prefs.getString("categories","[]")); for(int i=0;i<a.length();i++){ JSONObject j=a.getJSONObject(i); result.add(new Category(j.getString("id"),j.getString("name"),j.optString("icon",CategoryIcons.defaultIcon(j.getString("id"))))); } } catch(JSONException e){ throw new IllegalStateException(e); }
        return result;
    }
    public synchronized String saveCategory(String id,String name) {
        name=name.trim(); if(name.isEmpty() || name.length()>60) throw new IllegalArgumentException("Use a name of 1–60 characters");
        List<Category> list=categories();
        for(Category c:list) if(c.name().equalsIgnoreCase(name) && !c.id().equals(id)) throw new IllegalArgumentException("Category already exists");
        if(id==null) id=UUID.randomUUID().toString();
        JSONArray a=new JSONArray(); boolean found=false;
        for(Category c:list) { boolean match=c.id().equals(id); a.put(json("id",c.id(),"name",match?name:c.name(),"icon",c.icon())); found|=match; }
        if(!found) a.put(json("id",id,"name",name));
        prefs.edit().putString("categories",a.toString()).apply(); return id;
    }
    public synchronized void saveIcon(String id,String icon) {
        if(!CategoryIcons.valid(icon))throw new IllegalArgumentException("Unknown icon");
        JSONArray a=new JSONArray();boolean found=false;
        for(Category c:categories()){boolean match=c.id().equals(id);found|=match;a.put(json("id",c.id(),"name",c.name(),"icon",match?icon:c.icon()));}
        if(!found)throw new IllegalArgumentException("Unknown category");
        prefs.edit().putString("categories",a.toString()).apply();
    }
    public String categoryName(String id) { for(Category c:categories()) if(c.id().equals(id))return c.name(); return "Uncategorised"; }
    public void assign(String pkg,String category,String source) { synchronized(ASSIGN_LOCK) {
        if(!category.isEmpty() && categories().stream().noneMatch(c->c.id().equals(category))) throw new IllegalArgumentException("Unknown category");
        try {
            JSONObject previous=new JSONObject(prefs.getString("app:"+pkg,"{}"));
            JSONObject next=json("category",category,"source",source).put("revision",previous.optLong("revision",0)+1);
            prefs.edit().putString("app:"+pkg,next.toString()).apply();
        }catch(JSONException e){throw new IllegalStateException(e);}
    }}
    /** A manual edit after the job snapshot always wins, including edits back to the same category. */
    public boolean assignAiIfUnchanged(App expected,String category) { synchronized(ASSIGN_LOCK) {
        try {
            JSONObject saved=new JSONObject(prefs.getString("app:"+expected.pkg(),"{}"));
            if(saved.optLong("revision",0)!=expected.revision())return false;
            assign(expected.pkg(),category,"AI");return true;
        }catch(JSONException e){throw new IllegalStateException(e);}
    }}
    public void resetApps() { synchronized(ASSIGN_LOCK) {
        Set<String> packages=new HashSet<>();
        for(String key:prefs.getAll().keySet())if(key.startsWith("app:"))packages.add(key.substring(4));
        for(App app:scan())packages.add(app.pkg());
        for(String pkg:packages)assign(pkg,"","Reset");
    }}
    @SuppressWarnings("deprecation")
    public List<App> scan() {
        PackageManager pm=context.getPackageManager();
        List<ResolveInfo> entries=pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0);
        Map<String,App> apps=new HashMap<>();
        for(ResolveInfo r:entries) {
            ApplicationInfo ai=r.activityInfo.applicationInfo;
            if(ai.packageName.equals(context.getPackageName())) continue;
            String cat="", source="Uncategorised";long revision=0;
            try { JSONObject saved=new JSONObject(prefs.getString("app:"+ai.packageName,"{}")); cat=saved.optString("category"); source=saved.optString("source","Uncategorised");revision=saved.optLong("revision",0); }catch(JSONException ignored){}
            if(cat.isEmpty() && !prefs.contains("app:"+ai.packageName)) {
                cat=androidCategory(ai.category);
                if(!cat.isEmpty()) { source="Android"; assign(ai.packageName,cat,source); revision=1; }
            }
            if(!cat.isEmpty() && !hasCategory(cat)) { cat=""; source="Uncategorised"; }
            apps.put(ai.packageName,new App(ai.packageName,ai.loadLabel(pm).toString(),cat,source,revision));
        }
        List<App> result=new ArrayList<>(apps.values()); result.sort(Comparator.comparing(App::name,String.CASE_INSENSITIVE_ORDER)); return result;
    }
    private boolean hasCategory(String id) { for(Category c:categories())if(c.id().equals(id))return true;return false; }
    static String androidCategory(int category) { return category>=0 && category<=8?"c"+category:""; }
}
