package com.pegasus.videoplayer

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.launch
import java.io.File

class SearchActivity : Activity() {

    companion object {
        private const val TAG = "PegasusSearch"
        private const val ALLOWED_ROOT = "/storage/emulated/0/PegasusVideoScraper/search"
        // Alias — some devices expose /sdcard as a different symlink
        private const val ALLOWED_ROOT_ALT = "/sdcard/PegasusVideoScraper/search"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent?.data)
        // Cleanup stale files best-effort (non blocca la ricerca)
        (application as VideoPlayerApp).searchScope.launch {
            try {
                val root = File(ALLOWED_ROOT).takeIf { it.isDirectory }
                    ?: File(ALLOWED_ROOT_ALT).takeIf { it.isDirectory }
                    ?: return@launch
                val cutoff = System.currentTimeMillis() - 60 * 60 * 1000L
                root.listFiles()?.forEach { f ->
                    if (f.isFile && f.lastModified() < cutoff) f.delete()
                }
            } catch (_: Exception) { /* ignore */ }
        }
        // NoDisplay richiede finish() prima che onCreate ritorni
        finish()
    }

    private fun handle(uri: Uri?) {
        if (uri == null || !"pegasus-video".equals(uri.scheme, true) ||
            !"search".equals(uri.host, true)) {
            Log.w(TAG, "Invalid URI: $uri")
            return
        }

        val query = uri.getQueryParameter("q")?.trim().orEmpty()
        val outPath = uri.getQueryParameter("out")?.trim().orEmpty()

        if (query.isEmpty() || query.length > 200) {
            Log.w(TAG, "Invalid query: len=${query.length}")
            return
        }

        val outFile = validateOutPath(outPath) ?: run {
            Log.w(TAG, "Rejected callback path: $outPath")
            return
        }

        val app = application as VideoPlayerApp
        app.searchScope.launch {
            val json = try {
                val items = YouTubeSearcher.search(query, limit = 20)
                SearchCallback.okJson(query, items)
            } catch (t: Throwable) {
                Log.e(TAG, "Search failed", t)
                SearchCallback.errorJson(query, t.message ?: "unknown")
            }
            SearchCallback.atomicWrite(outFile, json)
            Log.i(TAG, "Search done: q='$query' → $outFile (${json.length}b)")
        }
    }

    /**
     * Prevents path traversal: write is only permitted inside the allowlisted search directory.
     * Accetta solo file .json dentro la directory allowlist.
     */
    private fun validateOutPath(raw: String): File? {
        if (raw.isEmpty()) return null
        val canonical = try { File(raw).canonicalFile } catch (_: Exception) { return null }
        val path = canonical.absolutePath
        val rootOk = path.startsWith("$ALLOWED_ROOT/") || path.startsWith("$ALLOWED_ROOT_ALT/")
        if (!rootOk) return null
        if (!path.endsWith(".json")) return null
        if (canonical.name.contains("..")) return null
        canonical.parentFile?.mkdirs()
        return canonical
    }
}
