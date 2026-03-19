package com.example.helloapp.ui.training.counters

import com.example.helloapp.ui.training.MathUtils
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.hypot

// 统一返回结果：是否完成了一次、当前的纠错反馈
data class CounterResult(
    val isRepCompleted: Boolean,
    val formFeedback: String? = null,
    val clearRealTimeFeedback: Boolean = false
)

interface ActionCounter {
    fun analyzeFrame(landmarks: List<NormalizedLandmark>): CounterResult
    fun reset()

}

// 深蹲计数器与纠错实现
class SquatCounter : ActionCounter {
    private var isDown = false
    private var minAngle = 180.0

    // 关键：记录站立时的初始脚宽，防止下蹲时脚踝坐标漂移
    private var initialAnkleDist = 0f

    private var kneesCavingFrames = 0
    private val ERROR_FRAMES_THRESHOLD = 2
    private var hasKneeErrorThisRep = false

    override fun analyzeFrame(landmarks: List<NormalizedLandmark>): CounterResult {
        val hipL = landmarks[23]; val kneeL = landmarks[25]; val ankleL = landmarks[27]
        val hipR = landmarks[24]; val kneeR = landmarks[26]; val ankleR = landmarks[28]

        // 基础角度计算
        val angleL = MathUtils.calculateAngle(hipL, kneeL, ankleL)
        val angleR = MathUtils.calculateAngle(hipR, kneeR, ankleR)
        val avgAngle = (angleL + angleR) / 2.0

        var feedback: String? = null
        var shouldClear = false

        // 1. 动态获取当前的物理距离
        val currentKneeDist = Math.abs(kneeL.x() - kneeR.x())
        val currentAnkleDist = Math.abs(ankleL.x() - ankleR.x())

        // 2. 站姿校准：当你直立时（角度>170），不断刷新并锁定你的标准脚宽
        if (avgAngle > 170.0) {
            if (ankleL.visibility().orElse(0f) > 0.5f && ankleR.visibility().orElse(0f) > 0.5f) {
                initialAnkleDist = currentAnkleDist
            }
        }

        // 3. 实时内扣检测
        if (avgAngle < 140.0) {
            // 使用锁定后的脚宽作为基准（如果站立时没抓到，就用当前帧）
            val referenceDist = if (initialAnkleDist > 0.05f) initialAnkleDist else currentAnkleDist

            // 💡 阈值调优：从 0.7f 提高到 0.9f
            // 正常的深蹲膝盖应该与脚齐平或略宽。如果膝盖距离 < 脚宽的 90%，就是明显内扣。
            if (currentKneeDist < referenceDist * 1.3f) {
                kneesCavingFrames++
                if (kneesCavingFrames >= ERROR_FRAMES_THRESHOLD) {
                    feedback = "膝盖向外推，不要内扣"
                    hasKneeErrorThisRep = true // 锁定本回合错误
                }
            } else {
                if (kneesCavingFrames >= ERROR_FRAMES_THRESHOLD) shouldClear = true
                kneesCavingFrames = 0
            }
        }

        // 4. 状态机：计数与深度
        var repCompleted = false

        // 进入下蹲判定（120度就开始记录）
        if (avgAngle < 120.0) {
            isDown = true
            if (avgAngle < minAngle) {
                minAngle = avgAngle
            }
        }
        // 站起结算
        else if (avgAngle > 160.0 && isDown) {
            isDown = false
            repCompleted = true
            // 严厉的深度标准：80度
            if (!hasKneeErrorThisRep) {
                // 严厉的深度标准：80度
                if (minAngle > 80.0) {
                    feedback = "蹲得不够深"
                }
            }

            // 重置本回合状态
            minAngle = 180.0
            hasKneeErrorThisRep = false
        }

        return CounterResult(repCompleted, feedback, shouldClear)
    }

    override fun reset() {
        isDown = false
        minAngle = 180.0
        kneesCavingFrames = 0
        hasKneeErrorThisRep = false
        initialAnkleDist = 0f
    }
}

// 俯卧撑计数器与纠错实现
class PushUpCounter : ActionCounter {
    // 状态机：记录当前是否处于“撑下去”的状态
    private var isDown = false
    // 记录下压过程中的手肘最小角度（最深处）
    private var minElbowAngle = 180.0

    // 防抖计数器：用于判断是否塌腰或撅屁股
    private var coreSaggingFrames = 0
    private val ERROR_FRAMES_THRESHOLD = 2 // 连续5帧姿势不对才提示

