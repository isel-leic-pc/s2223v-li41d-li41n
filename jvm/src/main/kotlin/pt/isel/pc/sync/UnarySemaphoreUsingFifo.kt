package pt.isel.pc.sync

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

/**
 * Semaphore using a monitor-style design, providing fairness by using a Firt In First Out policy to grant units.
 */
class UnarySemaphoreUsingFifo(
    initialUnits: Int,
) {
    init {
        require(initialUnits > 0) { "Number of initial units must be greater than zero" }
    }

    private var availableUnits = initialUnits
    private val queue = NodeLinkedList<Unit>()
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    fun release() = lock.withLock {
        availableUnits += 1
        signalIfNeeded()
    }

    @Throws(InterruptedException::class)
    fun acquire(timeout: Duration): Boolean {
        lock.withLock {
            if (queue.empty && availableUnits > 0) {
                availableUnits -= 1
                return true
            }
            var remainingNanos = timeout.inWholeNanoseconds
            val localRequest = queue.enqueue(Unit)
            while (true) {
                try {
                    remainingNanos = condition.awaitNanos(remainingNanos)
                } catch (e: InterruptedException) {
                    queue.remove(localRequest)
                    signalIfNeeded()
                    throw e
                }
                if (queue.isHeadNode(localRequest) && availableUnits > 0) {
                    queue.remove(localRequest)
                    availableUnits -= 1
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
        if (queue.notEmpty && availableUnits > 0) {
            condition.signalAll()
        }
    }
}