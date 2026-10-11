package pw.rkd.appcat;

import android.content.Context;
import android.security.keystore.*;
import android.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.security.*;

/** The API key is encrypted with a non-exportable Android Keystore key. */
final class KeyStore {
    private static javax.crypto.SecretKey key() throws Exception {
        java.security.KeyStore store=java.security.KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        if(!store.containsAlias("appcat-api")) {
            KeyGenerator gen=KeyGenerator.getInstance("AES","AndroidKeyStore");
            gen.init(new KeyGenParameterSpec.Builder("appcat-api",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build()); gen.generateKey();
        }
        return (javax.crypto.SecretKey)store.getKey("appcat-api",null);
    }
    static void save(Context c,String value) throws Exception {
        if(value.isEmpty()){c.getSharedPreferences("secrets",0).edit().clear().apply();return;}
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        c.getSharedPreferences("secrets",0).edit().putString("key",Base64.encodeToString(cipher.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)),Base64.NO_WRAP)).putString("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)).apply();
    }
    static String read(Context c) throws Exception {
        var p=c.getSharedPreferences("secrets",0); if(!p.contains("key"))return "";
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(p.getString("iv",""),Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(p.getString("key",""),Base64.NO_WRAP)),java.nio.charset.StandardCharsets.UTF_8);
    }
}
