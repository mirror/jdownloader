package jd.plugins.a;

import jd.nutils.encoding.Encoding;

public class Config {
    public final static byte[] AMZ_IV  = new byte[] { (byte) 0x5E, 0x72, (byte) 0xD7, (byte) 0x9A, 0x11, (byte) 0xB3, 0x4F, (byte) 0xEE };

    public static final byte[] AMZ_SEC = new byte[] { 0x29, (byte) 0xAB, (byte) 0x9D, 0x18, (byte) 0xB2, 0x44, (byte) 0x9E, 0x31 };
    public static final byte[] D       = Encoding.base16Decode("447e787351e60e2c6a96b3964be0c9bd");
    public static final String C       = "46543262752";

    public static final byte[] RSDF    = new byte[] { (byte) 0x8C, (byte) 0x35, (byte) 0x19, (byte) 0x2D, (byte) 0x96, (byte) 0x4D, (byte) 0xC3, (byte) 0x18, (byte) 0x2C, (byte) 0x6F, (byte) 0x84, (byte) 0xF3, (byte) 0x25, (byte) 0x22, (byte) 0x39, (byte) 0xEB, (byte) 0x4A, (byte) 0x32, (byte) 0x0D, (byte) 0x25, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

    public static final byte[] RSDFIV  = new byte[] { (byte) 0xa3, (byte) 0xd5, (byte) 0xa3, (byte) 0x3c, (byte) 0xb9, (byte) 0x5a, (byte) 0xc1, (byte) 0xf5, (byte) 0xcb, (byte) 0xdb, (byte) 0x1a, (byte) 0xd2, (byte) 0x5c, (byte) 0xb0, (byte) 0xa7, (byte) 0xaa };

}
