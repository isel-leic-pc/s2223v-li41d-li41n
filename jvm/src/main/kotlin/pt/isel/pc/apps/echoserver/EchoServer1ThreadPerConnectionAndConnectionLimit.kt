package pt.isel.pc.apps.echoserver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pt.isel.pc.utils.writeLine
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import java.util.concurrent.Semaphore

private fun main() {
    EchoServer1ThreadPerConnectionAndConnectionLimit().run("0.0.0.0", 8080, 2)
}

class EchoServer1ThreadPerConnectionAndConnectionLimit {

    companion object {
        private val logger: Logger =
            LoggerFactory.getLogger(EchoServer1ThreadPerConnectionAndConnectionLimit::class.java)
        private const val EXIT_LINE = "exit"
    }

    fun run(address: String, port: Int, maxConnections: Int) {
        val acceptPermits = Semaphore(maxConnections)
        ServerSocket().use { serverSocket ->
            serverSocket.bind(InetSocketAddress(address, port))
            logger.info("server socket bound to {}:{}", address, port)
            acceptLoop(serverSocket, acceptPermits)
        }
    }

    private fun acceptLoop(serverSocket: ServerSocket, acceptPermits: Semaphore) {
        while (true) {
            logger.info("asking permission to wait for a new connection")
            acceptPermits.acquire()
            logger.info("waiting for a new connection")
            val socket = serverSocket.accept()
            logger.info("client socket accepted, remote address is {}", socket.inetAddress.hostAddress)

            // Launching a thread to handle the new connection
            val clientNo = newClientNumber()
            Thread {
                echoLoop(socket, acceptPermits, clientNo)
            }.apply {
                start()
            }
        }
    }

    private fun echoLoop(socket: Socket, semaphore: Semaphore, clientNo: Int) = try {
        var lineNo = 0
        try {
            socket.getInputStream().bufferedReader().use { reader ->
                socket.getOutputStream().bufferedWriter().use { writer ->
                    writer.writeLine("Hi! You are client number %s", clientNo.toString())
                    while (true) {
                        val line = reader.readLine()
                        if (line == null || line == EXIT_LINE) {
                            writer.writeLine("Bye.")
                            socket.close()
                            return
                        }
                        logger.info(
                            "Received line '{}', echoing it back",
                            line
                        )
                        writer.writeLine("%d: %s", lineNo++, line.uppercase(Locale.getDefault()))
                    }
                }
            }
        } catch (e: IOException) {
            logger.warn("Connection ended with IO error: {}", e.message)
        }
    } finally {
        semaphore.release()
        socket.close()
    }

    private var clientNoCounter = 1
    private fun newClientNumber(): Int {
        val clientNo = clientNoCounter
        clientNoCounter += 1
        return clientNo
    }
}