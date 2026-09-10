package io.vgrente.aichatwithmemory.chatmemory.repository

import io.vgrente.aichatwithmemory.common.ChatNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@JdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration::class)
class ChatMemoryIDRepositoryTest {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
    }

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var repository: ChatMemoryIDRepository

    @BeforeEach
    fun setUp() {
        repository = ChatMemoryIDRepository(jdbcTemplate)
        jdbcTemplate.update("DELETE FROM spring_ai_chat_memory")
        jdbcTemplate.update("DELETE FROM chat_memory")
    }

    @Test
    fun `createChat inserts a row with the given id`() {
        val chatId = UUID.randomUUID().toString()
        repository.createChat(chatId, "Vincent", "a new chat")

        assertTrue(repository.chatIdExists(chatId))
    }

    @Test
    fun `chatIdExists returns false for an unknown chatId`() {
        assertFalse(repository.chatIdExists("00000000-0000-0000-0000-000000000000"))
    }

    @Test
    fun `requireChatExists throws ChatNotFoundException for an unknown chatId`() {
        assertThrows<ChatNotFoundException> {
            repository.requireChatExists("00000000-0000-0000-0000-000000000000")
        }
    }

    @Test
    fun `requireChatExists does not throw for a known chatId`() {
        val chatId = UUID.randomUUID().toString()
        repository.createChat(chatId, "Vincent", "a new chat")

        repository.requireChatExists(chatId)
    }

    @Test
    fun `getAllChatsForUser returns only chats belonging to that user, newest first`() {
        val firstChatId = UUID.randomUUID().toString()
        val secondChatId = UUID.randomUUID().toString()
        jdbcTemplate.update(
            "INSERT INTO chat_memory (conversation_id, user_id, description, created_at) VALUES (?::uuid, ?, ?, ?)",
            firstChatId,
            "Vincent",
            "first chat",
            java.sql.Timestamp(0),
        )
        jdbcTemplate.update(
            "INSERT INTO chat_memory (conversation_id, user_id, description, created_at) VALUES (?::uuid, ?, ?, ?)",
            secondChatId,
            "Vincent",
            "second chat",
            java.sql.Timestamp(1000),
        )
        repository.createChat(UUID.randomUUID().toString(), "SomeoneElse", "not vincent's chat")

        val chats = repository.getAllChatsForUser("Vincent")

        assertEquals(listOf(secondChatId, firstChatId), chats.map { it.id })
    }

    @Test
    fun `getChatMessages returns an empty list when no messages exist`() {
        val chatId = UUID.randomUUID().toString()
        repository.createChat(chatId, "Vincent", "empty chat")

        val messages = repository.getChatMessages(chatId)

        assertTrue(messages.isEmpty())
    }

    @Test
    fun `getChatMessages returns messages for the chat ordered by timestamp`() {
        val chatId = UUID.randomUUID().toString()
        repository.createChat(chatId, "Vincent", "chat with messages")
        jdbcTemplate.update(
            "INSERT INTO spring_ai_chat_memory (conversation_id, content, type, \"timestamp\") VALUES (?, ?, ?, ?)",
            chatId,
            "hello",
            "USER",
            java.sql.Timestamp(0),
        )
        jdbcTemplate.update(
            "INSERT INTO spring_ai_chat_memory (conversation_id, content, type, \"timestamp\") VALUES (?, ?, ?, ?)",
            chatId,
            "hi there",
            "ASSISTANT",
            java.sql.Timestamp(1000),
        )

        val messages = repository.getChatMessages(chatId)

        assertEquals(listOf("hello" to "USER", "hi there" to "ASSISTANT"), messages.map { it.content to it.type })
    }
}
