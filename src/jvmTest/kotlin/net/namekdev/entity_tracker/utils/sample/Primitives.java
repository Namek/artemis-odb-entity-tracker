package net.namekdev.entity_tracker.utils.sample;

public class Primitives {
    public boolean boolp = true;
    public Boolean boolRef = true;
    public int intp = 124;
    public Integer intRef = 124;

    public Primitives() {
    }

    public Primitives(boolean boolp, Boolean boolRef, int intp, Integer intRef) {
        this.boolp = boolp;
        this.boolRef = boolRef;
        this.intp = intp;
        this.intRef = intRef;
    }

    public Primitives(Boolean boolRef, Integer intRef) {
        this.boolRef = boolRef;
        this.intRef = intRef;
    }
}