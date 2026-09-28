package com.exam.stateownedenterprise.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Associates a reusable [Subject] with a specific [Exam] paper, carrying the subject's
 * display order within that exam. This is the entity [Question] and [Attempt] actually
 * belong to, so the same [Subject] used across different exams has independent question
 * sets and independent attempts.
 */
@Entity(
    tableName = "exam_subjects",
    foreignKeys = [
        ForeignKey(
            entity = Exam::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["examId", "subjectId"], unique = true),
        Index(value = ["examId"]),
        Index(value = ["subjectId"])
    ]
)
data class ExamSubject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val examId: Long,
    val subjectId: Long,
    val sortOrder: Int
)
