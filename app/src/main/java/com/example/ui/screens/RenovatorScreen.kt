package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.BudgetEstimateEntity
import com.example.ui.theme.PrimaryOak
import com.example.ui.theme.SecondarySage
import com.example.ui.theme.TertiaryClay
import com.example.ui.viewmodel.DesignViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenovatorScreen(viewModel: DesignViewModel) {
    val selectedProj by viewModel.selectedProject.collectAsState()
    val savedBudgets by viewModel.budgetEstimatesList.collectAsState()
    
    val roomSize by viewModel.calcRoomSizeSqFt.collectAsState()
    val paintQual by viewModel.calcPaintQuality.collectAsState()
    val furnitureLvl by viewModel.calcFurnitureLevel.collectAsState()
    val floorMaterial by viewModel.calcFloorMaterial.collectAsState()
    val projectedCostBreakdown by viewModel.localProjectedEstimate.collectAsState()

    var customPlanTitle by remember { mutableStateOf("") }
    var expandedPaint by remember { mutableStateOf(false) }
    var expandedFurniture by remember { mutableStateOf(false) }
    var expandedFloor by remember { mutableStateOf(false) }

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
                text = "BUDGET ESTIMATOR",
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryOak,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Renovation Estimator",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Calculator Frame
        Card(
            modifier = Modifier.fillMaxWidth().testTag("calculator_settings_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Calculate Project Specifications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryOak
                )

                // Room Size Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Room Area Size:", style = MaterialTheme.typography.labelLarge)
                        Text("$roomSize Sq. Ft", fontWeight = FontWeight.Bold, color = PrimaryOak)
                    }
                    Slider(
                        value = roomSize.toFloat(),
                        onValueChange = { viewModel.calcRoomSizeSqFt.value = it.toInt() },
                        valueRange = 50f..800f,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryOak,
                            activeTrackColor = PrimaryOak,
                            inactiveTrackColor = PrimaryOak.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("size_slider")
                    )
                }

                // Dropdowns for Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Paint drop
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Paint Tier", style = MaterialTheme.typography.labelSmall)
                        ExposedDropdownMenuBox(
                            expanded = expandedPaint,
                            onExpandedChange = { expandedPaint = !expandedPaint }
                        ) {
                            OutlinedTextField(
                                value = paintQual,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPaint) },
                                modifier = Modifier.menuAnchor(),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryOak
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expandedPaint,
                                onDismissRequest = { expandedPaint = false }
                            ) {
                                listOf("Standard", "Premium", "Luxury").forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item) },
                                        onClick = {
                                            viewModel.calcPaintQuality.value = item
                                            expandedPaint = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Floor item drop
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Flooring", style = MaterialTheme.typography.labelSmall)
                        ExposedDropdownMenuBox(
                            expanded = expandedFloor,
                            onExpandedChange = { expandedFloor = !expandedFloor }
                        ) {
                            OutlinedTextField(
                                value = floorMaterial,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFloor) },
                                modifier = Modifier.menuAnchor(),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryOak
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expandedFloor,
                                onDismissRequest = { expandedFloor = false }
                            ) {
                                listOf("Laminate", "Vinyl", "Oak Wood", "Italian Tile").forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item) },
                                        onClick = {
                                            viewModel.calcFloorMaterial.value = item
                                            expandedFloor = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Furniture quality select dropdown
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Furniture Arrangement Level", style = MaterialTheme.typography.labelSmall)
                    ExposedDropdownMenuBox(
                        expanded = expandedFurniture,
                        onExpandedChange = { expandedFurniture = !expandedFurniture }
                    ) {
                        OutlinedTextField(
                            value = furnitureLvl,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFurniture) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryOak
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedFurniture,
                            onDismissRequest = { expandedFurniture = false }
                        ) {
                            listOf("Minimal", "Comfortable", "High-End").forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = {
                                        viewModel.calcFurnitureLevel.value = item
                                        expandedFurniture = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Cost Predictions Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("live_projections_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryOak.copy(alpha = 0.08f)),
            border = BorderStroke(1.5.dp, PrimaryOak.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "📊 Projected Cost Projections",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryOak
                )

                Divider(color = PrimaryOak.copy(alpha = 0.12f))

                // Line items
                RowItemCost("Wall Painting Estimate", projectedCostBreakdown.paintCost)
                RowItemCost("Flooring Wood/Tile Estimate", projectedCostBreakdown.flooringCost)
                RowItemCost("Furniture Catalog Base", projectedCostBreakdown.furnitureCost)

                Divider(color = PrimaryOak.copy(alpha = 0.12f), modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Renovation Price", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(
                        text = String.format(Locale.US, "$%,.2f", projectedCostBreakdown.totalCost),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = SecondarySage
                    )
                }
            }
        }

        // Saving Form
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = customPlanTitle,
                    onValueChange = { customPlanTitle = it },
                    label = { Text("Plan Specification Label") },
                    placeholder = { Text("e.g. Master Hall High Spec") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        viewModel.onAddCurrentBudgetEstimate(customPlanTitle)
                        customPlanTitle = "" // flush title input after saving
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondarySage),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(56.dp).padding(top = 4.dp).testTag("save_estimate_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, size = 18.dp, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save")
                }
            }
        }

        // Saved Estimates list
        if (savedBudgets.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Saved Renovation Plans (${savedBudgets.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                savedBudgets.forEach { budget ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("budget_item_card_${budget.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = budget.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${budget.roomSizeSqFt} Sq Ft | Paint: ${budget.paintQuality} | Floor: ${budget.floorMaterial}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "$%,.0f", budget.totalCost),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = SecondarySage
                                )
                                
                                IconButton(
                                    onClick = { viewModel.onDeleteBudgetPlan(budget.id) },
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = TertiaryClay)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete entry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowItemCost(title: String, costVal: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(
            text = String.format(Locale.US, "$%,.2f", costVal),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
