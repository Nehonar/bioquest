package com.bioquest.data.health

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

/**
 * Fallback step source using TYPE_STEP_COUNTER. This only reports whether the
 * device exposes the sensor and the raw cumulative count; turning that into a
 * per-day delta requires a persisted baseline (left as documented tech debt).
 * Requires ACTIVITY_RECOGNITION on Android 10+.
 */
class StepSensorSource(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    val isSupported: Boolean get() = stepSensor != null
}
