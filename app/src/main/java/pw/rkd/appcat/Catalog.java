package pw.rkd.appcat;

import android.content.*;
import android.content.pm.*;
import org.json.*;
import java.util.*;

/** Package identity and stable category IDs survive rescans, renames and reinstalls. */
public final class Catalog {
    private static final Object ASSIGN_LOCK=new Object();
    public static final String UNASSIGNED = "";
    public record Category(String id, String name) {}
    public record App(String pkg, String name, String category, String source) {}
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
        try { JSONArray a=new JSONArray(prefs.getString("categories","[]")); for(int i=0;i<a.length();i++){ JSONObject j=a.getJSONObject(i); result.add(new Category(j.getString("id"),j.getString("name"))); } } catch(JSONException e){ throw new IllegalStateException(e); }
        return result;
    }
    public synchronized String saveCategory(String id,String name) {
        name=name.trim(); if(name.isEmpty() || name.length()>60) throw new IllegalArgumentException("Use a name of 1–60 characters");
        List<Category> list=categories();
        for(Category c:list) if(c.name().equalsIgnoreCase(name) && !c.id().equals(id)) throw new IllegalArgumentException("Category already exists");
        if(id==null) id=UUID.randomUUID().toString();
        JSONArray a=new JSONArray(); boolean found=false;
        for(Category c:list) { boolean match=c.id().equals(id); a.put(json("id",c.id(),"name",match?name:c.name())); found|=match; }
        if(!found) a.put(json("id",id,"name",name));
        prefs.edit().putString("categories",a.toString()).apply(); return id;
    }
    public String categoryName(String id) { for(Category c:categories()) if(c.id().equals(id))return c.name(); return "Uncategorised"; }
    public void assign(String pkg,String category,String source) { synchronized(ASSIGN_LOCK) {
        if(!category.isEmpty() && categories().stream().noneMatch(c->c.id().equals(category))) throw new IllegalArgumentException("Unknown category");
        prefs.edit().putString("app:"+pkg,json("category",category,"source",source).toString()).apply();
    }}
    public void assignAiIfUnknown(String pkg,String category) { synchronized(ASSIGN_LOCK) {
        try { JSONObject j=new JSONObject(prefs.getString("app:"+pkg,"{}")); if(j.optString("category").isEmpty()) assign(pkg,category,"AI"); } catch(JSONException e){throw new IllegalStateException(e);}
    }}
    @SuppressWarnings("deprecation")
    public List<App> scan() {
        PackageManager pm=context.getPackageManager();
        List<ResolveInfo> entries=pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0);
        Map<String,App> apps=new HashMap<>();
        for(ResolveInfo r:entries) {
            ApplicationInfo ai=r.activityInfo.applicationInfo;
            if(ai.packageName.equals(context.getPackageName())) continue;
            String cat="", source="Uncategorised";
            try { JSONObject saved=new JSONObject(prefs.getString("app:"+ai.packageName,"{}")); cat=saved.optString("category"); source=saved.optString("source","Uncategorised"); }catch(JSONException ignored){}
            if(cat.isEmpty() && !source.equals("Manual")) {
                cat=androidCategory(ai.category);
                if(!cat.isEmpty()) { source="Android"; assign(ai.packageName,cat,source); }
            }
            if(!cat.isEmpty() && !hasCategory(cat)) { cat=""; source="Uncategorised"; }
            apps.put(ai.packageName,new App(ai.packageName,ai.loadLabel(pm).toString(),cat,source));
        }
        List<App> result=new ArrayList<>(apps.values()); result.sort(Comparator.comparing(App::name,String.CASE_INSENSITIVE_ORDER)); return result;
    }
    private boolean hasCategory(String id) { for(Category c:categories())if(c.id().equals(id))return true;return false; }
    static String androidCategory(int category) { return category>=0 && category<=8?"c"+category:""; }
}
