package io.vgrente.ai_chat_with_memory.service

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service

@Service
class SimpleChatService(private val chatClientBuilder: ChatClient.Builder) {

    private val chatClient = chatClientBuilder
        .build();


    fun chat(message: String): String {
        return chatClient
            .prompt()
            .user(message)
            .call()
            .content() ?: "No response return from Google AI"
    }

}