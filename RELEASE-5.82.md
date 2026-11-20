# Kuro 5.82

**Cut 2 · versionCode 85 · 2026-11-19**

Kuro now updates itself. Until this build the app could check for a release and download
it, but nothing happened until you found the notification and tapped it. This cut closes
that gap: a newer release is found, downloaded, verified and handed to Android's installer
— with the whole thing visible in a download screen built from Kuro's own theme.

## What's new

### Updates install themselves

- **No reminder cadence to sit through.** A release found by the daily check goes straight
  to a download. The old three-notice sequence is gone from the automatic path, and there
  is no "remind me later" to dismiss.
- **The installer opens on its own.** When the file passes verification, Kuro opens
  Android's installer. Android shows its own confirmation — every Android app is required
  to — and one tap finishes the update. Install it over the top; your profiles, watch list,
  history, gallery, downloads and logins stay.
- **Once per version.** A cancelled install is not re-offered. Kuro remembers that it
  already handed you this version, so nothing loops.
- **You choose how far it goes.** Settings → Kuro updates → *Automatic updates*:
  - **Check, download and install** — the default, fully automatic;
  - **Check and download only** — the file is prepared, you tap Install;
  - **Off, remind me first** — the 5.72 behaviour, exactly as it was.
- **The network setting still rules.** Wi-Fi-only update downloads are respected; a queued
  download waits for Wi-Fi and continues by itself when it appears.

### The download screen

The download is no longer only a notification. There is now an in-app update screen on
Kuro's own palette — deep violet panel, rounded corners, the violet accent strip, the same
focus rings as the rest of the settings — showing:

- the release it is fetching and a state chip: **CHECKING**, **DOWNLOADING**, **VERIFYING**,
  **READY**, **WAITING**, **FAILED**;
- a large percentage and a progress bar that is indeterminate while the total is unknown
  and never claims more than 100%;
- what has actually arrived — *"18.4 MiB of 41.97 MiB · 2.1 MiB/s · 14 s left"* — computed
  from bytes written, not from a timer;
- **Stop** (keeps the partial file so the next attempt resumes), **Hide** (the download
  carries on in the foreground service and the notification), **Retry** on failure, and
  **Install now** when the file is verified.

It opens itself when a download starts or resumes, so the transfer is never invisible, and
it stays closed once you close it — a dismissed screen does not nag its way back.

### Everything above the surface is unchanged

The release is only accepted if the package name matches, the signer set equals the
installed one, that set contains the pinned Kuro certificate
(`1D:37:C3:CA:…:03:E9:3E`, CN=Kuro Anime), and the `versionCode` is strictly newer. The
published SHA-256 is checked over the finished file; a truncated or altered download is
deleted rather than installed. Nothing installs silently, and Kuro still cannot do anything
Android's installer does not show you.

## Also in this cut

Cut 1 of 5.82 was the version-code rollover: it moved `versionCode` from 84 to 85 with the
exact 5.81 rev 11.5 code, which is why the updater can finally see these builds at all. This
cut keeps 85, so **the in-app updater will offer this build to anyone on 5.81 or older**.

Still in, unchanged from 5.81 rev 11.5:

- **Skip suggestions on every episode** — a source marker wins, a marker you set wins, and a
  suggestion only fills the gap, labelled as a suggestion, never auto-skipped.
- **Updater v2** — pinned release signer and resumable downloads with a verified SHA-256.

## Unchanged

Playback engine, catalogue parsing, mirrors, Discord presence, offline downloads, PiP,
profiles, the gallery, search filters, the hero deck, casting and every theme behave exactly
as they did in 5.81.

## Install

Install over the top. **No uninstall.** Same package `app.lovelycrxshx.kuro`, same signing
key. On a TV or Fire TV stick, sideload it once by hand — after this build, updates can
arrive on their own.

## Verify

```
sha256sum Kuro-tv-5.82.apk
# c4ad59211b1f931c4696c90310daffda9e092177f595aadb6b9190bdb58b4566

apksigner verify --print-certs Kuro-tv-5.82.apk
# CN=Kuro Anime
# SHA-256 1D:37:C3:CA:28:ED:80:CC:48:46:01:A7:A7:90:7D:5C:43:97:0A:5D:57:D5:1A:A4:E9:14:75:A6:07:03:E9:3E
```

## Notes

- APK 44,008,672 bytes · source archive 7,246,876 bytes.
- `versionCode` stays 85 for the rest of the 5.82 line; 5.83 opens the next code.
- Android decides when background work runs, so a check or a download can still be delayed
  by the system. Opening Kuro runs a check on resume, which is the fastest way to see one.
