package net.namekdev.entity_tracker.utils.sample;

/**
 *
 */
public class RepeatingModelsTestClass {
    RepeatedClass a;
    RepeatedClass b;
    AnotherClass c;


    static class AnotherClass {
        /**
         * model for this field should be different than 'a' and 'b'!
         */
        RepeatedClass d;
    }

    public static class RepeatedClass {
        boolean omg = true;
    }
}
