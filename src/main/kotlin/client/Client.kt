package me.lucat1.sock.reader

import me.lucat1.sock.Message
import me.lucat1.sock.MessageType
import me.lucat1.sock.ReaderWriter
import java.io.File
import java.net.StandardProtocolFamily

import java.net.UnixDomainSocketAddress
import java.nio.channels.SocketChannel

const val CLIENT_USAGE = """        2 "hello world"       Append hello world to the output file
        3                     Clear the output file
        5                     Send a ping
        q                     Quit
"""

class Client(sock: String) {
    private val sockFile: File = File(sock)
    private val sockAddress: UnixDomainSocketAddress = UnixDomainSocketAddress.of(sockFile.path)

    fun run() {
        val channel = SocketChannel.open(StandardProtocolFamily.UNIX)
        println("Connecting to ${sockFile.path}")
        channel.use { chan ->
            channel.connect(sockAddress)

            println("Usage:\n$CLIENT_USAGE")

            while (true) {
                val line = readln()
                if (line.isEmpty())
                    continue

                if (line == "q") {
                    println("Disconnecting")
                    break
                }

                handleCommand(chan, line.split(" "))
            }
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun handleCommand(chan: SocketChannel, parts: List<String>) {
        try {
            val cmd = parts[0]
            val arg = if (parts.size > 1) parts[1] else null
            val rw = ReaderWriter(chan)
            when (cmd) {
                "2" -> {
                    if (arg != null) {
                        val msg = Message(MessageType.Write, arg)
                        rw.write(msg)
                    } else {
                        println("Missing argument for: $cmd <content>")
                    }
                }

                else -> {
                    println("Unrecognised command: $cmd")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}