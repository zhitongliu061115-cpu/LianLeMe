package com.example.helloapp.ui.training

import android.content.Context
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayDeque
import org.tensorflow.lite.Interpreter
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class PoseActionAnalyzer(context: Context) {
    private val windowSize = 30
    private val numFeatures = 33 * 3
    private val poseDataBuffer = ArrayDeque<FloatArray>()
    private var tfliteInterpreter: Interpreter? = null

    // 增加冷却机制变量
    private var cooldownCounter = 0
    private val COOLDOWN_FRAMES = 5 // 根据帧率调整，假设识别成功后忽略接下来 8 帧
    private val CONFIDENCE_THRESHOLD = 0.85f // 置信度必须大于 80%


    init {
        try {
            val assetFileDescriptor = context.assets.openFd("lstm_v2.tflite")
            val fileInputStream = java.io.FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val model = fileChannel.map(
                java.nio.channels.FileChannel.MapMode.READ_ONLY,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.declaredLength
            )
            tfliteInterpreter = Interpreter(model)

            // 👇 新增：让模型“自报家门”
            tfliteInterpreter?.let {
                val inputTensor = it.getInputTensor(0)
                val outputTensor = it.getOutputTensor(0)
                Log.d("AI_Coach", " 模型加载成功！")
                Log.d("AI_Coach", " 期望的输入 Shape: ${inputTensor.shape().contentToString()}, 类型: ${inputTensor.dataType()}")
                Log.d("AI_Coach", " 期望的输出 Shape: ${outputTensor.shape().contentToString()}, 类型: ${outputTensor.dataType()}")
            }

        } catch (e: Exception) {
            Log.e("AI_Coach", " 模型加载彻底失败！原因：", e)
        }
    }



    fun processLandmarks(landmarks: List<NormalizedLandmark>): Int {
        // 1. 基础防崩检查
        if (landmarks.size < 33) return -1

        // 2. 核心点位可见度检查 (重点！)
        // 检查肩膀 (11, 12) 和 胯部 (23, 24) 是否清晰可见
        val essentialIndices = listOf(11, 12, 23, 24)
        val isVisible = essentialIndices.all { landmarks[it].visibility().orElse(0f) > 0.5f }

        if (!isVisible) {
            // 如果看不全核心部位，清空旧数据，防止“断层”动作干扰下次识别
            poseDataBuffer.clear()
            return -1
        }

        // 移除 1.0f - 的翻转，直接读取原始坐标！
        val mirroredX = FloatArray(33) { landmarks[it].x() }
        val origY = FloatArray(33) { landmarks[it].y() }
        val origZ = FloatArray(33) { landmarks[it].z() }

        // 2. 计算身体中心 (髋部中心: 23 和 24) —— 后面的代码用 fakeX 和 fakeY 算
        val cx = (mirroredX[23] + mirroredX[24]) / 2f
        val cy = (origY[23] + origY[24]) / 2f
        val cz = (origZ[23] + origZ[24]) / 2f

        // 3. 计算肩膀中心 (11 和 12)
        val sx = (mirroredX[11] + mirroredX[12]) / 2f
        val sy = (origY[11] + origY[12]) / 2f
        val sz = (origZ[11] + origZ[12]) / 2f

        // 4. 计算躯干长度作为缩放比例
        var scale = Math.sqrt(
            ((cx - sx) * (cx - sx) +
                    (cy - sy) * (cy - sy) +
                    (cz - sz) * (cz - sz)).toDouble()
        ).toFloat()
        if (scale < 0.01f) scale = 0.01f

        // 5. 归一化并装入特征数组
        val features = FloatArray(33 * 3)
        for (i in 0 until 33) {
            features[i * 3] = (mirroredX[i] - cx) / scale
            features[i * 3 + 1] = (origY[i] - cy) / scale
            features[i * 3 + 2] = (origZ[i] - cz) / scale
        }

        // 4. 滑动窗口维护
        poseDataBuffer.addLast(features)
        if (poseDataBuffer.size > windowSize) {
            poseDataBuffer.removeFirst()
        }

        // 2. 状态 B：冷却中或缓冲区不满
        if (cooldownCounter > 0) {
            cooldownCounter--
            return -3 // 返回 -3，代表“正在工作，但还没到下一次判定”,不做处理
        }

        if (poseDataBuffer.size < windowSize) {
            return -2 // 缓冲区未满，也在工作中
        }

        // 3. 状态 C：成功判定
        return runInference()
    }

    private fun runInference(): Int {
        // 1. 准备模型的输入 ByteBuffer
        // 容量计算：1(批次) * 30(帧) * 99(特征) * 4(Float占4字节)
        val inputBuffer = ByteBuffer.allocateDirect(1 * windowSize * numFeatures * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        // 2. 将过去 30 帧的数据连续写入 ByteBuffer
        for (features in poseDataBuffer) {
            for (value in features) {
                inputBuffer.putFloat(value)
            }
        }
        // 重置指针到开头，极其重要，否则模型读不到数据！
        inputBuffer.rewind()

        // 3. 准备模型的输出数组 (1 行，10 列)
        val output = Array(1) { FloatArray(10) }

        // 4. 运行模型
        tfliteInterpreter?.let {
            it.run(inputBuffer, output)
        }

        // 5. 解析结果
        val results = output[0]
        val maxIndex = results.indices.maxByOrNull { results[it] } ?: -1

        Log.d("AI_Coach", "10个动作概率: [${results.joinToString(", ") { String.format("%.2f", it) }}] ===> 最终判定: $maxIndex")

        if (maxIndex != -1 && results[maxIndex] > CONFIDENCE_THRESHOLD) {
            cooldownCounter = COOLDOWN_FRAMES
            return maxIndex
        }
        return -4  //未知动作
    }

    fun close() {
        tfliteInterpreter?.close()
        tfliteInterpreter = null
        poseDataBuffer.clear() // 顺便清空缓存队列
    }

    fun clearBuffer() {
        poseDataBuffer.clear()
        cooldownCounter = 0
    }

    fun getBufferSize() = poseDataBuffer.size
}