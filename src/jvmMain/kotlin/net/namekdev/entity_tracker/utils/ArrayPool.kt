package net.namekdev.entity_tracker.utils

import java.util.Arrays
import java.util.LinkedList

import com.artemis.utils.reflect.ArrayReflection

class ArrayPool<T>(private val type: Class<T>) {
    private val _arrays = LinkedList<Array<T>>()

    fun obtain(size: Int, exactSize: Boolean): Array<T> {
        var imin = 0
        var imax = _arrays.size - 1

        while (imax >= imin) {
            val imid = (imax + imin) / 2
            val arr = _arrays[imid]

            if (arr.size == size) {
                _arrays.removeAt(imid)
                return arr
            }
            else if (arr.size < size) {
                imin = imid + 1
            }
            else {
                if (!exactSize) {
                    return arr
                }

                imax = imid - 1
            }
        }

        return create(size)
    }

    @JvmOverloads fun free(array: Array<T>, nullify: Boolean = false) {
        if (nullify) {
            Arrays.fill(array, null)
        }

        val size = array.size
        var imin = 0
        var imax = _arrays.size - 1
        var index = 0

        while (imin <= imax) {
            index = (imax + imin) / 2
            val arr = _arrays[index]

            if (arr.size == size) {
                break
            }

            if (arr.size > size) {
                imax = index - 1
            }
            else {
                imin = index + 1
            }
        }

        if (index < imin) {
            index = imin
        }

        _arrays.add(index, array)
    }

    protected fun create(size: Int): Array<T> {
        return ArrayReflection.newInstance(type, size) as Array<T>
    }
}
