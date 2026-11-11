package com.impostor.cultura

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.impostor.cultura.model.RoleAssignment
import com.impostor.cultura.model.SessionInfo
import com.impostor.cultura.net.HotspotTransport

class JoinActivity : AppCompatActivity() {

    private lateinit var nick: String
    private lateinit var sessionsAdapter: ArrayAdapter<String>
    private var sessions: List<SessionInfo> = emptyList()

    private lateinit var statusText: TextView
    private lateinit var sessionsList: ListView
    private lateinit var discoveryGroup: View
    private lateinit var playGroup: View
    private lateinit var endedGroup: View
    private lateinit var roleBadge: TextView
    private lateinit var wordText: TextView
    private lateinit var revealButton: Button

    private var wordVisible = false
    private var currentRole: RoleAssignment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join)
        nick = intent.getStringExtra("nick") ?: "Jugador"

        statusText = findViewById(R.id.statusText)
        sessionsList = findViewById(R.id.sessionsList)
        discoveryGroup = findViewById(R.id.discoveryGroup)
        playGroup = findViewById(R.id.playGroup)
        endedGroup = findViewById(R.id.endedGroup)
        roleBadge = findViewById(R.id.roleBadge)
        wordText = findViewById(R.id.wordText)
        revealButton = findViewById(R.id.revealButton)

        val manualGroup = findViewById<View>(R.id.manualGroup)
        if (intent.getStringExtra("mode") == "hotspot") manualGroup.visibility = View.VISIBLE
        findViewById<Button>(R.id.manualConnect).setOnClickListener {
            val ip = findViewById<EditText>(R.id.manualIp).text.toString().trim()
            if (ip.isEmpty()) {
                Toast.makeText(this, "Escribe la IP del anfitrión", Toast.LENGTH_SHORT).show()
            } else {
                GameSession.connectToHost("$ip:${HotspotTransport.TCP_PORT}", nick)
            }
        }

        sessionsAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        sessionsList.adapter = sessionsAdapter

        GameSession.startDiscovery(this)

        GameSession.sessionsLd.observe(this) { list ->
            sessions = list
            sessionsAdapter.clear()
            sessionsAdapter.addAll(list.map { "📶 ${it.name}" })
            sessionsAdapter.notifyDataSetChanged()
        }
        sessionsList.setOnItemClickListener { _, _, position, _ ->
            val s = sessions.getOrNull(position) ?: return@setOnItemClickListener
            GameSession.connectToHost(s.endpointId, nick)
            sessionsList.isEnabled = false
        }
        GameSession.statusLd.observe(this) { statusText.text = it }
        GameSession.connectedLd.observe(this) { connected ->
            if (connected) {
                discoveryGroup.visibility = View.GONE
                manualGroup.visibility = View.GONE
            }
        }
        GameSession.myRoleLd.observe(this) { role ->
            currentRole = role
            if (role != null) {
                discoveryGroup.visibility = View.GONE
                endedGroup.visibility = View.GONE
                playGroup.visibility = View.VISIBLE
                showRole(role)
            }
        }
        GameSession.phaseLd.observe(this) { phase ->
            if (phase == Phase.ENDED) {
                playGroup.visibility = View.GONE
                discoveryGroup.visibility = View.GONE
                endedGroup.visibility = View.VISIBLE
            }
        }

        revealButton.setOnClickListener { toggleWord() }
        findViewById<Button>(R.id.voteRepeatButton).setOnClickListener {
            GameSession.castVote("repeat")
            Toast.makeText(this, "Votaste: Repetir", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.voteEndButton).setOnClickListener {
            GameSession.castVote("end")
            Toast.makeText(this, "Votaste: Terminar", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.backButton).setOnClickListener {
            GameSession.stopAll()
            finish()
        }
    }

    private fun showRole(role: RoleAssignment) {
        wordVisible = false
        revealButton.visibility = View.VISIBLE
        if (role.isImpostor) {
            roleBadge.text = "🕵️ ERES EL IMPOSTOR"
            roleBadge.setTextColor(getColor(R.color.impostor))
            if (role.word == null) {
                wordText.text = "No tienes palabra. ¡Adivina la de los demás!"
                revealButton.visibility = View.GONE
            } else {
                wordText.text = "•••••••"
            }
        } else {
            roleBadge.text = "🎉 Eres jugador"
            roleBadge.setTextColor(getColor(R.color.player))
            wordText.text = "•••••••"
        }
        revealButton.text = "Mostrar palabra"
    }

    private fun toggleWord() {
        val word = currentRole?.word ?: return
        wordVisible = !wordVisible
        wordText.text = if (wordVisible) word else "•••••••"
        revealButton.text = if (wordVisible) "Ocultar palabra" else "Mostrar palabra"
    }

    override fun onBackPressed() {
        GameSession.stopAll()
        super.onBackPressed()
    }
}
