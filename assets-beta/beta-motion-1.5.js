/* Kuro 1.5-beta - motion layer, take 2.
 *
 * Take 1 was rings and fades.  This one is built from what animation.mp4
 * actually does, beat for beat, using Kuro's own posters and the accent the
 * user picked:
 *
 *   0.00  dot grid drifts in
 *   0.16  the thin pill rail slides in, one pill at a time
 *   0.18  the browser frame springs up from the bottom (traffic lights, URL pill)
 *   0.30  the slanted accent ticker band drops in and starts running
 *   0.34  three poster cards spring in around the frame and lap over its edge
 *   0.42  the accent connector curve draws itself
 *   0.50  "KURO" rises letter by letter, then the tracked-out "TV"
 *   2.14  iris wipe out through the middle
 *   2.78  overlay gone, the home cards spring in behind it
 *
 * Then, for the rest of the session: the same card spring on scroll, a squash
 * on press, a browser-chrome band on the hero, and an accent sweep on every
 * screen change.
 *
 * The app's CSP forbids inline style attributes, so every visual state here is
 * a class.  Nothing is hidden until this file has armed the page, and anything
 * armed is force-revealed after 1.6 s, so a failure can never leave a blank
 * shelf.  Reduced-motion users never see the intro and get no reveals at all.
 */
