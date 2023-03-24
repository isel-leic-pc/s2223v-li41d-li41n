package pt.isel.pc.sketches.leic41n.lecture7

import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class Semaphore(
    maxUnits: Int,
) {
    private var nOfUnits = maxUnits
    private val lock: Lock = ReentrantLock()

    // condition: nOfUnits > 0
    private val condition: Condition = lock.newCondition()

    @Throws(TimeoutException::class)
    fun acquire(timeout: Duration): Unit = lock.withLock {
        // fast-path?
        if (nOfUnits > 0) {
            nOfUnits -= 1
            return
        }
        // wait-path
        var remainingNanos = timeout.inWholeNanoseconds
        while (true) {
            remainingNanos = condition.awaitNanos(remainingNanos)
            if (nOfUnits > 0) {
                nOfUnits -= 1
                return
            }
            if (remainingNanos <= 0) {
                // timeout
                throw TimeoutException()
            }
        }
    }

    fun release() = lock.withLock {
        nOfUnits += 1
        // nOfUnits > 0
        condition.signal()
    }
}