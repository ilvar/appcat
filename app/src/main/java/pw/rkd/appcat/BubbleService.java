package pw.rkd.appcat;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public final class BubbleService extends Service {
    private WindowManager wm;
    private TextView bubble;
    private View panel;
    private WindowManager.LayoutParams params;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private float downX,downY;private int originalX,originalY;private boolean dragged,longPressed;
    private final Runnable longPress=()->{longPressed=true;openPicker(null);};
    @Override public IBinder onBind(Intent i){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent!=null && "stop".equals(intent.getAction())){getSharedPreferences("settings",0).edit().putBoolean("bubble",false).apply();stopSelf();return START_NOT_STICKY;}
        if(!Settings.canDrawOverlays(this)||!getSharedPreferences("settings",0).getBoolean("bubble",false)){stopSelf();return START_NOT_STICKY;}
        NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("bubble","Floating category button",NotificationManager.IMPORTANCE_LOW));
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,BubbleService.class).setAction("stop"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);
        startForeground(1,new Notification.Builder(this,"bubble").setSmallIcon(R.drawable.ic_appcat).setContentTitle("AppCat floating button").setContentText("Long press the circle to choose a category").setContentIntent(open).addAction(new Notification.Action.Builder(null,"Stop",stop).build()).build());
        if(bubble==null)try{addBubble();}catch(Exception e){stopSelf();return START_NOT_STICKY;}
        return START_STICKY;
    }
    private WindowManager.LayoutParams layout(int w,int h){return new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,android.graphics.PixelFormat.TRANSLUCENT);}
    private void addBubble(){
        wm=getSystemService(WindowManager.class);bubble=new TextView(this);bubble.setText("▦");bubble.setTextSize(26);bubble.setTextColor(Color.WHITE);bubble.setGravity(Gravity.CENTER);bubble.setContentDescription("AppCat categories. Long press to choose; drag to move.");bubble.setBackground(Ui.rounded(0xE6536DFE,Ui.dp(this,28)));
        params=layout(Ui.dp(this,52),Ui.dp(this,52));params.gravity=Gravity.TOP|Gravity.LEFT;
        var p=getSharedPreferences("settings",0);params.x=p.getInt("bubbleX",getResources().getDisplayMetrics().widthPixels-Ui.dp(this,56));params.y=p.getInt("bubbleY",Ui.dp(this,250));clamp();
        bubble.setOnClickListener(v->openPicker(null));bubble.setOnLongClickListener(v->{openPicker(null);return true;});
        bubble.setOnTouchListener((v,e)->{
            switch(e.getActionMasked()){
                case MotionEvent.ACTION_DOWN:downX=e.getRawX();downY=e.getRawY();originalX=params.x;originalY=params.y;dragged=false;longPressed=false;handler.postDelayed(longPress,ViewConfiguration.getLongPressTimeout());return true;
                case MotionEvent.ACTION_MOVE:
                    if(Math.hypot(e.getRawX()-downX,e.getRawY()-downY)>ViewConfiguration.get(this).getScaledTouchSlop()){dragged=true;handler.removeCallbacks(longPress);}
                    if(dragged){params.x=originalX+(int)(e.getRawX()-downX);params.y=originalY+(int)(e.getRawY()-downY);clamp();wm.updateViewLayout(bubble,params);}return true;
                case MotionEvent.ACTION_UP:
                    handler.removeCallbacks(longPress);if(!dragged&&!longPressed)v.performClick();if(dragged){params.x=params.x<getResources().getDisplayMetrics().widthPixels/2?0:getResources().getDisplayMetrics().widthPixels-params.width;wm.updateViewLayout(bubble,params);getSharedPreferences("settings",0).edit().putInt("bubbleX",params.x).putInt("bubbleY",params.y).apply();}return true;
                case MotionEvent.ACTION_CANCEL:handler.removeCallbacks(longPress);return true;
            }return false;
        });wm.addView(bubble,params);
    }
    private void clamp(){params.x=Math.max(0,Math.min(params.x,getResources().getDisplayMetrics().widthPixels-params.width));params.y=Math.max(Ui.dp(this,32),Math.min(params.y,getResources().getDisplayMetrics().heightPixels-Ui.dp(this,100)));}
    private void closePicker(){if(panel!=null){wm.removeView(panel);panel=null;}}
    private void openPicker(String category){
        closePicker();Catalog catalog=new Catalog(this);var apps=catalog.scan();LinearLayout card=Ui.column(this);card.setBackground(Ui.rounded(Color.WHITE,Ui.dp(this,18)));ScrollView scroll=new ScrollView(this);scroll.addView(card);panel=scroll;
        card.addView(Ui.text(this,category==null?"Categories":catalog.categoryName(category),22));card.addView(Ui.button(this,"Close",this::closePicker));
        if(category==null){
            for(var cat:catalog.categories()){long n=apps.stream().filter(a->a.category().equals(cat.id())).count();card.addView(Ui.button(this,cat.name()+" · "+n,()->openPicker(cat.id())));}
            long n=apps.stream().filter(a->a.category().isEmpty()).count();if(n>0)card.addView(Ui.button(this,"Uncategorised · "+n,()->openPicker("")));
        }else{
            card.addView(Ui.button(this,"‹ Categories",()->openPicker(null)));
            if(apps.stream().noneMatch(a->a.category().equals(category)))card.addView(Ui.text(this,"No apps in this category yet",16));
            for(var app:apps)if(app.category().equals(category))card.addView(Ui.button(this,app.name(),()->{closePicker();Ui.launch(this,app.pkg());}));
        }
        var lp=layout(Math.min(Ui.dp(this,340),getResources().getDisplayMetrics().widthPixels-Ui.dp(this,24)),Math.min(Ui.dp(this,520),getResources().getDisplayMetrics().heightPixels-Ui.dp(this,120)));lp.gravity=Gravity.CENTER;wm.addView(panel,lp);
    }
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);if(wm!=null){closePicker();if(bubble!=null)wm.removeView(bubble);}bubble=null;super.onDestroy();}
}
