/* Kuro 1.5-beta: Continue watching on Home, and quick actions on any poster.
 *
 * The rail is built from the app's own episode history (kuro-episode-history-v1,
 * read through the app's historyEntries()) and resumes through the app's own
 * openHistory(), so it behaves exactly like the Continue row in Account.
 *
 * Playhead minutes come from the shell (KuroNative.progress15), which reads the
 * same checkpoint the native player writes. When that bridge is missing the card
 * simply shows the episode without a time - nothing is guessed.
 *
 * Long-press: one delegated handler on <main> covers every rail, grid, search
 * result and library list, because they all render the app's own .card button.
 * The card's item is resolved from the array the app bound to that shelf
 * (bindCards is wrapped) so "My List" writes exactly what the app writes.
 */
(function () {
 'use strict';

 if (!window.km15) return;
 var K = window.km15;
 var HIDDEN_KEY = 'kuro-hidden-15';
 var RAIL_ID = 'km-continue-15';
 var CARD_LIMIT = 8;

 /* ------------------------------------------------------------ helpers --- */
 function hiddenList() {
  var raw = K.lsGet(HIDDEN_KEY, []);
  return Array.isArray(raw) ? raw : [];
 }

 function isHidden(title) {
  var n = K.norm(title);
  if (!n) return false;
  return hiddenList().indexOf(n) >= 0;
 }

 function setHidden(title, on) {
  var n = K.norm(title);
  if (!n) return false;
  var list = hiddenList().filter(function (x) { return x !== n; });
  if (on) list.push(n);
  K.lsSet(HIDDEN_KEY, list.slice(-200));
  return true;
 }

 function historyRows() {
  try {
   if (typeof window.historyEntries !== 'function') return [];
   var seen = {}, out = [];
   window.historyEntries().forEach(function (row) {
    if (!row || !row.item || !row.item.url || !row.ep) return;
    var key = String(row.item.source || '') + '|' + String(row.item.url);
    if (seen[key]) return;
    seen[key] = 1;
    out.push(row);
   });
   return out;
  } catch (e) { return []; }
 }

 function minutesIn(item, ep) {
  if (!K.nativeHas('progress15')) return 0;
  var ms = Number(K.nativeCall('progress15', [String(item.source || ''), String(item.url || ''), String(ep.number || '')]));
  return isFinite(ms) && ms > 45000 ? Math.round(ms / 60000) : 0;
 }

 /* ------------------------------------------------------------ the rail --- */
 function cardMarkup(row, index) {
  var item = row.item || {}, ep = row.ep || {};
  var mins = minutesIn(item, ep);
  var art = item.img
   ? '<img src="' + K.esc(item.img) + '" alt="" loading="lazy" decoding="async">'
   : '<span class="km-continue-fallback-15" aria-hidden="true">\u2726</span>';
  return '<button type="button" class="km-continue-card-15" data-continue-15="' + index + '"' +
   ' aria-label="Continue ' + K.esc(item.title || 'this title') + ' episode ' + K.esc(String(ep.number || '')) + '">' +
   '<span class="km-continue-art-15">' + art +
   '<span class="km-continue-ep-15">EP ' + K.esc(String(ep.number || '?')) + '</span></span>' +
   '<strong>' + K.esc(item.title || '') + '</strong>' +
   '<small>' + (mins ? mins + ' min in' : 'Continue') + '</small></button>';
 }

 function railMarkup(rows) {
  return '<div class="km-continue-head-15"><div>' +
   '<div class="overline">PICK UP WHERE YOU LEFT OFF</div>' +
   '<h2 id="km-continue-title-15">Continue watching</h2></div>' +
   '<span class="km-continue-tag-15">ON THIS PHONE</span></div>' +
   '<div class="km-continue-row-15">' +
   rows.map(cardMarkup).join('') + '</div>';
 }

 function renderRail(main) {
  var spotlight = main.querySelector('#spotlight');
  var rail = document.getElementById(RAIL_ID);
  if (!spotlight) { if (rail) rail.remove(); return; }

  var rows = historyRows().filter(function (r) { return !isHidden(r.item.title); }).slice(0, CARD_LIMIT);

  /* the app's own "Recently watched" rail is the same list without episodes or
     positions; the new rail replaces it instead of repeating it. With nothing to
     show, leave the app's rail exactly as it was. */
  var old = main.querySelector('.home-rail #recent');
  var oldRail = old && old.closest ? old.closest('.home-rail') : null;
  if (!rows.length) {
   if (rail) rail.remove();
   if (oldRail) oldRail.classList.remove('km-rail-replaced-15');
   return;
  }
  if (oldRail) oldRail.classList.add('km-rail-replaced-15');

  var sig = rows.map(function (r) { return r.item.url + '#' + r.ep.number; }).join(',');
  if (rail && rail.dataset.sig === sig && rail.parentNode) return;

  var section = rail || document.createElement('section');
  section.id = RAIL_ID;
  section.className = 'km-continue-15';
  section.setAttribute('aria-labelledby', 'km-continue-title-15');
  section.dataset.sig = sig;
  section.innerHTML = railMarkup(rows);
  section.__rows = rows;
  section.querySelector('.km-continue-row-15').addEventListener('click', function (ev) {
   var b = ev.target.closest && ev.target.closest('[data-continue-15]');
   if (!b || typeof window.openHistory !== 'function') return;
   var row = section.__rows[Number(b.getAttribute('data-continue-15'))];
   if (row) window.openHistory(row);
  });

  if (!rail) spotlight.insertAdjacentElement('afterend', section);
 }

 /* --------------------------------------------------------- hidden cards --- */
 function applyHidden(main) {
  var list = hiddenList();
  if (!list.length) return;
  Array.prototype.forEach.call(main.querySelectorAll('.card'), function (card) {
   var title = card.querySelector('strong');
   if (!title) return;
   if (list.indexOf(K.norm(title.textContent)) >= 0) card.classList.add('km-card-hidden-15');
  });
 }

 /* ------------------------------------------------------- item resolution --- */
 function resolveItem(card) {
  var idx = card.getAttribute('data-card');
  var node = card.parentNode;
  while (node && node !== document.body) {
   if (node.__kmItems && idx != null && node.__kmItems[Number(idx)]) return node.__kmItems[Number(idx)];
   node = node.parentNode;
  }
  /* bindCards(items, document) sets the pool on the document itself */
  if (document.__kmItems && idx != null && document.__kmItems[Number(idx)]) return document.__kmItems[Number(idx)];

  var title = K.norm(card.querySelector('strong') ? card.querySelector('strong').textContent : '');
  if (!title) return null;
  var pools = [];
  try { pools.push(window.stored('kuro-recent') || []); } catch (e) {}
  try { pools.push((window.historyEntries() || []).map(function (r) { return r.item; })); } catch (e) {}
  try { pools.push(window.stored('kuro-library') || []); } catch (e) {}
  for (var p = 0; p < pools.length; p++) {
   var arr = pools[p];
   for (var i = 0; i < arr.length; i++) {
    var it = arr[i];
    if (it && it.url && K.norm(it.title) === title) return it;
   }
  }
  return null;
 }

 function inMyList(item) {
  try {
   return window.stored('kuro-library').some(function (x) { return x && x.url === item.url; });
  } catch (e) { return false; }
 }

 function toggleMyList(item) {
  try {
   var a = window.stored('kuro-library');
   var i = -1;
   for (var n = 0; n < a.length; n++) { if (a[n] && a[n].url === item.url) { i = n; break; } }
   if (i >= 0) a.splice(i, 1);
   else a.unshift(window.compactItem(item));
   window.save('kuro-library', a);
   return i < 0;
  } catch (e) { return false; }
 }

 /* ------------------------------------------------------------- long press --- */
 function openCardSheet(card) {
  var titleEl = card.querySelector('strong');
  var title = titleEl ? titleEl.textContent.trim() : 'This title';
  var item = resolveItem(card);
  var rows = [];
  var hidden = isHidden(title);

  rows.push({ label: 'Open', sub: 'Details, episodes and servers', tone: 'primary', onPick: function () { card.click(); } });

  if (item) {
   var saved = inMyList(item);
   rows.push({
    label: saved ? 'Remove from My List' : 'Add to My List',
    sub: saved ? 'Removes the bookmark, keeps your history' : 'Same as the bookmark button on the title page',
    onPick: function () {
     var added = toggleMyList(item);
     K.toast(added ? 'Added to My List' : 'Removed from My List');
     K.schedule();
    }
   });
  }

  rows.push({
   label: hidden ? 'Show this title again' : 'Hide this title',
   sub: hidden ? 'Brings it back on Home' : 'Hides it from Home on this phone only',
   onPick: function () {
    setHidden(title, !hidden);
    K.toast(hidden ? 'Shown again' : 'Hidden from Home');
    K.schedule();
   }
  });

  K.sheet(title, 'Quick actions', rows);
 }

 function wireLongPress() {
  var main = document.getElementById('main');
  if (!main || main.dataset.kmPress15) return;
  main.dataset.kmPress15 = '1';

  var timer = 0, start = null, fired = false;

  function clear() {
   clearTimeout(timer);
   timer = 0;
   start = null;
  }

  main.addEventListener('pointerdown', function (ev) {
   if (ev.button != null && ev.button !== 0) return;
   var card = ev.target.closest && ev.target.closest('#main .card, #main .km-continue-card-15');
   if (!card || ev.target.closest('.km-sheet-15')) return;
   fired = false;
   start = { x: ev.clientX, y: ev.clientY, card: card };
   clearTimeout(timer);
   timer = setTimeout(function () {
    if (!start) return;
    fired = true;
    var cardEl = start.card;
    clear();
    openCardSheet(cardEl);
   }, 460);
  }, true);

  main.addEventListener('pointermove', function (ev) {
   if (!start) return;
   if (Math.abs(ev.clientX - start.x) > 12 || Math.abs(ev.clientY - start.y) > 12) clear();
  }, true);

  ['pointerup', 'pointercancel'].forEach(function (type) {
   main.addEventListener(type, function () {
    if (!fired) { clear(); return; }
    /* the finger lifted: let the release click through the guard, but do not
       keep swallowing taps if Android never sends that click at all */
    setTimeout(function () { fired = false; }, 400);
   }, true);
  });
  window.addEventListener('scroll', function () { if (!fired) clear(); }, true);

  /* a long press must not also open the title behind the sheet */
  document.addEventListener('click', function (ev) {
   if (!fired) return;
   if (!ev.target.closest || !ev.target.closest('#main .card, .km-continue-card-15')) return;
   ev.preventDefault();
   ev.stopPropagation();
  }, true);

  /* Android sometimes reports a long press as a context menu; take it too */
  main.addEventListener('contextmenu', function (ev) {
   var card = ev.target.closest && ev.target.closest('.card, .km-continue-card-15');
   if (!card) return;
   ev.preventDefault();
   openCardSheet(card);
  });
 }

 /* ------------------------------------------------------- bindCards hook --- */
 function hookBindCards() {
  if (typeof window.bindCards !== 'function' || window.bindCards.km15) return;
  var original = window.bindCards;
  var wrapped = function (items, root) {
   try {
    if (root && items && items.length) root.__kmItems = items;
   } catch (e) {}
   return original.apply(this, arguments);
  };
  wrapped.km15 = true;
  window.bindCards = wrapped;
 }

 /* ---------------------------------------------------------------- boot --- */
 function render(main) {
  hookBindCards();
  wireLongPress();
  applyHidden(main);
  renderRail(main);
 }

 K.observe(render);
})();
