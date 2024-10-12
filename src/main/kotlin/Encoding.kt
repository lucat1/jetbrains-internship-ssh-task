package me.lucat1.sock

/**
 * This file provides primitives for encoding uint32s into a byte array.
 * UByteArray was used even though it is currently experimental, as it's semantically more correct.
 * Further, it appears that sh{r,l} perform arithmetic shift operations on Bytes, thus the sign matters.
 */

// The uIntToUBytes function is taken from https://stackoverflow.com/a/72851059
// and slightly modified to fit the purpose of this project.
@OptIn(ExperimentalUnsignedTypes::class)
fun uIntToUBytes(i: UInt): UByteArray {
    return (3 downTo 0).map {
        (i shr (it * Byte.SIZE_BITS)).toUByte()
    }.toUByteArray()
}

@OptIn(ExperimentalUnsignedTypes::class)
fun uBytesToUInt(b: UByteArray): UInt {
    assert(b.size == 4)
    var n = 0u
    for (i in 0..3) {
        // Byte in the i-th position is the (i+1)-th Most Significant Byte (MSB)
        // i.e., byte 0 is the 1st MSB
        // Thus, we need to shift it by 3-i bytes to give it the correct value
        val partialN = b[i].toUInt() shl ((3-i) * Byte.SIZE_BITS)
        n += partialN
    }
    return n
}