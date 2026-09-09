package com.proxybrowser.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.proxybrowser.app.ui.BrowserScreen
import com.proxybrowser.app.ui.ScanningScreen
import com.proxybrowser.app.viewmodel.ConnectionState
import com.proxybrowser.app.viewmodel.ProxyViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ProxyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (val state = viewModel.connectionState.value) {
                        is ConnectionState.Scanning -> {
                            ScanningScreen(failed = false, onRetry = { viewModel.scanForFastProxy() })
                        }
                        is ConnectionState.Failed -> {
                            ScanningScreen(failed = true, onRetry = { viewModel.scanForFastProxy() })
                        }
                        is ConnectionState.Connected -> {
                            BrowserScreen(
                                proxy = state.proxy,
                                onProxyLost = { viewModel.scanForFastProxy() }
                            )
                        }
                    }
                }
            }
        }
    }
}
