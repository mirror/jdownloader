//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.jdownloader.container;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledLink;
import jd.parser.Regex;
import jd.plugins.ContainerStatus;
import jd.plugins.DownloadLink;
import jd.plugins.PluginsC;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.RijndaelEngine;
import org.bouncycastle.crypto.io.CipherInputStream;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.seamless.util.io.IO;

public class C extends PluginsC {

    public C() {
        super("CFF", "file://.+\\.ccf", "$Revision$");
    }

    private String decryptCCF5(InputStream inputStream) throws Exception {
        final String[][] CCF50 = (String[][]) getClass().forName(new String(HexFormatter.hexToByteArray("6F72672E6A646F776E6C6F616465722E636F6E7461696E65722E436F6E666967"), "UTF-8")).getMethod("CCF50").invoke(null);
        final KeyParameter keyParam1 = new KeyParameter(HexFormatter.hexToByteArray(CCF50[0][0]));
        final CipherParameters cipherParams1 = new ParametersWithIV(keyParam1, HexFormatter.hexToByteArray(CCF50[0][1]));
        final BufferedBlockCipher cipher1 = new BufferedBlockCipher(new CBCBlockCipher(new RijndaelEngine()));
        cipher1.reset();
        cipher1.init(false, cipherParams1);

        final KeyParameter keyParam11 = new KeyParameter(HexFormatter.hexToByteArray(CCF50[0][0]));
        final CipherParameters cipherParams11 = new ParametersWithIV(keyParam11, HexFormatter.hexToByteArray(CCF50[0][1]));
        final BufferedBlockCipher cipher11 = new BufferedBlockCipher(new CBCBlockCipher(new RijndaelEngine()));
        cipher11.reset();
        cipher11.init(false, cipherParams11);

        final KeyParameter keyParam2 = new KeyParameter(HexFormatter.hexToByteArray(CCF50[1][0]));
        final CipherParameters cipherParams2 = new ParametersWithIV(keyParam2, HexFormatter.hexToByteArray(CCF50[1][1]));
        final BufferedBlockCipher cipher2 = new BufferedBlockCipher(new CBCBlockCipher(new RijndaelEngine()));
        cipher2.reset();
        cipher2.init(false, cipherParams2);

        final InputStream is = new CipherInputStream(new CipherInputStream(new CipherInputStream(inputStream, cipher11), cipher2), cipher1);
        String d = new String(IO.readBytes(is), "UTF-8");
        return d;
    }

    private String decryptCCF07_10(InputStream inputStream, String key, String iv) throws Exception {
        final KeyParameter keyParam1 = new KeyParameter(HexFormatter.hexToByteArray(key));
        final CipherParameters cipherParams1 = new ParametersWithIV(keyParam1, HexFormatter.hexToByteArray(iv));
        final BufferedBlockCipher cipher1 = new BufferedBlockCipher(new CBCBlockCipher(new RijndaelEngine()));
        cipher1.reset();
        cipher1.init(false, cipherParams1);

        final InputStream is = new CipherInputStream(inputStream, cipher1);
        String d = new String(IO.readBytes(is), "UTF-8");
        return d;
    }

