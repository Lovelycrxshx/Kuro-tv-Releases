package app.lovelycrxshx.kuro;
import androidx.webkit.ScriptHandler;import androidx.webkit.WebViewCompat;import androidx.webkit.WebViewFeature;import androidx.webkit.WebMessageCompat;import androidx.webkit.JavaScriptReplyProxy;
import android.app.*;import android.content.*;import android.content.pm.ActivityInfo;import android.graphics.Color;import android.graphics.Bitmap;import android.graphics.BitmapFactory;import android.net.Uri;import android.os.*;import android.view.*;import android.webkit.*;import android.widget.*;import org.json.JSONObject;import org.json.JSONArray;import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.concurrent.*;

public class MainActivity extends Activity {
 private static final String SOURCE_UA="Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36";
 private static final String MIRURO_ORIGIN="https://www.miruro.cx";
 private final ConcurrentHashMap<String,byte[]> responses=new ConcurrentHashMap<>(); private WebView ui,player; private FrameLayout root,full,watchPlayerBox; private LinearLayout playerLayer; private View customView,watchHeader,watchScroll,enhancedToolbar; private boolean enhancedFullscreen=false; private WebChromeClient.CustomViewCallback customCallback; private final ExecutorService pool=Executors.newFixedThreadPool(3);private final Handler main=new Handler(Looper.getMainLooper());
 private interface WatchSink{void accept(String session,JSONObject data);}
 private String watchOverlay="";private int watchOverlayToken=0;private PlayerOptionsDialog playerOptions;private WatchSink watchSink;private String watchSession="";private long playerAttempt=0;private boolean playerHls=false;
 private KuroNativePlayer561 nativePlayer561;private JSONObject watchInfo561,serverOption561;private String resumeKey561="",directAddress561="",directReferer561="",directKind561="";private boolean webPreferred561=false;private boolean rewatch563=false;private String profileArt581="";private static final int SUBTITLE_PICKER561=5611;private LinearLayout directTools561;private TextView mode561,captions561,markers561,subtitleStyle571;private java.util.function.Consumer<Boolean> nativeFailure561;private Runnable nativeEnded561;
 private TextView restart572,fit572;
 private String lastPlayerIssue573="No player error detected in this session.";
 private AlertDialog autoNextDialog573;private Runnable autoNextTick573;private KuroEpisodeDrawer581 episodeDrawer581;
 private KuroDiscordRpc563 discordRpc563;private KuroArtwork567 discordArtwork567;private KuroDiscordPreferences564 discordPrefs564;private KuroPresenceNotification564 discordNotification564;private static final int RPC_NOTIFICATION_PERMISSION564=5640;private final KuroPresence564.PauseClear discordPause564=new KuroPresence564.PauseClear();private boolean discordPauseCleared564;private long discordSessionStart563;
 private ScriptHandler playerErrorScript,fxFrameScript,presenceFrameScript565;private boolean errorFrameMode=false,adFrameMode=false,completionFrameMode=false,completionStartMode=false,presenceFrameMode565=false;
 private static final class PresenceFrame565 {final JavaScriptReplyProxy reply;final long attempt;long lastReceive=-1;KuroProgress565.Sample sample;PresenceFrame565(JavaScriptReplyProxy r,long a){reply=r;attempt=a;}}
 private final ArrayList<PresenceFrame565> presenceFrames565=new ArrayList<>();
 private volatile PlayerAdBlocker playerAdDomains;
 private KuroAccounts accounts;private GitHubAdmin568 githubAdmin568;private String pendingAccountEvent="",pendingImageKind="";private static final int ACCOUNT_IMAGE_PICKER=542;
 private static final int BACKUP_EXPORT560=5600,BACKUP_IMPORT560=5601,ERROR_PERMISSION560=5602;
 private boolean startupPending560=false,profileSignedIn560=false,pipExpanded560=false,pipRequest560=false;private String profileAvatar560="",backupExport560="";private long lastErrorNotification560=0;
 private PlayerAdBlocker adBlocker(){
  PlayerAdBlocker b=playerAdDomains;if(b!=null)return b;
  synchronized(this){b=playerAdDomains;if(b==null){try{b=PlayerAdBlocker.from(getAssets().open("player-ad-hosts541.txt"));}catch(IOException|RuntimeException e){b=PlayerAdBlocker.empty();}playerAdDomains=b;}return b;}
 }
 private final ArrayList<JavaScriptReplyProxy> skipFrames=new ArrayList<>(); private boolean skipFrameMode=false,activityResumed=false,uiReady=false,pendingNews=false,pendingNewsCold=false;
 private static final Set<String> FX_ORIGINS=Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
  "https://gogoanime.com.by","https://hianime.ms","https://hianime.dk","https://hianime.fm","https://hianime.al","https://hianimes.es","https://megaplay.buzz","https://vidtube.site","https://vidnest.fun","https://tryembed.us.cc","https://zokoanime.video","https://player.hianimes.es","https://my.1anime.site","https://zorotv.ba","https://player.abyssplayer.com","https://play.vhscdn.to","https://hianimeq.top","https://www.zenkaitv.com","https://aniwixi.xyz","https://animeav1.uns.bio","https://byselapuix.com","https://www.mp4upload.com","https://1anime.site","https://www.animeparadise.moe","https://kuro.local")));
 private static final class FxFrame{final JavaScriptReplyProxy reply;final long attempt;JSONObject status;FxFrame(JavaScriptReplyProxy reply,long attempt){this.reply=reply;this.attempt=attempt;}}
 private final ArrayList<FxFrame> fxFrames=new ArrayList<>();private boolean fxFrameMode=false,fxComparing=false;private JSONObject fxConfig,fxTopStatus; private static final int NEWS_PERMISSION=526;
 // HiAnime serves the same catalogue from several domains. Keep them in one place
 // so the parser allowlist, the player allowlist and the Referer map cannot drift.
 private static final java.util.List<String> HI_MIRROR_HOSTS=java.util.Arrays.asList(
  "hianime.dk","hianime.fm","hianime.al","hianimes.es");
 private static final java.util.Map<String,String> HI_MIRROR_ORIGINS=new java.util.HashMap<>();
 static{for(String h:HI_MIRROR_HOSTS)HI_MIRROR_ORIGINS.put(h,"https://"+h);}
 private static boolean sourceHost(String h){return "www.animevillage.org".equals(h)||"gogoanimes.cv".equals(h)||"aniwaves.tv".equals(h)||"anikoto.cz".equals(h)||"hianime.ms".equals(h)||HI_MIRROR_HOSTS.contains(h)||"9anime.or.at".equals(h)||"zorotv.ba".equals(h)||"animesalt.cx".equals(h)||"aniwaves.ru".equals(h)||"hianimeq.top".equals(h)||"www.zenkaitv.com".equals(h)||"aniwixi.xyz".equals(h)||"animeav1.com".equals(h)||"aniwatch.co.at".equals(h)||"www.miruro.cx".equals(h)||"kitsunee.moe".equals(h)||"api.animeparadise.moe".equals(h);}
 private static boolean playerHost(String h){if(h==null)return false;String[] hosts={"gogoanime.com.by","hianime.ms","hianime.dk","hianime.fm","hianime.al","hianimes.es","megaplay.buzz","vidtube.site","vidnest.fun","tryembed.us.cc","zokoanime.video","player.hianimes.es","my.1anime.site","zorotv.ba","player.abyssplayer.com","play.vhscdn.to","hianimeq.top","www.zenkaitv.com","aniwixi.xyz","animeav1.uns.bio","byselapuix.com","www.mp4upload.com","1anime.site","www.animeparadise.moe","kuro.local"};for(String x:hosts)if(h.equals(x)||h.endsWith("."+x))return true;return false;}
 @Override public void onCreate(Bundle state){super.onCreate(state);profileArt581=getSharedPreferences("kuro-native-581",MODE_PRIVATE).getString("profile-art","");try{com.discord.socialsdk.DiscordSocialSdkInit.setEngineActivity(this);}catch(Throwable ignored){}KuroPipControls571.attach(this);discordPrefs564=new KuroDiscordPreferences564(this);discordRpc563=new KuroDiscordRpc563(this);discordArtwork567=new KuroArtwork567(task->pool.execute(task));if(state!=null)pendingImageKind=state.getString("kuro.pendingImage","");accounts=new KuroAccounts(getApplicationContext());githubAdmin568=new GitHubAdmin568();boolean openingAccount=isAccountIntent(getIntent())||KuroDiscordOAuth564.isCallback(getIntent());pendingNews=getIntent()!=null&&NewsSync.ACTION_OPEN.equals(getIntent().getAction());pendingNewsCold=pendingNews;startupPending560=!pendingNews&&!openingAccount;NewsSync.ensureChannel(this);KuroUpdater572.ensureChannel(this);KuroUpdater572.schedule(this);getWindow().setStatusBarColor(Color.rgb(10,7,15));getWindow().setNavigationBarColor(Color.rgb(10,7,15));root=new FrameLayout(this);setContentView(root);ui=new WebView(this);ui.setBackgroundColor(Color.rgb(10,7,15));ui.getSettings().setJavaScriptEnabled(true);ui.getSettings().setDomStorageEnabled(true);ui.getSettings().setAllowFileAccess(false);ui.getSettings().setAllowContentAccess(false);ui.addJavascriptInterface(new Bridge(),"KuroNative");ui.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView v,String url){if("https://kuro.local/index.html".equals(url)){uiReady=true;dismissEntry15();openPendingNews();flushAccountEvent();if(startupPending560){startupPending560=false;v.evaluateJavascript("window.kuroRestoreStartup560&&window.kuroRestoreStartup560()",null);}}}@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return true;}@Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest req){Uri u=req.getUrl();if("https".equals(u.getScheme())&&"kuro.local".equals(u.getHost())&&u.getPath()!=null&&u.getPath().startsWith("/result/")){byte[] bytes=responses.remove(u.getLastPathSegment());if(bytes!=null)return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(bytes));return new WebResourceResponse("text/plain","UTF-8",404,"Missing",Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));}if("https".equals(u.getScheme())&&"kuro.local".equals(u.getHost())&& ("/index.html".equals(u.getPath())||"/app.js".equals(u.getPath())||"/providers531.js".equals(u.getPath())||"/providers532.js".equals(u.getPath())||"/providers535.js".equals(u.getPath())||"/providers537.js".equals(u.getPath())||"/providers539.js".equals(u.getPath())||"/simulcasts532.js".equals(u.getPath())||"/unified533.js".equals(u.getPath())||"/unified-episodes533.js".equals(u.getPath())||"/catalog573.js".equals(u.getPath())||"/mirrors579.js".equals(u.getPath())||"/failover531.js".equals(u.getPath())||"/route-state538.js".equals(u.getPath())||"/accounts542.js".equals(u.getPath())||"/tracker-sync553.js".equals(u.getPath())||"/local-restore-safety550.js".equals(u.getPath())||"/accounts542.css".equals(u.getPath())||"/profile-settings560.css".equals(u.getPath())||"/settings560.js".equals(u.getPath())||"/backup560.js".equals(u.getPath())||"/preset-manifest571.js".equals(u.getPath())||"/profile-presets571.js".equals(u.getPath())||"/profile-presets571.css".equals(u.getPath())||"/library-tools571.css".equals(u.getPath())||"/smooth-ui.css".equals(u.getPath())||"/smooth-ui.js".equals(u.getPath())||"/watch-queue571.js".equals(u.getPath())||"/privacy571.js".equals(u.getPath())||"/anime-preferences571.js".equals(u.getPath())||"/accessibility571.js".equals(u.getPath())||"/profile-default571.png".equals(u.getPath())||"/profile-art-arm.js".equals(u.getPath())||"/profile-art-arm.css".equals(u.getPath())||u.getPath().matches("/profile-art/(pfp|banner)/art-[0-9]{2}\\.webp")||u.getPath().matches("/preset571-[ab]-[0-9]{3}\\.jpg")||"/profile-banner547.jpg".equals(u.getPath())||u.getPath().matches("/profile-avatar547-[a-l]\\.jpg")||u.getPath().matches("/profile-card548-(anime|history)-[ab]\\.jpg")||u.getPath().matches("/profile-(character|banner)549-[0-9]{2}\\.jpg")||"/style.css".equals(u.getPath())||"/kuro-logo.png".equals(u.getPath())||"/kuro-logo.svg".equals(u.getPath()))){try{return new WebResourceResponse(u.getPath().endsWith(".html")?"text/html":u.getPath().endsWith(".css")?"text/css":u.getPath().endsWith(".png")?"image/png":u.getPath().endsWith(".jpg")?"image/jpeg":u.getPath().endsWith(".webp")?"image/webp":u.getPath().endsWith(".svg")?"image/svg+xml":"application/javascript","UTF-8",getAssets().open(u.getPath().substring(1)));}catch(IOException ignored){}}return null;}});root.addView(ui);applyWindowPreferences560();ui.loadUrl("https://kuro.local/index.html");if(state==null&&!pendingNews&&!openingAccount)showEntry();handleDiscordOAuth564(getIntent());handleAccountIntent(getIntent());handleUpdaterIntent(getIntent());}
 private void showMiruroVerification(){
  WebView check=new WebView(this);WebSettings settings=check.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setUserAgentString(SOURCE_UA);
  android.webkit.CookieManager.getInstance().setAcceptCookie(true);android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(check,true);
  check.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest request){Uri u=request.getUrl();String host=u.getHost();return !"https".equals(u.getScheme())||!("www.miruro.cx".equals(host)||"miruro.cx".equals(host)||"challenges.cloudflare.com".equals(host));}});
  FrameLayout holder=new FrameLayout(this);holder.addView(check,new FrameLayout.LayoutParams(-1,dp(340)));
  AlertDialog dialog=new AlertDialog.Builder(KuroDialogs581.themed(this)).setTitle("Library 11 verification").setMessage("If a verification check appears, complete it yourself. Then tap Done and retry the source. Access is not guaranteed.").setView(holder).setPositiveButton("Done",(d,w)->Toast.makeText(MainActivity.this,"Now retry the unavailable source.",Toast.LENGTH_LONG).show()).setNegativeButton("Cancel",(d,w)->{}).create();
  dialog.setOnDismissListener(d->{try{android.webkit.CookieManager.getInstance().flush();check.stopLoading();check.destroy();}catch(RuntimeException ignored){}});dialog.show();check.loadUrl(MIRURO_ORIGIN+"/");
 }
 private void openPendingNews(){
  if(!pendingNews||!uiReady||ui==null)return;boolean restore=pendingNewsCold;pendingNews=false;pendingNewsCold=false;
  // Consume the action so Activity recreation alone cannot replay an old tap.
  Intent delivered=getIntent();if(delivered!=null&&NewsSync.ACTION_OPEN.equals(delivered.getAction()))delivered.setAction(null);
  if(playerLayer!=null){if(!"news".equals(watchOverlay))showWatchOverlay("news");return;}
  ui.evaluateJavascript(restore?"window.kuroRestoreForNews?window.kuroRestoreForNews():(window.kuroNewsOpen&&window.kuroNewsOpen())":"window.kuroNewsOpen&&window.kuroNewsOpen()",null);
 }
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(intent!=null&&NewsSync.ACTION_OPEN.equals(intent.getAction())){pendingNews=true;if(!uiReady)pendingNewsCold=true;openPendingNews();}handleDiscordOAuth564(intent);handleAccountIntent(intent);handleUpdaterIntent(intent);}
 private static boolean isAccountIntent(Intent intent){return intent!=null&&Intent.ACTION_VIEW.equals(intent.getAction())&&intent.getData()!=null&&AccountOAuth.SCHEME.equals(intent.getData().getScheme())&&"oauth".equals(intent.getData().getHost());}
 private void flushAccountEvent(){if(ui!=null&&uiReady&&!pendingAccountEvent.isEmpty()){String event=pendingAccountEvent;pendingAccountEvent="";ui.evaluateJavascript("window.kuroAccountEvent&&window.kuroAccountEvent("+event+")",null);}}
 private void postAccountEvent(JSONObject event){main.post(()->{pendingAccountEvent=event.toString();flushAccountEvent();});}
 private void handleAccountIntent(Intent intent){
  if(!isAccountIntent(intent))return;String raw=intent.getData().toString();intent.setData(null); // Never replay a callback after Activity recreation.
  pool.execute(()->{try{postAccountEvent(accounts.callback(raw));}catch(Exception e){try{postAccountEvent(new JSONObject().put("error",AccountError.display(e)));}catch(Exception ignored){}}});
 }
 private void openAccountBrowser(String target){
  try{Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(target));i.addCategory(Intent.CATEGORY_BROWSABLE);startActivity(i);}catch(Exception e){try{postAccountEvent(new JSONObject().put("error","No browser is available to open the provider sign-in."));}catch(Exception ignored){}}
 }
 private void copyGitHubCode568(String code){
  if(code==null||!code.matches("[A-Z0-9]{4}-[A-Z0-9]{4}"))return;
  android.content.ClipboardManager c=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(c==null)return;
  ClipData clip=ClipData.newPlainText("Kuro GitHub verification code",code);
  if(Build.VERSION.SDK_INT>=33){android.os.PersistableBundle extra=new android.os.PersistableBundle();extra.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE,true);clip.getDescription().setExtras(extra);}
  c.setPrimaryClip(clip);Toast.makeText(this,"Verification code copied. Open GitHub to enter it.",Toast.LENGTH_SHORT).show();
  main.postDelayed(()->{try{ClipData current=c.getPrimaryClip();if(current!=null&&current.getItemCount()>0&&code.contentEquals(current.getItemAt(0).coerceToText(this))){if(Build.VERSION.SDK_INT>=28)c.clearPrimaryClip();else c.setPrimaryClip(ClipData.newPlainText("", ""));}}catch(RuntimeException ignored){}},120000);
 }
 private void pickAccountImage(String kind){
  if(!("avatar".equals(kind)||"banner".equals(kind)))return;pendingImageKind=kind;
  try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,ACCOUNT_IMAGE_PICKER);}catch(Exception e){pendingImageKind="";Toast.makeText(this,"No image picker is available.",Toast.LENGTH_SHORT).show();}
 }
 @Override protected void onSaveInstanceState(Bundle state){state.putString("kuro.pendingImage",pendingImageKind);super.onSaveInstanceState(state);}
 @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
  super.onActivityResult(requestCode,resultCode,data);if(requestCode==SUBTITLE_PICKER561){finishSubtitle561(resultCode,data);return;}if(requestCode==BACKUP_EXPORT560||requestCode==BACKUP_IMPORT560){finishBackup560(requestCode,resultCode,data);return;}if(requestCode!=ACCOUNT_IMAGE_PICKER)return;final String kind=pendingImageKind;pendingImageKind="";
  if(resultCode!=RESULT_OK||data==null||data.getData()==null||!("avatar".equals(kind)||"banner".equals(kind)))return;final Uri uri=data.getData();if(!"content".equals(uri.getScheme()))return;
  pool.execute(()->{
   Bitmap bitmap=null,crop=null,scaled=null;try{
    String type=getContentResolver().getType(uri);if(type==null||!type.toLowerCase(Locale.ROOT).startsWith("image/"))throw new IOException();
    BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException();BitmapFactory.decodeStream(in,null,bounds);}
    if(bounds.outWidth<1||bounds.outHeight<1||bounds.outWidth>32768||bounds.outHeight>32768||(long)bounds.outWidth*bounds.outHeight>150000000L)throw new IOException();
    int w="avatar".equals(kind)?320:1000,h="avatar".equals(kind)?320:400,sample=1;while(bounds.outWidth/sample>w*2&&bounds.outHeight/sample>h*2)sample*=2;
    BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=sample;try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException();bitmap=BitmapFactory.decodeStream(in,null,options);}if(bitmap==null)throw new IOException();
    int bw=bitmap.getWidth(),bh=bitmap.getHeight(),cw=bw,ch=bh;if((long)bw*h>(long)bh*w)cw=Math.max(1,bh*w/h);else ch=Math.max(1,bw*h/w);
    crop=Bitmap.createBitmap(bitmap,(bw-cw)/2,(bh-ch)/2,cw,ch);scaled=Bitmap.createScaledBitmap(crop,w,h,true);ByteArrayOutputStream out=new ByteArrayOutputStream();if(!scaled.compress(Bitmap.CompressFormat.JPEG,82,out)||out.size()>600000)throw new IOException();
    String result="data:image/jpeg;base64,"+java.util.Base64.getEncoder().encodeToString(out.toByteArray());main.post(()->{if(ui!=null&&uiReady)ui.evaluateJavascript("window.kuroImagePicked&&window.kuroImagePicked("+JSONObject.quote(kind)+","+JSONObject.quote(result)+")",null);});
   }catch(Exception|OutOfMemoryError e){main.post(()->Toast.makeText(MainActivity.this,"Couldn't read that image. Try another photo.",Toast.LENGTH_LONG).show());}
   finally{if(scaled!=null&&scaled!=crop&&scaled!=bitmap)scaled.recycle();if(crop!=null&&crop!=bitmap)crop.recycle();if(bitmap!=null)bitmap.recycle();}
  });
 }
 private void newsStatusChanged(){if(ui!=null&&uiReady)ui.evaluateJavascript("window.kuroNewsPermissionsChanged&&window.kuroNewsPermissionsChanged()",null);}
 private void checkNewsOnResume(){
  if(!NewsSync.enabled(this))return;
  pool.execute(()->{NewsSync.refreshIfEnabled(getApplicationContext());main.post(()->{if(ui!=null&&uiReady)ui.evaluateJavascript("window.kuroNewsAutoUpdated&&window.kuroNewsAutoUpdated()",null);});});
 }
 private void requestNewsPermissionIfFirstTime(){
  if(Build.VERSION.SDK_INT<33||!NewsSync.enabled(this)||checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED||getSharedPreferences(NewsSync.PREFS,MODE_PRIVATE).getBoolean("permission_asked",false))return;
  main.postDelayed(()->{if(!activityResumed||isFinishing()||!NewsSync.enabled(this)||getSharedPreferences(NewsSync.PREFS,MODE_PRIVATE).getBoolean("permission_asked",false))return;getSharedPreferences(NewsSync.PREFS,MODE_PRIVATE).edit().putBoolean("permission_asked",true).apply();requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},NEWS_PERMISSION);},1800);
 }
 private void openNotificationSettings(){try{Intent intent=new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName());startActivity(intent);}catch(Exception e){try{startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}catch(Exception ignored){Toast.makeText(this,"Open Android app settings to allow notifications",Toast.LENGTH_LONG).show();}}}
 private void changeNewsAlerts(boolean enabled){
  NewsSync.setEnabled(this,enabled);
  if(enabled&&!NewsSync.allowed(this)){
   if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){boolean asked=getSharedPreferences(NewsSync.PREFS,MODE_PRIVATE).getBoolean("permission_asked",false);if(!asked||shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)){getSharedPreferences(NewsSync.PREFS,MODE_PRIVATE).edit().putBoolean("permission_asked",true).apply();requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},NEWS_PERMISSION);}else openNotificationSettings();}
   else openNotificationSettings();
  }
  newsStatusChanged();if(enabled)checkNewsOnResume();
 }
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==NEWS_PERMISSION){NewsSync.schedule(this);newsStatusChanged();}else if(request==RPC_NOTIFICATION_PERMISSION564)discordRefresh563();}
 private FrameLayout entrySplash15;private void dismissEntry15(){FrameLayout s=entrySplash15;if(s==null)return;entrySplash15=null;s.animate().alpha(0f).setDuration(200).withEndAction(()->{if(root!=null)root.removeView(s);}).start();}
 void showEntry(){FrameLayout splash=new FrameLayout(this);splash.setBackgroundColor(Color.rgb(10,7,15));splash.setClickable(false);entrySplash15=splash;root.addView(splash,new FrameLayout.LayoutParams(-1,-1));main.postDelayed(this::dismissEntry15,650);}

 private SharedPreferences settings560(){return KuroPreferences560.prefs(this);}
 private void applyWindowPreferences560(){
  SharedPreferences p=settings560();String orientation=p.getString("orientation","auto");int want="portrait".equals(orientation)?ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:"landscape".equals(orientation)?ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
  if(getRequestedOrientation()!=want)setRequestedOrientation(want);
  int brightness=p.getInt("brightness",-1);WindowManager.LayoutParams lp=getWindow().getAttributes();lp.screenBrightness=brightness<0?WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE:brightness/100f;getWindow().setAttributes(lp);
  int zoom=p.getInt("textZoom",100);if(ui!=null)ui.getSettings().setTextZoom(zoom);if(player!=null){player.getSettings().setTextZoom(zoom);android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(player,p.getBoolean("thirdCookies",false));}updateKeepScreen560();
 }
 private void updateKeepScreen560(){boolean keep=(customView!=null||enhancedFullscreen)&&settings560().getBoolean("keepOn",true);if(keep)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}
 private void applySettings560(String raw){boolean before=settings560().getBoolean("errorAlerts",false);if(!KuroPreferences560.store(this,raw))return;applyWindowPreferences560();if(!before&&settings560().getBoolean("errorAlerts",false)&&Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},ERROR_PERMISSION560);}
 // The countdown is session-bound and never advances from the background or PiP.
 private void cancelAutoNext573(){
  if(autoNextTick573!=null)main.removeCallbacks(autoNextTick573);autoNextTick573=null;
  AlertDialog d=autoNextDialog573;autoNextDialog573=null;if(d!=null&&d.isShowing())d.dismiss();
 }
 private void offerAutoNext573(String session,int next){
  if(!activityResumed||isInPictureInPictureMode()||playerLayer==null||ui==null||!session.equals(watchSession)||!getPreferences(MODE_PRIVATE).getBoolean("auto_next",false))return;
  cancelAutoNext573();final long end=android.os.SystemClock.elapsedRealtime()+10000;
  final Runnable advance=()->{if(!session.equals(watchSession)||!activityResumed||isInPictureInPictureMode()||playerLayer==null||ui==null||!getPreferences(MODE_PRIVATE).getBoolean("auto_next",false)){cancelAutoNext573();return;}cancelAutoNext573();closePlayer();if(ui!=null)ui.evaluateJavascript("window.kuroChooseEpisode&&window.kuroChooseEpisode("+next+")",null);};
  AlertDialog d=new AlertDialog.Builder(KuroDialogs581.themed(this)).setTitle("Next episode")
   .setMessage("Playing the next episode in 10 seconds.")
   .setNegativeButton("Cancel",(dialog,which)->cancelAutoNext573())
   .setPositiveButton("Play now",(dialog,which)->advance.run()).create();
  autoNextDialog573=d;d.setOnCancelListener(dialog->cancelAutoNext573());d.setOnDismissListener(dialog->{if(autoNextDialog573==d){if(autoNextTick573!=null)main.removeCallbacks(autoNextTick573);autoNextTick573=null;autoNextDialog573=null;}});
  d.show();autoNextTick573=new Runnable(){@Override public void run(){
   if(autoNextDialog573!=d||!session.equals(watchSession)||!activityResumed||isInPictureInPictureMode()||!getPreferences(MODE_PRIVATE).getBoolean("auto_next",false)){cancelAutoNext573();return;}
   long remaining=end-android.os.SystemClock.elapsedRealtime();if(remaining<=0){advance.run();return;}d.setMessage("Playing the next episode in "+((remaining+999)/1000)+" seconds.");main.postDelayed(this,250);
  }};main.post(autoNextTick573);
 }
 private void notifyPlayerError560(){
  if(activityResumed||!settings560().getBoolean("errorAlerts",false))return;long now=android.os.SystemClock.elapsedRealtime();if(now-lastErrorNotification560<60000&&lastErrorNotification560!=0)return;
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
  NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(nm==null||!nm.areNotificationsEnabled())return;
  try{nm.createNotificationChannel(new NotificationChannel("kuro-playback-errors560","Playback errors",NotificationManager.IMPORTANCE_DEFAULT));Intent launch=new Intent(this,MainActivity.class);launch.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);PendingIntent tap=PendingIntent.getActivity(this,5602,launch,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification n=new Notification.Builder(this,"kuro-playback-errors560").setSmallIcon(getApplicationInfo().icon).setContentTitle("Kuro.tv playback").setContentText("A player reported an error. Open Kuro to try another server.").setContentIntent(tap).setAutoCancel(true).build();nm.notify(5602,n);lastErrorNotification560=now;}catch(RuntimeException ignored){}
 }
 private void openUserLink560(String address){String mode=settings560().getString("external","allow");if("blocked".equals(mode)){Toast.makeText(this,"External links are blocked in Settings.",Toast.LENGTH_SHORT).show();return;}Runnable open=()->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(address)));}catch(Exception e){Toast.makeText(this,"No app can open this link.",Toast.LENGTH_SHORT).show();}};if("ask".equals(mode))new AlertDialog.Builder(KuroDialogs581.themed(this)).setTitle("Open external link?").setMessage("This will open another app or browser.").setNegativeButton("Cancel",null).setPositiveButton("Open",(d,w)->open.run()).show();else open.run();}
 private String diagnostics560(){String web="unknown";try{android.content.pm.PackageInfo info=WebView.getCurrentWebViewPackage();if(info!=null)web=info.packageName+" "+info.versionName;}catch(RuntimeException ignored){}return "Kuro.tv 5.73 · Android "+Build.VERSION.RELEASE+" (API "+Build.VERSION.SDK_INT+") · WebView "+web+" · Available device storage (approx.): "+(getFilesDir().getUsableSpace()/1048576L)+" MiB. No account or browsing data is included.";}
 private void settingsAction560(String action){
  switch(action){case "cache":new AlertDialog.Builder(KuroDialogs581.themed(this)).setTitle("Clear cached files?").setMessage("This keeps cookies, account connections, profile, lists and history. Pages may load more slowly next time.").setNegativeButton("Cancel",null).setPositiveButton("Clear cache",(d,w)->{if(ui!=null)ui.clearCache(true);Toast.makeText(this,"Cached files cleared; saved data was kept.",Toast.LENGTH_SHORT).show();}).show();break;
   case "downloads":KuroOffline561.showDownloads(this);break;
   case "developer":openUserLink560("https://discord.com/users/825998094067695616");break;
   case "licenses":try{new AlertDialog.Builder(KuroDialogs581.themed(this)).setTitle("Third-party notices").setMessage(assetText("THIRD_PARTY_NOTICES.txt")+"\n\n"+assetText("DISCORD-SOCIAL-SDK-NOTICES.txt")).setPositiveButton("Close",null).show();}catch(IOException e){Toast.makeText(this,"Could not open the notices.",Toast.LENGTH_SHORT).show();}break;
   case "updates572":showUpdater572();break;
   case "updatesAuto582":showUpdateAuto582();break;
   case "updatesDelay583":showUpdateDelay583();break;
   case "updatesInterval573":showUpdateInterval573();break;
   case "updatesNetwork573":showUpdateNetwork573();break;
   case "pip":enterPip560();break;
   case "reload":if(player==null){Toast.makeText(this,"No player is open to reload.",Toast.LENGTH_SHORT).show();break;}final WebView target=player;new AlertDialog.Builder(KuroDialogs581.themed(this)).setTitle("Reload this player?").setMessage("Playback may restart; saved lists and history stay on this phone.").setNegativeButton("Cancel",null).setPositiveButton("Reload",(d,w)->{if(target==player)target.reload();}).show();break;
  }
 }
 private void showUpdateNetwork573(){
  boolean current=KuroUpdater572.wifiOnly(this);
  KuroDialogs581.build(this,"Update download network")
   .subtitle("Choose the network allowed to download a Kuro update.")
   .choice(new String[]{"Wi-Fi or mobile data","Wi-Fi only"},
    new String[]{"Downloads as soon as a release is found.","Waits until Wi-Fi is the active network."},current?1:0,
    which->{KuroUpdater572.setWifiOnly(this,which==1);Toast.makeText(this,"Update download network saved.",Toast.LENGTH_SHORT).show();})
   .note("Release checks may still use mobile data; only the APK download follows this setting.")
   .action("Cancel",false,null)
   .show();
 }
 private void showUpdateInterval573(){
  int current=KuroUpdater572.reminderDays(KuroUpdater572.prefs(this));
  KuroDialogs581.build(this,"Update reminder spacing")
   .subtitle("How often Kuro may remind you about a release.")
   .choice(new String[]{"About 1 day","About 2 days","About 3 days"},
    new String[]{"Nudges soon after a release appears.","A quieter cadence for a settled library.","Fewest interruptions."},Math.max(0,Math.min(2,current-1)),
    which->{KuroUpdater572.setReminderDays(this,which+1);Toast.makeText(this,"Update reminder spacing saved. Android may delay notices.",Toast.LENGTH_LONG).show();})
   .note("Spacing is advisory: Android decides when background work actually runs.")
   .action("Cancel",false,null)
   .show();
 }
 /** Settings \u2192 Kuro updates: status, automatic mode and the manual controls. */
 private void showUpdater572(){
  if(isFinishing())return;
  JSONObject status=KuroUpdater572.status(this);
  KuroUpdateCenter582.Snapshot snap=KuroUpdateCenter582.snapshot(this);
  boolean on=KuroUpdater572.enabled(this);
  boolean auto=KuroUpdateCenter582.autoEnabled(this);
  KuroDialogs581.Builder dialog=KuroDialogs581.build(this,"Kuro updates")
   .subtitle(KuroUpdateCenter582.summary(this))
   .row("Automatic updates",auto?(KuroUpdateCenter582.autoInstall(this)?"On \u2014 download and install":"On \u2014 download only"):"Off \u2014 remind me")
   .row("Installed","Kuro "+KuroUpdater572.installedVersionName(this))
   .row("GitHub offers",status.optString("latestSeen","\u2014"))
   .row("Download network",KuroUpdater572.wifiOnly(this)?"Wi-Fi only":"Wi-Fi or mobile data")
   .row("Install delay",KuroUpdateSchedule583.delayLabel(KuroUpdateSchedule583.delayHours(this))
     +" after the release is published");
  if(!status.optBoolean("allowed",true))dialog.row("Notifications","Blocked in Android");
  String error=status.optString("lastError","");
  if(!error.isEmpty())dialog.note(error);
  if(snap.busy())dialog.action("Show progress",true,this::openUpdateDialog582);
  else if(snap.ready())dialog.action("Install",true,this::installDownloadedUpdate572);
  else if(snap.scheduled())dialog.action("Update now",true,this::checkAndMaybeInstall582);
  else dialog.action(auto?"Update now":"Check now",true,this::checkAndMaybeInstall582);
  if(snap.scheduled())dialog.note(KuroUpdateCenter582.scheduledLine(snap)+" Kuro updates itself on the next check once that window is over, so the wait never becomes indefinite.");
  dialog.action("Automatic update settings",false,this::showUpdateAuto582);
  dialog.action("Update delay",false,this::showUpdateDelay583);
  dialog.action(on?"Turn checks off":"Turn checks on",false,()->{KuroUpdater572.setEnabled(this,!on);showUpdater572();});
  dialog.action("Close",false,null);
  dialog.note("Android decides when background work runs, so a check or download can be delayed. Android always asks before installing; Kuro cannot install silently.");
  try{dialog.show();}catch(RuntimeException ignored){}
 }
 /** 5.82: how far automatic updating goes, from full auto to reminders only. */
 private void showUpdateAuto582(){
  boolean auto=KuroUpdateCenter582.autoEnabled(this);
  boolean install=KuroUpdateCenter582.autoInstall(this);
  KuroDialogs581.build(this,"Automatic updates")
   .subtitle("Kuro reads the public release page, then prepares the update on this phone.")
   .choice(new String[]{"Check, download and install","Check and download only","Off \u2014 remind me first"},
    new String[]{"A newer release downloads by itself, and Android's installer opens once it is verified.",
     "The file is prepared in the background; you tap Install when you want it.",
     "Kuro only tells you that a release exists."},
    !auto?2:(install?0:1),
    which->{
     KuroUpdateCenter582.setAutoEnabled(this,which<2);
     KuroUpdateCenter582.setAutoInstall(this,which==0);
     Toast.makeText(this,which==2?"Kuro will only tell you about new releases.":which==1?"Downloads are automatic; installing stays your tap.":"Kuro now installs verified updates automatically.",Toast.LENGTH_LONG).show();
     showUpdater572();
    })
   .note("The download network setting still applies. The file is checked against the published SHA-256 and the release certificate, and Android always asks before installing.")
   .action("Cancel",false,null)
   .show();
 }
 /** 5.83: how long a published release waits before it installs itself. */
 private void showUpdateDelay583(){
  int current=KuroUpdateSchedule583.delayHours(this);
  int[] values={24,48,0};
  String[] labels={"After 24 hours","After 48 hours","As soon as it is found"};
  String[] hints={"Default. Time to finish what you are watching, then it installs itself.",
   "The quieter end of the window; still automatic.",
   "No wait: a release installs the moment Kuro sees it."};
  int selected=0;
  for(int i=0;i<values.length;i++)if(values[i]==current)selected=i;
  KuroDialogs581.build(this,"When automatic updates land")
   .subtitle("A release installs itself once its window is over, or on the next check after that.")
   .choice(labels,hints,selected,
    which->{
     KuroUpdateSchedule583.setDelayHours(this,values[which]);
     Toast.makeText(this,which==2?"Kuro now installs a release as soon as it finds one.":"Kuro now installs a release "+KuroUpdateSchedule583.delayLabel(values[which])+" it is published.",Toast.LENGTH_LONG).show();
     showUpdater572();
    })
   .note("The window is measured from the release's own publish time, so a device that was off for two days updates straight away instead of waiting again. Android decides when background work runs; opening Kuro runs a check immediately.")
   .action("Cancel",false,null)
   .show();
 }
 /** One check off the main thread, with the update screen open so progress is visible. */
 private void checkAndMaybeInstall582(){
  if(isFinishing())return;
  KuroUpdateDialog582.resetHidden();
  openUpdateDialog582();
  pool.execute(()->{
   try{KuroUpdateCenter582.checkNow(this,true);}catch(RuntimeException ignored){}
   main.post(this::openUpdateDialog582);
  });
 }
 /* 1.5.1: the in-app Update button. Run the check off the main thread via
  * checkAndMaybeInstall582() and open the theme-aware update screen immediately. */
 private void updateButton15(){
  if(isFinishing())return;
  KuroUpdater572.prefs(this).edit().putString("stage",KuroUpdateCenter582.CHECKING).putString("lastError","").apply();
  checkAndMaybeInstall582();
 }
 private void openUpdateDialog582(){
  if(isFinishing()||(!activityResumed&&!KuroUpdateDialog582.isOpen()))return;
  KuroUpdateDialog582.open(this,updateHost582);
 }
 private final KuroUpdateDialog582.Host updateHost582=new KuroUpdateDialog582.Host(){
  @Override public void retryUpdate582(){checkAndMaybeInstall582();}
  @Override public void installUpdate582(){installDownloadedUpdate572();}
 };
 /**
  * While the app is in front, a download that started in the background keeps the update
  * screen visible and a verified file goes straight to Android's installer once.
  */
 private final Runnable updateWatch582=new Runnable(){
  @Override public void run(){
   if(isFinishing()||!activityResumed)return;
   KuroUpdateCenter582.Snapshot snap=KuroUpdateCenter582.snapshot(MainActivity.this);
   if(KuroUpdateSchedule583.claimCatchUp(MainActivity.this))pool.execute(()->{try{KuroUpdater572.run(MainActivity.this);}catch(RuntimeException ignored){}});
   if(snap.paused()&&KuroUpdater572.activeWifi(MainActivity.this)!=null)KuroUpdater572.requestDownload(MainActivity.this);
   if((snap.transferring()||snap.verifying())&&player==null&&nativePlayer561==null&&KuroUpdateDialog582.autoOpenAllowed(MainActivity.this))openUpdateDialog582();
   main.postDelayed(this,1500);
  }
 };
 private void handleUpdaterIntent(Intent intent){
  if(intent==null||!KuroUpdater572.ACTION_INSTALL.equals(intent.getAction()))return;
  intent.setAction(null); // A hand-off must never replay after a relaunch.
  main.post(this::installDownloadedUpdate572);
 }
 /**
  * Re-checks the downloaded file, then asks Android to install it. Android re-verifies
  * the package and signature and always shows its own confirmation; Kuro cannot and
  * does not install anything silently.
  */
 private void installDownloadedUpdate572(){
  File apk=KuroUpdater572.readyFile(this);
  if(apk==null){Toast.makeText(this,"No checked Kuro update is ready. Open Settings → Kuro updates.",Toast.LENGTH_LONG).show();return;}
  KuroApkPreflight572.Result check=KuroApkPreflight572.inspect(this,apk);
  if(!check.ok()){
   KuroUpdater572.discard(this,check.message());
   new AlertDialog.Builder(KuroDialogs581.themed(this)).setTitle("Update not used").setMessage(check.message()+"\n\nNothing was installed and Kuro was not changed.").setPositiveButton("Close",null).show();
   return;
  }
  if(Build.VERSION.SDK_INT>=26&&!getPackageManager().canRequestPackageInstalls()){
   new AlertDialog.Builder(KuroDialogs581.themed(this)).setTitle("Allow Kuro to install updates?").setMessage("Android needs your permission before it will open its installer. Turn on “Allow from this source” for Kuro, then return and tap Install.").setNegativeButton("Cancel",null).setPositiveButton("Open settings",(d,w)->{try{startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));}catch(RuntimeException e){Toast.makeText(this,"Could not open this setting on your device.",Toast.LENGTH_LONG).show();}}).show();
   return;
  }
  try{
   Uri uri=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".updates572",apk);
   Intent install=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
   startActivity(install);
  }catch(RuntimeException e){Toast.makeText(this,"Android could not open its installer for this file.",Toast.LENGTH_LONG).show();}
 }
 private android.app.PictureInPictureParams pipParams571(){String state=nativePlayer561==null?"":""+nativePlayer561.presenceState563();return KuroPipControls571.params(this,watchSession,nativePlayer561!=null,!("paused".equals(state)||"ended".equals(state)));}
 private void updatePipActions571(){if(isInPictureInPictureMode())try{setPictureInPictureParams(pipParams571());}catch(RuntimeException ignored){}}
 void pipCommand571(String session,String action){if(!isInPictureInPictureMode()||nativePlayer561==null||watchSession.isEmpty()||!watchSession.equals(session))return;if(!("play".equals(action)||"pause".equals(action)))return;nativePlayer561.command564("play".equals(action));updatePipActions571();discordRefresh563();}
 private void enterPip560(){
  if(player==null||playerLayer==null||!getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)){Toast.makeText(this,"Open a supported player before using picture-in-picture.",Toast.LENGTH_LONG).show();return;}
  try{if(customView==null&&!enhancedFullscreen){enterEnhancedFullscreen();pipExpanded560=enhancedFullscreen;}pipRequest560=true;boolean ok=enterPictureInPictureMode(pipParams571());if(!ok){pipRequest560=false;if(pipExpanded560){pipExpanded560=false;exitEnhancedFullscreen();}Toast.makeText(this,"Picture-in-picture is not available for this player.",Toast.LENGTH_LONG).show();}}catch(RuntimeException e){pipRequest560=false;if(pipExpanded560){pipExpanded560=false;exitEnhancedFullscreen();}Toast.makeText(this,"Picture-in-picture is not available for this player.",Toast.LENGTH_LONG).show();}
 }
 @Override public void onPictureInPictureModeChanged(boolean inPip,android.content.res.Configuration config){super.onPictureInPictureModeChanged(inPip,config);if(inPip)discordForeground563();else if(!activityResumed)discordBackground563();pipRequest560=false;if(enhancedToolbar!=null){if(inPip){main.removeCallbacks(hideFullscreenExit569);enhancedToolbar.setVisibility(View.GONE);}else showFullscreenExit569();}if(inPip){if(player!=null)player.onResume();}else{if(pipExpanded560){pipExpanded560=false;exitEnhancedFullscreen();}if(!activityResumed&&player!=null)player.onPause();if(!activityResumed&&nativePlayer561!=null)nativePlayer561.pauseForBackground();}}
 private void chooseBackup560(String mode,String payload){
  if(!("export".equals(mode)||"import".equals(mode)))return;if("export".equals(mode)&&(payload==null||payload.length()>6000000))return;
  try{Intent intent=new Intent("export".equals(mode)?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/json");if("export".equals(mode)){backupExport560=payload;intent.putExtra(Intent.EXTRA_TITLE,"Kuro-local-backup.json");}startActivityForResult(intent,"export".equals(mode)?BACKUP_EXPORT560:BACKUP_IMPORT560);}catch(Exception e){backupExport560="";Toast.makeText(this,"Could not open Android's file picker.",Toast.LENGTH_LONG).show();}
 }
 private void backupResult560(String text,String error){if(ui==null||!uiReady){Toast.makeText(this,"Import was interrupted. Please choose Import again.",Toast.LENGTH_LONG).show();return;}ui.evaluateJavascript("(function(){return !!(window.kuroBackupLoaded560&&window.kuroBackupLoaded560("+JSONObject.quote(text)+","+JSONObject.quote(error)+"))})()",result->{if(!"true".equals(result))Toast.makeText(this,"Import was interrupted. Please choose Import again.",Toast.LENGTH_LONG).show();});}
 private void finishBackup560(int request,int result,Intent data){
  final String exported=backupExport560;backupExport560="";if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();if(!"content".equals(uri.getScheme()))return;
  if(request==BACKUP_EXPORT560&&exported.isEmpty()){Toast.makeText(this,"Export was interrupted. Please choose Export again.",Toast.LENGTH_LONG).show();return;}
  pool.execute(()->{try{if(request==BACKUP_EXPORT560){try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException();out.write(exported.getBytes(StandardCharsets.UTF_8));out.flush();}main.post(()->Toast.makeText(this,"Local backup saved. It is not encrypted.",Toast.LENGTH_LONG).show());}
   else{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(n>6000000-bytes.size())throw new IOException("too large");bytes.write(buf,0,n);}}String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes.toByteArray())).toString();main.post(()->backupResult560(text,""));}}
   catch(Exception e){main.post(()->{if(request==BACKUP_IMPORT560)backupResult560("","Could not read this backup. Nothing was changed.");else Toast.makeText(this,"Could not write the backup.",Toast.LENGTH_LONG).show();});}});
 }
 class Bridge{
  @JavascriptInterface public void get(String id,String url){
   if(!safeId(id)||url==null||url.length()>4096)return;
   pool.execute(()->{String result="",err="";HttpURLConnection c=null;
    try{
     URI u=new URI(url);if(!"https".equals(u.getScheme())||!sourceHost(u.getHost())||u.getUserInfo()!=null||u.getPort()!=-1)throw new Exception("Source not allowed");
     if("api.animeparadise.moe".equals(u.getHost())&&(u.getPath()==null||!(u.getPath().equals("/search")||u.getPath().matches("/anime/[A-Za-z0-9_-]{1,80}/episode"))))throw new Exception("Source path not allowed");
     if("www.animevillage.org".equals(u.getHost())&&(u.getPath()==null||!u.getPath().matches("/api/(search|episodes/[a-z0-9-]{1,160}|stream/[a-z0-9-]{1,160})")))throw new Exception("Source path not allowed");
     boolean miruro="www.miruro.cx".equals(u.getHost()),kitsuneEpisodes="kitsunee.moe".equals(u.getHost())&&u.getPath()!=null&&u.getPath().matches("/api/episodes/anikoto/[0-9]{1,8}");int maxBytes=kitsuneEpisodes?6500000:2500000;c=(HttpURLConnection)u.toURL().openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent",SOURCE_UA);c.setRequestProperty("Accept","text/html,application/json,*/*");c.setRequestProperty("X-Requested-With","XMLHttpRequest");
     if("aniwaves.tv".equals(u.getHost())||"anikoto.cz".equals(u.getHost())) c.setRequestProperty("Referer","https://"+u.getHost()+"/");
     if(miruro){c.setRequestProperty("Origin",MIRURO_ORIGIN);c.setRequestProperty("Referer",MIRURO_ORIGIN+"/");String cookie=android.webkit.CookieManager.getInstance().getCookie(MIRURO_ORIGIN+"/");if(cookie!=null&&!cookie.isEmpty()&&cookie.length()<=8192)c.setRequestProperty("Cookie",cookie);}
     int code=c.getResponseCode();if(code!=200)throw new Exception("Source returned HTTP "+code+" (no verification bypass)");
     byte[] bytes;try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(n>maxBytes-out.size())throw new Exception("Response too large");out.write(b,0,n);}bytes=out.toByteArray();}
     String type=c.getContentType();if(miruro&&u.getPath()!=null&&u.getPath().startsWith("/api/v1/anime")&&type!=null&&type.toLowerCase(Locale.ROOT).contains("application/octet-stream"))bytes=MiruroCatalog.decode(bytes,2500000);
     result=new String(bytes,StandardCharsets.UTF_8);if(kitsuneEpisodes)result=KitsuneCatalog.compact(result);
    }catch(Exception e){err=e.getMessage()==null?"Network error":e.getMessage();}finally{if(c!=null)c.disconnect();}
    if(err.isEmpty())responses.put(id,result.getBytes(StandardCharsets.UTF_8));final String js="window.__nativeResult("+JSONObject.quote(id)+","+JSONObject.quote(err)+")";main.post(()->{if(ui!=null)ui.evaluateJavascript(js,null);});
   });
  }
  @JavascriptInterface public void watchSearchClosed(int token){main.post(()->finishWatchOverlay("search",token));}
  @JavascriptInterface public void watchNewsClosed(int token){main.post(()->finishWatchOverlay("news",token));}
  @JavascriptInterface public void watchAccountClosed(int token){main.post(()->finishWatchOverlay("account",token));}
  @JavascriptInterface public void watchSearchSelected(String payload,int token){
   if(payload==null||payload.length()>100000)return;try{
    JSONObject item=new JSONObject(payload);String code=item.optString("source",""),title=item.optString("title","");
    String expected="ms".equals(code)?"hianime.ms":"dk".equals(code)?"hianime.dk":"nine".equals(code)?"9anime.or.at":"zoro".equals(code)?"zorotv.ba":"ani".equals(code)?"aniwaves.ru":"salt".equals(code)?"animesalt.cx":"flix".equals(code)?"hianimeq.top":"zenkai".equals(code)?"www.zenkaitv.com":"wixi".equals(code)?"aniwixi.xyz":"aw".equals(code)?"aniwatch.co.at":"miruro".equals(code)?"www.miruro.cx":"kitsune".equals(code)?"kitsunee.moe":"paradise".equals(code)?"api.animeparadise.moe":"";
    URI url=new URI(item.optString("url",""));if(title.trim().isEmpty()||expected.isEmpty()||!"https".equals(url.getScheme())||!expected.equals(url.getHost())||url.getUserInfo()!=null||url.getPort()!=-1)return;
   }catch(Exception e){return;}
   main.post(()->{if(!"search".equals(watchOverlay)||token!=watchOverlayToken||playerLayer==null||ui==null)return;finishWatchOverlay("search",token);closePlayer();ui.evaluateJavascript("window.kuroWatchSearchResult&&window.kuroWatchSearchResult("+JSONObject.quote(payload)+")",null);});
  }
  @JavascriptInterface public void openExternalSource(String code){
   final String target="anicult".equals(code)?"https://anicult.vercel.app/":"aniraku".equals(code)?"https://www.aniraku.tech/":"justanime".equals(code)?"https://justanime.to/":"paradise".equals(code)?"https://www.animeparadise.moe/":"";if(target.isEmpty())return;
   main.post(()->openUserLink560(target));
  }
  @JavascriptInterface public void openAnirakuTitle(String id){if(id==null||!id.matches("[0-9]{1,8}"))return;main.post(()->openUserLink560(Uri.parse("https://www.aniraku.tech/").buildUpon().appendPath("anime").appendPath(id).build().toString()));}
  @JavascriptInterface public void openLibrary12Episode(String id,String episode){
   if(id==null||!id.matches("[0-9]{1,8}")||episode==null||!episode.matches("[0-9]{1,4}"))return;int n;try{n=Integer.parseInt(episode);}catch(Exception e){return;}if(n<1||n>5000)return;
   main.post(()->{try{Uri uri=Uri.parse("https://kitsunee.moe/anime/watch").buildUpon().appendQueryParameter("anime",id).appendQueryParameter("ep",Integer.toString(n)).build();openUserLink560(uri.toString());}catch(Exception e){Toast.makeText(MainActivity.this,"No browser can open this source.",Toast.LENGTH_SHORT).show();}});
  }
  @JavascriptInterface public void verifyLibrary11(){main.post(()->showMiruroVerification());}
  @JavascriptInterface public void openLibrary11Episode(String id,String episode){
   if(id==null||!id.matches("[A-Za-z0-9_-]{1,80}")||episode==null||!episode.matches("[0-9]{1,4}"))return;int number;try{number=Integer.parseInt(episode);}catch(NumberFormatException e){return;}if(number<1||number>5000)return;
   main.post(()->{try{Uri uri=Uri.parse(MIRURO_ORIGIN).buildUpon().appendPath("watch").appendPath(id).appendQueryParameter("ep",Integer.toString(number)).build();openUserLink560(uri.toString());}catch(Exception e){Toast.makeText(MainActivity.this,"No browser can open this source.",Toast.LENGTH_SHORT).show();}});
  }
  private boolean safeId(String id){return id!=null&&id.matches("[A-Za-z0-9_-]{1,40}");}
  private void reply(String id,String data,String error){if(!safeId(id))return;if(error.isEmpty())responses.put(id,data.getBytes(StandardCharsets.UTF_8));String js="window.__nativeResult("+JSONObject.quote(id)+","+JSONObject.quote(error)+")";main.post(()->{if(ui!=null)ui.evaluateJavascript(js,null);});}
  @JavascriptInterface public void account(String id,String action,String payload){
   if(!safeId(id)||action==null||payload==null||payload.length()>30000)return;
   // Revocation cannot wait behind a long-poll/identity request on the worker pool.
   if("adminCancel".equals(action)){long generation=githubAdmin568.invalidate();pool.execute(()->{githubAdmin568.clearInvalidated(generation);reply(id,"{\"cancelled\":true}","");});return;}
   pool.execute(()->{try{
    JSONObject p=new JSONObject(payload),result;
    switch(action){
     case "status":result=accounts.status();break;
     case "adminStatus":result=githubAdmin568.status();break;
     case "adminBegin":result=githubAdmin568.begin();break;
     case "adminPoll":result=githubAdmin568.poll();break;
     case "adminOpen":githubAdmin568.currentCode();result=new JSONObject().put("opened",true);main.post(()->openAccountBrowser(GitHubAdmin568.VERIFY_URL));break;
     case "adminCopy":{String code=githubAdmin568.currentCode();result=new JSONObject().put("copied",true);main.post(()->copyGitHubCode568(code));break;}
     case "configure":{
      // The check and write share the same lock: WebView calls cannot bypass
      // owner verification, cancellation, or session expiration.
      synchronized(githubAdmin568){if(!githubAdmin568.verified())throw new AccountError("Owner verification expired or was not completed. Verify with GitHub before changing setup.");result=accounts.saveConfig(p);}break;
     }
     case "begin":{
      String provider=p.optString("provider","");
      String address=accounts.begin(provider);result=new JSONObject().put("message","anilist".equals(provider)?"After AniList opens, copy the token shown by AniList and paste it into Kuro on this phone.":"Continue in your browser, then return to Kuro.").put("pin","anilist".equals(provider));main.post(()->openAccountBrowser(address));
      break;
     }
     case "email":result=accounts.emailLogin(p.optString("email",""),p.optString("password",""),false);break;
     case "signup":result=accounts.emailLogin(p.optString("email",""),p.optString("password",""),true);break;
     case "anilistPin":result=accounts.connectAniList(p.optString("token",""));break;
     case "disconnect":{
      String provider=p.optString("provider","");
      result=accounts.disconnect(provider);break;
     }
     case "import":result=accounts.importList(p.optString("provider",""));break;
     case "importDetails":result=accounts.importDetails(p.optString("provider",""),p.optString("sourceId",""));break;
     case "trackerSync":result=accounts.syncTracker(p);break;
     case "trackerSearch":result=accounts.searchTracker(p.optString("query",""));break;
     default:throw new AccountError("Unknown account action.");
    }
    reply(id,result.toString(),"");
   }catch(Exception e){reply(id,"",AccountError.display(e));}});
  }
  @JavascriptInterface public void profileHeader560(boolean signedIn,String avatar){if(avatar==null||avatar.length()>16)return;main.post(()->{profileSignedIn560=signedIn;profileAvatar560=avatar;if(watchHeader instanceof WatchHeader)((WatchHeader)watchHeader).setProfile(profileSignedIn560,profileAvatar560,profileArt581);});}
 @JavascriptInterface public void profileArt581(String id){String v=id==null?"":id;if(!v.isEmpty()&&!v.matches("art-[0-9]{2}"))return;main.post(()->{if(v.equals(profileArt581))return;profileArt581=v;getSharedPreferences("kuro-native-581",MODE_PRIVATE).edit().putString("profile-art",v).apply();if(watchHeader instanceof WatchHeader)((WatchHeader)watchHeader).setProfile(profileSignedIn560,profileAvatar560,profileArt581);});}
  @JavascriptInterface public void watchMenuNavigate560(String action,int token){if(!Arrays.asList("profile","connections","watchlist","history","bookmarks","continue").contains(action))return;main.post(()->{if(!"account".equals(watchOverlay)||token!=watchOverlayToken||playerLayer==null)return;closePlayer();if(ui!=null)ui.evaluateJavascript("window.kuroMenuNavigate560&&window.kuroMenuNavigate560("+JSONObject.quote(action)+")",null);});}
  @JavascriptInterface public void applySettings560(String raw){if(raw!=null&&raw.length()<=10000)main.post(()->MainActivity.this.applySettings560(raw));}
  @JavascriptInterface public void updateButton15(){main.post(()->MainActivity.this.updateButton15());}
 @JavascriptInterface public void settingsAction560(String action){if(Arrays.asList("cache","downloads","developer","licenses","pip","reload","updates572","updatesInterval573","updatesNetwork573","updatesAuto582","updatesDelay583").contains(action))main.post(()->MainActivity.this.settingsAction560(action));}
  @JavascriptInterface public boolean discordRpcEnabled563(){return discordEnabled563();}
  @JavascriptInterface public String discordRpcStatus563(){return discordRpc563==null?"Discord RPC is not ready.":discordRpc563.status();}
  @JavascriptInterface public void setDiscordRpcEnabled563(boolean enabled){getSharedPreferences("kuro-discord-rpc563",MODE_PRIVATE).edit().putBoolean("enabled",enabled).apply();main.post(()->discordEnabledChanged563(enabled));}
  @JavascriptInterface public String discordOptions564(){return discordPrefs564==null?"{}":discordPrefs564.json().toString();}
  @JavascriptInterface public boolean setDiscordOption564(String key,String value){
   if(discordPrefs564==null||!discordPrefs564.set(key,value))return false;
   main.post(()->{discordPause564.reset();discordPauseCleared564=false;if(discordRpc563!=null)discordRpc563.settingsChanged();discordRefresh563();if("notification".equals(key))discordNotificationSetting564();});return true;
  }
  @JavascriptInterface public boolean setDiscordDownloadUrl564(String value){
   if(discordPrefs564==null||!discordPrefs564.setDownloadUrl(value))return false;
   main.post(()->{if(discordRpc563!=null)discordRpc563.settingsChanged();discordRefresh563();});return true;
  }
  @JavascriptInterface public String discordHistory564(){return discordRpc563==null?"No connection events yet.":discordRpc563.history();}
  @JavascriptInterface public void discordAction564(String action){if(Arrays.asList("clear","current","linked","test","link","unlink").contains(action))main.post(()->{if(!isFinishing()&&!isDestroyed())MainActivity.this.discordAction564(action);});}

  @JavascriptInterface public String diagnostics560(){return MainActivity.this.diagnostics560();}
  @JavascriptInterface public void backupFile560(String action,String payload){if(payload!=null&&payload.length()<=6000000)main.post(()->chooseBackup560(action,payload));}
  @JavascriptInterface public void openAccountHelp(String kind){
   final String address="discord".equals(kind)?"https://discord.com/developers/applications":"mal".equals(kind)?"https://myanimelist.net/apiconfig":"anilist".equals(kind)?"https://anilist.co/settings/developer":"";
   if(!address.isEmpty())main.post(()->openAccountBrowser(address));
  }
  @JavascriptInterface public void copyAccountRedirect(String kind){
   final String value="mal".equals(kind)?AccountOAuth.MAL_REDIRECT:"discord".equals(kind)?AccountOAuth.DISCORD_REDIRECT:"anilist".equals(kind)?AccountOAuth.ANILIST_PIN:"";
   if(!value.isEmpty())main.post(()->copySetupValue(value));
  }
  private void copySetupValue(String value){android.content.ClipboardManager c=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(c!=null){c.setPrimaryClip(android.content.ClipData.newPlainText("Kuro setup redirect",value));Toast.makeText(MainActivity.this,"Setup redirect copied.",Toast.LENGTH_SHORT).show();}}
  @JavascriptInterface public void news(String id,String mode){if(!safeId(id)||!("cache".equals(mode)||"refresh".equals(mode)||"force".equals(mode)))return;pool.execute(()->{try{JSONObject data="cache".equals(mode)?NewsSync.cached(MainActivity.this):NewsSync.refresh(MainActivity.this,"force".equals(mode));reply(id,data.toString(),"");}catch(Exception e){reply(id,"","News unavailable");}});}
  @JavascriptInterface public String newsSettings(){return NewsSync.status(MainActivity.this).toString();}
  @JavascriptInterface public void setNewsAlerts(boolean enabled){main.post(()->changeNewsAlerts(enabled));}
  @JavascriptInterface public void openNewsUrl(String address){String safe=NewsParser.safeLink(address==null?"":address);if(safe.isEmpty())return;main.post(()->openUserLink560(safe));}
  @JavascriptInterface public void newsMetadata(String id,String names){if(!safeId(id))return;pool.execute(()->{try{reply(id,NewsMetadata.lookup(names),"");}catch(Exception e){reply(id,"","Catalog details unavailable");}});}
  @JavascriptInterface public void catalog573(String id,String action,String payload){if(!safeId(id))return;pool.execute(()->{try{reply(id,KuroCatalog573.lookup(action,payload).toString(),"");}catch(Exception e){reply(id,"","AniList catalogue is unavailable. Try again.");}});}
  @JavascriptInterface public void adultMetadata(String id,String ids){if(!safeId(id))return;pool.execute(()->{try{reply(id,AdultMetadata.lookup(ids),"");}catch(Exception e){reply(id,"","18+ classification temporarily unavailable");}});}
  @JavascriptInterface public void addServers(String session,String payload){if(!safeId(session)||payload==null||payload.length()>100000)return;try{JSONObject data=new JSONObject(payload);main.post(()->{if(watchSink!=null&&session.equals(watchSession))watchSink.accept(session,data);});}catch(Exception ignored){}}
  @JavascriptInterface public void play(String payload){if(payload!=null&&payload.length()<600000)main.post(()->openWatch(payload));}
 }

 // The message listener only accepts fixed ready/bye messages; it exposes no
 // device, account, file, navigation, or arbitrary-code commands to a player.
 private String assetText(String name) throws IOException {
  try(InputStream in=getAssets().open(name);ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");
  }
 }
 // The script runs inside player frames only; it exposes no native methods and
 // does not touch the separate verification WebView.
 private void setupAdFrames(WebView view){
  adFrameMode=false;
  if(!settings560().getBoolean("adBlock",true))return;
  try{if(WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)){
   WebViewCompat.addDocumentStartJavaScript(view,assetText("player-ad-guard541.js"),Collections.singleton("*"));adFrameMode=true;
  }}catch(IOException|RuntimeException|LinkageError ignored){adFrameMode=false;}
 }
 private boolean shouldScanSkips(){return player!=null&&activityResumed&&getPreferences(MODE_PRIVATE).getBoolean("auto_skip",false);}
 private void setupSkipFrames(WebView view){
  skipFrameMode=false;skipFrames.clear();boolean added=false;
  try{
   if(!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)||!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))return;
   Set<String> origins=Collections.singleton("*");
   WebViewCompat.addWebMessageListener(view,"KuroSkipState",origins,(sender,message,origin,isMainFrame,reply)->{
    if(sender!=player||message.getType()!=WebMessageCompat.TYPE_STRING)return;
    String request=message.getData();
    if("bye".equals(request)){try{reply.postMessage("off");}catch(RuntimeException ignored){}skipFrames.remove(reply);return;}
    if(!"ready".equals(request)||skipFrames.contains(reply))return;
    // Never enable a frame that cannot be retained and disabled later.
    if(skipFrames.size()>=128){try{reply.postMessage("off");}catch(RuntimeException ignored){}return;}
    skipFrames.add(reply);
    try{reply.postMessage(shouldScanSkips()?"on":"off");}catch(RuntimeException ignored){}
   });
   added=true;
   WebViewCompat.addDocumentStartJavaScript(view,"window.__kuroFrameMode=true;\n"+assetText("auto-skip.js"),origins);
   skipFrameMode=true;
  }catch(IOException|RuntimeException|LinkageError e){
   if(added)try{WebViewCompat.removeWebMessageListener(view,"KuroSkipState");}catch(RuntimeException|LinkageError ignored){}
   skipFrames.clear();skipFrameMode=false;
  }
 }
 private void updateSkipFrames(){
  if(player==null)return;updateWebMarkers574();boolean on=shouldScanSkips();
  if(skipFrameMode){
   for(JavaScriptReplyProxy reply:new ArrayList<>(skipFrames))try{reply.postMessage(on?"on":"off");}catch(RuntimeException ignored){}
  }else{
   player.evaluateJavascript("window.__kuroSkip="+(on?"true":"false")+";if(window.kuroSetAutoSkip)window.kuroSetAutoSkip(window.__kuroSkip);",null);
  }
 }
 private void suspendSkipFrames(){
  boolean wasResumed=activityResumed;activityResumed=false;updateSkipFrames();activityResumed=wasResumed;skipFrames.clear();
 }
 // Restrict nested-player failure reports to a single known player origin.
 // Scripts run for only two bounded checks and report only the current attempt.
 private void setupFailureFrames(WebView view,java.util.function.Consumer<String> report){
  errorFrameMode=false;playerErrorScript=null;
  try{
   if(!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)||!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))return;
   Set<String> origins=Collections.singleton("https://megaplay.buzz");
   WebViewCompat.addWebMessageListener(view,"KuroPlayerReport",origins,(sender,message,origin,isMainFrame,reply)->{
    if(sender!=player||message.getType()!=WebMessageCompat.TYPE_STRING||!"https".equals(origin.getScheme())||!"megaplay.buzz".equals(origin.getHost()))return;
    report.accept(message.getData());
   });
   errorFrameMode=true;
  }catch(RuntimeException|LinkageError ignored){errorFrameMode=false;}
 }
 // Run the same local renderer within known HTTPS player frames. This is not
 // a cross-origin/DRM bypass: each frame still performs its own canvas check.
 // The bridge has no account, file, URL, navigation or arbitrary-code command.
 private void setupFxFrames(WebView view){
  fxFrameMode=false;fxFrames.clear();boolean added=false;
  try{
   if(!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)||!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))return;
   WebViewCompat.addWebMessageListener(view,"KuroFx559",FX_ORIGINS,(sender,message,origin,isMainFrame,reply)->{
    if(message.getType()==WebMessageCompat.TYPE_STRING)onFxFrameMessage(sender,origin,reply,message.getData());
   });added=true;
   fxFrameMode=true;
  }catch(RuntimeException|LinkageError e){
   if(added)try{WebViewCompat.removeWebMessageListener(view,"KuroFx559");}catch(RuntimeException|LinkageError ignored){}
   fxFrames.clear();fxFrameMode=false;
  }
 }
 private void prepareFxFrameScript(){
  if(!fxFrameMode||player==null)return;
  try{
   if(fxFrameScript!=null){fxFrameScript.remove();fxFrameScript=null;}
   String code="window.__kuroFxEpoch559="+playerAttempt+";\n"+assetText("ai-upscale540.js")+"\n"+assetText("video-effects537.js")+"\n"+assetText("fx-frame559.js");
   fxFrameScript=WebViewCompat.addDocumentStartJavaScript(player,code,FX_ORIGINS);
  }catch(IOException|RuntimeException|LinkageError e){
   fxFrameScript=null;fxFrameMode=false;fxFrames.clear();try{WebViewCompat.removeWebMessageListener(player,"KuroFx559");}catch(RuntimeException|LinkageError ignored){}
  }
 }
 static boolean fxOriginAllowed(Uri origin){
  return origin!=null&&"https".equals(origin.getScheme())&&origin.getHost()!=null&&origin.getPort()==-1&&FX_ORIGINS.contains("https://"+origin.getHost().toLowerCase(Locale.ROOT));
 }
 private FxFrame findFxFrame(JavaScriptReplyProxy reply){for(FxFrame f:fxFrames)if(f.reply==reply&&f.attempt==playerAttempt)return f;return null;}
 void onFxFrameMessage(WebView sender,Uri origin,JavaScriptReplyProxy reply,String data){
  if(sender!=player||reply==null||!fxOriginAllowed(origin)||data==null||data.length()>1200)return;
  String[] parts=data.split(":",3);if(parts.length<2||!parts[1].matches("[1-9][0-9]{0,18}"))return;
  try{if(Long.parseLong(parts[1])!=playerAttempt)return;}catch(NumberFormatException e){return;}
  FxFrame frame=findFxFrame(reply);
  if("bye".equals(parts[0])&&parts.length==2){if(frame!=null){fxFrames.remove(frame);publishFxStatus();}return;}
  if("ready".equals(parts[0])&&parts.length==2){
   if(frame==null){if(fxFrames.size()>=8)return;frame=new FxFrame(reply,playerAttempt);fxFrames.add(frame);}
   try{JSONObject current=fxConfig;if(current==null){current=PlayerOptionsDialog.readConfig(this);current.put("visible",activityResumed&&customView==null);}sendFxFrame(frame,current);}catch(Exception ignored){}
   return;
  }
  if(frame==null||!"status".equals(parts[0])||parts.length!=3)return;
  try{JSONObject st=new JSONObject(parts[2]);String v=st.optString("video","");String a=st.optString("ai","");
   if(!v.matches("off|waiting|active|color|unavailable|paused|compare")||!a.matches("off|waiting|active|unavailable|paused"))return;
   for(String key:new String[]{"reason","aiReason"}){String value=st.optString(key,"");if(value.length()>140)st.put(key,value.substring(0,140));}
   frame.status=st;publishFxStatus();
  }catch(Exception ignored){}
 }
 private void sendFxFrame(FxFrame frame,JSONObject config){
  if(frame==null||frame.attempt!=playerAttempt||config==null)return;
  try{JSONObject envelope=new JSONObject();envelope.put("version",559);envelope.put("settings",config);frame.reply.postMessage(envelope.toString());}catch(Exception ignored){}
 }
 private void updateFxFrames(JSONObject config){
  try{fxConfig=new JSONObject(config.toString());}catch(Exception e){return;}
  for(FxFrame frame:new ArrayList<>(fxFrames))sendFxFrame(frame,fxConfig);
 }
 private void clearFxFrames(){
  try{JSONObject off=new JSONObject();off.put("enabled",false);off.put("gain",100);off.put("visible",false);for(FxFrame frame:new ArrayList<>(fxFrames))sendFxFrame(frame,off);}catch(Exception ignored){}
  fxFrames.clear();fxTopStatus=null;
 }
 private static int fxScore(JSONObject status){
  if(status==null)return -1;String state=status.optString("video","");int score="active".equals(state)?100:"compare".equals(state)?90:"color".equals(state)?80:"unavailable".equals(state)?60:"waiting".equals(state)?40:"paused".equals(state)?20:0;
  return score+("active".equals(status.optString("ai"))?5:0);
 }
 private void publishFxStatus(){
  if(playerOptions==null||!playerOptions.isShowing())return;JSONObject best=fxTopStatus;int score=fxScore(best);
  for(FxFrame frame:fxFrames){int candidate=fxScore(frame.status);if(frame.attempt==playerAttempt&&candidate>score){score=candidate;best=frame.status;}}
  if(best!=null)playerOptions.setRuntimeStatus(best);
 }
 private void setupFxStatus(WebView view){
  try{if(!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))return;
   WebViewCompat.addWebMessageListener(view,"KuroFxStatus",Collections.singleton("*"),(sender,message,origin,isMainFrame,reply)->{
    if(sender!=player||!isMainFrame||message.getType()!=WebMessageCompat.TYPE_STRING||origin==null||!"https".equals(origin.getScheme())||!playerHost(origin.getHost()))return;
    String data=message.getData();if(data==null||data.length()>1000)return;try{fxTopStatus=new JSONObject(data);publishFxStatus();}catch(Exception ignored){}
   });
  }catch(RuntimeException|LinkageError ignored){}
 }
 // Accept only a verified, current main-frame end event; child-frame players may
 // not be observable. No timers or assumed playback durations are used here.
 private void setupCompletionFrames(WebView view,String session,String number,boolean[] verification,boolean[] sent){
  completionFrameMode=false;completionStartMode=false;
  if(!number.matches("[1-9][0-9]{0,3}"))return;
  try{
   if(!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))return;
   WebViewCompat.addWebMessageListener(view,"KuroFinished553",Collections.singleton("*"),(sender,message,origin,isMainFrame,reply)->{
    if(sender!=player||!isMainFrame||!activityResumed||sent[0]||verification[0]||message.getType()!=WebMessageCompat.TYPE_STRING||!"ended".equals(message.getData())||origin==null||!"https".equals(origin.getScheme())||!playerHost(origin.getHost())||!session.equals(watchSession))return;
    String loaded=sender.getUrl();Uri current=loaded==null?null:Uri.parse(loaded);if(current==null||!"https".equals(current.getScheme())||!origin.getHost().equals(current.getHost()))return;
    long attempt=playerAttempt;String check="(function(){try{var a=document.querySelectorAll('video');if(a.length!==1)return false;var v=a[0];return !!(v.ended&&Number.isFinite(v.duration)&&v.duration>=180&&v.currentTime>=v.duration*.98)}catch(e){return false}})()";
    sender.evaluateJavascript(check,result->{if(!"true".equals(result)||sender!=player||attempt!=playerAttempt||!session.equals(watchSession)||verification[0]||sent[0]||ui==null)return;sent[0]=true;ui.evaluateJavascript("window.kuroEpisodeEnded553&&window.kuroEpisodeEnded553("+JSONObject.quote(session)+","+JSONObject.quote(number)+")",null);});
   });
   completionFrameMode=true;
   if(WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)){WebViewCompat.addDocumentStartJavaScript(view,assetText("player-ended553.js"),Collections.singleton("*"));completionStartMode=true;}
  }catch(IOException|RuntimeException|LinkageError ignored){completionStartMode=false;}
 }
 private void applyPlayerOptions(JSONObject options){
  if(player==null)return;WebView current=player;long attempt=playerAttempt;JSONObject config;
  try{config=new JSONObject((options==null?PlayerOptionsDialog.readConfig(this):options).toString());if(options!=null)fxComparing=options.optBoolean("compare",false);config.put("compare",fxComparing);config.put("visible",activityResumed&&customView==null);}catch(Exception e){return;}
  updateFxFrames(config);
  current.evaluateJavascript("(function(){return window.__kuroFx537?window.__kuroFx537.configure("+config+"):null})()",result->{
   if(current!=player||attempt!=playerAttempt||playerOptions==null||!playerOptions.isShowing()||result==null||"null".equals(result))return;
   try{Object value=new org.json.JSONTokener(result).nextValue();if(value instanceof String){fxTopStatus=new JSONObject((String)value);publishFxStatus();}}catch(Exception ignored){}
  });
 }
 private void showPlayerOptions(){if(player==null)return;if(enhancedFullscreen||customView!=null){Toast.makeText(this,"Exit fullscreen to change Advanced settings.",Toast.LENGTH_SHORT).show();return;}if(nativePlayer561!=null){new AlertDialog.Builder(KuroDialogs581.themed(this)).setTitle("Advanced effects use the Web player").setMessage("The native player does not support Kuro’s Web video effects or audio boost. Switch to the existing Web player to use them; your position will be saved.").setNegativeButton("Cancel",null).setPositiveButton("Switch to Web",(d,w)->{webPreferred561=true;loadPlayer(directAddress561,directReferer561,directKind561);showPlayerOptions();}).show();return;}if(playerOptions!=null&&playerOptions.isShowing())return;playerOptions=new PlayerOptionsDialog(this,this::applyPlayerOptions);playerOptions.show();}
 private void prepareFailureScript(){
  if(playerErrorScript!=null){try{playerErrorScript.remove();}catch(RuntimeException ignored){}playerErrorScript=null;}
  if(!errorFrameMode||player==null)return;
  try{playerErrorScript=WebViewCompat.addDocumentStartJavaScript(player,"window.__kuroAttempt="+playerAttempt+";\n"+assetText("frame-errors532.js"),Collections.singleton("https://megaplay.buzz"));}
  catch(IOException|RuntimeException|LinkageError ignored){errorFrameMode=false;}
 }
 int dp(int v){return (int)(getResources().getDisplayMetrics().density*v+.5f);}
 TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextColor(color);t.setTextSize(size);return t;}
 android.graphics.drawable.GradientDrawable rounded(int color,int border){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(5));if(border!=0)d.setStroke(dp(1),border);return d;}
 private android.graphics.drawable.GradientDrawable diagnosticsShape573(int fill,int stroke,int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));d.setStroke(dp(1),stroke);return d;}
 private void diagnosticsCard573(LinearLayout panel,String heading,String value,String tag){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),dp(12),dp(14),dp(12));box.setBackground(diagnosticsShape573(0xff281a32,0xff4f395e,12));TextView label=text(heading,10,0xffbf93df);label.setLetterSpacing(.08f);if(Build.VERSION.SDK_INT>=28)label.setAccessibilityHeading(true);box.addView(label);addGap(box,5);TextView content=text(value,14,0xfff4edf9);content.setLineSpacing(dp(2),1f);content.setTag(tag);box.addView(content);panel.addView(box,new LinearLayout.LayoutParams(-1,-2));addGap(panel,9);}
 TextView chip(String title,boolean active){TextView t=text(title,13,active?0xff12081b:0xffe3dce9);t.setGravity(Gravity.CENTER);t.setPadding(dp(13),dp(10),dp(13),dp(10));t.setBackground(rounded(active?0xffae71e7:0xff281b32,active?0:0xff4c3a59));t.setOnTouchListener((v,e)->{if(e.getActionMasked()==MotionEvent.ACTION_DOWN)v.animate().scaleX(.96f).scaleY(.96f).setDuration(100).start();else if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL)v.animate().scaleX(1f).scaleY(1f).setDuration(150).start();return false;});return t;}
 void addGap(LinearLayout parent,int height){Space gap=new Space(this);parent.addView(gap,new LinearLayout.LayoutParams(1,dp(height)));}
 private boolean discordEnabled563(){return getSharedPreferences("kuro-discord-rpc563",MODE_PRIVATE).getBoolean("enabled",false);}
 private boolean discordVisible563(){return activityResumed||isInPictureInPictureMode()||pipRequest560;}
 // Frame-local progress observations are opt-in and contain only bounded timing/state.
 // No native commands, URLs, cookies, account data or video bytes are sent to frames.
 private boolean observePresenceFrames565(){return discordRpc563!=null&&discordEnabled563()&&discordVisible563()&&player!=null&&playerLayer!=null&&nativePlayer561==null&&!playerHls;}
 private void updatePresenceFrames565(){
  boolean on=observePresenceFrames565();
  for(PresenceFrame565 f:new ArrayList<>(presenceFrames565)){if(!on)f.sample=null;try{f.reply.postMessage(on?"on":"off");}catch(RuntimeException ignored){}}
 }
 private void clearPresenceFrames565(){
  for(PresenceFrame565 f:new ArrayList<>(presenceFrames565))try{f.reply.postMessage("off");}catch(RuntimeException ignored){}
  presenceFrames565.clear();
  if(presenceFrameScript565!=null)try{presenceFrameScript565.remove();}catch(RuntimeException|LinkageError ignored){}
  presenceFrameScript565=null;
 }
 private void setupPresenceFrames565(WebView view){
  presenceFrameMode565=false;clearPresenceFrames565();boolean added=false;
  try{
   if(!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)||!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))return;
   WebViewCompat.addWebMessageListener(view,"KuroPresenceFrame565",FX_ORIGINS,(sender,message,origin,isMainFrame,reply)->{
    if(message.getType()==WebMessageCompat.TYPE_STRING)onPresenceFrame565(sender,origin,reply,message.getData());
   });added=true;presenceFrameMode565=true;
  }catch(RuntimeException|LinkageError e){if(added)try{WebViewCompat.removeWebMessageListener(view,"KuroPresenceFrame565");}catch(RuntimeException|LinkageError ignored){}presenceFrameMode565=false;}
 }
 private void preparePresenceFrame565(){
  clearPresenceFrames565();if(!presenceFrameMode565||player==null||playerHls)return;
  try{presenceFrameScript565=WebViewCompat.addDocumentStartJavaScript(player,"window.__kuroPresenceAttempt565="+playerAttempt+";\n"+assetText("presence-frame565.js"),FX_ORIGINS);}
  catch(IOException|RuntimeException|LinkageError ignored){presenceFrameScript565=null;presenceFrameMode565=false;}
 }
 void onPresenceFrame565(WebView sender,Uri origin,JavaScriptReplyProxy reply,String data){
  if(sender!=player||reply==null||!fxOriginAllowed(origin)||playerAttempt<1||data==null||data.length()>400)return;
  PresenceFrame565 found=null;for(PresenceFrame565 f:presenceFrames565)if(f.reply==reply&&f.attempt==playerAttempt){found=f;break;}
  if(data.equals("ready:"+playerAttempt)){
   if(found==null&&presenceFrames565.size()<16){found=new PresenceFrame565(reply,playerAttempt);presenceFrames565.add(found);}
   try{reply.postMessage(found!=null&&observePresenceFrames565()?"on":"off");}catch(RuntimeException ignored){}return;
  }
  if(found==null)return;
  if(data.equals("bye:"+playerAttempt)){presenceFrames565.remove(found);return;}
  if(!observePresenceFrames565()){found.sample=null;return;}
  long now=SystemClock.elapsedRealtime();if(found.lastReceive>=0&&now>=found.lastReceive&&now-found.lastReceive<75)return;found.lastReceive=now;
  KuroProgress565.Sample next=KuroProgress565.parse(data,playerAttempt,now),previous=found.sample;found.sample=next;
  if((next==null)!=(previous==null)||next!=null&&previous!=null&&(!next.state.equals(previous.state)||Math.abs(next.position-previous.positionAt(now))>5000||Math.abs(next.rate-previous.rate)>.01))discordRefresh563();
 }
 private static final class PresenceSelection565 {int count;KuroProgress565.Sample sample;}
 private PresenceSelection565 presenceSelection565(){
  PresenceSelection565 selected=new PresenceSelection565();long now=SystemClock.elapsedRealtime();
  for(PresenceFrame565 f:presenceFrames565)if(f.attempt==playerAttempt&&f.sample!=null&&f.sample.fresh(now)){selected.count++;selected.sample=f.sample;}
  return selected;
 }
 private void discordSampleTop565(WebView target,JSONObject info,JSONObject option,boolean hls){
  final long attempt=playerAttempt;final String session=watchSession,loaded=target.getUrl();
  Uri u=loaded==null?null:Uri.parse(loaded);
  if(u==null||!"https".equals(u.getScheme())||u.getUserInfo()!=null||u.getPort()!=-1||!playerHost(u.getHost())){discordPublish563(info,option,"selected",-1,-1,1);return;}
  String js="(function(){try{var a=document.querySelectorAll('video');if(a.length!==1)return {s:'selected',p:-1,d:-1,r:1};var v=a[0],b=v.getBoundingClientRect(),q=getComputedStyle(v),d=v.duration,p=v.currentTime,r=v.playbackRate;if(!Number.isFinite(d)||d<"+(hls?"5":"60")+"||d>86400||!Number.isFinite(p)||p<0||p>d||!Number.isFinite(r)||r<.25||r>4||b.width<80||b.height<45||q.display==='none'||q.visibility==='hidden'||Number(q.opacity)===0)return {s:'selected',p:-1,d:-1,r:1};return {s:v.ended?'ended':v.paused?'paused':v.seeking||v.readyState<3?'buffering':'playing',p:Math.round(p*1000),d:Math.round(d*1000),r:r}}catch(e){return {s:'selected',p:-1,d:-1,r:1}}})()";
  target.evaluateJavascript(js,result->{
   if(target!=player||attempt!=playerAttempt||!session.equals(watchSession)||nativePlayer561!=null||hls!=playerHls||!loaded.equals(target.getUrl())||!discordEnabled563()||!discordVisible563())return;
   if(!hls){PresenceSelection565 selection=presenceSelection565();if(selection.count>1){discordPublish563(info,option,"selected",-1,-1,1);return;}if(selection.count==1){KuroProgress565.Sample x=selection.sample;discordPublish563(info,option,x.state,x.positionAt(SystemClock.elapsedRealtime()),x.duration,x.rate);return;}}
   try{JSONObject v=new JSONObject(result);discordPublish563(info,option,v.optString("s","selected"),v.optLong("p",-1),v.optLong("d",-1),v.optDouble("r",1));}catch(Exception ignored){discordPublish563(info,option,"selected",-1,-1,1);}
  });
 }
 private void discordNotificationSetting564(){
  if(discordPrefs564==null||!"on".equals(discordPrefs564.get("notification"))){discordNotificationCancel564();return;}
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
   if(shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)||!getSharedPreferences("kuro-discord-rpc563",MODE_PRIVATE).getBoolean("notification-asked",false)){
    getSharedPreferences("kuro-discord-rpc563",MODE_PRIVATE).edit().putBoolean("notification-asked",true).apply();requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},RPC_NOTIFICATION_PERMISSION564);
   }else Toast.makeText(this,"Enable Kuro notifications in Android settings to show playback.",Toast.LENGTH_LONG).show();
  }else discordRefresh563();
 }
 private void discordNotificationUpdate564(KuroPresence563.Snapshot snapshot,String playback,long pos,long duration,JSONObject info){
  if(discordPrefs564==null||!"on".equals(discordPrefs564.get("notification"))||!discordEnabled563())return;
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
  boolean controlled=nativePlayer561!=null||player!=null&&playerHls&&player.getUrl()!=null&&player.getUrl().startsWith("https://kuro.local/stream-player.html?");
  JSONArray eps=info.optJSONArray("episodes");int idx=info.optInt("current",-1);boolean next=ui!=null&&eps!=null&&idx>=0&&idx+1<eps.length();
  if(discordNotification564==null)discordNotification564=new KuroPresenceNotification564(this,a->main.post(()->discordNotificationCommand564(a)));
  discordNotification564.update(snapshot,playback,pos,duration,controlled,next);
 }
 private void discordNotificationCancel564(){if(discordNotification564!=null){discordNotification564.close();discordNotification564=null;}}
 private void discordNotificationCommand564(String action){
  if("stop".equals(action)){getSharedPreferences("kuro-discord-rpc563",MODE_PRIVATE).edit().putBoolean("enabled",false).apply();discordEnabledChanged563(false);return;}
  if(!discordEnabled563()||watchInfo561==null||playerLayer==null)return;
  if("next".equals(action)){JSONArray eps=watchInfo561.optJSONArray("episodes");int idx=watchInfo561.optInt("current",-1);if(eps!=null&&idx>=0&&idx+1<eps.length()&&ui!=null){int next=idx+1;closePlayer();ui.evaluateJavascript("window.kuroChooseEpisode&&window.kuroChooseEpisode("+next+")",null);}return;}
  boolean play="play".equals(action);if(!play&&!"pause".equals(action))return;
  if(nativePlayer561!=null)nativePlayer561.command564(play);
  else if(player!=null&&playerHls&&player.getUrl()!=null&&player.getUrl().startsWith("https://kuro.local/stream-player.html?"))player.evaluateJavascript("(function(){var v=document.querySelectorAll('video');if(v.length!==1)return;try{"+(play?"var p=v[0].play();if(p&&p.catch)p.catch(function(){})":"v[0].pause()")+"}catch(e){}})()",null);
  discordRefresh563();
 }
 private void handleDiscordOAuth564(Intent intent){
  KuroDiscordOAuth564.Result r=KuroDiscordOAuth564.consume(this,intent);if(r==null)return;
  if(!r.error.isEmpty()){Toast.makeText(this,r.error,Toast.LENGTH_LONG).show();return;}
  if(!discordEnabled563()){Toast.makeText(this,"Turn on Discord Rich Presence, then link again.",Toast.LENGTH_LONG).show();return;}
  if(discordRpc563!=null){discordRpc563.start();discordRpc563.exchange(r.code,r.verifier);Toast.makeText(this,"Connecting the linked Discord account…",Toast.LENGTH_LONG).show();}
 }
 private void discordAction564(String action){
  if(discordRpc563==null){Toast.makeText(this,"Discord Rich Presence is not available yet.",Toast.LENGTH_SHORT).show();return;}
  if("clear".equals(action)){getSharedPreferences("kuro-discord-rpc563",MODE_PRIVATE).edit().putBoolean("enabled",false).apply();discordEnabledChanged563(false);Toast.makeText(this,"Presence turned off; clearing was requested for both accounts.",Toast.LENGTH_LONG).show();return;}
  if("unlink".equals(action)){new AlertDialog.Builder(KuroDialogs581.themed(this)).setTitle("Disconnect linked Discord?").setMessage("Remove the linked authorization from this phone. The signed-in Discord app account is kept. To revoke permission completely, use Discord's Authorized Apps settings.").setNegativeButton("Cancel",null).setPositiveButton("Disconnect",(d,w)->{discordRpc563.unlink();Toast.makeText(this,"Linked account disconnected on this phone.",Toast.LENGTH_SHORT).show();}).show();return;}
  if(!discordEnabled563()){Toast.makeText(this,"Turn on Discord Rich Presence first.",Toast.LENGTH_SHORT).show();return;}
  if("current".equals(action)&&discordPrefs564!=null&&"off".equals(discordPrefs564.get("current"))){Toast.makeText(this,"Turn on current account sharing in Presence customization first.",Toast.LENGTH_LONG).show();return;}
  if("linked".equals(action)&&discordPrefs564!=null&&"off".equals(discordPrefs564.get("linked"))){Toast.makeText(this,"Turn on linked account sharing in Presence customization first.",Toast.LENGTH_LONG).show();return;}
  if("linked".equals(action)&&!KuroDiscordVault564.hasStored(this,"refresh")){Toast.makeText(this,"Link another Discord account first, then try reconnecting.",Toast.LENGTH_LONG).show();return;}
  if(!"link".equals(action)&&!discordRpc563.isActive()){if(discordVisible563())discordForeground563();if(!discordRpc563.isActive()){Toast.makeText(this,"Discord SDK is not ready. Check Connection status and try again.",Toast.LENGTH_LONG).show();return;}}
  if("current".equals(action)){discordRpc563.reconnectCurrent();discordRefresh563();Toast.makeText(this,"Reconnecting current Discord…",Toast.LENGTH_SHORT).show();}
  else if("linked".equals(action)){discordRpc563.reconnectLinked();discordRefresh563();Toast.makeText(this,"Reconnecting linked Discord…",Toast.LENGTH_SHORT).show();}
  else if("test".equals(action)){boolean requested=discordRpc563.test();Toast.makeText(this,requested?"A 15-second test was requested. Check Connection status for the result.":"Test unavailable. Enable an account target and check Connection status.",Toast.LENGTH_LONG).show();}
  else if("link".equals(action))new AlertDialog.Builder(KuroDialogs581.themed(this)).setTitle("Link another Discord account").setMessage("Discord opens in your browser. Sign in there as the account you want to link, then check the account on the consent screen. Kuro stores the refresh credential encrypted on this phone; it is never included in Kuro backups. This requires the Kuro Discord application to be a Public Client with the mobile redirect registered. The current Discord app account remains separate.").setNegativeButton("Cancel",null).setPositiveButton("Continue",(d,w)->{if(!KuroDiscordOAuth564.begin(this))Toast.makeText(this,"Could not open secure Discord authorization on this phone.",Toast.LENGTH_LONG).show();}).show();
 }
 private void discordEnabledChanged563(boolean enabled){
  if(discordRpc563==null)return;updatePresenceFrames565();if(!enabled){main.removeCallbacks(discordPoll563);main.removeCallbacks(discordStop563);discordRpc563.stop();discordNotificationCancel564();discordPause564.reset();discordPauseCleared564=false;discordSessionStart563=0;return;}
  if(discordVisible563())discordForeground563();
 }
 private final Runnable discordStop563=()->{if(!activityResumed&&!isInPictureInPictureMode()&&!pipRequest560&&discordRpc563!=null){discordRpc563.stop();discordNotificationCancel564();discordPause564.reset();discordPauseCleared564=false;discordSessionStart563=0;}};
 private void discordBackground563(){
  updatePresenceFrames565();main.removeCallbacks(discordStop563);
  String away=discordPrefs564.get("away");
  if("keep".equals(away))return; // The poll keeps running while the user asked for it.
  main.removeCallbacks(discordPoll563);
  main.postDelayed(discordStop563,"min1".equals(away)?60000L:"min5".equals(away)?300000L:12000L);
 }
 private void discordForeground563(){
  if(discordRpc563==null||!discordEnabled563()||!discordVisible563())return;
  main.removeCallbacks(discordStop563);if(discordSessionStart563==0)discordSessionStart563=System.currentTimeMillis();discordRpc563.start();updatePresenceFrames565();discordRefresh563();
 }
 private void discordClose563(){main.removeCallbacks(discordPoll563);main.removeCallbacks(discordStop563);if(discordRpc563!=null){discordRpc563.close();discordRpc563=null;}discordNotificationCancel564();discordPause564.reset();discordPauseCleared564=false;discordSessionStart563=0;}
 private void discordRefresh563(){if(discordRpc563==null||!discordEnabled563()||!discordVisible563())return;main.removeCallbacks(discordPoll563);main.post(discordPoll563);}
 private void discordPublish563(JSONObject info,JSONObject option,String playback,long pos,long duration,double rate){
  if(discordRpc563==null||!discordEnabled563()||!discordVisible563()||info==null)return;
  KuroPresence564.Options o=discordPrefs564.options();o.rewatch=rewatch563;
  if(discordPause564.shouldClear(playback,o.pause,SystemClock.elapsedRealtime())){if(!discordPauseCleared564){discordPauseCleared564=true;discordRpc563.clearForPause();discordNotificationCancel564();}return;}
  discordPauseCleared564=false;
  JSONObject anime=info.optJSONObject("animeInfo");String suppliedCover=anime==null?"":anime.optString("image","");String image="",lang=option==null?"":option.optString("lang","");
  // Only anime cover artwork is used, never episodeImage. Search neutral
  // metadata only when presence, title and poster sharing are on. The callback
  // re-samples current playback; it never publishes stale metadata.
  if(o.title&&o.poster&&discordArtwork567!=null)image=discordArtwork567.resolve(info.optString("title",""),suppliedCover,()->main.post(this::discordRefresh563));
  String total=discordReportedTotal564(anime);
  String server=discordServerLabel564(info,option,lang);
  KuroPresence563.Snapshot snapshot=KuroPresence564.watching(o,info.optString("title","Anime"),info.optString("episode",""),info.optString("episodeName",""),lang,image,"","",server,total,playback,pos,duration,System.currentTimeMillis(),rate);
  discordRpc563.publish(snapshot);discordNotificationUpdate564(snapshot,playback,pos,duration,info);
 }
 static String discordReportedTotal564(JSONObject anime){
  JSONObject counts=anime==null?null:anime.optJSONObject("counts");String origin=anime==null?"":anime.optString("countsSource","");
  return counts!=null&&Arrays.asList("ms","dk","flix","aw","ani","miruro").contains(origin)?counts.optString("total",""):"";
 }
 static String discordServerLabel564(JSONObject info,JSONObject option,String lang){
  if(info==null||option==null||lang==null)return "";JSONArray servers=info.optJSONArray("servers");int number=0;boolean found=false;String url=option.optString("url","");
  if(servers!=null)for(int i=0;i<servers.length();i++){JSONObject s=servers.optJSONObject(i);if(s==null)continue;if(lang.equalsIgnoreCase(s.optString("lang","")))number++;if(s==option||!url.isEmpty()&&url.equals(s.optString("url"))&&lang.equalsIgnoreCase(s.optString("lang",""))){found=true;break;}}
  String n=option.optString("note","").trim();if(!n.matches("[A-Za-z0-9][A-Za-z0-9 _.-]{0,23}"))n="";
  return found&&number>0?("Server "+number+(n.isEmpty()?"":" "+n)):n;
 }
 private final Runnable discordPoll563=new Runnable(){@Override public void run(){
  if(discordRpc563==null||!discordEnabled563())return;
  if(!discordVisible563()&&!"keep".equals(discordPrefs564.get("away")))return;
  JSONObject info=watchInfo561,option=serverOption561;
  if(info==null||playerLayer==null){discordPause564.reset();discordPauseCleared564=false;discordRpc563.publish(KuroPresence564.browsing(discordSessionStart563,discordPrefs564.options()));discordNotificationCancel564();}
  else if(nativePlayer561!=null)discordPublish563(info,option,nativePlayer561.presenceState563(),nativePlayer561.position(),nativePlayer561.duration(),nativePlayer561.speed565());
  else if(player!=null&&playerHls&&player.getUrl()!=null&&player.getUrl().startsWith("https://kuro.local/stream-player.html?"))discordSampleTop565(player,info,option,true);
  else if(player!=null&&!playerHls){
   PresenceSelection565 selection=presenceSelection565();
   if(selection.count==1){KuroProgress565.Sample sample=selection.sample;discordPublish563(info,option,sample.state,sample.positionAt(SystemClock.elapsedRealtime()),sample.duration,sample.rate);}
   else if(selection.count>1)discordPublish563(info,option,"selected",-1,-1,1);
   else discordSampleTop565(player,info,option,false);
  }else discordPublish563(info,option,"selected",-1,-1,1); // Cross-origin embeds cannot reliably report state unless a known frame exposes it.
  main.postDelayed(this,3000);
 }};
 void openWatch(String data){try{
  JSONObject info=new JSONObject(data);JSONArray options=info.getJSONArray("servers"), eps=info.getJSONArray("episodes");int current=info.optInt("current",0),chosen=info.optInt("index",0);String ref=info.optString("source"),session=info.optString("session","");if(!session.matches("[A-Za-z0-9_-]{1,40}"))throw new Exception("Invalid watch session");if(options.length()==0||options.length()>50||eps.length()>5000||chosen<0||chosen>=options.length()||current<0||current>=eps.length())throw new Exception("Invalid watch data");
  if(playerLayer!=null)closePlayer();lastPlayerIssue573="No player error detected in this session.";watchSession=session;watchInfo561=info;resumeKey561=KuroPlayback561.key(ref,info.optString("showIdentity",""),info.optString("episode",""));rewatch563=KuroPlayback561.read(this,resumeKey561)>0;JSONObject effects561=PlayerOptionsDialog.readConfig(this);webPreferred561=effects561.optBoolean("enabled",false)||effects561.optInt("gain",100)!=100;playerLayer=new LinearLayout(this);playerLayer.setOrientation(LinearLayout.VERTICAL);playerLayer.setBackgroundColor(0xff110917);
  WatchHeader bar=new WatchHeader(this,new WatchHeader.Listener(){
   @Override public void home(){navigateFromWatch("home");}
   @Override public void search(){showWatchOverlay("search");}
   @Override public void notifications(){showWatchOverlay("news");}
   @Override public void episodes(){openEpisodeDrawer581();}
   @Override public void login(){showWatchOverlay("account");}
  });watchHeader=bar;bar.setProfile(profileSignedIn560,profileAvatar560,profileArt581);playerLayer.addView(bar,new LinearLayout.LayoutParams(-1,dp(62)));
  final Runnable[] retryServer={null};final View[] connectionCover={null};final TextView[] connectionText={null};final boolean[] verification={false},completionReported={false};
  player=new WebView(this);player.setOnTouchListener((view,event)->{if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN)showFullscreenExit569();return false;});player.setBackgroundColor(Color.BLACK);WebSettings p=player.getSettings();p.setJavaScriptEnabled(true);p.setDomStorageEnabled(true);p.setAllowFileAccess(false);p.setAllowContentAccess(false);p.setSupportMultipleWindows(false);p.setJavaScriptCanOpenWindowsAutomatically(false);p.setMediaPlaybackRequiresUserGesture(!getPreferences(MODE_PRIVATE).getBoolean("autoplay",true));p.setTextZoom(settings560().getInt("textZoom",100));boolean desktop560="desktop".equals(settings560().getString("siteView","mobile"));if(desktop560)p.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36");p.setUseWideViewPort(desktop560);p.setLoadWithOverviewMode(desktop560);android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(player,settings560().getBoolean("thirdCookies",false));setupSkipFrames(player);setupAdFrames(player);setupFxStatus(player);setupFxFrames(player);setupCompletionFrames(player,session,info.optString("episode",""),verification,completionReported);setupProgress561(player);setupPresenceFrames565(player);
  final long[] failedAttempt={-1};java.util.function.Consumer<WebView> reportFailure=v->{long attempt=playerAttempt;if(v!=player||verification[0]||failedAttempt[0]==attempt)return;failedAttempt[0]=attempt;lastPlayerIssue573="The Web player reported a playback or connection failure.";notifyPlayerError560();v.stopLoading();if(connectionCover[0]!=null){connectionText[0].setText("Connecting to another source…");connectionCover[0].setVisibility(View.VISIBLE);}v.loadUrl("about:blank");main.post(()->{if(v==player&&attempt==playerAttempt&&!verification[0]&&retryServer[0]!=null)retryServer[0].run();});};
  nativeFailure561=blocked->{if(player==null||playerLayer==null)return;lastPlayerIssue573=blocked?"The stream reported restricted access or verification.":"Native playback failed; Kuro tried its Web player.";if(blocked){verification[0]=true;lastPlayerIssue573="The player reported restricted access or verification.";if(connectionCover[0]!=null){connectionText[0].setText("Verification or access permission is required. Kuro stopped here.");connectionCover[0].setVisibility(View.VISIBLE);}Toast.makeText(MainActivity.this,"This stream requires verification or has blocked access.",Toast.LENGTH_LONG).show();return;}if(!directAddress561.isEmpty()){webPreferred561=true;Toast.makeText(MainActivity.this,"Native playback failed; trying the existing Web player.",Toast.LENGTH_SHORT).show();loadPlayer(directAddress561,directReferer561,directKind561);}};
  nativeEnded561=()->{if(verification[0]||nativePlayer561==null)return;updatePipActions571();long duration=nativePlayer561.duration(),position=nativePlayer561.position();if(duration<180000||position<duration*.98)return;if(!completionReported[0]&&ui!=null){completionReported[0]=true;ui.evaluateJavascript("window.kuroEpisodeEnded553&&window.kuroEpisodeEnded553("+JSONObject.quote(session)+","+JSONObject.quote(info.optString("episode",""))+")",null);}if(getPreferences(MODE_PRIVATE).getBoolean("auto_next",false)&&current+1<eps.length())offerAutoNext573(session,current+1);};
  setupFailureFrames(player,message->{String kind=FrameReport.currentEvent(message,playerAttempt);if(kind.isEmpty()||verification[0])return;if("verification".equals(kind)){verification[0]=true;lastPlayerIssue573="The player reported restricted access or verification.";Toast.makeText(MainActivity.this,"This player requires verification or has blocked access.",Toast.LENGTH_LONG).show();}else reportFailure.accept(player);});
  player.setWebViewClient(new WebViewClient(){
   private void failed(WebView v){reportFailure.accept(v);}
   @Override public void onReceivedError(WebView v,WebResourceRequest request,WebResourceError error){super.onReceivedError(v,request,error);int code=error.getErrorCode();if(request.isForMainFrame()&&request.getUrl().toString().equals(v.getUrl())&&(code==ERROR_CONNECT||code==ERROR_TIMEOUT||code==ERROR_HOST_LOOKUP||code==ERROR_IO))failed(v);}
   @Override public void onReceivedHttpError(WebView v,WebResourceRequest request,WebResourceResponse response){super.onReceivedHttpError(v,request,response);if(v!=player)return;int status=response.getStatusCode();if(!request.isForMainFrame()){Uri u=request.getUrl();if((status==401||status==403||status==429)&&"megaplay.buzz".equals(u.getHost())&&u.getPath()!=null&&u.getPath().startsWith("/stream/")&&!verification[0]){verification[0]=true;lastPlayerIssue573="The player reported restricted access or verification.";Toast.makeText(MainActivity.this,"This player requires verification or has blocked access.",Toast.LENGTH_LONG).show();}return;}if(!request.getUrl().toString().equals(v.getUrl()))return;if(status==401||status==403||status==429){verification[0]=true;lastPlayerIssue573="The player reported restricted access or verification.";Toast.makeText(MainActivity.this,"This player requires verification or has blocked access.",Toast.LENGTH_LONG).show();}else if(status>=400)failed(v);}
   @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest request){Uri u=request.getUrl();if("https".equals(u.getScheme())&&"kuro.local".equals(u.getHost())&&("/stream-player.html".equals(u.getPath())||"/stream-player.js".equals(u.getPath())||"/video-effects537.js".equals(u.getPath())||"/ai-upscale540.js".equals(u.getPath())||"/hls-1.6.16.min.js".equals(u.getPath()))){try{return new WebResourceResponse(u.getPath().endsWith(".html")?"text/html":"application/javascript","UTF-8",getAssets().open(u.getPath().substring(1)));}catch(IOException ignored){}}
    String scheme=u.getScheme(),host=u.getHost();boolean playerOrigin=host!=null&&playerHost(host.toLowerCase(Locale.ROOT));
    if(settings560().getBoolean("adBlock",true)&&!request.isForMainFrame()&&("https".equalsIgnoreCase(scheme)||"http".equalsIgnoreCase(scheme))&&!playerOrigin&&adBlocker().blocked(scheme,host,u.getPath(),false,false))return new WebResourceResponse("text/plain","UTF-8",204,"No Content",Collections.singletonMap("Cache-Control","no-store"),new ByteArrayInputStream(new byte[0]));
    return null;}
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){Uri n=r.getUrl();if("kuro-player".equals(n.getScheme())){String current=v.getUrl();if(v==player&&playerHls&&current!=null&&current.startsWith("https://kuro.local/stream-player.html")){if("verification".equals(n.getQueryParameter("type"))){verification[0]=true;lastPlayerIssue573="The player reported restricted access or verification.";Toast.makeText(MainActivity.this,"This stream requires verification or has blocked access.",Toast.LENGTH_LONG).show();}else failed(v);}return true;}return !"https".equals(n.getScheme())||!playerHost(n.getHost());}
   @Override public void onPageFinished(WebView v,String url){
    super.onPageFinished(v,url);
    try{
     Uri u=Uri.parse(url);if(v!=player||!"https".equals(u.getScheme())||!playerHost(u.getHost()))return;
     if(playerHls&&url.startsWith("https://kuro.local/stream-player.html?"))updateWebMarkers574();
     v.evaluateJavascript("window.__kuroAutoplay="+(getPreferences(MODE_PRIVATE).getBoolean("autoplay",true)?"true":"false")+";",null);
     long attempt=playerAttempt;v.evaluateJavascript("(function(){var t=(document.title||'').trim();return /^(just a moment|attention required|verify you are human|access denied)/i.test(t)||!!document.querySelector('#challenge-form,#challenge-stage,.cf-challenge')})()",result->{if(v==player&&attempt==playerAttempt&&"true".equals(result)){verification[0]=true;lastPlayerIssue573="The player reported restricted access or verification.";}});
     if(completionFrameMode&&!completionStartMode)v.evaluateJavascript(assetText("player-ended553.js"),null);
     if(!adFrameMode&&settings560().getBoolean("adBlock",true))v.evaluateJavascript(assetText("player-ad-guard541.js"),null);
     v.evaluateJavascript(assetText("player-skin.js"),null);
     String aiScript=assetText("ai-upscale540.js"),fxScript=assetText("video-effects537.js");
     v.evaluateJavascript(aiScript,aiIgnored->{if(v==player&&attempt==playerAttempt)v.evaluateJavascript(fxScript,ignored->{if(v==player&&attempt==playerAttempt)applyPlayerOptions(null);});});
     // Two bounded, top-frame checks for an explicit player error. Cross-origin
     // most cross-origin frames and silent stalls remain undetectable; never guess based on delay.
     final String loaded=url;Runnable inspect=()->{
      if(v!=player||attempt!=playerAttempt||verification[0]||!loaded.equals(v.getUrl()))return;
      String script="(function(){try{var vs=document.querySelectorAll('video');if(vs.length===1&&vs[0].error&&vs[0].error.code)return true;var a=document.querySelectorAll('.jw-error .jw-error-msg,.jw-error .jw-error-text,.vjs-error-display .vjs-modal-dialog-content,.player-error,.video-error');return Array.prototype.some.call(a,function(e){var c=getComputedStyle(e),t=(e.textContent||'').trim();return e.getClientRects().length&&c.visibility!=='hidden'&&c.display!=='none'&&/file not found|video not found|error loading media|no playable sources|source not supported|cannot be played/i.test(t)})}catch(e){return false}})()";
      v.evaluateJavascript(script,result->{if(v==player&&attempt==playerAttempt&&!verification[0]&&loaded.equals(v.getUrl())&&"true".equals(result))failed(v);});
     };main.postDelayed(inspect,4000);main.postDelayed(inspect,12000);
     if(!skipFrameMode)v.evaluateJavascript("window.__kuroFrameMode=false;window.__kuroSkip="+(shouldScanSkips()?"true":"false")+";\n"+assetText("auto-skip.js"),null);
    }catch(Exception ignored){}
   }
  });
  player.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onCreateWindow(WebView v,boolean dialog,boolean userGesture,Message resultMsg){return false;}
   @Override public void onShowCustomView(View v,CustomViewCallback cb){
    if(customView!=null){cb.onCustomViewHidden();return;}
    if(playerOptions!=null&&playerOptions.isShowing()){playerOptions.dismiss();playerOptions=null;}
    // Android's separate video-only custom view can discard the HTML canvas.
    // When frames are available, retain the same WebView and resize it instead.
    if(preferOverlayFullscreen569()){
     cb.onCustomViewHidden();enterEnhancedFullscreen();return;
    }
    customView=v;customCallback=cb;updateKeepScreen560();full=new FrameLayout(MainActivity.this);full.setBackgroundColor(Color.BLACK);full.addView(v);root.addView(full,new FrameLayout.LayoutParams(-1,-1));getWindow().getDecorView().setSystemUiVisibility(5894);applyPlayerOptions(null);
    if(PlayerOptionsDialog.readConfig(MainActivity.this).optBoolean("enabled",false))Toast.makeText(MainActivity.this,"Enhancement is unavailable for this player; using its normal fullscreen.",Toast.LENGTH_LONG).show();
   }
   @Override public void onHideCustomView(){hideFullscreen();}
  });
  FrameLayout playerBox=new FrameLayout(this){@Override protected void onMeasure(int w,int h){if(enhancedFullscreen){super.onMeasure(w,h);return;}int width=View.MeasureSpec.getSize(w);super.onMeasure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(width*9/16,View.MeasureSpec.EXACTLY));}};watchPlayerBox=playerBox;playerBox.setBackgroundColor(Color.BLACK);playerBox.addView(player,new FrameLayout.LayoutParams(-1,-1));
  LinearLayout cover=new LinearLayout(this);cover.setOrientation(LinearLayout.VERTICAL);cover.setGravity(Gravity.CENTER);cover.setBackgroundColor(Color.BLACK);TextView coverLabel=text("Connecting to another source…",13,0xffd7c7e4);coverLabel.setGravity(Gravity.CENTER);cover.addView(coverLabel);cover.setVisibility(View.GONE);connectionCover[0]=cover;connectionText[0]=coverLabel;playerBox.addView(cover,new FrameLayout.LayoutParams(-1,-1));playerLayer.addView(playerBox,new LinearLayout.LayoutParams(-1,-2));
  // Keep the video in place while the richer control panel and details scroll together.
  ScrollView scroll=new ScrollView(this);watchScroll=scroll;scroll.setFillViewport(false);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(18),dp(20),dp(18),dp(30));scroll.addView(content);playerLayer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  final boolean canNext=current+1<eps.length();JSONObject following=canNext?eps.optJSONObject(current+1):null;
  String followingNumber=canNext?(following!=null?following.optString("number",""):eps.optString(current+1,"")):"";
  PlaybackPanel controls=new PlaybackPanel(this,info.optString("episode",""),followingNumber,canNext,
   getPreferences(MODE_PRIVATE).getBoolean("autoplay",true),getPreferences(MODE_PRIVATE).getBoolean("auto_next",false),getPreferences(MODE_PRIVATE).getBoolean("auto_skip",false),new PlaybackPanel.Listener(){
    @Override public void nextEpisode(){int next=current+1;if(next>=eps.length())return;closePlayer();ui.evaluateJavascript("window.kuroChooseEpisode&&window.kuroChooseEpisode("+next+")",null);}
    @Override public void autoplayChanged(boolean on){getPreferences(MODE_PRIVATE).edit().putBoolean("autoplay",on).apply();if(nativePlayer561!=null)nativePlayer561.setAutoplay(on);if(player!=null){player.getSettings().setMediaPlaybackRequiresUserGesture(!on);player.evaluateJavascript("window.__kuroAutoplay="+(on?"true":"false")+";if(window.__kuroSetAutoplay561)window.__kuroSetAutoplay561("+(on?"true":"false")+");"+(on?"document.querySelectorAll('video').forEach(function(v){if(v.paused){try{var p=v.play();if(p&&p.catch)p.catch(function(){})}catch(e){}}});":""),null);}Toast.makeText(MainActivity.this,on?"Autoplay requested; some players require a tap.":"Autoplay off for new videos.",Toast.LENGTH_SHORT).show();}
    @Override public void autoNextChanged(boolean on){getPreferences(MODE_PRIVATE).edit().putBoolean("auto_next",on).apply();Toast.makeText(MainActivity.this,on?"Auto next shows a cancellable countdown when a supported player reports an ending.":"Auto next off.",Toast.LENGTH_SHORT).show();}
    @Override public void autoSkipChanged(boolean on){getPreferences(MODE_PRIVATE).edit().putBoolean("auto_skip",on).apply();updateSkipFrames();Toast.makeText(MainActivity.this,nativePlayer561!=null?(on?"Auto skip uses available or saved markers.":"Auto skip off."):on?(skipFrameMode?"Auto skip will try visible Skip controls.":"Auto skip is limited on this WebView; try updating Android System WebView."):"Auto skip off.",Toast.LENGTH_SHORT).show();}
   });
  content.addView(controls,new LinearLayout.LayoutParams(-1,-2));addGap(content,12);addDirectTools561(content);
  TextView diagnostics573=chip("Playback help & diagnostics",false);diagnostics573.setTag("player-diagnostics-573");diagnostics573.setContentDescription("Playback help and diagnostics");diagnostics573.setMinHeight(dp(48));
  diagnostics573.setOnClickListener(v->{
   if(playerLayer==null||!session.equals(watchSession))return;
   String mode=nativePlayer561!=null?"Kuro native player":playerHls?"Kuro Web stream player":"Embedded Web player";
   String connection="Network status unavailable.";try{android.net.ConnectivityManager cm=(android.net.ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);android.net.NetworkCapabilities caps=cm==null?null:cm.getNetworkCapabilities(cm.getActiveNetwork());connection=caps!=null&&caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)?"Android reports an internet-capable network; the source may still be unavailable.":"Android does not report an internet-capable network.";}catch(RuntimeException ignored){}
   ScrollView scroll573=new ScrollView(this);scroll573.setFillViewport(false);scroll573.setTag("diagnostics-dialog-573");scroll573.setBackground(diagnosticsShape573(0xff190f21,0xff624475,18));
   LinearLayout panel573=new LinearLayout(this);panel573.setOrientation(LinearLayout.VERTICAL);panel573.setPadding(dp(22),dp(22),dp(22),dp(20));scroll573.addView(panel573);
   TextView eyebrow573=text("KURO.TV  ·  PLAYER SUPPORT",11,0xffbf93df);eyebrow573.setLetterSpacing(.08f);panel573.addView(eyebrow573);addGap(panel573,7);
   TextView title573=text("Playback diagnostics",22,0xfff8f1ff);title573.setTypeface(null,android.graphics.Typeface.BOLD);if(Build.VERSION.SDK_INT>=28)title573.setAccessibilityHeading(true);panel573.addView(title573);addGap(panel573,8);
   TextView intro573=text("Quick checks for your current playback session.",13,0xffcbbbd6);intro573.setLineSpacing(dp(3),1f);panel573.addView(intro573);addGap(panel573,18);
   diagnosticsCard573(panel573,"PLAYER",mode,"diagnostics-player-573");diagnosticsCard573(panel573,"NETWORK",connection,"diagnostics-network-573");diagnosticsCard573(panel573,"LAST REPORTED ISSUE",lastPlayerIssue573,"diagnostics-issue-573");
   TextView guidance573=text("Some embedded-player failures cannot be detected. Check your connection or choose another listed server. Kuro cannot bypass verification or access restrictions. No URLs, account details, or cookies are shown.",13,0xffcbbbd6);guidance573.setLineSpacing(dp(3),1f);guidance573.setTag("diagnostics-guidance-573");panel573.addView(guidance573);addGap(panel573,18);
   AlertDialog dialog573=new AlertDialog.Builder(KuroDialogs581.themed(this)).setView(scroll573).create();
   if(!verification[0]&&retryServer[0]!=null){TextView retry573=chip("Try another server",true);retry573.setTag("diagnostics-retry-573");retry573.setMinHeight(dp(48));retry573.setOnClickListener(a->{dialog573.dismiss();if(playerLayer!=null&&session.equals(watchSession)&&!verification[0]&&retryServer[0]!=null)retryServer[0].run();});panel573.addView(retry573,new LinearLayout.LayoutParams(-1,-2));addGap(panel573,8);}
   TextView close573=chip("Close",false);close573.setTag("diagnostics-close-573");close573.setMinHeight(dp(48));close573.setOnClickListener(a->dialog573.dismiss());panel573.addView(close573,new LinearLayout.LayoutParams(-1,-2));
   dialog573.show();if(dialog573.getWindow()!=null){dialog573.getWindow().setBackgroundDrawableResource(android.R.color.transparent);int max=dp(560),width=getResources().getDisplayMetrics().widthPixels-dp(32);dialog573.getWindow().setLayout(Math.max(dp(240),Math.min(max,width)),WindowManager.LayoutParams.WRAP_CONTENT);}

  });content.addView(diagnostics573,new LinearLayout.LayoutParams(-1,-2));addGap(content,8);
  LinearLayout fxActions=new LinearLayout(this);fxActions.setOrientation(LinearLayout.HORIZONTAL);
  TextView optionsButton=chip("⚙   Advanced",false);optionsButton.setTag("player-options");optionsButton.setContentDescription("Advanced video enhancement and volume boost");optionsButton.setMinHeight(dp(48));optionsButton.setOnClickListener(v->showPlayerOptions());fxActions.addView(optionsButton,new LinearLayout.LayoutParams(0,-2,1));
  TextView enhancedButton=chip("⛶  Enhanced fullscreen",false);enhancedButton.setTag("enhanced-fullscreen-enter");enhancedButton.setContentDescription("Enhanced fullscreen, retains supported video effects");enhancedButton.setMinHeight(dp(48));enhancedButton.setOnClickListener(v->enterEnhancedFullscreen());LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(0,-2,1);fp.setMargins(dp(8),0,0,0);fxActions.addView(enhancedButton,fp);content.addView(fxActions,new LinearLayout.LayoutParams(-1,-2));addGap(content,20);
  final WebView watchedPlayer=player;final boolean[] advanced={false};final Runnable[] completionCheck={null};completionCheck[0]=()->{if(player!=watchedPlayer||playerLayer==null||advanced[0])return;if(nativePlayer561!=null||!getPreferences(MODE_PRIVATE).getBoolean("auto_next",false)||current+1>=eps.length()){main.postDelayed(completionCheck[0],2000);return;}watchedPlayer.evaluateJavascript("(function(){try{var v=document.querySelectorAll('video');if(v.length!==1)return false;var m=v[0];return !!(m.ended&&Number.isFinite(m.duration)&&m.duration>=180&&m.currentTime>=m.duration*0.98)}catch(e){return false}})()",result->{if(player!=watchedPlayer||playerLayer==null||advanced[0])return;if("true".equals(result)&&getPreferences(MODE_PRIVATE).getBoolean("auto_next",false)){advanced[0]=true;if(!completionReported[0]&&ui!=null){completionReported[0]=true;ui.evaluateJavascript("window.kuroEpisodeEnded553&&window.kuroEpisodeEnded553("+JSONObject.quote(session)+","+JSONObject.quote(info.optString("episode",""))+")",null);}offerAutoNext573(session,current+1);}else main.postDelayed(completionCheck[0],2000);});};main.postDelayed(completionCheck[0],2000);
  TextView title=text(info.optString("title","Anime"),21,Color.WHITE);title.setTypeface(null,android.graphics.Typeface.BOLD);content.addView(title);addGap(content,7);TextView epText=text("EPISODE "+info.optString("episode","")+"   •   SELECT A SERVER BELOW",11,0xffb37ee4);content.addView(epText);addGap(content,22);TextView serverTitle=text("Servers & audio",16,Color.WHITE);serverTitle.setTypeface(null,android.graphics.Typeface.BOLD);content.addView(serverTitle);addGap(content,9);
  LinearLayout groupRow=new LinearLayout(this);groupRow.setOrientation(LinearLayout.HORIZONTAL);content.addView(groupRow);LinearLayout subCol=new LinearLayout(this),dubCol=new LinearLayout(this),extraCol=new LinearLayout(this);subCol.setOrientation(LinearLayout.VERTICAL);dubCol.setOrientation(LinearLayout.VERTICAL);extraCol.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams subParams=new LinearLayout.LayoutParams(0,-2,1);subParams.setMargins(0,0,dp(6),0);LinearLayout.LayoutParams dubParams=new LinearLayout.LayoutParams(0,-2,1);dubParams.setMargins(dp(6),0,0,0);groupRow.addView(subCol,subParams);groupRow.addView(dubCol,dubParams);TextView subTitle=text("SUB",12,0xffb37ee4),dubTitle=text("DUB",12,0xffb37ee4);subTitle.setTypeface(null,android.graphics.Typeface.BOLD);dubTitle.setTypeface(null,android.graphics.Typeface.BOLD);subCol.addView(subTitle);dubCol.addView(dubTitle);addGap(subCol,8);addGap(dubCol,8);final ArrayList<TextView> buttons=new ArrayList<>();final boolean[] tried=new boolean[50];final int[] active={chosen},countsByLang={0,0,0};final boolean[] waiting={false},lookupDone={info.optBoolean("lookupDone",false)},lookupBlocked={info.optBoolean("blocked",false)};tried[chosen]=true;
  TextView noSub=text("Not available yet",12,0xff91809f),noDub=text("Not available yet",12,0xff91809f);subCol.addView(noSub);dubCol.addView(noDub);
  java.util.function.BiConsumer<Integer,JSONObject> addButton=(pos,opt)->{
   String url=opt.optString("url"),serverRef=opt.optString("referer",ref),kind=opt.optString("type",""),lang=opt.optString("lang","").toLowerCase(Locale.ROOT),note=opt.optString("note","");
   boolean isSub=lang.equals("sub"),isDub=lang.equals("dub");LinearLayout column=isSub?subCol:isDub?dubCol:extraCol;int index=isSub?0:isDub?1:2;int num=++countsByLang[index];if(isSub)noSub.setVisibility(View.GONE);if(isDub)noDub.setVisibility(View.GONE);
   String displayName="Server "+num+(note.isEmpty()?"":" • "+(note.length()>35?note.substring(0,35):note));TextView btn=chip(displayName,pos==chosen);buttons.add(btn);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(7));column.addView(btn,lp);
   btn.setOnClickListener(v->{verification[0]=false;if(loadPlayerOption561(opt,ref)){if(connectionCover[0]!=null)connectionCover[0].setVisibility(View.GONE);Arrays.fill(tried,false);tried[pos]=true;active[0]=pos;waiting[0]=false;for(int j=0;j<buttons.size();j++){boolean on=j==pos;buttons.get(j).setTextColor(on?0xff12081b:0xffe3dce9);buttons.get(j).setBackground(rounded(on?0xffae71e7:0xff281b32,on?0:0xff4c3a59));}}});
  };
  for(int i=0;i<options.length();i++)addButton.accept(i,options.getJSONObject(i));
  retryServer[0]=()->{
   if(player==null||playerLayer==null||verification[0])return;
   try{
    if(lookupBlocked[0]){waiting[0]=false;if(connectionText[0]!=null)connectionText[0].setText("Verification is required. Check that library manually.");return;}
    String currentLang=options.getJSONObject(active[0]).optString("lang","");int next=RetrySelector.next(options,tried,currentLang,"dub".equals(settings560().getString("audio","dub"))?KuroPreferences560.preferredServers(MainActivity.this):null);
    if(next<0){waiting[0]=!lookupDone[0];if(connectionText[0]!=null)connectionText[0].setText(waiting[0]?"Connecting to another source…":"No working exact-match source was detected. Try another server.");return;}
    tried[next]=true;JSONObject target=options.getJSONObject(next);if(!loadPlayerOption561(target,ref)){main.post(retryServer[0]);return;}
    active[0]=next;waiting[0]=false;if(connectionCover[0]!=null)connectionCover[0].setVisibility(View.GONE);for(int j=0;j<buttons.size();j++){boolean on=j==next;buttons.get(j).setTextColor(on?0xff12081b:0xffe3dce9);buttons.get(j).setBackground(rounded(on?0xffae71e7:0xff281b32,on?0:0xff4c3a59));}
   }catch(Exception ignored){}
  };
  watchSink=(incomingSession,update)->{
   if(!session.equals(incomingSession)||player==null||playerLayer==null)return;
   JSONArray found=update.optJSONArray("servers");if(found!=null)for(int k=0;k<found.length()&&options.length()<50;k++){
    JSONObject item=found.optJSONObject(k);if(item==null)continue;String url=item.optString("url"),lang=item.optString("lang","");Uri u=Uri.parse(url);if(!"https".equals(u.getScheme())||!playerHost(u.getHost())||u.getUserInfo()!=null||!("sub".equals(lang)||"dub".equals(lang)))continue;
    boolean dup=false;for(int j=0;j<options.length();j++){JSONObject old=options.optJSONObject(j);if(old!=null&&url.equals(old.optString("url"))&&lang.equals(old.optString("lang"))){dup=true;break;}}if(dup)continue;
    int index=options.length();options.put(item);addButton.accept(index,item);
   }
   if(update.optBoolean("blocked",false))lookupBlocked[0]=true;if(update.optBoolean("done",false))lookupDone[0]=true;
   if(waiting[0])retryServer[0].run();
  };
  if(countsByLang[2]>0){addGap(content,8);TextView moreTitle=text("OTHER AUDIO",12,0xffb37ee4);moreTitle.setTypeface(null,android.graphics.Typeface.BOLD);content.addView(moreTitle);addGap(content,8);content.addView(extraCol);}
  addGap(content,22);TextView epTitle=text("Episodes",16,Color.WHITE);epTitle.setTypeface(null,android.graphics.Typeface.BOLD);content.addView(epTitle);addGap(content,6);TextView epHint=text("Search by number or name • "+eps.length()+" available",12,0xffa79ab2);content.addView(epHint);addGap(content,10);
  EditText search=new EditText(this);search.setSingleLine(true);search.setTextColor(Color.WHITE);search.setHintTextColor(0xff907f9d);search.setHint("Search episodes");search.setTextSize(14);search.setPadding(dp(12),0,dp(12),0);search.setBackground(rounded(0xff24172e,0xff4f3f5c));content.addView(search,new LinearLayout.LayoutParams(-1,dp(46)));addGap(content,12);
  LinearLayout pagination=new LinearLayout(this);pagination.setGravity(Gravity.CENTER_VERTICAL);content.addView(pagination);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);final int[] page={current/100};final ArrayList<Integer> matches=new ArrayList<>();
  Runnable refresh=()->{matches.clear();String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);for(int i=0;i<eps.length();i++){JSONObject ep=eps.optJSONObject(i);String num=ep!=null?ep.optString("number",""):eps.optString(i,"");String name=ep!=null?ep.optString("name",""):"";if(query.isEmpty()||num.toLowerCase(Locale.ROOT).contains(query)||name.toLowerCase(Locale.ROOT).contains(query))matches.add(i);}int pages=Math.max(1,(matches.size()+99)/100);if(page[0]>=pages)page[0]=pages-1;pagination.removeAllViews();list.removeAllViews();if(matches.size()>100){TextView prev=chip("‹",false),indicator=text("   "+(page[0]+1)+" / "+pages+"   ",12,0xffb9aac6),next=chip("›",false);pagination.addView(prev);pagination.addView(indicator);pagination.addView(next);prev.setOnClickListener(v->{if(page[0]>0){page[0]--;pagination.post(()->((Runnable)list.getTag()).run());}});next.setOnClickListener(v->{if(page[0]+1<pages){page[0]++;pagination.post(()->((Runnable)list.getTag()).run());}});addGap(list,10);}if(matches.isEmpty())list.addView(text("No matching episodes.",13,0xffa79ab2));for(int k=page[0]*100;k<Math.min((page[0]+1)*100,matches.size());k++){int idx=matches.get(k);JSONObject ep=eps.optJSONObject(idx);String num=ep!=null?ep.optString("number","?"):eps.optString(idx,"?");String name=ep!=null?ep.optString("name","").trim():"";if(num.length()>20)num=num.substring(0,20);if(name.length()>160)name=name.substring(0,160);if(name.isEmpty())name="Episode "+num;LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),0,dp(12),0);row.setBackgroundColor(idx==current?0xff39234b:(k%2==0?0xff24182d:0xff1c1124));TextView number=text(num,14,idx==current?0xffbf88f2:0xffb2a6bc);number.setGravity(Gravity.CENTER);row.addView(number,new LinearLayout.LayoutParams(dp(36),-1));TextView titleLabel=text(name,14,idx==current?0xffcfa2fb:0xffe6e1eb);titleLabel.setPadding(dp(12),0,0,0);row.addView(titleLabel,new LinearLayout.LayoutParams(0,-2,1));if(idx==current)row.addView(text("▶",14,0xffae73e5));list.addView(row,new LinearLayout.LayoutParams(-1,dp(54)));row.setOnClickListener(v->{if(idx==current)return;closePlayer();ui.evaluateJavascript("window.kuroChooseEpisode&&window.kuroChooseEpisode("+idx+")",null);});}};
  list.setTag(refresh);search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){page[0]=0;refresh.run();}public void afterTextChanged(android.text.Editable e){}});refresh.run();
  addGap(content,24);JSONObject anime=info.optJSONObject("animeInfo");if(anime==null)anime=new JSONObject();
  TextView detailHeading=text("Anime details",16,0xfff3eef7);detailHeading.setTypeface(null,android.graphics.Typeface.BOLD);content.addView(detailHeading);addGap(content,10);
  LinearLayout detail=new LinearLayout(this);detail.setOrientation(LinearLayout.HORIZONTAL);detail.setPadding(dp(12),dp(12),dp(12),dp(12));detail.setBackground(rounded(0xff21142a,0xff3f2f4c));content.addView(detail,new LinearLayout.LayoutParams(-1,-2));
  ImageView poster=new ImageView(this);poster.setScaleType(ImageView.ScaleType.CENTER_CROP);poster.setBackgroundColor(0xff372447);LinearLayout.LayoutParams posterLP=new LinearLayout.LayoutParams(dp(76),dp(114));posterLP.setMargins(0,0,dp(14),0);detail.addView(poster,posterLP);loadPoster(poster,anime.optString("image",""));
  LinearLayout detailText=new LinearLayout(this);detailText.setOrientation(LinearLayout.VERTICAL);detail.addView(detailText,new LinearLayout.LayoutParams(0,-2,1));TextView detailName=text(info.optString("title","Anime"),18,Color.WHITE);detailName.setTypeface(null,android.graphics.Typeface.BOLD);detailText.addView(detailName);addGap(detailText,7);
  JSONObject counts=anime.optJSONObject("counts");StringBuilder facts=new StringBuilder();if(counts!=null){String sub=counts.optString("sub","");String dub=counts.optString("dub","");String total=counts.optString("total","");if(sub.matches("[0-9]{1,5}"))facts.append("CC ").append(sub);if(dub.matches("[0-9]{1,5}")){if(facts.length()>0)facts.append("  •  ");facts.append("DUB ").append(dub);}if(total.matches("[0-9]{1,5}")){if(facts.length()>0)facts.append("  •  ");facts.append(total).append(" EPS");}}String meta=anime.optString("meta","").trim();if(!meta.isEmpty()){if(facts.length()>0)facts.append("  •  ");facts.append(meta.length()>80?meta.substring(0,80):meta);}if(facts.length()>0){detailText.addView(text(facts.toString(),11,0xffb98ae5));addGap(detailText,8);}String desc=anime.optString("description","").trim();if(desc.length()>3000)desc=desc.substring(0,3000);TextView synopsis=text(desc.isEmpty()?"Description not available from this source.":desc,13,0xffdcd5e2);synopsis.setLineSpacing(dp(3),1);detailText.addView(synopsis);addGap(detailText,10);TextView more=chip("View details",false);detailText.addView(more);more.setOnClickListener(v->{closePlayer();ui.evaluateJavascript("window.kuroOpenDetails&&window.kuroOpenDetails()",null);});
  addGap(content,10);TextView note=text("Playback uses a third-party player. Autoplay is best effort; tap play if needed. Use its fullscreen control to expand.",12,0xffa08fae);content.addView(note);
  root.addView(playerLayer,new FrameLayout.LayoutParams(-1,-1));playerLayer.setTranslationX(dp(24));playerLayer.setAlpha(0f);playerLayer.animate().translationX(0f).alpha(1f).setDuration(260).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();if(!loadPlayerOption561(options.getJSONObject(chosen),ref)){if(connectionCover[0]!=null)connectionCover[0].setVisibility(View.VISIBLE);main.post(retryServer[0]);}
 }catch(Exception e){Toast.makeText(this,"Unable to open watch page",Toast.LENGTH_LONG).show();if(playerLayer!=null)closePlayer();}}
 void loadPoster(ImageView target,String address){pool.execute(()->{try{URI u=new URI(address);String h=u.getHost();if(!"https".equals(u.getScheme())||u.getUserInfo()!=null||u.getPort()!=-1||!("hianime.ms".equals(h)||"hianime.dk".equals(h)||"cdn.hianime.dk".equals(h)||"s4.anilist.co".equals(h)||"cdn.noitatnemucod.net".equals(h)))return;HttpURLConnection c=(HttpURLConnection)u.toURL().openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(12000);c.setRequestProperty("User-Agent","Mozilla/5.0");if(c.getResponseCode()!=200){c.disconnect();return;}byte[] bytes;try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(out.size()+n>2000000){c.disconnect();return;}out.write(buf,0,n);}bytes=out.toByteArray();}c.disconnect();android.graphics.BitmapFactory.Options bounds=new android.graphics.BitmapFactory.Options();bounds.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);if(bounds.outWidth<=0||bounds.outHeight<=0||bounds.outWidth>6000||bounds.outHeight>6000)return;android.graphics.BitmapFactory.Options opts=new android.graphics.BitmapFactory.Options();opts.inSampleSize=Math.max(1,Math.max(bounds.outWidth/300,bounds.outHeight/450));android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);if(bitmap!=null)main.post(()->{if(target.isAttachedToWindow())target.setImageBitmap(bitmap);});}catch(Exception ignored){}});}
 private void setupProgress561(WebView view){
  try{if(!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))return;
   WebViewCompat.addWebMessageListener(view,"KuroProgress561",Collections.singleton("https://kuro.local"),(sender,message,origin,isMainFrame,reply)->{
    if(sender!=player||nativePlayer561!=null||!playerHls||!isMainFrame||message.getType()!=WebMessageCompat.TYPE_STRING||origin==null||!"https".equals(origin.getScheme())||!"kuro.local".equals(origin.getHost()))return;
    String loaded=sender.getUrl(),raw=message.getData();if(loaded==null||!loaded.startsWith("https://kuro.local/stream-player.html?")||raw==null||raw.length()>600)return;
    try{JSONObject v=new JSONObject(raw);if(v.optLong("attempt",-1)!=playerAttempt)return;long position=v.optLong("position",-1),duration=v.optLong("duration",-1);if(v.optBoolean("ended",false)&&duration>=5000&&position>=duration*.98)KuroPlayback561.completed(MainActivity.this,resumeKey561);else KuroPlayback561.save(MainActivity.this,resumeKey561,position,duration);}catch(Exception ignored){}
   });
  }catch(RuntimeException|LinkageError ignored){}
 }
 private TextView directChip561(String label,String tag){TextView v=chip(label,false);v.setTextSize(11);v.setSingleLine(true);v.setEllipsize(android.text.TextUtils.TruncateAt.END);v.setMinHeight(dp(48));v.setTag(tag);return v;}
 private void addDirectTools561(LinearLayout content){
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);directTools561=panel;panel.setVisibility(View.GONE);
  LinearLayout row1=new LinearLayout(this),row2=new LinearLayout(this);row1.setOrientation(LinearLayout.HORIZONTAL);row2.setOrientation(LinearLayout.HORIZONTAL);
  mode561=directChip561("Web + AI","player-mode-561");captions561=directChip561("Captions","player-captions-561");markers561=directChip561("Skip markers","player-markers-561");subtitleStyle571=directChip561("Native subtitle appearance","player-subtitle-style-571");
  TextView pip=directChip561("PiP","player-pip-561"),download=directChip561("Download","player-download-561");
  restart572=directChip561("Restart","player-restart-572");fit572=directChip561("Crop","player-fit-572");
  LinearLayout row3=new LinearLayout(this);row3.setOrientation(LinearLayout.HORIZONTAL);
  for(TextView v:new TextView[]{mode561,captions561,markers561}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(0,0,dp(5),dp(6));row1.addView(v,lp);}
  for(TextView v:new TextView[]{pip,download}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(0,0,dp(5),dp(6));row2.addView(v,lp);}
  for(TextView v:new TextView[]{restart572,fit572}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(0,0,dp(5),dp(6));row3.addView(v,lp);}
  panel.addView(row1);panel.addView(row2);panel.addView(row3);panel.addView(subtitleStyle571,new LinearLayout.LayoutParams(-1,-2));content.addView(panel,new LinearLayout.LayoutParams(-1,-2));
  mode561.setOnClickListener(v->{if(nativePlayer561!=null){webPreferred561=true;loadPlayer(directAddress561,directReferer561,directKind561);}else{captureWebPosition561(false,pos->{webPreferred561=false;loadPlayer(directAddress561,directReferer561,directKind561);});}});
  captions561.setOnClickListener(v->pickSubtitle561());subtitleStyle571.setOnClickListener(v->{if(nativePlayer561!=null)nativePlayer561.subtitleStyleDialog();});markers561.setOnClickListener(v->{if(nativePlayer561!=null)nativePlayer561.markersDialog();else webMarkersDialog574();});pip.setOnClickListener(v->enterPip560());restart572.setOnClickListener(v->restartEpisode572());fit572.setOnClickListener(v->toggleFit572());download.setOnClickListener(v->{if(watchInfo561!=null)KuroOffline561.confirmDownload(this,directAddress561,resumeKey561,watchInfo561.optString("title","Video"),watchInfo561.optString("episode",""));});
 }
 private void refreshDirectTools561(){if(directTools561==null)return;boolean direct=playerHls&&!directAddress561.isEmpty(),nativeActive=nativePlayer561!=null;directTools561.setVisibility(direct?View.VISIBLE:View.GONE);if(mode561!=null){mode561.setText(nativeActive?"Web + AI":"Native");mode561.setContentDescription(nativeActive?"Switch to Web player for effects":"Switch to native player");}for(TextView v:new TextView[]{captions561,subtitleStyle571})if(v!=null){v.setEnabled(nativeActive);v.setAlpha(nativeActive?1f:.45f);}if(markers561!=null){markers561.setEnabled(direct);markers561.setAlpha(direct?1f:.45f);}
  if(restart572!=null){boolean canRestart=nativeActive||(player!=null&&playerHls);restart572.setEnabled(canRestart);restart572.setAlpha(canRestart?1f:.45f);restart572.setContentDescription("Restart this episode from the beginning where the player reports a real position");}
  if(fit572!=null){fit572.setEnabled(nativeActive);fit572.setAlpha(nativeActive?1f:.45f);updateFitChip572();}
 }
 private void updateWebMarkers574(){
  if(player==null||nativePlayer561!=null||!playerHls||player.getUrl()==null||!player.getUrl().startsWith("https://kuro.local/stream-player.html?"))return;
  JSONArray ranges=new JSONArray();JSONObject source=serverOption561==null?null:serverOption561.optJSONObject("skip");
  KuroPlayback561.Range intro=KuroPlayback561.manualRange(this,resumeKey561),outro=KuroPlayback561.manualOutro(this,resumeKey561);
  if(intro==null&&source!=null)intro=KuroPlayback561.fromJson(source.optJSONObject("intro"));if(outro==null&&source!=null)outro=KuroPlayback561.fromJson(source.optJSONObject("outro"));
  for(KuroPlayback561.Range r:new KuroPlayback561.Range[]{intro,outro})if(r!=null&&r.endMs-r.startMs<=1800000){try{JSONArray pair=new JSONArray();pair.put(r.startMs/1000.0);pair.put(r.endMs/1000.0);ranges.put(pair);}catch(Exception ignored){}}
  JSONObject config=new JSONObject();try{config.put("on",getPreferences(MODE_PRIVATE).getBoolean("auto_skip",false));config.put("ranges",ranges);}catch(Exception ignored){}
  player.evaluateJavascript("window.__kuroSetMarkers574&&window.__kuroSetMarkers574("+config.toString()+")",null);
 }
 private void webMarkersDialog574(){
  if(!KuroPlayback561.validKey(resumeKey561)){Toast.makeText(this,"This episode has no stable identity for markers.",Toast.LENGTH_LONG).show();return;}
  new AlertDialog.Builder(KuroDialogs581.themed(this)).setTitle("Edit skip markers").setItems(new String[]{"Intro","Outro"},(d,which)->editWebMarker574(which==1)).setNegativeButton("Cancel",null).show();
 }
 private void editWebMarker574(boolean outro){
  final String key=resumeKey561,kind=outro?"Outro":"Intro";LinearLayout box=new LinearLayout(this);box.setPadding(dp(20),dp(4),dp(20),0);box.setOrientation(LinearLayout.VERTICAL);
  TextView hint=new TextView(this);hint.setText("Set this episode's marker in seconds. Auto skip must be on. Clear saved to use an available source marker.");box.addView(hint);
  EditText start=new EditText(this),end=new EditText(this);start.setSingleLine(true);end.setSingleLine(true);start.setHint(kind+" start, seconds");end.setHint(kind+" end, seconds");start.setInputType(8194);end.setInputType(8194);KuroPlayback561.Range old=outro?KuroPlayback561.manualOutro(this,key):KuroPlayback561.manualRange(this,key);if(old!=null){start.setText(String.valueOf(old.startMs/1000.0));end.setText(String.valueOf(old.endMs/1000.0));}box.addView(start);box.addView(end);
  AlertDialog dialog=new AlertDialog.Builder(KuroDialogs581.themed(this)).setTitle(kind+" marker").setView(box).setNegativeButton("Cancel",null).setNeutralButton("Clear saved",(d,w)->{if(outro)KuroPlayback561.setManualOutro(this,key,null);else KuroPlayback561.setManualRange(this,key,null);updateWebMarkers574();}).setPositiveButton("Save",null).create();
  dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{if(!key.equals(resumeKey561))throw new IllegalArgumentException();KuroPlayback561.Range r=KuroPlayback561.range(Double.parseDouble(start.getText().toString()),Double.parseDouble(end.getText().toString()));if(r==null||r.endMs-r.startMs>1800000)throw new IllegalArgumentException();if(outro)KuroPlayback561.setManualOutro(this,key,r);else KuroPlayback561.setManualRange(this,key,r);dialog.dismiss();updateWebMarkers574();Toast.makeText(this,"Marker saved for this episode.",Toast.LENGTH_SHORT).show();}catch(Exception e){end.setError("Enter an end after the start, within 30 minutes.");}}));dialog.show();
 }
 private void updateFitChip572(){
  if(fit572==null)return;
  String mode=nativePlayer561!=null?nativePlayer561.fit572():KuroVideoFit572.normalize(getPreferences(MODE_PRIVATE).getString(KuroVideoFit572.PREF,KuroVideoFit572.FIT));
  fit572.setText(KuroVideoFit572.actionLabel(mode));fit572.setContentDescription(KuroVideoFit572.description(mode));
 }
 private void toggleFit572(){
  if(nativePlayer561==null){Toast.makeText(this,"Fit and crop apply to Kuro’s own native player only.",Toast.LENGTH_SHORT).show();return;}
  String mode=nativePlayer561.cycleFit572();updateFitChip572();
  Toast.makeText(this,KuroVideoFit572.CROP.equals(mode)?"Cropped to fill the screen. Fullscreen and cutout behaviour are unchanged.":"Fitted, with the whole frame visible. Fullscreen and cutout behaviour are unchanged.",Toast.LENGTH_SHORT).show();
 }
 /**
  * Restart only where the player proves it has a real position and can seek. Kuro's own
  * stream page is driven directly; any other embed keeps the last-episode behaviour.
  */
 private void restartEpisode572(){
  if(nativePlayer561!=null){
   boolean ok=nativePlayer561.restart572();
   Toast.makeText(this,ok?"Restarted this episode from the beginning.":"Restart is unavailable until this player reports a position, so Kuro kept the last-episode behaviour.",Toast.LENGTH_LONG).show();
   if(ok)discordRefresh563();
   return;
  }
  if(player==null||!playerHls||player.getUrl()==null||!player.getUrl().startsWith("https://kuro.local/stream-player.html?")){
   Toast.makeText(this,KuroResume572.webRestartMessage(null),Toast.LENGTH_LONG).show();return;
  }
  final WebView target=player;final long attempt=playerAttempt;
  target.evaluateJavascript(KuroResume572.WEB_RESTART_JS,result->{
   if(target!=player||attempt!=playerAttempt)return;
   if(KuroResume572.webRestartApplied(result))KuroPlayback561.completed(this,resumeKey561);
   Toast.makeText(this,KuroResume572.webRestartMessage(result),Toast.LENGTH_LONG).show();
  });
 }
 private void pickSubtitle561(){if(nativePlayer561==null)return;try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/vtt","application/x-subrip","text/plain"});i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(i,SUBTITLE_PICKER561);}catch(Exception e){Toast.makeText(this,"No subtitle file picker is available.",Toast.LENGTH_SHORT).show();}}
 private void finishSubtitle561(int result,Intent data){
  if(result!=RESULT_OK||data==null||data.getData()==null||nativePlayer561==null)return;Uri uri=data.getData();if(!"content".equals(uri.getScheme()))return;
  String name="",type="";try{type=getContentResolver().getType(uri);try(android.database.Cursor c=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())name=c.getString(0);}}catch(RuntimeException ignored){}
  String lower=name==null?"":name.toLowerCase(Locale.ROOT),mime;if(lower.endsWith(".vtt")||"text/vtt".equalsIgnoreCase(type))mime=androidx.media3.common.MimeTypes.TEXT_VTT;else if(lower.endsWith(".srt")||"application/x-subrip".equalsIgnoreCase(type))mime=androidx.media3.common.MimeTypes.APPLICATION_SUBRIP;else{Toast.makeText(this,"Choose a WebVTT (.vtt) or SubRip (.srt) subtitle file.",Toast.LENGTH_LONG).show();return;}nativePlayer561.useSubtitle(uri,mime);
 }
 private void captureWebPosition561(boolean pause,java.util.function.Consumer<Long> done){
  if(player==null||!playerHls||nativePlayer561!=null||player.getUrl()==null||!player.getUrl().startsWith("https://kuro.local/stream-player.html?")){done.accept(KuroPlayback561.read(this,resumeKey561));return;}
  WebView target=player;long attempt=playerAttempt;String js="(function(){var v=document.querySelector('video');if(!v||v.readyState<1)return {p:-1,playing:false};var p=Math.round(v.currentTime*1000)||0,playing=!v.paused;"+(pause?"v.pause();":"")+"return {p:p,playing:playing}})()";
  target.evaluateJavascript(js,result->{if(target!=player||attempt!=playerAttempt)return;long position=KuroPlayback561.read(this,resumeKey561);try{JSONObject obj=new JSONObject(result);long captured=obj.optLong("p",-1);if(captured>=0){position=captured;KuroPlayback561.save(this,resumeKey561,position,-1);}}catch(Exception ignored){}done.accept(position);});
 }
 boolean loadPlayer(String address,String referer,String kind){
  Uri u=Uri.parse(address);if(!"https".equals(u.getScheme())||!playerHost(u.getHost())||u.getUserInfo()!=null||u.getPort()!=-1)return false;
  if("paradise".equals(referer)&&(!"www.animeparadise.moe".equals(u.getHost())||u.getPath()==null||!u.getPath().matches("/watch/[a-fA-F0-9]{8}-(?:[a-fA-F0-9]{4}-){3}[a-fA-F0-9]{12}")||u.getEncodedQuery()==null||!u.getEncodedQuery().matches("origin=[A-Za-z0-9_-]{1,80}")||u.getFragment()!=null))return false;
  boolean hls="hls".equals(kind);if(hls&&!KuroPlayback561.allowedHls(address,referer,kind))return false;
  Map<String,String> headers=new HashMap<>();if("village578".equals(referer))headers.put("Referer","https://www.animevillage.org/");else if("gogo-cv".equals(referer))headers.put("Referer","https://gogoanimes.cv/");else if("aniwaves-tv".equals(referer))headers.put("Referer","https://aniwaves.tv/");else if("anikoto-cz".equals(referer))headers.put("Referer","https://anikoto.cz/");else if("ms".equals(referer))headers.put("Referer","https://hianime.ms/");else if(referer!=null&&HI_MIRROR_ORIGINS.containsKey(referer))headers.put("Referer",HI_MIRROR_ORIGINS.get(referer)+"/");else if("dk".equals(referer))headers.put("Referer","https://hianime.dk/");else if("nine".equals(referer))headers.put("Referer","https://9anime.or.at/");else if("zoro".equals(referer))headers.put("Referer","https://zorotv.ba/");else if("salt".equals(referer))headers.put("Referer","https://animesalt.cx/");else if("ani".equals(referer))headers.put("Referer","https://aniwaves.ru/");else if("flix".equals(referer))headers.put("Referer","https://hianimeq.top/");else if("wixi".equals(referer))headers.put("Referer","https://aniwixi.xyz/");else if("aw".equals(referer))headers.put("Referer","https://aniwatch.co.at/");else if("miruro".equals(referer))headers.put("Referer",MIRURO_ORIGIN+"/");else if("kitsune".equals(referer))headers.put("Referer","https://kitsunee.moe/");else if("paradise".equals(referer))headers.put("Referer","https://www.animeparadise.moe/");else if("zenkai".equals(referer))headers.put("Referer","https://www.zenkaitv.com/");else if("zenkai-mirror".equals(referer))headers.put("Referer","https://animeav1.com/");
  if(nativePlayer561!=null){nativePlayer561.release();nativePlayer561=null;}clearFxFrames();playerAttempt++;playerHls=hls;directAddress561=hls?address:"";directReferer561=referer;directKind561=kind;suspendSkipFrames();prepareFailureScript();prepareFxFrameScript();preparePresenceFrame565();
  if(hls&&!webPreferred561&&watchPlayerBox!=null){try{player.stopLoading();player.loadUrl("about:blank");long attempt=playerAttempt;nativePlayer561=new KuroNativePlayer561(this,watchPlayerBox,address,resumeKey561,serverOption561,new KuroNativePlayer561.Listener(){@Override public void failure(boolean blocked){if(attempt==playerAttempt&&nativeFailure561!=null)nativeFailure561.accept(blocked);}@Override public void ended(){if(attempt==playerAttempt&&nativeEnded561!=null)nativeEnded561.run();}});player.setVisibility(View.GONE);refreshDirectTools561();return true;}catch(RuntimeException|LinkageError e){if(nativePlayer561!=null){nativePlayer561.release();nativePlayer561=null;}webPreferred561=true;}}
  player.setVisibility(View.VISIBLE);String target=hls?"https://kuro.local/stream-player.html?src="+Uri.encode(address)+"&startMs="+KuroPlayback561.read(this,resumeKey561)+"&attempt="+playerAttempt+"&autoplay="+(getPreferences(MODE_PRIVATE).getBoolean("autoplay",true)?"1":"0"):address;player.loadUrl(target,headers);refreshDirectTools561();return true;
 }
 private boolean loadPlayerOption561(JSONObject option,String fallback){if(option==null)return false;serverOption561=option;boolean loaded=loadPlayer(option.optString("url"),option.optString("referer",fallback),option.optString("type",""));discordRefresh563();return loaded;}
 void hideFullscreen(){if(customView==null)return;if(full!=null){full.removeAllViews();root.removeView(full);full=null;}customView=null;updateKeepScreen560();getWindow().getDecorView().setSystemUiVisibility(enhancedFullscreen?5894:0);if(customCallback!=null){customCallback.onCustomViewHidden();customCallback=null;}applyPlayerOptions(null);}
 // The overlay is useful only when it is enabled and the player has not
 // explicitly reported that readable frames/GPU access are unavailable. Unknown
 // or waiting states may become renderable after the fullscreen resize.
 private boolean preferOverlayFullscreen569(){
  if(player==null||playerLayer==null||watchPlayerBox==null||watchHeader==null||watchScroll==null||nativePlayer561!=null||!PlayerOptionsDialog.readConfig(this).optBoolean("enabled",false))return false;
  boolean unavailable=false;
  if(fxTopStatus!=null){String v=fxTopStatus.optString("video","");if("active".equals(v)||"color".equals(v)||"compare".equals(v))return true;unavailable="unavailable".equals(v);}
  for(FxFrame frame:fxFrames)if(frame.attempt==playerAttempt&&frame.status!=null){String v=frame.status.optString("video","");if("active".equals(v)||"color".equals(v)||"compare".equals(v))return true;if("unavailable".equals(v))unavailable=true;}
  return !unavailable;
 }
 private final Runnable hideFullscreenExit569=()->{if(enhancedToolbar!=null)enhancedToolbar.setVisibility(View.GONE);};
 private void showFullscreenExit569(){
  if(!enhancedFullscreen||enhancedToolbar==null||isInPictureInPictureMode())return;
  enhancedToolbar.setVisibility(View.VISIBLE);main.removeCallbacks(hideFullscreenExit569);main.postDelayed(hideFullscreenExit569,3500);
 }
 // Fullscreen for the existing WebView without detaching, navigating or
 // replacing it. The ordinary fullscreen callback prefers this route only
 // for enabled, potentially renderable video; restricted players fall back.
 private void enterEnhancedFullscreen(){
  if(enhancedFullscreen||player==null||playerLayer==null||watchPlayerBox==null||watchHeader==null||watchScroll==null||customView!=null)return;
  if(playerOptions!=null&&playerOptions.isShowing()){playerOptions.dismiss();playerOptions=null;}
  enhancedFullscreen=true;updateKeepScreen560();watchHeader.setVisibility(View.GONE);watchScroll.setVisibility(View.GONE);
  watchPlayerBox.setLayoutParams(new LinearLayout.LayoutParams(-1,-1));
  LinearLayout toolbar=new LinearLayout(this);toolbar.setOrientation(LinearLayout.HORIZONTAL);toolbar.setGravity(Gravity.END|Gravity.TOP);
  TextView exit=chip("⤢",false);exit.setTag("enhanced-fullscreen-exit");exit.setContentDescription("Exit enhanced fullscreen");exit.setMinWidth(dp(48));exit.setMinHeight(dp(48));exit.setOnClickListener(v->exitEnhancedFullscreen());LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-2,-2);ep.setMargins(dp(5),0,0,0);toolbar.addView(exit,ep);
  FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.END);tp.setMargins(dp(6),dp(6),dp(6),0);watchPlayerBox.addView(toolbar,tp);enhancedToolbar=toolbar;
  getWindow().getDecorView().setSystemUiVisibility(5894);watchPlayerBox.requestLayout();player.requestLayout();applyPlayerOptions(null);showFullscreenExit569();
 }
 private void exitEnhancedFullscreen(){
  if(!enhancedFullscreen)return;enhancedFullscreen=false;main.removeCallbacks(hideFullscreenExit569);updateKeepScreen560();
  if(watchPlayerBox!=null){if(enhancedToolbar!=null)watchPlayerBox.removeView(enhancedToolbar);watchPlayerBox.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));watchPlayerBox.requestLayout();}
  enhancedToolbar=null;if(watchHeader!=null)watchHeader.setVisibility(View.VISIBLE);if(watchScroll!=null)watchScroll.setVisibility(View.VISIBLE);
  if(customView==null)getWindow().getDecorView().setSystemUiVisibility(0);if(player!=null){player.requestLayout();applyPlayerOptions(null);}
 }
 private void showWatchOverlay(String kind){
  if(ui==null||playerLayer==null||!("search".equals(kind)||"news".equals(kind)||"account".equals(kind)))return;
  if(playerOptions!=null&&playerOptions.isShowing()){playerOptions.dismiss();playerOptions=null;}
  if(kind.equals(watchOverlay)){closeWatchOverlay();return;}if(!watchOverlay.isEmpty())closeWatchOverlay();
  watchOverlay=kind;final int token=++watchOverlayToken;ui.setBackgroundColor(Color.TRANSPARENT);ui.setAlpha(0f);ui.bringToFront();ui.requestFocus();
  String fn="search".equals(kind)?"kuroWatchSearchOpen":"news".equals(kind)?"kuroWatchNewsOpen":"kuroWatchAccountOpen";
  String element="news".equals(kind)?"news-overlay":"search".equals(kind)?"search-popup":"account-overlay";
  ui.evaluateJavascript("(function(){try{if(typeof window."+fn+"!=='function')return false;window."+fn+"("+token+");return !!document.getElementById('"+element+"')}catch(e){return false}})()",result->watchOverlayResult(kind,token,result));
 }
 private void watchOverlayResult(String kind,int token,String result){
  if(!kind.equals(watchOverlay)||token!=watchOverlayToken||ui==null)return;
  if("false".equals(result)){finishWatchOverlay(kind,token);Toast.makeText(this,"Overlay isn't ready. Please try again.",Toast.LENGTH_SHORT).show();return;}
  ui.setAlpha(1f);ui.requestFocus();
 }
 private void finishWatchOverlay(String kind,int token){
  if(!kind.equals(watchOverlay)||token!=watchOverlayToken)return;watchOverlay="";
  if(ui!=null){ui.setAlpha(1f);ui.setBackgroundColor(Color.rgb(10,7,15));ui.clearFocus();}if(customView!=null&&full!=null)full.bringToFront();else if(playerLayer!=null)playerLayer.bringToFront();
 }
 private void closeWatchOverlay(){
  if(watchOverlay.isEmpty())return;String kind=watchOverlay;int token=watchOverlayToken;String fn="search".equals(kind)?"kuroWatchSearchClose":"news".equals(kind)?"kuroWatchNewsClose":"kuroWatchAccountClose";
  if(ui!=null)ui.evaluateJavascript("window."+fn+"&&window."+fn+"()",null);finishWatchOverlay(kind,token);
 }
 private void navigateFromWatch(String destination){
  if(!"home".equals(destination))return;
  closePlayer();if(ui!=null)ui.evaluateJavascript("window.kuroWatchHeader&&window.kuroWatchHeader("+JSONObject.quote(destination)+")",null);
 }
 void openEpisodeDrawer581(){
  JSONObject info=watchInfo561;JSONArray eps=info==null?null:info.optJSONArray("episodes");
  if(eps==null||eps.length()==0){Toast.makeText(this,"No episode list for this title.",Toast.LENGTH_SHORT).show();return;}
  if(playerLayer==null)return;
  if(episodeDrawer581==null)episodeDrawer581=new KuroEpisodeDrawer581(this,root,index->chooseEpisode581(index));episodeDrawer581.setProgress(number->{
    JSONObject wi=watchInfo561;if(wi==null||number==null||number.isEmpty())return "";
    long at=KuroPlayback561.read(this,KuroPlayback561.key(wi.optString("source"),wi.optString("showIdentity"),number));
    if(at<=0)return "";
    long sec=at/1000;return String.format(java.util.Locale.ROOT,"Resume %d:%02d",sec/60,sec%60);
   });
  if(episodeDrawer581.isOpen()){episodeDrawer581.close();return;}
  episodeDrawer581.show(eps,info.optInt("current",0));
 }
 private void chooseEpisode581(int index){
  JSONObject info=watchInfo561;JSONArray eps=info==null?null:info.optJSONArray("episodes");
  if(eps==null||index<0||index>=eps.length()||index==info.optInt("current",-1))return;
  if(ui==null)return;
  closePlayer();
  ui.evaluateJavascript("window.kuroChooseEpisode&&window.kuroChooseEpisode("+index+")",null);
 }
 void closePlayer(){cancelAutoNext573();if(episodeDrawer581!=null){episodeDrawer581.close();episodeDrawer581=null;}clearPresenceFrames565();presenceFrameMode565=false;discordNotificationCancel564();discordRefresh563();if(nativePlayer561!=null){nativePlayer561.release();nativePlayer561=null;}nativeFailure561=null;nativeEnded561=null;watchInfo561=null;serverOption561=null;resumeKey561="";directAddress561="";directReferer561="";directKind561="";directTools561=null;mode561=null;captions561=null;markers561=null;subtitleStyle571=null;if(playerOptions!=null&&playerOptions.isShowing())playerOptions.dismiss();playerOptions=null;closeWatchOverlay();String closed=watchSession;watchSession="";updatePipActions571();watchSink=null;clearFxFrames();fxConfig=null;fxComparing=false;playerAttempt++;playerHls=false;if(!closed.isEmpty()&&ui!=null)ui.evaluateJavascript("window.kuroPlayerClosed&&window.kuroPlayerClosed("+JSONObject.quote(closed)+")",null);hideFullscreen();exitEnhancedFullscreen();if(player!=null){suspendSkipFrames();player.stopLoading();player.loadUrl("about:blank");player.destroy();player=null;skipFrameMode=false;fxFrameMode=false;fxFrameScript=null;playerErrorScript=null;errorFrameMode=false;completionFrameMode=false;completionStartMode=false;}if(playerLayer!=null){root.removeView(playerLayer);playerLayer=null;}watchPlayerBox=null;watchHeader=null;watchScroll=null;enhancedToolbar=null;enhancedFullscreen=false;fxConfig=null;fxTopStatus=null;fxComparing=false;}
 @Override public void onConfigurationChanged(android.content.res.Configuration config){super.onConfigurationChanged(config);if(player!=null)player.requestLayout();if(ui!=null)ui.requestLayout();if(playerOptions!=null&&playerOptions.isShowing())playerOptions.relayout();}
 @Override public void onBackPressed(){if(playerOptions!=null&&playerOptions.isShowing()){playerOptions.dismiss();playerOptions=null;return;}if(!watchOverlay.isEmpty()){if("account".equals(watchOverlay)&&ui!=null){final int token=watchOverlayToken;ui.evaluateJavascript("window.kuroWatchAccountBack?window.kuroWatchAccountBack():false",result->{if(!"true".equals(result)&&"account".equals(watchOverlay)&&token==watchOverlayToken)closeWatchOverlay();});}else closeWatchOverlay();return;}if(customView!=null){hideFullscreen();return;}if(enhancedFullscreen){exitEnhancedFullscreen();return;}if(playerLayer!=null){closePlayer();return;}ui.evaluateJavascript("window.kuroBack&&window.kuroBack()",null);}
 @Override protected void onPause(){cancelAutoNext573();if(ui!=null&&uiReady){ui.evaluateJavascript("window.kuroSavePage&&window.kuroSavePage()",null);ui.evaluateJavascript("window.kuroSyncSuspend553&&window.kuroSyncSuspend553()",null);}activityResumed=false;main.removeCallbacks(updateWatch582);if(!pipRequest560&&!isInPictureInPictureMode())discordBackground563();updateSkipFrames();if(nativePlayer561!=null&&!pipRequest560&&!isInPictureInPictureMode())nativePlayer561.pauseForBackground();if(player!=null){applyPlayerOptions(null);if(!pipRequest560&&!isInPictureInPictureMode())player.onPause();}super.onPause();} @Override protected void onResume(){super.onResume();activityResumed=true;main.removeCallbacks(updateWatch582);main.postDelayed(updateWatch582,1200);discordForeground563();if(ui!=null&&uiReady)ui.evaluateJavascript("window.kuroSyncResume553&&window.kuroSyncResume553()",null);NewsSync.schedule(this);requestNewsPermissionIfFirstTime();newsStatusChanged();checkNewsOnResume();if(nativePlayer561!=null)nativePlayer561.resumeForeground();if(player!=null){player.onResume();updateSkipFrames();applyPlayerOptions(null);}} @Override protected void onDestroy(){KuroPipControls571.detach(this);if(githubAdmin568!=null)githubAdmin568.close();discordClose563();closePlayer();pool.shutdownNow();if(ui!=null){ui.removeJavascriptInterface("KuroNative");ui.destroy();ui=null;}super.onDestroy();}
}
