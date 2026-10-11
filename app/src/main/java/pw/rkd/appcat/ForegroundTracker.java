package pw.rkd.appcat;

/** Uses activity transitions, not accumulated screen time, to identify the current app. */
final class ForegroundTracker {
    private String current="";
    private long timestamp;
    void event(String pkg,int type,long at){
        if(pkg==null||at<timestamp)return;
        if(type==1){current=pkg;timestamp=at;}
        else if(type==2 && pkg.equals(current)){current="";timestamp=at;}
    }
    boolean isHome(String home){return home!=null&&!home.isEmpty()&&home.equals(current);}
    void clear(){current="";timestamp=0;}
}
