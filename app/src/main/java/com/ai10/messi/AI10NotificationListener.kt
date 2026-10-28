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
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class AI10NotificationListener : NotificationListenerService() {
    private var window: LinearLayout? = null
    private var content: LinearLayout? = null
    private var wm: WindowManager? = null
    private var currentTab = "AutoZap"
    private var currentApp = ""
    private var currentPkg = ""
    private var currentSender = ""
    private var currentMessage = ""
    private var currentAction: Notification.Action? = null
    private var draft: EditText? = null

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

        currentApp = app
        currentPkg = sbn.packageName
        currentSender = sender
        currentMessage = message
        currentAction = action

        if (Settings.canDrawOverlays(this)) {
            val autoEnabled = prefs.getBoolean("floating_autozap", true)
            val suggestionsEnabled = prefs.getBoolean("floating_sugeszap", true)

            if (autoEnabled || suggestionsEnabled) {
                showFloatingWindow()
            } else {
                removeWindow()
            }
        }

        if (prefs.getBoolean("floating_autozap", true) &&
            ContactRules.automatic(this, sbn.packageName, sender) &&
            action != null
        ) {
            GeminiAI.suggest(this, "$sender: $message") { answer ->
                if (isUsableAnswer(answer)) sendReply(action, answer)
            }
        }
    }

    private fun isUsableAnswer(answer: String): Boolean {
        val a = answer.trim()
        return a.isNotBlank() &&
            listOf(
                "Ajoute d’abord", "Connexion Gemini", "Erreur Gemini",
                "Limite Gemini", "Clé Gemini"
            ).none { a.startsWith(it, ignoreCase = true) }
    }

    private fun showFloatingWindow() {
        try {
            val manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm = manager

            if (window == null) {
                val root = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(12, 10, 12, 10)
                    background = GradientDrawable().apply {
                        setColor(Color.rgb(12, 17, 28))
                        cornerRadius = 24f
                        setStroke(2, Color.rgb(0, 127, 255))
                    }
                }

                val header = TextView(this).apply {
                    text = "S • AI10 V7"
                    textSize = 19f
                    setTextColor(Color.rgb(230, 184, 55))
                    setPadding(4, 4, 4, 8)
                }
                root.addView(header)

                val tabs = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                tabs.addView(makeButton("AutoZap") {
                    currentTab = "AutoZap"
                    renderContent()
                }, LinearLayout.LayoutParams(0, -2, 1f))
                tabs.addView(makeButton("SugesZap") {
                    currentTab = "SugesZap"
                    renderContent()
                }, LinearLayout.LayoutParams(0, -2, 1f))
                tabs.addView(makeButton("Fermer ✕") {
                    removeWindow()
                }, LinearLayout.LayoutParams(0, -2, 1f))
                root.addView(tabs)

                val scroll = ScrollView(this)
                content = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                }
                scroll.addView(content)
                root.addView(
                    scroll,
                    LinearLayout.LayoutParams(-1, -2).apply {
                        weight = 1f
                    }
                )

                window = root
                manager.addView(root, layoutParams())
            }

            renderContent()
        } catch (_: Exception) {
            // Android peut refuser l'affichage si l'autorisation est absente.
        }
    }

    private fun makeButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }
    }

    private fun addText(parent: LinearLayout, value: String) {
        parent.addView(TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(4, 8, 4, 8)
        })
    }

    private fun renderContent() {
        val panel = content ?: return
        panel.removeAllViews()
        draft = null

        val autoEnabled = prefs.getBoolean("floating_autozap", true)
        val suggestionsEnabled = prefs.getBoolean("floating_sugeszap", true)

        addText(panel, "$currentApp • $currentSender")
        addText(panel, currentMessage)

        if (currentTab == "AutoZap") {
            if (!autoEnabled) {
                addText(panel, "AutoZap est désactivé dans les paramètres.")
                return
            }

            val allowed = ContactRules.allowed(this, currentPkg, currentSender)
            val automatic = ContactRules.automatic(this, currentPkg, currentSender)

            addText(panel, "Contacts autorisés : ${ContactRules.count(this, currentPkg)}/10")
            panel.addView(makeButton(
                if (allowed) "Retirer ce contact"
                else "Autoriser ce contact"
            ) {
                val changed = ContactRules.setAllowed(
                    this, currentPkg, currentSender, !allowed
                )
                if (!changed) {
                    Toast.makeText(
                        this, "Maximum 10 contacts pour cette application",
                        Toast.LENGTH_LONG
                    ).show()
                }
                renderContent()
            })

            panel.addView(makeButton(
                if (automatic) "Réponse automatique : ACTIVÉE"
                else "Activer la réponse automatique"
            ) {
                if (!ContactRules.allowed(this, currentPkg, currentSender)) {
                    Toast.makeText(
                        this, "Autorise d'abord ce contact",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    ContactRules.setAutomatic(
                        this, currentPkg, currentSender, !automatic
                    )
                }
                renderContent()
            })

            addText(
                panel,
                if (automatic) "Gemini peut répondre automatiquement à ce contact."
                else "La réponse automatique n'est pas activée pour ce contact."
            )
        } else {
            if (!suggestionsEnabled) {
                addText(panel, "SugesZap est désactivé dans les paramètres.")
                return
            }

            val editor = EditText(this).apply {
                hint = "Écris ou modifie ta réponse"
                setTextColor(Color.WHITE)
                setHintTextColor(Color.LTGRAY)
                setText("")
                minLines = 2
            }
            draft = editor
            panel.addView(editor, LinearLayout.LayoutParams(-1, -2))

            panel.addView(makeButton("Générer une suggestion Gemini") {
                GeminiAI.suggest(this, "$currentSender: $currentMessage") { answer ->
                    if (isUsableAnswer(answer)) {
                        editor.setText(answer)
                        editor.setSelection(editor.text.length)
                    } else {
                        Toast.makeText(
                            this, answer, Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })

            panel.addView(makeButton("Envoyer la réponse") {
                val answer = editor.text.toString().trim()
                val action = currentAction
                if (answer.isBlank()) {
                    Toast.makeText(
                        this, "Écris ou génère une réponse d'abord",
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (action == null) {
                    Toast.makeText(
                        this,
                        "Aucune action de réponse directe dans cette notification.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    sendReply(action, answer)
                }
            })
        }
    }

    private fun layoutParams(): WindowManager.LayoutParams {
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
            y = 70
        }
    }

    private fun removeWindow() {
        val root = window ?: return
        try {
            wm?.removeView(root)
        } catch (_: Exception) {
        }
        window = null
        content = null
        draft = null
    }

    override fun onDestroy() {
        removeWindow()
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
