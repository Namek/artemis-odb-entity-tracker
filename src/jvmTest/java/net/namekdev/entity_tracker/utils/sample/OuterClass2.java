package net.namekdev.entity_tracker.utils.sample;

/**
 * This class test especially reference {@link #c}.
 */
public class OuterClass2 {
    InnerClass a = new InnerClass();
    InnerClass.MoreInnerClass b;

    class InnerClass {
        MoreInnerClass c = new MoreInnerClass();

        InnerClass() {
            b = c;
        }

        class MoreInnerClass {
            InnerClass a;//a1
            MoreInnerClass b;//b1
            MoreInnerClass c;//c1
            OuterClass2 d;
            boolean g = true;
        }
    }
}
