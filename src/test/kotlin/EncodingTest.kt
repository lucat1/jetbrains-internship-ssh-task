import me.lucat1.sock.protocol.uBytesToUInt
import me.lucat1.sock.protocol.uIntToUBytes
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import kotlin.math.pow

@OptIn(ExperimentalUnsignedTypes::class)
class EncodingTest {
    @Test
    fun uIntToUBytes() {
        for (i in 0..31) {
            val pow = 2.0.pow(i).toUInt()

            val powBA = ubyteArrayOf(0u, 0u, 0u, 0u)
            val powBAi = 3 - (i / Byte.SIZE_BITS) // index of the byte to update
            val byteVal = 1 shl (i % Byte.SIZE_BITS)
            powBA[powBAi] = byteVal.toUByte()

            assert(powBA.contentEquals(uIntToUBytes(pow)))
        }
    }

    @Test
    fun uBytesToUInt() {
        for (i in 0..3) {
            val powBA = ubyteArrayOf(0u, 0u, 0u, 0u)
            for (j in 0..7) {
                powBA[3-i] = (1 shl j).toUByte()
                val pow = 2.0.pow(i*Byte.SIZE_BITS + j).toUInt()
                assertEquals(pow, uBytesToUInt(powBA))
            }
        }
    }

    @Test
    fun endToEnd() {
        // This tests numbers up to: 33558528
        var sum = 0u
        for (i in 0u..8192u) {
            assertEquals(sum, uBytesToUInt(uIntToUBytes(sum)))
            sum += i
        }
    }
}