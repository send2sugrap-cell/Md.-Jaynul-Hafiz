package com.example.api

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.putJsonArray
import java.util.concurrent.TimeUnit

class LiveApiClient(private val apiKey: String, private val onMessage: (String) -> Unit) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
        
    private var webSocket: WebSocket? = null
    private var isRecording = false
    private var audioRecord: AudioRecord? = null

    fun connect() {
        val request = Request.Builder()
            .url("wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey")
            .build()
            
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onMessage("Connected to Live API")
                sendSetupMessage()
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                // Parse server message (simplification)
                if (text.contains("serverContent")) {
                    onMessage("Received data from Gemini Live")
                }
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onMessage("Disconnected: \$reason")
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onMessage("Error: \${t.message}")
            }
        })
    }
    
    private fun sendSetupMessage() {
        val setupMsg = buildJsonObject { 
            putJsonObject("setup") {
                put("model", "models/gemini-3.1-flash-live-preview")
            }
        }
        webSocket?.send(setupMsg.toString())
    }

    fun startRecording() {
        if (isRecording) return
        isRecording = true
        
        val bufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            
            audioRecord?.startRecording()
            
            GlobalScope.launch(Dispatchers.IO) {
                val audioBuffer = ByteArray(bufferSize)
                while (isRecording) {
                    val readResult = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                    if (readResult > 0) {
                        val base64Audio = Base64.encodeToString(audioBuffer, 0, readResult, Base64.NO_WRAP)
                        val msg = buildJsonObject {
                            putJsonObject("clientContent") {
                                putJsonArray("turns") {
                                    add(buildJsonObject { 
                                        put("role", "user")
                                        putJsonArray("parts") {
                                            add(buildJsonObject { 
                                                putJsonObject("inlineData") {
                                                    put("mimeType", "audio/pcm;rate=16000")
                                                    put("data", base64Audio)
                                                }
                                            })
                                        }
                                    })
                                }
                                put("turnComplete", true)
                            }
                        }
                        webSocket?.send(msg.toString())
                    }
                }
            }
        } catch (e: SecurityException) {
            onMessage("Microphone permission denied")
        }
    }
    
    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
    
    fun disconnect() {
        stopRecording()
        webSocket?.close(1000, "User disconnected")
    }
}
