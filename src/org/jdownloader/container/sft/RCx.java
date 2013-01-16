package org.jdownloader.container.sft;

public class RCx {

    private byte[] pD;
    private int    pI;
    private int    pJ;
    private int    pF;
    private byte[] key;

    public RCx(byte[] key) {
        pD = new byte[256];
        if (key != null) this.init(key);
    }

    public void init(byte[] key) {
        this.key = key;
        this.reset();
    }

    public void reset() {
        byte[] M = new byte[256];
        int L = 0;

        for (int S = 0; S < 256; S++) {
            pD[S] = (byte) S;
            M[S] = (byte) ((((int) key[S % key.length]) ^ L));
            L = (int) (((long) L + ((long) M[S] & 0xFF) * 257) % 2147483647) + 1;
        }

        pI = pJ = 0;
        int R = (L & 0xFF);
        pF = (int) ((L >> 8) & 0xFFFFFFFF);

        for (int S = 0; S < 256; S++) {
            R = ((R + ((int) pD[S] & 0xFF) + ((int) M[S] & 0xFF)) & 0xFF);

            byte T = pD[S];
            pD[S] = pD[R];
            pD[R] = T;
        }
    }

    public void init2(byte[] key) {
        this.key = key;
        this.reset2();
    }

    public void reset2() {
        byte[] M = new byte[256];
        int L = 0;

        for (int S = 0; S < 256; S++) {
            pD[S] = (byte) S;
            M[S] = (byte) ((((int) key[S % key.length]) ^ L));
            L = (int) (((long) L + ((long) M[S] & 0xFF) * 0x14B) % 0x7FFFFFFF) + 1;
        }

        pI = pJ = 0;
        byte R = (byte) (L >> 4);
        pF = (int) ((L >> 8) & 0xFFFFFFFF);

        byte DL = (byte) (L & 0xFF);
        for (int S = 0; S < 256; S++) {
            DL = (byte) (((int) R & 0xFF) + ((int) pD[S] & 0xFF) + ((int) M[S] & 0xFF) + ((int) DL & 0xFF));

            R = pD[S];
            pD[S] = pD[(int) DL & 0xFF];
            pD[(int) DL & 0xFF] = (byte) R;
        }
    }

    public void encode2(byte[] data) {
        if (data == null) return;
        byte swap;
        int n = data.length;
        int p = 0;

        while (n-- != 0) {
            pI++;
            pJ = (pJ + (int) (pD[pI & 0xFF] & 0xFF)) & 0xFF;

            swap = pD[pI & 0xFF];
            pD[pI & 0xFF] = (byte) (pD[pJ & 0xFF] ^ pF);

            pD[pJ & 0xFF] = (byte) (((int) swap & 0xFF) - pF);

            swap = (byte) (((int) swap & 0xFF) + ((int) pD[pI & 0xFF] & 0xFF));
            swap = (byte) (pD[(int) swap & 0xFF] ^ data[p]);

            data[p] = swap;
            pF ^= swap;

            p++;
        }
    }

    public void encode(byte[] data) {
        if (data == null) return;
        for (int C = 0; C < data.length; C++) {
            pI++;
            byte T = pD[pI];

            pJ = pJ + (int) T & 0xFF;

            pD[pI] = (byte) (pD[pJ] ^ pF);
            pD[pJ] = (byte) (T - pF);
            T = (byte) (T + pD[pI]);

            byte K = data[C];
            data[C] = (byte) (K ^ pD[(int) T & 0xFF]);
            pF = pF ^ K;
        }
    }

    public void decode(byte[] data) {
        for (int C = 0; C < data.length; C++) {

            pI++;
            byte T = pD[pI];

            pJ = pJ + (int) T & 0xFF;

            pD[pI] = (byte) (pD[pJ] ^ pF);
            pD[pJ] = (byte) (T - pF);
            T = (byte) (T + pD[pI]);

            byte K = (byte) (data[C] ^ pD[(int) T & 0xFF]);
            data[C] = K;
            pF = pF ^ K;
        }
    }
}
