package com.impostor.cultura

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.impostor.cultura.data.WordData
import com.impostor.cultura.model.ImpostorMode
import com.impostor.cultura.model.Player
import com.impostor.cultura.model.RoleAssignment
import com.impostor.cultura.model.SessionInfo
import com.impostor.cultura.net.HotspotTransport
import com.impostor.cultura.net.NearbyTransport
import com.impostor.cultura.net.Protocol
import com.impostor.cultura.net.Transport
import com.impostor.cultura.net.TransportListener
import com.impostor.cultura.net.TransportMode
import org.json.JSONObject

enum class SessionRole { NONE, HOST, CLIENT }

enum class Phase { MENU, LOBBY, ROUND, ENDED }

/**
 * Estado de la sesión de juego + lógica (reparto de roles, votos).
 * La conexión física se delega a un [Transport] (Nearby o Hotspot).
 */
object GameSession : TransportListener {

    const val HOST_ENDPOINT = "HOST_SELF"

    var role: SessionRole = SessionRole.NONE
        private set
    var myNickname: String = ""
        private set
    var roomName: String = "Sala Impostor"
        private set
    var transportMode: TransportMode = TransportMode.NEARBY
        private set

    private var appContext: Context? = null
    private var transport: Transport? = null

    private val connectedClients = LinkedHashSet<String>()
    private val nickByEndpoint = HashMap<String, String>()
    private val votes = LinkedHashMap<String, String>()
    private var hostEndpointId: String? = null
    private var lastCategory: String = ""
    private var lastMode: ImpostorMode = ImpostorMode.FAKE_WORD

    val playersLd = MutableLiveData<List<Player>>(emptyList())
    val sessionsLd = MutableLiveData<List<SessionInfo>>(emptyList())
    val statusLd = MutableLiveData<String>("")
    val hostInfoLd = MutableLiveData<String>("")
    val myRoleLd = MutableLiveData<RoleAssignment?>(null)
    val votesLd = MutableLiveData<Pair<Int, Int>>(0 to 0)
    val phaseLd = MutableLiveData<Phase>(Phase.MENU)
    val connectedLd = MutableLiveData<Boolean>(false)

    fun init(context: Context) {
        appContext = context.applicationContext
        WordData.load(context.applicationContext)
    }

    fun setTransportMode(mode: TransportMode) {
        transportMode = mode
    }

    private fun createTransport(): Transport {
        return when (transportMode) {
            TransportMode.NEARBY -> NearbyTransport(appContext!!)
            TransportMode.HOTSPOT -> HotspotTransport()
        }
    }

    fun startHosting(context: Context, nick: String, room: String) {
        init(context)
        stopInternal()
        role = SessionRole.HOST
        myNickname = nick
        roomName = room
        phaseLd.postValue(Phase.LOBBY)
        statusLd.postValue("Creando sala...")
        publishPlayers()
        transport = createTransport()
        transport?.startHosting(nick, room, this)
    }
    fun startRound(categoryName: String, mode: ImpostorMode) {
        if (role != SessionRole.HOST) return
        val category = WordData.get(categoryName)
        if (category == null || category.words.isEmpty()) {
            statusLd.postValue("Esa categoría no tiene palabras")
            return
        }

        val realWord = category.words.random()
        val endpoints = mutableListOf(HOST_ENDPOINT)
        endpoints.addAll(connectedClients)
        if (endpoints.size < 2) {
            statusLd.postValue("Necesitas al menos 1 jugador más para repartir roles (ideal 3+)")
            return
        }
        val impostorEndpoint = endpoints.random()
        val fakeWord = category.words.filter { it != realWord }.randomOrNull()

        lastCategory = categoryName
        lastMode = mode
        votes.clear()
        updateVotes()

        for (ep in endpoints) {
            val isImpostor = ep == impostorEndpoint
            val assignment = if (isImpostor) {
                RoleAssignment(
                    isImpostor = true,
                    word = if (mode == ImpostorMode.FAKE_WORD) fakeWord else null,
                    mode = mode
                )
            } else {
                RoleAssignment(isImpostor = false, word = realWord, mode = mode)
            }

            if (ep == HOST_ENDPOINT) {
                myRoleLd.postValue(assignment)
            } else {
                sendBytes(ep, Protocol.roundJson(assignment).toString())
            }
        }

        phaseLd.postValue(Phase.ROUND)
        statusLd.postValue("Ronda iniciada · Categoría: $categoryName")
    }

    fun repeatRound() {
        if (lastCategory.isNotEmpty()) startRound(lastCategory, lastMode)
    }

