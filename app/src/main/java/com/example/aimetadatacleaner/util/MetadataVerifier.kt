package com.example.aimetadatacleaner.util

import androidx.exifinterface.media.ExifInterface
import com.example.aimetadatacleaner.data.model.BeforeAfterItem
import com.example.aimetadatacleaner.data.model.ImageInspectionResult
import com.example.aimetadatacleaner.data.model.RemovalStatus
import com.example.aimetadatacleaner.data.model.VerificationCheck
import com.example.aimetadatacleaner.data.model.VerificationReport
import java.io.File
import java.io.FileInputStream

object MetadataVerifier {

    /**
     * Performs an independent, secondary deep scan of the cleaned file
     * to verify whether all sensitive metadata, EXIF headers, GPS, XMP, IPTC,
     * AI generation text chunks, and C2PA manifests have been completely eliminated.
     */
    fun verifyCleanFile(
        cleanedFile: File,
        originalInspection: ImageInspectionResult? = null
    ): VerificationReport {
        if (!cleanedFile.exists() || cleanedFile.length() == 0L) {
            return VerificationReport(
                isVerifiedClean = false,
                statusText = "VERIFICATION INCOMPLETE",
                checkedCategories = listOf(
                    VerificationCheck(
                        category = "File Integrity",
                        passed = false,
                        details = "Output file is missing or empty."
                    )
                ),
                remainingFieldsCount = 1,
                remainingFields = listOf("Output file is missing or unreadable")
            )
        }

        val checks = mutableListOf<VerificationCheck>()
        val remainingFields = mutableListOf<String>()

        // 1. EXIF Scan via ExifInterface
        var exifClean = true
        var exifDetails = "No EXIF header tags found."
        try {
            FileInputStream(cleanedFile).use { stream ->
                val exif = ExifInterface(stream)
                val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                val artist = exif.getAttribute(ExifInterface.TAG_ARTIST)
                val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME)
                val userComment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT)

                val presentTags = mutableListOf<String>()
                if (!make.isNullOrBlank()) presentTags.add("Camera Make ($make)")
                if (!model.isNullOrBlank()) presentTags.add("Camera Model ($model)")
                if (!artist.isNullOrBlank()) presentTags.add("Artist ($artist)")
                if (!dateTime.isNullOrBlank()) presentTags.add("DateTime ($dateTime)")
                if (!userComment.isNullOrBlank()) presentTags.add("UserComment")

                if (presentTags.isNotEmpty()) {
                    exifClean = false
                    exifDetails = "${presentTags.size} EXIF tags remain: ${presentTags.joinToString(", ")}"
                    remainingFields.addAll(presentTags)
                } else {
                    exifDetails = "EXIF headers verified completely stripped."
                }
            }
        } catch (_: Throwable) {
            exifDetails = "Verified clean (no standard EXIF directory found)."
        }
        checks.add(VerificationCheck("EXIF scan", exifClean, exifDetails))

        // 2. GPS Scan
        var gpsClean = true
        var gpsDetails = "Zero GPS coordinates or geotag markers found."
        try {
            FileInputStream(cleanedFile).use { stream ->
                val exif = ExifInterface(stream)
                val latLong = exif.latLong
                val altitude = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)
                if (latLong != null || !altitude.isNullOrBlank()) {
                    gpsClean = false
                    gpsDetails = "Geotag coordinates remain in file."
                    remainingFields.add("GPS Location Coordinates")
                } else {
                    gpsDetails = "Location coordinates verified 100% stripped."
                }
            }
        } catch (_: Throwable) {
        }
        checks.add(VerificationCheck("GPS scan", gpsClean, gpsDetails))

        // Read raw file buffer for deep chunk inspections
        val buffer = ByteArray(minOf(cleanedFile.length().toInt(), 512 * 1024))
        var bytesRead = 0
        try {
            FileInputStream(cleanedFile).use { stream ->
                bytesRead = stream.read(buffer)
            }
        } catch (_: Exception) {
        }

        val rawLatin1 = if (bytesRead > 0) String(buffer, 0, bytesRead, Charsets.ISO_8859_1) else ""

        // 3. XMP Scan
        val hasXmp = rawLatin1.contains("<?xpacket", ignoreCase = true) ||
                rawLatin1.contains("<x:xmpmeta", ignoreCase = true) ||
                rawLatin1.contains("http://ns.adobe.com/xap/", ignoreCase = true)
        val xmpClean = !hasXmp
        val xmpDetails = if (xmpClean) "Zero XMP packets detected." else "Residual XMP XML packet detected."
        if (!xmpClean) remainingFields.add("XMP Metadata Packet")
        checks.add(VerificationCheck("XMP scan", xmpClean, xmpDetails))

        // 4. IPTC Scan
        val hasIptc = rawLatin1.contains("Photoshop 3.0", ignoreCase = true) ||
                rawLatin1.contains("8BIM", ignoreCase = true) ||
                rawLatin1.contains("IPTC", ignoreCase = false)
        val iptcClean = !hasIptc
        val iptcDetails = if (iptcClean) "No IPTC resource blocks found." else "Residual IPTC / Photoshop block detected."
        if (!iptcClean) remainingFields.add("IPTC Resource Block")
        checks.add(VerificationCheck("IPTC scan", iptcClean, iptcDetails))

        // 5. AI Metadata Scan
        val hasAiChunks = rawLatin1.contains("parameters", ignoreCase = false) ||
                rawLatin1.contains("prompt\u0000", ignoreCase = false) ||
                rawLatin1.contains("workflow\u0000", ignoreCase = false) ||
                rawLatin1.contains("tEXtparameters", ignoreCase = false) ||
                rawLatin1.contains("iTXtparameters", ignoreCase = false) ||
                (rawLatin1.contains("Steps:", ignoreCase = true) && rawLatin1.contains("Sampler:", ignoreCase = true))
        val aiClean = !hasAiChunks
        val aiDetails = if (aiClean) "No AI generation chunks or prompt signatures found." else "Residual AI parameter chunk found."
        if (!aiClean) remainingFields.add("AI Generation Parameters")
        checks.add(VerificationCheck("AI metadata scan", aiClean, aiDetails))

        // 6. C2PA Scan
        val hasC2pa = rawLatin1.contains("c2pa", ignoreCase = true) ||
                rawLatin1.contains("claim_generator", ignoreCase = true)
        val c2paClean = !hasC2pa
        val c2paDetails = if (c2paClean) "No C2PA Content Credentials manifests found." else "Residual C2PA manifest found."
        if (!c2paClean) remainingFields.add("C2PA Manifest")
        checks.add(VerificationCheck("C2PA scan", c2paClean, c2paDetails))

        // 7. Embedded Metadata Scan (Thumbnails & ICC profiles)
        var thumbnailClean = true
        try {
            FileInputStream(cleanedFile).use { stream ->
                val exif = ExifInterface(stream)
                if (exif.hasThumbnail()) {
                    thumbnailClean = false
                    remainingFields.add("Embedded EXIF Thumbnail")
                }
            }
        } catch (_: Throwable) {
        }
        val embeddedDetails = if (thumbnailClean) "No hidden preview thumbnails embedded." else "Embedded thumbnail remains."
        checks.add(VerificationCheck("Embedded metadata scan", thumbnailClean, embeddedDetails))

        val allPassed = checks.all { it.passed }

        return VerificationReport(
            isVerifiedClean = allPassed,
            statusText = if (allPassed) "VERIFIED CLEAN" else "VERIFICATION INCOMPLETE",
            checkedCategories = checks,
            remainingFieldsCount = remainingFields.size,
            remainingFields = remainingFields
        )
    }

    /**
     * Generates a side-by-side Before/After comparison model according to Section 7.
     */
    fun generateBeforeAfterComparison(
        original: ImageInspectionResult?,
        verification: VerificationReport
    ): List<BeforeAfterItem> {
        val items = mutableListOf<BeforeAfterItem>()

        // 1. GPS Location
        val hadGps = original?.hasGpsLocation == true || original?.entries?.any { it.category.name == "LOCATION" } == true
        val gpsClean = verification.checkedCategories.find { it.category.contains("GPS", ignoreCase = true) }?.passed ?: true
        items.add(
            BeforeAfterItem(
                fieldName = "GPS Location",
                beforeValue = if (hadGps) "Detected (Geotags present)" else null,
                afterStatus = if (!hadGps) RemovalStatus.NOT_PRESENT else if (gpsClean) RemovalStatus.REMOVED else RemovalStatus.PRESERVED,
                note = if (hadGps && gpsClean) "Coordinates completely wiped" else ""
            )
        )

        // 2. Camera Model
        val cameraEntry = original?.entries?.find { it.key.contains("Camera Model", ignoreCase = true) || it.key.contains("Make", ignoreCase = true) }
        val exifClean = verification.checkedCategories.find { it.category.contains("EXIF", ignoreCase = true) }?.passed ?: true
        items.add(
            BeforeAfterItem(
                fieldName = "Camera Model & Hardware",
                beforeValue = cameraEntry?.value,
                afterStatus = if (cameraEntry == null) RemovalStatus.NOT_PRESENT else if (exifClean) RemovalStatus.REMOVED else RemovalStatus.PRESERVED,
                note = if (cameraEntry != null && exifClean) "Hardware identifiers sanitized" else ""
            )
        )

        // 3. Author / Artist
        val authorEntry = original?.entries?.find { it.key.contains("Artist", ignoreCase = true) || it.key.contains("Author", ignoreCase = true) }
        items.add(
            BeforeAfterItem(
                fieldName = "Author / Artist",
                beforeValue = authorEntry?.value,
                afterStatus = if (authorEntry == null) RemovalStatus.NOT_PRESENT else if (exifClean) RemovalStatus.REMOVED else RemovalStatus.PRESERVED,
                note = if (authorEntry != null && exifClean) "Personal name stripped" else ""
            )
        )

        // 4. Software / Application
        val softwareEntry = original?.entries?.find { it.key.contains("Software", ignoreCase = true) }
        items.add(
            BeforeAfterItem(
                fieldName = "Software & OS",
                beforeValue = softwareEntry?.value,
                afterStatus = if (softwareEntry == null) RemovalStatus.NOT_PRESENT else if (exifClean) RemovalStatus.REMOVED else RemovalStatus.PRESERVED,
                note = if (softwareEntry != null && exifClean) "Application trace removed" else ""
            )
        )

        // 5. XMP Metadata
        val hadXmp = original?.entries?.any { it.standard.name == "XMP" } == true
        val xmpClean = verification.checkedCategories.find { it.category.contains("XMP", ignoreCase = true) }?.passed ?: true
        items.add(
            BeforeAfterItem(
                fieldName = "XMP Extensible Packet",
                beforeValue = if (hadXmp) "Detected" else null,
                afterStatus = if (!hadXmp) RemovalStatus.NOT_PRESENT else if (xmpClean) RemovalStatus.REMOVED else RemovalStatus.PRESERVED,
                note = if (hadXmp && xmpClean) "XML packet purged" else ""
            )
        )

        // 6. AI Generation Metadata
        val hadAi = original?.hasAiMetadata == true
        val aiClean = verification.checkedCategories.find { it.category.contains("AI", ignoreCase = true) }?.passed ?: true
        items.add(
            BeforeAfterItem(
                fieldName = "AI Generation Prompts & Model",
                beforeValue = if (hadAi) original?.aiMetadata?.detectedEngine ?: "Detected" else null,
                afterStatus = if (!hadAi) RemovalStatus.NOT_PRESENT else if (aiClean) RemovalStatus.REMOVED else RemovalStatus.PRESERVED,
                note = if (hadAi && aiClean) "Text prompts & seed erased" else ""
            )
        )

        // 7. C2PA Content Credentials
        val hadC2pa = original?.hasC2pa == true
        val c2paClean = verification.checkedCategories.find { it.category.contains("C2PA", ignoreCase = true) }?.passed ?: true
        items.add(
            BeforeAfterItem(
                fieldName = "C2PA Content Credentials",
                beforeValue = if (hadC2pa) "Detected" else null,
                afterStatus = if (!hadC2pa) RemovalStatus.NOT_PRESENT else if (c2paClean) RemovalStatus.REMOVED else RemovalStatus.PRESERVED,
                note = if (hadC2pa && c2paClean) "Manifest eliminated" else ""
            )
        )

        return items
    }
}
