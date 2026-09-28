package com.exam.stateownedenterprise.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SubjectDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(subject: Subject): Long

    @Query("SELECT * FROM subjects WHERE id = :subjectId")
    suspend fun getById(subjectId: Long): Subject?

    @Query("SELECT * FROM subjects WHERE name = :name")
    suspend fun findByName(name: String): Subject?
}

