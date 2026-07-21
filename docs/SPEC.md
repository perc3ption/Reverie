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
| Browse Folders / Artists / Albums | Done | Navigable Folders, Artists, and Albums with song lists |
| Home (recently played / library preview) | Done | Tap tracks to play |
| Home Now Playing card | Done | Compact player on Home (seek + transport) |
| Mini player (bottom, all main tabs) | Done | Shown when a track is active |
| Playback controls | Done | Play/pause, seek, next/prev, shuffle, repeat |
| Background playback + media notification | Done | Media3 `MediaSessionService` |
| Full Now Playing screen | Done | Switchable Album Art / Visualizer media view |
| Basic spectrum visualizer | Done | Classic CRT spectrum (free) |
| Visualizer style picker | Done | SPECTRUM free; SCOPE, RADIAL, VU, STARBURST premium |
| Playlists | Done | Library Playlists tab; All Songs; create/delete; detail add/remove; description + cover |
| Theme (system / light / dark) | Done | Settings |
| Free-tier limits | Done | Max **500** songs, **3** playlists |
| Search | Done | Shared Search screen from Home / Library; songs, artists, albums, playlists, folders |
| Album art display | Done | Embedded + folder covers cached on disk; Coil in UI |
| Play from album / artist / folder | Done | Play song or Play All from folder / artist / album browsers |
| Queue UI (“next up” management) | Done | Queue sheet: tap to play, session mute, reorder (up/down); Add to Queue from Library / Search / Song Detail |
| Tutorial / Discover Reverie | Done | Home tile + chapter hub; first-run welcome when library empty; Try-it deep links; progress persisted |

## Premium

Gated via `AppFeature` / `FeatureAccessChecker`. Entitlements are currently a **mock/dev toggle** (Play Billing not wired).

| Feature | Status | Description |
| --- | --- | --- |
| Unlimited Library | Partial (gate only) | No 500-song import limit |
| Unlimited Playlists | Partial (gate only) | Beyond the 3-playlist free limit |
| Song Ratings | Done | Premium: 1–5 stars on Song Detail; tap same star to clear; replaces Favorites |
| Tags | Partial | Add/remove tags on Song Detail; Library browse still stubbed |
| Collections | Planned | Custom listening collections |
| Smart Playlists | Done | Premium: rule-based playlists (artist/album/genre/year/rating/tag/date added/play count/recent); evaluate on open |
| Playback Scope | Dropped | Covered by queue source (folder / album / playlist / etc.) |
| Advanced Visualizers | Done | Scope, Radial, VU, Starburst skins |
| Advanced Search | Planned | Filters and saved searches |
| Lyrics | Done | Sidecar `.lrc` / `.txt`; import via + on empty Lyrics tab; synced highlight |
| Metadata Editing | Partial | Edit title/artist/album/year/genre on Song Detail; writes to file + Room |
| Album Art Editing | Done | Premium: empty-art import on Player + Song Detail; reimport via cover icon on Song Detail; cache + Room (album-scoped) |
| Library Stats | Done | Premium: Stats screen — library counts, play totals, top tracks/artists (on-demand from play_history) |
| Audio FX | Done | Premium: 10-band EQ + preamp + bass boost + presets; loudness leveling; crossfade; gapless/track-gap |
| Tutorial / Discover Reverie | Done | (Listed under Core) Home hub + first-run; screenshot slots ready for assets |
| Play Billing purchase / restore | Planned | Replace mock premium toggle |

## Supported audio formats

Source of truth: `SupportedAudioFormats` (also used by Import UI, in-app tutorial, and library README).

**Import / scan:** `mp3`, `flac`, `m4a`, `aac`, `ogg`, `opus`, `wav`, `wma`, `alac`, `aiff`, `aif`

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
| WMA | No | Imported/indexed, but ExoPlayer has no default decoder; filtered before queue |

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

## Lyrics

Synced lyrics load from sidecar files next to the audio file (same basename):

- `Song.lrc` — timed LRC (preferred)
- `Song.txt` — plain unsynced text
- fallback: `lyrics.lrc` / `lyrics.txt` in the same folder

Empty Lyrics tab offers **+** to pick a file; `LyricsSidecarImporter` validates `.lrc`/`.txt`, copies next to the track, and renames to match the audio basename.

User-facing steps: see [`docs/TUTORIAL.md`](TUTORIAL.md).

Premium-gated via `AppFeature.LYRICS`.

## Keep in sync

- Premium list ↔ `AppFeature` and the Premium screen
- Codec list ↔ `SupportedAudioFormats` (Import UI, tutorial, README, playback filter)
- Free limits ↔ `FeatureAccessChecker.FREE_MAX_SONGS` / `FREE_MAX_PLAYLISTS`
