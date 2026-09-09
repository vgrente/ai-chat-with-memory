package io.vgrente.ai_chat_with_memory

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.modulith.Modulithic

@Modulithic
@SpringBootApplication
class AiChatWithMemoryApplication

fun main(args: Array<String>) {
	runApplication<AiChatWithMemoryApplication>(*args)
}
