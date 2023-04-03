package pt.isel.pc.sync

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

/**
 * Manual Reset Event using a kernel-style design.
 * - [currentRequest] is early allocated to make the algorithm simpler to understand.
 */
class ManualResetEvent {
    private class Request(
        val condition: Condition,
        var isDone: Boolean = false,
    )

    private val lock = ReentrantLock()
    private var state: Boolean = false
    private var currentRequest: Request = Request(lock.newCondition())

    fun set() = lock.withLock {
        state = true
        currentRequest.condition.signalAll()
        currentRequest.isDone = true
        currentRequest = Request(lock.newCondition())
    }

    fun reset() = lock.withLock {
        state = false
    }

    fun await(timeout: Duration): Boolean {
        lock.withLock {
            if (state) {
                return true
            }
            var remainingNanos = timeout.inWholeNanoseconds
            val observedRequest = currentRequest
            while (true) {
                remainingNanos = observedRequest.condition.awaitNanos(remainingNanos)
                if (observedRequest.isDone) {
                    return true
                }
                if (remainingNanos <= 0) {
                    return false
                }
            }
        }
    }
}