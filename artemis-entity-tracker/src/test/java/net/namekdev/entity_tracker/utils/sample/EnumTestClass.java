package net.namekdev.entity_tracker.utils.sample;

public class EnumTestClass {
	public static enum TestEnum {
		First,
		Second,
		Third
	}
	
	
	TestEnum enumUndefined = null;
	TestEnum enumValued = TestEnum.First;

	TestEnum[] enums = new TestEnum[] {
		TestEnum.First,
		TestEnum.Second,
		TestEnum.Third
	};

	public Object getEnumUndefined() {
		return enumUndefined;
	}

	public Object getEnumValued() {
		return enumValued;
	}
}