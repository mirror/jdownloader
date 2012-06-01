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

// Copy: copy arrays of bytes
public class Copy {
    private static final int Nb = 4;

    // copy: copy state to out
    public static void copy(byte[] out, byte[][] state) {
        int outLoc = 0;
        for (int c = 0; c < Nb; c++) {
            for (int r = 0; r < 4; r++) {
                out[outLoc++] = state[r][c];
            }
        }
    }

    // copy: copy in to state
    public static void copy(byte[][] state, byte[] in) {
        int inLoc = 0;
        for (int c = 0; c < Nb; c++) {
            for (int r = 0; r < 4; r++) {
                state[r][c] = in[inLoc++];
            }
        }
    }
}