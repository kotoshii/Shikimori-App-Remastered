<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="112" alt="">
</p>

<h1 align="center">Shikimori App Remastered</h1>

<p align="center">
  <a href="https://github.com/kotoshii/Shikimori-App-Remastered/releases/latest"><img src="https://img.shields.io/github/v/release/kotoshii/Shikimori-App-Remastered" alt="Release"></a>
  <a href="https://github.com/kotoshii/Shikimori-App-Remastered/releases"><img src="https://img.shields.io/github/downloads/kotoshii/Shikimori-App-Remastered/total" alt="Downloads"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-blue" alt="License"></a>
</p>

Android client for [shikimori.io](https://shikimori.io). Anime and manga database, your lists, forum, and watching episodes in the app.

This is a fork of [gnoemes/Shikimori-App-Remastered](https://github.com/gnoemes/Shikimori-App-Remastered). The original is still around, but it gets updated mostly when Shikimori breaks something, and builds go to the author's Telegram instead of GitHub. This fork is kept working, and APKs are published here.

## Install

[Download the latest APK](https://github.com/kotoshii/Shikimori-App-Remastered/releases/latest)

Needs Android 4.1 or newer. Allow installs from unknown sources, and when you open the app Play Protect will probably ask to scan or verify it. That happens with any APK installed outside the store.

Versions are `major.minor.patch`, like `0.8.8`. Older releases had a fourth build number in the end, like `0.8.7.1`. It is not used anymore.

## Screenshots

<p align="center">
  <img src="docs/screenshots/anime_catalog.png" width="19%" alt="Catalog">
  <img src="docs/screenshots/anime.png" width="19%" alt="Anime page">
  <img src="docs/screenshots/character.png" width="19%" alt="Character">
  <img src="docs/screenshots/calendar.png" width="19%" alt="Calendar">
  <img src="docs/screenshots/settings.png" width="19%" alt="Settings">
</p>

## What's different from the original

Both apps can watch episodes. The difference is mostly in where the work happens.

**Video hosts**

- The app parses hosts itself. 11 parsers here, 3 in the original. When a host changes its site or moves to a new domain, it gets fixed in the app instead of waiting for the backend.
- Working hosts: VK, OK, mail.ru, Sibnet, SovetRomantica, AnimeJoy, AllVideo, Dzen, cda.pl.
- cda.pl was added here.
- A setting to open the secondary source by default instead of the main one.
- A setting to hide Anime 365 from the list when you are not logged in, since those links do not play without an account.

**Shikimori API**

- 🎉 **Posters are back!** Shikimori stopped sending them in the JSON API for anything added recently and returns a grey placeholder instead. The app now asks their GraphQL API for the real image, so covers show up again in the catalog, calendar, search, anime and manga pages, chronology, your lists and the forum.
- Much fewer "Too Many Requests" errors. Shikimori limits both how fast and how many requests you send. All requests now go through one queue that spaces them out, which mostly showed up when opening an anime page and then chronology right after.
- The update check reads releases from this repo.

**Fixed along the way**

- Light novels were shown as manga on some screens.
- Some anime kinds were missing.
- Chronology skipped entries, and could hang on titles with an empty franchise.
- Switching episodes could pick the wrong author.
- Downloading an Anime 365 episode could crash.
- Other minor bug fixes.

## Building

Old project with an old toolchain. StorIO is used with deprecated modules and code generation, which is what keeps everything pinned to 2018 versions. Moving to Room would fix that.

- JDK 8. Gradle 4.10.1 and AGP 3.2.1 do not run on newer ones.
- [Android Studio 4.1.1](https://developer.android.com/studio/archive) or older.
- Android SDK 28.

```
./gradlew assembleDebug
```

There is no signing config in the repo, so release builds come out unsigned.

## Credits

Original app by [gnoemes](https://github.com/gnoemes). GPL-3.0, same as the original, see [LICENSE](LICENSE).
