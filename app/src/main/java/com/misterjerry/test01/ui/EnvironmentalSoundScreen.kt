package com.misterjerry.test01.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.misterjerry.test01.R
import com.misterjerry.test01.ui.theme.TenadaFontFamily
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.misterjerry.test01.data.SoundSettings
import com.misterjerry.test01.data.UrgencySetting
import com.misterjerry.test01.data.VibrationPattern
import com.misterjerry.test01.ui.theme.LightTextPrimary
import com.misterjerry.test01.ui.theme.SafeColor
import com.misterjerry.test01.ui.theme.SurfaceColor
import com.misterjerry.test01.ui.theme.TextSecondary
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.misterjerry.test01.ui.theme.WarningColor
import com.misterjerry.test01.ui.theme.NotificationAccent
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider

@Composable
fun EnvironmentalSoundScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.startEnvironmentMode()
            } else {
                Toast.makeText(context, "환경 소리 감지를 위해 마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    DisposableEffect(Unit) {
        val permissionCheck = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        )
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            viewModel.startEnvironmentMode()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        
        onDispose {
            viewModel.stopEnvironmentMode()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(Color(0x1AF63030)), // Light Red
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_environment_sound_mode),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "환경 소리 모드",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = TenadaFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Radar View
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                RadarView(
                    soundEvents = uiState.soundEvents,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .padding(bottom = 24.dp)
                )
            }

            var showSettingsDialog by remember { mutableStateOf(false) }

            if (showSettingsDialog) {
                SoundSettingsDialog(
                    currentSettings = uiState.soundSettings,
                    onDismiss = { showSettingsDialog = false },
                    onConfirm = { newSettings ->
                        viewModel.updateSoundSettings(newSettings)
                        showSettingsDialog = false
                    }
                )
            }

            SafetySection(
                soundEvents = uiState.soundEvents,
                modifier = Modifier.weight(1f),
                onSettingsClick = { showSettingsDialog = true },
                onClearClick = { viewModel.clearSoundEvents() }
            )
        }
    }
}

@Composable
fun SoundSettingsDialog(
    currentSettings: SoundSettings,
    onDismiss: () -> Unit,
    onConfirm: (SoundSettings) -> Unit
) {
    var highSetting by remember { mutableStateOf(currentSettings.highUrgency) }
    var mediumSetting by remember { mutableStateOf(currentSettings.mediumUrgency) }
    var lowSetting by remember { mutableStateOf(currentSettings.lowUrgency) }

    AlertDialog(
        containerColor = Color.White,
        onDismissRequest = onDismiss,
        title = { Text(text = "알림 설정") },
        text = {
            Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                UrgencySettingItem(
                    title = "위험 소리",
                    setting = highSetting,
                    onSettingChange = { highSetting = it }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF0F0F0))
                UrgencySettingItem(
                    title = "주의 소리",
                    setting = mediumSetting,
                    onSettingChange = { mediumSetting = it }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF0F0F0))
                UrgencySettingItem(
                    title = "일상 소리",
                    setting = lowSetting,
                    onSettingChange = { lowSetting = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        SoundSettings(
                            highUrgency = highSetting,
                            mediumUrgency = mediumSetting,
                            lowUrgency = lowSetting
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NotificationAccent,
                    contentColor = Color.Black
                )
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
            ) {
                Text("취소")
            }
        }
    )
}

@Composable
fun UrgencySettingItem(
    title: String,
    setting: UrgencySetting,
    onSettingChange: (UrgencySetting) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontWeight = FontWeight.Bold)
            Switch(
                checked = setting.isEnabled,
                onCheckedChange = { onSettingChange(setting.copy(isEnabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = NotificationAccent,
                    checkedThumbColor = Color.White
                )
            )
        }
        
        if (setting.isEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            Spacer(modifier = Modifier.height(8.dp))
            
            var expanded by remember { mutableStateOf(false) }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "진동 패턴", style = MaterialTheme.typography.bodyMedium)
                
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                        .clickable { expanded = true }
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = setting.vibrationPattern.label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = Color.Gray
                        )
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        VibrationPattern.values().forEach { pattern ->
                            DropdownMenuItem(
                                text = { Text(text = pattern.label) },
                                onClick = {
                                    onSettingChange(setting.copy(vibrationPattern = pattern))
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "알림이 꺼져 있습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SafetySection(
    soundEvents: List<SoundEvent>,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "감지된 소리",
                    style = MaterialTheme.typography.titleLarge,
                    color = LightTextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onClearClick) {
                        Text(text = "초기화", color = TextSecondary)
                    }
                    androidx.compose.material3.IconButton(onClick = onSettingsClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings_custom),
                            contentDescription = "Settings",
                            tint = Color.Unspecified
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (soundEvents.isEmpty()) {
                Text(
                    text = "소리를 듣는 중...",
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(soundEvents) { event ->
                        SoundEventItem(event)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${"%.1f".format(event.distance)}m 거리",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = formatTimeAgo(event.id),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        seconds < 60 -> "방금 전"
        minutes < 60 -> "${minutes}분 전"
        else -> "${hours}시간 전"
    }
}
