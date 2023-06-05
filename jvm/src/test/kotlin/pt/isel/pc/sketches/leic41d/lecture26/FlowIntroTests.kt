package pt.isel.pc.sketches.leic41d.lecture26

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

class FlowIntroTests {

    @Test
    fun `using sequences`() {
        val seq = sequence {
            logger.info("Start")
            Thread.sleep(100)
            yield("hello")
            Thread.sleep(100)
            yield("world")
        }
        logger.info("After sequence creation")
        seq.forEach {
            logger.info("Receiving {}", it)
        }
        seq.forEach {
            logger.info("Receiving {}", it)
        }
        seq.forEach {
            logger.info("Receiving {}", it)
        }
    }

    @Test
    fun `using sequences with coroutines`() {
        val seq = sequence {
            Thread.sleep(100)
            // delay(100) - not possible to call a suspend function on a sequence's block
            yield("hello")
            Thread.sleep(100)
            yield("world")
        }

        runBlocking {
            launch {
                seq.forEach {
                    logger.info("Receiving {}", it)
                }
            }
            launch {
                seq.forEach {
                    logger.info("Receiving {}", it)
                }
            }
        }
    }

    @Test
    fun `using flows with coroutines, but still using thread sleep`() {
        val flow = flow {
            logger.info("Start")
            Thread.sleep(100)
            emit("hello")
            Thread.sleep(100)
            emit("world")
        }

        runBlocking {
            launch {
                flow.collect {
                    logger.info("Receiving {}", it)
                }
            }
            launch {
                flow.collect {
                    logger.info("Receiving {}", it)
                }
            }
        }
    }

    @Test
    fun `using flows with coroutines, but now using suspendable delay`() {
        val flow = flow {
            logger.info("Start")
            delay(100)
            emit("hello")
            delay(100)
            emit("world")
        }

        logger.info("After flow building")

        runBlocking {
            launch {
                flow.collect {
                    logger.info("Receiving {}", it)
                }
            }
            launch {
                flow.collect {
                    logger.info("Receiving {}", it)
                }
            }
        }
    }

    /**
     * A collector is _something_ that knows how to handle value send to it, in a suspendable way,
     * i.e., the processing of each value may suspend.
     * A collector works in a push-style, the values are pushed into the consumer,
     * instead of being pulled by the consumer.
     * - Push-style - producer of information calls a function of the consumer.
     * - Pull-style - consumer of information calls a function of the producer.
     */
    fun interface OurFlowCollector<T> {
        suspend fun emit(value: T): Unit
    }

    /**
     * A flow is _something_ that knows how to produce a sequence of values and push them into a collector.
     * The sequence is computed in a potentially suspendable way when collect is called.
     */
    interface OurFlow<T> {
        suspend fun collect(collector: OurFlowCollector<T>)
    }

    fun <T> ourFlow(block: suspend OurFlowCollector<T>.() -> Unit): OurFlow<T> = object : OurFlow<T> {
        override suspend fun collect(collector: OurFlowCollector<T>) {
            collector.block()
        }
    }

    @Test
    fun `using our flows`() {
        val flow = ourFlow<String> {
            logger.info("Start")
            delay(100)
            emit("hello")
            delay(100)
            emit("world")
        }

        runBlocking {
            launch {
                flow.collect {
                    logger.info("Receiving {}", it)
                }
            }
            launch {
                flow.collect {
                    logger.info("Receiving {}", it)
                }
            }
        }
    }

    @Test
    fun `using operators`() {
        val flow: Flow<String> = flow {
            logger.info("Begin")
            delay(100)
            emit("hello")
            delay(100)
            emit("world")
        }

        val upperCaseFlow: Flow<String> = flow.map {
            logger.info("mapping to uppercase")
            delay(100)
            it.uppercase()
        }

        logger.info("Before runBlocking")

        runBlocking {
            launch {
                upperCaseFlow.collect {
                    logger.info("Receiving {}", it)
                }
            }
            launch {
                upperCaseFlow.collect {
                    logger.info("Receiving {}", it)
                }
            }
        }
    }

    fun <T, R> OurFlow<T>.map(mapper: suspend (T) -> R): OurFlow<R> = object : OurFlow<R> {

        override suspend fun collect(collector: OurFlowCollector<R>) {
            this@map.collect {
                collector.emit(mapper(it))
            }
        }
    }

    @Test
    fun `using our operators`() {
        val flow = ourFlow<String> {
            delay(100)
            emit("hello")
            delay(100)
            emit("world")
        }

        val upperCaseFlow = flow.map {
            it.uppercase()
        }

        runBlocking {
            launch {
                flow.collect {
                    logger.info("Receiving {}", it)
                }
            }
            launch {
                upperCaseFlow.collect {
                    logger.info("Receiving {}", it)
                }
            }
        }
    }

