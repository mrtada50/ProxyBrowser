package com.proxybrowser.app.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proxybrowser.app.model.ProxyInfo
import com.proxybrowser.app.network.ProxyFetcher
import com.proxybrowser.app.network.ProxyTester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProxyViewModel : ViewModel() {

    val workingProxies = mutableStateListOf<ProxyInfo>()

    var isLoading = mutableStateOf(false)
        private set

    var selectedProxy = mutableStateOf<ProxyInfo?>(null)
        private set

    fun startScan() {
        if (isLoading.value) return
        workingProxies.clear()
        isLoading.value = true

        viewModelScope.launch {
            val candidates = withContext(Dispatchers.IO) {
                ProxyFetcher.fetchAll()
            }

            ProxyTester.testProxies(candidates) { tested ->
                workingProxies.add(tested)
                workingProxies.sortBy { it.latencyMs }
            }

            isLoading.value = false
        }
    }

    fun selectProxy(proxy: ProxyInfo) {
        selectedProxy.value = proxy
    }

    fun clearSelection() {
        selectedProxy.value = null
    }
}
