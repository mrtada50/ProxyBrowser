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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.proxybrowser.app.service.ProxyScanService
import com.proxybrowser.app.state.ConnectionState
import com.proxybrowser.app.state.ProxyState
import com.proxybrowser.app.state.ShortcutAction
import com.proxybrowser.app.state.ShortcutIntentState
import com.proxybrowser.app.state.ThemeState
import com.proxybrowser.app.ui.BrowserScreen
import com.proxybrowser.app.ui.ScanningScreen

class MainActivity : ComponentActivity() {

    private var scanService: ProxyScanService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ProxyScanService.LocalBinder
            scanService = binder.getService()
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            scanService = null
            bound = false
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

        // نشغّل خدمة الفحص كـ Foreground Service عشان تستمر حتى لو المستخدم طلع من التطبيق
        val serviceIntent = Intent(this, ProxyScanService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        ThemeState.init(applicationContext)
        handleShortcutIntent(intent)

        setContent {
            val isDark by ThemeState.isDarkTheme.collectAsState()
            MaterialTheme(
                colorScheme = if (isDark) darkColorScheme() else lightColorScheme()
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by ProxyState.connectionState.collectAsState()

                    when (val current = state) {
                        is ConnectionState.Scanning -> {
                            ScanningScreen(thresholdMs = current.thresholdMs)
                        }
                        is ConnectionState.Connected -> {
                            BrowserScreen(
                                proxy = current.proxy,
                                onProxyLost = { scanService?.rescan() }
                            )
                        }
                    }
                }
            }
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
