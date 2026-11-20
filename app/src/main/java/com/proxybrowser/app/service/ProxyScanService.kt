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
import com.proxybrowser.app.data.PrefsManager
import com.proxybrowser.app.data.ProxyHistoryManager
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
 * تبدأ بحد سرعة 500ms، وإذا ما لقت بروكسي خلال 5 ثواني ترفع الحد 100ms
 * وتعيد المحاولة تلقائياً. بعد الاتصال يتوقف الفحص تماماً، ولا يعاود إلا
 * لو انقطع البروكسي (عبر rescan). تتوقف الخدمة نفسها تماماً لما المستخدم
 * يحذف التطبيق من صفحة التطبيقات المفتوحة (Recents).
 */
class ProxyScanService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var scanJob: Job? = null

    private val binder = LocalBinder()

    /** صار true لما المستخدم يوقف البروكسي، عشان ما يرجع إشعار أو اتصال بعد الإيقاف. */
    @Volatile
    private var stopped = false
    private var lastStatus = "جاري البحث عن بروكسي..."

    inner class LocalBinder : Binder() {
        fun getService(): ProxyScanService = this@ProxyScanService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // لازم startForeground بعد أي startForegroundService وإلا يطيح التطبيق،
        // حتى لو بنوقف الخدمة فوراً لأن المستخدم موقف البروكسي
        startForeground(NOTIFICATION_ID, buildNotification(lastStatus))
        if (!PrefsManager.isProxyEnabled(this)) {
            shutdown()
            return
        }
        startScanLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(lastStatus))
        if (!PrefsManager.isProxyEnabled(this)) {
            shutdown()
            return START_NOT_STICKY
        }
        stopped = false
        startScanLoop()
        return START_STICKY
    }

    /**
     * إيقاف كامل: يلغي الفحص ويشيل الإشعار ويوقف الخدمة نفسها.
     * يُستدعى لما المستخدم يوقف البروكسي من التطبيق.
     */
    fun shutdown() {
        stopped = true
        scanJob?.cancel()
        scanJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** يوقف كل شي تماماً (الفحص + الخدمة نفسها) لما المستخدم يحذف التطبيق من التطبيقات المفتوحة. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopped = true
        scanJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** يعيد بدء البحث من الصفر (يُستدعى لما ينقطع البروكسي أثناء التصفح). */
    fun rescan() {
        // لو البروكسي موقوف يدوياً ما نعيد الفحص أبداً
        if (stopped || !PrefsManager.isProxyEnabled(this)) return
        scanJob?.cancel()
        ProxyState.connectionState.value = ConnectionState.Scanning(START_THRESHOLD_MS)
        startScanLoop()
    }

    private fun startScanLoop() {
        if (scanJob?.isActive == true) return
        if (ProxyState.connectionState.value is ConnectionState.Connected) return

        scanJob = scope.launch {
            var threshold = START_THRESHOLD_MS
            var found: ProxyInfo? = null

            while (isActive && found == null) {
                ProxyState.connectionState.value = ConnectionState.Scanning(threshold)
                updateNotification("جاري البحث... الحد الحالي ${threshold}ms")

                // نجرب البروكسيات اللي اشتغلت زين سابقاً أول (أسرع اتصال)
                val recentGood = ProxyHistoryManager.getRecent(applicationContext)
                if (recentGood.isNotEmpty()) {
                    found = withTimeoutOrNull(RETRY_WINDOW_MS) {
                        ProxyTester.findFirstWorking(recentGood, threshold)
                    }
                }

                if (found == null) {
                    val candidates = withContext(Dispatchers.IO) {
                        ProxyFetcher.fetchAll(threshold)
                    }

                    found = withTimeoutOrNull(RETRY_WINDOW_MS) {
                        ProxyTester.findFirstWorking(candidates, threshold)
                    }
                }

                if (found == null) {
                    threshold += STEP_MS
                }
            }

            val connectedProxy = found ?: return@launch
            // لو المستخدم أوقف البروكسي أثناء الفحص ما ننشر اتصال جديد
            if (stopped || !PrefsManager.isProxyEnabled(applicationContext)) return@launch
            ProxyHistoryManager.recordSuccess(applicationContext, connectedProxy)
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
        if (stopped) return
        lastStatus = text
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
        private const val RETRY_WINDOW_MS = 5_000L
    }
}
