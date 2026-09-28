package com.exam.stateownedenterprise.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A reusable subject/paper name, e.g. "企業管理" or "法學緒論". Not tied to any single
 * [Exam] — the same subject can appear across many years/categories via [ExamSubject].
 */
@Entity(
    tableName = "subjects",
    indices = [Index(value = ["name"], unique = true)]
)
data class Subject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)

