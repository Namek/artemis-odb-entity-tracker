package net.namekdev.entity_tracker.utils.sample;

public class CyclicClassIndirectly {
	public OtherClass obj;
	
	public static class OtherClass {
		public CyclicClassIndirectly obj;
	}
	
}
