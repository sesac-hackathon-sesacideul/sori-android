package com.misterjerry.test01.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.misterjerry.test01.data.SoundEvent
import com.misterjerry.test01.data.Urgency
import com.misterjerry.test01.ui.theme.ErrorColor
import com.misterjerry.test01.ui.theme.SafeColor
import com.misterjerry.test01.ui.theme.SurfaceColor
import com.misterjerry.test01.ui.theme.TextSecondary
import com.misterjerry.test01.ui.theme.WarningColor

@Composable
fun EnvironmentalSoundScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        SafetySection(soundEvents = uiState.soundEvents)
    }
}

@Composable
fun SafetySection(soundEvents: List<SoundEvent>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "안전 모니터링",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (soundEvents.isEmpty()) {
                Text(
                    text = "소리를 듣는 중...",
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                soundEvents.forEach { event ->
                    SoundEventItem(event)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun SoundEventItem(event: SoundEvent) {
    val color = when (event.urgency) {
        Urgency.HIGH -> ErrorColor
        Urgency.MEDIUM -> WarningColor
        Urgency.LOW -> SafeColor
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        // Direction Indicator (Arrow)
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning, // Placeholder for arrow
                contentDescription = "Direction",
                tint = color,
                modifier = Modifier.rotate(event.direction)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = event.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${"%.1f".format(event.distance)}m 거리",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
