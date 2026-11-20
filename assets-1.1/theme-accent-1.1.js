/* Kuro 1.1 theme accents.
   The shipped palette is one fluorescent purple family baked into hundreds of
   literal hex values; tools/theme_gen.py re-hues that family into one flat
   stylesheet per accent (theme-<accent>-1.1.css).  This module only decides
   which of those stylesheets is on, remembers the choice on the phone, and
   paints the picker in Account.

   Default is Teal.  Violet is the 1.0 look, kept so a user can go back
   without reinstalling. */
(function () {
 'use strict';
 var KEY = 'kuro-accent-v1';
 var DEFAULT = 'teal';
 var ACCENTS = [
  { id: 'teal',    label: 'Teal',    swatch: '#2bd8c6' },
  { id: 'crimson', label: 'Crimson', swatch: '#de4e6b' },
  { id: 'indigo',  label: 'Indigo',  swatch: '#4559dc' },
  { id: 'amber',   label: 'Amber',   swatch: '#dc9e41' },
  { id: 'violet',  label: 'Violet',  swatch: '#a662e4' }
 ];
 var IDS = ACCENTS.map(function (a) { return a.id; });

 function valid(id) { return IDS.indexOf(id) >= 0; }

 function read() {
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

 function swatches(current) {
  return ACCENTS.map(function (a) {
   var on = a.id === current;
   return '<button type="button" class="accent-tile111' + (on ? ' is-on' : '') + '"' +
    ' data-accent="' + a.id + '" aria-pressed="' + (on ? 'true' : 'false') + '"' +
    ' aria-label="' + a.label + ' theme"><span data-swatch="' + a.id + '"></span>' +
    '<em>' + a.label + '</em></button>';
  }).join('');
 }

 function build(current) {
  var el = document.createElement('div');
  el.className = 'account-section accent-111';
  el.id = 'accent-111';
  el.innerHTML = '<div class="account-section-head"><div><h2>Theme colour</h2>' +
   '<p>Teal is the default. Every screen, dialog and player control follows it. Saved on this phone.</p>' +
   '</div></div><div class="accent-grid111">' + swatches(current) + '</div>' +
   '<p class="accent-note111">Violet is the 1.0 look, kept in case you want it back.</p>';
  return el;
 }

 function panel(current) {
  var page = document.querySelector('.profile-page547');
  if (!page) return;
  var host = page.querySelector('#accent-111');
  if (!host) {
   host = build(current);
   var art = page.querySelector('#profile-art581');
   if (art && art.parentNode === page) page.insertBefore(host, art);
   else {
    var stats = page.querySelector('.profile-stats571'), anchor = stats && stats.nextElementSibling;
    if (anchor && anchor.parentNode === page) page.insertBefore(host, anchor); else page.appendChild(host);
   }
  }
  var cur = read();
  Array.prototype.forEach.call(host.querySelectorAll('.accent-tile111'), function (b) {
   var on = b.getAttribute('data-accent') === cur;
   b.classList.toggle('is-on', on);
   b.setAttribute('aria-pressed', on ? 'true' : 'false');
  });
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
