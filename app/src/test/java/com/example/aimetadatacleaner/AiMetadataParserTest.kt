package com.example.aimetadatacleaner

import com.example.aimetadatacleaner.util.AiMetadataParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiMetadataParserTest {

    @Test
    fun `parses Stable Diffusion Automatic1111 parameters chunk`() {
        val rawSdParameters = """
            masterpiece, best quality, cybernetic privacy shield, glowing neon circuits
            Negative prompt: low quality, blurry, deformed, watermark, signature
            Steps: 28, Sampler: DPM++ 2M Karras, CFG scale: 7.5, Seed: 19842026, Size: 1024x1024, Model: dreamshaper_v8
        """.trimIndent()

        val scan = AiMetadataParser.RawAiScan(
            rawText = rawSdParameters,
            sourceTag = "PNG Chunk: \"parameters\"",
            evidenceDescription = "Diffusion parameters found."
        )

        val result = AiMetadataParser.parseAiMetadata(listOf(scan))

        assertNotNull(result)
        assertEquals("Stable Diffusion (WebUI / Forge)", result!!.detectedEngine)
        assertEquals("Metadata evidence only", result.confidence)
        assertEquals("masterpiece, best quality, cybernetic privacy shield, glowing neon circuits", result.positivePrompt)
        assertEquals("low quality, blurry, deformed, watermark, signature", result.negativePrompt)
        assertEquals("28", result.steps)
        assertEquals("DPM++ 2M Karras", result.sampler)
        assertEquals("7.5", result.cfgScale)
        assertEquals("19842026", result.seed)
        assertEquals("dreamshaper_v8", result.model)
    }

    @Test
    fun `parses Midjourney prompt and generation flags`() {
        val rawMjText = "Cybernetic privacy shield guarding database --v 6.0 --ar 16:9 --stylize 250 --seed 4242"
        val scan = AiMetadataParser.RawAiScan(
            rawText = rawMjText,
            sourceTag = "Embedded Parameters: Midjourney",
            evidenceDescription = "Midjourney flags detected."
        )

        val result = AiMetadataParser.parseAiMetadata(listOf(scan))

        assertNotNull(result)
        assertTrue(result!!.detectedEngine.contains("Midjourney"))
        assertEquals("Cybernetic privacy shield guarding database", result.positivePrompt)
        assertEquals("4242", result.seed)
        assertEquals("Aspect Ratio 16:9", result.dimensions)
        assertEquals("Metadata evidence only", result.confidence)
    }

    @Test
    fun `detects C2PA Content Credentials provenance manifest`() {
        val c2paScan = AiMetadataParser.RawAiScan(
            rawText = "c2pa manifest assertion claim_generator: Adobe Photoshop 2026",
            sourceTag = "Provenance: C2PA Manifest",
            evidenceDescription = "C2PA manifest block detected."
        )

        val result = AiMetadataParser.parseAiMetadata(listOf(c2paScan))

        assertNotNull(result)
        assertTrue(result!!.hasC2paManifest)
        assertEquals("Metadata evidence only", result.confidence)
    }
}
