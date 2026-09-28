package com.exam.stateownedenterprise.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AnswerRecordDao {

    @Insert
    suspend fun insert(answerRecord: AnswerRecord): Long

    @Update
    suspend fun update(answerRecord: AnswerRecord)

    @Query("SELECT * FROM answer_records WHERE attemptId = :attemptId AND questionId = :questionId LIMIT 1")
    suspend fun findByAttemptAndQuestion(attemptId: Long, questionId: Long): AnswerRecord?

    @Query("SELECT COUNT(*) FROM answer_records WHERE attemptId = :attemptId AND isCorrect = 1")
    suspend fun countCorrectByAttempt(attemptId: Long): Int

    @Query("SELECT COUNT(*) FROM answer_records WHERE attemptId = :attemptId")
    suspend fun countAnsweredByAttempt(attemptId: Long): Int

    @Query("SELECT * FROM answer_records WHERE attemptId = :attemptId ORDER BY answeredAt")
    fun observeByAttempt(attemptId: Long): Flow<List<AnswerRecord>>

    @Transaction
    @Query(
        """
        SELECT answer_records.* FROM answer_records
        INNER JOIN questions ON questions.id = answer_records.questionId
        WHERE answer_records.attemptId = :attemptId AND answer_records.isCorrect = 0
        ORDER BY questions.questionNumber ASC
        """
    )
    fun observeIncorrectRecordsForAttempt(attemptId: Long): Flow<List<AnswerRecordWithQuestion>>

    @Query(
        """
        SELECT answer_records.* FROM answer_records
        INNER JOIN questions ON questions.id = answer_records.questionId
        WHERE answer_records.isCorrect = 0 AND questions.examSubjectId = :examSubjectId
        ORDER BY answer_records.answeredAt DESC
        """
    )
    fun observeIncorrectByExamSubject(examSubjectId: Long): Flow<List<AnswerRecord>>

    @Query(
        """
        SELECT COUNT(*) FROM answer_records
        WHERE isCorrect = 1 AND questionId IN (
            SELECT id FROM questions WHERE examSubjectId = :examSubjectId
        )
        """
    )
    suspend fun countCorrectByExamSubject(examSubjectId: Long): Int
}
