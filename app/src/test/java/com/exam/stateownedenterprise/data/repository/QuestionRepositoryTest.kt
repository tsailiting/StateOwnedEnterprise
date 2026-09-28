package com.exam.stateownedenterprise.data.repository

import com.exam.stateownedenterprise.data.local.AnswerRecord
import com.exam.stateownedenterprise.data.local.AnswerRecordDao
import com.exam.stateownedenterprise.data.local.AnswerRecordWithQuestion
import com.exam.stateownedenterprise.data.local.Attempt
import com.exam.stateownedenterprise.data.local.AttemptDao
import com.exam.stateownedenterprise.data.local.Exam
import com.exam.stateownedenterprise.data.local.ExamDao
import com.exam.stateownedenterprise.data.local.ExamSubject
import com.exam.stateownedenterprise.data.local.ExamSubjectDao
import com.exam.stateownedenterprise.data.local.ExamSubjectOption
import com.exam.stateownedenterprise.data.local.Question
import com.exam.stateownedenterprise.data.local.QuestionDao
import com.exam.stateownedenterprise.data.local.Subject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuestionRepositoryTest {

    private lateinit var fakeExamDao: FakeExamDao
    private lateinit var fakeExamSubjectDao: FakeExamSubjectDao
    private lateinit var fakeQuestionDao: FakeQuestionDao
    private lateinit var fakeAttemptDao: FakeAttemptDao
    private lateinit var fakeAnswerRecordDao: FakeAnswerRecordDao
    private lateinit var repository: QuestionRepository

    @Before
    fun setUp() {
        fakeExamDao = FakeExamDao()
        fakeExamSubjectDao = FakeExamSubjectDao()
        fakeQuestionDao = FakeQuestionDao()
        fakeAttemptDao = FakeAttemptDao()
        fakeAnswerRecordDao = FakeAnswerRecordDao()
        repository = DefaultQuestionRepository(
            fakeExamDao,
            fakeExamSubjectDao,
            fakeQuestionDao,
            fakeAttemptDao,
            fakeAnswerRecordDao
        )
    }

    @Test
    fun `getYears delegates to examDao`() = runBlocking {
        fakeExamDao.years = listOf(113, 112)
        val years = repository.getYears().first()
        assertEquals(listOf(113, 112), years)
    }

    @Test
    fun `getCategoriesForYear delegates to examDao`() = runBlocking {
        fakeExamDao.categories = listOf("人資", "企管")
        val categories = repository.getCategoriesForYear(113).first()
        assertEquals(listOf("人資", "企管"), categories)
    }

    @Test
    fun `getSubjectOptions delegates to examSubjectDao`() = runBlocking {
        fakeExamSubjectDao.options = listOf(ExamSubjectOption(1L, "企業管理"))
        val options = repository.getSubjectOptions(113, "人資").first()
        assertEquals(1, options.size)
        assertEquals("企業管理", options[0].subjectName)
    }

    @Test
    fun `getQuestionsForExamSubject delegates to questionDao`() = runBlocking {
        val sampleQuestion = Question(
            id = 1L,
            examSubjectId = 10L,
            questionNumber = 1,
            stem = "測試",
            optionA = "A",
            optionB = "B",
            optionC = "C",
            optionD = "D",
            correctOptions = "B"
        )
        fakeQuestionDao.questions = listOf(sampleQuestion)
        val questions = repository.getQuestionsForExamSubject(10L).first()
        assertEquals(1, questions.size)
        assertEquals(sampleQuestion, questions[0])
    }

    @Test
    fun `createAttempt inserts new Attempt and returns it with id`() = runBlocking {
        val attempt = repository.createAttempt(examSubjectId = 5L, totalQuestions = 25)
        assertTrue(attempt.id > 0)
        assertEquals(5L, attempt.examSubjectId)
        assertEquals(25, attempt.totalQuestions)
        assertEquals(0, attempt.correctCount)
        assertTrue(attempt.startedAt > 0)
    }

    @Test
    fun `saveAnswerRecord inserts record when non-existent`() = runBlocking {
        repository.saveAnswerRecord(attemptId = 1L, questionId = 100L, selectedOption = "B", isCorrect = true)
        val records = fakeAnswerRecordDao.records
        assertEquals(1, records.size)
        assertEquals("B", records[0].selectedOption)
        assertTrue(records[0].isCorrect)
    }

    @Test
    fun `saveAnswerRecord updates record when already exists for same attempt and question`() = runBlocking {
        // First selection: "B" (correct)
        repository.saveAnswerRecord(attemptId = 1L, questionId = 100L, selectedOption = "B", isCorrect = true)
        val initialRecord = fakeAnswerRecordDao.records.single()
        assertEquals("B", initialRecord.selectedOption)
        assertTrue(initialRecord.isCorrect)

        // Change answer to "A" (incorrect)
        repository.saveAnswerRecord(attemptId = 1L, questionId = 100L, selectedOption = "A", isCorrect = false)

        // Must still be 1 record, not duplicate
        assertEquals(1, fakeAnswerRecordDao.records.size)
        val updatedRecord = fakeAnswerRecordDao.records.single()
        assertEquals(initialRecord.id, updatedRecord.id)
        assertEquals("A", updatedRecord.selectedOption)
        assertEquals(false, updatedRecord.isCorrect)
    }

    @Test
    fun `finishAttempt sets finishedAt and calculates correct count`() = runBlocking {
        val attempt = repository.createAttempt(examSubjectId = 5L, totalQuestions = 25)
        repository.saveAnswerRecord(attemptId = attempt.id, questionId = 101L, selectedOption = "B", isCorrect = true)
        repository.saveAnswerRecord(attemptId = attempt.id, questionId = 102L, selectedOption = "A", isCorrect = false)

        val finished = repository.finishAttempt(attempt.id)
        assertNotNull(finished.finishedAt)
        assertEquals(1, finished.correctCount)
    }
}

