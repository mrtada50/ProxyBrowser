package com.proxybrowser.app.ui

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import android.os.Message
import android.view.View
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.util.concurrent.atomic.AtomicInteger

private data class PendingDownload(
    val url: String,
    val userAgent: String,
    val contentDisposition: String,
    val mimeType: String,
    val fileName: String
)

/** حالة تبويب وحد بالمتصفح. */
private class TabState(val id: Int, initialUrl: String) {
    var webView: WebView? = null
    var title by mutableStateOf(initialUrl)
    var url by mutableStateOf(initialUrl)
    var canGoBack by mutableStateOf(false)
    var progress by mutableStateOf(100)
}

// أخطاء اتصال حقيقية فقط (استبعاد أخطاء الحظر الطبيعية زي حظر الإعلانات
// أو منع النوافذ المنبثقة، عشان ما تنعتبر خطأ بالبروكسي بالغلط)
private val PROXY_FAILURE_ERROR_CODES = setOf(
    WebViewClient.ERROR_CONNECT,
    WebViewClient.ERROR_HOST_LOOKUP,
    WebViewClient.ERROR_TIMEOUT,
    WebViewClient.ERROR_PROXY_AUTHENTICATION,
    WebViewClient.ERROR_FAILED_SSL_HANDSHAKE
)

