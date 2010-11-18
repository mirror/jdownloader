package jd.plugins.optional.remoteserv.test;

import jd.plugins.optional.remoteserv.RemoteCallData;

public class DataImpl implements RemoteCallData {
    private int      i         = 3;
    private double   d         = 0.43d;
    private long     smallLong = 4l;
    private long     bigLong   = Long.MAX_VALUE;
    private float    f         = 0.123f;
    private String   string    = "I'm a string";
    private DataImpl obj       = null;

    public long getBigLong() {
        return this.bigLong;
    }

    public double getD() {
        return this.d;
    }

    public float getF() {
        return this.f;
    }

    public int getI() {
        return this.i;
    }

    public DataImpl getObj() {
        return this.obj;
    }

    public long getSmallLong() {
        return this.smallLong;
    }

    public String getString() {
        return this.string;
    }

    public void setBigLong(final long bigLong) {
        this.bigLong = bigLong;
    }

    public void setD(final double d) {
        this.d = d;
    }

    public void setF(final float f) {
        this.f = f;
    }

    public void setI(final int i) {
        this.i = i;
    }

    public void setObj(final DataImpl obj) {
        this.obj = obj;
    }

    public void setSmallLong(final long smallLong) {
        this.smallLong = smallLong;
    }

    public void setString(final String string) {
        this.string = string;
    }
}
