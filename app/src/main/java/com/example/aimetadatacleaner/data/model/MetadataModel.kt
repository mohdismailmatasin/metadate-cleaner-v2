package com.example.aimetadatacleaner.data.model

import android.net.Uri
import com.example.aimetadatacleaner.util.FileValidationResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MetadataStandard(val displayName: String, val shortName: String) {
    EXIF("Exchangeable Image File Format (EXIF)", "EXIF"),
    GPS("Geotagging & Coordinates", "GPS"),
    XMP("Extensible Metadata Platform (XMP)", "XMP"),
    IPTC("IPTC Photo Metadata", "IPTC"),
    AI_METADATA("AI Generation Parameters", "AI Gen"),
    C2PA("Content Credentials / C2PA Manifest", "C2PA"),
    EMBEDDED_OTHER("Embedded Structure & Color Profiles", "Other")
}

enum class MetadataCategory(val displayName: String, val iconDescription: String) {
    AI_PROVENANCE("AI & Generation Evidence", "AI Prompts, Models, Seeds & Parameters"),
    LOCATION("Location & GPS", "Latitude, Longitude & Geotags"),
    CAMERA_DEVICE("Camera & Device", "Hardware make, model & lens settings"),
    TIMESTAMPS_FILE("Timestamps & Dates", "Creation date, timestamps & format info"),
    AUTHOR_SYSTEM("Author & Software", "Artist, copyright & editing software"),
    EMBEDDED_PROFILES("Embedded Profiles & Thumbnails", "ICC color profile & metadata chunks")
}

enum class PrivacyExposure(val label: String, val meterPercentage: Int, val description: String) {
    LOW(
        label = "LOW",
        meterPercentage = 25,
        description = "No significant privacy or identifying metadata detected."
    ),
    MEDIUM(
        label = "MEDIUM",
        meterPercentage = 60,
        description = "Metadata exists but contains limited identifying information (e.g. camera model or timestamps)."
    ),
    HIGH(
        label = "HIGH",
        meterPercentage = 95,
        description = "Sensitive information detected (precise GPS coordinates, author identity, or full AI generation prompts)."
    )
}

enum class RemovalStatus(val label: String, val symbol: String) {
    REMOVED("Removed", "✓"),
    PRESERVED("Preserved", "⚠"),
    NOT_PRESENT("Not present", "—"),
    UNABLE_TO_VERIFY("Unable to verify", "?")
}

data class BeforeAfterItem(
    val fieldName: String,
    val beforeValue: String?,
    val afterStatus: RemovalStatus,
    val note: String = ""
)

data class MetadataEntry(
    val standard: MetadataStandard = MetadataStandard.EXIF,
    val category: MetadataCategory,
    val key: String,
    val value: String,
    val isSensitive: Boolean = false,
    val description: String = ""
)

data class AiGenerationMetadata(
    val detectedEngine: String,
    val evidenceSummary: String = "AI generation metadata detected in file header/chunks.",
    val confidence: String = "Metadata evidence only",
    val positivePrompt: String? = null,
    val negativePrompt: String? = null,
    val steps: String? = null,
    val sampler: String? = null,
    val cfgScale: String? = null,
    val seed: String? = null,
    val model: String? = null,
    val dimensions: String? = null,
    val loras: List<String> = emptyList(),
    val otherParameters: Map<String, String> = emptyMap(),
    val rawParametersText: String? = null,
    val metaTagsInvolved: List<String> = emptyList(),
    val hasC2paManifest: Boolean = false,
    val c2paDetails: String? = null
)

data class VerificationCheck(
    val category: String,
    val passed: Boolean,
    val details: String
)

data class VerificationReport(
    val isVerifiedClean: Boolean,
    val statusText: String, // "VERIFIED CLEAN" or "VERIFICATION INCOMPLETE"
    val checkedCategories: List<VerificationCheck>,
    val remainingFieldsCount: Int,
    val remainingFields: List<String>,
    val scanTimestamp: Long = System.currentTimeMillis()
)

data class ImageInspectionResult(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val width: Int,
    val height: Int,
    val entries: List<MetadataEntry>,
    val riskLevel: PrivacyExposure,
    val riskReasons: List<String>,
    val hasAiMetadata: Boolean,
    val hasGpsLocation: Boolean,
    val hasC2pa: Boolean = false,
    val rawPromptText: String? = null,
    val aiMetadata: AiGenerationMetadata? = null,
    val validationResult: FileValidationResult? = null
)

