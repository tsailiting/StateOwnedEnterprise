package com.exam.stateownedenterprise.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AttemptDao {

    @Insert
    suspend fun insert(attempt: Attempt): Long

    @Update
    suspend fun update(attempt: Attempt)

    @Query("SELECT * FROM attempts WHERE id = :attemptId")
    suspend fun getById(attemptId: Long): Attempt?

    @Query("SELECT * FROM attempts WHERE id = :attemptId")
    fun observeById(attemptId: Long): Flow<Attempt?>

    @Query("SELECT * FROM attempts ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<Attempt>>

    @Query("SELECT * FROM attempts WHERE examSubjectId = :examSubjectId ORDER BY startedAt DESC")
    fun observeByExamSubject(examSubjectId: Long): Flow<List<Attempt>>
}
