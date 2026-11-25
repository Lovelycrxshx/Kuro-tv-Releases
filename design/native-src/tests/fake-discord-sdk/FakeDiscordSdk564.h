// Handwritten test double. This is NOT Discord's SDK or its header.
#pragma once
#include <functional>
#include <string>
#include <vector>
#include <memory>
#include <utility>
#include <cstdint>
namespace discordpp {
 inline std::vector<std::function<void()>> queue;
 inline void RunCallbacks(){auto work=std::move(queue);queue.clear();for(auto &f:work)f();}
 struct ClientResult{bool ok=true;bool Successful()const{return ok;}};
 enum class AuthorizationTokenType{Bearer};
 enum class LoggingSeverity{Verbose,Info,Warning,Error,None};
 enum class ActivityTypes{Watching};
 struct ActivityAssets{std::string large;void SetLargeImage(const std::string& s){large=s;}};
 struct ActivityButton{std::string label,url;void SetLabel(const std::string&s){label=s;}void SetUrl(const std::string&s){url=s;}};
 struct ActivityTimestamps{uint64_t start=0,end=0;void SetStart(uint64_t s){start=s;}void SetEnd(uint64_t s){end=s;}};
 struct Activity{
  ActivityTypes type=ActivityTypes::Watching;std::string name,details,state;ActivityAssets assets;std::vector<ActivityButton> buttons;ActivityTimestamps times;
  void SetType(ActivityTypes t){type=t;}void SetName(const std::string&s){name=s;}void SetDetails(const std::string&s){details=s;}void SetState(const std::string&s){state=s;}
  void SetAssets(const ActivityAssets&a){assets=a;}void AddButton(const ActivityButton&b){buttons.push_back(b);}void SetTimestamps(const ActivityTimestamps&t){times=t;}
 };
 class Client{
 public:
  enum class Status{Disconnected,Connecting,Connected,Ready,Reconnecting,Disconnecting,HttpWait};enum class Error{None,Other};
  using TokenCb=std::function<void(ClientResult,std::string,std::string,AuthorizationTokenType,int32_t,std::string)>;
  inline static TokenCb refreshCallback;
  Status status=Status::Disconnected;uint64_t app=0;int clearCount=0,updateCount=0,disconnectCount=0,connectCount=0;bool refuse=false;Activity last;
  std::function<void(Status,Error,int32_t)> statusCallback;
  void SetApplicationId(uint64_t n){app=n;}
  void SetStatusChangedCallback(std::function<void(Status,Error,int32_t)> f){statusCallback=std::move(f);}
  Status GetStatus()const{return status;}
  void UpdateRichPresence(const Activity&a,std::function<void(ClientResult)> cb){last=a;updateCount++;bool ok=!refuse;queue.push_back([cb,ok]{cb(ClientResult{ok});});}
  void ClearRichPresence(){clearCount++;}
  void Disconnect(){disconnectCount++;status=Status::Disconnected;}
  void UpdateToken(AuthorizationTokenType,std::string,std::function<void(ClientResult)> cb){queue.push_back([cb]{cb(ClientResult{true});});}
  void AddLogCallback(std::function<void(std::string,LoggingSeverity)>,LoggingSeverity) {}
  void Connect(){connectCount++;status=Status::Ready;if(statusCallback){auto cb=statusCallback;queue.push_back([cb]{cb(Status::Ready,Error::None,0);});}}
  void RefreshToken(uint64_t,const std::string&,TokenCb cb){refreshCallback=std::move(cb);}
  void GetToken(uint64_t,const std::string&,const std::string&,const std::string&,TokenCb cb){refreshCallback=std::move(cb);}
 };
}
