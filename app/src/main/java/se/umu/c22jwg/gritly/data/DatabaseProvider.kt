package se.umu.c22jwg.gritly.data

import android.content.Context
import androidx.room.Room

/**
 * Singleton provider for the Room database instance.
 * Ensures that only one instance of the database is created throughout the application.
 */
object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "gritly_database"
            )
                .fallbackToDestructiveMigration(true)
                .build()
            INSTANCE = instance
            instance
        }
    }
}