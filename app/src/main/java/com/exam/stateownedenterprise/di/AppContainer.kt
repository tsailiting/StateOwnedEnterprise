package com.exam.stateownedenterprise.di

import android.content.Context
import com.exam.stateownedenterprise.data.local.AppDatabase
import com.exam.stateownedenterprise.data.repository.DefaultQuestionRepository
import com.exam.stateownedenterprise.data.repository.QuestionRepository
import com.exam.stateownedenterprise.data.seed.SeedImporter

interface AppContainer {
    val questionRepository: QuestionRepository
    val seedImporter: SeedImporter
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    override val questionRepository: QuestionRepository by lazy {
        DefaultQuestionRepository(
            examDao = database.examDao(),
            examSubjectDao = database.examSubjectDao(),
            questionDao = database.questionDao(),
            attemptDao = database.attemptDao(),
            answerRecordDao = database.answerRecordDao()
        )
    }

    override val seedImporter: SeedImporter by lazy {
        SeedImporter(database)
    }
}
