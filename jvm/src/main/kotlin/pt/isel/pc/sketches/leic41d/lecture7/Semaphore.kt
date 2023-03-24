package pt.isel.pc.sketches.leic41d.lecture7

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class Semaphore(
    initialAvailableUnits: Int,
) {

    private var availableUnits: Int = initialAvailableUnits

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    fun acquire(timeout: Duration): Boolean {
        lock.withLock {
            // fast-path
            if (availableUnits > 0) {
                availableUnits -= 1
                return true
            }
            // wait-path
            var remainingNanos: Long = timeout.inWholeNanoseconds
            while (true) {
                remainingNanos = condition.awaitNanos(remainingNanos)
                if (remainingNanos <= 0) {
                    if (availableUnits > 0) {
                        // propagates the signal
                        condition.signal()
                    }
                    return false
                }
                if (availableUnits > 0) {
                    availableUnits -= 1
                    return true
                }
            }
        }
    }

    fun release() = lock.withLock {
        availableUnits += 1
        condition.signal()
    }
}