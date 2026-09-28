package com.exam.stateownedenterprise.data.seed

import kotlinx.serialization.Serializable

/**
 * Deserialization target for one exam-metadata file under assets/seed/exams/.
 * Declares an [com.exam.stateownedenterprise.data.local.Exam] and the subjects it
 * contains (name + display order). Does not carry any question content — see
 * [QuestionFileSeedDto] for that.
 */
@Serializable
data class ExamMetadataSeedDto(
    val year: Int,
    val category: String,
    val examName: String,
    val subjects: List<ExamSubjectSeedDto>
)

@Serializable
data class ExamSubjectSeedDto(
    val name: String,
    val sortOrder: Int
)

/**
 * Deserialization target for one question file under assets/seed/questions/.
 * Identifies its exam+subject purely via JSON content (year, category, subject name),
 * never via the file name, so the same subject can be reused freely across exams.
 */
@Serializable
data class QuestionFileSeedDto(
    val year: Int,
    val category: String,
    val subject: String,
    val questions: List<QuestionSeedDto>
)

@Serializable
data class QuestionSeedDto(
    val questionNumber: Int,
    val stem: String,
    val options: QuestionOptionsDto,
    val correctOptions: List<String>,
    val isBonus: Boolean = false,
    val explanation: String? = null
)

@Serializable
data class QuestionOptionsDto(
    val A: String,
    val B: String,
    val C: String,
    val D: String
)
