package net.namekdev.entity_tracker.utils;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.Test;

public class AutoSizedArrayPoolTest {

	@Test
	public void obtaining_non_exact_sizes() {
		ArrayPool<Integer> pool = new ArrayPool<>(Integer.class);

		Integer[] arr = pool.obtain(10, true);
		pool.free(arr, false);

		Integer[] arr2 = pool.obtain(9, false);

		assertEquals(arr.length, arr2.length);
		assertTrue(arr == arr2);
	}

	@Test
	public void obtaining_edge_exact_sizes() {
		ArrayPool<Integer> pool = new ArrayPool<>(Integer.class);

		pool.free(pool.obtain(11, true));
		pool.free(pool.obtain(31, true));
		pool.free(pool.obtain(14, true));
		pool.free(pool.obtain(20, true));
		pool.free(pool.obtain(10, true));
		pool.free(pool.obtain(30, true));

		assertEquals(10, pool.obtain(10, true).length);
		assertEquals(31, pool.obtain(31, true).length);
	}

	@Test
	public void no_duplicates() {
		ArrayPool<Integer> pool = new ArrayPool<>(Integer.class);
		LinkedList<Integer[]> arrays = (LinkedList<Integer[]>) ReflectionUtils.getHiddenFieldValue(ArrayPool.class, "_arrays", pool);

		int[] sizes = new int[] {
			1, 2, 3, 2, 3, 1
		};
		for (int size : sizes) {
			pool.free(pool.obtain(size, true));
		}

		assertEquals(3, arrays.size());
	}

	@Test
	public void is_internally_sorted() {
		ArrayPool<Integer> pool = new ArrayPool<>(Integer.class);
		LinkedList<Integer[]> arrays = (LinkedList<Integer[]>) ReflectionUtils.getHiddenFieldValue(ArrayPool.class, "_arrays", pool);

		int[] sizes = new int[] {
			1, 2, 3, 2, 3, 1, 4, 1, 5, 8, 7, 6, 7, 2
		};
		for (int size : sizes) {
			pool.free(pool.obtain(size, true));
		}

		assertEquals(8, arrays.size());

		int prevSize = arrays.get(0).length;
		for (int i = 1; i < arrays.size(); ++i) {
			if (arrays.get(i).length < prevSize) {
				fail("Internal array is not sorted.");
			}
		}
	}
}
