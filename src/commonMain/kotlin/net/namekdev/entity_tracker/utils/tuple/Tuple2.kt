package net.namekdev.entity_tracker.utils.tuple

class Tuple2<E1, E2>(var item1: E1, var item2: E2) {
    companion object {

        fun <E1, E2> create(item1: E1, item2: E2): Tuple2<E1, E2> {
            return Tuple2(item1, item2)
        }
    }
}
