package pt.isel.pc.sync

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

/**
 * Semaphore with acquisition and release of more than one unit, using monitor-style.
 */
class NArySemaphoreUsingFifo(
    initialUnits: Int,
) {
    init {
        require(initialUnits > 0) { "Number of initial units must be greater than zero" }
    }

    data class Request(val requestedUnits: Int)

    private var availableUnits = initialUnits
    private val queue = NodeLinkedList<Request>()
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    fun release(releasedUnits: Int) {
        require(releasedUnits > 0) { "releasedUnits must be greater than zero" }
        lock.withLock {
            availableUnits += releasedUnits
            signalIfNeeded()
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
            val localRequest = queue.enqueue(Request(requestedUnits))
            while (true) {
                try {
                    remainingNanos = condition.awaitNanos(remainingNanos)
                } catch (e: InterruptedException) {
                    queue.remove(localRequest)
                    signalIfNeeded()
                    throw e
                }
                if (queue.isHeadNode(localRequest) && availableUnits >= requestedUnits) {
                    queue.remove(localRequest)
                    availableUnits -= requestedUnits
                    signalIfNeeded()
                    return true
                }
                if (remainingNanos <= 0) {
                    queue.remove(localRequest)
                    signalIfNeeded()
                    return false
                }
            }
        }
    }

    private fun signalIfNeeded() {
        if (queue.headCondition { availableUnits >= it.requestedUnits }) {
            condition.signalAll()
        }
    }
}