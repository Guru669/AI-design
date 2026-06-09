package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryOak
import com.example.ui.theme.SecondarySage
import com.example.ui.theme.TertiaryClay
import com.example.ui.theme.SandSurfaceLight
import com.example.ui.viewmodel.DesignViewModel

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StudioTransformScreen(viewModel: DesignViewModel) {
    val selectedProj by viewModel.selectedProject.collectAsState()
    val isTransforming by viewModel.isTransformingRoom.collectAsState()
    val transformNotes by viewModel.activeTransformationResult.collectAsState()

    var activeTransformStyle by remember { mutableStateOf("Scandinavian") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab header
        Column {
            Text(
                text = "AI STUDIO",
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryOak,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Style Transformer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (selectedProj == null) {
            // Friendly guide to build project first
            Card(
                modifier = Modifier.fillMaxWidth().testTag("unselected_studio_splash"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        size = 52.dp,
                        tint = PrimaryOak
                    )
                    Text(
                        text = "No Room Selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "To preview virtual style transformations, analyze your space or open an existing design project first in the Design Hub.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }
        } else {
            val proj = selectedProj!!

            // Style variations grid
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Choose Architectural Style Variation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("Scandinavian", "Minimalist", "Luxury", "Modern", "Traditional").forEach { style ->
                            val isActive = activeTransformStyle == style
                            FilterChip(
                                selected = isActive,
                                onClick = {
                                    activeTransformStyle = style
                                    // Instantly update the mock/real transform description values
                                    viewModel.onTriggerStyleTransformation(style)
                                },
                                label = { Text(style) },
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.testTag("style_chip_$style")
                            )
                        }
                    }
                }
            }

            // Real-time Before/After Viewport Slider (Module 4)
            Text(
                text = "Swipe Redesign Vision Slider",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )

            InteractiveSwipeViewport(
                styleKey = activeTransformStyle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("interactive_slider_frame")
            )

            // Description generated notes card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🛠️ Reimagined Blueprint Specification",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isTransforming) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }

                    val activeSpec = transformNotes ?: proj.beforeAfterTransformationDesc.ifBlank {
                        "Tap on your preferred style variation above to have Aura AI synthesize the custom $activeTransformStyle design plan model for your ${proj.roomType}."
                    }

                    Text(
                        text = activeSpec,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * A beautiful horizontal drag viewport that draws the Room "Before" vs "After" using custom gradients and wireframes.
 */
@Composable
fun InteractiveSwipeViewport(styleKey: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    var widthPx by remember { mutableStateOf(1) }
    var dragPercent by remember { mutableStateOf(0.5f) } // starts in center (50% split)

    Box(
        modifier = modifier
            .background(Color(0xFFEADBC8))
            .onSizeChanged { widthPx = if (it.width > 0) it.width else 1 }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    val addition = dragAmount / widthPx
                    dragPercent = (dragPercent + addition).coerceIn(0.01f, 0.99f)
                }
            }
    ) {
        // --- Layer 1 (Left Side / Full background) -> BEFORE raw look ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Draw a warm minimalist layout sketch of a room
                    // Base background
                    drawRect(color = Color(0xFFAAA095))
                    
                    // Simple schematic perspective grids representing empty layout
                    val h = size.height
                    val w = size.width
                    
                    // Walls
                    drawLine(color = Color.White.copy(alpha = 0.5f), start = Offset(0f, 0f), end = Offset(w * 0.2f, h * 0.25f), strokeWidth = 3f)
                    drawLine(color = Color.White.copy(alpha = 0.5f), start = Offset(0f, h), end = Offset(w * 0.2f, h * 0.75f), strokeWidth = 3f)
                    drawLine(color = Color.White.copy(alpha = 0.5f), start = Offset(w, 0f), end = Offset(w * 0.8f, h * 0.25f), strokeWidth = 3f)
                    drawLine(color = Color.White.copy(alpha = 0.5f), start = Offset(w, h), end = Offset(w * 0.8f, h * 0.75f), strokeWidth = 3f)
                    
                    // Bed / Couch Wireframe outline
                    drawRect(
                        color = Color.White.copy(alpha = 0.2f),
                        topLeft = Offset(w * 0.35f, h * 0.45f),
                        size = Size(w * 0.3f, h * 0.35f)
                    )
                    // Wireframe lines
                    drawLine(color = Color.White, start = Offset(w * 0.35f, h * 0.45f), end = Offset(w * 0.65f, h * 0.45f), strokeWidth = 2f)
                    drawLine(color = Color.White, start = Offset(w * 0.35f, h * 0.80f), end = Offset(w * 0.65f, h * 0.80f), strokeWidth = 2f)
                    drawLine(color = Color.White, start = Offset(w * 0.35f, h * 0.45f), end = Offset(w * 0.35f, h * 0.80f), strokeWidth = 2f)
                    drawLine(color = Color.White, start = Offset(w * 0.65f, h * 0.45f), end = Offset(w * 0.65f, h * 0.80f), strokeWidth = 2f)
                }
        )

        // Native overlay label for BEFORE state
        Text(
            text = "BEFORE: EMPTY SPACE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        )

        // --- Layer 2 (Right Side of handle) -> AFTER decorated rendering ---
        val parsedAfterColors = when (styleKey.lowercase()) {
            "scandinavian" -> listOf(Color(0xFFFAF6F0), Color(0xFF6E8D72), Color(0xFF8D5B38)) // warm sage
            "minimalist" -> listOf(Color(0xFFEAEAEA), Color(0xFF272727), Color(0xFF909090)) // graphite white
            "luxury" -> listOf(Color(0xFFE2D3BE), Color(0xFF2C221A), Color(0xFFBD6B50)) // warm gold/earthy
            "modern" -> listOf(Color(0xFFDCD2C4), Color(0xFF7F6049), Color(0xFF425644)) // cozy walnut
            else -> listOf(Color(0xFFFAF6F0), Color(0xFF4E342E), Color(0xFF5D7B93)) // traditional
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = 1f - dragPercent)
                .align(Alignment.CenterEnd)
                .drawBehind {
                    // Draw the rendered room containing custom colored style spaces!
                    val w = size.width
                    val h = size.height
                    
                    // Background base
                    drawRect(color = parsedAfterColors.getOrElse(0) { Color.LightGray })
                    
                    // Wall panels (Sage Green / Walnut accent)
                    drawRect(
                        color = parsedAfterColors.getOrElse(1) { Color.DarkGray },
                        topLeft = Offset(0f, 0f),
                        size = Size(w * 0.3f, h)
                    )
                    
                    // Custom decorative warm wood beam lines
                    for (i in 1..4) {
                        drawLine(
                            color = parsedAfterColors.getOrElse(2) { Color.Gray }.copy(alpha = 0.3f),
                            start = Offset(w * 0.05f * i, 0f),
                            end = Offset(w * 0.05f * i, h),
                            strokeWidth = 6f
                        )
                    }

                    // Luxury Soft Velvet Bed / Sofa drawing (with layers and pillows!)
                    drawRoundRect(
                        color = parsedAfterColors.getOrElse(2) { Color.Gray },
                        topLeft = Offset(w * 0.15f, h * 0.4f),
                        size = Size(w * 0.65f, h * 0.45f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f)
                    )
                    
                    // Potted Plant leaf dots
                    drawCircle(color = Color(0xFF3B5E3C), radius = 24f, center = Offset(w * 0.88f, h * 0.35f))
                    drawCircle(color = Color(0xFF4C754D), radius = 18f, center = Offset(w * 0.84f, h * 0.40f))
                    drawLine(color = Color(0xFF865D36), start = Offset(w * 0.86f, h * 0.42f), end = Offset(w * 0.86f, h * 0.6f), strokeWidth = 8f)
                }
        )

        // Native overlay label for AFTER state
        Text(
            text = "AFTER: REDESIGNED ($styleKey)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )

        // --- Vertical drag divider line ---
        val offsetInDp = with(density) { (dragPercent * widthPx).toDp() }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .align(Alignment.CenterStart)
                .offset(x = offsetInDp)
                .background(PrimaryOak)
        ) {
            // Central thumb handle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PrimaryOak)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swipe handler slider",
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
            }
        }
    }
}
