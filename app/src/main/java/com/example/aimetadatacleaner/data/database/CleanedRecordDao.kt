package com.example.aimetadatacleaner.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CleanedRecordDao {
    @Query("SELECT * FROM cleaned_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<CleanedRecordEntity>>

    @Query("SELECT COUNT(*) FROM cleaned_records")
    fun getRecordCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(tagsRemovedCount), 0) FROM cleaned_records")
    fun getTotalTagsRemoved(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: CleanedRecordEntity): Long

    @Query("DELETE FROM cleaned_records WHERE id = :id")
    suspend fun deleteRecord(id: Long)

    @Query("DELETE FROM cleaned_records")
    suspend fun clearAll()
}
