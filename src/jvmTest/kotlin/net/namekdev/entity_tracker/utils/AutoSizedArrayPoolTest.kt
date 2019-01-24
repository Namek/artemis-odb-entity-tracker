package net.namekdev.entity_tracker.utils

import org.junit.Assert.*

import java.util.LinkedList

import org.junit.Test

class AutoSizedArrayPoolTest {

    @Test
    fun obtaining_non_exact_sizes() {
        val pool = ArrayPool(Integer::class.java)

        val arr = pool.obtain(10, true)
        pool.free(arr, false)

        val arr2 = pool.obtain(9, false)

        assertEquals(arr.size.toLong(), arr2.size.toLong())
        assertTrue(arr == arr2)
    }

    @Test
    fun obtaining_edge_exact_sizes() {
        val pool = ArrayPool(Integer::class.java)

        pool.free(pool.obtain(11, true))
        pool.free(pool.obtain(31, true))
        pool.free(pool.obtain(14, true))
        pool.free(pool.obtain(20, true))
        pool.free(pool.obtain(10, true))
        pool.free(pool.obtain(30, true))

        assertEquals(10, pool.obtain(10, true).size)
        assertEquals(31, pool.obtain(31, true).size)
    }

    @Test
    fun no_duplicates() {
        val pool = ArrayPool(Integer::class.java)
        val arrays = ReflectionUtils.getHiddenFieldValue(
            ArrayPool::class.java,
            "_arrays",
            pool
        ) as LinkedList<Array<Integer>>

        val sizes = intArrayOf(1, 2, 3, 2, 3, 1)
        for (size in sizes) {
            pool.free(pool.obtain(size, true))
        }

        assertEquals(3, arrays.size.toLong())
    }

    @Test
    fun is_internally_sorted() {
        val pool = ArrayPool(Integer::class.java)
        val arrays = ReflectionUtils.getHiddenFieldValue(
            ArrayPool::class.java,
            "_arrays",
            pool
        ) as LinkedList<Array<Int>>

        val sizes = intArrayOf(1, 2, 3, 2, 3, 1, 4, 1, 5, 8, 7, 6, 7, 2)
        for (size in sizes) {
            pool.free(pool.obtain(size, true))
        }

        assertEquals(8, arrays.size.toLong())

        val prevSize = arrays[0].size
        for (i in 1 until arrays.size) {
            if (arrays[i].size < prevSize) {
                fail("Internal array is not sorted.")
            }
        }
    }
}
