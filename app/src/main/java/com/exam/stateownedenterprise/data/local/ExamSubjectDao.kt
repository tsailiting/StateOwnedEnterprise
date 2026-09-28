package com.exam.stateownedenterprise.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamSubjectDao {

    @Insert
    suspend fun insert(examSubject: ExamSubject): Long

    @Update
    suspend fun update(examSubject: ExamSubject)

    @Query("SELECT * FROM exam_subjects WHERE examId = :examId AND subjectId = :subjectId")
    suspend fun findByExamAndSubject(examId: Long, subjectId: Long): ExamSubject?

    @Query("SELECT * FROM exam_subjects WHERE id = :examSubjectId")
    suspend fun getById(examSubjectId: Long): ExamSubject?

    /** All subjects for a given exam (year + category), ordered by their sortOrder. */
    @Query(
        """
        SELECT subjects.* FROM subjects
        INNER JOIN exam_subjects ON exam_subjects.subjectId = subjects.id
        INNER JOIN exams ON exams.id = exam_subjects.examId
        WHERE exams.year = :year AND exams.category = :category
        ORDER BY exam_subjects.sortOrder
        """
    )
    fun observeSubjectsForExam(year: Int, category: String): Flow<List<Subject>>

    /**
     * Same as [observeSubjectsForExam] but also projects the [ExamSubject.id], since
     * callers (e.g. the home screen) need it to start practice on a specific paper.
     */
    @Query(
        """
        SELECT exam_subjects.id AS examSubjectId, subjects.name AS subjectName
        FROM exam_subjects
        INNER JOIN subjects ON subjects.id = exam_subjects.subjectId
        INNER JOIN exams ON exams.id = exam_subjects.examId
        WHERE exams.year = :year AND exams.category = :category
        ORDER BY exam_subjects.sortOrder
        """
    )
    fun observeSubjectOptionsForExam(year: Int, category: String): Flow<List<ExamSubjectOption>>

    /** All exams (any category/year) that include the given subject name. */
    @Query(
        """
        SELECT DISTINCT exams.* FROM exams
        INNER JOIN exam_subjects ON exam_subjects.examId = exams.id
        INNER JOIN subjects ON subjects.id = exam_subjects.subjectId
        WHERE subjects.name = :subjectName
        ORDER BY exams.year DESC
        """
    )
    fun observeExamsContainingSubject(subjectName: String): Flow<List<Exam>>
}

/** Projection of an [ExamSubject] + its [Subject.name], for exam/subject picker UIs. */
data class ExamSubjectOption(
    val examSubjectId: Long,
    val subjectName: String
)
