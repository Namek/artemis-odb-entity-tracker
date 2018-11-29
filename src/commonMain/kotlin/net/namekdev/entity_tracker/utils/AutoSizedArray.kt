package net.namekdev.entity_tracker.utils

open class AutoSizedArray<T> {
    protected val _array: ArrayList<T?>


    constructor() {
        _array = ArrayList()
    }

    constructor(capacity: Int) {
        _array = ArrayList(capacity)
    }

    operator fun get(index: Int): T {
        return _array[index]!!
    }

    operator fun set(index: Int, value: T) {
        ensureSize(index + 1)
        _array[index] = value
    }

    fun ensureSize(size: Int) {
        _array.ensureCapacity(size)

        var i = _array.size
        while (i++ < size) {
            _array.add(null)
        }
    }

    fun clear() {
        _array.clear()
    }
}
