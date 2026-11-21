/* Kuro 1.5.2 WebView polish:
 *   1. Pure black (OLED) mode toggle in Settings > Appearance and startup, synced
 *      across all 20 WebView accents and native KuroTheme12 surfaces.
 *   2. One-tap Backup & restore (.json) shortcut in Settings > Quick actions.
 *   3. Layout-matched shimmer loading skeletons on Home shelves and Browse/Home grids.
 */
(function () {
 'use strict';

 var AMOLED_KEY = 'kuro-amoled-152';

 function isLightMode() {
  if (document.documentElement.getAttribute('data-theme') === 'light') return true;
  try {
   var raw = localStorage.getItem('kuro-settings560-v1');
   if (raw && JSON.parse(raw).theme === 'light') return true;
  } catch (e) {}
  return false;
 }

 function amoledEnabled() {
  try { return localStorage.getItem(AMOLED_KEY) === '1'; } catch (e) { return false; }
 }

 function syncAmoled() {
  var on = amoledEnabled();
  var active = on && !isLightMode();
  if (active) document.documentElement.setAttribute('data-amoled', '1');
  else document.documentElement.removeAttribute('data-amoled');
  try {
   if (window.KuroNative && typeof window.KuroNative.themeAmoled152 === 'function') {
    window.KuroNative.themeAmoled152(Boolean(active));
   }
  } catch (e) {}
 }

 function setAmoled(on) {
  try { localStorage.setItem(AMOLED_KEY, on ? '1' : '0'); } catch (e) {}
  syncAmoled();
 }

 window.kuroAmoled152 = {
  enabled: amoledEnabled,
  set: setAmoled,
  sync: syncAmoled
 };

 /* Apply before first paint of main content */
 syncAmoled();

 if (!window.km15) return;
 var K = window.km15;

 /* -------------------------------------------- Pure black Settings row --- */
 function updateAmoledRow(row) {
  if (!row) return;
  var on = amoledEnabled();
  var light = isLightMode();
  row.setAttribute('aria-pressed', on ? 'true' : 'false');
  var pill = row.querySelector('.km-amoled-pill-152');
  if (pill) pill.textContent = on ? (light ? 'Saved (Dark)' : 'On') : 'Off';
  var sub = row.querySelector('small');
  if (sub) {
   sub.textContent = light
    ? 'Saved for Dark theme \u00b7 switch App theme to Dark to activate #000000 OLED surfaces.'
    : 'True #000000 background and #0A0A0E cards across all twenty accents.';
  }
 }

 function mountAmoledRow() {
  var overlay = document.getElementById('settings-overlay560');
  if (!overlay) return;

  var themeSelect = overlay.querySelector('select[data-setting="theme"]');
  if (themeSelect && !themeSelect.__kmAmoled152) {
   themeSelect.__kmAmoled152 = true;
   themeSelect.addEventListener('change', function () {
    setTimeout(function () {
     syncAmoled();
     updateAmoledRow(overlay.querySelector('.km-amoled-row-152'));
    }, 0);
   });
  }

  var existing = overlay.querySelector('.km-amoled-row-152');
  if (existing) {
   updateAmoledRow(existing);
   return;
  }

  var group = null;
  Array.prototype.forEach.call(overlay.querySelectorAll('.settings-group560'), function (g) {
   var key = (g.getAttribute('data-section') || '').toLowerCase();
   if (!group && key.indexOf('appearance') === 0) group = g;
  });
  if (!group) return;
  var rows = group.querySelector('.settings-rows560');
  if (!rows) return;

  var row = document.createElement('button');
  row.type = 'button';
  row.className = 'settings-row560 settings-action560 km-amoled-row-152';
  row.setAttribute('data-filter', 'pure black oled amoled dark background battery');
  row.setAttribute('aria-pressed', amoledEnabled() ? 'true' : 'false');
  row.innerHTML =
   '<span><strong>Pure black (OLED)</strong>' +
   '<small>True #000000 background and #0A0A0E cards across all twenty accents.</small></span>' +
   '<span class="km-amoled-pill-152">' + (amoledEnabled() ? 'On' : 'Off') + '</span>';

  row.addEventListener('click', function () {
   var next = !amoledEnabled();
   setAmoled(next);
   updateAmoledRow(row);
   K.toast(next ? 'Pure black (OLED) on' : 'Pure black (OLED) off');
  });

  var picker = rows.querySelector('#accent-111');
  if (picker && picker.nextSibling) rows.insertBefore(row, picker.nextSibling);
  else if (picker) rows.appendChild(row);
  else rows.insertBefore(row, rows.firstChild);
  updateAmoledRow(row);
 }

 /* ------------------------------------- One-tap Backup & Restore sheet --- */
 function exportBackupNow() {
  try {
   if (!window.kuroBackup560 || typeof window.kuroBackup560.exportText !== 'function') {
    K.toast('Backup tool is not ready yet');
    return;
   }
   var payload = window.kuroBackup560.exportText();
   if (K.nativeHas('backupFile560')) {
    K.nativeCall('backupFile560', ['export', payload]);
   } else {
    K.toast('File export is available in the Android app');
   }
  } catch (e) {
   K.toast((e && e.message) || 'Could not create backup');
  }
 }

 function importBackupNow() {
  var overlay = document.getElementById('settings-overlay560');
  var transferBtn = overlay && overlay.querySelector('[data-action="transfer"]');
  if (transferBtn) transferBtn.click();
  if (K.nativeHas('backupFile560')) {
   setTimeout(function () { K.nativeCall('backupFile560', ['import', '']); }, 60);
  } else {
   K.toast('File import is available in the Android app');
  }
 }

 function openBackupQuickSheet() {
  var overlay = document.getElementById('settings-overlay560');
  K.sheet('Backup & restore', 'Lists, history, checkpoints, profile art & theme', [
   {
    label: 'Export local backup (.json)',
    sub: 'Save your lists, watch progress, profile art, theme and settings to a file',
    tone: 'primary',
    onPick: exportBackupNow
   },
   {
    label: 'Import & merge backup (.json)',
    sub: 'Pick a .json backup to preview and safely merge without overwriting',
    onPick: importBackupNow
   },
   {
    label: 'Tracker import (MyAnimeList / AniList)',
    sub: 'Open the full Import / Export panel',
    onPick: function () {
     var btn = overlay && overlay.querySelector('[data-action="transfer"]');
     if (btn) btn.click();
    }
   }
  ]);
 }

 function mountBackupQuickRow() {
  var overlay = document.getElementById('settings-overlay560');
  if (!overlay || overlay.querySelector('.km-backup-quick-152')) return;
  var group = null;
  Array.prototype.forEach.call(overlay.querySelectorAll('.settings-group560'), function (g) {
   var key = (g.getAttribute('data-section') || '').toLowerCase();
   if (!group && key.indexOf('quick actions') === 0) group = g;
  });
  if (!group) return;
  var rows = group.querySelector('.settings-rows560');
  if (!rows) return;

  var row = document.createElement('button');
  row.type = 'button';
  row.className = 'settings-row560 settings-action560 km-backup-quick-152';
  row.setAttribute('data-filter', 'backup restore export import json transfer local file');
  row.innerHTML =
   '<span><strong>Backup &amp; restore (.json)</strong>' +
   '<small>One-tap local export or selective restore with episode checkpoints</small></span>' +
   '<b aria-hidden="true">\u203a</b>';
  row.addEventListener('click', openBackupQuickSheet);
  rows.appendChild(row);
 }

 /* ------------------------------------- Home & Browse v2 Skeletons ------- */
 function skelMarkup(count) {
  var out = [];
  for (var i = 0; i < count; i++) {
   out.push(
    '<div class="km-skel-card-152">' +
    '<div class="km-skel-poster-152"></div>' +
    '<div class="km-skel-line-152"></div>' +
    '<div class="km-skel-line-152 km-skel-short-152"></div>' +
    '</div>'
   );
  }
  return out.join('');
 }

 function syncShelfSkeleton(id, count) {
  var shelf = document.getElementById(id);
  if (!shelf) return false;
  var hasCards = Boolean(shelf.querySelector('[data-card]'));
  var loadingDot = Boolean(!hasCards && shelf.querySelector('.rail-message .pager-dot'));
  var skel = shelf.querySelector('.km-skel-shelf-152');
  if (loadingDot) {
   if (!shelf.classList.contains('km-has-skel-152')) shelf.classList.add('km-has-skel-152');
   if (!skel) {
    var wrap = document.createElement('div');
    wrap.className = 'km-skel-shelf-152';
    wrap.setAttribute('aria-hidden', 'true');
    wrap.innerHTML = skelMarkup(count);
    shelf.appendChild(wrap);
   }
   return true;
  }
  if (shelf.classList.contains('km-has-skel-152')) shelf.classList.remove('km-has-skel-152');
  if (skel && skel.parentNode) skel.parentNode.removeChild(skel);
  return false;
 }

 function syncResultsSkeleton(main) {
  var results = main.querySelector('#results');
  if (!results) return;
  var grid = results.querySelector('.pager-grid');
  var status = results.querySelector('.pager-status');
  var existing = results.querySelector('.km-skel-results-152');
  var isLoading = Boolean(
   grid &&
   grid.children.length === 0 &&
   status &&
   status.querySelector('.pager-dot, .pager-loading')
  );
  if (isLoading) {
   if (!existing) {
    var wrap = document.createElement('div');
    wrap.className = 'km-skel-results-152';
    wrap.setAttribute('aria-hidden', 'true');
    wrap.innerHTML = skelMarkup(10);
    grid.insertAdjacentElement('afterend', wrap);
   }
  } else if (existing && existing.parentNode) {
   existing.parentNode.removeChild(existing);
  }
 }

 function syncSkeletons(main) {
  var popLoading = syncShelfSkeleton('home-popular', 6);
  syncShelfSkeleton('home-new', 6);
  syncResultsSkeleton(main);

  var spot = main.querySelector('#spotlight');
  var hasDeck = Boolean(spot && spot.querySelector('.hero-deck581'));
  var wantHeroSkel = Boolean(spot && !hasDeck && popLoading);
  if (wantHeroSkel && !main.classList.contains('km-home-loading-152')) {
   main.classList.add('km-home-loading-152');
  } else if (!wantHeroSkel && main.classList.contains('km-home-loading-152')) {
   main.classList.remove('km-home-loading-152');
  }
 }

 /* ---------------------------------------------------------------- boot --- */
 function render(main) {
  syncAmoled();
  mountAmoledRow();
  mountBackupQuickRow();
  syncSkeletons(main);
 }

 K.observe(render);
})();
