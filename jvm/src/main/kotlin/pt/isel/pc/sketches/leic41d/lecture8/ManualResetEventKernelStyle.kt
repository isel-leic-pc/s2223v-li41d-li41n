package pt.isel.pc.sketches.leic41d.lecture8

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class ManualResetEventKernelStyle {

    private class Request(
        val condition: Condition,
        var isDone: Boolean = false,
    )

    private var value: Boolean = false
    private val lock = ReentrantLock()
    private var currentRequest = Request(condition = lock.newCondition())

    fun set() = lock.withLock {
        value = true
        currentRequest.condition.signalAll()
        currentRequest.isDone = true
        currentRequest = Request(lock.newCondition())
    }

    fun reset() = lock.withLock {
        value = false
    }

    @Throws(InterruptedException::class)
    fun waitUntilSet(timeout: Duration): Boolean {
        lock.withLock {
            // fast-path
            if (value) {
                return true
            }
            // wait-path
            var remainingNanos = timeout.inWholeNanoseconds
            val localRequest = currentRequest
            while (true) {
                remainingNanos = localRequest.condition.awaitNanos(remainingNanos)
                if (localRequest.isDone) {
                    return true
                }
                if (remainingNanos <= 0) {
                    return false
                }
            }
        }
    }
}