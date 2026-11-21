package com.proxybrowser.app.ui

import com.proxybrowser.app.MainActivity
import com.proxybrowser.app.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.clip
import com.proxybrowser.app.state.TabSession
import com.proxybrowser.app.ui.theme.warningColor

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.MediaStore
import android.speech.RecognizerIntent
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.proxybrowser.app.data.AdBlockManager
import com.proxybrowser.app.data.DomainVisitTracker
import com.proxybrowser.app.data.FavoriteItem
import com.proxybrowser.app.data.ForceDarkManager
import com.proxybrowser.app.data.HistoryEntry
import com.proxybrowser.app.data.HistoryManager
import com.proxybrowser.app.data.PrefsManager
import com.proxybrowser.app.data.ProxyBypassManager
import com.proxybrowser.app.data.UiPrefsManager
import com.proxybrowser.app.model.ProxyInfo
import com.proxybrowser.app.model.ProxyType
import com.proxybrowser.app.state.ThemeState
import com.proxybrowser.app.state.ShortcutAction
import com.proxybrowser.app.state.ShortcutIntentState
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

/** روابط ما يفتحها المتصفح نفسه: أي بروتوكول غير هذي يُمرَّر لتطبيق خارجي (whatsapp:// و tg:// و intent:// ...). */
private val INTERNAL_SCHEMES = setOf("http", "https", "about", "data", "blob", "file", "javascript", "content", "view-source")

private class ExternalLink(val intent: Intent, val fallbackUrl: String?)

private class PendingExternalOpen(val link: ExternalLink, val key: String, val tab: TabState)

/** يحوّل الرابط لـ Intent لو كان يخص تطبيق خارجي، وإلا يرجع null (يعني المتصفح يفتحه عادي). */
private fun parseExternalLink(url: String): ExternalLink? {
    return try {
        val uri = Uri.parse(url)
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme in INTERNAL_SCHEMES) return null
        if (scheme == "intent") {
            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            val fallback = intent.getStringExtra("browser_fallback_url")
            // تنظيف أمني: ما نسمح للصفحة تحدد مكوّن معيّن أو صلاحيات وصول للملفات
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.component = null
            intent.selector = null
            val grantFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            intent.flags = intent.flags and grantFlags.inv()
            ExternalLink(intent, fallback)
        } else {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            ExternalLink(intent, null)
        }
    } catch (e: Exception) {
        null
    }
}

/** يفتح التطبيق الخارجي؛ لو غير مثبت يستخدم رابط الاحتياط (إن وُجد) أو يعرض تنبيه. */
private fun launchExternal(context: Context, link: ExternalLink, onFallback: (String) -> Unit) {
    try {
        context.startActivity(link.intent)
    } catch (e: ActivityNotFoundException) {
        val fallback = link.fallbackUrl
        if (fallback != null && (fallback.startsWith("http://") || fallback.startsWith("https://"))) {
            onFallback(fallback)
        } else {
            Toast.makeText(context, "التطبيق غير مثبت على الجهاز", Toast.LENGTH_SHORT).show()
        }
    } catch (e: SecurityException) {
        Toast.makeText(context, "تعذّر فتح التطبيق", Toast.LENGTH_SHORT).show()
    }
}

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
    var lastProcessedUrl: String? = null
    var proxyRetryCount = 0
    var favicon by mutableStateOf<Bitmap?>(null)
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

// يراقب تحديد النص بالصفحة عشان زر "ترجم النص المحدد"
private const val SELECTION_WATCH_JS = """
(function() {
    try {
        document.addEventListener('selectionchange', function() {
            var sel = window.getSelection().toString();
            if (sel && sel.length > 0 && sel.length < 500) {
                AndroidSelection.onSelection(sel);
            } else {
                AndroidSelection.onSelection('');
            }
        });
    } catch (e) {}
})();
"""

/** جسر JavaScript يستقبل النص المحدد حالياً بالصفحة (لزر الترجمة السريعة). */
private class SelectionBridge(private val onSelected: (String) -> Unit) {
    @JavascriptInterface
    fun onSelection(text: String) {
        Handler(Looper.getMainLooper()).post { onSelected(text) }
    }
}

// يفحص الصفحة عن وجود حقل كلمة مرور (لتنبيه المستخدم وهو يتصفح عبر بروكسي)
private const val PASSWORD_FIELD_SCAN_JS = """
(function() {
    try {
        var hasPassword = document.querySelector('input[type="password"]') !== null;
        AndroidSecurity.onPasswordFieldFound(hasPassword);
    } catch (e) {}
})();
"""

