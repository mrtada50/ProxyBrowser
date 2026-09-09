package com.example.proxybrowser

import android.content.Context
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val proxyManager = ProxyManager()
    private val defaultUrl = "https://www.google.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()
        }
        setContentView(webView)

        val prefs = getPreferences(Context.MODE_PRIVATE)
        val homeUrl = prefs.getString("home_url", defaultUrl) ?: defaultUrl

        initProxyAndLoad(homeUrl)
    }

    private fun initProxyAndLoad(targetUrl: String) {
        Toast.makeText(this, "جاري فحص وجلب أسرع بروكسي...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val proxies = proxyManager.fetchProxies()
            val fastestProxy = proxyManager.findFastestProxy(proxies)

            if (fastestProxy != null) {
                setWebViewProxy(fastestProxy.host, fastestProxy.port)
                Toast.makeText(this@MainActivity, "تم الاتصال بـ: ${fastestProxy.host} (${fastestProxy.latency}ms)", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@MainActivity, "فشل العثور على بروكسي يعمل. التصفح مباشر.", Toast.LENGTH_LONG).show()
            }

            webView.loadUrl(targetUrl)
        }
    }

    private fun setWebViewProxy(host: String, port: Int) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            val proxyConfig = ProxyConfig.Builder()
                .addProxyRule("$host:$port")
                .build()

            ProxyController.getInstance().setProxyOverride(proxyConfig, {
                // Connected successfully
            }, {
                // Connection failed
            })
        }
    }
}
