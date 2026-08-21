package com.abht.manga_dt

import android.app.Application
import com.abht.manga_dt.data.SettingsStorage

class MangaDTApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsStorage.init(this)
    }
}
