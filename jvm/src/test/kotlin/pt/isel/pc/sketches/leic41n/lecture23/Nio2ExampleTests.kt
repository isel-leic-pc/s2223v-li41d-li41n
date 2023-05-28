package pt.isel.pc.sketches.leic41n.lecture23

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.Semaphore
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class Nio2ExampleTests {

    @Test
    fun first() {
        val sem = Semaphore(0)
        val socket = AsynchronousSocketChannel.open()
        socket.connect(
            InetSocketAddress("127.0.0.1", 8080),
            null,
            object : CompletionHandler<Void, Any?> {
                override fun completed(result: Void?, attachment: Any?) {
                    logger.info("connect completed")

                    sem.release()
                }

                override fun failed(exc: Throwable?, attachment: Any?) {
                    logger.info("connect failed")
                    sem.release()
                }
            }
        )
        logger.info("after connect")
        // read?
        sem.acquire()
        logger.info("test ending")
    }

    @Test
    fun second() {
        runBlocking {
            val socket = AsynchronousSocketChannel.open()
            logger.info("Before connectSuspend")
            socket.connectSuspend(InetSocketAddress("127.0.0.1", 8080))
            logger.info("After connectSuspend")
            val buffer = ByteBuffer.allocate(1024)
            val readLen = socket.readSuspend(buffer)
            val s = String(buffer.array(), 0, readLen)
            logger.info("After readSuspend with received string = '{}'", s)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Nio2ExampleTests::class.java)

        suspend fun AsynchronousSocketChannel.connectSuspend(address: InetSocketAddress) {
            suspendCoroutine<Unit> { continuation ->
                this.connect(
                    address,
                    null,
                    object : CompletionHandler<Void?, Any?> {
                        override fun completed(result: Void?, attachment: Any?) {
                            logger.info("Connection CH completed")
                            continuation.resume(Unit)
                        }

                        override fun failed(exc: Throwable, attachment: Any?) {
                            logger.info("Connection CH failed")
                            continuation.resumeWithException(exc)
                        }
                    }
                )
            }
        }

        suspend fun AsynchronousSocketChannel.readSuspend(byteBuffer: ByteBuffer): Int {
            return suspendCoroutine<Int> { continuation ->
                this.read(
                    byteBuffer,
                    null,
                    object : CompletionHandler<Int, Any?> {
                        override fun completed(result: Int, attachment: Any?) {
                            logger.info("Read CH completed")
                            continuation.resume(result)
                        }

                        override fun failed(exc: Throwable, attachment: Any?) {
                            logger.info("Read CH failed")
                            continuation.resumeWithException(exc)
                        }
                    }
                )
            }
        }

        @BeforeAll
        @JvmStatic
        fun checkRequirements() {
            Assumptions.assumeTrue(
                {
                    try {
                        Socket().connect(InetSocketAddress("127.0.0.1", 8080))
                        true
                    } catch (ex: IOException) {
                        false
                    }
                },
                "Requires listening echo server"
            )
        }
    }
}