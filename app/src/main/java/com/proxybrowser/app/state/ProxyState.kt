package com.proxybrowser.app.state

import android.content.Context
import com.proxybrowser.app.data.PrefsManager
import com.proxybrowser.app.model.ProxyInfo
import kotlinx.coroutines.flow.MutableStateFlow

sealed class ConnectionState {
    data class Scanning(val thresholdMs: Int) : ConnectionState()
    data class Connected(val proxy: ProxyInfo) : ConnectionState()

    /**
     * المستخدم أوقف البروكسي يدوياً: تصفح مباشر، لا فحص ولا خدمة بالخلفية،
     * ويبقى كذلك (محفوظ بالإعدادات) إلى أن يضغط "تفعيل" بنفسه.
     */
    object Disabled : ConnectionState()
}

/**
 * حالة مشتركة بين خدمة الفحص بالخلفية (ProxyScanService) وواجهة المستخدم،
 * عشان يستمر الفحص حتى لو المستخدم طلع من شاشة التطبيق.
 */
object ProxyState {
    const val START_THRESHOLD_MS = 500

    val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Scanning(START_THRESHOLD_MS))

    /** يطابق الحالة بالذاكرة مع اختيار المستخدم المحفوظ (يُستدعى عند فتح التطبيق). */
    fun syncWithPrefs(context: Context) {
        val enabled = PrefsManager.isProxyEnabled(context)
        val current = connectionState.value
        if (!enabled && current !is ConnectionState.Disabled) {
            connectionState.value = ConnectionState.Disabled
        } else if (enabled && current is ConnectionState.Disabled) {
            connectionState.value = ConnectionState.Scanning(START_THRESHOLD_MS)
        }
    }
}

/**
 * يحفظ روابط التبويبات بالذاكرة لما شاشة المتصفح تنهدم (مثلاً عند تفعيل البروكسي
 * من جديد وتمر بشاشة الفحص)، عشان ترجع نفس التبويبات بدل ما تضيع.
 */
object TabSession {
    var urls: List<String> = emptyList()
    var activeIndex: Int = 0
}
