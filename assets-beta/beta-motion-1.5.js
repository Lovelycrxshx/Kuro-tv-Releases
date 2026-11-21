/* Kuro 1.5-beta - motion layer, take 5.
 *
 * Where this ended up, after five cuts: one short launch film, and then a
 * single quiet motion system for the whole app.  Two things are gone on
 * purpose - the tap ripple (it read as water droplets on the glass) and the
 * browser-chrome decoration on the hero banner (it read as a rendering
 * fault).  Effects that draw attention to themselves are not polish.
 *
 * The rules this file follows:
 *
 *  - Nothing real is ever held invisible.  Banners, hero slides and news
 *    posters are never touched.  Grid cards animate once, at insertion, and
 *    only if their poster has already decoded; the hidden state lives inside
 *    the keyframe, so nothing can be stranded by a slow network.
 *  - One easing family (the theme's own --ku-ease-out), short durations,
 *    small distances.  Motion should be felt as the app responding, not as
 *    an effect playing on top of it.
 *  - Everything is class-driven, because the app's CSP forbids inline style
 *    attributes, and everything re-tints from the accent the user picked.
 *  - prefers-reduced-motion removes all of it: no film, no springs.
 *
 * Timeline of the film (~3.6 s, skippable, once per app session):
 *   0.00 dot grid          0.24 browser frame springs up
 *   0.18 pill rail         0.36 ticker band drops in
 *   0.48-0.76 cards fan out (one sweeps across the type)
 *   0.72 "KURO" letter by letter, 1.10 tracked-out "TV"
 *   3.00 iris closes       3.60 overlay gone, cards spring in behind it
 */
