package com.example.aimetadatacleaner.data.repository

import android.content.Context
import android.net.Uri
import com.example.aimetadatacleaner.data.database.CleanedRecordDao
import com.example.aimetadatacleaner.data.database.CleanedRecordEntity
import com.example.aimetadatacleaner.data.model.CleanExecutionResult
import com.example.aimetadatacleaner.data.model.CleaningOptions
import com.example.aimetadatacleaner.data.model.ImageInspectionResult
import com.example.aimetadatacleaner.util.MetadataCleaner
import com.example.aimetadatacleaner.util.MetadataExtractor
import kotlinx.coroutines.flow.Flow
import java.io.File

class MetadataCleanerRepository(
    private val context: Context,
    private val dao: CleanedRecordDao
) {
    val allRecords: Flow<List<CleanedRecordEntity>> = dao.getAllRecords()
    val totalRecordsCount: Flow<Int> = dao.getRecordCount()
    val totalTagsRemoved: Flow<Int> = dao.getTotalTagsRemoved()

    fun inspectImage(uri: Uri): ImageInspectionResult {
        return MetadataExtractor.inspectImage(context, uri)
    }

    suspend fun cleanSingleImage(
        uri: Uri,
        options: CleaningOptions
    ): CleanExecutionResult {
        val result = MetadataCleaner.cleanImage(context, uri, options)
        if (result.success && result.cleanedFilePath != null) {
            dao.insertRecord(
                CleanedRecordEntity(
                    originalFileName = result.originalFileName,
                    cleanedFileName = result.cleanedFileName,
                    cleanedFilePath = result.cleanedFilePath,
                    originalSizeBytes = result.originalSizeBytes,
                    cleanedSizeBytes = result.cleanedSizeBytes,
                    tagsRemovedCount = result.tagsRemovedCount,
                    removedAiTags = result.removedAiTags,
                    removedGps = result.removedGps,
                    isVerifiedClean = result.isVerifiedClean
                )
            )
        }
        return result
    }

    suspend fun saveToGallery(filePath: String): Uri? {
        val file = File(filePath)
        if (!file.exists()) return null
        return MetadataCleaner.saveToGallery(context, file)
    }

    fun shareImage(uri: Uri, fileName: String) {
        MetadataCleaner.shareImage(context, uri, fileName)
    }

    suspend fun deleteRecord(id: Long, filePath: String?) {
        if (filePath != null) {
            try {
                File(filePath).delete()
            } catch (_: Exception) {
            }
        }
        dao.deleteRecord(id)
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }
}
