package com.exam.stateownedenterprise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.exam.stateownedenterprise.ui.navigation.AppNavHost
import com.exam.stateownedenterprise.ui.theme.StateOwnedEnterpriseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as StateOwnedEnterpriseApplication).container
        setContent {
            StateOwnedEnterpriseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AppNavHost(appContainer = appContainer)
                    }
                }
            }
        }
    }
}
