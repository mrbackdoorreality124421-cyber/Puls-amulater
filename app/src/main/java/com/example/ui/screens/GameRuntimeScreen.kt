package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.PerformanceProfile
import com.example.engine.RuntimeState
import com.example.ui.components.TelemetryHud
import com.example.ui.components.TouchControlOverlay
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPurple
import com.example.viewmodel.GamePlayerViewModel
import kotlin.math.sin

@Composable
fun GameRuntimeScreen(
    gameId: String,
    onExitGame: () -> Unit,
    modifier: Modifier = Modifier,
    playerViewModel: GamePlayerViewModel = viewModel()
) {
    val runtimeState by playerViewModel.runtimeState.collectAsState()
    val telemetry by playerViewModel.telemetry.collectAsState()
    val currentGame by playerViewModel.currentGame.collectAsState()
    val controlLayout by playerViewModel.controlLayout.collectAsState()
    val showHud by playerViewModel.showPerformanceHud.collectAsState()
    val isPaused by playerViewModel.isPaused.collectAsState()
    val thermalEvent by playerViewModel.thermalState.collectAsState()

    var carPositionX by remember { mutableFloatStateOf(0.5f) }
    var speedMultiplier by remember { mutableFloatStateOf(1.0f) }

    LaunchedEffect(gameId) {
        playerViewModel.initializeGameSession(gameId)
    }

    DisposableEffect(Unit) {
        onDispose {
            playerViewModel.terminateSession()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "game_render")
    // Map FPS to animation speed multiplier for crazy performance visual feel
    val fpsSpeedFactor = if (telemetry.fps > 90f) 2.5f else if (telemetry.fps > 60f) 1.5f else 1.0f
    val roadOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((700 / (speedMultiplier * fpsSpeedFactor).coerceAtLeast(0.5f)).toInt(), easing = LinearEasing)
        ),
        label = "road_anim"
    )

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Real interactive 3D/2D Game Visual Simulation Viewport
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Cyber Horizon gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF030712), Color(0xFF0B1426), Color(0xFF1E1B4B)),
                    startY = 0f,
                    endY = h * 0.55f
                ),
                topLeft = Offset.Zero,
                size = androidx.compose.ui.geometry.Size(w, h * 0.55f)
            )

            // Neon Grid Floor
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E1B4B), Color(0xFF0B0F19)),
                    startY = h * 0.55f,
                    endY = h
                ),
                topLeft = Offset(0f, h * 0.55f),
                size = androidx.compose.ui.geometry.Size(w, h * 0.45f)
            )

            // Dynamic Road Perspective Lines
            val horizonY = h * 0.55f
            val roadTopW = w * 0.12f
            val roadBotW = w * 0.85f
            val roadLeftTop = (w - roadTopW) / 2
            val roadRightTop = roadLeftTop + roadTopW
            val roadLeftBot = (w - roadBotW) / 2
            val roadRightBot = roadLeftBot + roadBotW

            val roadPath = Path().apply {
                moveTo(roadLeftTop, horizonY)
                lineTo(roadRightTop, horizonY)
                lineTo(roadRightBot, h)
                lineTo(roadLeftBot, h)
                close()
            }
            drawPath(
                path = roadPath,
                color = Color(0xFF0F172A)
            )

            // Animated grid lines (Perspective speed illusion)
            for (i in 0..8) {
                val progress = (roadOffset + i / 8f) % 1f
                val lineY = horizonY + (progress * progress) * (h - horizonY)
                val currentWidth = roadTopW + (progress * (roadBotW - roadTopW))
                val lineLeft = (w - currentWidth) / 2
                drawLine(
                    color = NeonCyan.copy(alpha = (progress * 0.8f)),
                    start = Offset(lineLeft, lineY),
                    end = Offset(lineLeft + currentWidth, lineY),
                    strokeWidth = 2f + (progress * 3f)
                )
            }

            // Interactive vehicle / player entity
            val carX = roadLeftBot + (carPositionX * (roadRightBot - roadLeftBot - 80.dp.toPx()))
            val carY = h - 110.dp.toPx()

            // Exhaust Boost Effect
            if (speedMultiplier > 1.0f) {
                val particleCount = if (speedMultiplier > 2.0f) 8 else 4
                for (i in 0..particleCount) {
                    val boostOffset = (roadOffset * 10f + (i * 0.5f)) % 1f
                    val sizeFactor = if (speedMultiplier > 2.0f) 1.5f else 1.0f
                    
                    drawCircle(
                        color = NeonCyan.copy(alpha = 1f - boostOffset),
                        radius = (12.dp.toPx() * sizeFactor) * (1f - boostOffset),
                        center = Offset(carX + 15.dp.toPx(), carY + 45.dp.toPx() + (boostOffset * 100.dp.toPx()))
                    )
                    drawCircle(
                        color = NeonCyan.copy(alpha = 1f - boostOffset),
                        radius = (12.dp.toPx() * sizeFactor) * (1f - boostOffset),
                        center = Offset(carX + 55.dp.toPx(), carY + 45.dp.toPx() + (boostOffset * 100.dp.toPx()))
                    )
                }
            }

            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(NeonPurple, NeonCyan)),
                topLeft = Offset(carX, carY),
                size = androidx.compose.ui.geometry.Size(70.dp.toPx(), 45.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())
            )
        }

        // Live Touch Controls Overlay
        TouchControlOverlay(
            layout = controlLayout,
            onInputTriggered = { key, isDown ->
                val movementSpeed = if (telemetry.fps > 90f) 0.15f else 0.08f // Super snappy controls at high FPS
                if (key.contains("A") || key.contains("LEFT")) {
                    if (isDown) carPositionX = (carPositionX - movementSpeed).coerceAtLeast(0.1f)
                }
                if (key.contains("D") || key.contains("RIGHT")) {
                    if (isDown) carPositionX = (carPositionX + movementSpeed).coerceAtMost(0.9f)
                }
                if (key.contains("W") || key.contains("SPACE") || key.contains("LCLICK")) {
                    val targetBoost = if (telemetry.frameGenActive) 3.5f else 1.8f
                    speedMultiplier = if (isDown) targetBoost else 1.0f
                }
                if (key.contains("S") || key.contains("BRAKE")) {
                    speedMultiplier = if (isDown) 0.4f else 1.0f
                }
                if (key.contains("ESC") && isDown) {
                    playerViewModel.pauseGame()
                }
                playerViewModel.handleInputTrigger(key, isDown)
            }
        )

        // Live Telemetry HUD Overlay (Top Right / Top Center)
        if (showHud) {
            TelemetryHud(
                telemetry = telemetry,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .testTag("live_telemetry_hud")
            )
        }

        // Top-Left In-Game Pause Trigger
        IconButton(
            onClick = { playerViewModel.pauseGame() },
            modifier = Modifier
                .padding(12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x88131B2A))
                .align(Alignment.TopStart)
                .testTag("in_game_pause_button")
        ) {
            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.White, modifier = Modifier.size(20.dp))
        }

        // Thermal Notification Callout (if severe)
        if (thermalEvent.requiresThrottle) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 70.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonOrange.copy(alpha = 0.9f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Thermal Warning: ${thermalEvent.statusText}. Auto-adjusted profile.",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Initializing Overlay
        if (runtimeState is RuntimeState.Initializing) {
            val step = (runtimeState as RuntimeState.Initializing).step
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE60B0F19)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Initializing PC Game Runtime…",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = step, color = NeonCyan, fontSize = 13.sp)
                }
            }
        }

        // IN-GAME PAUSE MENU MODAL
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000))
                    .clickable { playerViewModel.resumeGame() },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(360.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, CyberCardBorder, RoundedCornerShape(16.dp))
                        .clickable(enabled = false) {},
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GAME PAUSED",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            IconButton(onClick = { playerViewModel.resumeGame() }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Profile Switcher
                        Text("ACTIVE PROFILE:", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(PerformanceProfile.SUPER_SMOOTH, PerformanceProfile.SMOOTH, PerformanceProfile.BALANCE, PerformanceProfile.HIGH, PerformanceProfile.ULTRA).forEach { prof ->
                                val isSelected = telemetry.activeProfile == prof
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color(prof.accentColorHex) else Color(0xFF1E293B))
                                        .clickable { playerViewModel.switchActiveProfile(prof) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = prof.title.split(" ").first(),
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // HUD toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Show Performance HUD", color = Color.White, fontSize = 13.sp)
                            Switch(
                                checked = showHud,
                                onCheckedChange = { playerViewModel.togglePerformanceHud() },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.4f))
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { playerViewModel.resumeGame() },
                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("resume_game_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF003038)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("RESUME GAME", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                playerViewModel.terminateSession()
                                onExitGame()
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("exit_game_button"),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF1744).copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("EXIT TO LIBRARY", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // CRASH RECOVERY DIALOG
        if (runtimeState is RuntimeState.Crashed) {
            val report = (runtimeState as RuntimeState.Crashed).report
            AlertDialog(
                onDismissRequest = { },
                containerColor = CyberSurface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Crash", tint = Color(0xFFFF1744))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Game Execution Fault", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Text(text = "Reason: ${report.errorReason}", color = Color(0xFFFF8A80), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = report.suggestedFix, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { playerViewModel.launchSafeMode() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonAmber, contentColor = Color.Black),
                        modifier = Modifier.testTag("crash_safe_mode_button")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = "Safe mode", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LAUNCH IN SAFE MODE", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            playerViewModel.terminateSession()
                            onExitGame()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                    ) {
                        Text("EXIT")
                    }
                }
            )
        }
    }
}