    override fun analyzeFrame(landmarks: List<NormalizedLandmark>): CounterResult {
        // 俯卧撑核心看上半身和躯干
        // 左右肩膀 (11, 12)，左右手肘 (13, 14)，左右手腕 (15, 16)
        // 左右髋部 (23, 24)，左右脚踝 (27, 28)
        val shoulderL = landmarks[11]
        val elbowL = landmarks[13]
        val wristL = landmarks[15]
        val hipL = landmarks[23]
        val ankleL = landmarks[27]

        val shoulderR = landmarks[12]
        val elbowR = landmarks[14]
        val wristR = landmarks[16]
        val hipR = landmarks[24]
        val ankleR = landmarks[28]

        // 核心点可见度检查（只要有一侧可见度高即可，因为俯卧撑经常是侧面拍摄）
        val isLeftVisible = elbowL.visibility().orElse(0f) > 0.5f && hipL.visibility().orElse(0f) > 0.5f
        val isRightVisible = elbowR.visibility().orElse(0f) > 0.5f && hipR.visibility().orElse(0f) > 0.5f

        if (!isLeftVisible && !isRightVisible) {
            return CounterResult(false)
        }

        // 计算手臂角度（取可见度高的一侧，或者平均值）
        var elbowAngle = 180.0
        var bodyAngle = 180.0

        if (isLeftVisible && isRightVisible) {
            val angleL = MathUtils.calculateAngle(shoulderL, elbowL, wristL)
            val angleR = MathUtils.calculateAngle(shoulderR, elbowR, wristR)
            elbowAngle = (angleL + angleR) / 2.0

            val bodyAngleL = MathUtils.calculateAngle(shoulderL, hipL, ankleL)
            val bodyAngleR = MathUtils.calculateAngle(shoulderR, hipR, ankleR)
            bodyAngle = (bodyAngleL + bodyAngleR) / 2.0
        } else if (isLeftVisible) {
            elbowAngle = MathUtils.calculateAngle(shoulderL, elbowL, wristL)
            bodyAngle = MathUtils.calculateAngle(shoulderL, hipL, ankleL)
        } else {
            elbowAngle = MathUtils.calculateAngle(shoulderR, elbowR, wristR)
            bodyAngle = MathUtils.calculateAngle(shoulderR, hipR, ankleR)
        }

        var feedback: String? = null
        var shouldClear = false

        // --- 纠错 1：实时纠错 - 核心未收紧（塌腰或撅屁股） ---
        // 正常情况下，身体应该是一条直线，角度接近 180 度。
        // 如果角度小于 155 度，说明腰部弯曲太厉害了。
        if (bodyAngle < 155.0) {
            coreSaggingFrames++
            if (coreSaggingFrames >= ERROR_FRAMES_THRESHOLD) {
                feedback = "核心收紧，身体保持直线！"
            }
        } else {
            if (coreSaggingFrames >= ERROR_FRAMES_THRESHOLD) {
                shouldClear = true
            }
            coreSaggingFrames = 0 // 姿势恢复正常，清空防抖
        }

        // --- 状态机：计数与深度纠错 ---
        var repCompleted = false

        // 判断下压：手肘角度小于 90 度（可以放宽到 100 度以照顾新手）
        if (elbowAngle < 110.0) {
            isDown = true
            // 记录下压最低点的角度
            if (elbowAngle < minElbowAngle) {
                minElbowAngle = elbowAngle
            }
        }
        // 判断撑起：手肘角度大于 150 度，并且之前处于下压状态
        else if (elbowAngle > 150.0 && isDown) {
            isDown = false
            repCompleted = true

            // 纠错 2：事后纠错 - 判断下压深度
            // 如果最深处的手肘角度都没小于 85 度，说明做的是“半程俯卧撑”
            if (minElbowAngle > 85.0) {
                feedback = "下压不够深"
            }
            // 重置最低角度，准备抓取下一次
            minElbowAngle = 180.0
        }

        return CounterResult(repCompleted, feedback, shouldClear)
    }

    override fun reset() {
        isDown = false
        minElbowAngle = 180.0
        coreSaggingFrames = 0
    }
}

// 开合跳计数器与纠错实现
class JumpingJackCounter : ActionCounter {
    private var isOpened = false
    private var handsReachedTop = false
    private var maxSpreadRatio = 0.0f

    private var consecutiveBadArms = 0
    private var consecutiveBadLegs = 0
    private val ERROR_REPS_THRESHOLD = 2

