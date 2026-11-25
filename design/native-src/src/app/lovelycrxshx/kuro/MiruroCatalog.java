package app.lovelycrxshx.kuro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

/** Decode the source's public XOR/gzip JSON response format, with a hard output limit. */
final class MiruroCatalog {
 private static final byte[] KEY="miruro/catalog".getBytes(StandardCharsets.UTF_8);
 static byte[] decode(byte[] input,int limit)throws IOException {
  if(input==null||input.length==0||limit<1||input.length>limit)throw new IOException("Invalid Library 11 response size");
  byte[] compressed=input.clone();for(int i=0;i<compressed.length;i++)compressed[i]^=KEY[i%KEY.length];
  try(GZIPInputStream in=new GZIPInputStream(new ByteArrayInputStream(compressed));ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(n>limit-out.size())throw new IOException("Library 11 response too large");out.write(buffer,0,n);}return out.toByteArray();
  }catch(IOException e){throw new IOException("Library 11 catalogue response could not be decoded",e);}
 }
 private MiruroCatalog(){}
}
