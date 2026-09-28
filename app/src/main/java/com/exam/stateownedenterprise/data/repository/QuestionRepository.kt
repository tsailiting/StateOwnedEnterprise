package com.exam.stateownedenterprise.data.repository

import com.exam.stateownedenterprise.data.local.AnswerRecord
import com.exam.stateownedenterprise.data.local.AnswerRecordWithQuestion
import com.exam.stateownedenterprise.data.local.Attempt
import com.exam.stateownedenterprise.data.local.ExamSubjectOption
import com.exam.stateownedenterprise.data.local.Question
import kotlinx.coroutines.flow.Flow

interface QuestionRepository {
    fun getYears(): Flow<List<Int>>
    fun getCategoriesForYear(year: Int): Flow<List<String>>
    fun getSubjectOptions(year: Int, category: String): Flow<List<ExamSubjectOption>>
    fun getQuestionsForExamSubject(examSubjectId: Long): Flow<List<Question>>

    // Attempt operations
    suspend fun createAttempt(examSubjectId: Long, totalQuestions: Int): Attempt
    suspend fun updateAttempt(attempt: Attempt)
    suspend fun getAttempt(attemptId: Long): Attempt?
    fun observeAttempt(attemptId: Long): Flow<Attempt?>
    suspend fun finishAttempt(attemptId: Long): Attempt

    // AnswerRecord operations
    suspend fun saveAnswerRecord(
        attemptId: Long,
        questionId: Long,
        selectedOption: String,
        isCorrect: Boolean
    )
    fun observeAnswerRecords(attemptId: Long): Flow<List<AnswerRecord>>
    fun observeIncorrectRecordsForAttempt(attemptId: Long): Flow<List<AnswerRecordWithQuestion>>
}
