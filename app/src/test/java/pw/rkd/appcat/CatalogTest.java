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
    @Test public void backupRoundTripRestoresCustomIconsMappingsAndSettings() throws Exception {
        context.getSharedPreferences("secrets",0).edit().clear().commit();context.getSharedPreferences("settings",0).edit().clear().putString("model","custom/model").putBoolean("homeOnly",true).commit();
        String custom=catalog.saveCategory(null,"Personal");catalog.saveIcon(custom,"star");catalog.assign("test.finance",custom,"Manual");catalog.assign("com.uninstalled.app","","Reset");
        Backup.Data backup=Backup.parse(Backup.export(context));catalog.assign("test.finance","c7","AI");catalog.saveCategory(null,"Remove on import");context.getSharedPreferences("settings",0).edit().putString("model","changed").commit();
        context.getSharedPreferences("secrets",0).edit().putString("key","unreadable-old-key").putString("iv","invalid").commit();
        Backup.apply(context,backup);assertEquals("",KeyStore.read(context));Catalog restored=new Catalog(context);assertEquals(custom,restored.scan().get(0).category());assertEquals("Manual",restored.scan().get(0).source());assertEquals("star",restored.categories().stream().filter(c->c.id().equals(custom)).findFirst().get().icon());assertEquals(18,restored.categories().size());
        assertEquals("custom/model",context.getSharedPreferences("settings",0).getString("model",""));assertTrue(context.getSharedPreferences("settings",0).getBoolean("homeOnly",false));assertEquals("Reset",Backup.parse(Backup.export(context)).mappings().getJSONObject("com.uninstalled.app").getString("source"));
    }
    @Test public void missingDefaultCategoryAfterImportDoesNotBreakAndroidScan() {
        context.getSharedPreferences("catalog",0).edit().clear().putString("categories","[{\"id\":\"custom\",\"name\":\"Custom\",\"icon\":\"folder\"}]").commit();assertEquals("",new Catalog(context).scan().get(0).category());
    }
    @Test public void hiddenAppsStayHiddenAcrossScanRenameAndRestartAndKeepAssignment(){
        catalog.assign("test.finance","c12","Manual");catalog.setHidden("test.finance",true);catalog.saveCategory("c12","Money");
        Catalog reopened=new Catalog(context);assertTrue(reopened.scan().isEmpty());assertEquals("c12",reopened.scanAll().get(0).category());assertEquals("Manual",reopened.scanAll().get(0).source());
        reopened.setHidden("test.finance",false);assertEquals("c12",reopened.scan().get(0).category());assertFalse(reopened.isHidden("test.finance"));
    }
    @Test public void hiddenAppsDoNotAcceptAnInflightAiResult(){var snapshot=catalog.scan().get(0);catalog.setHidden(snapshot.pkg(),true);assertFalse(catalog.assignAiIfUnchanged(snapshot,"c12"));assertEquals("c7",catalog.scanAll().get(0).category());}
    @Test public void resetAssignmentsDoesNotUnhideApps(){catalog.setHidden("test.finance",true);catalog.resetApps();assertTrue(catalog.scan().isEmpty());assertEquals("",catalog.scanAll().get(0).category());assertTrue(catalog.isHidden("test.finance"));}
    @Test public void backupRestoresHiddenInstalledAndUninstalledPackages() throws Exception {
        context.getSharedPreferences("secrets",0).edit().clear().commit();context.getSharedPreferences("settings",0).edit().clear().commit();
        catalog.assign("test.finance","c12","Manual");catalog.setHidden("test.finance",true);catalog.setHidden("com.uninstalled.app",true);
        var backup=Backup.parse(Backup.export(context));assertEquals(2,backup.hidden().size());assertEquals("c12",backup.mappings().getJSONObject("test.finance").getString("category"));catalog.setHidden("test.finance",false);
        Backup.apply(context,backup);Catalog reopened=new Catalog(context);assertTrue(reopened.scan().isEmpty());assertTrue(reopened.isHidden("com.uninstalled.app"));reopened.setHidden("test.finance",false);assertEquals("c12",reopened.scan().get(0).category());
    }
}
