package com.misterjerry.test01.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class ConversationItem(
    val id: Long,
    val speaker: String,
    val text: String,
    val emotion: String, // Emojis: 😠, 😃, 😐
    val emotionLabel: String, // Text: 화남, 기쁨, 평범
    val isUser: Boolean = false,
    val timestamp: String = "", // e.g., "오후 07:36"
    val isLoading: Boolean = false
)

class ConversationRepository {
    private val mockDialogues = listOf(
        ConversationItem(0, "낯선 사람", "실례합니다, 좀 도와주시겠어요?", "", "중립"),
        ConversationItem(0, "낯선 사람", "길을 잃은 것 같아요.", "", "부정"),
        ConversationItem(0, "낯선 사람", "가장 가까운 지하철역이 어디인가요?", "", "중립"),
        ConversationItem(0, "낯선 사람", "정말 감사합니다!", "", "긍정"),
        ConversationItem(0, "낯선 사람", "조심하세요!", "", "부정"),
        ConversationItem(0, "낯선 사람", "괜찮으세요?", "", "중립")
    )

    fun getConversationStream(): Flow<ConversationItem> = flow {
        var index = 0
        while (true) {
            delay(2500) // Simulate speech every 2.5 seconds
            val currentTime = java.text.SimpleDateFormat("a hh:mm", java.util.Locale.KOREA).format(java.util.Date())
            val item = mockDialogues[index % mockDialogues.size].copy(
                id = System.currentTimeMillis(),
                timestamp = currentTime
            )
            emit(item)
            index++
        }
    }
}
