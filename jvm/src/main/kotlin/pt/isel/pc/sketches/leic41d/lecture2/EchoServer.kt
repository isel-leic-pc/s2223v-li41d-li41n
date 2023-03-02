package pt.isel.pc.sketches.leic41d.lecture2

import pt.isel.pc.utils.writeLine
import java.net.InetSocketAddress
import java.net.ServerSocket

private fun main() {
    EchoServer().run()
}

class Counter {
    private var value = 0

    fun inc(): Int {
        value += 1
        return value
    }
}

class EchoServer {

    fun run() {
        val serverSocket = ServerSocket()
        serverSocket.bind(InetSocketAddress(8080))
        val clientId = Counter()
        while (true) {
            val clientSocket = serverSocket.accept()
            val reader = clientSocket.getInputStream().bufferedReader()
            val writer = clientSocket.getOutputStream().bufferedWriter()
            val newClientId = clientId.inc()
            val th = Thread {
                var lineNumber = 0
                writer.writeLine("Welcome client $newClientId")
                while (true) {
                    val requestLine: String? = reader.readLine()
                    if (requestLine == null) {
                        break
                    }
                    val response = "$lineNumber:${requestLine.uppercase()}"
                    lineNumber += 1
                    writer.writeLine(response)
                }
            }
            th.start()
        }
    }
}