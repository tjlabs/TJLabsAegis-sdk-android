package com.tjlabs.tjlabsaegis_sdk_android.uvd

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.tjlabs.tjlabsaegis_sdk_android.utils.TJLabsUtilFunctions
import java.util.Timer
import java.util.TimerTask


internal class TJLabsSensorManager(private val context : Context, private val frequency : Int): SensorEventListener {
    interface SensorResultListener {
        fun onSensorChangedResult(sensorData : SensorData)
    }

    private val sensorManager by lazy {//센서데이터 수집용
        context.getSystemService(SENSOR_SERVICE) as SensorManager
    }

    private var sensorData = SensorData()
    private var sensorTimer: Timer? = null
    private var isRunning = false

    init {
        initSensorManager()
    }

    private fun initSensorManager() {
        sensorManager.registerListener(this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this,
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED), SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this,
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this,
            sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_GAME)
    }

    fun checkSensorAvailability() : Pair<Boolean, String> {
        val accCheck = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroCheck = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val magUnCaliCheck = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED)
        val magCheck = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val pressureCheck = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        val rotationVectorCheck = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val gameRotationVectorCheck = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        var errorMsg = ""
        if (accCheck == null) {
            errorMsg += "acc"
        }
        if (gyroCheck == null) {
            errorMsg += ",gyro"
        }
        if (magUnCaliCheck == null) {
            errorMsg += ",unCali mag"
        }
        if (magCheck == null) {
            errorMsg += ",mag"
        }
        if (pressureCheck == null) {
            errorMsg += ",pressure"
        }
        if (rotationVectorCheck == null) {
            errorMsg += ",rotation vector"
        }
        if (gameRotationVectorCheck == null) {
            errorMsg += ",game rotation vector"
        }

        return if (accCheck != null && gyroCheck != null && magUnCaliCheck != null && magCheck != null &&
            pressureCheck != null && rotationVectorCheck != null && gameRotationVectorCheck != null){
            Pair(true, "Sensor is available")
        }else{
            Pair(false, "Sensor is not available")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, sensorData.acc, 0, sensorData.acc.size)
                }

                Sensor.TYPE_GYROSCOPE -> {
                    System.arraycopy(event.values, 0, sensorData.gyro, 0, sensorData.gyro.size)
                }

                Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> {
                    System.arraycopy(event.values, 0, sensorData.magRaw, 0, sensorData.magRaw.size)
                }

                Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                    System.arraycopy(event.values, 0, sensorData.gameVector, 0, sensorData.gameVector.size)
                }

                Sensor.TYPE_ROTATION_VECTOR -> {
                    System.arraycopy(event.values, 0, sensorData.rotVector, 0, sensorData.rotVector.size)
                }

                Sensor.TYPE_PRESSURE -> {
                    System.arraycopy(event.values, 0, sensorData.pressure, 0, sensorData.pressure.size)
                }
            }
        }
    }

    fun getSensorDataResultOrNull(callback: SensorResultListener) {
        isRunning = true
        initSensorManager()

        val intervalMillis = TJLabsUtilFunctions.frequency2Millis(frequency).toLong()
        val timer = Timer()
        sensorTimer = timer

        timer.schedule(object : TimerTask() {
            override fun run() {
                if (!isRunning) {
                    timer.cancel()
                    return
                }
                callback.onSensorChangedResult(sensorData)
            }
        }, 0, intervalMillis)
    }


    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }

    fun stopSensorChanged() : Pair<Boolean, String> {
        if (!isRunning)  return Pair(false, "SensorManager is not started.")

        sensorManager.unregisterListener(this)
        sensorData = SensorData()
        // 실행 중이 아니면 무시
        isRunning = false
        sensorTimer?.cancel()
        sensorTimer = null
        return Pair(true, "Success Stop Sensor Changed")
    }

}