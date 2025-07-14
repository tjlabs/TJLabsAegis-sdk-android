package com.tjlabs.tjlabsaegis_sdk_android.callback

interface AegisCallback {
    fun onAegisSuccess(isSuccess: Boolean, msg: String)
    fun onAegisResult(rssiScore : Float, stepScore : Float) // TBD

}