private class SecurityBridge(private val onResult: (Boolean) -> Unit) {
    @JavascriptInterface
    fun onPasswordFieldFound(found: Boolean) {
        Handler(Looper.getMainLooper()).post { onResult(found) }
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

private val AVATAR_COLORS = listOf(
    Color(0xFF5C6BC0), Color(0xFF26A69A), Color(0xFFEF5350),
    Color(0xFFAB47BC), Color(0xFF66BB6A), Color(0xFFFFA726),
    Color(0xFF29B6F6), Color(0xFF8D6E63)
)

@Composable
private fun SiteAvatar(label: String, sizeDp: androidx.compose.ui.unit.Dp = 28.dp) {
    val letter = label.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val color = AVATAR_COLORS[(label.hashCode().and(Int.MAX_VALUE)) % AVATAR_COLORS.size]
    Box(
        modifier = Modifier
            .size(sizeDp)
            .background(color, shape = androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(letter, color = Color.White, fontSize = 12.sp)
    }
}

private fun isOnWifi(context: android.content.Context): Boolean {
    return try {
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
        val network = cm?.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
    } catch (e: Exception) {
        false
    }
}

/** قياس بينج حي فعلي عبر البروكسي الحالي (طلب خفيف لجوجل، يقيس زمن الاستجابة فقط). */
private suspend fun measureLivePing(proxyInfo: ProxyInfo): Long? = withContext(Dispatchers.IO) {
    try {
        val type = if (proxyInfo.type == ProxyType.SOCKS5) java.net.Proxy.Type.SOCKS else java.net.Proxy.Type.HTTP
        val javaProxy = java.net.Proxy(type, java.net.InetSocketAddress(proxyInfo.host, proxyInfo.port))
        val client = okhttp3.OkHttpClient.Builder()
            .proxy(javaProxy)
            .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        val request = okhttp3.Request.Builder().url("https://www.google.com/generate_204").build()
        val start = System.currentTimeMillis()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) System.currentTimeMillis() - start else null
        }
    } catch (e: Exception) {
        null
    }
}

private fun permissionLabel(resource: String): String = when (resource) {
    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> "الكاميرا"
    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> "المايكروفون"
    else -> resource
}

/**
 * أندرويد الحديث (WebView/Chromium الحديثة) يقرر تفعيل التظليل الخوارزمي
 * (الوضع الداكن القسري) بالاعتماد على Configuration.uiMode الفعلي للـ Context
 * اللي انبنى منه الـ WebView، مو بس على علم setForceDark وحده. لهذا نلف
 * الـ Context بإعداد ليلي/نهاري صريح حسب الحاجة قبل إنشاء كل WebView.
 */
private fun darkAwareContext(base: android.content.Context, dark: Boolean): android.content.Context {
    val config = android.content.res.Configuration(base.resources.configuration)
    config.uiMode = (config.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or
        if (dark) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        } else {
            android.content.res.Configuration.UI_MODE_NIGHT_NO
        }
    return base.createConfigurationContext(config)
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
    proxy: ProxyInfo?,
    onProxyLost: () -> Unit,
    onSetProxyEnabled: (Boolean) -> Unit
) {
    val context = LocalContext.current

    val tabs = remember { androidx.compose.runtime.mutableStateListOf<TabState>() }
    var activeTabId by remember { mutableStateOf(-1) }
    var containerRef by remember { mutableStateOf<FrameLayout?>(null) }
    var swipeRefreshRef by remember { mutableStateOf<SwipeRefreshLayout?>(null) }
    var addressBarText by remember { mutableStateOf("") }

    var proxyReady by remember { mutableStateOf(false) }
    var proxySupported by remember { mutableStateOf(true) }
    // proxy == null يعني المستخدم موقف البروكسي (تصفح مباشر)
    val isProxyOn = proxy != null
    // نسخة محدّثة دائماً للاستخدام داخل كولباكات WebView طويلة العمر
    val currentProxy by rememberUpdatedState(proxy)
    var showProxyDialog by remember { mutableStateOf(false) }
    var showDisableConfirm by remember { mutableStateOf(false) }
    // رابط يخص تطبيق خارجي ينتظر اختيار المستخدم (فتح دائماً / مرة واحدة)
    var pendingExternal by remember { mutableStateOf<PendingExternalOpen?>(null) }
    // تبويب منبثق فاضي نبي نسكّره بعد ما فتحنا الرابط في التطبيق الخارجي
    var tabToClose by remember { mutableStateOf<Int?>(null) }
    var livePingMs by remember { mutableStateOf<Long?>(null) }

    var showUiCustomizeDialog by remember { mutableStateOf(false) }
    var addressBarPosition by remember { mutableStateOf(UiPrefsManager.getAddressBarPosition(context)) }
    var addressBarShape by remember { mutableStateOf(UiPrefsManager.getAddressBarShape(context)) }
    var uiScale by remember { mutableStateOf(UiPrefsManager.getUiScale(context)) }
    var toolbarOrder by remember { mutableStateOf(UiPrefsManager.getToolbarOrder(context)) }
    var toolbarHidden by remember { mutableStateOf(UiPrefsManager.getToolbarHidden(context)) }

    val addressBarCorner = if (addressBarShape == "square") 4.dp else 24.dp
    val iconButtonSize = when (uiScale) {
        "compact" -> 36.dp
        "comfortable" -> 52.dp
        else -> 44.dp
    }
    val iconGlyphSize = when (uiScale) {
        "compact" -> 18.dp
        "comfortable" -> 26.dp
        else -> 22.dp
    }
    val addressBarVPad = when (uiScale) {
        "compact" -> 3.dp
        "comfortable" -> 10.dp
        else -> 6.dp
    }
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
    val coroutineScope = rememberCoroutineScope()

    var showFindBar by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var findMatchInfo by remember { mutableStateOf("") }
    var selectedText by remember { mutableStateOf("") }
    var translationResult by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var fullscreenView by remember { mutableStateOf<View?>(null) }
    var fullscreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var showAdBlockStatsDialog by remember { mutableStateOf(false) }
    var showSensitiveLoginWarning by remember { mutableStateOf(false) }
    var showWhatsNewDialog by remember { mutableStateOf(false) }

    fun exportFavorites() {
        val json = org.json.JSONArray().apply {
            favorites.forEach {
                put(org.json.JSONObject().apply { put("title", it.title); put("url", it.url) })
            }
        }.toString(2)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, json)
            putExtra(Intent.EXTRA_SUBJECT, "مفضلة Proxy Browser")
        }
        context.startActivity(Intent.createChooser(intent, "تصدير المفضلة").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    val importFavoritesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    val arr = org.json.JSONArray(text)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        PrefsManager.addFavorite(context, obj.optString("title"), obj.optString("url"))
                    }
                    favorites = PrefsManager.getFavorites(context)
                    Toast.makeText(context, "تم استيراد المفضلة", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "ملف غير صالح للاستيراد", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importChromeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    val regex = Regex("<A[^>]*HREF=\"([^\"]+)\"[^>]*>([^<]*)</A>", RegexOption.IGNORE_CASE)
                    var count = 0
                    regex.findAll(text).forEach { match ->
                        val linkUrl = match.groupValues[1]
                        val linkTitle = match.groupValues[2].ifBlank { linkUrl }
                        if (linkUrl.startsWith("http")) {
                            PrefsManager.addFavorite(context, linkTitle, linkUrl)
                            count++
                        }
                    }
                    favorites = PrefsManager.getFavorites(context)
                    Toast.makeText(context, "تم استيراد $count رابط من كروم", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "تعذّر قراءة ملف المفضلة", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                addressBarInput = spoken
                tabs.find { it.id == activeTabId }?.webView?.loadUrl(resolveAddressInput(spoken))
                addressBarEditing = false
            }
        }
    }

    val qrLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (!contents.isNullOrBlank()) {
            tabs.find { it.id == activeTabId }?.webView?.loadUrl(resolveAddressInput(contents))
            addressBarEditing = false
        }
    }
    var proxyBypassVersion by remember { mutableStateOf(0) }

    val isDarkTheme by ThemeState.isDarkTheme.collectAsState()

    val activeTab = tabs.find { it.id == activeTabId }

    fun switchTab(id: Int) {
        tabs.forEach { it.webView?.visibility = if (it.id == id) View.VISIBLE else View.GONE }
        activeTabId = id
        addressBarText = tabs.find { it.id == id }?.url ?: ""
    }

    fun handleExternalLink(link: ExternalLink, tab: TabState) {
        val key = "scheme:" + (link.intent.scheme ?: "intent")
        if (PrefsManager.isExternalAlwaysAllowed(context, key)) {
            launchExternal(context, link) { fallback -> tab.webView?.loadUrl(fallback) }
        } else {
            pendingExternal = PendingExternalOpen(link, key, tab)
        }
    }

    fun configureWebView(tab: TabState): WebView {
        val initialHost = Uri.parse(tab.url).host
        val wantDark = ForceDarkManager.isEnabled(context) && !ForceDarkManager.isSiteExempt(context, initialHost)
        val webViewContext = if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            darkAwareContext(context, wantDark)
        } else {
            context
        }

        return WebView(webViewContext).apply {
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

            val dataSaverOn = PrefsManager.isDataSaverOn(context)
            settings.blockNetworkImage = dataSaverOn
            if (dataSaverOn) settings.mediaPlaybackRequiresUserGesture = true

            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(
                    settings,
                    if (wantDark) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
                )
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, wantDark)
                }
            }

            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            addJavascriptInterface(
                MediaBridge { links -> tab.mediaLinks = links },
                "AndroidMedia"
            )
            addJavascriptInterface(
                SelectionBridge { text -> if (tab.id == activeTabId) selectedText = text },
                "AndroidSelection"
            )
            addJavascriptInterface(
                SecurityBridge { found ->
                    if (found && currentProxy != null && tab.id == activeTabId &&
                        !PrefsManager.getSensitiveLoginWarningSuppressed(context)
                    ) {
                        showSensitiveLoginWarning = true
                    }
                },
                "AndroidSecurity"
            )

            setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
                if (tab.id == activeTabId && isDoneCounting) {
                    findMatchInfo = if (numberOfMatches > 0) {
                        "${activeMatchOrdinal + 1}/$numberOfMatches"
                    } else {
                        "0/0"
                    }
                }
            }

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
                    tab.proxyRetryCount = 0
                    tab.mediaLinks = emptyList()
                    if (tab.id == activeTabId) selectedText = ""

                    // نشغّل الفحوصات الثقيلة (فحص وسائط، مراقبة تحديد نص، وضع
                    // داكن قسري) مرة وحدة بس لكل رابط فعلي — بعض المواقع
                    // (زي جوجل) تستدعي onPageFinished عدة مرات لنفس الرابط
                    // بسبب تحديثات AJAX داخلية، وتكرار هالفحوصات كان يبطّئ
                    // ظهور المحتوى بشكل ملحوظ
                    if (url != null && url != tab.lastProcessedUrl) {
                        tab.lastProcessedUrl = url

                        view?.evaluateJavascript(MEDIA_SCAN_JS, null)
                        view?.evaluateJavascript(SELECTION_WATCH_JS, null)
                        view?.evaluateJavascript(PASSWORD_FIELD_SCAN_JS, null)
                        if (tab.readerMode) view?.evaluateJavascript(READER_MODE_JS, null)

                        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                            val host = Uri.parse(url).host
                            val enabled = ForceDarkManager.isEnabled(context) &&
                                !ForceDarkManager.isSiteExempt(context, host)
                            view?.settings?.let {
                                WebSettingsCompat.setForceDark(
                                    it,
                                    if (enabled) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
                                )
                            }
                        }
                    }

                    CookieManager.getInstance().flush()
                    swipeRefreshRef?.isRefreshing = false
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    if (request == null || !request.isForMainFrame) return false
                    val link = parseExternalLink(request.url.toString()) ?: return false
                    handleExternalLink(link, tab)
                    return true
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
                        AdBlockManager.recordBlocked()
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

                    // شبكة أمان: بعض الحالات (الكتابة بشريط العنوان، النوافذ المنبثقة) تتجاوز
                    // shouldOverrideUrlLoading وتطلع ERR_UNKNOWN_URL_SCHEME — نعالجها هنا
                    if (request?.isForMainFrame == true &&
                        error?.errorCode == WebViewClient.ERROR_UNSUPPORTED_SCHEME
                    ) {
                        val externalLink = parseExternalLink(request.url.toString())
                        if (externalLink != null) {
                            handleExternalLink(externalLink, tab)
                            view?.let { v ->
                                v.stopLoading()
                                if (v.canGoBack()) {
                                    v.goBack()
                                } else if (tab.url.isBlank()) {
                                    tabToClose = tab.id
                                } else {
                                    v.loadUrl(PrefsManager.getHomepage(context))
                                }
                            }
                            return
                        }
                    }

                    // نعيد الفحص فقط عند فشل اتصال حقيقي بالبروكسي، مو عند حظر
                    // إعلان أو منع نافذة منبثقة أو أي خطأ عادي ثاني
                    val errorCode = error?.errorCode
                    if (currentProxy != null &&
                        request?.isForMainFrame == true &&
                        !lostTriggered &&
                        errorCode != null &&
                        errorCode in PROXY_FAILURE_ERROR_CODES
                    ) {
                        // نعطي نفس البروكسي فرصتين قبل ما نعتبره ميت — بعض
                        // الانقطاعات مؤقتة (لحظية) وترجع تشتغل من نفسها
                        if (tab.proxyRetryCount < 2) {
                            tab.proxyRetryCount++
                            Handler(Looper.getMainLooper()).postDelayed({
                                view?.reload()
                            }, 1500)
                        } else {
                            lostTriggered = true
                            onProxyLost()
                        }
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

                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    super.onReceivedIcon(view, icon)
                    if (icon != null) tab.favicon = icon
                }

                override fun onPermissionRequest(request: PermissionRequest?) {
                    if (request == null) return
                    pendingPermission = request
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (view == null) return
                    fullscreenView = view
                    fullscreenCallback = callback
                }

                override fun onHideCustomView() {
                    fullscreenCallback?.onCustomViewHidden()
                    fullscreenView = null
                    fullscreenCallback = null
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

    /** يعيد بناء WebView التبويب بإعدادات جديدة (مطلوب للوضع الداكن القسري لأن الإعداد مربوط بالـ Context وقت الإنشاء). */
    fun recreateWebView(tab: TabState) {
        val url = tab.url
        val wasActive = tab.id == activeTabId
        val oldWebView = tab.webView
        containerRef?.removeView(oldWebView)
        oldWebView?.destroy()

        val newWebView = configureWebView(tab)
        tab.webView = newWebView
        containerRef?.addView(
            newWebView,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        newWebView.visibility = if (wasActive) View.VISIBLE else View.GONE
        newWebView.loadUrl(url)
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

    fun closeAllTabsExcept(keepId: Int) {
        val toClose = tabs.filter { it.id != keepId }
        toClose.forEach { t ->
            containerRef?.removeView(t.webView)
            t.webView?.destroy()
        }
        tabs.removeAll { it.id != keepId }
        if (activeTabId != keepId) switchTab(keepId)
    }

    fun closeAllTabs() {
        tabs.forEach { t ->
            containerRef?.removeView(t.webView)
            t.webView?.destroy()
        }
        tabs.clear()
        openTab(PrefsManager.getHomepage(context), activate = true)
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
    LaunchedEffect(tabToClose) {
        tabToClose?.let { id ->
            closeTab(id)
            tabToClose = null
        }
    }

    // نحفظ روابط التبويبات لما الشاشة تنهدم (مثلاً تفعيل البروكسي من جديد يمر بشاشة الفحص)
    DisposableEffect(Unit) {
        onDispose {
            TabSession.urls = tabs.map { it.url }
            TabSession.activeIndex = tabs.indexOfFirst { it.id == activeTabId }.coerceAtLeast(0)
        }
    }

    LaunchedEffect(Unit) {
        if (tabs.isEmpty()) {
            val saved = TabSession.urls
            if (saved.isNotEmpty()) {
                val activeIdx = TabSession.activeIndex.coerceIn(0, saved.lastIndex)
                TabSession.urls = emptyList()
                saved.forEachIndexed { index, savedUrl ->
                    openTab(savedUrl, activate = index == activeIdx)
                }
            } else {
                openTab(PrefsManager.getHomepage(context), activate = true)
            }
        }
        if (PrefsManager.shouldShowWhatsNew(context)) {
            showWhatsNewDialog = true
        }
    }

    // ينفّذ أوامر اختصارات التطبيق أو أيقونة موقع مثبّتة بالشاشة الرئيسية
    val shortcutAction by ShortcutIntentState.pendingAction.collectAsState()
    LaunchedEffect(shortcutAction) {
        when (val action = shortcutAction) {
            is ShortcutAction.NewTab -> {
                openTab(PrefsManager.getHomepage(context), activate = true)
                ShortcutIntentState.pendingAction.value = null
            }
            is ShortcutAction.Homepage -> {
                activeTab?.webView?.loadUrl(PrefsManager.getHomepage(context))
                ShortcutIntentState.pendingAction.value = null
            }
            is ShortcutAction.OpenUrl -> {
                openTab(action.url, activate = true)
                ShortcutIntentState.pendingAction.value = null
            }
            null -> {}
        }
    }

    // بينج حي فعلي، بس أثناء الواي فاي — على بيانات الجوال نبقي الرقم الثابت
    // من الفحص الأول كما هو (توفيراً للبيانات والبطارية)
    LaunchedEffect(isProxyOn, proxy) {
        while (true) {
            val activeProxy = proxy
            if (activeProxy != null && isOnWifi(context)) {
                livePingMs = measureLivePing(activeProxy)
            } else {
                livePingMs = null
            }
            delay(4000)
        }
    }

    // يفتح الكيبورد تلقائياً لما ندخل وضع التعديل بشريط العنوان
    LaunchedEffect(addressBarEditing) {
        if (addressBarEditing) {
            addressFocusRequester.requestFocus()
        }
    }

    // يقترح لصق رابط من الحافظة لو دخلنا وضع التعديل والحقل لسا فاضي
    var clipboardSuggestion by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(addressBarEditing) {
        clipboardSuggestion = null
        if (addressBarEditing) {
            try {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                val text = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
                if (!text.isNullOrBlank() && !text.contains(" ") &&
                    (text.startsWith("http://") || text.startsWith("https://"))
                ) {
                    clipboardSuggestion = text
                }
            } catch (e: Exception) {
                // تجاهل (حافظة فاضية أو غير نصية)
            }
        }
    }

    // اقتراحات بحث من جوجل أثناء الكتابة (بتأخير بسيط عشان ما نرسل طلب كل حرف)
    LaunchedEffect(addressBarInput, addressBarEditing) {
        if (addressBarEditing && addressBarInput.isNotBlank()) {
            delay(300)
            suggestions = SuggestionsFetcher.fetch(addressBarInput, proxy)
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
        } else if (proxy != null) {
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

    // Box خارجي يلف كل شي عشان طبقة الفيديو بملء الشاشة (لو انفعلت) تغطي
    // حتى الشريط العلوي وشريط العنوان، مو بس منطقة عرض الصفحة
    Box(modifier = Modifier.fillMaxSize()) {
    val toolbarContent: @Composable () -> Unit = {
                // شريط عنوان الموقع لوحده — مستطيل مدوّر بعرض الشاشة كامل
                Column {
                    Surface(
                        shape = RoundedCornerShape(addressBarCorner),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            width = if (addressBarEditing) 2.dp else 1.dp,
                            color = if (addressBarEditing) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = addressBarVPad),
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
                                IconButton(
                                    onClick = {
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(
                                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                            )
                                        }
                                        try {
                                            voiceLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "البحث الصوتي غير متوفر بجهازك", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Mic, contentDescription = "بحث صوتي", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        qrLauncher.launch(
                                            ScanOptions()
                                                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                                .setPrompt("وجّه الكاميرا نحو رمز QR")
                                                .setBeepEnabled(false)
                                        )
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.QrCodeScanner, contentDescription = "مسح QR", modifier = Modifier.size(16.dp))
                                }
                            } else {
                                Text(
                                    text = (
                                        activeTab?.url
                                            ?.let { runCatching { Uri.parse(it).host }.getOrNull() }
                                            ?.removePrefix("www.")
                                            ?.ifBlank { null }
                                            ?: activeTab?.title?.ifBlank { null }
                                            ?: addressBarText
                                        ).ifBlank { "ابحث أو اكتب رابط" },
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

                    if (addressBarEditing && clipboardSuggestion != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                                .clickable {
                                    val link = clipboardSuggestion ?: return@clickable
                                    activeTab?.webView?.loadUrl(link)
                                    addressBarEditing = false
                                    clipboardSuggestion = null
                                }
                        ) {
                            Text(
                                "لصق وفتح: $clipboardSuggestion",
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
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
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (showFindBar) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = findQuery,
                            onValueChange = {
                                findQuery = it
                                activeTab?.webView?.findAllAsync(it)
                            },
                            singleLine = true,
                            placeholder = { Text("بحث بالصفحة") },
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            findMatchInfo,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                        IconButton(onClick = { activeTab?.webView?.findNext(false) }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "السابق")
                        }
                        IconButton(onClick = { activeTab?.webView?.findNext(true) }) {
                            Icon(Icons.Filled.ArrowForward, contentDescription = "التالي")
                        }
                        IconButton(onClick = {
                            activeTab?.webView?.clearMatches()
                            showFindBar = false
                            findQuery = ""
                            findMatchInfo = ""
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "إغلاق البحث")
                        }
                    }
                }
    }

    val navBarContent: @Composable () -> Unit = {
                // صف أزرار التنقل + مربع عدد التبويبات + المفضلة
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    toolbarOrder.forEach { id ->
                        if (id in toolbarHidden) return@forEach
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        when (id) {
                            "back" -> IconButton(
                                onClick = { activeTab?.webView?.goBack() },
                                modifier = Modifier.size(iconButtonSize)
                            ) {
                                Icon(
                                    Icons.Filled.ArrowBack,
                                    contentDescription = "رجوع",
                                    modifier = Modifier.size(iconGlyphSize),
                                    tint = if (activeTab?.canGoBack == true) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                )
                            }
                            "forward" -> IconButton(
                                onClick = { activeTab?.webView?.goForward() },
                                modifier = Modifier.size(iconButtonSize)
                            ) {
                                Icon(Icons.Filled.ArrowForward, contentDescription = "تقدم", modifier = Modifier.size(iconGlyphSize))
                            }
                            "refresh" -> IconButton(
                                onClick = { activeTab?.webView?.reload() },
                                modifier = Modifier.size(iconButtonSize)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = "تحديث", modifier = Modifier.size(iconGlyphSize))
                            }
                            "home" -> IconButton(
                                onClick = {
                                    val home = PrefsManager.getHomepage(context)
                                    activeTab?.webView?.loadUrl(home)
                                },
                                modifier = Modifier.size(iconButtonSize)
                            ) {
                                Icon(Icons.Filled.Home, contentDescription = "الرئيسية", modifier = Modifier.size(iconGlyphSize))
                            }
                            "star" -> IconButton(
                                onClick = {
                                    PrefsManager.addFavorite(context, activeTab?.title ?: addressBarText, addressBarText)
                                    favorites = PrefsManager.getFavorites(context)
                                },
                                modifier = Modifier.size(iconButtonSize)
                            ) {
                                Icon(Icons.Filled.Star, contentDescription = "إضافة للمفضلة", modifier = Modifier.size(iconGlyphSize))
                            }
                            "favorites" -> IconButton(
                                onClick = {
                                    favorites = PrefsManager.getFavorites(context)
                                    favoritesQuery = ""
                                    showFavoritesDialog = true
                                },
                                modifier = Modifier.size(iconButtonSize)
                            ) {
                                Icon(Icons.Filled.List, contentDescription = "المفضلة", modifier = Modifier.size(iconGlyphSize))
                            }
                        }
                        }
                    }

                    if ((activeTab?.mediaLinks?.size ?: 0) > 0) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        IconButton(onClick = { showMediaDialog = true }, modifier = Modifier.size(iconButtonSize)) {
                            Icon(
                                Icons.Filled.PlayCircle,
                                contentDescription = "مقاطع مكتشفة بالصفحة",
                                modifier = Modifier.size(iconGlyphSize),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        }
                    }

                    // مربع صغير فيه رقم عدد التبويبات — الضغط عليه يفتح قائمة التبويبات
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Transparent,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size((iconButtonSize - 14.dp).coerceAtLeast(26.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showTabsDialog = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = tabs.size.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    }
                }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                // صف الحالة المضغوط: كبسولة البروكسي + أزرار سريعة (بدل شريط التطبيق الكبير)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 4.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        val currentHost = activeTab?.url?.let { Uri.parse(it).host }
                        val isBypassed = ProxyBypassManager.isBypassed(context, currentHost)
                        val accent = when {
                            isBypassed -> MaterialTheme.colorScheme.secondary
                            proxy != null -> MaterialTheme.colorScheme.tertiary
                            else -> warningColor()
                        }
                        val statusTitle = when {
                            isBypassed -> "استثناء لهذا الموقع"
                            proxy != null -> "محمي بالبروكسي"
                            else -> "البروكسي موقوف"
                        }
                        val statusDetail = when {
                            isBypassed -> "اتصال مباشر لهذا الموقع فقط"
                            proxy != null -> {
                                val pingText = livePingMs?.let { "${it}ms 📶" }
                                    ?: proxy.latencyMs?.let { "${it}ms" }
                                    ?: "—"
                                "${proxy.host}:${proxy.port} • $pingText"
                            }
                            else -> "اتصال مباشر — اضغط للتفاصيل"
                        }
                        ProxyStatusPill(
                            title = statusTitle,
                            detail = statusDetail,
                            accent = accent,
                            onClick = { showProxyDialog = true }
                        )
                    }
                        // زر تشغيل/إيقاف البروكسي بالكامل (الإيقاف يحتاج تأكيد ويتحفظ)
                        IconButton(onClick = {
                            if (isProxyOn) showDisableConfirm = true else onSetProxyEnabled(true)
                        }) {
                            Icon(
                                imageVector = Icons.Filled.PowerSettingsNew,
                                contentDescription = if (isProxyOn) "إيقاف البروكسي" else "تفعيل البروكسي",
                                tint = if (isProxyOn) MaterialTheme.colorScheme.tertiary else warningColor()
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
                                HorizontalDivider()
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
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(if (isDarkTheme) "✓ الثيم الداكن" else "الثيم الداكن") },
                                    leadingIcon = { Icon(Icons.Filled.DarkMode, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        ThemeState.toggle(context)
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        val on = ForceDarkManager.isEnabled(context)
                                        Text(if (on) "✓ وضع داكن قسري للمواقع" else "وضع داكن قسري للمواقع")
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        ForceDarkManager.setEnabled(context, !ForceDarkManager.isEnabled(context))
                                        tabs.forEach { recreateWebView(it) }
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        val host = activeTab?.url?.let { Uri.parse(it).host }
                                        val exempt = ForceDarkManager.isSiteExempt(context, host)
                                        Text(if (exempt) "✓ تعطيل الوضع الداكن لهذا الموقع" else "تعطيل الوضع الداكن لهذا الموقع")
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        val host = activeTab?.url?.let { Uri.parse(it).host }
                                        ForceDarkManager.toggleSiteExemption(context, host)
                                        activeTab?.let { recreateWebView(it) }
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        val on = PrefsManager.isDataSaverOn(context)
                                        Text(if (on) "✓ وضع توفير البيانات" else "وضع توفير البيانات")
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        val newValue = !PrefsManager.isDataSaverOn(context)
                                        PrefsManager.setDataSaverOn(context, newValue)
                                        tabs.forEach {
                                            it.webView?.settings?.blockNetworkImage = newValue
                                            it.webView?.reload()
                                        }
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("بحث بالصفحة الحالية") },
                                    leadingIcon = { Icon(Icons.Filled.FindInPage, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showFindBar = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("قص شاشة كاملة للصفحة") },
                                    leadingIcon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        captureFullPageScreenshot(context, activeTab?.webView)
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("تصدير المفضلة") },
                                    leadingIcon = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        exportFavorites()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("استيراد المفضلة") },
                                    leadingIcon = { Icon(Icons.Filled.FileUpload, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        importFavoritesLauncher.launch("*/*")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("استيراد مفضلة من كروم") },
                                    onClick = {
                                        showOverflowMenu = false
                                        importChromeLauncher.launch("text/html")
                                    }
                                )
                                if (isProxyOn) {
                                    DropdownMenuItem(
                                        text = { Text("بدّل البروكسي الآن") },
                                        onClick = {
                                            showOverflowMenu = false
                                            onProxyLost()
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("مسح اختيارات فتح التطبيقات") },
                                    onClick = {
                                        showOverflowMenu = false
                                        PrefsManager.clearExternalAlwaysAllowed(context)
                                        Toast.makeText(context, "تم المسح — سيسألك عند كل رابط تطبيق", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("تثبيت هذا الموقع بالشاشة الرئيسية") },
                                    onClick = {
                                        showOverflowMenu = false
                                        activeTab?.let { pinSiteToHomeScreen(context, it) }
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("إحصائية حظر الإعلانات") },
                                    onClick = {
                                        showOverflowMenu = false
                                        showAdBlockStatsDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("الجديد بهذا التحديث") },
                                    onClick = {
                                        showOverflowMenu = false
                                        showWhatsNewDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("تخصيص الواجهة") },
                                    onClick = {
                                        showOverflowMenu = false
                                        showUiCustomizeDialog = true
                                    }
                                )
                            }
                        }
                    }

                if (!isProxyOn) {
                    ProxyDisabledBanner(onEnable = { onSetProxyEnabled(true) })
                }

                if (addressBarPosition == "top") {
                    toolbarContent()
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        bottomBar = {
            // شريط سفلي دائماً: (شريط العنوان لو اخترت مكانه تحت) + أزرار التنقل
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (addressBarPosition == "bottom") {
                    toolbarContent()
                }
                navBarContent()
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

            if (selectedText.isNotBlank() && proxyReady && proxySupported) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(bottom = 24.dp)
                            .clickable {
                                val textToTranslate = selectedText
                                isTranslating = true
                                coroutineScope.launch {
                                    translationResult = translateSelectedText(
                                        textToTranslate,
                                        proxy
                                    )
                                    isTranslating = false
                                }
                            }
                    ) {
                        Text(
                            if (isTranslating) "جاري الترجمة..." else "ترجم النص المحدد",
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }

    pendingExternal?.let { pending ->
        val appLabel = remember(pending) {
            runCatching {
                pending.link.intent.`package`?.let { pkg ->
                    val pm = context.packageManager
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                }
            }.getOrNull()
        }
        val schemeLabel = pending.link.intent.scheme ?: "تطبيق"
        AlertDialog(
            onDismissRequest = { pendingExternal = null },
            icon = { Icon(Icons.Filled.OpenInNew, contentDescription = null) },
            title = { Text("فتح في تطبيق؟", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (appLabel != null) {
                            "هذا الرابط يريد فتح تطبيق «$appLabel»."
                        } else {
                            "هذا الرابط يريد فتح تطبيق خارجي ($schemeLabel)."
                        }
                    )
                    Button(
                        onClick = {
                            PrefsManager.addExternalAlwaysAllowed(context, pending.key)
                            launchExternal(context, pending.link) { fb -> pending.tab.webView?.loadUrl(fb) }
                            pendingExternal = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("فتح دائماً") }
                    OutlinedButton(
                        onClick = {
                            launchExternal(context, pending.link) { fb -> pending.tab.webView?.loadUrl(fb) }
                            pendingExternal = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("فتح مرة واحدة") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pendingExternal = null }) { Text("إلغاء") }
            }
        )
    }

    if (showProxyDialog) {
        ProxyControlDialog(
            proxy = proxy,
            livePingMs = livePingMs,
            onToggle = { wantOn ->
                showProxyDialog = false
                if (wantOn) onSetProxyEnabled(true) else showDisableConfirm = true
            },
            onSwitchProxy = {
                showProxyDialog = false
                onProxyLost()
            },
            onDismiss = { showProxyDialog = false }
        )
    }

    if (showDisableConfirm) {
        DisableProxyConfirmDialog(
            onConfirm = {
                showDisableConfirm = false
                onSetProxyEnabled(false)
            },
            onDismiss = { showDisableConfirm = false }
        )
    }

    fullscreenView?.let { fsView ->
        AndroidView(
            factory = { fsView },
            modifier = Modifier.fillMaxSize()
        )
        BackHandler(enabled = true) {
            fullscreenCallback?.onCustomViewHidden()
            fullscreenView = null
            fullscreenCallback = null
        }
    }
    } // إغلاق الـ Box الخارجي

    translationResult?.let { result ->
        AlertDialog(
            onDismissRequest = { translationResult = null },
            title = { Text("الترجمة") },
            text = { Text(result) },
            confirmButton = {
                TextButton(onClick = { translationResult = null }) { Text("إغلاق") }
            }
        )
    }

    if (showTabsDialog) {
        AlertDialog(
            onDismissRequest = { showTabsDialog = false },
            title = { Text("التبويبات (${tabs.size})") },
            text = {
                val grouped = tabs.groupBy { it.groupName ?: "" }
                LazyColumn {
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                            TextButton(onClick = {
                                activeTab?.let { closeAllTabsExcept(it.id) }
                            }) { Text("إغلاق الكل ما عدا هذا") }
                            TextButton(onClick = { closeAllTabs() }) { Text("إغلاق الكل") }
                        }
                    }
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
                                val icon = tab.favicon
                                if (icon != null) {
                                    Image(
                                        bitmap = icon.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    SiteAvatar(tab.title, sizeDp = 24.dp)
                                }
                                Spacer(modifier = Modifier.padding(start = 8.dp))
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

    if (showSensitiveLoginWarning) {
        AlertDialog(
            onDismissRequest = { showSensitiveLoginWarning = false },
            title = { Text("⚠️ تنبيه") },
            text = {
                Text(
                    "هذي الصفحة تطلب كلمة مرور، وأنت متصل عبر بروكسي مجهول المصدر. " +
                        "تجنّب إدخال بيانات حساسة (بنكية أو حسابات مهمة) عبر بروكسيات عامة غير موثوقة."
                )
            },
            confirmButton = {
                TextButton(onClick = { showSensitiveLoginWarning = false }) { Text("فهمت") }
            },
            dismissButton = {
                TextButton(onClick = {
                    PrefsManager.setSensitiveLoginWarningSuppressed(context, true)
                    showSensitiveLoginWarning = false
                }) { Text("لا تعرضه مرة ثانية") }
            }
        )
    }

    if (showAdBlockStatsDialog) {
        LaunchedEffect(Unit) { AdBlockManager.persistSessionCount(context) }
        val total = AdBlockManager.getTotalBlockedCount(context)
        AlertDialog(
            onDismissRequest = { showAdBlockStatsDialog = false },
            title = { Text("إحصائية حظر الإعلانات") },
            text = { Text("تم حظر $total طلب إعلان/تتبع منذ تثبيت التطبيق.") },
            confirmButton = {
                TextButton(onClick = { showAdBlockStatsDialog = false }) { Text("إغلاق") }
            }
        )
    }

    if (showUiCustomizeDialog) {
        AlertDialog(
            onDismissRequest = { showUiCustomizeDialog = false },
            title = { Text("تخصيص الواجهة") },
            text = {
                LazyColumn {
                    item {
                        Text("مكان شريط العنوان", style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            TextButton(onClick = {
                                addressBarPosition = "top"
                                UiPrefsManager.setAddressBarPosition(context, "top")
                            }) { Text(if (addressBarPosition == "top") "✓ أعلى" else "أعلى") }
                            TextButton(onClick = {
                                addressBarPosition = "bottom"
                                UiPrefsManager.setAddressBarPosition(context, "bottom")
                            }) { Text(if (addressBarPosition == "bottom") "✓ أسفل" else "أسفل") }
                        }

                        Spacer(modifier = Modifier.padding(top = 10.dp))
                        Text("شكل شريط العنوان", style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            TextButton(onClick = {
                                addressBarShape = "rounded"
                                UiPrefsManager.setAddressBarShape(context, "rounded")
                            }) { Text(if (addressBarShape == "rounded") "✓ مستدير" else "مستدير") }
                            TextButton(onClick = {
                                addressBarShape = "square"
                                UiPrefsManager.setAddressBarShape(context, "square")
                            }) { Text(if (addressBarShape == "square") "✓ مربع" else "مربع") }
                        }

                        Spacer(modifier = Modifier.padding(top = 10.dp))
                        Text("حجم عناصر الواجهة", style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            listOf("compact" to "مضغوط", "normal" to "عادي", "comfortable" to "مريح").forEach { (key, label) ->
                                TextButton(onClick = {
                                    uiScale = key
                                    UiPrefsManager.setUiScale(context, key)
                                }) { Text(if (uiScale == key) "✓ $label" else label) }
                            }
                        }

                        Spacer(modifier = Modifier.padding(top = 12.dp))
                        Text("أزرار شريط الأدوات (ترتيب/إخفاء)", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "اضغط ↑↓ لتغيير الترتيب، وعلّم/أزل العلامة لإظهار أو إخفاء الزر",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    itemsIndexed(toolbarOrder) { index, id ->
                        val isHidden = id in toolbarHidden
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val newOrder = toolbarOrder.toMutableList()
                                        val tmp = newOrder[index - 1]
                                        newOrder[index - 1] = newOrder[index]
                                        newOrder[index] = tmp
                                        toolbarOrder = newOrder
                                        UiPrefsManager.setToolbarOrder(context, newOrder)
                                    }
                                },
                                enabled = index > 0,
                                modifier = Modifier.size(32.dp)
                            ) { Text("↑") }
                            IconButton(
                                onClick = {
                                    if (index < toolbarOrder.size - 1) {
                                        val newOrder = toolbarOrder.toMutableList()
                                        val tmp = newOrder[index + 1]
                                        newOrder[index + 1] = newOrder[index]
                                        newOrder[index] = tmp
                                        toolbarOrder = newOrder
                                        UiPrefsManager.setToolbarOrder(context, newOrder)
                                    }
                                },
                                enabled = index < toolbarOrder.size - 1,
                                modifier = Modifier.size(32.dp)
                            ) { Text("↓") }
                            Text(
                                UiPrefsManager.TOOLBAR_LABELS[id] ?: id,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp),
                                style = if (isHidden) {
                                    MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    MaterialTheme.typography.bodyMedium
                                }
                            )
                            TextButton(onClick = {
                                val newHidden = toolbarHidden.toMutableSet()
                                if (isHidden) newHidden.remove(id) else newHidden.add(id)
                                toolbarHidden = newHidden
                                UiPrefsManager.setToolbarHidden(context, newHidden)
                            }) {
                                Text(if (isHidden) "إظهار" else "إخفاء")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUiCustomizeDialog = false }) { Text("تم") }
            }
        )
    }

    if (showWhatsNewDialog) {
        AlertDialog(
            onDismissRequest = {
                showWhatsNewDialog = false
                PrefsManager.markWhatsNewSeen(context)
            },
            title = { Text("الجديد بهذا التحديث") },
            text = {
                Column {
                    Text("• الروابط اللي تفتح تطبيقات (واتساب، تيليجرام...) صارت تشتغل مع خيار «فتح دائماً» أو «مرة واحدة»")
                    Text("• زر إيقاف البروكسي بالكامل — يتذكّر اختيارك حتى بعد إغلاق التطبيق")
                    Text("• تصميم جديد: ثيم عصري، شريط حالة ذكي، وشاشة فحص متحركة")
                    Text("• إصلاح الوضع الداكن القسري للمواقع")
                    Text("• إصلاح تدوير الشاشة (ما يعيد فتح التطبيق من جديد)")
                    Text("• دعم تشغيل الفيديو بملء الشاشة")
                    Text("• الصق وافتح الروابط من الحافظة مباشرة")
                    Text("• اختصارات تطبيق وتثبيت مواقع بالشاشة الرئيسية")
                    Text("• إحصائية حظر الإعلانات")
                    Text("• إغلاق تبويبات بالجملة")
                    Text("• أيقونات المواقع بالتبويبات والسجل والمفضلة")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showWhatsNewDialog = false
                    PrefsManager.markWhatsNewSeen(context)
                }) { Text("تمام") }
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
                                    SiteAvatar(fav.title)
                                    Spacer(modifier = Modifier.padding(start = 8.dp))
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
                                    SiteAvatar(entry.title)
                                    Spacer(modifier = Modifier.padding(start = 8.dp))
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
                        if (proxy != null) {
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

/** يترجم نص محدد (بدون فتح صفحة جديدة) عبر خدمة جوجل، بنفس بروكسي التصفح الحالي. */
private suspend fun translateSelectedText(text: String, proxyInfo: ProxyInfo?): String =
    withContext(Dispatchers.IO) {
        try {
            val builder = okhttp3.OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)

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

            val url = "https://translate.googleapis.com/translate_a/single" +
                "?client=gtx&sl=auto&tl=ar&dt=t&q=" + URLEncoder.encode(text, "UTF-8")
            val request = okhttp3.Request.Builder().url(url).build()

            builder.build().newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@withContext "تعذّرت الترجمة"
                val segments = org.json.JSONArray(body).optJSONArray(0) ?: return@withContext "تعذّرت الترجمة"
                val sb = StringBuilder()
                for (i in 0 until segments.length()) {
                    sb.append(segments.optJSONArray(i)?.optString(0).orEmpty())
                }
                sb.toString().ifBlank { "تعذّرت الترجمة" }
            }
        } catch (e: Exception) {
            "تعذّرت الترجمة (تحقق من الاتصال)"
        }
    }

/**
 * يلتقط لقطة للصفحة كاملة (حتى الجزء اللي مو ظاهر بالشاشة) ويحفظها بمعرض الصور.
 * ملاحظة: يعتمد على أبعاد المحتوى المحسوبة من WebView، فبعض الصفحات ذات
 * التخطيط المعقّد (عناصر عائمة/تحميل كسول) ممكن ما تُلتقط بدقة كاملة.
 */
private fun captureFullPageScreenshot(context: android.content.Context, webView: WebView?) {
    if (webView == null) return
    try {
        val contentHeight = (webView.contentHeight * webView.scale).toInt().coerceAtLeast(webView.height)
        val bitmap = Bitmap.createBitmap(webView.width, contentHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        webView.draw(canvas)

        val filename = "page_${System.currentTimeMillis()}.png"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ProxyBrowser")
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Toast.makeText(context, "تم حفظ صورة الصفحة بمعرض الصور", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "تعذّر حفظ الصورة", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "تعذّر التقاط الصفحة", Toast.LENGTH_SHORT).show()
    }
}

private fun pinSiteToHomeScreen(context: android.content.Context, tab: TabState) {
    try {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(context, "جهازك ما يدعم تثبيت اختصارات بالشاشة الرئيسية", Toast.LENGTH_SHORT).show()
            return
        }
        val shortcutIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("proxybrowser://open?url=" + Uri.encode(tab.url)),
            context,
            MainActivity::class.java
        )
        val label = tab.title.ifBlank { tab.url }.take(30)

        // نستخدم أيقونة الموقع نفسه (favicon) إذا كانت متوفرة، وإلا نرجع
        // لأيقونة التطبيق كبديل احتياطي
        val siteIcon = tab.favicon?.let { fav ->
            try {
                val scaled = Bitmap.createScaledBitmap(fav, 108, 108, true)
                IconCompat.createWithBitmap(scaled)
            } catch (e: Exception) {
                null
            }
        } ?: IconCompat.createWithResource(context, R.mipmap.ic_launcher)

        val shortcut = ShortcutInfoCompat.Builder(context, "site_${tab.url.hashCode()}")
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(siteIcon)
            .setIntent(shortcutIntent)
            .build()
        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    } catch (e: Exception) {
        Toast.makeText(context, "تعذّر تثبيت الموقع", Toast.LENGTH_SHORT).show()
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
    val lower = trimmed.lowercase()
    val hasOtherScheme = Regex("^[a-z][a-z0-9+.-]*://").containsMatchIn(lower) &&
        !lower.startsWith("http://") && !lower.startsWith("https://")
    val isSpecialScheme = lower.startsWith("mailto:") || lower.startsWith("tel:") ||
        lower.startsWith("sms:") || lower.startsWith("intent:")
    if (hasOtherScheme || isSpecialScheme) return trimmed
    val looksLikeUrl = !trimmed.contains(" ") && (trimmed.contains(".") || trimmed.startsWith("http"))
    return if (looksLikeUrl) {
        normalizeUrl(trimmed)
    } else {
        "https://www.google.com/search?q=" + URLEncoder.encode(trimmed, "UTF-8")
    }
}
