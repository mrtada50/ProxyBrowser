package com.proxybrowser.app.ui

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.proxybrowser.app.data.AdBlockManager
import com.proxybrowser.app.data.DomainVisitTracker
import com.proxybrowser.app.data.PrefsManager
import com.proxybrowser.app.model.ProxyInfo
import com.proxybrowser.app.model.ProxyType
import java.util.concurrent.Executor

private data class PendingDownload(
    val url: String,
    val userAgent: String,
    val contentDisposition: String,
    val mimeType: String,
    val fileName: String
)

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    proxy: ProxyInfo,
    onProxyLost: () -> Unit
) {
    val context = LocalContext.current

    var urlText by remember { mutableStateOf(PrefsManager.getHomepage(context)) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var proxyReady by remember { mutableStateOf(false) }
    var proxySupported by remember { mutableStateOf(true) }
    var isProxyOn by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(100) }
    var lostTriggered by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFavoritesDialog by remember { mutableStateOf(false) }
    var homepageInput by remember { mutableStateOf(PrefsManager.getHomepage(context)) }
    var favorites by remember { mutableStateOf(PrefsManager.getFavorites(context)) }
    var pendingDownload by remember { mutableStateOf<PendingDownload?>(null) }

    // زر الرجوع بالهاتف يرجع صفحة بالمتصفح إذا فيه صفحات سابقة
    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    // يفعّل أو يلغي توجيه WebView عبر البروكسي حسب حالة زر تشغيل/إيقاف
    DisposableEffect(proxy, isProxyOn) {
        val immediateExecutor = Executor { it.run() }
        proxyReady = false

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            proxySupported = false
            proxyReady = true
        } else if (isProxyOn) {
            val scheme = if (proxy.type == ProxyType.SOCKS5) "socks5" else "http"
            val proxyConfig = ProxyConfig.Builder()
                .addProxyRule("$scheme://${proxy.host}:${proxy.port}")
                .build()

            ProxyController.getInstance().setProxyOverride(proxyConfig, immediateExecutor) {
                proxyReady = true
            }
        } else {
            ProxyController.getInstance().clearProxyOverride(immediateExecutor) {
                proxyReady = true
            }
        }

        onDispose {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                ProxyController.getInstance().clearProxyOverride(immediateExecutor) {}
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        val status = if (isProxyOn) {
                            "${proxy.host}:${proxy.port}  •  ${proxy.latencyMs} ms"
                        } else {
                            "اتصال مباشر (بدون بروكسي)"
                        }
                        Text(status, style = MaterialTheme.typography.titleSmall)
                    },
                    actions = {
                        TextButton(onClick = { isProxyOn = !isProxyOn }) {
                            Text(if (isProxyOn) "إيقاف البروكسي" else "تشغيل البروكسي")
                        }
                    }
                )

                // شريط تقدم تحميل الصفحة الحالية
                if (loadProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { loadProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { webViewRef?.goBack() }) { Text("<") }
                    TextButton(onClick = { webViewRef?.goForward() }) { Text(">") }
                    TextButton(onClick = { webViewRef?.reload() }) { Text("⟳") }
                    TextButton(onClick = {
                        val home = PrefsManager.getHomepage(context)
                        urlText = home
                        webViewRef?.loadUrl(home)
                    }) { Text("🏠") }
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            webViewRef?.loadUrl(normalizeUrl(urlText))
                        })
                    )
                    TextButton(onClick = {
                        PrefsManager.addFavorite(context, urlText)
                        favorites = PrefsManager.getFavorites(context)
                    }) { Text("⭐") }
                    TextButton(onClick = {
                        favorites = PrefsManager.getFavorites(context)
                        showFavoritesDialog = true
                    }) { Text("☰") }
                    TextButton(onClick = {
                        homepageInput = PrefsManager.getHomepage(context)
                        showSettingsDialog = true
                    }) { Text("⚙") }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                !proxySupported -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("الجهاز لا يدعم توجيه WebView عبر بروكسي (PROXY_OVERRIDE غير متوفر)")
                    }
                }
                !proxyReady -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                // إعدادات التوافق: فتح كل أنواع الصفحات وتناسقها مع حجم الشاشة
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                                // تشديد الحماية
                                settings.safeBrowsingEnabled = true
                                settings.javaScriptCanOpenWindowsAutomatically = false
                                settings.setSupportMultipleWindows(false)
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                settings.setGeolocationEnabled(false)

                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        url?.let {
                                            urlText = it
                                            DomainVisitTracker.recordVisit(context, Uri.parse(it).host)
                                        }
                                        canGoBack = view?.canGoBack() == true
                                        CookieManager.getInstance().flush()
                                    }

                                    override fun shouldInterceptRequest(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): WebResourceResponse? {
                                        AdBlockManager.ensureLoadedSync(context)
                                        val host = request?.url?.host
                                        if (AdBlockManager.isBlocked(host)) {
                                            return WebResourceResponse("text/plain", "utf-8", null)
                                        }
                                        return super.shouldInterceptRequest(view, request)
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        // إذا انقطع البروكسي بصفحة رئيسية، نعيد الفحص تلقائياً
                                        if (isProxyOn && request?.isForMainFrame == true && !lostTriggered) {
                                            lostTriggered = true
                                            onProxyLost()
                                        }
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        super.onProgressChanged(view, newProgress)
                                        loadProgress = newProgress
                                    }
                                }

                                // أي تنزيل ملف يحتاج تأكيد صريح من المستخدم قبل ما يصير
                                setDownloadListener { dUrl, userAgent, contentDisposition, mimeType, _ ->
                                    val fileName = URLUtil.guessFileName(dUrl, contentDisposition, mimeType)
                                    pendingDownload = PendingDownload(dUrl, userAgent, contentDisposition, mimeType, fileName)
                                }

                                webViewRef = this
                                loadUrl(urlText)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("تعديل الصفحة الرئيسية") },
            text = {
                OutlinedTextField(
                    value = homepageInput,
                    onValueChange = { homepageInput = it },
                    singleLine = true,
                    label = { Text("رابط الصفحة الرئيسية") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    PrefsManager.setHomepage(context, normalizeUrl(homepageInput))
                    showSettingsDialog = false
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showFavoritesDialog) {
        AlertDialog(
            onDismissRequest = { showFavoritesDialog = false },
            title = { Text("المفضلة") },
            text = {
                if (favorites.isEmpty()) {
                    Text("ما فيه مفضلات محفوظة بعد")
                } else {
                    LazyColumn {
                        items(favorites) { fav ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = fav,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                TextButton(onClick = {
                                    urlText = fav
                                    webViewRef?.loadUrl(fav)
                                    showFavoritesDialog = false
                                }) { Text("فتح") }
                                TextButton(onClick = {
                                    PrefsManager.removeFavorite(context, fav)
                                    favorites = PrefsManager.getFavorites(context)
                                }) { Text("✕") }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFavoritesDialog = false }) { Text("إغلاق") }
            }
        )
    }

    pendingDownload?.let { dl ->
        AlertDialog(
            onDismissRequest = { pendingDownload = null },
            title = { Text("طلب تنزيل ملف") },
            text = {
                Text("الموقع يحاول تنزيل الملف:\n${dl.fileName}\n\nهل تسمح بالتنزيل؟")
            },
            confirmButton = {
                TextButton(onClick = {
                    startDownload(context, dl)
                    pendingDownload = null
                }) { Text("سماح") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDownload = null }) { Text("رفض") }
            }
        )
    }
}

private fun startDownload(context: android.content.Context, dl: PendingDownload) {
    try {
        val request = DownloadManager.Request(Uri.parse(dl.url)).apply {
            val cookie = CookieManager.getInstance().getCookie(dl.url)
            addRequestHeader("cookie", cookie ?: "")
            addRequestHeader("User-Agent", dl.userAgent)
            setMimeType(dl.mimeType)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, dl.fileName)
        }
        val manager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    } catch (e: Exception) {
        // تجاهل فشل التنزيل (مثلاً اسم ملف غير صالح أو رابط غير مدعوم)
    }
}

private fun normalizeUrl(input: String): String {
    val trimmed = input.trim()
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
}
