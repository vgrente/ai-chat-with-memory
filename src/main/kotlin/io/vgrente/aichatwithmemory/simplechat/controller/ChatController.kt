package io.vgrente.aichatwithmemory.simplechat.controller

import io.vgrente.aichatwithmemory.common.ChatRequest
import io.vgrente.aichatwithmemory.common.toResponseEntity
import io.vgrente.aichatwithmemory.simplechat.service.SimpleChatService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val chatService: SimpleChatService,
) {
    @PostMapping
    fun chat(
        @RequestBody request: ChatRequest,
    ): ResponseEntity<Any> = chatService.chat(request.message).toResponseEntity()
}
