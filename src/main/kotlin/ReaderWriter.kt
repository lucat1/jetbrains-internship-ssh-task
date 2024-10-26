package me.lucat1.sock

import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

@OptIn(ExperimentalUnsignedTypes::class)
class ReaderWriter(private val socket: SocketChannel) {
    fun write(message: Message) {
        val bytes = message.toUByteArray()

        val buff = ByteBuffer.allocate(bytes.size)
        buff.put(bytes.toByteArray())
        buff.flip()

        while(buff.hasRemaining()) {
            socket.write(buff)
        }
    }

    fun read(): Message? {
        val buffer = ByteBuffer.allocate(HEADER_SIZE)
        val bytesRead = socket.read(buffer)
        if (bytesRead <= 0) {
            return null
        }
        buffer.flip();

        val header = Header.fromByteArray(buffer.array().toUByteArray())
        lateinit var content: String
        if (header.contentLength > 0u) {
            val contentBuffer = ByteBuffer.allocate(header.contentLength.toInt())
            socket.read(contentBuffer)
            contentBuffer.flip();
            content = String(contentBuffer.array())
        }

        return Message(header.messageType, content)
    }
}