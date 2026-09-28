package com.proxybrowser.app.data

import android.content.Context
import org.json.JSONArray

/** تخصيص واجهة المتصفح: ترتيب/إخفاء أزرار شريط الأدوات، مكان وشكل شريط العنوان، وحجم العناصر. */
object UiPrefsManager {

    private const val PREFS_NAME = "ui_prefs"
    private const val KEY_ADDRESS_BAR_POSITION = "address_bar_position" // "top" / "bottom"
    private const val KEY_ADDRESS_BAR_SHAPE = "address_bar_shape" // "rounded" / "square"
    private const val KEY_UI_SCALE = "ui_scale" // "compact" / "normal" / "comfortable"
    private const val KEY_TOOLBAR_ORDER = "toolbar_order"
    private const val KEY_TOOLBAR_HIDDEN = "toolbar_hidden"

    /** الترتيب الافتراضي لأزرار شريط الأدوات القابلة للتخصيص. */
    val DEFAULT_TOOLBAR_ORDER = listOf("back", "forward", "refresh", "home", "star", "favorites")

    val TOOLBAR_LABELS = mapOf(
        "back" to "رجوع",
        "forward" to "تقدم",
        "refresh" to "تحديث",
        "home" to "الرئيسية",
        "star" to "إضافة للمفضلة",
        "favorites" to "المفضلة"
    )

    fun getAddressBarPosition(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ADDRESS_BAR_POSITION, "top") ?: "top"
    }

    fun setAddressBarPosition(context: Context, position: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ADDRESS_BAR_POSITION, position).apply()
    }

    fun getAddressBarShape(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ADDRESS_BAR_SHAPE, "rounded") ?: "rounded"
    }

    fun setAddressBarShape(context: Context, shape: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ADDRESS_BAR_SHAPE, shape).apply()
    }

    fun getUiScale(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_UI_SCALE, "normal") ?: "normal"
    }

    fun setUiScale(context: Context, scale: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_UI_SCALE, scale).apply()
    }

    fun getToolbarOrder(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TOOLBAR_ORDER, null) ?: return DEFAULT_TOOLBAR_ORDER
        return try {
            val array = JSONArray(raw)
            val list = (0 until array.length()).map { array.getString(it) }
            // نضيف أي عنصر جديد أضيف بتحديث لاحق ما كان موجود بترتيب محفوظ قديم
            val missing = DEFAULT_TOOLBAR_ORDER.filter { it !in list }
            list + missing
        } catch (e: Exception) {
            DEFAULT_TOOLBAR_ORDER
        }
    }

    fun setToolbarOrder(context: Context, order: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        order.forEach { array.put(it) }
        prefs.edit().putString(KEY_TOOLBAR_ORDER, array.toString()).apply()
    }

    fun getToolbarHidden(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_TOOLBAR_HIDDEN, emptySet()) ?: emptySet()
    }

    fun setToolbarHidden(context: Context, hidden: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_TOOLBAR_HIDDEN, hidden).apply()
    }
}
