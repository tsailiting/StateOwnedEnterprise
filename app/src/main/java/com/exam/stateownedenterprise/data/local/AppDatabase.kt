package com.exam.stateownedenterprise.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Exam::class,
        Subject::class,
        ExamSubject::class,
        Question::class,
        Attempt::class,
        AnswerRecord::class,
        ImportMeta::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun examDao(): ExamDao
    abstract fun subjectDao(): SubjectDao
    abstract fun examSubjectDao(): ExamSubjectDao
    abstract fun questionDao(): QuestionDao
    abstract fun attemptDao(): AttemptDao
    abstract fun answerRecordDao(): AnswerRecordDao
    abstract fun importMetaDao(): ImportMetaDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "state_owned_enterprise.db"
                )
                    // No production data yet: wipe & recreate on schema change instead of
                    // writing real Migrations. Replace with proper Migration(s) once the
                    // app has real users.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
