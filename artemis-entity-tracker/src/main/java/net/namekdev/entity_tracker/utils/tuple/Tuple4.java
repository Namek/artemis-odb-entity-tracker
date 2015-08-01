package net.namekdev.entity_tracker.utils.tuple;

public final class Tuple4<E1, E2, E3, E4> {
	public E1 item1;
	public E2 item2;
	public E3 item3;
	public E4 item4;

	public Tuple4(E1 item1, E2 item2, E3 item3, E4 item4) {
		this.item1 = item1;
		this.item2 = item2;
		this.item3 = item3;
		this.item4 = item4;
	}

	public static <E1, E2, E3, E4> Tuple4<E1, E2, E3, E4> create(E1 item1, E2 item2, E3 item3, E4 item4) {
		return new Tuple4<E1, E2, E3, E4>(item1, item2, item3, item4);
	}
}
