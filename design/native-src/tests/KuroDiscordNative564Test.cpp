// Host-side simulated SDK/JNI test, NOT a real Discord runtime test.
#include <cassert>
#include <cstring>
#include <iostream>
#include <memory>
#include <vector>
#include <string>
#include <jni.h>
#include "../native/kuro_discord_rpc563.cpp"
struct FakeBytes{std::vector<jbyte> v;};
static std::vector<std::unique_ptr<FakeBytes>> buffers;
static jbyteArray makeBytes(const std::string&s){auto p=std::make_unique<FakeBytes>();p->v.assign(s.begin(),s.end());auto raw=p.get();buffers.push_back(std::move(p));return reinterpret_cast<jbyteArray>(raw);}
static std::string stringOf(jbyteArray a){auto b=reinterpret_cast<FakeBytes*>(a);return std::string(b->v.begin(),b->v.end());}
int main(){
 JNINativeInterface_ table{};
 table.GetArrayLength=[](JNIEnv*,jarray a)->jsize{return (jsize)reinterpret_cast<FakeBytes*>(a)->v.size();};
 table.GetByteArrayRegion=[](JNIEnv*,jbyteArray a,jsize begin,jsize len,jbyte*out){auto&p=reinterpret_cast<FakeBytes*>(a)->v;std::memcpy(out,p.data()+begin,len);};
 table.NewByteArray=[](JNIEnv*,jsize len)->jbyteArray{auto p=std::make_unique<FakeBytes>();p->v.resize(len);auto raw=p.get();buffers.push_back(std::move(p));return reinterpret_cast<jbyteArray>(raw);};
 table.SetByteArrayRegion=[](JNIEnv*,jbyteArray a,jsize begin,jsize len,const jbyte*in){auto&p=reinterpret_cast<FakeBytes*>(a)->v;std::memcpy(p.data()+begin,in,len);};
 JNIEnv_ env{&table};constexpr jlong app=1569077670074122250LL,one=11,two=22;
 assert(Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeInit(&env,nullptr,app,one));
 assert(direct&&direct->app==uint64_t(app));
 // 5.81 rev 11.4: presence uses the unauthenticated RPC transport, so the client is
 // never connected and is not gated on Status::Ready.
 assert(direct->connectCount==0);
 assert(direct->GetStatus()==discordpp::Client::Status::Disconnected);
 assert(Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeDirectState(&env,nullptr,one)==0);
 assert(direct->updateCount==0);
 auto d=makeBytes("Actual title"),st=makeBytes("Episode 1 · DUB"),img=makeBytes("https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx1-example.jpg"),watch=makeBytes("https://example.org/show"),download=makeBytes("https://example.org/get");
 auto linkedClient=newLinked();linkedClient->status=discordpp::Client::Status::Ready;
 Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeTargets(&env,nullptr,one,JNI_TRUE,JNI_TRUE);
 discordpp::ActivityButton stale;stale.SetLabel("Watch");stale.SetUrl("https://hianime.ms/watch-old");direct->last.AddButton(stale);linkedClient->last.AddButton(stale);
 Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativePublish(&env,nullptr,one,d,st,img,watch,download,1000,2000);
 assert(direct->updateCount==1&&linkedClient->updateCount==1);
 assert(direct->GetStatus()==discordpp::Client::Status::Disconnected); // published without ever connectingassert(direct->last.type==discordpp::ActivityTypes::Watching);assert(direct->last.name=="Kuro.tv");
 assert(direct->last.details=="Actual title"&&direct->last.state=="Episode 1 · DUB");assert(direct->last.assets.large=="https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx1-example.jpg");assert(direct->last.buttons.empty()&&linkedClient->last.buttons.empty());assert(direct->last.times.start==1000&&direct->last.times.end==2000);
 auto officialDl=makeBytes("https://lovelycrxshx.github.io/Kuro-tv-Releases/");
 Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativePublish(&env,nullptr,one,d,st,img,watch,officialDl,1000,2000);
 assert(direct->last.buttons.size()==1&&direct->last.buttons[0].label=="Download"&&direct->last.buttons[0].url=="https://lovelycrxshx.github.io/Kuro-tv-Releases/");
 assert(linkedClient->last.buttons.size()==1&&linkedClient->last.buttons[0].label=="Download"&&linkedClient->last.buttons[0].url=="https://lovelycrxshx.github.io/Kuro-tv-Releases/");
 auto providerImg=makeBytes("https://hianime.ms/poster.jpg");
 Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativePublish(&env,nullptr,one,d,st,providerImg,watch,download,1000,2000);
 assert(direct->last.assets.large.empty()&&linkedClient->last.assets.large.empty());
 assert(direct->last.buttons.empty()&&linkedClient->last.buttons.empty());
 auto epImg=makeBytes("https://media.kitsu.app/episodes/thumbnails/229114/original.jpeg");
 Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativePublish(&env,nullptr,one,d,st,epImg,watch,download,1000,2000);
 assert(direct->last.assets.large.empty()&&linkedClient->last.assets.large.empty());
 auto animeCover=makeBytes("https://media.kitsu.app/anime/poster_images/1/original.jpg");
 Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativePublish(&env,nullptr,one,d,st,animeCover,watch,download,1000,2000);
 assert(direct->last.assets.large=="https://media.kitsu.app/anime/poster_images/1/original.jpg"&&linkedClient->last.assets.large==direct->last.assets.large);
 auto banner=makeBytes("https://s4.anilist.co/file/anilistcdn/media/anime/banner/bx1.jpg");
 Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativePublish(&env,nullptr,one,d,st,banner,watch,download,1000,2000);
 assert(direct->last.assets.large.empty()&&linkedClient->last.assets.large.empty());
 auto badKitsu=makeBytes("https://media.kitsu.app/episodes/thumbnails/229114/secret-hianime.jpg");
 Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativePublish(&env,nullptr,one,d,st,badKitsu,watch,download,1000,2000);
 assert(direct->last.assets.large.empty()&&linkedClient->last.assets.large.empty());
 Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativePump(&env,nullptr,one);assert(Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeResult(&env,nullptr,one)==1);assert(Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeDirectState(&env,nullptr,one)==2);assert(Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeLinkedResult(&env,nullptr,one)==1);
 int directClear=direct->clearCount;Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeTargets(&env,nullptr,one,JNI_FALSE,JNI_TRUE);assert(direct->clearCount==directClear+1);
 int dCount=direct->updateCount,lCount=linkedClient->updateCount;Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativePublish(&env,nullptr,one,d,st,img,watch,download,0,0);assert(direct->updateCount==dCount&&linkedClient->updateCount==lCount+1);
 int linkedClear=linkedClient->clearCount;Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeTargets(&env,nullptr,one,JNI_TRUE,JNI_FALSE);assert(linkedClient->clearCount==linkedClear+1);
 lCount=linkedClient->updateCount;Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativePublish(&env,nullptr,one,d,st,img,watch,download,0,0);assert(linkedClient->updateCount==lCount);
 Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeTargets(&env,nullptr,one,JNI_TRUE,JNI_TRUE);
 direct->refuse=true;Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativePublish(&env,nullptr,one,d,st,img,watch,download,0,0);
 Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativePump(&env,nullptr,one);
 assert(Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeResult(&env,nullptr,one)==-1);
 assert(Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeDirectState(&env,nullptr,one)==-1);
 assert(Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeLogs(&env,nullptr,one)!=nullptr);
 // A refusal rebuilds the transport on the retry clock instead of waiting for a handshake.
 directRetryAt=Clock::now();
 Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativePump(&env,nullptr,one);
 assert(direct->clearCount==0);   // a fresh client, not the one that was just cleared
 assert(direct->updateCount==1);  // republished exactly once on the rebuilt transport
 assert(direct->GetStatus()==discordpp::Client::Status::Disconnected);
 Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeReconnectCurrent(&env,nullptr,one);assert(Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeResult(&env,nullptr,one)==0);Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativePump(&env,nullptr,one);assert(Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeResult(&env,nullptr,one)==0); // old callback ignored
 Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeRestore(&env,nullptr,one,makeBytes("refresh-old"));assert(Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeRefreshing(&env,nullptr,one));
 assert(Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeBusy(&env,nullptr,two));assert(!Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeInit(&env,nullptr,app,two));
 Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeQuiesce(&env,nullptr,one);assert(quiescing&&!lastActivity);
 auto cb=discordpp::Client::refreshCallback;assert(bool(cb));cb(discordpp::ClientResult{true},"access-token","refresh-rotated",discordpp::AuthorizationTokenType::Bearer,3600,"");
 assert(!Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeRefreshing(&env,nullptr,one));assert(Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeBusy(&env,nullptr,two));
 assert(stringOf(Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativePendingRefresh(&env,nullptr,one))=="refresh-rotated");
 Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeAckRefresh(&env,nullptr,one,makeBytes("wrong"));assert(Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeBusy(&env,nullptr,two));
 Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeAckRefresh(&env,nullptr,one,makeBytes("refresh-rotated"));assert(!Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeBusy(&env,nullptr,two));
 Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeStop(&env,nullptr,two);assert(owner==one); // stale owner cannot stop the active session
 Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeStop(&env,nullptr,one);assert(owner==0&&!direct&&!linked);assert(Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeInit(&env,nullptr,app,two));
 Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativeStop(&env,nullptr,two);
 std::cout<<"PASS simulated native SDK: presence published without connecting, refusal rebuilds the transport, anime covers for both targets, no episode artwork/buttons, clear/reconnect, stale ownership, serialized token rotation\n";
}
