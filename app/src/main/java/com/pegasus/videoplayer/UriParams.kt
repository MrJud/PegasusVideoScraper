package com.pegasus.videoplayer

import android.net.Uri

data class UriParams(
    val url: String,
    val title: String,
    val gameKey: String,
    val downloadRequested: Boolean
) {
    companion object {
        fun fromIntentData(data: Uri?): UriParams? {
            if (data == null) return null
            if (!"pegasus-video".equals(data.scheme, ignoreCase = true)) return null

            val url = data.getQueryParameter("url")?.trim().orEmpty()
            if (url.isEmpty()) return null
            // Sicurezza: solo HTTPS, rifiuta http:// e file://
            if (!url.startsWith("https://", ignoreCase = true)) return null

            return UriParams(
                url              = url,
                title            = data.getQueryParameter("title").orEmpty(),
                gameKey          = data.getQueryParameter("gameKey").orEmpty(),
                downloadRequested = data.getQueryParameter("download") == "true"
            )
        }
    }
}
