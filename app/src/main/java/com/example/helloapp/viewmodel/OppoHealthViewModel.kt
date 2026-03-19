package com.example.helloapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloapp.data.health.HealthRepository
import com.example.helloapp.data.health.model.HealthSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OppoHealthUiState(
    val isLoading: Boolean = false,
    val summary: HealthSummary? = null,
    val errorMessage: String? = null
)

class OppoHealthViewModel(
    private val repository: HealthRepository = HealthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(OppoHealthUiState())
    val uiState: StateFlow<OppoHealthUiState> = _uiState.asStateFlow()

    fun loadHealthData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            runCatching {
                repository.getHealthSummary()
            }.onSuccess { summary ->
                _uiState.value = OppoHealthUiState(
                    isLoading = false,
                    summary = summary,
                    errorMessage = null
                )
            }.onFailure { e ->
                _uiState.value = OppoHealthUiState(
                    isLoading = false,
                    summary = null,
                    errorMessage = e.message ?: "加载失败"
                )
            }
        }
    }

    fun connectHealth() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            runCatching {
                repository.connectHealth().getOrThrow()
                repository.refreshHealthData()
            }.onSuccess { summary ->
                _uiState.value = OppoHealthUiState(
                    isLoading = false,
                    summary = summary,
                    errorMessage = null
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "连接失败"
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            runCatching {
                repository.refreshHealthData()
            }.onSuccess { summary ->
                _uiState.value = OppoHealthUiState(
                    isLoading = false,
                    summary = summary,
                    errorMessage = null
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "刷新失败"
                )
            }
        }
    }
}
