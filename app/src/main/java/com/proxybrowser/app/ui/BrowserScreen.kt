package com.proxybrowser.app.ui

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.proxybrowser.app.data.AdBlockManager
import com.proxybrowser.app.data.DomainVisitTracker
import com.proxybrowser.app.data.FavoriteItem
import com.proxybrowser.app.data.HistoryEntry
import com.proxybrowser.app.data.HistoryManager
import com.proxybrowser.app.data.PrefsManager
import com.proxybrowser.app.data.ProxyBypassManager
import com.proxybrowser.app.model.ProxyInfo
import com.proxybrowser.app.model.ProxyType
import com.proxybrowser.app.state.ThemeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

private data class PendingDownload(
    val url: String,
    val userAgent: String,
    val contentDisposition: String,
    val mimeType: String,
    val fileName: String
)

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

/** حالة تبويب وحد بالمتصفح. */
private class TabState(val id: Int, initialUrl: String) {
    var webView: WebView? = null
    var title by mutableStateOf(initialUrl)
    var url by mutableStateOf(initialUrl)
    var canGoBack by mutableStateOf(false)
    var progress by mutableStateOf(100)
    var mediaLinks by mutableStateOf<List<String>>(emptyList())
    var desktopMode by mutableStateOf(false)
    var readerMode by mutableStateOf(false)
    var groupName by mutableStateOf<String?>(null)
}

// سكربت يفحص الصفحة الحالية عن وسوم فيديو/صوت ويجمع روابطها
private const val MEDIA_SCAN_JS = """
(function() {
    try {
        var urls = [];
        document.querySelectorAll('video, audio').forEach(function(el) {
            if (el.src) urls.push(el.src);
            el.querySelectorAll('source').forEach(function(s) {
                if (s.src) urls.push(s.src);
            });
        });
        urls = urls.filter(function(v, i) { return urls.indexOf(v) === i; });
        AndroidMedia.onMediaFound(JSON.stringify(urls));
    } catch (e) {}
})();
"""

// سكربت وضع القراءة: يبسّط شكل الصفحة (خط أكبر، عرض مريح، إخفاء عناصر جانبية شائعة)
private const val READER_MODE_JS = """
(function() {
    try {
        var old = document.getElementById('proxybrowser-reader-style');
        if (old) old.remove();
        var style = document.createElement('style');
        style.id = 'proxybrowser-reader-style';
        style.innerHTML = 'body{max-width:700px !important;margin:0 auto !important;padding:16px !important;' +
            'font-size:20px !important;line-height:1.7 !important;background:#fdfdfd !important;color:#111 !important;}' +
            'img,video{max-width:100% !important;height:auto !important;}' +
            'nav,header,footer,aside,.ad,.ads,.advert,.advertisement,.sidebar,.comments,.comment,.related,.share,.social{display:none !important;}';
        document.head.appendChild(style);
    } catch (e) {}
})();
"""