(function () {
 'use strict';

 var SEEN = 'kuro-intro-15';
 var INTRO_HOLD = 2800;          /* the iris closes at 2.78 s */
 var FORCE_AFTER = 1600;         /* safety: never stay invisible */
 var REVEAL = '.poster, .hero-slide, .card, .history-card, .news-poster';

 var reduced = false;
 try {
  reduced = !!(window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches);
 } catch (e) {}

 var introEl = null;
 var started = false;
 var observer = null;
 var seq = 0;

 /* ------------------------------------------------------------- utilities */
 function body() { return document.body || document.documentElement; }

 function must(html) {
  var d = document.createElement('div');
  d.innerHTML = html;
  return d.firstChild;
 }

 /* Posters already on screen.  The intro borrows the user's own library art,
  * which also keeps third-party reel artwork out of the shipped build. */
 function posterList(n) {
  var out = [];
  var seen = {};
  var nodes;
  try {
   nodes = document.querySelectorAll('.shelf .poster img, .grid .poster img, .poster img, .spotlight>img');
  } catch (e) { nodes = []; }
  for (var i = 0; i < nodes.length && out.length < n; i++) {
   var src = nodes[i].currentSrc || nodes[i].getAttribute('src') || '';
   if (!src || seen[src]) continue;
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
  var art = introEl.querySelector('.km-art');
  var c1 = introEl.querySelector('.km-c1');
  var c2 = introEl.querySelector('.km-c2');
  var c3 = introEl.querySelector('.km-c3');
  if (p.length) putImage(art, p[0]);
  if (p.length) putImage(c1, p[1] || p[0]);
  if (p.length) putImage(c2, p[2] || p[1] || p[0]);
  if (p.length) putImage(c3, p[3] || p[2] || p[0]);
  ["art", "c1", "c2", "c3"].forEach(function (k) {
   var node = introEl.querySelector('.' + (k === 'art' ? 'km-art' : 'km-' + k));
   if (node && !node.getAttribute('data-km-filled')) {
    node.classList.add('km-empty');
    node.setAttribute('data-km-filled', 'empty');
   }
  });
 }

 /* ------------------------------------------------------- the launch film */
 var TICKER = 'Kuro \u00b7 Anime \u00b7 No ads \u00b7 Your next story awaits \u00b7 ';

 function buildIntro() {
  var html = '' +
   '<div class="km-dots"></div>' +
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

  var el = document.createElement('div');
  el.id = 'km-intro-15';
  el.setAttribute('aria-hidden', 'true');
  el.innerHTML = html;
  return el;
 }

 /* --------------------------------------------------------- scroll reveals */
 function arm(el) {
  if (!el || el.classList.contains('km-reveal')) return;
  if (el.classList.contains('poster') && el.closest &&
      el.closest('.card, .history-card')) return;   /* the card springs, not both */
  var r;
  try { r = el.getBoundingClientRect(); } catch (e) { r = { width: 0, height: 0 }; }
  if (!r || r.height < 6 || r.width < 6) return;      /* not laid out yet: leave it alone */
  el.classList.add('km-reveal', 'km-d' + ((seq++ % 8) + 1));
  el.setAttribute('data-km-at', String(Date.now()));
  if (observer) observer.observe(el);
  else el.classList.add('km-in');
 }

 function armAll() {
  var nodes;
  try { nodes = document.querySelectorAll(REVEAL); } catch (e) { return; }
  for (var i = 0; i < nodes.length; i++) arm(nodes[i]);
 }

 function watch() {
  if (!('IntersectionObserver' in window)) { armAll(); return; }
  observer = new IntersectionObserver(function (entries) {
   for (var i = 0; i < entries.length; i++) {
    var entry = entries[i];
    if (!entry.isIntersecting) continue;
    entry.target.classList.add('km-in');
    try { observer.unobserve(entry.target); } catch (e) {}
   }
  }, { rootMargin: '0px 0px -6% 0px', threshold: 0.06 });
  armAll();

  /* Nothing may stay invisible: anything armed for longer than FORCE_AFTER
   * gets revealed whether or not the observer ever saw it. */
  setInterval(function () {
   var now = Date.now();
   var left;
   try { left = document.querySelectorAll('.km-reveal:not(.km-in)'); } catch (e) { return; }
   for (var i = 0; i < left.length; i++) {
    var at = Number(left[i].getAttribute('data-km-at') || 0);
    if (!at || now - at > FORCE_AFTER) left[i].classList.add('km-in');
   }
  }, 700);
 }

 /* ------------------------------------------------ accent sweep on a route change */
 var lastSweep = 0;

 function sweep() {
  var now = Date.now();
  if (now - lastSweep < 380) return;
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

 /* ------------------------------------------ browser chrome on the hero card */
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
     '<p>Browser frame, stacked posters, kinetic type, the ticker and the iris \u2014 ' +
     'that is what this build now does on launch and on every screen.</p>' +
   '</div></div>' +
   '<div class="km-beta-row"><button type="button" class="km-beta-play">\u25b6 Play the reel</button></div>' +
   '<p class="km-beta-note">The clip is the reference; everything above is Kuro\u2019s own, ' +
   'built from classes so it works under the app\u2019s CSP and re-tints with your accent. ' +
   'Beta build only.</p>';
  card.querySelector('.km-beta-play').addEventListener('click', playReel);
  var art = page.querySelector('#profile-art581');
  if (art && art.parentNode === page) page.insertBefore(card, art);
  else page.appendChild(card);
 }

 /* --------------------------------------------------------------- the boot */
 function finish() {
  if (started) return;
  started = true;
  try { body().classList.add('km-armed'); } catch (e) {}
  watch();
  decorate();
  if (introEl && introEl.parentNode) {
   introEl.classList.add('km-gone');
   var gone = introEl;
   setTimeout(function () {
    if (gone.parentNode) gone.parentNode.removeChild(gone);
    introEl = null;
   }, 220);
  }
  if (window.MutationObserver && document.body) {
   new MutationObserver(function () {
    if (!started) return;
    if (queued) return;
    queued = true;
    setTimeout(function () {
     queued = false;
     armAll();
     decorate();
     betaCard();
    }, 90);
   }).observe(document.body, { childList: true, subtree: true });
   var mainEl = document.getElementById('main') || document.querySelector('main');
   if (mainEl) new MutationObserver(function () { sweep(); }).observe(mainEl, { childList: true });
  }
  betaCard();
 }

 var queued = false;

 function boot() {
  var seen = false;
  try { seen = sessionStorage.getItem(SEEN) === '1'; } catch (e) {}

  if (!reduced && !seen) {
   try { sessionStorage.setItem(SEEN, '1'); } catch (e) {}
   introEl = buildIntro();
   body().appendChild(introEl);
   fillArt();
   setTimeout(fillArt, 240);
   setTimeout(fillArt, 700);

   var skip = function () { finish(); };
   introEl.addEventListener('pointerdown', skip);
   introEl.addEventListener('click', skip);
   window.addEventListener('keydown', function (ev) {
    if (!started) { if (ev.key) finish(); }
   });
   setTimeout(finish, INTRO_HOLD);
  } else {
   setTimeout(finish, 0);
  }
 }

 if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
 else boot();
})();
