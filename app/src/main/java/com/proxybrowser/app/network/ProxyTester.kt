package com.proxybrowser.app.network

import com.proxybrowser.app.model.ProxyInfo
import com.proxybrowser.app.model.ProxyType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * يفحص كل بروكسي عبر طلب خفيف لموقع جوجل (generate_204) ويقيس زمن الاستجابة.
 * يقبل فقط البروكسيات اللي تستجيب بنجاح وبزمن أقل من 500ms.
 */
object ProxyTester {

    private const val TEST_URL = "https://www.google.com/generate_204"
    private const val TIMEOUT_MS = 500L

    /**
     * يفحص كل المرشحين بالتوازي، ويرجع أول بروكسي يستجيب بنجاح
     * ويوقف باقي الفحص فوراً (بدون انتظار بقية القائمة).
     * يرجع null إذا ما لقى أي بروكسي شغال بين كل المرشحين.
     */
    suspend fun findFirstWorking(candidates: List<ProxyInfo>): ProxyInfo? = coroutineScope {
        if (candidates.isEmpty()) return@coroutineScope null

        val found = CompletableDeferred<ProxyInfo?>()

        val jobs = candidates.map { proxy ->
            launch(Dispatchers.IO) {
                val result = testSingle(proxy)
                if (result != null) {
                    found.complete(result)
                }
            }
        }

        // إذا خلصت كل المحاولات بدون أي نجاح، نكمل بـ null
        launch {
            jobs.joinAll()
            if (!found.isCompleted) {
                found.complete(null)
            }
        }

        val result = found.await()
        jobs.forEach { it.cancel() }
        result
    }

    private fun testSingle(proxy: ProxyInfo): ProxyInfo? {
        return try {
            val javaType = if (proxy.type == ProxyType.SOCKS5) Proxy.Type.SOCKS else Proxy.Type.HTTP
            val javaProxy = Proxy(javaType, InetSocketAddress(proxy.host, proxy.port))

            val client = OkHttpClient.Builder()
                .proxy(javaProxy)
                .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build()

            val request = Request.Builder().url(TEST_URL).build()
            val start = System.currentTimeMillis()

            client.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - start
                if (response.isSuccessful && elapsed < 500) {
                    proxy.copy(latencyMs = elapsed)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
