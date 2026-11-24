/* Kuro 1.5-beta extras: the small shared pieces the new Home and Settings
 * surfaces need.
 *
 *  - one bottom sheet, used by the poster long-press, What's new and the
 *    downloads clean-up panel (CSP-safe: class-driven, no inline style)
 *  - one render scheduler: every module registers a function, and one
 *    MutationObserver on <body> calls them after the app re-renders a page
 *  - a toast for confirmations the app has no home for
 *
 * Nothing here touches the app's own state directly; the modules that use it
 * read and write the same localStorage keys the app already uses.
 */
window.km15 = (function () {
 'use strict';

 var hooks = [], queued = false, openSheet = null, lastFocus = null, toastEl = null, toastTimer = 0;

 function esc(s) {
  return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
   return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
  });
 }

 function lsGet(key, fallback) {
  try {
   var raw = localStorage.getItem(key);
   return raw == null ? fallback : JSON.parse(raw);
  } catch (e) { return fallback; }
 }

 function lsSet(key, value) {
  try { localStorage.setItem(key, JSON.stringify(value)); return true; }
  catch (e) { return false; }
 }

 function norm(s) { return String(s == null ? '' : s).toLowerCase().replace(/[^a-z0-9]+/g, ''); }

 function nativeHas(name) {
  try { return !!window.KuroNative && typeof window.KuroNative[name] === 'function'; }
  catch (e) { return false; }
 }

 function nativeCall(name, args) {
  try { return window.KuroNative[name].apply(window.KuroNative, args || []); }
  catch (e) { return null; }
 }

 /* ------------------------------------------------------------- toast --- */
 function toast(text) {
  if (!document.body) return;
  if (!toastEl) {
   toastEl = document.createElement('div');
   toastEl.className = 'km-toast-15';
   toastEl.setAttribute('role', 'status');
   toastEl.setAttribute('aria-live', 'polite');
   document.body.appendChild(toastEl);
  }
  toastEl.textContent = String(text);
  toastEl.classList.add('km-toast-on-15');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(function () {
   if (toastEl) toastEl.classList.remove('km-toast-on-15');
  }, 2600);
 }

 /* ------------------------------------------------------------- sheet --- */
 function closeSheet() {
  if (!openSheet) return;
  var el = openSheet;
  openSheet = null;
  el.classList.remove('km-sheet-on-15');
  setTimeout(function () { if (el.parentNode) el.parentNode.removeChild(el); }, 180);
  if (lastFocus && lastFocus.isConnected && typeof lastFocus.focus === 'function') {
   try { lastFocus.focus({ preventScroll: true }); } catch (e) { try { lastFocus.focus(); } catch (_) {} }
  }
  lastFocus = null;
 }

 /**
  * rows: [{ label, sub, onPick, tone }]
  * tone: 'primary' | 'danger' | undefined
  */
 function sheet(title, subtitle, rows) {
  closeSheet();
  var el = document.createElement('div');
  el.className = 'km-sheet-15';
  el.innerHTML =
   '<div class="km-sheet-scrim-15" data-sheet-close="1"></div>' +
   '<div class="km-sheet-panel-15" role="dialog" aria-modal="true" aria-labelledby="km-sheet-title-15">' +
   '<div class="km-sheet-head-15"><div>' +
   '<strong id="km-sheet-title-15">' + esc(title) + '</strong>' +
   (subtitle ? '<small>' + esc(subtitle) + '</small>' : '') +
   '</div><button type="button" class="km-sheet-close-15" aria-label="Close" data-sheet-close="1">\u00d7</button></div>' +
   '<div class="km-sheet-rows-15">' + rows.map(function (r, i) {
    return '<button type="button" class="km-sheet-row-15' +
     (r.tone ? ' km-sheet-' + r.tone + '-15' : '') + '" data-sheet-row="' + i + '">' +
     '<span><strong>' + esc(r.label) + '</strong>' +
     (r.sub ? '<small>' + esc(r.sub) + '</small>' : '') + '</span><b aria-hidden="true">\u203a</b></button>';
   }).join('') + '</div></div>';

  lastFocus = document.activeElement;
  document.body.appendChild(el);
  openSheet = el;
  requestAnimationFrame(function () { el.classList.add('km-sheet-on-15'); });

  el.addEventListener('click', function (ev) {
   var close = ev.target.closest && ev.target.closest('[data-sheet-close]');
   if (close) { closeSheet(); return; }
   var row = ev.target.closest && ev.target.closest('[data-sheet-row]');
   if (!row) return;
   var picked = rows[Number(row.getAttribute('data-sheet-row'))];
   closeSheet();
   if (picked && typeof picked.onPick === 'function') setTimeout(picked.onPick, 120);
  });

  var first = el.querySelector('.km-sheet-row-15') || el.querySelector('.km-sheet-close-15');
  if (first) { try { first.focus({ preventScroll: true }); } catch (e) { try { first.focus(); } catch (_) {} } }
  return el;
 }

 if (typeof document !== 'undefined') {
  document.addEventListener('keydown', function (ev) {
   if (!openSheet) return;
   if (ev.key === 'Escape') { ev.preventDefault(); closeSheet(); }
  });
 }

 /* ---------------------------------------------------------- scheduler --- */
 function run() {
  var main = document.getElementById('main');
  if (!main) return;
  for (var i = 0; i < hooks.length; i++) {
   try { hooks[i](main); } catch (e) {}
  }
 }

 function schedule() {
  if (queued) return;
  queued = true;
  setTimeout(function () { queued = false; run(); }, 120);
 }

 function observe(fn) {
  if (hooks.indexOf(fn) < 0) hooks.push(fn);
  schedule();
 }

 function isIgnoredMutation15(records) {
  if (!records || !records.length) return true;
  for (var i = 0; i < records.length; i++) {
   var rec = records[i];
   var t = rec.target;
   if (t && t.closest && t.closest('#km-intro-15, #km-sweep-15, .km-toast-15, .km-sheet-15, #kb-whatsnew')) {
    continue;
   }
   var lists = [rec.addedNodes, rec.removedNodes];
   for (var l = 0; l < 2; l++) {
    var nodes = lists[l];
    if (!nodes) continue;
    for (var j = 0; j < nodes.length; j++) {
     var n = nodes[j];
     if (!n || n.nodeType !== 1) continue;
     if (n.id === 'km-intro-15' || n.id === 'km-sweep-15' || n.id === 'kb-whatsnew') continue;
     var cl = n.classList;
     if (cl && (cl.contains('k154-poster-split') || cl.contains('km-toast-15') ||
                cl.contains('km-skel-shelf-152') || cl.contains('km-skel-results-152'))) {
      continue;
     }
     return false;
    }
   }
  }
  return true;
 }

 function boot() {
  if (window.MutationObserver && document.body) {
   new MutationObserver(function (records) {
    if (!isIgnoredMutation15(records)) schedule();
   }).observe(document.body, { childList: true, subtree: true });
  }
  if (typeof window.kuroBack === 'function' && !window.kuroBack.__km15SheetBack) {
   var prevBack = window.kuroBack;
   var wrappedBack = function () {
    if (openSheet) { closeSheet(); return; }
    return prevBack.apply(this, arguments);
   };
   wrappedBack.__km15SheetBack = true;
   window.kuroBack = wrappedBack;
  }
  schedule();
 }

 if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
 else boot();

 return {
  esc: esc, norm: norm, lsGet: lsGet, lsSet: lsSet,
  sheet: sheet, closeSheet: closeSheet, isSheetOpen: function () { return !!openSheet; }, toast: toast,
  observe: observe, schedule: schedule,
  nativeHas: nativeHas, nativeCall: nativeCall
 };
})();
