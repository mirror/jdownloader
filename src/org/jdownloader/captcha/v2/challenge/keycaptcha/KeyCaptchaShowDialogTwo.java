package org.jdownloader.captcha.v2.challenge.keycaptcha;

public class KeyCaptchaShowDialogTwo {
    int    x;
    int    y;
    byte[] z = new byte[256];

    final int A() {
        int x;
        int y;
        int zx, zy;

        x = this.x + 1 & 0xff;
        zx = z[x];
        y = zx + this.y & 0xff;
        zy = z[y];
        this.x = x;
        this.y = y;
        z[y] = (byte) (zx & 0xff);
        z[x] = (byte) (zy & 0xff);
        return z[(zx + zy & 0xff)];
    }

    private void B(final byte[] b) {
        int p, o;
        int w;
        int m;
        int n;

        for (n = 0; n < 256; n++) {
            z[n] = (byte) n;
        }
        w = 0;
        m = 0;
        for (n = 0; n < 256; n++) {
            p = z[n];
            m = m + b[w] + p & 0xff;
            o = z[m];
            z[m] = (byte) (p & 0xff);
            z[n] = (byte) (o & 0xff);
            if (++w >= b.length) {
                w = 0;
            }
        }
    }

    public synchronized void C(final byte[] a, final int b, final byte[] c, final int d, final int e) {
        final int end = b + e;
        for (int si = b, di = d; si < end; si++, di++) {
            c[di] = (byte) ((a[si] ^ A()) & 0xff);
        }
    }

    public byte[] D(final byte[] a, final byte[] b) {
        B(a);
        final byte[] dest = new byte[b.length];
        C(b, 0, dest, 0, b.length);
        return dest;
    }

}