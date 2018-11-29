package net.namekdev.entity_tracker.utils

expect fun assert(condition: Boolean)
expect fun Long.Companion.numberOfLeadingZeros(l: Long): Int
expect fun Long.Companion.numberOfTrailingZeros(l: Long): Int
expect fun Long.Companion.bitCount(l: Long): Int
expect fun Double.Companion.longBitsToDouble(l: Long): Double
expect fun Float.Companion.intBitsToFloat(i: Int): Float