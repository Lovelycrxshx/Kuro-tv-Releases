/* Kuro News Logos (1.5.6-beta1) — proper brand logos for news sources.
   Replaces the generic ✦ news avatar with the correct brand mark based on
   the source name: Crunchyroll gets its orange orb logo, Anime Trending
   gets its gradient trend-arrow wordmark. Other sources fall back to ✦. */
(function () {
  'use strict';

  // Crunchyroll orb mark — orange #F47521
  var CRUNCHYROLL_LOGO =
    '<svg viewBox="0 0 48 48" width="20" height="20" aria-label="Crunchyroll" role="img" xmlns="http://www.w3.org/2000/svg">' +
    '<circle cx="24" cy="24" r="21" fill="#F47521"/>' +
    '<circle cx="24" cy="24" r="14" fill="#1C1C1C"/>' +
    '<path d="M24 11a13 13 0 0 0 0 26 14 14 0 0 1 0-26Z" fill="#F47521"/>' +
    '<circle cx="24" cy="24" r="5" fill="#F47521"/>' +
    '</svg>';

  // Anime Trending — gradient trend-arrow wordmark
  var ANIME_TRENDING_LOGO =
    '<svg viewBox="0 0 48 48" width="20" height="20" aria-label="Anime Trending" role="img" xmlns="http://www.w3.org/2000/svg">' +
    '<defs><linearGradient id="atGrad15" x1="0" y1="0" x2="1" y2="1">' +
    '<stop offset="0" stop-color="#7C5CFF"/><stop offset="1" stop-color="#00C2A8"/></linearGradient></defs>' +
    '<rect x="2" y="2" width="44" height="44" rx="11" fill="#1a1230"/>' +
    '<path d="M14 30 L22 20 L27 27 L34 15" fill="none" stroke="url(#atGrad15)" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>' +
    '<circle cx="34" cy="15" r="3.5" fill="#00C2A8"/>' +
    '<path d="M10 36 L18 36" stroke="#7C5CFF" stroke-width="3.5" stroke-linecap="round"/>' +
    '</svg>';

  function logoForSource(source) {
    var s = (source || '').toLowerCase();
    if (s.indexOf('crunchyroll') !== -1) return CRUNCHYROLL_LOGO;
    if (s.indexOf('anime trending') !== -1 || s.indexOf('anitrend') !== -1 || s.indexOf('trending') !== -1) return ANIME_TRENDING_LOGO;
    return '';
  }

  function replaceAvatar(card) {
    var avatar = card.querySelector('.news-avatar');
    if (!avatar) return;
    if (avatar.dataset.logoApplied === '1') return;
    var sourceEl = card.querySelector('.news-byline strong');
    var source = sourceEl ? sourceEl.textContent : '';
    var logo = logoForSource(source);
    if (logo) {
      avatar.innerHTML = logo;
      avatar.dataset.logoApplied = '1';
      avatar.style.background = 'transparent';
      avatar.style.boxShadow = 'none';
    }
  }

  function processAll(root) {
    var rootEl = root || document;
    var cards = rootEl.querySelectorAll ? rootEl.querySelectorAll('.news-post') : [];
    for (var i = 0; i < cards.length; i++) replaceAvatar(cards[i]);
  }

  function init() {
    processAll(document);
    // News posts render dynamically — observe and patch new cards.
    if (typeof MutationObserver !== 'undefined') {
      var obs = new MutationObserver(function (muts) {
        for (var i = 0; i < muts.length; i++) {
          var nodes = muts[i].addedNodes;
          for (var j = 0; j < nodes.length; j++) {
            var n = nodes[j];
            if (n.classList && n.classList.contains('news-post')) replaceAvatar(n);
            else if (n.querySelectorAll) processAll(n);
          }
        }
      });
      var tryObserve = function () {
        var box = document.getElementById('news-posts');
        if (box) { obs.observe(box, { childList: true, subtree: true }); return true; }
        return false;
      };
      if (!tryObserve()) {
        var timer = setInterval(function () {
          if (tryObserve()) clearInterval(timer);
        }, 500);
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
