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

public final class MainActivity extends androidx.appcompat.app.AppCompatActivity {
    private androidx.appcompat.app.AlertDialog settingsDialog;
    private Catalog catalog;
    private LinearLayout content;
    private TextView status;
    private Button classify;
    private boolean allApps;
    private String search="";
    private final Runnable listener=()->{if(status!=null){status.setText(Classifier.INSTANCE.status);status.setVisibility(Classifier.INSTANCE.status.isEmpty()?android.view.View.GONE:android.view.View.VISIBLE);classify.setText(Classifier.INSTANCE.running?"Stop categorisation":"Categorise missing apps");if(!Classifier.INSTANCE.running)render();}};
    @Override public void onCreate(Bundle state){super.onCreate(state);androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(),false);catalog=new Catalog(this);if(state!=null){allApps=state.getBoolean("apps");search=state.getString("search","");}render();}
    @Override protected void onStart(){super.onStart();Classifier.INSTANCE.listen(listener);}
    @Override protected void onStop(){Classifier.INSTANCE.unlisten(listener);super.onStop();}
    @Override protected void onSaveInstanceState(Bundle b){b.putBoolean("apps",allApps);b.putString("search",search);super.onSaveInstanceState(b);}
    @Override protected void onResume(){super.onResume();render();var p=getSharedPreferences("settings",0);if(p.getBoolean("bubble",false)&&Settings.canDrawOverlays(this))startBubble();}
    private void render(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);Ui.insets(root);setContentView(root);
        com.google.android.material.appbar.MaterialToolbar toolbar=new com.google.android.material.appbar.MaterialToolbar(this);toolbar.setTitle("AppCat");root.addView(toolbar);
        toolbar.getMenu().add(0,1,0,"New category").setIcon(R.drawable.ic_add).setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS);
        toolbar.getMenu().add(0,2,1,"Settings").setIcon(R.drawable.ic_settings).setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS);
        toolbar.getMenu().add(0,3,2,"Reset all assignments");
        toolbar.setOnMenuItemClickListener(item->{if(item.getItemId()==1)categoryDialog(null,null);else if(item.getItemId()==2)settings();else resetApps();return true;});
        com.google.android.material.tabs.TabLayout tabs=new com.google.android.material.tabs.TabLayout(this);tabs.addTab(tabs.newTab().setText("Categories"));tabs.addTab(tabs.newTab().setText("Apps"));tabs.selectTab(tabs.getTabAt(allApps?1:0));root.addView(tabs);
        tabs.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener(){public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab){allApps=tab.getPosition()==1;search="";render();}public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab){}public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab){}});
        LinearLayout header=Ui.column(this);root.addView(header);
        var apps=catalog.scan();long unknown=apps.stream().filter(app->app.category().isEmpty()).count();
        header.addView(Ui.text(this,apps.size()+" apps · "+unknown+" uncategorised",14));
        EditText filter=Ui.input(this,"Search apps or categories",search);
        com.google.android.material.textfield.TextInputLayout searchBox=new com.google.android.material.textfield.TextInputLayout(this,null,com.google.android.material.R.attr.textInputOutlinedStyle);searchBox.setHint("Search apps or categories");filter.setHint(null);searchBox.setEndIconMode(com.google.android.material.textfield.TextInputLayout.END_ICON_CLEAR_TEXT);searchBox.addView(filter,new LinearLayout.LayoutParams(-1,-2));header.addView(searchBox);
        classify=Ui.button(this,Classifier.INSTANCE.running?"Stop categorisation":"Categorise missing apps",()->{if(Classifier.INSTANCE.running)Classifier.INSTANCE.cancel();else{Classifier.INSTANCE.start(this);render();}});
        if(unknown>0||Classifier.INSTANCE.running)header.addView(classify);
        status=Ui.text(this,Classifier.INSTANCE.status,14);status.setVisibility(Classifier.INSTANCE.status.isEmpty()?android.view.View.GONE:android.view.View.VISIBLE);header.addView(status);
        if(getSharedPreferences("settings",0).getBoolean("bubble",false)&&getSharedPreferences("settings",0).getBoolean("homeOnly",false)&&!HomeScreenMonitor.hasAccess(this))header.addView(Ui.button(this,"Grant Usage Access for home-only button",()->startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))));
        ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));content=Ui.column(this);scroll.addView(content);populate(apps,unknown);
        filter.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int n,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){search=s.toString();populate(apps,unknown);}public void afterTextChanged(Editable e){}});
    }
    private void populate(List<Catalog.App> apps,long unknown){
        content.removeAllViews();String query=search.toLowerCase(Locale.ROOT);
        if(allApps){for(var app:apps)if((app.name()+" "+app.pkg()+" "+catalog.categoryName(app.category())).toLowerCase(Locale.ROOT).contains(query))Ui.appRow(this,content,app,()->appActions(app));}
        else {
            if(unknown>0)content.addView(Ui.text(this,"Categorise or manually assign the remaining apps to enable home-screen shortcuts.",14));
            for(var cat:catalog.categories()) {
                if(!cat.name().toLowerCase(Locale.ROOT).contains(query))continue;
                long count=apps.stream().filter(a->a.category().equals(cat.id())).count();
                com.google.android.material.card.MaterialCardView card=new com.google.android.material.card.MaterialCardView(this);card.setRadius(Ui.dp(this,16));card.setCardElevation(0);content.addView(card,new LinearLayout.LayoutParams(-1,-2));((LinearLayout.LayoutParams)card.getLayoutParams()).bottomMargin=Ui.dp(this,8);
                LinearLayout row=new LinearLayout(this);row.setGravity(android.view.Gravity.CENTER_VERTICAL);row.setMinimumHeight(Ui.dp(this,72));row.setPadding(Ui.dp(this,12),Ui.dp(this,8),Ui.dp(this,4),Ui.dp(this,8));card.addView(row);
                ImageView icon=new ImageView(this);icon.setImageDrawable(CategoryIcons.drawable(this,cat.icon()));row.addView(icon,new LinearLayout.LayoutParams(Ui.dp(this,40),Ui.dp(this,40)));
                LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);labels.setPadding(Ui.dp(this,16),0,Ui.dp(this,8),0);TextView title=Ui.text(this,cat.name(),18);title.setPadding(0,0,0,0);labels.addView(title);TextView subtitle=Ui.text(this,count+" apps",13);subtitle.setPadding(0,Ui.dp(this,4),0,0);labels.addView(subtitle);row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
                row.setOnClickListener(v->startActivity(new Intent(this,CategoryActivity.class).putExtra("category",cat.id())));
                ImageButton menu=Ui.iconButton(this,R.drawable.ic_more_vert,"Actions for "+cat.name(),()->{});row.addView(menu,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
                menu.setOnClickListener(v->{androidx.appcompat.widget.PopupMenu popup=new androidx.appcompat.widget.PopupMenu(this,menu);popup.getMenu().add(0,1,0,"Add to home screen").setEnabled(unknown==0);popup.getMenu().add(0,2,1,"Rename");popup.getMenu().add(0,3,2,"Change icon");popup.getMenu().add(0,4,3,"Reclassify category").setEnabled(count>0&&!Classifier.INSTANCE.running);popup.setOnMenuItemClickListener(item->{switch(item.getItemId()){case 1:Shortcuts.pin(this,cat);break;case 2:categoryDialog(cat.id(),null);break;case 3:iconDialog(cat);break;case 4:reclassify(cat.id());break;}return true;});popup.show();});
            }
        }
    }
    private void iconDialog(Catalog.Category category){
        ScrollView scroll=new ScrollView(this);LinearLayout body=Ui.column(this);scroll.addView(body);
        androidx.appcompat.app.AlertDialog dialog=new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Icon for "+category.name()).setView(scroll).setNegativeButton("Cancel",null).create();
        java.util.List<Ui.Tile> tiles=new ArrayList<>();
        for(var option:CategoryIcons.OPTIONS)tiles.add(new Ui.Tile(option.label(),CategoryIcons.drawable(this,option.id()),()->{catalog.saveIcon(category.id(),option.id());Shortcuts.refresh(this);dialog.dismiss();render();}));
        Ui.tileGrid(this,body,tiles);dialog.show();
    }
    private void reclassify(String id){
        if(Classifier.INSTANCE.running){Ui.toast(this,"Stop the current categorisation first");return;}
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Reclassify "+catalog.categoryName(id)+"?")
                .setMessage("Run AI again for every app currently in this category, including manual and Android assignments. Apps can stay in this category. Existing assignments remain if a request fails.")
                .setNegativeButton("Cancel",null).setPositiveButton("Reclassify",(d,n)->{Classifier.INSTANCE.start(this,id);render();}).show();
    }
    private void resetApps(){
        if(Classifier.INSTANCE.running){Ui.toast(this,"Stop the current categorisation first");return;}
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Reset all app assignments?")
                .setMessage("Clear all manual, Android and AI assignments. Categories, shortcuts and settings stay. Apps remain uncategorised until you assign them or run AI; Android categories will not be reimported.")
                .setNegativeButton("Cancel",null).setPositiveButton("Reset all",(d,n)->{catalog.resetApps();Classifier.INSTANCE.status="All app assignments reset. Tap Categorise missing apps to run AI for every app.";render();}).show();
    }
    private void appActions(Catalog.App app){
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle(app.name()).setItems(new String[]{"Change category","Hide app"},(d,n)->{
            if(n==0)assignDialog(app);else{catalog.setHidden(app.pkg(),true);render();Ui.toast(this,"App hidden. Restore it in Settings → Hidden apps.");}
        }).show();
    }
    private void hiddenApps(){
        var hidden=catalog.hiddenPackages();if(hidden.isEmpty()){Ui.toast(this,"No hidden apps");return;}
        Map<String,String> labels=new HashMap<>();for(var app:catalog.scanAll())labels.put(app.pkg(),app.name());
        List<String> packages=new ArrayList<>(hidden);packages.sort(Comparator.comparing(pkg->labels.getOrDefault(pkg,pkg),String.CASE_INSENSITIVE_ORDER));
        String[] names=new String[packages.size()];for(int i=0;i<names.length;i++){String pkg=packages.get(i);names[i]=labels.containsKey(pkg)?labels.get(pkg)+"\n"+pkg:pkg+" (not installed)";}
        Set<String> restore=new HashSet<>();
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Hidden apps — select to restore")
            .setMultiChoiceItems(names,new boolean[names.length],(d,n,checked)->{if(checked)restore.add(packages.get(n));else restore.remove(packages.get(n));})
            .setNegativeButton("Cancel",null).setNeutralButton("Show all",(d,n)->{for(String pkg:packages)catalog.setHidden(pkg,false);render();})
            .setPositiveButton("Restore",(d,n)->{for(String pkg:restore)catalog.setHidden(pkg,false);render();}).show();
    }
    private void assignDialog(Catalog.App app){
        var cats=catalog.categories();String[] names=new String[cats.size()+2];names[0]="Uncategorised";for(int i=0;i<cats.size();i++)names[i+1]=cats.get(i).name();names[names.length-1]="+ New category";
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle(app.name()).setItems(names,(d,n)->{
            if(n==names.length-1){categoryDialog(null,app);return;}
            catalog.assign(app.pkg(),n==0?"":cats.get(n-1).id(),"Manual");render();
        }).show();
    }
    private void categoryDialog(String id,Catalog.App app){
        EditText name=Ui.input(this,"Category name",id==null?"":catalog.categoryName(id));
        androidx.appcompat.app.AlertDialog dialog=new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle(id==null?"New category":"Rename category").setView(name).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{try{String saved=catalog.saveCategory(id,name.getText().toString());if(app!=null)catalog.assign(app.pkg(),saved,"Manual");Shortcuts.refresh(this);dialog.dismiss();render();}catch(Exception e){name.setError(e.getMessage());}}));dialog.show();
    }
    private void settings(){
        var p=getSharedPreferences("settings",0);LinearLayout form=Ui.column(this);ScrollView scroll=new ScrollView(this);scroll.addView(form);
        form.addView(Ui.text(this,"System One receives app names, package IDs and available Android/Google Play descriptions. Requests use your account credit. Other is excluded from classifier choices.",14));
        form.addView(Ui.button(this,"Hidden apps ("+catalog.hiddenPackages().size()+")",()->{dialogDismissSettings();hiddenApps();}));
        form.addView(Ui.text(this,"Hide apps from the Apps tab’s action menu. Hidden apps keep their category but are excluded from lists, category grids/counts and AI categorisation.",14));
        form.addView(Ui.text(this,"API key",14));EditText key=Ui.input(this,"API key","");key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);try{key.setText(KeyStore.read(this));}catch(Exception e){Ui.toast(this,"Stored key unavailable. Enter it again.");}form.addView(key);
        form.addView(Ui.text(this,"Full System One endpoint (HTTPS)",14));EditText url=Ui.input(this,"Endpoint",p.getString("url",SystemOne.DEFAULT_URL));url.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);form.addView(url);
        form.addView(Ui.text(this,"System One model",14));EditText model=Ui.input(this,"Model",p.getString("model",SystemOne.DEFAULT_MODEL));form.addView(model);
        com.google.android.material.materialswitch.MaterialSwitch lookup=new com.google.android.material.materialswitch.MaterialSwitch(this);lookup.setText("Look up descriptions on Google Play");lookup.setChecked(p.getBoolean("playLookup",true));form.addView(lookup);
        form.addView(Ui.text(this,"Uses each app’s package ID to fetch its public listing. Failed lookups fall back to available local information. Descriptions are cached for 7 days.",14));
        form.addView(Ui.text(this,"JSON backup includes settings, category names/icons, app mappings and your API key in plaintext. Keep exported files private. Save settings before exporting; unsaved edits are not included.",14));
        form.addView(Ui.button(this,"Export JSON backup",()->{dialogDismissSettings();chooseBackup(false);}));
        form.addView(Ui.button(this,"Import JSON backup",()->{dialogDismissSettings();chooseBackup(true);}));
        Button reset=Ui.button(this,"Reset all app assignments",this::resetApps);reset.setEnabled(!Classifier.INSTANCE.running);form.addView(reset);
        com.google.android.material.materialswitch.MaterialSwitch homeOnly=new com.google.android.material.materialswitch.MaterialSwitch(this);homeOnly.setText("Show only on the home screen");homeOnly.setChecked(p.getBoolean("homeOnly",false));form.addView(homeOnly);
        form.addView(Ui.text(this,"Requires Usage Access to identify your default launcher. Foreground app events stay on this device. Visibility can take about a second to update.",14));
        if(!HomeScreenMonitor.hasAccess(this))form.addView(Ui.button(this,"Grant Usage Access",()->startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))));
        form.addView(Ui.text(this,"Popup theme",14));Spinner popupTheme=new Spinner(this);popupTheme.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Follow system","Light","Dark"}));popupTheme.setSelection(PopupTheme.index(p.getString("popupTheme","system")));form.addView(popupTheme);
        com.google.android.material.materialswitch.MaterialSwitch bubble=new com.google.android.material.materialswitch.MaterialSwitch(this);bubble.setText("Floating category button");bubble.setChecked(p.getBoolean("bubble",false));form.addView(bubble);
        form.addView(Ui.text(this,"Drag the circle to position it. Tap or long press to open categories. Enabling it requests Android’s “display over other apps” permission.",14));
        androidx.appcompat.app.AlertDialog dialog=new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Settings").setView(scroll).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        settingsDialog=dialog;
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{
            try{
                SystemOne.validateUrl(url.getText().toString().trim());if(model.getText().toString().trim().isEmpty())throw new IllegalArgumentException("Enter a System One model");
                KeyStore.save(this,key.getText().toString().trim());p.edit().putString("url",url.getText().toString().trim()).putString("model",model.getText().toString().trim()).putBoolean("bubble",bubble.isChecked()).putBoolean("playLookup",lookup.isChecked()).putBoolean("homeOnly",homeOnly.isChecked()).putString("popupTheme",PopupTheme.VALUES[popupTheme.getSelectedItemPosition()]).apply();
                dialog.dismiss();
                if(bubble.isChecked())enableBubble();else stopService(new Intent(this,BubbleService.class));
            }catch(Exception e){Ui.toast(this,e.getMessage()==null?"Could not save settings":e.getMessage());}
        }));dialog.show();
    }
    private void dialogDismissSettings(){if(settingsDialog!=null)settingsDialog.dismiss();}
    private void chooseBackup(boolean importing){
        if(Classifier.INSTANCE.running){Ui.toast(this,"Stop categorisation before importing or exporting");return;}
        Intent intent=new Intent(importing?Intent.ACTION_OPEN_DOCUMENT:Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(importing?"*/*":"application/json");
        if(!importing)intent.putExtra(Intent.EXTRA_TITLE,"appcat-backup.json");
        try{startActivityForResult(intent,importing?5:4);}catch(ActivityNotFoundException e){Ui.toast(this,"No document picker available");}
    }
    private void backupResult(boolean importing,Uri uri){
        if(Classifier.INSTANCE.running){Ui.toast(this,"Stop categorisation before importing or exporting");return;}
        new Thread(()->{
            try{
                if(importing){
                    String json;
                    try(java.io.InputStream stream=getContentResolver().openInputStream(uri)){
                        if(stream==null)throw new java.io.IOException("Could not open backup");
                        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;
                        while((count=stream.read(buffer))!=-1){if(bytes.size()+count>Backup.MAX_BYTES)throw new java.io.IOException("Backup exceeds 5 MB");bytes.write(buffer,0,count);}
                        json=bytes.toString(java.nio.charset.StandardCharsets.UTF_8.name());
                    }
                    Backup.Data backup=Backup.parse(json);
                    runOnUiThread(()->{if(isFinishing()||isDestroyed())return;new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Import backup?")
                        .setMessage("Replace all saved settings, categories, icons and app assignments with "+backup.categories().length()+" categories and "+backup.mappings().length()+" mappings? The API key will also be replaced. Android permissions and home-screen placement are not restored.")
                        .setNegativeButton("Cancel",null).setPositiveButton("Import",(d,n)->{try{Backup.apply(this,backup);stopService(new Intent(this,BubbleService.class));Shortcuts.refresh(this);Classifier.INSTANCE.status="Backup imported";render();if(getSharedPreferences("settings",0).getBoolean("bubble",false))enableBubble();}catch(Exception e){Ui.toast(this,"Import failed: "+e.getMessage());}}).show();});
                }else{
                    String json=Backup.export(this);
                    try(java.io.OutputStream stream=getContentResolver().openOutputStream(uri,"wt")){if(stream==null)throw new java.io.IOException("Could not open export file");stream.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
                    runOnUiThread(()->Ui.toast(this,"Backup exported; includes your API key"));
                }
            }catch(Exception e){runOnUiThread(()->Ui.toast(this,(importing?"Import failed. Check the backup format and file size.":"Export failed. Check file access and the stored API key.")));}
        },"appcat-backup").start();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if((request==4||request==5)&&result==RESULT_OK&&data!=null&&data.getData()!=null){backupResult(request==5,data.getData());return;}if((request==2&&Settings.canDrawOverlays(this))||(request==3&&HomeScreenMonitor.hasAccess(this)))enableBubble();}
    private void enableBubble(){
        if(!Settings.canDrawOverlays(this)){startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())),2);return;}
        if(getSharedPreferences("settings",0).getBoolean("homeOnly",false)&&!HomeScreenMonitor.hasAccess(this)){startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),3);return;}
        startBubble();
        var p=getSharedPreferences("settings",0);if(Build.VERSION.SDK_INT>=33&&!p.getBoolean("notificationAsked",false)){p.edit().putBoolean("notificationAsked",true).apply();requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},1);}
    }
    private void startBubble(){try{startForegroundService(new Intent(this,BubbleService.class));}catch(Exception e){Ui.toast(this,"Could not start floating button. Try switching it off and on.");}}
}
