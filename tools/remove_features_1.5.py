#!/usr/bin/env python3
"""Kuro 1.5.6-beta1: Remove unwanted features from the base source.

Runs during the build after the overlay is applied. Patches:
  - app.js:       remove the "All local titles" library tab (concept: user
                  wants only specific filter tabs, not the catch-all "All")
  - settings560.js: remove the "Manage downloads" action and the Discord
                  "Download button" toggle (user wants local downloads gone)

Usage: python3 remove_features_1.5.py <project_root>
"""
import sys
import pathlib


def patch_app_js(root):
    """Remove the 'All' tab from libraryFilters557 and default to bookmarks."""
    path = root / "assets/app.js"
    if not path.exists():
        print("remove_features: app.js not found, skipping")
        return

    text = path.read_text(encoding="utf-8")
    original = text

    # 1. Remove the ['all','All'] entry from libraryFilters557
    #    libraryFilters557=[['all','All'],['watching','Watching'],...]
    #    Replace with just the non-'all' entries.
    old_filters = "const libraryFilters557=[['all','All'],['watching','Watching'],"
    new_filters = "const libraryFilters557=[['watching','Watching'],"
    if old_filters in text:
        text = text.replace(old_filters, new_filters)
        print("remove_features: removed 'all' from libraryFilters557")

    # 2. Change default listFilter from 'all' to 'bookmarks'
    old_default = "let listFilter='all';"
    new_default = "let listFilter='bookmarks';"
    if old_default in text:
        text = text.replace(old_default, new_default)
        print("remove_features: default listFilter changed to 'bookmarks'")

    # 3. Change nav handler: library click should go to 'bookmarks' not 'all'
    old_nav = "if(b.dataset.nav==='library')listFilter='all';"
    new_nav = "if(b.dataset.nav==='library')listFilter='bookmarks';"
    if old_nav in text:
        text = text.replace(old_nav, new_nav)
        print("remove_features: library nav default changed to 'bookmarks'")

    # 4. Change the heading text from 'All local titles' to 'My library'
    #    listFilter==='all'?'All local titles':...
    old_heading = "listFilter==='all'?'All local titles':"
    new_heading = "listFilter==='bookmarks'?'My library':"
    if old_heading in text:
        text = text.replace(old_heading, new_heading)
        print("remove_features: removed 'All local titles' heading")

    # 5. Remove the allLocalTitles557 function — it's no longer referenced
    #    since 'all' is no longer a valid filter. But it's harmless to keep.
    #    We'll leave it to avoid breaking the call in case it's still used.

    if text != original:
        path.write_text(text, encoding="utf-8")
        print("remove_features: app.js patched")
    else:
        print("remove_features: app.js — no changes needed")


def patch_settings560(root):
    """Remove 'Manage downloads' action and Discord Download button toggle."""
    path = root / "assets/settings560.js"
    if not path.exists():
        print("remove_features: settings560.js not found, skipping")
        return

    text = path.read_text(encoding="utf-8")
    original = text

    # 1. Remove the 'Manage downloads' action from the Privacy and links group
    #    action('downloads','Manage downloads','View Kuro offline downloads',true)
    old_action = "+action('downloads','Manage downloads','View Kuro offline downloads',true)"
    if old_action in text:
        text = text.replace(old_action, "")
        print("remove_features: removed 'Manage downloads' action")

    # 2. Remove the Discord 'Download button' toggle from the settings list
    #    ['download','Download button','Show a Download button on your Discord activity...']
    old_discord = "  ['download','Download button','Show a Download button on your Discord activity for a direct APK download from GitHub Releases.',['on','off'],['Show','Hide']],"
    if old_discord in text:
        text = text.replace(old_discord, "")
        print("remove_features: removed Discord 'Download button' toggle")

    # 3. Remove kuro-download-clean-15 from the settings keys (download cleanup)
    old_key = "'kuro-download-clean-15'"
    if old_key in text:
        # Remove the key and its surrounding comma/bracket
        text = text.replace(",'kuro-download-clean-15'", "")
        text = text.replace("['kuro-settings560-v1','kuro-accent-1.1','kuro-amoled-152','kuro-download-clean-15']",
                            "['kuro-settings560-v1','kuro-accent-1.1','kuro-amoled-152']")
        print("remove_features: removed kuro-download-clean-15 key")

    if text != original:
        path.write_text(text, encoding="utf-8")
        print("remove_features: settings560.js patched")
    else:
        print("remove_features: settings560.js — no changes needed")


def main():
    if len(sys.argv) < 2:
        print("Usage: python3 remove_features_1.5.py <project_root>")
        sys.exit(1)
    root = pathlib.Path(sys.argv[1])
    patch_app_js(root)
    patch_settings560(root)
    print("remove_features: done")


if __name__ == "__main__":
    main()
