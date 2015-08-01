package net.namekdev.entity_tracker.utils.tuple;

public final class Tuple2<E1, E2> {
	public E1 item1;
	public E2 item2;

	public Tuple2(E1 item1, E2 item2) {
		this.item1 = item1;
		this.item2 = item2;
	}

	public static <E1, E2> Tuple2<E1, E2> create(E1 item1, E2 item2) {
		return new Tuple2<E1, E2>(item1, item2);
	}
}
