package com.tjlabs.tjlabsaegis_sdk_android

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tjlabs.tjlabsaegis_sdk_android.callback.AegisCallback
import com.tjlabs.tjlabsaegis_sdk_android.databinding.ActivityMainBinding
import com.tjlabs.tjlabsaegis_sdk_android.manager.AegisManager
import com.tjlabs.tjlabsaegis_sdk_android.until.PermissionHandler

const val TAG = "TJLabsAegisSDK"

enum class Alert(){
    NORMAL, WARNING, EMERGENCY
}

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private var stepSetting = 0f
    private var rssiSetting = 0
    private val requiredPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        }

    private lateinit var permissionHandler: PermissionHandler
    private lateinit var aegisManager : AegisManager

    private var currentAlertMsg = Alert.NORMAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionHandler = PermissionHandler(
            activity = this,
            permissions = requiredPermissions,
            onAllGranted = {
                startMainFlow()
            }
        )
        permissionHandler.checkAndRequestPermissions()
    }

    private fun startMainFlow() {
        aegisManager = AegisManager(application)
        val tenantId = "nstory"
        val tenantPw = "Nstory1234!"

        binding.btnStart.setOnClickListener {
            aegisManager.startAegis(tenantID = tenantId, tenantPw = tenantPw, callback = object : AegisCallback{
                override fun onAegisSuccess(isSuccess: Boolean, msg: String) {
                    Toast.makeText(applicationContext, "Start Service", Toast.LENGTH_SHORT).show()
                    Log.d(TAG,"aegis start $isSuccess // msg : $msg")
                }

                override fun onAegisResult(rssiScore: Float, stepScore: Float) {
                    runOnUiThread {
                        binding.txtRssiScore.text = rssiScore.toString()
                        binding.txtStepScore.text = stepScore.toString()

                        val checkAlert = checkAlert(rssiScore, rssiSetting, stepScore, stepSetting)
                        Log.d(TAG,"checkAlert : $checkAlert // currentAlertMsg : $currentAlertMsg")
                        if (currentAlertMsg == Alert.EMERGENCY) {
                            currentAlertMsg = Alert.EMERGENCY
                            binding.btnReset.isEnabled = true
                        } else {
                            currentAlertMsg = checkAlert
                        }

                        binding.txtResult.text = currentAlertMsg.toString()
                    }
                }
            })
        }

        binding.btnStop.setOnClickListener {
            aegisManager.stopAegis()
        }

        binding.btnFind.setOnClickListener {
            val nearestBWard = aegisManager.findNearestBWard()
            binding.txtFindBWard.text = nearestBWard.first
            binding.txtFindBWardRssi.text = nearestBWard.second.toString()
        }

        binding.btnSetBwardId.setOnClickListener {
            aegisManager.setNearestBWardID(binding.editSetBWardId.text.toString()) { success, message ->
                if (success) {
                    Log.d("SetBWard", "Success: $message")
                } else {
                    Log.e("SetBWard", "Error: $message")
                }
            }
        }

        aegisManager.setStepWindowSec(binding.barStepSetting.progress)
        binding.barTimeSetting.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.txtTimeSetting.text = "시간 설정: ${progress}초"
                aegisManager.setStepWindowSec(progress)

            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        stepSetting = binding.barStepSetting.progress / 10f
        binding.barStepSetting.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.txtStepSetting.text = "걸음 기준 설정: ${progress / 10f}"
                stepSetting = progress / 10f
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        rssiSetting = binding.barRssiSetting.progress
        binding.barRssiSetting.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.txtRssiSetting.text = "신호 기준 설정: ${progress}"
                rssiSetting = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnReset.setOnClickListener {
            currentAlertMsg = Alert.NORMAL
            binding.btnReset.isEnabled = false
            binding.txtResult.text = currentAlertMsg.toString()
        }
    }

    private fun checkAlert(rssiScore: Float, rssiSetting: Int, stepScore: Float, stepSetting: Float) : Alert {
        return if (rssiScore < rssiSetting && stepScore < stepSetting) Alert.NORMAL
        else if (rssiScore >= rssiSetting && stepScore < stepSetting) Alert.WARNING
        else if (rssiScore < rssiSetting && stepScore >= stepSetting) Alert.WARNING
        else Alert.EMERGENCY
    }

}