package com.exam.stateownedenterprise.data.seed

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.exam.stateownedenterprise.data.local.AppDatabase
import com.exam.stateownedenterprise.data.local.correctOptionsList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies [SeedImporter]'s two-pass exam-metadata / question-file import: idempotency
 * (unchanged files are skipped, changed files are re-imported), the Exam → Subject →
 * ExamSubject → Question hierarchy landing correctly in Room, Subject reuse across
 * different exams, and the fail-loudly behavior when a question file references an
 * exam/subject combination that hasn't been declared in exam metadata yet.
 */
@RunWith(AndroidJUnit4::class)
class SeedImporterInstrumentedTest {

    private lateinit var database: AppDatabase
    private lateinit var importer: SeedImporter

    private val examMetadataJson = """
        {
          "year": 113,
          "category": "人資",
          "examName": "經濟部所屬事業機構113年新進職員甄試",
          "subjects": [
            {"name": "企業管理", "sortOrder": 1},
            {"name": "法學緒論", "sortOrder": 2}
          ]
        }
    """.trimIndent()

    private val questionFileJson = """
        {
          "year": 113,
          "category": "人資",
          "subject": "企業管理",
          "questions": [
            {
              "questionNumber": 1,
              "stem": "測試題目一？",
              "options": { "A": "選項A", "B": "選項B", "C": "選項C", "D": "選項D" },
              "correctOptions": ["B"],
              "explanation": "解析一"
            },
            {
              "questionNumber": 2,
              "stem": "測試題目二？",
              "options": { "A": "選項A", "B": "選項B", "C": "選項C", "D": "選項D" },
              "correctOptions": ["A"],
              "explanation": "解析二"
            }
          ]
        }
    """.trimIndent()

    private val questionFileJsonWithNewQuestion = """
        {
          "year": 113,
          "category": "人資",
          "subject": "企業管理",
          "questions": [
            {
              "questionNumber": 1,
              "stem": "測試題目一？",
              "options": { "A": "選項A", "B": "選項B", "C": "選項C", "D": "選項D" },
              "correctOptions": ["B"],
              "explanation": "解析一"
            },
            {
              "questionNumber": 2,
              "stem": "測試題目二？",
              "options": { "A": "選項A", "B": "選項B", "C": "選項C", "D": "選項D" },
              "correctOptions": ["A"],
              "explanation": "解析二"
            },
            {
              "questionNumber": 3,
              "stem": "測試題目三？",
              "options": { "A": "選項A", "B": "選項B", "C": "選項C", "D": "選項D" },
              "correctOptions": ["C"]
            }
          ]
        }
    """.trimIndent()

    private val multiAnswerAndBonusQuestionFileJson = """
        {
          "year": 113,
          "category": "人資",
          "subject": "企業管理",
          "questions": [
            {
              "questionNumber": 5,
              "stem": "測試題目五（雙重正解）？",
              "options": { "A": "選項A", "B": "選項B", "C": "選項C", "D": "選項D" },
              "correctOptions": ["B", "D"]
            },
            {
              "questionNumber": 35,
              "stem": "本題一律送分。",
              "options": { "A": "選項A", "B": "選項B", "C": "選項C", "D": "選項D" },
              "correctOptions": [],
              "isBonus": true
            }
          ]
        }
    """.trimIndent()

