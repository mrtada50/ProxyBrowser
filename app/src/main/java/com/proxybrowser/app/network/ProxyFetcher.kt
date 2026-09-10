package com.proxybrowser.app.network

import com.proxybrowser.app.model.ProxyInfo
import com.proxybrowser.app.model.ProxyType
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * يجلب قوائم بروكسيات خام (غير مفحوصة محلياً بعد) من مصدر عام مجاني.
 * timeoutMs يتحكم بحد السرعة اللي يطلبه المصدر نفسه قبل ما يرجع القائمة.
 */
object ProxyFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun httpUrl(timeoutMs: Int) =
        "https://api.proxyscrape.com/v2/?request=getproxies&protocol=http&timeout=$timeoutMs&country=all&ssl=all&anonymity=all"

    private fun socks5Url(timeoutMs: Int) =
        "https://api.proxyscrape.com/v2/?request=getproxies&protocol=socks5&timeout=$timeoutMs&country=all"

    fun fetchAll(timeoutMs: Int): List<ProxyInfo> {
        val result = mutableListOf<ProxyInfo>()
        result += fetchOne(httpUrl(timeoutMs), ProxyType.HTTP)
        result += fetchOne(socks5Url(timeoutMs), ProxyType.SOCKS5)
        return result
    }

    private fun fetchOne(url: String, type: ProxyType): List<ProxyInfo> {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                parseBody(body, type)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseBody(body: String, type: ProxyType): List<ProxyInfo> {
        return body.lineSequence()
            .map { it.trim() }
            .filter { it.contains(":") }
            .mapNotNull { line ->
                val parts = line.split(":")
                if (parts.size != 2) return@mapNotNull null
                val port = parts[1].toIntOrNull() ?: return@mapNotNull null
                ProxyInfo(host = parts[0], port = port, type = type)
            }
            .toList()
    }
}