    override fun analyzeFrame(landmarks: List<NormalizedLandmark>): CounterResult {
        val shoulderL = landmarks[11]
        val shoulderR = landmarks[12]
        val wristL = landmarks[15]
        val wristR = landmarks[16]
        val ankleL = landmarks[27]
        val ankleR = landmarks[28]
        val kneeL = landmarks[25]
        val kneeR = landmarks[26]

        // 1. 核心点检查：放宽条件，严禁死等脚踝！只要上半身在画面里就能算
        if (shoulderL.visibility().orElse(0f) < 0.4f) {
            return CounterResult(false)
        }

        // 2. 稳定的基准尺：使用 2D 欧氏距离计算肩宽，防止侧身导致 X 轴失真
        val shoulderWidth = hypot(
            (shoulderL.x() - shoulderR.x()).toDouble(),
            (shoulderL.y() - shoulderR.y()).toDouble()
        ).toFloat()

        if (shoulderWidth < 0.05f) return CounterResult(false)

        // 🌟 3. 核心修复：脚踝防丢机制 (Fallback)
        var spreadWidth = 0f
        if (ankleL.visibility().orElse(0f) > 0.5f && ankleR.visibility().orElse(0f) > 0.5f) {
            // 优先用脚踝真实宽度
            spreadWidth = abs(ankleL.x() - ankleR.x())
        } else if (kneeL.visibility().orElse(0f) > 0.5f && kneeR.visibility().orElse(0f) > 0.5f) {
            // 脚踝看不见或有拖影？用膝盖宽度代替！(膝盖张开的物理幅度约为脚踝的 80%，所以乘 1.25 补平)
            spreadWidth = abs(kneeL.x() - kneeR.x()) * 1.25f
        } else {
            // 腿全出画了，没法评判，跳过这帧
            return CounterResult(false)
        }

        val spreadRatio = spreadWidth / shoulderWidth

        // 4. 手臂状态检测
        val isHandsUp = wristL.y() < shoulderL.y() && wristR.y() < shoulderR.y()
        // 手腕低于肩膀即可视为手已放下，不用苛求贴紧大腿
        val isHandsDown = wristL.y() > shoulderL.y() && wristR.y() > shoulderR.y()

        if (spreadRatio > maxSpreadRatio) {
            maxSpreadRatio = spreadRatio
        }
        if (isHandsUp) {
            handsReachedTop = true
        }

        var repCompleted = false
        var feedback: String? = null
        var shouldClear = false

        // --- 5. 状态机：更符合人类真实跳跃的动作闭环 ---

        // 动作启动：只要腿明显张开了，或者手举起来了，就认为进入了“打开”状态
        if (spreadRatio > 1.15f || isHandsUp) {
            isOpened = true
        }
        // 🌟 修改点 2：闭合判定
        else if (isOpened && spreadRatio < 1.1f && isHandsDown) {
            isOpened = false
            repCompleted = true

            // --- 结算逻辑 ---

            // 1. 优先查手臂 (保持原样)
            if (!handsReachedTop) {
                consecutiveBadArms++
                if (consecutiveBadArms >= ERROR_REPS_THRESHOLD) {
                    feedback = "手臂请充分举过头顶！"
                }
            } else {
                consecutiveBadArms = 0
            }

            // 2. 查腿部
            // 🌟 修改点 3：提高标准。
            // 如果最大张开度连 1.45 倍肩宽都不到，说明张得太小了
            if (feedback == null) {
                if (maxSpreadRatio < 1.45f) {
                    consecutiveBadLegs++
                    // 测试阶段可以把 ERROR_REPS_THRESHOLD 改成 1，看能不能立刻触发
                    if (consecutiveBadLegs >= ERROR_REPS_THRESHOLD) {
                        feedback = "双腿可以再张开一点！"
                    }
                } else {
                    consecutiveBadLegs = 0
                }
            }

            // 重置状态
            handsReachedTop = false
            maxSpreadRatio = 0.0f
        }

        return CounterResult(repCompleted, feedback, shouldClear)
    }

    override fun reset() {
        isOpened = false
        handsReachedTop = false
        maxSpreadRatio = 0.0f
        consecutiveBadArms = 0
        consecutiveBadLegs = 0
    }
}

// 仰卧起坐计数器与纠错实现
class SitUpCounter : ActionCounter {
    // 状态机：记录当前是否处于“坐起”状态
    private var isUp = false
    // 记录起身过程中的最小髋部角度（最深处/最紧凑处）
    private var minHipAngle = 180.0

