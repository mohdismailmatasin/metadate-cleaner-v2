package com.example.aimetadatacleaner.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.aimetadatacleaner.data.model.CleanExecutionResult
import com.example.aimetadatacleaner.ui.BatchItemInspection
import com.example.aimetadatacleaner.ui.MainViewModel
import com.example.aimetadatacleaner.ui.components.AiMetadataInspectorCard
import com.example.aimetadatacleaner.ui.components.PrivacyReportDialog
import com.example.aimetadatacleaner.ui.theme.AmberWarning
import com.example.aimetadatacleaner.ui.theme.CyanAccent
import com.example.aimetadatacleaner.ui.theme.EmeraldSuccess
import com.example.aimetadatacleaner.ui.theme.IndigoLight
import com.example.aimetadatacleaner.ui.theme.RedDanger
import com.example.aimetadatacleaner.ui.theme.Slate400
import com.example.aimetadatacleaner.ui.theme.Slate800
import com.example.aimetadatacleaner.ui.theme.Slate950

@Composable
fun BatchScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val batchUris by viewModel.batchUris.collectAsStateWithLifecycle()
    val batchState by viewModel.batchState.collectAsStateWithLifecycle()
    val batchInspections by viewModel.batchInspections.collectAsStateWithLifecycle()
    val activeReport by viewModel.activePrivacyReport.collectAsStateWithLifecycle()

    val multiPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.setBatchUris(uris)
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(IndigoLight.copy(alpha = 0.15f))
                                .border(1.dp, IndigoLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderZip,
                                contentDescription = "Batch Clean",
                                tint = IndigoLight,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Batch Metadata Sanitizer",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Inspect, strip & verify multiple files simultaneously",
                                fontSize = 12.sp,
                                color = Slate400
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            multiPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("select_batch_photos_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = Slate950
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Pick Photos",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (batchUris.isEmpty()) "Select Files for Batch" else "Reselect Files (${batchUris.size} loaded)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (batchUris.isNotEmpty()) {
            val totalFiles = batchUris.size
            val withMetadataCount = batchInspections.count { (it.inspection?.entries?.size ?: 0) > 0 }
            val gpsCount = batchInspections.count { it.inspection?.hasGpsLocation == true }
            val aiCount = batchInspections.count { it.inspection?.hasAiMetadata == true }
            val c2paCount = batchInspections.count { it.inspection?.hasC2pa == true }

            // BATCH FINDINGS BREAKDOWN (Section 9)
            item {
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
                                text = "$totalFiles FILES QUEUED",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            if (batchInspections.any { it.isInspecting }) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CyanAccent)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BatchStatPill("$withMetadataCount", "Contain Meta", CyanAccent, Modifier.weight(1f))
                            BatchStatPill("$gpsCount", "Contain GPS", RedDanger, Modifier.weight(1f))
                            BatchStatPill("$aiCount", "Contain AI", IndigoLight, Modifier.weight(1f))
                            BatchStatPill("$c2paCount", "Contain C2PA", EmeraldSuccess, Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (batchState.isRunning) {
                            val progress = if (batchState.total > 0) batchState.current.toFloat() / batchState.total.toFloat() else 0f
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = CyanAccent,
                                trackColor = Slate800
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Processing ${batchState.current} / ${batchState.total} [${(progress * 100).toInt()}%]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Slate400
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Button(
                            onClick = { viewModel.startBatchCleaning() },
                            enabled = !batchState.isRunning,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("start_batch_clean_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanAccent,
                                contentColor = Slate950
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = "Clean All",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (batchState.isRunning) "Sanitizing Batch..." else "Clean All $totalFiles Files",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // POST-PROCESSING VERIFICATION BREAKDOWN (Section 9)
            if (batchState.completedResults.isNotEmpty() && !batchState.isRunning) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldSuccess.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "${batchState.completedResults.size} PROCESSED",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = EmeraldSuccess,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BatchStatPill("${batchState.verifiedCleanCount}", "Verified Clean", EmeraldSuccess, Modifier.weight(1f))
                                BatchStatPill("${batchState.partialCleanCount}", "Partially Cleaned", AmberWarning, Modifier.weight(1f))
                                BatchStatPill("${batchState.failedCount}", "Failed", RedDanger, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // INDIVIDUAL FILE INSPECTIONS
            item {
                Text(
                    text = "INDIVIDUAL FILE INSPECTIONS (${batchInspections.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(batchInspections) { item ->
                BatchInspectionRow(
                    item = item,
                    onInspectSingle = { viewModel.selectImage(item.uri) }
                )
            }

            // RESULTS LIST
            if (batchState.completedResults.isNotEmpty()) {
                item {
                    Text(
                        text = "SANITIZED & VERIFIED FILES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                items(batchState.completedResults) { result ->
                    BatchResultRow(
                        result = result,
                        onSave = { viewModel.saveCleanedToGallery(result.cleanedFilePath) },
                        onShare = { viewModel.shareImage(result.cleanedUri, result.cleanedFileName) },
                        onViewReport = {
                            result.privacyReport?.let { viewModel.showPrivacyReport(it) }
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    activeReport?.let { report ->
        PrivacyReportDialog(
            report = report,
            onDismiss = { viewModel.dismissPrivacyReport() },
            onShare = { viewModel.sharePrivacyReport(report) },
            onCopyFeedback = { viewModel.showToast(it) }
        )
    }
}

@Composable
private fun BatchStatPill(
    count: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BatchInspectionRow(
    item: BatchItemInspection,
    onInspectSingle: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val inspection = item.inspection

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = item.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = inspection?.fileName ?: "File",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    if (item.isInspecting) {
                        Text("Scanning...", fontSize = 11.sp, color = Slate400)
                    } else {
                        Text(
                            text = "${inspection?.entries?.size ?: 0} tags • ${if (inspection?.hasGpsLocation == true) "GPS ⚠ " else ""}${if (inspection?.hasAiMetadata == true) "AI Gen ⚠" else "Clean"}",
                            fontSize = 11.sp,
                            color = if (inspection?.hasGpsLocation == true || inspection?.hasAiMetadata == true) AmberWarning else EmeraldSuccess
                        )
                    }
                }

                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Slate400
                    )
                }
            }

            AnimatedVisibility(visible = expanded && inspection != null) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = Slate800)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onInspectSingle,
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text("Open in Main Inspector", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchResultRow(
    result: CleanExecutionResult,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onViewReport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (result.isVerifiedClean) Icons.Default.VerifiedUser else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (result.isVerifiedClean) EmeraldSuccess else AmberWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = result.originalFileName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                Text(
                    text = if (result.isVerifiedClean) "VERIFIED CLEAN" else "UNVERIFIED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (result.isVerifiedClean) EmeraldSuccess else AmberWarning
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onViewReport,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Report", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Share", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = onSave,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Slate950),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
