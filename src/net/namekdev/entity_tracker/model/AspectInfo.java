package net.namekdev.entity_tracker.model;

import java.util.BitSet;
import java.util.Map;

import com.artemis.Component;

public class AspectInfo {
	public Map<String, Class<? extends Component>> allTypes;
	public Map<String, Class<? extends Component>> oneTypes;
	public Map<String, Class<? extends Component>> exclusionTypes;

	public BitSet allTypesBitset;
	public BitSet oneTypesBitset;
	public BitSet exclusionTypesBitset;


	public String toString() {
		StringBuilder sb = new StringBuilder();

		int n = allTypes.size();
		if (n > 0) {
			sb.append("(");
			for (int i = 0; i < n; ++i) {

				if (i != n-1) {
					sb.append(" & ");
				}
			}
			sb.append(")");
		}

		n = oneTypes.size();
		if (n > 0) {
			sb.append("  (");
			for (int i = 0; i < n; ++i) {

				if (i != n-1) {
					sb.append(" | ");
				}
			}
			sb.append(")");
		}

		n = exclusionTypes.size();
		if (n > 0) {
			sb.append("  ~(");
			for (int i = 0; i < n; ++i) {

				if (i != n-1) {
					sb.append(" & ");
				}
			}
			sb.append(")");
		}

		return sb.toString().trim();
	}
}