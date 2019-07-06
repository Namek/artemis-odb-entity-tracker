package net.namekdev.entity_tracker.utils.serialization

class ValueTree {
    var id: Short = -1

    /**
     * This is always an array. The reason for this to be described as `Any` type
     * is that it may be a array of primitives or references where both don't have
     * any more specific common type than `Any`.
     */
    lateinit var values: CommonArray
    var model: ObjectModelNode? = null
    var parent: ValueTree? = null

    constructor() { }

    constructor(array: Array<Any?>) {
        values = CommonArray(array)
    }

    fun <T> asRefArray(): Array<T> =
        values.array as Array<T>

    fun asIterable(): Iterable<Any?> {
        val values = values.array
        return when (values) {
            is Iterable<*> -> values
            is Array<*> -> values.asIterable()
            is BooleanArray -> values.asIterable()
            is ByteArray -> values.asIterable()
            is CharArray -> values.asIterable()
            is ShortArray -> values.asIterable()
            is IntArray -> values.asIterable()
            is LongArray -> values.asIterable()
            is FloatArray -> values.asIterable()
            is DoubleArray -> values.asIterable()
            else -> throw Exception("unknown type")
        }
    }
}

class CommonArray(val array: Any) {
    val size: Int
    val getter: (Int) -> Any?

    init {
        val pair = when (array) {
            is Array<*> -> Pair(array.size, { i: Int -> array[i] })
            is BooleanArray -> Pair(array.size, { i: Int -> array[i] })
            is ByteArray -> Pair(array.size, { i: Int -> array[i] })
            is CharArray -> Pair(array.size, { i: Int -> array[i] })
            is ShortArray -> Pair(array.size, { i: Int -> array[i] })
            is IntArray -> Pair(array.size, { i: Int -> array[i] })
            is LongArray -> Pair(array.size, { i: Int -> array[i] })
            is FloatArray -> Pair(array.size, { i: Int -> array[i] })
            is DoubleArray -> Pair(array.size, { i: Int -> array[i] })
            else -> Pair(0, { _ -> null }) // we don't care, let it compile!
        }
        size = pair.first
        getter = pair.second
    }

//    val setter =
//        when (array) {
//            is Array<*> -> { i: Int, value: Any? -> array[i] = value }
//            is BooleanArray -> { i: Int -> array[i] }
//            is ByteArray -> { i: Int -> array[i] }
//            is CharArray -> { i: Int -> array[i] }
//            is ShortArray -> { i: Int -> array[i] }
//            is IntArray -> { i: Int -> array[i] }
//            is LongArray -> { i: Int -> array[i] }
//            is FloatArray -> { i: Int -> array[i] }
//            is DoubleArray -> { i: Int -> array[i] }
//            else -> { i -> null } // we don't care, let it compile!
//        }



    inline operator fun <T> get(index: Int): T =
        getter(index) as T


//    operator fun <T> set(index: Int, value: T) {
//
//    }
}
