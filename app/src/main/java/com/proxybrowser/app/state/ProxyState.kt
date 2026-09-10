package com.proxybrowser.app.state

import com.proxybrowser.app.model.ProxyInfo
import kotlinx.coroutines.flow.MutableStateFlow

sealed class ConnectionState {
    data class Scanning(val thresholdMs: Int) : ConnectionState()
    data class Connected(val proxy: ProxyInfo) : ConnectionState()
}

/**
 * حالة مشتركة بين خدمة الفحص بالخلفية (ProxyScanService) وواجهة المستخدم،
 * عشان يستمر الفحص حتى لو المستخدم طلع من شاشة التطبيق.
 */
object ProxyState {
    val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Scanning(500))
}
