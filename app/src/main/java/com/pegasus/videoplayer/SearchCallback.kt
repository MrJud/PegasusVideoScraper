package com.pegasus.videoplayer

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object SearchCallback {

    fun okJson(query: String, items: List<YouTubeSearcher.Result>): String {
        val arr = JSONArray()
        items.forEach { r ->
            arr.put(JSONObject().apply {
                put("videoId", r.videoId)
                put("title", r.title)
                put("author", r.author)
                put("durationSec", r.durationSec)
                put("thumbUrl", r.thumbUrl)
                put("ytPageUrl", "https://www.youtube.com/watch?v=${r.videoId}")
            })
        }
        val status = if (items.isEmpty()) "no_results" else "ok"
        return JSONObject().apply {
            put("schema", 1)
            put("query", query)
            put("status", status)
            put("results", arr)
        }.toString()
    }

    fun errorJson(query: String, message: String): String =
        JSONObject().apply {
            put("schema", 1)
            put("query", query)
            put("status", "error")
            put("error", message.take(500))
            put("results", JSONArray())
        }.toString()

    /**
     * Scrittura atomica: write tmp → rename.
     * Impedisce a QML di leggere un JSON parziale.
     */
    fun atomicWrite(target: File, content: String) {
        val tmp = File(target.parentFile, "${target.name}.tmp")
        tmp.writeText(content, Charsets.UTF_8)
        if (!tmp.renameTo(target)) {
            // fallback: copy+delete
            target.writeText(content, Charsets.UTF_8)
            tmp.delete()
        }
    }
}
