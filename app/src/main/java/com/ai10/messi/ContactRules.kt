package com.ai10.messi

import android.content.Context

object ContactRules {
    private const val PREFS = "ai10_settings"
    private const val ALLOWED = "allowed_contacts"
    private const val AUTO = "automatic_contacts"
    private const val SEP = "\u001f"
    private const val LIMIT = 4
    private fun id(pkg: String, sender: String) = "$pkg$SEP$sender"
    private fun read(ctx: Context, key: String) =
        (ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(key, emptySet()) ?: emptySet()).toMutableSet()

    @Synchronized fun allowed(ctx: Context, pkg: String, sender: String) = read(ctx, ALLOWED).contains(id(pkg, sender))
    @Synchronized fun automatic(ctx: Context, pkg: String, sender: String) = allowed(ctx, pkg, sender) && read(ctx, AUTO).contains(id(pkg, sender))
    @Synchronized fun count(ctx: Context, pkg: String) = read(ctx, ALLOWED).count { it.startsWith("$pkg$SEP") }

    @Synchronized fun setAllowed(ctx: Context, pkg: String, sender: String, enabled: Boolean): Boolean {
        val key = id(pkg, sender); val allow = read(ctx, ALLOWED); val auto = read(ctx, AUTO)
        if (enabled && !allow.contains(key) && count(ctx, pkg) >= LIMIT) return false
        if (enabled) allow.add(key) else { allow.remove(key); auto.remove(key) }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet(ALLOWED, allow).putStringSet(AUTO, auto).apply()
        return true
    }

    @Synchronized fun setAutomatic(ctx: Context, pkg: String, sender: String, enabled: Boolean): Boolean {
        if (!allowed(ctx, pkg, sender)) return false
        val auto = read(ctx, AUTO); val key = id(pkg, sender)
        if (enabled) auto.add(key) else auto.remove(key)
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet(AUTO, auto).apply()
        return true
    }
}
