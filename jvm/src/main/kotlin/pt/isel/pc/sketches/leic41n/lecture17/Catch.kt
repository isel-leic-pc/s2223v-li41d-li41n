package pt.isel.pc.sketches.leic41n.lecture17

import java.util.concurrent.CompletionStage

fun <R> CompletionStage<R>.catch(block: (Throwable) -> R): CompletionStage<R> =
    this.handle { success: R?, error: Throwable? ->
        // require(success != null || error != null)
        if (success != null) {
            success
        } else {
            requireNotNull(error)
            block(error)
        }
    }