package io.vgrente.aichatwithmemory.chatmemory.controller

import io.vgrente.aichatwithmemory.chatmemory.Chat
import io.vgrente.aichatwithmemory.chatmemory.ChatMessage
import io.vgrente.aichatwithmemory.chatmemory.ChatStartResponse
import io.vgrente.aichatwithmemory.chatmemory.service.ChatMemoryService
import io.vgrente.aichatwithmemory.common.ApiResult
import io.vgrente.aichatwithmemory.common.ChatNotFoundException
import io.vgrente.aichatwithmemory.common.ChatRequest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

@WebMvcTest(MemoryChatController::class)
class MemoryChatControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var chatMemoryService: ChatMemoryService

    @Test
    fun `GET api-chat-memory returns all chats`() {
        given(chatMemoryService.getAllChats()).willReturn(ApiResult.Success(listOf(Chat("chat-1", "desc"))))

        mockMvc
            .perform(get("/api/chat-memory"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value("chat-1"))
            .andExpect(jsonPath("$[0].description").value("desc"))
    }

    @Test
    fun `GET api-chat-memory chatId returns messages for an existing chat`() {
        given(chatMemoryService.getMessagesByChatId("chat-1"))
            .willReturn(ApiResult.Success(listOf(ChatMessage("hi", "USER"))))

        mockMvc
            .perform(get("/api/chat-memory/chat-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].content").value("hi"))
            .andExpect(jsonPath("$[0].type").value("USER"))
    }

    @Test
    fun `GET api-chat-memory chatId returns 404 when chat does not exist`() {
        given(chatMemoryService.getMessagesByChatId("missing"))
            .willThrow(ChatNotFoundException("missing"))

        mockMvc
            .perform(get("/api/chat-memory/missing"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.detail").value("Chat not found: missing"))
    }

    @Test
    fun `POST api-chat-memory-start creates a new chat`() {
        given(chatMemoryService.createChatWithResponse("hello"))
            .willReturn(ApiResult.Success(ChatStartResponse("chat-1", "hi there", "desc")))

        mockMvc
            .perform(
                post("/api/chat-memory/start")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(ChatRequest("hello"))),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.chatId").value("chat-1"))
            .andExpect(jsonPath("$.message").value("hi there"))
            .andExpect(jsonPath("$.description").value("desc"))
    }

    @Test
    fun `POST api-chat-memory-start returns 500 when chat could not be created`() {
        given(chatMemoryService.createChatWithResponse("hello"))
            .willReturn(ApiResult.Failure("Could not create chat"))

        mockMvc
            .perform(
                post("/api/chat-memory/start")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(ChatRequest("hello"))),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.detail").value("Could not create chat"))
    }

    @Test
    fun `POST api-chat-memory chatId adds a message and returns the assistant reply`() {
        given(chatMemoryService.chat("chat-1", "hello")).willReturn(ApiResult.Success("hi there"))

        mockMvc
            .perform(
                post("/api/chat-memory/chat-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(ChatRequest("hello"))),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.content").value("hi there"))
            .andExpect(jsonPath("$.type").value("ASSISTANT"))
    }

    @Test
    fun `POST api-chat-memory chatId returns 404 when chat does not exist`() {
        given(chatMemoryService.chat("missing", "hello")).willThrow(ChatNotFoundException("missing"))

        mockMvc
            .perform(
                post("/api/chat-memory/missing")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(ChatRequest("hello"))),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.detail").value("Chat not found: missing"))
    }

    @Test
    fun `POST api-chat-memory chatId returns 500 when the model fails to respond`() {
        given(chatMemoryService.chat("chat-1", "hello")).willReturn(ApiResult.Failure("No response from AI"))

        mockMvc
            .perform(
                post("/api/chat-memory/chat-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(ChatRequest("hello"))),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.detail").value("No response from AI"))
    }

    @Test
    fun `POST api-chat-memory chatId returns 404 when the service returns NotFound directly`() {
        given(chatMemoryService.chat("chat-1", "hello")).willReturn(ApiResult.NotFound("Chat not found: chat-1"))

        mockMvc
            .perform(
                post("/api/chat-memory/chat-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(ChatRequest("hello"))),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.detail").value("Chat not found: chat-1"))
    }
}
