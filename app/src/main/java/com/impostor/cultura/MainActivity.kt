package com.impostor.cultura

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.impostor.cultura.net.TransportMode

class MainActivity : AppCompatActivity() {

    private lateinit var nicknameInput: EditText
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        GameSession.init(this)
        com.impostor.cultura.data.WordData.load(this)

        nicknameInput = findViewById(R.id.nicknameInput)
        statusText = findViewById(R.id.statusText)

        findViewById<Button>(R.id.createRoomButton).setOnClickListener {
            val nick = readNickname() ?: return@setOnClickListener
            val mode = applyTransportMode()
            if (mode == "nearby" && !ensurePermissions()) return@setOnClickListener
            startActivity(
                Intent(this, HostActivity::class.java)
                    .putExtra("nick", nick)
                    .putExtra("mode", mode)
            )
        }
        findViewById<Button>(R.id.joinRoomButton).setOnClickListener {
            val nick = readNickname() ?: return@setOnClickListener
            val mode = applyTransportMode()
            if (mode == "nearby" && !ensurePermissions()) return@setOnClickListener
            startActivity(
                Intent(this, JoinActivity::class.java)
                    .putExtra("nick", nick)
                    .putExtra("mode", mode)
            )
        }

        findViewById<RadioGroup>(R.id.transportGroup).setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.transportNearby) {
                val missing = PermissionHelper.missingPermissions(this)
                if (missing.isNotEmpty()) {
                    ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQ)
                }
            }
        }
    }

    private fun readNickname(): String? {
        val nick = nicknameInput.text.toString().trim()
        if (nick.isEmpty()) {
            Toast.makeText(this, "Escribe tu nombre", Toast.LENGTH_SHORT).show()
            return null
        }
        return nick
    }

    private fun applyTransportMode(): String {
        val hotspot =
            findViewById<RadioGroup>(R.id.transportGroup).checkedRadioButtonId == R.id.transportHotspot
        GameSession.setTransportMode(if (hotspot) TransportMode.HOTSPOT else TransportMode.NEARBY)
        return if (hotspot) "hotspot" else "nearby"
    }

    private fun ensurePermissions(): Boolean {
        val missing = PermissionHelper.missingPermissions(this)
        if (missing.isEmpty()) return true
        ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQ)
        Toast.makeText(this, "Concede los permisos y vuelve a intentar", Toast.LENGTH_LONG).show()
        return false
    }

    companion object {
        private const val REQ = 100
    }
}
