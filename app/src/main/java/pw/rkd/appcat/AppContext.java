package pw.rkd.appcat;

import android.content.Context;
import org.json.*;

final class AppContext {
    static JSONObject load(Context context,Catalog.App app) throws Exception {
        JSONObject result=new JSONObject();
        try {
            var info=context.getPackageManager().getApplicationInfo(app.pkg(),0);
            CharSequence description=info.loadDescription(context.getPackageManager());
            if(description!=null)result.put("android_description",PlayMetadata.limit(description.toString(),2000));
        }catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
        var settings=context.getSharedPreferences("settings",0);
        if(!settings.getBoolean("playLookup",true))return result;
        var cache=context.getSharedPreferences("app-context",0);String cacheKey="play:"+app.pkg();JSONObject saved=new JSONObject(cache.getString(cacheKey,"{}"));
        long age=System.currentTimeMillis()-saved.optLong("time",0);
        long ttl=saved.optBoolean("useful")?7*24*60*60*1000L:60*60*1000L;
        if(age<0 || age>ttl){
            try {
                var info=PlayMetadata.fetch(app.pkg());
                saved=new JSONObject().put("time",System.currentTimeMillis()).put("useful",info.useful()).put("title",info.title()).put("description",info.description()).put("category",info.category()).put("url",info.url());
            }catch(InterruptedException e){throw e;}
            catch(Exception e){saved=new JSONObject().put("time",System.currentTimeMillis()).put("useful",false);}
            cache.edit().putString(cacheKey,saved.toString()).apply();
        }
        if(saved.optBoolean("useful"))result.put("google_play",new JSONObject().put("title",saved.optString("title")).put("description",saved.optString("description")).put("store_category",saved.optString("category")).put("url",saved.optString("url")));
        return result;
    }
}
