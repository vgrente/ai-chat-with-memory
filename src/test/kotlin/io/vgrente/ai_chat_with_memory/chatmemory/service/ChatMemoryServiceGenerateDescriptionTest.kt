package io.vgrente.ai_chat_with_memory.chatmemory.service

import io.vgrente.ai_chat_with_memory.chatmemory.repository.ChatMemoryIDRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.chat.prompt.Prompt
import kotlin.test.assertEquals

/**
 * Regression test for a bug where generateDescription() shared the same
 * memory-advisor-wrapped ChatClient as chat(), so every "start new chat" call
 * failed with `IllegalArgumentException: conversationId cannot be null` —
 * generateDescription() is a one-shot call unrelated to any conversation and
 * never sets a conversation ID.
 *
 * Unlike ChatMemoryServiceTest, this builds a REAL ChatClient from a mocked
 * ChatModel instead of mocking ChatClient itself, so it actually runs Spring
 * AI's real MessageChatMemoryAdvisor. A fully mocked ChatClient chain never
 * executes the advisor's real validation and let this bug through silently.
 */
@ExtendWith(MockitoExtension::class)
class ChatMemoryServiceGenerateDescriptionTest {

    @Mock
    private lateinit var chatModel: ChatModel

    @Mock
    private lateinit var jdbcChatMemoryRepository: JdbcChatMemoryRepository

    @Mock
    private lateinit var chatMemoryRepository: ChatMemoryIDRepository

    @Test
    fun `generateDescription does not require a conversation id`() {
        given(chatModel.getOptions()).willReturn(ChatOptions.builder().build())
        given(chatModel.call(any<Prompt>())).willReturn(
            ChatResponse(listOf(Generation(AssistantMessage("A short description"))))
        )

        val chatClientBuilder: ChatClient.Builder = ChatClient.builder(chatModel)
        val service = ChatMemoryService(chatClientBuilder, jdbcChatMemoryRepository, chatMemoryRepository)

        val description = service.generateDescription("Remember the number 42.")

        assertEquals("A short description", description)
    }
}
