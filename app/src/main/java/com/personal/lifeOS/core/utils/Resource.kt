package com.personal.lifeOS.core.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Sealed class for wrapping results with error handling.
 */
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()

    data class Error(val message: String, val exception: Throwable? = null) : Resource<Nothing>()

    data object Loading : Resource<Nothing>()
}

/**
 * Extension to safely execute a suspend block and wrap in Resource.
 */
suspend fun <T> safeCall(block: suspend () -> T): Resource<T> {
    return try {
        Resource.Success(block())
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Unknown error", e)
    }
}

/**
 * Extension to wrap a Flow in Resource.
 */
fun <T> Flow<T>.asResource(): Flow<Resource<T>> {
    return this
        .map<T, Resource<T>> { Resource.Success(it) }
        .catch { emit(Resource.Error(it.localizedMessage ?: "Unknown error", it)) }
}
