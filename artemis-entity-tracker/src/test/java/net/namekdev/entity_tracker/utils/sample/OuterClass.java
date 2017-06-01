package net.namekdev.entity_tracker.utils.sample;

/**
 * This class tests references to inner and outer classes.
 */
public class OuterClass {
    InnerClass a = new InnerClass();

    public OuterClass() {
        a.b.c.mutate(this);
    }

    class InnerClass {
        MoreInnerClass b = new MoreInnerClass();

        class MoreInnerClass {
            EvenMoreInnerClass c = new EvenMoreInnerClass();

            class EvenMoreInnerClass {
                OuterClass _a;
                boolean d = false;

                EvenMoreInnerClass() {
                    // at this point `a`, `b` and `c` are null because there's still not constructed
                }

                void mutate(OuterClass _a) {
//                    this._a = _a;
                    a.b.c.d = true;
                }
            }
        }
    }
}
