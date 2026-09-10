package io.vgrente.aichatwithmemory.common

import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import kotlin.test.assertEquals

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    @Test
    fun `handleChatNotFound returns 404 with the exception message as detail`() {
        val response = handler.handleChatNotFound(ChatNotFoundException("chat-1"))

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        val body = response.body as ProblemDetail
        assertEquals("Chat not found: chat-1", body.detail)
        assertEquals(404, body.status)
    }

    @Test
    fun `handleChatNotFound falls back to a default message when the exception message is null`() {
        val ex = mock<ChatNotFoundException>()
        given(ex.message).willReturn(null)

        val response = handler.handleChatNotFound(ex)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        val body = response.body as ProblemDetail
        assertEquals("No data found", body.detail)
    }

    @Test
    fun `handleUnexpected returns 500 with a generic message, not the raw exception message`() {
        val response = handler.handleUnexpected(RuntimeException("db connection refused: password=secret"))

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        val body = response.body as ProblemDetail
        assertEquals("Unexpected error", body.detail)
        assertEquals(500, body.status)
    }
}
