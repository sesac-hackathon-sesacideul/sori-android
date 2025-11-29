package com.misterjerry.test01.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class ConversationItem(
    val id: Long,
    val speaker: String,
    val text: String,
    val emotion: String, // Emojis: 😠, 😃, 😐
    val isUser: Boolean = false
)

class ConversationRepository {
    private val mockDialogues = listOf(
        ConversationItem(0, "Stranger", "Excuse me, can you help me?", "😐"),
        ConversationItem(0, "Stranger", "I seem to be lost.", "😟"),
        ConversationItem(0, "Stranger", "Where is the nearest subway station?", "🤔"),
        ConversationItem(0, "Stranger", "Thank you so much!", "😃"),
        ConversationItem(0, "Stranger", "Watch out!", "😠"),
        ConversationItem(0, "Stranger", "Are you okay?", "😟")
    )

    fun getConversationStream(): Flow<ConversationItem> = flow {
        var index = 0
        while (true) {
            delay(2500) // Simulate speech every 2.5 seconds
            val item = mockDialogues[index % mockDialogues.size].copy(id = System.currentTimeMillis())
            emit(item)
            index++
        }
    }
}
