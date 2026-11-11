package com.impostor.cultura.net

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy

/**
 * Transporte basado en Google Nearby Connections (Bluetooth + Wi-Fi automático).
 * No requiere hotspot ni misma red.
 */
class NearbyTransport(context: Context) : Transport {

    companion object {
        private const val SERVICE_ID = "com.impostor.cultura.game"
    }

    private val client: ConnectionsClient =
        Nearby.getConnectionsClient(context.applicationContext)
    private var listener: TransportListener? = null
    private var isHost = false
    private val pendingNicks = HashMap<String, String>()
    private val hostEndpoints = HashSet<String>()

    override fun startHosting(localName: String, roomName: String, listener: TransportListener) {
        this.listener = listener
        isHost = true
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        client.startAdvertising(roomName, SERVICE_ID, lifecycle, options)
            .addOnSuccessListener {
                listener.onStatus("Sala \"$roomName\" creada (Nearby). Esperando jugadores...")
            }
            .addOnFailureListener { e ->
                listener.onError("No se pudo crear la sala en Nearby: ${e.message}")
            }
    }

    override fun startDiscovery(localName: String, listener: TransportListener) {
        this.listener = listener
        isHost = false
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        client.startDiscovery(SERVICE_ID, discovery, options)
            .addOnFailureListener { e ->
                listener.onError("No se pudo buscar salas en Nearby: ${e.message}")
            }
    }

    override fun connectToHost(sessionId: String, localName: String, listener: TransportListener) {
        this.listener = listener
        isHost = false
        client.stopDiscovery()
        client.requestConnection(localName, sessionId, lifecycle)
            .addOnFailureListener { e ->
                listener.onError("No se pudo conectar por Nearby: ${e.message}")
            }
    }

    private val lifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            if (isHost) {
                if (hostEndpoints.size >= 7) {
                    client.rejectConnection(endpointId)
                    listener?.onError("Sala llena (máximo 8 jugadores)")
                    return
                }
                hostEndpoints.add(endpointId)
                pendingNicks[endpointId] = connectionInfo.endpointName
            }
            client.acceptConnection(endpointId, payloads)
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            if (resolution.status.isSuccess) {
                if (isHost) listener?.onPlayerConnected(endpointId, pendingNicks[endpointId] ?: "Jugador")
                else listener?.onConnectedToHost(endpointId)
            } else {
                if (isHost) hostEndpoints.remove(endpointId)
                listener?.onError("Conexión fallida: ${resolution.status.statusMessage}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            if (isHost) {
                hostEndpoints.remove(endpointId)
                listener?.onPlayerDisconnected(endpointId)
            } else {
                listener?.onDisconnected()
            }
        }
    }

    private val discovery = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            listener?.onSessionFound(endpointId, info.endpointName)
        }

        override fun onEndpointLost(endpointId: String) {
            listener?.onSessionLost(endpointId)
        }
    }

    private val payloads = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            listener?.onMessage(endpointId, String(bytes, Charsets.UTF_8))
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // sin uso
        }
    }

    override fun sendTo(endpointId: String, text: String) {
        client.sendPayload(endpointId, Payload.fromBytes(text.toByteArray(Charsets.UTF_8)))
            .addOnFailureListener { e -> listener?.onError("Error de envío: ${e.message}") }
    }

    override fun stop() {
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
    }
}
