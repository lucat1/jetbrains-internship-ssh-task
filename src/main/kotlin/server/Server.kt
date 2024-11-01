package me.lucat1.sock.server

import io.klogging.context.logContext
import io.klogging.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.ServerSocketChannel
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

class Server(sock: String, output: String) {
    private val logger = logger("server")

    private val sockFile: File = File(sock)
    private val sockAddress: UnixDomainSocketAddress = UnixDomainSocketAddress.of(sockFile.path)

    private val outputPath: Path = Path(output)

    private val writerChannel = Channel<WriterMessage>()

    suspend fun run() = withContext(Dispatchers.IO) {
        try {
            Files.deleteIfExists(sockFile.toPath())
        } catch (e: Exception) {
            logger.fatal("Could not delete previous UNIX socket at {sockPath}", sockFile.path)
        }
        val channel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
        logger.info("Listening on {sockPath}, writing to {outputPath}", sockFile.path, outputPath)

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
                Writer(outputPath, writerChannel, logger).write()
            }
        }
    }

    /*
     * Binds to the socket channel and listens for incoming connections.
     * When one is received, a new ClientHandler is instantiated to handle it
     * and gets executed in a separate coroutine.
     */
    private suspend fun bind(channel: ServerSocketChannel) = withContext(Dispatchers.IO) {
        var clientId = 0
        try {
            channel.bind(sockAddress).run {
                while (true) {
                    val socket = channel.accept()
                    val cid = clientId++

                    withContext(logContext("clientId" to cid)) {
                        launch {
                            /*
                             * Handle the client messages in a separate coroutine.
                             * This way we can support multiple clients sending requests concurrently.
                             */
                            socket.run {
                                ClientHandler(socket, writerChannel, logger).handle()
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            logger.fatal(e, "Could not bind to UNIX socket on {sockPath}", sockFile.path)
        }
    }
}