private val nextTabIdCounter = AtomicInteger(1)

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    proxy: ProxyInfo,
    onProxyLost: () -> Unit
) {
    val context = LocalContext.current

    val tabs = remember { androidx.compose.runtime.mutableStateListOf<TabState>() }
    var activeTabId by remember { mutableStateOf(-1) }
    var containerRef by remember { mutableStateOf<FrameLayout?>(null) }
    var addressBarText by remember { mutableStateOf("") }

    var proxyReady by remember { mutableStateOf(false) }
    var proxySupported by remember { mutableStateOf(true) }
    var isProxyOn by remember { mutableStateOf(true) }
    var lostTriggered by remember { mutableStateOf(false) }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFavoritesDialog by remember { mutableStateOf(false) }
    var homepageInput by remember { mutableStateOf(PrefsManager.getHomepage(context)) }
    var favorites by remember { mutableStateOf(PrefsManager.getFavorites(context)) }
    var pendingDownload by remember { mutableStateOf<PendingDownload?>(null) }

    val activeTab = tabs.find { it.id == activeTabId }

    fun switchTab(id: Int) {
        tabs.forEach { it.webView?.visibility = if (it.id == id) View.VISIBLE else View.GONE }
        activeTabId = id
        addressBarText = tabs.find { it.id == id }?.url ?: ""
    }

    fun configureWebView(tab: TabState): WebView {
        return WebView(context).apply {
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
            settings.safeBrowsingEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(true)
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.setGeolocationEnabled(false)

            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    url?.let {
                        tab.url = it
                        val pageTitle = view?.title
                        tab.title = if (!pageTitle.isNullOrBlank()) pageTitle else it
                        DomainVisitTracker.recordVisit(context, Uri.parse(it).host)
                        if (tab.id == activeTabId) addressBarText = it
                    }
                    tab.canGoBack = view?.canGoBack() == true
                    CookieManager.getInstance().flush()
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    // مانحظر الصفحة الرئيسية أبداً — بس الموارد الفرعية
                    if (request?.isForMainFrame == true) {
                        return super.shouldInterceptRequest(view, request)
                    }
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
                    // نعيد الفحص فقط عند فشل اتصال حقيقي بالبروكسي، مو عند حظر
                    // إعلان أو منع نافذة منبثقة أو أي خطأ عادي ثاني
                    val errorCode = error?.errorCode
                    if (isProxyOn &&
                        request?.isForMainFrame == true &&
                        !lostTriggered &&
                        errorCode != null &&
                        errorCode in PROXY_FAILURE_ERROR_CODES
                    ) {
                        lostTriggered = true
                        onProxyLost()
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    tab.progress = newProgress
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    if (!title.isNullOrBlank()) tab.title = title
                }

                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    // نفتح النافذة المنبثقة بتبويب بالخلفية بدون ما نتحول له
                    val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                    val popupTab = TabState(nextTabIdCounter.getAndIncrement(), "")
                    val popupWebView = configureWebView(popupTab)
                    popupTab.webView = popupWebView
                    containerRef?.addView(
                        popupWebView,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    )
                    popupWebView.visibility = View.GONE
                    tabs.add(popupTab)
                    transport.webView = popupWebView
                    resultMsg.sendToTarget()
                    return true
                }
            }

            setDownloadListener { dUrl, userAgent, contentDisposition, mimeType, _ ->
                val fileName = URLUtil.guessFileName(dUrl, contentDisposition, mimeType)
                pendingDownload = PendingDownload(dUrl, userAgent, contentDisposition, mimeType, fileName)
            }
        }
    }

    fun openTab(url: String, activate: Boolean) {
        val tab = TabState(nextTabIdCounter.getAndIncrement(), url)
        val webView = configureWebView(tab)
        tab.webView = webView
        containerRef?.addView(
            webView,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        webView.visibility = View.GONE
        tabs.add(tab)
        webView.loadUrl(url)
        if (activate) switchTab(tab.id)
    }

    fun closeTab(id: Int) {
        if (tabs.size <= 1) return
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        val tab = tabs[index]
        containerRef?.removeView(tab.webView)
        tab.webView?.destroy()
        tabs.removeAt(index)
        if (activeTabId == id) {
            val newActive = tabs.getOrNull((index - 1).coerceAtLeast(0)) ?: tabs.firstOrNull()
            newActive?.let { switchTab(it.id) }
        }
    }

    // إنشاء أول تبويب عند فتح المتصفح لأول مرة
    LaunchedEffect(Unit) {
        if (tabs.isEmpty()) {
            openTab(PrefsManager.getHomepage(context), activate = true)
        }
    }

    // زر الرجوع بالهاتف يرجع صفحة بالتبويب الحالي إذا فيه صفحات سابقة
    BackHandler(enabled = activeTab?.canGoBack == true) {
        activeTab?.webView?.goBack()
    }

    // يفعّل أو يلغي توجيه WebView عبر البروكسي حسب حالة زر تشغيل/إيقاف (يشمل كل التبويبات)
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
                            "${proxy.host}:${proxy.port} • ${proxy.latencyMs}ms"
                        } else {
                            "اتصال مباشر"
                        }
                        Text(status, style = MaterialTheme.typography.titleSmall)
                    },
                    actions = {
                        IconButton(onClick = { isProxyOn = !isProxyOn }) {
                            Icon(
                                imageVector = if (isProxyOn) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                                contentDescription = if (isProxyOn) "إيقاف البروكسي" else "تشغيل البروكسي"
                            )
                        }
                        IconButton(onClick = {
                            homepageInput = PrefsManager.getHomepage(context)
                            showSettingsDialog = true
                        }) {
                            Icon(Icons.Filled.Settings, contentDescription = "الإعدادات")
                        }
                    }
                )

                // شريط التبويبات
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(tabs, key = { it.id }) { tab ->
                        val isActive = tab.id == activeTabId
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isActive) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .widthIn(max = 130.dp)
                                .clickable { switchTab(tab.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tab.title.ifBlank { "تبويب" },
                                    maxLines = 1,
                                    fontSize = 12.sp,
                                    modifier = Modifier.widthIn(max = 80.dp)
                                )
                                IconButton(
                                    onClick = { closeTab(tab.id) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "إغلاق التبويب",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    item {
                        IconButton(onClick = {
                            openTab(PrefsManager.getHomepage(context), activate = true)
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = "تبويب جديد")
                        }
                    }
                }

                if ((activeTab?.progress ?: 100) in 1..99) {
                    LinearProgressIndicator(
                        progress = { (activeTab?.progress ?: 100) / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { activeTab?.webView?.goBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                    IconButton(onClick = { activeTab?.webView?.goForward() }) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = "تقدم")
                    }
                    IconButton(onClick = { activeTab?.webView?.reload() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "تحديث")
                    }
                    IconButton(onClick = {
                        val home = PrefsManager.getHomepage(context)
                        activeTab?.webView?.loadUrl(home)
                    }) {
                        Icon(Icons.Filled.Home, contentDescription = "الرئيسية")
                    }

                    // شريط عنوان الموقع على شكل مستطيل مدوّر (شكل احترافي زي متصفحات الأندرويد المعروفة)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Language,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.padding(start = 6.dp))
                            BasicTextField(
                                value = addressBarText,
                                onValueChange = { addressBarText = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = {
                                    activeTab?.webView?.loadUrl(normalizeUrl(addressBarText))
                                }),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    IconButton(onClick = {
                        PrefsManager.addFavorite(context, addressBarText)
                        favorites = PrefsManager.getFavorites(context)
                    }) {
                        Icon(Icons.Filled.Star, contentDescription = "إضافة للمفضلة")
                    }
                    IconButton(onClick = {
                        favorites = PrefsManager.getFavorites(context)
                        showFavoritesDialog = true
                    }) {
                        Icon(Icons.Filled.List, contentDescription = "المفضلة")
                    }
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
                            FrameLayout(ctx).also { containerRef = it }
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
            title = { Text("الإعدادات") },
            text = {
                Column {
                    Text("الصفحة الرئيسية", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = homepageInput,
                        onValueChange = { homepageInput = it },
                        singleLine = true,
                        label = { Text("رابط الصفحة الرئيسية") }
                    )

                    Spacer(modifier = Modifier.padding(top = 16.dp))

                    TextButton(onClick = {
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()
                        WebStorage.getInstance().deleteAllData()
                        activeTab?.webView?.clearCache(true)
                        activeTab?.webView?.clearHistory()
                        DomainVisitTracker.clearAll(context)
                        Toast.makeText(context, "تم مسح كل الكاش والكوكيز", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("مسح كل الكاش والكوكيز")
                    }
                }
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
                                    activeTab?.webView?.loadUrl(fav)
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
