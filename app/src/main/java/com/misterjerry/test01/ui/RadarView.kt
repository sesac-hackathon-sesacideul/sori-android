package com.misterjerry.test01.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.misterjerry.test01.data.SoundEvent
import com.misterjerry.test01.data.Urgency
import com.misterjerry.test01.ui.theme.ErrorColor
import com.misterjerry.test01.ui.theme.SafeColor
import com.misterjerry.test01.ui.theme.WarningColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarView(
    soundEvents: List<SoundEvent>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // Animation for the scanning wave
    val infiniteTransition = rememberInfiniteTransition(label = "RadarWave")
    val waveRadiusRatio by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveRadius"
    )

    Box(modifier = modifier.aspectRatio(1f)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = center
            val radius = size.minDimension / 2
            val maxDistance = 10.0f // Max distance to display on radar (e.g., 10 meters)

            // Draw scanning wave
            val currentWaveRadius = radius * waveRadiusRatio
            val waveAlpha = 1f - waveRadiusRatio // Fade out as it expands
            
            drawCircle(
                color = primaryColor.copy(alpha = waveAlpha * 0.5f), // Adjust base opacity as needed
                radius = currentWaveRadius,
                style = Stroke(width = 4.dp.toPx())
            )

            // Draw concentric circles (Grid)
            val circles = 3
            for (i in 1..circles) {
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.3f),
                    radius = radius * (i.toFloat() / circles),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Draw crosshair lines
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.height),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(0f, center.y),
                end = Offset(size.width, center.y),
                strokeWidth = 2.dp.toPx()
            )

            // Draw user (center dot)
            drawCircle(
                color = primaryColor,
                radius = 8.dp.toPx()
            )

            // Draw sound events
            soundEvents.forEach { event ->
                val distanceRatio = (event.distance / maxDistance).coerceIn(0f, 1f)
                val distanceRadius = radius * distanceRatio
                
                // Convert angle to radians (subtract 90 degrees to make 0 degrees represent "Up/North")
                val angleRad = Math.toRadians((event.direction - 90).toDouble())

                val x = center.x + (distanceRadius * cos(angleRad)).toFloat()
                val y = center.y + (distanceRadius * sin(angleRad)).toFloat()

                val color = when (event.urgency) {
                    Urgency.HIGH -> ErrorColor
                    Urgency.MEDIUM -> WarningColor
                    Urgency.LOW -> SafeColor
                }

                drawCircle(
                    color = color,
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )
                
                // Optional: Draw a line from center to the event for better visibility
                drawLine(
                    color = color.copy(alpha = 0.3f),
                    start = center,
                    end = Offset(x, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}
