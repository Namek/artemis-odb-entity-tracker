package net.namekdev.entity_tracker.utils.sample;

public class EnumArrayTestClass {
    TestEnum[] enums = new TestEnum[] {
        TestEnum.First,
        TestEnum.Second,
        TestEnum.Third,
        null
    };

    public TestEnum[] getEnums() {
        return enums;
    }
}
