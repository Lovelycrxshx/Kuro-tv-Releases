/* Kuro 1.5-beta: "What's new", shown once per version and reachable later.
 *
 * The list below is this build's own change log - it ships inside the APK, so it
 * always describes the version you are actually running. Bump STAMP together
 * with the version, or the card will not come back.
 *
 * Where it appears: one card at the top of Home, once, and a row in
 * Settings -> Developer that reopens it in a sheet at any time.
 */
(function () {
 'use strict';

 if (!window.km15) return;
 var K = window.km15;

 var STAMP = '1.5-beta.11';
 var SEEN_KEY = 'kuro-whatsnew-15';
 var CARD_ID = 'km-whatsnew-15';

 var ITEMS = [
  'Continue watching now sits at the top of Home, with the episode number and how far in you are.',
  'Hold any poster for quick actions: open it, add it to My List, or hide it from Home.',
  'Downloads can clean themselves up: delete watched episodes, keep one per series, or clear them all.',
  'Theme colour lives in Settings > Appearance and startup, with twenty accents.',
  'Discord Rich Presence now includes a Download button linking to Kuro\u2019s download page, with a Show / Hide option in Presence customization.',
  'The update arrow in the header is now the only update button - the Account card is gone.',
  'The "Private profile / LOCAL" card is gone from Account.'
 ];

 function seen() {
  try { return localStorage.getItem(SEEN_KEY) === STAMP; } catch (e) { return false; }
 }

 function markSeen() {
  try { localStorage.setItem(SEEN_KEY, STAMP); } catch (e) {}
 }

 function openSheet() {
  K.sheet("What's new", 'Kuro 1.5 beta \u00b7 this build', ITEMS.map(function (text) {
   return { label: text };
  }));
 }

 function card(main) {
  var existing = document.getElementById(CARD_ID);
  if (seen()) { if (existing) existing.remove(); return; }
  var spotlight = main.querySelector('#spotlight');
  if (!spotlight) { if (existing) existing.remove(); return; }
  if (existing) return;

  var el = document.createElement('section');
  el.id = CARD_ID;
  el.className = 'km-whatsnew-15';
  el.setAttribute('aria-labelledby', 'km-whatsnew-title-15');
  el.innerHTML =
   '<div class="overline">NEW IN THIS BETA</div>' +
   '<h2 id="km-whatsnew-title-15">What\u2019s new</h2>' +
   '<ul class="km-whatsnew-list-15">' + ITEMS.slice(0, 4).map(function (t) {
    return '<li>' + K.esc(t) + '</li>';
   }).join('') + '</ul>' +
   '<div class="km-whatsnew-row-15">' +
   '<button type="button" class="km-whatsnew-go-15">Got it</button>' +
   '<button type="button" class="km-whatsnew-more-15">All changes</button>' +
   '</div>';

  el.querySelector('.km-whatsnew-go-15').addEventListener('click', function () {
   markSeen();
   el.classList.add('km-whatsnew-gone-15');
   setTimeout(function () { if (el.parentNode) el.parentNode.removeChild(el); }, 220);
  });
  el.querySelector('.km-whatsnew-more-15').addEventListener('click', openSheet);

  main.insertBefore(el, main.firstElementChild);
 }

 function settingsRow(main) {
  var overlay = document.getElementById('settings-overlay560');
  if (!overlay) return;
  var group = null;
  Array.prototype.forEach.call(overlay.querySelectorAll('.settings-group560'), function (g) {
   var key = (g.getAttribute('data-section') || '').toLowerCase();
   if (!group && key.indexOf('developer') === 0) group = g;
  });
  if (!group) return;
  var rows = group.querySelector('.settings-rows560');
  if (!rows || rows.querySelector('.km-wn-row-15')) return;

  var row = document.createElement('button');
  row.type = 'button';
  row.className = 'settings-row560 settings-action560 km-wn-row-15';
  row.innerHTML = '<span><strong>What\u2019s new</strong><small>Changes in this beta build</small></span>' +
   '<b aria-hidden="true">\u203a</b>';
  row.addEventListener('click', openSheet);
  rows.insertBefore(row, rows.firstChild);
 }

 function render(main) {
  card(main);
  settingsRow(main);
 }

 /* the sheet is built by km15, so it survives settings being rebuilt */
 K.observe(render);
})();
