package app.lovelycrxshx.kuro;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Small, private Android-Keystore vault; neither plaintext nor ciphertext is included in backups. */
final class KuroDiscordVault564 {
 private static final String ALIAS="kuro-discord-presence-key-v1",FILE="kuro-discord-presence-vault-v1";
 private KuroDiscordVault564(){}
 private static SecretKey key(boolean create)throws Exception{
  KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
  if(ks.containsAlias(ALIAS))return (SecretKey)ks.getKey(ALIAS,null);
  if(!create)return null;
  KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
  g.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
   .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
   .setRandomizedEncryptionRequired(true).build());return g.generateKey();
 }
 private static boolean field(String f){return "refresh".equals(f)||"pending".equals(f);}
 static synchronized boolean store(Context c,String f,byte[] value){
  if(!field(f)||value==null||value.length==0||value.length>8192)return false;
  try{
   Cipher enc=Cipher.getInstance("AES/GCM/NoPadding");enc.init(Cipher.ENCRYPT_MODE,key(true));enc.updateAAD(f.getBytes(StandardCharsets.US_ASCII));
   byte[] iv=enc.getIV(),data=enc.doFinal(value),all=new byte[iv.length+data.length];
   if(iv.length!=12)return false;System.arraycopy(iv,0,all,0,iv.length);System.arraycopy(data,0,all,iv.length,data.length);
   return c.getSharedPreferences(FILE,Context.MODE_PRIVATE).edit().putString(f,Base64.encodeToString(all,Base64.NO_WRAP)).commit();
  }catch(Exception ignored){return false;}
 }
 /** Returns null if absent or inaccessible; no credential or exception is logged. */
 static synchronized byte[] load(Context c,String f){
  if(!field(f))return null;
  try{
   SharedPreferences p=c.getSharedPreferences(FILE,Context.MODE_PRIVATE);String stored=p.getString(f,"");if(stored.isEmpty()||stored.length()>16000)return null;
   SecretKey k=key(false);if(k==null)return null;byte[] all=Base64.decode(stored,Base64.NO_WRAP);if(all.length<29||all.length>8250)return null;
   byte[] iv=Arrays.copyOfRange(all,0,12);Cipher dec=Cipher.getInstance("AES/GCM/NoPadding");dec.init(Cipher.DECRYPT_MODE,k,new GCMParameterSpec(128,iv));dec.updateAAD(f.getBytes(StandardCharsets.US_ASCII));return dec.doFinal(all,12,all.length-12);
  }catch(Exception ignored){return null;}
 }
 static synchronized boolean hasStored(Context c,String f){return field(f)&&c.getSharedPreferences(FILE,Context.MODE_PRIVATE).contains(f);}
 static synchronized void remove(Context c,String f){if(field(f))c.getSharedPreferences(FILE,Context.MODE_PRIVATE).edit().remove(f).commit();}
 static synchronized void unlink(Context c){
  c.getSharedPreferences(FILE,Context.MODE_PRIVATE).edit().clear().commit();
  try{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);if(ks.containsAlias(ALIAS))ks.deleteEntry(ALIAS);}catch(Exception ignored){}
 }
}
