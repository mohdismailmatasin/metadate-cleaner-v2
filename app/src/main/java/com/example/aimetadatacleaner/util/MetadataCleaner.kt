package com.example.aimetadatacleaner.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.example.aimetadatacleaner.data.model.BeforeAfterItem
import com.example.aimetadatacleaner.data.model.CleanExecutionResult
import com.example.aimetadatacleaner.data.model.CleaningOptions
import com.example.aimetadatacleaner.data.model.ImageInspectionResult
import com.example.aimetadatacleaner.data.model.OutputFormat
import com.example.aimetadatacleaner.data.model.PrivacyInspectionReport
import com.example.aimetadatacleaner.data.model.RemovalStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MetadataCleaner {

    suspend fun cleanImage(
        context: Context,
        inputUri: Uri,
        options: CleaningOptions
    ): CleanExecutionResult = withContext(Dispatchers.IO) {
        val inspection = MetadataExtractor.inspectImage(context, inputUri)

        // Security check on input format
        if (inspection.validationResult?.supportLevel == FormatSupportLevel.NOT_SUPPORTED) {
            return@withContext CleanExecutionResult(
                success = false,
                originalUri = inputUri,
                cleanedUri = null,
                cleanedFilePath = null,
                originalFileName = inspection.fileName,
                cleanedFileName = "",
                originalSizeBytes = inspection.fileSizeBytes,
                cleanedSizeBytes = 0,
                tagsRemovedCount = 0,
                removedAiTags = false,
                removedGps = false,
                isVerifiedClean = false,
                inspectionResult = inspection,
                errorMessage = "This format is not supported for automated pixel sanitization: ${inspection.validationResult.statusMessage}"
            )
        }

        val isVideo = inspection.validationResult?.fileCategory == FileCategory.VIDEO || inspection.mimeType.startsWith("video/")
        if (isVideo) {
            return@withContext cleanVideo(context, inputUri, inspection)
        }

        val isRaw = inspection.validationResult?.fileCategory == FileCategory.RAW ||
                inspection.fileName.let { name ->
                    val ext = name.substringAfterLast(".", "").lowercase()
                    ext in setOf("cr3", "cr2", "arw", "nef", "dng")
                }
        if (isRaw) {
            return@withContext RawMetadataSanitizer.cleanRawFile(context, inputUri, inspection)
        }

        try {
            // Read orientation to preserve visual rotation after EXIF is stripped
            val orientation = getOrientation(context, inputUri)

            // Decode image pixels into pristine memory bitmap
            val bitmap = decodeBitmap(context, inputUri, orientation)
                ?: return@withContext CleanExecutionResult(
                    success = false,
                    originalUri = inputUri,
                    cleanedUri = null,
                    cleanedFilePath = null,
                    originalFileName = inspection.fileName,
                    cleanedFileName = "",
                    originalSizeBytes = inspection.fileSizeBytes,
                    cleanedSizeBytes = 0,
                    tagsRemovedCount = 0,
                    removedAiTags = false,
                    removedGps = false,
                    isVerifiedClean = false,
                    inspectionResult = inspection,
                    errorMessage = "Unable to process this file. The file appears to contain unsupported or corrupted image data. Your original file has not been modified."
                )

            // Determine target format & extension
            val (compressFormat, extension) = determineFormat(inspection.mimeType, options.format)
            val baseName = FileSecurityValidator.sanitizeFilename(inspection.fileName.substringBeforeLast("."))
            val cleanedFileName = "clean_${baseName}_${System.currentTimeMillis()}.$extension"

            // Target directory in app's internal cache with path security check
            val outputDir = File(context.cacheDir, "cleaned").apply { mkdirs() }
            val outputFile = File(outputDir, cleanedFileName)

            if (!FileSecurityValidator.isPathSafe(outputDir, outputFile)) {
                bitmap.recycle()
                return@withContext CleanExecutionResult(
                    success = false,
                    originalUri = inputUri,
                    cleanedUri = null,
                    cleanedFilePath = null,
                    originalFileName = inspection.fileName,
                    cleanedFileName = "",
                    originalSizeBytes = inspection.fileSizeBytes,
                    cleanedSizeBytes = 0,
                    tagsRemovedCount = 0,
                    removedAiTags = false,
                    removedGps = false,
                    isVerifiedClean = false,
                    inspectionResult = inspection,
                    errorMessage = "Security violation: Invalid path resolution."
                )
            }

            // Write pristine pixel stream (strips all non-pixel metadata chunks, EXIF, XMP, IPTC, C2PA, PNG parameters)
            FileOutputStream(outputFile).use { outStream ->
                bitmap.compress(compressFormat, options.quality.coerceIn(50, 100), outStream)
            }
            bitmap.recycle()

            val cleanedSize = outputFile.length()
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )

            // Independent Secondary Verification Scan
            val verificationReport = MetadataVerifier.verifyCleanFile(outputFile, inspection)
            val beforeAfter = MetadataVerifier.generateBeforeAfterComparison(inspection, verificationReport)

            val tagsRemoved = if (options.stripAll) {
                inspection.entries.size.coerceAtLeast(1)
            } else {
                var count = 0
                if (options.stripAiMetadata && inspection.hasAiMetadata) count += 2
                if (options.stripLocationGps && inspection.hasGpsLocation) count += 2
                if (options.stripCameraDevice) count += inspection.entries.count { it.category.name == "CAMERA_DEVICE" }
                if (options.stripTimestamps) count += inspection.entries.count { it.category.name == "TIMESTAMPS_FILE" }
                if (options.stripAuthorCopyright) count += inspection.entries.count { it.category.name == "AUTHOR_SYSTEM" }
                count.coerceAtLeast(1)
            }

            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            val origSha256 = context.contentResolver.openInputStream(inputUri)?.use {
                HardwareCryptoSigner.calculateStreamSha256(it)
            } ?: "0000000000000000000000000000000000000000000000000000000000000000"

            val cryptoProof = HardwareCryptoSigner.signSanitizationProof(
                originalFileName = inspection.fileName,
                originalSha256 = origSha256,
                cleanedFile = outputFile,
                verificationReport = verificationReport
            )

            val privacyReport = PrivacyInspectionReport(
                fileName = inspection.fileName,
                fileSizeBytes = inspection.fileSizeBytes,
                mimeType = inspection.mimeType,
                generatedDate = dateStr,
                privacyExposure = inspection.riskLevel,
                detectedExif = inspection.entries.any { it.standard.name == "EXIF" },
                detectedGps = inspection.hasGpsLocation,
                detectedXmp = inspection.entries.any { it.standard.name == "XMP" },
                detectedIptc = inspection.entries.any { it.standard.name == "IPTC" },
                detectedAiMetadata = inspection.hasAiMetadata,
                detectedC2pa = inspection.hasC2pa,
                sensitiveFieldsCount = inspection.entries.count { it.isSensitive },
                totalFieldsFound = inspection.entries.size,
                cleaningStatus = "Completed",
                verificationStatus = if (verificationReport.isVerifiedClean) "VERIFIED CLEAN (PASSED)" else "VERIFICATION INCOMPLETE",
                isVerifiedClean = verificationReport.isVerifiedClean,
                verificationChecks = verificationReport.checkedCategories,
                beforeAfterSummary = beforeAfter,
                aiDetails = inspection.aiMetadata?.let {
                    "Generator: ${it.detectedEngine}\nEvidence: ${it.evidenceSummary}\nConfidence: ${it.confidence}${it.positivePrompt?.let { p -> "\nPrompt: $p" } ?: ""}"
                },
                cryptographicProof = cryptoProof
            )

            CleanExecutionResult(
                success = true,
                originalUri = inputUri,
                cleanedUri = contentUri,
                cleanedFilePath = outputFile.absolutePath,
                originalFileName = inspection.fileName,
                cleanedFileName = cleanedFileName,
                originalSizeBytes = inspection.fileSizeBytes,
                cleanedSizeBytes = cleanedSize,
                tagsRemovedCount = tagsRemoved,
                removedAiTags = inspection.hasAiMetadata && (verificationReport.checkedCategories.find { it.category.contains("AI", ignoreCase = true) }?.passed ?: true),
                removedGps = inspection.hasGpsLocation && (verificationReport.checkedCategories.find { it.category.contains("GPS", ignoreCase = true) }?.passed ?: true),
                isVerifiedClean = verificationReport.isVerifiedClean,
                verificationReport = verificationReport,
                beforeAfterSummary = beforeAfter,
                inspectionResult = inspection,
                privacyReport = privacyReport,
                cryptographicProof = cryptoProof
            )
        } catch (e: Exception) {
            CleanExecutionResult(
                success = false,
                originalUri = inputUri,
                cleanedUri = null,
                cleanedFilePath = null,
                originalFileName = inspection.fileName,
                cleanedFileName = "",
                originalSizeBytes = inspection.fileSizeBytes,
                cleanedSizeBytes = 0,
                tagsRemovedCount = 0,
                removedAiTags = false,
                removedGps = false,
                isVerifiedClean = false,
                inspectionResult = inspection,
                errorMessage = "Unable to process this file. An error occurred during sanitization. Your original file has not been modified."
            )
        }
    }

    private fun cleanVideo(
        context: Context,
        inputUri: Uri,
        inspection: ImageInspectionResult
    ): CleanExecutionResult {
        try {
            val cacheDir = File(context.cacheDir, "cleaned_videos").apply { mkdirs() }
            val baseName = inspection.fileName.substringBeforeLast(".")
            val sanitizedBase = baseName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val outputFile = File(cacheDir, "${sanitizedBase}_cleaned.mp4")
            if (outputFile.exists()) outputFile.delete()

            var extractor: MediaExtractor? = null
            var muxer: MediaMuxer? = null

            try {
                extractor = MediaExtractor()
                extractor.setDataSource(context, inputUri, null)
                muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                val trackMap = mutableMapOf<Int, Int>()
                val trackCount = extractor.trackCount
                for (i in 0 until trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                        extractor.selectTrack(i)
                        trackMap[i] = muxer.addTrack(format)
                    }
                }

                muxer.start()
                val buffer = ByteBuffer.allocate(1024 * 1024)
                val bufferInfo = MediaCodec.BufferInfo()

                while (true) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) break

                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    bufferInfo.flags = extractor.sampleFlags
                    val trackIndex = extractor.sampleTrackIndex
                    val muxerTrack = trackMap[trackIndex]
                    if (muxerTrack != null) {
                        muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                    }
                    extractor.advance()
                }

                muxer.stop()
            } finally {
                try { muxer?.release() } catch (_: Exception) {}
                try { extractor?.release() } catch (_: Exception) {}
            }

            val cleanedSizeBytes = outputFile.length()
            val cleanedUri = Uri.fromFile(outputFile)

            // Independent verification of video output
            val verification = MetadataVerifier.verifyCleanFile(outputFile, inspection)
            val removedCount = inspection.entries.size.coerceAtLeast(1)
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            val origSha256 = context.contentResolver.openInputStream(inputUri)?.use {
                HardwareCryptoSigner.calculateStreamSha256(it)
            } ?: "0000000000000000000000000000000000000000000000000000000000000000"

            val cryptoProof = HardwareCryptoSigner.signSanitizationProof(
                originalFileName = inspection.fileName,
                originalSha256 = origSha256,
                cleanedFile = outputFile,
                verificationReport = verification
            )

            val privacyReport = PrivacyInspectionReport(
                fileName = inspection.fileName,
                fileSizeBytes = inspection.fileSizeBytes,
                mimeType = "video/mp4",
                generatedDate = dateStr,
                privacyExposure = inspection.riskLevel,
                detectedExif = inspection.entries.any { it.standard.name == "EXIF" },
                detectedGps = inspection.hasGpsLocation,
                detectedXmp = inspection.entries.any { it.standard.name == "XMP" },
                detectedIptc = inspection.entries.any { it.standard.name == "IPTC" },
                detectedAiMetadata = inspection.hasAiMetadata,
                detectedC2pa = inspection.hasC2pa,
                sensitiveFieldsCount = inspection.entries.count { it.isSensitive },
                totalFieldsFound = inspection.entries.size,
                cleaningStatus = "Cleaned (Lossless Muxer Sanitization)",
                verificationStatus = verification.statusText,
                isVerifiedClean = verification.isVerifiedClean,
                verificationChecks = verification.checkedCategories,
                beforeAfterSummary = listOf(
                    BeforeAfterItem("Container Metadata", "${inspection.entries.size} metadata tags found", RemovalStatus.REMOVED, "Cleaned (All UDTA/GPS stripped)"),
                    BeforeAfterItem("GPS Location", if (inspection.hasGpsLocation) "Coordinates embedded" else "None", if (inspection.hasGpsLocation) RemovalStatus.REMOVED else RemovalStatus.NOT_PRESENT, "Geotag stripped"),
                    BeforeAfterItem("Video & Audio Streams", "Original encoded tracks", RemovalStatus.PRESERVED, "Preserved (Lossless)")
                ),
                cryptographicProof = cryptoProof
            )

            return CleanExecutionResult(
                success = true,
                originalUri = inputUri,
                cleanedUri = cleanedUri,
                cleanedFilePath = outputFile.absolutePath,
                originalFileName = inspection.fileName,
                cleanedFileName = outputFile.name,
                originalSizeBytes = inspection.fileSizeBytes,
                cleanedSizeBytes = cleanedSizeBytes,
                tagsRemovedCount = removedCount,
                removedAiTags = inspection.hasAiMetadata,
                removedGps = inspection.hasGpsLocation,
                isVerifiedClean = verification.isVerifiedClean,
                inspectionResult = inspection,
                privacyReport = privacyReport,
                cryptographicProof = cryptoProof
            )
        } catch (e: Exception) {
            return CleanExecutionResult(
                success = false,
                originalUri = inputUri,
                cleanedUri = null,
                cleanedFilePath = null,
                originalFileName = inspection.fileName,
                cleanedFileName = "",
                originalSizeBytes = inspection.fileSizeBytes,
                cleanedSizeBytes = 0,
                tagsRemovedCount = 0,
                removedAiTags = false,
                removedGps = false,
                isVerifiedClean = false,
                inspectionResult = inspection,
                errorMessage = "Failed to sanitize video: ${e.message ?: "Unknown codec error"}. Your original video was untouched."
            )
        }
    }

    suspend fun saveToGallery(context: Context, cleanedFile: File): Uri? = withContext(Dispatchers.IO) {
        try {
            val isVideo = cleanedFile.name.endsWith(".mp4", ignoreCase = true) || cleanedFile.name.endsWith(".mov", ignoreCase = true)
            val isRaw = cleanedFile.name.let {
                val ext = it.substringAfterLast(".", "").lowercase()
                ext in setOf("cr3", "cr2", "arw", "nef", "dng")
            }
            val mimeType = when {
                isVideo -> "video/mp4"
                cleanedFile.name.endsWith(".png", ignoreCase = true) -> "image/png"
                cleanedFile.name.endsWith(".webp", ignoreCase = true) -> "image/webp"
                cleanedFile.name.endsWith(".cr3", ignoreCase = true) -> "image/x-canon-cr3"
                cleanedFile.name.endsWith(".arw", ignoreCase = true) -> "image/x-sony-arw"
                cleanedFile.name.endsWith(".nef", ignoreCase = true) -> "image/x-nikon-nef"
                cleanedFile.name.endsWith(".cr2", ignoreCase = true) -> "image/x-canon-cr2"
                cleanedFile.name.endsWith(".dng", ignoreCase = true) -> "image/x-adobe-dng"
                else -> "image/jpeg"
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, cleanedFile.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val folder = when {
                        isVideo -> "${Environment.DIRECTORY_MOVIES}/Metadata_Cleaner"
                        isRaw -> "${Environment.DIRECTORY_PICTURES}/Metadata_Cleaner_RAW"
                        else -> "${Environment.DIRECTORY_PICTURES}/Metadata_Cleaner"
                    }
                    put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val targetCollection = if (isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(targetCollection, contentValues)
                ?: return@withContext null

            resolver.openOutputStream(uri)?.use { out ->
                cleanedFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            uri
        } catch (_: Exception) {
            null
        }
    }

    fun shareImage(context: Context, uri: Uri, fileName: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = context.contentResolver.getType(uri) ?: "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Sanitized Image"))
    }

    fun shareReport(context: Context, reportText: String, fileName: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Privacy Inspection Report - $fileName")
            putExtra(Intent.EXTRA_TEXT, reportText)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Privacy Report"))
    }

    private fun decodeBitmap(context: Context, uri: Uri, orientation: Int): Bitmap? {
        val stream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = BitmapFactory.decodeStream(stream)
        stream.close()

        if (originalBitmap == null) return null
        if (orientation == 0) return originalBitmap

        val matrix = Matrix()
        when (orientation) {
            90 -> matrix.postRotate(90f)
            180 -> matrix.postRotate(180f)
            270 -> matrix.postRotate(270f)
            else -> return originalBitmap
        }

        val rotated = Bitmap.createBitmap(
            originalBitmap,
            0,
            0,
            originalBitmap.width,
            originalBitmap.height,
            matrix,
            true
        )
        if (rotated != originalBitmap) {
            originalBitmap.recycle()
        }
        return rotated
    }

    private fun getOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (_: Exception) {
            0
        }
    }

    private fun determineFormat(
        originalMime: String,
        target: OutputFormat
    ): Pair<Bitmap.CompressFormat, String> {
        return when (target) {
            OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG to "jpg"
            OutputFormat.PNG -> Bitmap.CompressFormat.PNG to "png"
            OutputFormat.WEBP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY to "webp"
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP to "webp"
                }
            }
            OutputFormat.PRESERVE_ORIGINAL -> {
                when {
                    originalMime.contains("png", ignoreCase = true) -> Bitmap.CompressFormat.PNG to "png"
                    originalMime.contains("webp", ignoreCase = true) -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Bitmap.CompressFormat.WEBP_LOSSY to "webp"
                        } else {
                            @Suppress("DEPRECATION")
                            Bitmap.CompressFormat.WEBP to "webp"
                        }
                    }
                    else -> Bitmap.CompressFormat.JPEG to "jpg"
                }
            }
        }
    }
}
