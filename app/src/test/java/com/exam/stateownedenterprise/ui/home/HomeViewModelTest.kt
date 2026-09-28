package com.exam.stateownedenterprise.ui.home

import com.exam.stateownedenterprise.data.local.AnswerRecord
import com.exam.stateownedenterprise.data.local.AnswerRecordWithQuestion
import com.exam.stateownedenterprise.data.local.Attempt
import com.exam.stateownedenterprise.data.local.ExamSubjectOption
import com.exam.stateownedenterprise.data.local.Question
import com.exam.stateownedenterprise.data.repository.QuestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class HomeViewModelTest {

    private lateinit var fakeRepository: FakeQuestionRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeQuestionRepository()
        viewModel = HomeViewModel(fakeRepository)
    }

    @Test
    fun `initial state has null selections`() {
        assertNull(viewModel.selectedYear.value)
        assertNull(viewModel.selectedCategory.value)
        assertNull(viewModel.selectedExamSubjectId.value)
    }

    @Test
    fun `selecting year resets category and subject`() {
        viewModel.selectCategory("人資")
        viewModel.selectExamSubject(100L)

        viewModel.selectYear(113)

        assertEquals(113, viewModel.selectedYear.value)
        assertNull(viewModel.selectedCategory.value)
        assertNull(viewModel.selectedExamSubjectId.value)
    }

    @Test
    fun `selecting category resets subject`() {
        viewModel.selectYear(113)
        viewModel.selectExamSubject(100L)

        viewModel.selectCategory("人資")

        assertEquals(113, viewModel.selectedYear.value)
        assertEquals("人資", viewModel.selectedCategory.value)
        assertNull(viewModel.selectedExamSubjectId.value)
    }

    @Test
    fun `selecting subject sets selectedExamSubjectId`() {
        viewModel.selectYear(113)
        viewModel.selectCategory("人資")

        viewModel.selectExamSubject(100L)

        assertEquals(100L, viewModel.selectedExamSubjectId.value)
    }

    @Test
    fun `changing year again clears previous category and subject`() {
        viewModel.selectYear(113)
        viewModel.selectCategory("人資")
        viewModel.selectExamSubject(100L)

        viewModel.selectYear(112)

        assertEquals(112, viewModel.selectedYear.value)
        assertNull(viewModel.selectedCategory.value)
        assertNull(viewModel.selectedExamSubjectId.value)
    }
}

class FakeQuestionRepository : QuestionRepository {
    var years = listOf(113, 112)
    var categoriesMap = mapOf(113 to listOf("人資", "企管"))
    var subjectsMap = mapOf(
        Pair(113, "人資") to listOf(
            ExamSubjectOption(1L, "企業管理"),
            ExamSubjectOption(2L, "法學緒論")
        )
    )

    override fun getYears(): Flow<List<Int>> = flowOf(years)

    override fun getCategoriesForYear(year: Int): Flow<List<String>> =
        flowOf(categoriesMap[year].orEmpty())

    override fun getSubjectOptions(year: Int, category: String): Flow<List<ExamSubjectOption>> =
        flowOf(subjectsMap[Pair(year, category)].orEmpty())

    override fun getQuestionsForExamSubject(examSubjectId: Long): Flow<List<Question>> =
        flowOf(emptyList())

    override suspend fun createAttempt(examSubjectId: Long, totalQuestions: Int): Attempt =
        Attempt(id = 1L, examSubjectId = examSubjectId, startedAt = 0L, totalQuestions = totalQuestions)

    override suspend fun updateAttempt(attempt: Attempt) {}

    override suspend fun getAttempt(attemptId: Long): Attempt? = null

    override fun observeAttempt(attemptId: Long): Flow<Attempt?> = flowOf(null)

    override suspend fun finishAttempt(attemptId: Long): Attempt =
        Attempt(id = attemptId, examSubjectId = 1L, startedAt = 0L)

    override suspend fun saveAnswerRecord(
        attemptId: Long,
        questionId: Long,
        selectedOption: String,
        isCorrect: Boolean
    ) {}

    override fun observeAnswerRecords(attemptId: Long): Flow<List<AnswerRecord>> =
        flowOf(emptyList())

    override fun observeIncorrectRecordsForAttempt(attemptId: Long): Flow<List<AnswerRecordWithQuestion>> =
        flowOf(emptyList())
}
