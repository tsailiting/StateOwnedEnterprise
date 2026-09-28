package com.exam.stateownedenterprise.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The user's answer to one [Question] within an [Attempt]. This is the source of truth
 * for score history, incorrect-answer review, and per-subject accuracy.
 * [selectedOption] is one of "A", "B", "C", "D", or null if skipped.
 */
@Entity(
    tableName = "answer_records",
    foreignKeys = [
        ForeignKey(
            entity = Attempt::class,
            parentColumns = ["id"],
            childColumns = ["attemptId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Question::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["attemptId", "questionId"], unique = true),
        Index(value = ["questionId"])
    ]
)
data class AnswerRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val attemptId: Long,
    val questionId: Long,
    val selectedOption: String?,
    val isCorrect: Boolean,
    val answeredAt: Long
)
