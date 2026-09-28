package com.exam.stateownedenterprise.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.exam.stateownedenterprise.data.local.AnswerRecordWithQuestion
import com.exam.stateownedenterprise.data.repository.QuestionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewUiState(
    val records: List<AnswerRecordWithQuestion> = emptyList(),
    val selectedRecordIndex: Int? = null,
    val isLoading: Boolean = true
) {
    val selectedRecord: AnswerRecordWithQuestion?
        get() = selectedRecordIndex?.let { records.getOrNull(it) }

    val isDetailMode: Boolean
        get() = selectedRecordIndex != null

    val hasPrevious: Boolean
        get() = selectedRecordIndex != null && selectedRecordIndex > 0

    val hasNext: Boolean
        get() = selectedRecordIndex != null && selectedRecordIndex < records.size - 1
}

class ReviewViewModel(
    private val repository: QuestionRepository,
    private val attemptId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        loadIncorrectRecords()
    }

    private fun loadIncorrectRecords() {
        viewModelScope.launch {
            repository.observeIncorrectRecordsForAttempt(attemptId).collect { records ->
                _uiState.update { state ->
                    state.copy(
                        records = records,
                        isLoading = false,
                        selectedRecordIndex = if (records.isNotEmpty() && state.selectedRecordIndex != null) {
                            state.selectedRecordIndex.coerceAtMost(records.size - 1)
                        } else {
                            state.selectedRecordIndex
                        }
                    )
                }
            }
        }
    }

    fun selectRecord(index: Int?) {
        _uiState.update { it.copy(selectedRecordIndex = index) }
    }

    fun nextRecord() {
        _uiState.update { state ->
            if (state.hasNext && state.selectedRecordIndex != null) {
                state.copy(selectedRecordIndex = state.selectedRecordIndex + 1)
            } else state
        }
    }

    fun previousRecord() {
        _uiState.update { state ->
            if (state.hasPrevious && state.selectedRecordIndex != null) {
                state.copy(selectedRecordIndex = state.selectedRecordIndex - 1)
            } else state
        }
    }
}

class ReviewViewModelFactory(
    private val repository: QuestionRepository,
    private val attemptId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReviewViewModel::class.java)) {
            return ReviewViewModel(repository, attemptId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
