package pt.isel.pc.sketches.leic41d.lecture8

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class ManualResetEvent {

    private var value: Boolean = false
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    fun set() = lock.withLock {
        value = true
        condition.signalAll()
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
            while (true) {
                remainingNanos = condition.awaitNanos(remainingNanos)
                if (value) {
                    return true
                }
                if (remainingNanos <= 0) {
                    return false
                }
            }
        }
    }
}