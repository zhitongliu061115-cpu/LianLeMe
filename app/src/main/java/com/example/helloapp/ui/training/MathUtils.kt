package com.example.helloapp.ui.training

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.atan2

object MathUtils {
    /**
     * 计算三个关节点形成的夹角 (例如：髋、膝、踝)
     * @return 返回角度，范围 0~180度
     */
    fun calculateAngle(
        first: NormalizedLandmark,
        middle: NormalizedLandmark,
        last: NormalizedLandmark
    ): Double {
        val radians = atan2((last.y() - middle.y()).toDouble(), (last.x() - middle.x()).toDouble()) -
                atan2((first.y() - middle.y()).toDouble(), (first.x() - middle.x()).toDouble())
        var angle = abs(radians * 180.0 / Math.PI)
        if (angle > 180.0) {
            angle = 360.0 - angle
        }
        return angle
    }
}