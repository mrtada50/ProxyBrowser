package com.proxybrowser.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proxybrowser.app.model.ProxyInfo
import com.proxybrowser.app.network.ProxyFetcher
import com.proxybrowser.app.network.ProxyTester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ConnectionState {
    object Scanning : ConnectionState()
    data class Connected(val proxy: ProxyInfo) : ConnectionState()
    object Failed : ConnectionState()
}

class ProxyViewModel : ViewModel() {

    var connectionState = mutableStateOf<ConnectionState>(ConnectionState.Scanning)
        private set

    private var scanJob: Job? = null

    init {
        scanForFastProxy()
    }

    /** يبدأ فحص من جديد ويوقف أي فحص سابق شغال. */
    fun scanForFastProxy() {
        scanJob?.cancel()
        connectionState.value = ConnectionState.Scanning

        scanJob = viewModelScope.launch {
            val candidates = withContext(Dispatchers.IO) {
                ProxyFetcher.fetchAll()
            }

            val found = ProxyTester.findFirstWorking(candidates)

            connectionState.value = if (found != null) {
                ConnectionState.Connected(found)
            } else {
                ConnectionState.Failed
            }
        }
    }
}
