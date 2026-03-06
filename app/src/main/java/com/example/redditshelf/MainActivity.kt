package com.example.redditshelf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.redditshelf.data.local.AppStorage
import com.example.redditshelf.data.repository.RedditShelfRepository
import com.example.redditshelf.ui.MainScreen
import com.example.redditshelf.ui.MainViewModel
import com.example.redditshelf.ui.theme.RedditShelfTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(RedditShelfRepository(AppStorage(applicationContext)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            RedditShelfTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        viewModel = viewModel,
                        onOpenAuth = { url ->
                            CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse(url))
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.dataString
        if (!data.isNullOrBlank()) {
            viewModel.handleAuthRedirect(data)
        }
    }
}
