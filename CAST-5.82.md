# Cast in 5.82 — what changed and what it costs

## What 5.81 actually had

`KuroCastActivity561` was a 73-line screen: a device picker, three buttons (Play / Pause /
Stop) and two hardcoded colours (`0xff12091c`, `0xff44265b`) that match nothing else in the
app. It sent the stream URL with the current position, then reported success by setting
`RESULT_OK`. Nothing came back.

Four consequences, all real:

1. **No media session.** Nothing told Android what was playing, so lock-screen controls,
   Bluetooth/headset media keys and Google TV's "Now playing" had no session to bind to.
   Cast could only be driven from inside that one screen.
2. **No position on the way back.** Leave the cast screen and the phone still believed it
   was at the old spot. Resume after a TV session restarted you minutes behind, and there
   was no way to see where the TV actually was.
3. **No seek.** Three transport buttons and no timeline.
4. **No state.** "Buffering", "TV disconnected", "receiver refused the stream" and
   "finished" all rendered as the same grey sentence.

## What 5.82 does

| Area | 5.81 | 5.82 |
|---|---|---|
| Panel | two hardcoded colours | app palette (`KuroDialogs581`): panel, card, violet accent, ink/muted, focus rings |
| Timeline | none | live position + duration, scrub sends `RemoteMediaClient.seek` |
| Play/Pause label | static | follows the TV's real state (`isPlaying`) |
| System session | none | `android.media.session.MediaSession` + `MediaStyle` notification; media keys, lock screen and Google TV control the TV |
| Device name | none | `getCastDevice().getFriendlyName()` shown as "Casting to …" |
| Leaving | `RESULT_OK`, nothing else | returns `castPosition`, `castStopped`, `castActive` |
| Phone resumes | at the old position | at the TV's position (`KuroNativePlayer561.seekTo582`, or a `<video>` nudge for the WebView player) |
| Exit choices | back | "Keep playing on TV" (default, back does this) vs "Resume here" (stops the TV and hands the position back) |

The stream gate is unchanged: only the direct HLS handoff is castable, the receiver gets the
URL and nothing else, and the existing consent prompt still explains that.

## Honest limits

- **`Resume here` stops the TV.** The Default Media Receiver cannot hand a position back
  over the wire; the position is read with `getApproximateStreamPosition()` at the moment
  you leave, which is accurate to a few seconds of buffered drift.
- **The MediaSession lives while the Cast screen is open.** Keeping controls alive after
  you leave needs a foreground `MediaSessionService`; that is the rest of item #16, and it
  should land together with the play queue (#13), which Cast also wants.
- **No queue yet.** One item at a time, exactly as before — the receiver supports queues,
  but Kuro has no queue model to hand it.
- **Not device-tested.** Everything here compiles and follows the documented Cast SDK and
  MediaSession contracts, but casting needs a physical receiver on the same network. The
  first real test should be: cast, let it play two minutes, back out, reopen Kuro, and
  confirm it offers the TV's position rather than the old one.
