package net.namekdev.entity_tracker.utils


import org.junit.Assert.*
import org.junit.Test

class KeySortedMapTest {
    @Test
    fun doubled_keys_are_not_repeated() {
        val map = IdMap<String>()

        for (i in 2..5)
            map[i] = i.toString()

        for (i in 10..13)
            map[i] = i.toString()

        for (i in 5..10)
            map[i] = i.toString()

        assertArrayEquals((2..13).toList().toIntArray(), map.keys.toIntArray())
    }

    @Test
    fun overlapping_sequence_of_keys() {
        val map = IdMap<String>()

        for (i in 2..5)
            map[i] = i.toString()
        for (i in 5..10)
            map[i] = i.toString()

        for (i in 2..13)
            map[i] = i.toString()

        assertArrayEquals((2..13).toList().toIntArray(), map.keys.toIntArray())
    }
}