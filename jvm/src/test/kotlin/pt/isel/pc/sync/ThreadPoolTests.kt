package pt.isel.pc.sync

import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
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
        assertTrue(sem.tryAcquire(2 * nOfThreads, 3000, TimeUnit.MILLISECONDS))
        assertEquals(nOfThreads, poolThreadIds.size)
    }

    @Test
    fun `not so simple test`() {
        val nOfThreads = 4
        val sem = Semaphore(0)
        val pool = SimpleThreadPool(nOfThreads)
        val poolThreadIds = ConcurrentHashMap<Thread, Boolean>()
        val ths = List(2 * nOfThreads) {
            thread {
                pool.execute {
                    Thread.sleep(1000)
                    poolThreadIds[Thread.currentThread()] = true
                    sem.release()
                }
            }
        }
        ths.forEach { it.join(1000) }
        assertTrue(ths.all { !it.isAlive })
        assertTrue(sem.tryAcquire(2 * nOfThreads, 3000, TimeUnit.MILLISECONDS))
        assertEquals(nOfThreads, poolThreadIds.size)
    }
}