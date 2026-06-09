package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryOak
import com.example.ui.theme.SecondarySage
import com.example.ui.theme.TertiaryClay
import com.example.ui.viewmodel.ChatMessage
import com.example.ui.viewmodel.DesignViewModel
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantScreen(viewModel: DesignViewModel) {
    val chatHistory by viewModel.chatLogs.collectAsState()
    val activeQuery by viewModel.activeChatQuery.collectAsState()
    val isRecording by viewModel.isRecordingAudio.collectAsState()
    val isThinking by viewModel.isAssistantThinking.collectAsState()

    val chatListState = rememberLazyListState()

    // Auto-scroll to lowest bubble when elements change
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            chatListState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tab header
        Column {
            Text(
                text = "AURA VOICE ASSISTANT",
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryOak,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "AI Design Companion",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Conversation History frame
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
            border = BorderStroke(1.dp, PrimaryOak.copy(alpha = 0.08f))
        ) {
            LazyColumn(
                state = chatListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatHistory) { msg ->
                    ChatBubble(msg)
                }

                if (isThinking) {
                    item {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Aura is typing suggestions...",
                                style = MaterialTheme.typography.labelMedium,
                                color = PrimaryOak.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // Animated Vocal Waveform block when Recording
        AnimatedVisibility(
            visible = isRecording,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryOak.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Listening to voice command... Speak now",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryOak,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    GlowingVocalWaveform()
                }
            }
        }

        // Vocal Mic CTA + Keyboard textbox console
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Voice Continuous recording MIC
            FilledIconButton(
                onClick = { viewModel.toggleRecordingVoice() },
                modifier = Modifier
                    .size(56.dp)
                    .testTag("voice_mic_toggle_button"),
                shape = RoundedCornerShape(14.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isRecording) TertiaryClay else PrimaryOak,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Voice search mic trigger",
                    size = 24.dp,
                    tint = Color.White
                )
            }

            // Keyboard form Text field
            OutlinedTextField(
                value = activeQuery,
                onValueChange = { viewModel.activeChatQuery.value = it },
                placeholder = { Text("Ask Aura about colors, pricing, sizes...") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("message_input_box"),
                trailingIcon = {
                    IconButton(
                        onClick = { viewModel.onSendMessage() },
                        modifier = Modifier.testTag("send_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send text query", size = 20.dp, tint = PrimaryOak)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOak
                )
            )
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val alignEnd = msg.isUser
    val backColor = if (alignEnd) PrimaryOak.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
    val borderStroke = if (alignEnd) BorderStroke(1.dp, PrimaryOak.copy(alpha = 0.25f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (alignEnd) 16.dp else 4.dp,
                bottomEnd = if (alignEnd) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 290.dp),
            colors = CardDefaults.cardColors(containerColor = backColor),
            border = borderStroke
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (alignEnd) "YOU" else msg.sender.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (alignEnd) PrimaryOak else SecondarySage,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = msg.message,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Custom Canvas drawing featuring beautifully undulating bezier waves inside the mic card.
 */
@Composable
fun GlowingVocalWaveform() {
    val infiniteTransition = rememberInfiniteTransition()
    val phaseOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        val w = size.width
        val h = size.height
        val midY = h / 2

        // Draw multiple overlapping sine waves
        val path1 = Path()
        val path2 = Path()

        path1.moveTo(0f, midY)
        path2.moveTo(0f, midY)

        val steps = 80
        val dx = w / steps
        for (i in 0..steps) {
            val x = i * dx
            // Compute heights using sine formula paired with window bounding to pinch ends
            val window = sin((i.toFloat() / steps) * Math.PI).toFloat() // 0 at ends, 1 in middle
            
            val y1 = midY + (sin((i.toFloat() * 0.15f) + phaseOffset) * 14f * window)
            val y2 = midY + (sin((i.toFloat() * 0.25f) - phaseOffset) * 8f * window)

            path1.lineTo(x, y1.toFloat())
            path2.lineTo(x, y2.toFloat())
        }

        drawPath(
            path = path1,
            color = PrimaryOak,
            style = Stroke(width = 4f)
        )
        drawPath(
            path = path2,
            color = SecondarySage.copy(alpha = 0.6f),
            style = Stroke(width = 2.5f)
        )
    }
}
