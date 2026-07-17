# Importing music & lyrics

How to get audio into Reverie and attach lyrics so they show on the Now Playing screen.

## Import music (in-app)

1. Open **Import** (from Library or the import entry in the app).
2. Tap **Import Music**.
3. Choose **files** or a **folder**, then pick **Copy** or **Move**, and (optionally) a destination folder under Reverie.
4. Continue and select the songs or folder in the system picker.
5. Wait for the status message. Songs appear in **Library → Songs**.

Reverie only imports what you pick — it does not scan your whole device.

### Supported audio types

`mp3`, `flac`, `m4a`, `aac`, `ogg`, `opus`, `wav`, `wma`, `alac`, `aiff`, `aif`

Note: **WMA** can be imported/indexed but does not play yet (ExoPlayer has no default WMA decoder).

### Free tier

Free accounts are limited to **500** songs. Premium removes that limit (mock toggle in Settings for now).

## Import music via USB / file manager

1. Copy audio files into the app media folder:

   `Android/media/com.perceptiveus.reverie/Reverie/`

   (Exact path can vary by device; use a file manager that can see app media storage.)

2. In Reverie, open **Import** and tap **Scan Library**.
3. New files are indexed into the library.

## Import lyrics

Lyrics are **sidecar files** next to the audio file, using the **same base name**:

| Audio file        | Lyrics file (preferred) |
| ----------------- | ----------------------- |
| `Song Title.mp3`  | `Song Title.lrc`        |
| `Song Title.flac` | `Song Title.txt`        |

Also accepted in the same folder: `lyrics.lrc` or `lyrics.txt`.

### Formats

- **`.lrc`** — synced lyrics with timestamps, e.g. `[00:12.50]First line`
- **`.txt`** — plain text, one line per lyric (not timed)

### Option A — from Now Playing (recommended)

1. Play the song and open the **Player** (Now Playing).
2. Switch to the **LYRICS** tab (Premium).
3. If no lyrics are found, tap the **+** button.
4. Pick a `.lrc` or `.txt` file.
5. Reverie copies it next to the song and renames it to match the track (e.g. `My Song.lrc`).
6. Lyrics appear immediately. Invalid types or empty files show an error message.

### Option B — manually next to the file

1. Place `Song.lrc` or `Song.txt` in the same folder as `Song.mp3` (or whatever the audio file is named).
2. Open the song again on Now Playing → **LYRICS**.

### Tips

- Prefer **`.lrc`** for karaoke-style highlighting while the track plays.
- Keep files under ~512 KB.
- Encoding: UTF-8 is best; Latin-1 is tried as a fallback.
- Lyrics viewing/import from the player is a **Premium** feature.

## Where files live

Imported library root:

`Android/media/<package>/Reverie/`

Tracks are stored as normal files under that tree; lyrics sit beside them after import.
