package net.namekdev.entity_tracker.utils

import kotlin.math.min


/**
 *
 * Performance optimized bitset implementation. Certain operations are
 * prefixed with `unsafe`; these methods perform no validation,
 * and are primarily leveraged internally to optimize access on entityId bitsets.
 *
 *
 * Originally adapted from `com.badlogic.gdx.utils.Bits`, it has been
 * renamed to avoid namespace confusion.
 *
 * @author mzechner
 * @author jshapcott
 * @author junkdog (fork/changes)
 *
 * @see com.artemis.EntityManager.registerEntityStore
 */
class CommonBitVector {
    companion object {
    }

    internal var words = longArrayOf(0)

    /** @return true if this bitset contains no bits that are set to true
     */
    val isEmpty: Boolean
        get() {
            val bits = this.words
            val length = bits.size
            for (i in 0 until length) {
                if (bits[i] != 0L) {
                    return false
                }
            }
            return true
        }

    constructor() {}

    /** Creates a bit set whose initial size is large enough to explicitly represent bits with indices in the range 0 through
     * nbits-1.
     * @param nbits the initial size of the bit set
     */
    constructor(nbits: Int) {
        checkCapacity(nbits.ushr(6))
    }

    /** Creates a bit set based off another bit vector.
     * @param copyFrom
     */
    constructor(copyFrom: CommonBitVector) {
        words = copyFrom.words.copyOf()
    }

    internal constructor(words: LongArray) {
        this.words = words
    }

    /** @param index the index of the bit
     * @return whether the bit is set
     * @throws ArrayIndexOutOfBoundsException if index < 0
     */
    operator fun get(index: Int): Boolean {
        val word = index.ushr(6)
        return word < words.size && words[word] and (1L shl index) != 0L
    }

    /** @param index the index of the bit to set
     * @throws ArrayIndexOutOfBoundsException if index < 0
     */
    fun set(index: Int) {
        val word = index.ushr(6)
        checkCapacity(word)
        words[word] = words[word] or (1L shl index)
    }

    /** @param index the index of the bit to set
     * @throws ArrayIndexOutOfBoundsException if index < 0
     */
    operator fun set(index: Int, value: Boolean) {
        if (value) {
            set(index)
        } else {
            clear(index)
        }
    }

    /** @param index the index of the bit
     * @return whether the bit is set
     * @throws ArrayIndexOutOfBoundsException if index < 0 or index >= words.length>>
     */
    fun unsafeGet(index: Int): Boolean {
        return words[index.ushr(6)] and (1L shl index) != 0L
    }

    /** @param index the index of the bit to set
     * @throws ArrayIndexOutOfBoundsException if index < 0 or index >= words.length
     */
    fun unsafeSet(index: Int) {
        words[index.ushr(6)] = words[index.ushr(6)] or (1L shl index)
    }

    /** @param index the index of the bit to set
     * @throws ArrayIndexOutOfBoundsException if index < 0 or index >= words.length
     */
    fun unsafeSet(index: Int, value: Boolean) {
        if (value) {
            unsafeSet(index)
        } else {
            unsafeClear(index)
        }
    }

    /** @param index the index of the bit to flip
     */
    fun flip(index: Int) {
        val word = index.ushr(6)
        checkCapacity(word)
        words[word] = words[word] xor (1L shl index)
    }

    /**
     * Grows the backing array (`long[]`) so that it can hold the requested
     * bits. Mostly applicable when relying on the `unsafe` methods,
     * including [.unsafeGet] and [.unsafeClear].
     *
     * @param bits number of bits to accomodate
     */
    fun ensureCapacity(bits: Int) {
        checkCapacity(bits.ushr(6))
    }

    private fun checkCapacity(len: Int) {
        if (len >= words.size) {
            val newBits = LongArray(len + 1)
            words.copyInto(newBits, 0, 0, words.size-1)
            words = newBits
        }
    }

    /** @param index the index of the bit to clear
     * @throws ArrayIndexOutOfBoundsException if index < 0
     */
    fun clear(index: Int) {
        val word = index.ushr(6)
        if (word >= words.size) return
        words[word] = words[word] and (1L shl index).inv()
    }

    /** @param index the index of the bit to clear
     * @throws ArrayIndexOutOfBoundsException if index < 0 or index >= words.length
     */
    fun unsafeClear(index: Int) {
        words[index.ushr(6)] = words[index.ushr(6)] and (1L shl index).inv()
    }

    /** Clears the entire bitset  */
    fun clear() {
        for (i in 0 until words.size)
            words[i] = 0L
    }

