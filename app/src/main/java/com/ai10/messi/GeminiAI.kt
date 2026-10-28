package com.ai10.messi

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object GeminiAI {
    private const val PREFS = "ai10_settings"
    private const val KEY = "gemini_key"
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun saveKey(ctx: Context, key: String) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, key.trim()).apply()
    fun hasKey(ctx: Context) = !ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").isNullOrBlank()

    fun suggest(ctx: Context, conversation: String, callback: (String) -> Unit) {
        val key = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()
        if (key.isBlank()) { main.post { callback("Ajoute d’abord ta clé API Gemini dans AI10.") }; return }
        executor.execute {
            var result = "Gemini n’a pas pu répondre. Vérifie Internet et ta clé API."
            var connection: HttpURLConnection? = null
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 15000; readTimeout = 25000; doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                val prompt = "Tu es AI10, assistant de messagerie. Écris une réponse courte, naturelle, polie en français. Retourne uniquement le texte de réponse. Conversation:\n$conversation"
                val body = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                    .put("generationConfig", JSONObject().put("temperature", 0.7).put("maxOutputTokens", 180))
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code in 200..299) result = JSONObject(raw).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text").orEmpty().trim().ifBlank { result }
                else result = when (code) { 400,403 -> "Clé Gemini refusée : vérifie la configuration."; 429 -> "Limite Gemini atteinte. Réessaie plus tard."; else -> "Erreur Gemini ($code)." }
            } catch (_: Exception) { result = "Connexion Gemini impossible. Vérifie Internet." }
            finally { connection?.disconnect() }
            main.post { callback(result) }
        }
    }
}
