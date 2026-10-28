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
    private var wm: WindowManager? = null
    private var floating: LinearLayout? = null

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val extras = sbn.notification.extras ?: return
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()).orEmpty().trim()
        if (sender.isBlank() || text.isBlank()) return
        val app = try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString() } catch (_: Exception) { sbn.packageName }
        val action = sbn.notification.actions?.firstOrNull { it.remoteInputs?.any { input -> input.allowFreeFormInput } == true }
        if (Settings.canDrawOverlays(this)) showFloating(app, sbn.packageName, sender, text, action)
        if (ContactRules.automatic(this, sbn.packageName, sender) && action != null) {
            GeminiAI.suggest(this, "$sender: $text") { answer ->
                if (answer.isNotBlank() && !answer.startsWith("Ajoute d’abord") && !answer.startsWith("Connexion Gemini") && !answer.startsWith("Erreur Gemini") && !answer.startsWith("Limite Gemini") && !answer.startsWith("Clé Gemini")) sendReply(action, answer)
            }
        }
    }

    private fun showFloating(app: String, pkg: String, sender: String, message: String, action: Notification.Action?) {
        try {
            val manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager; wm = manager
            floating?.let { try { manager.removeView(it) } catch (_: Exception) {} }
            val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(14,12,14,12); background = GradientDrawable().apply { setColor(Color.rgb(12,12,12)); cornerRadius = 28f; setStroke(2, Color.rgb(212,175,55)) } }
            root.addView(TextView(this).apply { text = "♛  AI10 • $app\n$sender"; textSize = 16f; setTextColor(Color.rgb(245,205,80)); setPadding(4,4,4,10) })
            root.addView(TextView(this).apply { text = message; textSize = 14f; setTextColor(Color.WHITE); setPadding(4,4,4,8) })
            val allowed = ContactRules.allowed(this, pkg, sender)
            addButton(root, if (allowed) "Retirer ce contact" else "Autoriser ce contact (${ContactRules.count(this,pkg)}/4)") {
                if (!ContactRules.setAllowed(this, pkg, sender, !allowed)) Toast.makeText(this, "Maximum 4 contacts pour cette application", Toast.LENGTH_LONG).show()
                else showFloating(app,pkg,sender,message,action)
            }
            addButton(root, if (ContactRules.automatic(this,pkg,sender)) "AutoZap : activé" else "AutoZap : désactivé") {
                if (!allowed) Toast.makeText(this, "Autorise d’abord ce contact", Toast.LENGTH_SHORT).show()
                else { ContactRules.setAutomatic(this,pkg,sender,!ContactRules.automatic(this,pkg,sender)); showFloating(app,pkg,sender,message,action) }
            }
            val draft = EditText(this).apply { setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); hint = "SugesZap : écris ou modifie ta réponse"; setText("") }
            root.addView(draft, LinearLayout.LayoutParams(-1, -2))
            addButton(root, "Générer une suggestion Gemini") { GeminiAI.suggest(this, "$sender: $message") { draft.setText(it); draft.setSelection(draft.text.length) } }
            addButton(root, "Envoyer la réponse") { if (action == null) Toast.makeText(this, "Cette application ne propose pas de réponse via notification. Copie le texte et réponds dans l’application.", Toast.LENGTH_LONG).show() else sendReply(action,draft.text.toString()) }
            val close = Button(this).apply { text = "Fermer ✕"; isAllCaps = false; setOnClickListener { try { manager.removeView(root) } catch (_: Exception) {}; if (floating === root) floating = null } }; root.addView(close)
            val params = WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT, if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = 80 }
            manager.addView(root, params); floating = root
        } catch (_: Exception) { }
    }

    private fun addButton(parent: LinearLayout, label: String, action: () -> Unit) {
        parent.addView(Button(this).apply { text = label; isAllCaps = false; setOnClickListener { action() } }, LinearLayout.LayoutParams(-1, -2))
    }

    private fun sendReply(action: Notification.Action, answer: String) {
        val inputs = action.remoteInputs ?: return
        val intent = Intent(); val results = Bundle()
        inputs.forEach { input -> results.putCharSequence(input.resultKey, answer) }
        RemoteInput.addResultsToIntent(inputs, intent, results)
        try { action.actionIntent.send(this, 0, intent) } catch (_: Exception) { Toast.makeText(this, "Envoi impossible depuis cette notification", Toast.LENGTH_LONG).show() }
    }
}
