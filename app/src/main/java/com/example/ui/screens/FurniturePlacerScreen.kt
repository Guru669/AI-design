package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.PlacerItemEntity
import com.example.ui.theme.PrimaryOak
import com.example.ui.theme.SecondarySage
import com.example.ui.theme.TertiaryClay
import com.example.ui.viewmodel.DesignViewModel
import kotlin.math.roundToInt

data class CatalogItem(val name: String, val icon: ImageVector, val hexColor: String)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FurniturePlacerScreen(viewModel: DesignViewModel) {
    val selectedProj by viewModel.selectedProject.collectAsState()
    val placedItems by viewModel.placedFurnitureList.collectAsState()

    var activeSelectedItemId by remember { mutableStateOf<Int?>(null) }
    var canvasWidth by remember { mutableStateOf(1) }
    var canvasHeight by remember { mutableStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tab header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "LAYOUT AR",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryOak,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Furniture Arranger",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (placedItems.isNotEmpty() && selectedProj != null) {
                TextButton(
                    onClick = {
                        viewModel.selectedProjectId.value?.let {
                            viewModel.removeFurnitureFromCanvas(-1) // triggers clear if customized
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = TertiaryClay)
                ) {
                    Text("Reset All")
                }
            }
        }

        if (selectedProj == null) {
            // Unselected guide banner
            Card(
                modifier = Modifier.fillMaxWidth().testTag("placer_unselected_banner"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        size = 52.dp,
                        tint = PrimaryOak
                    )
                    Text(
                        text = "No Active Workspace Room",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Activate an design project inside the Design Hub tab first to utilize the virtual drag-and-drop layout planner model.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }
        } else {
            val proj = selectedProj!!

            // Element Injector Palette (Tray of Furniture catalog)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tap Element to Place in Room",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOak
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            CatalogItem("Sofa", Icons.Default.Weekend, "#8D5B38"),
                            CatalogItem("Table", Icons.Default.TableRestaurant, "#BD6B50"),
                            CatalogItem("Plant", Icons.Default.Park, "#6E8D72"),
                            CatalogItem("Lamp", Icons.Default.Lightbulb, "#E9C46A"),
                            CatalogItem("Painting", Icons.Default.Image, "#2A9D8F"),
                            CatalogItem("Chair", Icons.Default.Chair, "#457B9D")
                        ).forEach { item ->
                            InputChip(
                                selected = false,
                                onClick = { viewModel.placeNewFurnitureOnCanvas(item.name, item.hexColor) },
                                label = { Text(item.name, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = { Icon(item.icon, contentDescription = null, size = 16.dp, tint = Color(android.graphics.Color.parseColor(item.hexColor))) },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("item_chip_${item.name}")
                            )
                        }
                    }
                }
            }

            // Interactive Drawing Canvas (Workspace Area)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE5DCD0)) // Sand Grid Base background
                    .border(1.5.dp, PrimaryOak.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .onSizeChanged {
                        canvasWidth = if (it.width > 0) it.width else 1
                        canvasHeight = if (it.height > 0) it.height else 1
                    }
                    .testTag("layout_placer_grid")
            ) {
                // Background isometric graph layout simulation
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = size.width / 12
                    for (i in 1..11) {
                        // vertical lines
                        drawLine(
                            color = Color.White.copy(alpha = 0.25f),
                            start = Offset(step * i, 0f),
                            end = Offset(step * i, size.height),
                            strokeWidth = 2f
                        )
                        // horizontal lines
                        drawLine(
                            color = Color.White.copy(alpha = 0.25f),
                            start = Offset(0f, step * i),
                            end = Offset(size.width, step * i),
                            strokeWidth = 2f
                        )
                    }
                }

                // Status text overlay
                Text(
                    text = "PROJECT: ${proj.title.uppercase()} FLOOR PLAN (Scale 1:20)",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryOak.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.TopCenter).padding(8.dp)
                )

                if (placedItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Room Canvas is currently empty.\nTap items from catalog to arrange layout.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryOak.copy(alpha = 0.5f)
                        )
                    }
                }

                // Render placed items dynamically as Draggable, Rotatable boxes!
                placedItems.forEach { item ->
                    val isSelected = activeSelectedItemId == item.id
                    val itemColor = try { Color(android.graphics.Color.parseColor(item.colorHex)) } catch (e: Exception) { PrimaryOak }

                    // Local offsets converted from percentage to real canvas scale
                    val posXInPx = item.posX * canvasWidth
                    val posYInPx = item.posY * canvasHeight

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (posXInPx - 50.dp.toPx()).roundToInt(),
                                    y = (posYInPx - 50.dp.toPx()).roundToInt()
                                )
                            }
                            .size(100.dp)
                            .rotate(item.rotation)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) itemColor.copy(alpha = 0.95f) else itemColor.copy(alpha = 0.8f)
                            )
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .pointerInput(item.id) {
                                detectDragGestures(
                                    onDragStart = { activeSelectedItemId = item.id },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val newX = (item.posX + dragAmount.x / canvasWidth).coerceIn(0.05f, 0.95f)
                                        val newY = (item.posY + dragAmount.y / canvasHeight).coerceIn(0.05f, 0.95f)
                                        viewModel.updateFurniturePosition(item, newX, newY)
                                    }
                                )
                            }
                            .clickable { activeSelectedItemId = item.id }
                            .testTag("draggable_item_${item.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Icon(
                                imageVector = when (item.itemName.lowercase()) {
                                    "sofa" -> Icons.Default.Weekend
                                    "table" -> Icons.Default.TableRestaurant
                                    "plant" -> Icons.Default.Park
                                    "lamp" -> Icons.Default.Lightbulb
                                    "painting" -> Icons.Default.Image
                                    else -> Icons.Default.Chair
                                },
                                contentDescription = null,
                                size = 28.dp,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.itemName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }

                        // Little remove button on selected elements
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .clickable { viewModel.removeFurnitureFromCanvas(item.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, size = 12.dp, tint = Color.White)
                            }
                        }
                    }
                }
            }

            // Controls Panel when an element is in active selected state
            AnimatedVisibility(
                visible = activeSelectedItemId != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val currentFocusedItem = placedItems.find { it.id == activeSelectedItemId }
                if (currentFocusedItem != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Selected: ${currentFocusedItem.itemName.uppercase()}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Drag box to offset. Tap controls to adjust orientation.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Rotate Left CCW
                                FilledIconButton(
                                    onClick = {
                                        val nextRot = (currentFocusedItem.rotation - 15f) % 360f
                                        viewModel.updateFurnitureOrientation(currentFocusedItem, nextRot, currentFocusedItem.scale)
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = PrimaryOak)
                                ) {
                                    Icon(Icons.Default.RotateLeft, contentDescription = "Rotate CCW", size = 18.dp, tint = Color.White)
                                }

                                // Rotate Right CW
                                FilledIconButton(
                                    onClick = {
                                        val nextRot = (currentFocusedItem.rotation + 15f) % 360f
                                        viewModel.updateFurnitureOrientation(currentFocusedItem, nextRot, currentFocusedItem.scale)
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = PrimaryOak)
                                ) {
                                    Icon(Icons.Default.RotateRight, contentDescription = "Rotate CW", size = 18.dp, tint = Color.White)
                                }

                                // Clear Selected Focus
                                OutlinedIconButton(
                                    onClick = { activeSelectedItemId = null }
                                ) {
                                    Icon(Icons.Default.Done, contentDescription = "Deselect", size = 18.dp, tint = PrimaryOak)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
