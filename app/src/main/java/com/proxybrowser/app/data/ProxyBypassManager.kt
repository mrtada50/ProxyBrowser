package com.proxybrowser.app.data

import android.content.Context

/**
 * يدير قائمة المواقع اللي تتصفح مباشرة بدون بروكسي (Bypass)، بالاعتماد على
 * addBypassRule الرسمية بمكتبة androidx.webkit (تدعمها WebView نفسها).
 */
object ProxyBypassManager {

    private const val PREFS_NAME = "proxy_bypass_prefs"
    private const val KEY_BYPASS = "bypass_hosts"

    fun getAll(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_BYPASS, emptySet()) ?: emptySet()
    }

    fun isBypassed(context: Context, host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        return getAll(context).contains(host.lowercase())
    }

    fun toggleBypass(context: Context, host: String?) {
        if (host.isNullOrBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getAll(context).toMutableSet()
        val key = host.lowercase()
        if (current.contains(key)) current.remove(key) else current.add(key)
        prefs.edit().putStringSet(KEY_BYPASS, current).apply()
    }

    /** يبني قواعد bypass لدومين معين (الدومين نفسه + كل الفروع الفرعية تحته). */
    fun buildBypassRules(host: String): List<String> {
        return listOf(host, "*.$host")
    }
}
