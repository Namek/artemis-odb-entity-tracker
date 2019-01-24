package net.namekdev.entity_tracker.utils.sample;

public class GameState {
	public GameObject[] objects;


	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof GameState)) {
			return false;
		}

		final GameState gs = (GameState) obj;

		if (objects == null && gs.objects != null || objects != null && gs.objects == null) {
			return false;
		}

		if (objects != null) {
			if (objects.length != gs.objects.length) {
				return false;
			}

			for (int i = 0, n = objects.length; i < n; ++i) {
				if (objects[i] == null && gs.objects[i] != null) {
					return false;
				}

				if (objects[i] != null && gs.objects[i] == null) {
					return false;
				}

				if (objects[i] != null) {
					if (!objects[i].equals(gs.objects[i])) {
						return false;
					}
				}
			}
		}

		return true;
	}
}
