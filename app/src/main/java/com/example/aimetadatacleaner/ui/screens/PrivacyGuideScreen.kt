package com.example.aimetadatacleaner.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aimetadatacleaner.util.FormatSupportLevel
import com.example.aimetadatacleaner.ui.theme.AmberWarning
import com.example.aimetadatacleaner.ui.theme.CyanAccent
import com.example.aimetadatacleaner.ui.theme.EmeraldSuccess
import com.example.aimetadatacleaner.ui.theme.IndigoLight
import com.example.aimetadatacleaner.ui.theme.RedDanger
import com.example.aimetadatacleaner.ui.theme.Slate400
import com.example.aimetadatacleaner.ui.theme.Slate800

@Composable
fun PrivacyGuideScreen(
    modifier: Modifier = Modifier
) {
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
                                .background(CyanAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Guide",
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Privacy & Technical Guide",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Supported formats, risk categories & AI principles",
                                fontSize = 12.sp,
                                color = Slate400
                            )
                        }
                    }
                }
            }
        }

        // 1. SUPPORTED FILE TYPES MATRIX (Section 10)
        item {
            Text(
                text = "SUPPORTED FILE FORMATS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 2.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormatCategoryRow(
                        title = "Images",
                        formats = "JPG • JPEG • PNG • WebP • HEIC • AVIF • TIFF",
                        status = FormatSupportLevel.SUPPORTED,
                        note = "Full scan, metadata purge & secondary verification.",
                        icon = Icons.Default.Image
                    )
                    HorizontalDivider(color = Slate800)
                    FormatCategoryRow(
                        title = "RAW Cameras",
                        formats = "DNG • CR2 • CR3 • NEF • ARW",
                        status = FormatSupportLevel.PARTIALLY_SUPPORTED,
                        note = "Header scanning & DNG re-encoding.",
                        icon = Icons.Default.PhonelinkLock
                    )
                    HorizontalDivider(color = Slate800)
                    FormatCategoryRow(
                        title = "Video Media",
                        formats = "MP4 • MOV • M4V • MKV • WebM",
                        status = FormatSupportLevel.SCAN_ONLY,
                        note = "Container & GPS inspection; stream remuxing in roadmap.",
                        icon = Icons.Default.VideoFile
                    )
                    HorizontalDivider(color = Slate800)
                    FormatCategoryRow(
                        title = "Documents",
                        formats = "PDF • DOCX • XLSX • PPTX",
                        status = FormatSupportLevel.SCAN_ONLY,
                        note = "Inspection of author, creator, and revision history.",
                        icon = Icons.Default.Description
                    )
                    HorizontalDivider(color = Slate800)
                    FormatCategoryRow(
                        title = "Audio Tracks",
                        formats = "MP3 • M4A • WAV",
                        status = FormatSupportLevel.SCAN_ONLY,
                        note = "ID3 & artist metadata inspection.",
                        icon = Icons.Default.AudioFile
                    )
                }
            }
        }

        // 2. PRIVACY EXPOSURE CATEGORIES (Section 19)
        item {
            Text(
                text = "METADATA EXPOSURE CLASSIFICATION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 2.dp, top = 4.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExposureExplainerItem(
                        badge = "LOW EXPOSURE",
                        badgeColor = EmeraldSuccess,
                        explanation = "No significant identifying information detected. Only harmless dimensions or standard baseline tags."
                    )
                    ExposureExplainerItem(
                        badge = "MEDIUM EXPOSURE",
                        badgeColor = AmberWarning,
                        explanation = "Metadata exists but contains limited identifiers, such as camera model, lens specifications, or original timestamps."
                    )
                    ExposureExplainerItem(
                        badge = "HIGH EXPOSURE",
                        badgeColor = RedDanger,
                        explanation = "Highly sensitive privacy data discovered: precise GPS coordinates, creator names, serial numbers, or full AI prompt recipes."
                    )
                    Text(
                        text = "* This classification is an informational indicator reflecting detected metadata, not a formal cybersecurity audit.",
                        fontSize = 11.sp,
                        color = Slate400,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // 3. IMPORTANT AI DETECTION PRINCIPLE (Section 5)
        item {
            GuideSectionCard(
                title = "AI Detection Principle",
                subtitle = "Evidence-based, strictly disclaimed attribution",
                description = "This application strictly distinguishes metadata evidence from actual image synthesis attribution. Rather than asserting 'This image was created by AI', we report 'AI generation metadata detected' with confidence classified as 'Metadata evidence only'.",
                icon = Icons.Default.AutoAwesome,
                accentColor = IndigoLight
            )
        }

        // 4. GPS & GEOTAG THREATS
        item {
            GuideSectionCard(
                title = "Precise GPS Coordinates",
                subtitle = "Exposes home, school, and work locations",
                description = "Modern phone cameras embed exact latitude, longitude, and altitude in EXIF tags. Sanitizing files before public upload prevents geo-stalking and OSINT location tracking.",
                icon = Icons.Default.LocationOff,
                accentColor = RedDanger
            )
        }

        // 5. C2PA & CONTENT CREDENTIALS
        item {
            GuideSectionCard(
                title = "C2PA & Content Credentials",
                subtitle = "Cryptographic provenance manifests",
                description = "Emerging standards (C2PA / CAI) embed cryptographic claims indicating camera hardware, editing history, and AI model assertions. Our sanitizer strips these manifests to produce clean, untracked media.",
                icon = Icons.Default.VpnKey,
                accentColor = CyanAccent
            )
        }

        // 6. ON-DEVICE LOCAL PROCESSING
        item {
            GuideSectionCard(
                title = "100% Local On-Device Processing",
                subtitle = "Zero network uploads, zero telemetry",
                description = "All metadata inspection, EXIF stripping, and pixel re-encoding occur purely inside local phone memory. Your photos never leave your device and are never sent to external servers.",
                icon = Icons.Default.Security,
                accentColor = EmeraldSuccess
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FormatCategoryRow(
    title: String,
    formats: String,
    status: FormatSupportLevel,
    note: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp).padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formats,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = note,
                    fontSize = 11.sp,
                    color = Slate400,
                    lineHeight = 15.sp
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(status.badgeColorHex).copy(alpha = 0.15f),
            border = BorderStroke(1.dp, Color(status.badgeColorHex).copy(alpha = 0.4f))
        ) {
            Text(
                text = status.label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(status.badgeColorHex),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun ExposureExplainerItem(
    badge: String,
    badgeColor: Color,
    explanation: String
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = badgeColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = badge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = explanation,
            fontSize = 12.sp,
            color = Slate400,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun GuideSectionCard(
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate800))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                fontSize = 13.sp,
                color = Slate400,
                lineHeight = 18.sp
            )
        }
    }
}
