package net.namekdev.entity_tracker.utils

import com.artemis.utils.BitVector
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

val BitVector_words = BitVector::class.memberProperties
    .find { m -> m.name == "words" }!!

fun CommonBitVector.Companion.ofOriginal(bv: BitVector): CommonBitVector {
    BitVector_words.isAccessible = true
    val words = BitVector_words.getter.call(bv) as LongArray
    return CommonBitVector(words)
}