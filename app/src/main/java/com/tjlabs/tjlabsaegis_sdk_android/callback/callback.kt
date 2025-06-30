package com.tjlabs.tjlabsaegis_sdk_android.callback

interface AegisCallback {
    fun onJAegisSuccess(isSuccess: Boolean, msg: String)
    fun onJAegisError(isFail: Boolean, msg: String)
    fun onJAegisResult(rssiScore : Float, stepScore : Float) // TBD

}
