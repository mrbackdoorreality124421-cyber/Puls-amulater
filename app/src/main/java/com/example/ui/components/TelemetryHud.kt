package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.RuntimeTelemetry
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPurple

@Composable
fun TelemetryHud(
    telemetry: RuntimeTelemetry,
    modifier: Modifier = Modifier
) {
    val fpsColor = when {
        telemetry.fps >= 55f -> NeonEmerald
        telemetry.fps >= 40f -> NeonCyan
        telemetry.fps >= 28f -> NeonAmber
        else -> Color(0xFFFF1744)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xCC0B0F19))
            .border(1.dp, Color(0x6600E5FF), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // FPS Counter
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(fpsColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${telemetry.fps.toInt()} FPS",
                    color = fpsColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Frame time
            Text(
                text = "${telemetry.frameTimeMs}ms",
                color = Color(0xFFE2E8F0),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            // CPU & GPU
            Text(
                text = "CPU ${telemetry.cpuUsagePercent}%",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "GPU ${telemetry.gpuUsagePercent}%",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            // Special Flags (Frame Gen / Async)
            if (telemetry.frameGenActive) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(NeonPurple.copy(alpha = 0.2f))
                        .border(1.dp, NeonPurple.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "AFMF 2.0",
                        color = NeonPurple,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (telemetry.asyncComputeActive) {
                Text(
                    text = "ASYNC",
                    color = NeonEmerald,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // RAM
            Text(
                text = "RAM ${String.format("%.1f", telemetry.ramUsedMb / 1024f)}G",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            // Temp
            val tempColor = if (telemetry.temperatureCelsius >= 43) NeonOrange else Color(0xFF94A3B8)
            Text(
                text = "${telemetry.temperatureCelsius}°C",
                color = tempColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            // Profile
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(telemetry.activeProfile.accentColorHex).copy(alpha = 0.2f))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = telemetry.activeProfile.title.uppercase(),
                    color = Color(telemetry.activeProfile.accentColorHex),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
