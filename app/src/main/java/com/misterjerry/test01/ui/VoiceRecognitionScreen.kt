package com.misterjerry.test01.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.misterjerry.test01.R
import com.misterjerry.test01.data.ConversationItem
import com.misterjerry.test01.ui.theme.TenadaFontFamily
import com.misterjerry.test01.ui.theme.SurfaceColor
import com.misterjerry.test01.ui.theme.TextSecondary

@Composable
fun VoiceRecognitionScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.startListening()
            } else {
                Toast.makeText(context, "음성 인식을 위해 마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (uiState.isListening) {
                        viewModel.stopListening()
                    } else {
                        val permissionCheck = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        )
                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            viewModel.startListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                containerColor = if (uiState.isListening) Color.Red else MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = if (uiState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (uiState.isListening) "Stop Listening" else "Start Listening"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(0x1A0044BA)), // Light Blue
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.icons_voice_recognition_mode),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "음성 인식 모드",
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
                ConversationSection(conversationHistory = uiState.conversationHistory)
            }
        }
    }
}

@Composable
fun ConversationSection(conversationHistory: List<ConversationItem>) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new items are added
    LaunchedEffect(conversationHistory.size) {
        if (conversationHistory.isNotEmpty()) {
            listState.animateScrollToItem(conversationHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // White background for chat area
            .padding(16.dp)
    ) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(conversationHistory) { item ->
                ConversationBubble(item)
            }
        }
    }
}

@Composable
fun ConversationBubble(item: ConversationItem) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (item.isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // User message doesn't have emotion header in this design, or maybe it does? 
            // Usually user messages are simple. The request implies "Voice Recognition Mode" which usually analyzes the *other* person's speech.
            // So I will apply this mainly to non-user messages (the "Stranger").

            Column(
                horizontalAlignment = if (item.isUser) Alignment.End else Alignment.Start,
                modifier = Modifier.weight(1f, fill = false) // Allow bubble to not stretch full width if short
            ) {
                // Bubble
                Box(
                    modifier = Modifier
                        .background(
                            color = if (item.isUser) Color(0xFFE0E0E0) else Color.White,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (item.isUser) Color.Transparent else Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        if (!item.isUser) {
                            // Emotion Header (Icon + Label)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                val emotionIcon = when (item.emotionLabel) {
                                    "긍정" -> R.drawable.icon_positive
                                    "부정" -> R.drawable.icon_negative
                                    else -> R.drawable.icon_neutrality
                                }
                                
                                Icon(
                                    painter = painterResource(id = emotionIcon),
                                    contentDescription = item.emotionLabel,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.Unspecified
                                )
                                
                                // Text is now pushed to the end due to SpaceBetween
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE0E0E0), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.emotionLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                        
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Timestamp
                Text(
                    text = item.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}
