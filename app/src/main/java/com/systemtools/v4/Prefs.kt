package com.systemtools.v4

import android.content.Context

/** Penyimpanan preferensi sederhana. */
object Prefs {
    private const val NAME = "sys40"

    fun get(context: Context, key: String, def: String = ""): String {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(key, def) ?: def
    }

    fun put(context: Context, key: String, value: String) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit().putString(key, value).apply()
    }

    fun getBool(context: Context, key: String, def: Boolean = false): Boolean {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getBoolean(key, def)
    }

    fun putBool(context: Context, key: String, value: Boolean) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(key, value).apply()
    }

    const val KEY_PIN = "pin"
    const val KEY_TRIGGER_NUMBER = "trigger_number"
    const val KEY_SELF_HIDDEN = "self_hidden"

    /** Nomor trigger default (bisa diganti di layar setup). */
    const val DEFAULT_TRIGGER = "9991"
}
