package io.vgrente.aichatwithmemory.chatmemory.service

import io.vgrente.aichatwithmemory.chatmemory.Chat
import io.vgrente.aichatwithmemory.chatmemory.ChatMessage
import io.vgrente.aichatwithmemory.chatmemory.repository.ChatMemoryIDRepository
import io.vgrente.aichatwithmemory.common.ApiResult
import io.vgrente.aichatwithmemory.common.ChatNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.quality.Strictness
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.api.Advisor
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatMemoryServiceTest {
    @Mock
    private lateinit var chatClientBuilder: ChatClient.Builder

    @Mock
    private lateinit var jdbcChatMemoryRepository: JdbcChatMemoryRepository

    @Mock
    private lateinit var chatMemoryRepository: ChatMemoryIDRepository

    private lateinit var callResponseSpec: ChatClient.CallResponseSpec

    private fun service(): ChatMemoryService {
        val chatClient: ChatClient = mock()
        val requestSpec: ChatClient.ChatClientRequestSpec = mock()
        callResponseSpec = mock()

        val advisorSpec: ChatClient.AdvisorSpec = mock()

        given(chatClientBuilder.clone()).willReturn(chatClientBuilder)
        given(chatClientBuilder.defaultAdvisors(any<Advisor>())).willReturn(chatClientBuilder)
        given(chatClientBuilder.build()).willReturn(chatClient)
        given(chatClient.prompt()).willReturn(requestSpec)
        given(requestSpec.user(any<String>())).willReturn(requestSpec)
        given(
            requestSpec.advisors(any<java.util.function.Consumer<ChatClient.AdvisorSpec>>()),
        ).willAnswer { invocation ->
            val consumer = invocation.getArgument<java.util.function.Consumer<ChatClient.AdvisorSpec>>(0)
            consumer.accept(advisorSpec)
            requestSpec
        }
        given(requestSpec.call()).willReturn(callResponseSpec)

        return ChatMemoryService(chatClientBuilder, jdbcChatMemoryRepository, chatMemoryRepository)
    }

    @Test
    fun `chat returns Success when chat exists and model responds`() {
        val service = service()
        given(callResponseSpec.content()).willReturn("hi there")

        val result = service.chat("chat-1", "hello")

        assertEquals(ApiResult.Success("hi there"), result)
    }

    @Test
    fun `chat returns Failure when model returns null content`() {
        val service = service()
        given(callResponseSpec.content()).willReturn(null)

        val result = service.chat("chat-1", "hello")

        assertEquals(ApiResult.Failure("No response from AI"), result)
    }

    @Test
    fun `chat throws ChatNotFoundException when chatId does not exist`() {
        val service = service()
        given(chatMemoryRepository.requireChatExists("missing")).willThrow(ChatNotFoundException("missing"))

        assertThrows<ChatNotFoundException> {
            service.chat("missing", "hello")
        }
    }

    @Test
    fun `getAllChats returns Success with the repository's chats`() {
        val service = service()
        val chats = listOf(Chat("chat-1", "desc"))
        given(chatMemoryRepository.getAllChatsForUser("Vincent")).willReturn(chats)

        val result = service.getAllChats()

        assertEquals(ApiResult.Success(chats), result)
    }

    @Test
    fun `getMessagesByChatId returns Success with the repository's messages`() {
        val service = service()
        val messages = listOf(ChatMessage("hi", "USER"))
        given(chatMemoryRepository.getChatMessages("chat-1")).willReturn(messages)

        val result = service.getMessagesByChatId("chat-1")

        assertEquals(ApiResult.Success(messages), result)
    }

    @Test
    fun `getMessagesByChatId throws ChatNotFoundException when chatId does not exist`() {
        val service = service()
        given(chatMemoryRepository.requireChatExists("missing")).willThrow(ChatNotFoundException("missing"))

        assertThrows<ChatNotFoundException> {
            service.getMessagesByChatId("missing")
        }
    }

    @Test
    fun `createChatWithResponse returns Success with generated chatId, description and reply`() {
        val service = service()
        given(callResponseSpec.content()).willReturn("Generated description", "hi there")

        val result = service.createChatWithResponse("hello")
        check(result is ApiResult.Success)

        assertEquals("hi there", result.data.message)
        assertEquals("Generated description", result.data.description)
        verify(chatMemoryRepository).createChat(result.data.chatId, "Vincent", "Generated description")
    }

    @Test
    fun `createChatWithResponse falls back to a default description when description generation fails`() {
        val service = service()
        given(callResponseSpec.content()).willReturn(null, "hi there")

        val result = service.createChatWithResponse("hello")
        check(result is ApiResult.Success)

        assertEquals("description", result.data.description)
        verify(chatMemoryRepository).createChat(result.data.chatId, "Vincent", "description")
    }

    @Test
    fun `createChatWithResponse returns Failure when the model fails to respond`() {
        val service = service()
        given(callResponseSpec.content()).willReturn("Generated description", null)

        val result = service.createChatWithResponse("hello")

        assertEquals(ApiResult.Failure("No response from AI"), result)
        verify(chatMemoryRepository, never()).createChat(any(), any(), any())
    }
}
