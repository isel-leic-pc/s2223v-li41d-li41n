package pt.isel.pc.sketches.leic41d.lecture18

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

fun <T> all(inputFutures: List<CompletionStage<T>>): CompletionStage<List<T>> {
    val futureToReturn = CompletableFuture<List<T>>()
    val listToReturn: MutableList<T> = mutableListOf<T>()
    val lock = ReentrantLock()
    var isDone = false
    inputFutures.forEach { inputFuture ->
        inputFuture.handle { success: T?, error: Throwable? ->
            var maybeSuccess: List<T>? = null
            var maybeError: Throwable? = null
            lock.withLock {
                if (isDone) {
                    return@handle
                }
                if (success != null) {
                    listToReturn.add(success)
                } else {
                    requireNotNull(error)
                    maybeError = error
                    isDone = true
                    return@withLock
                }
                if (listToReturn.size == inputFutures.size) {
                    maybeSuccess = listToReturn
                    isDone = true
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