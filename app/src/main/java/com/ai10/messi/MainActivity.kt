package com.ai10.messi

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val black = Color.rgb(8,8,8)
    private val gold = Color.rgb(212,175,55)
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = black; window.navigationBarColor = black
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(18), dp(20), dp(16)); setBackgroundColor(black) }
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(TextView(this).apply { text = "♛"; textSize = 58f; gravity = Gravity.CENTER; setTextColor(gold) })
        body.addView(TextView(this).apply { text = "AI10"; textSize = 34f; gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        body.addView(TextView(this).apply { text = "Ton assistant de messagerie • Propulsé par Gemini"; textSize = 13f; gravity = Gravity.CENTER; setTextColor(Color.LTGRAY); setPadding(0, dp(4), 0, dp(24)) })
        button(body, "Configurer Gemini", "Clé API enregistrée sur cet appareil") { configureGemini() }
        button(body, "Activer la fenêtre flottante", "Couronne dorée • permissions Android") { enableOverlay() }
        button(body, "Autoriser la lecture des notifications", "Recevoir les messages notifiés") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        button(body, "Configurer AutoZap et SugesZap", "Contacts autorisés et modes de réponse") { showInfo() }
        body.addView(TextView(this).apply { text = "Limite : 4 contacts par application. La réponse automatique ne fonctionne que si l’application source expose une action de réponse compatible. AI10 n’accepte pas les demandes d’amis à ta place."; textSize = 12f; setTextColor(Color.GRAY); setPadding(dp(4), dp(16), dp(4), dp(10)) })
        scroll.addView(body); page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(page)
    }

    private fun button(parent: LinearLayout, title: String, subtitle: String, action: () -> Unit) {
        val b = Button(this).apply {
            text = "$title\n$subtitle"; isAllCaps = false; textSize = 14f; gravity = Gravity.CENTER_VERTICAL; setTextColor(Color.WHITE); setPadding(dp(14), dp(10), dp(14), dp(10))
            background = GradientDrawable().apply { setColor(Color.rgb(24,24,24)); cornerRadius = dp(16).toFloat(); setStroke(dp(1), gold) }
            setOnClickListener { action() }
        }
        parent.addView(b, LinearLayout.LayoutParams(-1, dp(68)).apply { bottomMargin = dp(10) })
    }

    private fun configureGemini() {
        val edit = EditText(this).apply { hint = "Clé API Gemini"; setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); inputType = 129; setText(getSharedPreferences("ai10_settings", MODE_PRIVATE).getString("gemini_key", "")) }
        AlertDialog.Builder(this).setTitle("Gemini — AI10").setMessage("Crée une clé API dans Google AI Studio et garde-la privée.").setView(edit)
            .setPositiveButton("Enregistrer") { _, _ -> GeminiAI.saveKey(this, edit.text.toString()); Toast.makeText(this, "Configuration Gemini enregistrée", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("Annuler", null).show()
    }

    private fun enableOverlay() {
        if (!Settings.canDrawOverlays(this)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        else Toast.makeText(this, "Active aussi l’accès aux notifications pour recevoir les messages.", Toast.LENGTH_LONG).show()
    }

    private fun showInfo() {
        AlertDialog.Builder(this).setTitle("AutoZap & SugesZap")
            .setMessage("SugesZap prépare une réponse modifiable. AutoZap envoie la réponse générée par Gemini uniquement pour un contact que tu as autorisé et mis en mode automatique, si sa notification permet la réponse. La sélection du contact se fait à partir d’une notification reçue, pas en envoyant une demande d’ami.")
            .setPositiveButton("Compris", null).show()
    }
}
