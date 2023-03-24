package pt.isel.pc.sketches.leic41d.lecture6

import pt.isel.pc.utils.writeLine
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private fun main() {
    EchoServer().run()
}

class ThreadSafeCounter {
    private var value: Int = 0
    private val theLock: Lock = ReentrantLock()

    fun inc() = theLock.withLock {
        add(1)
    }

    fun dec() = theLock.withLock {
        add(-1)
    }

    fun add(delta: Int) = theLock.withLock {
        value += delta
    }

    fun read(): Int = theLock.withLock {
        value
    }
}

class TheSynchronizer(
    private val maximumConnections: Int,
) {
    private var nOfActiveConnections: Int = 0
    private val theLock: Lock = ReentrantLock()

    // condition: nOfActiveConnections < maximumConnections
    private val condition: Condition = theLock.newCondition()

    fun startHandlingConnection() = theLock.withLock {
        nOfActiveConnections += 1
    }

    fun endHandlingConnection() = theLock.withLock {
        nOfActiveConnections -= 1
        if (nOfActiveConnections < maximumConnections) {
            condition.signal()
        }
    }

    fun waitForActiveConnectionsBelowMaximum() = theLock.withLock {
        if (nOfActiveConnections < maximumConnections) {
            // fast-path
            return
        }
        // wait-path
        while (!(nOfActiveConnections < maximumConnections)) {
            condition.await()
        }
    }
}

class Semaphore(
    initialAvailableUnits: Int,
) {

    private var availableUnits: Int = initialAvailableUnits
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    fun acquire() = lock.withLock {
        while (availableUnits <= 0) {
            condition.await()
        }
        // availableResources > 0
        availableUnits -= 1
    }

    fun release() = lock.withLock {
        availableUnits += 1
        condition.signal()
    }
}

class EchoServer {

    private var nOfAcceptedConnections = 0
    private val nOfActiveConnections = ThreadSafeCounter()
    private val nOfHandledMessages = ThreadSafeCounter()
    private val theSynchronizer = TheSynchronizer(2)
    private val semaphore = Semaphore(2)

    fun run() {
        val serverSocket = ServerSocket()
        serverSocket.bind(InetSocketAddress(8080))
        while (true) {
            // only accept a new connection if there are NOT
            // more than N active connections
            semaphore.acquire()
            // nOfActiveConnections.read() < N
            val clientSocket = serverSocket.accept()
            nOfAcceptedConnections += 1
            val reader = clientSocket.getInputStream().bufferedReader()
            val writer = clientSocket.getOutputStream().bufferedWriter()
            val th = Thread {
                nOfActiveConnections.inc()
                theSynchronizer.startHandlingConnection()
                try {
                    var lineNumber = 0
                    writer.writeLine("Welcome client")
                    while (true) {
                        val requestLine: String? = reader.readLine()
                        if (requestLine == null) {
                            break
                        }
                        nOfHandledMessages.inc()
                        val response = "$lineNumber:${requestLine.uppercase()}"
                        lineNumber += 1
                        writer.writeLine(response)
                    }
                } finally {
                    nOfActiveConnections.dec()
                    theSynchronizer.endHandlingConnection()
                    semaphore.release()
                }
            }
            th.start()
        }
    }
}