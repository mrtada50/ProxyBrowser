package com.proxybrowser.app

import android.app.Application
import android.webkit.CookieManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.proxybrowser.app.worker.DailyMaintenanceWorker
import java.util.concurrent.TimeUnit

class ProxyBrowserApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // حفظ الكوكيز دائم لكل المواقع
        CookieManager.getInstance().setAcceptCookie(true)

        scheduleDailyMaintenance()
    }

    private fun scheduleDailyMaintenance() {
        val request = PeriodicWorkRequestBuilder<DailyMaintenanceWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_maintenance",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
