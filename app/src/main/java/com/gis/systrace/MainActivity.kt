package com.gis.systrace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.gis.systrace.presentation.SysTraceAppRoot
import com.gis.systrace.service.ServiceStarter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceStarter.start(this)
        setContent {
            SysTraceAppRoot()
        }
    }
}
