/* Kuro 1.5 theme accents.
   The shipped palette is one fluorescent purple family baked into hundreds of
   literal hex values; tools/theme_gen.py re-hues that family into one flat
   stylesheet per accent (theme-<accent>-1.1.css).  This module only decides
   which of those stylesheets is on, remembers the choice on the phone, and
   paints the picker at the top of Settings -> Appearance and startup.

   Twenty accents in three groups.  Teal is the default; Violet is the 1.0 look,
   kept so a user can go back without reinstalling; Graphite is the same depth
   with no hue at all.  The colour table lives in tools/theme_picker_gen.py,
   which writes both this list and the preview rules in theme-picker-1.1.css.

   The native shell reads the same choice through KuroNative.themeAccent111, so
   the status bar, dialogs and player panels follow too. */
(function () {
 'use strict';
 var KEY = 'kuro-accent-v1';
 var MIGRATED_154 = 'kuro-default-violet-154';
 var DEFAULT = 'violet';
 var ACCENTS = [
  { id: 'violet',   label: 'Violet',   group: 'Signature', swatch: '#a65be4' },
  { id: 'teal',     label: 'Teal',     group: 'Signature', swatch: '#27d7c5' },
  { id: 'plum',     label: 'Plum',     group: 'Signature', swatch: '#d252e6' },
  { id: 'graphite', label: 'Graphite', group: 'Signature', swatch: '#989898' },
  { id: 'sky',      label: 'Sky',      group: 'Cool',     swatch: '#2e92d9' },
  { id: 'ice',      label: 'Ice',      group: 'Cool',     swatch: '#25b4e0' },
  { id: 'royal',    label: 'Royal',    group: 'Cool',     swatch: '#805be7' },
  { id: 'sapphire', label: 'Sapphire', group: 'Cool',     swatch: '#4f7ce8' },
  { id: 'indigo',   label: 'Indigo',   group: 'Cool',     swatch: '#3f54dd' },
  { id: 'emerald',  label: 'Emerald',  group: 'Cool',     swatch: '#2ed984' },
  { id: 'mint',     label: 'Mint',     group: 'Cool',     swatch: '#32e2ad' },
  { id: 'lime',     label: 'Lime',     group: 'Cool',     swatch: '#8fe444' },
  { id: 'citron',   label: 'Citron',   group: 'Warm',     swatch: '#e2e756' },
  { id: 'gold',     label: 'Gold',     group: 'Warm',     swatch: '#e9c25a' },
  { id: 'amber',    label: 'Amber',    group: 'Warm',     swatch: '#dc9b3b' },
  { id: 'sunset',   label: 'Sunset',   group: 'Warm',     swatch: '#de7548' },
  { id: 'coral',    label: 'Coral',    group: 'Warm',     swatch: '#ed9183' },
  { id: 'rose',     label: 'Rose',     group: 'Warm',     swatch: '#e968a9' },
  { id: 'crimson',  label: 'Crimson',  group: 'Warm',     swatch: '#de4866' },
  { id: 'magenta',  label: 'Magenta',  group: 'Warm',     swatch: '#e050c3' },
 ];
 var IDS = ACCENTS.map(function (a) { return a.id; });
 var GROUPS = ['Signature', 'Cool', 'Warm'];

 function valid(id) { return IDS.indexOf(id) >= 0; }

 function entry(id) {
  for (var i = 0; i < ACCENTS.length; i++) {
   if (ACCENTS[i].id === id) return ACCENTS[i];
  }
  return ACCENTS[0];
 }

 function migrateDefault154() {
  try {
   if (localStorage.getItem(MIGRATED_154) === '1') return;
   localStorage.setItem(MIGRATED_154, '1');
   var raw = localStorage.getItem(KEY);
   if (!raw || raw === 'teal') localStorage.removeItem(KEY);
  } catch (e) {}
 }

 function read() {
  migrateDefault154();
  try {
   var raw = localStorage.getItem(KEY);
   return valid(raw || '') ? raw : DEFAULT;
  } catch (e) { return DEFAULT; }
 }

 function save(id) {
  try { localStorage.setItem(KEY, id); } catch (e) {}
 }

 function link() {
  var el = document.getElementById('kuro-accent-css');
  if (!el) {
   el = document.createElement('link');
   el.id = 'kuro-accent-css';
   el.rel = 'stylesheet';
   (document.head || document.documentElement).appendChild(el);
  }
  return el;
 }

 function apply(id) {
  if (!valid(id)) id = DEFAULT;
  var href = 'theme-' + id + '-1.1.css';
  var el = link();
  if (el.getAttribute('href') !== href) el.setAttribute('href', href);
  document.documentElement.setAttribute('data-accent', id);
  // The native shell reads this to tint its own surfaces.
  try {
   if (window.KuroNative && typeof window.KuroNative.themeAccent111 === 'function') {
    window.KuroNative.themeAccent111(id);
   }
  } catch (e) {}
 }

 function tile(a, current) {
  var on = a.id === current;
  return '<button type="button" class="accent-tile111' + (on ? ' is-on' : '') + '"' +
   ' data-accent="' + a.id + '" data-group="' + a.group + '"' +
   ' aria-pressed="' + (on ? 'true' : 'false') + '"' +
   ' aria-label="' + a.label + ' theme">' +
   '<span class="accent-swatch111" aria-hidden="true"></span>' +
   '<em>' + a.label + '</em>' +
   '<b class="accent-tick111" aria-hidden="true">\u2713</b></button>';
 }

 function groups(current) {
  return GROUPS.map(function (name) {
   var tiles = ACCENTS.filter(function (a) { return a.group === name; })
    .map(function (a) { return tile(a, current); }).join('');
   return '<p class="accent-group-label111">' + name.toUpperCase() + '</p>' +
    '<div class="accent-grid111">' + tiles + '</div>';
  }).join('');
 }

 function preview() {
  return '<div class="accent-preview111">' +
   '<div class="accent-preview-head111">' +
   '<span class="accent-preview-dot111" aria-hidden="true"></span>' +
   '<strong class="accent-preview-name111">Violet</strong>' +
   '<span class="accent-preview-tag111">CURRENT</span>' +
   '</div>' +
   '<div class="accent-preview-body111">' +
   '<div class="accent-preview-card111" aria-hidden="true">' +
   '<span class="pv-line pv-line-wide"></span><span class="pv-line pv-line-short"></span>' +
   '<span class="pv-pill pv-pill-solid">Play</span>' +
   '<span class="pv-pill pv-pill-ghost">+ List</span>' +
   '</div>' +
   '<div class="accent-preview-nav111" aria-hidden="true">' +
   '<span class="pv-nav on"></span><span class="pv-nav"></span>' +
   '<span class="pv-nav"></span><span class="pv-nav"></span>' +
   '</div>' +
   '</div></div>';
 }

 function build(current) {
  var el = document.createElement('div');
  el.className = 'accent-111 accent-settings111';
  el.id = 'accent-111';
  el.setAttribute('role', 'group');
  el.setAttribute('aria-labelledby', 'accent-111-title');
  el.innerHTML = '<div class="accent-head111"><h3 id="accent-111-title">Theme colour</h3>' +
   '<p>Every screen, dialog and player control follows it. Violet is the default. ' +
   'Saved on this phone.</p></div>' + preview() +
   '<div class="accent-groups111">' + groups(current) + '</div>' +
   '<p class="accent-note111">Twenty themes across Signature, Cool and Warm. ' +
   'Graphite keeps the same depth with no colour at all.</p>';
  return el;
 }

 /* The picker lives inside Settings -> "Appearance and startup", as that
  * group's first block.  The overlay is rebuilt each time it opens, so the
  * panel is rebuilt with it; nothing is ever mounted in Account. */
 function settingsRows() {
  var overlay = document.getElementById('settings-overlay560');
  if (!overlay) return null;
  var found = null;
  Array.prototype.forEach.call(overlay.querySelectorAll('.settings-group560'), function (g) {
   if (found) return;
   var key = (g.getAttribute('data-section') || '').toLowerCase();
   if (key.indexOf('appearance') === 0) found = g;
  });
  if (found) return found.querySelector('.settings-rows560') || found;
  /* if the shell renames the group, take the first one rather than lose it */
  return overlay.querySelector('.settings-rows560');
 }

 function panel(current) {
  var rows = settingsRows();
  var host = document.getElementById('accent-111');
  if (!rows) {
   if (host && host.parentNode) host.parentNode.removeChild(host);
   return;
  }
  if (!host) host = build(read());
  if (host.parentNode !== rows) rows.insertBefore(host, rows.firstChild);
  /* the shell's old eight-swatch "Accent color" select is superseded by the
     picker; hide its row so the group has one accent control, not two */
  var old = rows.querySelector('select[data-setting=accent]');
  var oldRow = old && old.closest ? old.closest('.settings-row560') : null;
  if (oldRow) oldRow.classList.add('accent-old-row111');
  var cur = read();
  Array.prototype.forEach.call(host.querySelectorAll('.accent-tile111'), function (b) {
   var on = b.getAttribute('data-accent') === cur;
   b.classList.toggle('is-on', on);
   b.setAttribute('aria-pressed', on ? 'true' : 'false');
  });
  var name = host.querySelector('.accent-preview-name111');
  if (name) name.textContent = entry(cur).label;
  if (!host.dataset.bound) {
   host.dataset.bound = '1';
   host.addEventListener('click', function (ev) {
    var b = ev.target.closest && ev.target.closest('.accent-tile111');
    if (!b) return;
    var id = b.getAttribute('data-accent');
    if (!valid(id)) return;
    save(id);
    apply(id);
    panel(cur);
   });
  }
 }

 function paint() { panel(read()); }

 var queued = false;
 function schedule() {
  if (queued) return;
  if (!document.getElementById('accent-111') && !document.getElementById('settings-overlay560')) return;
  queued = true;
  setTimeout(function () { queued = false; paint(); }, 0);
 }

 function boot() {
  apply(read());
  if (window.MutationObserver && document.body) {
   new MutationObserver(schedule).observe(document.body, { childList: true, subtree: true });
  }
  paint();
 }

 window.kuroAccent111 = { apply: apply, read: read, ids: IDS.slice(), defaultId: DEFAULT };
 if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
 else boot();
})();
