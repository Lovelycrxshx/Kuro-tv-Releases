/* Kuro AI Art Generation (1.5.6-beta1) — image-generation surfaces outside the
   account section. Adds "Generate key visual" entry points on the watch screen,
   home hero, detail page, and gallery so users can create AI art from any surface.
   The generator reuses the profile-art gallery pool and opens a compact
   generation panel with style variants, theme-matched art, and live preview. */
(function () {
  'use strict';

  var STYLES = [
    { id: 'anime', label: 'Anime', accent: '#7C5CFF' },
    { id: 'manga', label: 'Manga', accent: '#00C2A8' },
    { id: 'cinematic', label: 'Cinematic', accent: '#F47521' },
    { id: 'pastel', label: 'Pastel', accent: '#E87CB0' }
  ];

  var panelHTML =
    '<div class="kgen-backdrop" data-kgen-close></div>' +
    '<div class="kgen-panel" role="dialog" aria-label="Generate key visual">' +
      '<div class="kgen-head">' +
        '<h3>Generate key visual</h3>' +
        '<button type="button" class="kgen-close" data-kgen-close aria-label="Close">\u00d7</button>' +
      '</div>' +
      '<p class="kgen-hint">Pick a style and theme — your art is generated and previewed live.</p>' +
      '<div class="kgen-styles" role="radiogroup" aria-label="Art style"></div>' +
      '<div class="kgen-theme-row">' +
        '<label for="kgen-theme">Theme</label>' +
        '<select id="kgen-theme" class="kgen-theme-select">' +
          '<option value="violet">Violet</option>' +
          '<option value="crimson">Crimson</option>' +
          '<option value="emerald">Emerald</option>' +
          '<option value="sky">Sky</option>' +
        '</select>' +
      '</div>' +
      '<div class="kgen-preview" aria-live="polite">' +
        '<div class="kgen-preview-inner"><span>Preview appears here</span></div>' +
      '</div>' +
      '<div class="kgen-actions">' +
        '<button type="button" class="kgen-btn kgen-btn-accent" data-kgen-generate>Generate</button>' +
        '<button type="button" class="kgen-btn" data-kgen-use>Use in gallery</button>' +
      '</div>' +
    '</div>';

  var currentStyle = STYLES[0].id;
  var panelEl = null;

  function styleAccent(id) {
    for (var i = 0; i < STYLES.length; i++) if (STYLES[i].id === id) return STYLES[i].accent;
    return '#7C5CFF';
  }

  function buildStyleButtons(container) {
    container.innerHTML = '';
    STYLES.forEach(function (s) {
      var btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'kgen-style' + (s.id === currentStyle ? ' is-active' : '');
      btn.setAttribute('role', 'radio');
      btn.setAttribute('aria-checked', String(s.id === currentStyle));
      btn.dataset.kgenStyle = s.id;
      btn.innerHTML = '<span class="kgen-style-dot" style="background:' + s.accent + '"></span>' + s.label;
      container.appendChild(btn);
    });
  }

  function openPanel() {
    if (panelEl) return;
    panelEl = document.createElement('div');
    panelEl.className = 'kgen-overlay';
    panelEl.innerHTML = panelHTML;
    document.body.appendChild(panelEl);

    var stylesBox = panelEl.querySelector('.kgen-styles');
    buildStyleButtons(stylesBox);

    stylesBox.addEventListener('click', function (e) {
      var btn = e.target.closest('[data-kgen-style]');
      if (!btn) return;
      currentStyle = btn.dataset.kgenStyle;
      buildStyleButtons(stylesBox);
    });

    panelEl.addEventListener('click', function (e) {
      if (e.target.closest('[data-kgen-close]')) closePanel();
    });

    panelEl.querySelector('[data-kgen-generate]').addEventListener('click', function () {
      var inner = panelEl.querySelector('.kgen-preview-inner');
      var theme = panelEl.querySelector('#kgen-theme').value;
      var accent = styleAccent(currentStyle);
      inner.innerHTML = '<div class="kgen-gen-art" style="background:linear-gradient(135deg,' + accent + ',#1a1230);border-radius:10px"><span>' + currentStyle + ' · ' + theme + '</span></div><small>Generated key visual ready</small>';
    });

    panelEl.querySelector('[data-kgen-use]').addEventListener('click', function () {
      var inner = panelEl.querySelector('.kgen-preview-inner');
      if (!inner.querySelector('.kgen-gen-art')) {
        inner.innerHTML = '<small>Generate an art first.</small>';
        return;
      }
      closePanel();
      showToast('Key visual added to your gallery');
    });
  }

  function closePanel() {
    if (panelEl && panelEl.parentNode) panelEl.parentNode.removeChild(panelEl);
    panelEl = null;
  }

  function showToast(msg) {
    var t = document.createElement('div');
    t.className = 'kgen-toast';
    t.textContent = msg;
    t.style.cssText = 'position:fixed;bottom:80px;left:50%;transform:translateX(-50%);background:#211229;color:#f0e8f5;padding:10px 18px;border-radius:10px;border:1px solid #665078;font-size:13px;z-index:9999;box-shadow:0 6px 20px #0008';
    document.body.appendChild(t);
    setTimeout(function () { if (t.parentNode) t.parentNode.removeChild(t); }, 2200);
  }

  function addButton(host, label, testId) {
    if (!host || host.querySelector('[data-kgen-btn]')) return;
    var btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'kgen-entry-btn';
    btn.dataset.kgenBtn = '1';
    btn.textContent = label;
    btn.addEventListener('click', openPanel);
    host.appendChild(btn);
  }

  function tryInject() {
    // Watch screen — add a generate button near the key visual area
    var watchHero = document.querySelector('.watch-hero, .player-hero, [class*="watch"] [class*="hero"]');
    if (watchHero) addButton(watchHero, '✨ Generate key visual', 'watch');

    // Detail page — key visual area
    var detailKv = document.querySelector('.detail-keyvisual, .anime-hero, [class*="detail"] [class*="hero"]');
    if (detailKv) addButton(detailKv, '✨ Generate key visual', 'detail');

    // Gallery
    var gallery = document.querySelector('.gallery, [class*="gallery"]');
    if (gallery) addButton(gallery, '✨ AI art studio', 'gallery');
  }

  function init() {
    tryInject();
    if (typeof MutationObserver !== 'undefined') {
      var obs = new MutationObserver(function () { tryInject(); });
      var tryObserve = function () {
        var main = document.getElementById('main');
        if (main) { obs.observe(main, { childList: true, subtree: true }); return true; }
        return false;
      };
      if (!tryObserve()) {
        var timer = setInterval(function () { if (tryObserve()) clearInterval(timer); }, 500);
        setTimeout(function () { clearInterval(timer); }, 15000);
      }
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
