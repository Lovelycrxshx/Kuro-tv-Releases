#!/usr/bin/env python3
"""Kuro 1.5.3 custom player & fullscreen overhaul:
  1. Universal OLED Segmented Chapters & Pill-Chips player skin (`player-skin.js` +
     `stream-player.html` + `KuroNativePlayer561`) injected across ALL embedded server
     iframes via `WebViewCompat.addDocumentStartJavaScript(..., Collections.singleton("*"))`,
     Kuro's Web HLS player (`stream-player.html`), and native ExoPlayer, dynamically
     following all 20 Kuro theme accents + Pure Black OLED mode.
  2. YouTube-style Fullscreen Swipe-Up from bottom (`KuroPlayerUi153`) across both
     WebView `onShowCustomView` fullscreen and Enhanced/Native fullscreen: dragging up
     from below the video smoothly pulls up the themed Kuro "Up Next & Episodes" sheet,
     and switching episodes from fullscreen stays in fullscreen seamlessly.
  3. Redesigned App-UI Auto-Play Next Episode overlay (`offerAutoNext573` + verified
     `FX_ORIGINS` child-frame completion bridge `KuroPlayerAction153`) replacing the
     plain stock text popup with a floating OLED card, live countdown pill, smooth
     animated accent progress bar, and one-tap Play Next / Cancel pills.

Idempotent. Run after polish_1.5.2.py:
    python3 tools/player_1.5.3.py <project-dir>
"""
import sys
from pathlib import Path


