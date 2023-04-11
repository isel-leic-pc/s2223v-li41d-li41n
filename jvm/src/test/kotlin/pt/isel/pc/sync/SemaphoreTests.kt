package pt.isel.pc.sync

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.slf4j.LoggerFactory
import pt.isel.pc.utils.TestHelper
import pt.isel.pc.utils.spinUntilTimedWait
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream
import kotlin.random.Random
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SemaphoreTests {

    @Test
    fun `interrupt test`() {
        val sem = NArySemaphoreUsingFifoAndKernelStyle(3)
        val testHelper = TestHelper(10.seconds)
        val th1 = testHelper.thread {
            assertThrows<InterruptedException> { sem.acquire(4, INFINITE) }
        }
        spinUntilTimedWait(th1)
        val th2 = testHelper.thread {
            assertTrue(sem.acquire(3, INFINITE))
        }
        spinUntilTimedWait(th2)
        th1.interrupt()
        testHelper.join()
    }

    @Test
    fun `timeout test`() {
        val initialUnits = 5
        val sem = NArySemaphoreUsingFifoAndKernelStyle(initialUnits)
        val testHelper = TestHelper(10.seconds)
        (initialUnits + 1 downTo 1).forEach {
            val timeout = it.seconds
            logger.info("starting thread requesting {} units with {} timeout", it, timeout)
            val th = testHelper.thread {
                assertFalse { sem.acquire(it, timeout) }
            }
            spinUntilTimedWait(th)
        }
        testHelper.thread {
            assertTrue { sem.acquire(initialUnits, INFINITE) }
        }
        testHelper.join()
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("provideUnaryAcquireAndReleaseWithoutTimeout")
    fun `stress test unary semaphore`(
        name: String,
        acquire: () -> Unit,
        release: () -> Unit,
    ) {
        val units = AtomicInteger(INITIAL_UNITS)
        val testHelper = TestHelper(3.seconds)
        testHelper.createAndStartMultiple(N_OF_THREADS) { _, isDone ->
            while (!isDone()) {
                acquire()
                val observedUnits = units.decrementAndGet()
                assertTrue(observedUnits >= 0)
                Thread.yield()
                units.incrementAndGet()
                release()
            }
        }
        testHelper.join()
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("provideNAryAcquireAndReleaseWithoutTimeout")
    fun `stress test nary semaphore`(
        name: String,
        acquire: (Int) -> Unit,
        release: (Int) -> Unit,
    ) {
        val units = AtomicInteger(INITIAL_UNITS)
        val testHelper = TestHelper(5.seconds)
        testHelper.createAndStartMultiple(N_OF_THREADS) { _, isDone ->
            val random = Random.Default
            while (!isDone()) {
                val unitsToAcquire = random.nextInt(1, INITIAL_UNITS + 1)
                acquire(unitsToAcquire)
                val observedUnits = units.addAndGet(-unitsToAcquire)
                assertTrue(observedUnits >= 0)
                units.addAndGet(unitsToAcquire)
                release(unitsToAcquire)
            }
        }
        testHelper.join()
    }

    companion object {
        private const val N_OF_THREADS = 24
        private const val INITIAL_UNITS = N_OF_THREADS / 2
        private val INFINITE = Duration.INFINITE

        private val logger = LoggerFactory.getLogger(SemaphoreTests::class.java)

        @JvmStatic
        fun provideUnaryAcquireAndReleaseWithoutTimeout(): Stream<Arguments> {
            val unarySemaphore = UnarySemaphore(INITIAL_UNITS)
            val unarySemaphoreUsingFifo = UnarySemaphoreUsingFifo(INITIAL_UNITS)
            val narySemaphoreUsingFifo = NArySemaphoreUsingFifo(INITIAL_UNITS)
            val nArySemaphoreUsingFifoAndKernelStyle = NArySemaphoreUsingFifoAndKernelStyle(INITIAL_UNITS)
            return Stream.of(
                Arguments.of(
                    "Using UnarySemaphore",
                    { unarySemaphore.acquire(INFINITE) },
                    unarySemaphore::release
                ),
                Arguments.of(
                    "Using UnarySemaphoreUsingFifo",
                    { unarySemaphoreUsingFifo.acquire(INFINITE) },
                    unarySemaphoreUsingFifo::release
                ),
                Arguments.of(
                    "Using NArySemaphoreUsingFifo",
                    { narySemaphoreUsingFifo.acquire(1, INFINITE) },
                    { narySemaphoreUsingFifo.release(1) }
                ),
                Arguments.of(
                    "Using NArySemaphoreUsingFifoAndKernelStyle",
                    { nArySemaphoreUsingFifoAndKernelStyle.acquire(1, INFINITE) },
                    { nArySemaphoreUsingFifoAndKernelStyle.release(1) }
                )
            )
        }

        @JvmStatic
        fun provideNAryAcquireAndReleaseWithoutTimeout(): Stream<Arguments> {
            val narySemaphoreUsingFifo = NArySemaphoreUsingFifo(INITIAL_UNITS)
            val nArySemaphoreUsingFifoAndKernelStyle = NArySemaphoreUsingFifoAndKernelStyle(INITIAL_UNITS)
            return Stream.of(
                Arguments.of(
                    "Using NArySemaphoreUsingFifo",
                    { units: Int -> narySemaphoreUsingFifo.acquire(units, INFINITE) },
                    { units: Int -> narySemaphoreUsingFifo.release(units) }
                ),
                Arguments.of(
                    "Using NArySemaphoreUsingFifoAndKernelStyle",
                    { units: Int -> nArySemaphoreUsingFifoAndKernelStyle.acquire(units, INFINITE) },
                    { units: Int -> nArySemaphoreUsingFifoAndKernelStyle.release(units) }
                )
            )
        }
    }
}