package net.namekdev.entity_tracker.utils.sample;

/**
 * This class test especially reference {@link #c}.
 */
public class OuterClass2 {
    InnerClass a = new InnerClass();
    InnerClass.MoreInnerClass c;

    class InnerClass {
        MoreInnerClass b = new MoreInnerClass();

        InnerClass() {
            c = b;
        }

        class MoreInnerClass {
            OuterClass2 d;
            InnerClass e;
            MoreInnerClass f;
            boolean g = true;
        }
    }
}
