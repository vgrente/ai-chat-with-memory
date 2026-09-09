package io.vgrente.ai_chat_with_memory.chatmemory.service

import io.vgrente.ai_chat_with_memory.chatmemory.Chat
import io.vgrente.ai_chat_with_memory.chatmemory.ChatMessage
import io.vgrente.ai_chat_with_memory.chatmemory.ChatStartResponse
import io.vgrente.ai_chat_with_memory.chatmemory.repository.ChatMemoryIDRepository
import io.vgrente.ai_chat_with_memory.common.ApiResult
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository
import org.springframework.stereotype.Service
import java.util.UUID

const val DEFAULT_USER_ID:String = "Vincent"
const val DESCRIPTION_PROMPT: String = "Generate a chat description based on the message, limiting the description to 30 characters: ";

    @Service
class ChatMemoryService(
    chatClientBuilder: ChatClient.Builder,
    jdbcChatMemoryRepository: JdbcChatMemoryRepository,
    private val chatMemoryRepository: ChatMemoryIDRepository
) {
    private val chatClient = chatClientBuilder.clone().build()

    private val chatMemory = MessageWindowChatMemory.builder()
        .chatMemoryRepository(jdbcChatMemoryRepository)
        .maxMessages(10)
        .build()

    private val chatClientWithMemory = chatClientBuilder
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
        .build()

    fun generateDescription(message: String): String? {
        return this.chatClient
            .prompt()
            .user(DESCRIPTION_PROMPT + message)
            .call()
            .content()
    }


    fun chat(chatId: String, message: String): ApiResult<String> {
        chatMemoryRepository.requireChatExists(chatId)
        return performChat(chatId, message)
    }

    private fun performChat(chatId: String, message: String): ApiResult<String> {
        val content = this.chatClientWithMemory
            .prompt()
            .user(message)
            .advisors { a -> a.param(ChatMemory.CONVERSATION_ID, chatId) }
            .call().content()
        return if (content != null) ApiResult.Success(content) else ApiResult.Failure("No response from AI")
    }

    fun getAllChats(): ApiResult<List<Chat>> {
        return ApiResult.Success(this.chatMemoryRepository.getAllChatsForUser(DEFAULT_USER_ID))
    }

    fun getMessagesByChatId(chatId: String): ApiResult<List<ChatMessage>> {
        this.chatMemoryRepository.requireChatExists(chatId)
        return ApiResult.Success(this.chatMemoryRepository.getChatMessages(chatId))
    }

    fun createChatWithResponse(message: String): ApiResult<ChatStartResponse> {
        val description = this.generateDescription(message) ?: "description"
        val chatId = UUID.randomUUID().toString()
        return when (val result = performChat(chatId, message)) {
            is ApiResult.Success -> {
                chatMemoryRepository.createChat(chatId, DEFAULT_USER_ID, description)
                ApiResult.Success(ChatStartResponse(chatId, result.data, description))
            }
            is ApiResult.NotFound, is ApiResult.Failure -> result
        }
    }
}