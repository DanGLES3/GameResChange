package com.game.reschange

import android.content.Context
import androidx.core.content.edit

object ScalePrefs {
    private const val PREF_NAME = "scale_prefs"

    fun saveScale(context: Context, packageName: String, scale: Float) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit {
                putFloat(packageName, scale)
            }
    }

    fun getAllPackages(context: Context): Set<String> {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).all.keys
    }

    fun getScale(context: Context, packageName: String): Float {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getFloat(packageName, 1.0f) // Default to 1.0 (no scale)
    }
}