PLAYER_SKIN_JS = r"""/* Kuro 1.5.3 — Universal OLED Segmented Chapters & Pill-Chips Player Skin.
 * Runs in top-level and nested player frames via document-start injection (and
 * onPageFinished fallback). Preserves provider playback pipelines, DRM, and gestures
 * while unifying JWPlayer, Video.js, Plyr, Artplayer, Vidstack, FluidPlayer, DPlayer,
 * Clappr, and kuro.local/stream-player.html under Kuro's active accent + OLED design. */
(function () {
  'use strict';
  if (!document || typeof document.createElement !== 'function') return;

  var DEFAULT_THEME = {
    accent: '#a65be4',
    soft: '#cfa2fb',
    card: '#0a0a0e',
    border: '#4f385d',
    ink: '#f3eef7',
    muted: '#b7a4c7',
    amoled: true,
    attempt: 0,
    hasNext: false,
    nextEp: ''
  };

  function hexToRgba(hex, alpha) {
    var clean = String(hex || '').replace('#', '').trim();
    if (!/^[0-9a-fA-F]{6}$/.test(clean)) return 'rgba(166,91,228,' + alpha + ')';
    var r = parseInt(clean.slice(0, 2), 16);
    var g = parseInt(clean.slice(2, 4), 16);
    var b = parseInt(clean.slice(4, 6), 16);
    return 'rgba(' + r + ',' + g + ',' + b + ',' + alpha + ')';
  }

  function sanitizeTheme(raw) {
    var t = raw && typeof raw === 'object' ? raw : {};
    var okHex = function (v, fb) { return typeof v === 'string' && /^#[0-9a-fA-F]{6}$/.test(v) ? v : fb; };
    return {
      accent: okHex(t.accent, DEFAULT_THEME.accent),
      soft: okHex(t.soft, DEFAULT_THEME.soft),
      card: okHex(t.card, DEFAULT_THEME.card),
      border: okHex(t.border, DEFAULT_THEME.border),
      ink: okHex(t.ink, DEFAULT_THEME.ink),
      muted: okHex(t.muted, DEFAULT_THEME.muted),
      amoled: t.amoled !== false,
      attempt: Number.isSafeInteger(t.attempt) && t.attempt > 0 ? t.attempt : 0,
      hasNext: !!t.hasNext,
      nextEp: typeof t.nextEp === 'string' && /^[0-9]{1,5}$/.test(t.nextEp) ? t.nextEp : ''
    };
  }

  var state = {
    theme: sanitizeTheme(window.__kuroSkinTheme153),
    sheet: null,
    styleEl: null,
    endedSent: false
  };

  function buildCss(t) {
    var glow = hexToRgba(t.accent, 0.52);
    var glowSoft = hexToRgba(t.soft, 0.34);
    var cue = hexToRgba(t.accent, 0.32);
    var cueBorder = hexToRgba(t.soft, 0.45);
    var pillBg = 'rgba(10,10,14,0.88)';
    var pillHover = hexToRgba(t.accent, 0.20);
    return `
      :root {
        --ku-p-accent: ${t.accent};
        --ku-p-soft: ${t.soft};
        --ku-p-glow: ${glow};
        --ku-p-glow-soft: ${glowSoft};
        --ku-p-cue: ${cue};
        --ku-p-cue-border: ${cueBorder};
        --ku-p-pill: ${pillBg};
        --ku-p-pill-hover: ${pillHover};
        --ku-p-border: ${t.border};
        --ku-p-ink: ${t.ink};
        --ku-p-muted: ${t.muted};
        --plyr-color-main: ${t.accent};
      }

      /* 1. Pure OLED bottom gradient fade across all web player engines */
      .jwplayer .jw-controlbar,
      .video-js .vjs-control-bar,
      .plyr--video .plyr__controls,
      .art-video-player .art-bottom,
      .dplayer .dplayer-controller,
      .vds-video-layout .vds-controls-group:last-child,
      [data-media-player] .vds-controls-group:last-child {
        background: linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0.68) 36%, rgba(0,0,0,0.95) 76%, #000000 100%) !important;
        border: 0 !important;
        box-shadow: none !important;
        padding-bottom: max(8px, env(safe-area-inset-bottom, 0px)) !important;
      }
      .jwplayer .jw-nextup-container {
        background: rgba(8,7,12,0.94) !important;
        border: 1.5px solid var(--ku-p-accent) !important;
        border-radius: 16px !important;
        box-shadow: 0 10px 28px rgba(0,0,0,0.8), 0 0 16px var(--ku-p-glow) !important;
      }

      /* 2. OLED Segmented Chapter Scrubber & Glowing Vertical Pill Handle */
      .jwplayer .jw-slider-time,
      .jwplayer .jw-slider-horizontal {
        height: 22px !important;
      }
      .jwplayer .jw-slider-horizontal .jw-rail,
      .video-js .vjs-progress-holder,
      .plyr--video .plyr__progress__buffer,
      .art-video-player .art-control-progress-inner {
        height: 6px !important;
        border-radius: 999px !important;
        background: rgba(255,255,255,0.16) !important;
      }
      .jwplayer .jw-timesegment {
        margin-right: 4px !important;
        border-radius: 999px !important;
        overflow: visible !important;
      }
      .jwplayer .jw-timesegment:last-child {
        margin-right: 0 !important;
      }
      .jwplayer .jw-timesegment-reset,
      .jwplayer .jw-timesegment-bar {
        border-radius: 999px !important;
        height: 6px !important;
        overflow: hidden !important;
      }
      .jwplayer .jw-buffer,
      .jwplayer .jw-timesegment-buffered,
      .video-js .vjs-load-progress,
      .video-js .vjs-load-progress div,
      .art-video-player .art-progress-loaded {
        background: rgba(255,255,255,0.26) !important;
        border-radius: 999px !important;
      }
      /* Replace yellow/gold/teal intro-outro chapter markers with subtle Kuro accent tint */
      .jwplayer .jw-cue,
      .jwplayer [data-kuro-cue="1"],
      .video-js .vjs-marker,
      .plyr__progress__marker,
      .art-video-player .art-progress-highlight span {
        background: var(--ku-p-cue) !important;
        background-color: var(--ku-p-cue) !important;
        border: 1px solid var(--ku-p-cue-border) !important;
        border-radius: 999px !important;
        box-shadow: none !important;
      }
      /* Luminous played fill */
      .jwplayer .jw-slider-time .jw-progress,
      .jwplayer .jw-slider-time .jw-timesegment-progress,
      .video-js .vjs-play-progress,
      .plyr--video .plyr__progress input[type="range"],
      .art-video-player .art-progress-played,
      .dplayer .dplayer-played {
        background: linear-gradient(90deg, var(--ku-p-accent), var(--ku-p-soft)) !important;
        box-shadow: 0 0 12px var(--ku-p-glow) !important;
        border-radius: 999px !important;
        color: var(--ku-p-accent) !important;
      }
      /* Vertical glowing pill scrubber handle */
      .jwplayer .jw-slider-time .jw-knob,
      .art-video-player .art-progress-indicator,
      .dplayer .dplayer-thumb {
        width: 8px !important;
        height: 20px !important;
        border-radius: 999px !important;
        background: #08060d !important;
        border: 2px solid var(--ku-p-soft) !important;
        box-shadow: 0 0 12px var(--ku-p-accent), 0 0 4px var(--ku-p-soft) !important;
        opacity: 1 !important;
        visibility: visible !important;
        transform: translate(-50%, -50%) !important;
        top: 50% !important;
      }
      .video-js .vjs-play-progress::before {
        content: "" !important;
        width: 8px !important;
        height: 20px !important;
        top: -7px !important;
        right: -4px !important;
        border-radius: 999px !important;
        background: #08060d !important;
        border: 2px solid var(--ku-p-soft) !important;
        box-shadow: 0 0 12px var(--ku-p-accent) !important;
      }
      .plyr--video .plyr__progress input[type="range"]::-webkit-slider-thumb {
        -webkit-appearance: none !important;
        width: 8px !important;
        height: 20px !important;
        border-radius: 999px !important;
        background: #08060d !important;
        border: 2px solid var(--ku-p-soft) !important;
        box-shadow: 0 0 12px var(--ku-p-accent) !important;
      }

      /* Chapter labels strip under segmented JWPlayer rail */
      .kuro-chapter-strip153 {
        display: flex;
        width: 100%;
        justify-content: space-between;
        padding: 2px 6px 4px;
        box-sizing: border-box;
        pointer-events: none;
        font: 500 10px/1.2 system-ui, -apple-system, sans-serif;
        color: rgba(243,238,247,0.78);
        letter-spacing: 0.02em;
      }
      @media (max-width: 350px) {
        .kuro-chapter-strip153 { display: none !important; }
      }

      /* 3. OLED Pill-Chip Buttons & Glowing Accent Focus Rings */
      .jwplayer .jw-button-container {
        padding: 4px 6px 2px !important;
        gap: 4px !important;
        align-items: center !important;
      }
      .jwplayer .jw-button-container > .jw-icon-inline,
      .jwplayer .jw-button-container > .jw-icon-tooltip,
      .video-js .vjs-control-bar > .vjs-control:not(.vjs-progress-control):not(.vjs-custom-control-spacer):not(.vjs-time-control),
      .plyr--video .plyr__controls > button,
      .plyr--video .plyr__controls > .plyr__controls__item:not(.plyr__progress__container):not(.plyr__time),
      .art-video-player .art-controls-left > .art-control,
      .art-video-player .art-controls-right > .art-control {
        background: var(--ku-p-pill) !important;
        border: 1px solid var(--ku-p-border) !important;
        border-radius: 999px !important;
        height: 32px !important;
        min-width: 36px !important;
        padding: 0 10px !important;
        margin: 2px 2px !important;
        color: var(--ku-p-ink) !important;
        opacity: 0.98 !important;
        box-sizing: border-box !important;
        display: inline-flex !important;
        align-items: center !important;
        justify-content: center !important;
        transition: border-color .16s ease, box-shadow .16s ease, background .16s ease !important;
      }
      /* Primary Play/Pause pill gets signature glowing Kuro accent border */
      .jwplayer .jw-button-container > .jw-icon-playback,
      .video-js .vjs-control-bar > .vjs-play-control,
      .plyr--video .plyr__controls > [data-plyr="play"],
      .art-video-player .art-controls-left > .art-control-playAndPause {
        border: 1.5px solid var(--ku-p-accent) !important;
        box-shadow: 0 0 12px var(--ku-p-glow) !important;
        padding: 0 14px !important;
      }
      .jwplayer .jw-controlbar .jw-icon:hover,
      .jwplayer .jw-controlbar .jw-icon:focus-visible,
      .video-js .vjs-control-bar .vjs-control:hover,
      .video-js .vjs-control-bar .vjs-control:focus-visible,
      .plyr--video .plyr__controls button:hover,
      .plyr--video .plyr__controls button:focus-visible,
      .kuro-pill-btn153:hover,
      .kuro-pill-btn153:focus-visible {
        color: #ffffff !important;
        border-color: var(--ku-p-soft) !important;
        background: var(--ku-p-pill-hover) !important;
        box-shadow: 0 0 12px var(--ku-p-glow) !important;
        outline: none !important;
      }
      .jwplayer .jw-text-elapsed,
      .jwplayer .jw-text-duration,
      .video-js .vjs-time-control,
      .plyr--video .plyr__time,
      .art-video-player .art-control-time {
        color: var(--ku-p-ink) !important;
        font-variant-numeric: tabular-nums !important;
        font-weight: 500 !important;
        font-size: 12px !important;
        background: transparent !important;
        border: 0 !important;
      }

      /* Injected Kuro OLED Pill Chips (Next Ep › & Speed) */
      .kuro-pill-btn153 {
        appearance: none;
        -webkit-appearance: none;
        background: var(--ku-p-pill);
        color: var(--ku-p-ink);
        border: 1px solid var(--ku-p-border);
        border-radius: 999px;
        height: 32px;
        padding: 0 12px;
        margin: 2px 3px;
        font: 600 11.5px/1 system-ui, -apple-system, sans-serif;
        letter-spacing: 0.01em;
        white-space: nowrap;
        cursor: pointer;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 4px;
        flex-shrink: 0;
        z-index: 9;
      }
      .kuro-pill-btn153[data-kuro-primary="1"] {
        border: 1.5px solid var(--ku-p-accent);
        box-shadow: 0 0 12px var(--ku-p-glow);
      }

      /* Center play indicator & tooltips/menus */
      .jwplayer .jw-display-icon-container,
      .video-js .vjs-big-play-button,
      .plyr--video .plyr__control--overlaid,
      .art-video-player .art-state {
        background: rgba(10,10,14,0.84) !important;
        border: 1.5px solid var(--ku-p-accent) !important;
        border-radius: 999px !important;
        box-shadow: 0 0 22px var(--ku-p-glow) !important;
        color: #ffffff !important;
      }
      .jwplayer .jw-settings-menu,
      .jwplayer .jw-time-tip,
      .video-js .vjs-menu .vjs-menu-content,
      .plyr--video .plyr__menu__container,
      .art-video-player .art-settings,
      .art-video-player .art-selector-list {
        background: rgba(10,10,14,0.95) !important;
        border: 1px solid var(--ku-p-border) !important;
        border-radius: 14px !important;
        color: var(--ku-p-ink) !important;
        box-shadow: 0 12px 32px rgba(0,0,0,0.85) !important;
      }
      .jwplayer .jw-settings-content-item.jw-settings-item-active,
      .video-js .vjs-menu-item.vjs-selected,
      .plyr__control[aria-checked="true"] {
        color: var(--ku-p-soft) !important;
        background: var(--ku-p-pill-hover) !important;
      }

      /* Provider Skip Intro / Skip Outro -> OLED glowing accent pill */
      .jwplayer .jw-skip,
      [class*="skip-button" i],
      [class*="skip_button" i],
      [class*="skip-intro" i],
      [class*="skip_intro" i],
      [class*="skipintro" i],
      [data-kuro-skip-styled="1"] {
        background: rgba(10,10,14,0.92) !important;
        color: #ffffff !important;
        border: 1.5px solid var(--ku-p-accent) !important;
        border-radius: 999px !important;
        box-shadow: 0 0 16px var(--ku-p-glow) !important;
        padding: 7px 16px !important;
        font: 600 12.5px/1.1 system-ui, -apple-system, sans-serif !important;
      }
    `;
  }

  function applyThemeCss() {
    var css = buildCss(state.theme);
    try {
      if (typeof CSSStyleSheet === 'function' && document.adoptedStyleSheets) {
        if (!state.sheet) {
          state.sheet = new CSSStyleSheet();
          state.sheet.replaceSync(css);
          document.adoptedStyleSheets = Array.prototype.slice.call(document.adoptedStyleSheets).concat([state.sheet]);
        } else {
          state.sheet.replaceSync(css);
        }
      }
    } catch (_) {}
    try {
      if (!state.styleEl || !state.styleEl.isConnected) {
        var existing = document.getElementById('kuro-video-skin');
        state.styleEl = existing || document.createElement('style');
        state.styleEl.id = 'kuro-video-skin';
        (document.head || document.documentElement).appendChild(state.styleEl);
      }
      state.styleEl.textContent = css;
    } catch (_) {}
  }

  function postAction(kind) {
    try {
      var attempt = state.theme.attempt || Number(new URLSearchParams(location.search).get('attempt')) || 0;
      if (window.KuroPlayerAction153 && typeof window.KuroPlayerAction153.postMessage === 'function') {
        window.KuroPlayerAction153.postMessage(kind + ':' + attempt);
        return true;
      }
    } catch (_) {}
    return false;
  }

  var SPEEDS = [1, 1.25, 1.5, 2, 0.75];
  var seenVideos = typeof WeakSet === 'function' ? new WeakSet() : null;

  function bindVideo(v) {
    if (!v || (seenVideos && seenVideos.has(v))) return;
    if (seenVideos) seenVideos.add(v);
    try {
      v.muted = false;
      v.volume = 1;
      if (window.__kuroAutoplay) {
        var p = v.play();
        if (p && p.catch) p.catch(function () {});
      }
    } catch (_) {}
    v.addEventListener('ended', function () {
      try {
        var all = document.querySelectorAll('video');
        if (state.endedSent || all.length !== 1 || all[0] !== v) return;
        if (!v.ended || !Number.isFinite(v.duration) || v.duration < 180 || v.currentTime < v.duration * 0.98) return;
        state.endedSent = true;
        postAction('ended');
      } catch (_) {}
    });
    v.addEventListener('seeking', function () {
      if (Number.isFinite(v.duration) && v.currentTime < v.duration * 0.95) state.endedSent = false;
    });
  }

  function enhanceEmbeddedPlayers() {
    if (location.hostname === 'kuro.local' && location.pathname === '/stream-player.html') return;
    document.querySelectorAll('video').forEach(bindVideo);

    var cueCandidates = document.querySelectorAll(
      '.jwplayer [style*="eab308" i], .jwplayer [style*="ffd000" i], .jwplayer [style*="f59e0b" i], ' +
      '.jwplayer [style*="255, 208" i], .jwplayer [style*="234, 179" i], .jwplayer .jw-cue'
    );
    for (var i = 0; i < cueCandidates.length; i++) {
      cueCandidates[i].setAttribute('data-kuro-cue', '1');
    }

    var btns = document.querySelectorAll('button, [role="button"], div[class*="skip" i]');
    for (var b = 0; b < btns.length && b < 120; b++) {
      var el = btns[b];
      if (el.getAttribute('data-kuro-skip-styled')) continue;
      var txt = String(el.textContent || '').trim();
      if (txt.length > 0 && txt.length <= 24 && /^skip\s+(?:intro|opening|outro|ending|op|ed)\b/i.test(txt)) {
        el.setAttribute('data-kuro-skip-styled', '1');
      }
    }

    var sliders = document.querySelectorAll('.jwplayer .jw-controlbar');
    for (var s = 0; s < sliders.length; s++) {
      var bar = sliders[s];
      var segs = bar.querySelectorAll('.jw-timesegment');
      var strip = bar.querySelector('.kuro-chapter-strip153');
      if (segs.length >= 3 && !strip) {
        strip = document.createElement('div');
        strip.className = 'kuro-chapter-strip153';
        var labels = segs.length >= 4
          ? ['Cold Open', 'Intro', 'Main Episode', 'Outro']
          : ['Intro', 'Main Episode', 'Outro'];
        strip.innerHTML = labels.map(function (l) { return '<span>' + l + '</span>'; }).join('');
        var group = bar.querySelector('.jw-Controlbar-slider-group, .jw-slider-time');
        if (group && group.parentNode) {
          group.parentNode.insertBefore(strip, group.nextSibling);
        }
      }
    }

    var containers = document.querySelectorAll(
      '.jwplayer .jw-button-container, .video-js .vjs-control-bar, .plyr--video .plyr__controls, .art-video-player .art-controls-left'
    );
    for (var c = 0; c < containers.length; c++) {
      var host = containers[c];
      if (!host.querySelector('[data-kuro-speed153]')) {
        var speedBtn = document.createElement('button');
        speedBtn.type = 'button';
        speedBtn.className = 'kuro-pill-btn153';
        speedBtn.setAttribute('data-kuro-speed153', '1');
        speedBtn.setAttribute('aria-label', 'Playback speed');
        speedBtn.textContent = '1.0x';
        speedBtn.addEventListener('click', function (ev) {
          ev.stopPropagation();
          var v = document.querySelector('video');
          if (!v) return;
          var cur = Number(v.playbackRate) || 1;
          var idx = SPEEDS.indexOf(cur);
          var next = SPEEDS[(idx + 1) % SPEEDS.length];
          try { v.playbackRate = next; } catch (_) {}
          this.textContent = (next % 1 === 0 ? next.toFixed(1) : String(next)) + 'x';
        });
        var spacer = host.querySelector('.jw-spacer, .vjs-custom-control-spacer');
        if (spacer) host.insertBefore(speedBtn, spacer.nextSibling);
        else host.appendChild(speedBtn);
      }

      var existingNext = host.querySelector('[data-kuro-next153]');
      if (state.theme.hasNext && !existingNext) {
        var nextBtn = document.createElement('button');
        nextBtn.type = 'button';
        nextBtn.className = 'kuro-pill-btn153';
        nextBtn.setAttribute('data-kuro-next153', '1');
        nextBtn.setAttribute('aria-label', state.theme.nextEp ? 'Next episode ' + state.theme.nextEp : 'Next episode');
        nextBtn.textContent = state.theme.nextEp ? ('Next Ep ' + state.theme.nextEp + ' \u203a') : 'Next Ep \u203a';
        nextBtn.addEventListener('click', function (ev) {
          ev.stopPropagation();
          postAction('next');
        });
        var sp = host.querySelector('.jw-spacer, .vjs-custom-control-spacer');
        if (sp) host.insertBefore(nextBtn, sp);
        else host.appendChild(nextBtn);
      } else if (!state.theme.hasNext && existingNext) {
        existingNext.remove();
      } else if (state.theme.hasNext && existingNext) {
        existingNext.textContent = state.theme.nextEp ? ('Next Ep ' + state.theme.nextEp + ' \u203a') : 'Next Ep \u203a';
      }
    }
  }

  function mountDirectStreamHud() {
    if (location.hostname !== 'kuro.local' || location.pathname !== '/stream-player.html') return;
    if (document.getElementById('kuro-oled-hud153')) return;
    var video = document.getElementById('video');
    if (!video || !document.body) return;
    video.controls = false;
    bindVideo(video);

    var hudStyle = document.createElement('style');
    hudStyle.id = 'kuro-oled-hud-style153';
    hudStyle.textContent = `
      #kuro-oled-hud153 {
        position: fixed; inset: 0; display: flex; flex-direction: column; justify-content: flex-end;
        background: linear-gradient(180deg, rgba(0,0,0,0) 52%, rgba(0,0,0,0.74) 76%, #000000 100%);
        padding: 16px max(16px, env(safe-area-inset-right, 0px)) max(14px, env(safe-area-inset-bottom, 0px)) max(16px, env(safe-area-inset-left, 0px));
        box-sizing: border-box; transition: opacity .22s ease; z-index: 20; user-select: none; -webkit-user-select: none;
      }
      #kuro-oled-hud153.is-hidden { opacity: 0; pointer-events: none; }
      .ko-skip-row { display: flex; justify-content: flex-end; margin-bottom: 10px; min-height: 34px; }
      .ko-skip-pill {
        display: none; background: rgba(10,10,14,0.94); color: #fff;
        border: 1.5px solid var(--ku-p-accent); border-radius: 999px;
        box-shadow: 0 0 16px var(--ku-p-glow); padding: 7px 16px;
        font: 600 12.5px/1.1 system-ui, -apple-system, sans-serif; cursor: pointer;
      }
      .ko-skip-pill.is-visible { display: inline-flex; align-items: center; gap: 6px; }
      .ko-timeline-wrap { position: relative; padding: 8px 0 20px; cursor: pointer; touch-action: none; }
      .ko-segments { display: flex; width: 100%; height: 6px; gap: 4px; align-items: center; }
      .ko-seg { position: relative; height: 6px; border-radius: 999px; background: rgba(255,255,255,0.16); overflow: hidden; flex-grow: 1; }
      .ko-seg[data-cue="1"] { background: var(--ku-p-cue); }
      .ko-seg-fill { position: absolute; left: 0; top: 0; bottom: 0; width: 0%; border-radius: 999px; background: linear-gradient(90deg, var(--ku-p-accent), var(--ku-p-soft)); box-shadow: 0 0 10px var(--ku-p-glow); }
      .ko-thumb {
        position: absolute; top: 11px; left: 0%; width: 8px; height: 22px; border-radius: 999px;
        background: #08060d; border: 2px solid var(--ku-p-soft);
        box-shadow: 0 0 12px var(--ku-p-accent); transform: translate(-50%, -50%); pointer-events: none;
      }
      .ko-chapters { display: flex; justify-content: space-between; font: 500 10.5px/1.2 system-ui, sans-serif; color: rgba(243,238,247,0.82); margin-top: 6px; }
      .ko-time-float { font-variant-numeric: tabular-nums; color: #fff; font-weight: 600; }
      .ko-pills { display: flex; align-items: center; justify-content: space-between; gap: 6px; flex-wrap: wrap; margin-top: 6px; }
      .ko-cluster { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
    `;
    document.head.appendChild(hudStyle);

    var hud = document.createElement('div');
    hud.id = 'kuro-oled-hud153';
    hud.innerHTML =
      '<div class="ko-skip-row"><button type="button" class="ko-skip-pill" id="ko-skip153">Skip Intro \u21c4</button></div>' +
      '<div class="ko-timeline-wrap" id="ko-scrub153">' +
        '<div class="ko-segments" id="ko-segs153">' +
          '<div class="ko-seg" style="flex:8"><div class="ko-seg-fill"></div></div>' +
          '<div class="ko-seg" data-cue="1" style="flex:12"><div class="ko-seg-fill"></div></div>' +
          '<div class="ko-seg" style="flex:70"><div class="ko-seg-fill"></div></div>' +
          '<div class="ko-seg" data-cue="1" style="flex:10"><div class="ko-seg-fill"></div></div>' +
        '</div>' +
        '<div class="ko-thumb" id="ko-thumb153"></div>' +
        '<div class="ko-chapters">' +
          '<span>Cold Open</span><span>Intro</span><span>Main Episode</span>' +
          '<span class="ko-time-float" id="ko-time153">00:00 / 00:00</span><span>Outro</span>' +
        '</div>' +
      '</div>' +
      '<div class="ko-pills">' +
        '<div class="ko-cluster">' +
          '<button type="button" class="kuro-pill-btn153" data-kuro-primary="1" id="ko-play153">Play</button>' +
          '<button type="button" class="kuro-pill-btn153" id="ko-rew153">-10s</button>' +
          '<button type="button" class="kuro-pill-btn153" id="ko-fwd153">+10s</button>' +
          '<button type="button" class="kuro-pill-btn153" id="ko-next153" style="display:none">Next Ep \u203a</button>' +
        '</div>' +
        '<div class="ko-cluster">' +
          '<button type="button" class="kuro-pill-btn153" id="ko-speed153">1.0x</button>' +
          '<button type="button" class="kuro-pill-btn153" id="ko-qual153">Auto</button>' +
          '<button type="button" class="kuro-pill-btn153" id="ko-pip153">PiP</button>' +
          '<button type="button" class="kuro-pill-btn153" id="ko-fs153">Fullscreen</button>' +
        '</div>' +
      '</div>';
    document.body.appendChild(hud);

    var hideTimer = null;
    function showHud() {
      hud.classList.remove('is-hidden');
      clearTimeout(hideTimer);
      if (!video.paused && !video.ended) {
        hideTimer = setTimeout(function () { hud.classList.add('is-hidden'); }, 3200);
      }
    }

    document.addEventListener('pointerdown', function (e) {
      if (hud.classList.contains('is-hidden')) {
        showHud();
        e.stopPropagation();
      } else if (e.target === hud) {
        hud.classList.add('is-hidden');
      } else {
        showHud();
      }
    }, true);

    var playBtn = document.getElementById('ko-play153');
    var skipBtn = document.getElementById('ko-skip153');
    var nextBtn = document.getElementById('ko-next153');
    var timeEl = document.getElementById('ko-time153');
    var thumbEl = document.getElementById('ko-thumb153');
    var qualBtn = document.getElementById('ko-qual153');
    var fills = hud.querySelectorAll('.ko-seg-fill');
    var weights = [0.08, 0.20, 0.90, 1.0];
    var activeSkipEnd = -1;

    function fmt(sec) {
      if (!Number.isFinite(sec) || sec < 0) return '00:00';
      var s = Math.floor(sec), m = Math.floor(s / 60), h = Math.floor(m / 60);
      if (h > 0) return h + ':' + String(m % 60).padStart(2, '0') + ':' + String(s % 60).padStart(2, '0');
      return String(m).padStart(2, '0') + ':' + String(s % 60).padStart(2, '0');
    }

    function syncHud() {
      playBtn.textContent = video.paused ? 'Play' : 'Pause';
      if (state.theme.hasNext) {
        nextBtn.style.display = 'inline-flex';
        nextBtn.textContent = state.theme.nextEp ? ('Next Ep ' + state.theme.nextEp + ' \u203a') : 'Next Ep \u203a';
      } else {
        nextBtn.style.display = 'none';
      }
      var dur = Number.isFinite(video.duration) && video.duration > 0 ? video.duration : 0;
      var cur = Number.isFinite(video.currentTime) ? video.currentTime : 0;
      var ratio = dur > 0 ? Math.max(0, Math.min(1, cur / dur)) : 0;
      thumbEl.style.left = (ratio * 100).toFixed(2) + '%';
      timeEl.textContent = fmt(cur) + ' / ' + fmt(dur);
      var prev = 0;
      for (var i = 0; i < fills.length; i++) {
        var end = weights[i];
        var segFill = ratio <= prev ? 0 : ratio >= end ? 100 : ((ratio - prev) / (end - prev)) * 100;
        fills[i].style.width = segFill.toFixed(1) + '%';
        prev = end;
      }
      if (video.videoHeight > 0) qualBtn.textContent = video.videoHeight + 'p';
      var markers = typeof window.__kuroReadMarkers153 === 'function' ? window.__kuroReadMarkers153() : null;
      activeSkipEnd = -1;
      if (markers && Array.isArray(markers.ranges)) {
        for (var r = 0; r < markers.ranges.length; r++) {
          var rng = markers.ranges[r];
          if (cur >= rng[0] && cur < rng[1]) {
            activeSkipEnd = rng[1];
            skipBtn.textContent = (r === 0 ? 'Skip Intro' : 'Skip Outro') + ' \u21c4';
            break;
          }
        }
      }
      skipBtn.classList.toggle('is-visible', activeSkipEnd > 0);
    }

    playBtn.addEventListener('click', function () {
      if (video.paused) { var p = video.play(); if (p && p.catch) p.catch(function () {}); }
      else video.pause();
      showHud();
    });
    document.getElementById('ko-rew153').addEventListener('click', function () {
      video.currentTime = Math.max(0, (video.currentTime || 0) - 10);
      showHud();
    });
    document.getElementById('ko-fwd153').addEventListener('click', function () {
      var d = Number.isFinite(video.duration) ? video.duration : Infinity;
      video.currentTime = Math.min(d, (video.currentTime || 0) + 10);
      showHud();
    });
    nextBtn.addEventListener('click', function () { postAction('next'); });
    skipBtn.addEventListener('click', function () {
      if (activeSkipEnd > 0) video.currentTime = activeSkipEnd;
      showHud();
    });
    document.getElementById('ko-speed153').addEventListener('click', function () {
      var cur = Number(video.playbackRate) || 1;
      var next = SPEEDS[(SPEEDS.indexOf(cur) + 1) % SPEEDS.length];
      video.playbackRate = next;
      this.textContent = (next % 1 === 0 ? next.toFixed(1) : String(next)) + 'x';
      showHud();
    });
    document.getElementById('ko-pip153').addEventListener('click', function () {
      if (!postAction('pip') && document.pictureInPictureEnabled && video.requestPictureInPicture) {
        video.requestPictureInPicture().catch(function () {});
      }
    });
    document.getElementById('ko-fs153').addEventListener('click', function () {
      if (!postAction('fullscreen')) {
        if (document.fullscreenElement) document.exitFullscreen().catch(function () {});
        else document.documentElement.requestFullscreen().catch(function () {});
      }
    });

    var scrub = document.getElementById('ko-scrub153');
    function seekAt(clientX) {
      if (!Number.isFinite(video.duration) || video.duration <= 0) return;
      var rect = scrub.getBoundingClientRect();
      var pct = Math.max(0, Math.min(1, (clientX - rect.left) / Math.max(1, rect.width)));
      video.currentTime = pct * video.duration;
      syncHud();
    }
    var scrubbing = false;
    scrub.addEventListener('pointerdown', function (e) { scrubbing = true; seekAt(e.clientX); showHud(); });
    window.addEventListener('pointermove', function (e) { if (scrubbing) seekAt(e.clientX); });
    window.addEventListener('pointerup', function () { scrubbing = false; });

    ['play', 'pause', 'timeupdate', 'loadedmetadata', 'resize'].forEach(function (ev) {
      video.addEventListener(ev, syncHud);
    });
    syncHud();
    showHud();
  }

  window.__kuroApplySkin153 = function (nextTheme) {
    state.theme = sanitizeTheme(nextTheme || window.__kuroSkinTheme153);
    applyThemeCss();
    if (location.hostname === 'kuro.local' && location.pathname === '/stream-player.html') {
      var nextBtn = document.getElementById('ko-next153');
      if (nextBtn) {
        nextBtn.style.display = state.theme.hasNext ? 'inline-flex' : 'none';
        nextBtn.textContent = state.theme.nextEp ? ('Next Ep ' + state.theme.nextEp + ' \u203a') : 'Next Ep \u203a';
      }
    } else {
      enhanceEmbeddedPlayers();
    }
  };

  function boot() {
    applyThemeCss();
    if (location.hostname === 'kuro.local' && location.pathname === '/stream-player.html') {
      mountDirectStreamHud();
    } else {
      enhanceEmbeddedPlayers();
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot, { once: true });
  } else {
    boot();
  }
  try {
    var scheduled = false;
    new MutationObserver(function () {
      if (scheduled) return;
      scheduled = true;
      setTimeout(function () {
        scheduled = false;
        if (!state.styleEl || !state.styleEl.isConnected) applyThemeCss();
        enhanceEmbeddedPlayers();
      }, 180);
    }).observe(document.documentElement, { childList: true, subtree: true });
  } catch (_) {}
})();
"""


