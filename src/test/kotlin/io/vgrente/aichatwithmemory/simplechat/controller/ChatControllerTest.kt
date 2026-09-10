package io.vgrente.aichatwithmemory.simplechat.controller

import io.vgrente.aichatwithmemory.common.ApiResult
import io.vgrente.aichatwithmemory.common.ChatRequest
import io.vgrente.aichatwithmemory.simplechat.ChatResponse
import io.vgrente.aichatwithmemory.simplechat.service.SimpleChatService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

@WebMvcTest(ChatController::class)
class ChatControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var chatService: SimpleChatService

    @Test
    fun `POST api-chat returns the service response`() {
        given(chatService.chat("hello")).willReturn(ApiResult.Success(ChatResponse("hi there")))

        mockMvc
            .perform(
                post("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(ChatRequest("hello"))),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("hi there"))
    }

    @Test
    fun `POST api-chat with blank message still calls the service`() {
        given(chatService.chat("")).willReturn(ApiResult.Success(ChatResponse("no input given")))

        mockMvc
            .perform(
                post("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(ChatRequest(""))),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("no input given"))
    }

    @Test
    fun `POST api-chat with malformed JSON returns 400`() {
        mockMvc
            .perform(
                post("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("not json"),
            ).andExpect(status().isBadRequest)
    }
}
