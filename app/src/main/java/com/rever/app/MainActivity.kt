package com.rever.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.rever.app.navigation.ReverNavGraph
import com.rever.app.ui.theme.ReverTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val app = application as ReverApplication
        
        setContent {
            ReverTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val onboardingCompleted by app.userPreferencesRepository
                        .onboardingCompleted
                        .collectAsState(initial = false)
                    
                    // Check if user has existing subjects
                    val hasExistingSubjects by app.subjectRepository
                        .allSubjects
                        .map { subjects -> subjects.isNotEmpty() }
                        .collectAsState(initial = false)
                    
                    ReverNavGraph(
                        onboardingCompleted = onboardingCompleted,
                        hasExistingSubjects = hasExistingSubjects
                    )
                }
            }
        }
    }
}
