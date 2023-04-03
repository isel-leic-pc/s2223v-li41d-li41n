package pt.isel.pc.sync

import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThreadPoolTests {

    @Test
    fun `simple test`() {
        val nOfThreads = 4
        val sem = Semaphore(0)
        val pool = SimpleThreadPool(nOfThreads)
        val poolThreadIds = ConcurrentHashMap<Thread, Boolean>()
        repeat(2 * nOfThreads) {
            pool.execute {
                Thread.sleep(1000)
                poolThreadIds[Thread.currentThread()] = true
                sem.release()
            }
        }
        assertTrue(sem.tryAcquire(nOfThreads, 3000, TimeUnit.MILLISECONDS))
        assertEquals(nOfThreads, poolThreadIds.size)
    }
}