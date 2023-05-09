package pt.isel.pc.sketches.leic41n.lecture18

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

fun <T> all(inputFutures: List<CompletionStage<T>>): CompletionStage<List<T>> {
    val futureToReturn = CompletableFuture<List<T>>()
    val resultList = mutableListOf<T>()
    val lock = ReentrantLock()
    var isCompleted = false
    inputFutures.forEach { inputFuture ->
        inputFuture.handle { success: T?, error: Throwable? ->
            var maybeSuccess: List<T>? = null
            var maybeError: Throwable? = null
            lock.withLock {
                if (isCompleted) {
                    return@handle
                }
                if (success != null) {
                    resultList.add(success)
                } else {
                    requireNotNull(error)
                    maybeError = error
                    isCompleted = true
                }
                if (resultList.size == inputFutures.size) {
                    maybeSuccess = resultList
                    isCompleted = true
                }
            }
            if (maybeSuccess != null) {
                futureToReturn.complete(maybeSuccess)
            } else if (maybeError != null) {
                futureToReturn.completeExceptionally(maybeError)
            }
        }
    }

    return futureToReturn
}