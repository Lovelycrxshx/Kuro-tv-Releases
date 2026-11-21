/* Kuro 1.5.3 — one-time "What's new" card, shown once after the splash on
   first launch of 1.5.3 and never again. Self-contained, dismissible by
   button, backdrop tap, or Escape / Android back. */
(function () {
  'use strict';

  var STAMP = '1.5.3';
  var KEY = 'kuro-beta-whatsnew-seen-1.5.3-player';

  try { if (localStorage.getItem(KEY) === STAMP) return; } catch (_) { return; }

  var ITEMS = [
    ['OLED Segmented Player (All Servers)', 'Pure black gradient fade, gapped chapter timeline with glowing vertical scrubber pill, OLED pill-chip controls, and live accent + Pure Black OLED sync across all embedded server players, Web HLS, and native ExoPlayer.'],
    ['Fullscreen Swipe-Up for Next Episode', 'Drag or swipe up from the bottom while in fullscreen to pull up the themed Up Next & Episodes drawer and switch episodes without leaving fullscreen.'],
    ['App-UI Auto-Play Next Episode', 'Redesigned floating OLED Up-Next card with episode number & title, smooth 10-second accent progress bar, Play Next & Cancel pills, and embedded server end detection.'],
    ['Direct GitHub APK Downloads', 'Release page, Discord Rich Presence Download button, and docs link directly to the signed Kuro-tv-1.5.3.apk on GitHub Releases.'],
    ['20 Accents + Pure Black OLED', 'Full theme palette across WebView, native dialogs, and every video player, with instant Backup & Restore checkpoints.']
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
    tag.textContent = 'KURO.TV  \u00b7  1.5.3';

    var h = document.createElement('h2');
    h.id = 'kb-wn-title';
    h.className = 'kb-wn-title';
    h.textContent = 'What\u2019s new in 1.5.3';

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
    setTimeout(function () { try { btn.focus(); } catch (_) {} }, 60);
  }

  function waitForIntro() {
    var tries = 0;
    var t = setInterval(function () {
      tries++;
      var intro = document.getElementById('kuro-beta-intro');
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
