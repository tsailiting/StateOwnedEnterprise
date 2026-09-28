package com.exam.stateownedenterprise.data.seed

import com.exam.stateownedenterprise.data.local.Question
import com.exam.stateownedenterprise.data.local.correctOptionsList
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SeedImporterParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `correctOptionsList splits a single letter`() {
        val question = sampleQuestion(correctOptions = "B")
        assertEquals(listOf("B"), question.correctOptionsList())
    }

    @Test
    fun `correctOptionsList splits multiple letters`() {
        val question = sampleQuestion(correctOptions = "B,D")
        assertEquals(listOf("B", "D"), question.correctOptionsList())
    }

    @Test
    fun `correctOptionsList is empty for a bonus question`() {
        val question = sampleQuestion(correctOptions = "", isBonus = true)
        assertEquals(emptyList<String>(), question.correctOptionsList())
    }

    private fun sampleQuestion(correctOptions: String, isBonus: Boolean = false) = Question(
        examSubjectId = 1,
        questionNumber = 1,
        stem = "stem",
        optionA = "A",
        optionB = "B",
        optionC = "C",
        optionD = "D",
        correctOptions = correctOptions,
        isBonus = isBonus
    )

    @Test
    fun `sha256Hex is deterministic for identical content`() {
        val content = "{\"year\":113}"
        assertEquals(sha256Hex(content), sha256Hex(content))
    }

    @Test
    fun `sha256Hex changes when content changes`() {
        val hashA = sha256Hex("{\"year\":113}")
        val hashB = sha256Hex("{\"year\":114}")
        assertNotEquals(hashA, hashB)
    }

    @Test
    fun `decodes exam metadata json into dto`() {
        val content = """
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

        val dto = json.decodeFromString<ExamMetadataSeedDto>(content)

        assertEquals(113, dto.year)
        assertEquals("人資", dto.category)
        assertEquals("經濟部所屬事業機構113年新進職員甄試", dto.examName)
        assertEquals(2, dto.subjects.size)
        assertEquals("企業管理", dto.subjects[0].name)
        assertEquals(1, dto.subjects[0].sortOrder)
        assertEquals("法學緒論", dto.subjects[1].name)
        assertEquals(2, dto.subjects[1].sortOrder)
    }

    @Test
    fun `decodes question file json into dto`() {
        val content = """
            {
              "year": 113,
              "category": "人資",
              "subject": "企業管理",
              "questions": [
                {
                  "questionNumber": 1,
                  "stem": "測試題目？",
                  "options": { "A": "選項A", "B": "選項B", "C": "選項C", "D": "選項D" },
                  "correctOptions": ["B"],
                  "explanation": "測試解析"
                }
              ]
            }
        """.trimIndent()

        val dto = json.decodeFromString<QuestionFileSeedDto>(content)

        assertEquals(113, dto.year)
        assertEquals("人資", dto.category)
        assertEquals("企業管理", dto.subject)
        val question = dto.questions.single()
        assertEquals(1, question.questionNumber)
        assertEquals("選項B", question.options.B)
        assertEquals(listOf("B"), question.correctOptions)
        assertEquals(false, question.isBonus)
    }

    @Test
    fun `decodes multiple correct options`() {
        val content = """
            {
              "year": 113,
              "category": "人資",
              "subject": "企業管理",
              "questions": [
                {
                  "questionNumber": 5,
                  "stem": "測試題目（多重正解）？",
                  "options": { "A": "選項A", "B": "選項B", "C": "選項C", "D": "選項D" },
                  "correctOptions": ["B", "D"]
                }
              ]
            }
        """.trimIndent()

        val dto = json.decodeFromString<QuestionFileSeedDto>(content)

        assertEquals(listOf("B", "D"), dto.questions.single().correctOptions)
    }

    @Test
    fun `decodes bonus question with empty correctOptions`() {
        val content = """
            {
              "year": 113,
              "category": "人資",
              "subject": "企業管理",
              "questions": [
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

        val dto = json.decodeFromString<QuestionFileSeedDto>(content)
        val question = dto.questions.single()

        assertEquals(true, question.isBonus)
        assertEquals(emptyList<String>(), question.correctOptions)
    }

    @Test
    fun `explanation defaults to null when absent`() {
        val content = """
            {
              "year": 113,
              "category": "人資",
              "subject": "企業管理",
              "questions": [
                {
                  "questionNumber": 1,
                  "stem": "測試題目？",
                  "options": { "A": "選項A", "B": "選項B", "C": "選項C", "D": "選項D" },
                  "correctOptions": ["A"]
                }
              ]
            }
        """.trimIndent()

        val dto = json.decodeFromString<QuestionFileSeedDto>(content)

        assertEquals(null, dto.questions.single().explanation)
    }
}
