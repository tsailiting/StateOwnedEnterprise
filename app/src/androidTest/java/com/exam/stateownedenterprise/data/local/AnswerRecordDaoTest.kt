package com.exam.stateownedenterprise.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnswerRecordDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var answerRecordDao: AnswerRecordDao
    private lateinit var attemptDao: AttemptDao
    private lateinit var examDao: ExamDao
    private lateinit var subjectDao: SubjectDao
    private lateinit var examSubjectDao: ExamSubjectDao
    private lateinit var questionDao: QuestionDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        answerRecordDao = database.answerRecordDao()
        attemptDao = database.attemptDao()
        examDao = database.examDao()
        subjectDao = database.subjectDao()
        examSubjectDao = database.examSubjectDao()
        questionDao = database.questionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertDuplicateAttemptAndQuestion_throwsSQLiteConstraintException() = runBlocking {
        val examId = examDao.insert(Exam(year = 113, category = "人資", examName = "113人資"))
        val subjectId = subjectDao.insert(Subject(name = "企業管理"))
        val examSubjectId = examSubjectDao.insert(ExamSubject(examId = examId, subjectId = subjectId, sortOrder = 1))
        val questionId = questionDao.insertAll(
            listOf(
                Question(
                    examSubjectId = examSubjectId,
                    questionNumber = 1,
                    stem = "stem",
                    optionA = "A",
                    optionB = "B",
                    optionC = "C",
                    optionD = "D",
                    correctOptions = "B"
                )
            )
        ).single()

        val attemptId = attemptDao.insert(Attempt(examSubjectId = examSubjectId, startedAt = 1000L))

        val record1 = AnswerRecord(
            attemptId = attemptId,
            questionId = questionId,
            selectedOption = "B",
            isCorrect = true,
            answeredAt = 1000L
        )

        val record2 = AnswerRecord(
            attemptId = attemptId,
            questionId = questionId,
            selectedOption = "A",
            isCorrect = false,
            answeredAt = 2000L
        )

        answerRecordDao.insert(record1)

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                answerRecordDao.insert(record2)
            }
        }
    }
}
