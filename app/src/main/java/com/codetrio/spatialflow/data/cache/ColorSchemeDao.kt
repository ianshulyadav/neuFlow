package com.codetrio.spatialflow.data.cache

import androidx.room.*

@Entity(tableName = "color_schemes")
data class CachedColorScheme(
    @PrimaryKey val uri: String,
    val paletteStyle: String,
    val lightSchemeJson: String,
    val darkSchemeJson: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Dao
interface ColorSchemeDao {
    @Query("SELECT * FROM color_schemes WHERE uri = :uri AND paletteStyle = :style LIMIT 1")
    suspend fun get(uri: String, style: String): CachedColorScheme?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scheme: CachedColorScheme)

    @Query("DELETE FROM color_schemes WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM color_schemes WHERE cachedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    @Query("SELECT COUNT(*) FROM color_schemes")
    suspend fun count(): Int
}
