package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ComponentDownloadItem
import com.example.data.model.ComponentScope
import com.example.data.model.DeviceCapability
import com.example.data.model.DownloadStatus
import com.example.viewmodel.MainViewModel

@Composable
fun GamingEnvironmentSetupScreen(
    viewModel: MainViewModel,
    onSetupComplete: () -> Unit,
    onContinueInBackground: () -> Unit,
    modifier: Modifier = Modifier
) {
    val device by viewModel.deviceCapability.collectAsState()
    val envState by viewModel.gamingEnvironmentState.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0D1117)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = "Environment",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Gaming Environment",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Preparing your device for PC games",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF90CAF9)
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Device Analysis & Architecture Detection
                item {
                    DeviceAnalysisCard(device = device)
                }

                // Section 1.5: Component Error Alert Card
                envState.failedComponentError?.let { err ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1214)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Download Error",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Download Error",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF5252)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Component: ${err.componentName}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Reason: ${err.reason}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFFFCDD2)
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.retrySingleComponent(err.componentId, onSetupComplete) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFF5252),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("RETRY", fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            if (err.isOptional) {
                                                viewModel.cancelSingleComponent(err.componentId)
                                            } else {
                                                viewModel.dismissDownloadError()
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = if (err.isOptional) "SKIP OPTIONAL" else "CANCEL",
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Progress overview (if downloading or ready)
                if (envState.isDownloadingInProgress || envState.isPaused || envState.isEnvironmentReady) {
                    item {
                        DownloadProgressCard(
                            state = envState,
                            pulseAlpha = pulseAlpha,
                            onPause = { viewModel.pauseDownloads() },
                            onResume = { viewModel.resumeDownloads(onSetupComplete) },
                            onRetry = { viewModel.retryFailedDownloads(onSetupComplete) }
                        )
                    }
                }

                // Section 3: Required Gaming Components List
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 2.dp)
                    ) {
                        Text(
                            text = "REQUIRED GAMING COMPONENTS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB0BEC5),
                                letterSpacing = 1.2.sp
                            )
                        )

                        val totalSizeMb = envState.totalBytesToDownload / (1024 * 1024)
                        Text(
                            text = "Total: $totalSizeMb MB",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF00E5FF)
                            )
                        )
                    }
                }

                items(envState.activeDownloads, key = { it.component.id }) { item ->
                    ComponentRowItem(item = item)
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Bottom Actions Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (envState.isEnvironmentReady) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Ready",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gaming Environment Ready — All components installed",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676)
                                )
                            )
                        }

                        Button(
                            onClick = onSetupComplete,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("continue_to_library_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E676),
                                contentColor = Color.Black
                            )
                        ) {
                            Text(
                                text = "CONTINUE TO LIBRARY",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    } else if (envState.isDownloadingInProgress) {
                        Text(
                            text = "You can continue using the app while setup completes in the background.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF8B949E),
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onContinueInBackground,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("continue_in_bg_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("RUN IN BACKGROUND", color = Color(0xFF90CAF9), fontSize = 13.sp)
                            }

                            FilledTonalButton(
                                onClick = { viewModel.pauseDownloads() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pause_setup_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF21262D),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PAUSE", fontSize = 13.sp)
                            }
                        }
                    } else if (envState.isPaused) {
                        Button(
                            onClick = { viewModel.resumeDownloads(onSetupComplete) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("resume_setup_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2979FF),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RESUME DOWNLOADS", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Not started yet
                        Text(
                            text = "Downloads are verified for SHA-256 integrity and installed to isolated sandboxes.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF8B949E),
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.startFirstLaunchSetup(onSetupComplete)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("download_and_setup_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E5FF),
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Download",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "DOWNLOAD & SET UP",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.1.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceAnalysisCard(device: DeviceCapability) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Analysis complete",
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Device & Architecture Analysis Complete",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            // Specs grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpecBadge(
                    label = "ARCH",
                    value = if (device.is64BitSupported) "ARM64-v8a" else "ARM32",
                    color = Color(0xFF00E5FF),
                    modifier = Modifier.weight(1f)
                )
                SpecBadge(
                    label = "SOC / CPU",
                    value = "${device.cpuCores} Cores @ ${device.cpuMaxFreqGhz}GHz",
                    color = Color(0xFF7C4DFF),
                    modifier = Modifier.weight(1.3f)
                )
                SpecBadge(
                    label = "RAM",
                    value = "${device.totalRamGb.toInt()} GB",
                    color = Color(0xFFFF9100),
                    modifier = Modifier.weight(0.8f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpecBadge(
                    label = "GPU & DRIVER",
                    value = if (device.gpuRenderer.contains("Adreno", ignoreCase = true)) "Qualcomm Adreno (Turnip)" else device.gpuRenderer.take(24),
                    color = Color(0xFF2979FF),
                    modifier = Modifier.weight(1.5f)
                )
                SpecBadge(
                    label = "VULKAN",
                    value = if (device.isVulkanSupported) device.vulkanVersion else "OpenGL ES ${device.openGlesVersion}",
                    color = if (device.isVulkanSupported) Color(0xFF00E676) else Color(0xFFFF5252),
                    modifier = Modifier.weight(1.2f)
                )
                SpecBadge(
                    label = "STORAGE FREE",
                    value = "${device.freeStorageGb.toInt()} GB",
                    color = Color(0xFFE040FB),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SpecBadge(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0D1117),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE6EDF3)
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DownloadProgressCard(
    state: com.example.data.model.GamingEnvironmentState,
    pulseAlpha: Float,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = pulseAlpha))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (state.isEnvironmentReady) "Environment Ready" else if (state.isPaused) "Download Paused" else "Downloading & Configuring…",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    state.currentStepText?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF90CAF9),
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Text(
                    text = "${(state.overallProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E5FF),
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = state.overallProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF00E5FF),
                trackColor = Color(0xFF21262D)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val dlMb = state.totalBytesDownloaded / (1024 * 1024)
                val totMb = state.totalBytesToDownload / (1024 * 1024)
                Text(
                    text = "$dlMb MB / $totMb MB",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF8B949E),
                        fontFamily = FontFamily.Monospace
                    )
                )

                val speedMb = state.overallSpeedBytesPerSec / (1024.0 * 1024.0)
                if (speedMb > 0 && state.isDownloadingInProgress) {
                    Text(
                        text = String.format("%.1f MB/s • ETA %ds", speedMb, state.overallEtaSeconds),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF00E676),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ComponentRowItem(item: ComponentDownloadItem) {
    val comp = item.component
    val isInstalled = item.status == DownloadStatus.INSTALLED
    val isDownloading = item.status == DownloadStatus.DOWNLOADING
    val isVerifying = item.status == DownloadStatus.VERIFYING || item.status == DownloadStatus.INSTALLING
    val isFailed = item.status == DownloadStatus.FAILED

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF161B22),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDownloading) Color(0xFF00E5FF).copy(alpha = 0.6f)
            else if (isInstalled) Color(0xFF00E676).copy(alpha = 0.4f)
            else Color(0xFF30363D)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Status Icon
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isInstalled -> Color(0xFF00E676).copy(alpha = 0.15f)
                                    isDownloading -> Color(0xFF00E5FF).copy(alpha = 0.15f)
                                    isFailed -> Color(0xFFFF1744).copy(alpha = 0.15f)
                                    else -> Color(0xFF21262D)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isInstalled -> Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Installed",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(18.dp)
                            )
                            isDownloading -> CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF00E5FF),
                                strokeWidth = 2.dp
                            )
                            isVerifying -> CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFFFFD600),
                                strokeWidth = 2.dp
                            )
                            isFailed -> Icon(
                                Icons.Default.Warning,
                                contentDescription = "Failed",
                                tint = Color(0xFFFF1744),
                                modifier = Modifier.size(16.dp)
                            )
                            else -> Text(
                                text = "○",
                                color = Color(0xFF8B949E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = comp.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isInstalled) Color(0xFFE6EDF3) else Color.White
                                )
                            )
                            if (comp.scope == ComponentScope.OPTIONAL) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF21262D)
                                ) {
                                    Text(
                                        text = "OPTIONAL",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            color = Color(0xFF8B949E)
                                        )
                                    )
                                }
                            }
                        }

                        Text(
                            text = comp.category.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF8B949E),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Size & status text
                Column(horizontalAlignment = Alignment.End) {
                    val downloadedMb = item.bytesDownloaded / (1024 * 1024)
                    val totalMb = comp.sizeBytes / (1024 * 1024)

                    Text(
                        text = if (isInstalled) comp.formattedSize else "$downloadedMb MB / $totalMb MB",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isInstalled) Color(0xFF00E676) else Color(0xFF90CAF9),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        text = when (item.status) {
                            DownloadStatus.INSTALLED -> "Ready"
                            DownloadStatus.DOWNLOADING -> "${(item.progress * 100).toInt()}%"
                            DownloadStatus.VERIFYING -> "Verifying"
                            DownloadStatus.INSTALLING -> "Configuring"
                            DownloadStatus.PAUSED -> "Paused"
                            DownloadStatus.FAILED -> "Failed"
                            DownloadStatus.NOT_INSTALLED -> "Pending"
                            DownloadStatus.QUEUED -> "Queued"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = when (item.status) {
                                DownloadStatus.INSTALLED -> Color(0xFF00E676)
                                DownloadStatus.DOWNLOADING -> Color(0xFF00E5FF)
                                DownloadStatus.FAILED -> Color(0xFFFF1744)
                                else -> Color(0xFF8B949E)
                            }
                        )
                    )
                }
            }

            // Live item progress bar if downloading or verifying
            if (isDownloading || isVerifying) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = item.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF00E5FF),
                    trackColor = Color(0xFF21262D)
                )
            }
        }
    }
}
