/* Kuro 1.5.5-beta5 — one-time "What's new" card, shown once after the splash on
   first launch of 1.5.5-beta5 and never again. Self-contained, dismissible by
   button, backdrop tap, or Escape / Android back. */
(function () {
  'use strict';

  var STAMP = '1.5.5-beta5';
  var KEY = 'kuro-beta-whatsnew-seen-1.5.5-beta5';

  try {
    if (localStorage.getItem(KEY) === STAMP || localStorage.getItem('kuro-beta-whatsnew-seen-1.5.5-beta4')) {
      localStorage.setItem(KEY, STAMP);
      return;
    }
  } catch (_) { return; }

  var ITEMS = [
    ['2K Render Resolution & Forced 500 DPI', 'App-wide 2K QHD (2560\u00d71440 / 1440\u00d72560) rendering and forced 500 DPI (3.125\u00d7 density) across native screens, dialogs, WebView UI, ExoPlayer 2K track selection, WebGL2 video shaders, and high-DPI artwork.'],
    ['Smooth Native Home Scrolling Restored', 'Fixed the Home screen scroll lock so vertical touch scrolling, fling momentum, wheel scrolling, and horizontal shelf swipes work effortlessly from the first touch while opening cleanly at the top.'],
    ['Full 20-Theme & Pure Black OLED Sync', 'Every Watch Screen v3 card, border, SUB/DUB tab, active server pill, episode progress bar, badge, WatchHeader kuro.tv wordmark, and Title Detail / Browse overlay dynamically syncs across all 20 accents and Pure Black OLED mode.'],
    ['Watch Screen v3 Clarity Upgrade', '1-tap < Prev \u2022 Episode \u2022 Next > + Auto-Next bar right below the player, compact SUB | DUB segmented Server Switcher with horizontal server pills, and 3-row Episode Playlist with 16:9 thumbnails and progress bars.'],
    ['Classic Video Player & Encrypted DNS', 'Clean original video player controls across embedded servers, HLS streams, native playback, and offline downloads, plus built-in Encrypted DNS (1.1.1.1 / 8.8.8.8) to unblock carrier-restricted sources without a VPN.'],
    ['Cinematic Title Detail v2 & Voice Search', 'Full-bleed banner header with \u2605 score, Franchise & Watch-Order strip, Canon / Filler / Recap badges, Skip Filler switch, Batch Offline Download, and native Android Voice Search.']
  ];

  function mark() {
    try { localStorage.setItem(KEY, STAMP); } catch (_) {}
  }

  function show() {
    if (document.getElementById('kb-whatsnew')) return;

    var scrim = document.createElement('div');
    scrim.id = 'kb-whatsnew';
    scrim.className = 'kb-wn-scrim';
    scrim.setAttribute('role', 'dialog');
    scrim.setAttribute('aria-modal', 'true');
    scrim.setAttribute('aria-labelledby', 'kb-wn-title');

    var card = document.createElement('div');
    card.className = 'kb-wn-card';

    var tag = document.createElement('div');
    tag.className = 'kb-wn-tag';
    tag.textContent = 'KURO.TV  \u00b7  1.5.5-BETA5';

    var h = document.createElement('h2');
    h.id = 'kb-wn-title';
    h.className = 'kb-wn-title';
    h.textContent = 'What\u2019s new in 1.5.5-beta5';

    var list = document.createElement('ul');
    list.className = 'kb-wn-list';
    for (var i = 0; i < ITEMS.length; i++) {
      var li = document.createElement('li');
      var b = document.createElement('strong');
      b.textContent = ITEMS[i][0];
      var s = document.createElement('span');
      s.textContent = ITEMS[i][1];
      li.appendChild(b);
      li.appendChild(s);
      list.appendChild(li);
    }

    var btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'kb-wn-ok';
    btn.textContent = 'Got it';

    function close() {
      mark();
      scrim.classList.add('is-out');
      document.removeEventListener('keydown', onKey, true);
      setTimeout(function () { if (scrim.parentNode) scrim.parentNode.removeChild(scrim); }, 220);
    }
    function onKey(e) {
      if (e.key === 'Escape' || e.key === 'Enter') { e.preventDefault(); e.stopPropagation(); close(); }
    }

    btn.addEventListener('click', close);
    scrim.addEventListener('click', function (e) { if (e.target === scrim) close(); });
    document.addEventListener('keydown', onKey, true);

    card.appendChild(tag);
    card.appendChild(h);
    card.appendChild(list);
    card.appendChild(btn);
    scrim.appendChild(card);
    document.body.appendChild(scrim);
    setTimeout(function () {
      try { btn.focus({ preventScroll: true }); } catch (_) { try { btn.focus(); } catch (__) {} }
    }, 60);
  }

  function waitForIntro() {
    var tries = 0;
    var t = setInterval(function () {
      tries++;
      var intro = document.getElementById('km-intro-15') || document.getElementById('kuro-beta-intro');
      if ((!intro && tries > 3) || tries > 30) {
        clearInterval(t);
        setTimeout(show, 380);
      }
    }, 250);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', waitForIntro);
  } else {
    waitForIntro();
  }
})();
