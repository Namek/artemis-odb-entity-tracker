package net.namekdev.entity_tracker.utils.tuple

class Tuple4<E1, E2, E3, E4>(var item1: E1, var item2: E2, var item3: E3, var item4: E4) {
    companion object {

        fun <E1, E2, E3, E4> create(item1: E1, item2: E2, item3: E3, item4: E4): Tuple4<E1, E2, E3, E4> {
            return Tuple4(item1, item2, item3, item4)
        }
    }
}
