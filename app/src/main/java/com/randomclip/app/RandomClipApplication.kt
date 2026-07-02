package com.randomclip.app

import android.app.Application
import com.randomclip.app.data.SettingsRepository
import com.randomclip.app.data.VideoRepository
import com.randomclip.app.data.VideoScanner

class RandomClipApplication : Application() {
    val settingsRepository by lazy { SettingsRepository(this) }
    val videoRepository by lazy { VideoRepository(this, VideoScanner(this)) }
}
