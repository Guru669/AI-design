package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an Authenticated User (Module 1).
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val fullName: String,
    val preferences: String = "Modern, Scandinavian",
    val token: String = "",
    val isLoggedIn: Boolean = false
)

/**
 * Entity representing a room design project (Module 2, 3, 4).
 */
@Entity(tableName = "room_projects")
data class RoomProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val roomType: String, // Living Room, Bedroom, Office, Dining, Kitchen
    val imagePresetCode: String, // e.g. "preset_living_1"
    val customImageUri: String? = null, // URI of uploaded photo if any
    val createdDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val selectedStyle: String = "Modern", // Modern, Minimalist, Scandinavian, Luxury, Traditional
    val detectedElementsKey: String = "", // comma-separated of identified items
    val recommendedColorsHex: String = "", // comma-separated colors e.g. "#EADBC8,#102C57,#DAC0A3"
    val beforeAfterTransformationDesc: String = "" // generated redesigned notes
)

/**
 * Entity representing single virtually placed furniture items in an AR canvas (Module 7).
 */
@Entity(tableName = "placer_items")
data class PlacerItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val itemName: String, // Sofa, Table, Plant, Lamp, Painting, Chair
    val category: String, // Category to identify style or item
    val colorHex: String,
    val posX: Float = 0.5f, // percentage based (0f..1f) of canvas size
    val posY: Float = 0.5f,
    val rotation: Float = 0f, // degrees (0..360)
    val scale: Float = 1.0f
)

/**
 * Entity representing calculated budget renovations (Module 5).
 */
@Entity(tableName = "budget_estimates")
data class BudgetEstimateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int = 0,
    val title: String,
    val roomSizeSqFt: Int = 120,
    val paintQuality: String = "Standard", // Standard, Premium, Luxury
    val furnitureLevel: String = "Comfortable", // Minimal, Moderate, Premium
    val floorMaterial: String = "Oak Wood", // Laminate, Vinyl, Oak Wood, Italian Tile
    val paintCost: Double = 0.0,
    val furnitureCost: Double = 0.0,
    val flooringCost: Double = 0.0,
    val totalCost: Double = 0.0,
    val createdDate: Long = System.currentTimeMillis()
)
