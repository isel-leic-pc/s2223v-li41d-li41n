package pt.isel.pc.sketches.leic41n.lecture2

import pt.isel.pc.utils.writeLine
import java.net.InetSocketAddress
import java.net.ServerSocket

private fun main() {
    EchoServer().run()
}

class MutableId {
    private var previousId = 0

    fun newId(): Int {
        previousId += 1
        return previousId
    }
}

class EchoServer {

    fun run() {
        val serverSocket = ServerSocket()
        serverSocket.bind(InetSocketAddress(8080))
        val mutableId = MutableId()
        while (true) {
            val clientSocket = serverSocket.accept()
            val clientId = mutableId.newId()
            val th = Thread {
                val reader = clientSocket.getInputStream().bufferedReader()
                val writer = clientSocket.getOutputStream().bufferedWriter()

                writer.writeLine("Hello client: $clientId")
                var requestNo = 0
                while (true) {
                    val request: String? = reader.readLine()
                    if (request == null) {
                        break
                    }
                    val response = "${requestNo++}:${request.uppercase()}"
                    writer.writeLine(response)
                }
            }
            th.start()
        }
    }
}