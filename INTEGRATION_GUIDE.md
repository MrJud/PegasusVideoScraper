# PegasusVideoScraper — Theme Integration Guide

A guide for Pegasus Frontend theme authors who want to add YouTube trailer search, video streaming, and offline download to their theme.

**Requirements:** Pegasus Frontend on Android, PegasusVideoScraper APK installed.
**Architecture:** URI scheme (theme → APK) + JSON file callback (APK → theme).
**No new dependencies** on the theme side — standard QML only.

> **Current compatibility:** only the ReStory theme is officially supported. Third-party theme support (custom storage paths) is planned for v1.1.

---

## 1. What you can do

| Feature | What it does | When to use |
|---|---|---|
| **Play URL** | Streams any HTTPS MP4/HLS/DASH in fullscreen | You already have the direct trailer URL |
| **Play YouTube URL** | Resolves stream via NewPipeExtractor and plays | You have a YouTube page URL, not a direct file |
| **Search YouTube** | Searches YouTube, returns a JSON result list | Trailer not available from another source |
| **Download offline** | Downloads a video to local storage | Offline playback after one-time download |

---

## 2. URI scheme contract

All interactions go through `Qt.openUrlExternally("pegasus-video://...")`. The APK is always the passive listener; the theme is the caller.

> **Note:** The current scheme is `pegasus-video://` for all Pegasus Frontend themes.

### 2.1 `play` — stream or play a video

```
pegasus-video://play?url=<URL>&title=<STR>&gameKey=<STR>
```

| Parameter | Type | Required | Notes |
|---|---|---|---|
| `url` | URL-encoded, HTTPS only | yes | MP4, HLS, DASH, or YouTube page URL |
| `title` | URL-encoded string | no | Shown in the top overlay. If YouTube and blank, the APK fetches it. |
| `gameKey` | URL-encoded, `[a-z0-9]*\|[a-z0-9]*` | recommended | `<game>\|<platform>`. Used for download file naming. |

**Response:** none — the APK opens a fullscreen Activity.

```qml
function openTrailer(url, title, gameKey) {
    if (Qt.platform.os !== "android") {
        Qt.openUrlExternally(url);   // desktop fallback: system browser/player
        return;
    }
    var uri = "pegasus-video://play"
            + "?url="     + encodeURIComponent(url)
            + "&title="   + encodeURIComponent(title || "")
            + "&gameKey=" + encodeURIComponent(gameKey || "");
    if (!Qt.openUrlExternally(uri)) {
        Qt.openUrlExternally("https://github.com/MrJud/PegasusVideoPlayer/releases/latest");
    }
}
```

### 2.2 `search` — search YouTube

```
pegasus-video://search?q=<QUERY>&out=<FILE_PATH>
```

| Parameter | Type | Required | Notes |
|---|---|---|---|
| `q` | URL-encoded, ≤200 chars | yes | YouTube search query |
| `out` | URL-encoded absolute path | yes | Where to write the JSON result. See §3. |

### 2.3 `download` — download a trailer

```
pegasus-video://download?url=<YT_URL>&gameKey=<KEY>&out=<JSON_PATH>
```

| Parameter | Type | Required | Notes |
|---|---|---|---|
| `url` | URL-encoded, HTTPS | yes | YouTube page URL to download |
| `gameKey` | URL-encoded | yes | Used to name the output MP4 file |
| `out` | URL-encoded absolute path | yes | Where to write the progress/completion JSON |

---

## 3. Allowed callback paths

The APK validates the `out` path to prevent path-traversal attacks. Only paths under these directories are accepted:

- `/storage/emulated/0/ReStory/search/`
- `/storage/emulated/0/ReStory/trailers/`

Third-party theme directories require an APK update with an extended allowlist (planned v1.1+).

**File name:** any, must end in `.json`, no `..` components.

**Recommended naming pattern:**
```
/sdcard/PegasusVideoScraper/search/yt_<timestamp>_<rnd>.json
```

---

## 4. JSON callback schema

### Search result

```json
{
  "schema": 1,
  "query": "celeste official trailer",
  "status": "ok",
  "error": null,
  "results": [
    {
      "videoId": "j7ITKxkmQcU",
      "title": "Celeste — Launch Trailer",
      "author": "Matt Makes Games",
      "durationSec": 134,
      "thumbUrl": "https://i.ytimg.com/vi/j7ITKxkmQcU/hqdefault.jpg",
      "ytPageUrl": "https://www.youtube.com/watch?v=j7ITKxkmQcU"
    }
  ]
}
```

### Download progress / completion

