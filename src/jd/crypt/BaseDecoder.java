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

public class BaseDecoder {

    public static class BadPaddingException extends Exception {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public BadPaddingException() {
            super("Bad padding");
        }

        public BadPaddingException(String message) {
            super(message);
        }
    }

    public static class IllegalAlphabetException extends Exception {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public IllegalAlphabetException() {
            super();
        }

        public IllegalAlphabetException(char ch) {
            this("Illegal alphabet: " + ch);
        }

        public IllegalAlphabetException(String message) {
            super(message);
        }
    }

    private boolean _finalized;

    private int bitsPerChar;

    private byte[] bytes;

    private int bytesPerWord;

    private int charsPerWord;

    private char[] remainedChars;

    public BaseDecoder(char[] alphabet) {
        super();

        bytes = new byte[94]; // number of printable character in ascii table
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = -1;
        }
        for (byte i = 0; i < alphabet.length; i++) {
            int index = alphabet[i] - 33;
            try {
                if (bytes[index] != -1) { throw new IllegalArgumentException("Duplicated alphabet: '" + alphabet[i] + "'"); }
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                throw new IllegalArgumentException("Illegal alphabet: '" + alphabet[i] + "'");
            }
            bytes[index] = i;
        }

        bitsPerChar = (int) (Math.log(alphabet.length) / Math.log(2.0d));
        bytesPerWord = lcm(8, bitsPerChar) / 8;
        charsPerWord = lcm(8, bitsPerChar) / bitsPerChar;

        remainedChars = new char[0];