    /** Returns the "logical size" of this bitset: the index of the highest set bit in the bitset plus one. Returns zero if the
     * bitset contains no set bits.
     *
     * @return the logical size of this bitset
     */
    fun length(): Int {
        val bits = this.words
        for (word in bits.indices.reversed()) {
            val bitsAtWord = bits[word]
            if (bitsAtWord != 0L)
                return (word shl 6) + 64 - Long.numberOfLeadingZeros(bitsAtWord)
        }

        return 0
    }

    /** Returns the index of the first bit that is set to true that occurs on or after the specified starting index. If no such bit
     * exists then -1 is returned.  */
    fun nextSetBit(fromIndex: Int): Int {
        val word = fromIndex.ushr(6)
        if (word >= words.size)
            return -1

        var bitmap = words[word].ushr(fromIndex)
        if (bitmap != 0L)
            return fromIndex + Long.numberOfTrailingZeros(bitmap)

        for (i in 1 + word until words.size) {
            bitmap = words[i]
            if (bitmap != 0L) {
                return i * 64 + Long.numberOfTrailingZeros(bitmap)
            }
        }

        return -1
    }

    /** Returns the index of the first bit that is set to false that occurs on or after the specified starting index.  */
    fun nextClearBit(fromIndex: Int): Int {
        val word = fromIndex.ushr(6)
        if (word >= words.size)
            return min(fromIndex, words.size shl 6)

        var bitmap = words[word].ushr(fromIndex).inv()
        if (bitmap != 0L)
            return fromIndex + Long.numberOfTrailingZeros(bitmap)

        for (i in 1 + word until words.size) {
            bitmap = words[i].inv()
            if (bitmap != 0L) {
                return i * 64 + Long.numberOfTrailingZeros(bitmap)
            }
        }

        return min(fromIndex, words.size shl 6)
    }

    /** Performs a logical **AND** of this target bit set with the argument bit set. This bit set is modified so that each bit in
     * it has the value true if and only if it both initially had the value true and the corresponding bit in the bit set argument
     * also had the value true.
     * @param other a bit set
     */
    fun and(other: CommonBitVector) {
        val commonWords = min(words.size, other.words.size)
        run {
            var i = 0
            while (commonWords > i) {
                words[i] = words[i] and other.words[i]
                i++
            }
        }

        if (words.size > commonWords) {
            var i = commonWords
            val s = words.size
            while (s > i) {
                words[i] = 0L
                i++
            }
        }
    }

    /** Clears all of the bits in this bit set whose corresponding bit is set in the specified bit set.
     *
     * @param other a bit set
     */
    fun andNot(other: CommonBitVector) {
        val commonWords = min(words.size, other.words.size)
        var i = 0
        while (commonWords > i) {
            words[i] = words[i] and other.words[i].inv()
            i++
        }
    }

    /** Performs a logical **OR** of this bit set with the bit set argument. This bit set is modified so that a bit in it has the
     * value true if and only if it either already had the value true or the corresponding bit in the bit set argument has the
     * value true.
     * @param other a bit set
     */
    fun or(other: CommonBitVector) {
        val commonWords = min(words.size, other.words.size)
        run {
            var i = 0
            while (commonWords > i) {
                words[i] = words[i] or other.words[i]
                i++
            }
        }

        if (commonWords < other.words.size) {
            checkCapacity(other.words.size)
            var i = commonWords
            val s = other.words.size
            while (s > i) {
                words[i] = other.words[i]
                i++
            }
        }
    }

    /** Performs a logical **XOR** of this bit set with the bit set argument. This bit set is modified so that a bit in it has
     * the value true if and only if one of the following statements holds:
     *
     *  * The bit initially has the value true, and the corresponding bit in the argument has the value false.
     *  * The bit initially has the value false, and the corresponding bit in the argument has the value true.
     *
     * @param other
     */
    fun xor(other: CommonBitVector) {
        val commonWords = min(words.size, other.words.size)

        run {
            var i = 0
            while (commonWords > i) {
                words[i] = words[i] xor other.words[i]
                i++
            }
        }

        if (commonWords < other.words.size) {
            checkCapacity(other.words.size)
            var i = commonWords
            val s = other.words.size
            while (s > i) {
                words[i] = other.words[i]
                i++
            }
        }
    }

