package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple

@Composable
fun StorageBar(
    totalBytes: Long,
    freeBytes: Long,
    gamesBytes: Long,
    modifier: Modifier = Modifier
) {
    val totalGb = (totalBytes / (1024.0 * 1024.0 * 1024.0)).coerceAtLeast(1.0)
    val freeGb = freeBytes / (1024.0 * 1024.0 * 1024.0)
    val gamesGb = gamesBytes / (1024.0 * 1024.0 * 1024.0)

    val gamesFraction = (gamesGb / totalGb).toFloat().coerceIn(0.01f, 0.95f)
    val otherUsedFraction = (((totalGb - freeGb - gamesGb) / totalGb)).toFloat().coerceIn(0.0f, 0.9f)
    val freeFraction = (freeGb / totalGb).toFloat().coerceIn(0.01f, 1.0f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF131B2A))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = "Storage",
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Internal Storage",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "${String.format("%.1f", freeGb)} GB Available of ${String.format("%.0f", totalGb)} GB",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress bar split into 3 segments
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1E293B))
        ) {
            if (gamesFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(gamesFraction)
                        .height(8.dp)
                        .background(NeonCyan)
                )
            }
            if (otherUsedFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(otherUsedFraction / (otherUsedFraction + freeFraction))
                        .height(8.dp)
                        .background(NeonPurple.copy(alpha = 0.7f))
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NeonCyan))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "PC Games (${String.format("%.1f", gamesGb)} GB)", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NeonPurple.copy(alpha = 0.7f)))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "System / Apps", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF475569)))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Free Space", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
        }
    }
}
