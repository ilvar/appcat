package pw.rkd.appcat;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.text.InputType;
import android.widget.*;
import java.util.*;

public final class MainActivity extends Activity {
    private Catalog catalog;
    private LinearLayout content;
    private TextView status;
    private Button classify;
    private boolean allApps;
    private String search="";
    private final Runnable listener=()->{if(status!=null){status.setText(Classifier.INSTANCE.status);classify.setText(Classifier.INSTANCE.running?"Stop categorisation":"Categorise missing apps");if(!Classifier.INSTANCE.running)render();}};
    @Override public void onCreate(Bundle state){super.onCreate(state);catalog=new Catalog(this);if(state!=null){allApps=state.getBoolean("apps");search=state.getString("search","");}render();}
    @Override protected void onStart(){super.onStart();Classifier.INSTANCE.listen(listener);}
    @Override protected void onStop(){Classifier.INSTANCE.unlisten(listener);super.onStop();}
    @Override protected void onSaveInstanceState(Bundle b){b.putBoolean("apps",allApps);b.putString("search",search);super.onSaveInstanceState(b);}
    @Override protected void onResume(){super.onResume();render();var p=getSharedPreferences("settings",0);if(p.getBoolean("bubble",false)&&Settings.canDrawOverlays(this))startBubble();}
    private void render(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);if(Build.VERSION.SDK_INT>=30)Ui.insets(root);setContentView(root);
        LinearLayout header=Ui.column(this);root.addView(header);header.addView(Ui.text(this,"AppCat",30));
        var apps=catalog.scan();long unknown=apps.stream().filter(a->a.category().isEmpty()).count();
        header.addView(Ui.text(this,apps.size()+" apps · "+unknown+" uncategorised",14));
        LinearLayout actions=new LinearLayout(this);header.addView(actions);
        actions.addView(Ui.button(this,allApps?"Categories":"All apps",()->{allApps=!allApps;search="";render();}),new LinearLayout.LayoutParams(0,-2,1));
        actions.addView(Ui.button(this,"Settings",this::settings),new LinearLayout.LayoutParams(0,-2,1));
        classify=Ui.button(this,Classifier.INSTANCE.running?"Stop categorisation":"Categorise missing apps",()->{if(Classifier.INSTANCE.running)Classifier.INSTANCE.cancel();else{Classifier.INSTANCE.start(this);listener.run();}});header.addView(classify);
        status=Ui.text(this,Classifier.INSTANCE.status,14);header.addView(status);
        header.addView(Ui.button(this,"+ Custom category",()->categoryDialog(null,null)));
        EditText filter=Ui.input(this,"Search apps or categories",search);header.addView(filter);
        ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));content=Ui.column(this);scroll.addView(content);populate(apps,unknown);
        filter.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int n,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){search=s.toString();populate(apps,unknown);}public void afterTextChanged(Editable e){}});
    }
    private void populate(List<Catalog.App> apps,long unknown){
        content.removeAllViews();String query=search.toLowerCase(Locale.ROOT);
        if(allApps){for(var app:apps)if((app.name()+" "+app.pkg()+" "+catalog.categoryName(app.category())).toLowerCase(Locale.ROOT).contains(query))Ui.appRow(this,content,app,()->assignDialog(app));}
        else {
            if(unknown>0)content.addView(Ui.text(this,"Categorise or manually assign the remaining apps to enable home-screen shortcuts.",14));
            for(var cat:catalog.categories()) {
                if(!cat.name().toLowerCase(Locale.ROOT).contains(query))continue;
                long count=apps.stream().filter(a->a.category().equals(cat.id())).count();
                LinearLayout card=Ui.column(this);card.setBackground(Ui.rounded(0xFFFFFFFF,Ui.dp(this,16)));content.addView(card);((LinearLayout.LayoutParams)card.getLayoutParams()).bottomMargin=Ui.dp(this,10);
                card.addView(Ui.text(this,cat.name()+"  ·  "+count,20));
                LinearLayout controls=new LinearLayout(this);card.addView(controls);
                controls.addView(Ui.button(this,"Open",()->startActivity(new Intent(this,CategoryActivity.class).putExtra("category",cat.id()))),new LinearLayout.LayoutParams(0,-2,1));
                Button pin=Ui.button(this,"Add to home",()->Shortcuts.pin(this,cat));pin.setEnabled(unknown==0);controls.addView(pin,new LinearLayout.LayoutParams(0,-2,1));
                controls.addView(Ui.button(this,"Rename",()->categoryDialog(cat.id(),null)));
            }
        }
    }
    private void assignDialog(Catalog.App app){
        var cats=catalog.categories();String[] names=new String[cats.size()+2];names[0]="Uncategorised";for(int i=0;i<cats.size();i++)names[i+1]=cats.get(i).name();names[names.length-1]="+ New category";
        new AlertDialog.Builder(this).setTitle(app.name()).setItems(names,(d,n)->{
            if(n==names.length-1){categoryDialog(null,app);return;}
            catalog.assign(app.pkg(),n==0?"":cats.get(n-1).id(),"Manual");render();
        }).show();
    }
    private void categoryDialog(String id,Catalog.App app){
        EditText name=Ui.input(this,"Category name",id==null?"":catalog.categoryName(id));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(id==null?"New category":"Rename category").setView(name).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{try{String saved=catalog.saveCategory(id,name.getText().toString());if(app!=null)catalog.assign(app.pkg(),saved,"Manual");Shortcuts.refresh(this);dialog.dismiss();render();}catch(Exception e){name.setError(e.getMessage());}}));dialog.show();
    }
    private void settings(){
        var p=getSharedPreferences("settings",0);LinearLayout form=Ui.column(this);ScrollView scroll=new ScrollView(this);scroll.addView(form);
        form.addView(Ui.text(this,"System One sends only app names and package IDs for apps missing a category to the configured service. Requests use your account credit.",14));
        form.addView(Ui.text(this,"API key",14));EditText key=Ui.input(this,"API key","");key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);try{key.setText(KeyStore.read(this));}catch(Exception e){Ui.toast(this,"Stored key unavailable. Enter it again.");}form.addView(key);
        form.addView(Ui.text(this,"Full System One endpoint (HTTPS)",14));EditText url=Ui.input(this,"Endpoint",p.getString("url",SystemOne.DEFAULT_URL));url.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);form.addView(url);
        form.addView(Ui.text(this,"System One model",14));EditText model=Ui.input(this,"Model",p.getString("model",SystemOne.DEFAULT_MODEL));form.addView(model);
        Switch bubble=new Switch(this);bubble.setText("Floating category button");bubble.setChecked(p.getBoolean("bubble",false));form.addView(bubble);
        form.addView(Ui.text(this,"Drag the circle to position it. Tap or long press to open categories. Enabling it requests Android’s “display over other apps” permission.",14));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Settings").setView(scroll).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{
            try{
                SystemOne.validateUrl(url.getText().toString().trim());if(model.getText().toString().trim().isEmpty())throw new IllegalArgumentException("Enter a System One model");
                KeyStore.save(this,key.getText().toString().trim());p.edit().putString("url",url.getText().toString().trim()).putString("model",model.getText().toString().trim()).putBoolean("bubble",bubble.isChecked()).apply();
                if(bubble.isChecked()) {
                    if(!Settings.canDrawOverlays(this))startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));
                    else startBubble();
                    if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},1);
                }else stopService(new Intent(this,BubbleService.class));
                dialog.dismiss();
            }catch(Exception e){Ui.toast(this,e.getMessage()==null?"Could not save settings":e.getMessage());}
        }));dialog.show();
    }
    private void startBubble(){try{startForegroundService(new Intent(this,BubbleService.class));}catch(Exception e){Ui.toast(this,"Could not start floating button. Try switching it off and on.");}}
}
