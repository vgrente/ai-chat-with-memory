package io.vgrente.aichatwithmemory.chatmemory

data class Chat(
    val id: String,
    val description: String,
)

data class ChatMessage(
    val content: String,
    val type: String,
)

data class ChatStartResponse(
    val chatId: String,
    val message: String,
    val description: String,
)
