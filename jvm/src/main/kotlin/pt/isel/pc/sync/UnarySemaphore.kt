package pt.isel.pc.sync

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

/**
 * Unary semaphore using a monitor-style design.
 */
class UnarySemaphore(
    initialUnits: Int,
) {
    init {
        require(initialUnits > 0) { "Number of initial units must be greater than zero" }
    }

    private var availableUnits = initialUnits
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    fun release() = lock.withLock {
        availableUnits += 1
        condition.signal()
    }

    @Throws(InterruptedException::class)
    fun acquire(timeout: Duration): Boolean = lock.withLock {
        var remainingNanos = timeout.inWholeNanoseconds
        while (availableUnits <= 0) {
            if (remainingNanos <= 0) {
                // No need to propagate signal because `availableUnits <= 0` is true
                return false
            }
            try {
                remainingNanos = condition.awaitNanos(remainingNanos)
            } catch (e: InterruptedException) {
                /*
                 * Not needed really needed due to
                 *  "An implementation can favor responding to an interrupt over normal method return in response
                 *  to a signal. In that case the implementation must ensure that the signal is redirected to another
                 *  waiting thread, if there is one."
                 * In https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/locks/Condition.html#awaitNanos(long)
                 *
                 */
                if (availableUnits > 0) {
                    condition.signal()
                }
                throw e
            }
        }
        availableUnits -= 1
        return true
    }
}