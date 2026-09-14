package com.proxybrowser.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(val title: String, val url: String, val timestamp: Long)

/** سجل التصفح — قائمة منفصلة عن المفضلة، تُحفظ محلياً وتُقصّ لآخر 300 عنصر. */
object HistoryManager {

    private const val PREFS_NAME = "history_prefs"
    private const val KEY_ITEMS = "items"
    private const val MAX_ITEMS = 300

    fun add(context: Context, title: String, url: String) {
        if (url.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = readArray(prefs)

        // نشيل أي إدخال سابق لنفس الرابط عشان يطلع بالأعلى كأحدث زيارة
        val filtered = JSONArray()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.optString("url") != url) filtered.put(obj)
        }

        val entry = JSONObject().apply {
            put("title", title)
            put("url", url)
            put("timestamp", System.currentTimeMillis())
        }
        filtered.put(entry)

        // نبقي آخر MAX_ITEMS بس
        val trimmed = JSONArray()
        val start = (filtered.length() - MAX_ITEMS).coerceAtLeast(0)
        for (i in start until filtered.length()) {
            trimmed.put(filtered.getJSONObject(i))
        }

        prefs.edit().putString(KEY_ITEMS, trimmed.toString()).apply()
    }

    fun getAll(context: Context): List<HistoryEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = readArray(prefs)
        val list = mutableListOf<HistoryEntry>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                HistoryEntry(
                    title = obj.optString("title"),
                    url = obj.optString("url"),
                    timestamp = obj.optLong("timestamp")
                )
            )
        }
        return list.sortedByDescending { it.timestamp }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun readArray(prefs: android.content.SharedPreferences): JSONArray {
        return try {
            JSONArray(prefs.getString(KEY_ITEMS, "[]") ?: "[]")
        } catch (e: Exception) {
            JSONArray()
        }
    }
}
