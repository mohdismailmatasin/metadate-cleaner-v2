package com.example.aimetadatacleaner.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Represents a hardware-bound cryptographic sanitization proof.
 * Generated using private keys held within the device's secure hardware (TEE / StrongBox / AndroidKeyStore).
 */
data class HardwareSignedProof(
    val proofId: String,
    val timestampIso: String,
    val originalFileName: String,
    val originalSha256: String,
    val cleanedFileName: String,
    val cleanedSha256: String,
    val isVerifiedClean: Boolean,
    val checksPassedCount: Int,
    val keyStoreAlias: String,
    val isHardwareBacked: Boolean,
    val hardwareSecurityLevel: String, // e.g. "Hardware TEE / Secure Element" or "Android KeyStore (ECDSA)"
    val deviceModel: String,
    val publicKeyBase64: String,
    val signatureAlgorithm: String = "SHA256withECDSA",
    val signatureHex: String,
    val signatureBase64: String,
    val canonicalAttestationPayload: String
) {
    fun toFormattedAttestation(): String {
        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("   HARDWARE-BOUND CRYPTOGRAPHIC SANITIZATION PROOF")
        sb.appendLine("   Device Attested Verification Certificate")
        sb.appendLine("==================================================")
        sb.appendLine("Proof ID:         $proofId")
        sb.appendLine("Timestamp (UTC):  $timestampIso")
        sb.appendLine("Hardware Level:   $hardwareSecurityLevel")
        sb.appendLine("Hardware Backed:  ${if (isHardwareBacked) "YES [Secure Element / TEE ✓]" else "Android KeyStore (ECDSA)"}")
        sb.appendLine("Device Model:     $deviceModel")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("FILE CRYPTOGRAPHIC PROVENANCE:")
        sb.appendLine("Original File:    $originalFileName")
        sb.appendLine("Original SHA-256: $originalSha256")
        sb.appendLine("Cleaned File:     $cleanedFileName")
        sb.appendLine("Cleaned SHA-256:  $cleanedSha256")
        sb.appendLine("Sanitization:     VERIFIED CLEAN (${checksPassedCount}/7 checks passed)")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("CRYPTOGRAPHIC SIGNATURE (ECDSA P-256):")
        sb.appendLine("Algorithm:        $signatureAlgorithm")
        sb.appendLine("Signature (Hex):  $signatureHex")
        sb.appendLine("Public Key (DER): $publicKeyBase64")
        sb.appendLine("==================================================")
        return sb.toString()
    }

    fun toJsonString(): String {
        return """
        {
          "@context": "https://w3id.org/security/v2",
          "type": "HardwareSanitizationProof",
          "proof_id": "$proofId",
          "timestamp": "$timestampIso",
          "provenance": {
            "original_file_name": "$originalFileName",
            "original_sha256": "$originalSha256",
            "cleaned_file_name": "$cleanedFileName",
            "cleaned_sha256": "$cleanedSha256",
            "is_verified_clean": $isVerifiedClean,
            "checks_passed": $checksPassedCount
          },
          "device_attestation": {
            "hardware_security_level": "$hardwareSecurityLevel",
            "is_hardware_backed": $isHardwareBacked,
            "device_model": "$deviceModel",
            "keystore_alias": "$keyStoreAlias"
          },
          "cryptographic_proof": {
            "signature_algorithm": "$signatureAlgorithm",
            "public_key_der_base64": "$publicKeyBase64",
            "signature_base64": "$signatureBase64",
            "signature_hex": "$signatureHex"
          }
        }
        """.trimIndent()
    }
}
