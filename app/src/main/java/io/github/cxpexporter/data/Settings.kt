package io.github.cxpexporter.data

import android.content.Context
import androidx.core.content.edit

/** Small preferences wrapper; holds no secrets. */
class Settings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var updateCheckEnabled: Boolean
        get() = prefs.getBoolean(KEY_UPDATE_CHECK, true)
        set(value) = prefs.edit { putBoolean(KEY_UPDATE_CHECK, value) }

    /** Epoch milliseconds of the last successful update check, 0 when never. */
    var lastCheckMs: Long
        get() = prefs.getLong(KEY_LAST_CHECK, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_CHECK, value) }

    companion object {
        const val PREFS_NAME = "settings"
        const val KEY_UPDATE_CHECK = "update_check"
        const val KEY_LAST_CHECK = "update_last_check"
    }
}
