package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        RoomProjectEntity::class,
        PlacerItemEntity::class,
        BudgetEstimateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun roomProjectDao(): RoomProjectDao
    abstract fun placerItemDao(): PlacerItemDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "interior_ai_database"
                )
                .fallbackToDestructiveMigration() // Simple migration strategy for prototype/dev
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
