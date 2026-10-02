package com.example.aimetadatacleaner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.aimetadatacleaner.data.model.BeforeAfterItem
import com.example.aimetadatacleaner.data.model.CleanExecutionResult
import com.example.aimetadatacleaner.data.model.RemovalStatus
import com.example.aimetadatacleaner.ui.components.HardwareProofDialog
import com.example.aimetadatacleaner.ui.theme.AmberWarning
import com.example.aimetadatacleaner.ui.theme.CyanAccent
import com.example.aimetadatacleaner.ui.theme.EmeraldSuccess
import com.example.aimetadatacleaner.ui.theme.IndigoLight
import com.example.aimetadatacleaner.ui.theme.RedDanger
import com.example.aimetadatacleaner.ui.theme.Slate400
import com.example.aimetadatacleaner.ui.theme.Slate800
import com.example.aimetadatacleaner.ui.theme.Slate950

@Composable
fun CleanResultDialog(
    result: CleanExecutionResult,
    onDismiss: () -> Unit,
    onSaveToGallery: (String?) -> Unit,
    onShare: () -> Unit,
    onViewReport: () -> Unit = {}
) {
    var showRemainingData by remember { mutableStateOf(false) }
    var showBeforeAfter by remember { mutableStateOf(true) }
    var showProofDialog by remember { mutableStateOf(false) }

    val verification = result.verificationReport
    val isVerified = result.isVerifiedClean && (verification?.isVerifiedClean == true)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.dp,
                    if (isVerified) EmeraldSuccess.copy(alpha = 0.6f) else AmberWarning.copy(alpha = 0.6f),
                    RoundedCornerShape(24.dp)
                ),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (isVerified) EmeraldSuccess.copy(alpha = 0.15f)
                            else AmberWarning.copy(alpha = 0.15f)
                        )
                        .border(
                            1.5.dp,
                            if (isVerified) EmeraldSuccess else AmberWarning,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isVerified) Icons.Default.VerifiedUser else Icons.Default.Warning,
                        contentDescription = "Verification Result",
                        tint = if (isVerified) EmeraldSuccess else AmberWarning,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isVerified) "VERIFIED CLEAN" else "VERIFICATION INCOMPLETE",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp,
                    color = if (isVerified) EmeraldSuccess else AmberWarning,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = if (isVerified)
                        "Independent secondary scan confirmed 100% clean."
                    else
                        "${verification?.remainingFieldsCount ?: 0} metadata fields remain.",
                    fontSize = 13.sp,
                    color = Slate400,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Verification Checklist Card (Section 8)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate800))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "INDEPENDENT SCAN VERIFICATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        verification?.checkedCategories?.forEach { check ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (check.passed) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (check.passed) EmeraldSuccess else RedDanger,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = check.category,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = if (check.passed) "Clean ✓" else "Detected ⚠",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (check.passed) EmeraldSuccess else RedDanger
                                )
                            }
                        }

                        if (!isVerified && (verification?.remainingFields?.isNotEmpty() == true)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (showRemainingData) "Hide Remaining Data" else "View Remaining Data (${verification.remainingFields.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning,
                                modifier = Modifier
                                    .clickable { showRemainingData = !showRemainingData }
                                    .padding(vertical = 4.dp)
                            )
                            AnimatedVisibility(visible = showRemainingData) {
                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                    verification.remainingFields.forEach { field ->
                                        Text(
                                            text = "• $field",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hardware-Bound Cryptographic Proof Card
                result.cryptographicProof?.let { proof ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showProofDialog = true }
                            .testTag("card_crypto_proof_badge"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (proof.isHardwareBacked) EmeraldSuccess.copy(alpha = 0.12f) else CyanAccent.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (proof.isHardwareBacked) EmeraldSuccess.copy(alpha = 0.4f) else CyanAccent.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (proof.isHardwareBacked) EmeraldSuccess.copy(alpha = 0.2f) else CyanAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (proof.isHardwareBacked) EmeraldSuccess else CyanAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Hardware-Bound Proof",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(if (proof.isHardwareBacked) EmeraldSuccess.copy(alpha = 0.2f) else CyanAccent.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (proof.isHardwareBacked) "TEE SEALED" else "ECDSA",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (proof.isHardwareBacked) EmeraldSuccess else CyanAccent
                                        )
                                    }
                                }
                                Text(
                                    text = "Signed by device KeyStore (${proof.proofId}) • Tap to inspect",
                                    fontSize = 11.sp,
                                    color = Slate400
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "View Certificate",
                                tint = if (proof.isHardwareBacked) EmeraldSuccess else CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // BEFORE / AFTER COMPARISON SECTION (Section 7)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate800))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showBeforeAfter = !showBeforeAfter },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BEFORE / AFTER COMPARISON",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.8.sp
                            )
                            Icon(
                                imageVector = if (showBeforeAfter) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(visible = showBeforeAfter) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Row(
                                    modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("METADATA FIELD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400)
                                    Text("STATUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400)
                                }
                                HorizontalDivider(color = Slate800)
                                Spacer(modifier = Modifier.height(6.dp))

                                result.beforeAfterSummary.forEach { item ->
                                    BeforeAfterRow(item = item)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Save, Share, Report
                Button(
                    onClick = { onSaveToGallery(result.cleanedFilePath) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_gallery_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = Slate950
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Save to Gallery",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Clean File", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_clean_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = IndigoLight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onViewReport,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("view_privacy_report_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Report",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Report", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_result_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }

    if (showProofDialog && result.cryptographicProof != null) {
        HardwareProofDialog(
            proof = result.cryptographicProof,
            onDismiss = { showProofDialog = false }
        )
    }
}

@Composable
private fun BeforeAfterRow(item: BeforeAfterItem) {
    val (statusColor, statusText) = when (item.afterStatus) {
        RemovalStatus.REMOVED -> EmeraldSuccess to "Removed ✓"
        RemovalStatus.PRESERVED -> AmberWarning to "Preserved ⚠"
        RemovalStatus.NOT_PRESENT -> Slate400 to "Not present —"
        RemovalStatus.UNABLE_TO_VERIFY -> Slate400 to "Unable to verify ?"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.fieldName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!item.beforeValue.isNullOrBlank()) {
                Text(
                    text = item.beforeValue.take(35),
                    fontSize = 10.sp,
                    color = Slate400,
                    maxLines = 1
                )
            }
        }
        Text(
            text = statusText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )
    }
}
