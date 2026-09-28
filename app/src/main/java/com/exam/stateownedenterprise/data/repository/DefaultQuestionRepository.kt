package com.exam.stateownedenterprise.data.repository

import com.exam.stateownedenterprise.data.local.AnswerRecord
import com.exam.stateownedenterprise.data.local.AnswerRecordDao
import com.exam.stateownedenterprise.data.local.AnswerRecordWithQuestion
import com.exam.stateownedenterprise.data.local.Attempt
import com.exam.stateownedenterprise.data.local.AttemptDao
import com.exam.stateownedenterprise.data.local.ExamDao
import com.exam.stateownedenterprise.data.local.ExamSubjectDao
import com.exam.stateownedenterprise.data.local.ExamSubjectOption
import com.exam.stateownedenterprise.data.local.Question
import com.exam.stateownedenterprise.data.local.QuestionDao
import kotlinx.coroutines.flow.Flow

class DefaultQuestionRepository(
    private val examDao: ExamDao,
    private val examSubjectDao: ExamSubjectDao,
    private val questionDao: QuestionDao,
    private val attemptDao: AttemptDao,
    private val answerRecordDao: AnswerRecordDao
) : QuestionRepository {

    override fun getYears(): Flow<List<Int>> =
        examDao.observeYears()

    override fun getCategoriesForYear(year: Int): Flow<List<String>> =
        examDao.observeCategoriesForYear(year)

    override fun getSubjectOptions(year: Int, category: String): Flow<List<ExamSubjectOption>> =
        examSubjectDao.observeSubjectOptionsForExam(year, category)

    override fun getQuestionsForExamSubject(examSubjectId: Long): Flow<List<Question>> =
        questionDao.observeByExamSubject(examSubjectId)

    override suspend fun createAttempt(examSubjectId: Long, totalQuestions: Int): Attempt {
        val attempt = Attempt(
            examSubjectId = examSubjectId,
            startedAt = System.currentTimeMillis(),
            totalQuestions = totalQuestions,
            correctCount = 0
        )
        val id = attemptDao.insert(attempt)
        return attempt.copy(id = id)
    }

    override suspend fun updateAttempt(attempt: Attempt) {
        attemptDao.update(attempt)
    }

    override suspend fun getAttempt(attemptId: Long): Attempt? {
        return attemptDao.getById(attemptId)
    }

    override fun observeAttempt(attemptId: Long): Flow<Attempt?> {
        return attemptDao.observeById(attemptId)
    }

    override suspend fun finishAttempt(attemptId: Long): Attempt {
        val attempt = attemptDao.getById(attemptId) ?: error("Attempt $attemptId not found")
        val correctCount = answerRecordDao.countCorrectByAttempt(attemptId)
        val finishedAttempt = attempt.copy(
            finishedAt = System.currentTimeMillis(),
            correctCount = correctCount
        )
        attemptDao.update(finishedAttempt)
        return finishedAttempt
    }

    override suspend fun saveAnswerRecord(
        attemptId: Long,
        questionId: Long,
        selectedOption: String,
        isCorrect: Boolean
    ) {
        val existing = answerRecordDao.findByAttemptAndQuestion(attemptId, questionId)
        val now = System.currentTimeMillis()
        if (existing != null) {
            answerRecordDao.update(
                existing.copy(
                    selectedOption = selectedOption,
                    isCorrect = isCorrect,
                    answeredAt = now
                )
            )
        } else {
            answerRecordDao.insert(
                AnswerRecord(
                    attemptId = attemptId,
                    questionId = questionId,
                    selectedOption = selectedOption,
                    isCorrect = isCorrect,
                    answeredAt = now
                )
            )
        }
    }

    override fun observeAnswerRecords(attemptId: Long): Flow<List<AnswerRecord>> {
        return answerRecordDao.observeByAttempt(attemptId)
    }

    override fun observeIncorrectRecordsForAttempt(attemptId: Long): Flow<List<AnswerRecordWithQuestion>> {
        return answerRecordDao.observeIncorrectRecordsForAttempt(attemptId)
    }
}
