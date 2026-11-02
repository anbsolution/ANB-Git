package com.anb.git

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.anb.git.data.LocalRepositoryStorage
import com.anb.git.ui.AnbGitApp
import com.anb.git.ui.theme.AnbGitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Establish the app-private repository root. Phase 2 adds repository operations.
        LocalRepositoryStorage.initialize(applicationContext)

        setContent {
            AnbGitTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AnbGitApp()
                }
            }
        }
    }
}
