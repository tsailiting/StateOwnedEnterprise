package com.exam.stateownedenterprise

import android.app.Application
import com.exam.stateownedenterprise.di.AppContainer
import com.exam.stateownedenterprise.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StateOwnedEnterpriseApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        applicationScope.launch {
            container.seedImporter.importFromAssets(this@StateOwnedEnterpriseApplication)
        }
    }
}
