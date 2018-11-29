package net.namekdev.entity_tracker.utils

import org.khronos.webgl.*


actual fun assert(condition: Boolean) {
    if (!condition) {
        throw Exception("Assertion Error")
    }
}

actual fun Long.Companion.numberOfLeadingZeros(l: Long): Int {
    if (l == 0L)
        return 64
    var n = 1
    var x = l.ushr(32).toInt()
    if (x == 0) {
        n += 32
        x = l.toInt()
    }
    if (x.ushr(16) == 0) {
        n += 16
        x = x shl 16
    }
    if (x.ushr(24) == 0) {
        n += 8
        x = x shl 8
    }
    if (x.ushr(28) == 0) {
        n += 4
        x = x shl 4
    }
    if (x.ushr(30) == 0) {
        n += 2
        x = x shl 2
    }
    n -= x.ushr(31)
    return n
}

actual fun Long.Companion.numberOfTrailingZeros(l: Long): Int {
    var x: Int
    var y: Int
    if (l == 0L) return 64
    var n = 63
    y = l.toInt()
    if (y != 0) {
        n = n - 32
        x = y
    }
    else x = l.ushr(32).toInt()
    y = x shl 16

    if (y != 0) {
        n = n - 16
        x = y
    }
    y = x shl 8

    if (y != 0) {
        n = n - 8
        x = y
    }
    y = x shl 4

    if (y != 0) {
        n = n - 4
        x = y
    }
    y = x shl 2

    if (y != 0) {
        n = n - 2
        x = y
    }
    return n - (x shl 1).ushr(31)
}

actual fun Long.Companion.bitCount(l: Long): Int {
    var i = l - (l.ushr(1) and 0x5555555555555555L)
    i = (i and 0x3333333333333333L) + (i.ushr(2) and 0x3333333333333333L)
    i = i + i.ushr(4) and 0x0f0f0f0f0f0f0f0fL
    i = i + i.ushr(8)
    i = i + i.ushr(16)
    i = i + i.ushr(32)
    return i.toInt() and 0x7f
}

val int8 = Int8Array(8);
val int32 = Int32Array(int8.buffer, 0, 2);
val float32 = Float32Array(int8.buffer, 0, 1);
val float64 = Float64Array(int8.buffer, 0, 1);


actual fun Double.Companion.longBitsToDouble(l: Long): Double {
    int32[0] = l.shr(32).toInt()
    int32[1] = l.toInt()
    return float64[0]
}

actual fun Float.Companion.intBitsToFloat(i: Int): Float {
    int32[0] = i
    return float32[0]
}