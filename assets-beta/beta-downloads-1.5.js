/* Kuro 1.5-beta: downloads that tidy themselves up.
 *
 * The list, the sizes and the deletes all come from the shell
 * (KuroNative.downloadsSummary15 / downloadsClean15), which reads Media3's own
 * download index and cache. This file only draws it and remembers the
 * auto-clean preference.
 *
 * "Watched" means: this download was opened at least once and the player no
 * longer holds a checkpoint for it - i.e. it was finished, or deliberately
 * restarted from the beginning. A download that is playing right now is never
 * removed, even by "Delete all".
 */
(function () {
 'use strict';

 if (!window.km15) return;
 var K = window.km15;

 var PREF_KEY = 'kuro-download-clean-15';
 var ROW_CLASS = 'km-dl-row-15';
 var DAY_MS = 24 * 60 * 60 * 1000;

 function pref() {
  var raw = K.lsGet(PREF_KEY, { days: 0, last: 0 });
  return { days: Number(raw.days) || 0, last: Number(raw.last) || 0 };
 }

 function setPref(next) { K.lsSet(PREF_KEY, next); }

 function size(bytes) {
  var n = Number(bytes) || 0;
  if (n <= 0) return 'nothing stored';
  if (n >= 1024 * 1024 * 1024) return (n / (1024 * 1024 * 1024)).toFixed(1) + ' GB';
  return Math.max(1, Math.round(n / (1024 * 1024))) + ' MB';
 }

 function summary() {
  if (!K.nativeHas('downloadsSummary15')) return null;
  var raw = K.nativeCall('downloadsSummary15', []);
  try {
   var data = typeof raw === 'string' ? JSON.parse(raw) : raw;
   if (!data || typeof data !== 'object') return null;
   return {
    count: Number(data.count) || 0,
    ready: Number(data.ready) || 0,
    watched: Number(data.watched) || 0,
    bytes: Number(data.bytes) || 0
   };
  } catch (e) { return null; }
 }

 function clean(action, days) {
  if (!K.nativeHas('downloadsClean15')) return -1;
  var n = K.nativeCall('downloadsClean15', [String(action), Number(days) || 0]);
  return typeof n === 'number' ? n : -1;
 }

 function panel() {
  var sum = summary();
  if (!sum) { K.toast('Downloads are not available in this build.'); return; }
  var p = pref();
  var subtitle = sum.count
   ? sum.count + (sum.count === 1 ? ' episode \u00b7 ' : ' episodes \u00b7 ') + size(sum.bytes) + ' on this phone'
   : 'Nothing downloaded right now';

  var rows = [];
  rows.push({
   label: 'Delete watched episodes' + (sum.watched ? ' (' + sum.watched + ')' : ''),
   sub: sum.watched ? 'Finished offline copies, kept nowhere else' : 'Nothing has been watched to the end yet',
   tone: sum.watched ? 'danger' : undefined,
   onPick: function () {
    var n = clean('watched', 0);
    K.toast(n > 0 ? 'Removed ' + n + (n === 1 ? ' download' : ' downloads') : 'Nothing to remove');
    setTimeout(panel, 500);
   }
  });
  rows.push({
   label: 'Keep only the latest episode of each series',
   sub: 'Earlier episodes of the same title are removed',
   onPick: function () {
    var n = clean('latest', 0);
    K.toast(n > 0 ? 'Removed ' + n + (n === 1 ? ' older episode' : ' older episodes') : 'Nothing older to remove');
    setTimeout(panel, 500);
   }
  });
  rows.push({
   label: 'Delete all downloads' + (sum.count ? ' (' + sum.count + ')' : ''),
   sub: 'Your lists, history and settings are not touched',
   tone: sum.count ? 'danger' : undefined,
   onPick: function () {
    var n = clean('all', 0);
    K.toast(n > 0 ? 'Removed ' + n + (n === 1 ? ' download' : ' downloads') : 'Nothing to remove');
    setTimeout(panel, 500);
   }
  });
  rows.push({
   label: 'Auto-clean watched episodes: ' + (p.days ? 'after ' + p.days + ' days' : 'off'),
   sub: 'Runs at most once a day, when you open Kuro',
   onPick: function () {
    var next = p.days === 0 ? 7 : p.days === 7 ? 30 : 0;
    setPref({ days: next, last: p.last });
    K.toast(next ? 'Auto-clean after ' + next + ' days' : 'Auto-clean off');
    setTimeout(panel, 300);
   }
  });

  K.sheet('Downloads', subtitle, rows);
 }

 function settingsRow(main) {
  if (!K.nativeHas('downloadsSummary15')) return;
  var overlay = document.getElementById('settings-overlay560');
  if (!overlay) return;
  var action = overlay.querySelector('[data-action="downloads"]');
  if (!action || !action.parentNode) return;
  if (overlay.querySelector('.' + ROW_CLASS)) return;

  var row = document.createElement('button');
  row.type = 'button';
  row.className = 'settings-row560 settings-action560 ' + ROW_CLASS;
  row.innerHTML = '<span><strong>Clean up downloads</strong>' +
   '<small>Storage used, watched episodes and automatic clean-up</small></span>' +
   '<b aria-hidden="true">\u203a</b>';
  row.addEventListener('click', panel);
  action.parentNode.insertBefore(row, action.nextSibling);
}

 function autoClean() {
  var p = pref();
  if(!p.days) return;
  if(Date.now() - p.last < DAY_MS) return;
  if(!K.nativeHas('downloadsClean15')) return;
  var n = clean('older', p.days);
  setPref({ days: p.days, last: Date.now() });
  if(n > 0) K.toast('Auto-cleaned ' + n + (n === 1 ? ' watched episode' : ' watched episodes'));
}

 function render(main) { settingsRow(main); }

 K.observe(render);
 setTimeout(autoClean, 2500);
})();
