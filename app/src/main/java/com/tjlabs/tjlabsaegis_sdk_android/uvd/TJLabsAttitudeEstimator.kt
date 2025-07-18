package com.tjlabs.tjlabsaegis_sdk_android.uvd

import com.tjlabs.tjlabsaegis_sdk_android.utils.TJLabsUtilFunctions.calAngleOfRotation
import com.tjlabs.tjlabsaegis_sdk_android.utils.TJLabsUtilFunctions.calAttEMA
import com.tjlabs.tjlabsaegis_sdk_android.utils.TJLabsUtilFunctions.calAttitudeUsingGameVector
import com.tjlabs.tjlabsaegis_sdk_android.utils.TJLabsUtilFunctions.calPitchUsingAcc
import com.tjlabs.tjlabsaegis_sdk_android.utils.TJLabsUtilFunctions.calRollUsingAcc
import com.tjlabs.tjlabsaegis_sdk_android.utils.TJLabsUtilFunctions.transBody2Nav


internal class TJLabsAttitudeEstimator(private val frequency : Int) {
    private var headingGyroGame: Float = 0f
    private var headingGyroAcc: Float = 0f
    private var preGameVecAttEMA = Attitude(0f, 0f, 0f)
    private var preAccAttEMA = Attitude(0f, 0f, 0f)
    private var preAngleOfRotation = 0f
    private var preAccAngleOfRotation = 0f
    private val avgAttitudeWindow = (frequency / 2)

    fun estimateAttitudeRadian(dtime : Long?, sensorData: SensorData): Attitude {
        val acc = sensorData.acc
        val gyro = sensorData.gyro
        val gameVector = sensorData.gameVector

        var gameVecAttitude = calAttitudeUsingGameVector(gameVector)
        val accRoll: Float
        val accPitch: Float
        if (acc[0] == 0f || acc[1] == 0f || acc[2] == 0f) {
            accRoll = preAccAttEMA.roll
            accPitch = preAccAttEMA.pitch
        } else {
            accRoll = calRollUsingAcc(acc)
            accPitch = calPitchUsingAcc(acc)
        }

        val accAttitude = Attitude(accRoll, accPitch, 0f)

        val gameVecAttEMA: Attitude
        var accAttEMA = Attitude(accRoll, accPitch, 0f)

        if (accAttEMA.isNan()){
            accAttEMA = preAccAttEMA
        }
        if (gameVecAttitude.isNan()){
            gameVecAttitude = preGameVecAttEMA
        }

        if (preGameVecAttEMA == Attitude(0f, 0f, 0f)) {
            gameVecAttEMA = gameVecAttitude
            accAttEMA = accAttitude
        } else {
            gameVecAttEMA = calAttEMA(preGameVecAttEMA, gameVecAttitude, avgAttitudeWindow)
            accAttEMA = calAttEMA(preAccAttEMA, accAttEMA, avgAttitudeWindow)
        }

        val gyroNavGame = transBody2Nav(gameVecAttEMA, gyro)
        val gyroNavEMAAcc = transBody2Nav(accAttEMA, gyro)

        // timeBefore 이 null 이면 초기화, 아니면 회전값 누적
        if (dtime != null) {
            var angleOfRotation = calAngleOfRotation(dtime, gyroNavGame[2])
            var accAngleOfRotation = calAngleOfRotation(dtime, gyroNavEMAAcc[2])

            if (!angleOfRotation.isNaN() || !accAngleOfRotation.isNaN()){
                preAngleOfRotation = angleOfRotation
                preAccAngleOfRotation = accAngleOfRotation
            }else{
                angleOfRotation = preAngleOfRotation
                accAngleOfRotation = preAccAngleOfRotation
            }
            headingGyroGame += angleOfRotation
            headingGyroAcc += accAngleOfRotation
        } else {
            headingGyroGame = gyroNavGame[2] * (1 / frequency)
            headingGyroAcc = gyroNavEMAAcc[2] * (1 / frequency)
        }

        // 누적된 회전값으로 현재 attitude 계산
        val curAttitude = Attitude(gameVecAttEMA.roll, gameVecAttEMA.pitch, headingGyroGame)

        if (curAttitude.yaw.isNaN()){
            curAttitude.yaw = preGameVecAttEMA.yaw
        }

        if (!gameVecAttitude.isNan()){
            preGameVecAttEMA = gameVecAttEMA
        }
        if (!accAttEMA.isNan()){
            preAccAttEMA = accAttEMA
        }

        return curAttitude
    }
}