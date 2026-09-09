package com.proxybrowser.app.model

enum class ProxyType { HTTP, SOCKS5 }

data class ProxyInfo(
    val host: String,
    val port: Int,
    val type: ProxyType,
    val latencyMs: Long? = null
)
