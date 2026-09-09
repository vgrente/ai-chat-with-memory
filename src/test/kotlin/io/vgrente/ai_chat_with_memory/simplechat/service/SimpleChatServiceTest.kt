package io.vgrente.ai_chat_with_memory.simplechat.service

import io.vgrente.ai_chat_with_memory.common.ApiResult
import io.vgrente.ai_chat_with_memory.simplechat.ChatResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.ai.chat.client.ChatClient
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class SimpleChatServiceTest {

    @Mock
    private lateinit var chatClientBuilder: ChatClient.Builder

    private fun service(): Pair<SimpleChatService, ChatClient> {
        val chatClient: ChatClient = mock()
        given(chatClientBuilder.build()).willReturn(chatClient)
        return SimpleChatService(chatClientBuilder) to chatClient
    }

    @Test
    fun `chat returns the model's response content`() {
        val (service, chatClient) = service()
        val requestSpec: ChatClient.ChatClientRequestSpec = mock()
        val callResponseSpec: ChatClient.CallResponseSpec = mock()

        given(chatClient.prompt()).willReturn(requestSpec)
        given(requestSpec.user("hello")).willReturn(requestSpec)
        given(requestSpec.call()).willReturn(callResponseSpec)
        given(callResponseSpec.content()).willReturn("hi there")

        val result = service.chat("hello")

        assertEquals(ApiResult.Success(ChatResponse("hi there")), result)
    }

    @Test
    fun `chat returns fallback message when model returns null content`() {
        val (service, chatClient) = service()
        val requestSpec: ChatClient.ChatClientRequestSpec = mock()
        val callResponseSpec: ChatClient.CallResponseSpec = mock()

        given(chatClient.prompt()).willReturn(requestSpec)
        given(requestSpec.user("hello")).willReturn(requestSpec)
        given(requestSpec.call()).willReturn(callResponseSpec)
        given(callResponseSpec.content()).willReturn(null)

        val result = service.chat("hello")

        assertEquals(ApiResult.Failure("No response return from Google AI"), result)
    }
}
