package com.example.aimetadatacleaner

import com.example.aimetadatacleaner.util.FileCategory
import com.example.aimetadatacleaner.util.FileSecurityValidator
import com.example.aimetadatacleaner.util.FormatSupportLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FileSecurityValidatorTest {

    @Test
    fun `detects valid JPEG magic bytes`() {
        val jpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10)
        val result = FileSecurityValidator.detectSignatureAndCategory(
            bytes = jpegHeader,
            length = jpegHeader.size,
            fileName = "sample.jpg",
            fileSizeBytes = 1024L
        )
        assertTrue(result.isValid)
        assertEquals("image/jpeg", result.mimeType)
        assertEquals(FormatSupportLevel.SUPPORTED, result.supportLevel)
        assertEquals(FileCategory.IMAGE, result.fileCategory)
    }

    @Test
    fun `detects valid PNG magic bytes`() {
        val pngHeader = byteArrayOf(
            0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
            0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte()
        )
        val result = FileSecurityValidator.detectSignatureAndCategory(
            bytes = pngHeader,
            length = pngHeader.size,
            fileName = "sample.png",
            fileSizeBytes = 2048L
        )
        assertTrue(result.isValid)
        assertEquals("image/png", result.mimeType)
        assertEquals(FormatSupportLevel.SUPPORTED, result.supportLevel)
    }

    @Test
    fun `detects valid WebP RIFF container`() {
        val webpHeader = "RIFF\u0000\u0000\u0000\u0000WEBPVP8 ".toByteArray(Charsets.US_ASCII)
        val result = FileSecurityValidator.detectSignatureAndCategory(
            bytes = webpHeader,
            length = webpHeader.size,
            fileName = "sample.webp",
            fileSizeBytes = 4096L
        )
        assertTrue(result.isValid)
        assertEquals("image/webp", result.mimeType)
        assertEquals(FormatSupportLevel.SUPPORTED, result.supportLevel)
    }

    @Test
    fun `detects PDF format and marks as Scan Only`() {
        val pdfHeader = "%PDF-1.7\n".toByteArray(Charsets.US_ASCII)
        val result = FileSecurityValidator.detectSignatureAndCategory(
            bytes = pdfHeader,
            length = pdfHeader.size,
            fileName = "document.pdf",
            fileSizeBytes = 12000L
        )
        assertTrue(result.isValid)
        assertEquals("application/pdf", result.mimeType)
        assertEquals(FormatSupportLevel.SCAN_ONLY, result.supportLevel)
        assertEquals(FileCategory.DOCUMENT, result.fileCategory)
    }

    @Test
    fun `sanitizes path traversal and illegal characters in filename`() {
        val malicious1 = "../../etc/passwd"
        val clean1 = FileSecurityValidator.sanitizeFilename(malicious1)
        assertFalse(clean1.contains(".."))
        assertFalse(clean1.contains("/"))

        val malicious2 = "photo\u0000_attack?.jpg"
        val clean2 = FileSecurityValidator.sanitizeFilename(malicious2)
        assertFalse(clean2.contains("\u0000"))
        assertFalse(clean2.contains("?"))
    }

    @Test
    fun `prevents path traversal outside cache directory`() {
        val baseDir = File("/data/user/0/com.example/cache/cleaned")
        val safeFile = File(baseDir, "clean_photo.jpg")
        val unsafeFile = File(baseDir, "../../shared_prefs/metaclean_settings.xml")

        assertTrue(FileSecurityValidator.isPathSafe(baseDir, safeFile))
        assertFalse(FileSecurityValidator.isPathSafe(baseDir, unsafeFile))
    }
}
