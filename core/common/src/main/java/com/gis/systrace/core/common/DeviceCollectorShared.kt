package com.gis.systrace.core.common

import android.content.Context
import com.gis.systrace.core.common.collectors.DynamicDeviceCollector
import com.gis.systrace.core.common.collectors.withDynamicFields

object DeviceCollectorShared {
    fun collect(context: Context, options: CollectOptions = CollectOptions()): DeviceSnapshot {
        val static = StaticDeviceCollector.collect(context)
        val dynamic = DynamicDeviceCollector.collect(context, options)
        return static.withDynamicFields(dynamic)
    }
}
