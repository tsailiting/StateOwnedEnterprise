package com.exam.stateownedenterprise.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class AnswerRecordWithQuestion(
    @Embedded
    val answerRecord: AnswerRecord,

    @Relation(
        parentColumn = "questionId",
        entityColumn = "id"
    )
    val question: Question
)
