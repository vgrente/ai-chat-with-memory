package io.vgrente.aichatwithmemory.common

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ChatNotFoundException::class)
    fun handleChatNotFound(ex: ChatNotFoundException): ResponseEntity<Any> {
        log.warn(ex.message)
        return ApiResult.NotFound(ex.message ?: "No data found").toResponseEntity()
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<Any> {
        log.error("Unhandled exception", ex)
        return ApiResult.Failure("Unexpected error").toResponseEntity()
    }
}
