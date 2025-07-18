package com.tjlabs.tjlabsaegis_sdk_android.uvd

import android.app.Application
import com.tjlabs.tjlabsaegis_sdk_android.simulation.JupiterSimulator
import com.tjlabs.tjlabsaegis_sdk_android.simulation.JupiterSimulator.convertToSensorData
import com.tjlabs.tjlabsaegis_sdk_android.simulation.JupiterSimulator.saveDataFunction
import com.tjlabs.tjlabsaegis_sdk_android.simulation.JupiterSimulator.sensorMutableList
import com.tjlabs.tjlabsaegis_sdk_android.simulation.JupiterSimulator.sensorSimulationIndex
import com.tjlabs.tjlabsaegis_sdk_android.uvd.pdr.TJLabsPDRDistanceEstimator


const val sensorFrequency = 40

internal class UVDGenerator(private val application: Application, private val userId : String = "") {
    interface UVDCallback {
        fun onUvdResult(mode : UserMode, uvd: UserVelocity)

        fun onPressureResult(hPa : Float)

        fun onVelocityResult(kmPh : Float)

        fun onMagNormSmoothingVarResult(value : Float)

        fun onUvdPauseMillis(time : Long)

        fun onUvdError(error : String)
    }
    private val tjLabsSensorManager : TJLabsSensorManager = TJLabsSensorManager(application,
        sensorFrequency
    )
    private var tjLabsAttitudeEstimator : TJLabsAttitudeEstimator = TJLabsAttitudeEstimator(
        sensorFrequency
    )
    private var tjLabsPdrDistanceEstimator : TJLabsPDRDistanceEstimator = TJLabsPDRDistanceEstimator()
    private var tjLabsUnitStatusEstimator = TJLabsUnitStatusEstimator()
    private var uvdGenerationTimeMillis = 0L
    private var userMode = UserMode.MODE_PEDESTRIAN
    private var drVelocityScale = 1f

    private var preTime = 0L
    fun setUserMode(mode: UserMode) {
        userMode = mode
    }

    fun checkIsAvailableUvd(callback : UVDCallback, completion : (Boolean, String) -> Unit) {
        val (isCheckSensorSuccess, msgCheckSensor) = tjLabsSensorManager.checkSensorAvailability()
        if (isCheckSensorSuccess) {
            completion(true, msgCheckSensor)
        } else {
            completion(false, msgCheckSensor)
            callback.onUvdError(msgCheckSensor)
        }
    }

    fun generateUvd(defaultPDRStepLength: Float = tjLabsPdrDistanceEstimator.getDefaultStepLength(),
                    minPDRStepLength : Float = tjLabsPdrDistanceEstimator.getMinStepLength(),
                    maxPDRStepLength : Float = tjLabsPdrDistanceEstimator.getMaxStepLength(),
                    isSaveData : Boolean = false,
                    fileName : String = "",
                    callback : UVDCallback
    ) {

        uvdGenerationTimeMillis = System.currentTimeMillis()
        tjLabsPdrDistanceEstimator.setDefaultStepLength(defaultPDRStepLength)
        tjLabsPdrDistanceEstimator.setMinStepLength(minPDRStepLength)
        tjLabsPdrDistanceEstimator.setMaxStepLength(maxPDRStepLength)

        tjLabsSensorManager.getSensorDataResultOrNull(object : TJLabsSensorManager.SensorResultListener{
            override fun onSensorChangedResult(sensorData: SensorData) {
                val curTime = System.currentTimeMillis()
                val dtime = if (preTime != 0L) {curTime - preTime} else {null}
                when (userMode) {
                    UserMode.MODE_PEDESTRIAN -> generatePedestrianUvd(curTime, dtime,sensorData, callback)
                    UserMode.MODE_VEHICLE -> {}
                    UserMode.MODE_AUTO -> {}
                }

                saveDataFunction(application, isSaveData, fileName, "${curTime},${sensorData.toCollectString()}" + "\n")
                preTime = curTime
            }
        })
    }

    private fun resetVelocityAfterSeconds(velocity : Float, sec : Int = 2) : Float {
        return if (System.currentTimeMillis() - uvdGenerationTimeMillis < sec * 1000) {
            velocity
        } else {
            0f
        }
    }

    private fun generatePedestrianUvd(time : Long, dtime : Long?, sensorData: SensorData, callback: UVDCallback) {
        val pdrUnit = tjLabsPdrDistanceEstimator.estimateDistanceInfo(time, sensorData)
        val attDegree = tjLabsAttitudeEstimator.estimateAttitudeRadian(dtime, sensorData).toDegree()
        val isLookingStatus = tjLabsUnitStatusEstimator.estimateStatus(attDegree, pdrUnit.isIndexChanged)
        if (pdrUnit.isIndexChanged) {
            val index = pdrUnit.index
            val length = pdrUnit.length
            val heading = attDegree.yaw
            callback.onUvdResult(UserMode.MODE_PEDESTRIAN, UserVelocity(userId, time, index, length, heading, isLookingStatus))
            uvdGenerationTimeMillis = time
        } else {
            callback.onUvdPauseMillis(time - uvdGenerationTimeMillis)
        }
        callback.onPressureResult(sensorData.pressure[0])
        callback.onVelocityResult(resetVelocityAfterSeconds(pdrUnit.velocity))
    }


    private fun generateAutoUvd() {
        TODO()
    }

    fun stopUvdGeneration() {
        tjLabsSensorManager.stopSensorChanged()
        tjLabsPdrDistanceEstimator = TJLabsPDRDistanceEstimator()
        tjLabsAttitudeEstimator = TJLabsAttitudeEstimator(sensorFrequency)
        tjLabsUnitStatusEstimator = TJLabsUnitStatusEstimator()
        uvdGenerationTimeMillis = 0L
        drVelocityScale = 1f
    }
}
