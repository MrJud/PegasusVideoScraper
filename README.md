# PegasusVideoScraper

A companion app for **Pegasus Frontend** themes that enables YouTube trailer search, video streaming, and offline trailer download — entirely from QML, with no native code required in the theme.

> **Current compatibility:** [ReStory theme](https://github.com/MrJud/ReStory) only.
> Windows and Linux builds are planned for a future release.

---

## What it does

| Feature | Description |
|---|---|
| **Stream any video** | Opens a fullscreen player for any HTTPS MP4, HLS, DASH, or YouTube URL |
| **Search YouTube** | Headless search via NewPipeExtractor — results delivered as a JSON callback file |
| **Download trailers** | Downloads a YouTube video to local storage; QML polls a progress JSON until done |

All interactions happen through a custom URI scheme (`pegasus-video://`). The theme calls `Qt.openUrlExternally()`, the app handles it silently or opens a fullscreen player, and results are returned via JSON files on shared storage (`/sdcard/PegasusVideoScraper/`).

---

## Requirements

- Android 8.0+ (API 26+), ARM64
- Pegasus Frontend installed on the same device
- `MANAGE_EXTERNAL_STORAGE` permission granted on first launch

---

## Installation

1. Download the latest APK from [Releases](../../releases/latest).
2. Install it on your device (`adb install -r -g pegasus-video-scraper.apk`).
3. Open the app once from the launcher and grant **"All files access"** when prompted.
4. Done — the theme can now invoke it via URI.

---

## URI scheme

### `pegasus-video://play` — stream or play a video

```
pegasus-video://play?url=<HTTPS_URL>&title=<TITLE>&gameKey=<KEY>
```

| Parameter | Required | Notes |
|---|---|---|
| `url` | yes | HTTPS only. Supports MP4, HLS, DASH, YouTube page URL |
| `title` | no | Shown in the top overlay bar |
| `gameKey` | recommended | `<game>\|<platform>` normalized to `[a-z0-9]`. Used for download file naming |

### `pegasus-video://search` — search YouTube

```
pegasus-video://search?q=<QUERY>&out=<JSON_PATH>
```

### `pegasus-video://download` — download a trailer

```
pegasus-video://download?url=<YT_URL>&gameKey=<KEY>&out=<JSON_PATH>
```

Results are written to `out` as a JSON file. See [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md) for the full schema and QML polling pattern.

---

## Building from source

```bash
# Debug build
.\gradlew.bat assembleDebug

# Install on connected device
adb install -r -g app\build\outputs\apk\debug\app-debug.apk
```

Requirements: Android Studio Meerkat+, SDK 35, JDK 17.

---

## Theme integration

See [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md) for:
- Minimal 10-line QML snippet (play only)
- Full search + polling pattern
- Download + progress callback pattern
- Error handling and best practices

---

## Roadmap

- [ ] Windows build (native URI handler via registry)
- [ ] Linux build (via `.desktop` URI scheme)
- [ ] Expand storage allowlist for third-party themes
- [ ] Release signing + Play Store (internal track)

---

## License

MIT
