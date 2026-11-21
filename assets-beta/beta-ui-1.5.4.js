/* ==========================================================================
   Kuro.tv 1.5.4-beta UI Suite
   - #2 Cinematic Title Detail Page v2 (Banner, Franchise Watch-Order, Canon/Filler/Recap,
        Skip Filler switch + Auto-Next integration, Rich Episode Rows, Batch Download)
   - #3 Widescreen Left-Rail Leanback UI & Glowing DPAD Focus (ONLY when Desktop site ON)
   - #4 Smart Multi-Filter Browse, Voice Command Search, Surprise Me & Rich Poster Badges
   ========================================================================== */
(function () {
  'use strict';

  var META_CACHE_KEY = 'kuro-meta154-v1';
  var SKIP_FILLER_KEY = 'kuro-skip-filler-154';
  var DEFAULT_VIEWPORT = 'width=device-width,initial-scale=1,viewport-fit=cover';
  var DESKTOP_VIEWPORT = 'width=1180,initial-scale=1,viewport-fit=cover';

  /* ------------------------------------------------------------------------
     Safe helpers & local storage cache
     ------------------------------------------------------------------------ */
  function esc(v) {
    return String(v == null ? '' : v).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }

  function normTitle(s) {
    return String(s || '')
      .normalize('NFKC')
      .toLowerCase()
      .replace(/\s*\((?:dub|sub|tv|uncensored)\)\s*$/gi, '')
      .replace(/[^\p{L}\p{N}]+/gu, ' ')
      .trim();
  }

  function lsRead(key, fallback) {
    try {
      var raw = localStorage.getItem(key);
      return raw ? JSON.parse(raw) : fallback;
    } catch (_) {
      return fallback;
    }
  }

  function lsWrite(key, val) {
    try {
      localStorage.setItem(key, JSON.stringify(val));
    } catch (_) {}
  }

  function toast(msg) {
    if (typeof window.placeholder === 'function') window.placeholder(msg);
  }

  function curView() {
    try { if (typeof view !== 'undefined') return view; } catch (_) {}
    return window.view;
  }

  function setCurView(v) {
    try { if (typeof view !== 'undefined') { view = v; return; } } catch (_) {}
    window.view = v;
  }

  function curSelected() {
    try { if (typeof selected !== 'undefined') return selected; } catch (_) {}
    return window.selected;
  }

  function curEpisodes() {
    try { if (typeof episodes !== 'undefined' && Array.isArray(episodes)) return episodes; } catch (_) {}
    return Array.isArray(window.episodes) ? window.episodes : [];
  }

  /* ------------------------------------------------------------------------
     Native AniList Catalogue Bridge (reuses app.js pending[] / requestNo)
     ------------------------------------------------------------------------ */
  function catalogCall154(action, payload) {
    return new Promise(function (resolve, reject) {
      if (!window.KuroNative || typeof window.KuroNative.catalog573 !== 'function') {
        reject(new Error('Catalogue bridge unavailable'));
        return;
      }
      try {
        var nextNo = typeof requestNo === 'number' ? (++requestNo) : Math.floor(Math.random() * 1e9);
        var id = 'cat154-' + nextNo + '-' + Date.now();
        if (typeof pending === 'object' && pending) {
          pending[id] = { resolve: resolve, reject: reject };
          window.KuroNative.catalog573(id, action, JSON.stringify(payload));
        } else {
          reject(new Error('Pending map unavailable'));
        }
      } catch (e) {
        reject(e);
      }
    }).then(function (raw) {
      return typeof raw === 'string' ? JSON.parse(raw) : raw;
    });
  }

  /* ------------------------------------------------------------------------
     Metadata Cache (by AniList ID and normalized title)
     ------------------------------------------------------------------------ */
  var metaStore = lsRead(META_CACHE_KEY, { byTitle: {}, byId: {} });
  if (!metaStore || typeof metaStore !== 'object') metaStore = { byTitle: {}, byId: {} };
  if (!metaStore.byTitle) metaStore.byTitle = {};
  if (!metaStore.byId) metaStore.byId = {};

  var detailMemCache = new Map();
  var inflightTitles = new Set();
  var saveMetaTimer = null;

  function scheduleMetaSave() {
    if (saveMetaTimer) return;
    saveMetaTimer = setTimeout(function () {
      saveMetaTimer = null;
      var keys = Object.keys(metaStore.byTitle);
      if (keys.length > 320) {
        keys.slice(0, keys.length - 320).forEach(function (k) { delete metaStore.byTitle[k]; });
      }
      lsWrite(META_CACHE_KEY, metaStore);
    }, 500);
  }

  function recordMediaMeta(media, extraKeyTitle) {
    if (!media || media.isAdult) return null;
    var compact = {
      id: Number(media.id) || 0,
      idMal: Number(media.idMal) || 0,
      score: Number(media.averageScore) || 0,
      format: String(media.format || ''),
      status: String(media.status || ''),
      episodes: Number(media.episodes) || 0,
      season: String(media.season || ''),
      year: Number(media.seasonYear) || 0,
      genres: Array.isArray(media.genres) ? media.genres.slice(0, 6) : []
    };
    if (compact.id > 0) metaStore.byId[String(compact.id)] = compact;
    var t1 = normTitle(media.title && media.title.english);
    var t2 = normTitle(media.title && media.title.romaji);
    var t3 = normTitle(extraKeyTitle);
    if (t1) metaStore.byTitle[t1] = compact;
    if (t2) metaStore.byTitle[t2] = compact;
    if (t3) metaStore.byTitle[t3] = compact;
    scheduleMetaSave();
    return compact;
  }

  function getItemMeta(item) {
    if (!item) return null;
    var aid = Number(item.anilistId) || 0;
    if (aid > 0 && metaStore.byId[String(aid)]) return metaStore.byId[String(aid)];
    var nt = normTitle(item.title);
    return nt && metaStore.byTitle[nt] ? metaStore.byTitle[nt] : null;
  }

  function mergedCounts(item) {
    var out = { sub: '', dub: '', total: '' };
    if (!item) return out;
    var list = [item].concat(Array.isArray(item.unifiedVariants) ? item.unifiedVariants : []);
    for (var i = 0; i < list.length; i++) {
      var c = list[i] && list[i].counts;
      if (c) {
        if (!out.sub && c.sub) out.sub = String(c.sub);
        if (!out.dub && c.dub) out.dub = String(c.dub);
        if (!out.total && c.total) out.total = String(c.total);
      }
      var m = String((list[i] && list[i].meta) || '');
      if (!out.dub && /\bdub(?:bed)?\b/i.test(m)) out.dub = 'DUB';
      if (!out.sub && /\bsub(?:bed)?\b/i.test(m)) out.sub = 'SUB';
    }
    return out;
  }

  /* ========================================================================
     #3 WIDESCREEN LEFT-RAIL LEANBACK UI & GLOWING DPAD FOCUS
        STRICTLY GATED ON: Desktop site === ON (siteView === 'desktop')
     ======================================================================== */
  function isDesktopSiteActive() {
    try {
      if (window.kuroSettings560 && typeof window.kuroSettings560.snapshot === 'function') {
        return window.kuroSettings560.snapshot().siteView === 'desktop';
      }
    } catch (_) {}
    var stored = lsRead('kuro-settings560-v1', null);
    if (stored && stored.siteView === 'desktop') return true;
    try {
      if (window.KuroNative && typeof window.KuroNative.siteView154 === 'function') {
        return window.KuroNative.siteView154() === 'desktop';
      }
    } catch (_) {}
    return false;
  }

  function ensureDesktopDecorations(on) {
    var dock = document.querySelector('nav.kuro-dock');
    var existingBadge = document.getElementById('k154-rail-badge');
    var existingHint = document.getElementById('k154-dpad-hint');

    if (!on) {
      if (existingBadge) existingBadge.remove();
      if (existingHint) existingHint.remove();
      document.querySelectorAll('.k154-dpad-focus').forEach(function (el) {
        el.classList.remove('k154-dpad-focus');
      });
      return;
    }

    if (dock && !existingBadge) {
      var badge = document.createElement('button');
      badge.type = 'button';
      badge.id = 'k154-rail-badge';
      badge.className = 'k154-rail-badge';
      badge.textContent = 'Desktop UI · Mobile ↺';
      badge.setAttribute('aria-label', 'Desktop widescreen mode active. Activate to switch back to Mobile view.');
      badge.onclick = function () {
        var cur = lsRead('kuro-settings560-v1', {});
        cur.siteView = 'mobile';
        lsWrite('kuro-settings560-v1', cur);
        if (window.kuroSettings560 && typeof window.kuroSettings560.reload === 'function') {
          window.kuroSettings560.reload();
        }
        syncDesktopSite154(false);
        toast('Switched back to standard Mobile layout.');
      };
      dock.appendChild(badge);
    }

    if (!existingHint && document.body) {
      var hint = document.createElement('div');
      hint.id = 'k154-dpad-hint';
      hint.className = 'k154-dpad-hint';
      hint.setAttribute('aria-hidden', 'true');
      hint.innerHTML = '<span><kbd>OK</kbd> Select</span><span><kbd>MENU</kbd> Quick Actions</span><span><kbd>ESC</kbd> Back</span>';
      document.body.appendChild(hint);
    }
  }

  function syncDesktopSite154(explicitFlag) {
    var active = typeof explicitFlag === 'boolean' ? explicitFlag : isDesktopSiteActive();
    document.documentElement.dataset.siteView = active ? 'desktop' : 'mobile';

    // On Android WebView, switching to Desktop site widens the layout viewport to 1180px
    // so phones, tablets, and Android TV / Fire TV all render the widescreen Left-Rail UI cleanly.
    var vp = document.querySelector('meta[name="viewport"]');
    if (vp && window.KuroNative) {
      var target = active ? DESKTOP_VIEWPORT : DEFAULT_VIEWPORT;
      if (vp.getAttribute('content') !== target) vp.setAttribute('content', target);
    }

    ensureDesktopDecorations(active);
  }

  window.kuroSyncDesktopSite154 = syncDesktopSite154;
  window.addEventListener('kuro-settings560-changed', function (ev) {
    var sv = ev && ev.detail && ev.detail.siteView;
    syncDesktopSite154(sv ? sv === 'desktop' : undefined);
  });

  /* Spatial 2D Arrow-Key / Remote DPAD Navigation (active ONLY in Desktop mode) */
  function isEditable(el) {
    if (!el) return false;
    var tag = el.tagName;
    return tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || el.isContentEditable;
  }

  function focusableCandidates() {
    var modal = document.querySelector(
      '#kb-qa-15, .k154-sheet-scrim, #catalog-modal573, .choice-backdrop567, #settings-overlay560, #discord-overlay569, #transfer-overlay560, #account-overlay, #search-popup:not([hidden])'
    );
    var scope = modal || document.querySelector('.app') || document.body;
    var nodes = scope.querySelectorAll('button:not([disabled]), a[href], input:not([disabled]):not([type="hidden"]), [tabindex]:not([tabindex="-1"])');
    var out = [];
    for (var i = 0; i < nodes.length; i++) {
      var el = nodes[i];
      if (el.hidden || el.getAttribute('aria-hidden') === 'true') continue;
      var r = el.getBoundingClientRect();
      if (r.width < 8 || r.height < 8) continue;
      out.push({ el: el, rect: r, cx: r.left + r.width / 2, cy: r.top + r.height / 2 });
    }
    return out;
  }

  document.addEventListener('keydown', function (e) {
    if (!isDesktopSiteActive()) return;
    if (e.defaultPrevented || e.altKey || e.ctrlKey || e.metaKey) return;

    // Contextual MENU / F2 / ContextMenu opens long-press Quick Actions on focused poster
    if (e.key === 'ContextMenu' || e.key === 'F2') {
      var activeCard = document.activeElement && document.activeElement.closest && document.activeElement.closest('.card');
      if (activeCard) {
        e.preventDefault();
        activeCard.dispatchEvent(new MouseEvent('contextmenu', { bubbles: true, cancelable: true }));
      }
      return;
    }

    var dir = e.key;
    if (dir !== 'ArrowUp' && dir !== 'ArrowDown' && dir !== 'ArrowLeft' && dir !== 'ArrowRight') return;
    if (isEditable(document.activeElement)) return;
    // Let open choice sheets handle their own radio list navigation
    if (document.querySelector('.choice-backdrop567')) return;

    var items = focusableCandidates();
    if (!items.length) return;

    var cur = document.activeElement;
    var curEntry = null;
    for (var i = 0; i < items.length; i++) {
      if (items[i].el === cur) { curEntry = items[i]; break; }
    }
    if (!curEntry) {
      e.preventDefault();
      items[0].el.focus();
      return;
    }

    var best = null;
    var bestScore = Infinity;
    for (var j = 0; j < items.length; j++) {
      var cand = items[j];
      if (cand.el === cur) continue;
      var dx = cand.cx - curEntry.cx;
      var dy = cand.cy - curEntry.cy;
      if (dir === 'ArrowRight' && dx <= 6) continue;
      if (dir === 'ArrowLeft' && dx >= -6) continue;
      if (dir === 'ArrowDown' && dy <= 6) continue;
      if (dir === 'ArrowUp' && dy >= -6) continue;

      var primary = (dir === 'ArrowLeft' || dir === 'ArrowRight') ? Math.abs(dx) : Math.abs(dy);
      var cross = (dir === 'ArrowLeft' || dir === 'ArrowRight') ? Math.abs(dy) : Math.abs(dx);
      var score = primary + cross * 2.65;
      if (score < bestScore) {
        bestScore = score;
        best = cand.el;
      }
    }

    if (best) {
      e.preventDefault();
      document.querySelectorAll('.k154-dpad-focus').forEach(function (n) { n.classList.remove('k154-dpad-focus'); });
      best.classList.add('k154-dpad-focus');
      best.focus({ preventScroll: false });
      try { best.scrollIntoView({ block: 'nearest', inline: 'nearest' }); } catch (_) {}
    }
  });

  /* ========================================================================
     #2 CINEMATIC TITLE DETAIL PAGE V2
     ======================================================================== */
  // Curated filler / recap episode ranges for major long-running anime (supplementing live HiAnime ssl-item-filler flags)
  var CURATED_FILLER_RANGES = {
    'naruto': [[26, 26], [97, 97], [101, 106], [136, 219]],
    'naruto shippuden': [[57, 71], [91, 112], [144, 151], [170, 171], [176, 196], [223, 242], [257, 260], [271, 271], [279, 281], [284, 295], [303, 320], [347, 361], [376, 377], [388, 390], [394, 413], [416, 417], [422, 423], [427, 450], [464, 469], [480, 483]],
    'bleach': [[33, 33], [50, 50], [64, 109], [128, 137], [147, 149], [168, 189], [204, 205], [213, 214], [227, 266], [287, 287], [298, 299], [303, 305], [311, 342], [355, 355]],
    'one piece': [[54, 60], [98, 99], [102, 102], [131, 143], [196, 206], [220, 225], [279, 283], [326, 336], [382, 384], [426, 429], [575, 578], [626, 628], [747, 750], [780, 782], [895, 896], [1029, 1030]],
    'black clover': [[29, 29], [66, 66], [82, 82], [123, 125], [130, 154]],
    'dragon ball z': [[9, 10], [39, 44], [108, 117], [124, 125], [170, 171], [195, 199], [202, 204], [288, 288]],
    'fairy tail': [[9, 9], [19, 19], [69, 75], [125, 150], [202, 226], [268, 268]],
    'boruto naruto next generations': [[16, 17], [40, 41], [48, 50], [67, 69], [96, 105], [112, 119], [138, 140], [152, 156], [231, 255]]
  };

  function inRanges(num, ranges) {
    if (!ranges || !Number.isFinite(num)) return false;
    for (var i = 0; i < ranges.length; i++) {
      if (num >= ranges[i][0] && num <= ranges[i][1]) return true;
    }
    return false;
  }

  function classifyEpisode154(item, ep) {
    if (!ep) return 'canon';
    var rawNum = String(ep.number || '').trim();
    var name = String(ep.name || '').trim();
    if (/^\d+\.\d+$/.test(rawNum) || /\b(recap|summary|special digest|omnibus|interlude recap)\b/i.test(name)) {
      return 'recap';
    }
    if (ep.filler === true || /\b(filler)\b/i.test(name)) {
      return 'filler';
    }
    var n = Number(rawNum);
    var key = normTitle(item && item.title);
    if (key && CURATED_FILLER_RANGES[key] && inRanges(n, CURATED_FILLER_RANGES[key])) {
      return 'filler';
    }
    return 'canon';
  }

  function isSkipFillerEnabled() {
    return lsRead(SKIP_FILLER_KEY, false) === true;
  }

  function setSkipFillerEnabled(on) {
    lsWrite(SKIP_FILLER_KEY, !!on);
  }

  window.kuroShouldSkipFiller154 = function () {
    return isSkipFillerEnabled();
  };

  window.kuroIsEpisodeSkipped154 = function (item, ep) {
    var kind = classifyEpisode154(item, ep);
    return kind === 'filler' || kind === 'recap';
  };

  function episodeProgressBucket(item, ep) {
    if (!item || !ep) return { pctClass: 'k154-prog-p0', label: '' };
    var num = String(ep.number || '');
    var watched = typeof window.watchedHas === 'function' && window.watchedHas(item, num);
    var mins = -1;
    try {
      if (window.KuroNative && typeof window.KuroNative.progress15 === 'function') {
        mins = Number(window.KuroNative.progress15(String(item.source || ''), String(item.url || ''), num));
      }
    } catch (_) {
      mins = -1;
    }
    if (mins > 0) {
      var ratio = Math.max(5, Math.min(95, Math.round((mins / 24) * 20) * 5));
      return { pctClass: 'k154-prog-p' + ratio, label: 'Resume ' + mins + 'm in' };
    }
    if (watched) {
      return { pctClass: 'k154-prog-p100', label: 'Watched' };
    }
    return { pctClass: 'k154-prog-p0', label: '' };
  }

  function formatStatus154(st) {
    if (st === 'RELEASING') return 'Airing';
    if (st === 'FINISHED') return 'Completed';
    if (st === 'NOT_YET_RELEASED') return 'Upcoming';
    return st ? String(st) : '';
  }

  function fetchDetailMeta154(item) {
    if (!item || !item.title) return Promise.resolve(null);
    var cacheKey = (Number(item.anilistId) ? 'a:' + Number(item.anilistId) : 't:' + normTitle(item.title));
    if (detailMemCache.has(cacheKey)) return Promise.resolve(detailMemCache.get(cacheKey));

    var aid = Number(item.anilistId) || 0;
    var mid = Number(item.malId) || 0;
    var payload = aid > 0
      ? { provider: 'anilist', id: aid }
      : (mid > 0 ? { provider: 'mal', id: mid } : { search: String(item.title).slice(0, 120) });

    return catalogCall154('detail154', payload)
      .then(function (res) {
        var media = res && res.data && res.data.Media;
        if (!media || media.isAdult) return null;
        recordMediaMeta(media, item.title);
        detailMemCache.set(cacheKey, media);
        return media;
      })
      .catch(function () {
        return null;
      });
  }

  function buildFranchiseOrder154(media) {
    if (!media) return [];
    var curTitle = String((media.title && (media.title.english || media.title.romaji)) || '').trim();
    var edges = (media.relations && Array.isArray(media.relations.edges)) ? media.relations.edges : [];
    var prequels = [];
    var sequels = [];
    var extras = [];

    for (var i = 0; i < edges.length; i++) {
      var e = edges[i];
      var n = e && e.node;
      if (!n || n.type !== 'ANIME' || n.isAdult) continue;
      var t = String((n.title && (n.title.english || n.title.romaji)) || '').trim();
      if (!t) continue;
      var rel = String(e.relationType || '');
      var entry = {
        id: Number(n.id) || 0,
        title: t,
        rel: rel,
        format: String(n.format || 'TV'),
        year: (n.startDate && n.startDate.year) ? String(n.startDate.year) : '',
        current: false
      };
      if (rel === 'PREQUEL') prequels.push(entry);
      else if (rel === 'SEQUEL') sequels.push(entry);
      else if (rel === 'SIDE_STORY' || rel === 'PARENT' || rel === 'SPIN_OFF' || n.format === 'MOVIE' || n.format === 'OVA') {
        extras.push(entry);
      }
    }

    var all = prequels.slice(0, 3)
      .concat(curTitle ? [{
        id: Number(media.id) || 0,
        title: curTitle,
        rel: 'CURRENT',
        format: String(media.format || 'TV'),
        year: media.seasonYear ? String(media.seasonYear) : '',
        current: true
      }] : [])
      .concat(sequels.slice(0, 3))
      .concat(extras.slice(0, 3));

    return all.length >= 2 ? all : [];
  }

  function renderDetailHeader154(item, media) {
    var info = document.querySelector('#main > .info');
    if (!info) return;

    var hero = document.getElementById('k154-detail-hero');
    if (!hero) {
      hero = document.createElement('section');
      hero.id = 'k154-detail-hero';
      hero.className = 'k154-detail-hero';
      info.parentNode.insertBefore(hero, info);
    }

    var counts = mergedCounts(item);
    var banner = (media && (media.bannerImage || (media.coverImage && media.coverImage.extraLarge))) || item.img || '';
    var score = media && Number(media.averageScore) > 0 ? (Number(media.averageScore) / 10).toFixed(1) : '';
    var dur = media && Number(media.duration) > 0 ? (Number(media.duration) + 'm / ep') : '';
    var fmt = (media && media.format) || '';
    var status = formatStatus154(media && media.status);
    var seasonText = (media && media.season && media.seasonYear)
      ? (media.season.charAt(0) + media.season.slice(1).toLowerCase() + ' ' + media.seasonYear)
      : (media && media.seasonYear ? String(media.seasonYear) : '');
    var studio = (media && media.studios && Array.isArray(media.studios.nodes) && media.studios.nodes[0] && media.studios.nodes[0].name) || '';

    var pills = [];
    if (score) pills.push('<span class="k154-meta-pill is-score">★ ' + esc(score) + '</span>');
    if (status) pills.push('<span class="k154-meta-pill' + (status === 'Airing' ? ' is-airing' : '') + '">' + esc(status) + '</span>');
    if (fmt) pills.push('<span class="k154-meta-pill">' + esc(fmt) + '</span>');
    if (dur) pills.push('<span class="k154-meta-pill">' + esc(dur) + '</span>');
    if (seasonText) pills.push('<span class="k154-meta-pill">' + esc(seasonText) + '</span>');
    if (studio) pills.push('<span class="k154-meta-pill">' + esc(studio) + '</span>');
    if (counts.sub || counts.dub) {
      var audioLabel = [counts.sub ? ('SUB ' + counts.sub) : '', counts.dub ? ('DUB ' + counts.dub) : ''].filter(Boolean).join(' · ');
      pills.push('<span class="k154-meta-pill">' + esc(audioLabel) + '</span>');
    }

    hero.classList.toggle('has-banner', !!banner);
    hero.innerHTML =
      (banner ? '<img class="k154-detail-banner-img" src="' + esc(banner) + '" alt="" loading="eager" decoding="async"><div class="k154-detail-banner-shade"></div>' : '') +
      '<div class="k154-detail-meta-strip">' + pills.join('') + '</div>';

    // Franchise / Watch-Order Pill Strip
    var order = buildFranchiseOrder154(media);
    var oldBox = document.getElementById('k154-franchise-box');
    if (!order.length) {
      if (oldBox) oldBox.remove();
      return;
    }
    var box = oldBox || document.createElement('section');
    box.id = 'k154-franchise-box';
    box.className = 'k154-franchise-box';
    box.setAttribute('aria-label', 'Franchise watch order');
    var anchor = document.getElementById('related-open573') || document.getElementById('detail-extra');
    if (!oldBox && anchor && anchor.parentNode) {
      anchor.parentNode.insertBefore(box, anchor);
    }

    box.innerHTML =
      '<div class="k154-franchise-head">' +
        '<span class="k154-franchise-kicker">Franchise &amp; Watch Order</span>' +
        '<span class="k154-franchise-sub">Tap any entry to switch</span>' +
      '</div>' +
      '<div class="k154-franchise-rail">' +
        order.map(function (step, idx) {
          var badge = step.current ? '✓' : String(idx + 1);
          var meta = [step.format, step.year].filter(Boolean).join(' · ');
          return '<button type="button" class="k154-franchise-pill' + (step.current ? ' is-current' : '') + '" data-franchise-idx="' + idx + '"' + (step.current ? ' aria-current="true"' : '') + '>' +
            '<span class="k154-franchise-step">' + esc(badge) + '</span>' +
            '<span>' + esc(step.title) + '</span>' +
            (meta ? '<span class="k154-franchise-meta">' + esc(meta) + '</span>' : '') +
          '</button>';
        }).join('') +
      '</div>';

    box.querySelectorAll('[data-franchise-idx]').forEach(function (btn) {
      btn.onclick = function () {
        var step = order[Number(btn.dataset.franchiseIdx)];
        if (!step || step.current) return;
        if (typeof window.searchPage === 'function') {
          setCurView('search');
          window.searchPage(step.title);
        }
      };
    });
  }

  /* Batch Offline Download Sheet (Zenkai Direct HLS + Watch Queue fallback) */
  function findZenkaiOwner(item) {
    if (!item) return Promise.resolve(null);
    if (item.source === 'zenkai') return Promise.resolve(item);
    var vars = Array.isArray(item.unifiedVariants) ? item.unifiedVariants : [];
    for (var i = 0; i < vars.length; i++) {
      if (vars[i] && vars[i].source === 'zenkai') return Promise.resolve(vars[i]);
    }
    if (typeof window.fetchCatalogPage !== 'function') return Promise.resolve(null);
    return window.fetchCatalogPage({ source: 'zenkai', mode: 'search', query: item.title }, 1)
      .then(function (res) {
        var list = (res && Array.isArray(res.items)) ? res.items : [];
        var want = normTitle(item.title);
        for (var j = 0; j < list.length; j++) {
          if (normTitle(list[j].title) === want) return list[j];
        }
        return list.length === 1 ? list[0] : null;
      })
      .catch(function () { return null; });
  }

  function openBatchDownloadSheet154(item) {
    var old = document.getElementById('k154-batch-sheet');
    if (old) old.remove();
    var eps = curEpisodes().filter(function (e) { return !e.missing; });
    if (!eps.length) {
      toast('Wait for episodes to finish loading first.');
      return;
    }

    var skipFiller = isSkipFillerEnabled();
    var unwatched = eps.filter(function (e) {
      if (skipFiller && window.kuroIsEpisodeSkipped154(item, e)) return false;
      return !(typeof window.watchedHas === 'function' && window.watchedHas(item, e.number));
    });
    var basePool = unwatched.length ? unwatched : eps;

    var scrim = document.createElement('div');
    scrim.id = 'k154-batch-sheet';
    scrim.className = 'k154-sheet-scrim';
    scrim.setAttribute('role', 'dialog');
    scrim.setAttribute('aria-modal', 'true');
    scrim.setAttribute('aria-labelledby', 'k154-batch-title');

    var presets = [
      { count: Math.min(3, basePool.length), label: 'Next 3 episodes', desc: 'Quick commute pack' },
      { count: Math.min(5, basePool.length), label: 'Next 5 episodes', desc: 'Standard offline arc' },
      { count: Math.min(12, basePool.length), label: 'Next 12 episodes (1 cour)', desc: 'Full season batch' },
      { count: Math.min(24, basePool.length), label: 'All unwatched (up to 24)', desc: skipFiller ? 'Skips filler & recap episodes automatically' : 'Includes all unwatched episodes' }
    ].filter(function (p, i, arr) {
      return p.count > 0 && arr.findIndex(function (x) { return x.count === p.count; }) === i;
    });

    scrim.innerHTML =
      '<section class="k154-sheet-card">' +
        '<div class="k154-sheet-head">' +
          '<div>' +
            '<h3 id="k154-batch-title">⬇ Batch Offline Download</h3>' +
            '<p>' + esc(item.title) + (skipFiller ? ' · Skip Filler ON' : '') + '</p>' +
          '</div>' +
          '<button type="button" class="k154-sheet-close" data-batch-close aria-label="Close">×</button>' +
        '</div>' +
        '<div class="k154-sheet-grid">' +
          presets.map(function (p, idx) {
            var slice = basePool.slice(0, p.count);
            var range = slice.length === 1
              ? ('Episode ' + slice[0].number)
              : ('Ep ' + slice[0].number + ' – ' + slice[slice.length - 1].number + ' (' + slice.length + ')');
            return '<button type="button" class="k154-sheet-option" data-batch-preset="' + idx + '">' +
              '<span><strong>' + esc(p.label) + '</strong><small>' + esc(p.desc) + '</small></span>' +
              '<span>' + esc(range) + ' ›</span>' +
            '</button>';
          }).join('') +
        '</div>' +
        '<p class="k154-sheet-status" id="k154-batch-status" role="status" aria-live="polite">Resolves verified direct HLS streams; falls back to Watch Queue if a title is embed-only.</p>' +
      '</section>';

    document.body.appendChild(scrim);
    function close() { if (scrim.parentNode) scrim.remove(); }
    scrim.querySelector('[data-batch-close]').onclick = close;
    scrim.onclick = function (e) { if (e.target === scrim) close(); };
    scrim.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') { e.preventDefault(); close(); }
    });

    var busy = false;
    scrim.querySelectorAll('[data-batch-preset]').forEach(function (btn) {
      btn.onclick = async function () {
        if (busy) return;
        busy = true;
        var p = presets[Number(btn.dataset.batchPreset)];
        var targetEps = basePool.slice(0, p.count);
        var statusEl = document.getElementById('k154-batch-status');
        if (statusEl) statusEl.textContent = 'Resolving direct HLS streams for ' + targetEps.length + ' episode(s)…';

        try {
          var zOwner = await findZenkaiOwner(item);
          var zEpMap = new Map();
          if (zOwner && typeof window.get === 'function' && typeof window.addedDetails === 'function') {
            try {
              var rawZ = await window.get(zOwner.url);
              var detZ = await window.addedDetails(zOwner, rawZ);
              if (detZ && Array.isArray(detZ.episodes)) {
                detZ.episodes.forEach(function (ze) { zEpMap.set(String(Number(ze.number)), ze); });
              }
            } catch (_) {}
          }

          var rows = [];
          if (zOwner && typeof window.zenkaiServers === 'function') {
            for (var i = 0; i < targetEps.length; i++) {
              var ep = targetEps[i];
              var zEp = zEpMap.get(String(Number(ep.number))) || (ep.source === 'zenkai' ? ep : null);
              if (!zEp) continue;
              if (statusEl) statusEl.textContent = 'Checking Episode ' + ep.number + ' (' + (i + 1) + '/' + targetEps.length + ')…';
              try {
                var sv = await window.zenkaiServers(zOwner, zEp);
                var hls = Array.isArray(sv) ? sv.find(function (s) { return s && s.type === 'hls' && s.referer === 'zenkai' && s.url; }) : null;
                if (hls) {
                  rows.push({
                    address: hls.url,
                    key: 'zenkai|' + String(zOwner.url || '').slice(0, 400) + '|' + String(ep.number),
                    title: String(item.title || 'Anime'),
                    episode: String(ep.number)
                  });
                }
              } catch (_) {}
            }
          }

          if (rows.length > 0 && window.KuroNative && typeof window.KuroNative.batchDownload154 === 'function') {
            close();
            window.KuroNative.batchDownload154(JSON.stringify(rows));
            return;
          }

          // Graceful fallback: add the selected batch to Kuro's local Watch Queue
          var queued = 0;
          if (window.kuroQueue571 && typeof window.kuroQueue571.add === 'function') {
            for (var q = 0; q < targetEps.length; q++) {
              try { if (window.kuroQueue571.add(item, targetEps[q])) queued++; } catch (_) {}
            }
          }
          close();
          toast(queued > 0
            ? ('Direct HLS unavailable on this source — added ' + queued + ' episode(s) to your Watch Queue.')
            : 'No direct HLS download stream is available for this source.');
        } finally {
          busy = false;
        }
      };
    });

    var firstBtn = scrim.querySelector('[data-batch-preset]');
    if (firstBtn) firstBtn.focus();
  }

  function enhanceDetailEpisodes154(item, media) {
    var epsContainer = document.getElementById('eps');
    var epsList = curEpisodes();
    if (!epsContainer || !epsList.length) return;

    // Map AniList streamingEpisodes thumbnails onto episode numbers when available
    var thumbByNum = {};
    if (media && Array.isArray(media.streamingEpisodes)) {
      for (var s = 0; s < media.streamingEpisodes.length; s++) {
        var se = media.streamingEpisodes[s];
        if (!se || !se.thumbnail) continue;
        var m = String(se.title || '').match(/Episode\s+(\d+(?:\.\d+)?)/i);
        var keyNum = m ? m[1] : String(s + 1);
        if (!thumbByNum[keyNum]) thumbByNum[keyNum] = se.thumbnail;
        if (!se.usedTitle && m && epsList[s] && !epsList[s].name) {
          var cleanName = String(se.title || '').replace(/^Episode\s+\d+(?:\.\d+)?\s*[-–:]\s*/i, '').trim();
          if (cleanName) epsList[s].name = cleanName;
        }
      }
    }

    var fallbackThumb = (media && (media.bannerImage || (media.coverImage && media.coverImage.large))) || item.img || '';
    var counts = { canon: 0, filler: 0, recap: 0 };
    var skipOn = isSkipFillerEnabled();

    var rows = epsContainer.querySelectorAll('.episode-row');
    rows.forEach(function (row) {
      var btn = row.querySelector('button[data-ep]');
      if (!btn) return;
      var idx = Number(btn.dataset.ep);
      var ep = epsList[idx];
      if (!ep) return;

      var kind = classifyEpisode154(item, ep);
      counts[kind] = (counts[kind] || 0) + 1;

      var thumb = ep.thumbnail || thumbByNum[String(ep.number)] || fallbackThumb;
      if (thumb && !ep.thumbnail) ep.thumbnail = thumb;

      var prog = episodeProgressBucket(item, ep);
      var epTitle = ep.name && String(ep.name).trim() ? String(ep.name).trim() : ('Episode ' + ep.number);
      var badgeLabel = kind === 'filler' ? 'Filler' : (kind === 'recap' ? 'Recap' : 'Canon');
      var subParts = ['Episode ' + ep.number];
      if (prog.label) subParts.push(prog.label);

      row.classList.add('k154-ep-upgraded');
      row.classList.toggle('is-filler-hidden', skipOn && (kind === 'filler' || kind === 'recap'));

      btn.innerHTML =
        '<span class="k154-ep-thumb">' +
          (thumb ? '<img src="' + esc(thumb) + '" alt="" loading="lazy" decoding="async">' : '<span aria-hidden="true">▶</span>') +
          '<span class="k154-ep-num-chip">EP ' + esc(ep.number) + '</span>' +
          '<span class="k154-ep-prog-track"><span class="k154-ep-prog-fill ' + prog.pctClass + '"></span></span>' +
        '</span>' +
        '<span class="k154-ep-copy">' +
          '<span class="k154-ep-top">' +
            '<span class="k154-ep-badge is-' + kind + '">' + badgeLabel + '</span>' +
            '<strong class="k154-ep-title">' + esc(epTitle) + '</strong>' +
          '</span>' +
          '<small class="k154-ep-sub">' + esc(subParts.join(' · ')) + '</small>' +
        '</span>';
    });

    // Inject / update Episode Toolbar (Skip Filler toggle + Canon/Filler summary + Batch Download)
    var toolbar = document.getElementById('k154-ep-toolbar');
    var searchLabel = document.querySelector('.episode-search571');
    if (!toolbar && searchLabel && searchLabel.parentNode) {
      toolbar = document.createElement('div');
      toolbar.id = 'k154-ep-toolbar';
      toolbar.className = 'k154-ep-toolbar';
      searchLabel.parentNode.insertBefore(toolbar, searchLabel);
    }
    if (toolbar) {
      var summaryParts = [counts.canon + ' Canon'];
      if (counts.filler) summaryParts.push(counts.filler + ' Filler');
      if (counts.recap) summaryParts.push(counts.recap + ' Recap');
      toolbar.innerHTML =
        '<div class="k154-ep-toolbar-left">' +
          '<button type="button" class="k154-skip-filler-btn" id="k154-skip-filler-btn" aria-pressed="' + String(skipOn) + '">' +
            '<span class="k154-switch-dot" aria-hidden="true"></span>' +
            '<span>Skip Filler</span>' +
          '</button>' +
          '<span class="k154-filler-summary">' + esc(summaryParts.join(' · ')) + '</span>' +
        '</div>' +
        '<div class="k154-ep-toolbar-right">' +
          '<button type="button" class="k154-batch-dl-btn" id="k154-batch-dl-btn">⬇ Batch Download</button>' +
        '</div>';

      var skipBtn = document.getElementById('k154-skip-filler-btn');
      if (skipBtn) {
        skipBtn.onclick = function () {
          var next = !isSkipFillerEnabled();
          setSkipFillerEnabled(next);
          enhanceDetailEpisodes154(item, media);
          toast(next ? 'Skip Filler enabled — hiding filler/recap episodes & skipping on Auto-Next.' : 'Skip Filler turned off.');
        };
      }
      var batchBtn = document.getElementById('k154-batch-dl-btn');
      if (batchBtn) {
        batchBtn.onclick = function () { openBatchDownloadSheet154(item); };
      }
    }

    // Upgrade Next Unwatched button so it respects Skip Filler when enabled
    var nextBtn = document.getElementById('next-unwatched');
    if (nextBtn) {
      nextBtn.onclick = function () {
        var skip = isSkipFillerEnabled();
        var listNow = curEpisodes();
        var found = listNow.find(function (e) {
          if (skip && window.kuroIsEpisodeSkipped154(item, e)) return false;
          return !(typeof window.watchedHas === 'function' && window.watchedHas(item, e.number));
        }) || listNow.find(function (e) {
          return !(typeof window.watchedHas === 'function' && window.watchedHas(item, e.number));
        });
        if (found && typeof window.openEpisode === 'function') window.openEpisode(found);
      };
    }
  }

  function hookDetailView154() {
    var activeDetailMedia = null;

    if (typeof window.openShow === 'function' && !window.openShow.__k154) {
      var origOpenShow = window.openShow;
      var wrappedOpenShow = async function (item) {
        activeDetailMedia = null;
        var p = origOpenShow.apply(this, arguments);
        var currentItem = curSelected() || item;
        var seqAtStart = window.renderSeq;
        renderDetailHeader154(currentItem, null);
        fetchDetailMeta154(currentItem).then(function (media) {
          if (curView() !== 'detail' || window.renderSeq !== seqAtStart) return;
          activeDetailMedia = media;
          renderDetailHeader154(curSelected() || currentItem, media);
          enhanceDetailEpisodes154(curSelected() || currentItem, media);
        });
        return p;
      };
      wrappedOpenShow.__k154 = true;
      window.openShow = wrappedOpenShow;
    }

    if (typeof window.renderDetailEpisodes === 'function' && !window.renderDetailEpisodes.__k154) {
      var origRenderEps = window.renderDetailEpisodes;
      var wrappedRenderEps = function (item, marker) {
        var r = origRenderEps.apply(this, arguments);
        if (marker === window.renderSeq) {
          enhanceDetailEpisodes154(item || curSelected(), activeDetailMedia);
        }
        return r;
      };
      wrappedRenderEps.__k154 = true;
      window.renderDetailEpisodes = wrappedRenderEps;
    }

    if (typeof window.updateWatchedUI === 'function' && !window.updateWatchedUI.__k154) {
      var origWatchedUI = window.updateWatchedUI;
      var wrappedWatchedUI = function (item) {
        var r = origWatchedUI.apply(this, arguments);
        if (curView() === 'detail') {
          enhanceDetailEpisodes154(item || curSelected(), activeDetailMedia);
        }
        return r;
      };
      wrappedWatchedUI.__k154 = true;
      window.updateWatchedUI = wrappedWatchedUI;
    }
  }

  /* ========================================================================
     #4 SMART MULTI-FILTER BROWSE, VOICE SEARCH, SURPRISE ME & POSTER BADGES
     ======================================================================== */
  var smartFilterState = {
    audio: '',   // 'dub' | 'sub'
    status: '',  // 'RELEASING' | 'FINISHED'
    minScore: 0, // 80 | 85
    genre: ''    // 'Action', 'Fantasy', etc.
  };

  function resetSmartFilters() {
    smartFilterState.audio = '';
    smartFilterState.status = '';
    smartFilterState.minScore = 0;
    smartFilterState.genre = '';
  }

  function hasActiveSmartFilters() {
    return !!(smartFilterState.audio || smartFilterState.status || smartFilterState.minScore || smartFilterState.genre);
  }

  function matchesSmartFilters154(item) {
    if (!hasActiveSmartFilters()) return true;
    var counts = mergedCounts(item);
    var metaStr = String((item && item.meta) || '');
    var m = getItemMeta(item);

    if (smartFilterState.audio === 'dub') {
      var hasDub = !!counts.dub || /\bdub\b/i.test(metaStr) || /\(dub\)/i.test(String(item && item.title || ''));
      if (!hasDub) return false;
    } else if (smartFilterState.audio === 'sub') {
      var hasSub = !!counts.sub || !/\(dub\)/i.test(String(item && item.title || ''));
      if (!hasSub) return false;
    }

    if (smartFilterState.status) {
      var st = (m && m.status) || (/\b(airing|ongoing|releasing)\b/i.test(metaStr) ? 'RELEASING' : (/\b(completed|finished)\b/i.test(metaStr) ? 'FINISHED' : ''));
      if (st !== smartFilterState.status) return false;
    }

    if (smartFilterState.minScore > 0) {
      var sc = m ? Number(m.score) || 0 : 0;
      if (sc < smartFilterState.minScore) return false;
    }

    if (smartFilterState.genre) {
      var genres = (m && Array.isArray(m.genres)) ? m.genres : [];
      var gLow = smartFilterState.genre.toLowerCase();
      var hit = genres.some(function (g) { return String(g).toLowerCase() === gLow; }) ||
        metaStr.toLowerCase().indexOf(gLow) >= 0;
      if (!hit) return false;
    }

    return true;
  }

  /* Batch Metadata Enrichment + Rich Poster Badges (SUB | DUB + ★ Score) */
  function decorateRenderedPosters154() {
    var cards = document.querySelectorAll('.card');
    if (!cards.length) return;

    var toLookup = [];
    cards.forEach(function (card) {
      var poster = card.querySelector('.poster');
      var titleEl = card.querySelector('strong');
      if (!poster || !titleEl) return;
      var title = titleEl.textContent.trim();
      var nt = normTitle(title);
      if (!nt) return;

      var meta = metaStore.byTitle[nt] || null;
      if (!meta && !inflightTitles.has(nt) && toLookup.length < 8 && toLookup.indexOf(title) < 0) {
        toLookup.push(title);
      }

      // 1. ★ Rating pill (top-right)
      var oldScore = poster.querySelector('.k154-poster-score');
      if (meta && meta.score >= 50) {
        var scoreStr = '★ ' + (meta.score / 10).toFixed(1);
        if (!oldScore) {
          oldScore = document.createElement('span');
          oldScore.className = 'k154-poster-score';
          poster.appendChild(oldScore);
        }
        if (oldScore.textContent !== scoreStr) oldScore.textContent = scoreStr;
      }

      // 2. SUB | DUB split pill (bottom-left) upgraded from existing .counts
      if (!poster.querySelector('.k154-poster-split')) {
        var countsEl = poster.querySelector('.counts');
        if (countsEl) {
          var subNode = countsEl.querySelector('.subcount');
          var dubNode = countsEl.querySelector('.dubcount');
          var subNum = subNode ? (subNode.textContent || '').replace(/^CC\s*/i, '').trim() : '';
          var dubNum = dubNode ? (dubNode.textContent || '').trim() : '';
          if (subNum || dubNum) {
            var split = document.createElement('span');
            split.className = 'k154-poster-split';
            split.innerHTML =
              (subNum ? '<span class="is-sub">SUB ' + esc(subNum) + '</span>' : '') +
              (dubNum ? '<span class="is-dub">DUB ' + esc(dubNum) + '</span>' : '');
            poster.classList.add('k154-has-split');
            poster.appendChild(split);
          }
        } else if (meta && meta.episodes > 0) {
          var epSplit = document.createElement('span');
          epSplit.className = 'k154-poster-split';
          epSplit.innerHTML = '<span class="is-sub">' + esc(meta.episodes) + ' EPS</span>';
          poster.appendChild(epSplit);
        }
      }
    });

    if (toLookup.length > 0 && window.KuroNative && typeof window.KuroNative.catalog573 === 'function') {
      toLookup.forEach(function (t) { inflightTitles.add(normTitle(t)); });
      catalogCall154('metaBatch154', { titles: toLookup })
        .then(function (res) {
          var data = res && res.data;
          if (!data) return;
          for (var i = 0; i < toLookup.length; i++) {
            var m = data['m' + i];
            if (m && !m.isAdult) recordMediaMeta(m, toLookup[i]);
          }
          decorateRenderedPosters154();
          if (activeBrowsePager154 && typeof window.renderChips581 === 'function') {
            renderSmartFilterBar154(activeBrowsePager154);
          }
        })
        .catch(function () {});
    }
  }

  /* Voice Search (Native Android RecognizerIntent + Web Speech API fallback) */
  var webSpeechRec = null;

  function setMicListeningState(on) {
    document.querySelectorAll('.k154-mic-btn').forEach(function (b) {
      b.classList.toggle('is-listening', !!on);
      b.setAttribute('aria-pressed', String(!!on));
    });
  }

  window.kuroOnVoiceSearch154 = function (spokenText) {
    setMicListeningState(false);
    var q = String(spokenText || '').trim();
    if (!q) return;
    var qInput = document.getElementById('q');
    if (qInput) qInput.value = q;
    if (typeof window.searchPage === 'function') {
      setCurView('search');
      window.searchPage(q);
    }
    toast('Voice search: “' + q + '”');
  };

  window.kuroOnVoiceSearchError154 = function () {
    setMicListeningState(false);
  };

  function triggerVoiceSearch154() {
    if (window.KuroNative && typeof window.KuroNative.voiceSearch154 === 'function') {
      setMicListeningState(true);
      try {
        window.KuroNative.voiceSearch154();
        setTimeout(function () { setMicListeningState(false); }, 8000);
        return;
      } catch (_) {
        setMicListeningState(false);
      }
    }

    var SR = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (SR) {
      try {
        if (webSpeechRec) { try { webSpeechRec.abort(); } catch (_) {} }
        webSpeechRec = new SR();
        webSpeechRec.lang = navigator.language || 'en-US';
        webSpeechRec.interimResults = false;
        webSpeechRec.maxAlternatives = 1;
        setMicListeningState(true);
        webSpeechRec.onresult = function (ev) {
          var transcript = ev.results && ev.results[0] && ev.results[0][0] && ev.results[0][0].transcript;
          window.kuroOnVoiceSearch154(transcript || '');
        };
        webSpeechRec.onerror = function () { setMicListeningState(false); toast('Could not hear a title. Try again.'); };
        webSpeechRec.onend = function () { setMicListeningState(false); };
        webSpeechRec.start();
        return;
      } catch (_) {
        setMicListeningState(false);
      }
    }
    toast('Voice search requires Android Speech Services or a supported microphone.');
  }

  var activeBrowsePager154 = null;

  function pruneHiddenControls154() {
    document.querySelectorAll('.open-sites, [data-miruro-verify], #k154-surprise-btn, .k154-surprise-btn').forEach(function (el) {
      el.remove();
    });
  }

  function enhanceBrowseCommandBar154() {
    var form = document.getElementById('discover-search');
    if (!form || form.classList.contains('k154-cmd-ready')) return;
    form.classList.add('k154-cmd-ready');

    var micBtn = document.createElement('button');
    micBtn.type = 'button';
    micBtn.id = 'k154-browse-mic';
    micBtn.className = 'k154-mic-btn';
    micBtn.setAttribute('aria-label', 'Voice search');
    micBtn.setAttribute('aria-pressed', 'false');
    micBtn.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="9" y="3" width="6" height="11" rx="3"/><path d="M5 11a7 7 0 0 0 14 0M12 18v3M8.5 21h7"/></svg><span>Voice</span>';
    micBtn.onclick = triggerVoiceSearch154;

    form.appendChild(micBtn);
  }

  function renderSmartFilterBar154(pager) {
    var filtersRoot = document.getElementById('filters581');
    if (!filtersRoot || !pager || !pager.state) return;
    enhanceBrowseCommandBar154(pager);

    var items = Array.isArray(pager.state.items) ? pager.state.items : [];
    if (!items.length) return;

    var genreCounts = new Map();
    for (var i = 0; i < items.length; i++) {
      var m = getItemMeta(items[i]);
      if (m && Array.isArray(m.genres)) {
        for (var g = 0; g < m.genres.length; g++) {
          var name = m.genres[g];
          genreCounts.set(name, (genreCounts.get(name) || 0) + 1);
        }
      }
    }
    var defaultGenres = ['Action', 'Fantasy', 'Adventure', 'Comedy', 'Romance', 'Sci-Fi', 'Drama'];
    var genrePills = genreCounts.size
      ? Array.from(genreCounts.keys()).sort(function (a, b) { return genreCounts.get(b) - genreCounts.get(a); }).slice(0, 7)
      : defaultGenres;

    var bar = document.getElementById('k154-smart-filters');
    if (!bar) {
      bar = document.createElement('div');
      bar.id = 'k154-smart-filters';
      bar.className = 'k154-smart-filters';
      bar.setAttribute('role', 'group');
      bar.setAttribute('aria-label', 'Smart multi-filters');
      filtersRoot.parentNode.insertBefore(bar, filtersRoot);
    }

    var matchedCount = items.filter(function (it) {
      return (typeof window.chipMatch581 === 'function' ? window.chipMatch581(it) : true) && matchesSmartFilters154(it);
    }).length;

    bar.innerHTML =
      '<div class="k154-filter-row">' +
        '<span class="k154-filter-label">Audio &amp; Score</span>' +
        '<button type="button" class="k154-pill" data-k154-audio="dub" aria-pressed="' + String(smartFilterState.audio === 'dub') + '">🎙 Dub Available</button>' +
        '<button type="button" class="k154-pill" data-k154-audio="sub" aria-pressed="' + String(smartFilterState.audio === 'sub') + '">CC Subbed</button>' +
        '<button type="button" class="k154-pill is-score" data-k154-score="80" aria-pressed="' + String(smartFilterState.minScore === 80) + '">★ 8.0+</button>' +
        '<button type="button" class="k154-pill is-score" data-k154-score="85" aria-pressed="' + String(smartFilterState.minScore === 85) + '">★ 8.5+</button>' +
        '<button type="button" class="k154-pill" data-k154-status="RELEASING" aria-pressed="' + String(smartFilterState.status === 'RELEASING') + '">◉ Airing</button>' +
        '<button type="button" class="k154-pill" data-k154-status="FINISHED" aria-pressed="' + String(smartFilterState.status === 'FINISHED') + '">✓ Completed</button>' +
      '</div>' +
      '<div class="k154-filter-row">' +
        '<span class="k154-filter-label">Genre</span>' +
        genrePills.map(function (gName) {
          return '<button type="button" class="k154-pill" data-k154-genre="' + esc(gName) + '" aria-pressed="' + String(smartFilterState.genre === gName) + '">' + esc(gName) + '</button>';
        }).join('') +
      '</div>' +
      (hasActiveSmartFilters()
        ? '<div class="k154-filter-summary"><span>Showing ' + matchedCount + ' of ' + items.length + ' loaded titles</span><button type="button" class="k154-reset-btn" data-k154-reset="1">Reset smart filters</button></div>'
        : '');

    function applyAndRedraw() {
      if (typeof window.renderPagerCards === 'function') window.renderPagerCards(pager, true);
      renderSmartFilterBar154(pager);
      decorateRenderedPosters154();
    }

    bar.querySelectorAll('[data-k154-audio]').forEach(function (b) {
      b.onclick = function () {
        var v = b.dataset.k154Audio;
        smartFilterState.audio = smartFilterState.audio === v ? '' : v;
        applyAndRedraw();
      };
    });
    bar.querySelectorAll('[data-k154-status]').forEach(function (b) {
      b.onclick = function () {
        var v = b.dataset.k154Status;
        smartFilterState.status = smartFilterState.status === v ? '' : v;
        applyAndRedraw();
      };
    });
    bar.querySelectorAll('[data-k154-score]').forEach(function (b) {
      b.onclick = function () {
        var v = Number(b.dataset.k154Score) || 0;
        smartFilterState.minScore = smartFilterState.minScore === v ? 0 : v;
        applyAndRedraw();
      };
    });
    bar.querySelectorAll('[data-k154-genre]').forEach(function (b) {
      b.onclick = function () {
        var v = b.dataset.k154Genre;
        smartFilterState.genre = smartFilterState.genre === v ? '' : v;
        applyAndRedraw();
      };
    });
    var resetBtn = bar.querySelector('[data-k154-reset]');
    if (resetBtn) {
      resetBtn.onclick = function () {
        resetSmartFilters();
        applyAndRedraw();
      };
    }
  }

  function hookBrowseFilters154() {
    if (typeof window.attachFilters581 === 'function' && !window.attachFilters581.__k154) {
      var origAttach = window.attachFilters581;
      var wrappedAttach = function (pager) {
        activeBrowsePager154 = pager;
        var res = origAttach.apply(this, arguments);
        if (pager) {
          var baseFilter = pager.itemFilter;
          pager.itemFilter = function (item) {
            if (typeof baseFilter === 'function' && !baseFilter(item)) return false;
            return matchesSmartFilters154(item);
          };
          var baseAfter = pager.afterRender;
          pager.afterRender = function (p) {
            if (typeof baseAfter === 'function') baseAfter(p);
            renderSmartFilterBar154(p);
            decorateRenderedPosters154();
          };
          if (hasActiveSmartFilters() && typeof window.renderPagerCards === 'function') {
            window.renderPagerCards(pager, true);
          }
          renderSmartFilterBar154(pager);
          decorateRenderedPosters154();
        }
        return res;
      };
      wrappedAttach.__k154 = true;
      window.attachFilters581 = wrappedAttach;
    }
  }

  /* ==========================================================================
     ACCOUNT SECTION (#3): UNIFIED TRACKERS & DISCORD RICH PRESENCE HUB
     ========================================================================== */
  var LAST_SYNC_KEY_154 = 'kuro-tracker-last-sync-154';
  var RPC_FALLBACK_KEY_154 = 'kuro-rpc-fallback-154';

  function readLastSyncMap154() {
    var d = lsRead(LAST_SYNC_KEY_154, {});
    return (d && typeof d === 'object' && !Array.isArray(d)) ? d : {};
  }

  function recordSyncSuccess154(provider) {
    if (provider !== 'anilist' && provider !== 'mal') return;
    var m = readLastSyncMap154();
    m[provider] = Date.now();
    lsWrite(LAST_SYNC_KEY_154, m);
  }

  function formatRelativeAgo154(ts) {
    var n = Number(ts) || 0;
    if (!n || n > Date.now() + 60000) return '';
    var diffSec = Math.max(0, Math.floor((Date.now() - n) / 1000));
    if (diffSec < 45) return 'just now';
    var mins = Math.floor(diffSec / 60);
    if (mins < 60) return mins + 'm ago';
    var hrs = Math.floor(mins / 60);
    if (hrs < 24) return hrs + 'h ago';
    return Math.floor(hrs / 24) + 'd ago';
  }

  function readTrackerStats154() {
    var raw = lsRead('kuro-tracker-sync-v1', null);
    var queue = (raw && Array.isArray(raw.queue)) ? raw.queue : [];
    var auto = (raw && raw.auto && typeof raw.auto === 'object') ? raw.auto : { anilist: true, mal: true };
    var imports = lsRead('kuro-imports-v1', []);
    if (!Array.isArray(imports)) imports = [];

    var watchedTotal = 0;
    var watchedObj = lsRead('kuro-watched-v1', {});
    if (watchedObj && typeof watchedObj === 'object' && !Array.isArray(watchedObj)) {
      Object.keys(watchedObj).forEach(function (k) {
        var row = watchedObj[k];
        if (row && typeof row === 'object' && !Array.isArray(row)) {
          Object.keys(row).forEach(function (ep) {
            if (row[ep] === true) watchedTotal++;
          });
        }
      });
    }

    function forProvider(p) {
      var pJobs = queue.filter(function (j) { return j && j.provider === p; });
      var pImports = imports.filter(function (x) { return x && x.provider === p; });
      var importedEps = pImports.reduce(function (sum, x) {
        return sum + Math.max(0, Math.min(10000, Number(x && x.progress) || 0));
      }, 0);
      return {
        autoOn: auto[p] !== false,
        pending: pJobs.filter(function (j) { return j.state === 'pending'; }).length,
        match: pJobs.filter(function (j) { return j.state === 'match'; }).length,
        conflicts: pJobs.filter(function (j) { return j.state === 'conflict'; }).length,
        blocked: pJobs.filter(function (j) { return j.state === 'blocked'; }).length,
        importsCount: pImports.length,
        mirroredEpisodes: Math.max(watchedTotal, importedEps)
      };
    }

    return {
      anilist: forProvider('anilist'),
      mal: forProvider('mal'),
      watchedTotal: watchedTotal
    };
  }

  function readProviderDomState154(page, provider) {
    var list = page.querySelector('.profile-account-list547');
    if (!list) return { connected: false, name: '', row: null };
    var ctrl = list.querySelector('[data-disconnect="' + provider + '"], [data-connect="' + provider + '"]');
    var row = ctrl ? ctrl.closest('.account-connection') : null;
    if (row) row.classList.add('k154-delegated-row');
    var isCon = !!(ctrl && ctrl.hasAttribute('data-disconnect'));
    var name = '';
    if (isCon && row) {
      var span = row.querySelector('.connection-copy span');
      var txt = span ? String(span.textContent || '').trim() : '';
      name = txt.replace(/^Connected as\s+/i, '').trim();
    }
    var importBtn = row ? row.querySelector('[data-import="' + provider + '"]') : null;
    var disconnectBtn = row ? row.querySelector('[data-disconnect="' + provider + '"]') : null;
    var connectBtn = row ? row.querySelector('[data-connect="' + provider + '"]') : null;
    return {
      connected: isCon,
      name: name,
      handle: name ? (name.charAt(0) === '@' ? name : '@' + name) : '',
      row: row,
      importBtn: importBtn,
      disconnectBtn: disconnectBtn,
      connectBtn: connectBtn
    };
  }

  function readDiscordRpcState154() {
    var fallback = lsRead(RPC_FALLBACK_KEY_154, { enabled: false, episode: 'show', title: 'show', poster: 'show', layout: 'anime' });
    var nativeHasRpc = !!(window.KuroNative && typeof window.KuroNative.discordRpcEnabled563 === 'function');
    var enabled = !!fallback.enabled;
    if (nativeHasRpc) {
      try { enabled = !!window.KuroNative.discordRpcEnabled563(); } catch (_) {}
    }
    var opts = {
      episode: fallback.episode || 'show',
      title: fallback.title || 'show',
      poster: fallback.poster || 'show',
      pause: fallback.pause || 'show',
      layout: fallback.layout || 'anime',
      download: fallback.download || 'on'
    };
    if (window.KuroNative && typeof window.KuroNative.discordOptions564 === 'function') {
      try {
        var parsed = JSON.parse(window.KuroNative.discordOptions564() || '{}');
        if (parsed && typeof parsed === 'object') {
          Object.keys(parsed).forEach(function (k) { opts[k] = parsed[k]; });
        }
      } catch (_) {}
    }
    return {
      enabled: enabled,
      showProgress: opts.episode !== 'hide',
      privacyHideTitles: opts.title === 'hide',
      showPoster: opts.poster !== 'icon',
      layout: opts.layout || 'anime'
    };
  }

  function setDiscordRpcMaster154(on) {
    var next = !!on;
    var fb = lsRead(RPC_FALLBACK_KEY_154, {});
    fb.enabled = next;
    lsWrite(RPC_FALLBACK_KEY_154, fb);
    if (window.KuroNative && typeof window.KuroNative.setDiscordRpcEnabled563 === 'function') {
      try { window.KuroNative.setDiscordRpcEnabled563(next); } catch (_) {}
    }
    toast(next ? 'Discord Rich Presence enabled.' : 'Discord Rich Presence turned off.');
  }

  function setDiscordRpcOption154(key, value) {
    var fb = lsRead(RPC_FALLBACK_KEY_154, {});
    fb[key] = value;
    lsWrite(RPC_FALLBACK_KEY_154, fb);
    if (window.KuroNative && typeof window.KuroNative.setDiscordOption564 === 'function') {
      try { window.KuroNative.setDiscordOption564(key, value); } catch (_) {}
    }
  }

  function readDiscordSample154() {
    var hist = lsRead('kuro-episode-history-v1', []);
    if (Array.isArray(hist) && hist.length) {
      for (var i = 0; i < hist.length; i++) {
        var h = hist[i];
        if (h && h.item && h.item.title) {
          var img = String(h.item.img || '');
          var safeImg = /^https:\/\//i.test(img) ? img : '';
          var epNum = (h.ep && h.ep.number != null) ? String(h.ep.number) : '1';
          var pctStep = 60;
          var remainText = '18:40 remaining';
          if (window.KuroNative && typeof window.KuroNative.progress15 === 'function') {
            try {
              var pRaw = window.KuroNative.progress15(String(h.item.title || '') + '::' + epNum);
              var pObj = pRaw ? JSON.parse(pRaw) : null;
              if (pObj && Number(pObj.duration) > 60000 && Number(pObj.position) > 0) {
                var remSec = Math.max(0, Math.round((Number(pObj.duration) - Number(pObj.position)) / 1000));
                var mins = Math.floor(remSec / 60);
                var secs = remSec % 60;
                remainText = mins + ':' + (secs < 10 ? '0' : '') + secs + ' remaining';
                var ratio = Math.max(10, Math.min(100, Math.round((Number(pObj.position) / Number(pObj.duration)) * 10) * 10));
                pctStep = ratio;
              }
            } catch (_) {}
          }
          return {
            title: String(h.item.title),
            epNum: epNum,
            img: safeImg,
            remainText: remainText,
            pctClass: 'k154-pct-' + pctStep
          };
        }
      }
    }
    return {
      title: 'Frieren: Beyond Journey\u2019s End',
      epNum: '28',
      img: '',
      remainText: '18:40 remaining',
      pctClass: 'k154-pct-60'
    };
  }

  function collectReviewRowsForProvider154(syncBox, labelPrefix) {
    if (!syncBox) return [];
    var out = [];
    var reviews = syncBox.querySelectorAll('.tracker-review553');
    reviews.forEach(function (el) {
      var sm = el.querySelector('small');
      var smText = sm ? String(sm.textContent || '') : '';
      if (smText.indexOf(labelPrefix) !== 0) return;
      var titleEl = el.querySelector('strong');
      var btns = [];
      el.querySelectorAll('button').forEach(function (b) {
        btns.push({ label: String(b.textContent || 'Action'), target: b });
      });
      out.push({
        title: titleEl ? String(titleEl.textContent || '') : 'Anime',
        detail: smText,
        btns: btns
      });
    });
    return out;
  }

  function triggerProviderConnect154(domInfo, provider) {
    if (domInfo && domInfo.connectBtn) {
      domInfo.connectBtn.click();
      setTimeout(function () {
        var btn = document.querySelector('#account-overlay [data-signin="' + provider + '"]');
        if (btn) {
          try { btn.focus(); } catch (_) {}
          btn.click();
        }
      }, 30);
    }
  }

  function renderAccountHub154(force) {
    var page = document.querySelector('#main > .profile-page547');
    if (!page) return;
    var syncBox = page.querySelector('#tracker-sync553');
    if (syncBox) syncBox.classList.add('k154-delegated-sync');

    var aniDom = readProviderDomState154(page, 'anilist');
    var malDom = readProviderDomState154(page, 'mal');
    var discDom = readProviderDomState154(page, 'discord');
    var tStats = readTrackerStats154();
    var lastSync = readLastSyncMap154();
    var rpc = readDiscordRpcState154();
    var sample = readDiscordSample154();

    var aniReviews = collectReviewRowsForProvider154(syncBox, 'AniList');
    var malReviews = collectReviewRowsForProvider154(syncBox, 'MyAnimeList');

    var sig = JSON.stringify({
      a: [aniDom.connected, aniDom.name, aniDom.disconnectBtn && !!aniDom.disconnectBtn.dataset.confirm, aniDom.importBtn && aniDom.importBtn.disabled, tStats.anilist, Boolean(lastSync.anilist), aniReviews.length],
      m: [malDom.connected, malDom.name, malDom.disconnectBtn && !!malDom.disconnectBtn.dataset.confirm, malDom.importBtn && malDom.importBtn.disabled, tStats.mal, Boolean(lastSync.mal), malReviews.length],
      d: [discDom.connected, discDom.name, rpc, sample.title, sample.epNum, sample.pctClass]
    });

    var hub = document.getElementById('k154-account-hub');
    if (!force && hub && hub.parentNode === page && hub.dataset.k154Sig === sig) return;

    if (!hub) {
      hub = document.createElement('section');
      hub.id = 'k154-account-hub';
      hub.className = 'k154-account-hub';
      hub.setAttribute('aria-label', 'Trackers and Discord Rich Presence');
    }
    hub.dataset.k154Sig = sig;

    var anchorLabel = null;
    page.querySelectorAll('.profile-label547').forEach(function (lbl) {
      if (String(lbl.textContent || '').trim().toLowerCase() === 'account') anchorLabel = lbl;
    });
    if (hub.parentNode !== page) {
      if (anchorLabel) page.insertBefore(hub, anchorLabel);
      else if (syncBox) page.insertBefore(hub, syncBox);
      else page.appendChild(hub);
    }

    var aniAgo = formatRelativeAgo154(lastSync.anilist);
    var aniStatusLine = aniDom.connected
      ? ('Synced ' + (aniAgo || 'ready') + ' \u00b7 ' + tStats.anilist.conflicts + ' conflicts' + (tStats.anilist.pending ? (' \u00b7 ' + tStats.anilist.pending + ' pending') : ''))
      : (tStats.anilist.importsCount ? (tStats.anilist.importsCount + ' titles saved locally \u00b7 connect to sync') : 'Mirror episodes, progress & anime lists');

    var aniImportLabel = (aniDom.importBtn && aniDom.importBtn.disabled)
      ? 'Importing\u2026'
      : (tStats.anilist.importsCount > 0 ? ('Import (' + tStats.anilist.importsCount + ' saved)') : 'Import Titles');

    var malImportLabel = (malDom.importBtn && malDom.importBtn.disabled)
      ? 'Importing\u2026'
      : (tStats.mal.importsCount > 0 ? ('Import (' + tStats.mal.importsCount + ' saved)') : 'Import Titles');

    var previewHeadline = rpc.privacyHideTitles ? 'Watching an anime' : sample.title;
    var previewSub = rpc.showProgress
      ? ('Episode ' + sample.epNum + ' \u00b7 ' + sample.remainText)
      : 'Episode number & progress bar hidden';
    var useCoverImg = !rpc.privacyHideTitles && rpc.showPoster && !!sample.img;

    function reviewHtml(list, prefix) {
      if (!list.length) return '';
      return '<div class="k154-hub-reviews">' + list.map(function (r, idx) {
        return '<div class="k154-hub-review-item">' +
          '<div><strong>' + esc(r.title) + '</strong><small>' + esc(r.detail) + '</small></div>' +
          '<div class="k154-hub-review-btns">' + r.btns.map(function (b, bi) {
            return '<button type="button" data-k154-rev="' + prefix + ':' + idx + ':' + bi + '">' + esc(b.label) + '</button>';
          }).join('') + '</div></div>';
      }).join('') + '</div>';
    }

    hub.innerHTML =
      '<div class="k154-hub-head">' +
        '<span class="k154-hub-head-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M21 12a9 9 0 0 0-15.5-6.36L3 8"/><path d="M3 3v5h5"/><path d="M3 12a9 9 0 0 0 15.5 6.36L21 16"/><path d="M16 16h5v5"/></svg></span>' +
        '<div><h2>Trackers &amp; Rich Presence</h2><p>Connect your anime trackers and control rich presence.</p></div>' +
      '</div>' +
      '<div class="k154-hub-grid">' +

        /* Card 1: AniList */
        '<article class="k154-hub-card is-anilist">' +
          '<div class="k154-hub-card-top">' +
            '<span class="k154-brand-badge is-al" aria-hidden="true">A<em>L</em></span>' +
            '<div class="k154-hub-identity">' +
              '<div class="k154-hub-title-row">' +
                '<strong>AniList</strong><span class="k154-hub-dot-sep">\u00b7</span>' +
                '<span class="k154-hub-state' + (aniDom.connected ? ' is-on' : '') + '">' + (aniDom.connected ? 'Connected' : 'Not connected') + '</span>' +
              '</div>' +
              '<div class="k154-hub-handle">' + esc(aniDom.connected ? (aniDom.handle || '@connected') : 'Encrypted local PIN token') + '</div>' +
              '<div class="k154-hub-meta-line">' +
                '<span class="k154-live-dot' + (aniDom.connected ? '' : ' is-off') + '" aria-hidden="true"></span>' +
                '<span>' + esc(aniStatusLine) + '</span>' +
              '</div>' +
            '</div>' +
            (aniDom.connected
              ? '<button type="button" class="k154-hub-unlink' + (aniDom.disconnectBtn && aniDom.disconnectBtn.dataset.confirm ? ' is-confirm' : '') + '" data-k154-unlink="anilist">' +
                  (aniDom.disconnectBtn && aniDom.disconnectBtn.dataset.confirm ? 'Confirm' : 'Disconnect') +
                '</button>'
              : '') +
          '</div>' +
          '<div class="k154-hub-divider"></div>' +
          '<div class="k154-hub-toggle-row">' +
            '<div class="k154-hub-toggle-copy">' +
              '<span class="k154-hub-row-ico" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M21 12a9 9 0 0 0-15.5-6.36L3 8"/><path d="M3 3v5h5"/><path d="M3 12a9 9 0 0 0 15.5 6.36L21 16"/><path d="M16 16h5v5"/></svg></span>' +
              '<div><strong>Two-Way Auto-Sync</strong><small>Episodes, progress &amp; list status</small></div>' +
            '</div>' +
            '<button type="button" role="switch" class="k154-oled-switch" data-k154-autosync="anilist" aria-label="AniList Two-Way Auto-Sync" aria-checked="' + String(tStats.anilist.autoOn) + '"></button>' +
          '</div>' +
          '<div class="k154-hub-actions">' +
            (aniDom.connected
              ? '<button type="button" class="k154-hub-btn" data-k154-syncnow="anilist">\u21bb Sync Now</button>' +
                '<button type="button" class="k154-hub-btn" data-k154-import="anilist"' + (aniDom.importBtn && aniDom.importBtn.disabled ? ' disabled' : '') + '>\u21e7 ' + esc(aniImportLabel) + '</button>'
              : '<button type="button" class="k154-hub-btn is-primary" data-k154-connect="anilist">Connect AniList</button>' +
                '<button type="button" class="k154-hub-btn is-muted" data-k154-open-imports="1">Imported (' + tStats.anilist.importsCount + ')</button>') +
          '</div>' +
          reviewHtml(aniReviews, 'ani') +
        '</article>' +

        /* Card 2: MyAnimeList */
        '<article class="k154-hub-card is-mal">' +
          '<div class="k154-hub-card-top">' +
            '<span class="k154-brand-badge is-mal" aria-hidden="true">MAL</span>' +
            '<div class="k154-hub-identity">' +
              '<div class="k154-hub-title-row">' +
                '<strong>MyAnimeList</strong><span class="k154-hub-dot-sep">\u00b7</span>' +
                '<span class="k154-hub-state' + (malDom.connected ? ' is-on' : '') + '">' + (malDom.connected ? 'Connected' : 'Not connected') + '</span>' +
              '</div>' +
              (malDom.connected && malDom.handle ? '<div class="k154-hub-handle">' + esc(malDom.handle) + '</div>' : '') +
              '<div class="k154-hub-meta-line">' +
                '<span class="k154-hub-mini-icon" aria-hidden="true">\u2713</span>' +
                '<span>' + tStats.mal.mirroredEpisodes + ' episodes mirrored</span>' +
              '</div>' +
              '<div class="k154-hub-meta-line">' +
                '<span class="k154-hub-mini-icon" aria-hidden="true">\u25f7</span>' +
                '<span>Queue: ' + tStats.mal.pending + ' pending offline updates' + (tStats.mal.conflicts ? (' \u00b7 ' + tStats.mal.conflicts + ' conflicts') : '') + '</span>' +
              '</div>' +
            '</div>' +
            (malDom.connected
              ? '<button type="button" class="k154-hub-unlink' + (malDom.disconnectBtn && malDom.disconnectBtn.dataset.confirm ? ' is-confirm' : '') + '" data-k154-unlink="mal">' +
                  (malDom.disconnectBtn && malDom.disconnectBtn.dataset.confirm ? 'Confirm' : 'Disconnect') +
                '</button>'
              : '') +
          '</div>' +
          '<div class="k154-hub-divider"></div>' +
          '<div class="k154-hub-toggle-row">' +
            '<div class="k154-hub-toggle-copy">' +
              '<span class="k154-hub-row-ico" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M21 12a9 9 0 0 0-15.5-6.36L3 8"/><path d="M3 3v5h5"/><path d="M3 12a9 9 0 0 0 15.5 6.36L21 16"/><path d="M16 16h5v5"/></svg></span>' +
              '<div><strong>Two-Way Auto-Sync</strong><small>Mirror watched episodes &amp; status</small></div>' +
            '</div>' +
            '<button type="button" role="switch" class="k154-oled-switch" data-k154-autosync="mal" aria-label="MyAnimeList Two-Way Auto-Sync" aria-checked="' + String(tStats.mal.autoOn) + '"></button>' +
          '</div>' +
          '<div class="k154-hub-actions">' +
            (malDom.connected
              ? '<button type="button" class="k154-hub-btn" data-k154-syncnow="mal">\u21bb Sync Now</button>' +
                '<button type="button" class="k154-hub-btn" data-k154-import="mal"' + (malDom.importBtn && malDom.importBtn.disabled ? ' disabled' : '') + '>\u21e7 ' + esc(malImportLabel) + '</button>'
              : '<button type="button" class="k154-hub-btn is-primary" data-k154-connect="mal">Connect MyAnimeList</button>' +
                '<button type="button" class="k154-hub-btn is-muted" data-k154-open-imports="1">Imported (' + tStats.mal.importsCount + ')</button>') +
          '</div>' +
          reviewHtml(malReviews, 'mal') +
        '</article>' +

        /* Card 3: Discord Rich Presence · Live Preview */
        '<article class="k154-hub-card is-discord">' +
          '<div class="k154-hub-card-top">' +
            '<span class="k154-brand-badge is-discord" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M6.1 5.1C8.1 4.2 9 4 10 4l.4.9a14 14 0 0 1 3.2 0L14 4c1 0 2 .3 3.9 1.1 2.3 3.1 3 6.5 2.5 10.6-1.8 1.5-3.2 2.2-4.7 2.6l-1-1.4c.7-.2 1.3-.5 1.9-.9-3.1 1.4-6.1 1.4-9.2 0 .6.4 1.2.7 1.9.9l-1 1.4c-1.5-.4-2.9-1.1-4.7-2.6-.5-4.1.2-7.5 2.5-10.6ZM9 13a1.3 1.3 0 1 0 0-2.6A1.3 1.3 0 0 0 9 13Zm6 0a1.3 1.3 0 1 0 0-2.6A1.3 1.3 0 0 0 15 13Z"/></svg></span>' +
            '<div class="k154-hub-identity">' +
              '<div class="k154-hub-title-row">' +
                '<strong>Discord Rich Presence</strong><span class="k154-hub-dot-sep">\u00b7</span>' +
                '<span class="k154-hub-state is-on">Live Preview</span>' +
              '</div>' +
              '<div class="k154-hub-handle">' + esc(discDom.connected && discDom.handle ? ('Connected as ' + discDom.handle + ' \u00b7 see how your activity appears') : 'See how your activity appears to others.') + '</div>' +
            '</div>' +
          '</div>' +

          '<div class="k154-rpc-preview" aria-label="Discord Rich Presence preview">' +
            '<div class="k154-rpc-art">' +
              (useCoverImg
                ? '<img src="' + esc(sample.img) + '" alt="" loading="lazy" decoding="async">'
                : '<img class="is-logo" src="kuro-logo.svg" alt="" decoding="async">') +
            '</div>' +
            '<div class="k154-rpc-body">' +
              '<div class="k154-rpc-eyebrow">' +
                '<span class="k154-rpc-k" aria-hidden="true">K</span>' +
                '<span>Kuro.tv</span>' +
                '<span class="k154-rpc-pill' + (rpc.enabled ? ' is-live' : '') + '">' + (rpc.enabled ? 'SHARING ON' : 'PREVIEW') + '</span>' +
              '</div>' +
              '<strong class="k154-rpc-title">' + esc(previewHeadline) + '</strong>' +
              '<span class="k154-rpc-watching">Watching on Kuro.tv</span>' +
              '<span class="k154-rpc-ep">' + esc(previewSub) + '</span>' +
              (rpc.showProgress ? '<div class="k154-rpc-bar" aria-hidden="true"><i class="' + esc(sample.pctClass) + '"></i></div>' : '') +
            '</div>' +
          '</div>' +

          '<div class="k154-hub-toggle-row">' +
            '<div class="k154-hub-toggle-copy">' +
              '<span class="k154-hub-row-ico" aria-hidden="true"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="m10 8 6 4-6 4V8Z"/></svg></span>' +
              '<div><strong>Share Activity on Discord</strong><small>Broadcast Watching on Kuro.tv while playing</small></div>' +
            '</div>' +
            '<button type="button" role="switch" class="k154-oled-switch" data-k154-rpc-master="1" aria-label="Share Activity on Discord" aria-checked="' + String(rpc.enabled) + '"></button>' +
          '</div>' +

          '<div class="k154-hub-toggle-row">' +
            '<div class="k154-hub-toggle-copy">' +
              '<span class="k154-hub-row-ico" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M4 19V11M10 19V7M16 19v-5M22 19V4"/></svg></span>' +
              '<div><strong>Show Episode Progress</strong><small>Display episode number &amp; progress bar in rich presence</small></div>' +
            '</div>' +
            '<button type="button" role="switch" class="k154-oled-switch" data-k154-rpc-progress="1" aria-label="Show Episode Progress" aria-checked="' + String(rpc.showProgress) + '"></button>' +
          '</div>' +

          '<div class="k154-hub-toggle-row">' +
            '<div class="k154-hub-toggle-copy">' +
              '<span class="k154-hub-row-ico" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z"/><path d="m9 12 2 2 4-4"/></svg></span>' +
              '<div><strong>Privacy Mode on 18+ &amp; Titles</strong><small>18+ is always hidden; turn on to hide all titles &amp; covers</small></div>' +
            '</div>' +
            '<button type="button" role="switch" class="k154-oled-switch" data-k154-rpc-privacy="1" aria-label="Privacy Mode on 18+ and Titles" aria-checked="' + String(rpc.privacyHideTitles) + '"></button>' +
          '</div>' +

          '<div class="k154-hub-foot-actions">' +
            '<button type="button" class="k154-hub-btn is-muted" data-k154-rpc-test="1">\u26a1 15s Test</button>' +
            '<button type="button" class="k154-hub-btn is-muted" data-k154-rpc-more="1">\u2699 Customize</button>' +
            (discDom.connected
              ? '<button type="button" class="k154-hub-btn" data-k154-rpc-account="1">Account &amp; Targets</button>'
              : '<button type="button" class="k154-hub-btn" data-k154-connect="discord">Connect Discord</button>') +
          '</div>' +
        '</article>' +

      '</div>';

    // Graceful image error fallback on Discord cover preview
    var artImg = hub.querySelector('.k154-rpc-art img:not(.is-logo)');
    if (artImg) {
      artImg.addEventListener('error', function () {
        artImg.className = 'is-logo';
        artImg.src = 'kuro-logo.svg';
      }, { once: true });
    }

    // Wire Auto-Sync switches (delegated to tracker-sync553 buttons or direct state)
    hub.querySelectorAll('[data-k154-autosync]').forEach(function (sw) {
      sw.onclick = function () {
        var p = sw.dataset.k154Autosync;
        var origToggle = syncBox ? syncBox.querySelector('[data-sync-toggle553="' + p + '"]') : null;
        if (origToggle) {
          origToggle.click();
        } else {
          var st = lsRead('kuro-tracker-sync-v1', { v: 1, queue: [], links: {}, initialized: {}, auto: { anilist: true, mal: true }, cooldown: {}, seq: 0 });
          if (!st.auto) st.auto = { anilist: true, mal: true };
          st.auto[p] = !(st.auto[p] !== false);
          lsWrite('kuro-tracker-sync-v1', st);
        }
        renderAccountHub154(true);
      };
    });

    // Wire Sync Now
    hub.querySelectorAll('[data-k154-syncnow]').forEach(function (btn) {
      btn.onclick = function () {
        var p = btn.dataset.k154Syncnow;
        var retryBtn = syncBox ? syncBox.querySelector('#tracker-retry553') : null;
        if (retryBtn) retryBtn.click();
        if (typeof window.kuroSyncResume553 === 'function') window.kuroSyncResume553();
        var cur = readTrackerStats154()[p];
        if (cur && cur.pending === 0 && cur.conflicts === 0 && cur.match === 0) {
          recordSyncSuccess154(p);
          toast((p === 'anilist' ? 'AniList' : 'MyAnimeList') + ' is up to date \u00b7 0 pending.');
        } else {
          toast('Syncing ' + (p === 'anilist' ? 'AniList' : 'MyAnimeList') + ' queue\u2026');
        }
        renderAccountHub154(true);
      };
    });

    // Wire Import Titles
    hub.querySelectorAll('[data-k154-import]').forEach(function (btn) {
      btn.onclick = function () {
        var p = btn.dataset.k154Import;
        var dom = p === 'anilist' ? aniDom : malDom;
        if (dom.importBtn) {
          dom.importBtn.click();
          setTimeout(function () { renderAccountHub154(true); }, 60);
        }
      };
    });

    // Wire Connect buttons
    hub.querySelectorAll('[data-k154-connect]').forEach(function (btn) {
      btn.onclick = function () {
        var p = btn.dataset.k154Connect;
        var dom = p === 'anilist' ? aniDom : (p === 'mal' ? malDom : discDom);
        triggerProviderConnect154(dom, p);
      };
    });

    // Wire Disconnect (2-step confirm preserved from accounts542.js)
    hub.querySelectorAll('[data-k154-unlink]').forEach(function (btn) {
      btn.onclick = function () {
        var p = btn.dataset.k154Unlink;
        var dom = p === 'anilist' ? aniDom : malDom;
        if (dom.disconnectBtn) {
          dom.disconnectBtn.click();
          renderAccountHub154(true);
          setTimeout(function () { renderAccountHub154(true); }, 5100);
        }
      };
    });

    // Wire Open Imported Lists
    hub.querySelectorAll('[data-k154-open-imports]').forEach(function (btn) {
      btn.onclick = function () {
        var imp = page.querySelector('#profile-imported');
        if (imp) imp.click();
      };
    });

    // Wire Inline Review actions
    hub.querySelectorAll('[data-k154-rev]').forEach(function (btn) {
      btn.onclick = function () {
        var parts = String(btn.dataset.k154Rev || '').split(':');
        var list = parts[0] === 'ani' ? aniReviews : malReviews;
        var item = list[Number(parts[1])];
        var action = item && item.btns[Number(parts[2])];
        if (action && action.target) {
          action.target.click();
          setTimeout(function () { renderAccountHub154(true); }, 50);
        }
      };
    });

    // Wire Discord Rich Presence switches & actions
    var rpcMaster = hub.querySelector('[data-k154-rpc-master]');
    if (rpcMaster) {
      rpcMaster.onclick = function () {
        var cur = readDiscordRpcState154();
        setDiscordRpcMaster154(!cur.enabled);
        renderAccountHub154(true);
      };
    }

    var rpcProg = hub.querySelector('[data-k154-rpc-progress]');
    if (rpcProg) {
      rpcProg.onclick = function () {
        var cur = readDiscordRpcState154();
        var next = !cur.showProgress;
        setDiscordRpcOption154('episode', next ? 'show' : 'hide');
        setDiscordRpcOption154('pause', next ? 'show' : 'noProgress');
        toast(next ? 'Episode progress enabled in Rich Presence.' : 'Episode progress hidden in Rich Presence.');
        renderAccountHub154(true);
      };
    }

    var rpcPriv = hub.querySelector('[data-k154-rpc-privacy]');
    if (rpcPriv) {
      rpcPriv.onclick = function () {
        var cur = readDiscordRpcState154();
        var next = !cur.privacyHideTitles;
        setDiscordRpcOption154('title', next ? 'hide' : 'show');
        toast(next ? 'Privacy Mode on: anime titles & covers hidden.' : 'Anime titles & covers shown (18+ stays hidden).');
        renderAccountHub154(true);
      };
    }

    var rpcTest = hub.querySelector('[data-k154-rpc-test]');
    if (rpcTest) {
      rpcTest.onclick = function () {
        if (window.KuroNative && typeof window.KuroNative.discordAction564 === 'function') {
          try {
            window.KuroNative.discordAction564('test');
            toast('Sent 15-second test activity to Discord.');
            return;
          } catch (_) {}
        }
        toast('Discord test activity requires the Kuro Android app.');
      };
    }

    var rpcMore = hub.querySelector('[data-k154-rpc-more]');
    if (rpcMore) {
      rpcMore.onclick = function () {
        if (window.kuroSettings560 && typeof window.kuroSettings560.openDiscord569 === 'function') {
          window.kuroSettings560.openDiscord569('customization', {
            onClosed: function () { renderAccountHub154(true); }
          });
        }
      };
    }

    var rpcAcc = hub.querySelector('[data-k154-rpc-account]');
    if (rpcAcc) {
      rpcAcc.onclick = function () {
        if (window.kuroSettings560 && typeof window.kuroSettings560.openDiscord569 === 'function') {
          window.kuroSettings560.openDiscord569('presence', {
            onClosed: function () { renderAccountHub154(true); }
          });
        }
      };
    }
  }

  function hookAccountTrackerHub154() {
    if (typeof window.kuroTrackerRequest553 === 'function' && !window.kuroTrackerRequest553.__k154) {
      var origReq = window.kuroTrackerRequest553;
      var wrappedReq = function (action, data) {
        return origReq.apply(this, arguments).then(function (res) {
          if (action === 'trackerSync' && data && data.provider && !(res && res.conflict)) {
            recordSyncSuccess154(data.provider);
            setTimeout(function () { renderAccountHub154(true); }, 30);
          }
          return res;
        });
      };
      wrappedReq.__k154 = true;
      window.kuroTrackerRequest553 = wrappedReq;
    }

    if (typeof window.kuroSyncPaint553 === 'function' && !window.kuroSyncPaint553.__k154) {
      var origPaint = window.kuroSyncPaint553;
      var wrappedPaint = function () {
        var r = origPaint.apply(this, arguments);
        renderAccountHub154(true);
        return r;
      };
      wrappedPaint.__k154 = true;
      window.kuroSyncPaint553 = wrappedPaint;
    }
  }

  /* ==========================================================================
     NETWORK, CACHE & COMPATIBILITY (1.5.4-BETA):
     - Encrypted DNS-over-HTTPS (1.1.1.1 / 8.8.8.8) ISP Unblock control
     - 1-tap safe temporary cache cleaner in Settings
     ========================================================================== */
  var PERF_KEY_154 = 'kuro-perf-154';
  var perfDefaults154 = {
    dohMode: 'auto',
    isTv: false,
    lowRam: false,
    ramMb: 4096,
    cacheMb: 0
  };

  function readPerf154() {
    var local = {};
    try { local = JSON.parse(localStorage.getItem(PERF_KEY_154) || '{}') || {}; } catch (_) {}
    var nat = {};
    if (window.KuroNative && typeof window.KuroNative.perfGet154 === 'function') {
      try { nat = JSON.parse(window.KuroNative.perfGet154() || '{}') || {}; } catch (_) {}
    }
    var merged = Object.assign({}, perfDefaults154, local, nat);
    try {
      localStorage.setItem(PERF_KEY_154, JSON.stringify({
        dohMode: merged.dohMode
      }));
    } catch (_) {}
    return merged;
  }

  function savePerf154(patch) {
    var cur = readPerf154();
    var next = {
      dohMode: patch.dohMode !== undefined ? patch.dohMode : cur.dohMode
    };
    try { localStorage.setItem(PERF_KEY_154, JSON.stringify(next)); } catch (_) {}
    var updated = Object.assign({}, cur, next);
    if (window.KuroNative && typeof window.KuroNative.perfSet154 === 'function') {
      try {
        var fromNative = JSON.parse(window.KuroNative.perfSet154(JSON.stringify(next)) || '{}');
        updated = Object.assign(updated, fromNative);
      } catch (_) {}
    }
    return updated;
  }

  function openPerfChoiceSheet154(overlayRoot, titleText, options, currentVal, onPick, triggerBtn) {
    if (!overlayRoot || !overlayRoot.isConnected) return;
    var prev = overlayRoot.querySelector('.choice-backdrop567');
    if (prev) prev.remove();

    var backdrop = document.createElement('div');
    backdrop.className = 'choice-backdrop567';

    var panel = document.createElement('section');
    panel.className = 'choice-panel567';
    panel.setAttribute('role', 'dialog');
    panel.setAttribute('aria-modal', 'true');

    var heading = document.createElement('div');
    heading.className = 'choice-head567';
    var copy = document.createElement('div');
    var eye = document.createElement('small');
    eye.textContent = 'KURO.TV \u00b7 NETWORK';
    var h2 = document.createElement('h2');
    h2.id = 'k154-choice-title';
    h2.textContent = titleText;
    panel.setAttribute('aria-labelledby', h2.id);
    copy.appendChild(eye);
    copy.appendChild(h2);

    var closeBtn = document.createElement('button');
    closeBtn.type = 'button';
    closeBtn.className = 'choice-close567';
    closeBtn.setAttribute('aria-label', 'Close choices');
    closeBtn.textContent = '\u00d7';
    heading.appendChild(copy);
    heading.appendChild(closeBtn);
    panel.appendChild(heading);

    var list = document.createElement('div');
    list.className = 'choice-list567';
    list.setAttribute('role', 'radiogroup');
    list.setAttribute('aria-label', titleText);

    function closeSheet() {
      if (!backdrop.isConnected) return;
      backdrop.remove();
      if (triggerBtn && triggerBtn.isConnected) {
        triggerBtn.setAttribute('aria-expanded', 'false');
        triggerBtn.focus();
      }
    }

    options.forEach(function (opt) {
      var row = document.createElement('button');
      row.type = 'button';
      row.className = 'choice-option567';
      row.setAttribute('role', 'radio');
      var active = String(opt.value) === String(currentVal);
      row.setAttribute('aria-checked', String(active));
      var span = document.createElement('span');
      span.textContent = opt.label;
      var dot = document.createElement('i');
      dot.setAttribute('aria-hidden', 'true');
      row.appendChild(span);
      row.appendChild(dot);
      row.onclick = function () {
        onPick(opt.value);
        closeSheet();
      };
      list.appendChild(row);
    });

    panel.appendChild(list);
    backdrop.appendChild(panel);
    overlayRoot.appendChild(backdrop);
    if (triggerBtn) triggerBtn.setAttribute('aria-expanded', 'true');

    closeBtn.onclick = closeSheet;
    backdrop.onclick = function (e) { if (e.target === backdrop) closeSheet(); };
    backdrop.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') {
        e.preventDefault();
        e.stopPropagation();
        closeSheet();
      }
    });
    var activeOpt = list.querySelector('[aria-checked="true"]') || closeBtn;
    if (activeOpt) activeOpt.focus();
  }

  function mountPerfSettingsGroup154() {
    var overlay = document.getElementById('settings-overlay560');
    if (!overlay) return;
    var body = overlay.querySelector('.settings-body560');
    if (!body || body.querySelector('#k154-perf-group')) return;

    var sec = document.createElement('section');
    sec.id = 'k154-perf-group';
    sec.className = 'settings-group560';
    sec.setAttribute('data-section', 'network & compatibility dns isp unblock cache storage tv');

    var appearance = body.querySelector('.settings-group560[data-section="appearance and startup"]');
    if (appearance && appearance.nextSibling) {
      body.insertBefore(sec, appearance.nextSibling);
    } else {
      body.appendChild(sec);
    }

    function renderPerfGroup() {
      var p = readPerf154();
      var devLabel = p.isTv ? 'Android TV / Fire TV' : (p.ramMb ? (Math.max(1, Math.round(p.ramMb / 1024)) + ' GB RAM') : 'Phone / Tablet');
      var dnsChip = p.dohMode === 'always' ? 'DNS: Always Encrypted' : (p.dohMode === 'auto' ? 'DNS: Smart Unblock' : 'DNS: System');
      var scanningCache = typeof p.cacheMb === 'number' && p.cacheMb < 0;
      var cacheLabel = scanningCache ? 'Scanning...' : ((typeof p.cacheMb === 'number' ? p.cacheMb.toFixed(1) : '0.0') + ' MB temp');
      if (scanningCache && !sec.__k154ScanWait) {
        sec.__k154ScanWait = true;
        setTimeout(function () {
          sec.__k154ScanWait = false;
          if (sec.isConnected) renderPerfGroup();
        }, 260);
      }

      var specs = [
        {
          key: 'dohMode',
          title: 'Network & ISP unblock (DNS)',
          desc: 'Uses encrypted Cloudflare 1.1.1.1 / Google 8.8.8.8 DNS when your carrier blocks anime sources.',
          filter: 'network isp unblock dns doh cloudflare 1.1.1.1 jio airtel wifi blocked',
          current: p.dohMode,
          options: [
            { value: 'auto', label: 'Smart Auto (recommended)' },
            { value: 'always', label: 'Always Encrypted DNS' },
            { value: 'off', label: 'System default DNS' }
          ]
        }
      ];

      var rowsHtml = specs.map(function (sp) {
        var found = sp.options.find(function (o) { return String(o.value) === String(sp.current); }) || sp.options[0];
        return '<div class="settings-row560" data-filter="' + esc(sp.filter) + '">' +
          '<span><strong>' + esc(sp.title) + '</strong><small>' + esc(sp.desc) + '</small></span>' +
          '<button type="button" class="settings-choice567" data-k154-perf-key="' + esc(sp.key) + '" aria-haspopup="dialog" aria-expanded="false" aria-label="' + esc(sp.title + ': ' + found.label) + '">' +
            '<span class="choice-value567">' + esc(found.label) + '</span>' +
            '<span class="choice-chevron567" aria-hidden="true">\u2304</span>' +
          '</button>' +
        '</div>';
      }).join('');

      var clearHtml = '<button type="button" class="settings-row560 settings-action560" data-k154-clear-cache data-filter="clear temporary stream cache storage memory free space">' +
        '<span><strong>Clear temporary stream cache</strong>' +
        '<small>Frees temporary web & poster files (' + esc(cacheLabel) + '). Keeps accounts, lists, history & offline downloads.</small></span>' +
        '<b aria-hidden="true">\u203a</b>' +
      '</button>';

      sec.innerHTML =
        '<div class="settings-group-head560">' +
          '<h3>Network &amp; compatibility</h3>' +
          '<p>Built-in carrier DNS unblock, temporary cache cleanup, and TV/low-RAM stability</p>' +
        '</div>' +
        '<div class="k154-perf-summary">' +
          '<span class="k154-perf-chip is-accent">' + esc(dnsChip) + '</span>' +
          '<span class="k154-perf-chip">' + esc(devLabel) + '</span>' +
          '<span class="k154-perf-chip">' + esc(cacheLabel) + '</span>' +
        '</div>' +
        '<div class="settings-rows560">' + rowsHtml + clearHtml + '</div>';

      specs.forEach(function (sp) {
        var btn = sec.querySelector('[data-k154-perf-key="' + sp.key + '"]');
        if (!btn) return;
        btn.onclick = function (e) {
          e.preventDefault();
          e.stopPropagation();
          openPerfChoiceSheet154(overlay, sp.title, sp.options, sp.current, function (picked) {
            var patch = {};
            patch[sp.key] = picked;
            savePerf154(patch);
            renderPerfGroup();
            toast(sp.title + ' saved.');
          }, btn);
        };
      });

      var clearBtn = sec.querySelector('[data-k154-clear-cache]');
      if (clearBtn) {
        clearBtn.onclick = function () {
          var freedText = '';
          if (window.KuroNative && typeof window.KuroNative.clearTempCache154 === 'function') {
            try {
              var res = JSON.parse(window.KuroNative.clearTempCache154() || '{}');
              if (typeof res.freedMb === 'number' && res.freedMb > 0) {
                freedText = ' (' + res.freedMb.toFixed(1) + ' MB freed)';
              }
            } catch (_) {}
          }
          renderPerfGroup();
          toast('Temporary cache cleared' + freedText + '. Accounts, lists & downloads kept.');
        };
      }
    }

    renderPerfGroup();
  }

  function isBadgeOnlyMutation154(records) {
    if (!records || !records.length) return false;
    for (var i = 0; i < records.length; i++) {
      var rec = records[i];
      var added = rec.addedNodes;
      var removed = rec.removedNodes;
      if ((!added || !added.length) && (!removed || !removed.length)) continue;
      if (removed && removed.length) return false;
      for (var j = 0; j < added.length; j++) {
        var n = added[j];
        if (!n || n.nodeType !== 1 || !n.classList) return false;
        if (!n.classList.contains('k154-poster-score') && !n.classList.contains('k154-poster-split')) {
          return false;
        }
      }
    }
    return true;
  }

  /* Observe DOM mutations so Home shelves, Browse grids, Account Hub, Settings & Player overlays stay clean */
  function startObserver154() {
    var rootEl = document.body || document.getElementById('main');
    if (!rootEl) return;
    var rafPending = false;
    var obs = new MutationObserver(function (records) {
      if (rafPending || isBadgeOnlyMutation154(records)) return;
      rafPending = true;
      requestAnimationFrame(function () {
        rafPending = false;
        pruneHiddenControls154();
        if (document.getElementById('discover-search')) {
          enhanceBrowseCommandBar154();
        }
        if (document.getElementById('settings-overlay560')) {
          mountPerfSettingsGroup154();
        }
        if (document.querySelector('#main > .profile-page547')) {
          renderAccountHub154(false);
        }
        decorateRenderedPosters154();
      });
    });
    obs.observe(rootEl, { childList: true, subtree: true });
  }

  function init154() {
    syncDesktopSite154();
    pruneHiddenControls154();
    hookDetailView154();
    hookBrowseFilters154();
    hookAccountTrackerHub154();
    startObserver154();
    if (document.getElementById('settings-overlay560')) {
      mountPerfSettingsGroup154();
    }
    if (document.querySelector('#main > .profile-page547')) {
      renderAccountHub154(false);
    }
    decorateRenderedPosters154();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init154);
  } else {
    init154();
  }
})();
