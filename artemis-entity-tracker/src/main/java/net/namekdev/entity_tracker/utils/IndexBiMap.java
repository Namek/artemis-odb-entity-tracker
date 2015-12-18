package net.namekdev.entity_tracker.utils;

public class IndexBiMap extends Array<Integer> {
	public IndexBiMap() {
		super();
	}

	public IndexBiMap(int capacity) {
		super(capacity);
	}

	public int getLocalIndex(int globalIndex) {
		for (int i = 0, n = _array.size(); i < n; ++i) {
			Integer val = _array.get(i);

			if (val == null) {
				break;
			}

			if (val == globalIndex) {
				return i;
			}
		}

		return -1;
	}

	public int getGlobalIndex(int localIndex) {
		return _array.get(localIndex);
	}
}
