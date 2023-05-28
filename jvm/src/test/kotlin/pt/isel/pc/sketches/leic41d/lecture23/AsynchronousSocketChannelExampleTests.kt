package pt.isel.pc.sketches.leic41d.lecture23

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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AsynchronousSocketChannelExampleTests {

    @Test
    fun `connect and send bytes`() {
        runBlocking {
            val socketChannel = AsynchronousSocketChannel.open()
            socketChannel.connectSuspend(InetSocketAddress("127.0.0.1", 8080))
            logger.info("connect completed")
            val byteBuffer = ByteBuffer.allocate(1024)
            val readLen = socketChannel.readSuspend(byteBuffer)
            val s = String(byteBuffer.array(), 0, readLen)
            logger.info("read completed: '{}'", s)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AsynchronousSocketChannelExampleTests::class.java)

        suspend fun AsynchronousSocketChannel.connectSuspend(address: InetSocketAddress) {
            suspendCoroutine<Unit> { continuation ->
                this.connect(
                    address,
                    null,
                    object : CompletionHandler<Void?, Unit?> {
                        override fun completed(result: Void?, attachment: Unit?) {
                            logger.info("completed called")
                            continuation.resume(Unit)
                        }

                        override fun failed(exc: Throwable, attachment: Unit?) {
                            logger.info("failed called")
                            continuation.resumeWithException(exc)
                        }
                    }
                )
            }
        }

        suspend fun AsynchronousSocketChannel.readSuspend(byteBuffer: ByteBuffer): Int {
            return suspendCoroutine { continuation ->
                this.read(
                    byteBuffer,
                    null,
                    object : CompletionHandler<Int, Unit?> {
                        override fun completed(result: Int, attachment: Unit?) {
                            logger.info("completed called")
                            continuation.resume(result)
                        }

                        override fun failed(exc: Throwable, attachment: Unit?) {
                            logger.info("failed called")
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