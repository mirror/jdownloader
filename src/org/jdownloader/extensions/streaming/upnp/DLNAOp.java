package org.jdownloader.extensions.streaming.upnp;

import java.util.ArrayList;
import java.util.List;

public enum DLNAOp {
    RANGE_SEEK_SUPPORTED(0x01),
    BYTE_SEEK_SUPPORTED(0x10);

    private int bit;

    private DLNAOp(int bit) {
        this.bit = bit;
    }

    public static List<DLNAOp> parse(int bitmask) {
        ArrayList<DLNAOp> ret = new ArrayList<DLNAOp>();

        for (DLNAOp v : values()) {
            if (v.isIn(bitmask)) {
                ret.add(v);
            }
        }
        return ret;
    }

    private boolean isIn(int bitmask) {
        return (bitmask & bit) > 0;
    }

    public static String create(DLNAOp... flags) {

        int i = 0;
        for (DLNAOp flag : flags) {
            i = i | flag.bit;
        }
        String ret = Integer.toHexString(i);
        while (ret.length() < 2)
            ret = "0" + ret;

        return ret;
    }
}
