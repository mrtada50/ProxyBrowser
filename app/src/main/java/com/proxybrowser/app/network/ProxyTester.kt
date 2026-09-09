package com.proxybrowser.app.network

import com.proxybrowser.app.model.ProxyInfo
import com.proxybrowser.app.model.ProxyType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    private const val MAX_CONCURRENT = 30

    /**
     * يفحص قائمة المرشحين على دفعات متوازية، وينادي onResult فوراً
     * لكل بروكسي شغال (بدون انتظار انتهاء القائمة كاملة).
     */
    suspend fun testProxies(
        candidates: List<ProxyInfo>,
        onResult: suspend (ProxyInfo) -> Unit
    ) = coroutineScope {
        candidates.chunked(MAX_CONCURRENT).forEach { chunk ->
            chunk.map { proxy ->
                async(Dispatchers.IO) {
                    testSingle(proxy)
                }
            }.awaitAll().filterNotNull().forEach { tested ->
                onResult(tested)
            }
        }
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
