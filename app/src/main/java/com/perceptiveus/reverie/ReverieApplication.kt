package com.perceptiveus.reverie

import android.app.Application

class ReverieApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
    }

    companion object {
        lateinit var instance: ReverieApplication
            private set
    }
}
