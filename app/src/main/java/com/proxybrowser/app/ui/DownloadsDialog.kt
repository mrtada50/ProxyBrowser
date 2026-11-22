package com.proxybrowser.app.ui

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private class DownloadItem(
    val id: Long,
    val title: String,
    val status: Int,
    val totalBytes: Long,
    val soFarBytes: Long,
    val mimeType: String?,
    val modified: Long
)

/** يجيب كل التنزيلات اللي بدأها المتصفح من مدير التنزيلات في النظام. */
private fun queryDownloads(context: Context): List<DownloadItem> {
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val result = mutableListOf<DownloadItem>()
    try {
        manager.query(DownloadManager.Query())?.use { c ->
            val idCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
            val titleCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)
            val statusCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
            val totalCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val soFarCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val mimeCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE)
            val modCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)
            while (c.moveToNext()) {
                result.add(
                    DownloadItem(
                        id = c.getLong(idCol),
                        title = c.getString(titleCol) ?: "",
                        status = c.getInt(statusCol),
                        totalBytes = c.getLong(totalCol),
                        soFarBytes = c.getLong(soFarCol),
                        mimeType = c.getString(mimeCol),
                        modified = c.getLong(modCol)
                    )
                )
            }
        }
    } catch (e: Exception) {
        // نتجاهل — القائمة تبقى فاضية أو بآخر نتيجة
    }
    return result.sortedByDescending { it.modified }
}

private fun openDownload(context: Context, item: DownloadItem) {
    try {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = manager.getUriForDownloadedFile(item.id)
        if (uri == null) {
            Toast.makeText(context, "الملف غير موجود (ربما انحذف)", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, item.mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "ما في تطبيق يفتح هذا الملف", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "تعذّر فتح الملف", Toast.LENGTH_SHORT).show()
    }
}

private fun statusText(context: Context, item: DownloadItem): String {
    val size = if (item.totalBytes > 0) Formatter.formatShortFileSize(context, item.totalBytes) else ""
    return when (item.status) {
        DownloadManager.STATUS_SUCCESSFUL -> if (size.isNotEmpty()) "تم • $size" else "تم"
        DownloadManager.STATUS_RUNNING -> {
            if (item.totalBytes > 0) {
                val percent = (item.soFarBytes * 100 / item.totalBytes).toInt().coerceIn(0, 100)
                "جاري التنزيل $percent% • $size"
            } else {
                "جاري التنزيل… " + Formatter.formatShortFileSize(context, item.soFarBytes)
            }
        }
        DownloadManager.STATUS_PENDING -> "في الانتظار…"
        DownloadManager.STATUS_PAUSED -> "متوقف مؤقتاً"
        DownloadManager.STATUS_FAILED -> "فشل التنزيل"
        else -> ""
    }
}

/** قائمة التنزيلات: تُحدَّث كل ثانية، الضغط على ملف مكتمل يفتحه، وزر الحذف يشيل الملف. */
@Composable
fun DownloadsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var downloads by remember { mutableStateOf<List<DownloadItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        while (true) {
            downloads = withContext(Dispatchers.IO) { queryDownloads(context) }
            delay(1000)
        }
    }

    fun removeDownloads(ids: List<Long>) {
        if (ids.isEmpty()) return
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        try {
            manager.remove(*ids.toLongArray())
        } catch (e: Exception) {
            // تجاهل
        }
        downloads = downloads.filter { it.id !in ids }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("التنزيلات", fontWeight = FontWeight.Bold) },
        text = {
            if (downloads.isEmpty()) {
                Text(
                    "ما في تنزيلات بعد. الملفات اللي تنزلها تظهر هنا وتقدر تفتحها منها.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(downloads, key = { it.id }) { item ->
                        val done = item.status == DownloadManager.STATUS_SUCCESSFUL
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = done) { openDownload(context, item) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.FileDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.title.ifBlank { "ملف" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    statusText(context, item),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.status == DownloadManager.STATUS_FAILED) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                if (item.status == DownloadManager.STATUS_RUNNING && item.totalBytes > 0) {
                                    LinearProgressIndicator(
                                        progress = {
                                            (item.soFarBytes.toFloat() / item.totalBytes).coerceIn(0f, 1f)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                    )
                                }
                            }
                            IconButton(onClick = { removeDownloads(listOf(item.id)) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        },
        dismissButton = {
            if (downloads.isNotEmpty()) {
                TextButton(onClick = { removeDownloads(downloads.map { it.id }) }) {
                    Text("مسح الكل", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}
