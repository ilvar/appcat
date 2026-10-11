package pw.rkd.appcat;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

/** Portable, versioned backup. The API key is plaintext in the file, encrypted on device. */
final class Backup {
    static final int MAX_BYTES=5*1024*1024;
    record Data(JSONObject settings, JSONArray categories, JSONObject mappings) {}
    static String export(Context context) throws Exception {
        Catalog catalog=new Catalog(context);var apps=catalog.scan();
        var prefs=context.getSharedPreferences("settings",0);
        JSONObject settings=new JSONObject().put("apiKey",KeyStore.read(context))
                .put("url",prefs.getString("url",SystemOne.DEFAULT_URL)).put("model",prefs.getString("model",SystemOne.DEFAULT_MODEL))
                .put("popupTheme",prefs.getString("popupTheme","system")).put("playLookup",prefs.getBoolean("playLookup",true)).put("bubble",prefs.getBoolean("bubble",false)).put("homeOnly",prefs.getBoolean("homeOnly",false));
        for(String key:List.of("bubbleX","bubbleY"))if(prefs.contains(key))settings.put(key,prefs.getInt(key,0));
        JSONArray categories=new JSONArray();for(var c:catalog.categories())categories.put(Catalog.json("id",c.id(),"name",c.name(),"icon",c.icon()));
        JSONObject mappings=new JSONObject();for(var entry:context.getSharedPreferences("catalog",0).getAll().entrySet())if(entry.getKey().startsWith("app:")){
            JSONObject saved=new JSONObject((String)entry.getValue());
            mappings.put(entry.getKey().substring(4),Catalog.json("category",saved.getString("category"),"source",saved.getString("source")));
        }
        for(var app:apps)if(!mappings.has(app.pkg()))mappings.put(app.pkg(),Catalog.json("category",app.category(),"source",app.source()));
        return new JSONObject().put("format","appcat").put("version",1).put("settings",settings).put("categories",categories).put("mappings",mappings).toString(2);
    }
    static Data parse(String json) throws Exception {
        JSONObject root=new JSONObject(json);
        if(!"appcat".equals(root.getString("format"))||!(root.get("version") instanceof Number)||root.getDouble("version")!=1)throw new IllegalArgumentException("Unsupported AppCat backup format/version");
        JSONObject input=root.getJSONObject("settings"), settings=new JSONObject();
        for(String key:List.of("apiKey","url","model"))settings.put(key,string(input,key));
        String theme=input.has("popupTheme")?string(input,"popupTheme"):"system";if(PopupTheme.index(theme)<0)throw new IllegalArgumentException("Unknown popup theme");settings.put("popupTheme",theme);
        SystemOne.validateUrl(settings.getString("url"));
        if(settings.getString("model").trim().isEmpty())throw new IllegalArgumentException("Model must not be empty");
        for(String key:List.of("playLookup","bubble","homeOnly")){Object value=input.get(key);if(!(value instanceof Boolean))throw new IllegalArgumentException("Invalid setting: "+key);settings.put(key,value);}
        for(String key:List.of("bubbleX","bubbleY"))if(input.has(key)){Object value=input.get(key);if(!(value instanceof Number)||input.getDouble(key)!=input.getInt(key))throw new IllegalArgumentException("Invalid position: "+key);settings.put(key,input.getInt(key));}
        JSONArray inputCategories=root.getJSONArray("categories"), categories=new JSONArray();Set<String> ids=new HashSet<>(),names=new HashSet<>();
        if(inputCategories.length()==0||inputCategories.length()>500)throw new IllegalArgumentException("Use 1–500 categories");
        for(int i=0;i<inputCategories.length();i++){
            JSONObject c=inputCategories.getJSONObject(i);String id=string(c,"id"),name=string(c,"name").trim(),icon=string(c,"icon");
            if(!id.matches("[A-Za-z0-9_-]{1,100}")||!ids.add(id))throw new IllegalArgumentException("Invalid or duplicate category ID");
            if(name.isEmpty()||name.length()>60||!names.add(name.toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Invalid or duplicate category name");
            if(!CategoryIcons.valid(icon))throw new IllegalArgumentException("Unknown category icon");
            categories.put(Catalog.json("id",id,"name",name,"icon",icon));
        }
        JSONObject inputMappings=root.getJSONObject("mappings"), mappings=new JSONObject();
        if(inputMappings.length()>20000)throw new IllegalArgumentException("Too many app mappings");
        for(Iterator<String> keys=inputMappings.keys();keys.hasNext();){
            String pkg=keys.next();if(!pkg.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)*")||pkg.length()>255)throw new IllegalArgumentException("Invalid package ID");
            JSONObject app=inputMappings.getJSONObject(pkg);String category=string(app,"category"),source=string(app,"source");
            if(!category.isEmpty()&&!ids.contains(category))throw new IllegalArgumentException("Mapping references an unknown category");
            if(!Set.of("Android","Manual","AI","Reset","Uncategorised").contains(source))throw new IllegalArgumentException("Unknown assignment source");
            mappings.put(pkg,Catalog.json("category",category,"source",source).put("revision",1));
        }
        return new Data(settings,categories,mappings);
    }
    private static String string(JSONObject object,String key) throws Exception {Object value=object.get(key);if(!(value instanceof String))throw new IllegalArgumentException("Expected text: "+key);return (String)value;}
    static void apply(Context context,Data data) throws Exception {
        if(Classifier.INSTANCE.running)throw new IllegalStateException("Stop categorisation before importing");
        var settings=context.getSharedPreferences("settings",0);var catalog=context.getSharedPreferences("catalog",0);
        var secrets=context.getSharedPreferences("secrets",0);
        Map<String,?> oldSettings=settings.getAll(),oldCatalog=catalog.getAll(),oldSecrets=secrets.getAll();
        try {
            KeyStore.save(context,data.settings.getString("apiKey"));
            SharedPreferences.Editor editor=catalog.edit().clear().putString("categories",data.categories.toString());
            for(Iterator<String> keys=data.mappings.keys();keys.hasNext();){String pkg=keys.next();editor.putString("app:"+pkg,data.mappings.getJSONObject(pkg).toString());}
            if(!editor.commit())throw new IllegalStateException("Could not save app mappings");
            editor=settings.edit();for(String key:List.of("url","model","popupTheme"))editor.putString(key,data.settings.getString(key));
            for(String key:List.of("playLookup","bubble","homeOnly"))editor.putBoolean(key,data.settings.getBoolean(key));
            for(String key:List.of("bubbleX","bubbleY")){editor.remove(key);if(data.settings.has(key))editor.putInt(key,data.settings.getInt(key));}
            if(!editor.commit())throw new IllegalStateException("Could not save settings");
        }catch(Exception error){restore(catalog,oldCatalog);restore(settings,oldSettings);restore(secrets,oldSecrets);throw error;}
    }
    private static void restore(SharedPreferences prefs,Map<String,?> values){var editor=prefs.edit().clear();for(var item:values.entrySet()){Object v=item.getValue();if(v instanceof String)editor.putString(item.getKey(),(String)v);else if(v instanceof Boolean)editor.putBoolean(item.getKey(),(Boolean)v);else if(v instanceof Integer)editor.putInt(item.getKey(),(Integer)v);}editor.commit();}
}
