package com.proxybrowser.app.data

import android.content.Context
import com.proxybrowser.app.model.ProxyInfo
import com.proxybrowser.app.model.ProxyType
import org.json.JSONArray
import org.json.JSONObject

/**
 * يحتفظ بآخر البروكسيات اللي اشتغلت زين، عشان يجربها الفحص الجديد أول
 * قبل ما يروح لقائمة كاملة جديدة من المصدر (اتصال أسرع بالمرات الجاية).
 */
object ProxyHistoryManager {

    private const val PREFS_NAME = "proxy_history_prefs"
    private const val KEY_ITEMS = "successful_proxies"
    private const val MAX_ITEMS = 10

    fun recordSuccess(context: Context, proxy: ProxyInfo) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = readArray(prefs)

        val filtered = JSONArray()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (!(obj.optString("host") == proxy.host && obj.optInt("port") == proxy.port)) {
                filtered.put(obj)
            }
        }

        val entry = JSONObject().apply {
            put("host", proxy.host)
            put("port", proxy.port)
            put("type", proxy.type.name)
        }
        filtered.put(entry)

        val trimmed = JSONArray()
        val start = (filtered.length() - MAX_ITEMS).coerceAtLeast(0)
        for (i in start until filtered.length()) trimmed.put(filtered.getJSONObject(i))

        prefs.edit().putString(KEY_ITEMS, trimmed.toString()).apply()
    }

    /** أحدث البروكسيات الناجحة أولاً (تُجرَّب قبل القائمة الكاملة). */
    fun getRecent(context: Context): List<ProxyInfo> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = readArray(prefs)
        val list = mutableListOf<ProxyInfo>()
        for (i in array.length() - 1 downTo 0) {
            val obj = array.getJSONObject(i)
            val type = try {
                ProxyType.valueOf(obj.optString("type"))
            } catch (e: Exception) {
                ProxyType.HTTP
            }
            list.add(ProxyInfo(host = obj.optString("host"), port = obj.optInt("port"), type = type))
        }
        return list
    }

    private fun readArray(prefs: android.content.SharedPreferences): JSONArray {
        return try {
            JSONArray(prefs.getString(KEY_ITEMS, "[]") ?: "[]")
        } catch (e: Exception) {
            JSONArray()
        }
    }
}
