package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.database.*
import com.example.data.network.Content
import com.example.data.network.GeminiApiClient
import com.example.data.network.Part
import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonClass
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AppRepository(private val db: AppDatabase) {

    private val userDao = db.userDao()
    private val roomProjectDao = db.roomProjectDao()
    private val placerItemDao = db.placerItemDao()
    private val budgetDao = db.budgetDao()

    // --- Module 1: Authentication ---

    fun getLoggedInUserFlow(): Flow<UserEntity?> = userDao.getLoggedInUserFlow()
    
    suspend fun getLoggedInUser(): UserEntity? = userDao.getLoggedInUser()

    suspend fun registerUser(email: String, fullName: String, preferences: String): Boolean {
        if (email.isEmpty() || fullName.isEmpty()) return false
        val existing = userDao.getUserByEmail(email)
        if (existing != null) return false
        
        val newUser = UserEntity(
            email = email,
            fullName = fullName,
            preferences = preferences,
            token = "jwt_sim_${UUID.randomUUID().toString().take(12)}",
            isLoggedIn = true
        )
        userDao.logoutAllUsers() // Ensure single session
        userDao.insertUser(newUser)
        return true
    }

    suspend fun loginUser(email: String): Boolean {
        if (email.isEmpty()) return false
        val user = userDao.getUserByEmail(email)
        if (user != null) {
            userDao.logoutAllUsers()
            userDao.insertUser(user.copy(isLoggedIn = true))
            return true
        }
        return false
    }

    suspend fun logoutUser() {
        userDao.logoutAllUsers()
    }

    // --- Module 2, 3, 4: Projects & Room AI ---

    val allProjects: Flow<List<RoomProjectEntity>> = roomProjectDao.getAllProjectsFlow()

    fun getProjectByIdFlow(id: Int): Flow<RoomProjectEntity?> = roomProjectDao.getProjectByIdFlow(id)

    suspend fun getProjectById(id: Int): RoomProjectEntity? = roomProjectDao.getProjectById(id)

    suspend fun insertProject(project: RoomProjectEntity): Long {
        return roomProjectDao.insertProject(project)
    }

    suspend fun deleteProject(projectId: Int) {
        roomProjectDao.deleteProjectById(projectId)
        placerItemDao.deleteItemsByProject(projectId)
        budgetDao.deleteEstimatesByProject(projectId)
    }

    /**
     * Conducts a real Vision Analysis using Gemini models, or triggers simulated analytics if token is unset.
     */
    suspend fun analyzeUploadedRoom(
        bitmap: Bitmap,
        roomType: String,
        promptNotes: String
    ): RoomAnalysisResult {
        val userPrompt = """
            Analyze this uploaded image of a $roomType. 
            Identify existing furniture elements, provide a rough dimension context estimate (in square feet), and list design suggestions.
            Extra criteria: $promptNotes
            
            OUTPUT RULES (Strict JSON format required):
            You must output ONLY a valid raw JSON matching this structure:
            {
               "roomType": "$roomType",
               "spaceSizeSqFt": 150,
               "detectedFurniture": ["Item A", "Item B"],
               "paletteColorsHex": ["#1A1A1A", "#FFFFFF"],
               "suggestions": "Suggestions summary here",
               "title": "$roomType Design Concept"
            }
        """.trimIndent()

        val systemPrompt = "You are an expert interior design architect specializing in computer-vision interior space analysis."

        return if (GeminiApiClient.isApiKeyAvailable()) {
            try {
                val responseText = GeminiApiClient.analyzeImage(bitmap, userPrompt, systemPrompt)
                parseAnalysisResponse(roomType, responseText)
            } catch (e: Exception) {
                Log.e("AppRepository", "Failed real image analysis, using fallback", e)
                getFallbackAnalysis(roomType)
            }
        } else {
            getFallbackAnalysis(roomType)
        }
    }

    /**
     * Recommends a room styled transformation (Module 4 AI Image Generation / preview simulation).
     */
    suspend fun generateTransformation(
        title: String,
        roomType: String,
        bitmap: Bitmap?,
        targetStyle: String
    ): String {
        val userPrompt = """
            We are designing a $roomType matching a "$targetStyle" design style.
            Describe the transformed layout, paint suggestions, furniture recommendations, and mood changes in beautiful detail.
            Also, provide 3 short, specific bullet recommendations.
        """.trimIndent()

        return if (GeminiApiClient.isApiKeyAvailable() && bitmap != null) {
            GeminiApiClient.analyzeImage(
                bitmap,
                userPrompt,
                "You are an elite interior decorator who generates high-fidelity before/after styling briefs."
            )
        } else {
            getFallbackTransformationDescription(roomType, targetStyle)
        }
    }

    // --- Module 6: Voice and Text Assistant ---

    suspend fun conductAiChat(userQuery: String, chatLogs: List<Content>): String {
        val systemPrompt = """
            You are 'Aura', an elite voice-enabled AI Interior Design Assistant. 
            Answer design queries with precision, warmth, and architectural intelligence.
            Try to keep your answers concise (3-4 sentences max) as they are meant to be spoken or displayed as quick tips.
            If the query is related to cost, give estimates. If about styles (Modern, Scandinavian, Minimalist), describe the palettes.
        """.trimIndent()
        
        return if (GeminiApiClient.isApiKeyAvailable()) {
            GeminiApiClient.chat(userQuery, chatLogs, systemPrompt)
        } else {
            getSimulatedChatResult(userQuery)
        }
    }

    // --- Module 7: Placer Furniture ---

    fun getPlacedItemsForProject(projectId: Int): Flow<List<PlacerItemEntity>> {
        return placerItemDao.getItemsForProjectFlow(projectId)
    }

    suspend fun insertPlacedItem(item: PlacerItemEntity) {
        placerItemDao.insertItem(item)
    }

    suspend fun deletePlacedItem(itemId: Int) {
        placerItemDao.deleteItemById(itemId)
    }

    suspend fun clearPlacedItemsForProject(projectId: Int) {
        placerItemDao.deleteItemsByProject(projectId)
    }

    // --- Module 5: Renovator Budget Calculations ---

    val allEstimates: Flow<List<BudgetEstimateEntity>> = budgetDao.getAllEstimatesFlow()

    suspend fun saveEstimate(estimate: BudgetEstimateEntity) {
        budgetDao.insertEstimate(estimate)
    }

    suspend fun deleteEstimate(estimateId: Int) {
        budgetDao.deleteEstimateById(estimateId)
    }

    // --- Helpers / Parsing / Local Sim Mocking ---

    private fun parseAnalysisResponse(roomType: String, responseText: String): RoomAnalysisResult {
        // Strip out triple backticks if outputted by markdown LLM
        val cleanJson = responseText
            .substringAfter("```json")
            .substringAfter("```")
            .substringBeforeLast("```")
            .trim()

        return try {
            val jsonAdapter = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
                .adapter(RoomAnalysisResult::class.java)
            
            jsonAdapter.fromJson(cleanJson) ?: getFallbackAnalysis(roomType)
        } catch (e: Exception) {
            Log.w("AppRepository", "Parsing failed for: $responseText. Handing over fallback.", e)
            
            // Extract some elements using simple string matchers if JSON parsing fully fails
            val detected = mutableListOf<String>()
            if (responseText.contains("sofa", true)) detected.add("Sofa")
            if (responseText.contains("bed", true)) detected.add("Bed")
            if (responseText.contains("table", true)) detected.add("Table")
            if (responseText.contains("desk", true)) detected.add("Desk")
            if (detected.isEmpty()) detected.addAll(listOf("Main seating", "Lamps", "Wall Decor"))

            RoomAnalysisResult(
                roomType = roomType,
                spaceSizeSqFt = if (roomType.contains("Living", true)) 240 else 140,
                detectedFurniture = detected,
                paletteColorsHex = listOf("#E3D4C4", "#4E3E34", "#7E8C82"),
                suggestions = responseText.take(600),
                title = "$roomType Analysis Overview"
            )
        }
    }

    private fun getFallbackAnalysis(roomType: String): RoomAnalysisResult {
        return when (roomType.lowercase()) {
            "living room" -> RoomAnalysisResult(
                roomType = "Living Room",
                spaceSizeSqFt = 220,
                detectedFurniture = listOf("Gray Chesterfield Sofa", "Oak Coffee Table", "Indoor Fig Tree", "Slim Floor Lamp"),
                paletteColorsHex = listOf("#7F7F7F", "#C2B280", "#228B22", "#D4AF37"),
                suggestions = "Excellent floor area with balanced windows on the east wall. The gray upholstery blends smoothly, but adding terracotta throw pillows could provide a sophisticated focal pop. We recommend a Scandinavian minimalist arrangement for optimal traffic flow.",
                title = "Scandinavian Living Studio"
            )
            "bedroom" -> RoomAnalysisResult(
                roomType = "Bedroom",
                spaceSizeSqFt = 160,
                detectedFurniture = listOf("King-Size Tufted Bed", "White Oak Nightstands", "Ceramic Bedside Sconces"),
                paletteColorsHex = listOf("#FFFFFF", "#E6DFD3", "#8A9A86", "#1F2833"),
                suggestions = "Cozy space feeling. The high-profile headboard anchors the main wall gracefully. Introduce sage green wall paneling on the bedding side to induce instant relaxation. Swapping out standard sconces for brass pendant lights saves precious side table space.",
                title = "Sage Retreat Bedroom"
            )
            "office" -> RoomAnalysisResult(
                roomType = "Office",
                spaceSizeSqFt = 120,
                detectedFurniture = listOf("Modular Desk", "Ergonomic Mesh Chair", "Bamboo Bookshelf"),
                paletteColorsHex = listOf("#B2C8DF", "#334E68", "#CCCCCC", "#272727"),
                suggestions = "Excellent workplace density. Position your modular desk perpendicular to the window to avoid direct visual glare on the monitors. We suggest mounting acoustic slat wood felt panels on the wall behind you to enhance virtual audio buffering.",
                title = "Executive Slat Office"
            )
            else -> RoomAnalysisResult(
                roomType = roomType,
                spaceSizeSqFt = 150,
                detectedFurniture = listOf("Modern Sitting Couch", "Pendant Light Drop", "Glass Side Table"),
                paletteColorsHex = listOf("#EFEFEF", "#D2B48C", "#4B5320"),
                suggestions = "Bright multi-functional layout. Keep pathways unhindered. Accent colors like olive green and warm sand will ground the architectural space while retaining its expansive, light-filled atmosphere.",
                title = "Aura Tailor-Fit Studio"
            )
        }
    }

    private fun getFallbackTransformationDescription(roomType: String, style: String): String {
        return when (style.lowercase()) {
            "scandinavian" -> """
                🌿 **SCANDINAVIAN DESIGN BRIEF: NATURAL LIGHT & ORGANIC COMFORT**
                The redesigned $roomType layout maximizes daylight distribution and incorporates high-texture bio-materials.

                **Core Elements:**
                • **Palette**: Soft cream backdrops paired with calm sage-green and raw oak textures.
                • **Furniture Choice**: Lighter wood-trimmed furniture with highly tactile bouclé fabrics. 
                • **Aesthetic Tone**: Embraces 'Hygge'—a sanctuary that feels both curated and extremely inviting.

                **Renovation Actions:**
                1. Repaint core wall surfaces in an ultra-matte linen white cream.
                2. Install natural white-oiled oak herringbone floor panels.
                3. Roll out an oversized hand-woven jute wool rug to bind the main lounge furniture.
            """.trimIndent()
            
            "minimalist" -> """
                📐 **MINIMALIST DESIGN BRIEF: REDUCTION AND ESSENTIAL FLOW**
                Your $roomType undergoes a profound decluttering, focusing purely on functional utility, negative space, and geometry.

                **Core Elements:**
                • **Palette**: High contrast polar white, rich matte charcoal grey, and brushed black metals.
                • **Furniture Choice**: Sleek, low-profile linear seating without heavy legs or bulk.
                • **Aesthetic Tone**: Intellectual, tranquil, and entirely focused on spatial clarity.

                **Renovation Actions:**
                1. Conceal standard storage shelves in flush, touch-latch white wall modules.
                2. Install low-profile, energy-efficient warm ceiling wash spots.
                3. Hang a single, highly impactive oversized monochrome canvas on the principal wall.
            """.trimIndent()

            "luxury" -> """
                💎 **LUXURY SAND & GOLD BRIEF: PREMIUM TEXTURES & INTENSITY**
                A hotel-grade, high-end reimagining of your $roomType utilizing polished marble surfaces, walnut paneling, and golden trims.

                **Core Elements:**
                • **Palette**: Warm champagne sand base, deep imperial gold metallic trims, and polished walnut.
                • **Furniture Choice**: Deep-set velvet sofas, custom brass accent leg tables, and silk bedding lines.
                • **Aesthetic Tone**: Rich, sensory, and deeply expressive of premium quality craft.

                **Renovation Actions:**
                1. Frame the main media wall with premium gold metal inlays and polished Calacatta marble slab.
                2. Introduce full-height heavy velvet acoustic drapes with subtle ceiling drop recesses.
                3. Place three brushed warm-gold spotlight canisters to paint corner spaces in cozy shadow play.
            """.trimIndent()

            "modern" -> """
                🏙️ **MID-CENTURY MODERN BRIEF: ARCHITECTURAL VINTAGE & METALS**
                A timeless layout balancing structural curves, architectural dark veneers, and vintage leather textures.

                **Core Elements:**
                • **Palette**: Walnut, mustard yellow accents, and warm deep forest greens.
                • **Furniture Choice**: Iconic tapered wooden legs, curved organic armchairs, and retro chrome fixtures.
                • **Aesthetic Tone**: Dynamic, artistic, and rich in design-history prestige.

                **Renovation Actions:**
                1. Install a stunning walnut wood slat divider screen to frame the entryway.
                2. Accentuate with double glass orb pendants reflecting warm vintage light.
                3. Roll out a geometric woven flat-weave rug featuring clean earthy pigments.
            """.trimIndent()

            else -> """
                🏛️ **TRADITIONAL ELEGANCE BRIEF: CLASSIC SYMMETRY & COMFORT**
                An inviting $roomType styled around classical architectural proportions, crafted moldings, and heirloom furniture.

                **Core Elements:**
                • **Palette**: Warm cream base, deep mahogany brown, and plush antique blue panels.
                • **Furniture Choice**: Rolled-arm sofas, ornamental crown molding frames, and dark-grain wood cabinetry.
                • **Aesthetic Tone**: Balanced, timeless, stately, and highly prestigious.

                **Renovation Actions:**
                1. Install ornate crown moldings and stately wall wainscoted wooden trims.
                2. Situate two identical upholstered wingback lounge chairs symmetrically facing the room fireplace.
                3. Introduce fine brass floor lamps equipped with classic pleated silk light shades.
            """.trimIndent()
        }
    }

    private fun getSimulatedChatResult(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("hello") || q.contains("hi") || q.contains("hey") -> 
                "Hello, I am Aura, your interior design coach! I can help you analyze rooms, select a style, calculate renovation costs, or configure furniture mockups. Ask me anything!"
            
            q.contains("cost") || q.contains("cheap") || q.contains("budget") || q.contains("price") -> 
                "Renovation budgets vary based on quality! For painting, standard emulsion runs at $2.50 per sq. ft, while custom textured plaster goes up to $8.00. Furniture lists can be managed economically in our 'Renovator' budget calculator tool. Try selecting standard vs luxury tiers there!"
            
            q.contains("scandinavian") || q.contains("scandi") -> 
                "Scandinavian styling integrates light pine or oak wood, warm woven blankets, rich linen textures, and indoor botanical greens like Fiddle Fig. It's meant to look incredibly clean and feel cozy!"
            
            q.contains("color") || q.contains("paint") || q.contains("palette") -> 
                "To choose colors, begin with a 60-30-10 ratio. 60% neutral background (such as Warm Sand or Cream White), 30% secondary texture (like Oak Walnut or Sage Gray), and 10% bold contrast punch (such as Matte Charcoal or Terracotta)!"
            
            q.contains("small") || q.contains("tight") || q.contains("studio") -> 
                "To make a tight room feel larger: (1) Choose low-profile furniture so light can pass, (2) Hang mirrors opposite windows to reflect daylight, and (3) Use tone-on-tone color palettes to prevent jarring architectural boundaries!"
                
            q.contains("tamil") ->
                "வணக்கம்! நான் ஆரா, உங்கள் உள்துறை வடிவமைப்பு உதவியாளர். நீங்கள் கேட்கும் கேள்விகளுக்கு உகந்த வடிவமைப்புகளை பரிந்துரைப்பேன்! (Welcome! I am Aura. I can assist in your interior design styling!)"

            else -> 
                "That's a very creative question. In modern interior architecture, we advise framing that element using soft warm recessed LEDs. Combining it with rich organic wood finishes and breathable linen fabrics will immediately elevate the space luxury!"
        }
    }
}

/**
 * Clean helper representing JSON structure returned from Gemini Vision model API.
 */
@JsonClass(generateAdapter = true)
data class RoomAnalysisResult(
    val roomType: String,
    val spaceSizeSqFt: Int,
    val detectedFurniture: List<String>,
    val paletteColorsHex: List<String>,
    val suggestions: String,
    val title: String
)
