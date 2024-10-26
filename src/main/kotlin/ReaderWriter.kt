package me.lucat1.sock

import io.klogging.Klogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

@OptIn(ExperimentalUnsignedTypes::class)
class ReaderWriter(private val socket: SocketChannel, private val logger: Klogger) {
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

    // Reads one message from the socket, along with its content.
    // NOTE: not guarantee the correctness of the message. It should be checked separately by the caller.
    @Throws(InvalidHeader::class)
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