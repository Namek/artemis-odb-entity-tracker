package net.namekdev.entity_tracker.utils.sample;

public class CyclicClassIndirectly {
	public OtherClass obj;
	public ArrayClass arr;
	
	public static class OtherClass {
		public CyclicClassIndirectly obj;
	}
	
	
	public static class ArrayClass {
		public CyclicClassIndirectly[] objs;
	}
}
