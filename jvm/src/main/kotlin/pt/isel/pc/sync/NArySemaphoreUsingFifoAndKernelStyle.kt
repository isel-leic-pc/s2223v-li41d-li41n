package pt.isel.pc.sync

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

/**
 * Semaphore with acquisition and release of more than one unit, using kernel-style.
 */
class NArySemaphoreUsingFifoAndKernelStyle(
    initialUnits: Int,
) {
    init {
        require(initialUnits > 0) { "Number of initial units must be greater than zero" }
    }

    private class Request(
        val requestedUnits: Int,
        val condition: Condition,
        var isDone: Boolean = false,
    )

    private var availableUnits = initialUnits
    private val queue = NodeLinkedList<Request>()
    private val lock = ReentrantLock()

    fun release(releasedUnits: Int) {
        require(releasedUnits > 0) { "releasedUnits must be greater than zero" }
        lock.withLock {
            availableUnits += releasedUnits
            completeAll()
        }
    }

    @Throws(InterruptedException::class)
    fun acquire(requestedUnits: Int, timeout: Duration): Boolean {
        require(requestedUnits > 0) { "requestedUnits must be greater than zero" }
        lock.withLock {
            if (queue.empty && availableUnits >= requestedUnits) {
                availableUnits -= requestedUnits
                return true
            }
            var remainingNanos = timeout.inWholeNanoseconds
            val localRequest = queue.enqueue(Request(requestedUnits, lock.newCondition()))
            while (true) {
                try {
                    remainingNanos = localRequest.value.condition.awaitNanos(remainingNanos)
                } catch (e: InterruptedException) {
                    if (localRequest.value.isDone) {
                        return true
                    }
                    queue.remove(localRequest)
                    completeAll()
                    throw e
                }
                if (localRequest.value.isDone) {
                    return true
                }
                if (remainingNanos <= 0) {
                    queue.remove(localRequest)
                    completeAll()
                    return false
                }
            }
        }
    }

    private fun completeAll() {
        while (queue.headCondition { availableUnits >= it.requestedUnits }) {
            val request = queue.pull().value
            availableUnits -= request.requestedUnits
            request.condition.signal()
            request.isDone = true
        }
    }
}