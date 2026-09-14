package com.proxybrowser.app.state

import android.content.Context
import com.proxybrowser.app.data.PrefsManager
import kotlinx.coroutines.flow.MutableStateFlow

/** حالة الثيم (داكن/فاتح) مشتركة بين MainActivity وشاشة المتصفح. */
object ThemeState {
    val isDarkTheme = MutableStateFlow(false)

    fun init(context: Context) {
        isDarkTheme.value = PrefsManager.isDarkTheme(context)
    }

    fun toggle(context: Context) {
        val newValue = !isDarkTheme.value
        isDarkTheme.value = newValue
        PrefsManager.setDarkTheme(context, newValue)
    }
}