    /** A second exam (different year+category) that also declares 企業管理, to test Subject reuse. */
    private val secondExamMetadataJson = """
        {
          "year": 112,
          "category": "土木",
          "examName": "112年測試甄試（土木）",
          "subjects": [
            {"name": "企業管理", "sortOrder": 3}
          ]
        }
    """.trimIndent()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        importer = SeedImporter(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun importExamMetadata_insertsExamSubjectsAndSubjects() = runBlocking {
        importer.importExamMetadata("exam.json", examMetadataJson)

        val exam = database.examDao().findByYearAndCategory(113, "人資")
        assertEquals(true, exam != null)

        val subjects = database.examSubjectDao().observeSubjectsForExam(113, "人資").first()
        assertEquals(listOf("企業管理", "法學緒論"), subjects.map { it.name })
    }

    @Test
    fun importQuestionFile_insertsQuestionsUnderResolvedExamSubject() = runBlocking {
        importer.importExamMetadata("exam.json", examMetadataJson)
        importer.importQuestionFile("questions.json", questionFileJson)

        val exam = database.examDao().findByYearAndCategory(113, "人資")!!
        val subject = database.subjectDao().findByName("企業管理")!!
        val examSubject = database.examSubjectDao().findByExamAndSubject(exam.id, subject.id)!!

        val questionCount = database.questionDao().countByExamSubject(examSubject.id)
        assertEquals(2, questionCount)
    }

    @Test
    fun importQuestionFile_isSkippedWhenContentUnchanged() = runBlocking {
        importer.importExamMetadata("exam.json", examMetadataJson)
        importer.importQuestionFile("questions.json", questionFileJson)
        importer.importQuestionFile("questions.json", questionFileJson) // re-import identical content

        val exam = database.examDao().findByYearAndCategory(113, "人資")!!
        val subject = database.subjectDao().findByName("企業管理")!!
        val examSubject = database.examSubjectDao().findByExamAndSubject(exam.id, subject.id)!!
        val questionCount = database.questionDao().countByExamSubject(examSubject.id)

        // Would be 4 if re-imported (duplicate insert), must stay 2 because the hash matched.
        assertEquals(2, questionCount)
    }

    @Test
    fun importQuestionFile_reimportsWhenContentChanged_pickingUpNewQuestions() = runBlocking {
        importer.importExamMetadata("exam.json", examMetadataJson)
        importer.importQuestionFile("questions.json", questionFileJson)

        // Edited file (question 3 appended): hash differs, so it must NOT be skipped.
        importer.importQuestionFile("questions.json", questionFileJsonWithNewQuestion)

        val exam = database.examDao().findByYearAndCategory(113, "人資")!!
        val subject = database.subjectDao().findByName("企業管理")!!
        val examSubject = database.examSubjectDao().findByExamAndSubject(exam.id, subject.id)!!
        val questionCount = database.questionDao().countByExamSubject(examSubject.id)

        // 3, not 5: the changed file was processed (not skipped), the new question was added,
        // and the two pre-existing questions were not duplicated.
        assertEquals(3, questionCount)
    }

    @Test
    fun importQuestionFile_persistsMultipleCorrectOptionsAndBonusFlag() = runBlocking {
        importer.importExamMetadata("exam.json", examMetadataJson)
        importer.importQuestionFile("multi_and_bonus.json", multiAnswerAndBonusQuestionFileJson)

        val exam = database.examDao().findByYearAndCategory(113, "人資")!!
        val subject = database.subjectDao().findByName("企業管理")!!
        val examSubject = database.examSubjectDao().findByExamAndSubject(exam.id, subject.id)!!
        val questions = database.questionDao().observeByExamSubject(examSubject.id)
            .first()
            .associateBy { it.questionNumber }

        val multiAnswerQuestion = questions.getValue(5)
        assertEquals(listOf("B", "D"), multiAnswerQuestion.correctOptionsList())
        assertEquals(false, multiAnswerQuestion.isBonus)

        val bonusQuestion = questions.getValue(35)
        assertEquals(emptyList<String>(), bonusQuestion.correctOptionsList())
        assertEquals(true, bonusQuestion.isBonus)
    }

    @Test
    fun importExamMetadata_reusesSameSubjectAcrossDifferentExams() = runBlocking {
        importer.importExamMetadata("exam_113_人資.json", examMetadataJson)
        importer.importExamMetadata("exam_112_土木.json", secondExamMetadataJson)

        // Only one Subject row named 企業管理 should exist; it is shared by both exams.
        val subject = database.subjectDao().findByName("企業管理")!!
        val examsContaining = database.examSubjectDao().observeExamsContainingSubject("企業管理").first()
        assertEquals(2, examsContaining.size)

        val examA = database.examDao().findByYearAndCategory(113, "人資")!!
        val examB = database.examDao().findByYearAndCategory(112, "土木")!!
        val examSubjectA = database.examSubjectDao().findByExamAndSubject(examA.id, subject.id)!!
        val examSubjectB = database.examSubjectDao().findByExamAndSubject(examB.id, subject.id)!!

        // Same Subject, but distinct ExamSubject rows (independent question sets/attempts).
        assertEquals(subject.id, database.examSubjectDao().getById(examSubjectA.id)!!.subjectId)
        assertEquals(subject.id, database.examSubjectDao().getById(examSubjectB.id)!!.subjectId)
        assertEquals(true, examSubjectA.id != examSubjectB.id)
    }

    @Test
    fun importQuestionFile_failsLoudlyWhenExamSubjectIsMissing() = runBlocking {
        // No exam metadata imported: 113/人資/企業管理 has no corresponding ExamSubject yet.
        assertThrows(IllegalStateException::class.java) {
            runBlocking { importer.importQuestionFile("questions.json", questionFileJson) }
        }
    }
}
