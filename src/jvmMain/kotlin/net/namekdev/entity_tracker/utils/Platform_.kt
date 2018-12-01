package net.namekdev.entity_tracker.utils

actual fun assert(condition: Boolean) {
    kotlin.assert(condition)
}

actual fun Long.Companion.numberOfLeadingZeros(l: Long): Int {
    return java.lang.Long.numberOfLeadingZeros(l)
}

actual fun Long.Companion.numberOfTrailingZeros(l: Long): Int {
    return java.lang.Long.numberOfTrailingZeros(l)
}

actual fun Long.Companion.bitCount(l: Long): Int {
    return java.lang.Long.bitCount(l)
}

actual fun Double.Companion.longBitsToDouble(l: Long): Double {
    return java.lang.Double.longBitsToDouble(l)
}

actual fun Double.Companion.doubleToLongBits(d: Double): Long {
    return java.lang.Double.doubleToLongBits(d)
}

actual fun Float.Companion.intBitsToFloat(i: Int): Float {
    return java.lang.Float.intBitsToFloat(i)
}

actual fun Float.Companion.floatToIntBits(f: Float): Int {
    return java.lang.Float.floatToIntBits(f)
}