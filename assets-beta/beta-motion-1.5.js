/* Kuro 1.5-beta - motion layer, take 3.
 *
 * Two things changed after take 2:
 *
 * 1. The "banners keep loading" glitch is fixed at the root.  Take 2 armed
 *    .hero-slide and .news-poster for an IntersectionObserver reveal, which
 *    meant a real banner could sit at opacity 0 until the observer fired - and
 *    when the hero carousel re-rendered the slide, the new node was armed
 *    again and went blank again.  Now: banners, hero slides and news posters
 *    are never touched, there is no observer at all, and grid cards animate
 *    exactly once, at insertion, only if their poster has already decoded.
 *    The hidden state lives inside the keyframe, so nothing can be stranded.
 *
 * 2. The launch film is louder and shorter (2.3 s): oversized cropped type as a
 *    background layer, the wordmark over the cards, a card that sweeps across
 *    the type, a bigger ticker, specks, iris.
 *
 * Timeline:
 *   0.00 dot grid          0.20 browser frame springs up
 *   0.14 pill rail         0.26 ticker band drops in
 *   0.44 cards (one sweeps across the type)
 *   0.50 "KURO" letter by letter, 0.74 tracked-out "TV"
 *   1.78 iris closes       2.30 overlay gone, cards spring in behind it
 */
