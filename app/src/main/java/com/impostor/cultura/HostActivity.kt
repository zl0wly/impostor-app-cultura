package com.impostor.cultura

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.impostor.cultura.data.WordData
import com.impostor.cultura.model.ImpostorMode
import com.impostor.cultura.model.RoleAssignment

class HostActivity : AppCompatActivity() {

    private lateinit var nick: String
    private lateinit var playersAdapter: ArrayAdapter<String>
    private lateinit var setupGroup: View
    private lateinit var playGroup: View
    private lateinit var roomText: TextView
    private lateinit var statusText: TextView
    private lateinit var roleBadge: TextView
    private lateinit var wordText: TextView
    private lateinit var votesText: TextView
    private lateinit var revealButton: Button
    private lateinit var categorySpinner: Spinner
    private lateinit var modeGroup: RadioGroup

    private var wordVisible = false
    private var currentRole: RoleAssignment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host)
        nick = intent.getStringExtra("nick") ?: "Anfitrión"

        setupGroup = findViewById(R.id.setupGroup)
        playGroup = findViewById(R.id.playGroup)
        roomText = findViewById(R.id.roomText)
        statusText = findViewById(R.id.statusText)
        roleBadge = findViewById(R.id.roleBadge)
        wordText = findViewById(R.id.wordText)
        votesText = findViewById(R.id.votesText)
        revealButton = findViewById(R.id.revealButton)
        categorySpinner = findViewById(R.id.categorySpinner)
        modeGroup = findViewById(R.id.modeGroup)

        val playersList = findViewById<ListView>(R.id.playersList)
        playersAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        playersList.adapter = playersAdapter

        val categories = WordData.names()
        categorySpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, categories).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        roomText.text = "Sala: $nick"
        GameSession.startHosting(this, nick, "Sala de $nick")

        GameSession.playersLd.observe(this) { players ->
            playersAdapter.clear()
            playersAdapter.addAll(players.map { (if (it.isHost) "👑 " else "🔵 ") + it.nickname })
            playersAdapter.notifyDataSetChanged()
        }
        GameSession.statusLd.observe(this) { statusText.text = it }
        val hostInfoText = findViewById<TextView>(R.id.hostInfoText)
        GameSession.hostInfoLd.observe(this) { hostInfoText.text = it }
        GameSession.myRoleLd.observe(this) { role ->
            currentRole = role
            if (role != null) {
                setupGroup.visibility = View.GONE
                playGroup.visibility = View.VISIBLE
                showRole(role)
            } else {
                setupGroup.visibility = View.VISIBLE
                playGroup.visibility = View.GONE
            }
        }
        GameSession.votesLd.observe(this) { votePair ->
            votesText.text = "Votos → Repetir: ${votePair.first} / Terminar: ${votePair.second}"
        }

        findViewById<Button>(R.id.startButton).setOnClickListener {
            val category = categorySpinner.selectedItem?.toString() ?: return@setOnClickListener
            val mode = if (modeGroup.checkedRadioButtonId == R.id.modeNone)
                ImpostorMode.NO_WORD else ImpostorMode.FAKE_WORD
            val total = GameSession.playersLd.value?.size ?: 0
            if (total in 1..2) {
                Toast.makeText(this, "Se recomiendan 3+ jugadores", Toast.LENGTH_SHORT).show()
            }
            GameSession.startRound(category, mode)
        }
        revealButton.setOnClickListener { toggleWord() }
        findViewById<Button>(R.id.nextRoundButton).setOnClickListener {
            wordVisible = false
            GameSession.repeatRound()
        }
        findViewById<Button>(R.id.endGameButton).setOnClickListener {
            GameSession.endGame()
            Handler(mainLooper).postDelayed({
                GameSession.stopAll()
                finish()
            }, 800)
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