KURO_PLAYER_UI_153_JAVA = r"""package app.lovelycrxshx.kuro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import androidx.annotation.OptIn;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.DefaultTimeBar;
import androidx.media3.ui.PlayerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/**
 * Kuro 1.5.3 player UI layer:
 *  - Universal OLED Segmented Chapters & Pill-Chips theme bootstrap for WebView frames
 *  - Native ExoPlayer OLED pill styling (Skip Intro pill, speed/episodes/fullscreen pills, timebar tint)
 *  - Fullscreen bottom swipe-up gesture + "Up Next & Episodes" drawer inside fullscreen
 *  - App-UI floating Up-Next autoplay countdown overlay (preserving AlertDialog test hooks)
 */
@OptIn(markerClass = UnstableApi.class)
final class KuroPlayerUi153 {

    interface FullscreenActions {
        JSONObject watchInfo();
        boolean isEnhancedFullscreen();
        void chooseEpisode(int index);
        void nextEpisode();
        void toggleEnhancedFullscreen();
        long savedPositionMs(String episodeNumber);
    }

    static final class AutoNextUi {
        final AlertDialog dialog;
        private final TextView badge;
        private final View progressFill;
        private final View progressTrack;

        AutoNextUi(AlertDialog dialog, TextView badge, View progressFill, View progressTrack) {
            this.dialog = dialog;
            this.badge = badge;
            this.progressFill = progressFill;
            this.progressTrack = progressTrack;
        }

        void updateRemainingMs(long remainingMs, long totalMs) {
            long secs = Math.max(1L, (remainingMs + 999L) / 1000L);
            if (badge != null) badge.setText("in " + secs + "s");
            if (progressFill != null && progressTrack != null) {
                int w = progressTrack.getWidth();
                if (w > 0 && totalMs > 0) {
                    float frac = Math.max(0f, Math.min(1f, (float) remainingMs / (float) totalMs));
                    ViewGroup.LayoutParams lp = progressFill.getLayoutParams();
                    int target = Math.max(1, Math.round(w * frac));
                    if (lp.width != target) {
                        lp.width = target;
                        progressFill.setLayoutParams(lp);
                    }
                }
            }
        }
    }

    private static View activeFullscreenSheet;
    private static final float[] SPEEDS = new float[]{1.0f, 1.25f, 1.5f, 2.0f, 0.75f};

    private KuroPlayerUi153() {}

    private static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    private static String hex6(int argb) {
        return String.format(Locale.ROOT, "#%06x", argb & 16777215);
    }

    static String skinBootstrapJs(JSONObject watchInfo, long attempt) {
        JSONObject theme = new JSONObject();
        try {
            theme.put("accent", hex6(KuroTheme12.accentColor()));
            theme.put("soft", hex6(KuroDialogs581.PURPLE_SOFT));
            theme.put("card", hex6(Color.rgb(10, 10, 14)));
            theme.put("border", hex6(KuroDialogs581.BORDER));
            theme.put("ink", hex6(KuroDialogs581.INK));
            theme.put("muted", hex6(KuroDialogs581.MUTED));
            theme.put("amoled", KuroTheme12.amoled());
            theme.put("attempt", attempt);
            boolean hasNext = false;
            String nextNum = "";
            if (watchInfo != null) {
                JSONArray eps = watchInfo.optJSONArray("episodes");
                int cur = watchInfo.optInt("current", -1);
                if (eps != null && cur >= 0 && cur + 1 < eps.length()) {
                    hasNext = true;
                    JSONObject n = eps.optJSONObject(cur + 1);
                    nextNum = n != null ? n.optString("number", "") : eps.optString(cur + 1, "");
                }
            }
            theme.put("hasNext", hasNext);
            theme.put("nextEp", nextNum);
        } catch (Exception ignored) {}
        return "window.__kuroSkinTheme153=" + theme.toString() + ";if(window.__kuroApplySkin153)window.__kuroApplySkin153(window.__kuroSkinTheme153);";
    }

    static GradientDrawable pillBackground(Context c, int fill, int strokeColor, float strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(c, 999));
        if (strokeDp > 0f) d.setStroke(Math.max(1, dp(c, strokeDp)), strokeColor);
        return d;
    }

    static RippleDrawable pillDrawable(Context c, boolean primary) {
        int fill = primary ? Color.argb(238, 12, 10, 18) : Color.argb(226, 10, 10, 14);
        int border = primary ? KuroTheme12.accentColor() : KuroDialogs581.BORDER;
        float width = primary ? 1.5f : 1.0f;
        GradientDrawable normal = pillBackground(c, fill, border, width);
        GradientDrawable focused = pillBackground(c, Color.argb(245, 22, 16, 32), KuroDialogs581.PURPLE_SOFT, 2.0f);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, focused);
        states.addState(new int[]{android.R.attr.state_pressed}, focused);
        states.addState(new int[]{}, normal);
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(64, 255, 255, 255)), states, null);
    }

    static TextView makePill(Context c, String label, boolean primary) {
        TextView t = new TextView(c);
        t.setText(label);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        t.setPadding(dp(c, 14), dp(c, 8), dp(c, 14), dp(c, 8));
        t.setMinHeight(dp(c, 36));
        t.setFocusable(true);
        t.setClickable(true);
        t.setBackground(pillDrawable(c, primary));
        return t;
    }

    /** Styles native ExoPlayer (`KuroNativePlayer561`) with OLED pill chips + accent timebar. */
    static void styleNativePlayer(
            Activity activity,
            FrameLayout surface,
            PlayerView view,
            TextView skip,
            ExoPlayer engine,
            Runnable onEpisodes,
            Runnable onFullscreen) {
        if (surface == null || view == null || skip == null || engine == null) return;
        skip.setBackground(pillDrawable(activity, true));
        skip.setTextColor(Color.WHITE);
        skip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        skip.setPadding(dp(activity, 16), dp(activity, 9), dp(activity, 16), dp(activity, 9));

        tintTimeBar(view);

        final LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setTag("native-oled-pills-153");

        final TextView speedPill = makePill(activity, "1.0x", false);
        speedPill.setContentDescription("Playback speed");
        speedPill.setOnClickListener(v -> {
            float cur = engine.getPlaybackParameters().speed;
            int idx = 0;
            for (int i = 0; i < SPEEDS.length; i++) {
                if (Math.abs(SPEEDS[i] - cur) < 0.04f) { idx = i; break; }
            }
            float next = SPEEDS[(idx + 1) % SPEEDS.length];
            engine.setPlaybackParameters(new PlaybackParameters(next));
            speedPill.setText((next == (int) next ? String.format(Locale.ROOT, "%.1f", next) : String.valueOf(next)) + "x");
        });
        bar.addView(speedPill);

        if (onEpisodes != null) {
            Space s1 = new Space(activity);
            bar.addView(s1, new LinearLayout.LayoutParams(dp(activity, 6), 1));
            TextView epPill = makePill(activity, "Episodes", false);
            epPill.setOnClickListener(v -> onEpisodes.run());
            bar.addView(epPill);
        }

        if (onFullscreen != null) {
            Space s2 = new Space(activity);
            bar.addView(s2, new LinearLayout.LayoutParams(dp(activity, 6), 1));
            TextView fsPill = makePill(activity, "Fullscreen", false);
            fsPill.setOnClickListener(v -> onFullscreen.run());
            bar.addView(fsPill);
        }

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        lp.setMargins(dp(activity, 12), dp(activity, 12), dp(activity, 12), 0);
        surface.addView(bar, lp);

        view.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility -> {
            bar.setVisibility(visibility == View.VISIBLE ? View.VISIBLE : View.GONE);
            tintTimeBar(view);
        });
    }

    /** Styles the offline downloads ExoPlayer (`KuroOfflineActivity561`) with OLED pills + accent timebar. */
    static void styleOfflinePlayer(
            Activity activity,
            LinearLayout top,
            TextView back,
            TextView pip,
            PlayerView view,
            ExoPlayer engine) {
        if (top == null || view == null || engine == null) return;
        top.setPadding(dp(activity, 10), dp(activity, 8), dp(activity, 10), dp(activity, 8));
        if (back != null) back.setBackground(pillDrawable(activity, false));
        if (pip != null) {
            pip.setText("PiP");
            pip.setBackground(pillDrawable(activity, false));
        }
        final TextView speedPill = makePill(activity, "1.0x", false);
        speedPill.setContentDescription("Playback speed");
        speedPill.setOnClickListener(v -> {
            float cur = engine.getPlaybackParameters().speed;
            int idx = 0;
            for (int i = 0; i < SPEEDS.length; i++) {
                if (Math.abs(SPEEDS[i] - cur) < 0.04f) { idx = i; break; }
            }
            float next = SPEEDS[(idx + 1) % SPEEDS.length];
            engine.setPlaybackParameters(new PlaybackParameters(next));
            speedPill.setText((next == (int) next ? String.format(Locale.ROOT, "%.1f", next) : String.valueOf(next)) + "x");
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.setMargins(0, 0, dp(activity, 6), 0);
        int insertAt = Math.max(0, top.getChildCount() - 1);
        top.addView(speedPill, insertAt, slp);
        tintTimeBar(view);
        view.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility -> tintTimeBar(view));
    }

    static void tintTimeBar(View root) {
        if (root == null) return;
        if (root instanceof DefaultTimeBar) {
            DefaultTimeBar tb = (DefaultTimeBar) root;
            tb.setPlayedColor(KuroTheme12.accentColor());
            tb.setScrubberColor(KuroDialogs581.PURPLE_SOFT);
            tb.setAdMarkerColor(KuroDialogs581.BORDER);
            tb.setBufferedColor(Color.argb(90, 255, 255, 255));
            tb.setUnplayedColor(Color.argb(45, 255, 255, 255));
            return;
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) tintTimeBar(vg.getChildAt(i));
        }
    }

    /** Creates the 16:9 watch player container that also intercepts bottom swipe-up in Enhanced Fullscreen. */
    static FrameLayout createWatchPlayerBox(Activity activity, FullscreenActions actions) {
        return new SwipeUpFullscreenLayout(activity, actions, true);
    }

    /** Wraps WebChromeClient custom fullscreen view with bottom swipe-up Up-Next gesture. */
    static FrameLayout wrapCustomFullscreen(Activity activity, View customView, FullscreenActions actions) {
        SwipeUpFullscreenLayout host = new SwipeUpFullscreenLayout(activity, actions, false);
        host.setBackgroundColor(Color.BLACK);
        host.addView(customView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        host.showSwipeHint();
        return host;
    }

    /** Enhances `enterEnhancedFullscreen()` toolbar + flashes bottom swipe-up hint. */
    static void attachEnhancedFullscreen(
            Activity activity,
            FrameLayout playerBox,
            LinearLayout toolbar,
            TextView exitBtn,
            FullscreenActions actions) {
        if (playerBox == null || toolbar == null || actions == null) return;
        JSONObject info = actions.watchInfo();
        JSONArray eps = info == null ? null : info.optJSONArray("episodes");
        int cur = info == null ? -1 : info.optInt("current", -1);
        boolean hasNext = eps != null && cur >= 0 && cur + 1 < eps.length();

        TextView epBtn = makePill(activity, "Episodes", false);
        epBtn.setMinHeight(dp(activity, 40));
        epBtn.setOnClickListener(v -> openFullscreenSheet(activity, playerBox, actions));
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp1.setMargins(0, 0, dp(activity, 6), 0);
        toolbar.addView(epBtn, 0, lp1);

        if (hasNext) {
            JSONObject nextObj = eps.optJSONObject(cur + 1);
            String nextNum = nextObj != null ? nextObj.optString("number", "") : eps.optString(cur + 1, "");
            TextView nextBtn = makePill(activity,
                    nextNum.isEmpty() ? "Next Ep \u203a" : ("Next Ep " + nextNum + " \u203a"), true);
            nextBtn.setMinHeight(dp(activity, 40));
            nextBtn.setOnClickListener(v -> actions.nextEpisode());
            LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp2.setMargins(0, 0, dp(activity, 6), 0);
            toolbar.addView(nextBtn, 1, lp2);
        }

        if (exitBtn != null) {
            exitBtn.setBackground(pillDrawable(activity, false));
            exitBtn.setTextColor(Color.WHITE);
            exitBtn.setPadding(dp(activity, 14), dp(activity, 8), dp(activity, 14), dp(activity, 8));
        }

        if (playerBox instanceof SwipeUpFullscreenLayout) {
            ((SwipeUpFullscreenLayout) playerBox).showSwipeHint();
        }
    }

    static void detachEnhancedFullscreen(FrameLayout playerBox) {
        closeActiveSheet();
        if (playerBox != null) {
            View oldHint = playerBox.findViewWithTag("kuro-fs-swipe-hint-153");
            if (oldHint != null) playerBox.removeView(oldHint);
        }
    }

    static boolean closeActiveSheet() {
        View sheet = activeFullscreenSheet;
        activeFullscreenSheet = null;
        if (sheet != null && sheet.getParent() instanceof ViewGroup) {
            ((ViewGroup) sheet.getParent()).removeView(sheet);
            return true;
        }
        return false;
    }

    static void openFullscreenSheet(Activity activity, FrameLayout host, FullscreenActions actions) {
        if (host == null || actions == null) return;
        JSONObject info = actions.watchInfo();
        JSONArray eps = info == null ? null : info.optJSONArray("episodes");
        if (eps == null || eps.length() == 0) return;
        closeActiveSheet();

        final int count = eps.length();
        final int current = Math.max(0, Math.min(count - 1, info.optInt("current", 0)));
        final boolean hasNext = current + 1 < count;

        final FrameLayout scrim = new FrameLayout(activity);
        scrim.setTag("kuro-fullscreen-sheet-153");
        scrim.setBackgroundColor(Color.argb(178, 0, 0, 0));
        scrim.setClickable(true);
        scrim.setFocusableInTouchMode(true);
        scrim.setOnClickListener(v -> closeActiveSheet());
        scrim.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                closeActiveSheet();
                return true;
            }
            return false;
        });

        final LinearLayout sheet = new LinearLayout(activity);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setClickable(true);
        GradientDrawable sheetBg = new GradientDrawable();
        sheetBg.setColor(KuroTheme12.amoled() ? Color.rgb(6, 6, 9) : KuroDialogs581.PANEL);
        float r = dp(activity, 22);
        sheetBg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        sheetBg.setStroke(Math.max(1, dp(activity, 1)), KuroDialogs581.BORDER);
        sheet.setBackground(sheetBg);
        sheet.setPadding(dp(activity, 18), dp(activity, 10), dp(activity, 18), dp(activity, 18));

        /* Drag handle at top (also supports swipe-down to dismiss) */
        LinearLayout handleWrap = new LinearLayout(activity);
        handleWrap.setOrientation(LinearLayout.VERTICAL);
        handleWrap.setGravity(Gravity.CENTER_HORIZONTAL);
        handleWrap.setPadding(0, dp(activity, 2), 0, dp(activity, 10));
        View handleBar = new View(activity);
        GradientDrawable hBg = new GradientDrawable();
        hBg.setColor(Color.argb(110, 255, 255, 255));
        hBg.setCornerRadius(dp(activity, 99));
        handleBar.setBackground(hBg);
        handleWrap.addView(handleBar, new LinearLayout.LayoutParams(dp(activity, 42), dp(activity, 4)));
        final float[] dragStartY = new float[]{0f};
        handleWrap.setOnTouchListener((v, ev) -> {
            if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
                dragStartY[0] = ev.getRawY();
                return true;
            }
            if (ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
                float dy = Math.max(0f, ev.getRawY() - dragStartY[0]);
                sheet.setTranslationY(dy);
                return true;
            }
            if (ev.getActionMasked() == MotionEvent.ACTION_UP || ev.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                if (ev.getRawY() - dragStartY[0] > dp(activity, 56)) closeActiveSheet();
                else sheet.animate().translationY(0f).setDuration(160).start();
                return true;
            }
            return false;
        });
        sheet.addView(handleWrap, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        /* Hero "UP NEXT" Banner inside Fullscreen Sheet */
        if (hasNext) {
            final int nextIdx = current + 1;
            JSONObject nextEp = eps.optJSONObject(nextIdx);
            String nextNum = nextEp != null ? nextEp.optString("number", String.valueOf(nextIdx + 1)) : eps.optString(nextIdx, String.valueOf(nextIdx + 1));
            String nextName = nextEp != null ? nextEp.optString("name", "").trim() : "";

            LinearLayout hero = new LinearLayout(activity);
            hero.setOrientation(LinearLayout.HORIZONTAL);
            hero.setGravity(Gravity.CENTER_VERTICAL);
            hero.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 12), dp(activity, 12));
            GradientDrawable heroBg = new GradientDrawable();
            heroBg.setColor(KuroTheme12.amoled() ? Color.rgb(12, 12, 18) : KuroDialogs581.CARD);
            heroBg.setCornerRadius(dp(activity, 16));
            heroBg.setStroke(Math.max(1, dp(activity, 1.5f)), KuroTheme12.accentColor());
            hero.setBackground(heroBg);

            LinearLayout meta = new LinearLayout(activity);
            meta.setOrientation(LinearLayout.VERTICAL);
            TextView badge = new TextView(activity);
            badge.setText("\u2726  UP NEXT");
            badge.setTextColor(KuroDialogs581.PURPLE_SOFT);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            badge.setLetterSpacing(0.08f);
            meta.addView(badge);

            TextView nextTitle = new TextView(activity);
            nextTitle.setText("Episode " + nextNum + (nextName.isEmpty() ? "" : " \u00b7 " + nextName));
            nextTitle.setTextColor(KuroDialogs581.INK);
            nextTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            nextTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            nextTitle.setSingleLine(true);
            nextTitle.setEllipsize(TextUtils.TruncateAt.END);
            meta.addView(nextTitle);
            hero.addView(meta, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            Space gap = new Space(activity);
            hero.addView(gap, new LinearLayout.LayoutParams(dp(activity, 10), 1));

            TextView playNext = makePill(activity, "\u25b6  Play Ep " + nextNum, true);
            playNext.setMinHeight(dp(activity, 42));
            playNext.setOnClickListener(v -> {
                closeActiveSheet();
                actions.nextEpisode();
            });
            hero.addView(playNext);
            sheet.addView(hero, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            Space afterHero = new Space(activity);
            sheet.addView(afterHero, new LinearLayout.LayoutParams(1, dp(activity, 12)));
        }

        /* Header row for Episodes list */
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView hTitle = new TextView(activity);
        hTitle.setText("Episodes  \u00b7  " + count);
        hTitle.setTextColor(KuroDialogs581.INK);
        hTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        hTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(hTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView closeBtn = makePill(activity, "Close \u2715", false);
        closeBtn.setOnClickListener(v -> closeActiveSheet());
        header.addView(closeBtn);
        sheet.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Space afterHead = new Space(activity);
        sheet.addView(afterHead, new LinearLayout.LayoutParams(1, dp(activity, 8)));

        /* Episode rows window around current episode */
        final ScrollView scroll = new ScrollView(activity);
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        int from = Math.max(0, current - 8);
        int to = Math.min(count, from + 60);
        View currentRowView = null;
        for (int i = from; i < to; i++) {
            final int idx = i;
            final boolean isCur = idx == current;
            JSONObject ep = eps.optJSONObject(idx);
            String num = ep != null ? ep.optString("number", String.valueOf(idx + 1)) : eps.optString(idx, String.valueOf(idx + 1));
            String name = ep != null ? ep.optString("name", "").trim() : "";
            if (name.isEmpty()) name = "Episode " + num;

            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
            row.setFocusable(true);
            row.setClickable(true);
            GradientDrawable rBg = new GradientDrawable();
            rBg.setColor(isCur
                    ? KuroDialogs581.SELECTED
                    : (KuroTheme12.amoled() ? Color.rgb(12, 12, 16) : KuroDialogs581.CARD));
            rBg.setCornerRadius(dp(activity, 12));
            rBg.setStroke(Math.max(1, dp(activity, 1)), isCur ? KuroTheme12.accentColor() : KuroDialogs581.DIVIDER);
            row.setBackground(rBg);

            TextView numView = new TextView(activity);
            numView.setText(num);
            numView.setTextColor(isCur ? KuroDialogs581.PURPLE_SOFT : KuroDialogs581.MUTED);
            numView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            numView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            numView.setMinWidth(dp(activity, 34));
            row.addView(numView);

            LinearLayout titleCol = new LinearLayout(activity);
            titleCol.setOrientation(LinearLayout.VERTICAL);
            TextView tView = new TextView(activity);
            tView.setText(name);
            tView.setTextColor(KuroDialogs581.INK);
            tView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            tView.setTypeface(Typeface.DEFAULT, isCur ? Typeface.BOLD : Typeface.NORMAL);
            tView.setSingleLine(true);
            tView.setEllipsize(TextUtils.TruncateAt.END);
            titleCol.addView(tView);

            long resumeMs = actions.savedPositionMs(num);
            if (resumeMs >= 5000L && !isCur) {
                long sec = resumeMs / 1000L;
                TextView sub = new TextView(activity);
                sub.setText(String.format(Locale.ROOT, "Resume %d:%02d", sec / 60L, sec % 60L));
                sub.setTextColor(KuroDialogs581.MUTED);
                sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
                titleCol.addView(sub);
            }
            row.addView(titleCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tag = new TextView(activity);
            tag.setText(isCur ? "PLAYING" : "\u25b6");
            tag.setTextColor(isCur ? KuroDialogs581.PURPLE_SOFT : KuroDialogs581.MUTED);
            tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, isCur ? 10f : 13f);
            tag.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            row.addView(tag);

            row.setOnClickListener(v -> {
                closeActiveSheet();
                if (!isCur) actions.chooseEpisode(idx);
            });

            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.setMargins(0, 0, 0, dp(activity, 6));
            list.addView(row, rlp);
            if (isCur) currentRowView = row;
        }
        scroll.addView(list);
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        int maxListH = Math.max(dp(activity, 140), Math.round(dm.heightPixels * 0.42f));
        sheet.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxListH));

        int maxSheetW = Math.min(dm.widthPixels, dp(activity, 640));
        FrameLayout.LayoutParams sheetLp = new FrameLayout.LayoutParams(
                maxSheetW, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        scrim.addView(sheet, sheetLp);
        host.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        activeFullscreenSheet = scrim;

        sheet.setTranslationY(dp(activity, 220));
        sheet.setAlpha(0.4f);
        sheet.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        final View scrollTarget = currentRowView;
        if (scrollTarget != null) {
            scroll.post(() -> scroll.scrollTo(0, Math.max(0, scrollTarget.getTop() - dp(activity, 28))));
        }
    }

    /** Builds the Kuro App-UI Auto-Next floating overlay card while keeping AlertDialog compatibility. */
    static AutoNextUi buildAutoNextDialog(
            Activity activity,
            AlertDialog.Builder preconfiguredBuilder,
            JSONObject watchInfo,
            int nextIndex,
            Runnable onCancel,
            Runnable onPlayNow) {
        String animeTitle = watchInfo != null ? watchInfo.optString("title", "Anime") : "Anime";
        String nextNum = String.valueOf(nextIndex + 1);
        String nextName = "";
        if (watchInfo != null) {
            JSONArray eps = watchInfo.optJSONArray("episodes");
            JSONObject ep = eps != null ? eps.optJSONObject(nextIndex) : null;
            if (ep != null) {
                nextNum = ep.optString("number", nextNum);
                nextName = ep.optString("name", "").trim();
            } else if (eps != null) {
                nextNum = eps.optString(nextIndex, nextNum);
            }
        }

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setTag("kuro-autonext-card-153");
        card.setPadding(dp(activity, 18), dp(activity, 16), dp(activity, 18), dp(activity, 16));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(KuroTheme12.amoled() ? Color.rgb(8, 7, 12) : KuroDialogs581.PANEL);
        cardBg.setCornerRadius(dp(activity, 20));
        cardBg.setStroke(Math.max(1, dp(activity, 1.5f)), KuroTheme12.accentColor());
        card.setBackground(cardBg);

        /* Top Eyebrow + Countdown Pill */
        LinearLayout topRow = new LinearLayout(activity);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView eyebrow = new TextView(activity);
        eyebrow.setText("\u2726  UP NEXT");
        eyebrow.setTextColor(KuroDialogs581.PURPLE_SOFT);
        eyebrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        eyebrow.setLetterSpacing(0.08f);
        topRow.addView(eyebrow, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView countBadge = new TextView(activity);
        countBadge.setText("in 10s");
        countBadge.setTextColor(Color.WHITE);
        countBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        countBadge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        countBadge.setPadding(dp(activity, 10), dp(activity, 4), dp(activity, 10), dp(activity, 4));
        countBadge.setBackground(pillBackground(activity, KuroDialogs581.SELECTED, KuroDialogs581.BORDER, 1f));
        topRow.addView(countBadge);
        card.addView(topRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Space s1 = new Space(activity);
        card.addView(s1, new LinearLayout.LayoutParams(1, dp(activity, 8)));

        /* Series + Next Episode Headline */
        TextView showView = new TextView(activity);
        showView.setText(animeTitle);
        showView.setTextColor(KuroDialogs581.MUTED);
        showView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        showView.setSingleLine(true);
        showView.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(showView);

        TextView epHeadline = new TextView(activity);
        epHeadline.setText("Episode " + nextNum + (nextName.isEmpty() ? "" : "  \u00b7  " + nextName));
        epHeadline.setTextColor(KuroDialogs581.INK);
        epHeadline.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        epHeadline.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        epHeadline.setMaxLines(2);
        epHeadline.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(epHeadline);

        Space s2 = new Space(activity);
        card.addView(s2, new LinearLayout.LayoutParams(1, dp(activity, 12)));

        /* Smooth Animated Glowing Progress Bar */
        FrameLayout track = new FrameLayout(activity);
        GradientDrawable trackBg = new GradientDrawable();
        trackBg.setColor(KuroDialogs581.CARD);
        trackBg.setCornerRadius(dp(activity, 99));
        track.setBackground(trackBg);
        View fill = new View(activity);
        GradientDrawable fillBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{KuroTheme12.accentColor(), KuroDialogs581.PURPLE_SOFT});
        fillBg.setCornerRadius(dp(activity, 99));
        fill.setBackground(fillBg);
        track.addView(fill, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        card.addView(track, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 5)));

        Space s3 = new Space(activity);
        card.addView(s3, new LinearLayout.LayoutParams(1, dp(activity, 14)));

        /* Pill Action Buttons */
        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        TextView cancelPill = makePill(activity, "Cancel", false);
        cancelPill.setMinHeight(dp(activity, 40));
        cancelPill.setOnClickListener(v -> onCancel.run());
        actions.addView(cancelPill);

        Space btnGap = new Space(activity);
        actions.addView(btnGap, new LinearLayout.LayoutParams(dp(activity, 8), 1));

        final TextView playPill = makePill(activity, "\u25b6  Play Ep " + nextNum, true);
        playPill.setMinHeight(dp(activity, 40));
        playPill.setOnClickListener(v -> onPlayNow.run());
        actions.addView(playPill);
        card.addView(actions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final AlertDialog d = preconfiguredBuilder
                .setTitle(null)
                .setMessage(null)
                .setView(card)
                .create();

        d.setOnShowListener(ignored -> {
            hideStockChrome(d);
            Window w = d.getWindow();
            if (w != null) {
                w.setBackgroundDrawableResource(android.R.color.transparent);
                w.setDimAmount(0.20f);
                DisplayMetrics dm = activity.getResources().getDisplayMetrics();
                int width = Math.min(dm.widthPixels - dp(activity, 28), dp(activity, 372));
                WindowManager.LayoutParams wlp = w.getAttributes();
                wlp.gravity = Gravity.BOTTOM | Gravity.END;
                wlp.x = dp(activity, 14);
                wlp.y = dp(activity, 18);
                wlp.width = Math.max(dp(activity, 250), width);
                wlp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                w.setAttributes(wlp);
            }
            card.setTranslationY(dp(activity, 24));
            card.setAlpha(0f);
            card.animate().translationY(0f).alpha(1f).setDuration(200)
                    .setInterpolator(new DecelerateInterpolator()).start();
            playPill.requestFocus();
        });

        return new AutoNextUi(d, countBadge, fill, track);
    }

    private static void hideStockChrome(AlertDialog d) {
        try {
            View neg = d.getButton(AlertDialog.BUTTON_NEGATIVE);
            View pos = d.getButton(AlertDialog.BUTTON_POSITIVE);
            if (neg != null) {
                neg.setVisibility(View.GONE);
                if (neg.getParent() instanceof View) ((View) neg.getParent()).setVisibility(View.GONE);
            }
            if (pos != null) pos.setVisibility(View.GONE);
        } catch (RuntimeException ignored) {}
    }

    private static final class SwipeUpFullscreenLayout extends FrameLayout {
        private final Activity activity;
        private final FullscreenActions actions;
        private final boolean watchBoxMode;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private float downX, downY;
        private boolean trackingBottom;

        SwipeUpFullscreenLayout(Activity activity, FullscreenActions actions, boolean watchBoxMode) {
            super(activity);
            this.activity = activity;
            this.actions = actions;
            this.watchBoxMode = watchBoxMode;
        }

        @Override
        protected void onMeasure(int w, int h) {
            if (!watchBoxMode || (actions != null && actions.isEnhancedFullscreen())) {
                super.onMeasure(w, h);
                return;
            }
            int width = MeasureSpec.getSize(w);
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(width * 9 / 16, MeasureSpec.EXACTLY));
        }

        void showSwipeHint() {
            View old = findViewWithTag("kuro-fs-swipe-hint-153");
            if (old != null) removeView(old);
            JSONObject info = actions != null ? actions.watchInfo() : null;
            JSONArray eps = info != null ? info.optJSONArray("episodes") : null;
            if (eps == null || eps.length() <= 1) return;
            LinearLayout pill = new LinearLayout(activity);
            pill.setTag("kuro-fs-swipe-hint-153");
            pill.setOrientation(LinearLayout.VERTICAL);
            pill.setGravity(Gravity.CENTER_HORIZONTAL);
            pill.setPadding(dp(activity, 14), dp(activity, 5), dp(activity, 14), dp(activity, 6));
            pill.setClickable(false);
            pill.setFocusable(false);
            pill.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            pill.setBackground(pillBackground(activity, Color.argb(165, 10, 10, 14), KuroDialogs581.BORDER, 1f));
            TextView label = new TextView(activity);
            label.setText("\u2303  Swipe up for Next Episode");
            label.setTextColor(Color.argb(225, 243, 238, 247));
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
            label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            pill.addView(label);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            lp.setMargins(0, 0, 0, dp(activity, 8));
            addView(pill, lp);
            handler.postDelayed(() -> pill.animate().alpha(0f).setDuration(350).withEndAction(() -> {
                if (pill.getParent() == this) removeView(pill);
            }).start(), 2500L);
        }

        private boolean fullscreenActive() {
            return !watchBoxMode || (actions != null && actions.isEnhancedFullscreen());
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            if (!fullscreenActive()) return false;
            if (activeFullscreenSheet != null && activeFullscreenSheet.getParent() == this) {
                return false;
            }
            int action = ev.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                downX = ev.getX();
                downY = ev.getY();
                int h = getHeight();
                trackingBottom = h > 0 && downY >= h - dp(activity, 92);
                return false;
            }
            if (action == MotionEvent.ACTION_MOVE && trackingBottom) {
                float dx = ev.getX() - downX;
                float dy = ev.getY() - downY;
                if (dy < -dp(activity, 28) && (-dy) > Math.abs(dx) * 1.55f) {
                    trackingBottom = false;
                    openFullscreenSheet(activity, this, actions);
                    return true;
                }
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                trackingBottom = false;
            }
            return false;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return fullscreenActive() || super.onTouchEvent(event);
        }
    }
}
"""


