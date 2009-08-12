//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.crypt;

// AESdecrypt: AES decryption
public class AESdecrypt {
    public final int Nb = 4; // words in a block, always 4 for now
    public int nk; // key length in words
    public int nr; // number of rounds, = Nk + 6
    AEStables tab; // all the tables needed for AES
    byte[] w; // the expanded key
    private int wCount; // position in w (= 4*Nb*(Nr+1) each encrypt)

    // AESdecrypt: constructor for class. Mainly expands key
    public AESdecrypt(byte[] key, int NkIn) {
        nk = NkIn; // words in a key, = 4, or 6, or 8
        nr = nk + 6; // corresponding number of rounds
        tab = new AEStables(); // class to give values of various functions
        w = new byte[4 * Nb * (nr + 1)]; // room for expanded key
        KeyExpansion(key, w); // length of w depends on Nr
    }

    // InvAddRoundKey: same as AddRoundKey, but backwards
    private void InvAddRoundKey(byte[][] state) {
        for (int c = Nb - 1; c >= 0; c--) {
            for (int r = 3; r >= 0; r--) {
                state[r][c] = (byte) (state[r][c] ^ w[--wCount]);
            }
        }
    }

    // InvCipher: actual AES decryption
    public void InvCipher(byte[] in, byte[] out) {
        wCount = 4 * Nb * (nr + 1); // count bytes during decryption
        byte[][] state = new byte[4][Nb]; // the state array
        Copy.copy(state, in); // actual component-wise copy
        InvAddRoundKey(state); // xor with expanded key
        for (int round = nr - 1; round >= 1; round--) {
            // Print.printArray("Start round " + (Nr - round) + ":", state);
            InvShiftRows(state); // mix up rows
            InvSubBytes(state); // inverse S-box substitution
            InvAddRoundKey(state); // xor with expanded key
            InvMixColumns(state); // complicated mix of columns
        }
        // Print.printArray("Start round " + Nr + ":", state);
        InvShiftRows(state); // mix up rows
        InvSubBytes(state); // inverse S-box substitution
        InvAddRoundKey(state); // xor with expanded key
        Copy.copy(out, state);
    }

    // InvMixColumns: complex and sophisticated mixing of columns
    private void InvMixColumns(byte[][] s) {
        int[] sp = new int[4];
        byte b0b = (byte) 0x0b;
        byte b0d = (byte) 0x0d;
        byte b09 = (byte) 0x09;
        byte b0e = (byte) 0x0e;
        for (int c = 0; c < 4; c++) {
            sp[0] = tab.FFMul(b0e, s[0][c]) ^ tab.FFMul(b0b, s[1][c]) ^ tab.FFMul(b0d, s[2][c]) ^ tab.FFMul(b09, s[3][c]);
            sp[1] = tab.FFMul(b09, s[0][c]) ^ tab.FFMul(b0e, s[1][c]) ^ tab.FFMul(b0b, s[2][c]) ^ tab.FFMul(b0d, s[3][c]);
            sp[2] = tab.FFMul(b0d, s[0][c]) ^ tab.FFMul(b09, s[1][c]) ^ tab.FFMul(b0e, s[2][c]) ^ tab.FFMul(b0b, s[3][c]);
            sp[3] = tab.FFMul(b0b, s[0][c]) ^ tab.FFMul(b0d, s[1][c]) ^ tab.FFMul(b09, s[2][c]) ^ tab.FFMul(b0e, s[3][c]);
            for (int i = 0; i < 4; i++) {
                s[i][c] = (byte) sp[i];
            }
        }
    }

    // InvShiftRows: right circular shift of rows 1, 2, 3 by 1, 2, 3
    private void InvShiftRows(byte[][] state) {
        byte[] t = new byte[4];
        for (int r = 1; r < 4; r++) {
            for (int c = 0; c < Nb; c++) {
                t[(c + r) % Nb] = state[r][c];
            }
            for (int c = 0; c < Nb; c++) {
                state[r][c] = t[c];
            }
        }
    }

    // InvSubBytes: apply inverse Sbox substitution to each byte of state
    private void InvSubBytes(byte[][] state) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < Nb; col++) {
                state[row][col] = tab.invSBox(state[row][col]);
            }
        }
    }

    // KeyExpansion: expand key, byte-oriented code, but tracks words
    // (the same as for encryption)
    private void KeyExpansion(byte[] key, byte[] w) {
        byte[] temp = new byte[4];
        // first just copy key to w
        int j = 0;
        while (j < 4 * nk) {
            w[j] = key[j++];
        }
        // here j == 4*Nk;
        int i;
        while (j < 4 * Nb * (nr + 1)) {
            i = j / 4; // j is always multiple of 4 here
            // handle everything word-at-a time, 4 bytes at a time
            for (int iTemp = 0; iTemp < 4; iTemp++) {
                temp[iTemp] = w[j - 4 + iTemp];
            }
            if (i % nk == 0) {
                byte ttemp, tRcon;
                byte oldtemp0 = temp[0];
                for (int iTemp = 0; iTemp < 4; iTemp++) {
                    if (iTemp == 3) {
                        ttemp = oldtemp0;
                    } else {
                        ttemp = temp[iTemp + 1];
                    }
                    if (iTemp == 0) {
                        tRcon = tab.Rcon(i / nk);
                    } else {
                        tRcon = 0;
                    }
                    temp[iTemp] = (byte) (tab.SBox(ttemp) ^ tRcon);
                }
            } else if (nk > 6 && i % nk == 4) {
                for (int iTemp = 0; iTemp < 4; iTemp++) {
                    temp[iTemp] = tab.SBox(temp[iTemp]);
                }
            }
            for (int iTemp = 0; iTemp < 4; iTemp++) {
                w[j + iTemp] = (byte) (w[j - 4 * nk + iTemp] ^ temp[iTemp]);
            }
            j = j + 4;
        }
    }
}
