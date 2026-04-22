package com.pegasus.videoplayer

import org.json.JSONObject
import java.io.File

/**
 * Builds and atomically writes JSON callback files consumed by the QML polling loop.
 *
 * Schema (schema version 1):
 *   { "schema": 1, "action": "download", "status": "ok|error|progress",
 *     "localPath": "/storage/emulated/0/ReStory/trailers/<key>.mp4",  // "ok" only
 *     "progress":  0.75,                                              // "progress" only
 *     "error":     "message"                                          // "error" only
 *   }
 */
object DownloadCallback {

    fun progressJson(progress: Float): String =
        JSONObject().apply {
            put("schema", 1)
            put("action", "download")
            put("status", "progress")
            put("progress", progress.coerceIn(0f, 1f).toDouble())
        }.toString()

    fun okJson(localPath: String): String =
        JSONObject().apply {
            put("schema", 1)
            put("action", "download")
            put("status", "ok")
            put("localPath", localPath)
        }.toString()

    fun errorJson(message: String): String =
        JSONObject().apply {
            put("schema", 1)
            put("action", "download")
            put("status", "error")
            put("error", message.take(500))
        }.toString()

    /**
     * Atomic write via temp-file rename — prevents QML from reading a partial JSON.
     * Mirrors SearchCallback.atomicWrite().
     */
    fun atomicWrite(target: File, content: String) {
        val tmp = File(target.parentFile, "${target.name}.tmp")
        tmp.writeText(content, Charsets.UTF_8)
        if (!tmp.renameTo(target)) {
            target.writeText(content, Charsets.UTF_8)
            tmp.delete()
        }
    }
}