def replace_once(src: str, old: str, new: str, label: str) -> str:
    if old not in src:
        raise RuntimeError(f"Expected snippet not found for {label}")
    return src.replace(old, new, 1)


def patch_stream_player_assets(project: Path) -> None:
    (project / "assets/player-skin.js").write_text(PLAYER_SKIN_JS, encoding="utf-8")

    html_path = project / "assets/stream-player.html"
    html = html_path.read_text(encoding="utf-8")
    if 'src="player-skin.js"' not in html:
        html = replace_once(
            html,
            '<script src="stream-player.js"></script>',
            '<script src="stream-player.js"></script>\n<script src="player-skin.js"></script>',
            "stream-player.html script link",
        )
        html_path.write_text(html, encoding="utf-8")

    js_path = project / "assets/stream-player.js"
    js = js_path.read_text(encoding="utf-8")
    if "__kuroReadMarkers153" not in js:
        js = replace_once(
            js,
            " let skipConfig={on:false,ranges:[]};",
            " let skipConfig={on:false,ranges:[]};try{window.__kuroReadMarkers153=function(){return skipConfig;};}catch(_){}",
            "stream-player.js marker reader",
        )
        js_path.write_text(js, encoding="utf-8")


def patch_native_player(project: Path) -> None:
    path = project / "src/app/lovelycrxshx/kuro/KuroNativePlayer561.java"
    src = path.read_text(encoding="utf-8")
    if "KuroPlayerUi153.styleNativePlayer" in src:
        return

    src = replace_once(
        src,
        "    interface Listener { void failure(boolean verification); void ended(); }",
        "    interface Listener { void failure(boolean verification); void ended(); default void openEpisodes(){} default void toggleFullscreen(){} }",
        "KuroNativePlayer561.Listener defaults",
    )

    src = replace_once(
        src,
        "        attachGestures581();",
        "        attachGestures581();\n"
        "        KuroPlayerUi153.styleNativePlayer(activity,surface,view,skip,engine,\n"
        "                activity instanceof MainActivity?listener::openEpisodes:null,\n"
        "                activity instanceof MainActivity?listener::toggleFullscreen:null);",
        "KuroNativePlayer561 OLED pill styling",
    )

    src = replace_once(
        src,
        'skip.setText("Skip "+label);',
        'skip.setText("Skip "+label+" \\u21c4");',
        "KuroNativePlayer561 skip pill arrow",
    )

    src = replace_once(
        src,
        "                    if(!dragging[0]&&Math.abs(dy)>dp(22)&&Math.abs(dy)>Math.abs(dx)*1.4f){",
        "                    if(!dragging[0]&&start[1]>dp(48)&&start[1]<Math.max(dp(120),view.getHeight()-dp(78))&&Math.abs(dy)>dp(22)&&Math.abs(dy)>Math.abs(dx)*1.4f){",
        "KuroNativePlayer561 bottom swipe-up gesture clearance",
    )

    path.write_text(src, encoding="utf-8")


