#!/usr/bin/env python3
"""1.5-beta: make the Developer row open the profile, not Discord's home.

The shipped shell opens the developer link with

    Intent(ACTION_VIEW, "https://discord.com/users/825998094067615616")

Android hands that https URL to the Discord app (its app-link filter claims
discord.com), and the Discord client opens on Home and drops the /users/
path - which is exactly what the owner reported: Discord opens, no profile.

Fix: try Discord's own deep link first (discord://-/users/<id>, which the
client routes to the profile sheet), then the two older/alternate shapes,
then an explicit com.discord intent, and only then the browser.  Each step is
resolved with PackageManager before it is launched, so a device without
Discord still ends up on the web profile instead of a dead tap.

The "External links" setting is still honoured: blocked refuses, ask prompts.

Idempotent: running twice changes nothing.
"""

import re
import sys
from pathlib import Path

CASE_RE = re.compile(r'case\s+"developer"\s*:\s*openUserLink560\(\s*"([^"]+)"\s*\)\s*;\s*break\s*;')
ID_RE = re.compile(r'/users/(\d{5,25})')

METHOD = '''
 /* 1.5-beta. The https link used to be handed straight to the Discord app,
  * which opens on Home and drops the /users/ path. Try Discord's own deep
  * links first, then an explicit package intent, then the browser. */
 private void openDiscordProfile569(){
  final String id="%s";
  String mode=settings560().getString("external","allow");
  if("blocked".equals(mode)){Toast.makeText(this,"External links are blocked in Settings.",Toast.LENGTH_SHORT).show();return;}
  Runnable open=()->{
   String[] deep={"discord://-/users/"+id,"discord://discord/users/"+id,"discord://discordapp.com/users/"+id};
   for(String uri:deep){
    try{
     Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(uri));
     i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
     if(getPackageManager().resolveActivity(i,0)!=null){startActivity(i);return;}
    }catch(RuntimeException ignored){}
   }
   try{
    Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("https://discord.com/users/"+id));
    i.setPackage("com.discord");i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if(getPackageManager().resolveActivity(i,0)!=null){startActivity(i);return;}
   }catch(RuntimeException ignored){}
   try{
    startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://discord.com/users/"+id)));
   }catch(RuntimeException e){
    Toast.makeText(this,"No app can open this link.",Toast.LENGTH_SHORT).show();
   }
  };
  if("ask".equals(mode)){
   new AlertDialog.Builder(KuroDialogs581.themed(this))
    .setTitle("Open Discord?")
    .setMessage("This opens the developer's Discord profile.")
    .setNegativeButton("Cancel",null)
    .setPositiveButton("Open",(d,w)->open.run())
    .show();
  }else open.run();
 }
'''.strip('\n')


def patch(project: Path) -> int:
    pkg = project / "src" / "app" / "lovelycrxshx" / "kuro"
    main = pkg / "MainActivity.java"
    if not main.is_file():
        print(f"::error::MainActivity.java not found at {main}")
        return 1

    src = main.read_text(encoding="utf-8", errors="surrogateescape")

    if "openDiscordProfile569" in src:
        print("already patched: openDiscordProfile569 is present")
        return 0

    m = CASE_RE.search(src)
    if not m:
        print("::error::the developer case was not found in MainActivity.java")
        return 1

    url = m.group(1)
    idm = ID_RE.search(url)
    if not idm:
        print(f"::error::could not read a Discord user id out of {url}")
        return 1
    user_id = idm.group(1)

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
    method = METHOD % user_id
    src = src[:at] + method + "\n " + src[at:]

    main.write_text(src, encoding="utf-8", errors="surrogateescape")
    print(f"patched: developer row now targets Discord user {user_id} via deep link")
    return 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("usage: discord_link_1.5.py <project dir>")
        sys.exit(2)
    sys.exit(patch(Path(sys.argv[1])))
