#!/usr/bin/env python3
"""Add a Download button to Kuro's Discord Rich Presence activity.

Publishes a single ActivityButton ("Download" -> direct GitHub Releases APK URL
https://github.com/Lovelycrxshx/Kuro-tv-Releases/releases/download/v<ver>/Kuro-tv-<ver>.apk)
enabled by default with a Show / Hide toggle in Settings -> Discord Rich Presence ->
Presence customization, while keeping source/episode watch URLs strictly blocked.

Idempotent: safe to run on a freshly extracted source tree or an already-patched tree
(including upgrading an older DOWNLOAD_PAGE_URL to the current build's direct APK URL).
"""
import pathlib
import re
import sys

REPO = "Lovelycrxshx/Kuro-tv-Releases"
DEFAULT_VERSION = "1.5.5"
ANY_PINNED_DL_RE = re.compile(
    r"https://(?:lovelycrxshx\.github\.io/Kuro-tv-Releases/|github\.com/Lovelycrxshx/Kuro-tv-Releases/releases/download/v[0-9.]+/Kuro-tv-[0-9.]+\.apk)"
)


def detect_download_url(root: pathlib.Path) -> str:
    gradle = root / "build.gradle"
    ver = DEFAULT_VERSION
    if gradle.exists():
        m = re.search(r"versionName\s+'([^']+)'", gradle.read_text(encoding="utf-8"))
        if m:
            cand = m.group(1).strip()
            if cand and "-" not in cand:
                ver = cand
    return f"https://github.com/{REPO}/releases/download/v{ver}/Kuro-tv-{ver}.apk"


def patch_once(path: pathlib.Path, old: str, new: str, marker: str) -> bool:
    if not path.exists():
        return False
    src = path.read_text(encoding="utf-8")
    if marker in src:
        return False
    if old not in src:
        raise SystemExit(f"{path}: expected snippet not found:\n{old}")
    path.write_text(src.replace(old, new, 1), encoding="utf-8")
    return True


def upgrade_pinned_url(path: pathlib.Path, download_url: str) -> bool:
    if not path.exists():
        return False
    src = path.read_text(encoding="utf-8")
    updated = ANY_PINNED_DL_RE.sub(download_url, src)
    if updated != src:
        path.write_text(updated, encoding="utf-8")
        return True
    return False


def patch_cpp(root: pathlib.Path, download_url: str) -> None:
    p = root / "native/kuro_discord_rpc563.cpp"
    old = (
        "  // Saved legacy link choices remain on the phone; no activity buttons are published.\n"
        "  (void)watch;(void)download;auto details=read(e,d),state=read(e,st),poster=read(e,image);\n"
        "  if(!details.empty())a->SetDetails(details);if(!state.empty())a->SetState(state);\n"
        "  if(isNeutralPoster566(poster)){discordpp::ActivityAssets assets;assets.SetLargeImage(poster);a->SetAssets(assets);}\n"
        "  if(start>0){discordpp::ActivityTimestamps t;t.SetStart(static_cast<uint64_t>(start));if(end>start)t.SetEnd(static_cast<uint64_t>(end));a->SetTimestamps(t);}\n"
        "  lastActivity=a;publishDirect();publishLinked();"
    )
    new = (
        "  // Episode/source watch links are never published; only Kuro's pinned GitHub APK download button may be attached.\n"
        "  (void)watch;auto details=read(e,d),state=read(e,st),poster=read(e,image),dl=read(e,download,256);\n"
        "  if(!details.empty())a->SetDetails(details);if(!state.empty())a->SetState(state);\n"
        "  if(isNeutralPoster566(poster)){discordpp::ActivityAssets assets;assets.SetLargeImage(poster);a->SetAssets(assets);}\n"
        "  if(start>0){discordpp::ActivityTimestamps t;t.SetStart(static_cast<uint64_t>(start));if(end>start)t.SetEnd(static_cast<uint64_t>(end));a->SetTimestamps(t);}\n"
        f'  if(dl=="{download_url}"){{discordpp::ActivityButton btn;btn.SetLabel("Download");btn.SetUrl(dl);a->AddButton(btn);}}\n'
        "  lastActivity=a;publishDirect();publishLinked();"
    )
    if patch_once(p, old, new, 'btn.SetLabel("Download")'):
        print(f"patched {p}")
    elif upgrade_pinned_url(p, download_url):
        print(f"upgraded URL in {p}")