```json
{ "schema": 1, "action": "download", "status": "progress", "progress": 0.45 }
{ "schema": 1, "action": "download", "status": "ok", "localPath": "/sdcard/PegasusVideoScraper/trailers/celeste_snes.mp4" }
{ "schema": 1, "action": "download", "status": "error", "error": "Network timeout" }
```

**Always check `schema === 1` before parsing.** Discard unknown schemas.

---

## 5. Minimal integration — play only (10 lines QML)

```qml
function openTrailer(url, title, gameKey) {
    if (Qt.platform.os !== "android") {
        Qt.openUrlExternally(url);
        return;
    }
    var uri = "pegasus-video://play"
            + "?url="     + encodeURIComponent(url)
            + "&title="   + encodeURIComponent(title || "")
            + "&gameKey=" + encodeURIComponent(gameKey || "");
    if (!Qt.openUrlExternally(uri)) {
        Qt.openUrlExternally("https://github.com/MrJud/PegasusVideoPlayer/releases/latest");
    }
}
```

```qml
Button {
    text: "Play Trailer"
    onClicked: openTrailer(game.trailerUrl, game.title, game.id + "|" + game.platform)
}
```

That is all for the "I already have the URL" case.

---

## 6. Full integration — search + play

```qml
property int    _ytSeq:       0
property int    _ytActiveSeq: 0
property string _ytFile:      ""
property real   _ytStartedAt: 0
readonly property int _ytTimeoutMs: 10000

Timer {
    id: ytPoll
    interval: 300
    repeat: true
    onTriggered: _pollSearchResult()
}

function searchYouTube(term) {
    if (Qt.platform.os !== "android") return;
    var q = (term || "").trim();
    if (!q) return;

    _ytSeq += 1;
    var seq  = _ytSeq;
    var ts   = Date.now();
    var rnd  = Math.floor(Math.random() * 0xFFFF).toString(16);
    var path = "/sdcard/PegasusVideoScraper/search/yt_" + ts + "_" + rnd + ".json";

    _ytActiveSeq = seq;
    _ytFile      = path;
    _ytStartedAt = ts;

    var uri = "pegasus-video://search"
            + "?q="   + encodeURIComponent(q)
            + "&out=" + encodeURIComponent(path);

    if (!Qt.openUrlExternally(uri)) {
        _onSearchError("PegasusVideoScraper not installed");
        return;
    }
    ytPoll.start();
}

function _pollSearchResult() {
    if (_ytActiveSeq === 0) { ytPoll.stop(); return; }
    if (Date.now() - _ytStartedAt > _ytTimeoutMs) {
        ytPoll.stop();
        _ytActiveSeq = 0;
        _onSearchError("Search timeout");
        return;
    }
    var capturedSeq = _ytActiveSeq;
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
        if (xhr.readyState !== XMLHttpRequest.DONE) return;
        if (capturedSeq !== _ytActiveSeq) return;
        if (xhr.status !== 200 && xhr.status !== 0) return;
        if (!xhr.responseText) return;
        try {
            var p = JSON.parse(xhr.responseText);
            if (p.schema !== 1) return;   // partial write — retry next tick
            ytPoll.stop();
            _ytActiveSeq = 0;
            _onSearchResults(p);
        } catch (e) { /* partial write — retry */ }
    };
    xhr.open("GET", "file://" + _ytFile);
    xhr.send();
}

function _onSearchResults(payload) {
    if (payload.status === "error" || payload.status === "no_results"
            || !payload.results || !payload.results.length) {
        _onSearchError(payload.error || "No results");
        return;
    }
    // payload.results: array of { videoId, title, author, durationSec, thumbUrl, ytPageUrl }
    // Pass ytPageUrl to openTrailer() to stream the video
}

function _onSearchError(msg) { /* show error in your theme UI */ }
```

---

## 7. Download + progress polling

