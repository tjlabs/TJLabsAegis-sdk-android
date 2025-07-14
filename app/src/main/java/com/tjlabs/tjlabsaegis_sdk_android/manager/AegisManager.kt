package com.tjlabs.tjlabsaegis_sdk_android.manager

import android.app.Application
import android.util.Log
import com.tjlabs.tjlabsaegis_sdk_android.TAG
import com.tjlabs.tjlabsaegis_sdk_android.callback.AegisCallback
import com.tjlabs.tjlabsaegis_sdk_android.constant.AegisTime
import com.tjlabs.tjlabsaegis_sdk_android.constant.AegisTime.BLE_SCAN_WINDOW_TIME_MILLIS
import com.tjlabs.tjlabsaegis_sdk_android.constant.AegisTime.RFD_INTERVAL
import com.tjlabs.tjlabsaegis_sdk_android.rfd.RFDGenerator
import com.tjlabs.tjlabsaegis_sdk_android.rfd.ReceivedForce
import com.tjlabs.tjlabsaegis_sdk_android.uvd.UVDGenerator
import com.tjlabs.tjlabsaegis_sdk_android.uvd.UserMode
import com.tjlabs.tjlabsaegis_sdk_android.uvd.UserVelocity
import com.tjlabs.tjlabsauth_sdk_android.TJLabsAuthManager
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs

class AegisManager(private val application: Application) : RFDGenerator.RFDCallback, UVDGenerator.UVDCallback {
    private val id = "temp_id"
    private val mode = UserMode.MODE_PEDESTRIAN
    private lateinit var rfdGenerator : RFDGenerator
    private lateinit var uvdGenerator: UVDGenerator

    private lateinit var updateTimer: Timer
    private var isStartTimer = false

    private var pressure = 0f
    private var isStartService = false

    private var currentRfd = ReceivedForce()
    private var nearestBWardId = ""
    private var calibrationBWardRSSI = -100f

    private val stepTimestamps = mutableListOf<Long>()
    private var stepWindowSec = 60
    private var stepScore = 0f
    private var rssiScore = 0f

    inner class UpdateTimer(private val callback: AegisCallback) : TimerTask() {
        override fun run() {
            // 시간 창 내 걸음만 유지
            val currentTime = System.currentTimeMillis()
            val cutoff = currentTime - stepWindowSec * 1000
            stepTimestamps.removeAll { it < cutoff }

            val stepsInWindow = stepTimestamps.size
            val stepsPerSec = stepsInWindow.toFloat() / stepWindowSec
            stepScore = stepsPerSec
            callback.onAegisResult(rssiScore, stepScore)
        }
    }

    fun startAegis(tenantID : String, tenantPw : String, callback: AegisCallback) {
        if (isStartService) {
            callback.onAegisSuccess(false, "The service is already running.")
            return
        }
        TJLabsAuthManager.setServerURL(serverType = "guardians")
        TJLabsAuthManager.initialize(application)
        TJLabsAuthManager.auth(tenantID, tenantPw) {
            code, isSuccess ->
            if (isSuccess) {
                //시작 하면 rfd, uvd 생성 시작
                rfdGenerator = RFDGenerator(application, id)
                uvdGenerator = UVDGenerator(application, id)
                rfdGenerator.checkIsAvailableRfd(this) {
                        isRfdSuccess, rfdMsg ->
                    if (isRfdSuccess) {
                        uvdGenerator.checkIsAvailableUvd(this){
                                isUvdSuccess, uvdMsg ->
                            if (isUvdSuccess) {
                                uvdGenerator.setUserMode(mode)
                                rfdGenerator.generateRfd(RFD_INTERVAL, BLE_SCAN_WINDOW_TIME_MILLIS, -100,
                                    0, getPressure = { pressure }, isSaveData = false, "aos_ble",this)
                                uvdGenerator.generateUvd(maxPDRStepLength = 0.7f, isSaveData = false, fileName = "aos_sensor", callback = this)
                                isStartService = true
                                startTimer(callback)
                                callback.onAegisSuccess(true, "start Aegis success")
                            } else{
                                callback.onAegisSuccess(false, uvdMsg)
                            }
                        }
                    } else {
                        callback.onAegisSuccess( false, rfdMsg)
                    }
                }
            } else {
                callback.onAegisSuccess(false, "unauthorized, code : $code")
            }
        }
    }

    fun stopAegis() {
        stopTimer()
        if (isStartService) {
            rfdGenerator.stopRfdGeneration()
            uvdGenerator.stopUvdGeneration()
            isStartService = false
        }
    }

    private fun startTimer(callback: AegisCallback){
        if (!::updateTimer.isInitialized){
            updateTimer = Timer()
        }else {
            stopTimer()
            updateTimer = Timer()
        }
        isStartTimer = true
        updateTimer.schedule(UpdateTimer(callback), 0, AegisTime.OUTPUT_INTERVAL.toLong())
    }

    private fun stopTimer(){
        if (::updateTimer.isInitialized){
            updateTimer.cancel()
        }

        isStartTimer = false
    }

    fun findNearestBWard(): Pair<String, Float> {
        nearestBWardId = currentRfd.rfs.maxByOrNull { it.value }?.key ?: ""
        calibrationBWardRSSI  = currentRfd.rfs.maxByOrNull { it.value }?.value ?: -100f
        return nearestBWardId to calibrationBWardRSSI
    }

    fun setNearestBWardID(bWardId: String, completion: (Boolean, String) -> Unit) {
        val rssi = currentRfd.rfs.entries.find { it.key == bWardId }?.value

        return if (rssi != null) {
            nearestBWardId = bWardId
            calibrationBWardRSSI = rssi
            completion(true, "Successfully set B-Ward ID: $bWardId with RSSI: $rssi")
        } else if (!isStartService) {
            completion(false, "Please start aegis service first")
        }
        else {
            completion(false, "Failed to set B-Ward ID: $bWardId. ID not found in current RFD data.")
        }
    }

    fun setStepWindowSec(sec : Int) {
        stepWindowSec = sec
    }

    override fun onRfdResult(rfd: ReceivedForce) {
        currentRfd = rfd
        val filteredRssi = rfd.rfs.entries.find { it.key == nearestBWardId }?.value ?: -100f
        rssiScore = abs(calibrationBWardRSSI - filteredRssi)
    }

    override fun onRfdError(code: Int, msg: String) {
        Log.d(TAG, "code : $code // msg : $msg")

    }

    override fun onRfdEmptyMillis(time: Long) {
        
    }

    override fun onUvdResult(mode: UserMode, uvd: UserVelocity) {
        val currentTime = System.currentTimeMillis()

        // 새 걸음 등록
        stepTimestamps.add(currentTime)
    }

    override fun onPressureResult(hPa: Float) {
        pressure = hPa
    }

    override fun onVelocityResult(kmPh: Float) {
        
    }

    override fun onMagNormSmoothingVarResult(value: Float) {
        
    }

    override fun onUvdPauseMillis(time: Long) {
        
    }

    override fun onUvdError(error: String) {
        
    }

}