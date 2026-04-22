package com.pegasus.videoplayer

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.launch
import java.io.File

/**
 * Headless Activity that receives a `pegasus-video://download` URI from Pegasus QML,
 * starts an async download coroutine, and immediately calls finish().
 *
 * URI format:
 *   pegasus-video://download?url=<ENCODED_HTTPS_URL>&gameKey=<KEY>&out=<ENCODED_JSON_PATH>
 *
 * Parameters:
 *   url     — HTTPS URL to download (YouTube page or direct MP4). Only HTTPS accepted.
 *   gameKey — sanitized key used to name the output video file (e.g. "supermarioworld_snes")
 *   out     — absolute path for the progress/completion JSON callback consumed by QML polling
 *
 * Output video written to: /storage/emulated/0/PegasusVideoScraper/trailers/<gameKey>.mp4
 * Callback JSON written to: <out> (must be inside ALLOWED_ROOT)
 *
 * Spec identical to SearchActivity (Theme.NoDisplay, singleInstance, finish() in onCreate).
 */
class DownloadActivity : Activity() {

    companion object {
        private const val TAG = "PegasusDownload"
        private const val ALLOWED_CALLBACK_ROOT     = "/storage/emulated/0/PegasusVideoScraper/trailers"
        private const val ALLOWED_CALLBACK_ROOT_ALT = "/sdcard/PegasusVideoScraper/trailers"
        private const val TRAILER_DIR               = "/storage/emulated/0/PegasusVideoScraper/trailers"
        private const val TRAILER_DIR_ALT           = "/sdcard/PegasusVideoScraper/trailers"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent?.data)
        // Cleanup stale files best-effort (doesn't block the download)
        (application as VideoPlayerApp).downloadScope.launch {
            try {
                val dir = File(TRAILER_DIR).takeIf { it.isDirectory }
                    ?: File(TRAILER_DIR_ALT).takeIf { it.isDirectory }
                    ?: return@launch
                val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                dir.listFiles()?.forEach { f ->
                    if (f.isFile && f.lastModified() < cutoff
                            && (f.name.endsWith(".json") || f.name.endsWith(".tmp"))) {
                        f.delete()
                    }
                }
            } catch (_: Exception) { /* ignore */ }
        }
        // Theme.NoDisplay: must call finish() before onCreate() returns
        finish()
    }

    private fun handle(uri: Uri?) {
        if (uri == null || !"pegasus-video".equals(uri.scheme, true) ||
            !"download".equals(uri.host, true)) {
            Log.w(TAG, "Invalid URI: $uri")
            return
        }

        val url     = uri.getQueryParameter("url")?.trim().orEmpty()
        val gameKey = uri.getQueryParameter("gameKey")?.trim().orEmpty()
        val outPath = uri.getQueryParameter("out")?.trim().orEmpty()

        // Security: only HTTPS URLs (matches UriParams constraint)
        if (!url.startsWith("https://", ignoreCase = true)) {
            Log.w(TAG, "Rejected non-HTTPS URL: $url")
            return
        }

        val sanitizedKey = sanitizeGameKey(gameKey)
        if (sanitizedKey.isEmpty()) {
            Log.w(TAG, "Rejected empty/invalid gameKey: $gameKey")
            return
        }

        val callbackFile = validateCallbackPath(outPath) ?: run {
            Log.w(TAG, "Rejected callback path: $outPath")
            return
        }

        val outVideoFile = resolveVideoFile(sanitizedKey)

        Log.i(TAG, "Download requested: url=$url key=$sanitizedKey out=${callbackFile.path}")

        val app = application as VideoPlayerApp
        app.downloadScope.launch {
            TrailerDownloader.download(
                url          = url,
                outVideoFile = outVideoFile,
                callbackFile = callbackFile
            )
        }
    }

    /**
     * Sanitizes gameKey to [a-z0-9_]* — replaces the pipe separator with underscore
     * so it can safely be used as a filename component.
     * Mirrors the QML Utils.gameKey() normalization.
     */
    private fun sanitizeGameKey(raw: String): String {
        if (raw.isEmpty() || raw.length > 200) return ""
        return raw.lowercase().replace(Regex("[^a-z0-9|]"), "").replace('|', '_')
    }

    /**
     * Prevents path traversal: callback JSON must be inside the allowlisted trailers directory.
     */
    private fun validateCallbackPath(raw: String): File? {
        if (raw.isEmpty()) return null
        val canonical = try { File(raw).canonicalFile } catch (_: Exception) { return null }
        val path = canonical.absolutePath
        val rootOk = path.startsWith("$ALLOWED_CALLBACK_ROOT/") ||
                     path.startsWith("$ALLOWED_CALLBACK_ROOT_ALT/")
        if (!rootOk) return null
        if (!path.endsWith(".json")) return null
        if (canonical.name.contains("..")) return null
        canonical.parentFile?.mkdirs()
        return canonical
    }

    /**
     * Returns the target MP4 file path for the given sanitized gameKey.
     * Example: sanitizedKey="supermarioworld_snes" → /storage/emulated/0/PegasusVideoScraper/trailers/supermarioworld_snes.mp4
     */
    private fun resolveVideoFile(sanitizedKey: String): File {
        val dir = File(TRAILER_DIR).takeIf { it.exists() || it.mkdirs() }
            ?: File(TRAILER_DIR_ALT).also { it.mkdirs() }
        return File(dir, "$sanitizedKey.mp4")
    }
}
