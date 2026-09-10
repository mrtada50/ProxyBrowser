package com.proxybrowser.app.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebStorage
import org.json.JSONObject
import java.util.concurrent.CountDownLatch

/**
 * يسجّل آخر زيارة لكل دومين، ويمسح كوكيز وكاش أي دومين ما تمت زيارته
 * خلال 30 يوم (يُستدعى دورياً من DailyMaintenanceWorker).
 */
object DomainVisitTracker {

    private const val PREFS_NAME = "domain_visits"
    private const val KEY_MAP = "visits"
    private const val EXPIRY_MS = 30L * 24 * 60 * 60 * 1000

    fun recordVisit(context: Context, host: String?) {
        if (host.isNullOrBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = readMap(prefs)
        json.put(host, System.currentTimeMillis())
        prefs.edit().putString(KEY_MAP, json.toString()).apply()
    }

    /** يمسح كل سجل الزيارات المحفوظ (يُستخدم مع مسح الكاش والكوكيز اليدوي الكامل). */
    fun clearAll(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /** يمسح كوكيز/كاش أي دومين ما تمت زيارته من 30 يوم. آمن الاستدعاء من أي خيط. */
    fun cleanupExpired(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = readMap(prefs)

        val now = System.currentTimeMillis()
        val expiredDomains = mutableListOf<String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val domain = keys.next()
            val lastVisit = json.optLong(domain, 0L)
            if (now - lastVisit > EXPIRY_MS) {
                expiredDomains.add(domain)
            }
        }

        if (expiredDomains.isEmpty()) return

        // عمليات CookieManager/WebStorage لازم تصير على الخيط الرئيسي
        val latch = CountDownLatch(1)
        Handler(Looper.getMainLooper()).post {
            try {
                expiredDomains.forEach { domain -> clearDomainData(domain) }
            } finally {
                latch.countDown()
            }
        }
        latch.await()

        expiredDomains.forEach { json.remove(it) }
        prefs.edit().putString(KEY_MAP, json.toString()).apply()
    }

    private fun readMap(prefs: android.content.SharedPreferences): JSONObject {
        return try {
            JSONObject(prefs.getString(KEY_MAP, "{}") ?: "{}")
        } catch (e: Exception) {
            JSONObject()
        }
    }

    private fun clearDomainData(domain: String) {
        val cookieManager = CookieManager.getInstance()
        for (scheme in listOf("https://", "http://")) {
            val url = "$scheme$domain"
            val cookieString = cookieManager.getCookie(url) ?: continue
            cookieString.split(";").forEach { pair ->
                val name = pair.substringBefore("=").trim()
                if (name.isNotEmpty()) {
                    cookieManager.setCookie(url, "$name=; Max-Age=0; path=/")
                }
            }
        }
        cookieManager.flush()

        val storage = WebStorage.getInstance()
        storage.deleteOrigin("https://$domain")
        storage.deleteOrigin("http://$domain")
    }
}