def patch_presence563(root: pathlib.Path) -> None:
    p = root / "src/app/lovelycrxshx/kuro/KuroPresence563.java"
    old = '  Snapshot(String d,String s,String i,String b,boolean w,long start,long end,String download){details=d;state=s;image=i;buttonUrl="";watching=w;startMs=start;endMs=end;downloadUrl="";}'
    new = (
        "  Snapshot(String d,String s,String i,String b,boolean w,long start,long end,String download){\n"
        '   details=d==null?"":d;state=s==null?"":s;image=i==null?"":i;buttonUrl="";watching=w;\n'
        "   startMs=Math.max(0,start);endMs=end>startMs?end:0;\n"
        '   downloadUrl=KuroPresence564.DOWNLOAD_PAGE_URL.equals(download)?download:"";\n'
        "  }"
    )
    if patch_once(p, old, new, "KuroPresence564.DOWNLOAD_PAGE_URL.equals"):
        print(f"patched {p}")


def patch_presence564(root: pathlib.Path, download_url: str) -> None:
    p = root / "src/app/lovelycrxshx/kuro/KuroPresence564.java"
    if not p.exists():
        return
    src = p.read_text(encoding="utf-8")
    if "DOWNLOAD_PAGE_URL" in src:
        if upgrade_pinned_url(p, download_url):
            print(f"upgraded URL in {p}")
        return

    replacements = [
        (
            " static final class Options {\n"
            '  String layout="anime",separator="dot",pause="show",downloadUrl="",away="quick";\n'
            "  boolean title=true,episode=true,poster=true,watch=true,server=false,total=false,buffering=true,download=false,rewatch=false;\n"
            " }",
            f' static final String DOWNLOAD_PAGE_URL="{download_url}";\n'
            " static final class Options {\n"
            '  String layout="anime",separator="dot",pause="show",downloadUrl="",away="quick";\n'
            "  boolean title=true,episode=true,poster=true,watch=true,server=false,total=false,buffering=true,download=true,rewatch=false;\n"
            " }\n"
            ' private static String downloadButton(Options o){return o!=null&&o.download&&o.title?DOWNLOAD_PAGE_URL:"";}',
        ),
        (
            " static KuroPresence563.Snapshot browsing(long sessionStart,Options o){\n"
            "  // No buttons are published. Title privacy applies here too: when the user\n"
            "  // hides titles the app never names the service it is idling in.\n"
            "  boolean named=o!=null&&o.title;\n"
            '  String details=named?"Looking for an anime to watch":"In the app";\n'
            '  String state=named?"Kuro.tv":"Browsing";\n'
            '  return new KuroPresence563.Snapshot(details,state,"","",true,Math.max(0,sessionStart),0,"");\n'
            " }",
            " static KuroPresence563.Snapshot browsing(long sessionStart,Options o){\n"
            "  // Source links are never published. Title privacy applies here too: when the user\n"
            "  // hides titles the app never names the service it is idling in.\n"
            "  boolean named=o!=null&&o.title;\n"
            '  String details=named?"Looking for an anime to watch":"In the app";\n'
            '  String state=named?"Kuro.tv":"Browsing";\n'
            '  return new KuroPresence563.Snapshot(details,state,"","",true,Math.max(0,sessionStart),0,downloadButton(o));\n'
            " }",
        ),
        (
            '  return new KuroPresence563.Snapshot(details,state,image,"",true,start,end,"");',
            '  return new KuroPresence563.Snapshot(details,state,image,"",true,start,end,downloadButton(o));',
        ),
        (
            ' static KuroPresence563.Snapshot test(){return new KuroPresence563.Snapshot("Kuro connection test","Test activity for 15 seconds","","",true,0,0,"");}',
            " static KuroPresence563.Snapshot test(){return test(null);}\n"
            ' static KuroPresence563.Snapshot test(Options o){return new KuroPresence563.Snapshot("Kuro connection test","Test activity for 15 seconds","","",true,0,0,(o==null||(o.download&&o.title))?DOWNLOAD_PAGE_URL:"");}',
        ),
    ]
    for old, new in replacements:
        if old not in src:
            raise SystemExit(f"{p}: expected snippet not found:\n{old}")
        src = src.replace(old, new, 1)
    p.write_text(src, encoding="utf-8")
    print(f"patched {p}")


