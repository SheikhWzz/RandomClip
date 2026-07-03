package com.randomclip.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "cached_videos")
data class VideoEntity(
    @PrimaryKey val uri: String,
    val displayName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val folderUri: String,
    val scannedAt: Long,
    val isPlayable: Boolean = true,
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoUri: String,
    val timestampMs: Long,
    val savedAt: Long,
    val displayName: String,
    val durationMs: Long,
)

@Dao
interface VideoDao {
    @Query("SELECT * FROM cached_videos WHERE folderUri = :folderUri AND isPlayable = 1")
    suspend fun getVideosForFolder(folderUri: String): List<VideoEntity>

    @Query("SELECT * FROM cached_videos WHERE isPlayable = 1")
    suspend fun getAllPlayableVideos(): List<VideoEntity>

    @Query("UPDATE cached_videos SET isPlayable = 0 WHERE uri = :uri")
    suspend fun markAsUnplayable(uri: String)

    @Query("DELETE FROM cached_videos WHERE folderUri = :folderUri")
    suspend fun deleteForFolder(folderUri: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<VideoEntity>)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY savedAt DESC")
    suspend fun getAllFavorites(): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun delete(id: Long)
}
