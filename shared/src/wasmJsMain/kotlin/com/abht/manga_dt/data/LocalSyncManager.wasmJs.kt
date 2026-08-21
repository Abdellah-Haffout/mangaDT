package com.abht.manga_dt.data

actual class LocalNetworkEngine actual constructor() {
    actual fun getLocalIpAddress(): String = "127.0.0.1"
    actual fun getDeviceName(): String = "Web Device"
    actual fun isServerRunning(): Boolean = false
    actual fun startSyncServer(port: Int, onMessageReceived: (action: String, body: String) -> String): Boolean = false
    actual fun stopSyncServer() {}
    actual suspend fun sendRequest(targetIp: String, port: Int, action: String, payload: String): String? = null
    actual suspend fun scanLocalPeers(port: Int, timeoutMs: Long): List<DiscoveredPeer> = emptyList()
}
