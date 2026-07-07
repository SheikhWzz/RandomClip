package com.randomclip.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.randomclip.app.data.SettingsRepository
import com.randomclip.app.data.VideoRepository
import com.randomclip.app.data.VideoScanner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class RandomClipApplication : Application() {

    val settingsRepository by lazy { SettingsRepository(this) }
    val videoRepository by lazy { VideoRepository(this, VideoScanner(this)) }

    override fun onCreate() {
        super.onCreate()
        applyStoredLocale()
    }

    private fun applyStoredLocale() {
        val language = runBlocking {
            settingsRepository.settings.first().language
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
    }
}
