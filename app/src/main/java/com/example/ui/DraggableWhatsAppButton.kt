package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun DraggableWhatsAppButton(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val companyWhatsAppNumbers by viewModel.companyWhatsAppNumbers.collectAsState()
    val idleOpacity by viewModel.whatsAppIdleOpacity.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    
    // We want to track if the button is active/touched
    var isInteracting by remember { mutableStateOf(false) }
    
    // Smoothly animate opacity changes
    val targetOpacity = if (isInteracting) 1.0f else idleOpacity
    val animatedOpacity = remember { Animatable(targetOpacity) }
    
    // Whenever targetOpacity or idleOpacity changes, animate it smoothly
    LaunchedEffect(targetOpacity, idleOpacity) {
        animatedOpacity.animateTo(
            targetValue = targetOpacity,
            animationSpec = tween(durationMillis = 250)
        )
    }
    
    // A timer to drop opacity back to idle state after 3 seconds of inactivity
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isInteracting) {
        if (isInteracting) return@LaunchedEffect
        while (true) {
            delay(500)
            if (System.currentTimeMillis() - lastInteractionTime > 3000) {
                isInteracting = false
            }
        }
    }
    
    // Screen dimensions and offsets
    var parentWidth by remember { mutableStateOf(0) }
    var parentHeight by remember { mutableStateOf(0) }
    var buttonSize by remember { mutableStateOf(0) }
    
    // The animated position
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    
    // Initial placement on screen load
    var initialized by remember { mutableStateOf(false) }
    
    val marginX = with(density) { 16.dp.toPx() }
    val marginY = with(density) { 80.dp.toPx() }
    
    LaunchedEffect(parentWidth, parentHeight, buttonSize) {
        if (parentWidth > 0 && parentHeight > 0 && buttonSize > 0 && !initialized) {
            // Initial positioning: 16dp from right and 80dp from bottom (above navigation bars/FABs)
            offsetX.snapTo(parentWidth - buttonSize - marginX)
            offsetY.snapTo(parentHeight - buttonSize - marginY)
            initialized = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                parentWidth = size.width
                parentHeight = size.height
            }
    ) {
        if (initialized) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                    .onSizeChanged { size ->
                        buttonSize = size.width
                    }
                    .size(56.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    // Visual Styling: Cyan background
                    .background(Color(0xFF00BCD4)) // Cyan / সায়ান
                    .alpha(animatedOpacity.value)
                    .pointerInput(Unit) {
                        var dragDistance = 0f
                        detectDragGestures(
                            onDragStart = {
                                dragDistance = 0f
                                isInteracting = true
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                isInteracting = true
                                lastInteractionTime = System.currentTimeMillis()
                                dragDistance += dragAmount.getDistance()
                                
                                coroutineScope.launch {
                                    val newX = (offsetX.value + dragAmount.x).coerceIn(0f, (parentWidth - buttonSize).toFloat())
                                    val newY = (offsetY.value + dragAmount.y).coerceIn(0f, (parentHeight - buttonSize).toFloat())
                                    offsetX.snapTo(newX)
                                    offsetY.snapTo(newY)
                                }
                            },
                            onDragEnd = {
                                isInteracting = false
                                lastInteractionTime = System.currentTimeMillis()
                                
                                if (dragDistance < 10f) {
                                    // Treat as Tap/Click
                                    val phoneToUse = companyWhatsAppNumbers.firstOrNull() ?: "+8801712553809"
                                    val formattedPhone = phoneToUse.replace(" ", "").replace("+", "").trim()
                                    val url = "https://api.whatsapp.com/send?phone=$formattedPhone"
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse(url)
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    // Snap to nearest horizontal edge with beautiful spring physics
                                    val leftEdge = marginX
                                    val rightEdge = (parentWidth - buttonSize).toFloat() - marginX
                                    val targetX = if (offsetX.value < parentWidth / 2f) leftEdge else rightEdge
                                    
                                    coroutineScope.launch {
                                        offsetX.animateTo(
                                            targetValue = targetX,
                                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
                                        )
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Official WhatsApp vector design or stylized White Icon
                Icon(
                    imageVector = Icons.Filled.Chat,
                    contentDescription = "WhatsApp Live Chat",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
