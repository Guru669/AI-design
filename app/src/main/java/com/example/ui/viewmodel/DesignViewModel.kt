package com.example.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.data.repository.AppRepository
import com.example.data.network.Content
import com.example.data.network.Part
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DesignViewModel(private val repository: AppRepository) : ViewModel() {

    // --- Authentication States (Module 1) ---
    val currentSessionUser = repository.getLoggedInUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var authEmailInput = MutableStateFlow("")
    var authFullNameInput = MutableStateFlow("")
    var authPreferencesInput = MutableStateFlow("Modern, Minimalist")
    var authMessage = MutableStateFlow<String?>(null)
    var authIsRegisterMode = MutableStateFlow(false)

    // --- Rooms & Projects List (Module 2, 3, 4) ---
    val projectsList = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedProjectId = MutableStateFlow<Int?>(null)
    
    val selectedProject = selectedProjectId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getProjectByIdFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Interactive Placed Canvas Items (Module 7)
    val placedFurnitureList = selectedProjectId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getPlacedItemsForProject(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- UI Processing States ---
    var isAnalyzingRoom = MutableStateFlow(false)
    var analysisError = MutableStateFlow<String?>(null)
    
    var isTransformingRoom = MutableStateFlow(false)
    var activeTransformationResult = MutableStateFlow<String?>(null)

    // --- Renovator Budget Planning States (Module 5) ---
    val budgetEstimatesList = repository.allEstimates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var calcRoomSizeSqFt = MutableStateFlow(150)
    var calcPaintQuality = MutableStateFlow("Premium") // Standard, Premium, Luxury
    var calcFurnitureLevel = MutableStateFlow("Comfortable") // Minimal, Comfortable, High-End
    var calcFloorMaterial = MutableStateFlow("Oak Wood") // Oak Wood, Laminate, Vinyl, Italian Tile
    
    // Dynamic local cost projections before saving
    val localProjectedEstimate = combine(
        calcRoomSizeSqFt,
        calcPaintQuality,
        calcFurnitureLevel,
        calcFloorMaterial
    ) { sqFt, paint, furniture, floor ->
        calculateEstimateBreakdown(sqFt, paint, furniture, floor)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProjectedBreakdown())

    // --- Voice / Conversation Chat Assistant States (Module 6) ---
    var chatLogs = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("Aura", "Hello! I am Aura, your vocal interior design companion. Ask me any design or cost questions, or ask me for a tip!", isUser = false)
    ))
    var activeChatQuery = MutableStateFlow("")
    var isAssistantThinking = MutableStateFlow(false)
    var isRecordingAudio = MutableStateFlow(false)

    // --- Authentication Queries ---

    fun onAuthenticate() {
        viewModelScope.launch {
            authMessage.value = null
            val email = authEmailInput.value.trim()
            val fullName = authFullNameInput.value.trim()
            val preferences = authPreferencesInput.value.trim()

            if (email.isEmpty()) {
                authMessage.value = "Please enter an email address."
                return@launch
            }

            if (authIsRegisterMode.value) {
                if (fullName.isEmpty()) {
                    authMessage.value = "Full name cannot be blank."
                    return@launch
                }
                val success = repository.registerUser(email, fullName, preferences)
                if (success) {
                    authMessage.value = "Account built successfully! Welcome."
                } else {
                    authMessage.value = "User already exists. Try logging in."
                }
            } else {
                val success = repository.loginUser(email)
                if (success) {
                    authMessage.value = "Welcome back!"
                } else {
                    authMessage.value = "Details unrecognized. Enable register mode."
                }
            }
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            repository.logoutUser()
            // Reset state
            authEmailInput.value = ""
            authFullNameInput.value = ""
            authMessage.value = "Securely logged out."
            selectedProjectId.value = null
        }
    }

    fun toggleAuthMode() {
        authIsRegisterMode.value = !authIsRegisterMode.value
        authMessage.value = null
    }

    // --- Room Projects & Uploads ---

    fun addNewProjectAndAnalyze(
        bitmap: Bitmap,
        roomType: String,
        roomTitle: String,
        promptNotes: String
    ) {
        viewModelScope.launch {
            isAnalyzingRoom.value = true
            analysisError.value = null
            try {
                // Call Repository to either execute Gemini API or simulated algorithms
                val result = repository.analyzeUploadedRoom(bitmap, roomType, promptNotes)
                
                // Form color code string
                val colorsStr = result.paletteColorsHex.joinToString(",")
                val detectedStr = result.detectedFurniture.joinToString(",")

                // Save onto local Database
                val projectEntity = RoomProjectEntity(
                    title = if(roomTitle.isNotEmpty()) roomTitle else result.title,
                    roomType = result.roomType,
                    imagePresetCode = "custom_upload", // code to identify custom upload vs preset
                    detectedElementsKey = detectedStr,
                    recommendedColorsHex = colorsStr,
                    notes = result.suggestions,
                    selectedStyle = "Scandinavian" // default style
                )

                val savedId = repository.insertProject(projectEntity).toInt()
                selectedProjectId.value = savedId

                // Pre-populate some interactive Placed items for drag-n-drop instant onboarding!
                populateDefaultPlacedFurniture(savedId, result.detectedFurniture)

            } catch (e: Exception) {
                analysisError.value = "Image compilation failed: ${e.localizedMessage}"
            } finally {
                isAnalyzingRoom.value = false
            }
        }
    }

    fun addPresetProject(presetCode: String, title: String, roomType: String) {
        viewModelScope.launch {
            isAnalyzingRoom.value = true
            try {
                // Instantly generate mock result from preset definitions (simulating fast analyze)
                val colorSet = when(roomType) {
                    "Bedroom" -> listOf("#FAF6F0", "#8A9A86", "#69503E")
                    "Office" -> listOf("#334E68", "#272727", "#CCCCCC")
                    else -> listOf("#7F7F7F", "#C2B280", "#C89D7C")
                }
                
                val projectEntity = RoomProjectEntity(
                    title = title,
                    roomType = roomType,
                    imagePresetCode = presetCode,
                    detectedElementsKey = when(roomType) {
                        "Bedroom" -> "King-Size Bed,Oak Nightstand,Read Lights"
                        "Office" -> "Slat Desk,Flex Chair,Pine Shelves"
                        else -> "Chesterfield Sofa,Teak Coffee Table,Ficus Tree"
                    },
                    recommendedColorsHex = colorSet.joinToString(","),
                    notes = "Beautiful sample of $roomType. Light entry source highlights focal points. High potential for Scandinavian wood layouts.",
                    selectedStyle = "Scandinavian"
                )
                val savedId = repository.insertProject(projectEntity).toInt()
                selectedProjectId.value = savedId
                
                // Pop default placed items
                populateDefaultPlacedFurniture(savedId, listOf("Sofa", "Planted Table", "Standing Lamp"))
            } finally {
                isAnalyzingRoom.value = false
            }
        }
    }

    fun selectProject(projectId: Int) {
        selectedProjectId.value = projectId
        activeTransformationResult.value = null // clear transient style transforms
    }

    fun deleteCurrentProject() {
        val id = selectedProjectId.value ?: return
        viewModelScope.launch {
            repository.deleteProject(id)
            selectedProjectId.value = null
        }
    }

    // --- Room Styled Remodels (Before/After) ---

    fun onTriggerStyleTransformation(style: String) {
        val proj = selectedProject.value ?: return
        viewModelScope.launch {
            isTransformingRoom.value = true
            activeTransformationResult.value = null
            try {
                val description = repository.generateTransformation(
                    proj.title,
                    proj.roomType,
                    null, // No custom bitmap passed for fast template descriptions
                    style
                )
                
                // Update local model
                repository.insertProject(proj.copy(
                    selectedStyle = style,
                    beforeAfterTransformationDesc = description
                ))
                activeTransformationResult.value = description
            } catch (e: Exception) {
                activeTransformationResult.value = "Failed style shift: ${e.localizedMessage}"
            } finally {
                isTransformingRoom.value = false
            }
        }
    }

    // --- Interactive Furniture Placer (Module 7) ---

    fun placeNewFurnitureOnCanvas(itemName: String, hexCode: String) {
        val id = selectedProjectId.value ?: return
        viewModelScope.launch {
            val item = PlacerItemEntity(
                projectId = id,
                itemName = itemName,
                category = "Utility",
                colorHex = hexCode,
                posX = 0.5f,
                posY = 0.5f,
                rotation = 0f,
                scale = 1.0f
            )
            repository.insertPlacedItem(item)
        }
    }

    fun updateFurniturePosition(item: PlacerItemEntity, posX: Float, posY: Float) {
        viewModelScope.launch {
            repository.insertPlacedItem(item.copy(posX = posX, posY = posY))
        }
    }

    fun updateFurnitureOrientation(item: PlacerItemEntity, rotAngle: Float, scaleFactor: Float) {
        viewModelScope.launch {
            repository.insertPlacedItem(item.copy(rotation = rotAngle, scale = scaleFactor))
        }
    }

    fun removeFurnitureFromCanvas(itemId: Int) {
        viewModelScope.launch {
            repository.deletePlacedItem(itemId)
        }
    }

    private suspend fun populateDefaultPlacedFurniture(projectId: Int, detectedNames: List<String>) {
        val colors = listOf("#C7A785", "#8D6E63", "#4E342E", "#728C74")
        detectedNames.forEachIndexed { idx, name ->
            val cleanName = name.substringBefore("(").trim()
            val entity = PlacerItemEntity(
                projectId = projectId,
                itemName = cleanName,
                category = "Base",
                colorHex = colors.getOrElse(idx) { "#9C6644" },
                posX = 0.3f + (0.2f * idx),
                posY = 0.4f + (0.1f * idx),
                rotation = 0f,
                scale = 1.0f
            )
            repository.insertPlacedItem(entity)
        }
    }

    // --- Budget Renovation Operations ---

    fun onAddCurrentBudgetEstimate(title: String) {
        viewModelScope.launch {
            val bd = localProjectedEstimate.value
            val isCustomTitle = title.isNotBlank()
            val estimateEntity = BudgetEstimateEntity(
                title = if(isCustomTitle) title else "${selectedProject.value?.title ?: "Room"} Budget Plan",
                projectId = selectedProjectId.value ?: 0,
                roomSizeSqFt = calcRoomSizeSqFt.value,
                paintQuality = calcPaintQuality.value,
                furnitureLevel = calcFurnitureLevel.value,
                floorMaterial = calcFloorMaterial.value,
                paintCost = bd.paintCost,
                furnitureCost = bd.furnitureCost,
                flooringCost = bd.flooringCost,
                totalCost = bd.totalCost
            )
            repository.saveEstimate(estimateEntity)
        }
    }

    fun onDeleteBudgetPlan(id: Int) {
        viewModelScope.launch {
            repository.deleteEstimate(id)
        }
    }

    private fun calculateEstimateBreakdown(
        sqFt: Int,
        paint: String,
        furniture: String,
        floor: String
    ): ProjectedBreakdown {
        val paintSfCost = when(paint) {
            "Standard" -> 2.20
            "Premium" -> 4.50
            "Luxury" -> 8.50
            else -> 3.00
        }
        val floorSfCost = when(floor) {
            "Laminate" -> 3.50
            "Vinyl" -> 4.20
            "Oak Wood" -> 11.00
            "Italian Tile" -> 16.50
            else -> 6.00
        }
        val baseFurnitureEstimate = when(furniture) {
            "Minimal" -> 800.0
            "Comfortable" -> 2400.0
            "High-End" -> 7500.0
            else -> 1500.0
        }

        val paintCost = sqFt * paintSfCost * 3.5 // multiply for wall coverage height estimate
        val flooringCost = sqFt * floorSfCost
        val total = paintCost + flooringCost + baseFurnitureEstimate

        return ProjectedBreakdown(
            paintCost = paintCost,
            flooringCost = flooringCost,
            furnitureCost = baseFurnitureEstimate,
            totalCost = total
        )
    }

    // --- Assistant Queries ---

    fun onSendMessage() {
        val text = activeChatQuery.value.trim()
        if (text.isEmpty()) return
        
        viewModelScope.launch {
            val userMsg = ChatMessage("User", text, isUser = true)
            chatLogs.value = chatLogs.value + userMsg
            activeChatQuery.value = ""
            isAssistantThinking.value = true

            // Gather context history for Gemini
            val historyContents = chatLogs.value.takeLast(10).map { msg ->
                Content(
                    role = if (msg.isUser) "user" else "model",
                    parts = listOf(Part(text = msg.message))
                )
            }

            try {
                val ans = repository.conductAiChat(text, historyContents)
                val assisMsg = ChatMessage("Aura", ans, isUser = false)
                chatLogs.value = chatLogs.value + assisMsg
            } catch (e: Exception) {
                chatLogs.value = chatLogs.value + ChatMessage("Aura", "I am having temporary networking issues. Let's restart: ${e.localizedMessage}", isUser = false)
            } finally {
                isAssistantThinking.value = false
            }
        }
    }

    /**
     * Simulation of Voice continuous recording commands (Speech to Text + execution)
     */
    fun toggleRecordingVoice() {
        if (isRecordingAudio.value) {
            isRecordingAudio.value = false
            // Trigger a simulated spoken command sequence!
            viewModelScope.launch {
                isAssistantThinking.value = true
                val listCommands = listOf(
                    "Show Scandinavian design tips",
                    "How much does hardwood flooring cost?",
                    "What's a good palette for my office?",
                    "Tamil design aesthetic help"
                )
                val voiceCommand = listCommands.random()
                
                chatLogs.value = chatLogs.value + ChatMessage("User", "[Voice] \"$voiceCommand\"", isUser = true)
                
                // Get AI resp
                val ans = repository.conductAiChat(voiceCommand, emptyList())
                chatLogs.value = chatLogs.value + ChatMessage("Aura", ans, isUser = false)
                isAssistantThinking.value = false
            }
        } else {
            isRecordingAudio.value = true
        }
    }

    companion object {
        fun provideFactory(repository: AppRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DesignViewModel(repository) as T
            }
        }
    }
}

// Helper models for VM state handling
data class ProjectedBreakdown(
    val paintCost: Double = 0.0,
    val flooringCost: Double = 0.0,
    val furnitureCost: Double = 0.0,
    val totalCost: Double = 0.0
)

data class ChatMessage(
    val sender: String,
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
