package net.namekdev.entity_tracker.model;

import java.util.BitSet;

public class AspectInfo {
	public BitSet allTypes;
	public BitSet oneTypes;
	public BitSet exclusionTypes;


	public AspectInfo() {
	}

	public AspectInfo(BitSet allTypes, BitSet oneTypes, BitSet exclusionTypes) {
		this.allTypes = allTypes;
		this.oneTypes = oneTypes;
		this.exclusionTypes = exclusionTypes;
	}
}