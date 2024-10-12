import me.lucat1.sock.HEADER_SIZE
import me.lucat1.sock.Header
import me.lucat1.sock.MessageType
import me.lucat1.sock.uIntToUBytes
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalUnsignedTypes::class)
class HeaderTest {
    @Test
    fun fromByteArray() {
        for (mt in MessageType.entries) {
            for (i in 0u..9u) {
                val ba = ubyteArrayOf(
                    mt.toUByte(),
                    0u, 0u, 0u, // reserved
                    *uIntToUBytes(i)
                )

                assertEquals(ba.size, HEADER_SIZE)
                val header = Header.fromByteArray(ba)

                assertEquals(header.messageType, mt)
                assertEquals(header.contentLength, i)
            }
        }
    }

    @Test
    fun toUByteArray() {
        for (mt in MessageType.entries) {
            for (i in 0u..9u) {
                val header = Header(mt, i)
                val ba = ubyteArrayOf(
                    mt.toUByte(),
                    0u, 0u, 0u, // reserved
                    *uIntToUBytes(i)
                )

                assert(header.toUByteArray().contentEquals(ba))
            }
        }
    }

    @Test
    fun endToEnd() {
        for (mt in MessageType.entries) {
            for (i in 0u..9u) {
                val header = Header(mt, i)
                val e2eHeader = Header.fromByteArray(header.toUByteArray())
                assertEquals(header.messageType, e2eHeader.messageType)
                assertEquals(header.contentLength, e2eHeader.contentLength)
            }
        }
    }
}