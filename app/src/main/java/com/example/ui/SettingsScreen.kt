package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Close
import com.example.util.FileUtils
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    isAdmin: Boolean = true,
    onLogout: () -> Unit = {},
    onNavigateToActivityLog: () -> Unit = {},
    onNavigateToDeveloperSettings: () -> Unit = {}
) {
    val companyWhatsAppNumbers by viewModel.companyWhatsAppNumbers.collectAsState()
    var newNumber by remember { mutableStateOf("") }
    val themeName by viewModel.themeName.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings & Preferences",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Theme Selector
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Theme Selector", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf("ROYAL_BLUE" to "Royal Blue & Cyan", "DARK" to "Dark Mode", "HIGH_CONTRAST" to "High Contrast").forEach { (theme, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (theme == themeName),
                                    onClick = { viewModel.saveThemeName(theme) },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (theme == themeName),
                                onClick = null
                            )
                            Text(text = label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.resetToDefaultTheme() }) {
                        Text("Reset to Default")
                    }
                }
            }
        }
        
        // Utilities
        item {
            var cacheSize by remember { mutableStateOf(0L) }
            var logs by remember { mutableStateOf(com.example.util.ErrorLogger.getLogs(context)) }
            var showDiagnosticReport by remember { mutableStateOf(false) }
            
            fun calculateCacheSize(): Long {
                val filesDir = context.filesDir
                val cacheDir = context.cacheDir
                var size = 0L
                
                // Add error_log.json
                val logFile = File(filesDir, "error_log.json")
                if (logFile.exists()) size += logFile.length()
                
                // Add cache dir
                cacheDir.walkTopDown().forEach { if (it.isFile) size += it.length() }
                
                return size
            }
            
            fun clearCache() {
                // Clear logs
                val logFile = File(context.filesDir, "error_log.json")
                if (logFile.exists()) logFile.delete()
                
                // Clear cache dir
                context.cacheDir.deleteRecursively()
                
                cacheSize = calculateCacheSize()
                logs = com.example.util.ErrorLogger.getLogs(context)
                Toast.makeText(context, "Cache & Logs cleared!", Toast.LENGTH_SHORT).show()
            }
            
            LaunchedEffect(Unit) {
                cacheSize = calculateCacheSize()
            }
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Storage & Cache Manager", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Language: English (Century Gothic)")
                    Text("Backup: Auto-Backup Enabled")
                    Text("Total Storage Footprint: ${cacheSize / 1024} KB")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { clearCache() }, modifier = Modifier.fillMaxWidth()) { 
                        Text("Clear Temporary Files & Logs") 
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Diagnostic & Error Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (logs.isNotEmpty()) {
                        val latest = logs.first()
                        Text("Latest Crash: ${latest.dateString}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Cause: ${latest.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showDiagnosticReport = true }, modifier = Modifier.weight(1f)) {
                                Text("View")
                            }
                            OutlinedButton(
                                onClick = { 
                                    val report = com.example.util.ErrorLogger.generateDiagnosticReport(context)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Diagnostic Report", report))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Copy")
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val report = com.example.util.ErrorLogger.generateDiagnosticReport(context)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Diagnostic Report (.txt)")
                                        putExtra(android.content.Intent.EXTRA_TEXT, report)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Share Diagnostic Report"))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Share (.txt)")
                            }
                            OutlinedButton(
                                onClick = {
                                    val file = File(context.filesDir, "error_log.json")
                                    val jsonContent = if (file.exists()) file.readText() else "[]"
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Diagnostic Logs (.json)")
                                        putExtra(android.content.Intent.EXTRA_TEXT, jsonContent)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Share Diagnostic JSON"))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Share (.json)")
                            }
                        }
                    } else {
                        Text("No recent crashes found.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (showDiagnosticReport) {
                ErrorDebugPanel(
                    logs = logs,
                    onDismiss = { showDiagnosticReport = false }
                )
            }
        }

        // Paper Type Management
        if (isAdmin) {
            item {
                val paperTypes by viewModel.paperTypes.collectAsState()
                var newPaperName by remember { mutableStateOf("") }
                var newPaperPrice by remember { mutableStateOf("") }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Manage Paper Types",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = newPaperName,
                                    onValueChange = { newPaperName = it },
                                    label = { Text("Paper Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = newPaperPrice,
                                    onValueChange = { newPaperPrice = it },
                                    label = { Text("Base Price") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val price = newPaperPrice.toDoubleOrNull() ?: 0.0
                                    if (newPaperName.isNotBlank()) {
                                        viewModel.savePaperType(com.example.data.PaperType(name = newPaperName, basePrice = price))
                                        newPaperName = ""
                                        newPaperPrice = ""
                                    }
                                },
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add Paper Type")
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        paperTypes.forEach { type ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(type.name, fontWeight = FontWeight.Bold)
                                    Text("৳${type.basePrice}", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { viewModel.deletePaperType(type.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Company WhatsApp Numbers
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Company WhatsApp Numbers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (isAdmin) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newNumber,
                                onValueChange = { newNumber = it },
                                label = { Text("Add New Number") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newNumber.isNotBlank() && !companyWhatsAppNumbers.contains(newNumber.trim())) {
                                        viewModel.saveCompanyWhatsAppNumbers(companyWhatsAppNumbers + newNumber.trim())
                                        newNumber = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add Number")
                            }
                        }
                    }
                    companyWhatsAppNumbers.forEach { number ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(number)
                            if (isAdmin) {
                                IconButton(onClick = {
                                    viewModel.saveCompanyWhatsAppNumbers(companyWhatsAppNumbers.filter { it != number })
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Logout
        item {
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Logout")
            }
        }
    }
}

@Composable
fun ErrorDebugPanel(
    logs: List<com.example.util.ErrorEntry>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Error Debug Panel", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (logs.isEmpty()) {
                    Text("No logs available.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(logs) { log ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Time: ${log.dateString}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Text("App Version: ${log.appVersion} | OS: ${log.osVersion}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Text("Device: ${log.deviceModel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Text("Action: ${log.userAction}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Message:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Text(log.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    var showStack by remember { mutableStateOf(false) }
                                    Text(
                                        text = if (showStack) "Hide Stack Trace" else "Show Stack Trace", 
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { showStack = !showStack }.padding(vertical = 4.dp)
                                    )
                                    if (showStack) {
                                        Text(log.stackTrace, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
