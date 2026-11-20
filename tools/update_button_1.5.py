#!/usr/bin/env python3
"""1.5-beta: give the web layer a real "check for updates" entry point.

The shell already owns a complete updater - check, download, verify, hand the
verified APK to Android's installer - but the only way in was buried three taps
deep in Settings (Kuro updates).  The new in-app button needs a bridge method:

    KuroNative.updateButton15()

which runs a check immediately and then opens the existing, theme-aware update
screen (KuroUpdateDialog582), so the stage, size, speed and the Install action
are visible while it works.  Auto-download and auto-install keep following the
user's existing Kuro updates settings; Android still asks before installing.

Idempotent: running twice changes nothing.
"""

import sys
from pathlib import Path

BRIDGE_ANCHOR = " @JavascriptInterface public void settingsAction560(String action){"
ACTIVITY_ANCHOR = " private void openUpdateDialog582(){"

BRIDGE = (' @JavascriptInterface public void updateButton15()'
          '{main.post(()->MainActivity.this.updateButton15());}\n')

ACTIVITY = ''' /* 1.5-beta: the in-app Update button. Check now, then open the real update
  * screen so the stage, size, speed and Install action are visible. */
 private void updateButton15(){
  if(isFinishing())return;
  try{KuroUpdateCenter582.checkNow(this,true);}catch(RuntimeException ignored){}
  openUpdateDialog582();
 }
'''


def patch(project: Path) -> int:
    main = project / "src" / "app" / "lovelycrxshx" / "kuro" / "MainActivity.java"
    if not main.is_file():
        print(f"::error::MainActivity.java not found at {main}")
        return 1

    src = main.read_text(encoding="utf-8", errors="surrogateescape")

    if "updateButton15" in src:
        print("already patched: updateButton15 is present")
        return 0

    if BRIDGE_ANCHOR not in src:
        print("::error::could not find the Bridge class anchor (settingsAction560)")
        return 1
    src = src.replace(BRIDGE_ANCHOR, BRIDGE + BRIDGE_ANCHOR, 1)

    if ACTIVITY_ANCHOR not in src:
        print("::error::could not find openUpdateDialog582 to insert beside")
        return 1
    src = src.replace(ACTIVITY_ANCHOR, ACTIVITY + ACTIVITY_ANCHOR, 1)

    main.write_text(src, encoding="utf-8", errors="surrogateescape")
    print("patched: KuroNative.updateButton15() -> check now + update screen")
    return 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("usage: update_button_1.5.py <project dir>")
        sys.exit(2)
    sys.exit(patch(Path(sys.argv[1])))
