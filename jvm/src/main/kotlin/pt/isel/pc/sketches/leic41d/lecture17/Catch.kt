package pt.isel.pc.sketches.leic41d.lecture17

import java.util.concurrent.CompletableFuture

fun <R> CompletableFuture<R>.catch(block: (Throwable) -> R): CompletableFuture<R> =
    this.handle { success: R?, error: Throwable? ->
        // require(success != null || error != null)
        if (success != null) {
            success
        } else {
            requireNotNull(error)
            block(error)
        }
    }