        _finalized = false;
    }

    // private byte[] _decode(char[] in, int off, int len, boolean finalize)
    // throws IllegalAlphabetException, BadPaddingException {

    // if (_finalized) {
    // throw new IllegalStateException("Hasn't been reset");
    // }

    // if (finalize) {
    // _finalized = true;
    // }

    // int all = remainedChars.length + len;
    // int rem = all % charsPerWord;

    // if (finalize && rem > 0) {
    // throw new BadPaddingException("Not aligned to " + charsPerWord);
    // }

    // char[] abuf = new char[all];
    // System.arraycopy(remainedChars, 0, abuf, 0, remainedChars.length);
    // System.arraycopy(in, off, abuf, remainedChars.length, len);

    // if (!finalize) {
    // if ((all / charsPerWord) > 0) {
    // rem += charsPerWord;
    // all -= charsPerWord;
    // }
    // }

    // remainedChars = new char[rem];
    // System.arraycopy(abuf, abuf.length - remainedChars.length,
    // remainedChars, 0, remainedChars.length);

    // char[] cbuf = new char[all];
    // System.arraycopy(abuf, 0, cbuf, 0, cbuf.length);

    // int words = cbuf.length / charsPerWord;
    // if (words == 0) { return new byte[0]; };

    // byte[] bbuf = new byte[words * bytesPerWord];
    // if (finalize) { words--; }
    // for (int i = 0; i < words; i++) {
    // int coff = i * charsPerWord;
    // int boff = i * bytesPerWord;
    // _decode(cbuf, coff, bbuf, boff, false);
    // }

    // if (finalize) {
    // int coff = words * charsPerWord;
    // int boff = words * bytesPerWord;
    // _decode(cbuf, coff, bbuf, boff, true);

    // int ipad = -1;
    // for (int i = 0; i < charsPerWord; i++) {
    // if (cbuf[cbuf.length - charsPerWord + i] == '=') {
    // ipad = i;
    // break;
    // }
    // }

    // if (ipad != -1) {
    // int min = 8 / bitsPerChar; // minimum non-pad index
    // if (ipad <= min) {
    // throw new BadPaddingException();
    // }

    // if ((ipad * bitsPerChar) / 8 ==
    // ((ipad - 1) * bitsPerChar) / 8) {
    // throw new BadPaddingException();
    // }

    // int vali = (ipad * bitsPerChar) / 8;
    // int inva = bytesPerWord - vali;
    // byte[] bbuf2 = new byte[bbuf.length - inva];
    // System.arraycopy(bbuf, 0, bbuf2, 0, bbuf2.length);

    // return bbuf2;
    // }
    // }

    // return bbuf;
    // }

    private void _decode(char[] in, int inoff, byte[] out, int outoff, boolean padAllowed) throws IllegalAlphabetException {

        int intValue = 0;
        int bitsInInt = 0;
        int byteIndex = bytesPerWord - 1;
        for (int i = charsPerWord - 1; i >= 0; i--) {
            char ch = in[inoff + i];
            // System.out.println(ch);
            intValue |= getByte(ch, padAllowed) << bitsInInt;
            bitsInInt += bitsPerChar;
            int byteCount = bitsInInt / 8;
            for (int j = 0; j < byteCount; j++) {
                out[outoff + byteIndex--] = (byte) (intValue & 0xff);
                intValue >>= 8;
                bitsInInt -= 8;
            }
        }
    }

    /*
     * private byte[] _decodeFinal(char[] in, int off, int len) throws
     * IllegalAlphabetException, BadPaddingException {
     * 
     * if (_finalized) { throw new IllegalStateException("Hasn't been reset"); }
     * 
     * _decode(in, off, len);
     * 
     * _finalized = true;
     * 
     * int all = remainedChars.length + len; int rem = all % charsPerWord;
     * 
     * if (rem > 0) { throw new BadPaddingException("Not aligned to " +
     * charsPerWord); }
     * 
     * char[] abuf = new char[all];
     * 
     * System.arraycopy(remainedChars, 0, abuf, 0, remainedChars.length);
     * System.arraycopy(in, off, abuf, remainedChars.length, len); //
     * remainedChars = new char[rem]; // System.arraycopy(abuf, abuf.length -
     * remainedChars.length, // remainedChars, 0, remainedChars.length);
     * 
     * char[] cbuf = new char[all]; System.arraycopy(abuf, 0, cbuf, 0,
     * cbuf.length);
     * 
     * int words = cbuf.length / charsPerWord; if (words == 0) { return new
     * byte[0]; };
     * 
     * byte[] bbuf = new byte[words bytesPerWord]; //if (finalize) { words--; }
     * words--; for (int i = 0; i < words; i++) { int coff = i charsPerWord; int
     * boff = i bytesPerWord; _decode(cbuf, coff, bbuf, boff, false); }
     * 
     * int coff = words charsPerWord; int boff = words bytesPerWord;
     * _decode(cbuf, coff, bbuf, boff, true);
     * 
     * int ipad = -1; for (int i = 0; i < charsPerWord; i++) { if
     * (cbuf[cbuf.length - charsPerWord + i] == '=') { ipad = i; break; } }
     * 
     * if (ipad != -1) { int min = 8 / bitsPerChar; // minimum non-pad index if
     * (ipad <= min) { throw new BadPaddingException(); }
     * 
     * if ((ipad bitsPerChar) / 8 == ((ipad - 1) bitsPerChar) / 8) { throw new
     * BadPaddingException(); }
     * 
     * int vali = (ipad bitsPerChar) / 8; int inva = bytesPerWord - vali; byte[]
     * bbuf2 = new byte[bbuf.length - inva]; System.arraycopy(bbuf, 0, bbuf2, 0,
     * bbuf2.length);
     * 
     * return bbuf2; }
     * 
     * return bbuf; }
     */

    private byte[] _decode(char[] in, int off, int len) throws IllegalAlphabetException {

        int all = remainedChars.length + len;
        int rem = all % charsPerWord;

        char[] abuf = new char[all];
        System.arraycopy(remainedChars, 0, abuf, 0, remainedChars.length);
        System.arraycopy(in, off, abuf, remainedChars.length, len);

        if (all / charsPerWord > 0) {
            rem += charsPerWord;
            all -= charsPerWord;
        }

        remainedChars = new char[rem];
        System.arraycopy(abuf, abuf.length - remainedChars.length, remainedChars, 0, remainedChars.length);

        char[] cbuf = new char[all];
        System.arraycopy(abuf, 0, cbuf, 0, cbuf.length);

        int words = cbuf.length / charsPerWord;
        if (words == 0) { return new byte[0]; }

        byte[] bbuf = new byte[words * bytesPerWord];
        for (int i = 0; i < words; i++) {
            int coff = i * charsPerWord;
            int boff = i * bytesPerWord;
            _decode(cbuf, coff, bbuf, boff, false);
        }

        return bbuf;
    }

    private byte[] _decodeFinal(char[] in, int off, int len) throws IllegalAlphabetException, BadPaddingException {

        if (_finalized) { throw new IllegalStateException("Hasn't been reset"); }

        _decode(in, off, len);

        _finalized = true;

        if (remainedChars.length == 0) { return new byte[0]; }

        if (remainedChars.length % charsPerWord > 0) { throw new BadPaddingException("Not aligned to " + charsPerWord); }

        char[] cbuf = remainedChars;

        int words = cbuf.length / charsPerWord;

        byte[] bbuf = new byte[words * bytesPerWord];
        // if (finalize) { words--; }
        words--;
        for (int i = 0; i < words; i++) {
            int coff = i * charsPerWord;
            int boff = i * bytesPerWord;
            _decode(cbuf, coff, bbuf, boff, false);
        }

        int coff = words * charsPerWord;
        int boff = words * bytesPerWord;
        _decode(cbuf, coff, bbuf, boff, true);

        int ipad = -1;
        for (int i = 0; i < charsPerWord; i++) {
            if (cbuf[cbuf.length - charsPerWord + i] == '=') {
                ipad = i;
                break;
            }
        }

        if (ipad != -1) {
            int min = 8 / bitsPerChar; // minimum non-pad index
            if (ipad <= min) { throw new BadPaddingException(); }

            if (ipad * bitsPerChar / 8 == (ipad - 1) * bitsPerChar / 8) { throw new BadPaddingException(); }

            int vali = ipad * bitsPerChar / 8;
            int inva = bytesPerWord - vali;
            byte[] bbuf2 = new byte[bbuf.length - inva];
            System.arraycopy(bbuf, 0, bbuf2, 0, bbuf2.length);

            return bbuf2;
        }

        return bbuf;
    }

    public byte[] decode(char[] in) throws IllegalAlphabetException {
        return decode(in, 0, in.length);
    }

    public byte[] decode(char[] in, int off, int len) throws IllegalAlphabetException {

        // return _decode(in, off, len, false);
        return _decode(in, off, len);
    }

    public byte[] decodeFinal() throws IllegalAlphabetException, BadPaddingException {

        return decodeFinal(new char[0]);
    }

    public byte[] decodeFinal(char[] in) throws IllegalAlphabetException, BadPaddingException {

        return decodeFinal(in, 0, in.length);
    }

    public byte[] decodeFinal(char[] in, int off, int len) throws IllegalAlphabetException, BadPaddingException {

        return _decodeFinal(in, off, len);
    }

    private int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    private byte getByte(char ch, boolean padAllowed) throws IllegalAlphabetException {

        if (ch == '=') {
            if (padAllowed) {
                return 0;
            } else {
                throw new IllegalAlphabetException(ch);
            }
        }

        int index = ch - 33;
        // System.out.println(ch+" "+index+" "+(int)ch);
        if (index < 0 || index >= bytes.length) { throw new IllegalArgumentException("Unknown char: " + ch + "(" + index); }
        byte b = bytes[index];
        if (b == -1) { throw new IllegalArgumentException("Unknown char?: " + ch); }
        return b;
    }

    public boolean isValidCharacter(char ch) {
        try {
            return bytes[ch - 33] != -1;
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            return false;
        }
    }

    private int lcm(int a, int b) {
        return a * b / gcd(a, b);
    }

    public BaseDecoder reset() {
        remainedChars = new char[0];
        _finalized = false;
        return this;
    }
}
