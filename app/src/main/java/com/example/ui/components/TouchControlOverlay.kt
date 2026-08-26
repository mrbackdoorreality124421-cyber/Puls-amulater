package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ControlElement
import com.example.data.model.ControlElementType
import com.example.data.model.ControlLayout
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPurple
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun TouchControlOverlay(
    layout: ControlLayout,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    selectedElementId: String? = null,
    onElementSelected: ((ControlElement) -> Unit)? = null,
    onElementMoved: ((elementId: String, xPercent: Float, yPercent: Float) -> Unit)? = null,
    onInputTriggered: ((key: String, isDown: Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val activeToggles = remember { mutableStateMapOf<String, Boolean>() }

    fun triggerHaptic() {
        if (!layout.hapticFeedbackEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(20)
                }
            }
        } catch (e: Exception) {
            // Ignore if vibration unavailable
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        layout.elements.forEach { elem ->
            val isSelected = elem.id == selectedElementId
            val elemX = (elem.xPercent * widthPx) - (elem.sizeDp.dp.value * 2)
            val elemY = (elem.yPercent * heightPx) - (elem.sizeDp.dp.value * 2)
            val elementColor = if (elem.customColorHex != null) Color(elem.customColorHex) else NeonCyan
            val effectiveOpacity = elem.opacity * layout.globalOpacity

            when (elem.type) {
                ControlElementType.JOYSTICK_LEFT, ControlElementType.JOYSTICK_RIGHT -> {
                    var stickOffset by remember { mutableStateOf(Offset.Zero) }
                    var activeKeys by remember { mutableStateOf(setOf<String>()) }

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (elem.xPercent * widthPx - (elem.sizeDp.dp.toPx() / 2)).toInt(),
                                    (elem.yPercent * heightPx - (elem.sizeDp.dp.toPx() / 2)).toInt()
                                )
                            }
                            .size(elem.sizeDp.dp)
                            .testTag("joystick_${elem.id}")
                            .pointerInput(isEditMode) {
                                if (isEditMode) {
                                    detectDragGestures(
                                        onDragStart = { onElementSelected?.invoke(elem) },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val newX = ((elem.xPercent * widthPx) + dragAmount.x) / widthPx
                                            val newY = ((elem.yPercent * heightPx) + dragAmount.y) / heightPx
                                            onElementMoved?.invoke(elem.id, newX.coerceIn(0.05f, 0.95f), newY.coerceIn(0.05f, 0.95f))
                                        }
                                    )
                                } else {
                                    detectDragGestures(
                                        onDragStart = { triggerHaptic() },
                                        onDragEnd = {
                                            stickOffset = Offset.Zero
                                            activeKeys.forEach { onInputTriggered?.invoke(it, false) }
                                            activeKeys = emptySet()
                                        },
                                        onDragCancel = {
                                            stickOffset = Offset.Zero
                                            activeKeys.forEach { onInputTriggered?.invoke(it, false) }
                                            activeKeys = emptySet()
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val maxRadius = (elem.sizeDp.dp.toPx() / 2) - 20
                                            val newPos = stickOffset + dragAmount
                                            val dist = sqrt(newPos.x * newPos.x + newPos.y * newPos.y)
                                            stickOffset = if (dist > maxRadius) {
                                                val angle = atan2(newPos.y, newPos.x)
                                                Offset(cos(angle) * maxRadius, sin(angle) * maxRadius)
                                            } else {
                                                newPos
                                            }
                                            
                                            val newKeys = mutableSetOf<String>()
                                            if (stickOffset.x < -15) newKeys.add("LEFT_A")
                                            if (stickOffset.x > 15) newKeys.add("RIGHT_D")
                                            if (stickOffset.y < -15) newKeys.add("UP_W")
                                            if (stickOffset.y > 15) newKeys.add("DOWN_S")
                                            
                                            val released = activeKeys - newKeys
                                            val pressed = newKeys - activeKeys
                                            
                                            released.forEach { onInputTriggered?.invoke(it, false) }
                                            pressed.forEach { onInputTriggered?.invoke(it, true) }
                                            activeKeys = newKeys
                                        }
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color(0xFF1E293B).copy(alpha = effectiveOpacity * 0.7f),
                                radius = size.minDimension / 2
                            )
                            drawCircle(
                                color = if (isSelected) NeonPurple else elementColor.copy(alpha = effectiveOpacity),
                                radius = size.minDimension / 2,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = if (isSelected) 6f else 3f)
                            )
                            // Inner thumbstick knob
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(elementColor, Color(0xFF0F172A))
                                ),
                                radius = size.minDimension / 4,
                                center = center + stickOffset
                            )
                        }
                    }
                }

                ControlElementType.DPAD -> {
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (elem.xPercent * widthPx - (elem.sizeDp.dp.toPx() / 2)).toInt(),
                                    (elem.yPercent * heightPx - (elem.sizeDp.dp.toPx() / 2)).toInt()
                                )
                            }
                            .size(elem.sizeDp.dp)
                            .testTag("dpad_${elem.id}")
                            .pointerInput(isEditMode) {
                                if (isEditMode) {
                                    detectDragGestures(
                                        onDragStart = { onElementSelected?.invoke(elem) },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val newX = ((elem.xPercent * widthPx) + dragAmount.x) / widthPx
                                            val newY = ((elem.yPercent * heightPx) + dragAmount.y) / heightPx
                                            onElementMoved?.invoke(elem.id, newX.coerceIn(0.05f, 0.95f), newY.coerceIn(0.05f, 0.95f))
                                        }
                                    )
                                } else {
                                    var activeKeys = setOf<String>()
                                    var currentOffset = Offset.Zero
                                    
                                    detectDragGestures(
                                        onDragStart = { triggerHaptic() },
                                        onDragEnd = {
                                            activeKeys.forEach { onInputTriggered?.invoke(it, false) }
                                        },
                                        onDragCancel = {
                                            activeKeys.forEach { onInputTriggered?.invoke(it, false) }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            currentOffset += dragAmount
                                            
                                            // Simple threshold
                                            val newKeys = mutableSetOf<String>()
                                            if (currentOffset.x < -20) newKeys.add("LEFT_A")
                                            if (currentOffset.x > 20) newKeys.add("RIGHT_D")
                                            if (currentOffset.y < -20) newKeys.add("UP_W")
                                            if (currentOffset.y > 20) newKeys.add("DOWN_S")
                                            
                                            val released = activeKeys - newKeys
                                            val pressed = newKeys - activeKeys
                                            
                                            released.forEach { onInputTriggered?.invoke(it, false) }
                                            pressed.forEach { onInputTriggered?.invoke(it, true) }
                                            activeKeys = newKeys
                                        }
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = if (isSelected) 5f else 2.5f
                            drawCircle(
                                color = Color(0xFF0F172A).copy(alpha = effectiveOpacity * 0.8f),
                                radius = size.minDimension / 2
                            )
                            drawCircle(
                                color = if (isSelected) NeonPurple else elementColor.copy(alpha = effectiveOpacity),
                                radius = size.minDimension / 2,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                            )
                            // Cross lines for D-Pad
                            val mid = size.minDimension / 2
                            drawLine(elementColor.copy(alpha = effectiveOpacity), Offset(mid, 15f), Offset(mid, size.height - 15f), strokeWidth = 3f)
                            drawLine(elementColor.copy(alpha = effectiveOpacity), Offset(15f, mid), Offset(size.width - 15f, mid), strokeWidth = 3f)
                        }
                    }
                }

                ControlElementType.BUTTON, ControlElementType.TRIGGER_BUTTON, ControlElementType.KEYBOARD_KEY -> {
                    val isToggled = activeToggles[elem.id] == true
                    var isPressed by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (elem.xPercent * widthPx - (elem.sizeDp.dp.toPx() / 2)).toInt(),
                                    (elem.yPercent * heightPx - (elem.sizeDp.dp.toPx() / 2)).toInt()
                                )
                            }
                            .size(elem.sizeDp.dp)
                            .clip(if (elem.type == ControlElementType.KEYBOARD_KEY) RoundedCornerShape(8.dp) else CircleShape)
                            .background(
                                if (isPressed || isToggled) elementColor.copy(alpha = 0.5f)
                                else Color(0xFF131B2A).copy(alpha = effectiveOpacity)
                            )
                            .border(
                                width = if (isSelected) 3.dp else 1.5.dp,
                                color = if (isSelected) NeonPurple else (if (isPressed || isToggled) elementColor else elementColor.copy(alpha = effectiveOpacity)),
                                shape = if (elem.type == ControlElementType.KEYBOARD_KEY) RoundedCornerShape(8.dp) else CircleShape
                            )
                            .testTag("btn_${elem.id}")
                            .pointerInput(isEditMode) {
                                if (isEditMode) {
                                    detectDragGestures(
                                        onDragStart = { onElementSelected?.invoke(elem) },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val newX = ((elem.xPercent * widthPx) + dragAmount.x) / widthPx
                                            val newY = ((elem.yPercent * heightPx) + dragAmount.y) / heightPx
                                            onElementMoved?.invoke(elem.id, newX.coerceIn(0.04f, 0.96f), newY.coerceIn(0.04f, 0.96f))
                                        }
                                    )
                                } else {
                                    detectTapGestures(
                                        onPress = {
                                            isPressed = true
                                            triggerHaptic()
                                            if (elem.isToggle) {
                                                val state = !(activeToggles[elem.id] ?: false)
                                                activeToggles[elem.id] = state
                                                onInputTriggered?.invoke(elem.mappedKey, state)
                                            } else {
                                                onInputTriggered?.invoke(elem.mappedKey, true)
                                            }
                                            tryAwaitRelease()
                                            isPressed = false
                                            if (!elem.isToggle) {
                                                onInputTriggered?.invoke(elem.mappedKey, false)
                                            }
                                        }
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = elem.label,
                            color = if (isPressed || isToggled) Color.White else elementColor,
                            fontSize = if (elem.sizeDp < 50) 10.sp else 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                ControlElementType.CAMERA_SWIPE_ZONE, ControlElementType.MOUSE_TRACKPAD -> {
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (elem.xPercent * widthPx - (elem.sizeDp.dp.toPx() / 2)).toInt(),
                                    (elem.yPercent * heightPx - (elem.sizeDp.dp.toPx() / 2)).toInt()
                                )
                            }
                            .size(width = elem.sizeDp.dp * 1.3f, height = elem.sizeDp.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0B1322).copy(alpha = effectiveOpacity * 0.5f))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) NeonPurple else Color(0xFF334155).copy(alpha = effectiveOpacity),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .testTag("trackpad_${elem.id}")
                            .pointerInput(isEditMode) {
                                if (isEditMode) {
                                    detectDragGestures(
                                        onDragStart = { onElementSelected?.invoke(elem) },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val newX = ((elem.xPercent * widthPx) + dragAmount.x) / widthPx
                                            val newY = ((elem.yPercent * heightPx) + dragAmount.y) / heightPx
                                            onElementMoved?.invoke(elem.id, newX.coerceIn(0.1f, 0.9f), newY.coerceIn(0.1f, 0.9f))
                                        }
                                    )
                                } else {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        onInputTriggered?.invoke(elem.mappedKey, true)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = elem.label,
                            color = Color(0xFF94A3B8).copy(alpha = effectiveOpacity),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
