package me.lucat1.sock.reader

import io.klogging.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.lucat1.sock.protocol.Message
import me.lucat1.sock.protocol.MessageType
import me.lucat1.sock.protocol.ReaderWriter
import java.io.File
import java.net.StandardProtocolFamily

import java.net.UnixDomainSocketAddress
import java.nio.channels.SocketChannel

const val CLIENT_USAGE = """        2                     Append hello world to the output file
        3                     Clear the output file
        5                     Send a ping
        q                     Quit
"""

class Client(sock: String) {
    // Logger is not used for printing in the client to have some form of pretty-printing.
    // Further, the logger takes hold of the stdin, making it unusable for the interactive experience.
    private val logger = logger("client")
    private val sockFile: File = File(sock)
    private val sockAddress: UnixDomainSocketAddress = UnixDomainSocketAddress.of(sockFile.path)

    suspend fun run() = withContext(Dispatchers.IO) {
        val channel = SocketChannel.open(StandardProtocolFamily.UNIX)
        println("Connecting to ${sockFile.path}")
        channel.use { chan ->
            channel.connect(sockAddress)

            println("Usage:\n$CLIENT_USAGE")

            while (true) {
                print("> ")
                val line = readln()
                if (line.isEmpty())
                    continue

                if (line == "q") {
                    println("Disconnecting")
                    break
                }

                val rw = ReaderWriter(chan, logger)
                // Returns if we're expecting a reply to the command just sent
                val expecting = handleCommand(rw, line.split(" "))
                if (!expecting)
                    continue

                val msg = rw.read() ?: break
                msg.header.validate()
                println("< ${msg.header.messageType} (${msg.header.contentLength}) ${msg.content}")
            }

            println("Server closed")
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private suspend fun handleCommand(rw: ReaderWriter, parts: List<String>): Boolean = withContext(Dispatchers.IO) {
        try {
            val cmd = parts[0]
            val content = parts.subList(1, parts.size).joinToString(" ")
            val messageType = when (cmd) {
                "1" -> MessageType.Ok
                "2" -> MessageType.Write
                "3" -> MessageType.Clear
                "4" -> MessageType.Error
                "5" -> MessageType.Ping
                else -> throw Exception("Invalid command: $cmd")
            }

            val msg = Message(messageType, content.ifEmpty { null })
            rw.write(msg)
            return@withContext messageType != MessageType.Ok
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}