/* Kuro 1.5.5-beta8 — one-time "What's new" card, shown once after the splash on
   first launch of 1.5.5-beta8 and never again. Self-contained, dismissible by
   button, backdrop tap, or Escape / Android back. */
(function () {
  'use strict';

  var STAMP = '1.5.5-beta8';
  var KEY = 'kuro-beta-whatsnew-seen-1.5.5-beta8';

  try {
    if (localStorage.getItem(KEY) === STAMP) {
      return;
    }
  } catch (_) { return; }

  var ITEMS = [
    ['App-Themed Up Next Card', 'Replaced the stock Android alert popup with a sleek Kuro-themed Up Next card featuring episode artwork, title & episode count, smooth live accent countdown bar, tap-to-hold pause, and themed Play Now / Cancel controls across all 20 accents and Pure Black OLED.'],
    ['Controllable App DPI & UI Size', 'Full control over App DPI (System Default or 280\u2013640 DPI) and App UI Size (75%\u2013135%), plus Up Next card size and countdown duration (5s\u201315s) right in Settings and Watch Tools.'],
    ['Instant Preview & Fast Density Apply', 'Preview the themed Up Next card directly from Settings or Watch Tools, and apply new App DPI & UI Size across native and WebView surfaces without replaying the launch intro.'],
    ['Auto-Play Next & Offline Stability', 'Reliable end-to-end Auto-Play Next across embedded web servers, HLS, and native playback with SUB/DUB carry-over, safe dialog tokens, and main-looper DownloadManager protection.']
  ];

  function mark() {
    try { localStorage.setItem(KEY, STAMP); } catch (_) {}
  }

  function show() {
    if (document.getElementById('kb-whatsnew') || document.getElementById('settings-overlay560')) return;

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
    tag.textContent = 'KURO.TV  \u00b7  1.5.5-BETA8';

    var h = document.createElement('h2');
    h.id = 'kb-wn-title';
    h.className = 'kb-wn-title';
    h.textContent = 'What\u2019s new in 1.5.5-beta8';

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
