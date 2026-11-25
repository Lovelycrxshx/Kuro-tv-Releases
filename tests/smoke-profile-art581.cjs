/* Smoke test: the profile art picker renders, persists, and follows the avatar into
   the header chip and the account drawer.
   Run: node tests/smoke-profile-art581.cjs   (needs jsdom resolvable) */
const fs = require('fs');
const path = require('path');
const { JSDOM } = require('jsdom');

const ROOT = path.resolve(__dirname, '..');
const CANDIDATES = [
  process.env.PROFILE_ART_SRC,
  path.join(ROOT, 'work/source/Kuro-tv-5.81-Source/assets/profile-art-arm.js'),
].filter(Boolean);
const SRC = CANDIDATES.find(p => fs.existsSync(p));
if (!SRC) {
  console.error('profile art module not found; set PROFILE_ART_SRC to assets/profile-art-arm.js');
  process.exit(2);
}
const js = fs.readFileSync(SRC, 'utf8');
const ASSET_ROOT = path.join(path.dirname(SRC), 'profile-art');

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
const wait = ms => new Promise(r => setTimeout(r, ms));

(async () => {
  const dom = new JSDOM(`<!doctype html><html><body>
<button type="button" id="header-login" class="login-button is-profile560"><span class="profile-mono571 header-mono571">AB</span></button>
${page()}</body></html>`, {
    url: 'https://kuro.local/index.html',
    runScripts: 'outside-only',
    pretendToBeVisual: true,
  });
  const { window } = dom;
  const doc = window.document;

  window.eval(js);
  await wait(30);

  const panel = doc.querySelector('#profile-art581');
  check(!!panel, 'the picker section was not injected into the profile page');
  if (panel) {
    const pfpTiles = panel.querySelectorAll('.art-grid-pfp581 .art-tile581');
    const bnTiles = panel.querySelectorAll('.art-grid-banner581 .art-tile581');
    check(pfpTiles.length === 6, `expected 6 avatar tiles (initials + 5), got ${pfpTiles.length}`);
    check(bnTiles.length === 6, `expected 6 banner tiles (none + 5), got ${bnTiles.length}`);
    const imgs = panel.querySelectorAll('img');
    check(imgs.length === 10, `expected 10 thumbnails, got ${imgs.length}`);
    check(!!imgs[0] && imgs[0].getAttribute('src') === 'profile-art/pfp/art-01.webp',
      `first avatar thumbnail points at ${imgs[0] && imgs[0].getAttribute('src')}`);
    check(!!imgs[0] && imgs[0].getAttribute('loading') === 'lazy', 'thumbnails should lazy-load');
    check(panel.querySelector('.art-grid-pfp581 .art-none581.is-on') !== null,
      'with no art chosen, the Initials tile should be the selected one');
  }

  // pick the third avatar + second banner
  doc.querySelector('.art-grid-pfp581 .art-tile581[data-id="art-03"]')
    .dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  await wait(30);

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

  // the header chip wears the same avatar
  const head = doc.querySelector('#header-login .header-mono571');
  check(!!head && head.classList.contains('has-art581'), 'the header badge did not take the avatar');
  check(!!head && head.style.backgroundImage.includes('profile-art/pfp/art-03.webp'),
    `header badge background is "${head && head.style.backgroundImage}"`);

  // the account drawer is built later by accounts542.js, so it starts empty
  const drawer = doc.createElement('section');
  drawer.className = 'profile-drawer560';
  drawer.innerHTML = '<div class="profile-identity560"><span class="profile-mono571">AB</span><div><strong>Test</strong></div></div>';
  doc.body.appendChild(drawer);
  await wait(40);
  const identity = doc.querySelector('.profile-drawer560 .profile-identity560 .profile-mono571');
  check(!!identity && identity.classList.contains('has-art581'), 'the drawer avatar did not take the art');
  check(!!identity && identity.style.backgroundImage.includes('profile-art/pfp/art-03.webp'),
    `drawer avatar background is "${identity && identity.style.backgroundImage}"`);

  // refreshHeader560 rebuilds the badge from scratch - the art must come back
  doc.querySelector('#header-login').innerHTML = '<span class="profile-mono571 header-mono571">AB</span>';
  await wait(40);
  const rebuilt = doc.querySelector('#header-login .header-mono571');
  check(!!rebuilt && rebuilt.classList.contains('has-art581'), 'a rebuilt header badge lost the avatar');

  // banner
  doc.querySelector('.art-grid-banner581 .art-tile581[data-id="art-02"]')
    .dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  await wait(30);
  const banner = doc.querySelector('.profile-banner');
  check(!!banner && banner.style.backgroundImage.includes('profile-art/banner/art-02.webp'),
    `banner background is "${banner && banner.style.backgroundImage}"`);
  check(!!banner && banner.classList.contains('has-art581'), 'the banner did not get the has-art class');

  const stored = JSON.parse(window.localStorage.getItem('kuro-profile-art-v1') || '{}');
  check(stored.pfp === 'art-03' && stored.banner === 'art-02', `stored choice is ${JSON.stringify(stored)}`);

  // the account page re-renders (edit toggle); everything must come back
  doc.querySelector('#main').innerHTML = page();
  await wait(50);
  check(!!doc.querySelector('#profile-art581'), 'the picker did not come back after the profile page re-rendered');
  const again = doc.querySelector('.profile-avatar img.profile-art-pfp581');
  check(!!again && again.getAttribute('src') === 'profile-art/pfp/art-03.webp',
    'the avatar was lost when the page re-rendered');
  check(!!doc.querySelector('.art-grid-banner581 .art-tile581[data-id="art-02"].is-on'),
    'the banner selection was lost when the page re-rendered');
  check(!!doc.querySelector('#header-login .header-mono571.has-art581'),
    'the header avatar was lost when the profile page re-rendered');

  // clearing goes back to initials everywhere
  doc.querySelector('.art-grid-pfp581 .art-none581').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  await wait(40);
  check(!doc.querySelector('.profile-avatar img.profile-art-pfp581'), 'clearing the avatar left the profile image behind');
  check(!doc.querySelector('.profile-avatar').classList.contains('has-art581'), 'clearing left the has-art class');
  check(!doc.querySelector('#header-login .header-mono571').classList.contains('has-art581'),
    'clearing the avatar left the header badge wearing art');
  check(!doc.querySelector('.profile-drawer560 .profile-mono571').classList.contains('has-art581'),
    'clearing the avatar left the drawer wearing art');
  check(JSON.parse(window.localStorage.getItem('kuro-profile-art-v1') || '{}').pfp === undefined,
    'clearing the avatar did not delete the stored choice');

  // the pack on disk must be real webp files
  for (const kind of ['pfp', 'banner']) {
    for (let i = 1; i <= 5; i++) {
      const f = path.join(ASSET_ROOT, kind, `art-0${i}.webp`);
      const ok = fs.existsSync(f) && fs.readFileSync(f).slice(0, 4).toString('ascii') === 'RIFF';
      check(ok, `missing or invalid webp: profile-art/${kind}/art-0${i}.webp`);
    }
  }

  console.log(problems.length ? `PROFILE ART PROBLEMS:\n- ${problems.join('\n- ')}` : 'profile art: problems none');
  console.log(`header chips painted: ${doc.querySelectorAll('#header-login .header-mono571.has-art581').length}`);
  console.log(`picker sections: ${doc.querySelectorAll('#profile-art581').length}`);
  process.exit(problems.length ? 1 : 0);
})().catch(e => { console.error('BOOT ERROR', e); process.exit(1); });
