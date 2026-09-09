package com.proxybrowser.app.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.proxybrowser.app.model.ProxyInfo
import com.proxybrowser.app.viewmodel.ProxyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyListScreen(
    viewModel: ProxyViewModel,
    onProxySelected: (ProxyInfo) -> Unit
) {
    val proxies = viewModel.workingProxies
    val isLoading = viewModel.isLoading.value

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Proxy Browser") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Button(
                onClick = { viewModel.startScan() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "جارٍ الفحص..." else "فحص البروكسيات")
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading && proxies.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                proxies.isEmpty() -> {
                    Text("اضغط \"فحص البروكسيات\" للبدء")
                }
                else -> {
                    LazyColumn {
                        items(proxies) { proxy ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onProxySelected(proxy) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("${proxy.host}:${proxy.port}")
                                        Text(
                                            proxy.type.name,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Text("${proxy.latencyMs} ms")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
