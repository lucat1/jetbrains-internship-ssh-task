package me.lucat1.sock

/*
 * Message header size. Given by:
 * + 1 for MessageType
 * + 3 reserved
 * + 4 for big-endian uint32 containing Content Length
 */
const val HEADER_SIZE = 8

// Start of the Content Length field in the header
const val CONTENT_LENGTH_START = 4
// End (inclusive) of the Content Length field in the header
const val CONTENT_LENGTH_END = 7

enum class MessageType(val code: UByte) {
    Ok(1u),
    Write(2u),
    Clear(3u),
    Error(4u),
    Ping(5u);

    companion object {
        fun fromByte(code: UByte) = entries.first { it.code == code }
    }

    fun toUByte(): UByte {
        return this.code
    }
}

@OptIn(ExperimentalStdlibApi::class)
class MessageTypeException(mt: UByte): Exception("Invalid message type: 0x${mt.toHexString()}")

@OptIn(ExperimentalUnsignedTypes::class)
class Header(val messageType: MessageType, val contentLength: UInt) {
    companion object {
        fun fromByteArray(rawHeader: UByteArray): Header {
            assert(rawHeader.size == HEADER_SIZE)

            val messageType: MessageType
            val contentLength: UInt
            val mt = rawHeader[0]

            try {
                messageType = MessageType.fromByte(mt)
            } catch(_: NoSuchElementException) {
                throw MessageTypeException(mt)
            }

            val cl = rawHeader.copyOfRange(CONTENT_LENGTH_START, CONTENT_LENGTH_END+1)
            contentLength = uBytesToUInt(cl)

            return Header(messageType, contentLength)
        }
    }

    fun toUByteArray(): UByteArray {
        val ba = UByteArray(HEADER_SIZE)
        ba[0] = messageType.toUByte()
        val cl = uIntToUBytes(contentLength)
        for (i in 0..3) {
            ba[CONTENT_LENGTH_START+i] = cl[i]
        }
        return ba
    }
}