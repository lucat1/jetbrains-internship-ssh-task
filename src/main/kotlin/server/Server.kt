package me.lucat1.sock.reader

import kotlinx.coroutines.*
import me.lucat1.sock.ReaderWriter

import java.io.IOException
import java.io.File
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import kotlin.system.exitProcess

class Server(sock: String, output: String) {
    private val sockFile: File = File(sock)
    private val sockAddress: UnixDomainSocketAddress = UnixDomainSocketAddress.of(sockFile.path)

    private val outputFile: File = File(output)

    fun run() {
        try {
            Files.deleteIfExists(sockFile.toPath())
        } catch (e: Exception) {
            println("Could not delete previous UNIX socket at ${sockFile.path}")
            e.printStackTrace()
            exitProcess(5)
        }
        val channel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
        println("Listening on ${sockFile.path}, writing to ${outputFile.path}")

        runBlocking {
            /*
             * One coroutine is spawned to handle the socket and the creation of further
             * coroutines for client connections. These are the "producer".
             */
            launch {
                bind(channel)
            }

            /*
             * Another coroutine is launched to receive commands from the "producers" and
             * apply changes to the file. This is the "consumer".
             *
             * This way, we can have multiple clients sending concurrent requests, which will
             * be streamlined into a channel and handled one-by-one sequentially by the "consumer".
             */
            launch {
                writer()
            }
        }
    }

    private suspend fun bind(channel: ServerSocketChannel) = withContext(Dispatchers.IO) {
        var clientId = 0
        try {
            channel.bind(sockAddress).run {
                while (true) {
                    val client = channel.accept()
                    launch {
                        /*
                         * Handle the client messages in a separate coroutine.
                         * This way we can support multiple clients sending requests concurrently.
                         */
                        handleClient(client, clientId++)
                    }
                }
            }
        } catch (e: IOException) {
            println("Could not bind to UNIX socket on ${sockFile.path}")
            e.printStackTrace()
            exitProcess(6)
        }
    }

    private suspend fun writer() = withContext(Dispatchers.IO) {
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private suspend fun handleClient(client: SocketChannel, id: Int) = withContext(Dispatchers.IO) {
        try {
            println("Client connected #$id")
            val rw = ReaderWriter(client)
            while (true) {
                val message = rw.read()
                if (message == null)
                    break
                println("Got message: ${message.header.messageType} (${message.header.contentLength}) \"${message.content ?: ""}\"")
            }
            println("Client #$id disconnected")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}