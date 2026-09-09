package io.vgrente.ai_chat_with_memory.simplechat.service

import io.vgrente.ai_chat_with_memory.common.ApiResult
import io.vgrente.ai_chat_with_memory.simplechat.ChatResponse
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class SimpleChatService(chatClientBuilder: ChatClient.Builder) {

    private val chatClient = chatClientBuilder
        .build()

    fun chat(message: String): ApiResult<ChatResponse> {
        val content = chatClient
            .prompt()
            .user(message)
            .call()
            .content()
        return if (content != null) ApiResult.Success(ChatResponse(content)) else ApiResult.Failure("No response return from Google AI")
    }
}