    /** Returns true if the specified CommonBitVector has any bits set to true that are also set to true in this CommonBitVector.
     *
     * @param other a bit set
     * @return boolean indicating whether this bit set intersects the specified bit set
     */
    fun intersects(other: CommonBitVector): Boolean {
        val bits = this.words
        val otherBits = other.words
        var i = 0
        val s = min(bits.size, otherBits.size)
        while (s > i) {
            if (bits[i] and otherBits[i] != 0L) {
                return true
            }
            i++
        }
        return false
    }

    /** Returns true if this bit set is a super set of the specified set,
     * i.e. it has all bits set to true that are also set to true
     * in the specified CommonBitVector.
     *
     * @param other a bit set
     * @return boolean indicating whether this bit set is a super set of the specified set
     */
    fun containsAll(other: CommonBitVector): Boolean {
        val bits = this.words
        val otherBits = other.words
        val otherBitsLength = otherBits.size
        val bitsLength = bits.size

        for (i in bitsLength until otherBitsLength) {
            if (otherBits[i] != 0L) {
                return false
            }
        }

        var i = 0
        val s = min(bitsLength, otherBitsLength)
        while (s > i) {
            if (bits[i] and otherBits[i] != otherBits[i]) {
                return false
            }
            i++
        }
        return true
    }

    fun cardinality(): Int {
        var count = 0
        for (i in words.indices)
            count += Long.bitCount(words[i])

        return count
    }

    /**
     * Decodes the set bits as integers. The destination
     * [IntBag] is reset before the bits are transposed.
     *
     * @param out decoded ints end up here
     * @return Same as out
     */
//    fun toIntBag(out: IntBag): IntBag {
//        if (isEmpty) {
//            out.setSize(0)
//            return out
//        }
//
//        val count = prepareBag(out, 1)
//
//        val data = out.getData()
//        var i = 0
//        var index = 0
//        while (count > index) {
//            var bitset = words[i]
//            val wordBits = i shl 6
//            while (bitset != 0L) {
//                val t = bitset and -bitset
//                data[index] = wordBits + java.lang.Long.bitCount(t - 1)
//                bitset = bitset xor t
//
//                index++
//            }
//            i++
//        }
//
//        return out
//    }
//
//    /**
//     * Decodes the set bits as pairs of `entity id` and
//     * [compositionId][World.compositionId]. The
//     * destination[IntBag] is reset before the bits are
//     * transposed.
//     *
//     * @param out decoded ints end up here
//     * @return Same as out
//     */
//    fun toIntBagIdCid(cm: ComponentManager, out: IntBag): IntBag {
//        if (isEmpty) {
//            out.setSize(0)
//            return out
//        }
//
//        val count = prepareBag(out, 2)
//
//        val data = out.getData()
//        var i = 0
//        var index = 0
//        while (count > index) {
//            var bitset = words[i]
//            val wordBits = i shl 6
//            while (bitset != 0L) {
//                val t = bitset and -bitset
//                val id = wordBits + java.lang.Long.bitCount(t - 1)
//                data[index] = id
//                data[index + 1] = cm.getIdentity(id)
//                index += 2
//                bitset = bitset xor t
//            }
//            i++
//        }
//
//        return out
//    }
//
//    private fun prepareBag(out: IntBag, elementsPerEntry: Int): Int {
//        val count = elementsPerEntry * cardinality()
//        out.ensureCapacity(count)
//        out.setSize(count)
//        return count
//    }

    override fun hashCode(): Int {
        val word = length().ushr(6)
        var hash = 0
        var i = 0
        while (word >= i) {
            hash = 127 * hash + (words[i] xor words[i].ushr(32)).toInt()
            i++
        }
        return hash
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj)
            return true
        if (obj == null)
            return false
        if (this::class != obj::class)
            return false

        val other = obj as CommonBitVector?
        val otherBits = other!!.words

        val commonWords = min(words.size, otherBits.size)
        var i = 0
        while (commonWords > i) {
            if (words[i] != otherBits[i])
                return false
            i++
        }

        return if (words.size == otherBits.size) true else length() == other.length()

    }

    override fun toString(): String {
        val cardinality = cardinality()
        val end = min(128, cardinality)
        var count = 0

        val sb = StringBuilder()
        sb.append("CommonBitVector[").append(cardinality)
        if (cardinality > 0) {
            sb.append(": {")
            var i = nextSetBit(0)
            while (end > count && i != -1) {
                if (count != 0)
                    sb.append(", ")

                sb.append(i)
                count++
                i = nextSetBit(i + 1)
            }

            if (cardinality > end)
                sb.append(" ...")

            sb.append("}")
        }
        sb.append("]")
        return sb.toString()
    }
}