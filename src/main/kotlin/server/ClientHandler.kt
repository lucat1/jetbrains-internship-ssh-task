package me.lucat1.sock.server

import io.klogging.Klogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import me.lucat1.sock.protocol.InvalidHeaderException
import me.lucat1.sock.protocol.Message
import me.lucat1.sock.protocol.MessageType
import me.lucat1.sock.protocol.ReaderWriter
import java.nio.channels.SocketChannel

class ClientHandler(socket: SocketChannel,
                    private val writerChan: Channel<WriterMessage>,
                    private val logger: Klogger) {
    private val rw = ReaderWriter(socket, logger)

    /*
     * Handles a client connection. Executes the following logic:
     * 1. Loop until we have messages to read in:
     *    1.1 Validate the received.
     *    1.2 Based on the message type, perform the requested action by either:
     *        1.2.1 Directly replying if the action doesn't involve any IO operation.
     *        1.2.2 Sending a message to the writer coroutine to perform an operation on the output file.
     * 2. When either the client disconnects or we receive a fatal exception, terminate the connection.
     */
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

    // writes an Error message on the socket, optionally containing an error message
    private suspend fun sendError(cause: String?) {
        rw.write(Message.checked(MessageType.Error, cause))
    }

    // writes an OK message on the socket
    private suspend fun sendOk() {
        rw.write(Message.checked(MessageType.Ok, null))
    }

    // Sends a message to the writer coroutine notifying it of an action which should be completed
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