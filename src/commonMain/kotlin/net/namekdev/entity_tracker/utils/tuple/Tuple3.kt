package net.namekdev.entity_tracker.utils.tuple

class Tuple3<E1, E2, E3>(var item1: E1, var item2: E2, var item3: E3) {
    companion object {

        fun <E1, E2, E3> create(item1: E1, item2: E2, item3: E3): Tuple3<E1, E2, E3> {
            return Tuple3(item1, item2, item3)
        }
    }
}