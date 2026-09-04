package io.vgrente.ai_chat_with_memory.controller

import io.vgrente.ai_chat_with_memory.service.SimpleChatService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController



data class ChatRequest(val message: String)
data class ChatResponse(val message: String)

@RestController
@RequestMapping("/api/chat")
class ChatController(private final val chatService: SimpleChatService) {

    @PostMapping
    fun chat(@RequestBody request: ChatRequest): ChatResponse {
        return ChatResponse(chatService.chat(request.message))
    }


}