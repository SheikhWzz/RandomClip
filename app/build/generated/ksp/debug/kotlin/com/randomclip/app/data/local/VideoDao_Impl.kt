package com.randomclip.app.`data`.local

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class VideoDao_Impl(
  __db: RoomDatabase,
) : VideoDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfVideoEntity: EntityInsertAdapter<VideoEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfVideoEntity = object : EntityInsertAdapter<VideoEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `cached_videos` (`uri`,`displayName`,`durationMs`,`width`,`height`,`folderUri`,`scannedAt`,`isPlayable`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: VideoEntity) {
        statement.bindText(1, entity.uri)
        statement.bindText(2, entity.displayName)
        statement.bindLong(3, entity.durationMs)
        statement.bindLong(4, entity.width.toLong())
        statement.bindLong(5, entity.height.toLong())
        statement.bindText(6, entity.folderUri)
        statement.bindLong(7, entity.scannedAt)
        val _tmp: Int = if (entity.isPlayable) 1 else 0
        statement.bindLong(8, _tmp.toLong())
      }
    }
  }

  public override suspend fun insertAll(videos: List<VideoEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfVideoEntity.insert(_connection, videos)
  }

  public override suspend fun getVideosForFolder(folderUri: String): List<VideoEntity> {
    val _sql: String = "SELECT * FROM cached_videos WHERE folderUri = ? AND isPlayable = 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, folderUri)
        val _columnIndexOfUri: Int = getColumnIndexOrThrow(_stmt, "uri")
        val _columnIndexOfDisplayName: Int = getColumnIndexOrThrow(_stmt, "displayName")
        val _columnIndexOfDurationMs: Int = getColumnIndexOrThrow(_stmt, "durationMs")
        val _columnIndexOfWidth: Int = getColumnIndexOrThrow(_stmt, "width")
        val _columnIndexOfHeight: Int = getColumnIndexOrThrow(_stmt, "height")
        val _columnIndexOfFolderUri: Int = getColumnIndexOrThrow(_stmt, "folderUri")
        val _columnIndexOfScannedAt: Int = getColumnIndexOrThrow(_stmt, "scannedAt")
        val _columnIndexOfIsPlayable: Int = getColumnIndexOrThrow(_stmt, "isPlayable")
        val _result: MutableList<VideoEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: VideoEntity
          val _tmpUri: String
          _tmpUri = _stmt.getText(_columnIndexOfUri)
          val _tmpDisplayName: String
          _tmpDisplayName = _stmt.getText(_columnIndexOfDisplayName)
          val _tmpDurationMs: Long
          _tmpDurationMs = _stmt.getLong(_columnIndexOfDurationMs)
          val _tmpWidth: Int
          _tmpWidth = _stmt.getLong(_columnIndexOfWidth).toInt()
          val _tmpHeight: Int
          _tmpHeight = _stmt.getLong(_columnIndexOfHeight).toInt()
          val _tmpFolderUri: String
          _tmpFolderUri = _stmt.getText(_columnIndexOfFolderUri)
          val _tmpScannedAt: Long
          _tmpScannedAt = _stmt.getLong(_columnIndexOfScannedAt)
          val _tmpIsPlayable: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsPlayable).toInt()
          _tmpIsPlayable = _tmp != 0
          _item = VideoEntity(_tmpUri,_tmpDisplayName,_tmpDurationMs,_tmpWidth,_tmpHeight,_tmpFolderUri,_tmpScannedAt,_tmpIsPlayable)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllPlayableVideos(): List<VideoEntity> {
    val _sql: String = "SELECT * FROM cached_videos WHERE isPlayable = 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfUri: Int = getColumnIndexOrThrow(_stmt, "uri")
        val _columnIndexOfDisplayName: Int = getColumnIndexOrThrow(_stmt, "displayName")
        val _columnIndexOfDurationMs: Int = getColumnIndexOrThrow(_stmt, "durationMs")
        val _columnIndexOfWidth: Int = getColumnIndexOrThrow(_stmt, "width")
        val _columnIndexOfHeight: Int = getColumnIndexOrThrow(_stmt, "height")
        val _columnIndexOfFolderUri: Int = getColumnIndexOrThrow(_stmt, "folderUri")
        val _columnIndexOfScannedAt: Int = getColumnIndexOrThrow(_stmt, "scannedAt")
        val _columnIndexOfIsPlayable: Int = getColumnIndexOrThrow(_stmt, "isPlayable")
        val _result: MutableList<VideoEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: VideoEntity
          val _tmpUri: String
          _tmpUri = _stmt.getText(_columnIndexOfUri)
          val _tmpDisplayName: String
          _tmpDisplayName = _stmt.getText(_columnIndexOfDisplayName)
          val _tmpDurationMs: Long
          _tmpDurationMs = _stmt.getLong(_columnIndexOfDurationMs)
          val _tmpWidth: Int
          _tmpWidth = _stmt.getLong(_columnIndexOfWidth).toInt()
          val _tmpHeight: Int
          _tmpHeight = _stmt.getLong(_columnIndexOfHeight).toInt()
          val _tmpFolderUri: String
          _tmpFolderUri = _stmt.getText(_columnIndexOfFolderUri)
          val _tmpScannedAt: Long
          _tmpScannedAt = _stmt.getLong(_columnIndexOfScannedAt)
          val _tmpIsPlayable: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsPlayable).toInt()
          _tmpIsPlayable = _tmp != 0
          _item = VideoEntity(_tmpUri,_tmpDisplayName,_tmpDurationMs,_tmpWidth,_tmpHeight,_tmpFolderUri,_tmpScannedAt,_tmpIsPlayable)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun markAsUnplayable(uri: String) {
    val _sql: String = "UPDATE cached_videos SET isPlayable = 0 WHERE uri = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, uri)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteForFolder(folderUri: String) {
    val _sql: String = "DELETE FROM cached_videos WHERE folderUri = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, folderUri)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
