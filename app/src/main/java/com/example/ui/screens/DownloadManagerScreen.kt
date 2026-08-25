package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ComponentDownloadItem
import com.example.data.model.DownloadStatus
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadManagerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val envState by viewModel.gamingEnvironmentState.collectAsState()
    var showReinstallDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0D1117),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Downloads & Runtime",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Gaming Environment Manager",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF8B949E),
                                fontSize = 11.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("download_manager_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshDeviceAnalysis() },
                        modifier = Modifier.testTag("refresh_runtime_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color(0xFF00E5FF)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF161B22)
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Error Alert (if present)
            envState.failedComponentError?.let { err ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                    onClick = { viewModel.retrySingleComponent(err.componentId) },
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

            // Overall Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Gaming Environment",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = when {
                                        envState.isDownloadingInProgress -> "Downloading: ${envState.activeComponentName ?: "Components"}"
                                        envState.isPaused -> "Paused"
                                        envState.isEnvironmentReady -> "All Core Components Ready (v${envState.latestEnvironmentVersion})"
                                        else -> "Pending Download"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = when {
                                            envState.isEnvironmentReady -> Color(0xFF00E676)
                                            envState.isDownloadingInProgress -> Color(0xFF00E5FF)
                                            else -> Color(0xFF8B949E)
                                        },
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            Text(
                                text = "${(envState.overallProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF00E5FF),
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = envState.overallProgress,
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
                            val dlMb = envState.totalBytesDownloaded / (1024 * 1024)
                            val totMb = envState.totalBytesToDownload / (1024 * 1024)
                            Text(
                                text = "$dlMb MB / $totMb MB",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF8B949E),
                                    fontFamily = FontFamily.Monospace
                                )
                            )

                            val speedMb = envState.overallSpeedBytesPerSec / (1024.0 * 1024.0)
                            if (speedMb > 0 && envState.isDownloadingInProgress) {
                                Text(
                                    text = String.format("%.1f MB/s • ETA %ds", speedMb, envState.overallEtaSeconds),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF00E676),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Controls row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (envState.isDownloadingInProgress) {
                                FilledTonalButton(
                                    onClick = { viewModel.pauseDownloads() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color(0xFF21262D),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PAUSE")
                                }
                            } else if (envState.isPaused || (!envState.isEnvironmentReady && envState.activeDownloads.isNotEmpty())) {
                                Button(
                                    onClick = { viewModel.resumeDownloads() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00E5FF),
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("RESUME", fontWeight = FontWeight.Bold)
                                }
                            }

                            if (envState.activeDownloads.any { it.status == DownloadStatus.FAILED }) {
                                OutlinedButton(
                                    onClick = { viewModel.retryFailedDownloads() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("RETRY FAILED", fontSize = 12.sp)
                                }
                            }

                            if (envState.isEnvironmentReady) {
                                OutlinedButton(
                                    onClick = { showReinstallDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reinstall", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("REINSTALL", fontSize = 12.sp, color = Color(0xFF90CAF9))
                                }
                            }
                        }
                    }
                }
            }

            // Section: Downloading Items
            val downloadingItems = envState.activeDownloads.filter {
                it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.VERIFYING || it.status == DownloadStatus.INSTALLING
            }
            if (downloadingItems.isNotEmpty()) {
                item {
                    Text(
                        text = "DOWNLOADING (↓)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            letterSpacing = 1.2.sp
                        )
                    )
                }
                items(downloadingItems, key = { it.component.id }) { item ->
                    DownloadManagerItemRow(item = item)
                }
            }

            // Section: Waiting / Queued Items
            val queuedItems = envState.activeDownloads.filter {
                it.status == DownloadStatus.NOT_INSTALLED || it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.PAUSED
            }
            if (queuedItems.isNotEmpty()) {
                item {
                    Text(
                        text = "WAITING IN QUEUE (○)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB0BEC5),
                            letterSpacing = 1.2.sp
                        )
                    )
                }
                items(queuedItems, key = { it.component.id }) { item ->
                    DownloadManagerItemRow(item = item)
                }
            }

            // Section: Completed Items
            val completedItems = envState.activeDownloads.filter {
                it.status == DownloadStatus.INSTALLED || envState.installedComponentIds.contains(it.component.id)
            }
            if (completedItems.isNotEmpty()) {
                item {
                    Text(
                        text = "COMPLETED (✓)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676),
                            letterSpacing = 1.2.sp
                        )
                    )
                }
                items(completedItems, key = { it.component.id }) { item ->
                    DownloadManagerItemRow(item = item)
                }
            }

            // Storage & Sandbox Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Storage",
                                tint = Color(0xFFE040FB),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Isolated Runtime Structure",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "App/Runtime/\n  ├── Wine/ (wine-9.2 prefix & server)\n  ├── Box64/ (x86_64 dynarec binaries)\n  ├── DXVK/ & VKD3D/ (Vulkan SPIR-V translations)\n  ├── Graphics/ (Turnip / Mesa ICD layers)\n  └── Input/ (Virtual touch controller bridge)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF8B949E),
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }
        }
    }

    if (showReinstallDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showReinstallDialog = false },
            title = { Text("Reinstall Gaming Environment?") },
            text = {
                Text("This will re-download and re-configure all core runtime binaries, DXVK configs, and translation layers. Existing games will not be deleted.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReinstallDialog = false
                        viewModel.reinstallEnvironment()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))
                ) {
                    Text("REINSTALL")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showReinstallDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
private fun DownloadManagerItemRow(item: ComponentDownloadItem) {
    val comp = item.component
    val isInstalled = item.status == DownloadStatus.INSTALLED
    val isDownloading = item.status == DownloadStatus.DOWNLOADING
    val isFailed = item.status == DownloadStatus.FAILED

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF161B22),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDownloading) Color(0xFF00E5FF).copy(alpha = 0.5f)
            else if (isInstalled) Color(0xFF00E676).copy(alpha = 0.3f)
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
                        Text(
                            text = comp.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isInstalled) Color(0xFFE6EDF3) else Color.White
                            )
                        )
                        Text(
                            text = "v${comp.version} • ${comp.targetSubdir}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF8B949E),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = comp.formattedSize,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isInstalled) Color(0xFF00E676) else Color(0xFF90CAF9),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    if (isDownloading) {
                        val speedMb = item.speedBytesPerSec / (1024.0 * 1024.0)
                        Text(
                            text = String.format("%.1f MB/s", speedMb),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF00E5FF),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }

            if (isDownloading) {
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
