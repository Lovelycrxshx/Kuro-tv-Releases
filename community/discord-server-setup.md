# Kuro TV Discord server setup

**Status:** draft blueprint — no Discord server or invite has been created yet.

This plan is tailored to Kuro TV, an open-source media client for Android TV, Fire TV and mobile devices. Kuro is a player/front-end; it does not host, upload or control media streams.

## Server profile

- **Suggested name:** Kuro TV Community
- **Description:** The community for Kuro TV. Get official release links, ask for help, share feedback and follow development. Kuro is a media player/front-end and does not host media.
- **Official links:**
  - Project: <https://github.com/Lovelycrxshx/Kuro-tv-Releases>
  - Releases: <https://github.com/Lovelycrxshx/Kuro-tv-Releases/releases>
  - Download page: <https://lovelycrxshx.github.io/Kuro-tv-Releases/>

## Suggested layout

Start small so the server feels active. Add optional channels only when there is enough activity to support them.

| Category | Channel | Type / access | Purpose |
| --- | --- | --- | --- |
| **START HERE** | `#welcome` | Read-only | Quick orientation and where to go for help. |
|  | `#rules` | Read-only | Community rules and safety notes. |
|  | `#announcements` | Read-only; staff posts | Project news and release announcements. Let members opt into an announcements role if desired. |
|  | `#official-links` | Read-only; staff posts | Canonical GitHub, release and download-page links. Never accept APKs or update links from strangers as official. |
| **SUPPORT** | `#help` | Forum; members can post | Installation, setup and general troubleshooting. One issue per post keeps answers together. |
|  | `#bug-reports` | Forum; members can post | Reproducible bugs, with a template and device/version details. |
|  | `#feature-ideas` | Forum; members can post | Suggestions and discussion of proposed features. |
| **COMMUNITY** | `#general` | Text | Kuro TV discussion and community chat. |
|  | `#anime-and-manga` | Text; optional | Off-topic fandom chat. Require spoiler tags for recent plot details. |
| **TEAM** | `#team-chat` | Private to staff | Internal coordination. Create only for the project team. |

Optional later: `#beta-feedback` (restricted to approved beta testers), `#screenshots-and-setups`, and a voice lounge. Avoid creating many empty channels on day one.

## Roles and permissions

Use Discord's built-in roles and onboarding first; a bot is not required to launch.

| Role | Suggested permissions |
| --- | --- |
| `Admin` | Manage server and roles; reserve for trusted owners. Keep the built-in **Administrator** permission limited to the smallest possible number of people. |
| `Moderator` | Manage messages, timeout members and handle reports; no Administrator permission. |
| `Developer` | Staff identity for project maintainers; grant channel access separately rather than broad server powers. |
| `Contributor` | Recognition role; no elevated permissions by default. |
| `Beta Tester` | Access to beta-only channels if those are used. Assign manually if access is not public. |
| `Announcements` | Optional, mentionable opt-in role for release/news pings. |
| `Member` | Optional verified/member role for community participation. |

Suggested access rules:

- `#welcome`, `#rules`, `#announcements` and `#official-links`: everyone can read; only staff can post (except reactions if wanted).
- Support forums and public chat: members can read and post; moderators can manage threads and messages.
- `#team-chat`: visible only to the project team.
- Beta channels: visible only to approved beta testers and staff.
- Do not grant bots or helpers Administrator unless there is a specific, trusted reason.

## Copy-ready channel messages

### `#welcome`

> Welcome to **Kuro TV Community**! 👋
>
> Start with <#rules>, then check <#official-links> for the project and official downloads. For help, open a post in <#help>; for a reproducible problem, use <#bug-reports>.
>
> Kuro TV is an open-source media player/front-end. It does not host or provide media streams. Please do not post stream links, credentials, private account details, or APKs from unofficial sources. Be kind, share enough device and app details to troubleshoot, and keep support in public channels so others can learn too.

Replace the channel mentions after creating the channels; Discord will insert the correct channel IDs when you type `#` and select them.

### `#rules`

> **Kuro TV Community Rules**
>
> 1. Treat members and maintainers respectfully. Harassment, hate speech, threats and targeted abuse are not allowed.
> 2. Keep posts in the right channel. Search before opening a duplicate bug or feature request.
> 3. Kuro TV is a media player/front-end; it does not host or control media. Do not request, share or promote copyrighted stream links, pirated copies, or ways to bypass payment/access controls.
> 4. Use only the official links in <#official-links> for downloads. Do not upload APKs or distribute modified builds here.
> 5. Never post passwords, tokens, private URLs, personal information, or unredacted logs. Staff will not ask you to send credentials by DM.
> 6. For support, use <#help> or <#bug-reports> rather than unsolicited direct messages. Include device, OS and Kuro version, and clear reproduction steps.
> 7. Use spoiler tags for anime/manga spoilers, especially recent episodes and chapters.
> 8. No spam, scams, phishing, malware, unsolicited ads or repeated pings. Follow moderator instructions and Discord's Terms of Service.
>
> Moderators may remove content or restrict access to keep the community safe. If you think a moderation action was a mistake, contact the team calmly through the server's designated contact route.

### `#official-links`

> **Official Kuro TV links**
>
> - Project and source: <https://github.com/Lovelycrxshx/Kuro-tv-Releases>
> - Latest releases: <https://github.com/Lovelycrxshx/Kuro-tv-Releases/releases>
> - Download page: <https://lovelycrxshx.github.io/Kuro-tv-Releases/>
>
> Verify downloads against the information on the official release page. The project does not distribute builds through DMs or third-party file hosts.

## Forum post templates

Set these as the channel's post guidelines or starter questions if Forum Channels are available.

### Help request

```text
Device/model:
Android / Fire OS version:
Kuro TV version:
What are you trying to do?
What happened, and what did you expect?
Steps to reproduce:
Anything you have already tried:
```

### Bug report

```text
Short title:
Device/model and OS version:
Kuro TV version:
Steps to reproduce:
Expected result:
Actual result:
Does it happen every time?
Sanitized logs or screenshot (remove personal data first):
```

### Feature idea

```text
What problem would this solve?
What would you like Kuro TV to do?
Which device/screen is this for?
Is there a workaround today?
```

Remind members not to include passwords, session tokens, personal data, or links to media streams in posts or screenshots.

## Launch checklist

1. Create a server named **Kuro TV Community** (or choose another name) in Discord.
2. Create the categories and starter channels above.
3. Create staff roles first; configure channel permissions before inviting members.
4. Enable Community features if you want Discord's rules screening, onboarding and announcement tools. Set `#rules` as the rules channel and `#announcements` as the announcement channel.
5. Configure Discord AutoMod for spam, suspicious links and mention spam. Keep moderation rules clear and review false positives.
6. Pin the copy-ready messages, then test the server with a non-staff account before sharing an invite.
7. Create an invite with the intended expiry and usage limits. Share it only after the official invite is confirmed.
8. Once the server is live, add the real invite to the repository's README/download page and replace this draft's status with the public invite.

## Reference-server note

The public invite resolves to **HiAnime**. Its invite preview shows about 6,147 members, 720 online, and a `🚪・welcome` entry channel. The public server profile advertises community onboarding, membership verification, AutoMod, discovery, news and voice/hangout features. That suggests a polished welcome/onboarding flow, interest roles and active moderation are part of the reference's approach.

The invite preview does **not** expose the full channel list, role list or permissions. This blueprint adapts those high-level patterns to Kuro TV rather than claiming to reproduce HiAnime's exact setup. A screenshot of the reference server's channel sidebar (and role list, if relevant) would let us match the organization more closely.
