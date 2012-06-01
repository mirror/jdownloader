// end function//    jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team support@jdownloader.org
//
//     This program is free software: you can redistribute it and/or modify
//     it under the terms of the GNU General Public License as published by
//     the Free Software Foundation, either version 3 of the License, or
//     (at your option) any later version.
//
//     This program is distributed in the hope that it will be useful,
//     but WITHOUT ANY WARRANTY; without even the implied warranty of
//     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//     GNU General Public License for more details.
//
//     You should have received a copy of the GNU General Public License
//     along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.crypt;

/******************************************************************************
 * 
 * Copyright (c) 1998,99 by Mindbright Technology AB, Stockholm, Sweden.
 * www.mindbright.se, info@mindbright.se
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 ***************************************************************************** 
 * $Author: nallen $ $Date: 2001/11/12 16:31:16 $ $Name: $
 *****************************************************************************/
/*
 * !!! Author's comment: I don't know if there are any copyright issues here but
 * this code is so trivial so I guess there are not (apart from the name RC4
 * which I believe is a trademark...).
 */

public class RC4 {
    int    x;
    int    y;
    byte[] state = new byte[256];

    final int arcfour_byte() {
        int x;
        int y;
        int sx, sy;

        x = this.x + 1 & 0xff;
        sx = (int) state[x];
        y = sx + this.y & 0xff;
        sy = (int) state[y];
        this.x = x;
        this.y = y;
        state[y] = (byte) (sx & 0xff);
        state[x] = (byte) (sy & 0xff);
        return (int) state[(sx + sy & 0xff)];
    }

    public byte[] decrypt(final byte[] key, final byte[] src) {
        setKey(key);
        final byte[] dest = new byte[src.length];
        encrypt(src, 0, dest, 0, src.length);
        return dest;
    }

    public void decrypt(final byte[] src, final int srcOff, final byte[] dest, final int destOff, final int len) {
        encrypt(src, srcOff, dest, destOff, len);
    }

    public synchronized void encrypt(final byte[] src, final int srcOff, final byte[] dest, final int destOff, final int len) {
        final int end = srcOff + len;
        for (int si = srcOff, di = destOff; si < end; si++, di++) {
            dest[di] = (byte) (((int) src[si] ^ arcfour_byte()) & 0xff);
        }
    }

    public void setKey(final byte[] key) {
        int t, u;
        int keyindex;
        int stateindex;
        int counter;

        for (counter = 0; counter < 256; counter++) {
            state[counter] = (byte) counter;
        }
        keyindex = 0;
        stateindex = 0;
        for (counter = 0; counter < 256; counter++) {
            t = (int) state[counter];
            stateindex = stateindex + key[keyindex] + t & 0xff;
            u = (int) state[stateindex];
            state[stateindex] = (byte) (t & 0xff);
            state[counter] = (byte) (u & 0xff);
            if (++keyindex >= key.length) {
                keyindex = 0;
            }
        }
    }

}
