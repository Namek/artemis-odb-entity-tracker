package net.namekdev.entity_tracker.utils.sample;

public class GameObject {
	public Vector3 pos = new Vector3(1, 2, 3);
	public Vector2 size = new Vector2(10, 5);


	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof GameObject)) {
			return false;
		}

		final GameObject go = (GameObject) obj;

		if (pos == null && go.pos != null || pos != null && go.pos == null) {
			return false;
		}

		if (size == null && go.size != null || size != null && go.size == null) {
			return false;
		}

		if (pos != null) {
			if (!(pos.equals(go.pos))) {
				return false;
			}
		}

		if (size != null) {
			if (!(size.equals(go.size))) {
				return false;
			}
		}

		return true;
	}
}