def patch_preferences564(root: pathlib.Path) -> None:
    p = root / "src/app/lovelycrxshx/kuro/KuroDiscordPreferences564.java"
    if not p.exists():
        return
    src = p.read_text(encoding="utf-8")
    if "option-download15" in src:
        return
    replacements = [
        (
            '  CHOICES.put("download",new String[]{"off","on"});',
            '  CHOICES.put("download",new String[]{"on","off"});',
        ),
        (
            ' String get(String key){String[] values=CHOICES.get(key);if(values==null)return "";String v=prefs.getString("option-"+key,values[0]);return Arrays.asList(values).contains(v)?v:values[0];}\n'
            ' boolean set(String key,String value){String[] values=CHOICES.get(key);if(values==null||value==null||!Arrays.asList(values).contains(value))return false;prefs.edit().putString("option-"+key,value).apply();return true;}',
            ' private static String storageKey(String key){return "download".equals(key)?"option-download15":"option-"+key;}\n'
            ' String get(String key){String[] values=CHOICES.get(key);if(values==null)return "";String v=prefs.getString(storageKey(key),values[0]);return Arrays.asList(values).contains(v)?v:values[0];}\n'
            " boolean set(String key,String value){String[] values=CHOICES.get(key);if(values==null||value==null||!Arrays.asList(values).contains(value))return false;prefs.edit().putString(storageKey(key),value).apply();return true;}",
        ),
    ]
    for old, new in replacements:
        if old not in src:
            raise SystemExit(f"{p}: expected snippet not found:\n{old}")
        src = src.replace(old, new, 1)
    p.write_text(src, encoding="utf-8")
    print(f"patched {p}")


def patch_rpc563(root: pathlib.Path) -> None:
    p = root / "src/app/lovelycrxshx/kuro/KuroDiscordRpc563.java"
    old = (
        "    KuroPresence563.Snapshot s=inTest?KuroPresence564.test():latest;\n"
        "    if(s!=null&&ready&&gate.due(s,now)){\n"
        "     nativePublish(owner,utf8(s.details),utf8(s.state),utf8(s.image),NO_PRESENCE_LINK,NO_PRESENCE_LINK,s.startMs,s.endMs);"
    )
    new = (
        "    KuroPresence563.Snapshot s=inTest?KuroPresence564.test(preferences.options()):latest;\n"
        "    if(s!=null&&ready&&gate.due(s,now)){\n"
        "     nativePublish(owner,utf8(s.details),utf8(s.state),utf8(s.image),NO_PRESENCE_LINK,utf8(s.downloadUrl),s.startMs,s.endMs);"
    )
    if patch_once(p, old, new, "utf8(s.downloadUrl)"):
        print(f"patched {p}")


def patch_settings_js(root: pathlib.Path) -> None:
    p = root / "assets/settings560.js"
    if not p.exists():
        return
    row_153 = "  ['download','Download button','Show a Download button on your Discord activity for a direct APK download from GitHub Releases.',['on','off'],['Show','Hide']],"
    row_150 = "  ['download','Download button','Show a Download button on your Discord activity linking to Kuro\\'s download page.',['on','off'],['Show','Hide']],"
    src = p.read_text(encoding="utf-8")
    if row_153 in src:
        return
    if row_150 in src:
        p.write_text(src.replace(row_150, row_153, 1), encoding="utf-8")
        print(f"upgraded {p}")
        return
    old = "  ['poster','Artwork','Uses an anime cover, not an episode thumbnail. Kuro may search AniList or Kitsu by title; otherwise the app icon is used.',['show','icon'],['Anime poster','App icon']],"
    if old not in src:
        raise SystemExit(f"{p}: expected snippet not found:\n{old}")
    p.write_text(src.replace(old, old + "\n" + row_153, 1), encoding="utf-8")
    print(f"patched {p}")


