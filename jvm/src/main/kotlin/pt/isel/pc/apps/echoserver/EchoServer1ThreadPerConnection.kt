package pt.isel.pc.apps.echoserver

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pt.isel.pc.utils.writeLine
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.*

private fun main() {
    EchoServer1ThreadPerConnection().run("0.0.0.0", 8080)
}

class EchoServer1ThreadPerConnection {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(EchoServer1ThreadPerConnection::class.java)
        private const val EXIT_LINE = "exit"
    }

    fun run(address: String, port: Int) {
        ServerSocket().use { serverSocket ->
            serverSocket.bind(InetSocketAddress(address, port))
            logger.info("server socket bound to {}:{}", address, port)
            acceptLoop(serverSocket)
        }
    }

    private fun acceptLoop(serverSocket: ServerSocket) {
        while (true) {
            val socket = serverSocket.accept()
            logger.info("client socket accepted, remote address is {}", socket.inetAddress.hostAddress)

            // Launching a thread to handle the new connection
            // FIXME - non-structured concurrency - How to cancel each echoLoop thread? How to know they are completed?
            val clientNo = newClientNumber()
            Thread {
                echoLoop(socket, clientNo)
            }.apply {
                start()
            }
        }
    }

    private fun echoLoop(socket: Socket, clientNo: Int) {
        var lineNo = 0
        try {
            socket.use {
                socket.getInputStream().bufferedReader().use { reader ->
                    socket.getOutputStream().bufferedWriter().use { writer ->
                        Thread.sleep(1000)
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
            }
        } catch (e: IOException) {
            logger.warn("Connection ended with IO error: {}", e.message)
        }
    }

    private var clientNoCounter = 1
    private fun newClientNumber(): Int {
        val clientNo = clientNoCounter
        clientNoCounter += 1
        return clientNo
    }
}