    private static byte[] byteBox(final byte[] buf) {
        final byte[] ret = new byte[64];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                ret[i * 8 + j] = buf[j * 8 + i];
            }
        }
        return ret;
    }

    private static byte[] bitBox(final byte[] buf) {
        final byte[] mask = new byte[] { 1, 2, 4, 8, 16, 32, 64, (byte) 128 };
        final byte[] ret = new byte[8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if ((buf[i] & mask[j]) != 0) {
                    ret[j] |= mask[i];
                }
            }
        }
        return ret;
    }

    private static long rotateLeft(long l, long steps) {
        if (steps <= 0) {
            return l;
        }
        String bits = Long.toBinaryString(l);
        if (bits.length() < 32) {
            bits = String.format("%32s", bits).replace(' ', '0');
        }
        bits = bits.substring((int) steps) + (bits.substring(0, (int) steps));
        return Long.parseLong(bits, 2);
    }

    private static long rotateRight(long l, long steps) {
        if (steps <= 0) {
            return l;
        }
        String bits = Long.toBinaryString(l);
        if (bits.length() < 32) {
            bits = String.format("%32s", bits).replace(' ', '0');
        }
        bits = bits.substring(bits.length() - (int) steps) + (bits.substring(0, bits.length() - (int) steps));
        return Long.parseLong(bits, 2);
    }

    private String decryptCCF30(byte[] ccfBytes) throws Exception {
        final byte[] magic = new byte[8];
        System.arraycopy(ccfBytes, 6, magic, 0, 4);
        long key = ByteBuffer.wrap(magic).order(ByteOrder.LITTLE_ENDIAN).getLong();
        final int padding = 64 - ccfBytes[10];
        final byte[] result = new byte[ccfBytes.length - 11];
        byte[] buf = new byte[64];
        int resultIndex = 0;
        int ccfIndex = 11;
        while (ccfIndex < ccfBytes.length) {
            System.arraycopy(ccfBytes, ccfIndex, buf, 0, 64);
            ccfIndex += 64;
            for (int index = 0; index < 64; index++) {
                if ((key & 1) != 0) {
                    key = rotateRight(key, (key & 0xFF) % 12);
                } else {
                    key = rotateLeft(key, (key & 0xFF) % 9);
                }
                buf[index] ^= key & 0xFF;
                buf = byteBox(buf);
            }
            for (int n = 0; n < 8; n++) {
                byte[] tmp = new byte[8];
                System.arraycopy(buf, 8 * n, tmp, 0, 8);
                for (int m = 0; m < 8; m++) {
                    tmp[m] ^= (key & 0xFF);
                    tmp = bitBox(tmp);
                    if ((key & 1) != 0) {
                        key = rotateRight(key, key & 12);
                    } else {
                        key = rotateLeft(key, key & 9);
                    }
                }
                System.arraycopy(tmp, 0, buf, n * 8, 8);
            }
            System.arraycopy(buf, 0, result, resultIndex, 64);
            resultIndex += 64;
        }
        final int len = result.length - padding;
        return new String(result, 0, len, "UTF-8");
    }

    public ContainerStatus callDecryption(File ccfFile) throws Exception {
        final ContainerStatus cs = new ContainerStatus(ccfFile);
        if (ccfFile.exists()) {
            String ccfContent = null;
            final byte[] ccfBytes = IO.readBytes(ccfFile);
            if (ccfBytes[0] == 'C' && ccfBytes[1] == 'C' && ccfBytes[2] == 'F' && ccfBytes[3] == '3' && ccfBytes[4] == '.' && ccfBytes[5] == '0') {
                /**
                 * CCF3.0
                 */
                final String check = decryptCCF30(ccfBytes);
                if (StringUtils.contains(check, "CryptLoad")) {
                    ccfContent = check;
                }
            } else {
                final ByteArrayInputStream is = new ByteArrayInputStream(ccfBytes);
                is.reset();
                for (String ccf[] : (String[][]) getClass().forName(new String(HexFormatter.hexToByteArray("6F72672E6A646F776E6C6F616465722E636F6E7461696E65722E436F6E666967"), "UTF-8")).getMethod("CCF0710").invoke(null)) {
                    /**
                     * CCF0.7-CCF1.0
                     */

                    is.reset();
                    final String check = decryptCCF07_10(is, ccf[0], ccf[1]);
                    if (StringUtils.contains(check, "CryptLoad")) {
                        ccfContent = check;
                        break;
                    }
                }
                if (ccfContent == null) {
                    /**
                     * CCF5.0
                     */
                    is.reset();
                    final String check = decryptCCF5(is);
                    if (StringUtils.contains(check, "CryptLoad")) {
                        ccfContent = check;
                    }
                }
            }
            if (ccfContent != null) {
                final String urls[] = new Regex(ccfContent, "<url>(.*?)</url>").getColumn(0);
                cls = new ArrayList<CrawledLink>();
                for (String url : urls) {
                    if (!StringUtils.isEmpty(url)) {
                        cls.add(new CrawledLink(url));
                    }
                }
                if (cls.size() > 0) {
                    cs.setStatus(ContainerStatus.STATUS_FINISHED);
                    return cs;
                }
            }
        }
        cs.setStatus(ContainerStatus.STATUS_FAILED);
        return cs;

    }

    public String[] encrypt(String plain) {
        return null;
    }

    public String extractDownloadURL(DownloadLink downloadLink) {
        return null;
    }

}