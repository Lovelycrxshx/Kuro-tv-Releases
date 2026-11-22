/* Kuro 1.5.5-beta — one-time "What's new" card, shown once after the splash on
   first launch of 1.5.5-beta and never again. Self-contained, dismissible by
   button, backdrop tap, or Escape / Android back. */
(function () {
  'use strict';

  var STAMP = '1.5.5-beta';
  var KEY = 'kuro-beta-whatsnew-seen-1.5.5-beta';

  try { if (localStorage.getItem(KEY) === STAMP) return; } catch (_) { return; }

  var ITEMS = [
    ['Classic Video Player Restored', 'Restored the clean original video player controls across embedded servers, HLS streams, native playback, and offline downloads — removing the oversized purple pill rings and floating circle artefacts.'],
    ['Signature Violet Theme & Faster Loading', 'Default app colour set to Signature Violet across all screens & dialogs, plus built-in Encrypted DNS (1.1.1.1 / 8.8.8.8) to unblock carrier-restricted sources without a VPN, faster 8-lane catalog loading, backup video codec support, low-RAM crash protection, and Android TV / Fire TV / foldable support.'],
    ['Cinematic Title Detail v2', 'Full-bleed banner header with ★ score, studio & runtime pills, interactive Franchise & Watch-Order strip, Canon / Filler / Recap badges, Skip Filler switch (also skips fillers on Auto-Next), 16:9 episode rows, and ⬇ Batch Offline Download.'],
    ['Desktop-Site Left-Rail & DPAD UI', 'Turn on Settings → Site view → Desktop (or launch on Android TV / Fire TV) to unlock the frosted-OLED left navigation rail, widescreen hero & multi-column shelves, glowing accent DPAD focus halo, and remote spatial navigation.'],
    ['Smart Multi-Filter Browse & Voice Search', 'Combinable OLED filter pills on Browse (Dub Available, ★ 8.0+, Airing/Completed, Genre, Format, Year), native Android Voice Search mic pill, and compact SUB | DUB poster pills.'],
    ['Streamlined YouTube-Style Watch Page', 'Compact 5-servers-per-page layout with an "Other servers" page switcher, 3-row scrollable YouTube-style episode playlist card, YouTube Desktop left-to-right horizontal recommended anime shelf right below Anime details, and verified developer Discord profile link.']
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
    tag.textContent = 'KURO.TV  \u00b7  1.5.5-BETA';

    var h = document.createElement('h2');
    h.id = 'kb-wn-title';
    h.className = 'kb-wn-title';
    h.textContent = 'What\u2019s new in 1.5.5-beta';

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
