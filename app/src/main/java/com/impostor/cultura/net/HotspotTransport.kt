package com.impostor.cultura.net

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Transporte alternativo por red local (hotspot o misma Wi-Fi).
 * - Anfitrión: abre un servidor TCP y anuncia la sala por broadcast UDP.
 * - Jugador: descubre la sala por UDP y se conecta por TCP.
 * Los mensajes son JSON separados por saltos de línea.
 */
class HotspotTransport : Transport {

    companion object {
        const val TCP_PORT = 8988
        const val UDP_PORT = 8989
        private const val HOST_ID = "HOST_TCP"
    }

    private var listener: TransportListener? = null
    private var isHost = false
    private val main = Handler(Looper.getMainLooper())
    private val lock = Any()

    @Volatile
    private var running = true

    // Anfitrión
    private var serverSocket: ServerSocket? = null
    private var broadcastSocket: DatagramSocket? = null
    private val clients = HashMap<String, PrintWriter>()
    private var counter = 0

    // Jugador
    private var discoverySocket: DatagramSocket? = null
    private var hostSocket: Socket? = null
    private var hostWriter: PrintWriter? = null

    override fun startHosting(localName: String, roomName: String, listener: TransportListener) {
        this.listener = listener
        isHost = true
        try {
            serverSocket = ServerSocket(TCP_PORT)
        } catch (e: Exception) {
            listener.onError("No se pudo abrir el puerto $TCP_PORT: ${e.message}")
            return
        }
        startAcceptLoop()
        startBroadcastLoop(roomName)
        val ip = localIpAddress() ?: "?"
        listener.onHostInfo("Modo Hotspot/misma red · IP anfitrión: $ip:$TCP_PORT")
        listener.onStatus("Sala abierta. Esperando jugadores...")
    }

    override fun startDiscovery(localName: String, listener: TransportListener) {
        this.listener = listener
        isHost = false
        try {
            discoverySocket = DatagramSocket(UDP_PORT)
        } catch (e: Exception) {
            listener.onError("No se pudo iniciar la búsqueda UDP: ${e.message}")
            return
        }
        thread { discoveryLoop() }
    }

    override fun connectToHost(sessionId: String, localName: String, listener: TransportListener) {
        this.listener = listener
        isHost = false
        closeQuietly(discoverySocket)
        val parts = sessionId.split(":")
        if (parts.size != 2) {
            listener.onError("Dirección inválida: $sessionId")
            return
        }
        val ip = parts[0]
        val port = parts[1].toIntOrNull() ?: TCP_PORT
        thread {
            try {
                val socket = Socket(ip, port)
                hostSocket = socket
                val writer = PrintWriter(
                    BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)),
                    true
                )
                hostWriter = writer
                writer.println(JSONObject().put("type", "join").put("nick", localName).toString())
                main.post { listener.onConnectedToHost(HOST_ID) }
                hostReadLoop(socket)
            } catch (e: Exception) {
                main.post { listener.onError("No se pudo conectar a $ip:$port: ${e.message}") }
            }
        }
    }

    override fun sendTo(endpointId: String, text: String) {
        synchronized(lock) {
            if (isHost) clients[endpointId]?.println(text) else hostWriter?.println(text)
        }
    }
    private fun startAcceptLoop() {
        thread {
            while (running) {
                try {
                    val socket = serverSocket?.accept() ?: break
                    val id = synchronized(lock) { "C${counter++}" }
                    val writer = PrintWriter(
                        BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)),
                        true
                    )
                    synchronized(lock) { clients[id] = writer }
                    thread { serverReadLoop(socket, id) }
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    private fun serverReadLoop(socket: Socket, id: String) {
        var nick: String? = null
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            while (running) {
                val line = reader.readLine() ?: break
                if (line.isBlank()) continue
                if (nick == null) {
                    val obj = try { JSONObject(line) } catch (e: Exception) { null }
                    if (obj != null && obj.optString("type") == "join") {
                        val n = obj.optString("nick").ifBlank { "Jugador" }
                        nick = n
                        main.post { listener?.onPlayerConnected(id, n) }
                        continue
                    }
                }
                main.post { listener?.onMessage(id, line) }
            }
        } catch (e: Exception) {
            // desconexión
        } finally {
            synchronized(lock) { clients.remove(id) }
            closeQuietly(socket)
            main.post { listener?.onPlayerDisconnected(id) }
        }
    }

    private fun startBroadcastLoop(roomName: String) {
        thread {
            try { broadcastSocket = DatagramSocket() } catch (e: Exception) { return@thread }
            val payload = "IMPOSTOR|$roomName|$TCP_PORT".toByteArray(Charsets.UTF_8)
            while (running) {
                try {
                    val packet = DatagramPacket(
                        payload, payload.size, InetAddress.getByName("255.255.255.255"), UDP_PORT
                    )
                    broadcastSocket?.send(packet)
                } catch (e: Exception) {
                    // ignorar
                }
                try { Thread.sleep(1500) } catch (e: InterruptedException) { break }
            }
        }
    }
    private fun discoveryLoop() {
        val buffer = ByteArray(1024)
        val seen = HashSet<String>()
        while (running) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                discoverySocket?.receive(packet) ?: break
                val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val parts = text.split("|")
                if (parts.size >= 3 && parts[0] == "IMPOSTOR") {
                    val room = parts[1]
                    val port = parts[2].toIntOrNull() ?: TCP_PORT
                    val sessionId = "${packet.address.hostAddress}:$port"
                    if (seen.add(sessionId)) {
                        main.post { listener?.onSessionFound(sessionId, room) }
                    }
                }
            } catch (e: Exception) {
                break
            }
        }
    }

    private fun hostReadLoop(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            while (running) {
                val line = reader.readLine() ?: break
                if (line.isNotBlank()) {
                    main.post { listener?.onMessage(HOST_ID, line) }
                }
            }
        } catch (e: Exception) {
            // desconexión
        } finally {
            closeQuietly(socket)
            main.post { listener?.onDisconnected() }
        }
    }

    override fun stop() {
        running = false
        closeQuietly(serverSocket)
        closeQuietly(broadcastSocket)
        closeQuietly(discoverySocket)
        closeQuietly(hostSocket)
        synchronized(lock) { clients.clear() }
        hostWriter = null
    }

    private fun localIpAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                if (!nif.isUp || nif.isLoopback) continue
                val addresses = nif.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun closeQuietly(closeable: java.io.Closeable?) {
        try { closeable?.close() } catch (e: Exception) { /* ignorar */ }
    }
}
