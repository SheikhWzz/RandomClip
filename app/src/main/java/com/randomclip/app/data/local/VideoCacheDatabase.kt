package com.randomclip.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [VideoEntity::class, FavoriteEntity::class], version = 3, exportSchema = false)
abstract class VideoCacheDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var instance: VideoCacheDatabase? = null

        fun getInstance(context: Context): VideoCacheDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VideoCacheDatabase::class.java,
                    "video_cache.db",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
