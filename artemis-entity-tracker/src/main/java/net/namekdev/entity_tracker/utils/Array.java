package net.namekdev.entity_tracker.utils;

import java.util.ArrayList;

public class Array<T> {
	protected final ArrayList<T> _array;


	public Array() {
		_array = new ArrayList<T>();
	}

	public Array(int capacity) {
		_array = new ArrayList<T>(capacity);
	}

	public T get(int index) {
		return _array.get(index);
	}

	public void set(int index, T value) {
		ensureSize(index+1);
		_array.set(index, value);
	}

	public void ensureSize(int size) {
		_array.ensureCapacity(size);

		int i = _array.size();
		while (i++ < size) {
			_array.add(null);
		}
	}
}
