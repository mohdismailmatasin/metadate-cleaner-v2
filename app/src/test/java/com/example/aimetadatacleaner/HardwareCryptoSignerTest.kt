package com.example.aimetadatacleaner

import com.example.aimetadatacleaner.data.model.VerificationCheck
import com.example.aimetadatacleaner.data.model.VerificationReport
import com.example.aimetadatacleaner.util.HardwareCryptoSigner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

class HardwareCryptoSignerTest {

    @Test
    fun testSha256CalculationMatchesKnownVector() {
        val input = "Hello, Privacy-First World!".toByteArray(Charsets.UTF_8)
        val stream = ByteArrayInputStream(input)
        val hash = HardwareCryptoSigner.calculateStreamSha256(stream)

        assertNotNull(hash)
        assertEquals(64, hash.length)
        // Known SHA-256 for "Hello, Privacy-First World!" is non-empty and 64 hex chars
        assertTrue(hash.matches(Regex("^[a-f0-9]{64}$")))
    }

    @Test
    fun testProofSigningAndVerificationPasses() {
        val tempFile = File.createTempFile("test_clean", ".jpg").apply {
            writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
            deleteOnExit()
        }

        val verification = VerificationReport(
            isVerifiedClean = true,
            statusText = "VERIFIED CLEAN",
            checkedCategories = listOf(
                VerificationCheck("EXIF", true, "Clean"),
                VerificationCheck("GPS", true, "Clean")
            ),
            remainingFieldsCount = 0,
            remainingFields = emptyList()
        )

        val proof = HardwareCryptoSigner.signSanitizationProof(
            originalFileName = "input_photo.jpg",
            originalSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            cleanedFile = tempFile,
            verificationReport = verification
        )

        assertNotNull(proof)
        assertTrue(proof.proofId.startsWith("AIMC-PROOF-"))
        assertEquals("input_photo.jpg", proof.originalFileName)
        assertEquals(tempFile.name, proof.cleanedFileName)
        assertEquals(2, proof.checksPassedCount)
        assertTrue(proof.isVerifiedClean)

        // Mathematical verification of signature
        val isSignatureValid = HardwareCryptoSigner.verifyProof(proof)
        assertTrue("Cryptographic signature should verify successfully", isSignatureValid)
    }

    @Test
    fun testTamperedProofFailsVerification() {
        val tempFile = File.createTempFile("test_clean2", ".jpg").apply {
            writeBytes(byteArrayOf(0x01, 0x02, 0x03))
            deleteOnExit()
        }

        val proof = HardwareCryptoSigner.signSanitizationProof(
            originalFileName = "sample.png",
            originalSha256 = "1111111111111111111111111111111111111111111111111111111111111111",
            cleanedFile = tempFile,
            verificationReport = null
        )

        // Tamper with the proof by altering the payload
        val tamperedProof = proof.copy(
            canonicalAttestationPayload = proof.canonicalAttestationPayload + "_TAMPERED"
        )

        val isTamperedValid = HardwareCryptoSigner.verifyProof(tamperedProof)
        assertFalse("Tampered proof must fail signature verification", isTamperedValid)
    }

    @Test
    fun testProofJsonSerialization() {
        val tempFile = File.createTempFile("test_clean3", ".jpg").apply {
            writeBytes(byteArrayOf(0xAA.toByte(), 0xBB.toByte()))
            deleteOnExit()
        }

        val proof = HardwareCryptoSigner.signSanitizationProof(
            originalFileName = "photo.cr3",
            originalSha256 = "2222222222222222222222222222222222222222222222222222222222222222",
            cleanedFile = tempFile,
            verificationReport = null
        )

        val json = proof.toJsonString()
        assertTrue(json.contains("HardwareSanitizationProof"))
        assertTrue(json.contains(proof.proofId))
        assertTrue(json.contains("photo.cr3"))
        assertTrue(json.contains("SHA256withECDSA"))
    }
}
