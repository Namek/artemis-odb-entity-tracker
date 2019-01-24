package net.namekdev.entity_tracker.utils.sample;

public class EnumFullTestClass {
	TestEnum enumUndefined = null;
	TestEnum enumValued = TestEnum.First;

	TestEnum[] enums = new TestEnum[] {
		TestEnum.First,
		TestEnum.Second,
		TestEnum.Third,
		null
	};

	public Object getEnumUndefined() {
		return enumUndefined;
	}

	public Object getEnumValued() {
		return enumValued;
	}

	public TestEnum[] getEnums() {
		return enums;
	}
}