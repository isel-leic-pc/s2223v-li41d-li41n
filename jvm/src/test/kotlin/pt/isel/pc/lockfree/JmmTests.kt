package pt.isel.pc.lockfree

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import pt.isel.pc.utils.TestHelper
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.seconds

class JmmTests {

    private var nonVolatileBoolean: Boolean = false

    @Volatile
    private var volatileBoolean: Boolean = false

    @Test
    fun `loop checking non-volatile shared variable`() {
        val th = thread(isDaemon = true) {
            while (!nonVolatileBoolean) {
                // nothing
            }
        }
        TimeUnit.SECONDS.sleep(2)
        nonVolatileBoolean = true
        th.join(2_000)
        assertNotEquals(Thread.State.TERMINATED, th.state)
    }

    @Test
    fun `loop checking volatile shared variable`() {
        val th = thread(isDaemon = true) {
            while (!volatileBoolean) {
                // nothing
            }
        }
        TimeUnit.SECONDS.sleep(2)
        volatileBoolean = true
        th.join(2_000)
        assertEquals(Thread.State.TERMINATED, th.state)
    }

    class A {
        var field: String = "hello"
    }

    class B {
        val finalField: String? = "hello"
    }

    companion object {

        private val logger = LoggerFactory.getLogger(JmmTests::class.java)

        private var nonVolatileShared: A? = null

        @Volatile
        private var volatileShared: A? = null

        private var nonVolatileSharedB: B? = null
    }

    @Test
    fun `may observe non-initialized field on shared non-volatile reference`() {
        val testHelper = TestHelper(3.seconds)
        val nullObservations = AtomicInteger()
        testHelper.createAndStartMultiple(1) { _, isDone ->
            while (!isDone()) {
                nonVolatileShared = A()
            }
        }
        testHelper.createAndStartMultiple(4) { _, isDone ->
            while (!isDone()) {
                val observed = nonVolatileShared
                if (observed != null) {
                    if (observed.field == null) {
                        nullObservations.incrementAndGet()
                    }
                }
            }
        }
        testHelper.join()
        logger.info("null observations: {}", nullObservations.get())
    }

    @Test
    fun `may NOT observe non-initialized field on shared volatile reference`() {
        val testHelper = TestHelper(5.seconds)
        val nullObservations = AtomicInteger()
        testHelper.createAndStartMultiple(1) { _, isDone ->
            while (!isDone()) {
                volatileShared = A()
            }
        }
        testHelper.createAndStartMultiple(4) { _, isDone ->
            while (!isDone()) {
                val observed = volatileShared
                if (observed != null) {
                    if (observed.field == null) {
                        nullObservations.incrementAndGet()
                    }
                }
            }
        }
        testHelper.join()
        logger.info("null observations: {}", nullObservations.get())
        assertEquals(0, nullObservations.get())
    }

    @Test
    fun `may NOT observe non-initialized final field on shared non-volatile reference`() {
        val testHelper = TestHelper(5.seconds)
        val nullObservations = AtomicInteger()
        testHelper.createAndStartMultiple(1) { _, isDone ->
            while (!isDone()) {
                nonVolatileSharedB = B()
            }
        }
        testHelper.createAndStartMultiple(4) { _, isDone ->
            while (!isDone()) {
                val observed = nonVolatileSharedB
                if (observed != null) {
                    if (observed.finalField == null) {
                        nullObservations.incrementAndGet()
                    }
                }
            }
        }
        testHelper.join()
        logger.info("null observations: {}", nullObservations.get())
        assertEquals(0, nullObservations.get())
    }
}