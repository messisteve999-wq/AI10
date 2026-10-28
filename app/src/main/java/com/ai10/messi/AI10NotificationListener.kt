package com.ai10.messi

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class AI10NotificationListener : NotificationListenerService() {
    private var autoWindow: LinearLayout? = null
    private var suggestionWindow: LinearLayout? = null
    private var wm: WindowManager? = null

    private val prefs by lazy {
        getSharedPreferences("ai10_settings", Context.MODE_PRIVATE)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras ?: return
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()?.trim().orEmpty()
        val message = (
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ).orEmpty().trim()

        if (sender.isBlank() || message.isBlank()) return

        val app = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        } catch (_: Exception) {
            sbn.packageName
        }

        val action = sbn.notification.actions?.firstOrNull { a ->
            a.remoteInputs?.any { it.allowFreeFormInput } == true
        }

        if (!Settings.canDrawOverlays(this)) return

        if (prefs.getBoolean("floating_autozap", true)) {
            showAutoZap(app, sbn.packageName, sender, message)
        } else {
            removeAutoWindow()
        }

        if (prefs.getBoolean("floating_sugeszap", true)) {
            showSugesZap(app, sbn.packageName, sender, message, action)
        } else {
            removeSuggestionWindow()
        }

        if (ContactRules.automatic(this, sbn.packageName, sender) &&
            action != null
        ) {
            GeminiAI.suggest(this, "$sender: $message") { answer ->
                if (answer.isNotBlank() &&
                    !answer.startsWith("Ajoute d’abord") &&
                    !answer.startsWith("Connexion Gemini") &&
                    !answer.startsWith("Erreur Gemini") &&
                    !answer.startsWith("Limite Gemini") &&
                    !answer.startsWith("Clé Gemini")
                ) {
                    sendReply(action, answer)
                }
            }
        }
    }

    private fun createWindow(title: String, y: Int): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 10, 12, 10)
            background = GradientDrawable().apply {
                setColor(Color.rgb(12, 17, 28))
                cornerRadius = 24f
                setStroke(2, Color.rgb(0, 127, 255))
            }
        }

        root.addView(TextView(this).apply {
            text = title
            textSize = 17f
            setTextColor(Color.rgb(230, 184, 55))
            setPadding(4, 4, 4, 8)
        })

        root.tag = y
        return root
    }

    private fun addButton(
        parent: LinearLayout,
        label: String,
        action: () -> Unit
    ) {
        parent.addView(Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(-1, -2))
    }

    private fun showAutoZap(
        app: String,
        pkg: String,
        sender: String,
        message: String
    ) {
        try {
            val manager = getSystemService(Context.WINDOW_SERVICE)
                as WindowManager
            wm = manager
            removeAutoWindow()

            val root = createWindow("♛ AutoZap • $app", 80)
            root.addView(TextView(this).apply {
                text = "$sender\n$message"
                textSize = 14f
                setTextColor(Color.WHITE)
            })

            val allowed = ContactRules.allowed(this, pkg, sender)
            addButton(
                root,
                if (allowed) "Retirer ce contact"
                else "Autoriser ce contact (${ContactRules.count(this, pkg)}/4)"
            ) {
                if (!ContactRules.setAllowed(this, pkg, sender, !allowed)) {
                    Toast.makeText(
                        this, "Maximum 4 contacts par application",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    showAutoZap(app, pkg, sender, message)
                }
            }

            addButton(
                root,
                if (ContactRules.automatic(this, pkg, sender))
                    "Réponse automatique : ACTIVÉE"
                else "Réponse automatique : DÉSACTIVÉE"
            ) {
                if (!ContactRules.allowed(this, pkg, sender)) {
                    Toast.makeText(
                        this, "Autorise d'abord ce contact",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    ContactRules.setAutomatic(
                        this, pkg, sender,
                        !ContactRules.automatic(this, pkg, sender)
                    )
                    showAutoZap(app, pkg, sender, message)
                }
            }

            addButton(root, "Fermer AutoZap ✕") {
                removeAutoWindow()
            }

            val params = layoutParams(70)
            manager.addView(root, params)
            autoWindow = root
        } catch (_: Exception) {
            // L'accès aux fenêtres peut être refusé par Android.
        }
    }

    private fun showSugesZap(
        app: String,
        pkg: String,
        sender: String,
        message: String,
        action: Notification.Action?
    ) {
        try {
            val manager = getSystemService(Context.WINDOW_SERVICE)
                as WindowManager
            wm = manager
            removeSuggestionWindow()

            val root = createWindow("✦ SugesZap • $app", 430)
            root.addView(TextView(this).apply {
                text = "Message de $sender"
                textSize = 14f
                setTextColor(Color.WHITE)
            })

            val draft = EditText(this).apply {
                hint = "Écris ou modifie ta réponse"
                setTextColor(Color.WHITE)
                setHintTextColor(Color.LTGRAY)
                setText("")
            }
            root.addView(draft, LinearLayout.LayoutParams(-1, -2))

            addButton(root, "Générer une suggestion Gemini") {
                GeminiAI.suggest(this, "$sender: $message") { answer ->
                    draft.setText(answer)
                    draft.setSelection(draft.text.length)
                }
            }

            addButton(root, "Envoyer la réponse") {
                if (action == null) {
                    Toast.makeText(
                        this,
                        "Pas de réponse directe disponible pour cette notification.",
                        Toast.LENGTH_LONG
                    ).show()
                } else if (draft.text.toString().isNotBlank()) {
                    sendReply(action, draft.text.toString())
                } else {
                    Toast.makeText(
                        this, "Génère ou écris une réponse d'abord",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            addButton(root, "Fermer SugesZap ✕") {
                removeSuggestionWindow()
            }

            manager.addView(root, layoutParams(390))
            suggestionWindow = root
        } catch (_: Exception) {
            // L'accès aux fenêtres peut être refusé par Android.
        }
    }

    private fun layoutParams(yPosition: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = yPosition
        }
    }

    private fun removeAutoWindow() {
        val root = autoWindow ?: return
        try {
            wm?.removeView(root)
        } catch (_: Exception) {
        }
        autoWindow = null
    }

    private fun removeSuggestionWindow() {
        val root = suggestionWindow ?: return
        try {
            wm?.removeView(root)
        } catch (_: Exception) {
        }
        suggestionWindow = null
    }

    override fun onDestroy() {
        removeAutoWindow()
        removeSuggestionWindow()
        super.onDestroy()
    }

    private fun sendReply(action: Notification.Action, answer: String) {
        val inputs = action.remoteInputs ?: return
        val intent = Intent()
        val results = Bundle()

        inputs.forEach { input ->
            results.putCharSequence(input.resultKey, answer)
        }
        RemoteInput.addResultsToIntent(inputs, intent, results)

        try {
            action.actionIntent.send(this, 0, intent)
        } catch (_: Exception) {
            Toast.makeText(
                this, "Envoi impossible depuis cette notification",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
