#!/usr/bin/env python3
"""1.5.4: open the developer's verified Discord profile and update the Settings section title.

Fixes two issues in the 1.0/1.5-beta developer link:
  1. The 1.0 source tree had a typo in the Discord snowflake
     (825998094067615616 instead of 825998094067695616).
  2. Discord's Android app does not route desktop `discord://-/users/<id>`
     protocol URLs; it routes `https://discord.com/users/<id>` App Links.

`openDiscordProfile569()` now targets https://discord.com/users/825998094067695616
via an explicit `com.discord` BROWSABLE VIEW intent first (so Android opens the
profile directly inside Discord when installed), then a standard BROWSABLE VIEW
intent for browsers / alternate clients, and `discord://-/users/<id>` only as a
last-resort fallback.

Also updates the Settings section heading in `assets/settings560.js` from
`Developer` to `Contact developer & notice for bugs and error`.

Idempotent: running twice changes nothing.
"""

import re
import sys
from pathlib import Path

DISCORD_USER_ID = "825998094067695616"
DISCORD_PROFILE_URL = f"https://discord.com/users/{DISCORD_USER_ID}"

OLD_SECTION_HEAD = "group('Developer','About this app',"
NEW_SECTION_HEAD = "group('Contact developer & notice for bugs and error','About this app',"

CASE_RE = re.compile(
    r'case\s+"developer"\s*:\s*openUserLink560\(\s*"([^"]+)"\s*\)\s*;\s*break\s*;'
)
METHOD_RE = re.compile(
    r'/\*[^*]*\*(?:[^/*][^*]*\*+)*/\s*private void openDiscordProfile569\(\)\{.*?\n \}',
    re.S,
)

METHOD = f'''
 /* 1.5.4. Open the developer's verified Discord profile link
  * ({DISCORD_PROFILE_URL}) in the installed Discord Android app first, then
  * fall back to the browser. */
 private void openDiscordProfile569(){{
  final String url="{DISCORD_PROFILE_URL}";
  final String id="{DISCORD_USER_ID}";
  String mode=settings560().getString("external","allow");
  if("blocked".equals(mode)){{Toast.makeText(this,"External links are blocked in Settings.",Toast.LENGTH_SHORT).show();return;}}
  Runnable open=()->{{
   try{{
    Intent app=new Intent(Intent.ACTION_VIEW,Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE).setPackage("com.discord");
    if(getPackageManager().resolveActivity(app,0)!=null){{startActivity(app);return;}}
   }}catch(RuntimeException ignored){{}}
   try{{
    startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE));
    return;
   }}catch(RuntimeException ignored){{}}
   try{{
    Intent deep=new Intent(Intent.ACTION_VIEW,Uri.parse("discord://-/users/"+id));
    if(getPackageManager().resolveActivity(deep,0)!=null){{startActivity(deep);return;}}
   }}catch(RuntimeException ignored){{}}
   try{{
    startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));
   }}catch(RuntimeException e){{
    Toast.makeText(this,"No app can open this link.",Toast.LENGTH_SHORT).show();
   }}
  }};
  if("ask".equals(mode)){{
   new AlertDialog.Builder(KuroDialogs581.themed(this))
    .setTitle("Open Discord?")
    .setMessage("This opens the developer's Discord profile.")
    .setNegativeButton("Cancel",null)
    .setPositiveButton("Open",(d,w)->open.run())
    .show();
  }}else open.run();
 }}
'''.strip('\n')


def patch_main(project: Path) -> int:
    pkg = project / "src" / "app" / "lovelycrxshx" / "kuro"
    main = pkg / "MainActivity.java"
    if not main.is_file():
        print(f"::error::MainActivity.java not found at {main}")
        return 1

    src = main.read_text(encoding="utf-8", errors="surrogateescape")

    if "openDiscordProfile569" in src:
        if DISCORD_PROFILE_URL in src and "825998094067615616" not in src:
            print("already patched: openDiscordProfile569 targets verified Discord user")
            return 0
        src, count = METHOD_RE.subn(METHOD, src, count=1)
        if count != 1:
            print("::error::could not replace existing openDiscordProfile569 in MainActivity.java")
            return 1
        main.write_text(src, encoding="utf-8", errors="surrogateescape")
        print(f"updated existing openDiscordProfile569 -> {DISCORD_PROFILE_URL}")
        return 0

    m = CASE_RE.search(src)
    if not m:
        print("::error::the developer case was not found in MainActivity.java")
        return 1

    src = src[:m.start()] + 'case "developer":openDiscordProfile569();break;' + src[m.end():]

    at = -1
    for anchor in ("private String diagnostics560()", "private void settingsAction560(",
                   "private void openUserLink560("):
        at = src.find(anchor)
        if at >= 0:
            break
    if at < 0:
        print("::error::could not find an insertion point in MainActivity.java")
        return 1
    src = src[:at] + METHOD + "\n " + src[at:]

    main.write_text(src, encoding="utf-8", errors="surrogateescape")
    print(f"patched: developer row now targets {DISCORD_PROFILE_URL}")
    return 0


def patch_settings_js(project: Path) -> int:
    settings_js = project / "assets" / "settings560.js"
    if not settings_js.is_file():
        print(f"::error::settings560.js not found at {settings_js}")
        return 1

    js = settings_js.read_text(encoding="utf-8")
    if NEW_SECTION_HEAD in js:
        print("already patched: settings560.js developer section title is updated")
        return 0
    if OLD_SECTION_HEAD not in js:
        print("::error::expected Developer group heading not found in settings560.js")
        return 1

    js = js.replace(OLD_SECTION_HEAD, NEW_SECTION_HEAD, 1)
    settings_js.write_text(js, encoding="utf-8")
    print("patched: settings560.js section title -> 'Contact developer & notice for bugs and error'")
    return 0


def patch(project: Path) -> int:
    rc = patch_main(project)
    if rc != 0:
        return rc
    return patch_settings_js(project)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("usage: discord_link_1.5.py <project dir>")
        sys.exit(2)
    sys.exit(patch(Path(sys.argv[1])))
