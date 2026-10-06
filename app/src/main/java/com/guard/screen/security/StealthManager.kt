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

    /**
     * App icon hide karo.
     */
    fun hideIcon() {
        try {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, MainActivity::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            saveIconState(true)
            Logger.d("Stealth", "Icon hidden")
        } catch (e: Exception) {
            Logger.e("Stealth", "hideIcon failed", e)
        }
    }

    /**
     * App icon wapas dikhao.
     */
    fun showIcon() {
        try {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, MainActivity::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            saveIconState(false)
            Logger.d("Stealth", "Icon shown")
        } catch (e: Exception) {
            Logger.e("Stealth", "showIcon failed", e)
        }
    }

    /**
     * Icon hidden hai?
     */
    fun isIconHidden(): Boolean {
        return context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(Constants.KEY_ICON_HIDDEN, false)
    }

    /**
     * App label change karo (advanced).
     */
    fun isAppEnabled(): Boolean {
        return try {
            val setting = context.packageManager.getComponentEnabledSetting(
                ComponentName(context, MainActivity::class.java)
            )
            setting != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } catch (e: Exception) {
            true
        }
    }

    private fun saveIconState(hidden: Boolean) {
        context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(Constants.KEY_ICON_HIDDEN, hidden)
            .apply()
    }
}
