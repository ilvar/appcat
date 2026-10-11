package pw.rkd.appcat;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public final class BubbleService extends Service {
    private HomeScreenMonitor homeMonitor;
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener catalogListener=(prefs,key)->{if("hidden".equals(key)||"categories".equals(key))closePicker();};
    @Override public void onCreate(){super.onCreate();getSharedPreferences("catalog",0).registerOnSharedPreferenceChangeListener(catalogListener);}
    private final Runnable visibilityTick=new Runnable(){public void run(){
        if(bubble!=null){boolean homeOnly=getSharedPreferences("settings",0).getBoolean("homeOnly",false);boolean visible;
            try{visible=!homeOnly||homeMonitor.isHome();}catch(Exception e){visible=!homeOnly;}
            bubble.setVisibility(visible?View.VISIBLE:View.GONE);
            if(!visible){handler.removeCallbacks(longPress);closePicker();}
        }
        handler.postDelayed(this,750);
    }};
    private WindowManager wm;
    private ImageButton bubble;
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
        if(homeMonitor==null)homeMonitor=new HomeScreenMonitor(this);handler.removeCallbacks(visibilityTick);visibilityTick.run();
        return START_STICKY;
    }
    private WindowManager.LayoutParams layout(int w,int h){return new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,android.graphics.PixelFormat.TRANSLUCENT);}
    private void addBubble(){
        wm=getSystemService(WindowManager.class);bubble=new ImageButton(this);bubble.setImageResource(R.drawable.ic_apps);bubble.setImageTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));bubble.setPadding(Ui.dp(this,14),Ui.dp(this,14),Ui.dp(this,14),Ui.dp(this,14));bubble.setContentDescription("AppCat categories. Long press to choose; drag to move.");bubble.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x40FFFFFF),Ui.rounded(0xE6536DFE,Ui.dp(this,28)),Ui.rounded(Color.WHITE,Ui.dp(this,28))));
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
        });if(getSharedPreferences("settings",0).getBoolean("homeOnly",false))bubble.setVisibility(View.GONE);wm.addView(bubble,params);
    }
    private void clamp(){params.x=Math.max(0,Math.min(params.x,getResources().getDisplayMetrics().widthPixels-params.width));params.y=Math.max(Ui.dp(this,32),Math.min(params.y,getResources().getDisplayMetrics().heightPixels-Ui.dp(this,100)));}
    private void closePicker(){if(panel!=null){wm.removeView(panel);panel=null;}}
    private void openPicker(String category){
        closePicker();Context popup=PopupTheme.context(this);Catalog catalog=new Catalog(this);var apps=catalog.scan();LinearLayout card=Ui.column(popup);card.setBackground(Ui.rounded(com.google.android.material.color.MaterialColors.getColor(popup,com.google.android.material.R.attr.colorSurface,Color.WHITE),Ui.dp(this,18)));ScrollView scroll=new ScrollView(popup);scroll.addView(card);
        Ui.pickerHeader(popup,card,category==null?"Categories":catalog.categoryName(category),this::closePicker);
        if(category==null){
            Ui.categoryGrid(popup,card,catalog,apps,this::openPicker);
        }else{
            card.addView(Ui.button(popup,"‹ Categories",()->openPicker(null)));
            if(apps.stream().noneMatch(a->a.category().equals(category)))card.addView(Ui.text(popup,"No apps in this category yet",12));
            java.util.List<Catalog.App> members=new java.util.ArrayList<>();
            for(var app:apps)if(app.category().equals(category))members.add(app);
            Ui.appGrid(popup,card,members,app->{closePicker();Ui.launch(this,app.pkg());});
        }
        var lp=layout(Math.min(Ui.dp(this,340),getResources().getDisplayMetrics().widthPixels-Ui.dp(this,24)),Math.min(Ui.dp(this,520),getResources().getDisplayMetrics().heightPixels-Ui.dp(this,120)));lp.gravity=Gravity.CENTER;try{wm.addView(scroll,lp);panel=scroll;}catch(RuntimeException e){Ui.toast(this,"Could not open floating categories");}
    }
    @Override public void onDestroy(){getSharedPreferences("catalog",0).unregisterOnSharedPreferenceChangeListener(catalogListener);handler.removeCallbacksAndMessages(null);if(wm!=null){closePicker();if(bubble!=null)wm.removeView(bubble);}bubble=null;super.onDestroy();}
}