(function () {
 'use strict';

 var SEEN = 'kuro-intro-15';
 var INTRO_HOLD = 3600;
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
  if (!slot || !src || slot.getAttribute('data-km-filled') === '1') return;
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

 

/* ------------------------------------------------------- scroll-linked motion
 * One rAF loop, driven by the scroll position.  Cards sit slightly small at
 * the edges of the screen and come up to full size in the middle; banner art
 * drifts inside its frame; headings travel at their own rate; a hairline under
 * the header shows how far down you are; the header condenses once you move.
 *
 * Only transform is written, nothing is hidden, and elements far off screen
 * are skipped entirely, so this stays cheap on a TV box.
 */
var parEls = null;
var parFrame = 0;
var barEl = null;
var scrolled = false;

function collectPar() {
  if (reduced) return;
  var out = [];
  try {
   var sel = '.shelf .poster, .grid .poster, .spotlight > img, .hero-slide img, ' +
             '.home-rail-heading, .heading, .discover-head';
   var list = document.querySelectorAll(sel);
   for (var i = 0; i < list.length && out.length < 90; i++) out.push(list[i]);
  } catch (e) {}
  parEls = out;
}

function paintPar() {
  parFrame = 0;
  if (reduced) return;
  var liteGpu = false;
  try { liteGpu = document.documentElement && document.documentElement.dataset.k154LiteGpu === '1'; } catch (e) {}
  var vh = window.innerHeight || 640;
  var se = document.scrollingElement || document.documentElement;
  var y = window.pageYOffset || se.scrollTop || 0;

  if (barEl) {
   var max = (se.scrollHeight || 0) - vh;
   var p = max > 8 ? Math.min(1, y / max) : 0;
   barEl.style.transform = 'scaleX(' + p.toFixed(4) + ')';
   if (p > 0.004) barEl.classList.remove('km-idle');
   else { barEl.classList.add('km-idle'); barEl.style.transform = 'scaleX(0)'; }
  }

  var nowScrolled = y > 18;
  if (nowScrolled !== scrolled) {
   scrolled = nowScrolled;
   if (document.body) document.body.classList.toggle('km-scrolled', scrolled);
  }

  if (liteGpu || !parEls) return;
  for (var i = 0; i < parEls.length; i++) {
   var el = parEls[i], r;
   try { r = el.getBoundingClientRect(); } catch (e) { continue; }
   if (!r.height) continue;
   if (r.bottom < -140 || r.top > vh + 140) continue;
   var d = ((r.top + r.height / 2) - vh / 2) / (vh / 2);
   if (d < -1) d = -1;
   if (d > 1) d = 1;
   if (el.tagName === 'IMG') {
    el.style.transform = 'translate3d(0,' + (d * 11).toFixed(2) + 'px,0) scale(1.07)';
   } else if (el.classList && el.classList.contains('poster')) {
    /* rails get the reel's focus-scale; grid posters only drift, because a
     * scaled poster next to an unscaled one reads as a broken grid */
    var inRail = el.parentNode && el.parentNode.parentNode &&
      el.parentNode.parentNode.parentNode &&
      el.parentNode.parentNode.parentNode.closest &&
      el.parentNode.parentNode.parentNode.closest('.shelf');
    if (inRail) {
     el.style.transform = 'translate3d(0,' + (d * 4).toFixed(2) + 'px,0) scale(' +
       (1 - Math.abs(d) * 0.05).toFixed(3) + ')';
    } else {
     el.style.transform = 'translate3d(0,' + (d * 3).toFixed(2) + 'px,0)';
    }
   } else {
    el.style.transform = 'translate3d(0,' + (d * -13).toFixed(2) + 'px,0)';
   }
  }
}

function queuePar() {
  if (parFrame) return;
  parFrame = window.requestAnimationFrame ? window.requestAnimationFrame(paintPar)
                                          : setTimeout(paintPar, 16);
}

function bindScroll() {
  if (reduced) return;
  if (!barEl && document.body) {
   barEl = document.createElement('div');
   barEl.id = 'km-scrollbar-15';
   barEl.className = 'km-idle';
   barEl.setAttribute('aria-hidden', 'true');
   document.body.appendChild(barEl);
  }
  collectPar();
  window.addEventListener('scroll', queuePar, { passive: true });
  window.addEventListener('resize', function () { collectPar(); queuePar(); });
  queuePar();
}

/* ------------------------------------------------------- update button ---
 * One entry point: the round arrow in the header, on every screen. It runs the
 * shell's real updater - check now, download in the background, then Android's
 * installer. The shell owns all of that; this only asks.
 */
function updateBridge() {
 try {
  if (window.KuroNative && typeof window.KuroNative.updateButton15 === 'function') {
   window.KuroNative.updateButton15();
   return true;
  }
  /* older shell without the shortcut: open the update screen instead */
  if (window.KuroNative && typeof window.KuroNative.settingsAction560 === 'function') {
   window.KuroNative.settingsAction560('updates572');
   return true;
  }
 } catch (e) {}
 return false;
}

function updateButton() {
 var actions = document.querySelector('.header-actions');
 if (!actions || actions.querySelector('#km-update-15')) return;
 var b = document.createElement('button');
 b.type = 'button';
 b.id = 'km-update-15';
 b.className = 'header-icon km-update-btn';
 b.setAttribute('aria-label', 'Check for updates');
 b.title = 'Check for updates';
 b.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true">' +
  '<path d="M12 4v9m0 0 3.4-3.4M12 13 8.6 9.6M5 16.5h14M5 19.8h14"/></svg>';
 b.addEventListener('click', function () {
  if (updateBridge()) return;
  b.classList.add('km-update-off');
  setTimeout(function () { b.classList.remove('km-update-off'); }, 600);
 });
 var login = actions.querySelector('#header-login');
 if (login && login.parentNode === actions) actions.insertBefore(b, login);
 else actions.appendChild(b);
}

/* --------------------------------------------- removed Account cards ---
 * 1.5-beta drops the "Private profile / LOCAL" card from Account: it restated
 * what the page already says under the avatar, and it was the one card the user
 * asked to lose. The CSS hides it before first paint; this also takes it out of
 * the DOM so nothing can tab to it.
 */
function dropPrivateCard() {
 var main = document.getElementById('main');
 if (!main) return;
 var cards = main.querySelectorAll('.profile-private547');
 for (var i = 0; i < cards.length; i++) {
  if (cards[i].parentNode) cards[i].parentNode.removeChild(cards[i]);
 }
}

/* --------------------------------------------------------------- boot */
 function finish() {
  if (started) return;
  started = true;
  bindScroll();
  updateButton();
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
     dropPrivateCard();
     updateButton();
     collectPar();
     queuePar();
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
  dropPrivateCard();
  updateButton();
  /* cards that were on screen behind the film get their spring now */
  setTimeout(armAll, 60);
}

 function boot() {
  var seen = false;
  var fastBoot = false;
  try { seen = sessionStorage.getItem(SEEN) === '1'; } catch (e) {}
  try {
   var rawPerf = localStorage.getItem('kuro-perf-154');
   if (rawPerf && JSON.parse(rawPerf).fastBoot === true) fastBoot = true;
   else if (window.KuroNative && typeof window.KuroNative.perfGet154 === 'function') {
    var nativePerf = JSON.parse(window.KuroNative.perfGet154() || '{}');
    if (nativePerf && nativePerf.fastBoot === true) fastBoot = true;
   }
  } catch (e) {}

  if (!reduced && !seen && !fastBoot) {
   try { sessionStorage.setItem(SEEN, '1'); } catch (e) {}
   introEl = buildIntro();
   body().appendChild(introEl);
   fillArt();
   setTimeout(fillArt, 220);
   setTimeout(fillArt, 620);
   setTimeout(fillArt, 1150);
   setTimeout(fillArt, 1800);

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
