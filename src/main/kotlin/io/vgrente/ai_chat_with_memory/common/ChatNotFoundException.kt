package io.vgrente.ai_chat_with_memory.common

class ChatNotFoundException(chatId: String) : RuntimeException("Chat not found: $chatId")
