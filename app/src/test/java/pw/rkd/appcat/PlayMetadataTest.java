package pw.rkd.appcat;

import org.junit.Test;
import org.json.JSONObject;
import static org.junit.Assert.*;

public class PlayMetadataTest {
    private String page(String pkg,String description,String body) throws Exception {
        JSONObject data=new JSONObject().put("@type","SoftwareApplication").put("url","https://play.google.com/store/apps/details?id="+pkg+"&hl=en")
                .put("name","Example").put("description",description).put("applicationCategory","FINANCE");
        return "<script type='application/ld+json'>"+data+"</script>"+body;
    }
    @Test public void prefersFullDescriptionAndDecodesHtml() throws Exception {
        var info=PlayMetadata.parse(page("test.app","Short tagline","<div data-g-id='description'>Manage bank &amp; card accounts<br>Pay bills</div>"),"test.app");
        assertEquals("Manage bank & card accounts Pay bills",info.description());assertEquals("FINANCE",info.category());assertTrue(info.useful());
    }
    @Test public void rejectsAnotherPackagesListing() throws Exception {assertFalse(PlayMetadata.parse(page("wrong.app","Useful description",""),"test.app").useful());}
    @Test public void fallsBackToStructuredDescription() throws Exception {assertEquals("Calendar planning",PlayMetadata.parse(page("test.app","Calendar planning",""),"test.app").description());}
    @Test public void consentAndErrorPagesAreNotContext() throws Exception {assertFalse(PlayMetadata.parse("<html><title>Consent</title><p>Sign in</p></html>","test.app").useful());}
    @Test public void boundsDescriptionsSentToClassifier() throws Exception {assertEquals(4000,PlayMetadata.parse(page("test.app","x".repeat(5000),""),"test.app").description().length());}
}
