package com.example.aimetadatacleaner.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.aimetadatacleaner.R
import com.example.aimetadatacleaner.data.model.ImageInspectionResult
import com.example.aimetadatacleaner.data.model.MetadataStandard
import com.example.aimetadatacleaner.data.model.PrivacyExposure
import com.example.aimetadatacleaner.ui.MainViewModel
import com.example.aimetadatacleaner.ui.components.AiMetadataInspectorCard
import com.example.aimetadatacleaner.ui.components.CategorySectionCard
import com.example.aimetadatacleaner.ui.components.CleaningOptionToggle
import com.example.aimetadatacleaner.ui.components.PrivacyExposureMeter
import com.example.aimetadatacleaner.ui.components.PrivacyReportDialog
import com.example.aimetadatacleaner.ui.theme.AmberWarning
import com.example.aimetadatacleaner.ui.theme.CyanAccent
import com.example.aimetadatacleaner.ui.theme.EmeraldSuccess
import com.example.aimetadatacleaner.ui.theme.IndigoLight
import com.example.aimetadatacleaner.ui.theme.RedDanger
import com.example.aimetadatacleaner.ui.theme.Slate400
import com.example.aimetadatacleaner.ui.theme.Slate700
import com.example.aimetadatacleaner.ui.theme.Slate800
import com.example.aimetadatacleaner.ui.theme.Slate950

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val inspection by viewModel.inspectionResult.collectAsStateWithLifecycle()
    val isInspecting by viewModel.isInspecting.collectAsStateWithLifecycle()
    val isCleaning by viewModel.isCleaning.collectAsStateWithLifecycle()
    val options by viewModel.cleaningOptions.collectAsStateWithLifecycle()
    val cleanResult by viewModel.cleanResult.collectAsStateWithLifecycle()
    val activeReport by viewModel.activePrivacyReport.collectAsStateWithLifecycle()
    val batchUris by viewModel.batchUris.collectAsStateWithLifecycle()

    if (batchUris.isNotEmpty() && inspection == null) {
        BatchScreen(
            viewModel = viewModel,
            modifier = modifier,
            onClearBatch = { viewModel.clearBatch() }
        )
        return
    }

    val singlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectImage(uri)
        }
    }

    val multiPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 30)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            if (uris.size == 1) {
                viewModel.selectImage(uris.first())
            } else {
                viewModel.setBatchUris(uris)
                viewModel.showToast("Loaded ${uris.size} files for batch cleaning.")
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(2.dp))
        }

        // 1. BRAND HEADER & LOCAL PROCESSING BADGE (Section 3)
        item {
            MainBrandHeader()
        }

        if (isInspecting) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Slate800)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = CyanAccent,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Running Privacy & AI Metadata Scan...",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Extracting EXIF, XMP, GPS, PNG chunks, and C2PA manifests",
                                color = Slate400,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        } else if (inspection != null) {
            // ACTIVE FILE INSPECTION WORKFLOW: Scan -> Explain -> Clean -> Verify
            item {
                ActivePhotoControlBar(
                    inspection = inspection!!,
                    onChangeFile = {
                        singlePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    onClear = { viewModel.clearSelection() }
                )
            }

            // PRIVACY SCAN BANNER & EXPOSURE METER (Section 4)
            item {
                PrivacyScanSummaryCard(inspection = inspection!!)
            }

            // PRIVACY EXPOSURE BAR
            item {
                PrivacyExposureMeter(exposure = inspection!!.riskLevel)
            }

            // AI GENERATION EVIDENCE CARD (Section 5)
            if (inspection!!.hasAiMetadata || inspection!!.aiMetadata != null) {
                item {
                    AiMetadataInspectorCard(
                        aiMetadata = inspection!!.aiMetadata,
                        onCopyFeedback = { viewModel.showToast(it) }
                    )
                }
            }

            // DETAILED METADATA VIEWER (Section 6)
            item {
                Text(
                    text = "DETAILED METADATA VIEWER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            val grouped = inspection!!.entries.groupBy { it.category }
            grouped.forEach { (category, entries) ->
                item {
                    CategorySectionCard(category = category, entries = entries)
                }
            }

            // SANITIZATION SETTINGS
            item {
                Text(
                    text = "SANITIZATION SETTINGS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                CleaningOptionToggle(
                    title = "Strip All Metadata (Recommended)",
                    subtitle = "Eliminate 100% of EXIF, GPS, AI tags, XMP, IPTC, and device serials",
                    checked = options.stripAll,
                    onCheckedChange = { viewModel.updateOptions { o -> o.copy(stripAll = it) } },
                    testTag = "toggle_strip_all"
                )
            }

            if (!options.stripAll) {
                item {
                    CleaningOptionToggle(
                        title = "Strip AI Generation Metadata",
                        subtitle = "Purge diffusion prompts, models, seeds, and workflow graphs",
                        checked = options.stripAiMetadata,
                        onCheckedChange = { viewModel.updateOptions { o -> o.copy(stripAiMetadata = it) } },
                        testTag = "toggle_strip_ai"
                    )
                }
                item {
                    CleaningOptionToggle(
                        title = "Strip Location & GPS Coordinates",
                        subtitle = "Wipe precise latitude, longitude, and altitude geotags",
                        checked = options.stripLocationGps,
                        onCheckedChange = { viewModel.updateOptions { o -> o.copy(stripLocationGps = it) } },
                        testTag = "toggle_strip_gps",
                        isWarning = true
                    )
                }
                item {
                    CleaningOptionToggle(
                        title = "Strip Camera & Device ID",
                        subtitle = "Wipe phone model, lens specs, camera serial",
                        checked = options.stripCameraDevice,
                        onCheckedChange = { viewModel.updateOptions { o -> o.copy(stripCameraDevice = it) } },
                        testTag = "toggle_strip_camera"
                    )
                }
                item {
                    CleaningOptionToggle(
                        title = "Strip Timestamps & Dates",
                        subtitle = "Wipe creation and digitized date-time records",
                        checked = options.stripTimestamps,
                        onCheckedChange = { viewModel.updateOptions { o -> o.copy(stripTimestamps = it) } },
                        testTag = "toggle_strip_timestamps"
                    )
                }
            }

            // CLEAN ACTION BUTTON (Workflow: Scan -> Explain -> Clean -> Verify)
            item {
                Button(
                    onClick = { viewModel.cleanCurrentImage() },
                    enabled = !isCleaning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("clean_photo_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = Slate950
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isCleaning) {
                        CircularProgressIndicator(
                            color = Slate950,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Cleaning & Verifying...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = "Clean",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Clean & Verify File",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        } else {
            // 2. LARGE DROPZONE / UPLOAD AREA (Section 3)
            item {
                ModernDropzoneUploadArea(
                    onChooseSingle = {
                        singlePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    onChooseMultiple = {
                        multiPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    }
                )
            }

            // 3. CORE WORKFLOW EXPLANATION CARD: Scan -> Explain -> Clean -> Verify
            item {
                CoreWorkflowCard()
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Cleaned result dialog with Verification & Before/After Comparison
    cleanResult?.let { result ->
        CleanResultDialog(
            result = result,
            onDismiss = { viewModel.dismissCleanResult() },
            onSaveToGallery = { path -> viewModel.saveCleanedToGallery(path) },
            onShare = {
                viewModel.shareImage(result.cleanedUri, result.cleanedFileName)
            },
            onViewReport = {
                result.privacyReport?.let { report ->
                    viewModel.showPrivacyReport(report)
                }
            }
        )
    }

    // Privacy Inspection Report Dialog
    activeReport?.let { report ->
        PrivacyReportDialog(
            report = report,
            onDismiss = { viewModel.dismissPrivacyReport() },
            onShare = { viewModel.sharePrivacyReport(report) },
            onCopyFeedback = { viewModel.showToast(it) }
        )
    }
}

/**
 * Main brand header matching Section 3 specification:
 * Brand: AI Metadata Cleaner
 * Subtitle: Inspect. Clean. Verify. Protect your privacy.
 * Status indicator: ● Local Processing
 */
@Composable
fun MainBrandHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(EmeraldSuccess)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Local Processing",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldSuccess.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "100% On-Device",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldSuccess,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Inspect. Clean. Verify. Protect your privacy.",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "🔒 Your files are processed locally on your device. Zero cloud uploads, zero telemetry.",
                fontSize = 12.sp,
                color = Slate400,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Modern upload dropzone matching Section 3:
 * "Drop your file here or [ Choose File ]
 * Supported formats: JPG • PNG • WebP • HEIC • PDF • MP4 • MOV"
 */
@Composable
fun ModernDropzoneUploadArea(
    onChooseSingle: () -> Unit,
    onChooseMultiple: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("upload_dropzone_area"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, CyanAccent.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(CyanAccent.copy(alpha = 0.12f))
                    .border(1.5.dp, CyanAccent.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = "Drop file",
                    tint = CyanAccent,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Drop your file here",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "or select from your photo & video library",
                fontSize = 13.sp,
                color = Slate400
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Choose File button
            Button(
                onClick = onChooseSingle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("choose_file_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanAccent,
                    contentColor = Slate950
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Choose File",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Batch selection button
            OutlinedButton(
                onClick = onChooseMultiple,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("choose_multiple_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderZip,
                    contentDescription = null,
                    tint = IndigoLight,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Batch Select Multiple Files",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Formats list footer
            Text(
                text = "Supported formats:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "JPG • PNG • WebP • HEIC • PDF • MP4 • MOV",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Slate400
            )
        }
    }
}

/**
 * Explains the 4-phase workflow: Scan -> Explain -> Clean -> Verify
 */
@Composable
fun CoreWorkflowCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "PRIVACY-FIRST METHODOLOGY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            WorkflowStepRow(step = "1. SCAN", title = "Extract all EXIF, XMP, GPS & AI parameters")
            WorkflowStepRow(step = "2. EXPLAIN", title = "Categorize sensitivity & show detected evidence")
            WorkflowStepRow(step = "3. CLEAN", title = "Sanitize pixels & purge all digital tracking chunks")
            WorkflowStepRow(step = "4. VERIFY", title = "Run secondary independent audit before download")
        }
    }
}

@Composable
private fun WorkflowStepRow(step: String, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            modifier = Modifier.width(76.dp)
        ) {
            Text(
                text = step,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 3.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ActivePhotoControlBar(
    inspection: ImageInspectionResult,
    onChangeFile: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                AsyncImage(
                    model = inspection.uri,
                    contentDescription = "Preview",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = inspection.fileName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${inspection.mimeType.substringAfter("/").uppercase()} • ${inspection.fileSizeBytes / 1024} KB",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onChangeFile,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Text("Change", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Slate400,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PrivacyScanSummaryCard(inspection: ImageInspectionResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PRIVACY SCAN FINDINGS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "${inspection.entries.size} metadata items detected",
                    fontSize = 11.sp,
                    color = Slate400
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Category Status Overview (Section 4)
            ScanCategoryItem(
                categoryName = "EXIF Camera & Device",
                found = inspection.entries.any { it.standard == MetadataStandard.EXIF },
                isSensitive = inspection.entries.any { it.standard == MetadataStandard.EXIF && it.isSensitive }
            )
            ScanCategoryItem(
                categoryName = "GPS Coordinates / Geotag",
                found = inspection.hasGpsLocation,
                isSensitive = true
            )
            ScanCategoryItem(
                categoryName = "XMP Extensible Packet",
                found = inspection.entries.any { it.standard == MetadataStandard.XMP },
                isSensitive = true
            )
            ScanCategoryItem(
                categoryName = "AI Generation Parameters",
                found = inspection.hasAiMetadata,
                isSensitive = true
            )
            ScanCategoryItem(
                categoryName = "C2PA Content Credentials",
                found = inspection.hasC2pa,
                isSensitive = false
            )
        }
    }
}

@Composable
private fun ScanCategoryItem(
    categoryName: String,
    found: Boolean,
    isSensitive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = categoryName,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (found) "FOUND" else "NOT FOUND",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (found) (if (isSensitive) RedDanger else AmberWarning) else EmeraldSuccess
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = if (found) (if (isSensitive) Icons.Default.Warning else Icons.Default.Info) else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (found) (if (isSensitive) RedDanger else AmberWarning) else EmeraldSuccess,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
