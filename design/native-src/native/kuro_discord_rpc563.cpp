// Kuro's JNI bridge to the separately supplied, unmodified Discord Social SDK.
// All entry points and SDK callback pumping run on one Java HandlerThread.
#define DISCORDPP_IMPLEMENTATION
#include "discordpp.h"
#include <jni.h>
#include <algorithm>
#include <chrono>
#include <cstdint>
#include <memory>
#include <mutex>
#include <string>
#include <vector>
#include <utility>

namespace {
 using Clock=std::chrono::steady_clock;
 std::shared_ptr<discordpp::Client> direct,linked;
 std::shared_ptr<discordpp::Activity> lastActivity;
 std::vector<std::pair<int,int>> trace; // last SDK status/error pairs, for the status card
 std::vector<std::string> logs;         // last SDK log lines, for the status card
 std::mutex guard;
 jlong owner=0;uint64_t applicationId=0,sessionGeneration=0,directGeneration=0,linkGeneration=0;
 bool sendDirect=true,sendLinked=true,refreshing=false,quiescing=false;
 int directResult=0,linkedResult=0,linkedState=0,directState=0,directError=0;
 jlong directAccepted=0,linkedAccepted=0;
 std::string activeRefresh,pendingRefresh;
 Clock::time_point refreshAt=Clock::time_point::max();
 Clock::time_point directRetryAt=Clock::time_point::max();
 const char* redirect="discord-1569077670074122250:/authorize/callback";
 jlong nowMs(){return std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();}
 void note(int st,int err){if(trace.size()>=8)trace.erase(trace.begin());trace.emplace_back(st,err);}
 std::string read(JNIEnv* e,jbyteArray arr,size_t limit=512){
  if(!arr)return {};jsize n=e->GetArrayLength(arr);if(n<=0||static_cast<size_t>(n)>limit)return {};
  std::string out(static_cast<size_t>(n),'\0');e->GetByteArrayRegion(arr,0,n,reinterpret_cast<jbyte*>(&out[0]));return out;
 }
 jbyteArray array(JNIEnv* e,const std::string& s){jbyteArray a=e->NewByteArray(static_cast<jsize>(s.size()));if(a&&!s.empty())e->SetByteArrayRegion(a,0,static_cast<jsize>(s.size()),reinterpret_cast<const jbyte*>(s.data()));return a;}
 bool ours(jlong id){return id!=0&&id==owner;}
 // The current-account client talks to the Discord app on the phone. It is only
 // useful once the SDK reports Ready, so nothing is published before that, and a
 // dropped connection is retried instead of leaving a silent dead client behind.
 // Presence-only transport: the SDK talks to the signed-in Discord app over its
 // RPC service, so UpdateRichPresence is the whole protocol. There is no gateway
 // session to wait for, and waiting for Status::Ready without a token is what kept
 // the old build on "connecting" forever with nothing ever published.
 void publishDirect(){
  if(!sendDirect||!direct||!lastActivity||quiescing)return;
  auto g=directGeneration,s=sessionGeneration;
  directState=1;
  direct->UpdateRichPresence(*lastActivity,[g,s](discordpp::ClientResult r){
   if(g!=directGeneration||s!=sessionGeneration)return;
   if(r.Successful()){directResult=1;directState=2;directError=0;directAccepted=nowMs();}
   else{directResult=-1;directState=-1;if(directRetryAt==Clock::time_point::max())directRetryAt=Clock::now()+std::chrono::seconds(10);}
  });
 }
 std::shared_ptr<discordpp::Client> newDirect(){
  if(direct){try{direct->ClearRichPresence();direct->Disconnect();}catch(...){}direct.reset();}
  direct=std::make_shared<discordpp::Client>();logs.clear();
  try{direct->AddLogCallback([](auto message,auto severity){(void)severity;if(logs.size()>=6)logs.erase(logs.begin());logs.push_back(std::string(message).substr(0,200));},discordpp::LoggingSeverity::Info);}catch(...){}
  direct->SetApplicationId(applicationId);
  directResult=0;directState=0;directError=0;directRetryAt=Clock::time_point::max();
  auto g=directGeneration,s=sessionGeneration;std::weak_ptr<discordpp::Client> weak=direct;
  direct->SetStatusChangedCallback([weak,g,s](discordpp::Client::Status st,discordpp::Client::Error err,int32_t){
   auto c=weak.lock();if(!c||g!=directGeneration||s!=sessionGeneration)return;
   note(static_cast<int>(st),static_cast<int>(err));
   if(err!=discordpp::Client::Error::None)directError=static_cast<int>(err);
  });
  // No Client::Connect() here on purpose: presence uses the unauthenticated RPC
  // transport documented for Android (Social SDK 1.10+), which needs only the
  // application id and a signed-in Discord app on the phone.
  return direct;
 }
 // Called from the pump: a refused update usually means the transport went stale
 // (Discord restarted or was updated), so the client is rebuilt and republished.
 void keepDirect(){
  if(!sendDirect||!direct||quiescing)return;
  if(directState<0&&Clock::now()>=directRetryAt){
   directRetryAt=Clock::now()+std::chrono::seconds(10);
   ++directGeneration;newDirect();publishDirect();
  }
 }
 void publishLinked(){
  if(!sendLinked||!linked||linked->GetStatus()!=discordpp::Client::Status::Ready||!lastActivity)return;
  auto g=linkGeneration,s=sessionGeneration;
  linked->UpdateRichPresence(*lastActivity,[g,s](discordpp::ClientResult r){if(g==linkGeneration&&s==sessionGeneration){linkedResult=r.Successful()?1:-1;if(r.Successful())linkedAccepted=nowMs();}});
 }
 void resetLinked(){
  ++linkGeneration;
  if(linked){try{if(linked->GetStatus()==discordpp::Client::Status::Ready)linked->ClearRichPresence();linked->Disconnect();}catch(...){}linked.reset();}
  activeRefresh.clear();pendingRefresh.clear();refreshing=false;linkedState=0;linkedResult=0;refreshAt=Clock::time_point::max();
 }
 std::shared_ptr<discordpp::Client> newLinked(){
  resetLinked();linked=std::make_shared<discordpp::Client>();linked->SetApplicationId(applicationId);
  auto g=linkGeneration,s=sessionGeneration;std::weak_ptr<discordpp::Client> weak=linked;
  linked->SetStatusChangedCallback([weak,g,s](discordpp::Client::Status st,discordpp::Client::Error err,int32_t){
   auto c=weak.lock();if(!c||g!=linkGeneration||s!=sessionGeneration)return;
   if(st==discordpp::Client::Status::Ready){linkedState=2;publishLinked();}
   else if(err!=discordpp::Client::Error::None)linkedState=-1;
   else if(st==discordpp::Client::Status::Reconnecting||st==discordpp::Client::Status::Connecting||st==discordpp::Client::Status::Connected)linkedState=1;
   else if(st==discordpp::Client::Status::Disconnected)linkedState=activeRefresh.empty()?0:1;
  });return linked;
 }
 void applyToken(uint64_t g,uint64_t s,discordpp::ClientResult r,std::string token,std::string refresh,discordpp::AuthorizationTokenType type,int32_t expires){
  if(g!=linkGeneration||s!=sessionGeneration||!linked)return;
  refreshing=false;
  if(!r.Successful()||token.empty()) {linkedState=-1;refreshAt=Clock::now()+std::chrono::minutes(5);return;}
  if(!refresh.empty()){activeRefresh=std::move(refresh);pendingRefresh=activeRefresh;}
  // A rotated refresh token must be committed by Java before the next refresh.
  refreshAt=Clock::now()+std::chrono::seconds(std::max(60,expires-300));linkedState=1;
  linked->UpdateToken(type,std::move(token),[g,s](discordpp::ClientResult t){
   if(g!=linkGeneration||s!=sessionGeneration||!linked)return;
   if(!t.Successful()){linkedState=-1;return;}
   if(linked->GetStatus()!=discordpp::Client::Status::Ready)linked->Connect();
  });
 }
 void tryRefresh(){
  if(quiescing||!linked||activeRefresh.empty()||refreshing||!pendingRefresh.empty()||Clock::now()<refreshAt)return;
  refreshing=true;linkedState=1;auto g=linkGeneration,s=sessionGeneration;
  linked->RefreshToken(applicationId,activeRefresh,[g,s](discordpp::ClientResult r,std::string token,std::string refresh,discordpp::AuthorizationTokenType type,int32_t expires,std::string){applyToken(g,s,std::move(r),std::move(token),std::move(refresh),type,expires);});
 }
 void clearActivity(){
  lastActivity.reset();try{if(direct)direct->ClearRichPresence();}catch(...){}
  try{if(linked&&linked->GetStatus()==discordpp::Client::Status::Ready)linked->ClearRichPresence();}catch(...){}
 }
 void shutdown(){
  ++sessionGeneration;++directGeneration;clearActivity();resetLinked();
  if(direct){try{discordpp::RunCallbacks();}catch(...){}direct.reset();}
  owner=0;directResult=0;directState=0;directError=0;trace.clear();logs.clear();directRetryAt=Clock::time_point::max();
 }
}
extern "C" {
JNIEXPORT jboolean JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeBusy(JNIEnv*,jclass,jlong requested){std::lock_guard<std::mutex> l(guard);return owner!=0&&owner!=requested&&(refreshing||!pendingRefresh.empty())?JNI_TRUE:JNI_FALSE;}
JNIEXPORT jboolean JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeRefreshing(JNIEnv*,jclass,jlong id){std::lock_guard<std::mutex> l(guard);return ours(id)&&refreshing?JNI_TRUE:JNI_FALSE;}
JNIEXPORT jboolean JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeInit(JNIEnv*,jclass,jlong id,jlong idOwner){
 std::lock_guard<std::mutex> l(guard);try{if(id<=0||idOwner<=0)return JNI_FALSE;if(owner!=0&&owner!=idOwner&&(refreshing||!pendingRefresh.empty()))return JNI_FALSE;shutdown();owner=idOwner;applicationId=static_cast<uint64_t>(id);sendDirect=true;sendLinked=true;quiescing=false;newDirect();return JNI_TRUE;}catch(...){shutdown();return JNI_FALSE;}
}
JNIEXPORT void JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeTargets(JNIEnv*,jclass,jlong id,jboolean current,jboolean other){
 std::lock_guard<std::mutex> l(guard);try{if(!ours(id))return;if(sendDirect&&!current&&direct)direct->ClearRichPresence();if(sendLinked&&!other&&linked&&linked->GetStatus()==discordpp::Client::Status::Ready)linked->ClearRichPresence();sendDirect=current;sendLinked=other;if(!sendDirect)directResult=0;if(!sendLinked)linkedResult=0;}catch(...){}
}
// Only recognizable anime poster paths are accepted; episode thumbnails,
// source-page URLs, credentials, query parameters and escapes are never assets.
static bool isNeutralPoster566(const std::string& url){
 if(url.size()>300)return false;
 const char* hosts[]={"https://s4.anilist.co/","https://s3.anilist.co/","https://cdn.myanimelist.net/","https://media.kitsu.app/","https://media.kitsu.io/"};
 size_t prefix=0;int kind=0;for(int i=0;i<5;i++){std::string h(hosts[i]);if(url.compare(0,h.size(),h)==0){prefix=h.size();kind=i<2?1:i==2?2:3;break;}}
 if(prefix==0||url.size()<=prefix)return false;
 std::string path;path.reserve(url.size()-prefix);
 for(size_t i=prefix;i<url.size();++i){unsigned char c=static_cast<unsigned char>(url[i]);
  if(!((c>='a'&&c<='z')||(c>='A'&&c<='Z')||(c>='0'&&c<='9')||c=='.'||c=='_'||c=='~'||c=='-'||c=='/'))return false;
  path+=static_cast<char>(c>='A'&&c<='Z'?c+('a'-'A'):c);
 }
 auto digits=[](const std::string& s){if(s.empty()||s.size()>10)return false;for(char c:s)if(c<'0'||c>'9')return false;return true;};
 auto fileImage=[](const std::string& f){if(f.empty()||f.find('/')!=std::string::npos)return false;for(const char* x:{".jpg",".jpeg",".png",".webp"}){std::string e(x);if(f.size()>e.size()&&f.compare(f.size()-e.size(),e.size(),e)==0)return true;}return false;};
 bool poster=false;
 if(kind==1){
  const std::string base="file/anilistcdn/media/anime/cover/";if(path.compare(0,base.size(),base)==0){std::string rest=path.substr(base.size());size_t n=rest.find('/');if(n!=std::string::npos){std::string size=rest.substr(0,n);poster=(size=="small"||size=="medium"||size=="large")&&fileImage(rest.substr(n+1));}}
 }else if(kind==2){
  const std::string base="images/anime/";if(path.compare(0,base.size(),base)==0){std::string rest=path.substr(base.size());size_t n=rest.find('/');if(n!=std::string::npos){std::string f=rest.substr(n+1);poster=digits(rest.substr(0,n))&&!f.empty()&&f[0]>='0'&&f[0]<='9'&&fileImage(f);}}
 }else if(kind==3){
  const std::string legacy="anime/poster_images/",newer="anime/";
  if(path.compare(0,legacy.size(),legacy)==0){std::string rest=path.substr(legacy.size());size_t n=rest.find('/');if(n!=std::string::npos&&digits(rest.substr(0,n))){std::string f=rest.substr(n+1);for(const char* size:{"original","large","medium","small","tiny"})for(const char* ext:{".jpg",".jpeg",".png",".webp"})if(f==std::string(size)+ext)poster=true;}}
  else if(path.compare(0,newer.size(),newer)==0){std::string rest=path.substr(newer.size());size_t n=rest.find('/');if(n!=std::string::npos&&digits(rest.substr(0,n))){std::string f=rest.substr(n+1);for(const char* folder:{"poster/","poster_image/"}){std::string base(folder);if(f.compare(0,base.size(),base)==0&&fileImage(f.substr(base.size())))poster=true;}}}
 }
 if(!poster)return false;
 const char* forbidden[]={"hianime","9anime","zorotv","animesalt","aniwaves","zenkaitv","aniwixi","animeav1","aniwatch","miruro","kitsunee","animeparadise"};
 for(const char* word:forbidden)if(path.find(word)!=std::string::npos)return false;
 return true;
}
JNIEXPORT void JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativePublish(JNIEnv* e,jclass,jlong id,jbyteArray d,jbyteArray st,jbyteArray image,jbyteArray watch,jbyteArray download,jlong start,jlong end){
 std::lock_guard<std::mutex> l(guard);try{
  if(!ours(id)||!direct)return;auto a=std::make_shared<discordpp::Activity>();a->SetType(discordpp::ActivityTypes::Watching);a->SetName("Kuro.tv");
  // Episode/source watch links are never published; only Kuro's pinned download page button may be attached.
  (void)watch;auto details=read(e,d),state=read(e,st),poster=read(e,image),dl=read(e,download,256);
  if(!details.empty())a->SetDetails(details);if(!state.empty())a->SetState(state);
  if(isNeutralPoster566(poster)){discordpp::ActivityAssets assets;assets.SetLargeImage(poster);a->SetAssets(assets);}
  if(start>0){discordpp::ActivityTimestamps t;t.SetStart(static_cast<uint64_t>(start));if(end>start)t.SetEnd(static_cast<uint64_t>(end));a->SetTimestamps(t);}
  if(dl=="https://github.com/Lovelycrxshx/Kuro-tv-Releases/releases/download/v1.5.4/Kuro-tv-1.5.4.apk"){discordpp::ActivityButton btn;btn.SetLabel("Download");btn.SetUrl(dl);a->AddButton(btn);}
  lastActivity=a;publishDirect();publishLinked();
 }catch(...){directResult=-1;}
}
JNIEXPORT void JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativePump(JNIEnv*,jclass,jlong id){std::lock_guard<std::mutex> l(guard);try{if(ours(id)){discordpp::RunCallbacks();tryRefresh();keepDirect();}}catch(...){directResult=-1;}}
JNIEXPORT jint JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeResult(JNIEnv*,jclass,jlong id){std::lock_guard<std::mutex> l(guard);return ours(id)?directResult:0;}
JNIEXPORT jint JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeDirectState(JNIEnv*,jclass,jlong id){std::lock_guard<std::mutex> l(guard);return ours(id)?directState:0;}
JNIEXPORT jint JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeDirectError(JNIEnv*,jclass,jlong id){std::lock_guard<std::mutex> l(guard);return ours(id)?directError:0;}
JNIEXPORT jint JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeTraceCount(JNIEnv*,jclass,jlong id){std::lock_guard<std::mutex> l(guard);return ours(id)?static_cast<jint>(trace.size()):0;}
JNIEXPORT jint JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeTraceStatus(JNIEnv*,jclass,jlong id,jint i){std::lock_guard<std::mutex> l(guard);if(!ours(id)||i<0||static_cast<size_t>(i)>=trace.size())return -1;return trace[static_cast<size_t>(i)].first;}
JNIEXPORT jint JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeTraceError(JNIEnv*,jclass,jlong id,jint i){std::lock_guard<std::mutex> l(guard);if(!ours(id)||i<0||static_cast<size_t>(i)>=trace.size())return -1;return trace[static_cast<size_t>(i)].second;}
JNIEXPORT jbyteArray JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeLogs(JNIEnv* e,jclass,jlong id){
 std::lock_guard<std::mutex> l(guard);if(!ours(id))return array(e,std::string());
 std::string all;for(const auto& line:logs){if(!all.empty())all+=" | ";all+=line;}return array(e,all);
}
JNIEXPORT jint JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeLinkedState(JNIEnv*,jclass,jlong id){std::lock_guard<std::mutex> l(guard);return ours(id)?linkedState:0;}
JNIEXPORT jint JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeLinkedResult(JNIEnv*,jclass,jlong id){std::lock_guard<std::mutex> l(guard);return ours(id)?linkedResult:0;}
JNIEXPORT jlong JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeLastAccepted(JNIEnv*,jclass,jlong id,jboolean other){std::lock_guard<std::mutex> l(guard);return ours(id)?(other?linkedAccepted:directAccepted):0;}
JNIEXPORT void JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeQuiesce(JNIEnv*,jclass,jlong id){std::lock_guard<std::mutex> l(guard);if(ours(id)){quiescing=true;clearActivity();}}
JNIEXPORT void JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeClear(JNIEnv*,jclass,jlong id){std::lock_guard<std::mutex> l(guard);if(ours(id))clearActivity();}
JNIEXPORT void JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeReconnectCurrent(JNIEnv*,jclass,jlong id){
 std::lock_guard<std::mutex> l(guard);try{if(!ours(id))return;++directGeneration;newDirect();}catch(...){directResult=-1;}
}
JNIEXPORT void JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeUnlink(JNIEnv*,jclass,jlong id){std::lock_guard<std::mutex> l(guard);if(ours(id))resetLinked();}
JNIEXPORT void JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeRestore(JNIEnv* e,jclass,jlong id,jbyteArray value){
 std::lock_guard<std::mutex> l(guard);try{auto token=read(e,value,4096);if(!ours(id)||token.empty())return;newLinked();activeRefresh=std::move(token);linkedState=1;refreshAt=Clock::now();tryRefresh();}catch(...){linkedState=-1;}
}
JNIEXPORT void JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeExchange(JNIEnv* e,jclass,jlong id,jbyteArray authCode,jbyteArray verifier){
 std::lock_guard<std::mutex> l(guard);try{auto code=read(e,authCode,2048),v=read(e,verifier,256);if(!ours(id)||code.empty()||v.empty())return;auto c=newLinked();auto g=linkGeneration,s=sessionGeneration;linkedState=1;refreshing=true;
  c->GetToken(applicationId,code,v,redirect,[g,s](discordpp::ClientResult r,std::string token,std::string refresh,discordpp::AuthorizationTokenType type,int32_t expires,std::string){applyToken(g,s,std::move(r),std::move(token),std::move(refresh),type,expires);});
 }catch(...){linkedState=-1;refreshing=false;}
}
JNIEXPORT jbyteArray JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativePendingRefresh(JNIEnv* e,jclass,jlong id){std::lock_guard<std::mutex> l(guard);return array(e,ours(id)?pendingRefresh:std::string());}
JNIEXPORT void JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeAckRefresh(JNIEnv* e,jclass,jlong id,jbyteArray value){std::lock_guard<std::mutex> l(guard);if(ours(id)&&read(e,value,4096)==pendingRefresh)pendingRefresh.clear();}
JNIEXPORT void JNICALL Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeStop(JNIEnv*,jclass,jlong id){std::lock_guard<std::mutex> l(guard);try{if(ours(id))shutdown();}catch(...){owner=0;direct.reset();linked.reset();lastActivity.reset();}}
}
