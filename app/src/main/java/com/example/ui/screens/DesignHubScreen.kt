package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.RoomProjectEntity
import com.example.ui.theme.PrimaryOak
import com.example.ui.theme.SecondarySage
import com.example.ui.theme.TertiaryClay
import com.example.ui.viewmodel.DesignViewModel
import java.io.InputStream

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DesignHubScreen(viewModel: DesignViewModel) {
    val context = LocalContext.current
    val projects by viewModel.projectsList.collectAsState()
    val selectedProj by viewModel.selectedProject.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingRoom.collectAsState()
    val analysisErr by viewModel.analysisError.collectAsState()

    // Inputs for upload
    var activeRoomType by remember { mutableStateOf("Living Room") }
    var inputTitle by remember { mutableStateOf("") }
    var promptNotesInput by remember { mutableStateOf("") }

    // Selected photo Uri
    var fileUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        fileUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Applet Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "DESIGN HUB",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryOak,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Your Spaces",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (selectedProj != null) {
                IconButton(
                    onClick = { viewModel.deleteCurrentProject() },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = TertiaryClay)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete current project")
                }
            }
        }

        if (selectedProj == null) {
            // Empty State / Project Creation Page
            Card(
                modifier = Modifier.fillMaxWidth().testTag("add_project_box"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, PrimaryOak.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Analyze a New Interior Room",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOak
                    )

                    // Room Type Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Select Room Type",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Living Room", "Bedroom", "Office", "Dining").forEach { type ->
                                val active = activeRoomType == type
                                FilterChip(
                                    selected = active,
                                    onClick = { activeRoomType = type },
                                    label = { Text(type) },
                                    leadingIcon = if(active) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryOak.copy(alpha = 0.15f),
                                        selectedLabelColor = PrimaryOak
                                    ),
                                    modifier = Modifier.testTag("chip_$type")
                                )
                            }
                        }
                    }

                    // Fields
                    OutlinedTextField(
                        value = inputTitle,
                        onValueChange = { inputTitle = it },
                        label = { Text("Project Name") },
                        placeholder = { Text("e.g. Modern Sand Penthouse") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = promptNotesInput,
                        onValueChange = { promptNotesInput = it },
                        label = { Text("AI Analysis Context Hints (Optional)") },
                        placeholder = { Text("e.g. Make child-friendly, add houseplants, focus on warm textures.") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Onboarding presets VS Native Photo picker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PrimaryOak.copy(alpha = 0.05f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = PrimaryOak)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (fileUri != null) "Photo Ready!" else "Upload Custom Room Image",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (fileUri != null) "Ready to run AI vision audit on your selection" else "Tap below to choose layout form your device gallery",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOak)
                        ) {
                            Text(if (fileUri != null) "Change" else "Choose")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Action buttons: Try with Preset VS Analyze Upload
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.addPresetProject(
                                    presetCode = "preset_${activeRoomType.lowercase().substringBefore(" ")}",
                                    title = if(inputTitle.isNotBlank()) inputTitle else "Sample $activeRoomType",
                                    roomType = activeRoomType
                                )
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Use Preset Demo")
                        }

                        Button(
                            onClick = {
                                if (fileUri != null) {
                                    try {
                                        val inputStream: InputStream? = context.contentResolver.openInputStream(fileUri!!)
                                        val bitmap = BitmapFactory.decodeStream(inputStream)
                                        if (bitmap != null) {
                                            viewModel.addNewProjectAndAnalyze(
                                                bitmap = bitmap,
                                                roomType = activeRoomType,
                                                roomTitle = inputTitle,
                                                promptNotes = promptNotesInput
                                            )
                                        } else {
                                            viewModel.analysisError.value = "Unable to decode selected file."
                                        }
                                    } catch (e: Exception) {
                                        viewModel.analysisError.value = "Error compiling file: ${e.localizedMessage}"
                                    }
                                } else {
                                    viewModel.analysisError.value = "Please select a custom photo or tap 'Use Preset Demo'."
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp).testTag("trigger_analyze_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondarySage)
                        ) {
                            Text("Run AI Analysis")
                        }
                    }

                    if (isAnalyzing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            CircularProgressIndicator(color = PrimaryOak, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Aura AI is compiling space dimensions...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (analysisErr != null) {
                        Text(
                            text = analysisErr!!,
                            color = TertiaryClay,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            // Selected Project details screen
            val proj = selectedProj!!

            // Quick Info Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("active_project_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = proj.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = PrimaryOak
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Badge 1 (Room Type)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SecondarySage.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(proj.roomType, color = SecondarySage, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        // Badge 2 (Active Style)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryOak.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(proj.selectedStyle, color = PrimaryOak, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Room Analysis Results Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "📷 Space Identification Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Identified items list
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Detected Items:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            proj.detectedElementsKey.split(",").filter { it.isNotBlank() }.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(32.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, size = 12.dp, tint = PrimaryOak, modifier = Modifier.padding(end = 4.dp))
                                        Text(item, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    // Custom palette colors
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Aura AI Recommended Color Base:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            proj.recommendedColorsHex.split(",").filter { it.isNotBlank() }.forEach { hexColor ->
                                val parsedColor = try { Color(android.graphics.Color.parseColor(hexColor)) } catch (e: Exception) { PrimaryOak }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(parsedColor)
                                            .border(1.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f), CircleShape)
                                    )
                                    Text(
                                        text = hexColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // AI suggestions textbox
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "AI Design Suggestions Plan:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = proj.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Simple CTA to switch projects / restart
            OutlinedButton(
                onClick = { viewModel.selectedProjectId.value = null },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, PrimaryOak)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, size = 16.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyze Another Room", color = PrimaryOak)
            }
        }

        // saved historical projects row at the very bottom
        if (projects.isNotEmpty() && selectedProj == null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Stored Designs (${projects.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("saved_projects_row")
                ) {
                    items(projects) { p ->
                        Card(
                            modifier = Modifier
                                .width(160.dp)
                                .clickable { viewModel.selectProject(p.id) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PrimaryOak.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Home, contentDescription = null, tint = PrimaryOak, modifier = Modifier.size(28.dp))
                                }
                                Text(
                                    text = p.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = p.roomType,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Inline helper because Compose Material 3 standard size modifier does not support explicit size parameter on older versions
@Composable
fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color = LocalContentColor.current, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(size)) {
        androidx.compose.material3.Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.fillMaxSize()
        )
    }
}
