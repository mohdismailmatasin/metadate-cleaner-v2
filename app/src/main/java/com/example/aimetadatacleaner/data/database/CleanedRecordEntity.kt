package com.example.aimetadatacleaner.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cleaned_records")
data class CleanedRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalFileName: String,
    val cleanedFileName: String,
    val cleanedFilePath: String,
    val originalSizeBytes: Long,
    val cleanedSizeBytes: Long,
    val tagsRemovedCount: Int,
    val removedAiTags: Boolean,
    val removedGps: Boolean,
    val isVerifiedClean: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
