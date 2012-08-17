package jd.captcha.pixelgrid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class BinLetters {
    public static boolean[] fromByte(byte b) {
        final boolean[] bits = new boolean[8];
        for (int i = 0; i < 8; i++) {
            bits[i] = (b & 1) == 1;
            b >>= 1;
        }
        return bits;
    }

    FileInputStream in;
    File file;

    public BinLetters(final File file) {
        this.file = file;
    }

    public BinLetters getNewInstance() {
        return new BinLetters(file);
    }

    public BinLetters open() throws FileNotFoundException {
        try {
            close();
        } catch (Exception e) {
            // TODO: handle exception
        }
        in = new FileInputStream(file);
        return this;
    }

    public LevenshteinLetter getLetter(final int i) throws IOException {
        open();
        for (int j = 0; j < i; j++) {
            in.skip(readNextBinHeader().bytes);
        }
        final LevenshteinLetter n = readNext();
        in.close();
        return n;
    }

    private int getInt() throws IOException {
        final byte[] bytes = new byte[4];
        in.read(bytes);
        return byte2int(bytes);
    }

    private static byte[] int2byte(int number) {
        final byte[] data = new byte[4];
        for (int i = 0; i < 4; ++i) {
            int shift = i << 3; // i * 8
            data[3 - i] = (byte) ((number & (0xff << shift)) >>> shift);
        }
        return data;
    }

    private static int byte2int(final byte[] data) {
        int number = 0;
        for (int i = 0; i < 4; ++i) {
            number |= (data[3 - i] & 0xff) << (i << 3);
        }
        return number;
    }

    public java.util.List<LevenshteinLetter> readAll() throws IOException {
        open();
        final java.util.List<LevenshteinLetter> lets = new ArrayList<LevenshteinLetter>();
        while (hasNext()) {
            lets.add(readNext());
        }
        return lets;
    }

    public boolean hasNext() {
        try {
            return in.available() > 7;
        } catch (IOException e) {
        }
        return false;
    }

    public boolean contains(final Letter let) throws IOException {
        return contains(new LevenshteinLetter(let));
    }

    public boolean contains(LevenshteinLetter let) throws IOException {
        return contains(BinLetter.LevenshteinLetter2BinLetter(let));
    }

    public boolean contains(final BinLetter let) throws IOException {
        open();
        while (hasNext()) {
            final BinLetter bin = readNextBinHeader();
            if (bin.value == let.value && bin.hight == let.hight && bin.width == let.width) {
                readBody(bin);
                final byte[] letBytesArray = let.bytesArray;
                final byte[] binBytesArray = bin.bytesArray;
                final int letBytesArrayLength = letBytesArray.length;
                for (int i = 0; i < letBytesArrayLength; i++) {
                    if (letBytesArray[i] != binBytesArray[i]) {
                        continue;
                    }
                }
                return true;
            } else
                in.skip(bin.bytes);
        }
        return false;
    }

    public boolean containsNoValue(final BinLetter let) throws IOException {
        open();
        while (hasNext()) {
            final BinLetter bin = readNextBinHeader();
            if (bin.hight == let.hight && bin.width == let.width) {
                readBody(bin);
                final byte[] letBytesArray = let.bytesArray;
                final byte[] binBytesArray = bin.bytesArray;
                final int letBytesArrayLength = letBytesArray.length;
                for (int i = 0; i < letBytesArrayLength; i++) {
                    if (letBytesArray[i] != binBytesArray[i]) {
                        continue;
                    }
                }
                return true;
            } else
                in.skip(bin.bytes);
        }
        return false;
    }

    public BinLetter readNextBinHeader() throws IOException {
        final BinLetter ret = new BinLetter();
        ret.width = getInt();
        ret.hight = getInt();
        ret.bytes = getInt();
        ret.value = (char) in.read();
        return ret;
    }

    public void readBody(BinLetter let) throws IOException {
        let.bytesArray = new byte[let.bytes];
        in.read(let.bytesArray);
    }

    public LevenshteinLetter readNext() throws IOException {
        // if(in.hasNextByte())
        final BinLetter bin = readNextBinHeader();
        readBody(bin);
        final int binWidth = bin.width;
        final int binHight = bin.hight;
        boolean[][] ret = new boolean[binWidth][binHight];
        int xd = 0;
        int yd = 0;
        int area = binWidth * binHight;
        int cre = 0;
        for (int i = 0; i < bin.bytesArray.length; i++) {
            for (int j = 0; j < 8; j++) {

                ret[xd][yd] = (bin.bytesArray[i] & 1) == 1;
                bin.bytesArray[i] >>= 1;
                if (++xd / binWidth > 0) {
                    yd++;
                    xd = 0;
                }
                if (++cre >= area) break;

            }
        }
        final LevenshteinLetter lev = new LevenshteinLetter(ret, bin.value);
        return lev;
    }

    public void close() throws IOException {
        in.close();
    }

    public static void write(final Letter b, final File file, final boolean append) throws IOException {
        write(new LevenshteinLetter(b), file, append);
    }

    public static void write(final LevenshteinLetter levenshteinLetter, final File file, final boolean append) throws IOException {
        write(BinLetter.LevenshteinLetter2BinLetter(levenshteinLetter), file, append);
    }

    public static void write(final BinLetter b, final File file, final boolean append) throws IOException {
        final FileOutputStream f = new FileOutputStream(file, append);
        f.write(int2byte(b.width));
        f.write(int2byte(b.hight));
        f.write(int2byte(b.bytes));
        f.write(b.value);
        f.write(b.bytesArray);
        f.close();
    }

}
