package com.exam.stateownedenterprise.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single past exam sitting, e.g. "113年台電新進雇員甄試" for category "電力類".
 * Year lives only here; [Subject] and [Question] never repeat it.
 */
@Entity(
    tableName = "exams",
    indices = [Index(value = ["year", "category", "examName"], unique = true)]
)
data class Exam(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val year: Int,
    val category: String,
    val examName: String
)
