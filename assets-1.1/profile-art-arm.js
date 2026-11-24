/* Kuro Profile Art Studio (1.5.5-beta10) — 40 anime & manga face avatars + 40 matching
   16:9 banners + device gallery photo upload. Uses real <img> elements everywhere so
   strict CSP (style-src 'self'; img-src 'self' https: data:) renders every PFP and
   banner across the Account hero, Live Preview stage, Header chip, Profile drawer,
   and Native Watch Header without inline-style violations. */
(function () {
  'use strict';

  var KEY = 'kuro-profile-art-v1';
  var CUSTOM_KEY = 'kuro-profile-art-custom-155';
  var PROFILE_KEY = 'kuro-profile-v1';

  var META = [
    ['art-11', 'Satoru Gojo \u00b7 Jujutsu Kaisen'],
    ['art-12', 'Ryomen Sukuna \u00b7 Jujutsu Kaisen'],
    ['art-13', 'Yuta Okkotsu \u00b7 Jujutsu Kaisen 0'],
    ['art-14', 'Toji Fushiguro \u00b7 Jujutsu Kaisen'],
    ['art-15', 'Sung Jinwoo \u00b7 Solo Leveling'],
    ['art-16', 'Light Yagami \u00b7 Death Note'],
    ['art-17', 'Denji \u00b7 Chainsaw Man'],
    ['art-18', 'Makima \u00b7 Chainsaw Man'],
    ['art-19', 'Asta \u00b7 Black Clover'],
    ['art-20', 'Ichigo Kurosaki \u00b7 Bleach'],
    ['art-21', 'Sosuke Aizen \u00b7 Bleach: TYBW'],
    ['art-22', 'Frieren \u00b7 Beyond Journey\u2019s End'],
    ['art-23', 'Mash Burnedead \u00b7 Mashle'],
    ['art-24', 'Goku \u00b7 Dragon Ball Z'],
    ['art-25', 'Gabimaru \u00b7 Hell\u2019s Paradise'],
    ['art-26', 'Akame \u00b7 Akame ga Kill!'],
    ['art-27', 'Kaoruko Waguri \u00b7 Fragrant Flower'],
    ['art-28', 'Eren Yeager \u00b7 Attack on Titan'],
    ['art-29', 'Levi Ackerman \u00b7 Attack on Titan'],
    ['art-30', 'Nagumo \u00b7 Sakamoto Days'],
    ['art-31', 'Ai Hoshino \u00b7 Oshi no Ko'],
    ['art-32', 'Buddha \u00b7 Record of Ragnarok'],
    ['art-33', 'Johan Liebert \u00b7 Monster'],
    ['art-34', 'Yuuichi Katagiri \u00b7 Tomodachi Game'],
    ['art-35', 'Monkey D. Luffy (Gear 5) \u00b7 One Piece'],
    ['art-36', 'Roronoa Zoro \u00b7 One Piece'],
    ['art-37', 'Itachi Uchiha \u00b7 Naruto Shippuden'],
    ['art-38', 'Naruto Uzumaki \u00b7 Naruto Shippuden'],
    ['art-39', 'Sasuke Uchiha \u00b7 Naruto Shippuden'],
    ['art-40', 'Tanjiro Kamado \u00b7 Demon Slayer'],
    ['art-41', 'Kyojuro Rengoku \u00b7 Demon Slayer: Mugen Train'],
    ['art-42', 'Killua Zoldyck \u00b7 Hunter \u00d7 Hunter'],
    ['art-43', 'Ken Kaneki \u00b7 Tokyo Ghoul'],
    ['art-44', 'Thorfinn \u00b7 Vinland Saga'],
    ['art-45', 'Guts \u00b7 Berserk'],
    ['art-46', 'Lelouch Lamperouge \u00b7 Code Geass'],
    ['art-47', 'Power \u00b7 Chainsaw Man'],
    ['art-48', 'Okarun (Turbo Granny) \u00b7 DAN DA DAN'],
    ['art-49', 'Zero Two \u00b7 DARLING in the FRANXX'],
    ['art-50', 'Spike Spiegel \u00b7 Cowboy Bebop']
  ];

  var IDS = META.map(function (m) { return m[0]; });
  var LABELS = {};
  var SPLIT_NAMES = {};
  META.forEach(function (m) {
    LABELS[m[0]] = m[1];
    var parts = m[1].split(' \u00b7 ');
    SPLIT_NAMES[m[0]] = {
      charName: parts[0] || m[1],
      series: parts[1] || 'Anime'
    };
  });

  var uiState = {
    tab: 'split',        // 'split' | 'pairs' | 'pfp' | 'banner'
    query: '',
    expandPfp: false,
    expandBanner: false,
    toast: '',
    toastTimer: null
  };

  function src(kind, id) {
    return 'profile-art/' + kind + '/' + id + '.webp';
  }

  function valid(id) {
    return IDS.indexOf(id) >= 0;
  }

  function validDataUrl(v) {
    return typeof v === 'string' && /^data:image\/(?:jpeg|png|webp);base64,[A-Za-z0-9+/=]+$/.test(v) && v.length <= 650000;
  }

  function esc(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;')
      .replace(/"/g, '&quot;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
  }

  function read() {
    try {
      var raw = localStorage.getItem(KEY);
      if (!raw) return {};
      var o = JSON.parse(raw);
      if (!o || typeof o !== 'object' || Array.isArray(o)) return {};
      var pfp = valid(o.pfp || o.avatar || '') ? (o.pfp || o.avatar) : '';
      var banner = valid(o.banner || '') ? o.banner : '';
      var clean = {};
      if (pfp) clean.pfp = pfp;
      if (banner) clean.banner = banner;
      return clean;
    } catch (_) {
      return {};
    }
  }

  function save(o) {
    try {
      var clean = {};
      if (valid(o.pfp || '')) clean.pfp = o.pfp;
      if (valid(o.banner || '')) clean.banner = o.banner;
      localStorage.setItem(KEY, JSON.stringify(clean));
    } catch (_) {}
  }

  function readCustom() {
    try {
      var raw = localStorage.getItem(CUSTOM_KEY);
      if (!raw) return {};
      var o = JSON.parse(raw);
      if (!o || typeof o !== 'object' || Array.isArray(o)) return {};
      var out = {};
      if (validDataUrl(o.pfp)) out.pfp = o.pfp;
      if (validDataUrl(o.banner)) out.banner = o.banner;
      return out;
    } catch (_) {
      return {};
    }
  }

  function saveCustom(c) {
    try {
      var clean = {};
      if (validDataUrl(c.pfp)) clean.pfp = c.pfp;
      if (validDataUrl(c.banner)) clean.banner = c.banner;
      if (clean.pfp || clean.banner) {
        localStorage.setItem(CUSTOM_KEY, JSON.stringify(clean));
      } else {
        localStorage.removeItem(CUSTOM_KEY);
      }
    } catch (_) {}
  }

  function readProfileInfo() {
    var name = 'Kuro Viewer';
    try {
      var raw = localStorage.getItem(PROFILE_KEY);
      if (raw) {
        var p = JSON.parse(raw);
        if (p && typeof p.name === 'string' && p.name.trim()) {
          name = p.name.trim();
        }
      }
    } catch (_) {}
    var heroName = document.querySelector('.profile-hero547 .profile-title strong');
    if (heroName && heroName.textContent && heroName.textContent.trim()) {
      name = heroName.textContent.trim();
    }
    var parts = name.trim().split(/\s+/).filter(Boolean);
    var mono = 'K';
    if (parts.length >= 2) {
      mono = (parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
    } else if (parts.length === 1 && parts[0].length > 0) {
      mono = parts[0].slice(0, 2).toUpperCase();
    }
    return { name: name, mono: mono };
  }

  function resolveState() {
    var o = read();
    var c = readCustom();
    var pfpId = c.pfp ? 'custom' : (valid(o.pfp || '') ? o.pfp : '');
    var bannerId = c.banner ? 'custom' : (valid(o.banner || '') ? o.banner : '');
    var pfpUrl = pfpId === 'custom' ? c.pfp : (pfpId ? src('pfp', pfpId) : '');
    var bannerUrl = bannerId === 'custom' ? c.banner : (bannerId ? src('banner', bannerId) : '');
    var pfpLabel = pfpId === 'custom' ? 'Custom photo' : (pfpId ? LABELS[pfpId] : 'Initials');
    var bannerLabel = bannerId === 'custom' ? 'Custom 16:9 banner' : (bannerId ? LABELS[bannerId] : 'Default backdrop');
    return {
      o: o,
      c: c,
      pfpId: pfpId,
      bannerId: bannerId,
      pfpUrl: pfpUrl,
      bannerUrl: bannerUrl,
      pfpLabel: pfpLabel,
      bannerLabel: bannerLabel,
      isPaired: !!(pfpId && pfpId !== 'custom' && pfpId === bannerId),
      canMatch: !!((pfpId && pfpId !== 'custom' && bannerId !== pfpId) || ((!pfpId || pfpId === 'custom') && bannerId && bannerId !== 'custom'))
    };
  }

  function showToast(msg) {
    uiState.toast = msg || '';
    if (uiState.toastTimer) clearTimeout(uiState.toastTimer);
    var el = document.querySelector('#profile-art581 .pa155-toast');
    if (el) {
      el.textContent = uiState.toast;
      el.hidden = !uiState.toast;
    }
    if (msg) {
      uiState.toastTimer = setTimeout(function () {
        uiState.toast = '';
        var node = document.querySelector('#profile-art581 .pa155-toast');
        if (node) {
          node.hidden = true;
          node.textContent = '';
        }
      }, 2400);
    }
  }

  function filteredIds() {
    var q = (uiState.query || '').trim().toLowerCase();
    if (!q) return IDS;
    return IDS.filter(function (id) {
      var label = (LABELS[id] || '').toLowerCase();
      return label.indexOf(q) >= 0 || id.indexOf(q) >= 0;
    });
  }

  function renderPfpGrid(st, ids, limitCount) {
    var searching = !!(uiState.query && uiState.query.trim());
    var shown = (!searching && limitCount > 0 && !uiState.expandPfp) ? ids.slice(0, limitCount) : ids;
    var html = '';

    if (!searching) {
      var noneOn = !st.pfpId;
      var info = readProfileInfo();
      html += '<button type="button" class="art-tile581 pa155-pfp-card pa155-special-card' + (noneOn ? ' is-on' : '') + '" data-kind="pfp" data-id="" aria-pressed="' + (noneOn ? 'true' : 'false') + '">' +
        '<span class="pa155-pfp-frame pa155-special-frame"><span class="pa155-mono-sample">' + esc(info.mono) + '</span>' +
        (noneOn ? '<span class="pa155-check" aria-hidden="true">\u2713</span>' : '') +
        '</span>' +
        '<strong class="pa155-card-title">Initials</strong>' +
        '<small class="pa155-card-sub">Default badge</small>' +
        '</button>';

      var customOn = st.pfpId === 'custom';
      html += '<button type="button" class="pa155-pfp-card pa155-upload-card' + (customOn ? ' is-on' : '') + '" data-pa-action="upload-pfp" aria-pressed="' + (customOn ? 'true' : 'false') + '">' +
        '<span class="pa155-pfp-frame pa155-special-frame">' +
        (customOn ? '<img src="' + esc(st.pfpUrl) + '" alt="">' : '<span class="pa155-plus-glyph" aria-hidden="true">+</span>') +
        (customOn ? '<span class="pa155-check" aria-hidden="true">\u2713</span>' : '') +
        '</span>' +
        '<strong class="pa155-card-title">' + (customOn ? 'Custom Photo' : 'Upload Photo') + '</strong>' +
        '<small class="pa155-card-sub">From device</small>' +
        '</button>';
    }

    for (var i = 0; i < shown.length; i++) {
      var id = shown[i];
      var meta = SPLIT_NAMES[id] || { charName: id, series: '' };
      var on = st.pfpId === id;
      html += '<button type="button" class="art-tile581 pa155-pfp-card' + (on ? ' is-on' : '') + '" data-kind="pfp" data-id="' + id + '" title="' + esc(LABELS[id]) + '" aria-label="Avatar: ' + esc(LABELS[id]) + '" aria-pressed="' + (on ? 'true' : 'false') + '">' +
        '<span class="pa155-pfp-frame">' +
        '<img src="' + src('pfp', id) + '" alt="" loading="lazy" decoding="async">' +
        (on ? '<span class="pa155-check" aria-hidden="true">\u2713</span>' : '') +
        '</span>' +
        '<strong class="pa155-card-title">' + esc(meta.charName) + '</strong>' +
        '<small class="pa155-card-sub">' + esc(meta.series) + '</small>' +
        '</button>';
    }

    if (!shown.length && searching) {
      return '<div class="pa155-empty">No character avatars match \u201c' + esc(uiState.query.trim()) + '\u201d.</div>';
    }

    var out = '<div class="pa155-pfp-grid">' + html + '</div>';
    if (!searching && limitCount > 0 && ids.length > limitCount) {
      out += '<button type="button" class="pa155-more-btn" data-pa-action="toggle-pfp">' +
        (uiState.expandPfp ? 'Show fewer avatars \u25b4' : 'Show all ' + ids.length + ' avatars \u25be') +
        '</button>';
    }
    return out;
  }

  function renderBannerGrid(st, ids, limitCount) {
    var searching = !!(uiState.query && uiState.query.trim());
    var shown = (!searching && limitCount > 0 && !uiState.expandBanner) ? ids.slice(0, limitCount) : ids;
    var html = '';

    if (!searching) {
      var noneOn = !st.bannerId;
      html += '<button type="button" class="art-tile581 pa155-banner-card pa155-special-card' + (noneOn ? ' is-on' : '') + '" data-kind="banner" data-id="" aria-pressed="' + (noneOn ? 'true' : 'false') + '">' +
        '<span class="pa155-banner-frame pa155-special-banner">' +
        '<span class="pa155-none-tag">Theme Gradient</span>' +
        (noneOn ? '<span class="pa155-check" aria-hidden="true">\u2713</span>' : '') +
        '</span>' +
        '<span class="pa155-banner-caption"><strong>None</strong><small>Default hero backdrop</small></span>' +
        '</button>';

      var customOn = st.bannerId === 'custom';
      html += '<button type="button" class="pa155-banner-card pa155-upload-card' + (customOn ? ' is-on' : '') + '" data-pa-action="upload-banner" aria-pressed="' + (customOn ? 'true' : 'false') + '">' +
        '<span class="pa155-banner-frame pa155-special-banner">' +
        (customOn ? '<img src="' + esc(st.bannerUrl) + '" alt="">' : '<span class="pa155-none-tag">+ Upload 16:9 Image</span>') +
        (customOn ? '<span class="pa155-check" aria-hidden="true">\u2713</span>' : '') +
        '</span>' +
        '<span class="pa155-banner-caption"><strong>' + (customOn ? 'Custom Banner' : 'Upload Banner') + '</strong><small>Choose from gallery</small></span>' +
        '</button>';
    }

    for (var i = 0; i < shown.length; i++) {
      var id = shown[i];
      var meta = SPLIT_NAMES[id] || { charName: id, series: '' };
      var on = st.bannerId === id;
      html += '<button type="button" class="art-tile581 pa155-banner-card' + (on ? ' is-on' : '') + '" data-kind="banner" data-id="' + id + '" title="' + esc(LABELS[id]) + '" aria-label="Banner: ' + esc(LABELS[id]) + '" aria-pressed="' + (on ? 'true' : 'false') + '">' +
        '<span class="pa155-banner-frame">' +
        '<img src="' + src('banner', id) + '" alt="" loading="lazy" decoding="async">' +
        (on ? '<span class="pa155-check" aria-hidden="true">\u2713</span>' : '') +
        '</span>' +
        '<span class="pa155-banner-caption"><strong>' + esc(meta.charName) + '</strong><small>' + esc(meta.series) + '</small></span>' +
        '</button>';
    }

    if (!shown.length && searching) {
      return '<div class="pa155-empty">No 16:9 banners match \u201c' + esc(uiState.query.trim()) + '\u201d.</div>';
    }

    var out = '<div class="pa155-banner-grid">' + html + '</div>';
    if (!searching && limitCount > 0 && ids.length > limitCount) {
      out += '<button type="button" class="pa155-more-btn" data-pa-action="toggle-banner">' +
        (uiState.expandBanner ? 'Show fewer banners \u25b4' : 'Show all ' + ids.length + ' banners \u25be') +
        '</button>';
    }
    return out;
  }

  function renderPairsGrid(st, ids) {
    if (!ids.length) {
      return '<div class="pa155-empty">No paired character sets match \u201c' + esc(uiState.query.trim()) + '\u201d.</div>';
    }
    var html = '';
    for (var i = 0; i < ids.length; i++) {
      var id = ids[i];
      var meta = SPLIT_NAMES[id] || { charName: id, series: '' };
      var on = st.pfpId === id && st.bannerId === id;
      html += '<button type="button" class="pa155-pair-card' + (on ? ' is-on' : '') + '" data-pair-id="' + id + '" aria-label="Apply avatar and banner: ' + esc(LABELS[id]) + '" aria-pressed="' + (on ? 'true' : 'false') + '">' +
        '<span class="pa155-pair-art">' +
        '<img class="pa155-pair-ban" src="' + src('banner', id) + '" alt="" loading="lazy" decoding="async">' +
        '<span class="pa155-pair-shade"></span>' +
        '<span class="pa155-pair-pfp"><img src="' + src('pfp', id) + '" alt="" loading="lazy" decoding="async"></span>' +
        (on ? '<span class="pa155-check" aria-hidden="true">\u2713</span>' : '') +
        '</span>' +
        '<span class="pa155-banner-caption"><strong>' + esc(meta.charName) + '</strong><small>' + esc(meta.series) + '</small></span>' +
        '</button>';
    }
    return '<div class="pa155-pairs-grid">' + html + '</div>';
  }

  function renderBodyContent(st) {
    var ids = filteredIds();
    if (uiState.tab === 'pairs') {
      return '<div class="pa155-subhead"><span>PAIRED SETS \u00b7 1-TAP AVATAR + 16:9 BANNER</span><small>' + ids.length + ' sets</small></div>' +
        renderPairsGrid(st, ids);
    }
    if (uiState.tab === 'pfp') {
      return '<div class="pa155-subhead"><span>ALL CHARACTER AVATARS</span><small>' + ids.length + ' portraits</small></div>' +
        renderPfpGrid(st, ids, 0);
    }
    if (uiState.tab === 'banner') {
      return '<div class="pa155-subhead"><span>ALL 16:9 WIDESCREEN BANNERS</span><small>' + ids.length + ' scenes</small></div>' +
        renderBannerGrid(st, ids, 0);
    }
    return '<div class="pa155-subhead"><span>AVATAR PORTRAIT</span><small>' + (st.pfpId ? esc(st.pfpLabel) : 'Initials') + '</small></div>' +
      renderPfpGrid(st, ids, 10) +
      '<div class="pa155-subhead pa155-subhead-gap"><span>16:9 PROFILE BANNER</span><small>' + (st.bannerId ? esc(st.bannerLabel) : 'None') + '</small></div>' +
      renderBannerGrid(st, ids, 6);
  }

  function buildStudioShell() {
    var el = document.createElement('section');
    el.className = 'account-section profile-art581 pa155-studio';
    el.id = 'profile-art581';
    el.innerHTML =
      '<button type="button" class="pa155-collapse-bar" data-pa-action="toggle-expand" aria-expanded="false" aria-controls="pa155-expand-body">' +
        '<span class="pa155-collapse-thumb"><img class="pa155-stage-pfp-img" alt="" hidden><span class="pa155-stage-mono" aria-hidden="true">K</span></span>' +
        '<span class="pa155-collapse-text"><strong>Profile art</strong><small>Tap to expand options</small></span>' +
        '<span class="pa155-collapse-chevron" aria-hidden="true">\u25be</span>' +
      '</button>' +
      '<div class="pa155-expand-body" id="pa155-expand-body" hidden>' +
      '<div class="pa155-header">' +
        '<div>' +
          '<div class="pa155-title-row"><h2>Profile art</h2><span class="pa155-count-pill">40 SETS + CUSTOM</span></div>' +
          '<p>Tap any anime avatar, 16:9 banner, or paired set \u2014 or upload any image from your phone.</p>' +
        '</div>' +
      '</div>' +
      '<div class="pa155-stage">' +
        '<div class="pa155-stage-banner">' +
          '<img class="pa155-stage-banner-img" alt="" hidden>' +
          '<div class="pa155-stage-fallback"><span>Default Theme Backdrop</span></div>' +
          '<div class="pa155-stage-shade"></div>' +
          '<span class="pa155-stage-badge">LIVE PREVIEW</span>' +
        '</div>' +
        '<div class="pa155-stage-bar">' +
          '<div class="pa155-stage-avatar">' +
            '<img class="pa155-stage-pfp-img" alt="" hidden>' +
            '<span class="pa155-stage-mono" aria-hidden="true">K</span>' +
          '</div>' +
          '<div class="pa155-stage-meta">' +
            '<strong class="pa155-stage-user">Kuro Viewer</strong>' +
            '<span class="pa155-stage-line pa155-line-pfp">Avatar: Initials</span>' +
            '<span class="pa155-stage-line pa155-line-ban">Banner: Default backdrop</span>' +
          '</div>' +
        '</div>' +
      '</div>' +
      '<div class="pa155-actions">' +
        '<button type="button" class="pa155-btn pa155-btn-accent" data-art-match="1" data-pa-action="match">Match banner</button>' +
        '<button type="button" class="pa155-btn" data-pa-action="random">Surprise pair</button>' +
        '<button type="button" class="pa155-btn" data-pa-action="upload-pfp">Upload PFP</button>' +
        '<button type="button" class="pa155-btn" data-pa-action="upload-banner">Upload banner</button>' +
        '<button type="button" class="pa155-btn pa155-btn-subtle" data-pa-action="reset">Reset</button>' +
      '</div>' +
      '<div class="pa155-toast" role="status" aria-live="polite" hidden></div>' +
      '<div class="pa155-controls-row">' +
        '<div class="pa155-tabs" role="tablist" aria-label="Profile art views">' +
          '<button type="button" role="tab" class="pa155-tab is-active" data-pa-tab="split" aria-selected="true">Split View</button>' +
          '<button type="button" role="tab" class="pa155-tab" data-pa-tab="pairs" aria-selected="false">Paired Sets</button>' +
          '<button type="button" role="tab" class="pa155-tab" data-pa-tab="pfp" aria-selected="false">Avatars (40)</button>' +
          '<button type="button" role="tab" class="pa155-tab" data-pa-tab="banner" aria-selected="false">Banners (40)</button>' +
        '</div>' +
        '<div class="pa155-search-wrap">' +
          '<input type="search" class="pa155-search" placeholder="Search character or anime (Gojo, Zero Two, Luffy, Berserk\u2026)" autocomplete="off" aria-label="Search profile art">' +
          '<button type="button" class="pa155-search-clear" data-pa-action="clear-search" aria-label="Clear search" hidden>\u00d7</button>' +
        '</div>' +
      '</div>' +
      '<div class="pa155-body"></div>' +
      '<input type="file" accept="image/*" class="pa155-file-input" data-file-kind="" hidden>' +
      '</div>';
    return el;
  }

  function triggerDeviceImagePick(host, kind) {
    var nativeKind = kind === 'banner' ? 'banner' : 'avatar';
    if (window.KuroNative && typeof window.KuroNative.pickProfileImage155 === 'function') {
      try {
        window.KuroNative.pickProfileImage155(nativeKind);
        return;
      } catch (_) {}
    }
    var fileInput = host && host.querySelector('.pa155-file-input');
    if (fileInput) {
      fileInput.setAttribute('data-file-kind', nativeKind);
      fileInput.value = '';
      fileInput.click();
    }
  }

  function cropFileToDataUrl(file, kind, cb) {
    if (!file || typeof FileReader === 'undefined') return;
    var reader = new FileReader();
    reader.onload = function () {
      var img = new Image();
      img.onload = function () {
        try {
          var tw = kind === 'banner' ? 1000 : 320;
          var th = kind === 'banner' ? 400 : 320;
          var bw = img.naturalWidth || img.width;
          var bh = img.naturalHeight || img.height;
          if (!bw || !bh) return;
          var cw = bw, ch = bh;
          if (bw * th > bh * tw) {
            cw = Math.max(1, Math.round((bh * tw) / th));
          } else {
            ch = Math.max(1, Math.round((bw * th) / tw));
          }
          var sx = Math.max(0, Math.round((bw - cw) / 2));
          var sy = Math.max(0, Math.round((bh - ch) / 2));
          var canvas = document.createElement('canvas');
          canvas.width = tw;
          canvas.height = th;
          var ctx = canvas.getContext('2d');
          ctx.drawImage(img, sx, sy, cw, ch, 0, 0, tw, th);
          var out = canvas.toDataURL('image/jpeg', 0.84);
          if (validDataUrl(out)) cb(out);
        } catch (_) {}
      };
      img.src = String(reader.result || '');
    };
    reader.readAsDataURL(file);
  }

  function applyCustomPickedImage(kind, dataUrl) {
    if (!validDataUrl(dataUrl)) return;
    var o = read();
    var c = readCustom();
    if (kind === 'banner') {
      c.banner = dataUrl;
      delete o.banner;
      saveCustom(c);
      save(o);
      showToast('Custom 16:9 banner applied!');
    } else {
      c.pfp = dataUrl;
      delete o.pfp;
      saveCustom(c);
      save(o);
      showToast('Custom avatar applied!');
    }
    paint(true);
  }

  function updateStageDom(host, st) {
    var info = readProfileInfo();
    var banImg = host.querySelector('.pa155-stage-banner-img');
    var banFallback = host.querySelector('.pa155-stage-fallback');
    var stageBadge = host.querySelector('.pa155-stage-badge');
    if (banImg) {
      if (st.bannerUrl) {
        if (banImg.getAttribute('src') !== st.bannerUrl) banImg.setAttribute('src', st.bannerUrl);
        banImg.hidden = false;
      } else {
        banImg.removeAttribute('src');
        banImg.hidden = true;
      }
    }
    if (banFallback) banFallback.hidden = !!st.bannerUrl;
    if (stageBadge) {
      stageBadge.textContent = st.isPaired ? 'PAIRED SET' : ((st.pfpId === 'custom' || st.bannerId === 'custom') ? 'CUSTOM ART' : 'LIVE PREVIEW');
      stageBadge.classList.toggle('is-paired', st.isPaired);
    }

    var pfpImgs = host.querySelectorAll('.pa155-stage-pfp-img');
    var monoEls = host.querySelectorAll('.pa155-stage-mono');
    for (var pi = 0; pi < pfpImgs.length; pi++) {
      var pfpImg = pfpImgs[pi];
      if (st.pfpUrl) {
        if (pfpImg.getAttribute('src') !== st.pfpUrl) pfpImg.setAttribute('src', st.pfpUrl);
        pfpImg.hidden = false;
      } else {
        pfpImg.removeAttribute('src');
        pfpImg.hidden = true;
      }
    }
    for (var mi = 0; mi < monoEls.length; mi++) {
      monoEls[mi].textContent = info.mono;
      monoEls[mi].hidden = !!st.pfpUrl;
    }

    var userEl = host.querySelector('.pa155-stage-user');
    var linePfp = host.querySelector('.pa155-line-pfp');
    var lineBan = host.querySelector('.pa155-line-ban');
    if (userEl) userEl.textContent = info.name;
    if (linePfp) linePfp.textContent = 'Avatar: ' + st.pfpLabel;
    if (lineBan) lineBan.textContent = 'Banner: ' + st.bannerLabel;

    var matchBtn = host.querySelector('[data-pa-action="match"]');
    if (matchBtn) {
      matchBtn.disabled = !st.canMatch;
      if (st.pfpId && st.pfpId !== 'custom' && st.bannerId !== st.pfpId) {
        matchBtn.textContent = 'Match banner';
      } else if ((!st.pfpId || st.pfpId === 'custom') && st.bannerId && st.bannerId !== 'custom') {
        matchBtn.textContent = 'Match avatar';
      } else {
        matchBtn.textContent = st.isPaired ? 'Banner matched' : 'Match banner';
      }
    }

    var resetBtn = host.querySelector('[data-pa-action="reset"]');
    if (resetBtn) {
      resetBtn.disabled = !st.pfpId && !st.bannerId;
    }

    var tabs = host.querySelectorAll('.pa155-tab');
    for (var i = 0; i < tabs.length; i++) {
      var active = tabs[i].getAttribute('data-pa-tab') === uiState.tab;
      tabs[i].classList.toggle('is-active', active);
      tabs[i].setAttribute('aria-selected', active ? 'true' : 'false');
    }

    var clearBtn = host.querySelector('.pa155-search-clear');
    if (clearBtn) clearBtn.hidden = !uiState.query;
  }

  function bindStudioEvents(host) {
    if (host.dataset.bound155 === '1') return;
    host.dataset.bound155 = '1';

    var searchInput = host.querySelector('.pa155-search');
    if (searchInput) {
      searchInput.addEventListener('input', function () {
        uiState.query = searchInput.value || '';
        var clearBtn = host.querySelector('.pa155-search-clear');
        if (clearBtn) clearBtn.hidden = !uiState.query;
        var body = host.querySelector('.pa155-body');
        if (body) body.innerHTML = renderBodyContent(resolveState());
      });
    }

    var fileInput = host.querySelector('.pa155-file-input');
    if (fileInput) {
      fileInput.addEventListener('change', function () {
        var f = fileInput.files && fileInput.files[0];
        var fk = fileInput.getAttribute('data-file-kind') || 'avatar';
        if (f) cropFileToDataUrl(f, fk, function (dataUrl) { applyCustomPickedImage(fk, dataUrl); });
      });
    }

    host.addEventListener('click', function (ev) {
      var target = ev.target;
      if (!target || typeof target.closest !== 'function') return;

      var tabBtn = target.closest('[data-pa-tab]');
      if (tabBtn) {
        var nextTab = tabBtn.getAttribute('data-pa-tab') || 'split';
        if (uiState.tab !== nextTab) {
          uiState.tab = nextTab;
          var stTab = resolveState();
          updateStageDom(host, stTab);
          var bodyTab = host.querySelector('.pa155-body');
          if (bodyTab) bodyTab.innerHTML = renderBodyContent(stTab);
        }
        return;
      }

      var actBtn = target.closest('[data-pa-action]');
      if (actBtn) {
        var act = actBtn.getAttribute('data-pa-action');
        if (act === 'match') {
          var stM = resolveState();
          var om = read();
          var cm = readCustom();
          if (stM.pfpId && stM.pfpId !== 'custom') {
            om.banner = stM.pfpId;
            delete cm.banner;
            saveCustom(cm);
            save(om);
            showToast('Paired matching 16:9 banner: ' + (SPLIT_NAMES[stM.pfpId] ? SPLIT_NAMES[stM.pfpId].charName : stM.pfpId));
            paint(true);
          } else if (stM.bannerId && stM.bannerId !== 'custom') {
            om.pfp = stM.bannerId;
            delete cm.pfp;
            saveCustom(cm);
            save(om);
            showToast('Paired matching avatar: ' + (SPLIT_NAMES[stM.bannerId] ? SPLIT_NAMES[stM.bannerId].charName : stM.bannerId));
            paint(true);
          }
          return;
        }
        if (act === 'random') {
          var stR = resolveState();
          var pool = IDS.filter(function (x) { return x !== stR.pfpId; });
          var pick = pool[Math.floor(Math.random() * pool.length)] || IDS[0];
          saveCustom({});
          save({ pfp: pick, banner: pick });
          showToast('Applied pair: ' + (LABELS[pick] || pick));
          paint(true);
          return;
        }
        if (act === 'reset') {
          saveCustom({});
          save({});
          showToast('Reset to default initials & backdrop');
          paint(true);
          return;
        }
        if (act === 'upload-pfp') {
          triggerDeviceImagePick(host, 'avatar');
          return;
        }
        if (act === 'upload-banner') {
          triggerDeviceImagePick(host, 'banner');
          return;
        }
        if (act === 'toggle-pfp') {
          uiState.expandPfp = !uiState.expandPfp;
          var bodyP = host.querySelector('.pa155-body');
          if (bodyP) bodyP.innerHTML = renderBodyContent(resolveState());
          return;
        }
        if (act === 'toggle-banner') {
          uiState.expandBanner = !uiState.expandBanner;
          var bodyB = host.querySelector('.pa155-body');
          if (bodyB) bodyB.innerHTML = renderBodyContent(resolveState());
          return;
        }
        if (act === 'toggle-expand') {
          var expandBody = host.querySelector('#pa155-expand-body');
          var bar = target.closest('.pa155-collapse-bar');
          var chevron = host.querySelector('.pa155-collapse-chevron');
          var isOpen = expandBody && !expandBody.hidden;
          if (expandBody) expandBody.hidden = isOpen;
          if (bar) bar.setAttribute('aria-expanded', String(!isOpen));
          if (chevron) chevron.textContent = isOpen ? '\u25be' : '\u25b4';
          if (!isOpen) {
            var bodyE = host.querySelector('.pa155-body');
            if (bodyE) bodyE.innerHTML = renderBodyContent(resolveState());
          }
          return;
        }
        if (act === 'clear-search') {
          uiState.query = '';
          var sIn = host.querySelector('.pa155-search');
          if (sIn) {
            sIn.value = '';
            sIn.focus();
          }
          var cBtn = host.querySelector('.pa155-search-clear');
          if (cBtn) cBtn.hidden = true;
          var bodyC = host.querySelector('.pa155-body');
          if (bodyC) bodyC.innerHTML = renderBodyContent(resolveState());
          return;
        }
      }

      var pairBtn = target.closest('[data-pair-id]');
      if (pairBtn) {
        var pid = pairBtn.getAttribute('data-pair-id') || '';
        if (valid(pid)) {
          saveCustom({});
          save({ pfp: pid, banner: pid });
          showToast('Applied pair: ' + (LABELS[pid] || pid));
          paint(true);
        }
        return;
      }

      var tile = target.closest('.art-tile581');
      if (tile) {
        var kind = tile.getAttribute('data-kind');
        var id = tile.getAttribute('data-id') || '';
        if (kind !== 'pfp' && kind !== 'banner') return;
        var o2 = read();
        var c2 = readCustom();
        if (id && valid(id)) {
          o2[kind] = id;
          delete c2[kind];
          showToast((kind === 'pfp' ? 'Avatar: ' : 'Banner: ') + (LABELS[id] || id));
        } else {
          delete o2[kind];
          delete c2[kind];
          showToast(kind === 'pfp' ? 'Avatar reset to initials' : 'Banner reset to default');
        }
        saveCustom(c2);
        save(o2);
        paint(true);
      }
    });
  }

  function mountOrUpdateStudio(st, forceBody) {
    var page = document.querySelector('.profile-page547');
    if (!page) return;
    var host = page.querySelector('#profile-art581');
    var created = false;
    if (!host) {
      host = buildStudioShell();
      var stats = page.querySelector('.profile-stats571');
      var anchor = stats && stats.nextElementSibling;
      if (anchor && anchor.parentNode === page) {
        page.insertBefore(host, anchor);
      } else {
        page.appendChild(host);
      }
      bindStudioEvents(host);
      created = true;
    }
    updateStageDom(host, st);
    var body = host.querySelector('.pa155-body');
    var wantKey = st.pfpId + '|' + st.bannerId + '|' + uiState.tab + '|' + uiState.query + '|' + (uiState.expandPfp ? '1' : '0') + '|' + (uiState.expandBanner ? '1' : '0');
    if (body && (created || forceBody || host.dataset.renderKey !== wantKey)) {
      host.dataset.renderKey = wantKey;
      body.innerHTML = renderBodyContent(st);
    }
  }

  function syncMonoImg(el, pfpUrl) {
    if (!el) return;
    var img = el.querySelector('img.profile-mono-img581');
    if (pfpUrl) {
      if (!img) {
        img = document.createElement('img');
        img.className = 'profile-mono-img581';
        img.alt = '';
        img.decoding = 'async';
        el.appendChild(img);
      }
      if (img.getAttribute('src') !== pfpUrl) img.setAttribute('src', pfpUrl);
      el.classList.add('has-art581');
    } else {
      if (img) img.remove();
      el.classList.remove('has-art581');
    }
  }

  function paintHeaderAndDrawer(st) {
    var monos = document.querySelectorAll('#header-login .header-mono571, .profile-drawer560 .profile-identity560 .profile-mono571, .profile-private547 .profile-mono571');
    for (var i = 0; i < monos.length; i++) {
      syncMonoImg(monos[i], st.pfpUrl);
    }

    var loginBtn = document.getElementById('header-login');
    if (loginBtn) {
      var isSigned = loginBtn.classList.contains('is-profile560');
      var guestImg = loginBtn.querySelector('img.pa155-guest-chip-img');
      if (!isSigned && st.pfpUrl) {
        if (!guestImg) {
          guestImg = document.createElement('img');
          guestImg.className = 'pa155-guest-chip-img';
          guestImg.alt = '';
          loginBtn.insertBefore(guestImg, loginBtn.firstChild);
        }
        if (guestImg.getAttribute('src') !== st.pfpUrl) guestImg.setAttribute('src', st.pfpUrl);
        loginBtn.classList.add('has-guest-art155');
      } else {
        if (guestImg) guestImg.remove();
        loginBtn.classList.remove('has-guest-art155');
      }
    }
  }

  var lastNativeArt = null;
  function syncNativeWatchHeader(st) {
    try {
      var want = st.pfpId === 'custom' ? st.pfpUrl : (st.pfpId || '');
      if (want === lastNativeArt) return;
      lastNativeArt = want;
      var n = window.KuroNative;
      if (n && typeof n.profileArt581 === 'function') {
        n.profileArt581(want);
      }
    } catch (_) {}
  }

  function scrollToStudio() {
    var studio = document.getElementById('profile-art581');
    if (!studio) return;
    try {
      studio.scrollIntoView({ behavior: 'auto', block: 'start' });
      studio.classList.remove('pa155-pulse');
      void studio.offsetWidth;
      studio.classList.add('pa155-pulse');
    } catch (_) {}
  }

  function paintAccountHero(st) {
    var hero = document.querySelector('.profile-hero547');
    if (!hero) return;

    var av = hero.querySelector('.profile-avatar');
    if (av) {
      var pfpImg = av.querySelector('img.profile-art-pfp581');
      if (st.pfpUrl) {
        if (!pfpImg) {
          pfpImg = document.createElement('img');
          pfpImg.className = 'profile-art-pfp581';
          pfpImg.alt = '';
          pfpImg.decoding = 'async';
          av.insertBefore(pfpImg, av.firstChild);
        }
        if (pfpImg.getAttribute('src') !== st.pfpUrl) pfpImg.setAttribute('src', st.pfpUrl);
        av.classList.add('has-art581');
      } else {
        if (pfpImg) pfpImg.remove();
        av.classList.remove('has-art581');
      }
      if (!av.dataset.paJump155) {
        av.dataset.paJump155 = '1';
        av.setAttribute('title', 'Tap to change avatar & banner');
        av.addEventListener('click', scrollToStudio);
      }
    }

    var bn = hero.querySelector('.profile-banner');
    if (bn) {
      var banImg = bn.querySelector('img.profile-art-banner581');
      if (st.bannerUrl) {
        if (!banImg) {
          banImg = document.createElement('img');
          banImg.className = 'profile-art-banner581';
          banImg.alt = '';
          banImg.decoding = 'async';
          bn.insertBefore(banImg, bn.firstChild);
        }
        if (banImg.getAttribute('src') !== st.bannerUrl) banImg.setAttribute('src', st.bannerUrl);
        bn.classList.add('has-art581');
      } else {
        if (banImg) banImg.remove();
        bn.classList.remove('has-art581');
      }

      var jumpBtn = bn.querySelector('.pa155-hero-jump');
      if (!jumpBtn) {
        jumpBtn = document.createElement('button');
        jumpBtn.type = 'button';
        jumpBtn.className = 'pa155-hero-jump';
        jumpBtn.textContent = 'Customize Art';
        jumpBtn.addEventListener('click', scrollToStudio);
        bn.appendChild(jumpBtn);
      }
    }
  }

  var isPainting = false;
  function paint(forceBody) {
    if (isPainting) return;
    isPainting = true;
    try {
      var st = resolveState();
      syncNativeWatchHeader(st);
      paintHeaderAndDrawer(st);
      paintAccountHero(st);
      mountOrUpdateStudio(st, !!forceBody);
    } finally {
      isPainting = false;
    }
  }

  var queued = false;
  function schedule() {
    if (queued || isPainting) return;
    queued = true;
    setTimeout(function () {
      queued = false;
      paint(false);
    }, 0);
  }

  function isInternalArtMutation(records) {
    if (!records || !records.length) return true;
    for (var i = 0; i < records.length; i++) {
      var t = records[i].target;
      if (!t || t.nodeType !== 1) return false;
      if (t.closest && t.closest('#profile-art581')) continue;
      if (t.classList && (t.classList.contains('profile-art-pfp581') || t.classList.contains('profile-art-banner581') || t.classList.contains('profile-mono-img581') || t.classList.contains('pa155-guest-chip-img'))) continue;
      return false;
    }
    return true;
  }

  function boot() {
    window.kuroImagePicked = function (kind, dataUrl) {
      applyCustomPickedImage(kind, dataUrl);
    };
    if (window.MutationObserver && document.body) {
      var mo = new MutationObserver(function (records) {
        if (!isPainting && !isInternalArtMutation(records)) {
          schedule();
        }
      });
      mo.observe(document.body, { childList: true, subtree: true });
    }
    paint(true);
  }

  window.kuroProfileArt581 = {
    paint: function () { paint(true); },
    read: read,
    ids: IDS.slice(),
    labels: LABELS
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }
})();