    // 1. 实时防抖计数器：用于检测腿部是否伸直借力
    private var straightLegFrames = 0
    private val ERROR_FRAMES_THRESHOLD = 2

    // 2. 事后容错计数器：用于检测连续起身幅度不够
    private var consecutiveBadRange = 0
    private val ERROR_REPS_THRESHOLD = 2

    override fun analyzeFrame(landmarks: List<NormalizedLandmark>): CounterResult {
        // 仰卧起坐通常是侧面拍摄，核心看：肩膀(11,12)、髋部(23,24)、膝盖(25,26)、脚踝(27,28)
        val shoulderL = landmarks[11]
        val hipL = landmarks[23]
        val kneeL = landmarks[25]
        val ankleL = landmarks[27]

        val shoulderR = landmarks[12]
        val hipR = landmarks[24]
        val kneeR = landmarks[26]
        val ankleR = landmarks[28]

        // 侧面拍摄容错：取可见度更高的一侧进行计算
        val isLeftVisible = hipL.visibility().orElse(0f) > 0.5f && kneeL.visibility().orElse(0f) > 0.5f
        val isRightVisible = hipR.visibility().orElse(0f) > 0.5f && kneeR.visibility().orElse(0f) > 0.5f

        if (!isLeftVisible && !isRightVisible) {
            return CounterResult(false)
        }

        var hipAngle = 180.0  // 躯干与大腿的夹角 (计数核心)
        var kneeAngle = 90.0  // 大腿与小腿的夹角 (纠错核心)

        if (isLeftVisible && isRightVisible) {
            // 如果都可见，取平均值（防止轻微的侧身导致单侧误差）
            val hipAngleL = MathUtils.calculateAngle(shoulderL, hipL, kneeL)
            val hipAngleR = MathUtils.calculateAngle(shoulderR, hipR, kneeR)
            hipAngle = (hipAngleL + hipAngleR) / 2.0

            val kneeAngleL = MathUtils.calculateAngle(hipL, kneeL, ankleL)
            val kneeAngleR = MathUtils.calculateAngle(hipR, kneeR, ankleR)
            kneeAngle = (kneeAngleL + kneeAngleR) / 2.0
        } else if (isLeftVisible) {
            hipAngle = MathUtils.calculateAngle(shoulderL, hipL, kneeL)
            kneeAngle = MathUtils.calculateAngle(hipL, kneeL, ankleL)
        } else {
            hipAngle = MathUtils.calculateAngle(shoulderR, hipR, kneeR)
            kneeAngle = MathUtils.calculateAngle(hipR, kneeR, ankleR)
        }

        var feedback: String? = null
        var shouldClear = false

        // --- 纠错 1：实时纠错 - 腿部伸直/脚离地 ---
        // 标准的仰卧起坐，膝盖应该是弯曲的（通常小于 110 度）
        // 如果膝盖角度变得很大（比如大于 120 度），说明腿伸直了
        if (kneeAngle > 160.0) {
            straightLegFrames++
            if (straightLegFrames >= ERROR_FRAMES_THRESHOLD) {
                feedback = "双脚踩实地面，膝盖保持弯曲！"
            }
        } else {
            // 用户意识到了，把腿弯回来了，立刻发送撤销红条的指令！
            if (straightLegFrames >= ERROR_FRAMES_THRESHOLD) {
                shouldClear = true
            }
            straightLegFrames = 0
        }

        // --- 状态机：计数与事后纠错 ---
        var repCompleted = false

        // 动态记录本次动作中最紧凑的角度（胸贴近大腿）
        if (hipAngle < minHipAngle) {
            minHipAngle = hipAngle
        }

        // 判定“坐起”：髋部角度小于 75 度（宽容阈值，保证新手也能触发）
        if (hipAngle < 100.0) {
            isUp = true
        }
        // 判定“躺下”：髋部角度大于 130 度（背部贴地），并且之前是坐起状态
        else if (hipAngle > 120.0 && isUp) {
            isUp = false
            repCompleted = true // 躺平的那一刻，算作完成了一次完整的仰卧起坐

            // 纠错 2：事后纠错 - 起身幅度不够
            // 虽然小于 75 度就能计数，但优秀的仰卧起坐应该压到 60 度以下。
            // 如果勉强只做到了 65~75 度之间，说明在“半程划水”
            if (minHipAngle > 60.0) {
                consecutiveBadRange++
                if (consecutiveBadRange >= ERROR_REPS_THRESHOLD) {
                    // 只有当没有实时错误提示时，才展示这个事后提示（避免文字打架）
                    if (feedback == null) {
                        feedback = "起身幅度不够，尽量让胸部贴近大腿！"
                    }
                    consecutiveBadRange = 0 // 报完错清零
                }
            } else {
                consecutiveBadRange = 0 // 只要做了一个标准的，历史记录一笔勾销
            }

            // 重置最低角度，为下一个动作做准备
            minHipAngle = 180.0
        }

        return CounterResult(repCompleted, feedback, shouldClear)
    }

