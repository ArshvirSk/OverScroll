package com.overscroll.app.overlay

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class MascotState {
    CONTENT, TIRED, WORN_OUT
}

@Composable
fun MascotFace(
    count: Int,
    thresholdA: Int,
    thresholdB: Int,
    modifier: Modifier = Modifier
) {
    val mascotState = derivedStateOf {
        when {
            count >= thresholdB -> MascotState.WORN_OUT
            count >= thresholdA -> MascotState.TIRED
            else -> MascotState.CONTENT
        }
    }

    // Shake animation on state change (threshold crossed)
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(mascotState.value) {
        if (count > 0) { // Don't shake on initial load
            shakeOffset.animateTo(10f, animationSpec = tween(50, easing = LinearEasing))
            shakeOffset.animateTo(-10f, animationSpec = tween(50, easing = LinearEasing))
            shakeOffset.animateTo(10f, animationSpec = tween(50, easing = LinearEasing))
            shakeOffset.animateTo(-10f, animationSpec = tween(50, easing = LinearEasing))
            shakeOffset.animateTo(0f, animationSpec = tween(50, easing = LinearEasing))
        }
    }

    // Blink animation
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay((3000..6000).random().toLong()) // 3-6 seconds idle
            isBlinking = true
            delay(150) // Blink duration
            isBlinking = false
        }
    }

    Canvas(
        modifier = modifier
            .size(52.dp)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        translate(left = shakeOffset.value) {
            // Draw teal squircle background
                drawRoundRect(
                    color = Color(0xFF00BFA5),
                    cornerRadius = CornerRadius(w * 0.42f, h * 0.42f)
                )

                val facePaintColor = Color.White
                val strokeWidth = 6f

                // Blink logic
                val eyeHeightScale = if (isBlinking) 0.1f else 1f

                when (mascotState.value) {
                    MascotState.CONTENT -> {
                        // Dot eyes
                        val eyeRadius = w * 0.08f
                        drawOval(
                            color = facePaintColor,
                            topLeft = Offset(cx - w * 0.2f - eyeRadius, cy - h * 0.1f - (eyeRadius * eyeHeightScale)),
                            size = androidx.compose.ui.geometry.Size(eyeRadius * 2, eyeRadius * 2 * eyeHeightScale)
                        )
                        drawOval(
                            color = facePaintColor,
                            topLeft = Offset(cx + w * 0.2f - eyeRadius, cy - h * 0.1f - (eyeRadius * eyeHeightScale)),
                            size = androidx.compose.ui.geometry.Size(eyeRadius * 2, eyeRadius * 2 * eyeHeightScale)
                        )

                        // Subtle smile
                        val mouthPath = Path().apply {
                            moveTo(cx - w * 0.12f, cy + h * 0.15f)
                            quadraticBezierTo(cx, cy + h * 0.3f, cx + w * 0.12f, cy + h * 0.15f)
                        }
                        drawPath(
                            path = mouthPath,
                            color = facePaintColor,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                    MascotState.TIRED -> {
                        // Half-closed eyes
                        val eyeWidth = w * 0.12f
                        val eyeHeight = h * 0.04f * eyeHeightScale
                        drawRoundRect(
                            color = facePaintColor,
                            topLeft = Offset(cx - w * 0.25f, cy - h * 0.1f),
                            size = androidx.compose.ui.geometry.Size(eyeWidth, eyeHeight),
                            cornerRadius = CornerRadius(eyeHeight/2, eyeHeight/2)
                        )
                        drawRoundRect(
                            color = facePaintColor,
                            topLeft = Offset(cx + w * 0.13f, cy - h * 0.1f),
                            size = androidx.compose.ui.geometry.Size(eyeWidth, eyeHeight),
                            cornerRadius = CornerRadius(eyeHeight/2, eyeHeight/2)
                        )

                        // Slightly downturned mouth
                        val mouthPath = Path().apply {
                            moveTo(cx - w * 0.1f, cy + h * 0.2f)
                            quadraticBezierTo(cx, cy + h * 0.15f, cx + w * 0.1f, cy + h * 0.2f)
                        }
                        drawPath(
                            path = mouthPath,
                            color = facePaintColor,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                    MascotState.WORN_OUT -> {
                        // Droopy eyes (angles)
                        val eyeWidth = w * 0.12f
                        val eyePathLeft = Path().apply {
                            moveTo(cx - w * 0.25f, cy - h * 0.05f)
                            lineTo(cx - w * 0.13f, cy - h * 0.1f)
                        }
                        drawPath(
                            path = eyePathLeft,
                            color = facePaintColor,
                            style = Stroke(width = strokeWidth * eyeHeightScale, cap = StrokeCap.Round)
                        )
                        val eyePathRight = Path().apply {
                            moveTo(cx + w * 0.13f, cy - h * 0.1f)
                            lineTo(cx + w * 0.25f, cy - h * 0.05f)
                        }
                        drawPath(
                            path = eyePathRight,
                            color = facePaintColor,
                            style = Stroke(width = strokeWidth * eyeHeightScale, cap = StrokeCap.Round)
                        )

                        // Small frown
                        val mouthPath = Path().apply {
                            moveTo(cx - w * 0.08f, cy + h * 0.25f)
                            quadraticBezierTo(cx, cy + h * 0.15f, cx + w * 0.08f, cy + h * 0.25f)
                        }
                        drawPath(
                            path = mouthPath,
                            color = facePaintColor,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        
                        // Subtle sweat drop
                        val sweatPath = Path().apply {
                            moveTo(cx + w * 0.25f, cy + h * 0.1f)
                            quadraticBezierTo(cx + w * 0.35f, cy + h * 0.15f, cx + w * 0.25f, cy + h * 0.25f)
                            quadraticBezierTo(cx + w * 0.15f, cy + h * 0.15f, cx + w * 0.25f, cy + h * 0.1f)
                        }
                        drawPath(path = sweatPath, color = Color(0x99FFFFFF))
                    }
                }
            }
        }
    }
