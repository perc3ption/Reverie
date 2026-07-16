# Spec

Internal product/feature spec for Reverie — what’s in scope, what’s done, and what’s still planned.  
Update this as features ship.

Status key: **Done** · **Partial** · **Planned**

## Core (free)

| Feature | Status | Notes |
| --- | --- | --- |
| Local music library (Room) | Done | Folders, tracks, playlists, play history, settings |
| Import music (SAF) | Done | Files or folders; copy or move into app storage |
| Library scan / re-index | Done | Syncs on-disk `Reverie/` folder into Room |
| Browse Songs | Done | Library → Songs; tap to play |
| Browse Folders / Artists / Albums | Partial | Lists exist; play-from-row not wired yet |
| Home (recently played / library preview) | Done | Tap tracks to play |
| Home Now Playing card | Done | Compact player on Home (seek + transport) |
| Mini player (bottom, all main tabs) | Done | Shown when a track is active |
| Playback controls | Done | Play/pause, seek, next/prev, shuffle, repeat |
| Background playback + media notification | Done | Media3 `MediaSessionService` |
| Full Now Playing screen | Done | Switchable Album Art / Visualizer media view |
| Basic spectrum visualizer | Done | Classic CRT spectrum (free) |
| Visualizer style picker | Done | SPECTRUM free; SCOPE, RADIAL, VU, STARBURST premium |
| Playlists | Partial | Room + repository ready; no playlist UI yet |
| Theme (system / light / dark) | Done | Settings |
| Free-tier limits | Done | Max **500** songs, **3** playlists |
| Search | Planned | Basic search TODOs on Home / Library |
| Album art display | Done | Embedded + folder covers cached on disk; Coil in UI |
| Play from album / artist / folder | Planned | |
| Queue UI (“next up” management) | Planned | Queue exists in player; no dedicated editor |

## Premium

Gated via `AppFeature` / `FeatureAccessChecker`. Entitlements are currently a **mock/dev toggle** (Play Billing not wired).

| Feature | Status | Description |
| --- | --- | --- |
| Unlimited Library | Partial (gate only) | No 500-song import limit |
| Unlimited Playlists | Partial (gate only) | Beyond the 3-playlist free limit |
| Favorites | Planned | Mark/loved tracks for quick access |
| Tags | Planned | Custom tags to organize the library |
| Collections | Planned | Custom listening collections |
| Smart Playlists | Planned | Rule-based playlists that update automatically |
| Playback Scope | Planned | Control what plays and shuffles across the library |
| Advanced Visualizers | Done | Scope, Radial, VU, Starburst skins |
| Advanced Search | Planned | Filters and saved searches |
| Lyrics | Planned | Synced lyrics while listening |
| Metadata Editing | Planned | Edit title, artist, album, etc. |
| Album Art Editing | Planned | Set custom cover art |
| Library Stats | Planned | Collection insights and statistics |
| Play Billing purchase / restore | Planned | Replace mock premium toggle |

## Supported audio formats

Import accepts these extensions (see `MusicIndexer.AUDIO_EXTENSIONS`):

`mp3`, `flac`, `m4a`, `aac`, `ogg`, `opus`, `wav`, `wma`, `alac`, `aiff`, `aif`

Playback uses Media3 ExoPlayer. Practical support:

| Format | Plays? | Notes |
| --- | --- | --- |
| MP3 | Yes | |
| FLAC | Yes | |
| M4A / AAC | Yes | |
| OGG / Opus | Yes | |
| WAV | Yes | |
| AIFF / AIF | Usually | |
| ALAC | Usually | Best as `.m4a`; bare `.alac` is less reliable |
| WMA | No | Imported/indexed, but ExoPlayer has no default decoder |

To play WMA (and expand codec coverage), we’d need something like Media3’s FFmpeg extension later.

## Library storage

Imported files live under the app’s media directory:

`Android/media/<package>/Reverie/`

Tracks are indexed into Room with absolute file paths.

## Stack (quick ref)

- Kotlin + Jetpack Compose
- Room
- Media3 ExoPlayer + MediaSession
- SAF import into app media storage
- `minSdk 24`

## Project layout (high level)

```
app/src/main/java/com/perceptiveus/reverie/
  data/          Room, import, repositories, storage
  domain/        Models
  feature/       Compose screens (home, library, player, …)
  playback/      MediaSessionService
  core/          Theme, navigation, entitlements, settings
```

## Keep in sync

- Premium list ↔ `AppFeature` and the Premium screen
- Codec list ↔ `MusicIndexer.AUDIO_EXTENSIONS` and Media3 capabilities
- Free limits ↔ `FeatureAccessChecker.FREE_MAX_SONGS` / `FREE_MAX_PLAYLISTS`
