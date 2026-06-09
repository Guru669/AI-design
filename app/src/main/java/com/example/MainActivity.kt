package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.AppRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DesignViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialise Room Database singleton
        val database = AppDatabase.getDatabase(this)
        
        // 2. Initialise Unified Repository
        val repository = AppRepository(database)
        
        // 3. Initialise DesignViewModel via custom factory
        val factory = DesignViewModel.provideFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[DesignViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Reactive check on User login session (Module 1)
                val loggedInUser by viewModel.currentSessionUser.collectAsState()

                AnimatedContent(
                    targetState = loggedInUser != null,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "auth_routing_transition"
                ) { isAuthenticated ->
                    if (isAuthenticated) {
                        DashboardScreen(viewModel = viewModel)
                    } else {
                        AuthScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

