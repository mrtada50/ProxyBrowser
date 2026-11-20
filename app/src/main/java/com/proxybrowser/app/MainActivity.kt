package com.proxybrowser.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.proxybrowser.app.data.PrefsManager
import com.proxybrowser.app.service.ProxyScanService
import com.proxybrowser.app.state.ConnectionState
import com.proxybrowser.app.state.ProxyState
import com.proxybrowser.app.state.ShortcutAction
import com.proxybrowser.app.state.ShortcutIntentState
import com.proxybrowser.app.state.TabSession
import com.proxybrowser.app.state.ThemeState
import com.proxybrowser.app.ui.BrowserScreen
import com.proxybrowser.app.ui.ScanningScreen
import com.proxybrowser.app.ui.theme.ProxyBrowserTheme

class MainActivity : ComponentActivity() {

    private var scanService: ProxyScanService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ProxyScanService.LocalBinder
            scanService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            scanService = null
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val mediaPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // نطلب صلاحيات الكاميرا/المايكروفون على مستوى النظام مرة وحدة، عشان
        // موافقة المستخدم على طلب موقع معين داخل المتصفح تشتغل فعلياً
        mediaPermissionsLauncher.launch(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        )

        // نطابق الحالة مع اختيار المستخدم المحفوظ: لو كان موقف البروكسي يدوياً
        // ما نشغّل أي فحص أو خدمة، ونروح مباشرة للمتصفح باتصال مباشر
        // فتح جديد للتطبيق (مو مجرد تبديل حالة) يبدأ بتبويب نظيف مثل قبل
        if (savedInstanceState == null) TabSession.urls = emptyList()
        ProxyState.syncWithPrefs(applicationContext)
        if (PrefsManager.isProxyEnabled(applicationContext)) {
            startProxyService()
        }

        ThemeState.init(applicationContext)
        handleShortcutIntent(intent)

        setContent {
            val isDark by ThemeState.isDarkTheme.collectAsState()
            ProxyBrowserTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by ProxyState.connectionState.collectAsState()

                    when (val current = state) {
                        is ConnectionState.Scanning -> {
                            ScanningScreen(
                                thresholdMs = current.thresholdMs,
                                onDisableProxy = { setProxyEnabled(false) }
                            )
                        }
                        else -> {
                            // نفس موضع BrowserScreen بحالتي (متصل / موقوف) عشان التبويبات
                            // ما تضيع لما المستخدم يوقف البروكسي أو يبدّل بين الحالتين
                            BrowserScreen(
                                proxy = (current as? ConnectionState.Connected)?.proxy,
                                onProxyLost = { scanService?.rescan() },
                                onSetProxyEnabled = { enabled -> setProxyEnabled(enabled) }
                            )
                        }
                    }
                }
            }
        }
    }

    /** يشغّل خدمة الفحص كـ Foreground Service ويربط بها (تستمر حتى لو طلع المستخدم من التطبيق). */
    private fun startProxyService() {
        val serviceIntent = Intent(this, ProxyScanService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        if (!bound) {
            bound = bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    /** يوقف الفحص والخدمة تماماً (ويشيل الإشعار). */
    private fun stopProxyService() {
        scanService?.shutdown()
        if (bound) {
            try {
                unbindService(connection)
            } catch (e: IllegalArgumentException) {
                // الربط مو موجود أصلاً — نتجاهل
            }
            bound = false
        }
        scanService = null
        stopService(Intent(this, ProxyScanService::class.java))
    }

    /**
     * تشغيل/إيقاف البروكسي بالكامل. الاختيار يتحفظ، فلو أوقفه المستخدم يبقى موقوفاً
     * حتى بعد إغلاق التطبيق، ولا يرجع يشتغل إلا لما يضغط "تفعيل" بنفسه.
     */
    private fun setProxyEnabled(enabled: Boolean) {
        PrefsManager.setProxyEnabled(applicationContext, enabled)
        if (enabled) {
            ProxyState.connectionState.value = ConnectionState.Scanning(ProxyState.START_THRESHOLD_MS)
            startProxyService()
        } else {
            stopProxyService()
            ProxyState.connectionState.value = ConnectionState.Disabled
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
    }

    private fun handleShortcutIntent(intent: Intent?) {
        val data = intent?.data ?: return
        when (data.host) {
            "new_tab" -> ShortcutIntentState.pendingAction.value = ShortcutAction.NewTab
            "homepage" -> ShortcutIntentState.pendingAction.value = ShortcutAction.Homepage
            "open" -> {
                val url = data.getQueryParameter("url")
                if (!url.isNullOrBlank()) {
                    ShortcutIntentState.pendingAction.value = ShortcutAction.OpenUrl(url)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }
}
