package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.RootAppNavigation
import com.example.ui.theme.SucharuTheme
import kotlinx.coroutines.delay

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("MainActivity", "Starting application...")
        enableEdgeToEdge()
        
        val app = application as SucharuApp
        
        setContent {
            com.example.util.ErrorBoundary {
                SucharuTheme {
                    var initError by remember { mutableStateOf<Throwable?>(null) }
                    var initializedViewModel by remember { mutableStateOf<MainViewModel?>(null) }

                    // Immediate UI Mounting logic
                    LaunchedEffect(Unit) {
                        try {
                            if (app.repository == null || app.settingsManager == null) {
                                throw RuntimeException("Critical dependencies (Database/Settings) failed to initialize. The local storage might be corrupt.")
                            }
                            // Attempt to create ViewModel
                            val vm = MainViewModel(app.repository!!, app.settingsManager!!)
                            initializedViewModel = vm
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Initialization failed", e)
                            initError = e
                        }
                    }

                    if (initError != null) {
                        com.example.util.SafeLaunchScreen(
                            error = initError!!,
                            onRestart = {
                                finish()
                                startActivity(intent)
                            },
                            onClearCache = {
                                android.widget.Toast.makeText(this@MainActivity, "Clearing cache...", android.widget.Toast.LENGTH_SHORT).show()
                                com.example.util.clearAppData(this@MainActivity)
                                val intent = packageManager.getLaunchIntentForPackage(packageName)
                                intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                finish()
                                startActivity(intent)
                            }
                        )
                    } else if (initializedViewModel != null) {
                        val viewModel = initializedViewModel!!
                        val themeName by viewModel.themeName.collectAsState()
                        
                        SucharuTheme(themeName = themeName) {
                            var showSplash by remember { mutableStateOf(true) }
                            var isOnboardingCompleted by remember { mutableStateOf(false) }
                            
                            LaunchedEffect(Unit) {
                                try {
                                    android.util.Log.d("MainActivity", "Performing async init...")
                                    isOnboardingCompleted = viewModel.isOnboardingCompleted()
                                    delay(1000)
                                    showSplash = false
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Splash init failed", e)
                                    showSplash = false // Proceed anyway
                                }
                            }
                            
                            if (showSplash) {
                                SplashScreen()
                            } else {
                                if (isOnboardingCompleted) {
                                    AuthenticatedApp(viewModel)
                                } else {
                                    com.example.ui.OnboardingScreen(onPermissionsGranted = {
                                        viewModel.setOnboardingCompleted(true)
                                        isOnboardingCompleted = true
                                    })
                                }
                            }
                        }
                    } else {
                        // Show splash immediately while initializing
                        SplashScreen()
                    }
                }
            }
        }
    }

    @Composable
    private fun SplashScreen() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0088CC)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.printing_logo_1784376787420),
                contentDescription = "App Logo",
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Fit
            )
        }
    }

    @Composable
    private fun AuthenticatedApp(viewModel: MainViewModel) {
        var isAuthenticated by remember { mutableStateOf(false) }
        
        if (isAuthenticated) {
            RootAppNavigation(viewModel = viewModel)
        } else {
            LaunchedEffect(Unit) {
                try {
                    if (com.example.util.BiometricAuthManager.canAuthenticate(this@MainActivity)) {
                        com.example.util.BiometricAuthManager.authenticate(
                            activity = this@MainActivity,
                            title = "Authenticate",
                            subtitle = "Use biometric to access the app",
                            onSuccess = { isAuthenticated = true },
                            onError = { _, _ -> /* Handle error */ },
                            onFailed = { /* Handle failed */ }
                        )
                    } else {
                        isAuthenticated = true
                    }
                } catch (e: Exception) {
                    isAuthenticated = true // Fallback on auth failure
                }
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = { 
                    com.example.util.BiometricAuthManager.authenticate(
                        activity = this@MainActivity,
                        title = "Authenticate",
                        subtitle = "Use biometric to access the app",
                        onSuccess = { isAuthenticated = true },
                        onError = { _, _ -> },
                        onFailed = { }
                    )
                }) {
                    Text("Authenticate")
                }
            }
        }
    }
}
