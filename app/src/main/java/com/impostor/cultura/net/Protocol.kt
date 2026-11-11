package com.impostor.cultura.net

import com.impostor.cultura.model.ImpostorMode
import com.impostor.cultura.model.Player
import com.impostor.cultura.model.RoleAssignment
import org.json.JSONArray
import org.json.JSONObject

/**
 * Mensajes que viajan entre el anfitrión y los jugadores por Nearby Connections.
 * Todos los mensajes son objetos JSON con un campo "type".
 */
object Protocol {

    const val TYPE_LOBBY = "lobby"
    const val TYPE_ROUND = "round"
    const val TYPE_VOTE = "vote"
    const val TYPE_END = "end"

    const val ACTION_REPEAT = "repeat"
    const val ACTION_END = "end"

    fun lobbyJson(players: List<Player>): JSONObject {
        val arr = JSONArray()
        for (p in players) {
            arr.put(
                JSONObject()
                    .put("id", p.endpointId)
                    .put("nick", p.nickname)
                    .put("host", p.isHost)
            )
        }
        return JSONObject().put("type", TYPE_LOBBY).put("players", arr)
    }

    fun roundJson(role: RoleAssignment): JSONObject {
        return JSONObject()
            .put("type", TYPE_ROUND)
            .put("impostor", role.isImpostor)
            .put("word", role.word ?: JSONObject.NULL)
            .put("mode", if (role.mode == ImpostorMode.FAKE_WORD) "fake" else "none")
    }

    fun voteJson(action: String): JSONObject =
        JSONObject().put("type", TYPE_VOTE).put("action", action)

    fun endJson(): JSONObject =
        JSONObject().put("type", TYPE_END)

    fun parseRole(obj: JSONObject): RoleAssignment {
        val word = if (obj.isNull("word")) null else obj.optString("word")
        val mode =
            if (obj.optString("mode") == "fake") ImpostorMode.FAKE_WORD else ImpostorMode.NO_WORD
        return RoleAssignment(
            isImpostor = obj.optBoolean("impostor"),
            word = word,
            mode = mode
        )
    }
}
