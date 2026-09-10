package com.proxybrowser.app.data

import android.content.Context
import org.json.JSONArray

/**
 * تخزين محلي بسيط (SharedPreferences) للصفحة الرئيسية وقائمة المفضلة.
 */
object PrefsManager {

    private const val PREFS_NAME = "proxy_browser_prefs"
    private const val KEY_HOMEPAGE = "homepage"
    private const val KEY_FAVORITES = "favorites"
    private const val DEFAULT_HOMEPAGE = "https://www.google.com"

    fun getHomepage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_HOMEPAGE, DEFAULT_HOMEPAGE) ?: DEFAULT_HOMEPAGE
    }

    fun setHomepage(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HOMEPAGE, url).apply()
    }

    fun getFavorites(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addFavorite(context: Context, url: String) {
        if (url.isBlank()) return
        val current = getFavorites(context).toMutableList()
        if (!current.contains(url)) {
            current.add(url)
            saveFavorites(context, current)
        }
    }

    fun removeFavorite(context: Context, url: String) {
        val current = getFavorites(context).toMutableList()
        current.remove(url)
        saveFavorites(context, current)
    }

    private fun saveFavorites(context: Context, list: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        list.forEach { array.put(it) }
        prefs.edit().putString(KEY_FAVORITES, array.toString()).apply()
    }
}
