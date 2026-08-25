package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ControlElement
import com.example.data.model.ControlLayout
import com.example.data.model.GameGenre
import com.example.engine.ControlEngine
import com.example.ui.components.TouchControlOverlay
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPurple
import com.example.viewmodel.MainViewModel

@Composable
fun ControlEditorScreen(
    gameId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allGames by viewModel.allGames.collectAsState()
    val game = allGames.firstOrNull { it.id == gameId }

    if (game == null) {
        Box(modifier = Modifier.fillMaxSize().background(CyberBackground), contentAlignment = Alignment.Center) {
            Text("Game not found", color = Color.White)
        }
        return
    }

    var currentLayout by remember(game.controlLayoutJson) {
        mutableStateOf(ControlEngine.deserializeFromJson(game.controlLayoutJson, game.genre))
    }

    var selectedElement by remember { mutableStateOf<ControlElement?>(null) }
    var isTestMode by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Visual Grid and Guidelines background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF090D15))
            )

            // Touch Control Overlay (Interactive Canvas)
            TouchControlOverlay(
                layout = currentLayout,
                isEditMode = !isTestMode,
                selectedElementId = selectedElement?.id,
                onElementSelected = { elem ->
                    selectedElement = elem
                },
                onElementMoved = { elemId, newX, newY ->
                    val updated = currentLayout.elements.map {
                        if (it.id == elemId) it.copy(xPercent = newX, yPercent = newY) else it
                    }
                    currentLayout = currentLayout.copy(elements = updated)
                    if (selectedElement?.id == elemId) {
                        selectedElement = selectedElement?.copy(xPercent = newX, yPercent = newY)
                    }
                },
                onInputTriggered = { key, isDown ->
                    // Test feedback
                }
            )

            // Top Control Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC131B2A))
                            .testTag("control_editor_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC131B2A))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isTestMode) "TEST MODE (TOUCH TO PLAY)" else "DRAG CONTROLS TO REPOSITION",
                            color = if (isTestMode) NeonEmerald else NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Auto Optimize button
                    OutlinedButton(
                        onClick = {
                            currentLayout = ControlEngine.autoOptimizeControlsForErgonomics(currentLayout)
                            Toast.makeText(context, "Controls ergonomically optimized for thumb reach", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("auto_optimize_controls_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Auto Optimize", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AUTO OPTIMIZE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Test mode toggle
                    Button(
                        onClick = { isTestMode = !isTestMode },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTestMode) NeonAmber else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("test_controls_toggle")
                    ) {
                        Text(if (isTestMode) "EXIT TEST" else "TEST", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Save button
                    Button(
                        onClick = {
                            viewModel.updateControlLayout(game.id, currentLayout)
                            Toast.makeText(context, "Touch layout saved!", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonEmerald,
                            contentColor = Color(0xFF003919)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_controls_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SAVE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Bottom Element Inspector / Preset Bar (Visible when not in test mode)
            if (!isTestMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color(0xE6131B2A))
                        .padding(12.dp)
                ) {
                    // Preset switcher chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PRESETS:",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        ) {
                            items(GameGenre.entries) { genre ->
                                val isSelected = currentLayout.preset == genre
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) NeonPurple else Color(0xFF1E293B))
                                        .clickable {
                                            currentLayout = ControlEngine.generateLayoutForGenre(genre)
                                            selectedElement = null
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = genre.displayName.split(" ").first(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Element Inspector if an item is selected
                    selectedElement?.let { elem ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Selected: ${elem.label} (${elem.mappedKey})",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Size: ${elem.sizeDp.toInt()}dp",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                                Slider(
                                    value = elem.sizeDp,
                                    onValueChange = { newSize ->
                                        val updatedElem = elem.copy(sizeDp = newSize)
                                        selectedElement = updatedElem
                                        currentLayout = currentLayout.copy(
                                            elements = currentLayout.elements.map { if (it.id == elem.id) updatedElem else it }
                                        )
                                    },
                                    valueRange = 36f..160f,
                                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Opacity: ${(elem.opacity * 100).toInt()}%",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                                Slider(
                                    value = elem.opacity,
                                    onValueChange = { newOpacity ->
                                        val updatedElem = elem.copy(opacity = newOpacity)
                                        selectedElement = updatedElem
                                        currentLayout = currentLayout.copy(
                                            elements = currentLayout.elements.map { if (it.id == elem.id) updatedElem else it }
                                        )
                                    },
                                    valueRange = 0.2f..1.0f,
                                    colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
