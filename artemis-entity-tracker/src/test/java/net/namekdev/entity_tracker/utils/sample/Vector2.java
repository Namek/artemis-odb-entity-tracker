package net.namekdev.entity_tracker.utils.sample;

public class Vector2 {
	public float x, y;

	public Vector2(float x, float y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Vector2)) {
			return false;
		}

		final Vector2 v2 = (Vector2) obj;

		return x == v2.x && y == v2.y;
	}
}
