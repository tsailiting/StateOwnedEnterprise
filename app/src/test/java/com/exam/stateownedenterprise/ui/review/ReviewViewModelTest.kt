package com.exam.stateownedenterprise.ui.review

import com.exam.stateownedenterprise.data.local.AnswerRecord
import com.exam.stateownedenterprise.data.local.AnswerRecordWithQuestion
import com.exam.stateownedenterprise.data.local.Attempt
import com.exam.stateownedenterprise.data.local.ExamSubjectOption
import com.exam.stateownedenterprise.data.local.Question
import com.exam.stateownedenterprise.data.repository.QuestionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val question10 = Question(
        id = 10L,
        examSubjectId = 1L,
        questionNumber = 10,
        stem = "第10題",
        optionA = "A", optionB = "B", optionC = "C", optionD = "D",
        correctOptions = "B"
    )

    private val question20 = Question(
        id = 20L,
        examSubjectId = 1L,
        questionNumber = 20,
        stem = "第20題",
        optionA = "A", optionB = "B", optionC = "C", optionD = "D",
        correctOptions = "D"
    )

    private val record10 = AnswerRecordWithQuestion(
        answerRecord = AnswerRecord(id = 1L, attemptId = 100L, questionId = 10L, selectedOption = "A", isCorrect = false, answeredAt = 1000L),
        question = question10
    )

    private val record20 = AnswerRecordWithQuestion(
        answerRecord = AnswerRecord(id = 2L, attemptId = 100L, questionId = 20L, selectedOption = "A", isCorrect = false, answeredAt = 2000L),
        question = question20
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads incorrect records ordered by question number`() {
        // Ordered by questionNumber (10 before 20)
        val repository = FakeReviewRepository(listOf(record10, record20))
        val viewModel = ReviewViewModel(repository, attemptId = 100L)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.records.size)
        assertEquals(10, state.records[0].question.questionNumber)
        assertEquals(20, state.records[1].question.questionNumber)
        assertNull(state.selectedRecordIndex)
        assertFalse(state.isDetailMode)
    }

    @Test
    fun `empty state when all answers are correct`() {
        val repository = FakeReviewRepository(emptyList())
        val viewModel = ReviewViewModel(repository, attemptId = 100L)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.records.isEmpty())
        assertNull(state.selectedRecordIndex)
    }

    @Test
    fun `selectRecord and next-previous record navigation`() {
        val repository = FakeReviewRepository(listOf(record10, record20))
        val viewModel = ReviewViewModel(repository, attemptId = 100L)

        // Select first record
        viewModel.selectRecord(0)
        assertTrue(viewModel.uiState.value.isDetailMode)
        assertEquals(record10, viewModel.uiState.value.selectedRecord)
        assertFalse(viewModel.uiState.value.hasPrevious)
        assertTrue(viewModel.uiState.value.hasNext)

        // Move to next record
        viewModel.nextRecord()
        assertEquals(1, viewModel.uiState.value.selectedRecordIndex)
        assertEquals(record20, viewModel.uiState.value.selectedRecord)
        assertTrue(viewModel.uiState.value.hasPrevious)
        assertFalse(viewModel.uiState.value.hasNext)

        // Move back to previous record
        viewModel.previousRecord()
        assertEquals(0, viewModel.uiState.value.selectedRecordIndex)
        assertEquals(record10, viewModel.uiState.value.selectedRecord)

        // Exit detail mode back to list
        viewModel.selectRecord(null)
        assertFalse(viewModel.uiState.value.isDetailMode)
        assertNull(viewModel.uiState.value.selectedRecord)
    }
}

private class FakeReviewRepository(
    private val incorrectRecords: List<AnswerRecordWithQuestion>
) : QuestionRepository {
    override fun getYears(): Flow<List<Int>> = flowOf(emptyList())
    override fun getCategoriesForYear(year: Int): Flow<List<String>> = flowOf(emptyList())
    override fun getSubjectOptions(year: Int, category: String): Flow<List<ExamSubjectOption>> = flowOf(emptyList())
    override fun getQuestionsForExamSubject(examSubjectId: Long): Flow<List<Question>> = flowOf(emptyList())
    override suspend fun createAttempt(examSubjectId: Long, totalQuestions: Int): Attempt = Attempt(id = 1, examSubjectId = 1, startedAt = 0)
    override suspend fun updateAttempt(attempt: Attempt) {}
    override suspend fun getAttempt(attemptId: Long): Attempt? = null
    override fun observeAttempt(attemptId: Long): Flow<Attempt?> = flowOf(null)
    override suspend fun finishAttempt(attemptId: Long): Attempt = Attempt(id = 1, examSubjectId = 1, startedAt = 0)
    override suspend fun saveAnswerRecord(attemptId: Long, questionId: Long, selectedOption: String, isCorrect: Boolean) {}
    override fun observeAnswerRecords(attemptId: Long): Flow<List<AnswerRecord>> = flowOf(emptyList())

    override fun observeIncorrectRecordsForAttempt(attemptId: Long): Flow<List<AnswerRecordWithQuestion>> =
        flowOf(incorrectRecords)
}
