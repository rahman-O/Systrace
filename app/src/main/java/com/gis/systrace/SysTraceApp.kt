package com.gis.systrace

import android.app.Application
import com.gis.systrace.service.ServiceStarter
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SysTraceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceStarter.start(this)
    }
}
