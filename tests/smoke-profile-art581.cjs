/* Smoke test: the profile art picker really renders and persists.
   Run: node smoke-profile-art.cjs  (needs jsdom in this folder) */
const fs = require('fs');
const path = require('path');
const { JSDOM } = require('jsdom');

const SRC = path.resolve(__dirname, '../../work/source/Kuro-tv-5.81-Source/assets/profile-art-arm.js');
const js = fs.readFileSync(SRC, 'utf8');

const page = () => `
<main id="main">
  <div class="profile-page547">
    <div class="profile-hero547">
      <div class="profile-banner"><div class="profile-banner-shade"></div></div>
      <div class="profile-summary">
        <div class="profile-avatar"><span class="profile-mono571">AB</span></div>
        <div class="profile-title"><strong>Test</strong></div>
      </div>
    </div>
    <div class="profile-stats571"></div>
    <div class="account-section profile-edit547"></div>
  </div>
</main>`;

const problems = [];
const check = (ok, msg) => { if (!ok) problems.push(msg); };

(async () => {
  const dom = new JSDOM(`<!doctype html><html><body>${page()}</body></html>`, {
    url: 'https://kuro.local/index.html',
    runScripts: 'outside-only',
    pretendToBeVisual: true,
  });
  const { window } = dom;
  const doc = window.document;

  // CSP-safe: the module only touches the DOM and localStorage.
  window.eval(js);
  await new Promise(r => setTimeout(r, 30));

  const panel = doc.querySelector('#profile-art581');
  check(!!panel, 'the picker section was not injected into the profile page');
  if (panel) {
    const pfpTiles = panel.querySelectorAll('.art-grid-pfp581 .art-tile581');
    const bnTiles = panel.querySelectorAll('.art-grid-banner581 .art-tile581');
    check(pfpTiles.length === 6, `expected 6 avatar tiles (initials + 5), got ${pfpTiles.length}`);
    check(bnTiles.length === 6, `expected 6 banner tiles (none + 5), got ${bnTiles.length}`);
    const imgs = panel.querySelectorAll('img');
    check(imgs.length === 10, `expected 10 thumbnails, got ${imgs.length}`);
    const firstImg = imgs[0];
    check(!!firstImg && firstImg.getAttribute('src') === 'profile-art/pfp/art-01.webp',
      `first avatar thumbnail points at ${firstImg && firstImg.getAttribute('src')}`);
    check(!!firstImg && firstImg.getAttribute('loading') === 'lazy', 'thumbnails should lazy-load');
    check(panel.querySelector('.art-grid-pfp581 .art-none581.is-on') !== null,
      'with no art chosen, the Initials tile should be the selected one');
  }

  // pick the third avatar + second banner
  const pfp3 = doc.querySelector('.art-grid-pfp581 .art-tile581[data-id="art-03"]');
  check(!!pfp3, 'avatar 3 tile is missing');
  pfp3.dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  await new Promise(r => setTimeout(r, 20));

  const avatar = doc.querySelector('.profile-avatar');
  const shown = avatar && avatar.querySelector('img.profile-art-pfp581');
  check(!!shown, 'choosing an avatar did not paint an <img> into the profile avatar');
  check(!!shown && shown.getAttribute('src') === 'profile-art/pfp/art-03.webp',
    `avatar img src is ${shown && shown.getAttribute('src')}`);
  check(!!avatar && avatar.classList.contains('has-art581'), 'the avatar did not get the has-art class');
  check(!!avatar && !!avatar.querySelector('.profile-mono571'), 'the initials fallback should stay in the DOM');
  check(doc.querySelector('.art-grid-pfp581 .art-tile581[data-id="art-03"]').classList.contains('is-on'),
    'the chosen avatar tile is not marked selected');
  check(doc.querySelector('.art-grid-pfp581 .art-none581').classList.contains('is-on') === false,
    'the Initials tile stayed selected after picking art');

  const bn2 = doc.querySelector('.art-grid-banner581 .art-tile581[data-id="art-02"]');
  bn2.dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  await new Promise(r => setTimeout(r, 20));
  const banner = doc.querySelector('.profile-banner');
  check(!!banner && banner.style.backgroundImage.includes('profile-art/banner/art-02.webp'),
    `banner background is "${banner && banner.style.backgroundImage}"`);
  check(!!banner && banner.classList.contains('has-art581'), 'the banner did not get the has-art class');

  const stored = JSON.parse(window.localStorage.getItem('kuro-profile-art-v1') || '{}');
  check(stored.pfp === 'art-03' && stored.banner === 'art-02', `stored choice is ${JSON.stringify(stored)}`);

  // the account page re-renders (edit toggle); the picker must come back with the choice intact
  doc.querySelector('#main').innerHTML = page();
  await new Promise(r => setTimeout(r, 40));
  const again = doc.querySelector('#profile-art581');
  check(!!again, 'the picker did not come back after the profile page re-rendered');
  const againImg = doc.querySelector('.profile-avatar img.profile-art-pfp581');
  check(!!againImg && againImg.getAttribute('src') === 'profile-art/pfp/art-03.webp',
    'the avatar was lost when the page re-rendered');
  check(!!doc.querySelector('.art-grid-banner581 .art-tile581[data-id="art-02"].is-on'),
    'the banner selection was lost when the page re-rendered');

  // clearing goes back to initials
  doc.querySelector('.art-grid-pfp581 .art-none581').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  await new Promise(r => setTimeout(r, 20));
  check(!doc.querySelector('.profile-avatar img.profile-art-pfp581'), 'clearing the avatar left the image behind');
  check(!doc.querySelector('.profile-avatar').classList.contains('has-art581'), 'clearing left the has-art class');
  check(JSON.parse(window.localStorage.getItem('kuro-profile-art-v1') || '{}').pfp === undefined,
    'clearing the avatar did not delete the stored choice');

  // the 5 pairs must exist on disk and be real webp files
  const root = path.resolve(__dirname, '../../work/source/Kuro-tv-5.81-Source/assets/profile-art');
  for (const kind of ['pfp', 'banner']) {
    for (let i = 1; i <= 5; i++) {
      const f = path.join(root, kind, `art-0${i}.webp`);
      const ok = fs.existsSync(f) && fs.readFileSync(f).slice(0, 4).toString('ascii') === 'RIFF';
      check(ok, `missing or invalid webp: profile-art/${kind}/art-0${i}.webp`);
    }
  }

  console.log(problems.length ? `PROFILE ART PROBLEMS:\n- ${problems.join('\n- ')}` : 'profile art: problems none');
  console.log(`picker sections: ${doc.querySelectorAll('#profile-art581').length}`);
  process.exit(problems.length ? 1 : 0);
})().catch(e => { console.error('BOOT ERROR', e); process.exit(1); });
