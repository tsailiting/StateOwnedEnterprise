package com.exam.stateownedenterprise.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ImportMetaDao {

    @Query("SELECT * FROM import_meta WHERE fileName = :fileName")
    suspend fun findByFileName(fileName: String): ImportMeta?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: ImportMeta)
}