    override fun reset() {
        isUp = false
        minHipAngle = 180.0
        straightLegFrames = 0
        consecutiveBadRange = 0
    }
}

// 卧推计数器与纠错实现
class BenchPressCounter : ActionCounter {
    private var isDown = false
    private var minElbowAngle = 180.0

    private var nonVerticalFrames = 0
    private val ERROR_FRAMES_THRESHOLD = 2

    private var consecutiveBadRange = 0
    private val ERROR_REPS_THRESHOLD = 2

    // 💡 新增：单回合错误锁。只要这一推发生了轨迹错误，结算时就跳过深度检查
    private var hasFormErrorThisRep = false

    override fun analyzeFrame(landmarks: List<NormalizedLandmark>): CounterResult {
        val shoulderL = landmarks[11]
        val elbowL = landmarks[13]
        val wristL = landmarks[15]

        val shoulderR = landmarks[12]
        val elbowR = landmarks[14]
        val wristR = landmarks[16]

        val isLeftVisible = elbowL.visibility().orElse(0f) > 0.5f && wristL.visibility().orElse(0f) > 0.5f
        val isRightVisible = elbowR.visibility().orElse(0f) > 0.5f && wristR.visibility().orElse(0f) > 0.5f

        if (!isLeftVisible && !isRightVisible) {
            return CounterResult(false)
        }

        var elbowAngle = 180.0
        var dx = 0f
        var dy = 1f

        if (isLeftVisible && isRightVisible) {
            val angleL = MathUtils.calculateAngle(shoulderL, elbowL, wristL)
            val angleR = MathUtils.calculateAngle(shoulderR, elbowR, wristR)
            elbowAngle = (angleL + angleR) / 2.0

            dx = (Math.abs(wristL.x() - elbowL.x()) + Math.abs(wristR.x() - elbowR.x())) / 2f
            dy = (Math.abs(wristL.y() - elbowL.y()) + Math.abs(wristR.y() - elbowR.y())) / 2f
        } else if (isLeftVisible) {
            elbowAngle = MathUtils.calculateAngle(shoulderL, elbowL, wristL)
            dx = Math.abs(wristL.x() - elbowL.x())
            dy = Math.abs(wristL.y() - elbowL.y())
        } else {
            elbowAngle = MathUtils.calculateAngle(shoulderR, elbowR, wristR)
            dx = Math.abs(wristR.x() - elbowR.x())
            dy = Math.abs(wristR.y() - elbowR.y())
        }

        if (dy < 0.001f) dy = 0.001f

        var feedback: String? = null
        var shouldClear = false

        // --- 纠错 1：实时纠错 - 小臂不垂直 (最高优先级) ---
        if (dx / dy > 1f) {
            nonVerticalFrames++
            if (nonVerticalFrames >= ERROR_FRAMES_THRESHOLD) {
                feedback = "小臂保持垂直，手腕不要过度偏移！"
                hasFormErrorThisRep = true // 🔒 触发报错，马上上锁！
            }
        } else {
            if (nonVerticalFrames >= ERROR_FRAMES_THRESHOLD) {
                shouldClear = true
            }
            nonVerticalFrames = 0
        }

        // --- 状态机：计数与事后纠错 (低优先级) ---
        var repCompleted = false

        if (elbowAngle < minElbowAngle) {
            minElbowAngle = elbowAngle
        }

        if (elbowAngle < 140.0) {
            isDown = true
        }
        else if (elbowAngle > 160.0 && isDown) {
            isDown = false
            repCompleted = true

            // 💡 核心逻辑：只有在这一个卧推完全没有发生轨迹错误的前提下，才检查深度！
            if (!hasFormErrorThisRep) {
                if (minElbowAngle > 90.0) {
                    consecutiveBadRange++
                    if (consecutiveBadRange >= ERROR_REPS_THRESHOLD) {
                        feedback = "下放幅度不够，杠铃需接近胸口！"
                        consecutiveBadRange = 0
                    }
                } else {
                    consecutiveBadRange = 0 // 姿势标准且深度够了，清空历史不良记录
                }
            }

            // 💡 重置最低角度和错误锁，迎接下一次推起
            minElbowAngle = 180.0
            hasFormErrorThisRep = false
        }

        return CounterResult(repCompleted, feedback, shouldClear)
    }

