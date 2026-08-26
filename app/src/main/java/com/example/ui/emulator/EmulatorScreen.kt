package com.example.ui.emulator

import android.view.SurfaceView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.emulator.EmulatorEngine

@Composable
fun EmulatorScreen() {
    val context = LocalContext.current
    val engine = remember { EmulatorEngine() }
    var isRunning by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Emulator display
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        holder.addCallback(object : android.view.SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                                engine.initialize(holder.surface, 256) // 256 MB RAM
                            }
                            override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {}
                            override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {}
                        })
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    if (!isRunning) {
                        engine.startEmulation(
                            "/sdcard/Download/bios.bin",
                            "/sdcard/Download/disk.img"
                        )
                        isRunning = true
                    } else {
                        engine.stopEmulation()
                        isRunning = false
                    }
                }
            ) {
                Text(if (isRunning) "Stop" else "Start")
            }
        }
    }
    
    // Render loop
    LaunchedEffect(isRunning) {
        while (isRunning) {
            engine.renderFrame()
            kotlinx.coroutines.delay(16) // ~60 FPS
        }
    }
}
