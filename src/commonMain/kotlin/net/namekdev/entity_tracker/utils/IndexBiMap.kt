package net.namekdev.entity_tracker.utils

class IndexBiMap : AutoSizedArray<Int> {
    constructor() : super() {}

    constructor(capacity: Int) : super(capacity) {}

    fun getLocalIndex(globalIndex: Int): Int {
        var i = 0
        val n = _array.size
        while (i < n) {
            val value = _array[i] ?: break

            if (value === globalIndex) {
                return i
            }
            ++i
        }

        return -1
    }

    fun getGlobalIndex(localIndex: Int): Int {
        return _array[localIndex]!!
    }
}
