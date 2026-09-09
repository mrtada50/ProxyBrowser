package com.example.proxybrowser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

data class ProxyItem(val host: String, val port: Int, var latency: Long = Long.MAX_VALUE)

class ProxyManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    suspend fun fetchProxies(): List<ProxyItem> = withContext(Dispatchers.IO) {
        val proxyList = mutableListOf<ProxyItem>()
        val request = Request.Builder()
            .url("https://raw.githubusercontent.com/TheSpeedX/SOCKS-List/master/http.txt")
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            
            body.lines().take(50).forEach { line ->
                val parts = line.trim().split(":")
                if (parts.size == 2) {
                    val host = parts[0]
                    val port = parts[1].toIntOrNull()
                    if (port != null) {
                        proxyList.add(ProxyItem(host, port))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext proxyList
    }

    suspend fun findFastestProxy(proxies: List<ProxyItem>): ProxyItem? = withContext(Dispatchers.IO) {
        val deferredResults = proxies.map { proxyItem ->
            async {
                val latency = testProxyLatency(proxyItem.host, proxyItem.port)
                if (latency > 0) {
                    proxyItem.latency = latency
                    proxyItem
                } else {
                    null
                }
            }
        }

        val workingProxies = deferredResults.awaitAll().filterNotNull()
        return@withContext workingProxies.minByOrNull { it.latency }
    }

    private fun testProxyLatency(host: String, port: Int): Long {
        val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port))
        val testClient = OkHttpClient.Builder()
            .proxy(proxy)
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("https://www.google.com")
            .build()

        val startTime = System.currentTimeMillis()
        return try {
            val response = testClient.newCall(request).execute()
            if (response.isSuccessful) {
                System.currentTimeMillis() - startTime
            } else {
                -1
            }
        } catch (e: Exception) {
            -1
        }
    }
}
