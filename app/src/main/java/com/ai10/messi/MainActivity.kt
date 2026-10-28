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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val black = Color.rgb(5, 10, 20)
    private val blue = Color.rgb(0, 127, 255)
    private val gold = Color.rgb(230, 184, 55)
    private val panel = Color.rgb(14, 23, 38)

    private fun dp(n: Int) =
        (n * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = black
        window.navigationBarColor = black

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            setBackgroundColor(black)
        }
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        body.addView(TextView(this).apply {
            text = "♛"
            textSize = 58f
            gravity = Gravity.CENTER
            setTextColor(gold)
        })
        body.addView(TextView(this).apply {
            text = "AI10 v2"
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
            setPadding(0, dp(3), 0, dp(8))
        })
        body.addView(TextView(this).apply {
            text = "Assistant de messagerie intelligent • v2.0.0"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, dp(20))
        })

        section(body, "AUTORISATIONS ANDROID")

        button(body, "1. Ouvrir la fiche Android",
            "Pour débloquer les paramètres restreints") {
            openAppDetails()
        }

        button(body, "2. Accès aux notifications",
            "Autoriser AI10 à lire les notifications") {
            openNotificationSettings()
        }

        button(body, "3. Fenêtre flottante",
            "Autoriser l'affichage par-dessus les applications") {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ))
            } else {
                Toast.makeText(
                    this, "Accès déjà autorisé",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        button(body, "Guide de déblocage",
            "Que faire si le bouton Android est grisé ?") {
            AlertDialog.Builder(this)
                .setTitle("Débloquer AI10")
                .setMessage(
                    "1. Ouvre la fiche Android d'AI10.\\n\\n" +
                    "2. Si le menu ⋮ propose « Autoriser les " +
                    "paramètres restreints », sélectionne cette option.\\n\\n" +
                    "3. Reviens dans AI10 et ouvre les paramètres " +
                    "d'accès aux notifications.\\n\\n" +
                    "4. Active l'autorisation si Android le permet.\\n\\n" +
                    "Cette option dépend de la version d'Android et " +
                    "de la manière dont l'application a été installée."
                )
                .setPositiveButton("Ouvrir la fiche") { _, _ ->
                    openAppDetails()
                }
                .setNegativeButton("Fermer", null)
                .show()
        }

        section(body, "ASSISTANT INTELLIGENT")

        button(body, "Configurer Gemini",
            "Gérer la clé API sur cet appareil") {
            configureGemini()
        }

        button(body, "AutoZap et SugesZap",
            "Réponses automatiques et suggestions") {
            AlertDialog.Builder(this)
                .setTitle("AutoZap & SugesZap")
                .setMessage(
                    "SugesZap prépare des réponses modifiables. " +
                    "AutoZap peut répondre pour les contacts autorisés " +
                    "si la notification de l'application source propose " +
                    "une action de réponse compatible. " +
                    "Maximum : 4 contacts par application."
                )
                .setPositiveButton("Compris", null)
                .show()
        }

        body.addView(TextView(this).apply {
            text = "STEVE • UNE EXPÉRIENCE PLUS INTELLIGENTE"
            textSize = 12f
            gravity = Gravity.CENTER
            typeface = Typeface.create("serif", Typeface.BOLD_ITALIC)
            setTextColor(gold)
            setPadding(0, dp(22), 0, dp(12))
        })

        scroll.addView(body)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(page)
    }

    private fun section(parent: LinearLayout, title: String) {
        parent.addView(TextView(this).apply {
            text = title
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(blue)
            setPadding(dp(4), dp(12), dp(4), dp(8))
        })
    }

    private fun button(
        parent: LinearLayout,
        title: String,
        subtitle: String,
        action: () -> Unit
    ) {
        val b = Button(this).apply {
            text = "$title\\n$subtitle"
            isAllCaps = false
            textSize = 14f
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.WHITE)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                setColor(panel)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), blue)
            }
            setOnClickListener { action() }
        }
        parent.addView(
            b,
            LinearLayout.LayoutParams(-1, dp(68)).apply {
                bottomMargin = dp(8)
            }
        )
    }

    private fun openAppDetails() {
        try {
            startActivity(Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            ))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun openNotificationSettings() {
        try {
            startActivity(Intent(
                Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            ))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun configureGemini() {
        val edit = EditText(this).apply {
            hint = "Clé API Gemini"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            inputType = 129
            setText(
                getSharedPreferences("ai10_settings", MODE_PRIVATE)
                    .getString("gemini_key", "")
            )
        }

        AlertDialog.Builder(this)
            .setTitle("Gemini — AI10")
            .setMessage(
                "Crée une clé API dans Google AI Studio " +
                "et garde-la privée."
            )
            .setView(edit)
            .setPositiveButton("Enregistrer") { _, _ ->
                GeminiAI.saveKey(this, edit.text.toString())
                Toast.makeText(
                    this, "Configuration enregistrée",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}