data class PrivacyInspectionReport(
    val fileName: String,
    val fileSizeBytes: Long,
    val mimeType: String,
    val generatedDate: String,
    val privacyExposure: PrivacyExposure,
    val detectedExif: Boolean,
    val detectedGps: Boolean,
    val detectedXmp: Boolean,
    val detectedIptc: Boolean,
    val detectedAiMetadata: Boolean,
    val detectedC2pa: Boolean,
    val sensitiveFieldsCount: Int,
    val totalFieldsFound: Int,
    val cleaningStatus: String,
    val verificationStatus: String,
    val isVerifiedClean: Boolean,
    val verificationChecks: List<VerificationCheck> = emptyList(),
    val beforeAfterSummary: List<BeforeAfterItem> = emptyList(),
    val aiDetails: String? = null
) {
    fun toFormattedText(): String {
        val sb = StringBuilder()
        sb.appendLine("==========================================")
        sb.appendLine("       PRIVACY INSPECTION REPORT")
        sb.appendLine("       Metadata Cleaner & Sanitizer")
        sb.appendLine("==========================================")
        sb.appendLine("File:           $fileName")
        sb.appendLine("Size:           ${fileSizeBytes / 1024} KB")
        sb.appendLine("Format:         $mimeType")
        sb.appendLine("Generated:      $generatedDate")
        sb.appendLine("Exposure Level: ${privacyExposure.name}")
        sb.appendLine("------------------------------------------")
        sb.appendLine("METADATA DETECTION BREAKDOWN:")
        sb.appendLine("  • EXIF:         ${if (detectedExif) "DETECTED [✓]" else "NOT FOUND [-]"}")
        sb.appendLine("  • GPS Geotags:  ${if (detectedGps) "DETECTED [⚠ HIGH EXPOSURE]" else "NOT FOUND [-]"}")
        sb.appendLine("  • XMP Data:     ${if (detectedXmp) "DETECTED [✓]" else "NOT FOUND [-]"}")
        sb.appendLine("  • IPTC Data:    ${if (detectedIptc) "DETECTED [✓]" else "NOT FOUND [-]"}")
        sb.appendLine("  • AI Metadata:  ${if (detectedAiMetadata) "DETECTED (Evidence only)" else "NOT FOUND [-]"}")
        sb.appendLine("  • C2PA Manifest:${if (detectedC2pa) "DETECTED [✓]" else "NOT FOUND [-]"}")
        sb.appendLine("  • Sensitive Fields Count: $sensitiveFieldsCount")
        sb.appendLine("------------------------------------------")
        if (detectedAiMetadata && aiDetails != null) {
            sb.appendLine("AI DETECTION EVIDENCE:")
            sb.appendLine(aiDetails)
            sb.appendLine("Note: Confidence is metadata evidence only, not an absolute generative attribution.")
            sb.appendLine("------------------------------------------")
        }
        sb.appendLine("CLEANING & SANITIZATION:")
        sb.appendLine("  Status:       $cleaningStatus")
        sb.appendLine("  Verification: $verificationStatus")
        sb.appendLine("------------------------------------------")
        if (verificationChecks.isNotEmpty()) {
            sb.appendLine("INDEPENDENT SECONDARY VERIFICATION:")
            verificationChecks.forEach { check ->
                val mark = if (check.passed) "[PASSED ✓]" else "[FAILED ✗]"
                sb.appendLine("  $mark ${check.category}: ${check.details}")
            }
            sb.appendLine("------------------------------------------")
        }
        sb.appendLine("PRIVACY NOTICE:")
        sb.appendLine("All processing was performed locally on-device.")
        sb.appendLine("No files or telemetry were transmitted over the network.")
        sb.appendLine("==========================================")
        return sb.toString()
    }

    fun toJsonString(): String {
        val checksJson = verificationChecks.joinToString(",") {
            """{"category":"${it.category}","passed":${it.passed},"details":"${it.details.replace("\"", "\\\"")}"}"""
        }
        return """
        {
          "report_type": "PRIVACY_INSPECTION_REPORT",
          "file_name": "$fileName",
          "file_size_bytes": $fileSizeBytes,
          "mime_type": "$mimeType",
          "generated_date": "$generatedDate",
          "privacy_exposure": "${privacyExposure.name}",
          "metadata_detected": {
            "exif": $detectedExif,
            "gps": $detectedGps,
            "xmp": $detectedXmp,
            "iptc": $detectedIptc,
            "ai_metadata": $detectedAiMetadata,
            "c2pa": $detectedC2pa
          },
          "sensitive_fields_count": $sensitiveFieldsCount,
          "total_fields_found": $totalFieldsFound,
          "cleaning_status": "$cleaningStatus",
          "verification_status": "$verificationStatus",
          "is_verified_clean": $isVerifiedClean,
          "verification_checks": [$checksJson]
        }
        """.trimIndent()
    }
}
