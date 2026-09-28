package com.exam.stateownedenterprise.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(exam: Exam): Long

    @Query("SELECT * FROM exams ORDER BY year DESC, category, examName")
    fun observeAll(): Flow<List<Exam>>

    @Query("SELECT DISTINCT year FROM exams ORDER BY year DESC")
    fun observeYears(): Flow<List<Int>>

    @Query("SELECT DISTINCT category FROM exams WHERE year = :year ORDER BY category")
    fun observeCategoriesForYear(year: Int): Flow<List<String>>

    @Query("SELECT * FROM exams WHERE id = :examId")
    suspend fun getById(examId: Long): Exam?

    @Query("SELECT * FROM exams WHERE year = :year AND category = :category AND examName = :examName")
    suspend fun findByYearCategoryName(year: Int, category: String, examName: String): Exam?

    /** Assumes (year, category) uniquely identifies an Exam (confirmed product decision). */
    @Query("SELECT * FROM exams WHERE year = :year AND category = :category")
    suspend fun findByYearAndCategory(year: Int, category: String): Exam?
}
