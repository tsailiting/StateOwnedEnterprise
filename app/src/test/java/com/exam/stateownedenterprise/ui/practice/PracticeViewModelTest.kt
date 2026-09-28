package com.exam.stateownedenterprise.ui.practice

import com.exam.stateownedenterprise.data.local.AnswerRecord
import com.exam.stateownedenterprise.data.local.AnswerRecordWithQuestion
import com.exam.stateownedenterprise.data.local.Attempt
import com.exam.stateownedenterprise.data.local.ExamSubjectOption
import com.exam.stateownedenterprise.data.local.Question
import com.exam.stateownedenterprise.data.repository.QuestionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val q1NormalSingle = Question(
        id = 1,
        examSubjectId = 10,
        questionNumber = 1,
        stem = "單一正解題目",
        optionA = "選項A",
        optionB = "選項B",
        optionC = "選項C",
        optionD = "選項D",
        correctOptions = "B",
        isBonus = false,
        explanation = "解析1"
    )

    private val q2MultiAnswer = Question(
        id = 2,
        examSubjectId = 10,
        questionNumber = 2,
        stem = "多重正解題目",
        optionA = "選項A",
        optionB = "選項B",
        optionC = "選項C",
        optionD = "選項D",
        correctOptions = "B,D",
        isBonus = false,
        explanation = "解析2"
    )

    private val q3Bonus = Question(
        id = 3,
        examSubjectId = 10,
        questionNumber = 3,
        stem = "送分題目",
        optionA = "選項A",
        optionB = "選項B",
        optionC = "選項C",
        optionD = "選項D",
        correctOptions = "",
        isBonus = true,
        explanation = "一律送分"
    )

    private lateinit var repository: FakePracticeQuestionRepository
    private lateinit var viewModel: PracticeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakePracticeQuestionRepository(listOf(q1NormalSingle, q2MultiAnswer, q3Bonus))
        viewModel = PracticeViewModel(repository, examSubjectId = 10L)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state creates one attempt and loads questions at index 0`() {
        val state = viewModel.uiState.value
        assertEquals(3, state.questions.size)
        assertEquals(0, state.currentIndex)
        assertEquals(q1NormalSingle, state.currentQuestion)
        assertFalse(state.hasPrevious)
        assertTrue(state.hasNext)
        assertFalse(state.isLastQuestion)
        assertNotNull(state.attemptId)
    }

    @Test
    fun `navigation updates current index and questions`() {
        viewModel.nextQuestion()
        val state1 = viewModel.uiState.value
        assertEquals(1, state1.currentIndex)
        assertEquals(q2MultiAnswer, state1.currentQuestion)
        assertTrue(state1.hasPrevious)
        assertTrue(state1.hasNext)
        assertFalse(state1.isLastQuestion)

        viewModel.nextQuestion()
        val state2 = viewModel.uiState.value
        assertEquals(2, state2.currentIndex)
        assertEquals(q3Bonus, state2.currentQuestion)
        assertTrue(state2.hasPrevious)
        assertFalse(state2.hasNext)
        assertTrue(state2.isLastQuestion)

        viewModel.previousQuestion()
        val state3 = viewModel.uiState.value
        assertEquals(1, state3.currentIndex)
        assertEquals(q2MultiAnswer, state3.currentQuestion)
    }

    @Test
    fun `selecting option persists AnswerRecord and updates correct count`() {
        // Select B for Q1 (correct)
        viewModel.selectOption("B")

        val state = viewModel.uiState.value
        assertEquals("B", state.currentSelectedOption)
        assertEquals(true, state.isCurrentAnswerCorrect())
        assertEquals(1, state.answeredCount)
        assertEquals(1, state.correctCount)
        assertEquals("100%", state.formattedAccuracy)
    }

    @Test
    fun `changing answer from correct to incorrect updates AnswerRecord and score`() {
        // Select B for Q1 (correct)
        viewModel.selectOption("B")
        assertEquals(1, viewModel.uiState.value.correctCount)
        assertEquals(1, viewModel.uiState.value.answeredCount)

        // Change selection to A (incorrect)
        viewModel.selectOption("A")

        val state = viewModel.uiState.value
        assertEquals("A", state.currentSelectedOption)
        assertEquals(false, state.isCurrentAnswerCorrect())
        assertEquals(1, state.answeredCount) // Still 1 answered question
        assertEquals(0, state.correctCount)
        assertEquals("0%", state.formattedAccuracy)
    }

    @Test
    fun `changing answer from incorrect to correct updates score correctly`() {
        // Select A for Q1 (incorrect)
        viewModel.selectOption("A")
        assertEquals(0, viewModel.uiState.value.correctCount)
        assertEquals(1, viewModel.uiState.value.answeredCount)

        // Change selection to B (correct)
        viewModel.selectOption("B")

        val state = viewModel.uiState.value
        assertEquals("B", state.currentSelectedOption)
        assertEquals(true, state.isCurrentAnswerCorrect())
        assertEquals(1, state.answeredCount)
        assertEquals(1, state.correctCount)
        assertEquals("100%", state.formattedAccuracy)
    }

    @Test
    fun `multi-answer scoring accepts any valid option`() {
        viewModel.nextQuestion() // Move to Q2 (correctOptions = "B,D")

        viewModel.selectOption("D")
        assertEquals(true, viewModel.uiState.value.isCurrentAnswerCorrect())

        viewModel.selectOption("B")
        assertEquals(true, viewModel.uiState.value.isCurrentAnswerCorrect())

        viewModel.selectOption("C")
        assertEquals(false, viewModel.uiState.value.isCurrentAnswerCorrect())
    }

    @Test
    fun `bonus question accepts any option as correct`() {
        viewModel.nextQuestion()
        viewModel.nextQuestion() // Move to Q3 (isBonus = true)

        viewModel.selectOption("A")
        assertEquals(true, viewModel.uiState.value.isCurrentAnswerCorrect())

        viewModel.selectOption("C")
        assertEquals(true, viewModel.uiState.value.isCurrentAnswerCorrect())
    }

    @Test
    fun `finishing practice completes attempt`() {
        viewModel.selectOption("B") // Q1 correct
        var finishedAttemptId: Long? = null
        viewModel.finishPractice { finishedAttemptId = it }

        assertNotNull(finishedAttemptId)
        val finishedAttempt = repository.attempts.find { it.id == finishedAttemptId }
        assertNotNull(finishedAttempt)
        assertNotNull(finishedAttempt?.finishedAt)
        assertEquals(1, finishedAttempt?.correctCount)
    }
}

private class FakePracticeQuestionRepository(
    private val questions: List<Question>
) : QuestionRepository {

    val attempts = mutableListOf<Attempt>()
    val answerRecordsFlowMap = mutableMapOf<Long, MutableStateFlow<List<AnswerRecord>>>()
    private var nextAttemptId = 1L
    private var nextRecordId = 1L

    override fun getYears(): Flow<List<Int>> = flowOf(emptyList())
    override fun getCategoriesForYear(year: Int): Flow<List<String>> = flowOf(emptyList())
    override fun getSubjectOptions(year: Int, category: String): Flow<List<ExamSubjectOption>> = flowOf(emptyList())
    override fun getQuestionsForExamSubject(examSubjectId: Long): Flow<List<Question>> = flowOf(questions)

    override suspend fun createAttempt(examSubjectId: Long, totalQuestions: Int): Attempt {
        val attempt = Attempt(
            id = nextAttemptId++,
            examSubjectId = examSubjectId,
            startedAt = System.currentTimeMillis(),
            totalQuestions = totalQuestions,
            correctCount = 0
        )
        attempts.add(attempt)
        answerRecordsFlowMap[attempt.id] = MutableStateFlow(emptyList())
        return attempt
    }

    override suspend fun updateAttempt(attempt: Attempt) {
        val idx = attempts.indexOfFirst { it.id == attempt.id }
        if (idx != -1) attempts[idx] = attempt
    }

    override suspend fun getAttempt(attemptId: Long): Attempt? = attempts.find { it.id == attemptId }

    override fun observeAttempt(attemptId: Long): Flow<Attempt?> = flowOf(attempts.find { it.id == attemptId })

    override suspend fun finishAttempt(attemptId: Long): Attempt {
        val attempt = getAttempt(attemptId)!!
        val records = answerRecordsFlowMap[attemptId]?.value.orEmpty()
        val finished = attempt.copy(
            finishedAt = System.currentTimeMillis(),
            correctCount = records.count { it.isCorrect }
        )
        updateAttempt(finished)
        return finished
    }

    override suspend fun saveAnswerRecord(
        attemptId: Long,
        questionId: Long,
        selectedOption: String,
        isCorrect: Boolean
    ) {
        val flow = answerRecordsFlowMap[attemptId] ?: return
        val currentRecords = flow.value.toMutableList()
        val existingIndex = currentRecords.indexOfFirst { it.questionId == questionId }
        val now = System.currentTimeMillis()
        if (existingIndex != -1) {
            currentRecords[existingIndex] = currentRecords[existingIndex].copy(
                selectedOption = selectedOption,
                isCorrect = isCorrect,
                answeredAt = now
            )
        } else {
            currentRecords.add(
                AnswerRecord(
                    id = nextRecordId++,
                    attemptId = attemptId,
                    questionId = questionId,
                    selectedOption = selectedOption,
                    isCorrect = isCorrect,
                    answeredAt = now
                )
            )
        }
        flow.value = currentRecords
    }

    override fun observeAnswerRecords(attemptId: Long): Flow<List<AnswerRecord>> {
        return answerRecordsFlowMap[attemptId] ?: flowOf(emptyList())
    }

    override fun observeIncorrectRecordsForAttempt(attemptId: Long): Flow<List<AnswerRecordWithQuestion>> {
        return flowOf(emptyList())
    }
}
