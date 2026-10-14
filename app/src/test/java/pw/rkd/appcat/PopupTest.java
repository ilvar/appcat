package pw.rkd.appcat;

import android.content.*;
import android.content.pm.*;
import android.content.res.Configuration;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class PopupTest {
    private Context context;
    @Before public void setUp(){context=RuntimeEnvironment.getApplication();context.getSharedPreferences("settings",0).edit().clear().commit();context.getSharedPreferences("catalog",0).edit().clear().commit();}
    @Test public void floatingCategoryNavigationBuildsAppPageAndReturnsWithoutCrashing() throws Exception {
        ApplicationInfo ai=new ApplicationInfo();ai.packageName="test.finance";ai.nonLocalizedLabel="Finance";
        ActivityInfo activity=new ActivityInfo();activity.applicationInfo=ai;activity.packageName=ai.packageName;activity.name="Main";ResolveInfo resolve=new ResolveInfo();resolve.activityInfo=activity;
        shadowOf(context.getPackageManager()).addResolveInfoForIntent(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),resolve);
        new Catalog(context).assign(ai.packageName,"c0","Manual");
        BubbleService service=Robolectric.buildService(BubbleService.class).create().get();
        var wm=BubbleService.class.getDeclaredField("wm");wm.setAccessible(true);wm.set(service,service.getSystemService(WindowManager.class));
        var open=BubbleService.class.getDeclaredMethod("openPicker",String.class);open.setAccessible(true);open.invoke(service,new Object[]{null});
        var field=BubbleService.class.getDeclaredField("panel");field.setAccessible(true);ScrollView panel=(ScrollView)field.get(service);LinearLayout card=(LinearLayout)panel.getChildAt(0);
        GridLayout categories=(GridLayout)card.getChildAt(1);measure(categories,Ui.dp(service,288));assertEquals(4,categories.getColumnCount());categories.getChildAt(0).performClick();
        panel=(ScrollView)field.get(service);card=(LinearLayout)panel.getChildAt(0);assertTrue(card.getChildAt(1) instanceof com.google.android.material.button.MaterialButton);
        GridLayout apps=(GridLayout)card.getChildAt(2);measure(apps,Ui.dp(service,288));assertEquals(1,apps.getChildCount());assertEquals(4,apps.getColumnCount());
        card.getChildAt(1).performClick();assertNotNull(field.get(service));service.onDestroy();
    }
    @Test public void popupThemesOverrideSystemAndCreateMaterialControls(){
        for(String value:new String[]{"light","dark"}){
            context.getSharedPreferences("settings",0).edit().putString("popupTheme",value).commit();Context popup=PopupTheme.context(context);
            assertEquals(value.equals("dark")?Configuration.UI_MODE_NIGHT_YES:Configuration.UI_MODE_NIGHT_NO,popup.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK);
            assertNotNull(Ui.button(popup,"Back",()->{}));
        }
    }
    @Test public void compactGridHasFourColumnsAndSmallLabelsAtDifferentWidths(){
        Context popup=PopupTheme.context(context);LinearLayout parent=Ui.column(popup);java.util.List<Ui.Tile> tiles=new java.util.ArrayList<>();for(int i=0;i<100;i++)tiles.add(new Ui.Tile("App "+i,null,()->{}));Ui.tileGrid(popup,parent,tiles,true);
        GridLayout grid=(GridLayout)parent.getChildAt(0);for(int width:new int[]{320,240,500}){measure(grid,Ui.dp(popup,width));assertEquals(4,grid.getColumnCount());assertEquals(Ui.dp(popup,width)/4,grid.getChildAt(0).getLayoutParams().width);}
        TextView label=(TextView)((LinearLayout)grid.getChildAt(0)).getChildAt(1);assertEquals(10*popup.getResources().getDisplayMetrics().scaledDensity,label.getTextSize(),.01f);
    }
    @Test public void responsiveGridCanShrinkWithoutInvalidColumnIndices(){LinearLayout parent=Ui.column(context);java.util.List<Ui.Tile> tiles=new java.util.ArrayList<>();for(int i=0;i<30;i++)tiles.add(new Ui.Tile("App",null,()->{}));Ui.tileGrid(context,parent,tiles);GridLayout grid=(GridLayout)parent.getChildAt(0);measure(grid,Ui.dp(context,400));measure(grid,Ui.dp(context,160));assertEquals(2,grid.getColumnCount());}
    @Test public void insetsRemainCorrectForReplacementRoots(){
        var activity=Robolectric.buildActivity(android.app.Activity.class).setup().get();
        for(int i=0;i<2;i++){LinearLayout root=new LinearLayout(activity);Ui.insets(root);activity.setContentView(root);
            var insets=new androidx.core.view.WindowInsetsCompat.Builder().setInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars(),androidx.core.graphics.Insets.of(0,24,0,48)).build();
            androidx.core.view.ViewCompat.dispatchApplyWindowInsets(root,insets);assertEquals(24,root.getPaddingTop());assertEquals(48,root.getPaddingBottom());}
    }
    @Test public void hiddenAppsAreAbsentFromCategoryAndAppPopupGrids(){
        ApplicationInfo ai=new ApplicationInfo();ai.packageName="test.hidden";ai.nonLocalizedLabel="Hidden";ActivityInfo activity=new ActivityInfo();activity.applicationInfo=ai;activity.packageName=ai.packageName;activity.name="Main";ResolveInfo resolve=new ResolveInfo();resolve.activityInfo=activity;
        shadowOf(context.getPackageManager()).addResolveInfoForIntent(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),resolve);
        Catalog catalog=new Catalog(context);catalog.assign(ai.packageName,"c0","Manual");catalog.setHidden(ai.packageName,true);Context popup=PopupTheme.context(context);LinearLayout parent=Ui.column(popup);
        Ui.categoryGrid(popup,parent,catalog,catalog.scan(),id->{});GridLayout categories=(GridLayout)parent.getChildAt(0);TextView games=(TextView)((LinearLayout)categories.getChildAt(0)).getChildAt(1);assertEquals("Games · 0",games.getText().toString());
        LinearLayout apps=Ui.column(popup);Ui.appGrid(popup,apps,catalog.scan(),app->{});assertEquals(0,((GridLayout)apps.getChildAt(0)).getChildCount());
    }
    private void measure(View view,int width){view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));}
}
