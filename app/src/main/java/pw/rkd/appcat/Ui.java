package pw.rkd.appcat;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

final class Ui {
    static int dp(Context c,int n){return (int)(n*c.getResources().getDisplayMetrics().density+.5f);}
    static LinearLayout column(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.VERTICAL);v.setPadding(dp(c,16),dp(c,12),dp(c,16),dp(c,12));return v;}
    static TextView text(Context c,String s,int size){TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(33,39,58));t.setPadding(0,dp(c,6),0,dp(c,6));return t;}
    static Button button(Context c,String s,Runnable action){Button b=new Button(c);b.setText(s);b.setAllCaps(false);b.setOnClickListener(v->action.run());return b;}
    static EditText input(Context c,String hint,String value){EditText e=new EditText(c);e.setHint(hint);e.setText(value);e.setSingleLine(true);return e;}
    static void toast(Context c,String s){Toast.makeText(c,s,Toast.LENGTH_LONG).show();}
    static void launch(Context c,String pkg){Intent i=c.getPackageManager().getLaunchIntentForPackage(pkg);if(i==null){toast(c,"App is no longer available");return;}try{c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception e){toast(c,"Cannot open this app");}}
    static GradientDrawable rounded(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;}
    @android.annotation.TargetApi(30)
    static void insets(View view){view.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets bars=i.getInsets(WindowInsets.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return i;});}
    static void appRow(Activity a,LinearLayout list,Catalog.App app,Runnable edit){
        LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,Ui.dp(a,5),0,Ui.dp(a,5));
        ImageView icon=new ImageView(a);try{icon.setImageDrawable(a.getPackageManager().getApplicationIcon(app.pkg()));}catch(Exception ignored){}
        row.addView(icon,new LinearLayout.LayoutParams(dp(a,40),dp(a,40)));
        LinearLayout labels=column(a);labels.addView(text(a,app.name(),16));labels.addView(text(a,new Catalog(a).categoryName(app.category())+" · "+app.source(),12));
        row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));row.setOnClickListener(v->launch(a,app.pkg()));row.setOnLongClickListener(v->{if(edit!=null)edit.run();return true;});
        if(edit!=null)row.addView(button(a,"Edit",edit));list.addView(row);
    }
}