    override fun reset() {
        isDown = false
        minElbowAngle = 180.0
        nonVerticalFrames = 0
        consecutiveBadRange = 0
        hasFormErrorThisRep = false // 切动作时也要重置锁
    }
}

// 引体向上计数器与纠错实现
class PullUpCounter : ActionCounter {
    // 状态机：记录当前是否处于“拉起”状态
    private var isUp = false
    // 记录拉起过程中的最小手肘角度（评估拉起高度/收缩程度）
    private var minElbowAngle = 180.0

    // 1. 实时防抖计数器：用于检测严重蹬腿或折叠髋部借力（Kipping）
    private var kippingFrames = 0
    private val ERROR_FRAMES_THRESHOLD = 2

    // 2. 事后容错计数器：用于检测连续拉起幅度不够（半程引体）
    private var consecutiveBadRange = 0
    private val ERROR_REPS_THRESHOLD = 2
    private var hasFormErrorThisRep = false

    override fun analyzeFrame(landmarks: List<NormalizedLandmark>): CounterResult {
        // 引体向上需要看全身（特别是判断借力时）：肩膀(11,12)、手肘(13,14)、手腕(15,16)
        // 髋部(23,24)、膝盖(25,26)、脚踝(27,28)
        val shoulderL = landmarks[11]
        val elbowL = landmarks[13]
        val wristL = landmarks[15]
        val hipL = landmarks[23]
        val kneeL = landmarks[25]
        val ankleL = landmarks[27]

        val shoulderR = landmarks[12]
        val elbowR = landmarks[14]
        val wristR = landmarks[16]
        val hipR = landmarks[24]
        val kneeR = landmarks[26]
        val ankleR = landmarks[28]

        // 容错：不管是看背面、正面还是侧面，只要手臂和躯干清晰可见即可
        val isLeftVisible = elbowL.visibility().orElse(0f) > 0.5f && hipL.visibility().orElse(0f) > 0.5f
        val isRightVisible = elbowR.visibility().orElse(0f) > 0.5f && hipR.visibility().orElse(0f) > 0.5f

        if (!isLeftVisible && !isRightVisible) {
            return CounterResult(false)
        }

        var elbowAngle = 180.0
        var hipAngle = 180.0
        var kneeAngle = 180.0

        if (isLeftVisible && isRightVisible) {
            val elbowAngleL = MathUtils.calculateAngle(shoulderL, elbowL, wristL)
            val elbowAngleR = MathUtils.calculateAngle(shoulderR, elbowR, wristR)
            elbowAngle = (elbowAngleL + elbowAngleR) / 2.0

            val hipAngleL = MathUtils.calculateAngle(shoulderL, hipL, kneeL)
            val hipAngleR = MathUtils.calculateAngle(shoulderR, hipR, kneeR)
            hipAngle = (hipAngleL + hipAngleR) / 2.0

            val kneeAngleL = MathUtils.calculateAngle(hipL, kneeL, ankleL)
            val kneeAngleR = MathUtils.calculateAngle(hipR, kneeR, ankleR)
            kneeAngle = (kneeAngleL + kneeAngleR) / 2.0
        } else if (isLeftVisible) {
            elbowAngle = MathUtils.calculateAngle(shoulderL, elbowL, wristL)
            hipAngle = MathUtils.calculateAngle(shoulderL, hipL, kneeL)
            kneeAngle = MathUtils.calculateAngle(hipL, kneeL, ankleL)
        } else {
            elbowAngle = MathUtils.calculateAngle(shoulderR, elbowR, wristR)
            hipAngle = MathUtils.calculateAngle(shoulderR, hipR, kneeR)
            kneeAngle = MathUtils.calculateAngle(hipR, kneeR, ankleR)
        }

        var feedback: String? = null
        var shouldClear = false

        // --- 纠错 1：实时纠错 - 蹬腿借力 / 核心不稳 ---
        // 很多人做引体向上会把小腿向后交叉叠起，这时候膝盖角度大约在 120~140 度，这是正常的。
        // 但如果膝盖角度剧烈弯曲（< 110度），或者髋部剧烈折叠（< 140度），说明在疯狂甩腿借力。
        if (kneeAngle < 110.0 || hipAngle < 140.0) {
            kippingFrames++
            if (kippingFrames >= ERROR_FRAMES_THRESHOLD) {
                feedback = "核心收紧，尽量避免蹬腿和躯干借力！"
                hasFormErrorThisRep = true // 🔒 只要发生借力，本回合上锁！
            }
        } else {
            // 用户稳住了身体，立刻撤销红条！
            if (kippingFrames >= ERROR_FRAMES_THRESHOLD) {
                shouldClear = true
            }
            kippingFrames = 0
        }

        // --- 状态机：计数与事后纠错 ---
        var repCompleted = false

        // 动态记录拉起时的最紧凑手肘角度
        if (elbowAngle < minElbowAngle) {
            minElbowAngle = elbowAngle
        }

        // 判定“拉起”：手肘角度小于 90 度（宽容触发，因为很多人最后几口力气拉不全）
        if (elbowAngle < 60.0) {
            isUp = true
        }
        // 判定“放下”：手肘角度大于 150 度（手臂基本伸直），并且之前是拉起状态
        else if (elbowAngle > 160.0 && isUp) {
            isUp = false
            repCompleted = true // 完成一次引体向上

            // 纠错 2：事后纠错 - 拉起高度不够
            // 标准的引体向上（下巴过杠），手肘角度通常会被挤压到 70 度以下（背阔肌充分收缩）。
            // 如果极限只拉到了 75~90 度之间，说明是“半程引体”。
            if (!hasFormErrorThisRep) {
                if (minElbowAngle > 40.0) {
                    consecutiveBadRange++
                    if (consecutiveBadRange >= ERROR_REPS_THRESHOLD) {
                        feedback = "拉起高度不够，尝试让下巴过杠！"
                        consecutiveBadRange = 0
                    }
                } else {
                    consecutiveBadRange = 0 // 姿势标准且高度够了，清空不良记录
                }
            }

            // 💡 重置最低角度和错误锁，迎接下一次拉起
            minElbowAngle = 180.0
            hasFormErrorThisRep = false
        }

        return CounterResult(repCompleted, feedback, shouldClear)
    }

