package com.example.helloapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloapp.model.ExerciseSpec
import com.example.helloapp.model.TrainingItem
import com.example.helloapp.ui.training.counters.ActionCounter
import com.example.helloapp.ui.training.counters.BenchPressCounter
import com.example.helloapp.ui.training.counters.FrontRaiseCounter
import com.example.helloapp.ui.training.counters.JumpingJackCounter
import com.example.helloapp.ui.training.counters.PullUpCounter
import com.example.helloapp.ui.training.counters.PushUpCounter
import com.example.helloapp.ui.training.counters.SitUpCounter
import com.example.helloapp.ui.training.counters.SquatCounter
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrainingViewModel : ViewModel() {

    // ─── 动作列表 ───
    private var exerciseList: List<TrainingItem> = emptyList()
    private var specs: List<ExerciseSpec> = emptyList()

    private val _currentExerciseIndex = MutableStateFlow(0)
    val currentExerciseIndex: StateFlow<Int> = _currentExerciseIndex.asStateFlow()

    private val _totalExercises = MutableStateFlow(1)
    val totalExercises: StateFlow<Int> = _totalExercises.asStateFlow()

    private val _currentExerciseName = MutableStateFlow("")
    val currentExerciseName: StateFlow<String> = _currentExerciseName.asStateFlow()

    private val _currentExerciseIcon = MutableStateFlow("🏋️")
    val currentExerciseIcon: StateFlow<String> = _currentExerciseIcon.asStateFlow()

    // ─── 组/次数 ───
    private val _currentSet = MutableStateFlow(1)
    val currentSet: StateFlow<Int> = _currentSet.asStateFlow()

    private val _totalSets = MutableStateFlow(1)
    val totalSets: StateFlow<Int> = _totalSets.asStateFlow()

    private val _currentRep = MutableStateFlow(0)
    val currentRep: StateFlow<Int> = _currentRep.asStateFlow()

    private val _repsPerSet = MutableStateFlow(1)
    val repsPerSet: StateFlow<Int> = _repsPerSet.asStateFlow()

    private val _isTimeBased = MutableStateFlow(false)
    val isTimeBased: StateFlow<Boolean> = _isTimeBased.asStateFlow()

    private val _setTimeRemaining = MutableStateFlow(0)
    val setTimeRemaining: StateFlow<Int> = _setTimeRemaining.asStateFlow()

    // ─── 通用状态 ───
    private val _isPaused = MutableStateFlow(true)  // 初始暂停，等用户点开始
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0)
    val elapsedTime: StateFlow<Int> = _elapsedTime.asStateFlow()

    /** 当前动作内进度 0f..1f */
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    /** 动作切换中间状态（显示“完成”提示） */
    private val _isTransitioning = MutableStateFlow(false)
    val isTransitioning: StateFlow<Boolean> = _isTransitioning.asStateFlow()

    /** 全部训练完成 */
    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished.asStateFlow()

    private val _currentDetectedAction = MutableStateFlow("等待识别...")
    val currentDetectedAction: StateFlow<String> = _currentDetectedAction.asStateFlow()

    // 模拟每次动作耗时（秒）
    private val repDurationSec = 3

    private var timerJob: Job? = null
    private var setElapsed = 0  // 当前组内已经过的秒数

    private val _formFeedback = MutableStateFlow<String?>(null)
    val formFeedback: StateFlow<String?> = _formFeedback.asStateFlow()

    private var feedbackClearJob: Job? = null // 用于定时清除纠错提示

    private val VOTING_WINDOW_SIZE = 10  // 窗口大小：记录最近10帧的 AI 预测结果
    private val VOTING_THRESHOLD = 6     // 投票阈值：10帧里至少有6帧是目标动作，才算真正识别
    private val actionHistoryBuffer = kotlin.collections.ArrayDeque<Int>() // 存储历史结果的队列

    private val actionIdToNameMap = mapOf(
        0 to "战绳",
        1 to "卧推",
        2 to "前平举",
        3 to "开合跳",
        4 to "其他动作",
        5 to "鞍马",
        6 to "引体向上",
        7 to "俯卧撑",
        8 to "仰卧起坐",
        9 to "深蹲"
    )

    // ─── 自由训练/LSTM 状态判定变量 ───
    private var lastTargetActionTime = System.currentTimeMillis() // 记录最后一次看到目标动作的时间戳
    private val WARNING_DELAY_MS = 2000L // 容忍时间：2秒没做目标动作，就提示

    private var currentCounter: ActionCounter? = null

    private var isDoingTargetAction = false  //一开始还没识别成功过,就不会计数

    // 用于管理提示文字消失的定时器
    private var feedbackJob: Job? = null

    /**
     * 初始化训练，必须在进入训练页面时调用。
     * @param exercises 当天全部训练动作
     * @param startIndex 从第几个动作开始（默认0）
     */
    fun init(exercises: List<TrainingItem>, startIndex: Int = 0) {
        if (exercises.isEmpty()) return
        exerciseList = exercises
        specs = exercises.map { it.toSpec() }
        _totalExercises.value = exercises.size
        _currentExerciseIndex.value = startIndex
        loadExercise(startIndex)
        startTimer()
    }

    private fun loadExercise(index: Int) {
        val item = exerciseList[index]
        val spec = specs[index]
        _currentExerciseName.value = item.title
        _currentExerciseIcon.value = item.icon
        _currentSet.value = 1
        _totalSets.value = spec.sets
        _currentRep.value = 0
        _isTimeBased.value = spec.isTimeBased
        if (spec.isTimeBased) {
            _repsPerSet.value = spec.durationMinutes * 60  // 用秒作为总量
            _setTimeRemaining.value = spec.durationMinutes * 60
        } else {
            _repsPerSet.value = spec.repsPerSet
            _setTimeRemaining.value = 0
        }
        setElapsed = 0

        currentCounter = when (_currentExerciseName.value) {
            "深蹲" -> SquatCounter()
            "俯卧撑" -> PushUpCounter()
            "开合跳" -> JumpingJackCounter()
            "仰卧起坐" -> SitUpCounter()
            "卧推" -> BenchPressCounter()
            "引体向上" -> PullUpCounter()
            "前平举" -> FrontRaiseCounter()
            else -> null
        }
        currentCounter?.reset()

        lastTargetActionTime = System.currentTimeMillis()
        actionHistoryBuffer.clear()
        updateProgress()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                if (!_isPaused.value && !_isTransitioning.value && !_isFinished.value) {
                    delay(1000L)
                    _elapsedTime.value += 1
                    setElapsed += 1
                    tickCurrentSet()
                } else {
                    delay(200L)
                }
            }
        }
    }

    /**
     * 每秒更新当前组的进度。
     */
    private fun tickCurrentSet() {
        val spec = specs[_currentExerciseIndex.value]
        if (spec.isTimeBased) {
            // 时间型动作（如平板支撑）：保留倒计时逻辑
            val totalSec = spec.durationMinutes * 60
            val remaining = (totalSec - setElapsed).coerceAtLeast(0)
            _setTimeRemaining.value = remaining
            // 可选：用 currentRep 来记录经过的秒数供 UI 进度条使用
            _currentRep.value = setElapsed.coerceAtMost(totalSec)
            if (remaining <= 0) {
                advanceSet()
            }
            updateProgress()
        } else {
            // 次数型动作：什么都不做！把计数的权力彻底交给 AI！
            // (删除了原来用 setElapsed / repDurationSec 模拟计数的代码)
        }
    }

    /**
     * 接收来自摄像头的 AI 动作识别结果
     */
    /**
     * 接收来自摄像头的 AI 动作识别结果
     */
    fun processFrameWithPose(actionIndex: Int, landmarks: List<NormalizedLandmark>) {
        if (_isPaused.value || _isTransitioning.value || _isFinished.value) return
        if (_isTimeBased.value) return

        val currentTime = System.currentTimeMillis()
        val expectedActionName = _currentExerciseName.value

        // 1. 找到当前目标动作对应的 ID
        // （假设 expectedActionName 能在 map 的 value 中匹配到）
        val targetActionId = actionIdToNameMap.entries.firstOrNull {
            expectedActionName.contains(it.value)
        }?.key ?: -1

        // 2. 维护滑动窗口
        if (actionIndex == -2 || actionIndex == -1) {
            // 🚨 核心修复：如果人不见了，或者关键点缺失严重，立刻清空投票池！
            // 打破“锁死”状态，防止幽灵计数
            actionHistoryBuffer.clear()
        } else if (actionIndex >= 0 || actionIndex == -4) {
            // 记录有效识别，或者未知动作（用来稀释之前的票数）
            actionHistoryBuffer.addLast(actionIndex)
            // 保持窗口大小恒定
            if (actionHistoryBuffer.size > VOTING_WINDOW_SIZE) {
                actionHistoryBuffer.removeFirst()
            }
        }

        // 3. 计票与状态仲裁
        // 统计窗口中有多少票投给了“目标动作”
        val targetActionCount = actionHistoryBuffer.count { it == targetActionId }

        if (targetActionCount >= VOTING_THRESHOLD) {
            // ✅ 投票通过：确实在做目标动作！
            isDoingTargetAction = true
            _currentDetectedAction.value = expectedActionName
            lastTargetActionTime = currentTime
        } else {
            // ❌ 投票未通过：可能在做别的动作，或者没动
            if(actionIndex != -2 && actionIndex != -1) {
                val timeSinceLastTarget = currentTime - lastTargetActionTime
                if (timeSinceLastTarget > WARNING_DELAY_MS) {
                    _currentDetectedAction.value = "请做 $expectedActionName 动作"
                }
            }
        }

        // 4. 数学计数与纠错层（保持与上一次沟通一致的隔离逻辑）
        val timeSinceLastTarget = currentTime - lastTargetActionTime
        if (isDoingTargetAction && timeSinceLastTarget <= WARNING_DELAY_MS) {
            // 只有当 AI 状态被“确认为目标动作”（或在容忍延迟内），才真正去算角度
            val result = currentCounter?.analyzeFrame(landmarks)

            if (result?.isRepCompleted == true) {
                incrementActualRep()
            }
            if (result?.formFeedback != null) {
                showFeedback(result.formFeedback) // 触发 UI 上的纠错提示
            }else if (result?.clearRealTimeFeedback == true) {
                clearFeedback() // 用户改对了，立刻把红条干掉！
            }
        }
    }

    /**
     * 专门用来展示纠错提示的方法
     */
    private fun showFeedback(message: String) {
        // 1. 立刻把文字显示到 UI 上
        _formFeedback.value = message

        // 2. 取消之前可能正在倒计时的旧定时器
        feedbackJob?.cancel()

        // 3. 开启一个新的 2 秒倒计时
        feedbackJob = viewModelScope.launch {
            delay(2000L) // 停留 2 秒钟 (2000毫秒)
            _formFeedback.value = null // 2 秒后自动清空屏幕文字
        }
    }

    /**
     * 专门用来瞬间清除提示的方法 (应对 shouldClear)
     */
    private fun clearFeedback() {
        _formFeedback.value = null
        feedbackJob?.cancel()
    }

    fun updateDetectedStatus(status: String) {
        _currentDetectedAction.value = status
    }

    /**
     * 真实的次数 +1 逻辑
     */
    private fun incrementActualRep() {
        val current = _currentRep.value
        val target = _repsPerSet.value

        // 还没做完这一组
        if (current < target) {
            _currentRep.value = current + 1
            updateProgress() // 刷新 UI 进度条

            // 如果正好做完了最后一次，触发进入下一组
            if (_currentRep.value >= target) {
                advanceSet()
            }
        }
    }

    /**
     * 当前组完成，进入下一组或下一个动作。
     */
    private fun advanceSet() {
        val curSet = _currentSet.value
        val total = _totalSets.value

        if (curSet < total) {
            // 还有下一组
            _currentSet.value = curSet + 1
            _currentRep.value = 0
            setElapsed = 0
            if (_isTimeBased.value) {
                _setTimeRemaining.value = specs[_currentExerciseIndex.value].durationMinutes * 60
            }
        } else {
            // 当前动作全部组完成，尝试进入下一动作
            advanceExercise()
        }
    }

    private fun advanceExercise() {
        val nextIndex = _currentExerciseIndex.value + 1
        if (nextIndex < exerciseList.size) {
            // 显示过渡提示 2 秒
            _isTransitioning.value = true
            viewModelScope.launch {
                delay(2000L)
                _currentExerciseIndex.value = nextIndex
                loadExercise(nextIndex)
                _isTransitioning.value = false
            }
        } else {
            // 全部完成
            _isFinished.value = true
            _progress.value = 1f
        }
    }

    /**
     * 计算当前动作内的进度。
     * 进度 = (已完成组数 + 当组内进度比) / 总组数
     */
    private fun updateProgress() {
        val spec = specs.getOrNull(_currentExerciseIndex.value) ?: return
        val completedSets = (_currentSet.value - 1).toFloat()
        val inSetProgress = if (spec.isTimeBased) {
            val totalSec = spec.durationMinutes * 60
            if (totalSec > 0) setElapsed.toFloat() / totalSec else 0f
        } else {
            if (spec.repsPerSet > 0) _currentRep.value.toFloat() / spec.repsPerSet else 0f
        }
        _progress.value = ((completedSets + inSetProgress) / spec.sets).coerceIn(0f, 1f)
    }

    fun togglePause() {
        _isPaused.value = !_isPaused.value
        if (!_isPaused.value) {
            // 当从暂停状态恢复为播放时，清空投票池，防止串戏
            actionHistoryBuffer.clear()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
