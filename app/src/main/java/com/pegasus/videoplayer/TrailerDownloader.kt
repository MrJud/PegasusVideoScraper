package com.pegasus.videoplayer

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "TrailerDownloader"

/**
 * Downloads a video trailer (YouTube or direct MP4) to a local file on /sdcard/.
 *
 * For YouTube URLs, stream resolution is delegated to [YouTubeResolver] (NewPipe extractor).
 * For direct HTTPS URLs (Steam CDN, etc.) the URL is downloaded as-is.
 *
 * Progress is written every ~2 seconds via [DownloadCallback.atomicWrite] so the QML
 * polling loop can update its UI. On completion or error, the final JSON is written and
 * the coroutine returns.
 */
object TrailerDownloader {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    /**
     * @param url           YouTube page URL or direct HTTPS MP4 URL.
     * @param outVideoFile  Target local file (will be created/overwritten).
     * @param callbackFile  JSON file QML polls for progress/completion.
     */
    suspend fun download(
        url: String,
        outVideoFile: File,
        callbackFile: File
    ) = withContext(Dispatchers.IO) {

        // Resolve actual downloadable URL
        val directUrl = try {
            if (YouTubeUrlDetector.isYouTube(url)) {
                Log.i(TAG, "Resolving YouTube URL: $url")
                val streams = YouTubeResolver.resolve(url)
                // Prefer progressive (single-file); DASH streams require muxing — not supported here
                streams.progressive?.url
                    ?: throw IOException("No progressive stream found for YouTube video — DASH not supported for download")
            } else {
                url
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stream resolution failed", e)
            DownloadCallback.atomicWrite(callbackFile, DownloadCallback.errorJson(e.message ?: "Resolution failed"))
            return@withContext
        }

        // Ensure output directory exists
        outVideoFile.parentFile?.mkdirs()

        val request = Request.Builder()
            .url(directUrl)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13; Odin2) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
            )
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                DownloadCallback.atomicWrite(callbackFile, DownloadCallback.errorJson("HTTP ${response.code}"))
                return@withContext
            }

            val body = response.body ?: run {
                DownloadCallback.atomicWrite(callbackFile, DownloadCallback.errorJson("Empty response body"))
                return@withContext
            }

            val contentLength = body.contentLength()   // -1 if unknown
            var bytesRead = 0L
            var lastProgressWrite = System.currentTimeMillis()
            val buffer = ByteArray(65_536)              // 64 KB chunks

            // Write initial progress
            DownloadCallback.atomicWrite(callbackFile, DownloadCallback.progressJson(0f))

            outVideoFile.outputStream().buffered().use { out ->
                body.byteStream().use { input ->
                    while (isActive) {
                        val n = input.read(buffer)
                        if (n == -1) break
                        out.write(buffer, 0, n)
                        bytesRead += n

                        // Write progress every ~2 seconds
                        val now = System.currentTimeMillis()
                        if (now - lastProgressWrite >= 2_000) {
                            lastProgressWrite = now
                            val progress = if (contentLength > 0) bytesRead.toFloat() / contentLength else 0f
                            DownloadCallback.atomicWrite(callbackFile, DownloadCallback.progressJson(progress))
                            Log.d(TAG, "Progress: $bytesRead / $contentLength bytes")
                        }
                    }
                }
            }

            if (!isActive) {
                // Coroutine was cancelled — delete incomplete file
                outVideoFile.delete()
                Log.i(TAG, "Download cancelled, file removed: ${outVideoFile.path}")
                return@withContext
            }

            Log.i(TAG, "Download complete: ${outVideoFile.path} ($bytesRead bytes)")
            DownloadCallback.atomicWrite(callbackFile, DownloadCallback.okJson(outVideoFile.absolutePath))

        } catch (e: IOException) {
            Log.e(TAG, "Download IO error", e)
            outVideoFile.delete()
            DownloadCallback.atomicWrite(callbackFile, DownloadCallback.errorJson(e.message ?: "IO error"))
        }
    }
}
