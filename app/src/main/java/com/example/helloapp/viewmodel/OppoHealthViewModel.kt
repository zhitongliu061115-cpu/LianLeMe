package com.example.helloapp.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloapp.data.health.HealthRepository
import com.example.helloapp.data.health.model.HealthSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OppoHealthUiState(
    val loading: Boolean = false,
    val summary: HealthSummary? = null,
    val error: String? = null
)

class OppoHealthViewModel(
    private val repository: HealthRepository = HealthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(OppoHealthUiState(loading = true))
    val uiState: StateFlow<OppoHealthUiState> = _uiState.asStateFlow()

    fun loadHealthData() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    error = null
                )
            }

            try {
                val summary = repository.getHealthSummary()
                _uiState.update {
                    it.copy(
                        loading = false,
                        summary = summary,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = e.message ?: "读取健康数据失败"
                    )
                }
            }
        }
    }

    fun connectHealth() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    error = null
                )
            }

            try {
                val result = repository.connectHealth()
                result.onSuccess {
                    val summary = repository.refreshHealthData()
                    _uiState.update {
                        it.copy(
                            loading = false,
                            summary = summary,
                            error = null
                        )
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = e.message ?: "连接 OPPO 健康失败"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = e.message ?: "连接 OPPO 健康失败"
                    )
                }
            }
        }
    }

    fun requestAuthorization(activity: Activity) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    error = null
                )
            }

            try {
                val result = repository.requestAuthorization(activity)
                result.onSuccess {
                    val summary = repository.refreshHealthData()
                    _uiState.update {
                        it.copy(
                            loading = false,
                            summary = summary,
                            error = null
                        )
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = e.message ?: "授权失败"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = e.message ?: "授权失败"
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    error = null
                )
            }

            try {
                val summary = repository.refreshHealthData()
                _uiState.update {
                    it.copy(
                        loading = false,
                        summary = summary,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = e.message ?: "刷新健康数据失败"
                    )
                }
            }
        }
    }
}
