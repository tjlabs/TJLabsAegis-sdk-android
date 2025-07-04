package com.tjlabs.tjlabsaegis_sdk_android.rfd

data class ReceivedForce(
    val tenant_user_name: String = "",
    val mobile_time: Long = 0L,
    val rfs: Map<String, Float> = mutableMapOf("temp" to -100f),
    val pressure: Float = 0f
)

internal data class BLEScanInfo(
    val id: String = "",
    val rssi: Int = -100,
    val timestampNanos: Long = 0L,
)


internal data class RSSIClass(
    val count: Int,
    val total: Int
) {
    fun getAverage(): Int {
        return total / count
    }

    fun getCountString(): String {
        return "$count"
    }
}

internal enum class ScanMode{
    NO_FILTER_SCAN, ONLY_WARD_SCAN, ONLY_SEI_SCAN, WARD_SEI_SCAN
}

internal object RFDErrorCode {
    //RFD Error 1XX
    //BLE Hardware
    const val BLUETOOTH_DISABLED = 100
    const val BLUETOOTH_NOT_SUPPORTED = 101
    const val AIRPLANE_MODE_ACTIVATION = 102

    //BLE Permission
    const val PERMISSION_DENIED = 110
    const val PERMISSION_STATE_CHANGED = 111

    //BLE Scan Result
    const val SCAN_TIMEOUT = 120
    const val INVALID_DEVICE_NAME = 121
    const val INVALID_RSSI = 122

    //RFD Generation Service
    const val DUPLICATE_SCAN_START = 130
}