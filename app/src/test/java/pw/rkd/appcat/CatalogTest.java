package pw.rkd.appcat;

import android.content.*;
import android.content.pm.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.robolectric.Shadows.shadowOf;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest=Config.NONE)
public class CatalogTest {
    private Context context;
    private Catalog catalog;
    @Before public void setUp(){
        context=RuntimeEnvironment.getApplication();context.getSharedPreferences("catalog",0).edit().clear().commit();
        ApplicationInfo ai=new ApplicationInfo();ai.packageName="test.finance";ai.category=ApplicationInfo.CATEGORY_PRODUCTIVITY;ai.nonLocalizedLabel="Banking";
        ActivityInfo activity=new ActivityInfo();activity.applicationInfo=ai;activity.packageName=ai.packageName;activity.name="Main";
        ResolveInfo resolve=new ResolveInfo();resolve.activityInfo=activity;
        shadowOf(context.getPackageManager()).addResolveInfoForIntent(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),resolve);
        catalog=new Catalog(context);
    }
    @Test public void resetStaysUnassignedAcrossRescanAndRestart(){
        assertEquals("c7",catalog.scan().get(0).category());
        catalog.assign("test.finance","c12","Manual");String custom=catalog.saveCategory(null,"Custom");
        catalog.resetApps();var reopened=new Catalog(context);assertEquals("",reopened.scan().get(0).category());assertEquals("Reset",reopened.scan().get(0).source());assertEquals("Custom",reopened.categoryName(custom));
    }
    @Test public void manualEditWinsEvenWhenItReturnsToTheSameCategory(){
        var snapshot=catalog.scan().get(0);catalog.assign(snapshot.pkg(),"c12","Manual");catalog.assign(snapshot.pkg(),"c7","Manual");
        assertFalse(catalog.assignAiIfUnchanged(snapshot,"c10"));assertEquals("c7",catalog.scan().get(0).category());assertEquals("Manual",catalog.scan().get(0).source());
    }
    @Test public void forcedAiCanKeepTheOriginalCategory(){var snapshot=catalog.scan().get(0);assertTrue(catalog.assignAiIfUnchanged(snapshot,snapshot.category()));assertEquals("c7",catalog.scan().get(0).category());assertEquals("AI",catalog.scan().get(0).source());}
    @Test public void iconSelectionSurvivesRename(){catalog.saveIcon("c7","work");catalog.saveCategory("c7","Work stuff");assertEquals("work",new Catalog(context).categories().stream().filter(c->c.id().equals("c7")).findFirst().get().icon());}
}
