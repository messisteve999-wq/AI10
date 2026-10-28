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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val black = Color.rgb(5, 10, 20)
    private val blue = Color.rgb(0, 127, 255)
    private val gold = Color.rgb(230, 184, 55)
    private val panel = Color.rgb(14, 23, 38)

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    private val prefs by lazy {
        getSharedPreferences("ai10_settings", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = black
        window.navigationBarColor = black

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setBackgroundColor(black)
        }
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        body.addView(TextView(this).apply {
            text = "♛"
            textSize = 54f
            gravity = Gravity.CENTER
            setTextColor(gold)
        })
        body.addView(TextView(this).apply {
            text = "AI10 v3"
            textSize = 32f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            setTextColor(Color.WHITE)
        })
        body.addView(TextView(this).apply {
            text = "by Steve"
            textSize = 20f
            gravity = Gravity.CENTER
            typeface = Typeface.create("serif", Typeface.BOLD_ITALIC)
            setTextColor(gold)
        })
        body.addView(TextView(this).apply {
            text = "Assistant intelligent • Deux fenêtres flottantes indépendantes"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(8), 0, dp(18))
        })

        section(body, "FENÊTRES FLOTTANTES")

        button(body, "AutoZap : " + state("floating_autozap"),
            "Activer ou désactiver la fenêtre AutoZap") {
            toggle("floating_autozap", "AutoZap")
        }
        button(body, "SugesZap : " + state("floating_sugeszap"),
            "Activer ou désactiver la fenêtre SugesZap") {
            toggle("floating_sugeszap", "SugesZap")
        }

        button(body, "Accès aux notifications",
            "Autoriser AI10 à lire les notifications") {
            openSetting(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }
        button(body, "Autorisation fenêtres flottantes",
            "Autoriser AI10 à s'afficher par-dessus les applications") {
            if (!Settings.canDrawOverlays(this)) {
                openSetting(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            } else {
                toast("Autorisation déjà accordée")
            }
        }

        section(body, "PARAMÈTRES AVANCÉS ANDROID")

        val settings = listOf(
            "Paramètres généraux" to Settings.ACTION_SETTINGS,
            "Fiche et autorisations AI10" to Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "Gestion des applications" to Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS,
            "Batterie et économie d'énergie" to Settings.ACTION_BATTERY_SAVER_SETTINGS,
            "Optimisation de la batterie" to Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
            "Wi-Fi" to Settings.ACTION_WIFI_SETTINGS,
            "Données mobiles" to Settings.ACTION_DATA_ROAMING_SETTINGS,
            "Bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
            "VPN" to Settings.ACTION_VPN_SETTINGS,
            "Réseaux sans fil" to Settings.ACTION_WIRELESS_SETTINGS,
            "Écran" to Settings.ACTION_DISPLAY_SETTINGS,
            "Son" to Settings.ACTION_SOUND_SETTINGS,
            "Localisation" to Settings.ACTION_LOCATION_SOURCE_SETTINGS,
            "Accessibilité" to Settings.ACTION_ACCESSIBILITY_SETTINGS,
            "Clavier et saisie" to Settings.ACTION_INPUT_METHOD_SETTINGS,
            "Date et heure" to Settings.ACTION_DATE_SETTINGS,
            "Sécurité" to Settings.ACTION_SECURITY_SETTINGS,
            "Confidentialité" to Settings.ACTION_PRIVACY_SETTINGS
        )

        settings.forEach { (label, action) ->
            button(body, label, "Ouvrir ce réglage Android") {
                val uri = if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    Uri.parse("package:$packageName") else null
                openSetting(action, uri)
            }
        }

        section(body, "ASSISTANT INTELLIGENT")
        button(body, "Configurer Gemini",
            "Enregistrer la clé API Gemini") {
            configureGemini()
        }
        button(body, "Règles des contacts",
            "AutoZap est limité aux contacts que tu autorises") {
            AlertDialog.Builder(this)
                .setTitle("AutoZap")
                .setMessage(
                    "AI10 ne répond automatiquement qu'aux contacts autorisés " +
                    "et si la notification propose une action de réponse compatible. " +
                    "SugesZap permet de préparer et modifier une réponse. " +
                    "Les fenêtres peuvent être activées séparément ici."
                )
                .setPositiveButton("Compris", null)
                .show()
        }

        body.addView(TextView(this).apply {
            text = "STEVE • AI10 v3"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(gold)
            setPadding(0, dp(22), 0, dp(12))
        })

        scroll.addView(body)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(page)
    }

    private fun state(key: String) =
        if (prefs.getBoolean(key, true)) "ACTIVÉ" else "DÉSACTIVÉ"

    private fun toggle(key: String, label: String) {
        val enabled = !prefs.getBoolean(key, true)
        prefs.edit().putBoolean(key, enabled).apply()
        toast("$label : " + if (enabled) "activé" else "désactivé")
        recreate()
    }

    private fun openSetting(action: String, uri: Uri? = null) {
        try {
            startActivity(if (uri == null) Intent(action) else Intent(action, uri))
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (_: Exception) {
                toast("Réglage indisponible sur cet appareil")
            }
        }
    }

    private fun section(parent: LinearLayout, title: String) {
        parent.addView(TextView(this).apply {
            text = title
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(blue)
            setPadding(dp(4), dp(14), dp(4), dp(8))
        })
    }

    private fun button(
        parent: LinearLayout,
        title: String,
        subtitle: String,
        action: () -> Unit
    ) {
        val b = Button(this).apply {
            text = "$title\n$subtitle"
            isAllCaps = false
            textSize = 14f
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.WHITE)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = GradientDrawable().apply {
                setColor(panel)
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), blue)
            }
            setOnClickListener { action() }
        }
        parent.addView(b, LinearLayout.LayoutParams(-1, dp(66)).apply {
            bottomMargin = dp(6)
        })
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun configureGemini() {
        val edit = android.widget.EditText(this).apply {
            hint = "Clé API Gemini"
            setSingleLine(true)
            inputType = 129
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setText(prefs.getString("gemini_key", ""))
        }
        AlertDialog.Builder(this)
            .setTitle("Gemini — AI10")
            .setMessage("Garde ta clé API privée.")
            .setView(edit)
            .setPositiveButton("Enregistrer") { _, _ ->
                GeminiAI.saveKey(this, edit.text.toString())
                toast("Configuration enregistrée")
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}