    override fun reset() {
        isUp = false
        minElbowAngle = 180.0
        kippingFrames = 0
        consecutiveBadRange = 0
    }
}

// 前平举计数器与纠错实现
class FrontRaiseCounter : ActionCounter {
    // 状态机：记录当前是否处于“举起”状态
    private var isUp = false
    // 记录一次动作中的最大抬臂角度（评估举起高度）
    private var maxRaiseAngle = 0.0

    // 1. 实时防抖计数器：用于检测手臂过度弯曲
    private var bentArmFrames = 0
    // 2. 实时防抖计数器：用于检测身体后仰/晃动借力
    private var bodySwayFrames = 0
    private val ERROR_FRAMES_THRESHOLD = 2

    // 3. 事后容错计数器：用于检测连续举起高度不够
    private var consecutiveBadRange = 0
    private val ERROR_REPS_THRESHOLD = 2

    override fun analyzeFrame(landmarks: List<NormalizedLandmark>): CounterResult {
        // 前平举主要看侧面或斜侧面：肩膀(11,12)、手肘(13,14)、手腕(15,16)
        // 躯干：髋部(23,24)、脚踝(27,28)
        val shoulderL = landmarks[11]
        val elbowL = landmarks[13]
        val wristL = landmarks[15]
        val hipL = landmarks[23]
        val ankleL = landmarks[27]

        val shoulderR = landmarks[12]
        val elbowR = landmarks[14]
        val wristR = landmarks[16]
        val hipR = landmarks[24]
        val ankleR = landmarks[28]

        // 视角的容错：取可见度高的一侧（前平举通常是侧面拍摄最佳）
        val isLeftVisible = wristL.visibility().orElse(0f) > 0.5f && hipL.visibility().orElse(0f) > 0.5f
        val isRightVisible = wristR.visibility().orElse(0f) > 0.5f && hipR.visibility().orElse(0f) > 0.5f

        if (!isLeftVisible && !isRightVisible) {
            return CounterResult(false)
        }

        var elbowAngle = 180.0     // 手肘弯曲度
        var raiseAngle = 0.0       // 抬臂高度 (髋部-肩膀-手腕的夹角)
        var bodyAngle = 180.0      // 身体直立度 (肩膀-髋部-脚踝的夹角)

        if (isLeftVisible && isRightVisible) {
            // 如果都可见，取平均值
            elbowAngle = (MathUtils.calculateAngle(shoulderL, elbowL, wristL) +
                    MathUtils.calculateAngle(shoulderR, elbowR, wristR)) / 2.0
            raiseAngle = (MathUtils.calculateAngle(hipL, shoulderL, wristL) +
                    MathUtils.calculateAngle(hipR, shoulderR, wristR)) / 2.0
            bodyAngle = (MathUtils.calculateAngle(shoulderL, hipL, ankleL) +
                    MathUtils.calculateAngle(shoulderR, hipR, ankleR)) / 2.0
        } else if (isLeftVisible) {
            elbowAngle = MathUtils.calculateAngle(shoulderL, elbowL, wristL)
            raiseAngle = MathUtils.calculateAngle(hipL, shoulderL, wristL)
            bodyAngle = MathUtils.calculateAngle(shoulderL, hipL, ankleL)
        } else {
            elbowAngle = MathUtils.calculateAngle(shoulderR, elbowR, wristR)
            raiseAngle = MathUtils.calculateAngle(hipR, shoulderR, wristR)
            bodyAngle = MathUtils.calculateAngle(shoulderR, hipR, ankleR)
        }

        var feedback: String? = null
        var shouldClear = false

        // --- 纠错 1：实时纠错 - 身体晃动/后仰借力 ---
        // 站直时 bodyAngle 接近 180 度。如果明显小于 165 度，说明在大幅度挺肚子、后仰借力。
        if (bodyAngle < 140.0) {
            bodySwayFrames++
            if (bodySwayFrames >= ERROR_FRAMES_THRESHOLD) {
                feedback = "收紧核心保持身体直立，不要后仰借力！"
            }
        } else {
            if (bodySwayFrames >= ERROR_FRAMES_THRESHOLD) shouldClear = true
            bodySwayFrames = 0
        }

        // --- 纠错 2：实时纠错 - 手臂过度弯曲 ---
        // 只有在身体没有晃动报错时，才提示手臂问题（优先级控制，防止同时报两个错让用户慌乱）
        if (feedback == null) {
            // 前平举允许手肘微曲（保护关节），但如果角度小于 140 度，就变成了“提拉”
            if (elbowAngle < 100.0) {
                bentArmFrames++
                if (bentArmFrames >= ERROR_FRAMES_THRESHOLD) {
                    feedback = "手臂尽量伸直，手肘不要过度弯曲！"
                    shouldClear = false // 覆盖上面的 clear 状态
                }
            } else {
                if (bentArmFrames >= ERROR_FRAMES_THRESHOLD) shouldClear = true
                bentArmFrames = 0
            }
        } else {
            bentArmFrames = 0 // 如果报了身体晃动的错，先清空手臂的报错计数
        }

        // --- 状态机：计数与事后纠错 ---
        var repCompleted = false

        // 动态记录举起过程中的最大角度（寻找最高点）
        if (raiseAngle > maxRaiseAngle) {
            maxRaiseAngle = raiseAngle
        }

        // 判定“举起”：手臂与躯干夹角大于 70 度（接近水平）
        if (raiseAngle > 70.0) {
            isUp = true
        }
        // 判定“放下”：手臂与躯干夹角小于 35 度（自然下垂状态），且之前是举起状态
        else if (raiseAngle < 40.0 && isUp) {
            isUp = false
            repCompleted = true // 完成一次完整的前平举

            // 纠错 3：事后纠错 - 举起高度不够
            // 标准动作要求手臂至少与地面平行（raiseAngle 约 85-90度）。
            // 如果极限高度连 80 度都没到，说明在做半程动作。
            if (maxRaiseAngle < 80.0) {
                consecutiveBadRange++
                if (consecutiveBadRange >= ERROR_REPS_THRESHOLD) {
                    if (feedback == null) {
                        feedback = "举起高度不够，尽量抬高至与肩同高！"
                    }
                    consecutiveBadRange = 0 // 报完错清零
                }
            } else {
                consecutiveBadRange = 0 // 只要举高了一次标准的，历史记录清空
            }

            // 重置最高角度，为下一次做准备
            maxRaiseAngle = 0.0
        }

        return CounterResult(repCompleted, feedback, shouldClear)
    }

    override fun reset() {
        isUp = false
        maxRaiseAngle = 0.0
        bentArmFrames = 0
        bodySwayFrames = 0
        consecutiveBadRange = 0
    }
}