package me.lucat1.sock.protocol

import io.klogging.Klogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

/**
 * ReaderWriter provides a two-way abstraction over a socket for speaking the protocol.
 *
 * @property socket The socket connection to use when sending/receiving messages.
 * @property logger The logger instance to use when logging traces.
 * @constructor Instantiate a ReaderWriter connected to the provided socket.
 */
@OptIn(ExperimentalUnsignedTypes::class)
class ReaderWriter(private val socket: SocketChannel, private val logger: Klogger) {
    /**
     * Writes a new message on the socket.
     * NOTE: we **don't check** for message validity here.
     *
     * @param message The message to be written over the socket.
     * @throws IOException If there was an error while writing to the socket.
     */
    @Throws(IOException::class)
    suspend fun write(message: Message) = withContext(Dispatchers.IO) {
        val bytes = message.toUByteArray()

        val buff = ByteBuffer.allocate(bytes.size)
        buff.put(bytes.toByteArray())
        buff.flip()

        while(buff.hasRemaining()) {
            socket.write(buff)
        }
        logger.debug("Wrote {nBytes} bytes", bytes.size)
    }

    /**
     * Reads a message from the socket and returns it.
     * NOTE: the returned message can be **invalid**.
     * The caller is expected to check for validity.
     *
     * @return The latest message read from the socket.
     *         If the value is null then the socket has been closed.
     */
    @Throws(InvalidHeaderException::class)
    suspend fun read(): Message? = withContext(Dispatchers.IO) {
        val buffer = ByteBuffer.allocate(HEADER_SIZE)
        logger.debug("Reading {nBytes} bytes", HEADER_SIZE)
        val bytesRead = socket.read(buffer)
        if (bytesRead <= 0) {
            return@withContext null
        }
        buffer.flip();

        val header = Header.fromByteArray(buffer.array().toUByteArray())
        var content: String? = null
        if (header.contentLength > 0u) {
            logger.debug("Reading {nBytes} bytes for content", header.contentLength)
            val contentBuffer = ByteBuffer.allocate(header.contentLength.toInt())
            socket.read(contentBuffer)
            contentBuffer.flip();
            content = String(contentBuffer.array())
        }

        return@withContext Message(header.messageType, content)
    }
}