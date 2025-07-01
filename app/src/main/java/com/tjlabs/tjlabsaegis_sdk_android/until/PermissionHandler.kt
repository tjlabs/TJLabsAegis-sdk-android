package com.tjlabs.tjlabsaegis_sdk_android.until

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

internal class PermissionHandler(
    private val activity: Activity,
    private val permissions: Array<String>,
    private val onAllGranted: () -> Unit
) {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    fun checkAndRequestPermissions() {
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        logPermissionStatus(activity, permissions)

        if (notGranted.isEmpty()) {
            onAllGranted()
        } else {
            ActivityCompat.requestPermissions(activity, notGranted.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                onAllGranted()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }

    private fun logPermissionStatus(activity: Activity, permissions: Array<String>) {
        permissions.forEach { permission ->
            val granted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
            Log.d("PermissionCheck", "$permission : ${if (granted) "GRANTED" else "DENIED"}")
        }
    }
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(activity)
            .setTitle("권한이 필요합니다")
            .setMessage("앱을 사용하려면 필수 권한을 허용해주세요. 설정 화면으로 이동하시겠습니까?")
            .setCancelable(false)
            .setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
            }
            .setNegativeButton("앱 종료") { _, _ ->
                activity.finishAffinity()
            }
            .show()
    }


}