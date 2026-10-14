package pw.rkd.appcat;

import android.app.AppOpsManager;
import android.app.KeyguardManager;
import android.app.usage.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.PowerManager;

final class HomeScreenMonitor {
    private final Context context;
    private final ForegroundTracker tracker=new ForegroundTracker();
    private long lastQuery;
    HomeScreenMonitor(Context context){this.context=context;}
    static boolean hasAccess(Context c){return c.getSystemService(AppOpsManager.class).checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),c.getPackageName())==AppOpsManager.MODE_ALLOWED;}
    boolean isHome(){
        if(!hasAccess(context)){lastQuery=0;tracker.clear();return false;}
        if(!context.getSystemService(PowerManager.class).isInteractive()||context.getSystemService(KeyguardManager.class).isKeyguardLocked())return false;
        long now=System.currentTimeMillis();long start=lastQuery==0?now-60*60*1000:Math.min(lastQuery-5000,now);
        UsageEvents events=context.getSystemService(UsageStatsManager.class).queryEvents(start,now);
        if(events==null)return false;
        UsageEvents.Event event=new UsageEvents.Event();
        while(events.hasNextEvent()){events.getNextEvent(event);tracker.event(event.getPackageName(),event.getEventType(),event.getTimeStamp());}
        lastQuery=now;
        var home=context.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),PackageManager.MATCH_DEFAULT_ONLY);
        return home!=null&&home.activityInfo!=null&&tracker.isHome(home.activityInfo.packageName);
    }
}
