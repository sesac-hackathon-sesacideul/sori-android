package com.misterjerry.test01.network

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OpenAIService {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(@Body request: ChatCompletionRequest): ChatCompletionResponse
}

data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val max_tokens: Int = 10,
    val temperature: Double = 0.7
)

data class Message(
    val role: String,
    val content: String
)

data class ChatCompletionResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)
