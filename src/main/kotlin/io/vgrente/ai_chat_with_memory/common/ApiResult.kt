package io.vgrente.ai_chat_with_memory.common

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class NotFound(val message: String) : ApiResult<Nothing>
    data class Failure(val error: String) : ApiResult<Nothing>
}

fun <T> ApiResult<T>.toResponseEntity(): ResponseEntity<Any> = when (this) {
    is ApiResult.Success -> ResponseEntity.ok(data)
    is ApiResult.NotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, message))
    is ApiResult.Failure -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, error))
}
