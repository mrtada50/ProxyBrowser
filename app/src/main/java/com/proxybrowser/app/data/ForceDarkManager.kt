package com.proxybrowser.app.data

import android.content.Context

/** يدير تفعيل الوضع الداكن القسري على محتوى المواقع، مع استثناءات لمواقع معينة. */
object ForceDarkManager {

    private const val PREFS_NAME = "force_dark_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_EXEMPT = "exempt_hosts"

    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isSiteExempt(context: Context, host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_EXEMPT, emptySet())?.contains(host.lowercase()) == true
    }

    fun toggleSiteExemption(context: Context, host: String?) {
        if (host.isNullOrBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_EXEMPT, emptySet())?.toMutableSet() ?: mutableSetOf()
        val key = host.lowercase()
        if (current.contains(key)) current.remove(key) else current.add(key)
        prefs.edit().putStringSet(KEY_EXEMPT, current).apply()
    }
}
