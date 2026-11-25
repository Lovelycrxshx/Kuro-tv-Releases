package app.lovelycrxshx.kuro;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.util.Base64;

interface AccountVault {
 String get(String key) throws Exception;
 void put(String key,String value) throws Exception;
 void remove(String key) throws Exception;
}

/** Sessions and PKCE verifiers never enter WebView storage or the APK. */
final class AndroidAccountVault implements AccountVault {
 private static final String ALIAS="kuro.account.v1";
 private static final Object KEY_LOCK=new Object();
 private final SharedPreferences prefs;
 AndroidAccountVault(Context context){prefs=context.getApplicationContext().getSharedPreferences("kuro_account_vault_v1",Context.MODE_PRIVATE);}
 private SecretKey key() throws Exception {
  synchronized(KEY_LOCK){KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
  if(!ks.containsAlias(ALIAS)){
   KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
   gen.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build());gen.generateKey();
  }
  return (SecretKey)ks.getKey(ALIAS,null);}
 }
 public synchronized void put(String name,String value) throws Exception {
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());c.updateAAD(name.getBytes(StandardCharsets.UTF_8));
  String encoded=Base64.getEncoder().encodeToString(c.getIV())+"."+Base64.getEncoder().encodeToString(c.doFinal(value.getBytes(StandardCharsets.UTF_8)));
  if(!prefs.edit().putString(name,encoded).commit())throw new AccountError("Could not save secure account data.");
 }
 public synchronized String get(String name) throws Exception {
  String encoded=prefs.getString(name,"");if(encoded.isEmpty())return "";String[] p=encoded.split("\\.",-1);if(p.length!=2)throw new AccountError("Secure account data could not be read.");
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.getDecoder().decode(p[0])));c.updateAAD(name.getBytes(StandardCharsets.UTF_8));
  return new String(c.doFinal(Base64.getDecoder().decode(p[1])),StandardCharsets.UTF_8);
 }
 public synchronized void remove(String name) throws Exception {if(!prefs.edit().remove(name).commit())throw new AccountError("Could not update secure account data.");}
}
