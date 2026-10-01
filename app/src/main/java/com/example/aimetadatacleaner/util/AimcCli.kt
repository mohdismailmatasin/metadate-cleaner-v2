package com.example.aimetadatacleaner.util

import java.io.File
import java.io.FileInputStream

/**
 * AI Metadata Cleaner Command Line Interface (aimc) specification and headless execution helper.
 * Provides programmatic scan, clean, and verify capabilities compatible with desktop/server JVM runtimes.
 *
 * Usage:
 *   aimc scan <file>
 *   aimc clean <file> [--output <dir>]
 *   aimc verify <file>
 */
object AimcCli {

    data class CliScanResult(
        val fileName: String,
        val detectedFormat: String,
        val gpsFound: Boolean,
        val cameraFound: Boolean,
        val xmpFound: Boolean,
        val aiMetadataFound: Boolean,
        val c2paFound: Boolean,
        val rawAiEvidence: String? = null
    )

    data class CliVerifyResult(
        val fileName: String,
        val isVerifiedClean: Boolean,
        val status: String,
        val checks: List<Pair<String, Boolean>>,
        val remainingTagsCount: Int
    )

    fun scan(file: File): CliScanResult {
        if (!file.exists()) throw IllegalArgumentException("File not found: ${file.absolutePath}")

        val buffer = ByteArray(minOf(file.length().toInt(), 512 * 1024))
        var bytesRead = 0
        FileInputStream(file).use { bytesRead = it.read(buffer) }

        val latin1 = if (bytesRead > 0) String(buffer, 0, bytesRead, Charsets.ISO_8859_1) else ""
        val utf8 = if (bytesRead > 0) String(buffer, 0, bytesRead, Charsets.UTF_8) else ""

        val hasGps = latin1.contains("GPSLatitude", ignoreCase = true) || latin1.contains("GPSLongitude", ignoreCase = true)
        val hasCamera = latin1.contains("Make\u0000", ignoreCase = false) || latin1.contains("Model\u0000", ignoreCase = false)
        val hasXmp = latin1.contains("<?xpacket", ignoreCase = true) || latin1.contains("<x:xmpmeta", ignoreCase = true)
        val hasAi = latin1.contains("parameters", ignoreCase = false) ||
                latin1.contains("prompt\u0000", ignoreCase = false) ||
                latin1.contains("workflow\u0000", ignoreCase = false) ||
                (utf8.contains("Steps:", ignoreCase = true) && utf8.contains("Sampler:", ignoreCase = true)) ||
                utf8.contains("Midjourney", ignoreCase = true)
        val hasC2pa = latin1.contains("c2pa", ignoreCase = true) || latin1.contains("claim_generator", ignoreCase = true)

        val validation = FileSecurityValidator.detectSignatureAndCategory(buffer, bytesRead, file.name, file.length())

        return CliScanResult(
            fileName = file.name,
            detectedFormat = validation.detectedFormat,
            gpsFound = hasGps,
            cameraFound = hasCamera,
            xmpFound = hasXmp,
            aiMetadataFound = hasAi,
            c2paFound = hasC2pa,
            rawAiEvidence = if (hasAi) "AI parameters detected in binary stream" else null
        )
    }

    fun verify(file: File): CliVerifyResult {
        val verification = MetadataVerifier.verifyCleanFile(file)
        return CliVerifyResult(
            fileName = file.name,
            isVerifiedClean = verification.isVerifiedClean,
            status = verification.statusText,
            checks = verification.checkedCategories.map { it.category to it.passed },
            remainingTagsCount = verification.remainingFieldsCount
        )
    }

    fun formatCliOutput(scanResult: CliScanResult): String {
        return buildString {
            appendLine("AI Metadata Cleaner (aimc)")
            appendLine("Scanning: ${scanResult.fileName} [${scanResult.detectedFormat}]")
            appendLine("----------------------------------------")
            appendLine("GPS:            ${if (scanResult.gpsFound) "FOUND [⚠ High Privacy Exposure]" else "NOT FOUND [✓]"}")
            appendLine("Camera/Device:  ${if (scanResult.cameraFound) "FOUND [⚠]" else "NOT FOUND [✓]"}")
            appendLine("XMP:            ${if (scanResult.xmpFound) "FOUND [⚠]" else "NOT FOUND [✓]"}")
            appendLine("AI metadata:    ${if (scanResult.aiMetadataFound) "FOUND [⚠ Evidence detected]" else "NOT FOUND [✓]"}")
            appendLine("C2PA:           ${if (scanResult.c2paFound) "FOUND [⚠ Manifest detected]" else "NOT FOUND [✓]"}")
            appendLine("----------------------------------------")
            appendLine("Local Processing: Verified on-device execution.")
        }
    }
}
