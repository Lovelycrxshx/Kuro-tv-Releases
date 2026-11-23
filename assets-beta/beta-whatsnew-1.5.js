/* Kuro 1.5.5-beta7 — one-time "What's new" card, shown once after the splash on
   first launch of 1.5.5-beta7 and never again. Self-contained, dismissible by
   button, backdrop tap, or Escape / Android back. */
(function () {
  'use strict';

  var STAMP = '1.5.5-beta7';
  var KEY = 'kuro-beta-whatsnew-seen-1.5.5-beta7';

  try {
    if (localStorage.getItem(KEY) === STAMP) {
      return;
    }
  } catch (_) { return; }

  var ITEMS = [
    ['Auto-Play Next Restored & Crash Fixed', 'Fixed the dialog window-token crash and restored end-to-end Auto-Play Next across embedded web servers, direct HLS streams, and the native player, with SUB/DUB preference carried forward automatically.'],
    ['Offline Downloads & Batch Queue Stability', 'Fixed background DownloadManager thread-affinity crashes and repaired batch offline download IDs and resume checkpoints so single and batch episodes download and play offline reliably.'],
    ['Dialog & Thread-Safety Hardening', 'Added Activity lifecycle and BadTokenException protection across every dialog, bottom sheet, and updater prompt, plus lock-free multi-threaded theme palette lookups.'],
    ['Accurate Episode Progress & Android Back', 'Fixed episode progress badges, Discord Rich Presence timestamps, Episode 0 / .5 playback, and Android Back dismissal across batch sheets, source pickers, and overlays.'],
    ['System Resolution + 500 DPI & 20-Theme OLED', 'Keeps native system resolution with crisp 500 DPI scaling, full 20-accent + Pure Black OLED sync, and built-in Encrypted DNS (1.1.1.1 / 8.8.8.8).']
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
    tag.textContent = 'KURO.TV  \u00b7  1.5.5-BETA7';

    var h = document.createElement('h2');
    h.id = 'kb-wn-title';
    h.className = 'kb-wn-title';
    h.textContent = 'What\u2019s new in 1.5.5-beta7';

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
