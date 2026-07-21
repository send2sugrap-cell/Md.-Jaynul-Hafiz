package com.example.util

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import android.app.Activity
import android.content.Context
import android.content.Intent
import java.io.File

val LocalErrorHandler = compositionLocalOf<(Throwable) -> Unit> { { it.printStackTrace() } }

@Composable
fun ErrorBoundary(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<Throwable?>(null) }
    
    val errorHandler = remember {
        { t: Throwable ->
            ErrorLogger.logError(context, t)
            error = t
        }
    }
    
    if (error != null) {
        SafeLaunchScreen(
            error = error!!,
            onRestart = {
                val activity = context as? Activity
                activity?.let {
                    val intent = it.intent
                    it.finish()
                    it.startActivity(intent)
                }
            },
            onClearCache = {
                clearAppData(context)
                val activity = context as? Activity
                activity?.let {
                    it.finish()
                    val intent = it.packageManager.getLaunchIntentForPackage(it.packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    it.startActivity(intent)
                }
            }
        )
    } else {
        CompositionLocalProvider(LocalErrorHandler provides errorHandler) {
            content()
        }
    }
}

@Composable
fun SafeLaunchScreen(
    error: Throwable,
    onRestart: () -> Unit,
    onClearCache: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Safe Launch Mode",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "The application encountered an unexpected error and could not start normally.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Error Details:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = error.localizedMessage ?: "Unknown error",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Restart App")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = onClearCache,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Clear Local Cache & Reset")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Clearing cache will delete all local data but may resolve startup issues.",
                style = MaterialTheme.typography.labelSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

fun clearAppData(context: Context) {
    try {
        // Clear databases
        val databasesDir = File(context.applicationInfo.dataDir, "databases")
        if (databasesDir.exists()) {
            databasesDir.listFiles()?.forEach { it.delete() }
        }
        
        // Clear shared preferences
        val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        if (sharedPrefsDir.exists()) {
            sharedPrefsDir.listFiles()?.forEach { it.delete() }
        }
        
        // Clear cache
        context.cacheDir.deleteRecursively()
        context.externalCacheDir?.deleteRecursively()
        
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
