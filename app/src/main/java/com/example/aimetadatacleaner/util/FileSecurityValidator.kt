package com.example.aimetadatacleaner.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.InputStream

enum class FormatSupportLevel(val label: String, val badgeColorHex: Long) {
    SUPPORTED("Supported", 0xFF10B981),          // Full scan, clean, and verification
    PARTIALLY_SUPPORTED("Partially Supported", 0xFFF59E0B), // Scanning & partial metadata stripping
    SCAN_ONLY("Scan Only", 0xFF6366F1),          // Metadata extraction & inspection only
    NOT_SUPPORTED("Not Supported", 0xFFEF4444)   // Cannot safely process
}

enum class FileCategory(val displayName: String) {
    IMAGE("Image"),
    RAW("RAW Image"),
    VIDEO("Video"),
    DOCUMENT("Document"),
    AUDIO("Audio"),
    UNKNOWN("Unknown")
}

data class FileValidationResult(
    val isValid: Boolean,
    val detectedFormat: String,
    val mimeType: String,
    val fileCategory: FileCategory,
    val supportLevel: FormatSupportLevel,
    val fileSizeBytes: Long,
    val sanitizedFileName: String,
    val statusMessage: String,
    val isDecompressionRisk: Boolean = false
)

object FileSecurityValidator {

    private const val MAX_ALLOWED_FILE_SIZE_BYTES = 150L * 1024L * 1024L // 150 MB