def patch_main_activity(project: Path) -> None:
    path = project / "src/app/lovelycrxshx/kuro/MainActivity.java"
    src = path.read_text(encoding="utf-8")
    if "KuroPlayerAction153" in src:
        return

    # 1. Fields
    src = replace_once(
        src,
        " private AlertDialog autoNextDialog573;private Runnable autoNextTick573;private KuroEpisodeDrawer581 episodeDrawer581;",
        " private AlertDialog autoNextDialog573;private KuroPlayerUi153.AutoNextUi autoNextUi153;private Runnable autoNextTick573;private KuroEpisodeDrawer581 episodeDrawer581;private long resumeFullscreenUntil153=0L;private ScriptHandler skinFrameScript153;",
        "MainActivity 1.5.3 fields",
    )

    # 2. Live theme sync into active player frames when theme/OLED changes
    src = replace_once(
        src,
        "void kuroTheme12WindowApplied(int floor){if(ui!=null)ui.setBackgroundColor(floor);if(root!=null)root.setBackgroundColor(floor);}",
        "void kuroTheme12WindowApplied(int floor){if(ui!=null)ui.setBackgroundColor(floor);if(root!=null)root.setBackgroundColor(floor);if(player!=null){prepareSkinFrameScript153();player.evaluateJavascript(KuroPlayerUi153.skinBootstrapJs(watchInfo561,playerAttempt),null);}}",
        "MainActivity live player theme sync",
    )

    # 3. cancelAutoNext573 & offerAutoNext573 App-UI floating card (preserving all static/Robolectric hooks)
    old_autonext = (
        " private void cancelAutoNext573(){\n"
        "  if(autoNextTick573!=null)main.removeCallbacks(autoNextTick573);autoNextTick573=null;\n"
        "  AlertDialog d=autoNextDialog573;autoNextDialog573=null;if(d!=null&&d.isShowing())d.dismiss();\n"
        " }\n"
        " private void offerAutoNext573(String session,int next){\n"
        "  if(!activityResumed||isInPictureInPictureMode()||playerLayer==null||ui==null||!session.equals(watchSession)||!getPreferences(MODE_PRIVATE).getBoolean(\"auto_next\",false))return;\n"
        "  cancelAutoNext573();final long end=android.os.SystemClock.elapsedRealtime()+10000;\n"
        "  final Runnable advance=()->{if(!session.equals(watchSession)||!activityResumed||isInPictureInPictureMode()||playerLayer==null||ui==null||!getPreferences(MODE_PRIVATE).getBoolean(\"auto_next\",false)){cancelAutoNext573();return;}cancelAutoNext573();closePlayer();if(ui!=null)ui.evaluateJavascript(\"window.kuroChooseEpisode&&window.kuroChooseEpisode(\"+next+\")\",null);};\n"
        "  AlertDialog d=new AlertDialog.Builder(KuroDialogs581.themed(this)).setTitle(\"Next episode\")\n"
        "   .setMessage(\"Playing the next episode in 10 seconds.\")\n"
        "   .setNegativeButton(\"Cancel\",(dialog,which)->cancelAutoNext573())\n"
        "   .setPositiveButton(\"Play now\",(dialog,which)->advance.run()).create();\n"
        "  autoNextDialog573=d;d.setOnCancelListener(dialog->cancelAutoNext573());d.setOnDismissListener(dialog->{if(autoNextDialog573==d){if(autoNextTick573!=null)main.removeCallbacks(autoNextTick573);autoNextTick573=null;autoNextDialog573=null;}});\n"
        "  d.show();autoNextTick573=new Runnable(){@Override public void run(){\n"
        "   if(autoNextDialog573!=d||!session.equals(watchSession)||!activityResumed||isInPictureInPictureMode()||!getPreferences(MODE_PRIVATE).getBoolean(\"auto_next\",false)){cancelAutoNext573();return;}\n"
        "   long remaining=end-android.os.SystemClock.elapsedRealtime();if(remaining<=0){advance.run();return;}d.setMessage(\"Playing the next episode in \"+((remaining+999)/1000)+\" seconds.\");main.postDelayed(this,250);\n"
        "  }};main.post(autoNextTick573);\n"
        " }"
    )
    new_autonext = (
        " private void cancelAutoNext573(){\n"
        "  if(autoNextTick573!=null)main.removeCallbacks(autoNextTick573);autoNextTick573=null;autoNextUi153=null;\n"
        "  AlertDialog d=autoNextDialog573;autoNextDialog573=null;if(d!=null&&d.isShowing())d.dismiss();\n"
        " }\n"
        " private void offerAutoNext573(String session,int next){\n"
        "  if(!activityResumed||isInPictureInPictureMode()||playerLayer==null||ui==null||!session.equals(watchSession)||!getPreferences(MODE_PRIVATE).getBoolean(\"auto_next\",false))return;\n"
        "  cancelAutoNext573();final long end=android.os.SystemClock.elapsedRealtime()+10000;\n"
        "  final Runnable advance=()->{if(!session.equals(watchSession)||!activityResumed||isInPictureInPictureMode()||playerLayer==null||ui==null||!getPreferences(MODE_PRIVATE).getBoolean(\"auto_next\",false)){cancelAutoNext573();return;}boolean keepFs153=customView!=null||enhancedFullscreen;cancelAutoNext573();if(keepFs153)resumeFullscreenUntil153=android.os.SystemClock.elapsedRealtime()+15000L;closePlayer();if(ui!=null)ui.evaluateJavascript(\"window.kuroChooseEpisode&&window.kuroChooseEpisode(\"+next+\")\",null);};\n"
        "  AlertDialog.Builder builder153=new AlertDialog.Builder(KuroDialogs581.themed(this)).setTitle(\"Next episode\")\n"
        "   .setMessage(\"Playing the next episode in 10 seconds.\")\n"
        "   .setNegativeButton(\"Cancel\",(dialog,which)->cancelAutoNext573())\n"
        "   .setPositiveButton(\"Play now\",(dialog,which)->advance.run());\n"
        "  final KuroPlayerUi153.AutoNextUi ui153=KuroPlayerUi153.buildAutoNextDialog(this,builder153,watchInfo561,next,this::cancelAutoNext573,advance);\n"
        "  AlertDialog d=ui153.dialog;\n"
        "  autoNextDialog573=d;autoNextUi153=ui153;d.setOnCancelListener(dialog->cancelAutoNext573());d.setOnDismissListener(dialog->{if(autoNextDialog573==d){if(autoNextTick573!=null)main.removeCallbacks(autoNextTick573);autoNextTick573=null;autoNextDialog573=null;autoNextUi153=null;}});\n"
        "  d.show();autoNextTick573=new Runnable(){@Override public void run(){\n"
        "   if(autoNextDialog573!=d||!session.equals(watchSession)||!activityResumed||isInPictureInPictureMode()||!getPreferences(MODE_PRIVATE).getBoolean(\"auto_next\",false)){cancelAutoNext573();return;}\n"
        "   long remaining=end-android.os.SystemClock.elapsedRealtime();if(remaining<=0){advance.run();return;}ui153.updateRemainingMs(remaining,10000L);main.postDelayed(this,120);\n"
        "  }};main.post(autoNextTick573);\n"
        " }"
    )
    src = replace_once(src, old_autonext, new_autonext, "MainActivity offerAutoNext573 App-UI")

    # 4. Register KuroPlayerAction153 bridge in openWatch
    src = replace_once(
        src,
        "setupProgress561(player);setupPresenceFrames565(player);",
        "setupProgress561(player);setupPresenceFrames565(player);setupPlayerActionBridge153(player,session,info.optString(\"episode\",\"\"),verification,completionReported);",
        "MainActivity setupPlayerActionBridge153",
    )

    # 5. Serve /player-skin.js on kuro.local for stream-player.html
    src = replace_once(
        src,
        '("/stream-player.html".equals(u.getPath())||"/stream-player.js".equals(u.getPath())||',
        '("/stream-player.html".equals(u.getPath())||"/stream-player.js".equals(u.getPath())||"/player-skin.js".equals(u.getPath())||',
        "MainActivity kuro.local /player-skin.js route",
    )

    # 6. Inject live Kuro theme + universal player-skin.js on top-frame finish fallback
    src = replace_once(
        src,
        'v.evaluateJavascript(assetText("player-skin.js"),null);',
        'v.evaluateJavascript(KuroPlayerUi153.skinBootstrapJs(watchInfo561,attempt)+"\\n"+assetText("player-skin.js"),null);',
        "MainActivity onPageFinished player-skin bootstrap",
    )

    # 7. Wrap WebView custom fullscreen with bottom swipe-up Up-Next gesture
    src = replace_once(
        src,
        "customView=v;customCallback=cb;updateKeepScreen560();full=new FrameLayout(MainActivity.this);full.setBackgroundColor(Color.BLACK);full.addView(v);root.addView(full,new FrameLayout.LayoutParams(-1,-1));",
        "customView=v;customCallback=cb;updateKeepScreen560();full=KuroPlayerUi153.wrapCustomFullscreen(MainActivity.this,v,fullscreenActions153());root.addView(full,new FrameLayout.LayoutParams(-1,-1));",
        "MainActivity onShowCustomView swipe-up wrapper",
    )

    # 8. Upgrade watchPlayerBox to SwipeUpFullscreenLayout
    src = replace_once(
        src,
        "FrameLayout playerBox=new FrameLayout(this){@Override protected void onMeasure(int w,int h){if(enhancedFullscreen){super.onMeasure(w,h);return;}int width=View.MeasureSpec.getSize(w);super.onMeasure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(width*9/16,View.MeasureSpec.EXACTLY));}};",
        "FrameLayout playerBox=KuroPlayerUi153.createWatchPlayerBox(this,fullscreenActions153());",
        "MainActivity watchPlayerBox swipe-up container",
    )

    # 9. Seamless fullscreen continuation when advancing episodes from fullscreen
    src = replace_once(
        src,
        "if(!loadPlayerOption561(options.getJSONObject(chosen),ref)){if(connectionCover[0]!=null)connectionCover[0].setVisibility(View.VISIBLE);main.post(retryServer[0]);}",
        "if(!loadPlayerOption561(options.getJSONObject(chosen),ref)){if(connectionCover[0]!=null)connectionCover[0].setVisibility(View.VISIBLE);main.post(retryServer[0]);}if(resumeFullscreenUntil153>android.os.SystemClock.elapsedRealtime()){resumeFullscreenUntil153=0L;enterEnhancedFullscreen();}",
        "MainActivity openWatch fullscreen continuation",
    )

    # 10. All-frame document-start injection of player-skin.js + native player Episodes/Fullscreen pills
    src = replace_once(
        src,
        "prepareFailureScript();prepareFxFrameScript();preparePresenceFrame565();",
        "prepareFailureScript();prepareFxFrameScript();preparePresenceFrame565();prepareSkinFrameScript153();",
        "MainActivity loadPlayer prepareSkinFrameScript153",
    )

    src = replace_once(
        src,
        "new KuroNativePlayer561.Listener(){@Override public void failure(boolean blocked){if(attempt==playerAttempt&&nativeFailure561!=null)nativeFailure561.accept(blocked);}@Override public void ended(){if(attempt==playerAttempt&&nativeEnded561!=null)nativeEnded561.run();}}",
        "new KuroNativePlayer561.Listener(){@Override public void failure(boolean blocked){if(attempt==playerAttempt&&nativeFailure561!=null)nativeFailure561.accept(blocked);}@Override public void ended(){if(attempt==playerAttempt&&nativeEnded561!=null)nativeEnded561.run();}@Override public void openEpisodes(){if(enhancedFullscreen&&watchPlayerBox!=null)KuroPlayerUi153.openFullscreenSheet(MainActivity.this,watchPlayerBox,fullscreenActions153());else openEpisodeDrawer581();}@Override public void toggleFullscreen(){if(enhancedFullscreen)exitEnhancedFullscreen();else enterEnhancedFullscreen();}}",
        "MainActivity native player OLED pill callbacks",
    )

    # 11. Fullscreen sheet cleanup + Enhanced Fullscreen OLED pills & swipe-up
    src = replace_once(
        src,
        "void hideFullscreen(){if(customView==null)return;",
        "void hideFullscreen(){KuroPlayerUi153.closeActiveSheet();if(customView==null)return;",
        "MainActivity hideFullscreen sheet cleanup",
    )

    src = replace_once(
        src,
        "watchPlayerBox.addView(toolbar,tp);enhancedToolbar=toolbar;",
        "watchPlayerBox.addView(toolbar,tp);enhancedToolbar=toolbar;KuroPlayerUi153.attachEnhancedFullscreen(this,watchPlayerBox,toolbar,exit,fullscreenActions153());",
        "MainActivity enterEnhancedFullscreen OLED pills",
    )

    src = replace_once(
        src,
        "if(!enhancedFullscreen)return;enhancedFullscreen=false;",
        "if(!enhancedFullscreen)return;enhancedFullscreen=false;KuroPlayerUi153.detachEnhancedFullscreen(watchPlayerBox);",
        "MainActivity exitEnhancedFullscreen detach",
    )

    # 12. Helper methods, closePlayer cleanup, and Back-button sheet priority
    helper_methods = """
 private KuroPlayerUi153.FullscreenActions fullscreenActions153(){
  return new KuroPlayerUi153.FullscreenActions(){
   @Override public JSONObject watchInfo(){return watchInfo561;}
   @Override public boolean isEnhancedFullscreen(){return enhancedFullscreen;}
   @Override public void chooseEpisode(int index){
    if(customView!=null||enhancedFullscreen)resumeFullscreenUntil153=android.os.SystemClock.elapsedRealtime()+15000L;
    chooseEpisode581(index);
   }
   @Override public void nextEpisode(){
    JSONObject wi=watchInfo561;JSONArray eps=wi==null?null:wi.optJSONArray("episodes");int cur=wi==null?-1:wi.optInt("current",-1);
    if(eps!=null&&cur>=0&&cur+1<eps.length()){
     if(customView!=null||enhancedFullscreen)resumeFullscreenUntil153=android.os.SystemClock.elapsedRealtime()+15000L;
     chooseEpisode581(cur+1);
    }
   }
   @Override public void toggleEnhancedFullscreen(){if(enhancedFullscreen)exitEnhancedFullscreen();else enterEnhancedFullscreen();}
   @Override public long savedPositionMs(String episodeNumber){
    JSONObject wi=watchInfo561;if(wi==null||episodeNumber==null||episodeNumber.isEmpty())return 0L;
    return KuroPlayback561.read(MainActivity.this,KuroPlayback561.key(wi.optString("source"),wi.optString("showIdentity"),episodeNumber));
   }
  };
 }
 private void prepareSkinFrameScript153(){
  if(skinFrameScript153!=null){try{skinFrameScript153.remove();}catch(RuntimeException|LinkageError ignored){}skinFrameScript153=null;}
  if(player==null||!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT))return;
  try{
   String code=KuroPlayerUi153.skinBootstrapJs(watchInfo561,playerAttempt)+"\\n"+assetText("player-skin.js");
   skinFrameScript153=WebViewCompat.addDocumentStartJavaScript(player,code,Collections.singleton("*"));
  }catch(IOException|RuntimeException|LinkageError ignored){skinFrameScript153=null;}
 }
 private void setupPlayerActionBridge153(WebView view,final String session,final String number,final boolean[] verification,final boolean[] completionReported){
  if(view==null||!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))return;
  try{
   WebViewCompat.addWebMessageListener(view,"KuroPlayerAction153",Collections.singleton("*"),(sender,message,origin,isMainFrame,reply)->{
    if(sender!=player||playerLayer==null||!activityResumed||message.getType()!=WebMessageCompat.TYPE_STRING)return;
    String data=message.getData();if(data==null||data.length()>40)return;
    int colon=data.indexOf(':');if(colon<=0)return;
    String action=data.substring(0,colon);long msgAttempt;
    try{msgAttempt=Long.parseLong(data.substring(colon+1));}catch(NumberFormatException e){return;}
    if(msgAttempt!=0L&&msgAttempt!=playerAttempt)return;
    if("next".equals(action)){
     fullscreenActions153().nextEpisode();
    }else if("episodes".equals(action)){
     if(full!=null)KuroPlayerUi153.openFullscreenSheet(MainActivity.this,full,fullscreenActions153());
     else if(enhancedFullscreen&&watchPlayerBox!=null)KuroPlayerUi153.openFullscreenSheet(MainActivity.this,watchPlayerBox,fullscreenActions153());
     else openEpisodeDrawer581();
    }else if("pip".equals(action)){
     enterPip560();
    }else if("fullscreen".equals(action)){
     if(enhancedFullscreen)exitEnhancedFullscreen();else enterEnhancedFullscreen();
    }else if("ended".equals(action)){
     if(!fxOriginAllowed(origin)||verification[0]||!session.equals(watchSession))return;
     JSONObject wi=watchInfo561;JSONArray eps=wi==null?null:wi.optJSONArray("episodes");
     int nextIdx=wi==null?-1:wi.optInt("current",-1)+1;
     if(!resumeKey561.isEmpty())KuroPlayback561.completed(MainActivity.this,resumeKey561);
     if(!completionReported[0]&&ui!=null&&number.matches("[1-9][0-9]{0,3}")){
      completionReported[0]=true;
      ui.evaluateJavascript("window.kuroEpisodeEnded553&&window.kuroEpisodeEnded553("+JSONObject.quote(session)+","+JSONObject.quote(number)+")",null);
     }
     if(getPreferences(MODE_PRIVATE).getBoolean("auto_next",false)&&eps!=null&&nextIdx>0&&nextIdx<eps.length()){
      offerAutoNext573(session,nextIdx);
     }
    }
   });
  }catch(RuntimeException|LinkageError ignored){}
 }
"""
    src = replace_once(
        src,
        " void openEpisodeDrawer581(){",
        helper_methods + " void openEpisodeDrawer581(){",
        "MainActivity 1.5.3 helper methods",
    )

    src = replace_once(
        src,
        "void closePlayer(){cancelAutoNext573();",
        "void closePlayer(){cancelAutoNext573();KuroPlayerUi153.closeActiveSheet();",
        "MainActivity closePlayer sheet cleanup",
    )

    src = replace_once(
        src,
        "fxFrameScript=null;playerErrorScript=null;",
        "if(skinFrameScript153!=null){try{skinFrameScript153.remove();}catch(RuntimeException|LinkageError ignored){}skinFrameScript153=null;}fxFrameScript=null;playerErrorScript=null;",
        "MainActivity closePlayer skinFrameScript153 cleanup",
    )

    src = replace_once(
        src,
        "@Override public void onBackPressed(){",
        "@Override public void onBackPressed(){if(KuroPlayerUi153.closeActiveSheet())return;",
        "MainActivity onBackPressed sheet priority",
    )

    path.write_text(src, encoding="utf-8")


