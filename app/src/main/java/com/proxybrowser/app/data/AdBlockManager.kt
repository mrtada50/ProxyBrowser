package com.proxybrowser.app.data

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * يدير قائمة حظر دومينات الإعلانات والتتبع.
 * يجيب القائمة الموحّدة المعروفة (StevenBlack/hosts، +79 ألف دومين) ويخزّنها
 * محلياً، ويحدّثها تلقائياً كل أسبوع. عنده قائمة احتياطية صغيرة تشتغل فوراً
 * حتى قبل أول تنزيل ناجح.
 */
object AdBlockManager {

    private val blockedDomains = ConcurrentHashMap.newKeySet<String>()
    @Volatile private var loaded = false

    private const val LIST_URL = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
    private const val CACHE_FILE = "adblock_domains.txt"
    private const val PREFS_NAME = "adblock_prefs"
    private const val KEY_LAST_UPDATE = "last_update"
    private const val REFRESH_INTERVAL_MS = 7L * 24 * 60 * 60 * 1000 // أسبوع

    private val FALLBACK_SEED = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "google-analytics.com", "adnxs.com", "adsafeprotected.com",
        "moatads.com", "scorecardresearch.com", "taboola.com", "outbrain.com",
        "criteo.com", "pubmatic.com", "rubiconproject.com", "adform.net",
        "adroll.com", "amazon-adsystem.com", "mopub.com", "vungle.com",
        "applovin.com", "unityads.unity3d.com", "adcolony.com", "chartboost.com",
        "inmobi.com", "startappservice.com", "appsflyer.com", "adjust.com",
        "branch.io", "flurry.com", "mixpanel.com", "segment.com", "hotjar.com",
        "mgid.com", "propellerads.com", "popads.net", "exoclick.com",
        "adsterra.com", "bidvertiser.com", "revcontent.com", "media.net",
        "adsystem.com", "advertising.com", "smartadserver.com", "yieldmo.com"
    )

    /** يحمّل القائمة من الملف المحلي (أو الاحتياطية إذا ماكو ملف بعد). خفيف وسريع. */
    fun ensureLoadedSync(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val file = File(context.filesDir, CACHE_FILE)
            if (file.exists()) {
                try {
                    file.bufferedReader().useLines { lines ->
                        lines.forEach { blockedDomains.add(it.trim()) }
                    }
                } catch (e: Exception) {
                    // تجاهل، بنستخدم القائمة الاحتياطية
                }
            }
            if (blockedDomains.isEmpty()) {
                blockedDomains.addAll(FALLBACK_SEED)
            }
            loaded = true
        }
    }

    /** يفحص الدومين ودومينات الأصل الأعلى (parent domains) بالتدريج. */
    fun isBlocked(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        var h = host.lowercase()
        while (true) {
            if (blockedDomains.contains(h)) return true
            val dot = h.indexOf('.')
            if (dot < 0) break
            h = h.substring(dot + 1)
        }
        return false
    }

    fun shouldRefresh(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST_UPDATE, 0L)
        return System.currentTimeMillis() - last > REFRESH_INTERVAL_MS
    }

    /** يجيب أحدث نسخة من القائمة ويحدّث النسخة المحلية. يُستدعى من خيط خلفي فقط. */
    fun refreshBlocking(context: Context) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(LIST_URL).build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return

                val domains = body.lineSequence()
                    .map { it.trim() }
                    .filter { it.startsWith("0.0.0.0 ") || it.startsWith("127.0.0.1 ") }
                    .mapNotNull { line ->
                        val parts = line.split(Regex("\\s+"))
                        if (parts.size >= 2) parts[1].lowercase() else null
                    }
                    .filter { it.isNotBlank() && it != "localhost" && it != "local" }
                    .toSet()

                // تحقق بسيط إن التنزيل نجح فعلاً وما رجع صفحة فاضية أو خطأ
                if (domains.size > 1000) {
                    val file = File(context.filesDir, CACHE_FILE)
                    file.bufferedWriter().use { writer ->
                        domains.forEach { d ->
                            writer.write(d)
                            writer.newLine()
                        }
                    }

                    blockedDomains.clear()
                    blockedDomains.addAll(domains)
                    loaded = true

                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply()
                }
            }
        } catch (e: Exception) {
            // فشل التحديث (مثلاً ماكو إنترنت) — تبقى القائمة الحالية/الاحتياطية سارية
        }
    }
}
