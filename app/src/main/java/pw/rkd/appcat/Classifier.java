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
    void start(Context context) {
        if(running)return;
        Context c=context.getApplicationContext(); running=true;
        task=executor.submit(()->{
            try {
                Catalog catalog=new Catalog(c); var settings=c.getSharedPreferences("settings",0);
                String key=KeyStore.read(c); if(key.isEmpty())throw new IllegalArgumentException("Add an API key in Settings first.");
                Map<String,String> categories=new LinkedHashMap<>(); for(var cat:catalog.categories())categories.put(cat.id(),cat.name());
                List<Catalog.App> pending=new ArrayList<>();for(var app:catalog.scan())if(app.category().isEmpty())pending.add(app);
                int done=0;
                for(var app:pending){
                    if(Thread.currentThread().isInterrupted())throw new InterruptedException();
                    update("Categorising "+(done+1)+" / "+pending.size()+": "+app.name());
                    String choice=SystemOne.classify(settings.getString("url",SystemOne.DEFAULT_URL),settings.getString("model",SystemOne.DEFAULT_MODEL),key,app,categories);
                    if(Thread.currentThread().isInterrupted())throw new InterruptedException();
                    catalog.assignAiIfUnknown(app.pkg(),choice);done++;
                }
                running=false;update("Done. "+done+" apps categorised.");
            }catch(InterruptedException e){Thread.currentThread().interrupt();running=false;update("Stopped. Completed categories are saved.");}
            catch(Exception e){running=false;update(e.getMessage()==null?"Categorisation failed. Retry to resume.":e.getMessage());}
        });
    }
}
