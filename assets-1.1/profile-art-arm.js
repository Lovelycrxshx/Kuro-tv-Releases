/* Kuro profile art — 24 manga character face avatars + 24 matching 16:9 banners.
   Self-contained: paints the profile hero and injects its own picker, so it
   survives every re-render of the account page. */
(function(){
 'use strict';
 var KEY='kuro-profile-art-v1';
 var META=[
  ['art-11','Satoru Gojo · Jujutsu Kaisen'],
  ['art-12','Ryomen Sukuna · Jujutsu Kaisen'],
  ['art-13','Yuta Okkotsu · Jujutsu Kaisen 0'],
  ['art-14','Toji Fushiguro · Jujutsu Kaisen'],
  ['art-15','Sung Jinwoo · Solo Leveling'],
  ['art-16','Light Yagami · Death Note'],
  ['art-17','Denji · Chainsaw Man'],
  ['art-18','Makima · Chainsaw Man'],
  ['art-19','Asta · Black Clover'],
  ['art-20','Ichigo Kurosaki · Bleach'],
  ['art-21','Sosuke Aizen · Bleach: TYBW'],
  ['art-22','Frieren · Beyond Journey\u2019s End'],
  ['art-23','Mash Burnedead · Mashle'],
  ['art-24','Goku · Dragon Ball Z'],
  ['art-25','Gabimaru · Hell\u2019s Paradise'],
  ['art-26','Akame · Akame ga Kill!'],
  ['art-27','Kaoruko Waguri · Fragrant Flower'],
  ['art-28','Eren Yeager · Attack on Titan'],
  ['art-29','Levi Ackerman · Attack on Titan'],
  ['art-30','Nagumo · Sakamoto Days'],
  ['art-31','Ai Hoshino · Oshi no Ko'],
  ['art-32','Buddha · Record of Ragnarok'],
  ['art-33','Johan Liebert · Monster'],
  ['art-34','Yuuichi Katagiri · Tomodachi Game']
 ];
 var IDS=META.map(function(m){return m[0]});
 var LABELS={};META.forEach(function(m){LABELS[m[0]]=m[1]});
 function src(kind,id){return 'profile-art/'+kind+'/'+id+'.webp'}
 function read(){try{var raw=localStorage.getItem(KEY);if(!raw)return{};var o=JSON.parse(raw);return o&&typeof o==='object'&&!Array.isArray(o)?o:{}}catch(e){return{}}}
 function save(o){try{localStorage.setItem(KEY,JSON.stringify(o))}catch(e){}}
 function valid(id){return IDS.indexOf(id)>=0}
 function esc(s){return String(s).replace(/&/g,'&amp;').replace(/"/g,'&quot;').replace(/</g,'&lt;')}
 function tiles(kind,current,label){
  var head='<button type="button" class="art-tile581 art-none581'+(current?'':' is-on')+'" data-kind="'+kind+'" data-id="" aria-label="'+esc(label)+'"><span>'+esc(label)+'</span></button>';
  return head+IDS.map(function(id,i){
   var nm=LABELS[id]||('Character '+(i+1));
   return '<button type="button" class="art-tile581'+(current===id?' is-on':'')+'" data-kind="'+kind+'" data-id="'+id+'" title="'+esc(nm)+'" aria-label="'+esc(nm)+'"><img src="'+src(kind,id)+'" alt="" loading="lazy"></button>';
  }).join('');
 }
 function build(o){
  var el=document.createElement('div');
  el.className='account-section profile-art581';el.id='profile-art581';
  el.innerHTML='<div class="account-section-head"><div><h2>Profile art</h2><p>24 manga face avatars, each paired with a matching manga banner. Saved on this phone.</p></div><button type="button" class="button ghost small" data-art-match="1">Match banner</button></div>'+
   '<div class="art-row581"><span class="art-label581">Avatar</span><div class="art-grid581 art-grid-pfp581">'+tiles('pfp',o.pfp,'Initials')+'</div></div>'+
   '<div class="art-row581"><span class="art-label581">Banner</span><div class="art-grid581 art-grid-banner581">'+tiles('banner',o.banner,'None')+'</div></div>'+
   '<p class="art-note581">Each avatar and banner share the same order \u2014 tap Match banner anytime to pair your avatar\u2019s manga spread.</p>';
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
  var curPfp=valid(o.pfp||'')?o.pfp:'',curBan=valid(o.banner||'')?o.banner:'';
  host.querySelectorAll('.art-tile581').forEach(function(b){
   var k=b.getAttribute('data-kind'),want=k==='pfp'?curPfp:curBan;
   var on=(b.getAttribute('data-id')||'')===want;
   b.classList.toggle('is-on',on);
   b.setAttribute('aria-pressed',on?'true':'false');
  });
  var mbtn=host.querySelector('[data-art-match]');
  if(mbtn)mbtn.disabled=!curPfp||curBan===curPfp;
  if(!host.dataset.bound){host.dataset.bound='1';host.addEventListener('click',function(ev){
   var mb=ev.target.closest&&ev.target.closest('[data-art-match]');
   if(mb){var om=read();if(valid(om.pfp||'')){om.banner=om.pfp;save(om);paint();}return;}
   var b=ev.target.closest&&ev.target.closest('.art-tile581');if(!b)return;
   var kind=b.getAttribute('data-kind'),id=b.getAttribute('data-id')||'',o2=read();
   if(!valid(o2.pfp||''))delete o2.pfp;
   if(!valid(o2.banner||''))delete o2.banner;
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
 var lastSynced=null;
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
    if(img.getAttribute('src')!==src('pfp',keep))img.setAttribute('src',keep?src('pfp',keep):'');
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
 window.kuroProfileArt581={paint:paint,read:read,ids:IDS.slice(),labels:LABELS};
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot);else boot();
})();
