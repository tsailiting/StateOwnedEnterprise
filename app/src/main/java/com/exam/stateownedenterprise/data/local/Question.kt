package com.exam.stateownedenterprise.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single multiple-choice question belonging to an [ExamSubject] (a specific subject
 * paper within a specific exam). [questionNumber] is the question's position within
 * that paper (1, 2, 3…).
 * [correctOptions] holds one or more accepted letters ("A".."D"), comma-separated and
 * sorted, e.g. "B" for a normal question or "B,D" when multiple answers are accepted.
 * Use [correctOptionsList] to read it back as a list. [isBonus] marks a question that
 * is credited as correct for every candidate regardless of what they selected
 * (e.g. an official "一律送分" bonus question).
 */
@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = ExamSubject::class,
            parentColumns = ["id"],
            childColumns = ["examSubjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["examSubjectId", "questionNumber"], unique = true),
        Index(value = ["examSubjectId"])
    ]
)
data class Question(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val examSubjectId: Long,
    val questionNumber: Int,
    val stem: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOptions: String,
    val isBonus: Boolean = false,
    val explanation: String? = null
)

/** Splits [Question.correctOptions] (e.g. "B,D") back into individual letters. */
fun Question.correctOptionsList(): List<String> =
    correctOptions.split(",").map { it.trim() }.filter { it.isNotEmpty() }
