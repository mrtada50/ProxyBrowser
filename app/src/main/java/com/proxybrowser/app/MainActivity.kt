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
import com.proxybrowser.app.ui.ProxyListScreen
import com.proxybrowser.app.viewmodel.ProxyViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ProxyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val selected = viewModel.selectedProxy.value
                    if (selected == null) {
                        ProxyListScreen(
                            viewModel = viewModel,
                            onProxySelected = { viewModel.selectProxy(it) }
                        )
                    } else {
                        BrowserScreen(
                            proxy = selected,
                            onBack = { viewModel.clearSelection() }
                        )
                    }
                }
            }
        }
    }
}
