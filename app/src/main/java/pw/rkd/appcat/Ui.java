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
    static TextView text(Context c,String s,int size){TextView t=new TextView(c);t.setText(s);t.setTextSize(size);var colors=c.obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});t.setTextColor(colors.getColor(0,Color.DKGRAY));colors.recycle();t.setPadding(0,dp(c,6),0,dp(c,6));return t;}
    static Button button(Context c,String s,Runnable action){Button b=new com.google.android.material.button.MaterialButton(c);b.setText(s);b.setAllCaps(false);b.setOnClickListener(v->action.run());return b;}
    static EditText input(Context c,String hint,String value){EditText e=new com.google.android.material.textfield.TextInputEditText(c);e.setHint(hint);e.setText(value);e.setSingleLine(true);return e;}
    static void toast(Context c,String s){Toast.makeText(c,s,Toast.LENGTH_LONG).show();}
    static void launch(Context c,String pkg){Intent i=c.getPackageManager().getLaunchIntentForPackage(pkg);if(i==null){toast(c,"App is no longer available");return;}try{c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception e){toast(c,"Cannot open this app");}}
    static GradientDrawable rounded(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;}
    @android.annotation.TargetApi(30)
    static void insets(View view){view.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets bars=i.getInsets(WindowInsets.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return i;});}
    record Tile(String label,android.graphics.drawable.Drawable icon,Runnable click) {}
    static ImageButton iconButton(Context c,int drawable,String description,Runnable action){
        ImageButton button=new ImageButton(c);button.setImageResource(drawable);button.setContentDescription(description);button.setBackgroundResource(android.R.drawable.list_selector_background);button.setPadding(dp(c,12),dp(c,12),dp(c,12),dp(c,12));button.setOnClickListener(v->action.run());return button;
    }
    static void pickerHeader(Context c,LinearLayout parent,String title,Runnable close){
        LinearLayout header=new LinearLayout(c);header.setGravity(Gravity.CENTER_VERTICAL);header.addView(text(c,title,22),new LinearLayout.LayoutParams(0,-2,1));header.addView(iconButton(c,R.drawable.ic_close,"Close",close),new LinearLayout.LayoutParams(dp(c,48),dp(c,48)));parent.addView(header);
    }
    static void categoryGrid(Context c,LinearLayout parent,Catalog catalog,java.util.List<Catalog.App> apps,java.util.function.Consumer<String> open){
        java.util.List<Tile> tiles=new java.util.ArrayList<>();
        for(var category:catalog.categories()){
            long count=apps.stream().filter(a->a.category().equals(category.id())).count();
            tiles.add(new Tile(category.name()+" · "+count,CategoryIcons.drawable(c,category.icon()),()->open.accept(category.id())));
        }
        long unknown=apps.stream().filter(a->a.category().isEmpty()).count();if(unknown>0)tiles.add(new Tile("Uncategorised · "+unknown,CategoryIcons.drawable(c,"apps"),()->open.accept("")));
        tileGrid(c,parent,tiles);
    }
    static void appGrid(Context context,LinearLayout parent,java.util.List<Catalog.App> apps,java.util.function.Consumer<Catalog.App> open) {
        java.util.List<Tile> tiles=new java.util.ArrayList<>();
        for(var app:apps){android.graphics.drawable.Drawable icon;try{icon=context.getPackageManager().getApplicationIcon(app.pkg());}catch(Exception e){icon=context.getDrawable(R.drawable.ic_appcat);}tiles.add(new Tile(app.name(),icon,()->open.accept(app)));}
        tileGrid(context,parent,tiles);
    }
    static void tileGrid(Context context,LinearLayout parent,java.util.List<Tile> tiles) {
        GridLayout grid=new GridLayout(context) {
            @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
                super.onSizeChanged(w,h,oldw,oldh);
                if(w<=0||w==oldw)return;
                int columns=Math.max(1,w/dp(context,80));setColumnCount(columns);
                for(int i=0;i<getChildCount();i++) {
                    GridLayout.LayoutParams lp=new GridLayout.LayoutParams(GridLayout.spec(i/columns),GridLayout.spec(i%columns));
                    lp.width=w/columns;lp.height=dp(context,112);getChildAt(i).setLayoutParams(lp);
                }
            }
        };
        grid.setColumnCount(3);parent.addView(grid,new LinearLayout.LayoutParams(-1,-2));
        for(var item:tiles) {
            LinearLayout tile=new LinearLayout(context);tile.setOrientation(LinearLayout.VERTICAL);tile.setGravity(Gravity.CENTER);tile.setPadding(dp(context,4),dp(context,8),dp(context,4),dp(context,4));
            ImageView icon=new ImageView(context);icon.setImageDrawable(item.icon());icon.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            tile.addView(icon,new LinearLayout.LayoutParams(dp(context,48),dp(context,48)));
            TextView label=text(context,item.label(),12);label.setGravity(Gravity.CENTER);label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tile.addView(label,new LinearLayout.LayoutParams(-1,-2));tile.setContentDescription(item.label());tile.setFocusable(true);tile.setBackgroundResource(android.R.drawable.list_selector_background);tile.setOnClickListener(v->item.click().run());
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=dp(context,80);lp.height=dp(context,112);grid.addView(tile,lp);
        }
    }
    static void appRow(Activity a,LinearLayout list,Catalog.App app,Runnable edit){
        LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,Ui.dp(a,5),0,Ui.dp(a,5));
        ImageView icon=new ImageView(a);try{icon.setImageDrawable(a.getPackageManager().getApplicationIcon(app.pkg()));}catch(Exception ignored){}
        row.addView(icon,new LinearLayout.LayoutParams(dp(a,40),dp(a,40)));
        LinearLayout labels=column(a);labels.addView(text(a,app.name(),16));labels.addView(text(a,new Catalog(a).categoryName(app.category())+" · "+app.source(),12));
        row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));row.setOnClickListener(v->launch(a,app.pkg()));row.setOnLongClickListener(v->{if(edit!=null)edit.run();return true;});
        if(edit!=null)row.addView(iconButton(a,R.drawable.ic_more_vert,"Change category for "+app.name(),edit),new LinearLayout.LayoutParams(dp(a,48),dp(a,48)));list.addView(row);
    }
}
