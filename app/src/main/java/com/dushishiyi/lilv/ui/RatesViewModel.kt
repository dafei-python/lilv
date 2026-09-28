package com.dushishiyi.lilv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dushishiyi.lilv.LilvApp
import com.dushishiyi.lilv.data.RateChange
import com.dushishiyi.lilv.data.RatesDto
import com.dushishiyi.lilv.data.RatesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 主共享 ViewModel：存款 / 贷款 Tab 共享同一份利率数据。
 * 计算器和关于页不使用此 VM。
 */
class RatesViewModel(
    private val repo: RatesRepository,
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(
            val data: RatesDto,
            val changes: List<RateChange>,
            val fetchedAt: String,   // 已格式化（如 "09-28 09:30"）
            val fetchedAtEpoch: Long,
        ) : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** 是否正在手动刷新（用于下拉刷新动画） */
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        // 先发本地缓存，再尝试联网刷新
        viewModelScope.launch {
            val cached = repo.getCached()
            if (cached != null) {
                publishSuccess(cached, System.currentTimeMillis(), repo.computeChanges(cached))
            }
            refresh()
        }
    }

    fun refresh() {
        if (_refreshing.value) return
        _refreshing.value = true
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val result = repo.refresh()
            _refreshing.value = false
            result
                .onSuccess { dto -> publishSuccess(dto, now, repo.computeChanges(dto)) }
                .onFailure { e ->
                    if (_uiState.value !is UiState.Success) {
                        _uiState.value = UiState.Error(e.message ?: "网络异常")
                    }
                    // 已有缓存时不覆盖错误，保留旧数据展示
                }
        }
    }

    private suspend fun publishSuccess(
        dto: RatesDto,
        epoch: Long,
        changesDeferred: List<RateChange>,
    ) {
        val formatted = repo.formatFetchedAt(epoch)
        _uiState.update {
            UiState.Success(dto, changesDeferred, formatted, epoch)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val repo = LilvApp.instance.ratesRepository
                RatesViewModel(repo)
            }
        }
    }
}
