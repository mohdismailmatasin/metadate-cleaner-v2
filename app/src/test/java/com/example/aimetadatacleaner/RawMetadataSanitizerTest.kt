package com.example.aimetadatacleaner

import com.example.aimetadatacleaner.util.FileCategory
import com.example.aimetadatacleaner.util.FileSecurityValidator
import com.example.aimetadatacleaner.util.FormatSupportLevel
import com.example.aimetadatacleaner.util.HardwareCryptoSigner
import com.example.aimetadatacleaner.util.RawMetadataSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RawMetadataSanitizerTest {

    @Test
    fun testCanonCr3FormatDetection() {
        // Construct minimal ISOBMFF header with 'crx ' major brand
        val header = ByteArray(16).apply {
            this[4] = 'f'.code.toByte()
            this[5] = 't'.code.toByte()
            this[6] = 'y'.code.toByte()
            this[7] = 'p'.code.toByte()
            this[8] = 'c'.code.toByte()
            this[9] = 'r'.code.toByte()
            this[10] = 'x'.code.toByte()
            this[11] = ' '.code.toByte()
        }

        val result = FileSecurityValidator.detectSignatureAndCategory(
            bytes = header,
            length = header.size,
            fileName = "photo_shot.cr3",
            fileSizeBytes = 25000000L
        )

        assertTrue(result.isValid)
        assertEquals(FileCategory.RAW, result.fileCategory)
        assertEquals("Canon RAW 3 (CR3)", result.detectedFormat)
        assertEquals("image/x-canon-cr3", result.mimeType)
        assertEquals(FormatSupportLevel.SUPPORTED, result.supportLevel)
    }

    @Test
    fun testSonyArwAndNikonNefTiffRawFormatDetection() {
        // TIFF Little Endian magic: II*\0
        val tiffLe = byteArrayOf(0x49.toByte(), 0x49.toByte(), 0x2A.toByte(), 0x00.toByte(), 0x08, 0x00, 0x00, 0x00)

        // Test Sony ARW
        val arwResult = FileSecurityValidator.detectSignatureAndCategory(
            bytes = tiffLe,
            length = tiffLe.size,
            fileName = "sony_a7iv.arw",
            fileSizeBytes = 35000000L
        )
        assertTrue(arwResult.isValid)
        assertEquals(FileCategory.RAW, arwResult.fileCategory)
        assertEquals("Sony Alpha RAW (ARW)", arwResult.detectedFormat)
        assertEquals(FormatSupportLevel.SUPPORTED, arwResult.supportLevel)

        // Test Nikon NEF
        val nefResult = FileSecurityValidator.detectSignatureAndCategory(
            bytes = tiffLe,
            length = tiffLe.size,
            fileName = "nikon_z8.nef",
            fileSizeBytes = 45000000L
        )
        assertTrue(nefResult.isValid)
        assertEquals(FileCategory.RAW, nefResult.fileCategory)
        assertEquals("Nikon RAW (NEF)", nefResult.detectedFormat)
        assertEquals(FormatSupportLevel.SUPPORTED, nefResult.supportLevel)

        // Test Adobe DNG
        val dngResult = FileSecurityValidator.detectSignatureAndCategory(
            bytes = tiffLe,
            length = tiffLe.size,
            fileName = "dng_profile.dng",
            fileSizeBytes = 30000000L
        )
        assertTrue(dngResult.isValid)
        assertEquals(FileCategory.RAW, dngResult.fileCategory)
        assertEquals("Adobe Digital Negative (DNG)", dngResult.detectedFormat)
    }

    @Test
    fun testRawVerificationPassesOnPristineRawFile() {
        val tempRawFile = File.createTempFile("sanitized_raw", ".cr3").apply {
            writeBytes("CanonCR3-RawSensorPayloadBayerPatternPureData0123456789".toByteArray(Charsets.ISO_8859_1))
            deleteOnExit()
        }

        val verification = RawMetadataSanitizer.verifySanitizedRaw(tempRawFile)
        assertTrue("Verification should pass on pristine sanitized RAW file", verification.isVerifiedClean)
        assertEquals("VERIFIED CLEAN", verification.statusText)
        assertEquals(0, verification.remainingFieldsCount)
        assertTrue(verification.checkedCategories.any { it.category == "RAW GPS Geotags" && it.passed })
        assertTrue(verification.checkedCategories.any { it.category == "RAW XMP Packet" && it.passed })
        assertTrue(verification.checkedCategories.any { it.category == "Hardware Serial Numbers & MakerNote" && it.passed })
    }

    @Test
    fun testRawVerificationFlagsResidualGpsOrXmp() {
        val tempRawWithGps = File.createTempFile("residual_gps_raw", ".nef").apply {
            writeBytes("NikonNEF-GPSLatitude-45.123-GPSLongitude-9.123-RawStream".toByteArray(Charsets.ISO_8859_1))
            deleteOnExit()
        }

        val verification = RawMetadataSanitizer.verifySanitizedRaw(tempRawWithGps)
        assertFalse("Verification must fail if residual GPS strings exist", verification.isVerifiedClean)
        assertEquals("VERIFICATION INCOMPLETE", verification.statusText)
        assertTrue(verification.remainingFields.contains("GPS coordinates"))
    }

    @Test
    fun testRawProofSigningWithHardwareAttestation() {
        val tempRaw = File.createTempFile("audited_raw", ".arw").apply {
            writeBytes("PureSonyAlphaBayerRawStreamWithZeroMetadataAttached".toByteArray(Charsets.ISO_8859_1))
            deleteOnExit()
        }

        val verification = RawMetadataSanitizer.verifySanitizedRaw(tempRaw)
        val proof = HardwareCryptoSigner.signSanitizationProof(
            originalFileName = "DSC00123.ARW",
            originalSha256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            cleanedFile = tempRaw,
            verificationReport = verification
        )

        assertNotNull(proof)
        assertEquals("DSC00123.ARW", proof.originalFileName)
        assertEquals(tempRaw.name, proof.cleanedFileName)
        assertTrue(proof.isVerifiedClean)
        assertTrue(HardwareCryptoSigner.verifyProof(proof))
    }
}
