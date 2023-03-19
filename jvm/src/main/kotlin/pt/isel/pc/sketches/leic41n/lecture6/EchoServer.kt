package pt.isel.pc.sketches.leic41n.lecture6

import pt.isel.pc.utils.writeLine
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private fun main() {
    EchoServer().run()
}

class ThreadSafeCounter {
    private var value: Int = 0
    private val lock: Lock = ReentrantLock()

    fun inc() = lock.withLock {
        add(1)
    }

    fun dec() = lock.withLock {
        add(-1)
    }

    fun read() = lock.withLock {
        value
    }

    fun add(delta: Int) = lock.withLock {
        value += delta
    }
}

// The synchronizer
class Semaphore(
    maxUnits: Int
) {
    private var nOfUnits = maxUnits
    private val lock: Lock = ReentrantLock()

    // condition: nOfUnits > 0
    private val condition: Condition = lock.newCondition()

    @Throws(TimeoutException::class)
    fun acquire(timeout: Duration) = lock.withLock {
        // fast-path?
        if (nOfUnits > 0) {
            nOfUnits -= 1
            return@withLock
        }
        var remainingNanos = timeout.inWholeNanoseconds
        while (true) {
            remainingNanos = condition.awaitNanos(remainingNanos)
            if (nOfUnits > 0) {
                nOfUnits -= 1
                return@withLock
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

class EchoServer {

    private var nOfAcceptedConnections: Int = 0
    private var nOfActiveConnections = ThreadSafeCounter()
    private var nOfHandledMessages = ThreadSafeCounter()

    private val semaphore = Semaphore(10)

    fun run() {
        val serverSocket = ServerSocket()
        serverSocket.bind(InetSocketAddress(8080))
        while (true) {
            // Only accept a new connection if the number of active
            // connection is less than the maximum
            // wait
            // FIXME handle timeout
            semaphore.acquire(5.seconds)
            val clientSocket = serverSocket.accept()
            nOfAcceptedConnections += 1
            val reader = clientSocket.getInputStream().bufferedReader()
            val writer = clientSocket.getOutputStream().bufferedWriter()
            nOfActiveConnections.inc()
            val th = Thread {
                try {
                    var lineNumber = 0
                    writer.writeLine("Welcome client ")
                    while (true) {
                        val requestLine: String? = reader.readLine()
                        if (requestLine == null) {
                            break
                        }
                        val response = "$lineNumber:${requestLine.uppercase()}"
                        lineNumber += 1
                        writer.writeLine(response)
                        nOfHandledMessages.inc()
                    }
                } finally {
                    nOfActiveConnections.dec()
                    semaphore.release()
                }
            }
            th.start()
        }
    }
}