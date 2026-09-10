package com.proxybrowser.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.proxybrowser.app.MainActivity
import com.proxybrowser.app.model.ProxyInfo
import com.proxybrowser.app.network.ProxyFetcher
import com.proxybrowser.app.network.ProxyTester
import com.proxybrowser.app.state.ConnectionState
import com.proxybrowser.app.state.ProxyState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * خدمة أمامية (Foreground Service) تسوي الفحص المستمر بالخلفية:
 * تبدأ بحد سرعة 500ms، وإذا ما لقت بروكسي خلال 10 ثواني ترفع الحد 100ms
 * وتعيد المحاولة تلقائياً بدون توقف لين تلقى بروكسي شغال.
 * تستمر حتى لو المستخدم طلع من شاشة التطبيق، لأنها Foreground Service
 * مع إشعار دائم (مطلوب من أندرويد لأي عمل شبكة مستمر بالخلفية).
 */
class ProxyScanService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var scanJob: Job? = null

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): ProxyScanService = this@ProxyScanService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("جاري البحث عن بروكسي..."))
        startScanLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startScanLoop()
        return START_STICKY
    }

    /** يعيد بدء البحث من الصفر (يُستدعى لما ينقطع البروكسي أثناء التصفح). */
    fun rescan() {
        scanJob?.cancel()
        ProxyState.connectionState.value = ConnectionState.Scanning(START_THRESHOLD_MS)
        startScanLoop()
    }

    private fun startScanLoop() {
        if (scanJob?.isActive == true) return

        scanJob = scope.launch {
            var threshold = START_THRESHOLD_MS
            var found: ProxyInfo? = null

            while (isActive && found == null) {
                ProxyState.connectionState.value = ConnectionState.Scanning(threshold)
                updateNotification("جاري البحث... الحد الحالي ${threshold}ms")

                val candidates = withContext(Dispatchers.IO) {
                    ProxyFetcher.fetchAll(threshold)
                }

                found = withTimeoutOrNull(RETRY_WINDOW_MS) {
                    ProxyTester.findFirstWorking(candidates, threshold)
                }

                if (found == null) {
                    threshold += STEP_MS
                }
            }

            val connectedProxy = found ?: return@launch
            ProxyState.connectionState.value = ConnectionState.Connected(connectedProxy)
            updateNotification("متصل: ${connectedProxy.host}:${connectedProxy.port} • ${connectedProxy.latencyMs}ms")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "فحص البروكسي",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Proxy Browser")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val CHANNEL_ID = "proxy_scan_channel"
        private const val NOTIFICATION_ID = 1
        private const val START_THRESHOLD_MS = 500
        private const val STEP_MS = 100
        private const val RETRY_WINDOW_MS = 10_000L
    }
}
