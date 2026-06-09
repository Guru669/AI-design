package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PrimaryOak
import com.example.ui.theme.SecondarySage
import com.example.ui.viewmodel.DesignViewModel

enum class DashboardTab(val label: String, val icon: ImageVector, val tag: String) {
    HUB("Design Hub", Icons.Outlined.Home, "tab_hub"),
    STUDIO("AI Studio", Icons.Outlined.AutoAwesome, "tab_studio"),
    PLACER("Layout AR", Icons.Outlined.Layers, "tab_placer"),
    RENOVATOR("Budget AI", Icons.Outlined.Calculate, "tab_renovator"),
    VOICE("Aura AI", Icons.Outlined.Mic, "tab_voice"),
    PROFILE("Profile", Icons.Outlined.AccountCircle, "tab_profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DesignViewModel) {
    var activeTab by remember { mutableStateOf(DashboardTab.HUB) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("dashboard_bottom_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                DashboardTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (activeTab == tab) PrimaryOak else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                color = if (activeTab == tab) PrimaryOak else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = PrimaryOak.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                DashboardTab.HUB -> DesignHubScreen(viewModel)
                DashboardTab.STUDIO -> StudioTransformScreen(viewModel)
                DashboardTab.PLACER -> FurniturePlacerScreen(viewModel)
                DashboardTab.RENOVATOR -> RenovatorScreen(viewModel)
                DashboardTab.VOICE -> VoiceAssistantScreen(viewModel)
                DashboardTab.PROFILE -> ProfileScreen(viewModel)
            }
        }
    }
}
