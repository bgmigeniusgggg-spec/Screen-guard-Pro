package com.guard.screen.core

/**
 * Sealed class for operation results.
 */
sealed class AppResult<out T> {

    data class Success<T>(val data: T) : AppResult<T>()

    data class Error(
        val type: ErrorType,
        val message: String,
        val cause: Throwable? = null
    ) : AppResult<Nothing>()

    object Loading : AppResult<Nothing>()

    data class Progress(
        val current: Int,
        val total: Int,
        val message: String = ""
    ) : AppResult<Nothing>()
}

enum class ErrorType {
    NETWORK,
    PERMISSION,
    STORAGE,
    CONSENT,
    TIMEOUT,
    VALIDATION,
    UNKNOWN
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onError(action: (String, ErrorType) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(message, type)
    return this
}

inline fun <T> AppResult<T>.getOrNull(): T? =
    (this as? AppResult.Success)?.data

inline fun <T> AppResult<T>.isSuccess(): Boolean = this is AppResult.Success
inline fun <T> AppResult<T>.isError(): Boolean = this is AppResult.Error
