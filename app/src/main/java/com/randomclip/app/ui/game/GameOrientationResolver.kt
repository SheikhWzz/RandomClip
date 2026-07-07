package com.randomclip.app.ui.game

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.provider.Settings
import android.view.Surface
import com.randomclip.app.ui.GameModeScreen

object GameOrientationResolver {

    fun isSystemAutoRotateEnabled(context: Context): Boolean {
        return Settings.System.getInt(
            context.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            0,
        ) == 1
    }

    /**
     * Hub, settings and preview follow phone rotation when system auto-rotate is on.
     * When auto-rotate is turned off, [frozenOrientation] keeps the last orientation.
     * Playing locks to the mode chosen in preview (portrait one-hand vs landscape).
     */
    fun resolveForGameFlow(
        context: Context,
        screen: GameModeScreen,
        landscapeGameEnabled: Boolean,
        frozenOrientation: Int,
    ): Int {
        val rotatesWithPhone = screen == GameModeScreen.HUB ||
            screen == GameModeScreen.SETTINGS ||
            screen == GameModeScreen.PREVIEW ||
            screen == GameModeScreen.NONE

        if (rotatesWithPhone) {
            return if (isSystemAutoRotateEnabled(context)) {
                ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            } else {
                frozenOrientation
            }
        }

        return if (landscapeGameEnabled) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    fun resolveForNormalApp(lockPortrait: Boolean): Int {
        return if (lockPortrait) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    fun orientationFromDisplay(rotation: Int?): Int {
        return when (rotation) {
            Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    fun orientationFromConfiguration(configuration: Configuration, displayRotation: Int?): Int {
        return if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            when (displayRotation) {
                Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                else -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        } else {
            when (displayRotation) {
                Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }
}
