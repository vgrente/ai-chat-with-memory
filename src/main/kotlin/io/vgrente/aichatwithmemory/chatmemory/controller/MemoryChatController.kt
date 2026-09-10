package io.vgrente.aichatwithmemory.chatmemory.controller

import io.vgrente.aichatwithmemory.chatmemory.ChatMessage
import io.vgrente.aichatwithmemory.chatmemory.service.ChatMemoryService
import io.vgrente.aichatwithmemory.common.ApiResult
import io.vgrente.aichatwithmemory.common.ChatRequest
import io.vgrente.aichatwithmemory.common.toResponseEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/chat-memory")
class MemoryChatController(
    private val chatMemoryService: ChatMemoryService,
) {
    @GetMapping
    fun getAllChats(): ResponseEntity<Any> = chatMemoryService.getAllChats().toResponseEntity()

    @GetMapping("/{chatId}")
    fun getChatMessages(
        @PathVariable chatId: String,
    ): ResponseEntity<Any> = chatMemoryService.getMessagesByChatId(chatId).toResponseEntity()

    @PostMapping("/start")
    fun startNewChat(
        @RequestBody request: ChatRequest,
    ): ResponseEntity<Any> = chatMemoryService.createChatWithResponse(request.message).toResponseEntity()

    @PostMapping("/{chatId}")
    fun addMessageToChat(
        @PathVariable chatId: String,
        @RequestBody request: ChatRequest,
    ): ResponseEntity<Any> =
        when (val result = chatMemoryService.chat(chatId, request.message)) {
            is ApiResult.Success -> ApiResult.Success(ChatMessage(result.data, "ASSISTANT"))
            is ApiResult.NotFound, is ApiResult.Failure -> result
        }.toResponseEntity()
}
