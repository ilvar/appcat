package pw.rkd.appcat;

import android.content.Context;
import android.os.*;
import java.util.*;
import java.util.concurrent.*;

final class Classifier {
    static final Classifier INSTANCE=new Classifier();
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Set<Runnable> listeners=new HashSet<>();
    volatile boolean running;
    volatile String status="";
    private Future<?> task;
    void listen(Runnable r){listeners.add(r);} void unlisten(Runnable r){listeners.remove(r);}
    private void update(String s){status=s;main.post(()->{for(Runnable r:new ArrayList<>(listeners))r.run();});}
    void cancel(){if(task!=null)task.cancel(true);update("Stopping… Completed categories are saved.");}
    void start(Context context) { start(context,null); }
    void start(Context context,String categoryId) {
        if(running)return;
        Context c=context.getApplicationContext(); running=true;
        task=executor.submit(()->{
            try {
                Catalog catalog=new Catalog(c); var settings=c.getSharedPreferences("settings",0);
                String key=KeyStore.read(c); if(key.isEmpty())throw new IllegalArgumentException("Add an API key in Settings first.");
                Map<String,String> categories=CategoryCriteria.choices(catalog.categories());
                List<Catalog.App> pending=new ArrayList<>();for(var app:catalog.scan())if(categoryId==null?app.category().isEmpty():app.category().equals(categoryId))pending.add(app);
                int done=0, skipped=0, enriched=0;
                for(var app:pending){
                    if(Thread.currentThread().isInterrupted())throw new InterruptedException();
                    if(catalog.isHidden(app.pkg())){skipped++;continue;}
                    update("Categorising "+(done+1)+" / "+pending.size()+": "+app.name());
                    var appContext=AppContext.load(c,app);
                    if(appContext.has("google_play")||appContext.has("android_description"))enriched++;
                    String choice=SystemOne.classify(settings.getString("url",SystemOne.DEFAULT_URL),settings.getString("model",SystemOne.DEFAULT_MODEL),key,app,categories,appContext);
                    if(Thread.currentThread().isInterrupted())throw new InterruptedException();
                    if(catalog.assignAiIfUnchanged(app,choice))done++;else skipped++;
                }
                running=false;update("Done. "+done+" apps categorised; "+enriched+" had descriptions."+(skipped>0?" "+skipped+" edited or hidden apps left unchanged.":""));
            }catch(InterruptedException e){Thread.currentThread().interrupt();running=false;update("Stopped. Completed categories are saved.");}
            catch(Exception e){running=false;update(e.getMessage()==null?"Categorisation failed. Retry to resume.":e.getMessage());}
        });
    }
}