    // Magic byte signatures
    private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte())
    private val GIF_MAGIC_87 = "GIF87a".toByteArray(Charsets.US_ASCII)
    private val GIF_MAGIC_89 = "GIF89a".toByteArray(Charsets.US_ASCII)
    private val PDF_MAGIC = "%PDF-".toByteArray(Charsets.US_ASCII)
    private val TIFF_LE_MAGIC = byteArrayOf(0x49.toByte(), 0x49.toByte(), 0x2A.toByte(), 0x00.toByte())
    private val TIFF_BE_MAGIC = byteArrayOf(0x4D.toByte(), 0x4D.toByte(), 0x00.toByte(), 0x2A.toByte())

    fun validateUri(context: Context, uri: Uri): FileValidationResult {
        var rawName = "uploaded_file"
        var size: Long = 0

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) rawName = cursor.getString(nameIndex) ?: rawName
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {
        }

        val sanitizedName = sanitizeFilename(rawName)

        if (size > MAX_ALLOWED_FILE_SIZE_BYTES) {
            return FileValidationResult(
                isValid = false,
                detectedFormat = "Oversized File",
                mimeType = "application/octet-stream",
                fileCategory = FileCategory.UNKNOWN,
                supportLevel = FormatSupportLevel.NOT_SUPPORTED,
                fileSizeBytes = size,
                sanitizedFileName = sanitizedName,
                statusMessage = "File exceeds the 150MB security limit to prevent resource exhaustion."
            )
        }

        // Read header bytes for magic signature validation
        val headerBytes = ByteArray(32)
        var bytesRead = 0
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                bytesRead = stream.read(headerBytes)
            }
        } catch (e: Exception) {
            return FileValidationResult(
                isValid = false,
                detectedFormat = "Unreadable",
                mimeType = "application/octet-stream",
                fileCategory = FileCategory.UNKNOWN,
                supportLevel = FormatSupportLevel.NOT_SUPPORTED,
                fileSizeBytes = size,
                sanitizedFileName = sanitizedName,
                statusMessage = "Unable to read file header: ${e.message}"
            )
        }

        if (bytesRead < 4) {
            return FileValidationResult(
                isValid = false,
                detectedFormat = "Empty / Corrupted",
                mimeType = "application/octet-stream",
                fileCategory = FileCategory.UNKNOWN,
                supportLevel = FormatSupportLevel.NOT_SUPPORTED,
                fileSizeBytes = size,
                sanitizedFileName = sanitizedName,
                statusMessage = "File appears corrupted or too small to contain valid metadata headers."
            )
        }

        return detectSignatureAndCategory(headerBytes, bytesRead, sanitizedName, size)
    }

    fun detectSignatureAndCategory(
        bytes: ByteArray,
        length: Int,
        fileName: String,
        fileSizeBytes: Long
    ): FileValidationResult {
        val ext = fileName.substringAfterLast(".", "").lowercase()

        // 1. JPEG: FF D8 FF
        if (length >= 3 && bytes[0] == JPEG_MAGIC[0] && bytes[1] == JPEG_MAGIC[1] && bytes[2] == JPEG_MAGIC[2]) {
            return FileValidationResult(
                isValid = true,
                detectedFormat = "JPEG Image",
                mimeType = "image/jpeg",
                fileCategory = FileCategory.IMAGE,
                supportLevel = FormatSupportLevel.SUPPORTED,
                fileSizeBytes = fileSizeBytes,
                sanitizedFileName = fileName,
                statusMessage = "Valid JPEG image signature verified via magic bytes."
            )
        }

        // 2. PNG: 89 50 4E 47 0D 0A 1A 0A
        if (length >= 8 && startsWith(bytes, PNG_MAGIC)) {
            return FileValidationResult(
                isValid = true,
                detectedFormat = "PNG Image",
                mimeType = "image/png",
                fileCategory = FileCategory.IMAGE,
                supportLevel = FormatSupportLevel.SUPPORTED,
                fileSizeBytes = fileSizeBytes,
                sanitizedFileName = fileName,
                statusMessage = "Valid PNG image signature verified via magic bytes."
            )
        }

        // 3. WebP: RIFF .... WEBP
        if (length >= 12 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) {
            return FileValidationResult(
                isValid = true,
                detectedFormat = "WebP Image",
                mimeType = "image/webp",
                fileCategory = FileCategory.IMAGE,
                supportLevel = FormatSupportLevel.SUPPORTED,
                fileSizeBytes = fileSizeBytes,
                sanitizedFileName = fileName,
                statusMessage = "Valid WebP container signature verified."
            )
        }

        // 4. ISO Base Media File Format: ftyp (HEIC, AVIF, MP4, MOV)
        if (length >= 12 &&
            bytes[4] == 'f'.code.toByte() && bytes[5] == 't'.code.toByte() &&
            bytes[6] == 'y'.code.toByte() && bytes[7] == 'p'.code.toByte()
        ) {
            val majorBrand = String(bytes, 8, 4, Charsets.US_ASCII)
            return when {
                majorBrand.startsWith("heic") || majorBrand.startsWith("heix") || majorBrand.startsWith("mif1") -> {
                    FileValidationResult(
                        isValid = true,
                        detectedFormat = "HEIC Image",
                        mimeType = "image/heic",
                        fileCategory = FileCategory.IMAGE,
                        supportLevel = FormatSupportLevel.SUPPORTED,
                        fileSizeBytes = fileSizeBytes,
                        sanitizedFileName = fileName,
                        statusMessage = "High Efficiency Image File (HEIC) verified."
                    )
                }
                majorBrand.startsWith("avif") -> {
                    FileValidationResult(
                        isValid = true,
                        detectedFormat = "AVIF Image",
                        mimeType = "image/avif",
                        fileCategory = FileCategory.IMAGE,
                        supportLevel = FormatSupportLevel.SUPPORTED,
                        fileSizeBytes = fileSizeBytes,
                        sanitizedFileName = fileName,
                        statusMessage = "AV1 Image File (AVIF) verified."
                    )
                }
                majorBrand.startsWith("isom") || majorBrand.startsWith("mp4") || majorBrand.startsWith("M4V") -> {
                    FileValidationResult(
                        isValid = true,
                        detectedFormat = "MP4 Video",
                        mimeType = "video/mp4",
                        fileCategory = FileCategory.VIDEO,
                        supportLevel = FormatSupportLevel.SUPPORTED,
                        fileSizeBytes = fileSizeBytes,
                        sanitizedFileName = fileName,
                        statusMessage = "MP4 video supported for metadata inspection and container sanitization."
                    )
                }
                majorBrand.startsWith("qt  ") -> {
                    FileValidationResult(
                        isValid = true,
                        detectedFormat = "QuickTime Video (MOV)",
                        mimeType = "video/quicktime",
                        fileCategory = FileCategory.VIDEO,
                        supportLevel = FormatSupportLevel.SUPPORTED,
                        fileSizeBytes = fileSizeBytes,
                        sanitizedFileName = fileName,
                        statusMessage = "QuickTime MOV video supported for metadata inspection and container sanitization."
                    )
                }
                else -> {
                    FileValidationResult(
                        isValid = true,
                        detectedFormat = "ISO Media ($majorBrand)",
                        mimeType = "video/mp4",
                        fileCategory = FileCategory.VIDEO,
                        supportLevel = FormatSupportLevel.SUPPORTED,
                        fileSizeBytes = fileSizeBytes,
                        sanitizedFileName = fileName,
                        statusMessage = "ISO base media container supported for inspection and sanitization."
                    )
                }
            }
        }

        // 5. TIFF / DNG / CR2
        if (length >= 4 && (startsWith(bytes, TIFF_LE_MAGIC) || startsWith(bytes, TIFF_BE_MAGIC))) {
            val isDng = ext == "dng"
            val isCr2 = ext == "cr2" || (length >= 10 && bytes[8] == 'C'.code.toByte() && bytes[9] == 'R'.code.toByte())
            val name = if (isCr2) "Canon RAW (CR2)" else if (isDng) "Adobe Digital Negative (DNG)" else "TIFF Image"
            return FileValidationResult(
                isValid = true,
                detectedFormat = name,
                mimeType = if (isDng || isCr2) "image/x-adobe-dng" else "image/tiff",
                fileCategory = if (isDng || isCr2) FileCategory.RAW else FileCategory.IMAGE,
                supportLevel = if (isDng) FormatSupportLevel.PARTIALLY_SUPPORTED else FormatSupportLevel.SUPPORTED,
                fileSizeBytes = fileSizeBytes,
                sanitizedFileName = fileName,
                statusMessage = "TIFF/RAW structure signature verified."
            )
        }

        // 6. PDF: %PDF-
        if (length >= 5 && startsWith(bytes, PDF_MAGIC)) {
            return FileValidationResult(
                isValid = true,
                detectedFormat = "PDF Document",
                mimeType = "application/pdf",
                fileCategory = FileCategory.DOCUMENT,
                supportLevel = FormatSupportLevel.SCAN_ONLY,
                fileSizeBytes = fileSizeBytes,
                sanitizedFileName = fileName,
                statusMessage = "PDF document verified. Metadata inspection supported."
            )
        }

        // Fallback by extension with caution
        return when (ext) {
            "jpg", "jpeg" -> FileValidationResult(
                isValid = true,
                detectedFormat = "JPEG (Extension match)",
                mimeType = "image/jpeg",
                fileCategory = FileCategory.IMAGE,
                supportLevel = FormatSupportLevel.SUPPORTED,
                fileSizeBytes = fileSizeBytes,
                sanitizedFileName = fileName,
                statusMessage = "Processed via file extension fallback."
            )
            "png" -> FileValidationResult(
                isValid = true,
                detectedFormat = "PNG (Extension match)",
                mimeType = "image/png",
                fileCategory = FileCategory.IMAGE,
                supportLevel = FormatSupportLevel.SUPPORTED,
                fileSizeBytes = fileSizeBytes,
                sanitizedFileName = fileName,
                statusMessage = "Processed via file extension fallback."
            )
            "webp" -> FileValidationResult(
                isValid = true,
                detectedFormat = "WebP (Extension match)",
                mimeType = "image/webp",
                fileCategory = FileCategory.IMAGE,
                supportLevel = FormatSupportLevel.SUPPORTED,
                fileSizeBytes = fileSizeBytes,
                sanitizedFileName = fileName,
                statusMessage = "Processed via file extension fallback."
            )
            "pdf" -> FileValidationResult(
                isValid = true,
                detectedFormat = "PDF Document",
                mimeType = "application/pdf",
                fileCategory = FileCategory.DOCUMENT,
                supportLevel = FormatSupportLevel.SCAN_ONLY,
                fileSizeBytes = fileSizeBytes,
                sanitizedFileName = fileName,
                statusMessage = "PDF Document format."
            )
            "mp4", "mov", "m4v" -> FileValidationResult(
                isValid = true,
                detectedFormat = "Video File",
                mimeType = "video/mp4",
                fileCategory = FileCategory.VIDEO,
                supportLevel = FormatSupportLevel.SCAN_ONLY,
                fileSizeBytes = fileSizeBytes,
                sanitizedFileName = fileName,
                statusMessage = "Video media file."
            )
            "docx", "xlsx", "pptx" -> FileValidationResult(
                isValid = true,
                detectedFormat = "Office Document (.${ext.uppercase()})",
                mimeType = "application/octet-stream",
                fileCategory = FileCategory.DOCUMENT,
                supportLevel = FormatSupportLevel.SCAN_ONLY,
                fileSizeBytes = fileSizeBytes,
                sanitizedFileName = fileName,
                statusMessage = "Document archive. Extracting embedded properties."
            )
            "mp3", "m4a", "wav" -> FileValidationResult(
                isValid = true,
                detectedFormat = "Audio File (.${ext.uppercase()})",
                mimeType = "audio/mpeg",
                fileCategory = FileCategory.AUDIO,
                supportLevel = FormatSupportLevel.SCAN_ONLY,
                fileSizeBytes = fileSizeBytes,
                sanitizedFileName = fileName,
                statusMessage = "Audio media file."
            )
            else -> FileValidationResult(
                isValid = false,
                detectedFormat = "Unsupported Format ($ext)",
                mimeType = "application/octet-stream",
                fileCategory = FileCategory.UNKNOWN,
                supportLevel = FormatSupportLevel.NOT_SUPPORTED,
                fileSizeBytes = fileSizeBytes,
                sanitizedFileName = fileName,
                statusMessage = "Format is not currently supported for automated metadata cleaning."
            )
        }
    }

    /**
     * Sanitizes filenames to prevent path traversal (../), null byte injection,
     * and unsafe filesystem characters.
     */
    fun sanitizeFilename(inputName: String): String {
        var clean = inputName.trim()
        clean = clean.replace("\u0000", "") // Strip null bytes
        clean = clean.replace(Regex("[/\\\\?%*:|\"<>]"), "_") // Replace path separators & illegal chars
        clean = clean.replace(Regex("\\.{2,}"), "_") // Prevent .. traversal
        if (clean.startsWith(".")) clean = "_$clean"
        if (clean.isBlank()) clean = "file_${System.currentTimeMillis()}"
        return clean.take(120) // Enforce sane length
    }

    /**
     * Confirms that a target file path resides strictly inside the designated directory,
     * protecting against symlink attacks and path traversal.
     */
    fun isPathSafe(baseDir: File, targetFile: File): Boolean {
        return try {
            val baseCanonical = baseDir.canonicalPath
            val targetCanonical = targetFile.canonicalPath
            targetCanonical.startsWith(baseCanonical)
        } catch (_: Exception) {
            false
        }
    }

    private fun startsWith(source: ByteArray, prefix: ByteArray): Boolean {
        if (source.size < prefix.size) return false
        for (i in prefix.indices) {
            if (source[i] != prefix[i]) return false
        }
        return true
    }
}
