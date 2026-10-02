# Metadata Cleaner

> **Inspect, remove, and verify metadata and AI-generation evidence from your files — with privacy-first processing.**

[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Linux%20%7C%20Docker-blue.svg)](https://github.com)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20On--Device-success.svg)](https://github.com)
[![Verification](https://img.shields.io/badge/Verification-Independent%20Audit-emerald.svg)](https://github.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](LICENSE)

**Metadata Cleaner (aimc)** is a professional, privacy-focused security utility engineered to discover, explain, sanitize, and independently verify hidden metadata, geotags, and generative AI parameter footprints in images and multimedia files.

---

## 🧭 Core Workflow: Scan → Explain → Clean → Verify

Unlike conventional metadata strippers that silently overwrite files or make unverified claims, Metadata Cleaner operates on a transparent four-phase privacy pipeline:

```text
       ┌───────────────────────────┐
       │        UPLOAD FILE        │
       └─────────────┬─────────────┘
                     ▼
       ┌───────────────────────────┐
       │     1. PRIVACY SCAN       │
       │  (EXIF, XMP, GPS, AI Gen) │
       └─────────────┬─────────────┘
                     ▼
       ┌───────────────────────────┐
       │    2. EXPLAIN FINDINGS    │
       │   (Exposure Meter & Risk) │
       └─────────────┬─────────────┘
                     ▼
       ┌───────────────────────────┐
       │   3. REMOVE METADATA      │
       │  (Pixel-Pristine Remux)   │
       └─────────────┬─────────────┘
                     ▼
       ┌───────────────────────────┐
       │   4. VERIFY CLEAN FILE    │
       │  (Independent 2nd Scan)   │
       └─────────────┬─────────────┘
                     ▼
       ┌───────────────────────────┐
       │    DOWNLOAD CLEAN FILE    │
       │    [ VERIFIED CLEAN ✓ ]   │
       └───────────────────────────┘
```

---

## 🛡️ Key Features

### 1. Privacy Scanner
- **Privacy Exposure Meter**: Categorizes exposure as `LOW`, `MEDIUM`, or `HIGH` (`████████░░ HIGH`). Explains that the indicator represents detected metadata exposure rather than an absolute security guarantee.
- **Deep Standards Extraction**:
  - **EXIF**: Camera make, camera model, serial number, lens configuration, aperture, shutter speed, ISO, focal length, timestamps.
  - **GPS**: High-precision latitude, longitude, altitude, and satellite sync timestamps.
  - **XMP / IPTC**: Extensible Metadata Platform packets, creator credits, copyright notices, editing history.
  - **Embedded Other**: Embedded preview thumbnails, ICC color profiles, and XML chunks.

### 2. Generative AI Evidence & Parameter Detection
- **Strict Evidence Principle**: We do **not** claim *"This image was definitely created by AI"*. Instead, the app precisely reports *"AI generation metadata detected"* with confidence classified as *"Metadata evidence only"*.
- **Supported AI Frameworks**:
  - **Stable Diffusion (WebUI / Automatic1111 / Forge)**: Positive prompts, negative prompts, seed, steps, sampler scheduler, CFG scale, base checkpoint, and LoRA weights.
  - **ComfyUI**: Visual node graphs, prompt pipelines, and workflow canvas state.
  - **Midjourney**: Prompt text, aspect ratios (`--ar`), version flags (`--v`), stylize multipliers (`--s`), and chaos seeds.
  - **NovelAI**: Generation configurations and prompt structures.
  - **OpenAI / DALL-E / Firefly**: DigitalSourceType (`trainedAlgorithmicMedia`) attribution tags.
  - **C2PA / Content Credentials**: Detects cryptographic provenance manifests and CAI assertion blocks.

### 3. Independent Verification System
- After sanitization, an independent second scan inspects the output file across 7 verification vectors:
  1. `✓ EXIF scan`
  2. `✓ XMP scan`
  3. `✓ IPTC scan`
  4. `✓ GPS scan`
  5. `✓ AI metadata scan`
  6. `✓ C2PA scan`
  7. `✓ Embedded metadata scan`
- Distinguishes **`VERIFIED CLEAN`** from **`VERIFICATION INCOMPLETE`**. If any metadata remains, the exact residual fields are reported.

### 4. Side-by-Side Before / After Comparison
- Visual status indicators:
  - `✓ Removed`: Successfully stripped and confirmed absent in verification.
  - `⚠ Preserved`: Kept per user settings.
  - `— Not present`: Not found in the original file.
  - `? Unable to verify`: Could not independently verify.

### 5. Batch Sanitization
- Select and process up to 50 files simultaneously.
- Queue breakdown: files with metadata, files with GPS, files with AI metadata, and files with C2PA manifests.
- Post-processing summary: `X processed, Y verified clean, Z partially cleaned, W failed`.

### 6. Exportable Privacy Inspection Reports
- Generates structured audit reports with sensitive field counts, detection breakdown, and verification proofs.
- One-tap export to formatted plain text or structured JSON.

---

## 📂 Supported Formats Matrix

| Category | File Extensions | Support Level | Capabilities |
| :--- | :--- | :--- | :--- |
| **Images** | `JPG`, `JPEG`, `PNG`, `WebP`, `HEIC`, `AVIF`, `TIFF` | **Supported** | Full scan, pixel reconstruction, metadata purge & secondary verification |
| **RAW** | `DNG`, `CR2`, `CR3`, `NEF`, `ARW` | **Partially Supported** | Header extraction, EXIF inspection & DNG sanitization |
| **Video** | `MP4`, `MOV`, `M4V`, `MKV`, `WebM` | **Scan Only** | Container metadata, GPS track & codec profile extraction |
| **Documents** | `PDF`, `DOCX`, `XLSX`, `PPTX` | **Scan Only** | Creator, author, revision count, and modification dates |
| **Audio** | `MP3`, `M4A`, `WAV` | **Scan Only** | ID3 tags, artist, album, and encoder software |

---

## 🔒 Privacy Model & Security Architecture

1. **100% On-Device Processing**:
   - `● Local Processing`: All file reading, chunk parsing, pixel decompression, and re-encoding occur purely in volatile device memory.
   - Zero network requests, zero remote analytics, and zero cloud backups.
2. **Untrusted Input Hardening**:
   - **Magic Byte Validation**: Sniffs true binary file headers (`FF D8 FF` for JPEG, `89 50 4E 47` for PNG, `RIFF...WEBP` for WebP, `%PDF-` for PDF) rather than trusting user-provided file extensions or MIME types.
   - **Path Traversal Protection**: Replaces directory traversal sequences (`../`, `..\\`), null bytes (`\0`), and illegal filesystem characters.
   - **Canonical Path Bounds**: Verifies that written output files remain strictly inside app-internal cache bounds.
   - **Resource Limits**: 150MB maximum file size guards against memory starvation and decompression bombs.
   - **Log Redaction**: Diagnostic logs never output GPS coordinates, prompts, creator names, or file payloads.

---

## 💻 CLI (aimc)

The headless CLI engine allows command-line scanning, cleaning, and verification:

```bash
# Scan a photo for hidden metadata and AI parameters
aimc scan photo.jpg

# Clean an image
aimc clean photo.jpg --output ./sanitized

# Independently verify a cleaned image
aimc verify ./sanitized/clean_photo.jpg

# Batch sanitize an entire folder
aimc clean ./photos --output ./clean-photos
```

### Example CLI Output:
```text
AI Metadata Cleaner (aimc)
Scanning: photo.jpg [JPEG Image]
----------------------------------------
GPS:            FOUND [⚠ High Privacy Exposure]
Camera/Device:  FOUND [⚠]
XMP:            FOUND [⚠]
AI metadata:    FOUND [⚠ Evidence detected]
C2PA:           NOT FOUND [✓]
----------------------------------------
Cleaning...
✓ EXIF removed
✓ XMP removed
✓ GPS wiped
✓ AI parameters stripped

Verifying output...
✓ EXIF scan: PASSED
✓ GPS scan: PASSED
✓ XMP scan: PASSED
✓ AI metadata scan: PASSED
✓ C2PA scan: PASSED

RESULT: ✓ VERIFIED CLEAN
```

---

## 🐳 Docker / Self-Hosted Deployment

Deploy as a local microservice or self-hosted utility:

```bash
# Clone the repository
git clone https://github.com/mohdismailmatasin/ai-metadatacleaner.git
cd ai-metadatacleaner

# Launch with Docker Compose
docker compose up -d
```

### Environment Configuration:
```yaml
services:
  ai-metadatacleaner:
    build: .
    container_name: aimc-sanitizer
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      - AIMC_PORT=8080
      - AIMC_LOG_LEVEL=INFO
    volumes:
      - ./data/input:/app/storage/input:ro
      - ./data/output:/app/storage/output:rw
```

---

## 🌐 REST API Specification

Optional headless API endpoints for automated pipelines:

### `POST /api/scan`
Inspects an uploaded multipart file:
```json
{
  "filename": "render.png",
  "mime_type": "image/png",
  "privacy_exposure": "HIGH",
  "metadata": {
    "gps": false,
    "exif": true,
    "xmp": true,
    "ai_metadata": true,
    "c2pa": false
  },
  "ai_evidence": {
    "engine": "Stable Diffusion (WebUI / Forge)",
    "confidence": "Metadata evidence only",
    "prompt_detected": true
  }
}
```

### `POST /api/clean`
Sanitizes the file and runs secondary verification:
```json
{
  "status": "success",
  "is_verified_clean": true,
  "tags_removed": 14,
  "verification": {
    "status": "VERIFIED CLEAN",
    "checks_passed": 7,
    "remaining_fields": 0
  }
}
```

### `POST /api/verify`
Performs an independent verification scan on any file.

---

## 🛠️ Testing & Verification

Comprehensive local JVM unit tests are implemented using JUnit:

```bash
# Run unit tests
gradle :app:testDebugUnitTest

# Assemble debug APK
gradle :app:assembleDebug
```

### Test Coverage:
- File signature & magic bytes validation (JPEG, PNG, WebP, PDF)
- Malicious filename sanitization and path traversal prevention
- Stable Diffusion Automatic1111 parameter extraction
- Midjourney flags and prompt parsing
- C2PA manifest detection
- Independent verification pass/fail audit
- Verification detection of intentional residual XMP & AI chunks
- Privacy inspection report text and JSON generation

---

## 🗺️ Roadmap

- [x] Independent secondary verification engine (`MetadataVerifier`)
- [x] Before / After comparison breakdown
- [x] Magic byte & file signature security validation
- [x] Exportable Privacy Inspection Reports (Text & JSON)
- [x] Batch processing with verification statistics
- [ ] Direct WebAssembly (Wasm) browser engine for zero-install client processing
- [ ] Lossless video stream remuxing for MP4/MOV container stripping
- [ ] Expanded RAW profile sanitization (CR3, ARW, NEF)
- [ ] Hardware-bound cryptographic signing of sanitization proofs

---

## 📄 License & Attribution

All Rights Reserved © Mohd Ismail Mat Asin (mohdismailmatasin@gmail.com) 2026.
Licensed under the Apache License, Version 2.0.
