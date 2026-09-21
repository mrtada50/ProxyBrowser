package com.proxybrowser.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class FavoriteItem(val title: String, val url: String)

/**
 * تخزين محلي بسيط (SharedPreferences) للصفحة الرئيسية، المفضلة، والثيم.
 */
object PrefsManager {

    private const val PREFS_NAME = "proxy_browser_prefs"
    private const val KEY_HOMEPAGE = "homepage"
    private const val KEY_FAVORITES = "favorites_v2"
    private const val KEY_DARK_THEME = "dark_theme"
    private const val KEY_TEXT_ZOOM = "text_zoom"
    private const val KEY_DATA_SAVER = "data_saver"
    private const val KEY_WHATS_NEW_SEEN = "whats_new_seen"
    private const val KEY_SUPPRESS_LOGIN_WARNING = "suppress_login_warning"
    private const val DEFAULT_HOMEPAGE = "https://www.google.com"

    fun getHomepage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_HOMEPAGE, DEFAULT_HOMEPAGE) ?: DEFAULT_HOMEPAGE
    }

    fun setHomepage(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HOMEPAGE, url).apply()
    }

    fun isDarkTheme(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_THEME, false)
    }

    fun setDarkTheme(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
    }

    fun getTextZoom(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_TEXT_ZOOM, 100)
    }

    fun setTextZoom(context: Context, zoom: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_TEXT_ZOOM, zoom).apply()
    }

    fun isDataSaverOn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DATA_SAVER, false)
    }

    fun setDataSaverOn(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DATA_SAVER, enabled).apply()
    }

    /** رقم يزيد يدوياً بكل دفعة ميزات جديدة، لعرض نافذة "الجديد بالتحديث" مرة وحدة. */
    const val CURRENT_WHATS_NEW_VERSION = 3

    fun shouldShowWhatsNew(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_WHATS_NEW_SEEN, 0) < CURRENT_WHATS_NEW_VERSION
    }

    fun markWhatsNewSeen(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_WHATS_NEW_SEEN, CURRENT_WHATS_NEW_VERSION).apply()
    }

    fun getSensitiveLoginWarningSuppressed(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SUPPRESS_LOGIN_WARNING, false)
    }

    fun setSensitiveLoginWarningSuppressed(context: Context, suppressed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SUPPRESS_LOGIN_WARNING, suppressed).apply()
    }

    fun getFavorites(context: Context): List<FavoriteItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map {
                val obj = array.getJSONObject(it)
                FavoriteItem(obj.optString("title"), obj.optString("url"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addFavorite(context: Context, title: String, url: String) {
        if (url.isBlank()) return
        val current = getFavorites(context).toMutableList()
        if (current.none { it.url == url }) {
            current.add(FavoriteItem(title.ifBlank { url }, url))
            saveFavorites(context, current)
        }
    }

    fun removeFavorite(context: Context, url: String) {
        val current = getFavorites(context).toMutableList()
        current.removeAll { it.url == url }
        saveFavorites(context, current)
    }

    fun updateFavorite(context: Context, oldUrl: String, newTitle: String, newUrl: String) {
        val current = getFavorites(context).toMutableList()
        val index = current.indexOfFirst { it.url == oldUrl }
        if (index >= 0) {
            current[index] = FavoriteItem(newTitle.ifBlank { newUrl }, newUrl)
            saveFavorites(context, current)
        }
    }

    private fun saveFavorites(context: Context, list: List<FavoriteItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        list.forEach {
            array.put(JSONObject().apply {
                put("title", it.title)
                put("url", it.url)
            })
        }
        prefs.edit().putString(KEY_FAVORITES, array.toString()).apply()
    }
}
