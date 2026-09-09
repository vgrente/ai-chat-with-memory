package io.vgrente.ai_chat_with_memory.chatmemory.controller

import io.vgrente.ai_chat_with_memory.chatmemory.ChatMessage
import io.vgrente.ai_chat_with_memory.chatmemory.service.ChatMemoryService
import io.vgrente.ai_chat_with_memory.common.ApiResult
import io.vgrente.ai_chat_with_memory.common.ChatRequest
import io.vgrente.ai_chat_with_memory.common.toResponseEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/chat-memory")
class MemoryChatController(
    val chatMemoryService: ChatMemoryService,
) {
    @GetMapping
    fun getAllChats(): ResponseEntity<Any> {
        return chatMemoryService.getAllChats().toResponseEntity()
    }

    @GetMapping("/{chatId}")
    fun getChatMessages(@PathVariable chatId: String): ResponseEntity<Any> {
        return chatMemoryService.getMessagesByChatId(chatId).toResponseEntity()
    }

    @PostMapping("/start")
    fun startNewChat(@RequestBody request: ChatRequest): ResponseEntity<Any> {
        return chatMemoryService.createChatWithResponse(request.message).toResponseEntity()
    }

    @PostMapping("/{chatId}")
    fun addMessageToChat(@PathVariable chatId: String, @RequestBody request: ChatRequest): ResponseEntity<Any> {
        return when (val result = chatMemoryService.chat(chatId, request.message)) {
            is ApiResult.Success -> ApiResult.Success(ChatMessage(result.data, "ASSISTANT"))
            is ApiResult.NotFound, is ApiResult.Failure -> result
        }.toResponseEntity()
    }

}
