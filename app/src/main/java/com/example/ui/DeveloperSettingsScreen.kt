package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.util.ErrorEntry
import kotlinx.serialization.json.Json
import java.io.File

@Composable
fun DeveloperSettingsScreen(
    onBack: () -> Unit,
    onNavigateToDiagnosticReporter: () -> Unit = {}
) {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(emptyList<ErrorEntry>()) }
    
    LaunchedEffect(Unit) {
        val file = File(context.filesDir, "error_log.json")
        if (file.exists()) {
            try {
                val json = Json { ignoreUnknownKeys = true }
                logs = json.decodeFromString<List<ErrorEntry>>(file.readText())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Developer Settings - Error Logs", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(logs) { log ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Timestamp: ${log.timestamp}", style = MaterialTheme.typography.bodySmall)
                        Text("Message: ${log.message}", style = MaterialTheme.typography.bodyMedium)
                        Text("Stacktrace: ${log.stackTrace}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onNavigateToDiagnosticReporter, modifier = Modifier.fillMaxWidth()) {
            Text("Diagnostic Reporter")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = {
            val logText = logs.joinToString("\n\n") { log ->
                "Timestamp: ${log.timestamp}\nMessage: ${log.message}\nStacktrace: ${log.stackTrace}"
            }
            val file = File(context.cacheDir, "error_logs.txt")
            file.writeText(logText)
            
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share Logs"))
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Export Logs")
        }
    }
}
