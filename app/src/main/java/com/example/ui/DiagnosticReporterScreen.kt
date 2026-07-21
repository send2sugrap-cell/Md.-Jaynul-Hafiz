package com.example.ui

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.ErrorLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticReporterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val logs = remember { ErrorLogger.getLogs(context) }
    val lastLog = logs.firstOrNull()

    val hardwareInfo = remember {
        mapOf(
            "Model" to Build.MODEL,
            "Manufacturer" to Build.MANUFACTURER,
            "Android Version" to Build.VERSION.RELEASE,
            "SDK Level" to Build.VERSION.SDK_INT.toString(),
            "Board" to Build.BOARD,
            "Hardware" to Build.HARDWARE,
            "Fingerprint" to Build.FINGERPRINT
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostic Reporter") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Hardware Information", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        hardwareInfo.forEach { (key, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = key, style = MaterialTheme.typography.labelLarge)
                                Text(text = value, style = MaterialTheme.typography.bodyMedium)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }

            item {
                Text("Last Recorded Error", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                if (lastLog != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Timestamp: ${lastLog.timestamp}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lastLog.message,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = lastLog.stackTrace,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                } else {
                    Text("No error logs recorded.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
