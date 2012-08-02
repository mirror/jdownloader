package org.jdownloader.extensions.vlcstreaming.upnp;

import java.util.ArrayList;
import java.util.List;

public enum DLNAOrg {
    BACKGROUND_TRANSFERT_MODE(1 << 22),
    BYTE_BASED_SEEK(1 << 29),
    CONNECTION_STALL(1 << 21),
    DLNA_V15(1 << 20),
    INTERACTIVE_TRANSFERT_MODE(1 << 23),
    PLAY_CONTAINER(1 << 28),
    RTSP_PAUSE(1 << 25),
    S0_INCREASE(1 << 27),
    SENDER_PACED(1 << 31),
    SN_INCREASE(1 << 26),
    STREAMING_TRANSFER_MODE(1 << 24),
    TIME_BASED_SEEK(1 << 30);

    private int bit;

    private DLNAOrg(int bit) {
        this.bit = bit;
    }

    public static List<DLNAOrg> parse(int bitmask) {
        ArrayList<DLNAOrg> ret = new ArrayList<DLNAOrg>();

        for (DLNAOrg v : values()) {
            if (v.isIn(bitmask)) {
                ret.add(v);
            }
        }
        return ret;
    }

    private boolean isIn(int bitmask) {
        return (bitmask & bit) > 0;
    }

    public static String create(DLNAOrg... flags) {

        int i = 0;
        for (DLNAOrg flag : flags) {
            i = i | flag.bit;
        }
        String ret = Integer.toHexString(i);
        while (ret.length() < 8)
            ret = "0" + ret;
        ret += "000000000000000000000000";
        return ret;
    }
}
