package jd.captcha.specials;

import java.awt.image.BufferedImage;

public class Hq2x {

    // -------------------------- HQ2x ---------------------------------------

    protected static final long Amask = 0xFF000000l;
    protected static final long Ymask = 0x00FF0000l;
    protected static final long Umask = 0x0000FF00l;
    protected static final long Vmask = 0x000000FFl;
    protected static final long trA   = 0xF0000000l;
    protected static final long trY   = 0x00300000l;
    protected static final long trU   = 0x00000700l;
    protected static final long trV   = 0x00000006l;

    protected static long abs(final long v) {
        if (v >= 0) { return v; }

        return -v;
    }

    protected static boolean diff(final long w1, final long w2) {
        final long YUV1 = Hq2x.RGBA32toYUVA(w1);
        final long YUV2 = Hq2x.RGBA32toYUVA(w2);

        return Hq2x.abs((YUV1 & Hq2x.Amask) - (YUV2 & Hq2x.Amask)) > Hq2x.trA || Hq2x.abs((YUV1 & Hq2x.Ymask) - (YUV2 & Hq2x.Ymask)) > Hq2x.trY || Hq2x.abs((YUV1 & Hq2x.Umask) - (YUV2 & Hq2x.Umask)) > Hq2x.trU || Hq2x.abs((YUV1 & Hq2x.Vmask) - (YUV2 & Hq2x.Vmask)) > Hq2x.trV;
    }

    protected static long RGBA32toYUVA(final long rgba) {
        final long b = rgba & 0x000000FF;
        final long g = (rgba & 0x0000FF00) >> 8;
        final long r = (rgba & 0x00FF0000) >> 16;
        final long a = (rgba & 0xFF000000) >> 24;

        final long Y = r + g + b >> 2;
        final long u = 128 + (r - b >> 2);
        final long v = 128 + (-r + 2 * g - b >> 3);

        return (a << 24) + (Y << 16) + (u << 8) + v;
        // return (Y<<16) + (u<<8) + v;
    }

