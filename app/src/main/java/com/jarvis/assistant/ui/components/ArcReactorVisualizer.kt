package com.jarvis.assistant.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.data.model.AssistantStatus
import com.jarvis.assistant.ui.theme.JarvisAmberWarning
import com.jarvis.assistant.ui.theme.JarvisBlueSecondary
import com.jarvis.assistant.ui.theme.JarvisCyanGlow
import com.jarvis.assistant.ui.theme.JarvisCyanLight
import com.jarvis.assistant.ui.theme.JarvisCyanPrimary
import com.jarvis.assistant.ui.theme.JarvisDeepBlack
import com.jarvis.assistant.ui.theme.JarvisRedAlert
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorVisualizer(
    status: AssistantStatus,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcReactorInfinite")

    // Slow continuous outer rotation
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OuterRingRotation"
    )

    // Fast counter-clockwise inner rotation
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "InnerRingRotation"
    )

    // Dynamic pulse scale based on state
    val pulseDuration = when (status) {
        AssistantStatus.LISTENING -> 800
        AssistantStatus.THINKING -> 600
        AssistantStatus.SPEAKING -> 1000
        AssistantStatus.EXECUTING -> 700
        AssistantStatus.IDLE -> 3000
    }

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val (coreColor, glowColor) = when (status) {
        AssistantStatus.LISTENING -> Pair(JarvisCyanLight, JarvisCyanPrimary)
        AssistantStatus.THINKING -> Pair(JarvisBlueSecondary, JarvisCyanPrimary)
        AssistantStatus.SPEAKING -> Pair(JarvisCyanPrimary, JarvisCyanGlow)
        AssistantStatus.EXECUTING -> Pair(JarvisAmberWarning, JarvisAmberWarning.copy(alpha = 0.3f))
        AssistantStatus.IDLE -> Pair(JarvisCyanPrimary, JarvisCyanGlow)
    }

    Box(
        modifier = modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer ambient glow blur
        Box(
            modifier = Modifier
                .size(170.dp)
                .background(glowColor, CircleShape)
                .blur(24.dp)
        )

        // Geometric canvas for Arc Reactor Rings and Nodes
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.88f * pulseScale

            // 1. Outermost guide circle
            drawCircle(
                color = JarvisSurfaceBorderColor,
                radius = baseRadius,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // 2. Segmented outer ring (Rotates clockwise)
            val segmentCount = 12
            val segmentSweep = 360f / segmentCount
            for (i in 0 until segmentCount) {
                val startAngle = outerRotation + (i * segmentSweep) + 4f
                val sweep = segmentSweep - 8f
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(JarvisCyanPrimary.copy(alpha = 0.2f), JarvisCyanPrimary, JarvisBlueSecondary)
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. Middle ring with dashed styling (Counter-clockwise)
            drawCircle(
                color = JarvisCyanPrimary.copy(alpha = 0.6f),
                radius = baseRadius * 0.68f,
                center = center,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), innerRotation)
                )
            )

            // 4. Arc Reactor spokes / triangular power nodes
            val nodeCount = 6
            val nodeRadius = baseRadius * 0.52f
            for (i in 0 until nodeCount) {
                val angleRad = Math.toRadians((outerRotation * 0.5 + (i * 60.0)))
                val x = center.x + (nodeRadius * cos(angleRad)).toFloat()
                val y = center.y + (nodeRadius * sin(angleRad)).toFloat()
                drawCircle(
                    color = coreColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            // 5. Innermost Glowing Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(coreColor, coreColor.copy(alpha = 0.4f), Color.Transparent),
                    center = center,
                    radius = baseRadius * 0.35f
                ),
                radius = baseRadius * 0.35f,
                center = center
            )

            drawCircle(
                color = coreColor,
                radius = baseRadius * 0.18f,
                center = center
            )
        }

        // Center Status HUD readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "JARVIS",
                color = JarvisDeepBlack,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

private val JarvisSurfaceBorderColor = Color(0x5500E5FF)
