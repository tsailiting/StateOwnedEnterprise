package com.exam.stateownedenterprise.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.exam.stateownedenterprise.data.local.AnswerRecord
import com.exam.stateownedenterprise.data.local.Attempt
import com.exam.stateownedenterprise.data.repository.QuestionRepository
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResultUiState(
    val attempt: Attempt? = null,
    val answerRecords: List<AnswerRecord> = emptyList(),
    val isLoading: Boolean = true
) {
    val totalQuestions: Int
        get() = attempt?.totalQuestions ?: 0

    val answeredCount: Int
        get() = answerRecords.size

    val correctCount: Int
        get() = attempt?.correctCount ?: answerRecords.count { it.isCorrect }

    val incorrectCount: Int
        get() = (answeredCount - correctCount).coerceAtLeast(0)

    val unansweredCount: Int
        get() = (totalQuestions - answeredCount).coerceAtLeast(0)

    val accuracyPercentage: Double
        get() = if (answeredCount > 0) (correctCount.toDouble() / answeredCount * 100) else 0.0

    val formattedAccuracy: String
        get() {
            if (answeredCount == 0) return "0%"
            return if (accuracyPercentage % 1.0 == 0.0) {
                "${accuracyPercentage.toInt()}%"
            } else {
                String.format(Locale.US, "%.1f%%", accuracyPercentage)
            }
        }
}

class ResultViewModel(
    private val repository: QuestionRepository,
    private val attemptId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init {
        loadResult()
    }

    private fun loadResult() {
        viewModelScope.launch {
            val attempt = repository.getAttempt(attemptId)
            if (attempt != null) {
                repository.observeAnswerRecords(attemptId).collect { records ->
                    _uiState.update {
                        it.copy(
                            attempt = attempt,
                            answerRecords = records,
                            isLoading = false
                        )
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

class ResultViewModelFactory(
    private val repository: QuestionRepository,
    private val attemptId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResultViewModel::class.java)) {
            return ResultViewModel(repository, attemptId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
