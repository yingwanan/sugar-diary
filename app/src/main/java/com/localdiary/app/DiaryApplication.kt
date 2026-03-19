package com.localdiary.app

import android.app.Application
import com.localdiary.app.di.AppContainer

class DiaryApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