    fun endGame() {
        if (role == SessionRole.HOST) {
            val json = Protocol.endJson().toString()
            for (ep in connectedClients) sendBytes(ep, json)
        }
        phaseLd.postValue(Phase.ENDED)
    }

    fun startDiscovery(context: Context) {
        init(context)
        stopInternal()
        role = SessionRole.CLIENT
        connectedLd.postValue(false)
        phaseLd.postValue(Phase.LOBBY)
        statusLd.postValue("Buscando salas cercanas...")
        transport = createTransport()
        transport?.startDiscovery("jugador", this)
    }

    fun connectToHost(sessionId: String, nick: String) {
        myNickname = nick
        hostEndpointId = null
        statusLd.postValue("Conectando...")
        transport?.connectToHost(sessionId, nick, this)
    }

    fun castVote(action: String) {
        val host = hostEndpointId ?: return
        sendBytes(host, Protocol.voteJson(action).toString())
    }
    override fun onStatus(message: String) {
        statusLd.postValue(message)
    }

    override fun onError(message: String) {
        statusLd.postValue(message)
    }

    override fun onHostInfo(info: String) {
        hostInfoLd.postValue(info)
    }

    override fun onPlayerConnected(endpointId: String, nickname: String) {
        connectedClients.add(endpointId)
        nickByEndpoint[endpointId] = nickname
        publishPlayers()
        broadcastLobby()
    }

    override fun onPlayerDisconnected(endpointId: String) {
        connectedClients.remove(endpointId)
        nickByEndpoint.remove(endpointId)
        votes.remove(endpointId)
        publishPlayers()
        broadcastLobby()
        updateVotes()
    }

    override fun onSessionFound(sessionId: String, name: String) {
        val current = sessionsLd.value?.toMutableList() ?: mutableListOf()
        if (current.none { it.endpointId == sessionId }) {
            current.add(SessionInfo(sessionId, name))
            sessionsLd.postValue(current)
        }
    }

    override fun onSessionLost(sessionId: String) {
        val current = sessionsLd.value?.toMutableList() ?: return
        current.removeAll { it.endpointId == sessionId }
        sessionsLd.postValue(current)
    }

    override fun onConnectedToHost(hostEndpointId: String) {
        this.hostEndpointId = hostEndpointId
        connectedLd.postValue(true)
        statusLd.postValue("Conectado. Esperando que el anfitrión inicie...")
    }

    override fun onDisconnected() {
        connectedLd.postValue(false)
        statusLd.postValue("Te desconectaste de la sala")
    }

    override fun onMessage(endpointId: String, text: String) {
        val obj = try {
            JSONObject(text)
        } catch (e: Exception) {
            return
        }
        when (obj.optString("type")) {
            Protocol.TYPE_ROUND -> myRoleLd.postValue(Protocol.parseRole(obj))
            Protocol.TYPE_VOTE -> {
                votes[endpointId] = obj.optString("action")
                updateVotes()
            }
            Protocol.TYPE_END -> {
                phaseLd.postValue(Phase.ENDED)
                statusLd.postValue("El anfitrión terminó el juego")
            }
            Protocol.TYPE_LOBBY -> { /* nada */ }
        }
    }

    private fun publishPlayers() {
        val list = mutableListOf(Player(HOST_ENDPOINT, "$myNickname (tú)", true))
        for (ep in connectedClients) list.add(Player(ep, nickByEndpoint[ep] ?: "Jugador", false))
        playersLd.postValue(list)
    }

    private fun broadcastLobby() {
        val list = mutableListOf(Player(HOST_ENDPOINT, myNickname, true))
        for (ep in connectedClients) list.add(Player(ep, nickByEndpoint[ep] ?: "Jugador", false))
        val json = Protocol.lobbyJson(list).toString()
        for (ep in connectedClients) sendBytes(ep, json)
    }

    private fun updateVotes() {
        val repeat = votes.values.count { it == Protocol.ACTION_REPEAT }
        val end = votes.values.count { it == Protocol.ACTION_END }
        votesLd.postValue(repeat to end)
    }

    private fun sendBytes(endpointId: String, text: String) {
        transport?.sendTo(endpointId, text)
    }

    private fun stopInternal() {
        transport?.stop()
        transport = null
        connectedClients.clear()
        nickByEndpoint.clear()
        votes.clear()
        hostEndpointId = null
        playersLd.postValue(emptyList())
        sessionsLd.postValue(emptyList())
        connectedLd.postValue(false)
        myRoleLd.postValue(null)
        votesLd.postValue(0 to 0)
        hostInfoLd.postValue("")
    }

    fun stopAll() {
        stopInternal()
        role = SessionRole.NONE
        phaseLd.postValue(Phase.MENU)
        statusLd.postValue("")
    }
}
