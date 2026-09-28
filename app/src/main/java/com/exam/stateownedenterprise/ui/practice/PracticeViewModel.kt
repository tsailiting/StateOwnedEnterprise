package com.exam.stateownedenterprise.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.exam.stateownedenterprise.data.local.AnswerRecord
import com.exam.stateownedenterprise.data.local.Question
import com.exam.stateownedenterprise.data.local.correctOptionsList
import com.exam.stateownedenterprise.data.repository.QuestionRepository
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PracticeUiState(
    val questions: List<Question> = emptyList(),
    val attemptId: Long? = null,
    val currentIndex: Int = 0,
    val answerRecords: List<AnswerRecord> = emptyList(),
    val isLoading: Boolean = true
) {
    val currentQuestion: Question?
        get() = questions.getOrNull(currentIndex)

    val currentAnswerRecord: AnswerRecord?
        get() = answerRecords.find { it.questionId == currentQuestion?.id }

    val currentSelectedOption: String?
        get() = currentAnswerRecord?.selectedOption

    val hasPrevious: Boolean
        get() = currentIndex > 0

    val hasNext: Boolean
        get() = currentIndex < questions.size - 1

    val isLastQuestion: Boolean
        get() = questions.isNotEmpty() && currentIndex == questions.size - 1

    val isCurrentQuestionAnswered: Boolean
        get() = currentSelectedOption != null

    val answeredCount: Int
        get() = answerRecords.size

    val correctCount: Int
        get() = answerRecords.count { it.isCorrect }

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

    val runningProgressText: String
        get() = "答對 $correctCount / 已刷 $answeredCount    正確率 $formattedAccuracy"

    /** Evaluates whether the user's selected answer for the current question is correct. */
    fun isCurrentAnswerCorrect(): Boolean? {
        val question = currentQuestion ?: return null
        val selectedOption = currentSelectedOption ?: return null
        if (question.isBonus) return true
        return question.correctOptionsList().contains(selectedOption)
    }
}

class PracticeViewModel(
    private val repository: QuestionRepository,
    private val examSubjectId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    init {
        loadQuestionsAndCreateAttempt()
    }

    private fun loadQuestionsAndCreateAttempt() {
        viewModelScope.launch {
            repository.getQuestionsForExamSubject(examSubjectId).collect { questionsList ->
                if (questionsList.isNotEmpty() && _uiState.value.attemptId == null) {
                    val attempt = repository.createAttempt(
                        examSubjectId = examSubjectId,
                        totalQuestions = questionsList.size
                    )
                    _uiState.update { state ->
                        state.copy(
                            questions = questionsList,
                            attemptId = attempt.id,
                            isLoading = false
                        )
                    }
                    observeAnswerRecords(attempt.id)
                } else {
                    _uiState.update { state ->
                        state.copy(
                            questions = questionsList,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    private fun observeAnswerRecords(attemptId: Long) {
        viewModelScope.launch {
            repository.observeAnswerRecords(attemptId).collect { records ->
                _uiState.update { it.copy(answerRecords = records) }
            }
        }
    }

    fun selectOption(option: String) {
        val state = _uiState.value
        val attemptId = state.attemptId ?: return
        val currentQuestion = state.currentQuestion ?: return

        val isCorrect = if (currentQuestion.isBonus) {
            true
        } else {
            currentQuestion.correctOptionsList().contains(option)
        }

        viewModelScope.launch {
            repository.saveAnswerRecord(
                attemptId = attemptId,
                questionId = currentQuestion.id,
                selectedOption = option,
                isCorrect = isCorrect
            )
        }
    }

    fun nextQuestion() {
        _uiState.update { state ->
            if (state.hasNext) {
                state.copy(currentIndex = state.currentIndex + 1)
            } else state
        }
    }

    fun previousQuestion() {
        _uiState.update { state ->
            if (state.hasPrevious) {
                state.copy(currentIndex = state.currentIndex - 1)
            } else state
        }
    }

    fun finishPractice(onFinished: (attemptId: Long) -> Unit) {
        val attemptId = _uiState.value.attemptId ?: return
        viewModelScope.launch {
            repository.finishAttempt(attemptId)
            onFinished(attemptId)
        }
    }
}

class PracticeViewModelFactory(
    private val repository: QuestionRepository,
    private val examSubjectId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PracticeViewModel::class.java)) {
            return PracticeViewModel(repository, examSubjectId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
