package com.example.aimetadatacleaner.data.model

import android.net.Uri

data class CleaningOptions(
    val stripAll: Boolean = true,
    val stripAiMetadata: Boolean = true,
    val stripLocationGps: Boolean = true,
    val stripCameraDevice: Boolean = true,
    val stripTimestamps: Boolean = true,
    val stripAuthorCopyright: Boolean = true,
    val format: OutputFormat = OutputFormat.PRESERVE_ORIGINAL,
    val quality: Int = 98
)

enum class OutputFormat(val displayName: String, val extension: String, val mimeType: String) {
    PRESERVE_ORIGINAL("Same as Original", "", ""),
    JPEG("JPEG (.jpg)", "jpg", "image/jpeg"),
    PNG("PNG (.png)", "png", "image/png"),
    WEBP("WebP (.webp)", "webp", "image/webp")
}

data class CleanExecutionResult(
    val success: Boolean,
    val originalUri: Uri,
    val cleanedUri: Uri?,
    val cleanedFilePath: String?,
    val originalFileName: String,
    val cleanedFileName: String,
    val originalSizeBytes: Long,
    val cleanedSizeBytes: Long,
    val tagsRemovedCount: Int,
    val removedAiTags: Boolean,
    val removedGps: Boolean,
    val isVerifiedClean: Boolean = false,
    val verificationReport: VerificationReport? = null,
    val beforeAfterSummary: List<BeforeAfterItem> = emptyList(),
    val inspectionResult: ImageInspectionResult? = null,
    val privacyReport: PrivacyInspectionReport? = null,
    val errorMessage: String? = null
)
