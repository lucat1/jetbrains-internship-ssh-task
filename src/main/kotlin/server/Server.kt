package me.lucat1.sock.reader

import io.klogging.Level
import io.klogging.context.logContext
import io.klogging.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.lucat1.sock.ReaderWriter
import me.lucat1.sock.server.ClientHandler
import java.io.File
import java.io.IOException
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import kotlin.system.exitProcess

class Server(sock: String, output: String) {
    private val logger = logger("server")
    private val sockFile: File = File(sock)
    private val sockAddress: UnixDomainSocketAddress = UnixDomainSocketAddress.of(sockFile.path)

    private val outputFile: File = File(output)

    suspend fun run() = withContext(Dispatchers.IO) {
        try {
            Files.deleteIfExists(sockFile.toPath())
        } catch (e: Exception) {
            logger.error("Could not delete previous UNIX socket at {sockPath}", sockFile.path)
            exitProcess(5)
        }
        val channel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
        logger.info("Listening on {sockPath}, writing to {outputPath}", sockFile.path, outputFile.path)

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
                    val socket = channel.accept()
                    val clientId = clientId++

                    withContext(logContext("clientId" to clientId)) {
                        val handler = ClientHandler(socket, clientId, logger)
                        socket.run {
                            launch {
                                /*
                                 * Handle the client messages in a separate coroutine.
                                 * This way we can support multiple clients sending requests concurrently.
                                 */
                                handler.handle()
                            }
                        }
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
}