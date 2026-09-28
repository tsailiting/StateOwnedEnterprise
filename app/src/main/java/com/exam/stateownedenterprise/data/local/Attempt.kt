package com.exam.stateownedenterprise.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One practice session against an [ExamSubject]. Aggregated fields ([totalQuestions],
 * [correctCount]) allow fast score-history queries without re-joining [AnswerRecord].
 */
@Entity(
    tableName = "attempts",
    foreignKeys = [
        ForeignKey(
            entity = ExamSubject::class,
            parentColumns = ["id"],
            childColumns = ["examSubjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["examSubjectId"])]
)
data class Attempt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val examSubjectId: Long,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val totalQuestions: Int = 0,
    val correctCount: Int = 0
)
