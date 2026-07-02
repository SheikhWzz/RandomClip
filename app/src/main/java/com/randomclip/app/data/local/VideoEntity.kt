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
    val folderUri: String,
    val scannedAt: Long,
)

@Dao
interface VideoDao {
    @Query("SELECT * FROM cached_videos WHERE folderUri = :folderUri")
    suspend fun getVideosForFolder(folderUri: String): List<VideoEntity>

    @Query("DELETE FROM cached_videos WHERE folderUri = :folderUri")
    suspend fun deleteForFolder(folderUri: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<VideoEntity>)
}
