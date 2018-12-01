package net.namekdev.entity_tracker.utils.sample;

public class Vector3 {
	public float x, y, z;

	public Vector3(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Vector3)) {
			return false;
		}

		final Vector3 v3 = (Vector3) obj;

		return x == v3.x && y == v3.y && z == v3.z;
	}
}
