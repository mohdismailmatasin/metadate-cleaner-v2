package com.example.aimetadatacleaner

import com.example.aimetadatacleaner.data.model.RemovalStatus
import com.example.aimetadatacleaner.util.MetadataVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream

class MetadataVerifierTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `verification succeeds on clean file with no metadata`() {
        val cleanFile = tempFolder.newFile("pristine_clean.jpg")
        FileOutputStream(cleanFile).use { out ->
            // Minimal valid JPEG header with clean pixel SOS
            out.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
        }

        val report = MetadataVerifier.verifyCleanFile(cleanFile)

        assertTrue("Expected verified clean for pristine file", report.isVerifiedClean)
        assertEquals("VERIFIED CLEAN", report.statusText)
        assertEquals(0, report.remainingFieldsCount)
        assertTrue(report.checkedCategories.all { it.passed })
    }

    @Test
    fun `verification catches residual XMP packet and fails verification`() {
        val dirtyFile = tempFolder.newFile("dirty_residual_xmp.jpg")
        FileOutputStream(dirtyFile).use { out ->
            val payload = "HEADER<?xpacket begin=\"\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?><x:xmpmeta>sensitive</x:xmpmeta>"
            out.write(payload.toByteArray(Charsets.ISO_8859_1))
        }

        val report = MetadataVerifier.verifyCleanFile(dirtyFile)

        assertFalse("Verification must fail if XMP packet remains", report.isVerifiedClean)
        assertEquals("VERIFICATION INCOMPLETE", report.statusText)
        assertTrue(report.remainingFieldsCount > 0)
        assertTrue(report.remainingFields.any { it.contains("XMP", ignoreCase = true) })

        val xmpCheck = report.checkedCategories.find { it.category.contains("XMP", ignoreCase = true) }
        assertNotNull(xmpCheck)
        assertFalse(xmpCheck!!.passed)
    }

    @Test
    fun `verification catches residual AI prompt parameters and fails verification`() {
        val dirtyFile = tempFolder.newFile("dirty_residual_ai.png")
        FileOutputStream(dirtyFile).use { out ->
            val payload = "PNG_HEADER\u0000parameters\u0000Steps: 20, Sampler: Euler, Seed: 12345"
            out.write(payload.toByteArray(Charsets.ISO_8859_1))
        }

        val report = MetadataVerifier.verifyCleanFile(dirtyFile)

        assertFalse("Verification must fail if AI parameter chunk remains", report.isVerifiedClean)
        assertEquals("VERIFICATION INCOMPLETE", report.statusText)
        assertTrue(report.remainingFields.any { it.contains("AI", ignoreCase = true) })
    }

    @Test
    fun `generates before and after comparison correctly`() {
        val cleanFile = tempFolder.newFile("clean.jpg")
        FileOutputStream(cleanFile).use {
            it.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
        }

        val report = MetadataVerifier.verifyCleanFile(cleanFile)
        val beforeAfter = MetadataVerifier.generateBeforeAfterComparison(original = null, verification = report)

        assertTrue(beforeAfter.isNotEmpty())
        val gpsItem = beforeAfter.find { it.fieldName.contains("GPS") }
        assertNotNull(gpsItem)
        assertEquals(RemovalStatus.NOT_PRESENT, gpsItem!!.afterStatus)
    }

    private fun assertNotNull(obj: Any?) {
        assertTrue(obj != null)
    }
}
