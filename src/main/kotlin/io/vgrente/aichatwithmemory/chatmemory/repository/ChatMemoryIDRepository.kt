package io.vgrente.aichatwithmemory.chatmemory.repository

import io.vgrente.aichatwithmemory.chatmemory.Chat
import io.vgrente.aichatwithmemory.chatmemory.ChatMessage
import io.vgrente.aichatwithmemory.common.ChatNotFoundException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ChatMemoryIDRepository(
    val jdbcTemplate: JdbcTemplate,
) {
    fun requireChatExists(chatId: String) {
        if (!chatIdExists(chatId)) throw ChatNotFoundException(chatId)
    }

    fun createChat(
        chatId: String,
        userId: String,
        description: String,
    ) {
        val sql = "INSERT INTO chat_memory (conversation_id, user_id, description) VALUES (?::uuid, ?, ?)"
        jdbcTemplate.update(sql, chatId, userId, description)
    }

    fun chatIdExists(chatId: String): Boolean {
        val sql = "SELECT COUNT(*) FROM chat_memory WHERE conversation_id = ?::uuid"
        return jdbcTemplate.queryForObject(sql, Int::class.java, chatId) != 0
    }

    fun getAllChatsForUser(userId: String): List<Chat> {
        val sql = "SELECT conversation_id, description FROM chat_memory WHERE user_id = ? ORDER BY created_at DESC"
        return jdbcTemplate.query(
            sql,
            { rs, _ -> Chat(rs.getString("conversation_id"), rs.getString("description")) },
            userId,
        )
    }

    fun getChatMessages(chatId: String): List<ChatMessage> {
        val sql = "SELECT content, type FROM spring_ai_chat_memory WHERE conversation_id = ? ORDER BY timestamp ASC"
        return jdbcTemplate.query(
            sql,
            { rs, _ -> ChatMessage(rs.getString("content"), rs.getString("type")) },
            chatId,
        )
    }
}
