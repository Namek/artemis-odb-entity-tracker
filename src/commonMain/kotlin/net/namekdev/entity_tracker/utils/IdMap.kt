package net.namekdev.entity_tracker.utils

typealias K = Int

/**
 * Map of elements with IDs of the Int type.
 * The Int type are for keys giving the O(1) map which also can be acquired as a sorted list of IDs.
 */
class IdMap<V> {
    private val _sortedKeys = mutableListOf<K>()
    private val _values = mutableMapOf<K, V>()


    val size: Int get() = _values.size
    val keys: List<K> get() = _sortedKeys

    operator fun get(key: K): V? = _values[key]

    operator fun set(key: K, value: V) {
        val prevValue = _values.put(key, value)

        if (prevValue == null) {
            val insertionIdx = _findInsertionIndex(key)
            _sortedKeys.add(insertionIdx, key)
        }
    }

    fun remove(key: K) {
        if (_values.remove(key) != null)
            _sortedKeys.remove(key)
    }

    fun clear() {
        _sortedKeys.clear()
        _values.clear()
    }

    private fun _findInsertionIndex(targetVal: Int): Int {
        var low = 0
        var high = _sortedKeys.size - 1

        while (low <= high) {
            val mid = (low + high).ushr(1) // safe from overflows
            val midVal = _sortedKeys[mid]

            if (midVal == targetVal)
                throw Exception("value $targetVal already exists at index $mid")

            val cmp = midVal.compareTo(targetVal)

            if (cmp < 0)
                low = mid + 1
            else if (cmp > 0)
                high = mid - 1
        }

        return low
    }
}