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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.proxybrowser.app.service.ProxyScanService
import com.proxybrowser.app.state.ConnectionState
import com.proxybrowser.app.state.ProxyState
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // نشغّل خدمة الفحص كـ Foreground Service عشان تستمر حتى لو المستخدم طلع من التطبيق
        val serviceIntent = Intent(this, ProxyScanService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        setContent {
            MaterialTheme {
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

    override fun onDestroy() {
        super.onDestroy()
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }
}
