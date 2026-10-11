package pw.rkd.appcat;

import org.junit.Test;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;

public class SystemOneTest {
    @Test public void requestUsesSystemOneTypedChoice() throws Exception {
        var request=SystemOne.request("typesafe/jev-1.13","org.example","Example",Map.of("social","Social","other","Other"));
        assertEquals("org.example",new JSONObject(request.getString("state")).getString("package_name"));
        assertFalse(request.has("messages"));
        assertEquals("choice",request.getJSONObject("questions").getJSONObject("category").getString("type"));
        assertEquals("Social",request.getJSONObject("questions").getJSONObject("category").getJSONObject("criteria").getString("social"));
    }
    @Test public void readsOnlyValidChoice() throws Exception {
        assertEquals("social",SystemOne.parse("{\"answers\":{\"category\":{\"type\":\"choice\",\"choice\":\"social\"}}}",Set.of("social")));
    }
    @Test(expected=JSONException.class) public void rejectsUnknownChoice() throws Exception {SystemOne.parse("{\"answers\":{\"category\":{\"type\":\"choice\",\"choice\":\"invented\"}}}",Set.of("social"));}
    @Test(expected=JSONException.class) public void rejectsWrongResponseType() throws Exception {SystemOne.parse("{\"answers\":{\"category\":{\"type\":\"noul\",\"choice\":\"social\"}}}",Set.of("social"));}
    @Test(expected=IllegalArgumentException.class) public void rejectsCleartextKeyTransport(){SystemOne.validateUrl("http://example.com/systemone");}
    @Test(expected=IllegalArgumentException.class) public void rejectsEmbeddedCredentials(){SystemOne.validateUrl("https://key@example.com/systemone");}
    @Test public void acceptsCustomHttpsEndpoint(){SystemOne.validateUrl("https://example.com/custom/systemone");}
}
