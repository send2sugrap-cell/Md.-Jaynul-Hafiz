package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import com.example.api.*
import com.example.BuildConfig
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(onBack: () -> Unit) {
    var messages by remember { mutableStateOf(listOf(Content(listOf(Part(text = "Hello! I am your AI assistant. How can I help you?")), role = "model"))) }
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf("Flash") } // Flash, Pro, Lite
    var isVoiceMode by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val liveApiClient = remember {
        LiveApiClient(BuildConfig.GEMINI_API_KEY) { status ->
            scope.launch {
                messages = messages + Content(listOf(Part(text = "Voice Status: $status")), role = "model")
            }
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                isRecording = true
                liveApiClient.connect()
                liveApiClient.startRecording()
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            liveApiClient.disconnect()
        }
    }

    val models = listOf("Flash", "Pro", "Lite")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("AI Assistant", color = MaterialTheme.colorScheme.onSurface) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            navigationIcon = {
                // Back button
                IconButton(onClick = onBack) {
                    Text("<", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            actions = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    models.forEach { model ->
                        FilterChip(
                            selected = selectedModel == model,
                            onClick = { selectedModel = model },
                            label = { Text(model) },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                            .widthIn(max = 300.dp)
                    ) {
                        Text(
                            text = msg.parts.firstOrNull()?.text ?: "",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray
                ),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (isRecording) {
                        isRecording = false
                        liveApiClient.stopRecording()
                    } else {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            isRecording = true
                            liveApiClient.connect()
                            liveApiClient.startRecording()
                        } else {
                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                modifier = Modifier.background(if (isRecording) Color.Red else Color(0xFF10B981), CircleShape)
            ) {
                Icon(if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = "Voice", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (input.isNotBlank()) {
                        val userText = input
                        val userMsg = Content(listOf(Part(text = userText)), role = "user")
                        messages = messages + userMsg
                        input = ""
                        isLoading = true

                        scope.launch {
                            try {
                                val apiKey = BuildConfig.GEMINI_API_KEY
                                val tools = buildJsonObject { 
                                    put("googleMaps", buildJsonObject {}) // Use googleMaps to get grounded data
                                }
                                val req = GenerateContentRequest(
                                    contents = messages,
                                    systemInstruction = Content(listOf(Part("You are a helpful and polite context-aware assistant for a Printing Business."))),
                                    tools = listOf(tools)
                                )
                                
                                val res = when (selectedModel) {
                                    "Pro" -> RetrofitClient.service.generateContentPro(apiKey, req)
                                    "Lite" -> RetrofitClient.service.generateContentLite(apiKey, req)
                                    else -> RetrofitClient.service.generateContent(apiKey, req) // default Flash
                                }
                                
                                val responseText = res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response"
                                messages = messages + Content(listOf(Part(text = responseText)), role = "model")
                            } catch (e: Exception) {
                                messages = messages + Content(listOf(Part(text = "Error: ${e.message}")), role = "model")
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
