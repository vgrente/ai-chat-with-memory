package io.vgrente.aichatwithmemory.common

class ChatNotFoundException(
    chatId: String,
) : RuntimeException("Chat not found: $chatId")
