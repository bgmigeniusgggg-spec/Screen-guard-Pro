package com.guard.screen.security

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.guard.screen.core.Constants
import com.guard.screen.core.Logger
import com.guard.screen.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StealthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun hideIcon() {
        try {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, MainActivity::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(Constants.KEY_ICON_HIDDEN, true).apply()
            Logger.d("Stealth", "Icon hidden")
        } catch (e: Exception) {
            Logger.e("Stealth", "hideIcon failed", e)
        }
    }

    fun showIcon() {
        try {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, MainActivity::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(Constants.KEY_ICON_HIDDEN, false).apply()
            Logger.d("Stealth", "Icon shown")
        } catch (e: Exception) {
            Logger.e("Stealth", "showIcon failed", e)
        }
    }

    fun isIconHidden(): Boolean {
        return context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(Constants.KEY_ICON_HIDDEN, false)
    }
}
