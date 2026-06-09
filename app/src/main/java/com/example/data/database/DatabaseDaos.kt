package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Access user profiles and auth sessions.
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    fun getLoggedInUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getLoggedInUser(): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isLoggedIn = 0")
    suspend fun logoutAllUsers()
    
    @Query("DELETE FROM users")
    suspend fun clearUserTable()
}

/**
 * Access uploaded room snapshots and analysis results.
 */
@Dao
interface RoomProjectDao {
    @Query("SELECT * FROM room_projects ORDER BY createdDate DESC")
    fun getAllProjectsFlow(): Flow<List<RoomProjectEntity>>

    @Query("SELECT * FROM room_projects WHERE id = :id LIMIT 1")
    fun getProjectByIdFlow(id: Int): Flow<RoomProjectEntity?>

    @Query("SELECT * FROM room_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Int): RoomProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: RoomProjectEntity): Long

    @Query("DELETE FROM room_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)
}

/**
 * Access individual items placed in real-time layout canvas.
 */
@Dao
interface PlacerItemDao {
    @Query("SELECT * FROM placer_items WHERE projectId = :projectId")
    fun getItemsForProjectFlow(projectId: Int): Flow<List<PlacerItemEntity>>

    @Query("SELECT * FROM placer_items WHERE projectId = :projectId")
    suspend fun getItemsForProject(projectId: Int): List<PlacerItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PlacerItemEntity)

    @Update
    suspend fun updateItem(item: PlacerItemEntity)

    @Query("DELETE FROM placer_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: Int)

    @Query("DELETE FROM placer_items WHERE projectId = :projectId")
    suspend fun deleteItemsByProject(projectId: Int)
}

/**
 * Access renovation budgets calculations.
 */
@Dao
interface BudgetDao {
    @Query("SELECT * FROM budget_estimates ORDER BY createdDate DESC")
    fun getAllEstimatesFlow(): Flow<List<BudgetEstimateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEstimate(estimate: BudgetEstimateEntity)

    @Query("DELETE FROM budget_estimates WHERE id = :id")
    suspend fun deleteEstimateById(id: Int)
    
    @Query("DELETE FROM budget_estimates WHERE projectId = :projectId")
    suspend fun deleteEstimatesByProject(projectId: Int)
}