    @Test
    fun `using cancellation`() {
        val flow = flow<String> {
            try {
                delay(1000)
                emit("hello")
                delay(1000)
                emit("world")
            } catch (ex: CancellationException) {
                logger.info("Caught CancellationException")
                throw ex
            }
        }

        val upperCaseFlow = flow.map {
            it.uppercase()
        }

        runBlocking {
            launch {
                withTimeout(1500) {
                    flow.collect {
                        logger.info("Receiving {}", it)
                    }
                }
            }
            launch {
                upperCaseFlow.collect {
                    logger.info("Receiving {}", it)
                }
            }
        }
    }

    @Test
    fun `using cancellation with Thread sleep`() {
        val flow = flow<String> {
            try {
                Thread.sleep(1000)
                emit("hello")
                Thread.sleep(1000)
                emit("world")
            } catch (ex: CancellationException) {
                logger.info("Caught CancellationException")
                throw ex
            }
        }

        val upperCaseFlow = flow.map {
            it.uppercase()
        }

        runBlocking {
            launch {
                withTimeout(1500) {
                    flow.collect {
                        logger.info("Receiving {}", it)
                    }
                }
            }
            launch {
                upperCaseFlow.collect {
                    logger.info("Receiving {}", it)
                }
            }
        }
    }

    @Test
    fun `using size limiting operators`() {
        val flow = flow {
            try {
                delay(1000)
                emit("hello")
                delay(1000)
                emit("world")
            } catch (ex: CancellationException) {
                logger.info("Caught CancellationException")
                throw ex
            }
        }

        val sizeLimitedFlow = flow.take(1)

        runBlocking {
            launch {
                flow.collect {
                    logger.info("Receiving {}", it)
                }
            }
            launch {
                sizeLimitedFlow.collect {
                    logger.info("Receiving {}", it)
                }
            }
        }
    }

    @Test
    fun `using flowOn`() {
        val flow = flow {
            delay(1000)
            logger.info("Flow is about to emit")
            emit("hello")
            logger.info("Flow returned from emit")
            delay(1000)
            logger.info("Flow is about to emit")
            emit("world")
            logger.info("Flow returned from emit")
        }

        val flowOnExecutorThread = flow.flowOn(dispatcher)

        runBlocking {
            launch {
                flow.collect {
                    logger.info("Receiving {}", it)
                }
            }
            launch {
                flowOnExecutorThread.collect {
                    logger.info("Receiving {}", it)
                }
            }
        }
    }

    @Test
    fun `using SharedFlow`() {
        val mutableSharedFlow = MutableSharedFlow<String>()
        val sharedFlow = mutableSharedFlow.asSharedFlow()
        val upperCaseFlow = sharedFlow.map { it.uppercase() }
        runBlocking(Dispatchers.Default) {
            val child0 = launch {
                upperCaseFlow.collect {
                    logger.info("Received {}", it)
                    delay(500)
                }
            }

            val child1 = launch {
                sharedFlow.collect {
                    logger.info("Received {}", it)
                    delay(1000)
                }
            }

            val producer = launch(dispatcher) {
                repeat(5) {
                    val line = "line-$it"
                    logger.info("Emitting {}", line)
                    mutableSharedFlow.emit(line)
                    delay(100)
                }
            }

            producer.join()
            child0.cancel()
            child1.cancel()
        }
    }

    @Test
    fun `using StateFlow`() {
        val mutableSharedFlow = MutableStateFlow<String>("start")
        val sharedFlow = mutableSharedFlow.asSharedFlow()
        val upperCaseFlow = sharedFlow.map { it.uppercase() }
        runBlocking(Dispatchers.Default) {
            val child0 = launch {
                upperCaseFlow.collect {
                    logger.info("Received {}", it)
                    delay(500)
                }
            }

            val child1 = launch {
                sharedFlow.collect {
                    logger.info("Received {}", it)
                    delay(1000)
                }
            }

            val producer = launch(dispatcher) {
                repeat(10) {
                    val line = "line-$it"
                    logger.info("Emitting {}", line)
                    mutableSharedFlow.emit(line)
                    delay(100)
                }
            }

            producer.join()
            child0.cancel()
            child1.cancel()
        }
    }

    @Test
    fun `temperature example`() {
        val mutableStateFlow = MutableStateFlow(20.0)
        val sharedStateFlow = mutableStateFlow.asSharedFlow()
        assertThrows<CancellationException> {
            runBlocking {
                // producer
                launch {
                    var temp = 20.0
                    while (true) {
                        delay(2000)
                        mutableStateFlow.emit(temp++)
                    }
                }

                // consumer
                launch {
                    withTimeout(15000) {
                        sharedStateFlow.collect {
                            logger.info("Consumer 1: {}", it)
                        }
                    }
                }

                delay(10000)
                launch {
                    sharedStateFlow.collect {
                        logger.info("Consumer 2: {}", it)
                    }
                }

                delay(20000)
                cancel()
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FlowIntroTests::class.java)
        private val dispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
    }
}