package com.proxybrowser.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.proxybrowser.app.data.AdBlockManager
import com.proxybrowser.app.data.DomainVisitTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * مهمة يومية بالخلفية (عبر WorkManager، تشتغل حتى لو التطبيق مقفول):
 * 1. تمسح كوكيز/كاش أي دومين ما تمت زيارته من 30 يوم.
 * 2. تحدّث قائمة حظر الإعلانات إذا مر أسبوع على آخر تحديث.
 */
class DailyMaintenanceWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            withContext(Dispatchers.Default) {
                DomainVisitTracker.cleanupExpired(applicationContext)
            }

            withContext(Dispatchers.IO) {
                if (AdBlockManager.shouldRefresh(applicationContext)) {
                    AdBlockManager.refreshBlocking(applicationContext)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
