package net.namekdev.entity_tracker.model;

import com.artemis.utils.BitVector;

public class AspectInfo {
	public BitVector allTypes;
	public BitVector oneTypes;
	public BitVector exclusionTypes;


	public AspectInfo() {
	}

	public AspectInfo(BitVector allTypes, BitVector oneTypes, BitVector exclusionTypes) {
		this.allTypes = allTypes;
		this.oneTypes = oneTypes;
		this.exclusionTypes = exclusionTypes;
	}
}