private class FakeExamDao : ExamDao {
    var years = emptyList<Int>()
    var categories = emptyList<String>()

    override suspend fun insert(exam: Exam): Long = 0
    override fun observeAll(): Flow<List<Exam>> = flowOf(emptyList())
    override fun observeYears(): Flow<List<Int>> = flowOf(years)
    override fun observeCategoriesForYear(year: Int): Flow<List<String>> = flowOf(categories)
    override suspend fun getById(examId: Long): Exam? = null
    override suspend fun findByYearCategoryName(year: Int, category: String, examName: String): Exam? = null
    override suspend fun findByYearAndCategory(year: Int, category: String): Exam? = null
}

private class FakeExamSubjectDao : ExamSubjectDao {
    var options = emptyList<ExamSubjectOption>()

    override suspend fun insert(examSubject: ExamSubject): Long = 0
    override suspend fun update(examSubject: ExamSubject) {}
    override suspend fun findByExamAndSubject(examId: Long, subjectId: Long) = null
    override suspend fun getById(examSubjectId: Long) = null
    override fun observeSubjectsForExam(year: Int, category: String): Flow<List<Subject>> = flowOf(emptyList())
    override fun observeSubjectOptionsForExam(year: Int, category: String): Flow<List<ExamSubjectOption>> = flowOf(options)
    override fun observeExamsContainingSubject(subjectName: String): Flow<List<Exam>> = flowOf(emptyList())
}

private class FakeQuestionDao : QuestionDao {
    var questions = emptyList<Question>()

    override suspend fun insertAll(questions: List<Question>): List<Long> = emptyList()
    override fun observeByExamSubject(examSubjectId: Long): Flow<List<Question>> = flowOf(questions)
    override suspend fun getById(questionId: Long): Question? = null
    override suspend fun countByExamSubject(examSubjectId: Long): Int = questions.size
    override fun observeAllQuestionsForSubject(subjectName: String): Flow<List<Question>> = flowOf(emptyList())
    override fun observeQuestions(year: Int, category: String, subjectName: String): Flow<List<Question>> = flowOf(questions)
}

private class FakeAttemptDao : AttemptDao {
    private val attemptsMap = mutableMapOf<Long, Attempt>()
    private var nextId = 1L

    override suspend fun insert(attempt: Attempt): Long {
        val id = nextId++
        val newAttempt = attempt.copy(id = id)
        attemptsMap[id] = newAttempt
        return id
    }

    override suspend fun update(attempt: Attempt) {
        attemptsMap[attempt.id] = attempt
    }

    override suspend fun getById(attemptId: Long): Attempt? = attemptsMap[attemptId]

    override fun observeById(attemptId: Long): Flow<Attempt?> = flowOf(attemptsMap[attemptId])

    override fun observeAll(): Flow<List<Attempt>> = flowOf(attemptsMap.values.toList())

    override fun observeByExamSubject(examSubjectId: Long): Flow<List<Attempt>> =
        flowOf(attemptsMap.values.filter { it.examSubjectId == examSubjectId })
}

private class FakeAnswerRecordDao : AnswerRecordDao {
    val records = mutableListOf<AnswerRecord>()
    private var nextId = 1L

    override suspend fun insert(answerRecord: AnswerRecord): Long {
        val id = nextId++
        val record = answerRecord.copy(id = id)
        records.add(record)
        return id
    }

    override suspend fun update(answerRecord: AnswerRecord) {
        val index = records.indexOfFirst { it.id == answerRecord.id }
        if (index != -1) {
            records[index] = answerRecord
        }
    }

    override suspend fun findByAttemptAndQuestion(attemptId: Long, questionId: Long): AnswerRecord? {
        return records.find { it.attemptId == attemptId && it.questionId == questionId }
    }

    override suspend fun countCorrectByAttempt(attemptId: Long): Int {
        return records.count { it.attemptId == attemptId && it.isCorrect }
    }

    override suspend fun countAnsweredByAttempt(attemptId: Long): Int {
        return records.count { it.attemptId == attemptId }
    }

    override fun observeByAttempt(attemptId: Long): Flow<List<AnswerRecord>> {
        return flowOf(records.filter { it.attemptId == attemptId })
    }

    override fun observeIncorrectByExamSubject(examSubjectId: Long): Flow<List<AnswerRecord>> {
        return flowOf(emptyList())
    }

    override fun observeIncorrectRecordsForAttempt(attemptId: Long): Flow<List<AnswerRecordWithQuestion>> {
        return flowOf(emptyList())
    }

    override suspend fun countCorrectByExamSubject(examSubjectId: Long): Int = 0
}
