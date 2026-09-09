package io.vgrente.ai_chat_with_memory.simplechat.controller

import io.vgrente.ai_chat_with_memory.common.ChatRequest
import io.vgrente.ai_chat_with_memory.common.toResponseEntity
import io.vgrente.ai_chat_with_memory.simplechat.service.SimpleChatService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/chat")

class ChatController(private final val chatService: SimpleChatService) {


    @PostMapping
    fun chat(@RequestBody request: ChatRequest): ResponseEntity<Any> {
        return chatService.chat(request.message).toResponseEntity()
    }
}
