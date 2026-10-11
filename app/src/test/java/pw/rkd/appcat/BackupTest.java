package pw.rkd.appcat;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class BackupTest {
    private JSONObject fixture() throws Exception {
        return new JSONObject().put("format","appcat").put("version",1)
            .put("settings",new JSONObject().put("apiKey","sk-private-test").put("url",SystemOne.DEFAULT_URL).put("model","custom/model").put("playLookup",false).put("bubble",true).put("homeOnly",true).put("bubbleX",42))
            .put("categories",new JSONArray().put(Catalog.json("id","custom","name","My tools","icon","build")))
            .put("mappings",new JSONObject().put("com.example.app",Catalog.json("category","custom","source","Manual")).put("com.example.reset",Catalog.json("category","","source","Reset")));
    }
    @Test public void preservesKeySettingsIconsAndExplicitUnassignedMappings() throws Exception {
        var data=Backup.parse(fixture().toString());assertEquals("sk-private-test",data.settings().getString("apiKey"));assertTrue(data.settings().getBoolean("homeOnly"));assertFalse(data.settings().getBoolean("playLookup"));assertEquals(42,data.settings().getInt("bubbleX"));
        assertEquals("build",data.categories().getJSONObject(0).getString("icon"));assertEquals("custom",data.mappings().getJSONObject("com.example.app").getString("category"));assertEquals("",data.mappings().getJSONObject("com.example.reset").getString("category"));
    }
    @Test public void rejectsUnknownCategory() throws Exception {JSONObject f=fixture();f.getJSONObject("mappings").getJSONObject("com.example.app").put("category","missing");reject(f);}
    @Test public void rejectsDuplicateCategory() throws Exception {JSONObject f=fixture();f.getJSONArray("categories").put(Catalog.json("id","custom","name","Another","icon","folder"));reject(f);}
    @Test public void rejectsUnknownIcon() throws Exception {JSONObject f=fixture();f.getJSONArray("categories").getJSONObject(0).put("icon","missing");reject(f);}
    @Test public void rejectsInsecureEndpoint() throws Exception {JSONObject f=fixture();f.getJSONObject("settings").put("url","http://example.com");reject(f);}
    @Test public void rejectsWrongTypes() throws Exception {JSONObject f=fixture();f.getJSONObject("settings").put("homeOnly","true");reject(f);}
    @Test public void rejectsUnsupportedVersion() throws Exception {reject(fixture().put("version",2));}
    @Test public void rejectsMissingKeyRatherThanSilentlyErasingIt() throws Exception {JSONObject f=fixture();f.getJSONObject("settings").remove("apiKey");reject(f);}
    @Test public void popupThemeIsPortableAndOlderBackupsDefaultToSystem() throws Exception {assertEquals("system",Backup.parse(fixture().toString()).settings().getString("popupTheme"));JSONObject f=fixture();f.getJSONObject("settings").put("popupTheme","dark");assertEquals("dark",Backup.parse(f.toString()).settings().getString("popupTheme"));f.getJSONObject("settings").put("popupTheme","invalid");reject(f);}
    @Test public void olderBackupsRestoreWithoutHiddenApps() throws Exception {assertTrue(Backup.parse(fixture().toString()).hidden().isEmpty());}
    @Test public void rejectsMalformedHiddenPackages() throws Exception {reject(fixture().put("hiddenApps",new JSONArray().put("invalid package")));reject(fixture().put("hiddenApps",new JSONArray().put(42)));reject(fixture().put("hiddenApps",new JSONArray().put("com.app").put("com.app")));}
    private void reject(JSONObject fixture){try{Backup.parse(fixture.toString());fail("Accepted invalid backup");}catch(Exception expected){}}
}
