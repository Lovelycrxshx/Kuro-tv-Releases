/* Kuro 1.5-beta - motion layer.
 *
 * Three pieces, all re-created from the animation.mp4 reel:
 *   1. a title sequence on launch (accent ring sweep, wordmark, three cards
 *      flying in), skipped by a tap or a key press, once per app session;
 *   2. staggered reveals as cards scroll into view (opacity + lift + scale,
 *      the same easing family the reel uses);
 *   3. a "Motion" card in Account that plays the reel itself, for comparison.
 *
 * The shell must serve this file and its stylesheet: tools/webview_assets_1.3.py
 * builds that list from the overlay directory, so nothing has to be hand-listed
 * in MainActivity.
 */
(function () {
 'use strict';
 var SEEN = 'kuro-intro-15';
 var REVEAL = '.poster, .hero-slide, .card, .history-card, .news-poster';
 var reduced = false;
 try {
  reduced = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
 } catch (e) {}

 /* ---------------- 1. title sequence ---------------- */
 function skip(intro) {
  if (!intro || intro.dataset.kdone) return;
  intro.dataset.kdone = '1';
  intro.classList.add('km-gone');
  if (intro.parentNode) intro.parentNode.removeChild(intro);
 }

 function intro() {
  if (reduced) return;
  var seen = false;
  try { seen = sessionStorage.getItem(SEEN) === '1'; } catch (e) {}
  if (seen) return;
  try { sessionStorage.setItem(SEEN, '1'); } catch (e) {}

  var el = document.createElement('div');
  el.id = 'km-intro-15';
  el.setAttribute('aria-hidden', 'true');
  el.innerHTML =
   '<span class="km-ring"></span><span class="km-ring km-r2"></span>' +
   '<span class="km-ghost km-g1"></span><span class="km-ghost km-g2"></span>' +
   '<span class="km-ghost km-g3"></span>' +
   '<div class="km-word">' +
   '<span>K</span><span>U</span><span>R</span><span>O</span><span>.</span>' +
   '<span>T</span><span>V</span>' +
   '</div>' +
   '<span class="km-skip">TAP TO SKIP</span>';
  el.addEventListener('click', function () { skip(el); });
  (document.body || document.documentElement).appendChild(el);
  setTimeout(function () { skip(el); }, 2500);
  try {
   window.addEventListener('keydown', function once() {
    window.removeEventListener('keydown', once);
    skip(el);
   });
  } catch (e) {}
 }

 /* ---------------- 2. staggered reveals ---------------- */
 var seq = 0;

 function arm(el) {
  if (!el || el.classList.contains('km-reveal')) return;
  el.classList.add('km-reveal');
  var step = (seq++ % 8) + 1;
  el.classList.add('km-d' + step);
  if (observer) observer.observe(el);
  else el.classList.add('km-in');
 }

 var observer = null;

 function armAll(root) {
  var nodes = (root || document).querySelectorAll(REVEAL);
  Array.prototype.forEach.call(nodes, arm);
}

 function watch() {
  if (!window.IntersectionObserver) {
   armAll();
   Array.prototype.forEach.call(document.querySelectorAll(REVEAL), function (el) {
    el.classList.add('km-in');
   });
   return;
  }
  observer = new IntersectionObserver(function (entries) {
   entries.forEach(function (entry) {
    if (!entry.isIntersecting) return;
    entry.target.classList.add('km-in');
    observer.unobserve(entry.target);
   });
  }, { rootMargin: '0px 0px -8% 0px', threshold: 0.08 });
  armAll();
 }

 /* ---------------- 3. the reel, and its card in Account ---------------- */
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
  try {
   window.addEventListener('keydown', function onEsc(ev) {
    if (ev.key !== 'Escape') return;
    window.removeEventListener('keydown', onEsc);
    closeReel();
   });
  } catch (e) {}
  document.body.appendChild(el);
  var video = el.querySelector('video');
  if (video) { try { video.play(); } catch (e) {} }
}

 function betaCard() {
  var page = document.querySelector('.profile-page547');
  if (!page || page.querySelector('#km-beta-15')) return;
  var card = document.createElement('div');
  card.className = 'account-section km-beta-card';
  card.id = 'km-beta-15';
  card.innerHTML =
   '<div class="account-section-head"><div><h2>Motion (beta)</h2>' +
   '<p>The reel this build was cut from, plus the reveals and press spring it inspired.</p>' +
   '</div></div>' +
   '<div class="km-beta-row"><button type="button" class="km-beta-play">\u25b6 Play the reel</button></div>' +
   '<p class="km-beta-note">Beta build only. The clip is the reference the motion layer was matched ' +
   'against; the reveals and the launch sequence above are Kuro\u2019s own, built from classes so ' +
   'they work under the app\u2019s CSP.</p>';
  card.querySelector('.km-beta-play').addEventListener('click', playReel);
  var art = page.querySelector('#profile-art581');
  if (art && art.parentNode === page) page.insertBefore(card, art);
  else page.appendChild(card);
 }

 /* ---------------- boot ---------------- */
 function boot() {
  document.body.classList.add('km-armed');
  intro();
  watch();
  betaCard();
  if (window.MutationObserver && document.body) {
   var queued = false;
   new MutationObserver(function () {
    if (queued) return;
    queued = true;
    setTimeout(function () {
     queued = false;
     armAll();
     betaCard();
    }, 60);
   }).observe(document.body, { childList: true, subtree: true });
  }
 }

 if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
 else boot();
})();