(function () {
 'use strict';

 var SEEN = 'kuro-intro-15';
 var INTRO_HOLD = 2320;
 var CARD = '.card, .history-card';
 var SETTLE = 360;          /* a card whose poster has not decoded by now is left alone */

 var reduced = false;
 try {
  reduced = !!(window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches);
 } catch (e) {}

 var introEl = null;
 var started = false;
 var seq = 0;
 var queued = false;

 function body() { return document.body || document.documentElement; }

 /* ------------------------------------------------------- the launch film */
 var TICKER = 'Kuro \u00b7 Anime \u00b7 No ads \u00b7 Your next story awaits \u00b7 ';

 function buildIntro() {
  var el = document.createElement('div');
  el.id = 'km-intro-15';
  el.setAttribute('aria-hidden', 'true');
  el.innerHTML = '' +
   '<div class="km-dots"></div>' +
   '<span class="km-t0">KURO</span>' +
   '<div class="km-rail">' +
     '<span class="km-pill"></span><span class="km-pill"></span><span class="km-pill"></span>' +
     '<span class="km-railbtn"></span><span class="km-railbtn"></span><span class="km-railbtn"></span>' +
   '</div>' +
   '<div class="km-frame">' +
     '<div class="km-chrome"><span class="km-lights"><i></i><i></i><i></i></span>' +
       '<span class="km-url"></span></div>' +
     '<div class="km-art"></div>' +
   '</div>' +
   '<div class="km-stack">' +
     '<div class="km-card km-c2"></div><div class="km-card km-c3"></div><div class="km-card km-c1"></div>' +
   '</div>' +
   '<svg class="km-curve" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">' +
     '<path d="M3 97 C 26 97 8 46 33 31 S 71 57 97 9"/></svg>' +
   '<div class="km-type">' +
     '<span class="km-t1"><span>K</span><span>U</span><span>R</span><span>O</span></span>' +
     '<span class="km-t2">TV</span>' +
   '</div>' +
   '<div class="km-ticker"><div class="km-track">' +
     '<span>' + TICKER + TICKER + TICKER + '</span>' +
     '<span>' + TICKER + TICKER + TICKER + '</span>' +
   '</div></div>' +
   '<div class="km-specks"><i></i><i></i><i></i><i></i><i></i><i></i></div>' +
   '<div class="km-iris"></div>' +
   '<span class="km-skip">Tap to skip</span>';
  return el;
 }

 /* The film borrows the user's own posters - never the reel's artwork. */
 function posterList(n) {
  var out = [], seen = {}, nodes;
  try {
   nodes = document.querySelectorAll('.shelf .poster img, .grid .poster img, .poster img');
  } catch (e) { nodes = []; }
  for (var i = 0; i < nodes.length && out.length < n; i++) {
   var src = nodes[i].currentSrc || nodes[i].getAttribute('src') || '';
   if (!src || seen[src]) continue;
   if (!nodes[i].complete || !nodes[i].naturalWidth) continue;   /* decoded only */
   seen[src] = 1;
   out.push(src);
  }
  return out;
 }

 function putImage(slot, src) {
  if (!slot || !src || slot.getAttribute('data-km-filled')) return;
  slot.setAttribute('data-km-filled', '1');
  var img = document.createElement('img');
  img.alt = '';
  img.setAttribute('src', src);
  slot.appendChild(img);
  slot.classList.remove('km-empty');
 }

 function fillArt() {
  if (!introEl) return;
  var p = posterList(4);
  var slots = [
   [introEl.querySelector('.km-art'), p[0]],
   [introEl.querySelector('.km-c1'), p[1] || p[0]],
   [introEl.querySelector('.km-c2'), p[2] || p[1] || p[0]],
   [introEl.querySelector('.km-c3'), p[3] || p[0]]
  ];
  for (var i = 0; i < slots.length; i++) putImage(slots[i][0], slots[i][1]);
  for (var j = 0; j < slots.length; j++) {
   var node = slots[j][0];
   if (node && !node.getAttribute('data-km-filled')) {
    node.classList.add('km-empty');
    node.setAttribute('data-km-filled', 'empty');
   }
  }
 }

 /* ------------------------------------------------------- card entrance */
 function ready(el) {
  var img = el.querySelector('img');
  if (!img) return true;
  return !!(img.complete && img.naturalWidth);
 }

 var retry = null;
function later() {
  if (retry) return;
  retry = setTimeout(function () { retry = null; armAll(); }, 420);
}

function armAll() {
  if (reduced) return;
  var nodes, now = Date.now();
  try { nodes = document.querySelectorAll(CARD); } catch (e) { return; }
  for (var i = 0; i < nodes.length; i++) {
   var el = nodes[i];
   if (el.getAttribute('data-km-anim')) continue;
   if (!el.getAttribute('data-km-born')) { el.setAttribute('data-km-born', String(now)); }
   if (ready(el)) {
    el.setAttribute('data-km-anim', 'done');
    el.classList.add('km-reveal', 'km-d' + ((seq++ % 6) + 1));
   } else if (now - Number(el.getAttribute('data-km-born')) > SETTLE) {
    /* image still not decoded: never animate it, so it can never look stuck */
    el.setAttribute('data-km-anim', 'skipped');
   } else {
    later();
   }
  }
 }

 /* ------------------------------------------------ accent sweep on a route */
 var lastSweep = 0;

 function sweep() {
  if (!started) return;
  var now = Date.now();
  if (now - lastSweep < 700) return;
  lastSweep = now;
  var el = document.getElementById('km-sweep-15');
  if (!el) {
   el = document.createElement('div');
   el.id = 'km-sweep-15';
   el.setAttribute('aria-hidden', 'true');
   body().appendChild(el);
  }
  el.classList.remove('km-run');
  void el.offsetWidth;
  el.classList.add('km-run');
 }

 /* ------------------------------------------ browser chrome on the banner */
 function decorate() {
  var spots;
  try { spots = document.querySelectorAll('.hero-slide'); } catch (e) { return; }
  for (var i = 0; i < spots.length; i++) {
   if (spots[i].querySelector('.km-spot-chrome')) continue;
   var bar = document.createElement('div');
   bar.className = 'km-spot-chrome';
   bar.setAttribute('aria-hidden', 'true');
   bar.innerHTML = '<i></i><i></i><i></i><b></b>';
   spots[i].insertBefore(bar, spots[i].firstChild);
  }
 }

 /* ------------------------------------------------- the reel, and its card */
 function closeReel() {
  var open = document.getElementById('km-reel-15');
  if (!open) return;
  var video = open.querySelector('video');
  if (video) { try { video.pause(); } catch (e) {} }
  if (open.parentNode) open.parentNode.removeChild(open);
 }

 function playReel() {
  if (document.getElementById('km-reel-15')) return;
  var el = document.createElement('div');
  el.id = 'km-reel-15';
  el.innerHTML =
   '<video src="intro/animation.mp4" controls autoplay playsinline></video>' +
   '<button type="button" class="km-reel-close">Close</button>';
  el.addEventListener('click', function (ev) {
   if (ev.target === el || ev.target.className === 'km-reel-close') closeReel();
  });
  body().appendChild(el);
  var video = el.querySelector('video');
  if (video) { try { video.play(); } catch (e) {} }
  try {
   window.addEventListener('keydown', function onEsc(ev) {
    if (ev.key !== 'Escape') return;
    window.removeEventListener('keydown', onEsc);
    closeReel();
   });
  } catch (e) {}
 }

 function betaCard() {
  var page = document.querySelector('.profile-page547');
  if (!page || page.querySelector('#km-beta-15')) return;
  var card = document.createElement('div');
  card.className = 'account-section km-beta-card';
  card.id = 'km-beta-15';
  card.innerHTML =
   '<div class="account-section-head"><div>' +
     '<div class="overline km-beta-head"><span class="km-beta-dots"><i></i><i></i><i></i></span>' +
     'Motion \u00b7 beta</div>' +
     '<h2>The reel, and the motion cut from it</h2>' +
     '<p>Cropped type, the browser frame, the poster stack and the ticker \u2014 ' +
     'that is the launch film in this build, and the spring every card uses.</p>' +
   '</div></div>' +
   '<div class="km-beta-row"><button type="button" class="km-beta-play">\u25b6 Play the reel</button></div>' +
   '<p class="km-beta-note">The clip is the reference; the film above is Kuro\u2019s own, ' +
   'built from classes so it works under the app\u2019s CSP and re-tints with your accent. ' +
   'Beta build only.</p>';
  card.querySelector('.km-beta-play').addEventListener('click', playReel);
  var art = page.querySelector('#profile-art581');
  if (art && art.parentNode === page) page.insertBefore(card, art);
  else page.appendChild(card);
 }

 /* --------------------------------------------------------------- boot */
 function finish() {
  if (started) return;
  started = true;
  decorate();
  armAll();
  if (introEl && introEl.parentNode) {
   introEl.classList.add('km-gone');
   var gone = introEl;
   setTimeout(function () {
    if (gone.parentNode) gone.parentNode.removeChild(gone);
    introEl = null;
   }, 200);
  }
  if (window.MutationObserver && document.body) {
   new MutationObserver(function () {
    if (queued) return;
    queued = true;
    setTimeout(function () {
     queued = false;
     armAll();
     decorate();
     betaCard();
    }, 110);
   }).observe(document.body, { childList: true, subtree: true });

   var mainEl = document.getElementById('main') || document.querySelector('main');
   if (mainEl) {
    new MutationObserver(function (records) {
     for (var i = 0; i < records.length; i++) {
      if (records[i].addedNodes && records[i].addedNodes.length) { sweep(); return; }
     }
    }).observe(mainEl, { childList: true });
   }
  }
  betaCard();
  /* cards that were on screen behind the film get their spring now */
  setTimeout(armAll, 60);
}

 function boot() {
  var seen = false;
  try { seen = sessionStorage.getItem(SEEN) === '1'; } catch (e) {}

  if (!reduced && !seen) {
   try { sessionStorage.setItem(SEEN, '1'); } catch (e) {}
   introEl = buildIntro();
   body().appendChild(introEl);
   fillArt();
   setTimeout(fillArt, 220);
   setTimeout(fillArt, 620);

   var skip = function () { finish(); };
   introEl.addEventListener('pointerdown', skip);
   introEl.addEventListener('click', skip);
   window.addEventListener('keydown', function (ev) { if (!started && ev.key) finish(); });
   setTimeout(finish, INTRO_HOLD);
  } else {
   setTimeout(finish, 0);
  }
 }

 if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
 else boot();
})();