def patch_tests(root: pathlib.Path, download_url: str) -> None:
    cpp_test = root / "tests/KuroDiscordNative564Test.cpp"
    if cpp_test.exists():
        old = (
            ' assert(direct->last.details=="Actual title"&&direct->last.state=="Episode 1 · DUB");assert(direct->last.assets.large=="https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx1-example.jpg");assert(direct->last.buttons.empty()&&linkedClient->last.buttons.empty());assert(direct->last.times.start==1000&&direct->last.times.end==2000);\n'
            ' auto providerImg=makeBytes("https://hianime.ms/poster.jpg");'
        )
        new = (
            ' assert(direct->last.details=="Actual title"&&direct->last.state=="Episode 1 · DUB");assert(direct->last.assets.large=="https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx1-example.jpg");assert(direct->last.buttons.empty()&&linkedClient->last.buttons.empty());assert(direct->last.times.start==1000&&direct->last.times.end==2000);\n'
            f' auto officialDl=makeBytes("{download_url}");\n'
            ' Java_app_lovelycrxshx_kuro_KuroDiscordRpc563_nativePublish(&env,nullptr,one,d,st,img,watch,officialDl,1000,2000);\n'
            f' assert(direct->last.buttons.size()==1&&direct->last.buttons[0].label=="Download"&&direct->last.buttons[0].url=="{download_url}");\n'
            f' assert(linkedClient->last.buttons.size()==1&&linkedClient->last.buttons[0].label=="Download"&&linkedClient->last.buttons[0].url=="{download_url}");\n'
            ' auto providerImg=makeBytes("https://hianime.ms/poster.jpg");'
        )
        if patch_once(cpp_test, old, new, "officialDl"):
            print(f"patched {cpp_test}")
        elif upgrade_pinned_url(cpp_test, download_url):
            print(f"upgraded URL in {cpp_test}")

    t564 = root / "tests/KuroPresence564HostTest.java"
    if t564.exists():
        src = t564.read_text(encoding="utf-8")
        if "DOWNLOAD_PAGE_URL" not in src:
            target = (
                '  o.download=true;o.downloadUrl="https://example.com/get/kuro";check(sample(o,"playing",0,10000).downloadUrl.isEmpty(),"download not published even when an older option was on");\n'
                '  o.title=false;s=sample(o,"playing",0,10000);check(!s.details.contains("Jujutsu")&&!s.state.contains("Jujutsu")&&s.image.isEmpty()&&s.buttonUrl.isEmpty()&&s.downloadUrl.isEmpty(),"title privacy hides identifying assets");\n'
                '  o.title=true;o.episode=false;s=sample(o,"playing",0,10000);check(!s.details.contains("12")&&!s.state.contains("12")&&s.buttonUrl.isEmpty()&&s.downloadUrl.isEmpty(),"episode privacy hides episode-specific links");'
            )
            repl = (
                '  o.download=true;o.downloadUrl="https://example.com/get/kuro";check(sample(o,"playing",0,10000).downloadUrl.equals(KuroPresence564.DOWNLOAD_PAGE_URL),"only pinned Kuro download URL is published");\n'
                '  o.download=false;check(sample(o,"playing",0,10000).downloadUrl.isEmpty(),"download button hidden when toggled off");o.download=true;\n'
                '  o.title=false;s=sample(o,"playing",0,10000);check(!s.details.contains("Jujutsu")&&!s.state.contains("Jujutsu")&&s.image.isEmpty()&&s.buttonUrl.isEmpty()&&s.downloadUrl.isEmpty(),"title privacy hides identifying assets");\n'
                '  o.title=true;o.episode=false;s=sample(o,"playing",0,10000);check(!s.details.contains("12")&&!s.state.contains("12")&&s.buttonUrl.isEmpty()&&s.downloadUrl.equals(KuroPresence564.DOWNLOAD_PAGE_URL),"episode privacy hides episode-specific links while keeping app download URL");'
            )
            if target in src:
                src = src.replace(target, repl, 1)
            src = src.replace(
                'browsing.details.equals("Browsing Kuro")',
                'browsing.details.equals("Looking for an anime to watch")&&browsing.downloadUrl.equals(KuroPresence564.DOWNLOAD_PAGE_URL)',
                1,
            )
            t564.write_text(src, encoding="utf-8")
            print(f"patched {t564}")

    t565 = root / "tests/KuroPresence565HostTest.java"
    if t565.exists():
        src = t565.read_text(encoding="utf-8")
        if "DOWNLOAD_PAGE_URL" not in src:
            src = src.replace(
                '  check(s.downloadUrl.isEmpty(),"no Download button");\n'
                '  o.download=true;o.downloadUrl="https://example.org/download";check(sample(o,"12","Title",episode,show,"playing",1,600_000,1).downloadUrl.isEmpty(),"old Download preference cannot publish");\n'
                '  check(KuroPresence564.browsing(1,o).downloadUrl.isEmpty(),"browsing cannot publish Download");',
                '  check(s.downloadUrl.equals(KuroPresence564.DOWNLOAD_PAGE_URL),"default Download button points to Kuro GitHub APK");\n'
                '  o.download=true;o.downloadUrl="https://example.org/download";check(sample(o,"12","Title",episode,show,"playing",1,600_000,1).downloadUrl.equals(KuroPresence564.DOWNLOAD_PAGE_URL),"custom downloadUrl cannot override pinned URL");\n'
                '  check(KuroPresence564.browsing(1,o).downloadUrl.equals(KuroPresence564.DOWNLOAD_PAGE_URL),"browsing publishes pinned Download URL when enabled");',
                1,
            )
            src = src.replace(
                '  for(String page:pages){s=sample(o,"12","Episode",page,page,"playing",0,600_000,1);check(s.buttonUrl.isEmpty()&&s.downloadUrl.isEmpty(),"no page URL may be published: "+page);}',
                '  for(String page:pages){s=sample(o,"12","Episode",page,page,"playing",0,600_000,1);check(s.buttonUrl.isEmpty()&&!s.downloadUrl.equals(page),"no source page URL may be published: "+page);}',
                1,
            )
            t565.write_text(src, encoding="utf-8")
            print(f"patched {t565}")

    t566 = root / "tests/KuroPresence566HostTest.java"
    if t566.exists():
        src = t566.read_text(encoding="utf-8")
        if "DOWNLOAD_PAGE_URL" not in src:
            src = src.replace(
                '  check(a.buttonUrl.isEmpty()&&a.downloadUrl.isEmpty(),"old on settings cannot create links");',
                '  check(a.buttonUrl.isEmpty()&&a.downloadUrl.equals(KuroPresence564.DOWNLOAD_PAGE_URL),"watch URLs blocked and only pinned Kuro download URL allowed");',
                1,
            )
            src = src.replace(
                '  a=KuroPresence564.browsing(1000,o);check(a.buttonUrl.isEmpty()&&a.downloadUrl.isEmpty(),"browsing has no buttons");\n'
                '  a=KuroPresence564.test();check(a.buttonUrl.isEmpty()&&a.downloadUrl.isEmpty(),"test presence has no buttons");',
                '  a=KuroPresence564.browsing(1000,o);check(a.buttonUrl.isEmpty()&&a.downloadUrl.isEmpty(),"browsing respects hidden download/title");\n'
                '  a=KuroPresence564.test();check(a.buttonUrl.isEmpty()&&a.downloadUrl.equals(KuroPresence564.DOWNLOAD_PAGE_URL),"test presence carries pinned Download button");',
                1,
            )
            t566.write_text(src, encoding="utf-8")
            print(f"patched {t566}")

    robo = root / "tests/native-panel/src/test/java/app/lovelycrxshx/kuro/KuroDiscord564Test.java"
    if robo.exists():
        src = robo.read_text(encoding="utf-8")
        if "KuroPresence564.DOWNLOAD_PAGE_URL" not in src:
            src = src.replace(
                'assertEquals("on",p.get("current"));assertEquals("off",p.get("download"));',
                'assertEquals("on",p.get("current"));assertEquals("on",p.get("download"));',
                1,
            )
            src = src.replace(
                "  assertTrue(s.buttonUrl.isEmpty());assertTrue(s.downloadUrl.isEmpty());assertTrue(s.image.isEmpty());",
                "  assertTrue(s.buttonUrl.isEmpty());assertEquals(KuroPresence564.DOWNLOAD_PAGE_URL,s.downloadUrl);assertTrue(s.image.isEmpty());",
                1,
            )
            robo.write_text(src, encoding="utf-8")
            print(f"patched {robo}")


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        raise SystemExit("usage: discord_rpc_download_1.5.py <project_dir>")
    root = pathlib.Path(argv[1])
    download_url = detect_download_url(root)
    print(f"Discord RPC Download URL -> {download_url}")
    patch_cpp(root, download_url)
    patch_presence563(root)
    patch_presence564(root, download_url)
    patch_preferences564(root)
    patch_rpc563(root)
    patch_settings_js(root)
    patch_tests(root, download_url)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