    protected void hq2x_32(final BufferedImage pIn, final BufferedImage pOut) {
        final int xres = pIn.getWidth();
        final int yres = pIn.getHeight();

        int i, j, k;
        int prevline, nextline;
        final long w[] = new long[10];

        // +----+----+----+
        // | | | |
        // | w1 | w2 | w3 |
        // +----+----+----+
        // | | | |
        // | w4 | w5 | w6 |
        // +----+----+----+
        // | | | |
        // | w7 | w8 | w9 |
        // +----+----+----+

        for (j = 0; j < yres; j++) {
            final int jj = j * 2;

            if (j > 0) {
                prevline = j - 1;
            } else {
                prevline = j;
            }
            if (j < yres - 1) {
                nextline = j + 1;
            } else {
                nextline = j;
            }

            for (i = 0; i < xres; i++) {
                final int ii = i * 2;

                w[2] = pIn.getRGB(i, prevline);
                w[5] = pIn.getRGB(i, j);
                w[8] = pIn.getRGB(i, nextline);

                if (i > 0) {
                    w[1] = pIn.getRGB(i - 1, prevline);
                    w[4] = pIn.getRGB(i - 1, j);
                    w[7] = pIn.getRGB(i - 1, nextline);
                } else {
                    w[1] = w[2];
                    w[4] = w[5];
                    w[7] = w[8];
                }

                if (i < xres - 1) {
                    w[3] = pIn.getRGB(i + 1, prevline);
                    w[6] = pIn.getRGB(i + 1, j);
                    w[9] = pIn.getRGB(i + 1, nextline);
                } else {
                    w[3] = w[2];
                    w[6] = w[5];
                    w[9] = w[8];
                }

                int pattern = 0;
                int flag = 1;

                final long YUV1 = Hq2x.RGBA32toYUVA(w[5]);

                for (k = 1; k <= 9; k++) {
                    if (k == 5) {
                        continue;
                    }

                    if (w[k] != w[5]) {
                        final long YUV2 = Hq2x.RGBA32toYUVA(w[k]);
                        if (Hq2x.abs((YUV1 & Hq2x.Amask) - (YUV2 & Hq2x.Amask)) > Hq2x.trA || Hq2x.abs((YUV1 & Hq2x.Ymask) - (YUV2 & Hq2x.Ymask)) > Hq2x.trY || Hq2x.abs((YUV1 & Hq2x.Umask) - (YUV2 & Hq2x.Umask)) > Hq2x.trU || Hq2x.abs((YUV1 & Hq2x.Vmask) - (YUV2 & Hq2x.Vmask)) > Hq2x.trV) {
                            pattern |= flag;
                        }
                    }
                    flag <<= 1;
                }

                switch (pattern) {
                case 0:
                case 1:
                case 4:
                case 32:
                case 128:
                case 5:
                case 132:
                case 160:
                case 33:
                case 129:
                case 36:
                case 133:
                case 164:
                case 161:
                case 37:
                case 165:
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    break;

                case 2:
                case 34:
                case 130:
                case 162:
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    break;
                case 16:
                case 17:
                case 48:
                case 49:
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                case 64:
                case 65:
                case 68:
                case 69:
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                case 8:
                case 12:
                case 136:
                case 140:
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    break;
                case 3:
                case 35:
                case 131:
                case 163:
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    break;
                case 6:
                case 38:
                case 134:
                case 166:
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    break;
                case 20:
                case 21:
                case 52:
                case 53:
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                case 144:
                case 145:
                case 176:
                case 177:
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    break;
                case 192:
                case 193:
                case 196:
                case 197: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    break;
                }
                case 96:
                case 97:
                case 100:
                case 101: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                }
                case 40:
                case 44:
                case 168:
                case 172: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    break;
                }
                case 9:
                case 13:
                case 137:
                case 141: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    break;
                }
                case 18:
                case 50: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                }
                case 80:
                case 81: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    }
                    break;
                }
                case 72:
                case 76: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    }
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                }
                case 10:
                case 138: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    } else {
                        pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    }
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    break;
                }
                case 66: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                }
                case 24: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                }
                case 7:
                case 39:
                case 135: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    break;
                }
                case 148:
                case 149:
                case 180: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    break;
                }
                case 224:
                case 228:
                case 225: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    break;
                }
                case 41:
                case 169:
                case 45: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    break;
                }
                case 22:
                case 54: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                }
                case 208:
                case 209: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    }
                    break;
                }
                case 104:
                case 108: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    }
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                }
                case 11:
                case 139: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    }
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    break;
                }
                case 19:
                case 51: {
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    } else {
                        pOut.setRGB(ii, jj, this.interp6(w[5], w[2], w[4])); // PIXEL00_60
                        pOut.setRGB(ii + 1, jj, this.interp9(w[5], w[2], w[6])); // PIXEL01_90
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                }
                case 146:
                case 178: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp9(w[5], w[2], w[6])); // PIXEL01_90
                        pOut.setRGB(ii + 1, jj + 1, this.interp6(w[5], w[6], w[8])); // PIXEL11_61
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    break;
                }
                case 84:
                case 85: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp6(w[5], w[6], w[2])); // PIXEL01_60
                        pOut.setRGB(ii + 1, jj + 1, this.interp9(w[5], w[6], w[8])); // PIXEL11_90
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    break;
                }
                case 112:
                case 113: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp6(w[5], w[8], w[4])); // PIXEL10_61
                        pOut.setRGB(ii + 1, jj + 1, this.interp9(w[5], w[6], w[8])); // PIXEL11_90
                    }
                    break;
                }
                case 200:
                case 204: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp9(w[5], w[8], w[4])); // PIXEL10_90
                        pOut.setRGB(ii + 1, jj + 1, this.interp6(w[5], w[8], w[6])); // PIXEL11_60
                    }
                    break;
                }
                case 73:
                case 77: {
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    } else {
                        pOut.setRGB(ii, jj, this.interp6(w[5], w[8], w[4])); // PIXEL00_61
                        pOut.setRGB(ii, jj + 1, this.interp9(w[5], w[8], w[4])); // PIXEL10_90
                    }
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                }
                case 42:
                case 170: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    } else {
                        pOut.setRGB(ii, jj, this.interp9(w[5], w[4], w[2])); // PIXEL00_90
                        pOut.setRGB(ii, jj + 1, this.interp6(w[5], w[4], w[8])); // PIXEL10_60
                    }
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    break;
                }
                case 14:
                case 142: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    } else {
                        pOut.setRGB(ii, jj, this.interp9(w[5], w[4], w[2])); // PIXEL00_90
                        pOut.setRGB(ii + 1, jj, this.interp6(w[5], w[2], w[6])); // PIXEL01_61
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    break;
                }
                case 67: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                }
                case 70: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                }
                case 28: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                }
                case 152: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    break;
                }
                case 194: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    break;
                }
                case 98: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                }
                case 56: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                }
                case 25: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                }
                case 26:
                case 31: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    }
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                }
                case 82:
                case 214: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    }
                    break;
                }
                case 88:
                case 248: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    }
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    }
                    break;
                }
                case 74:
                case 107: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    }
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    }
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                }
                case 27: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    }
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                }
                case 86: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    break;
                }
                case 216: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    }
                    break;
                }
                case 106: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    }
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                }
                case 30: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                }
                case 210: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    }
                    break;
                }
                case 120: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    }
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    break;
                }
                case 75: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    }
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                }
                case 29: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                }
                case 198: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    break;
                }
                case 184: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    break;
                }
                case 99: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                }
                case 57: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                }
                case 71: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                }
                case 156: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    break;
                }
                case 226: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    break;
                }
                case 60: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                }
                case 195: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    break;
                }
                case 102: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                }
                case 153: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    break;
                }
                case 58: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    } else {
                        pOut.setRGB(ii, jj, this.interp7(w[5], w[4], w[2])); // PIXEL00_70
                    }
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp7(w[5], w[2], w[6])); // PIXEL01_70
                    }
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                }
                case 83: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp7(w[5], w[2], w[6])); // PIXEL01_70
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp7(w[5], w[6], w[8])); // PIXEL11_70
                    }
                    break;
                }
                case 92: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp7(w[5], w[8], w[4])); // PIXEL10_70
                    }
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp7(w[5], w[6], w[8])); // PIXEL11_70
                    }
                    break;
                }
                case 202: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    } else {
                        pOut.setRGB(ii, jj, this.interp7(w[5], w[4], w[2])); // PIXEL00_70
                    }
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp7(w[5], w[8], w[4])); // PIXEL10_70
                    }
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    break;
                }
                case 78: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    } else {
                        pOut.setRGB(ii, jj, this.interp7(w[5], w[4], w[2])); // PIXEL00_70
                    }
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp7(w[5], w[8], w[4])); // PIXEL10_70
                    }
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                }
                case 154: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    } else {
                        pOut.setRGB(ii, jj, this.interp7(w[5], w[4], w[2])); // PIXEL00_70
                    }
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp7(w[5], w[2], w[6])); // PIXEL01_70
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    break;
                }
                case 114: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp7(w[5], w[2], w[6])); // PIXEL01_70
                    }
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp7(w[5], w[6], w[8])); // PIXEL11_70
                    }
                    break;
                }
                case 89: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp7(w[5], w[8], w[4])); // PIXEL10_70
                    }
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp7(w[5], w[6], w[8])); // PIXEL11_70
                    }
                    break;
                }
                case 90: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    } else {
                        pOut.setRGB(ii, jj, this.interp7(w[5], w[4], w[2])); // PIXEL00_70
                    }
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp7(w[5], w[2], w[6])); // PIXEL01_70
                    }
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp7(w[5], w[8], w[4])); // PIXEL10_70
                    }
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp7(w[5], w[6], w[8])); // PIXEL11_70
                    }
                    break;
                }
                case 55:
                case 23: {
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp6(w[5], w[2], w[4])); // PIXEL00_60
                        pOut.setRGB(ii + 1, jj, this.interp9(w[5], w[2], w[6])); // PIXEL01_90
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                }
                case 182:
                case 150: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp9(w[5], w[2], w[6])); // PIXEL01_90
                        pOut.setRGB(ii + 1, jj + 1, this.interp6(w[5], w[6], w[8])); // PIXEL11_61
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    break;
                }
                case 213:
                case 212: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp6(w[5], w[6], w[2])); // PIXEL01_60
                        pOut.setRGB(ii + 1, jj + 1, this.interp9(w[5], w[6], w[8])); // PIXEL11_90
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    break;
                }
                case 241:
                case 240: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp6(w[5], w[8], w[4])); // PIXEL10_61
                        pOut.setRGB(ii + 1, jj + 1, this.interp9(w[5], w[6], w[8])); // PIXEL11_90
                    }
                    break;
                }
                case 236:
                case 232: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp9(w[5], w[8], w[4])); // PIXEL10_90
                        pOut.setRGB(ii + 1, jj + 1, this.interp6(w[5], w[8], w[6])); // PIXEL11_60
                    }
                    break;
                }
                case 109:
                case 105: {
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp6(w[5], w[8], w[4])); // PIXEL00_61
                        pOut.setRGB(ii, jj + 1, this.interp9(w[5], w[8], w[4])); // PIXEL10_90
                    }
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                }
                case 171:
                case 43: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    } else {
                        pOut.setRGB(ii, jj, this.interp9(w[5], w[4], w[2])); // PIXEL00_90
                        pOut.setRGB(ii, jj + 1, this.interp6(w[5], w[4], w[8])); // PIXEL10_60
                    }
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    break;
                }
                case 143:
                case 15: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    } else {
                        pOut.setRGB(ii, jj, this.interp9(w[5], w[4], w[2])); // PIXEL00_90
                        pOut.setRGB(ii + 1, jj, this.interp6(w[5], w[2], w[6])); // PIXEL01_61
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    break;
                }
                case 124: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    }
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    break;
                }
                case 203: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    }
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    break;
                }
                case 62: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    }
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                }
                case 211: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    }
                    break;
                }
                case 118: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    }
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    break;
                }
                case 217: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    }
                    break;
                }
                case 110: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    }
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                }
                case 155: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    }
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    break;
                }
                case 188: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    break;
                }
                case 185: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    break;
                }
                case 61: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                }
                case 157: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    break;
                }
                case 103: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                }
                case 227: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    break;
                }
                case 230: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    break;
                }
                case 199: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    break;
                }
                case 220: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp7(w[5], w[8], w[4])); // PIXEL10_70
                    }
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    }
                    break;
                }
                case 158: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    } else {
                        pOut.setRGB(ii, jj, this.interp7(w[5], w[4], w[2])); // PIXEL00_70
                    }
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    break;
                }
                case 234: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    } else {
                        pOut.setRGB(ii, jj, this.interp7(w[5], w[4], w[2])); // PIXEL00_70
                    }
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    }
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    break;
                }
                case 242: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp7(w[5], w[2], w[6])); // PIXEL01_70
                    }
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    }
                    break;
                }
                case 59: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    }
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp7(w[5], w[2], w[6])); // PIXEL01_70
                    }
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                }
                case 121: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    }
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp7(w[5], w[6], w[8])); // PIXEL11_70
                    }
                    break;
                }
                case 87: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp7(w[5], w[6], w[8])); // PIXEL11_70
                    }
                    break;
                }
                case 79: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    }
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp7(w[5], w[8], w[4])); // PIXEL10_70
                    }
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                }
                case 122: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    } else {
                        pOut.setRGB(ii, jj, this.interp7(w[5], w[4], w[2])); // PIXEL00_70
                    }
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp7(w[5], w[2], w[6])); // PIXEL01_70
                    }
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    }
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp7(w[5], w[6], w[8])); // PIXEL11_70
                    }
                    break;
                }
                case 94: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    } else {
                        pOut.setRGB(ii, jj, this.interp7(w[5], w[4], w[2])); // PIXEL00_70
                    }
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    }
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp7(w[5], w[8], w[4])); // PIXEL10_70
                    }
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp7(w[5], w[6], w[8])); // PIXEL11_70
                    }
                    break;
                }
                case 218: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    } else {
                        pOut.setRGB(ii, jj, this.interp7(w[5], w[4], w[2])); // PIXEL00_70
                    }
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp7(w[5], w[2], w[6])); // PIXEL01_70
                    }
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp7(w[5], w[8], w[4])); // PIXEL10_70
                    }
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    }
                    break;
                }
                case 91: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    }
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp7(w[5], w[2], w[6])); // PIXEL01_70
                    }
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp7(w[5], w[8], w[4])); // PIXEL10_70
                    }
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp7(w[5], w[6], w[8])); // PIXEL11_70
                    }
                    break;
                }
                case 229: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    break;
                }
                case 167: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    break;
                }
                case 173: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    break;
                }
                case 181: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    break;
                }
                case 186: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    } else {
                        pOut.setRGB(ii, jj, this.interp7(w[5], w[4], w[2])); // PIXEL00_70
                    }
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp7(w[5], w[2], w[6])); // PIXEL01_70
                    }
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    break;
                }
                case 115: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp7(w[5], w[2], w[6])); // PIXEL01_70
                    }
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp7(w[5], w[6], w[8])); // PIXEL11_70
                    }
                    break;
                }
                case 93: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp7(w[5], w[8], w[4])); // PIXEL10_70
                    }
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp7(w[5], w[6], w[8])); // PIXEL11_70
                    }
                    break;
                }
                case 206: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    } else {
                        pOut.setRGB(ii, jj, this.interp7(w[5], w[4], w[2])); // PIXEL00_70
                    }
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp7(w[5], w[8], w[4])); // PIXEL10_70
                    }
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    break;
                }
                case 205:
                case 201: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp7(w[5], w[8], w[4])); // PIXEL10_70
                    }
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    break;
                }
                case 174:
                case 46: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    } else {
                        pOut.setRGB(ii, jj, this.interp7(w[5], w[4], w[2])); // PIXEL00_70
                    }
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    break;
                }
                case 179:
                case 147: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp7(w[5], w[2], w[6])); // PIXEL01_70
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    break;
                }
                case 117:
                case 116: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp7(w[5], w[6], w[8])); // PIXEL11_70
                    }
                    break;
                }
                case 189: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    break;
                }
                case 231: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    break;
                }
                case 126: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    }
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    }
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    break;
                }
                case 219: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    }
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    }
                    break;
                }
                case 125: {
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp6(w[5], w[8], w[4])); // PIXEL00_61
                        pOut.setRGB(ii, jj + 1, this.interp9(w[5], w[8], w[4])); // PIXEL10_90
                    }
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    break;
                }
                case 221: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp6(w[5], w[6], w[2])); // PIXEL01_60
                        pOut.setRGB(ii + 1, jj + 1, this.interp9(w[5], w[6], w[8])); // PIXEL11_90
                    }
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    break;
                }
                case 207: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                        pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    } else {
                        pOut.setRGB(ii, jj, this.interp9(w[5], w[4], w[2])); // PIXEL00_90
                        pOut.setRGB(ii + 1, jj, this.interp6(w[5], w[2], w[6])); // PIXEL01_61
                    }
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    break;
                }
                case 238: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp9(w[5], w[8], w[4])); // PIXEL10_90
                        pOut.setRGB(ii + 1, jj + 1, this.interp6(w[5], w[8], w[6])); // PIXEL11_60
                    }
                    break;
                }
                case 190: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                        pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp9(w[5], w[2], w[6])); // PIXEL01_90
                        pOut.setRGB(ii + 1, jj + 1, this.interp6(w[5], w[6], w[8])); // PIXEL11_61
                    }
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    break;
                }
                case 187: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    } else {
                        pOut.setRGB(ii, jj, this.interp9(w[5], w[4], w[2])); // PIXEL00_90
                        pOut.setRGB(ii, jj + 1, this.interp6(w[5], w[4], w[8])); // PIXEL10_60
                    }
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    break;
                }
                case 243: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp6(w[5], w[8], w[4])); // PIXEL10_61
                        pOut.setRGB(ii + 1, jj + 1, this.interp9(w[5], w[6], w[8])); // PIXEL11_90
                    }
                    break;
                }
                case 119: {
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp6(w[5], w[2], w[4])); // PIXEL00_60
                        pOut.setRGB(ii + 1, jj, this.interp9(w[5], w[2], w[6])); // PIXEL01_90
                    }
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    break;
                }
                case 237:
                case 233: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp10(w[5], w[8], w[4])); // PIXEL10_100
                    }
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    break;
                }
                case 175:
                case 47: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp10(w[5], w[4], w[2])); // PIXEL00_100
                    }
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    break;
                }
                case 183:
                case 151: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp10(w[5], w[2], w[6])); // PIXEL01_100
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    break;
                }
                case 245:
                case 244: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp10(w[5], w[6], w[8])); // PIXEL11_100
                    }
                    break;
                }
                case 250: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    }
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    }
                    break;
                }
                case 123: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    }
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    }
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    break;
                }
                case 95: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    }
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    }
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    break;
                }
                case 222: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    }
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    }
                    break;
                }
                case 252: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[2])); // PIXEL00_21
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    }
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp10(w[5], w[6], w[8])); // PIXEL11_100
                    }
                    break;
                }
                case 249: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[2])); // PIXEL01_22
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp10(w[5], w[8], w[4])); // PIXEL10_100
                    }
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    }
                    break;
                }
                case 235: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    }
                    pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[3], w[6])); // PIXEL01_21
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp10(w[5], w[8], w[4])); // PIXEL10_100
                    }
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    break;
                }
                case 111: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp10(w[5], w[4], w[2])); // PIXEL00_100
                    }
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    }
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[6])); // PIXEL11_22
                    break;
                }
                case 63: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp10(w[5], w[4], w[2])); // PIXEL00_100
                    }
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    }
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[9], w[8])); // PIXEL11_21
                    break;
                }
                case 159: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    }
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp10(w[5], w[2], w[6])); // PIXEL01_100
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[8])); // PIXEL10_22
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    break;
                }
                case 215: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp10(w[5], w[2], w[6])); // PIXEL01_100
                    }
                    pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[7], w[4])); // PIXEL10_21
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    }
                    break;
                }
                case 246: {
                    pOut.setRGB(ii, jj, this.interp2(w[5], w[1], w[4])); // PIXEL00_22
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    }
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp10(w[5], w[6], w[8])); // PIXEL11_100
                    }
                    break;
                }
                case 254: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[1])); // PIXEL00_10
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    }
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    }
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp10(w[5], w[6], w[8])); // PIXEL11_100
                    }
                    break;
                }
                case 253: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[2])); // PIXEL00_12
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[2])); // PIXEL01_11
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp10(w[5], w[8], w[4])); // PIXEL10_100
                    }
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp10(w[5], w[6], w[8])); // PIXEL11_100
                    }
                    break;
                }
                case 251: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    }
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[3])); // PIXEL01_10
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp10(w[5], w[8], w[4])); // PIXEL10_100
                    }
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    }
                    break;
                }
                case 239: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp10(w[5], w[4], w[2])); // PIXEL00_100
                    }
                    pOut.setRGB(ii + 1, jj, this.interp1(w[5], w[6])); // PIXEL01_12
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp10(w[5], w[8], w[4])); // PIXEL10_100
                    }
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[6])); // PIXEL11_11
                    break;
                }
                case 127: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp10(w[5], w[4], w[2])); // PIXEL00_100
                    }
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp2(w[5], w[2], w[6])); // PIXEL01_20
                    }
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp2(w[5], w[8], w[4])); // PIXEL10_20
                    }
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[9])); // PIXEL11_10
                    break;
                }
                case 191: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp10(w[5], w[4], w[2])); // PIXEL00_100
                    }
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp10(w[5], w[2], w[6])); // PIXEL01_100
                    }
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[8])); // PIXEL10_11
                    pOut.setRGB(ii + 1, jj + 1, this.interp1(w[5], w[8])); // PIXEL11_12
                    break;
                }
                case 223: {
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp2(w[5], w[4], w[2])); // PIXEL00_20
                    }
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp10(w[5], w[2], w[6])); // PIXEL01_100
                    }
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[7])); // PIXEL10_10
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp2(w[5], w[6], w[8])); // PIXEL11_20
                    }
                    break;
                }
                case 247: {
                    pOut.setRGB(ii, jj, this.interp1(w[5], w[4])); // PIXEL00_11
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp10(w[5], w[2], w[6])); // PIXEL01_100
                    }
                    pOut.setRGB(ii, jj + 1, this.interp1(w[5], w[4])); // PIXEL10_12
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp10(w[5], w[6], w[8])); // PIXEL11_100
                    }
                    break;
                }
                case 255:
                    if (Hq2x.diff(w[4], w[2])) {
                        pOut.setRGB(ii, jj, (int) w[5]); // PIXEL00_0
                    } else {
                        pOut.setRGB(ii, jj, this.interp10(w[5], w[4], w[2])); // PIXEL00_100
                    }
                    if (Hq2x.diff(w[2], w[6])) {
                        pOut.setRGB(ii + 1, jj, (int) w[5]); // PIXEL01_0
                    } else {
                        pOut.setRGB(ii + 1, jj, this.interp10(w[5], w[2], w[6])); // PIXEL01_100
                    }
                    if (Hq2x.diff(w[8], w[4])) {
                        pOut.setRGB(ii, jj + 1, (int) w[5]); // PIXEL10_0
                    } else {
                        pOut.setRGB(ii, jj + 1, this.interp10(w[5], w[8], w[4])); // PIXEL10_100
                    }
                    if (Hq2x.diff(w[6], w[8])) {
                        pOut.setRGB(ii + 1, jj + 1, (int) w[5]); // PIXEL11_0
                    } else {
                        pOut.setRGB(ii + 1, jj + 1, this.interp10(w[5], w[6], w[8])); // PIXEL11_100
                    }
                    break;

                /*
                 * default : pOut.setRGB(ii,jj,(int)w[5]);
                 * pOut.setRGB(ii+1,jj,(int)w[5]);
                 * pOut.setRGB(ii,jj+1,(int)w[5]);
                 * pOut.setRGB(ii+1,jj+1,(int)w[5]);
                 */
                }
            }
        }
    }

    protected int interp(final long c[], final int nb[]) {
        this.normalize(c, nb);

        long total = 0;
        long rf = 0;
        for (int i = 0; i < c.length; i++) {
            total += nb[i];
            final long r = c[i] & 0x00FF0000;
            rf += r * nb[i];
        }

        rf = rf / total & 0x00FF0000;

        long gf = 0;
        for (int i = 0; i < c.length; i++) {
            final long g = c[i] & 0x0000FF00;
            gf += g * nb[i];
        }

        gf = gf / total & 0x0000FF00;

        long bf = 0;
        for (int i = 0; i < c.length; i++) {
            final long b = c[i] & 0x000000FF;
            bf += b * nb[i];
        }

        bf = bf / total & 0x000000FF;

        long af = 0;
        for (int i = 0; i < c.length; i++) {
            final long a = c[i] & 0xFF000000;
            af += a * nb[i];
        }

        af = af / total & 0xFF000000;

        return (int) (af + rf + gf + bf);
    }

    protected int interp(final long c1, final int nb1, final long c2, final int nb2, final long c3, final int nb3) {
        final int total = nb1 + nb2 + nb3;

        final long c[] = { c1, c2, c3 };
        final int nb[] = { nb1, nb2, nb3 };

        this.normalize(c, nb);

        final long r1 = c[0] & 0x00FF0000;
        final long r2 = c[1] & 0x00FF0000;
        final long r3 = c[2] & 0x00FF0000;
        final long rf = (r1 * nb[0] + r2 * nb[1] + r3 * nb[2]) / total & 0x00FF0000;

        final long g1 = c[0] & 0x0000FF00;
        final long g2 = c[1] & 0x0000FF00;
        final long g3 = c[2] & 0x0000FF00;
        final long gf = (g1 * nb[0] + g2 * nb[1] + g3 * nb[2]) / total & 0x0000FF00;

        final long b1 = c[0] & 0x000000FF;
        final long b2 = c[1] & 0x000000FF;
        final long b3 = c[2] & 0x000000FF;
        final long bf = (b1 * nb[0] + b2 * nb[1] + b3 * nb[2]) / total & 0x000000FF;

        final long a1 = c[0] & 0xFF000000;
        final long a2 = c[1] & 0xFF000000;
        final long a3 = c[2] & 0xFF000000;
        final long af = (a1 * nb[0] + a2 * nb[1] + a3 * nb[2]) / total & 0xFF000000;

        return (int) (af + rf + gf + bf);
    }

    protected int interp1(final long c1, final long c2) {
        return this.interp(c1, 3, c2, 1, 0, 0);
    }

    protected int interp10(final long c1, final long c2, final long c3) {
        return this.interp(c1, 14, c2, 1, c3, 1);
    }

    protected int interp2(final long c1, final long c2, final long c3) {
        return this.interp(c1, 2, c2, 1, c3, 1);
    }

    protected int interp5(final long c1, final long c2) {
        return this.interp(c1, 1, c2, 1, 0, 0);
    }

    protected int interp6(final long c1, final long c2, final long c3) {
        return this.interp(c1, 5, c2, 2, c3, 1);
    }

    protected int interp7(final long c1, final long c2, final long c3) {
        return this.interp(c1, 6, c2, 1, c3, 1);
    }

    protected int interp9(final long c1, final long c2, final long c3) {
        return this.interp(c3, 3, c1, 2, c2, 1);
    }

    protected void normalize(final long c[], final int nb[]) {
        long nc = 0;

        for (int i = c.length - 1; i >= 0; i--) {
            if ((c[i] & 0xFF000000) != 0) {
                nc = c[i];
            }
        }

        for (int i = 0; i < c.length; i++) {
            if ((c[i] & 0xFF000000) == 0) {
                c[i] = nc & 0x00FFFFFF;
            }
        }
    }

    public BufferedImage scale(final BufferedImage img) {
        final BufferedImage out = new BufferedImage(img.getWidth() * 2, img.getHeight() * 2, BufferedImage.TYPE_INT_ARGB);
        this.hq2x_32(img, out);
        return out;
    }
}
