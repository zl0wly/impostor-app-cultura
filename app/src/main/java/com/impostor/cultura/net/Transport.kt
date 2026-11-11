package com.impostor.cultura.net

/** Modo de conexión entre teléfonos. */
enum class TransportMode { NEARBY, HOTSPOT }

/** Callbacks que cualquier transporte (Nearby o Hotspot) avisa a GameSession. */
interface TransportListener {
    fun onStatus(message: String)
    fun onError(message: String)
    fun onHostInfo(info: String)
    fun onPlayerConnected(endpointId: String, nickname: String)
    fun onPlayerDisconnected(endpointId: String)
    fun onSessionFound(sessionId: String, name: String)
    fun onSessionLost(sessionId: String)
    fun onConnectedToHost(hostEndpointId: String)
    fun onDisconnected()
    fun onMessage(endpointId: String, text: String)
}

/**
 * Capa de red abstracta. GameSession no sabe si por debajo hay Nearby Connections
 * o sockets TCP/UDP sobre una red local (hotspot).
 */
interface Transport {
    fun startHosting(localName: String, roomName: String, listener: TransportListener)
    fun startDiscovery(localName: String, listener: TransportListener)
    fun connectToHost(sessionId: String, localName: String, listener: TransportListener)
    fun sendTo(endpointId: String, text: String)
    fun stop()
}
