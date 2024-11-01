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
        /**
         * Obtain a MessageType from a 1-byte encoding.
         *
         * @param byt the unsigned byte encoding to be parsed.
         */
        fun fromUByte(byt: UByte) = entries.first { it.code == byt }
    }

    /**
     * Obtain the associated UByte representation of a MessageType.
     *
     * @return the 1-byte representation of this MessageType.
     */
    fun toUByte(): UByte {
        return this.code
    }
}

@OptIn(ExperimentalStdlibApi::class)
/**
 * Signals an invalid MessageType error.
 *
 * @constructor Builds from raw 1-byte value of the invalid MessageType.
 *
 * @param messageType The invalid unsigned byte representing a MessageType.
 */
class MessageTypeException(messageType: UByte): Exception("Invalid message type: 0x${messageType.toHexString()}")

/**
 * Signals an invalid header, due to an invalid content length given the MessageType.
 *
 * @constructor Builds the error from the MessageType and the content length.
 *
 * @param messageType The MessageType associated with the header.
 * @param contentLength The content length provided in the header.
 */
class InvalidHeaderException(messageType: MessageType, contentLength: UInt): Exception("Message type $messageType has content length $contentLength")

/**
 * Representation of the Header for a Message.
 *
 * @property messageType The header's message type.
 * @property contentLength The header's content length.
 * @constructor Creates a Header given the message type and content length
 */
@OptIn(ExperimentalUnsignedTypes::class)
class Header(val messageType: MessageType, val contentLength: UInt) {
    companion object {
        /**
         * Parses the header from an 8 unsigned byte buffer.
         * NOTE: the header is not checked for validity. This is useful to
         * construct malformed headers from the client to test error
         * handling on the server side.
         *
         * @param rawHeader 8-byte long unsigned byte array.
         * @return A Header instance containing the appropriate MessageType and content length.
         * @throws MessageTypeException
         */
        @Throws(MessageTypeException::class)
        fun fromByteArray(rawHeader: UByteArray): Header {
            assert(rawHeader.size == HEADER_SIZE)

            val messageType: MessageType
            val contentLength: UInt
            val mt = rawHeader[0]

            try {
                messageType = MessageType.fromUByte(mt)
            } catch(_: NoSuchElementException) {
                throw MessageTypeException(mt)
            }

            val cl = rawHeader.copyOfRange(CONTENT_LENGTH_START, CONTENT_LENGTH_END+1)
            contentLength = uBytesToUInt(cl)

            return Header(messageType, contentLength)
        }
    }

    /**
     * Encodes the Header into an 8 unsigned byte array.
     *
     * @return the 8 unsigned byte representation of this header.
     */
    fun toUByteArray(): UByteArray {
        val ba = UByteArray(HEADER_SIZE)
        ba[0] = messageType.toUByte()
        val cl = uIntToUBytes(contentLength)
        for (i in 0..3) {
            ba[CONTENT_LENGTH_START+i] = cl[i]
        }
        return ba
    }

    /**
     * Validates the header against the following constraints:
     * - If messageType is Writer or Error, then content length
     *   can be an arbitrary value.
     * - Else, content length must be 0.
     */
    @Throws(InvalidHeaderException::class)
    fun validate() {
        // These messageTypes allow for Content Length > 0
        if (
            messageType == MessageType.Write ||
            messageType == MessageType.Error
        )
            return

        if (contentLength > 0u)
            throw InvalidHeaderException(messageType, contentLength)
    }
}

/**
 * Representation of a protocol Message.
 *
 * @property messageType The message's type.
 * @property content The message's content.
 * @constructor Create an **unchecked** message. This constructor should only
 *              be used when it's desirable to construct malformed messages.
 */
@OptIn(ExperimentalUnsignedTypes::class)
class Message(private val messageType: MessageType, val content: String?) {
    val contentLength = content?.encodeToByteArray()?.size?.toUInt() ?: 0u
    val header: Header = Header(messageType, contentLength)

    companion object {
        // This constructor should always be used, except when we want to craft malformed messages
        /**
         * Creates a **checked** message with the given message type and content.
         *
         * @param messageType The message's type.
         * @param content The message's content.
         * @return A valid Message instance.
         */
        @Throws(InvalidHeaderException::class)
        fun checked(messageType: MessageType, content: String?): Message {
            val msg = Message(messageType, content)
            msg.header.validate()
            return msg
        }
    }

    /**
     * Returns an unsigned byte array of the appropriate size, containing the message
     * content encoded as per the protocol specification.
     *
     * @return an unsigned byte array containing the encoded message.
     */
    fun toUByteArray(): UByteArray {
        val size = HEADER_SIZE + header.contentLength.toInt();
        val arr = UByteArray(size);
        header.toUByteArray().copyInto(arr)
        content?.toByteArray()?.toUByteArray()?.copyInto(arr, HEADER_SIZE);

        return arr
    }
}