/** جسر JavaScript بسيط يستقبل روابط الوسائط المكتشفة بالصفحة فقط (بدون أي صلاحيات ثانية). */
private class MediaBridge(private val onFound: (List<String>) -> Unit) {
    @JavascriptInterface
    fun onMediaFound(json: String) {
        val list = try {
            val arr = org.json.JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
        Handler(Looper.getMainLooper()).post { onFound(list) }
    }
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

private fun permissionLabel(resource: String): String = when (resource) {
    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> "الكاميرا"
    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> "المايكروفون"
    else -> resource
}

/** يجيب اقتراحات بحث من جوجل أثناء الكتابة، عبر نفس البروكسي المستخدم بالتصفح. */
private object SuggestionsFetcher {
    suspend fun fetch(query: String, proxyInfo: ProxyInfo?): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val builder = okhttp3.OkHttpClient.Builder()
                .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(3, java.util.concurrent.TimeUnit.SECONDS)

            if (proxyInfo != null) {
                val type = if (proxyInfo.type == ProxyType.SOCKS5) {
                    java.net.Proxy.Type.SOCKS
                } else {
                    java.net.Proxy.Type.HTTP
                }
                builder.proxy(
                    java.net.Proxy(type, java.net.InetSocketAddress(proxyInfo.host, proxyInfo.port))
                )
            }

            val url = "https://suggestqueries.google.com/complete/search?client=firefox&q=" +
                URLEncoder.encode(query, "UTF-8")
            val request = okhttp3.Request.Builder().url(url).build()

            builder.build().newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@withContext emptyList()
                val arr = org.json.JSONArray(body)
                val items = arr.optJSONArray(1) ?: return@withContext emptyList()
                (0 until items.length()).map { items.getString(it) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BrowserScreen(
    proxy: ProxyInfo,
    onProxyLost: () -> Unit
) {
    val context = LocalContext.current

    val tabs = remember { androidx.compose.runtime.mutableStateListOf<TabState>() }
    var activeTabId by remember { mutableStateOf(-1) }
    var containerRef by remember { mutableStateOf<FrameLayout?>(null) }
    var swipeRefreshRef by remember { mutableStateOf<SwipeRefreshLayout?>(null) }
    var addressBarText by remember { mutableStateOf("") }

    var proxyReady by remember { mutableStateOf(false) }
    var proxySupported by remember { mutableStateOf(true) }
    var isProxyOn by remember { mutableStateOf(true) }
    var lostTriggered by remember { mutableStateOf(false) }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFavoritesDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showTabsDialog by remember { mutableStateOf(false) }
    var showMediaDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var homepageInput by remember { mutableStateOf(PrefsManager.getHomepage(context)) }
    var favorites by remember { mutableStateOf(PrefsManager.getFavorites(context)) }
    var historyItems by remember { mutableStateOf(HistoryManager.getAll(context)) }
    var favoritesQuery by remember { mutableStateOf("") }
    var historyQuery by remember { mutableStateOf("") }
    var editingFavorite by remember { mutableStateOf<FavoriteItem?>(null) }
    var editTitleInput by remember { mutableStateOf("") }
    var editUrlInput by remember { mutableStateOf("") }
    var textZoomInput by remember { mutableStateOf(PrefsManager.getTextZoom(context)) }
    var pendingDownload by remember { mutableStateOf<PendingDownload?>(null) }
    var pendingPermission by remember { mutableStateOf<PermissionRequest?>(null) }
    var longPressUrl by remember { mutableStateOf<String?>(null) }
    var groupEditTab by remember { mutableStateOf<TabState?>(null) }
    var groupNameInput by remember { mutableStateOf("") }
    var addressBarEditing by remember { mutableStateOf(false) }
    var addressBarInput by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    val addressFocusRequester = remember { FocusRequester() }
    var proxyBypassVersion by remember { mutableStateOf(0) }

    val isDarkTheme by ThemeState.isDarkTheme.collectAsState()

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
            settings.textZoom = PrefsManager.getTextZoom(context)

            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            addJavascriptInterface(
                MediaBridge { links -> tab.mediaLinks = links },
                "AndroidMedia"
            )

            setOnLongClickListener {
                val result = hitTestResult
                val type = result.type
                if (type == android.webkit.WebView.HitTestResult.SRC_ANCHOR_TYPE ||
                    type == android.webkit.WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                ) {
                    longPressUrl = result.extra
                    true
                } else {
                    false
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    url?.let {
                        tab.url = it
                        val pageTitle = view?.title
                        val finalTitle = if (!pageTitle.isNullOrBlank()) pageTitle else it
                        tab.title = finalTitle
                        DomainVisitTracker.recordVisit(context, Uri.parse(it).host)
                        HistoryManager.add(context, finalTitle, it)
                        historyItems = HistoryManager.getAll(context)
                        if (tab.id == activeTabId) addressBarText = it
                    }
                    tab.canGoBack = view?.canGoBack() == true
                    tab.mediaLinks = emptyList()
                    view?.evaluateJavascript(MEDIA_SCAN_JS, null)
                    if (tab.readerMode) view?.evaluateJavascript(READER_MODE_JS, null)
                    CookieManager.getInstance().flush()
                    swipeRefreshRef?.isRefreshing = false
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    // مانحظر الصفحة الرئيسية أبداً — بس الموارد الفرعية
                    if (request?.isForMainFrame == true) {
                        return super.shouldInterceptRequest(view, request)
                    }
                    val pageHost = Uri.parse(tab.url).host
                    if (AdBlockManager.isSiteWhitelisted(context, pageHost)) {
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

                override fun onPermissionRequest(request: PermissionRequest?) {
                    if (request == null) return
                    pendingPermission = request
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

    fun toggleDesktopMode() {
        val tab = activeTab ?: return
        tab.desktopMode = !tab.desktopMode
        tab.webView?.settings?.userAgentString = if (tab.desktopMode) DESKTOP_USER_AGENT else null
        tab.webView?.reload()
    }

    fun toggleReaderMode() {
        val tab = activeTab ?: return
        tab.readerMode = !tab.readerMode
        if (tab.readerMode) {
            tab.webView?.evaluateJavascript(READER_MODE_JS, null)
        } else {
            tab.webView?.reload()
        }
    }

    fun shareCurrentUrl() {
        val url = activeTab?.url ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الرابط").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    // إنشاء أول تبويب عند فتح المتصفح لأول مرة. مهم: هذا يعتمد على وجود
    // containerRef مسبقاً، لهذا الحاوية (FrameLayout) الحين تُبنى مباشرة
    // بدون انتظار جاهزية البروكسي (proxyReady) — انظر شجرة الواجهة بالأسفل.
    LaunchedEffect(Unit) {
        if (tabs.isEmpty()) {
            openTab(PrefsManager.getHomepage(context), activate = true)
        }
    }

    // يفتح الكيبورد تلقائياً لما ندخل وضع التعديل بشريط العنوان
    LaunchedEffect(addressBarEditing) {
        if (addressBarEditing) {
            addressFocusRequester.requestFocus()
        }
    }

    // اقتراحات بحث من جوجل أثناء الكتابة (بتأخير بسيط عشان ما نرسل طلب كل حرف)
    LaunchedEffect(addressBarInput, addressBarEditing) {
        if (addressBarEditing && addressBarInput.isNotBlank()) {
            delay(300)
            suggestions = SuggestionsFetcher.fetch(addressBarInput, if (isProxyOn) proxy else null)
        } else {
            suggestions = emptyList()
        }
    }

    // زر الرجوع بالهاتف يرجع صفحة بالتبويب الحالي إذا فيه صفحات سابقة
    BackHandler(enabled = activeTab?.canGoBack == true) {
        activeTab?.webView?.goBack()
    }

    // يفعّل أو يلغي توجيه WebView عبر البروكسي حسب حالة زر تشغيل/إيقاف (يشمل كل التبويبات)
    DisposableEffect(proxy, isProxyOn, proxyBypassVersion) {
        val immediateExecutor = Executor { it.run() }
        proxyReady = false

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            proxySupported = false
            proxyReady = true
        } else if (isProxyOn) {
            val scheme = if (proxy.type == ProxyType.SOCKS5) "socks5" else "http"
            val builder = ProxyConfig.Builder()
                .addProxyRule("$scheme://${proxy.host}:${proxy.port}")

            ProxyBypassManager.getAll(context).forEach { host ->
                ProxyBypassManager.buildBypassRules(host).forEach { rule ->
                    builder.addBypassRule(rule)
                }
            }

            val proxyConfig = builder.build()

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
                        val currentHost = activeTab?.url?.let { Uri.parse(it).host }
                        val isBypassed = ProxyBypassManager.isBypassed(context, currentHost)
                        val status = when {
                            isBypassed -> "بدون بروكسي (استثناء لهذا الموقع)"
                            isProxyOn -> "${proxy.host}:${proxy.port} • ${proxy.latencyMs}ms"
                            else -> "اتصال مباشر"
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
                            textZoomInput = PrefsManager.getTextZoom(context)
                            showSettingsDialog = true
                        }) {
                            Icon(Icons.Filled.Settings, contentDescription = "الإعدادات")
                        }

                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "المزيد")
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("مشاركة الرابط") },
                                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                                    onClick = { showOverflowMenu = false; shareCurrentUrl() }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(if (activeTab?.desktopMode == true) "✓ نسخة سطح المكتب" else "نسخة سطح المكتب")
                                    },
                                    onClick = { showOverflowMenu = false; toggleDesktopMode() }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(if (activeTab?.readerMode == true) "✓ وضع القراءة" else "وضع القراءة")
                                    },
                                    onClick = { showOverflowMenu = false; toggleReaderMode() }
                                )
                                DropdownMenuItem(
                                    text = { Text("سجل التصفح") },
                                    onClick = {
                                        showOverflowMenu = false
                                        historyItems = HistoryManager.getAll(context)
                                        showHistoryDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        val host = activeTab?.url?.let { Uri.parse(it).host }
                                        val whitelisted = AdBlockManager.isSiteWhitelisted(context, host)
                                        Text(if (whitelisted) "✓ تعطيل حظر الإعلانات لهذا الموقع" else "تعطيل حظر الإعلانات لهذا الموقع")
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        val host = activeTab?.url?.let { Uri.parse(it).host }
                                        AdBlockManager.toggleSiteWhitelist(context, host)
                                        activeTab?.webView?.reload()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        val host = activeTab?.url?.let { Uri.parse(it).host }
                                        val bypassed = ProxyBypassManager.isBypassed(context, host)
                                        Text(if (bypassed) "✓ تصفح هذا الموقع بدون بروكسي" else "تصفح هذا الموقع بدون بروكسي")
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        val host = activeTab?.url?.let { Uri.parse(it).host }
                                        ProxyBypassManager.toggleBypass(context, host)
                                        proxyBypassVersion++
                                        activeTab?.webView?.reload()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isDarkTheme) "✓ الثيم الداكن" else "الثيم الداكن") },
                                    leadingIcon = { Icon(Icons.Filled.DarkMode, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        ThemeState.toggle(context)
                                    }
                                )
                            }
                        }
                    }
                )

                // شريط عنوان الموقع لوحده — مستطيل مدوّر بعرض الشاشة كامل
                Column {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isHttps = activeTab?.url?.startsWith("https://") == true
                            IconButton(
                                onClick = { showSecurityDialog = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isHttps) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                    contentDescription = "تفاصيل الاتصال",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isHttps) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }

                            if (addressBarEditing) {
                                var hasBeenFocused by remember(activeTabId) { mutableStateOf(false) }
                                BasicTextField(
                                    value = addressBarInput,
                                    onValueChange = { addressBarInput = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                    keyboardActions = KeyboardActions(onGo = {
                                        activeTab?.webView?.loadUrl(resolveAddressInput(addressBarInput))
                                        addressBarEditing = false
                                        suggestions = emptyList()
                                    }),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 6.dp)
                                        .focusRequester(addressFocusRequester)
                                        .onFocusChanged { state ->
                                            if (state.isFocused) {
                                                hasBeenFocused = true
                                            } else if (hasBeenFocused) {
                                                // ضغط بمكان ثاني (فقدان التركيز) — نرجع للوضع الطبيعي
                                                addressBarEditing = false
                                                suggestions = emptyList()
                                            }
                                        }
                                )
                            } else {
                                Text(
                                    text = (activeTab?.title?.ifBlank { null } ?: addressBarText).ifBlank { "ابحث أو اكتب رابط" },
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 6.dp)
                                        .clickable {
                                            addressBarInput = ""
                                            addressBarEditing = true
                                        }
                                )
                                IconButton(
                                    onClick = {
                                        addressBarInput = addressBarText
                                        addressBarEditing = true
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "تعديل الرابط مباشرة",
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val current = activeTab?.url
                                    if (!current.isNullOrBlank()) {
                                        activeTab?.webView?.loadUrl(buildGoogleTranslateUrl(current))
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Translate,
                                    contentDescription = "ترجمة الصفحة",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (addressBarEditing && suggestions.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp)
                        ) {
                            Column {
                                suggestions.take(6).forEach { suggestion ->
                                    Text(
                                        text = suggestion,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                activeTab?.webView?.loadUrl(resolveAddressInput(suggestion))
                                                addressBarEditing = false
                                                suggestions = emptyList()
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if ((activeTab?.progress ?: 100) in 1..99) {
                    LinearProgressIndicator(
                        progress = { (activeTab?.progress ?: 100) / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // صف أزرار التنقل + مربع عدد التبويبات + المفضلة
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
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

                    Spacer(modifier = Modifier.weight(1f))

                    if ((activeTab?.mediaLinks?.size ?: 0) > 0) {
                        IconButton(onClick = { showMediaDialog = true }) {
                            Icon(
                                Icons.Filled.PlayCircle,
                                contentDescription = "مقاطع مكتشفة بالصفحة",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(onClick = {
                        PrefsManager.addFavorite(context, activeTab?.title ?: addressBarText, addressBarText)
                        favorites = PrefsManager.getFavorites(context)
                    }) {
                        Icon(Icons.Filled.Star, contentDescription = "إضافة للمفضلة")
                    }
                    IconButton(onClick = {
                        favorites = PrefsManager.getFavorites(context)
                        favoritesQuery = ""
                        showFavoritesDialog = true
                    }) {
                        Icon(Icons.Filled.List, contentDescription = "المفضلة")
                    }

                    // مربع صغير فيه رقم عدد التبويبات — الضغط عليه يفتح قائمة التبويبات
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(32.dp)
                            .clickable { showTabsDialog = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = tabs.size.toString(),
                                fontSize = 14.sp,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        // الحاوية (FrameLayout داخل SwipeRefreshLayout للسحب-للتحديث) تُبنى دائماً
        // بغض النظر عن حالة البروكسي، عشان ما يصير سباق (race) بين إنشاء أول
        // تبويب وجاهزية الحاوية
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AndroidView(
                factory = { ctx ->
                    val inner = FrameLayout(ctx).also { containerRef = it }
                    SwipeRefreshLayout(ctx).apply {
                        addView(
                            inner,
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        )
                        setOnRefreshListener {
                            tabs.find { it.id == activeTabId }?.webView?.reload()
                        }
                        // بدون هذا، SwipeRefreshLayout يتحقق من FrameLayout الفارغة (دايماً
                        // "بالأعلى") بدل الـ WebView الفعلي، فيسوي رفرش حتى لو الصفحة
                        // مو بأعلاها فعلياً — نربطه بحالة تمرير الـ WebView النشط الحقيقية
                        setOnChildScrollUpCallback { _, _ ->
                            tabs.find { it.id == activeTabId }?.webView?.canScrollVertically(-1) == true
                        }
                        swipeRefreshRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (!proxySupported) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("الجهاز لا يدعم توجيه WebView عبر بروكسي (PROXY_OVERRIDE غير متوفر)")
                }
            } else if (!proxyReady) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (showTabsDialog) {
        AlertDialog(
            onDismissRequest = { showTabsDialog = false },
            title = { Text("التبويبات (${tabs.size})") },
            text = {
                val grouped = tabs.groupBy { it.groupName ?: "" }
                LazyColumn {
                    grouped.forEach { (group, tabsInGroup) ->
                        if (group.isNotBlank()) {
                            item {
                                Text(
                                    text = group,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                )
                            }
                        }
                        items(tabsInGroup, key = { it.id }) { tab ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .combinedClickable(
                                        onClick = {
                                            switchTab(tab.id)
                                            showTabsDialog = false
                                        },
                                        onLongClick = {
                                            groupEditTab = tab
                                            groupNameInput = tab.groupName ?: ""
                                        }
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tab.title.ifBlank { "تبويب" },
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f),
                                    style = if (tab.id == activeTabId) {
                                        MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        MaterialTheme.typography.bodyMedium
                                    }
                                )
                                IconButton(onClick = { closeTab(tab.id) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "إغلاق التبويب")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    openTab(PrefsManager.getHomepage(context), activate = true)
                    showTabsDialog = false
                }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تبويب جديد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTabsDialog = false }) { Text("إغلاق") }
            }
        )
    }

    groupEditTab?.let { tab ->
        AlertDialog(
            onDismissRequest = { groupEditTab = null },
            title = { Text("مجموعة التبويب") },
            text = {
                OutlinedTextField(
                    value = groupNameInput,
                    onValueChange = { groupNameInput = it },
                    singleLine = true,
                    label = { Text("اسم المجموعة (اتركه فاضي لإزالته من أي مجموعة)") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    tab.groupName = groupNameInput.trim().ifBlank { null }
                    groupEditTab = null
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { groupEditTab = null }) { Text("إلغاء") }
            }
        )
    }

    if (showMediaDialog) {
        val links = activeTab?.mediaLinks ?: emptyList()
        AlertDialog(
            onDismissRequest = { showMediaDialog = false },
            title = { Text("مقاطع بالصفحة (${links.size})") },
            text = {
                if (links.isEmpty()) {
                    Text("ما فيه مقاطع مكتشفة حالياً بهذي الصفحة")
                } else {
                    LazyColumn {
                        items(links) { link ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = link,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                TextButton(onClick = {
                                    openWithExternalPlayer(context, link)
                                }) { Text("فتح خارجي") }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMediaDialog = false }) { Text("إغلاق") }
            }
        )
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

                    Text("حجم خط الصفحة: $textZoomInput%", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = {
                            textZoomInput = (textZoomInput - 10).coerceAtLeast(50)
                        }) { Text("－") }
                        Spacer(modifier = Modifier.width(12.dp))
                        TextButton(onClick = {
                            textZoomInput = (textZoomInput + 10).coerceAtMost(200)
                        }) { Text("＋") }
                    }

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
                    PrefsManager.setTextZoom(context, textZoomInput)
                    tabs.forEach { it.webView?.settings?.textZoom = textZoomInput }
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
                Column {
                    OutlinedTextField(
                        value = favoritesQuery,
                        onValueChange = { favoritesQuery = it },
                        singleLine = true,
                        placeholder = { Text("بحث بالمفضلة") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.padding(top = 8.dp))

                    val filtered = favorites.filter {
                        favoritesQuery.isBlank() ||
                            it.title.contains(favoritesQuery, ignoreCase = true) ||
                            it.url.contains(favoritesQuery, ignoreCase = true)
                    }

                    if (filtered.isEmpty()) {
                        Text("ما فيه نتائج")
                    } else {
                        LazyColumn {
                            items(filtered) { fav ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            activeTab?.webView?.loadUrl(fav.url)
                                            showFavoritesDialog = false
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = fav.title,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    IconButton(onClick = {
                                        editingFavorite = fav
                                        editTitleInput = fav.title
                                        editUrlInput = fav.url
                                    }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "تعديل")
                                    }
                                    TextButton(onClick = {
                                        PrefsManager.removeFavorite(context, fav.url)
                                        favorites = PrefsManager.getFavorites(context)
                                    }) { Text("✕") }
                                }
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

    editingFavorite?.let { fav ->
        AlertDialog(
            onDismissRequest = { editingFavorite = null },
            title = { Text("تعديل المفضلة") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editTitleInput,
                        onValueChange = { editTitleInput = it },
                        singleLine = true,
                        label = { Text("الاسم") }
                    )
                    Spacer(modifier = Modifier.padding(top = 8.dp))
                    OutlinedTextField(
                        value = editUrlInput,
                        onValueChange = { editUrlInput = it },
                        singleLine = true,
                        label = { Text("الرابط") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    PrefsManager.updateFavorite(context, fav.url, editTitleInput, normalizeUrl(editUrlInput))
                    favorites = PrefsManager.getFavorites(context)
                    editingFavorite = null
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { editingFavorite = null }) { Text("إلغاء") }
            }
        )
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("سجل التصفح") },
            text = {
                Column {
                    OutlinedTextField(
                        value = historyQuery,
                        onValueChange = { historyQuery = it },
                        singleLine = true,
                        placeholder = { Text("بحث بالسجل") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.padding(top = 8.dp))

                    val filtered = historyItems.filter {
                        historyQuery.isBlank() ||
                            it.title.contains(historyQuery, ignoreCase = true) ||
                            it.url.contains(historyQuery, ignoreCase = true)
                    }

                    if (filtered.isEmpty()) {
                        Text("ما فيه نتائج")
                    } else {
                        LazyColumn {
                            items(filtered) { entry: HistoryEntry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            activeTab?.webView?.loadUrl(entry.url)
                                            showHistoryDialog = false
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.title, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            entry.url,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    HistoryManager.clear(context)
                    historyItems = emptyList()
                }) { Text("مسح السجل") }
            },
            dismissButton = {
                TextButton(onClick = { showHistoryDialog = false }) { Text("إغلاق") }
            }
        )
    }

    if (showSecurityDialog) {
        val url = activeTab?.url.orEmpty()
        val host = try { Uri.parse(url).host } catch (e: Exception) { null }
        val isHttps = url.startsWith("https://")
        AlertDialog(
            onDismissRequest = { showSecurityDialog = false },
            title = { Text(if (isHttps) "الاتصال مشفّر" else "الاتصال غير مشفّر") },
            text = {
                Column {
                    Text("الموقع: ${host ?: "—"}")
                    Spacer(modifier = Modifier.padding(top = 6.dp))
                    Text(
                        if (isHttps) {
                            "البيانات بين جهازك وهذا الموقع مشفّرة عبر HTTPS."
                        } else {
                            "تنبيه: هذا الموقع يستخدم HTTP بدون تشفير — أي بيانات ترسلها له ممكن تكون مكشوفة على الشبكة."
                        }
                    )
                    Spacer(modifier = Modifier.padding(top = 6.dp))
                    Text(
                        if (isProxyOn) {
                            "التصفح حالياً عبر بروكسي: ${proxy.host}:${proxy.port}"
                        } else {
                            "التصفح حالياً باتصال مباشر (بدون بروكسي)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSecurityDialog = false }) { Text("إغلاق") }
            }
        )
    }

    longPressUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { longPressUrl = null },
            title = { Text("خيارات الرابط") },
            text = { Text(url, maxLines = 2, style = MaterialTheme.typography.bodySmall) },
            confirmButton = {
                TextButton(onClick = {
                    openTab(url, activate = false)
                    longPressUrl = null
                }) { Text("فتح بتبويب جديد") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(ClipData.newPlainText("link", url))
                        longPressUrl = null
                    }) { Text("نسخ") }
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, url)
                        }
                        context.startActivity(Intent.createChooser(intent, "مشاركة الرابط").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                        longPressUrl = null
                    }) { Text("مشاركة") }
                }
            }
        )
    }

    pendingPermission?.let { request ->
        val labels = request.resources.joinToString("، ") { permissionLabel(it) }
        AlertDialog(
            onDismissRequest = { request.deny(); pendingPermission = null },
            title = { Text("طلب صلاحية") },
            text = { Text("الموقع (${request.origin.host}) يطلب الوصول لـ: $labels") },
            confirmButton = {
                TextButton(onClick = {
                    request.grant(request.resources)
                    pendingPermission = null
                }) { Text("سماح") }
            },
            dismissButton = {
                TextButton(onClick = {
                    request.deny()
                    pendingPermission = null
                }) { Text("رفض") }
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

private fun openWithExternalPlayer(context: android.content.Context, url: String) {
    try {
        val mime = when {
            url.contains(".m3u8", ignoreCase = true) -> "application/x-mpegURL"
            url.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
            url.endsWith(".webm", ignoreCase = true) -> "video/webm"
            url.endsWith(".mkv", ignoreCase = true) -> "video/x-matroska"
            url.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
            url.endsWith(".m4a", ignoreCase = true) -> "audio/mp4"
            else -> "video/*"
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), mime)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "افتح المقطع باستخدام").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    } catch (e: Exception) {
        Toast.makeText(context, "ما فيه تطبيق يقدر يفتح هذا المقطع", Toast.LENGTH_SHORT).show()
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

private fun buildGoogleTranslateUrl(originalUrl: String): String {
    val encoded = URLEncoder.encode(originalUrl, "UTF-8")
    // نستخدم خدمة ترجمة جوجل كوسيط (بدون مفتاح API) — تجيب الصفحة وتترجمها
    // للعربية تلقائياً. الرجوع للصفحة الأصلية يصير بزر الرجوع العادي بالمتصفح.
    return "https://translate.google.com/translate?sl=auto&tl=ar&u=$encoded"
}

private fun normalizeUrl(input: String): String {
    val trimmed = input.trim()
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
}

/** إذا النص المكتوب مو شكل رابط (فيه مسافات أو ماكو نقطة)، نرسله كبحث جوجل بدل ما نحاول نفتحه كرابط مكسور. */
private fun resolveAddressInput(input: String): String {
    val trimmed = input.trim()
    val looksLikeUrl = !trimmed.contains(" ") && (trimmed.contains(".") || trimmed.startsWith("http"))
    return if (looksLikeUrl) {
        normalizeUrl(trimmed)
    } else {
        "https://www.google.com/search?q=" + URLEncoder.encode(trimmed, "UTF-8")
    }
}
