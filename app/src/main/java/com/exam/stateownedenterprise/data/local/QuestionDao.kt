package com.exam.stateownedenterprise.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(questions: List<Question>): List<Long>

    @Query("SELECT * FROM questions WHERE examSubjectId = :examSubjectId ORDER BY questionNumber")
    fun observeByExamSubject(examSubjectId: Long): Flow<List<Question>>

    @Query("SELECT * FROM questions WHERE id = :questionId")
    suspend fun getById(questionId: Long): Question?

    @Query("SELECT COUNT(*) FROM questions WHERE examSubjectId = :examSubjectId")
    suspend fun countByExamSubject(examSubjectId: Long): Int

    /** All questions ever recorded for a subject name, across every exam/year/category. */
    @Query(
        """
        SELECT questions.* FROM questions
        INNER JOIN exam_subjects ON exam_subjects.id = questions.examSubjectId
        INNER JOIN subjects ON subjects.id = exam_subjects.subjectId
        WHERE subjects.name = :subjectName
        ORDER BY exam_subjects.examId, questions.questionNumber
        """
    )
    fun observeAllQuestionsForSubject(subjectName: String): Flow<List<Question>>

    /** Questions for one specific exam paper's subject, e.g. 113 人資 / 企業管理. */
    @Query(
        """
        SELECT questions.* FROM questions
        INNER JOIN exam_subjects ON exam_subjects.id = questions.examSubjectId
        INNER JOIN exams ON exams.id = exam_subjects.examId
        INNER JOIN subjects ON subjects.id = exam_subjects.subjectId
        WHERE exams.year = :year AND exams.category = :category AND subjects.name = :subjectName
        ORDER BY questions.questionNumber
        """
    )
    fun observeQuestions(year: Int, category: String, subjectName: String): Flow<List<Question>>
}
