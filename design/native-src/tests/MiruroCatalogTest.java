package app.lovelycrxshx.kuro;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

public final class MiruroCatalogTest {
 private static byte[] encoded(byte[] value)throws Exception{
  ByteArrayOutputStream out=new ByteArrayOutputStream();try(GZIPOutputStream z=new GZIPOutputStream(out)){z.write(value);}byte[] bytes=out.toByteArray(),key="miruro/catalog".getBytes(StandardCharsets.UTF_8);for(int i=0;i<bytes.length;i++)bytes[i]^=key[i%key.length];return bytes;
 }
 private static void fail(RunnableIO operation)throws Exception{try{operation.run();throw new AssertionError("Expected failure");}catch(IOException expected){}}
 interface RunnableIO{void run()throws IOException;}
 public static void main(String[] args)throws Exception{
  byte[] original="{\"title\":\"日本語 — hello\",\"episodes\":[1,2]}".getBytes(StandardCharsets.UTF_8),source=encoded(original),copy=source.clone();
  if(!Arrays.equals(original,MiruroCatalog.decode(source,5000))||!Arrays.equals(source,copy))throw new AssertionError("round trip or mutation");
  fail(()->MiruroCatalog.decode(new byte[0],5000));fail(()->MiruroCatalog.decode(new byte[]{1,2,3},5000));fail(()->MiruroCatalog.decode(source,source.length-1));
  byte[] bomb=new byte[10000];Arrays.fill(bomb,(byte)'a');byte[] encodedBomb=encoded(bomb);fail(()->MiruroCatalog.decode(encodedBomb,1000));
  System.out.println("PASS Library 11 JSON XOR/gzip decoding, UTF-8, bounded output and malformed input");
 }
}
