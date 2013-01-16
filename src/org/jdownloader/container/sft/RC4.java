package org.jdownloader.container.sft;

public class RC4 {

    private int[] pD;
    private int   pI;
    private int   pJ;

    public RC4(final byte[] key) {
        pD = new int[256];
        this.init(key);
    }

    public void init(final byte[] key) {
        for (pI = 0; pI < 256; pI++) {
            pD[pI] = pI;
        }

        for (pI = pJ = 0; pI < 256; pI++) {
            int chr = key[pI % key.length] & 0xFF;
            pJ = (pJ + pD[pI] + chr) & 0xFF;
            int x = pD[pI];
            pD[pI] = pD[pJ];
            pD[pJ] = x;
        }
    }

    public void encode(byte[] data) {
        pI = pJ = 0;
        for (int y = 0; y < data.length; y++) {
            pI = (pI + 1) & 0xFF;
            pJ = (pJ + pD[pI]) & 0xFF;
            int x = pD[pI];
            pD[pI] = pD[pJ];
            pD[pJ] = x;

            x = data[y] & 0xff;
            x ^= pD[(pD[pI] + pD[pJ]) & 0xff];
            data[y] = (byte) x;
        }
    }
}
