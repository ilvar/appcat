package pw.rkd.appcat;

import android.app.*;
import android.os.Bundle;
import android.widget.*;

public final class CategoryActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);show(getIntent().getStringExtra("category"));}
    private void show(String id){
        Catalog catalog=new Catalog(this);var apps=catalog.scan();ScrollView scroll=new ScrollView(this);LinearLayout list=Ui.column(this);scroll.addView(list);setContentView(scroll);
        list.addView(Ui.text(this,id==null?"Categories":catalog.categoryName(id),24));
        if(id==null){
            for(var cat:catalog.categories()){long count=apps.stream().filter(a->a.category().equals(cat.id())).count();list.addView(Ui.button(this,cat.name()+"  ·  "+count,()->show(cat.id())));}
            long unknown=apps.stream().filter(a->a.category().isEmpty()).count();if(unknown>0)list.addView(Ui.button(this,"Uncategorised  ·  "+unknown,()->show("")));
            if(apps.isEmpty())list.addView(Ui.text(this,"No launchable apps found",16));
        } else {
            list.addView(Ui.button(this,"‹ All categories",()->show(null)));int count=0;
            for(var app:apps)if(app.category().equals(id)){Ui.appRow(this,list,app,null);count++;}
            if(count==0)list.addView(Ui.text(this,"No apps in this category yet",16));
        }
        list.addView(Ui.button(this,"Close",this::finish));
        getWindow().setLayout(Math.min(Ui.dp(this,360),getResources().getDisplayMetrics().widthPixels-Ui.dp(this,24)),android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
