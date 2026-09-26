package com.abht.manga_dt.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.*

actual class LocalNetworkEngine actual constructor() {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private var serverThread: Thread? = null

    actual fun getLocalIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            var fallbackIp = "127.0.0.1"
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            return host
                        }
                        fallbackIp = host
                    }
                }
            }
            fallbackIp
        } catch (_: Exception) {
            "127.0.0.1"
        }
    }

    actual fun getDeviceName(): String {
        return try {
            val model = android.os.Build.MODEL
            val manufacturer = android.os.Build.MANUFACTURER
            if (model.isNotBlank()) "$manufacturer $model" else "Android Phone"
        } catch (_: Exception) {
            "Android Phone"
        }
    }

    actual fun isServerRunning(): Boolean {
        return isRunning && serverSocket != null && !(serverSocket?.isClosed ?: true)
    }

    private fun writeStringWithOptionalGzip(outStream: DataOutputStream, text: String) {
        val rawBytes = text.toByteArray(Charsets.UTF_8)
        if (rawBytes.size > 256) {
            val baos = java.io.ByteArrayOutputStream()
            val gzos = java.util.zip.GZIPOutputStream(baos)
            gzos.write(rawBytes)
            gzos.close()
            val compressed = baos.toByteArray()
            outStream.writeInt(-compressed.size)
            outStream.write(compressed)
        } else {
            outStream.writeInt(rawBytes.size)
            if (rawBytes.isNotEmpty()) {
                outStream.write(rawBytes)
            }
        }
    }

    private fun readStringWithOptionalGzip(inStream: DataInputStream): String {
        val len = inStream.readInt()
        if (len == 0) return ""
        val isGzip = len < 0
        val actualLen = kotlin.math.abs(len)
        val bytes = ByteArray(actualLen)
        inStream.readFully(bytes)
        return if (isGzip) {
            val bais = java.io.ByteArrayInputStream(bytes)
            val gzis = java.util.zip.GZIPInputStream(bais)
            val baos = java.io.ByteArrayOutputStream()
            val buf = ByteArray(16384)
            var r: Int
            while (gzis.read(buf).also { r = it } != -1) {
                baos.write(buf, 0, r)
            }
            gzis.close()
            String(baos.toByteArray(), Charsets.UTF_8)
        } else {
            String(bytes, Charsets.UTF_8)
        }
    }

    actual fun startSyncServer(port: Int, onMessageReceived: (action: String, body: String) -> String): Boolean {
        if (isServerRunning()) return true
        return try {
            serverSocket = ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))
            isRunning = true

            serverThread = Thread {
                while (isRunning && serverSocket != null && !(serverSocket?.isClosed ?: true)) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        Thread {
                            try {
                                client.soTimeout = 120_000
                                val inStream = DataInputStream(BufferedInputStream(client.getInputStream()))
                                val outStream = DataOutputStream(BufferedOutputStream(client.getOutputStream()))

                                val action = inStream.readUTF()
                                val body = readStringWithOptionalGzip(inStream)

                                val response = onMessageReceived(action, body)
                                writeStringWithOptionalGzip(outStream, response)
                                outStream.flush()
                            } catch (_: Exception) {
                            } finally {
                                runCatching { client.close() }
                            }
                        }.start()
                    } catch (_: Exception) {
                        break
                    }
                }
            }.apply {
                isDaemon = true
                start()
            }
            true
        } catch (e: Exception) {
            isRunning = false
            false
        }
    }

    actual fun stopSyncServer() {
        isRunning = false
        runCatching { serverSocket?.close() }
        serverSocket = null
        serverThread = null
    }

    actual suspend fun sendRequest(targetIp: String, port: Int, action: String, payload: String): String? = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(targetIp, port), 8000)
            socket.soTimeout = 120_000

            val outStream = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
            val inStream = DataInputStream(BufferedInputStream(socket.getInputStream()))

            outStream.writeUTF(action)
            writeStringWithOptionalGzip(outStream, payload)
            outStream.flush()

            val response = readStringWithOptionalGzip(inStream)
            socket.close()
            response
        } catch (_: Exception) {
            null
        }
    }

    actual suspend fun scanLocalPeers(port: Int, timeoutMs: Long): List<DiscoveredPeer> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<DiscoveredPeer>()
        val myIp = getLocalIpAddress()
        if (myIp == "127.0.0.1") return@withContext emptyList()

        val prefix = myIp.substringBeforeLast(".") + "."

        val deferredList = (1..254).map { i ->
            async {
                val target = "$prefix$i"
                if (target != myIp) {
                    try {
                        val socket = Socket()
                        socket.connect(InetSocketAddress(target, port), 400)
                        socket.soTimeout = 1500

                        val outStream = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
                        val inStream = DataInputStream(BufferedInputStream(socket.getInputStream()))

                        val actionBytes = "PING".toByteArray(Charsets.UTF_8)
                        outStream.writeInt(actionBytes.size)
                        outStream.write(actionBytes)
                        outStream.writeInt(0)
                        outStream.flush()

                        val respLen = inStream.readInt()
                        val respBytes = ByteArray(respLen)
                        inStream.readFully(respBytes)
                        val res = String(respBytes, Charsets.UTF_8)

                        socket.close()

                        if (res.startsWith("PONG|||")) {
                            val parts = res.split("|||")
                            val peerName = parts.getOrNull(1) ?: "Manga DT Device"
                            val peerPort = parts.getOrNull(2)?.toIntOrNull() ?: port
                            val isAndroid = peerName.contains("Android", ignoreCase = true) || peerName.contains("Phone", ignoreCase = true) || peerName.contains("Galaxy", ignoreCase = true) || peerName.contains("Pixel", ignoreCase = true) || peerName.contains("Xiaomi", ignoreCase = true)
                            DiscoveredPeer(
                                ip = target,
                                port = peerPort,
                                deviceName = peerName,
                                isAndroid = isAndroid
                            )
                        } else null
                    } catch (_: Exception) {
                        null
                    }
                } else null
            }
        }

        val results = deferredList.awaitAll().filterNotNull()
        discovered.addAll(results)
        discovered
    }
}
