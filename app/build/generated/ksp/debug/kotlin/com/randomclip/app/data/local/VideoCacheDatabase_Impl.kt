package com.randomclip.app.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class VideoCacheDatabase_Impl : VideoCacheDatabase() {
  private val _videoDao: Lazy<VideoDao> = lazy {
    VideoDao_Impl(this)
  }

  private val _favoriteDao: Lazy<FavoriteDao> = lazy {
    FavoriteDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(3, "fd2e327178b1a40636d95a08c17048fb", "4cde782b874a3748df96179d38b28ab1") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `cached_videos` (`uri` TEXT NOT NULL, `displayName` TEXT NOT NULL, `durationMs` INTEGER NOT NULL, `width` INTEGER NOT NULL, `height` INTEGER NOT NULL, `folderUri` TEXT NOT NULL, `scannedAt` INTEGER NOT NULL, `isPlayable` INTEGER NOT NULL, PRIMARY KEY(`uri`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `favorites` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `videoUri` TEXT NOT NULL, `timestampMs` INTEGER NOT NULL, `savedAt` INTEGER NOT NULL, `displayName` TEXT NOT NULL, `durationMs` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'fd2e327178b1a40636d95a08c17048fb')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `cached_videos`")
        connection.execSQL("DROP TABLE IF EXISTS `favorites`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsCachedVideos: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCachedVideos.put("uri", TableInfo.Column("uri", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCachedVideos.put("displayName", TableInfo.Column("displayName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCachedVideos.put("durationMs", TableInfo.Column("durationMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCachedVideos.put("width", TableInfo.Column("width", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCachedVideos.put("height", TableInfo.Column("height", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCachedVideos.put("folderUri", TableInfo.Column("folderUri", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCachedVideos.put("scannedAt", TableInfo.Column("scannedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCachedVideos.put("isPlayable", TableInfo.Column("isPlayable", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCachedVideos: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCachedVideos: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoCachedVideos: TableInfo = TableInfo("cached_videos", _columnsCachedVideos, _foreignKeysCachedVideos, _indicesCachedVideos)
        val _existingCachedVideos: TableInfo = read(connection, "cached_videos")
        if (!_infoCachedVideos.equals(_existingCachedVideos)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |cached_videos(com.randomclip.app.data.local.VideoEntity).
              | Expected:
              |""".trimMargin() + _infoCachedVideos + """
              |
              | Found:
              |""".trimMargin() + _existingCachedVideos)
        }
        val _columnsFavorites: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsFavorites.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFavorites.put("videoUri", TableInfo.Column("videoUri", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFavorites.put("timestampMs", TableInfo.Column("timestampMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFavorites.put("savedAt", TableInfo.Column("savedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFavorites.put("displayName", TableInfo.Column("displayName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFavorites.put("durationMs", TableInfo.Column("durationMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysFavorites: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesFavorites: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoFavorites: TableInfo = TableInfo("favorites", _columnsFavorites, _foreignKeysFavorites, _indicesFavorites)
        val _existingFavorites: TableInfo = read(connection, "favorites")
        if (!_infoFavorites.equals(_existingFavorites)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |favorites(com.randomclip.app.data.local.FavoriteEntity).
              | Expected:
              |""".trimMargin() + _infoFavorites + """
              |
              | Found:
              |""".trimMargin() + _existingFavorites)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "cached_videos", "favorites")
  }

  public override fun clearAllTables() {
    super.performClear(false, "cached_videos", "favorites")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(VideoDao::class, VideoDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(FavoriteDao::class, FavoriteDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun videoDao(): VideoDao = _videoDao.value

  public override fun favoriteDao(): FavoriteDao = _favoriteDao.value
}
