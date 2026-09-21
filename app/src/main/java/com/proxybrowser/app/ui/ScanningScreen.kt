package com.proxybrowser.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * شاشة انتقالية تظهر أثناء البحث عن بروكسي. البحث تلقائي بالكامل ومستمر
 * بالخلفية (Foreground Service)، وحد السرعة يرتفع تدريجياً كل 10 ثواني
 * إذا ما لقى نتيجة، فلا حاجة لأي زر إعادة محاولة يدوي.
 */
@Composable
fun ScanningScreen(thresholdMs: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.padding(8.dp))
        Text("جاري البحث عن بروكسي أسرع من ${thresholdMs}ms...")
        Spacer(modifier = Modifier.padding(6.dp))
        Text(
            "🔒 تم قطع كل الاتصال بالكامل ريثما نجد بروكسي بديل — ما يصير أي تصفح خارج بروكسي.",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
