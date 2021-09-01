package com.thoughtcrime.v2raylite

import android.app.Application
import com.tencent.mmkv.MMKV
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App: Application() {
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
    }
}