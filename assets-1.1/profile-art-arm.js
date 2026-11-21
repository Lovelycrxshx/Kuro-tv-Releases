/* Kuro profile art — anime avatars + matching banners (1.1: 10 pairs).
   Self-contained: paints the profile hero and injects its own picker, so it
   survives every re-render of the account page. */
(function(){
 'use strict';
 var KEY='kuro-profile-art-v1',IDS=['art-01','art-02','art-03','art-04','art-05','art-06','art-07','art-08','art-09','art-10'];
 function src(kind,id){return 'profile-art/'+kind+'/'+id+'.webp'}
 function read(){try{var raw=localStorage.getItem(KEY);if(!raw)return{};var o=JSON.parse(raw);return o&&typeof o==='object'&&!Array.isArray(o)?o:{}}catch(e){return{}}}
 function save(o){try{localStorage.setItem(KEY,JSON.stringify(o))}catch(e){}}
 function valid(id){return IDS.indexOf(id)>=0}
 function tiles(kind,current,label){
  var head='<button type="button" class="art-tile581 art-none581'+(current?'':' is-on')+'" data-kind="'+kind+'" data-id="" aria-label="'+label+'"><span>'+label+'</span></button>';
  return head+IDS.map(function(id,i){
   return '<button type="button" class="art-tile581'+(current===id?' is-on':'')+'" data-kind="'+kind+'" data-id="'+id+'" aria-label="Art '+i+1+'"><img src="'+src(kind,id)+'" alt="" loading="lazy"></button>';
  }).join('');
 }
 function build(o){
  var el=document.createElement('div');
  el.className='account-section profile-art581';el.id='profile-art581';
  el.innerHTML='<div class="account-section-head"><div><h2>Profile art</h2><p>Anime avatars, each with a matching banner. Saved on this phone.</p></div></div>'+
   '<div class="art-row581"><span class="art-label581">Avatar</span><div class="art-grid581 art-grid-pfp581">'+tiles('pfp',o.pfp,'Initials')+'</div></div>'+
   '<div class="art-row581"><span class="art-label581">Banner</span><div class="art-grid581 art-grid-banner581">'+tiles('banner',o.banner,'None')+'</div></div>'+
   '<p class="art-note581">Banners crop from the left, so the scene stays readable behind your avatar.</p>';
  return el;
 }
 function panel(o){
  var page=document.querySelector('.profile-page547');if(!page)return;
  var host=page.querySelector('#profile-art581');
  if(!host){
   host=build(o);
   var stats=page.querySelector('.profile-stats571'),anchor=stats&&stats.nextElementSibling;
   if(anchor&&anchor.parentNode===page)page.insertBefore(host,anchor);else page.appendChild(host);
  }
  host.querySelectorAll('.art-tile581').forEach(function(b){
   var on=(b.getAttribute('data-id')||'')===(o[b.getAttribute('data-kind')]||'');
   b.classList.toggle('is-on',on);
   b.setAttribute('aria-pressed',on?'true':'false');
  });
  if(!host.dataset.bound){host.dataset.bound='1';host.addEventListener('click',function(ev){
   var b=ev.target.closest&&ev.target.closest('.art-tile581');if(!b)return;
   var kind=b.getAttribute('data-kind'),id=b.getAttribute('data-id')||'',o2=read();
   if(id&&valid(id))o2[kind]=id;else delete o2[kind];
   save(o2);paint();
  });}
 }
 function paintMonos(o){
  var id=valid(o.pfp||'')?o.pfp:'';
  var list=document.querySelectorAll('#header-login .header-mono571, .profile-drawer560 .profile-identity560 .profile-mono571, .profile-private547 .profile-mono571');
  Array.prototype.forEach.call(list,function(el){
   if(id){var u='url("'+src('pfp',id)+'")';if(el.style.backgroundImage!==u)el.style.backgroundImage=u;el.classList.add('has-art581');}
   else{if(el.style.backgroundImage)el.style.backgroundImage='';el.classList.remove('has-art581');}
  });
 }
 var lastSynced='';
 function sync(o){
  // Keep the native player chip in step with the Web UI choice.
  try{var want=valid(o.pfp||'')?o.pfp:'';if(want===lastSynced)return;lastSynced=want;var n=window.KuroNative;if(n&&typeof n.profileArt581==='function')n.profileArt581(want);}catch(e){}
 }
 function paint(){
  var o=read();
  sync(o);
  paintMonos(o);
  var hero=document.querySelector('.profile-hero547');if(!hero)return;
  var av=hero.querySelector('.profile-avatar');
  if(av){
   var keep=valid(o.pfp||'')?o.pfp:'',img=av.querySelector('.profile-art-pfp581');
   if(keep){
    if(!img){img=document.createElement('img');img.className='profile-art-pfp581';img.alt='';av.insertBefore(img,av.firstChild);}
    if(img.getAttribute('src')!==src('pfp',keep))img.setAttribute('src',src('pfp',keep));
    av.classList.add('has-art581');
   }else{
    if(img)img.remove();
    av.classList.remove('has-art581');
   }
  }
  var bn=hero.querySelector('.profile-banner');
  if(bn){
   var kb=valid(o.banner||'')?o.banner:'';
   if(kb){var u='url("'+src('banner',kb)+'")';if(bn.style.backgroundImage!==u)bn.style.backgroundImage=u;bn.classList.add('has-art581');}
   else{bn.style.backgroundImage='';bn.classList.remove('has-art581');}
  }
  panel(o);
 }
 var queued=false;
 function schedule(){if(queued)return;queued=true;setTimeout(function(){queued=false;paint()},0)}
 function boot(){
  if(window.MutationObserver){
   var mo=new MutationObserver(schedule);
   if(document.body)mo.observe(document.body,{childList:true,subtree:true});
  }
  paint();
 }
 window.kuroProfileArt581={paint:paint,read:read,ids:IDS.slice()};
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot);else boot();
})();
