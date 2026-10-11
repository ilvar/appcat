package pw.rkd.appcat;

import android.app.*;
import android.os.Bundle;
import android.widget.*;

public final class CategoryActivity extends androidx.appcompat.app.AppCompatActivity {
    private String currentCategory;
    @Override protected void onResume(){super.onResume();show(currentCategory);}
    @Override protected void onSaveInstanceState(Bundle state){state.putString("category",currentCategory);super.onSaveInstanceState(state);}
    @Override public void onCreate(Bundle state){setTheme(PopupTheme.activityStyle(this));super.onCreate(state);show(state==null?getIntent().getStringExtra("category"):state.getString("category"));}
    private void show(String id){
        currentCategory=id;
        Catalog catalog=new Catalog(this);var apps=catalog.scan();ScrollView scroll=new ScrollView(this);android.content.Context popup=PopupTheme.context(this);LinearLayout list=Ui.column(popup);scroll.addView(list);setContentView(scroll);
        Ui.pickerHeader(popup,list,id==null?"Categories":catalog.categoryName(id),this::finish);
        if(id==null){
            Ui.categoryGrid(popup,list,catalog,apps,this::show);
            if(apps.isEmpty())list.addView(Ui.text(popup,"No launchable apps found",16));
        } else {
            list.addView(Ui.button(popup,"‹ All categories",()->show(null)));int count=0;
            java.util.List<Catalog.App> members=new java.util.ArrayList<>();
            for(var app:apps)if(app.category().equals(id))members.add(app);
            count=members.size();Ui.appGrid(popup,list,members,app->Ui.launch(this,app.pkg()));
            if(count==0)list.addView(Ui.text(popup,"No apps in this category yet",16));
        }

        getWindow().setLayout(Math.min(Ui.dp(this,360),getResources().getDisplayMetrics().widthPixels-Ui.dp(this,24)),android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
