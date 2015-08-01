package net.namekdev.entity_tracker.utils.tuple;

public final class Tuple3<E1, E2, E3> {
	public E1 item1;
	public E2 item2;
	public E3 item3;

	public Tuple3(E1 item1, E2 item2, E3 item3) {
		this.item1 = item1;
		this.item2 = item2;
		this.item3 = item3;
	}

	public static <E1, E2, E3> Tuple3<E1, E2, E3> create(E1 item1, E2 item2, E3 item3) {
		return new Tuple3<E1, E2, E3>(item1, item2, item3);
	}
}