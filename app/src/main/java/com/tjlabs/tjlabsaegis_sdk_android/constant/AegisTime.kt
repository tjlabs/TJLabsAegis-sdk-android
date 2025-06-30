package com.tjlabs.tjlabsaegis_sdk_android.constant

internal object AegisTime {
    const val SECOND_TO_MILLIS = 1000
    const val OUTPUT_INTERVAL: Float = 0.5f * SECOND_TO_MILLIS// second
    const val RFD_INTERVAL = 500L // millis
    const val BLE_SCAN_WINDOW_TIME_MILLIS = 5000L // millis
    const val TIME_INIT_THRESHOLD: Float = 25f * SECOND_TO_MILLIS// seconds
    const val TIME_INIT: Float = TIME_INIT_THRESHOLD + SECOND_TO_MILLIS
}