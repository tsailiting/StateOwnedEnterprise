package com.exam.stateownedenterprise.ui.result

import com.exam.stateownedenterprise.data.local.AnswerRecord
import com.exam.stateownedenterprise.data.local.Attempt
import org.junit.Assert.assertEquals
import org.junit.Test

class ResultViewModelTest {

    @Test
    fun `accuracy uses answered count as denominator`() {
        val attempt = Attempt(id = 1L, examSubjectId = 10L, startedAt = 1000L, totalQuestions = 25, correctCount = 15)
        val answerRecords = (1..18).map { index ->
            AnswerRecord(
                id = index.toLong(),
                attemptId = 1L,
                questionId = index.toLong(),
                selectedOption = "B",
                isCorrect = index <= 15, // 15 correct out of 18 answered
                answeredAt = 1000L + index
            )
        }

        val state = ResultUiState(attempt = attempt, answerRecords = answerRecords, isLoading = false)

        assertEquals(25, state.totalQuestions)
        assertEquals(18, state.answeredCount)
        assertEquals(15, state.correctCount)
        assertEquals(3, state.incorrectCount)
        assertEquals(7, state.unansweredCount)
        assertEquals("83.3%", state.formattedAccuracy)
    }

    @Test
    fun `zero answered questions yields 0 percent accuracy without division by zero`() {
        val attempt = Attempt(id = 1L, examSubjectId = 10L, startedAt = 1000L, totalQuestions = 25, correctCount = 0)
        val state = ResultUiState(attempt = attempt, answerRecords = emptyList(), isLoading = false)

        assertEquals(25, state.totalQuestions)
        assertEquals(0, state.answeredCount)
        assertEquals(0, state.correctCount)
        assertEquals(0, state.incorrectCount)
        assertEquals(25, state.unansweredCount)
        assertEquals("0%", state.formattedAccuracy)
    }

    @Test
    fun `exact integer percentage formats without decimal places`() {
        val attempt = Attempt(id = 1L, examSubjectId = 10L, startedAt = 1000L, totalQuestions = 25, correctCount = 20)
        val answerRecords = (1..25).map { index ->
            AnswerRecord(
                id = index.toLong(),
                attemptId = 1L,
                questionId = index.toLong(),
                selectedOption = "B",
                isCorrect = index <= 20, // 20 correct out of 25
                answeredAt = 1000L + index
            )
        }

        val state = ResultUiState(attempt = attempt, answerRecords = answerRecords, isLoading = false)

        assertEquals("80%", state.formattedAccuracy)
    }
}
