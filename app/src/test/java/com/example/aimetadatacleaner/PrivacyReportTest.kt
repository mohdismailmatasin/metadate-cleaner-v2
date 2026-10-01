package com.example.aimetadatacleaner

import com.example.aimetadatacleaner.data.model.PrivacyExposure
import com.example.aimetadatacleaner.data.model.PrivacyInspectionReport
import com.example.aimetadatacleaner.data.model.VerificationCheck
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyReportTest {

    @Test
    fun `generates structured text report with all security disclaimers`() {
        val report = PrivacyInspectionReport(
            fileName = "sample_photo.jpg",
            fileSizeBytes = 204800L,
            mimeType = "image/jpeg",
            generatedDate = "2026-10-01 12:00:00",
            privacyExposure = PrivacyExposure.HIGH,
            detectedExif = true,
            detectedGps = true,
            detectedXmp = true,
            detectedIptc = false,
            detectedAiMetadata = true,
            detectedC2pa = false,
            sensitiveFieldsCount = 3,
            totalFieldsFound = 8,
            cleaningStatus = "Completed",
            verificationStatus = "VERIFIED CLEAN (PASSED)",
            isVerifiedClean = true,
            verificationChecks = listOf(
                VerificationCheck("EXIF scan", true, "Clean"),
                VerificationCheck("GPS scan", true, "Clean"),
                VerificationCheck("AI metadata scan", true, "Clean")
            ),
            aiDetails = "Generator: Stable Diffusion\nEvidence: Parameters chunk detected"
        )

        val formattedText = report.toFormattedText()
        assertTrue(formattedText.contains("PRIVACY INSPECTION REPORT"))
        assertTrue(formattedText.contains("sample_photo.jpg"))
        assertTrue(formattedText.contains("EXIF:         DETECTED [✓]"))
        assertTrue(formattedText.contains("GPS Geotags:  DETECTED [⚠ HIGH EXPOSURE]"))
        assertTrue(formattedText.contains("AI Metadata:  DETECTED (Evidence only)"))
        assertTrue(formattedText.contains("Confidence is metadata evidence only"))
        assertTrue(formattedText.contains("VERIFIED CLEAN (PASSED)"))
        assertTrue(formattedText.contains("All processing was performed locally on-device."))

        val json = report.toJsonString()
        assertTrue(json.contains("\"file_name\": \"sample_photo.jpg\""))
        assertTrue(json.contains("\"is_verified_clean\": true"))
    }
}
