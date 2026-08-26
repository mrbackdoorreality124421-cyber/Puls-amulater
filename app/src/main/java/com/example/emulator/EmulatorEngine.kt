package com.example.emulator

import android.view.Surface

class EmulatorEngine {
    
    companion object {
        init {
            System.loadLibrary("pulse-emulator")
        }
    }
    
    fun initialize(surface: Surface, ramSizeMB: Int): Boolean {
        return initialize(surface.nativeHandle, ramSizeMB)
    }
    
    fun startEmulation(biosPath: String, diskImagePath: String) {
        startEmulation(biosPath, diskImagePath)
    }
    
    fun stopEmulation() {
        stopEmulation()
    }
    
    fun renderFrame() {
        renderFrame()
    }
    
    fun sendKeyEvent(keyCode: Int, isDown: Boolean) {
        sendKeyEvent(keyCode, isDown)
    }
    
    fun sendMouseEvent(x: Int, y: Int, buttonMask: Int) {
        sendMouseEvent(x, y, buttonMask)
    }
    
    // Native methods
    private external fun initialize(nativeWindow: Long, ramSizeMB: Int): Boolean
    private external fun startEmulation(biosPath: String, diskImagePath: String)
    private external fun stopEmulation()
    private external fun renderFrame()
    private external fun sendKeyEvent(keyCode: Int, isDown: Boolean)
    private external fun sendMouseEvent(x: Int, y: Int, buttonMask: Int)
}
