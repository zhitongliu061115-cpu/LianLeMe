package com.example.helloapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloapp.model.ExerciseSpec
import com.example.helloapp.model.TrainingItem
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

    // 模拟每次动作耗时（秒）
    private val repDurationSec = 3

    private var timerJob: Job? = null
    private var setElapsed = 0  // 当前组内已经过的秒数

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
            // 时间型：倒计时
            val totalSec = spec.durationMinutes * 60
            val remaining = (totalSec - setElapsed).coerceAtLeast(0)
            _setTimeRemaining.value = remaining
            _currentRep.value = setElapsed.coerceAtMost(totalSec)
            if (remaining <= 0) {
                advanceSet()
            }
        } else {
            // 次数型：模拟每 repDurationSec 秒完成一次
            val newRep = (setElapsed / repDurationSec).coerceAtMost(spec.repsPerSet)
            _currentRep.value = newRep
            if (newRep >= spec.repsPerSet) {
                advanceSet()
            }
        }
        updateProgress()
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
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
