package com.gis.systrace.core.common.collectors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

data class SensorDetail(
    val name: String,
    val type: String,
    val vendor: String,
)

object SensorsCollector {
    fun collect(context: Context): List<SensorDetail> {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return emptyList()
        return manager.getSensorList(Sensor.TYPE_ALL).map { sensor ->
            SensorDetail(
                name = sensor.name.orEmpty(),
                type = sensorTypeLabel(sensor.type),
                vendor = sensor.vendor.orEmpty(),
            )
        }
    }

    private fun sensorTypeLabel(type: Int): String = when (type) {
        Sensor.TYPE_ACCELEROMETER -> "accelerometer"
        Sensor.TYPE_MAGNETIC_FIELD -> "magnetic_field"
        Sensor.TYPE_ORIENTATION -> "orientation"
        Sensor.TYPE_GYROSCOPE -> "gyroscope"
        Sensor.TYPE_LIGHT -> "light"
        Sensor.TYPE_PRESSURE -> "pressure"
        Sensor.TYPE_PROXIMITY -> "proximity"
        Sensor.TYPE_GRAVITY -> "gravity"
        Sensor.TYPE_LINEAR_ACCELERATION -> "linear_acceleration"
        Sensor.TYPE_ROTATION_VECTOR -> "rotation_vector"
        Sensor.TYPE_AMBIENT_TEMPERATURE -> "ambient_temperature"
        Sensor.TYPE_RELATIVE_HUMIDITY -> "relative_humidity"
        else -> "type_$type"
    }
}
