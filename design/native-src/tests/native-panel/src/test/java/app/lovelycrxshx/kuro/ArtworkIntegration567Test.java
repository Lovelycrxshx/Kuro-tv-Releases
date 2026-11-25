package app.lovelycrxshx.kuro;

import android.content.Context;
import android.os.Bundle;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Shadow Android test: fake executor only; no real network, Discord or device. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=33,manifest=Config.NONE,qualifiers="w360dp-h800dp-xhdpi")
public class ArtworkIntegration567Test {
 private static final String COVER="https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx1-example.jpg";
 private static final String EP="https://media.kitsu.app/episodes/thumbnails/100/original.jpeg";
 private ActivityController<MainActivity> controller;private MainActivity activity;private KuroArtwork567 artwork;
 private final List<Runnable> lookups=new ArrayList<>();private final List<String> queries=new ArrayList<>();
 private Object field(String n)throws Exception{Field f=MainActivity.class.getDeclaredField(n);f.setAccessible(true);return f.get(activity);}
 private void set(String n,Object x)throws Exception{Field f=MainActivity.class.getDeclaredField(n);f.setAccessible(true);f.set(activity,x);}
 static class NoNetwork extends AbstractExecutorService {
  public void shutdown(){}public List<Runnable> shutdownNow(){return Collections.emptyList();}public boolean isShutdown(){return false;}public boolean isTerminated(){return false;}public boolean awaitTermination(long t,TimeUnit u){return true;}public void execute(Runnable r){}
 }
 @Before public void create()throws Exception{
  controller=Robolectric.buildActivity(MainActivity.class);activity=controller.get();set("pool",new NoNetwork());controller.create(new Bundle()).start().resume().visible();
  artwork=new KuroArtwork567(lookups::add,t->{queries.add(t);JSONObject item=new JSONObject().put("id",1).put("title",new JSONObject().put("english","Cowboy Bebop")).put("coverImage",new JSONObject().put("extraLarge",COVER));return new JSONObject().put("data",new JSONObject().put("Page",new JSONObject().put("media",new JSONArray().put(item))));},()->1000000L);
  set("discordArtwork567",artwork);activity.getSharedPreferences("kuro-discord-rpc563",Context.MODE_PRIVATE).edit().clear().commit();
 }
 @After public void close(){controller.pause().stop().destroy();}
 private void enable(){activity.getSharedPreferences("kuro-discord-rpc563",Context.MODE_PRIVATE).edit().putBoolean("enabled",true).commit();}
 private void publish(String cover,String episodeImage,String episode)throws Exception{
  JSONObject anime=new JSONObject().put("image",cover);
  JSONObject info=new JSONObject().put("title","Cowboy Bebop").put("episode",episode).put("episodeImage",episodeImage).put("animeInfo",anime);
  Method m=MainActivity.class.getDeclaredMethod("discordPublish563",JSONObject.class,JSONObject.class,String.class,long.class,long.class,double.class);m.setAccessible(true);m.invoke(activity,info,null,"selected",-1L,-1L,1.0);
 }
 @Test public void disabledPresenceAndHiddenTitleOrPosterDoNotSearch()throws Exception{
  KuroDiscordPreferences564 prefs=(KuroDiscordPreferences564)field("discordPrefs564");
  publish("",EP,"2");assertTrue(lookups.isEmpty());enable();
  prefs.set("title","hide");publish("",EP,"2");assertTrue(lookups.isEmpty());
  prefs.set("title","show");prefs.set("poster","icon");publish("",EP,"2");assertTrue(lookups.isEmpty());
 }
 @Test public void episodeVisibilityAndNumberNeverCauseEpisodeArtworkLookup()throws Exception{
  enable();KuroDiscordPreferences564 prefs=(KuroDiscordPreferences564)field("discordPrefs564");prefs.set("title","show");prefs.set("poster","show");prefs.set("episode","hide");
  publish("",EP,"1");assertEquals(1,lookups.size());prefs.set("episode","show");publish("",EP,"2");assertEquals("same anime cover lookup is shared",1,lookups.size());
  lookups.remove(0).run();assertEquals(Collections.singletonList("Cowboy Bebop"),queries);
  publish("",EP,"3");assertTrue("cached cover is reused across episodes",lookups.isEmpty());assertEquals(COVER,artwork.resolve("Cowboy Bebop","",null));
 }
 @Test public void suppliedAnimeCoverWinsEvenWhenEpisodeImageIsPresent()throws Exception{
  enable();KuroDiscordPreferences564 prefs=(KuroDiscordPreferences564)field("discordPrefs564");prefs.set("title","show");prefs.set("poster","show");prefs.set("episode","show");
  publish(COVER,EP,"2");assertTrue("valid supplied anime cover needs no lookup",lookups.isEmpty());assertEquals(COVER,artwork.resolve("Cowboy Bebop",COVER,null));
  publish(EP,EP,"2");assertEquals("episode path mislabeled as anime cover is rejected",1,lookups.size());
 }
 @Test public void snapshotKeepsEpisodeTextButFiltersEpisodeArtwork(){
  KuroPresence564.Options o=new KuroPresence564.Options();
  KuroPresence563.Snapshot a=KuroPresence564.watching(o,"Cowboy Bebop","2","Asteroid Blues","sub",COVER,"","","Server 1","24","selected",-1,-1,1800000000000L,1.0);
  assertEquals(COVER,a.image);assertTrue(a.state.contains("Episode 2"));
  KuroPresence563.Snapshot b=KuroPresence564.watching(o,"Cowboy Bebop","2","Asteroid Blues","sub",EP,"","","Server 1","24","selected",-1,-1,1800000000000L,1.0);
  assertTrue(b.image.isEmpty());assertTrue(b.state.contains("Episode 2"));
 }
}