def patch_offline_player(project: Path) -> None:
    path = project / "src/app/lovelycrxshx/kuro/KuroOfflineActivity561.java"
    if not path.is_file():
        return
    src = path.read_text(encoding="utf-8")
    if "KuroPlayerUi153.styleOfflinePlayer" in src:
        return
    src = replace_once(
        src,
        "setSeekBackIncrementMs(10000).setSeekForwardIncrementMs(10000).build();view.setPlayer(engine);",
        "setSeekBackIncrementMs(10000).setSeekForwardIncrementMs(10000).build();view.setPlayer(engine);KuroPlayerUi153.styleOfflinePlayer(this,top,back,pip,view,engine);",
        "KuroOfflineActivity561 OLED pill styling",
    )
    path.write_text(src, encoding="utf-8")


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: python3 tools/player_1.5.3.py <project-dir>")
    project = Path(sys.argv[1]).resolve()
    patch_stream_player_assets(project)
    (project / "src/app/lovelycrxshx/kuro/KuroPlayerUi153.java").write_text(
        KURO_PLAYER_UI_153_JAVA, encoding="utf-8"
    )
    patch_native_player(project)
    patch_offline_player(project)
    patch_main_activity(project)
    print("Applied Kuro 1.5.3 custom OLED player, fullscreen swipe-up, and App-UI Auto-Next patches.")


if __name__ == "__main__":
    main()
