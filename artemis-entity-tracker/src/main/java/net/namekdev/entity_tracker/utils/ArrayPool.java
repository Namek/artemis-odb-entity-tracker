package net.namekdev.entity_tracker.utils;

import java.util.Arrays;
import java.util.LinkedList;

import com.artemis.utils.reflect.ArrayReflection;

public class ArrayPool<T> {
	private final Class<T> type;
	private final LinkedList<T[]> _arrays = new LinkedList<>();


	public ArrayPool(Class<T> type) {
		this.type = type;
	}

	public T[] obtain(int size, boolean exactSize) {
		int imin = 0, imax = _arrays.size() - 1;

		while (imax >= imin) {
			int imid = (imax + imin) / 2;
			T[] arr = _arrays.get(imid);

			if (arr.length == size) {
				_arrays.remove(imid);
				return arr;
			}
			else if (arr.length < size) {
				imin = imid + 1;
			}
			else {
				if (!exactSize) {
					return arr;
				}

				imax = imid - 1;
			}
		}

		return create(size);
	}

	public void free(T[] array, boolean nullify) {
		if (nullify) {
			Arrays.fill(array, null);
		}

		int size = array.length;
		int imin = 0, imax = _arrays.size() - 1;
		int index = 0;

		while (imin <= imax) {
			index = (imax + imin) / 2;
			T[] arr = _arrays.get(index);

			if (arr.length == size) {
				break;
			}

			if (arr.length > size) {
				imax = index - 1;
			}
			else {
				imin = index + 1;
			}
		}

		if (index < imin) {
			index = imin;
		}

		_arrays.add(index, array);
	}

	public void free(T[] array) {
		free(array, false);
	}

	@SuppressWarnings("unchecked")
	protected T[] create(int size) {
		return (T[]) ArrayReflection.newInstance(type, size);
	}
}
