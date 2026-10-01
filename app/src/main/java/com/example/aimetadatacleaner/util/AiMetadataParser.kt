package com.example.aimetadatacleaner.util

import com.example.aimetadatacleaner.data.model.AiGenerationMetadata
import org.json.JSONObject
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

object AiMetadataParser {

    data class RawAiScan(
        val rawText: String,
        val sourceTag: String,
        val evidenceDescription: String
    )

    fun scanStreamForAiPayloads(stream: InputStream): List<RawAiScan> {
        val results = mutableListOf<RawAiScan>()
        val buffer = ByteArray(512 * 1024) // Scan first 512KB for headers, text chunks, XMP
        val bytesRead = stream.read(buffer)
        if (bytesRead <= 0) return results

        val latin1Content = String(buffer, 0, bytesRead, StandardCharsets.ISO_8859_1)
        val utf8Content = try {
            String(buffer, 0, bytesRead, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            latin1Content
        }

        // 1. Check PNG "parameters" chunk (Stable Diffusion / Automatic1111 / WebUI / Forge)
        if (latin1Content.contains("parameters", ignoreCase = false)) {
            val idx = latin1Content.indexOf("parameters")
            val endIdx = findChunkEnd(buffer, idx + 10, bytesRead)
            val chunkStr = extractSafeString(buffer, idx + 10, endIdx)
            if (chunkStr.isNotBlank()) {
                results.add(
                    RawAiScan(
                        rawText = chunkStr,
                        sourceTag = "PNG Chunk: \"parameters\"",
                        evidenceDescription = "Stable Diffusion parameters chunk embedded in PNG metadata."
                    )
                )
            }
        }

        // 2. Check PNG "prompt" chunk (ComfyUI API graph)
        if (latin1Content.contains("prompt\u0000") || latin1Content.contains("tEXtprompt") || latin1Content.contains("iTXtprompt")) {
            val idx = latin1Content.indexOf("prompt")
            val endIdx = findChunkEnd(buffer, idx + 7, bytesRead)
            val chunkStr = extractSafeString(buffer, idx + 7, endIdx)
            if (chunkStr.contains("{") && chunkStr.contains("}")) {
                results.add(
                    RawAiScan(
                        rawText = chunkStr,
                        sourceTag = "PNG Chunk: \"prompt\"",
                        evidenceDescription = "ComfyUI node execution graph embedded in PNG text chunk."
                    )
                )
            }
        }

        // 3. Check PNG "workflow" chunk (ComfyUI Visual graph)
        if (latin1Content.contains("workflow\u0000") || latin1Content.contains("tEXtworkflow") || latin1Content.contains("iTXtworkflow")) {
            val idx = latin1Content.indexOf("workflow")
            val endIdx = findChunkEnd(buffer, idx + 9, bytesRead)
            val chunkStr = extractSafeString(buffer, idx + 9, endIdx)
            if (chunkStr.contains("{") && chunkStr.contains("}")) {
                results.add(
                    RawAiScan(
                        rawText = chunkStr,
                        sourceTag = "PNG Chunk: \"workflow\"",
                        evidenceDescription = "ComfyUI visual UI canvas workflow embedded in PNG metadata."
                    )
                )
            }
        }

        // 4. Check for Stable Diffusion signature in raw stream / EXIF UserComment
        if (results.isEmpty() && (utf8Content.contains("Steps:", ignoreCase = true) || utf8Content.contains("Negative prompt:", ignoreCase = true))) {
            val startIdx = maxOf(0, utf8Content.indexOf("Negative prompt:").let { if (it > 200) it - 200 else 0 })
            val sub = utf8Content.substring(startIdx, minOf(utf8Content.length, startIdx + 3000))
            results.add(
                RawAiScan(
                    rawText = sub.trim(),
                    sourceTag = "Embedded Diffusion Metadata",
                    evidenceDescription = "Diffusion model inference steps and prompt structure detected."
                )
            )
        }

        // 5. Check Midjourney embedded signatures
        if (utf8Content.contains("Midjourney", ignoreCase = true) || utf8Content.contains("--v ") || utf8Content.contains("--ar ")) {
            val mjPattern = Pattern.compile("([^\\x00-\\x1F\\x7F-\\x9F]{10,800}(?:--v |--ar |--stylize |--seed )[^\\x00-\\x1F\\x7F-\\x9F]{0,300})")
            val m = mjPattern.matcher(utf8Content)
            if (m.find()) {
                results.add(
                    RawAiScan(
                        rawText = m.group(1)?.trim() ?: "Midjourney generation parameters",
                        sourceTag = "Embedded Parameters: Midjourney",
                        evidenceDescription = "Midjourney command flags (--v, --ar, --stylize) found in image stream."
                    )
                )
            } else if (!results.any { it.sourceTag.contains("Midjourney") }) {
                results.add(
                    RawAiScan(
                        rawText = "Midjourney signature detected in file metadata",
                        sourceTag = "Embedded Signature: Midjourney",
                        evidenceDescription = "Midjourney software attribution detected."
                    )
                )
            }
        }

        // 6. Check DALL-E / OpenAI / Bing Image Creator / Adobe Firefly XMP
        if (utf8Content.contains("DALL-E", ignoreCase = true) ||
            utf8Content.contains("openai", ignoreCase = true) ||
            utf8Content.contains("trainedAlgorithmicMedia", ignoreCase = true) ||
            utf8Content.contains("adobe:generative", ignoreCase = true)
        ) {
            results.add(
                RawAiScan(
                    rawText = "Generative AI provenance metadata detected in XMP/IPTC",
                    sourceTag = "XMP / IPTC: trainedAlgorithmicMedia",
                    evidenceDescription = "DigitalSourceType metadata indicates algorithmic generative origin."
                )
            )
        }

        // 7. Check C2PA / Content Credentials
        if (latin1Content.contains("c2pa", ignoreCase = true) ||
            latin1Content.contains("claim_generator", ignoreCase = true) ||
            latin1Content.contains("jumb", ignoreCase = true)
        ) {
            results.add(
                RawAiScan(
                    rawText = "C2PA Cryptographic Content Credentials manifest block detected",
                    sourceTag = "Provenance: C2PA Manifest",
                    evidenceDescription = "Coalition for Content Provenance and Authenticity (C2PA) manifest detected."
                )
            )
        }

        return results
    }

    fun parseAiMetadata(
        scans: List<RawAiScan>,
        exifComment: String? = null,
        exifDescription: String? = null,
        exifSoftware: String? = null
    ): AiGenerationMetadata? {
        val metaTagsInvolved = mutableListOf<String>()
        val allRawParts = mutableListOf<String>()
        var hasC2pa = false
        var c2paDetails: String? = null
        val evidenceList = mutableListOf<String>()

        scans.forEach {
            metaTagsInvolved.add(it.sourceTag)
            allRawParts.add(it.rawText)
            evidenceList.add(it.evidenceDescription)
            if (it.sourceTag.contains("C2PA", ignoreCase = true)) {
                hasC2pa = true
                c2paDetails = it.evidenceDescription
            }
        }

        if (!exifComment.isNullOrBlank() && looksLikeAiText(exifComment)) {
            metaTagsInvolved.add("EXIF UserComment (0x9286)")
            allRawParts.add(exifComment)
            evidenceList.add("Generative AI generation configuration stored in EXIF UserComment.")
        }
        if (!exifDescription.isNullOrBlank() && looksLikeAiText(exifDescription)) {
            metaTagsInvolved.add("EXIF ImageDescription (0x010E)")
            allRawParts.add(exifDescription)
            evidenceList.add("AI text prompt found in EXIF ImageDescription.")
        }
        if (!exifSoftware.isNullOrBlank() && isAiEngineName(exifSoftware)) {
            metaTagsInvolved.add("EXIF Software: \"$exifSoftware\"")
            allRawParts.add("Software: $exifSoftware")
            evidenceList.add("AI generation software recorded in EXIF Software tag.")
        }

        if (metaTagsInvolved.isEmpty() && allRawParts.isEmpty()) {
            return null
        }

        val combinedRaw = allRawParts.joinToString("\n\n")
        val summaryText = evidenceList.distinct().joinToString(" ")

        // 1. Try Automatic1111 / WebUI
        val sdResult = parseAutomatic1111(combinedRaw, metaTagsInvolved, summaryText, hasC2pa, c2paDetails)
        if (sdResult != null) return sdResult

        // 2. Try ComfyUI
        val comfyResult = parseComfyUi(combinedRaw, metaTagsInvolved, summaryText, hasC2pa, c2paDetails)
        if (comfyResult != null) return comfyResult

        // 3. Try Midjourney
        val mjResult = parseMidjourney(combinedRaw, metaTagsInvolved, summaryText, hasC2pa, c2paDetails)
        if (mjResult != null) return mjResult

        // 4. Try NovelAI
        val naiResult = parseNovelAi(combinedRaw, metaTagsInvolved, summaryText, hasC2pa, c2paDetails)
        if (naiResult != null) return naiResult

        // Generic fallback with precise disclaimer
        val engine = detectEngineName(combinedRaw, exifSoftware)
        return AiGenerationMetadata(
            detectedEngine = engine,
            evidenceSummary = summaryText.ifBlank { "Possible AI-generation evidence detected in file metadata headers." },
            confidence = "Metadata evidence only",
            positivePrompt = extractGeneralPrompt(combinedRaw),
            negativePrompt = null,
            rawParametersText = combinedRaw.take(3000),
            metaTagsInvolved = metaTagsInvolved.distinct(),
            hasC2paManifest = hasC2pa,
            c2paDetails = c2paDetails
        )
    }

    private fun parseAutomatic1111(
        text: String,
        tags: List<String>,
        summary: String,
        hasC2pa: Boolean,
        c2paDetails: String?
    ): AiGenerationMetadata? {
        val stepsPattern = Pattern.compile("(?i)Steps:\\s*(\\d+)")
        val stepsMatcher = stepsPattern.matcher(text)
        val hasSteps = stepsMatcher.find()
        val hasNegative = text.contains("Negative prompt:", ignoreCase = true)

        if (!hasSteps && !hasNegative && !text.contains("CFG scale:", ignoreCase = true)) {
            return null
        }

        var positivePrompt: String? = null
        var negativePrompt: String? = null
        val otherParams = mutableMapOf<String, String>()

        val negIdx = text.indexOf("Negative prompt:", ignoreCase = true)
        val stepsIdx = findStepsIndex(text)

        if (negIdx != -1) {
            positivePrompt = text.substring(0, negIdx).trim()
            if (stepsIdx != -1 && stepsIdx > negIdx) {
                negativePrompt = text.substring(negIdx + "Negative prompt:".length, stepsIdx).trim()
            } else {
                negativePrompt = text.substring(negIdx + "Negative prompt:".length).trim()
            }
        } else if (stepsIdx != -1) {
            positivePrompt = text.substring(0, stepsIdx).trim()
        }

        val paramSection = if (stepsIdx != -1) text.substring(stepsIdx) else text
        val steps = extractRegex(paramSection, "(?i)Steps:\\s*(\\d+)")
        val sampler = extractRegex(paramSection, "(?i)Sampler:\\s*([^,]+)")
        val cfg = extractRegex(paramSection, "(?i)CFG scale:\\s*([0-9.]+)")
        val seed = extractRegex(paramSection, "(?i)Seed:\\s*(\\d+)")
        val size = extractRegex(paramSection, "(?i)Size:\\s*(\\d+x\\d+)")
        val model = extractRegex(paramSection, "(?i)Model:\\s*([^,]+)")
        val modelHash = extractRegex(paramSection, "(?i)Model hash:\\s*([^,]+)")
        val denoise = extractRegex(paramSection, "(?i)Denoising strength:\\s*([0-9.]+)")
        val clipSkip = extractRegex(paramSection, "(?i)Clip skip:\\s*(\\d+)")

        if (modelHash != null) otherParams["Model Hash"] = modelHash
        if (denoise != null) otherParams["Denoising Strength"] = denoise
        if (clipSkip != null) otherParams["Clip Skip"] = clipSkip

        val loras = mutableListOf<String>()
        val loraPattern = Pattern.compile("<lora:([^:>]+):?([^>]*)>")
        val loraMatcher = loraPattern.matcher(text)
        while (loraMatcher.find()) {
            val name = loraMatcher.group(1) ?: ""
            val weight = loraMatcher.group(2) ?: "1.0"
            loras.add("$name ($weight)")
        }

        return AiGenerationMetadata(
            detectedEngine = "Stable Diffusion (WebUI / Forge)",
            evidenceSummary = summary.ifBlank { "Generative AI parameters and prompt discovered in metadata." },
            confidence = "Metadata evidence only",
            positivePrompt = positivePrompt?.takeIf { it.isNotBlank() },
            negativePrompt = negativePrompt?.takeIf { it.isNotBlank() },
            steps = steps,
            sampler = sampler,
            cfgScale = cfg,
            seed = seed,
            model = model,
            dimensions = size,
            loras = loras,
            otherParameters = otherParams,
            rawParametersText = text.trim(),
            metaTagsInvolved = tags.distinct(),
            hasC2paManifest = hasC2pa,
            c2paDetails = c2paDetails
        )
    }

    private fun parseComfyUi(
        text: String,
        tags: List<String>,
        summary: String,
        hasC2pa: Boolean,
        c2paDetails: String?
    ): AiGenerationMetadata? {
        if (!text.contains("\"inputs\"") && !text.contains("\"class_type\"")) return null
        return try {
            val jsonStart = text.indexOf("{")
            val jsonEnd = text.lastIndexOf("}")
            if (jsonStart == -1 || jsonEnd <= jsonStart) return null

            val jsonStr = text.substring(jsonStart, jsonEnd + 1)
            val root = JSONObject(jsonStr)

            var posPrompt: String? = null
            var negPrompt: String? = null
            var steps: String? = null
            var sampler: String? = null
            var cfg: String? = null
            var seed: String? = null
            var model: String? = null
            val loras = mutableListOf<String>()

            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val node = root.optJSONObject(key) ?: continue
                val classType = node.optString("class_type", "")
                val inputs = node.optJSONObject("inputs") ?: continue

                when {
                    classType.contains("CLIPTextEncode", ignoreCase = true) -> {
                        val promptText = inputs.optString("text", "")
                        if (promptText.isNotBlank()) {
                            if (posPrompt == null) posPrompt = promptText
                            else if (negPrompt == null) negPrompt = promptText
                        }
                    }
                    classType.contains("KSampler", ignoreCase = true) -> {
                        if (steps == null) steps = inputs.optString("steps", null)
                        if (sampler == null) {
                            val sName = inputs.optString("sampler_name", "")
                            val sched = inputs.optString("scheduler", "")
                            sampler = if (sched.isNotBlank()) "$sName ($sched)" else sName
                        }
                        if (cfg == null) cfg = inputs.optString("cfg", null)
                        if (seed == null) seed = inputs.optString("seed", null)
                    }
                    classType.contains("CheckpointLoader", ignoreCase = true) -> {
                        if (model == null) model = inputs.optString("ckpt_name", null)
                    }
                    classType.contains("LoraLoader", ignoreCase = true) -> {
                        val loraName = inputs.optString("lora_name", "")
                        val str = inputs.optString("strength_model", "1.0")
                        if (loraName.isNotBlank()) loras.add("$loraName ($str)")
                    }
                }
            }

            AiGenerationMetadata(
                detectedEngine = "ComfyUI (Node Graph Workflow)",
                evidenceSummary = summary.ifBlank { "ComfyUI node execution graph and prompt metadata discovered." },
                confidence = "Metadata evidence only",
                positivePrompt = posPrompt,
                negativePrompt = negPrompt,
                steps = steps,
                sampler = sampler,
                cfgScale = cfg,
                seed = seed,
                model = model,
                loras = loras,
                rawParametersText = text.trim(),
                metaTagsInvolved = tags.distinct(),
                hasC2paManifest = hasC2pa,
                c2paDetails = c2paDetails
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseMidjourney(
        text: String,
        tags: List<String>,
        summary: String,
        hasC2pa: Boolean,
        c2paDetails: String?
    ): AiGenerationMetadata? {
        if (!text.contains("--v ") && !text.contains("--ar ") && !text.contains("Midjourney", ignoreCase = true)) {
            return null
        }

        val flagIdx = text.indexOf("--")
        val prompt = if (flagIdx != -1) text.substring(0, flagIdx).trim() else text.trim()
        val version = extractRegex(text, "--v\\s+([0-9.]+)")
        val ar = extractRegex(text, "--ar\\s+([0-9:]+)")
        val seed = extractRegex(text, "--seed\\s+(\\d+)")
        val stylize = extractRegex(text, "--stylize\\s+(\\d+)") ?: extractRegex(text, "--s\\s+(\\d+)")
        val chaos = extractRegex(text, "--chaos\\s+(\\d+)")

        val other = mutableMapOf<String, String>()
        if (stylize != null) other["Stylize"] = stylize
        if (chaos != null) other["Chaos"] = chaos

        return AiGenerationMetadata(
            detectedEngine = "Midjourney (v${version ?: "6"})",
            evidenceSummary = summary.ifBlank { "Midjourney parameters and generation flags found." },
            confidence = "Metadata evidence only",
            positivePrompt = prompt.takeIf { it.isNotBlank() },
            negativePrompt = null,
            model = "Midjourney ${version?.let { "v$it" } ?: ""}".trim(),
            dimensions = ar?.let { "Aspect Ratio $it" },
            seed = seed,
            otherParameters = other,
            rawParametersText = text.trim(),
            metaTagsInvolved = tags.distinct(),
            hasC2paManifest = hasC2pa,
            c2paDetails = c2paDetails
        )
    }

    private fun parseNovelAi(
        text: String,
        tags: List<String>,
        summary: String,
        hasC2pa: Boolean,
        c2paDetails: String?
    ): AiGenerationMetadata? {
        if (!text.contains("\"uc\":") && !text.contains("\"prompt\":")) return null
        return try {
            val jsonStart = text.indexOf("{")
            val jsonEnd = text.lastIndexOf("}")
            if (jsonStart == -1) return null
            val obj = JSONObject(text.substring(jsonStart, jsonEnd + 1))
            AiGenerationMetadata(
                detectedEngine = "NovelAI Diffusion",
                evidenceSummary = summary.ifBlank { "NovelAI generation parameters found in metadata." },
                confidence = "Metadata evidence only",
                positivePrompt = obj.optString("prompt", null),
                negativePrompt = obj.optString("uc", null),
                steps = obj.optString("steps", null),
                cfgScale = obj.optString("scale", null),
                seed = obj.optString("seed", null),
                sampler = obj.optString("sampler", null),
                rawParametersText = text.trim(),
                metaTagsInvolved = tags.distinct(),
                hasC2paManifest = hasC2pa,
                c2paDetails = c2paDetails
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun extractRegex(input: String, patternStr: String): String? {
        val p = Pattern.compile(patternStr)
        val m = p.matcher(input)
        return if (m.find()) m.group(1)?.trim() else null
    }

    private fun findStepsIndex(text: String): Int {
        val p = Pattern.compile("(?i)Steps:\\s*\\d+")
        val m = p.matcher(text)
        return if (m.find()) m.start() else -1
    }

    private fun looksLikeAiText(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("steps:") ||
                lower.contains("sampler:") ||
                lower.contains("cfg scale:") ||
                lower.contains("seed:") ||
                lower.contains("negative prompt:") ||
                lower.contains("midjourney") ||
                lower.contains("comfyui") ||
                lower.contains("--v ") ||
                lower.contains("--ar ") ||
                lower.contains("dall-e") ||
                lower.contains("flux") ||
                lower.contains("parameters") ||
                lower.contains("trainedalgorithmicmedia")
    }

    private fun isAiEngineName(software: String): Boolean {
        val lower = software.lowercase()
        return lower.contains("stable diffusion") ||
                lower.contains("midjourney") ||
                lower.contains("comfyui") ||
                lower.contains("novelai") ||
                lower.contains("dall-e") ||
                lower.contains("flux") ||
                lower.contains("automatic1111") ||
                lower.contains("webui") ||
                lower.contains("firefly") ||
                lower.contains("trainedalgorithmicmedia")
    }

    private fun detectEngineName(text: String, software: String?): String {
        val lower = text.lowercase()
        val sLower = software?.lowercase() ?: ""
        return when {
            lower.contains("midjourney") || sLower.contains("midjourney") -> "Midjourney"
            lower.contains("dall-e") || lower.contains("openai") -> "OpenAI DALL-E"
            lower.contains("comfyui") || sLower.contains("comfyui") -> "ComfyUI Workflow"
            lower.contains("novelai") -> "NovelAI"
            lower.contains("flux") -> "Flux.1 Generator"
            lower.contains("c2pa") || lower.contains("claim_generator") -> "C2PA Provenance Manifest"
            lower.contains("firefly") || sLower.contains("firefly") -> "Adobe Firefly"
            else -> "AI Generator (Metadata Fingerprint)"
        }
    }

    private fun extractGeneralPrompt(text: String): String {
        return text.take(600).filter { it.code in 32..126 || it == '\n' || it == '\r' || it == '\t' }.trim()
    }

    private fun findChunkEnd(buffer: ByteArray, start: Int, max: Int): Int {
        var i = start
        while (i < max && i < start + 6000) {
            if (buffer[i] == 0.toByte() && i + 1 < max && buffer[i + 1] == 0.toByte()) {
                return i
            }
            i++
        }
        return minOf(max, start + 6000)
    }

    private fun extractSafeString(buffer: ByteArray, start: Int, end: Int): String {
        val len = maxOf(0, end - start)
        if (len <= 0) return ""
        return try {
            String(buffer, start, len, StandardCharsets.UTF_8)
                .filter { it.code in 32..126 || it == '\n' || it == '\r' || it == '\t' }
                .trim()
        } catch (_: Exception) {
            ""
        }
    }
}
