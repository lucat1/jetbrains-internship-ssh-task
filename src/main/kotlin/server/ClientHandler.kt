package me.lucat1.sock.server

import io.klogging.Klogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import me.lucat1.sock.InvalidHeaderException
import me.lucat1.sock.Message
import me.lucat1.sock.MessageType
import me.lucat1.sock.ReaderWriter
import java.nio.channels.SocketChannel

class ClientHandler(private val socket: SocketChannel,
                    private val id: Int,
                    private val writerChan: Channel<WriterMessage>,
                    private val logger: Klogger) {
    private val rw = ReaderWriter(socket, logger)

    suspend fun handle() = withContext(Dispatchers.IO) {
        logger.info("Client connected")
        try {
            while (true) {
                // If message read is empty the client has disconnected
                val message = rw.read() ?: break
                logger.debug("Received message {type} ({contentLength}) {content}", message.header.messageType, message.header.contentLength, message.content)
                try {
                    message.header.validate()

                    when(message.header.messageType) {
                        MessageType.Ok -> {
                            // Do nothing
                        }
                        MessageType.Write -> {
                            callWriter(WriterAction.Write, message.content)
                            sendOk()
                        }
                        MessageType.Clear -> {
                            callWriter(WriterAction.Clear, message.content)
                            sendOk()
                        }
                        MessageType.Error -> {
                            sendError("Unexpected message")
                        }
                        MessageType.Ping -> {
                            sendOk()
                        }
                    }
                } catch (e: InvalidHeaderException) {
                    logger.warn("Received invalid message {type} {contentLength}", message.header.messageType, message.header.contentLength)
                    // Send an error and keep the connection alive
                    sendError("Invalid message: ${e.message}")
                }
            }
            logger.info("Client disconnected")
        } catch (e: Exception) {
            logger.error(e, "Error while handling client messages")
            sendError("Fatal error. Disconnecting")
        }
    }

    private suspend fun sendError(cause: String?) {
        rw.write(Message.checked(MessageType.Error, cause))
    }

    private suspend fun sendOk() {
        rw.write(Message.checked(MessageType.Ok, null))
    }

    private suspend fun callWriter(writerAction: WriterAction, content: String?) {
        val ackChannel = Channel<Throwable?>()
        val writerMessage = WriterMessage(writerAction, content, ackChannel)
        writerChan.send(writerMessage)
        // Wait for the acknowledgement before proceeding
        val res = ackChannel.receive()
        if (res != null)
            throw res
    }
}