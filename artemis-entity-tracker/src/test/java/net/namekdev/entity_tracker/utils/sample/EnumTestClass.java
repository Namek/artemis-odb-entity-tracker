package net.namekdev.entity_tracker.utils.sample;

public class EnumTestClass {
	public static enum TestEnum {
		First,
		Second,
		Third
	}
	
	
	TestEnum enumUndefined;
	TestEnum enumValued = TestEnum.First;

	public Object getEnumUndefined() {
		return enumUndefined;
	}

	public Object getEnumValued() {
		return enumValued;
	}
}