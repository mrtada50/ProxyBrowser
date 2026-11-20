package com.proxybrowser.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.proxybrowser.app.model.ProxyInfo
import com.proxybrowser.app.ui.theme.warningColor
import com.proxybrowser.app.ui.theme.warningContainerColor

/** كبسولة حالة البروكسي بالشريط العلوي: نقطة ملونة + عنوان + تفصيل. الضغط يفتح لوحة التحكم. */
@Composable
fun ProxyStatusPill(title: String, detail: String, accent: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Surface(
        shape = shape,
        color = accent.copy(alpha = 0.12f),
        modifier = Modifier
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(accent, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** شريط تنبيه يظهر تحت الشريط العلوي طول ما البروكسي موقوف، مع زر تفعيل سريع. */
@Composable
fun ProxyDisabledBanner(onEnable: () -> Unit) {
    Surface(
        color = warningContainerColor(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = warningColor(),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "البروكسي موقوف — تتصفح باتصال مباشر",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onEnable) {
                Text("تفعيل", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** لوحة تحكم البروكسي: الحالة الحالية + مفتاح تشغيل/إيقاف + تبديل البروكسي يدوياً. */
@Composable
fun ProxyControlDialog(
    proxy: ProxyInfo?,
    livePingMs: Long?,
    onToggle: (Boolean) -> Unit,
    onSwitchProxy: () -> Unit,
    onDismiss: () -> Unit
) {
    val enabled = proxy != null
    val accent = if (enabled) MaterialTheme.colorScheme.tertiary else warningColor()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(shape = CircleShape, color = accent.copy(alpha = 0.14f)) {
                Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (enabled) Icons.Filled.Security else Icons.Filled.PowerSettingsNew,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        title = {
            Text(
                if (enabled) "البروكسي مفعّل" else "البروكسي موقوف",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (proxy != null) {
                            Text(
                                "${proxy.host}:${proxy.port}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val pingText = livePingMs?.let { "${it}ms (مباشر)" }
                                ?: proxy.latencyMs?.let { "${it}ms" }
                                ?: "—"
                            Text(
                                "النوع: ${proxy.type.name} • السرعة: $pingText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                "اتصال مباشر بدون بروكسي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "الفحص والاتصال التلقائي متوقفين، ويبقى الوضع كذلك حتى لو أغلقت التطبيق.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "تفعيل البروكسي",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = enabled, onCheckedChange = { onToggle(it) })
                }

                if (enabled) {
                    OutlinedButton(
                        onClick = onSwitchProxy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("بدّل البروكسي الآن")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

/** تأكيد قبل إيقاف البروكسي بالكامل (لأن الإيقاف يكشف عنوان IP الحقيقي). */
@Composable
fun DisableProxyConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.PowerSettingsNew,
                contentDescription = null,
                tint = warningColor()
            )
        },
        title = { Text("إيقاف البروكسي بالكامل؟", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("• التصفح يصير باتصال مباشر، وعنوانك (IP) الحقيقي يكون ظاهر للمواقع.")
                Text("• يتوقف الفحص والاتصال التلقائي بالخلفية ويختفي الإشعار.")
                Text("• يبقى موقوفاً حتى بعد إغلاق التطبيق وفتحه، ولا يرجع يشتغل إلا لما تضغط «تفعيل».")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "إيقاف",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
