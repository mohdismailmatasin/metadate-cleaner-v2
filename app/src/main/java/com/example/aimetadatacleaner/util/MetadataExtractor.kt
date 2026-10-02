package com.example.aimetadatacleaner.util

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.example.aimetadatacleaner.data.model.AiGenerationMetadata
import com.example.aimetadatacleaner.data.model.ImageInspectionResult
import com.example.aimetadatacleaner.data.model.MetadataCategory
import com.example.aimetadatacleaner.data.model.MetadataEntry
import com.example.aimetadatacleaner.data.model.MetadataStandard
import com.example.aimetadatacleaner.data.model.PrivacyExposure

object MetadataExtractor {

    fun inspectImage(context: Context, uri: Uri): ImageInspectionResult {
        // Validate via security validator
        val validation = FileSecurityValidator.validateUri(context, uri)

        var fileName = validation.sanitizedFileName
        var fileSize: Long = validation.fileSizeBytes
        var mimeType = validation.mimeType

        // Determine image dimensions safely
        var width = 0
        var height = 0
        val isVideo = validation.fileCategory == FileCategory.VIDEO || mimeType.startsWith("video/")

        if (isVideo) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val vWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val vHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                if (vWidth > 0 && vHeight > 0) {
                    width = vWidth
                    height = vHeight
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)?.let {
                    if (it.isNotBlank()) mimeType = it
                }
                retriever.release()
            } catch (_: Exception) {}
        } else {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(stream, null, options)
                    width = options.outWidth
                    height = options.outHeight
                    if (options.outMimeType != null) {
                        mimeType = options.outMimeType
                    }
                }
            } catch (_: Exception) {
            }
        }

        val entries = mutableListOf<MetadataEntry>()
        val riskReasons = mutableListOf<String>()
        var hasAiMetadata = false
        var hasGpsLocation = false
        var hasC2pa = false
        var rawPromptText: String? = null
        var aiMetadata: AiGenerationMetadata? = null

        if (isVideo) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)

                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                if (durationMs > 0) {
                    val sec = durationMs / 1000
                    entries.add(
                        MetadataEntry(
                            standard = MetadataStandard.EMBEDDED_OTHER,
                            category = MetadataCategory.TIMESTAMPS_FILE,
                            key = "Video Duration",
                            value = "${sec / 60}m ${sec % 60}s (${durationMs}ms)",
                            description = "Video stream playback duration"
                        )
                    )
                }

                val location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
                if (!location.isNullOrBlank()) {
                    hasGpsLocation = true
                    riskReasons.add("Embedded video GPS coordinates (${location.trim()})")
                    entries.add(
                        MetadataEntry(
                            standard = MetadataStandard.GPS,
                            category = MetadataCategory.LOCATION,
                            key = "GPS Geotag (ISO 6709)",
                            value = location.trim(),
                            isSensitive = true,
                            description = "Precise recording coordinates embedded in video container"
                        )
                    )
                }

                val date = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
                if (!date.isNullOrBlank()) {
                    entries.add(
                        MetadataEntry(
                            standard = MetadataStandard.EXIF,
                            category = MetadataCategory.TIMESTAMPS_FILE,
                            key = "Creation Date",
                            value = date,
                            description = "Recording timestamp"
                        )
                    )
                }

                val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                if (!bitrate.isNullOrBlank()) {
                    val kbps = bitrate.toLongOrNull()?.let { it / 1000 } ?: bitrate
                    entries.add(
                        MetadataEntry(
                            standard = MetadataStandard.EMBEDDED_OTHER,
                            category = MetadataCategory.CAMERA_DEVICE,
                            key = "Bitrate",
                            value = "$kbps kbps"
                        )
                    )
                }

                val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                if (!rotation.isNullOrBlank()) {
                    entries.add(
                        MetadataEntry(
                            standard = MetadataStandard.EMBEDDED_OTHER,
                            category = MetadataCategory.CAMERA_DEVICE,
                            key = "Rotation",
                            value = "$rotation°"
                        )
                    )
                }

                val author = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
                if (!author.isNullOrBlank()) {
                    entries.add(
                        MetadataEntry(
                            standard = MetadataStandard.IPTC,
                            category = MetadataCategory.AUTHOR_SYSTEM,
                            key = "Author",
                            value = author,
                            isSensitive = true,
                            description = "Author metadata in video container"
                        )
                    )
                }

                retriever.release()
            } catch (_: Exception) {}
        }

        var exifComment: String? = null
        var exifDescription: String? = null
        var exifSoftware: String? = null

        // 1. Read EXIF via AndroidX ExifInterface
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exifComment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT)
                exifDescription = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)
                exifSoftware = exif.getAttribute(ExifInterface.TAG_SOFTWARE)
                extractExifEntries(exif, entries)

                // Check thumbnail presence
                if (exif.hasThumbnail()) {
                    entries.add(
                        MetadataEntry(
                            standard = MetadataStandard.EMBEDDED_OTHER,
                            category = MetadataCategory.EMBEDDED_PROFILES,
                            key = "Embedded EXIF Thumbnail",
                            value = "Embedded preview thumbnail present (${exif.thumbnailBytes?.size ?: 0} bytes)",
                            isSensitive = true,
                            description = "Thumbnails may retain cropped or deleted visual details."
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }

        // 2. Deep scan for AI metadata chunks, parameters, and C2PA
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val scans = AiMetadataParser.scanStreamForAiPayloads(stream)
                aiMetadata = AiMetadataParser.parseAiMetadata(
                    scans = scans,
                    exifComment = exifComment,
                    exifDescription = exifDescription,
                    exifSoftware = exifSoftware
                )
            }
        } catch (_: Exception) {
        }

        // If AI metadata parsed, populate structured entries
        aiMetadata?.let { ai ->
            hasAiMetadata = true
            hasC2pa = ai.hasC2paManifest
            rawPromptText = ai.positivePrompt ?: ai.rawParametersText

            entries.add(
                MetadataEntry(
                    standard = MetadataStandard.AI_METADATA,
                    category = MetadataCategory.AI_PROVENANCE,
                    key = "AI Generator Engine",
                    value = ai.detectedEngine,
                    isSensitive = true,
                    description = "Detected generative AI tool or framework (Confidence: ${ai.confidence})"
                )
            )

            ai.positivePrompt?.let {
                entries.add(
                    MetadataEntry(
                        standard = MetadataStandard.AI_METADATA,
                        category = MetadataCategory.AI_PROVENANCE,
                        key = "AI Positive Prompt",
                        value = it,
                        isSensitive = true,
                        description = "Exact text prompt used during generation"
                    )
                )
            }

            ai.negativePrompt?.let {
                entries.add(
                    MetadataEntry(
                        standard = MetadataStandard.AI_METADATA,
                        category = MetadataCategory.AI_PROVENANCE,
                        key = "AI Negative Prompt",
                        value = it,
                        isSensitive = true,
                        description = "Excluded attributes/negative constraints"
                    )
                )
            }

            ai.model?.let {
                entries.add(
                    MetadataEntry(
                        standard = MetadataStandard.AI_METADATA,
                        category = MetadataCategory.AI_PROVENANCE,
                        key = "AI Model Checkpoint",
                        value = it,
                        isSensitive = true,
                        description = "Base neural network weights or checkpoint filename"
                    )
                )
            }

            ai.seed?.let {
                entries.add(
                    MetadataEntry(
                        standard = MetadataStandard.AI_METADATA,
                        category = MetadataCategory.AI_PROVENANCE,
                        key = "Generation Seed",
                        value = it,
                        isSensitive = true,
                        description = "Deterministic random seed"
                    )
                )
            }

            ai.steps?.let {
                entries.add(
                    MetadataEntry(
                        standard = MetadataStandard.AI_METADATA,
                        category = MetadataCategory.AI_PROVENANCE,
                        key = "Inference Steps",
                        value = it,
                        isSensitive = false,
                        description = "Diffusion sampling iteration count"
                    )
                )
            }

            ai.sampler?.let {
                entries.add(
                    MetadataEntry(
                        standard = MetadataStandard.AI_METADATA,
                        category = MetadataCategory.AI_PROVENANCE,
                        key = "Sampler / Scheduler",
                        value = it,
                        isSensitive = false,
                        description = "Diffusion noise scheduler algorithm"
                    )
                )
            }

            ai.cfgScale?.let {
                entries.add(
                    MetadataEntry(
                        standard = MetadataStandard.AI_METADATA,
                        category = MetadataCategory.AI_PROVENANCE,
                        key = "CFG Guidance Scale",
                        value = it,
                        isSensitive = false,
                        description = "Prompt adherence multiplier"
                    )
                )
            }

            if (ai.loras.isNotEmpty()) {
                entries.add(
                    MetadataEntry(
                        standard = MetadataStandard.AI_METADATA,
                        category = MetadataCategory.AI_PROVENANCE,
                        key = "LoRA Fine-Tunes",
                        value = ai.loras.joinToString(", "),
                        isSensitive = true,
                        description = "Low-Rank Adaptation models"
                    )
                )
            }

            ai.metaTagsInvolved.forEach { tag ->
                entries.add(
                    MetadataEntry(
                        standard = if (tag.contains("C2PA", ignoreCase = true)) MetadataStandard.C2PA else MetadataStandard.AI_METADATA,
                        category = MetadataCategory.AI_PROVENANCE,
                        key = "Metadata Container Tag",
                        value = tag,
                        isSensitive = true,
                        description = "Physical container chunk holding parameters"
                    )
                )
            }

            if (ai.hasC2paManifest) {
                entries.add(
                    MetadataEntry(
                        standard = MetadataStandard.C2PA,
                        category = MetadataCategory.AI_PROVENANCE,
                        key = "C2PA Content Credentials",
                        value = ai.c2paDetails ?: "C2PA Provenance Manifest block detected",
                        isSensitive = true,
                        description = "Cryptographic provenance manifest asserting content history"
                    )
                )
            }
        }

        // Check for GPS
        for (entry in entries) {
            if (entry.category == MetadataCategory.LOCATION && entry.isSensitive) {
                hasGpsLocation = true
            }
        }

        // 3. Scan XMP / IPTC textual headers
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                extractXmpAndIptcText(stream, entries)
            }
        } catch (_: Exception) {
        }

        // Evaluate privacy exposure (clearly communicated as an informational indicator)
        val exposureLevel: PrivacyExposure = when {
            hasGpsLocation -> {
                riskReasons.add("Precise GPS coordinates detected (exposes exact geographic shooting location).")
                if (hasAiMetadata) riskReasons.add("AI generation prompt & model fingerprint detected in metadata chunks.")
                PrivacyExposure.HIGH
            }
            hasAiMetadata -> {
                riskReasons.add("AI generation parameters & text prompt detected.")
                PrivacyExposure.HIGH
            }
            entries.any { it.isSensitive } -> {
                riskReasons.add("Identifying camera serial, artist/author name, or embedded thumbnail detected.")
                PrivacyExposure.MEDIUM
            }
            entries.isNotEmpty() -> {
                riskReasons.add("Standard camera settings or timestamp metadata present.")
                PrivacyExposure.LOW
            }
            else -> {
                riskReasons.add("No detectable metadata or privacy identifiers found.")
                PrivacyExposure.LOW
            }
        }

        return ImageInspectionResult(
            uri = uri,
            fileName = fileName,
            mimeType = mimeType,
            fileSizeBytes = fileSize,
            width = width,
            height = height,
            entries = entries,
            riskLevel = exposureLevel,
            riskReasons = riskReasons,
            hasAiMetadata = hasAiMetadata,
            hasGpsLocation = hasGpsLocation,
            hasC2pa = hasC2pa,
            rawPromptText = rawPromptText,
            aiMetadata = aiMetadata,
            validationResult = validation
        )
    }

    private fun extractExifEntries(exif: ExifInterface, list: MutableList<MetadataEntry>) {
        // Location & GPS
        val latLong = exif.latLong
        if (latLong != null) {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.GPS,
                    category = MetadataCategory.LOCATION,
                    key = "GPS Coordinates",
                    value = String.format("%.5f°, %.5f°", latLong[0], latLong[1]),
                    isSensitive = true,
                    description = "Latitude and Longitude of shooting location"
                )
            )
        }
        exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)?.let {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.GPS,
                    category = MetadataCategory.LOCATION,
                    key = "GPS Altitude",
                    value = "$it m",
                    isSensitive = true,
                    description = "GPS Altitude above sea level"
                )
            )
        }
        exif.getAttribute(ExifInterface.TAG_GPS_DATESTAMP)?.let {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.GPS,
                    category = MetadataCategory.LOCATION,
                    key = "GPS Date Stamp",
                    value = it,
                    isSensitive = true,
                    description = "GPS satellite timestamp synchronization"
                )
            )
        }

        // Camera & Device
        exif.getAttribute(ExifInterface.TAG_MAKE)?.let {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.EXIF,
                    category = MetadataCategory.CAMERA_DEVICE,
                    key = "Camera Make",
                    value = it
                )
            )
        }
        exif.getAttribute(ExifInterface.TAG_MODEL)?.let {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.EXIF,
                    category = MetadataCategory.CAMERA_DEVICE,
                    key = "Camera Model",
                    value = it,
                    isSensitive = true,
                    description = "Device hardware identifier"
                )
            )
        }
        exif.getAttribute(ExifInterface.TAG_BODY_SERIAL_NUMBER)?.let {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.EXIF,
                    category = MetadataCategory.CAMERA_DEVICE,
                    key = "Camera Body Serial Number",
                    value = it,
                    isSensitive = true,
                    description = "Unique hardware serial number (cross-shoot fingerprinting risk)"
                )
            )
        }
        exif.getAttribute(ExifInterface.TAG_LENS_MODEL)?.let {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.EXIF,
                    category = MetadataCategory.CAMERA_DEVICE,
                    key = "Lens Model",
                    value = it
                )
            )
        }
        exif.getAttribute(ExifInterface.TAG_LENS_SERIAL_NUMBER)?.let {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.EXIF,
                    category = MetadataCategory.CAMERA_DEVICE,
                    key = "Lens Serial Number",
                    value = it,
                    isSensitive = true,
                    description = "Optical lens hardware serial number"
                )
            )
        }
        exif.getAttribute(ExifInterface.TAG_CAMERA_OWNER_NAME)?.let {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.EXIF,
                    category = MetadataCategory.AUTHOR_SYSTEM,
                    key = "Camera Owner Name",
                    value = it,
                    isSensitive = true,
                    description = "Registered hardware owner identifier"
                )
            )
        }
        exif.getAttribute(ExifInterface.TAG_MAKER_NOTE)?.let {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.EXIF,
                    category = MetadataCategory.CAMERA_DEVICE,
                    key = "Camera MakerNote",
                    value = "Proprietary camera manufacturer binary records",
                    isSensitive = true,
                    description = "Contains private camera diagnostic logs, shutter count, and calibration data"
                )
            )
        }
        exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.EXIF,
                    category = MetadataCategory.CAMERA_DEVICE,
                    key = "Focal Length",
                    value = "$it mm"
                )
            )
        }
        exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.EXIF,
                    category = MetadataCategory.CAMERA_DEVICE,
                    key = "Aperture",
                    value = "f/$it"
                )
            )
        }
        exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.EXIF,
                    category = MetadataCategory.CAMERA_DEVICE,
                    key = "Exposure Time",
                    value = "$it sec"
                )
            )
        }
        exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.EXIF,
                    category = MetadataCategory.CAMERA_DEVICE,
                    key = "ISO Speed",
                    value = "ISO $it"
                )
            )
        }

        // Timestamps
        exif.getAttribute(ExifInterface.TAG_DATETIME)?.let {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.EXIF,
                    category = MetadataCategory.TIMESTAMPS_FILE,
                    key = "Date / Time Original",
                    value = it,
                    description = "Creation timestamp embedded by camera"
                )
            )
        }
        exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)?.let {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.EXIF,
                    category = MetadataCategory.TIMESTAMPS_FILE,
                    key = "Date Digitized",
                    value = it
                )
            )
        }

        // Author & Software
        exif.getAttribute(ExifInterface.TAG_ARTIST)?.let {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.EXIF,
                    category = MetadataCategory.AUTHOR_SYSTEM,
                    key = "Artist / Author",
                    value = it,
                    isSensitive = true,
                    description = "Photographer or creator identity"
                )
            )
        }
        exif.getAttribute(ExifInterface.TAG_COPYRIGHT)?.let {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.EXIF,
                    category = MetadataCategory.AUTHOR_SYSTEM,
                    key = "Copyright Notice",
                    value = it
                )
            )
        }
        exif.getAttribute(ExifInterface.TAG_SOFTWARE)?.let {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.EXIF,
                    category = MetadataCategory.AUTHOR_SYSTEM,
                    key = "Software / Firmware",
                    value = it,
                    description = "Operating system or editing application"
                )
            )
        }
    }

    private fun extractXmpAndIptcText(stream: java.io.InputStream, list: MutableList<MetadataEntry>) {
        val buffer = ByteArray(256 * 1024)
        val bytesRead = stream.read(buffer)
        if (bytesRead <= 0) return

        val content = String(buffer, 0, bytesRead, Charsets.ISO_8859_1)

        // Check XMP Packet
        if (content.contains("<?xpacket", ignoreCase = true) || content.contains("<x:xmpmeta", ignoreCase = true)) {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.XMP,
                    category = MetadataCategory.AUTHOR_SYSTEM,
                    key = "XMP Metadata Packet",
                    value = "Extensible Metadata Platform (XML packet detected)",
                    isSensitive = true,
                    description = "Contains editing history, camera profiles, and application data"
                )
            )
        }

        // Check Photoshop / IPTC markers
        if (content.contains("Photoshop 3.0", ignoreCase = true) || content.contains("8BIM", ignoreCase = true)) {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.IPTC,
                    category = MetadataCategory.AUTHOR_SYSTEM,
                    key = "IPTC / Photoshop Resource Block",
                    value = "IPTC / 8BIM metadata records present",
                    isSensitive = true,
                    description = "May contain captions, keywords, credit lines, and editing history"
                )
            )
        }

        // Check ICC Color Profile
        if (content.contains("ICC_PROFILE", ignoreCase = true) || content.contains("mntrRGB", ignoreCase = true)) {
            list.add(
                MetadataEntry(
                    standard = MetadataStandard.EMBEDDED_OTHER,
                    category = MetadataCategory.EMBEDDED_PROFILES,
                    key = "ICC Color Profile",
                    value = "Embedded color management profile",
                    isSensitive = false,
                    description = "Display color profile tag"
                )
            )
        }
    }
}