```qml
property int    _dlSeq:       0
property int    _dlActiveSeq: 0
property string _dlFile:      ""
property real   _dlStartedAt: 0
readonly property int _dlTimeoutMs: 300000  // 5 minutes

Timer {
    id: dlPoll
    interval: 500
    repeat: true
    onTriggered: _pollDownload()
}

function startDownload(ytPageUrl, gameKey) {
    if (Qt.platform.os !== "android") return;

    _dlSeq += 1;
    var seq     = _dlSeq;
    var ts      = Date.now();
    var keyFile = gameKey.replace(/\|/g, "_");
    var outPath = "/sdcard/PegasusVideoScraper/trailers/dl_" + keyFile + "_" + ts + ".json";

    _dlActiveSeq = seq;
    _dlFile      = outPath;
    _dlStartedAt = ts;

    var uri = "pegasus-video://download"
            + "?url="     + encodeURIComponent(ytPageUrl)
            + "&gameKey=" + encodeURIComponent(gameKey)
            + "&out="     + encodeURIComponent(outPath);

    if (!Qt.openUrlExternally(uri)) {
        _onDownloadError("PegasusVideoScraper not installed");
        return;
    }
    dlPoll.start();
}

function _pollDownload() {
    if (_dlActiveSeq === 0) { dlPoll.stop(); return; }
    if (Date.now() - _dlStartedAt > _dlTimeoutMs) {
        dlPoll.stop();
        _dlActiveSeq = 0;
        _onDownloadError("Download timeout");
        return;
    }
    var capturedSeq = _dlActiveSeq;
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
        if (xhr.readyState !== XMLHttpRequest.DONE) return;
        if (capturedSeq !== _dlActiveSeq) return;
        if (xhr.status !== 200 && xhr.status !== 0) return;
        if (!xhr.responseText) return;
        try {
            var p = JSON.parse(xhr.responseText);
            if (p.schema !== 1 || p.action !== "download") return;
            if (p.status === "progress") {
                _onDownloadProgress(p.progress);   // 0.0–1.0
            } else if (p.status === "ok") {
                dlPoll.stop();
                _dlActiveSeq = 0;
                _onDownloadComplete("file://" + p.localPath);
            } else if (p.status === "error") {
                dlPoll.stop();
                _dlActiveSeq = 0;
                _onDownloadError(p.error || "Download failed");
            }
        } catch (e) { /* partial write — retry */ }
    };
    xhr.open("GET", "file://" + _dlFile);
    xhr.send();
}

function _onDownloadProgress(pct) { /* update progress bar: Math.round(pct * 100) + "%" */ }
function _onDownloadComplete(fileUrl) { /* store fileUrl in your custom video map */ }
function _onDownloadError(msg) { /* show error in your theme UI */ }
```

---

## 8. APK not installed — handling

`Qt.openUrlExternally()` returns `false` if no app is registered for the scheme. Always handle this:

```qml
if (!Qt.openUrlExternally(uri)) {
    // Show install prompt
    Qt.openUrlExternally("https://github.com/MrJud/PegasusVideoPlayer/releases/latest");
}
```

Do **not** assume the APK is installed. There is no reliable way to check from QML before calling.

---

## 9. First-launch permission

On first run the APK requests `MANAGE_EXTERNAL_STORAGE` (required to write to `/sdcard/`):

1. User installs the APK.
2. User opens it **once** from the launcher → grants "All files access".
3. All subsequent calls from QML work without further user interaction.

---

## 10. Best practices

| # | Practice | Why |
|---|---|---|
| 1 | Debounce search input ≥300ms | Avoids launching a SearchActivity per keystroke |
| 2 | Cache results in-memory (key = normalized query, TTL 15min) | Repeating the same search is instant |
| 3 | Invalidate seq number when user changes query mid-search | Prevents applying stale results |
| 4 | Stop Timer when user closes the page | Avoids zombie polling |
| 5 | Reject payloads with `schema !== 1` | Forward-compatible with future schema bumps |
| 6 | Sanitize `gameKey` to `[a-z0-9]*\|[a-z0-9]*` | APK uses it for file naming |
| 7 | HTTPS only in `play` URLs | APK rejects `http://` for security |
| 8 | Clean up stale callback files >1h | Avoids storage accumulation |

---

## 11. Troubleshooting

| Problem | Diagnosis | Fix |
|---|---|---|
| `Qt.openUrlExternally` always returns false | APK not installed or scheme not registered | Reinstall APK, verify manifest intent-filter |
| Activity opens but closes immediately | Invalid URL (http://) or unreachable file | Validate URL on the QML side |
| Callback JSON never appears | Storage permission denied or path not in APK allowlist | Complete first-launch, use paths under `/sdcard/ReStory/` |
| JSON present but `status: "error"` | NewPipeExtractor error | Check `error` field; wait for APK update if persistent |
| Search times out consistently | APK killed after finish() (low-memory device) | Increase timeout to 15s in the poll Timer |

---

## 12. Known limitations

- The theme cannot detect APK installation before calling — `Qt.openUrlExternally` return value is the only indicator.
- A search cannot be cancelled server-side — the old SearchActivity completes and writes its file. Use seq-id to ignore it.
- No streaming progress for playback — only download exposes a progress callback.
- Callback files on `/sdcard/` require `MANAGE_EXTERNAL_STORAGE`. Unavoidable until the APK uses a content URI scheme.

---

## 13. Contract versions

| Component | Version |
|---|---|
| URI scheme | `pegasus-video://` v1 |
| JSON callback schema | `1` |
| APK minimum version | 1.0.0 |

Breaking changes will be documented in [MAINTENANCE.md](MAINTENANCE.md) with a deprecation period. Subscribe to release notifications on GitHub.


