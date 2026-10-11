package pw.rkd.appcat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Public package-specific listing; never a name search that can accidentally match another app. */
final class PlayMetadata {
    record Info(String title,String description,String category,String url) {
        boolean useful(){return !description.isEmpty() || !category.isEmpty();}
    }
    static String url(String pkg) throws UnsupportedEncodingException {
        return "https://play.google.com/store/apps/details?id="+URLEncoder.encode(pkg,"UTF-8")+"&hl=en";
    }
    static Info parse(String html,String pkg) throws Exception {
        Document doc=Jsoup.parse(html);String title="",description="",category="";
        for(Element script:doc.select("script[type=application/ld+json]")) {
            try {
                JSONObject j=new JSONObject(script.data());
                if(!"SoftwareApplication".equals(j.optString("@type")))continue;
                // Reject a response listing a different package (including store error/redirect pages).
                URI identity=new URI(j.getString("url"));boolean matches=false;
                String query=identity.getRawQuery();
                if(query!=null)for(String part:query.split("&")){String[] kv=part.split("=",2);if(kv.length==2&&kv[0].equals("id")&&URLDecoder.decode(kv[1],"UTF-8").equals(pkg))matches=true;}
                if(!matches)continue;
                title=j.optString("name");description=j.optString("description");category=j.optString("applicationCategory");
                Element full=doc.selectFirst("[data-g-id=description]");if(full!=null&&!full.text().isEmpty())description=full.text();
                return new Info(limit(title,200),limit(description,4000),limit(category,120),url(pkg));
            }catch(JSONException|URISyntaxException ignored){}
        }
        return new Info("","","","");
    }
    static String limit(String text,int max){return text.length()>max?text.substring(0,max):text;}
    static Info fetch(String pkg) throws Exception {
        HttpURLConnection conn=(HttpURLConnection)new URL(url(pkg)).openConnection();
        conn.setInstanceFollowRedirects(false);conn.setConnectTimeout(6000);conn.setReadTimeout(6000);
        conn.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android) AppCat/0.2");
        try {
            if(conn.getResponseCode()!=200)return new Info("","","","");
            try(InputStream in=conn.getInputStream()) {
                ByteArrayOutputStream body=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
                while((n=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedException();if(body.size()+n>3_000_000)throw new IOException("Store page too large");body.write(buffer,0,n);}
                return parse(new String(body.toByteArray(),StandardCharsets.UTF_8),pkg);
            }
        }finally{conn.disconnect();}
    }
}
