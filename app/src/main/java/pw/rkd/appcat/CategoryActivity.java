package pw.rkd.appcat;

import android.app.*;
import android.os.Bundle;
import android.widget.*;

public final class CategoryActivity extends androidx.appcompat.app.AppCompatActivity {
    @Override public void onCreate(Bundle state){super.onCreate(state);show(getIntent().getStringExtra("category"));}
    private void show(String id){
        Catalog catalog=new Catalog(this);var apps=catalog.scan();ScrollView scroll=new ScrollView(this);LinearLayout list=Ui.column(this);scroll.addView(list);setContentView(scroll);
        Ui.pickerHeader(this,list,id==null?"Categories":catalog.categoryName(id),this::finish);
        if(id==null){
            Ui.categoryGrid(this,list,catalog,apps,this::show);
            if(apps.isEmpty())list.addView(Ui.text(this,"No launchable apps found",16));
        } else {
            list.addView(Ui.button(this,"‹ All categories",()->show(null)));int count=0;
            java.util.List<Catalog.App> members=new java.util.ArrayList<>();
            for(var app:apps)if(app.category().equals(id))members.add(app);
            count=members.size();Ui.appGrid(this,list,members,app->Ui.launch(this,app.pkg()));
            if(count==0)list.addView(Ui.text(this,"No apps in this category yet",16));
        }

        getWindow().setLayout(Math.min(Ui.dp(this,360),getResources().getDisplayMetrics().widthPixels-Ui.dp(this,24)),android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
