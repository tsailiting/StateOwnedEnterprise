package com.exam.stateownedenterprise.data.seed

import android.content.Context
import androidx.room.withTransaction
import com.exam.stateownedenterprise.data.local.AppDatabase
import com.exam.stateownedenterprise.data.local.Exam
import com.exam.stateownedenterprise.data.local.ExamSubject
import com.exam.stateownedenterprise.data.local.ImportMeta
import com.exam.stateownedenterprise.data.local.Question
import com.exam.stateownedenterprise.data.local.Subject
import kotlinx.serialization.json.Json

private const val EXAMS_ASSET_DIR = "seed/exams"
private const val QUESTIONS_ASSET_DIR = "seed/questions"

private val json = Json { ignoreUnknownKeys = true }

/**
 * Discovers JSON seed files under assets/seed/exams/ and assets/seed/questions/, and
 * imports them into Room in two passes: exam metadata (Exam → Subject → ExamSubject)
 * always runs before question content (Question), since a question file can only
 * resolve its ExamSubject if the corresponding exam metadata was already imported.
 * Each file is skipped if its content hash matches the last import (idempotent across
 * app launches), but re-imported if it was added or edited.
 */
class SeedImporter(private val database: AppDatabase) {

    /** Scans assets/seed/{exams,questions}/ and imports every *.json file found there. */
    suspend fun importFromAssets(context: Context) {
        listJsonFiles(context, EXAMS_ASSET_DIR).forEach { fileName ->
            val content = readAsset(context, EXAMS_ASSET_DIR, fileName)
            importExamMetadata(fileName, content)
        }
        listJsonFiles(context, QUESTIONS_ASSET_DIR).forEach { fileName ->
            val content = readAsset(context, QUESTIONS_ASSET_DIR, fileName)
            importQuestionFile(fileName, content)
        }
    }

    /**
     * Imports one exam-metadata file's JSON [content], identified by [fileName].
     * Upserts the [Exam], upserts each named [Subject] (globally, by name), and
     * upserts the [ExamSubject] linking them with the declared sortOrder. No-ops if
     * [fileName] was already imported with the same content hash.
     */
    suspend fun importExamMetadata(fileName: String, content: String) {
        val contentHash = sha256Hex(content)
        val previousImport = database.importMetaDao().findByFileName(fileName)
        if (previousImport?.contentHash == contentHash) return

        val examMetadata = json.decodeFromString<ExamMetadataSeedDto>(content)
        database.withTransaction {
            val examId = upsertExam(examMetadata)
            examMetadata.subjects.forEach { subjectSeed ->
                val subjectId = upsertSubject(subjectSeed.name)
                upsertExamSubject(examId, subjectId, subjectSeed.sortOrder)
            }
            recordImport(fileName, contentHash)
        }
    }

    /**
     * Imports one question file's JSON [content], identified by [fileName]. Resolves
     * the owning [ExamSubject] via (year, category) + subject name; throws if the
     * corresponding exam metadata hasn't been imported yet. No-ops if [fileName] was
     * already imported with the same content hash.
     */
    suspend fun importQuestionFile(fileName: String, content: String) {
        val contentHash = sha256Hex(content)
        val previousImport = database.importMetaDao().findByFileName(fileName)
        if (previousImport?.contentHash == contentHash) return

        val questionFile = json.decodeFromString<QuestionFileSeedDto>(content)
        database.withTransaction {
            val exam = database.examDao().findByYearAndCategory(questionFile.year, questionFile.category)
                ?: error(
                    "No Exam found for year=${questionFile.year}, category=${questionFile.category} " +
                        "while importing $fileName. Import its exam-metadata file first."
                )
            val subject = database.subjectDao().findByName(questionFile.subject)
                ?: error("No Subject named \"${questionFile.subject}\" while importing $fileName.")
            val examSubject = database.examSubjectDao().findByExamAndSubject(exam.id, subject.id)
                ?: error(
                    "Exam (year=${questionFile.year}, category=${questionFile.category}) does not " +
                        "declare subject \"${questionFile.subject}\" while importing $fileName. " +
                        "Add it to the exam-metadata file first."
                )

            val questions = questionFile.questions.map { it.toEntity(examSubject.id) }
            database.questionDao().insertAll(questions)
            recordImport(fileName, contentHash)
        }
    }

    private suspend fun recordImport(fileName: String, contentHash: String) {
        database.importMetaDao().upsert(
            ImportMeta(fileName = fileName, contentHash = contentHash, importedAt = System.currentTimeMillis())
        )
    }

    private suspend fun upsertExam(dto: ExamMetadataSeedDto): Long {
        val existing = database.examDao()
            .findByYearCategoryName(dto.year, dto.category, dto.examName)
        if (existing != null) return existing.id
        return database.examDao().insert(
            Exam(year = dto.year, category = dto.category, examName = dto.examName)
        )
    }

    private suspend fun upsertSubject(name: String): Long {
        val existing = database.subjectDao().findByName(name)
        if (existing != null) return existing.id
        return database.subjectDao().insert(Subject(name = name))
    }

    private suspend fun upsertExamSubject(examId: Long, subjectId: Long, sortOrder: Int): Long {
        val existing = database.examSubjectDao().findByExamAndSubject(examId, subjectId)
        if (existing == null) {
            return database.examSubjectDao().insert(
                ExamSubject(examId = examId, subjectId = subjectId, sortOrder = sortOrder)
            )
        }
        if (existing.sortOrder != sortOrder) {
            database.examSubjectDao().update(existing.copy(sortOrder = sortOrder))
        }
        return existing.id
    }

    private fun QuestionSeedDto.toEntity(examSubjectId: Long) = Question(
        examSubjectId = examSubjectId,
        questionNumber = questionNumber,
        stem = stem,
        optionA = options.A,
        optionB = options.B,
        optionC = options.C,
        optionD = options.D,
        correctOptions = correctOptions.sorted().joinToString(separator = ","),
        isBonus = isBonus,
        explanation = explanation
    )

    private fun listJsonFiles(context: Context, dir: String): List<String> =
        context.assets.list(dir)?.filter { it.endsWith(".json") }.orEmpty()

    private fun readAsset(context: Context, dir: String, fileName: String): String =
        context.assets.open("$dir/$fileName").bufferedReader().use { it.readText() }
}
