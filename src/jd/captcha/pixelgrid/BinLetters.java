package jd.captcha.pixelgrid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import jd.captcha.JAntiCaptcha;

public class BinLetters {
    public static boolean[] fromByte(byte b) {
        boolean[] bits = new boolean[8];
        for (int i = 0; i < 8; i++) {
            bits[i] = (b & 1) == 1;
            b >>= 1;
        }
        return bits;
    }

    FileInputStream in;
    File file;

    public BinLetters(File file) {
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

    public LevenshteinLetter getLetter(int i) throws IOException {
        open();
        for (int j = 0; j < i; j++) {
            in.skip(readNextBinHeader().bytes);
        }
        LevenshteinLetter n = readNext();
        in.close();
        return n;
    }

    private int getInt() throws IOException {
        byte[] bytes = new byte[4];
        in.read(bytes);
        return byte2int(bytes);
    }

    private static byte[] int2byte(int number) {
        byte[] data = new byte[4];
        for (int i = 0; i < 4; ++i) {
            int shift = i << 3; // i * 8
            data[3 - i] = (byte) ((number & (0xff << shift)) >>> shift);
        }
        return data;
    }

    private static int byte2int(byte[] data) {
        int number = 0;
        for (int i = 0; i < 4; ++i) {
            number |= (data[3 - i] & 0xff) << (i << 3);
        }
        return number;
    }

    public ArrayList<LevenshteinLetter> readAll() throws IOException {
        open();
        ArrayList<LevenshteinLetter> lets = new ArrayList<LevenshteinLetter>();
        while (hasNext())
            lets.add(readNext());
        return lets;
    }

    public boolean hasNext() {
        try {
            return in.available() > 7;
        } catch (IOException e) {
        }
        return false;
    }

    public boolean contains(Letter let) throws IOException {
        return contains(new LevenshteinLetter(let));
    }

    public boolean contains(LevenshteinLetter let) throws IOException {
        return contains(BinLetter.LevenshteinLetter2BinLetter(let));
    }

    public boolean contains(BinLetter let) throws IOException {
        open();
        while (hasNext()) {
            BinLetter bin = readNextBinHeader();
            if (bin.value == let.value && bin.hight == let.hight && bin.width == let.width) {
                readBody(bin);
                for (int i = 0; i < let.bytesArray.length; i++) {
                    if(let.bytesArray[i]!=bin.bytesArray[i])continue;
                }
                return true;
            }
            else
                in.skip(bin.bytes);

        }
        return false;
    }
    public boolean containsNoValue(BinLetter let) throws IOException {
        open();
        while (hasNext()) {
            BinLetter bin = readNextBinHeader();
            if (bin.hight == let.hight && bin.width == let.width) {
                readBody(bin);
                for (int i = 0; i < let.bytesArray.length; i++) {
                    if(let.bytesArray[i]!=bin.bytesArray[i])continue;
                }
                return true;
            }
            else
                in.skip(bin.bytes);

        }
        return false;
    }
    public BinLetter readNextBinHeader() throws IOException {
        BinLetter ret = new BinLetter();
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
        BinLetter bin = readNextBinHeader();
        readBody(bin);
        boolean[][] ret = new boolean[bin.width][bin.hight];
        int xd = 0;
        int yd = 0;
        int area = bin.width * bin.hight;
        int cre = 0;
        for (int i = 0; i < bin.bytesArray.length; i++) {
            for (int j = 0; j < 8; j++) {

                ret[xd][yd] = (bin.bytesArray[i] & 1) == 1;
                bin.bytesArray[i] >>= 1;
                if (++xd / bin.width > 0) {
                    yd++;
                    xd = 0;
                }
                if (++cre >= area) break;

            }
        }
        LevenshteinLetter lev = new LevenshteinLetter(ret, bin.value);
        return lev;
    }

    public void close() throws IOException {
        in.close();
    }

    public static void write(Letter b, File file, boolean append) throws IOException {
        write(new LevenshteinLetter(b), file, append);
    }

    public static void write(LevenshteinLetter levenshteinLetter, File file, boolean append) throws IOException {
        write(BinLetter.LevenshteinLetter2BinLetter(levenshteinLetter), file, append);
    }

    public static void write(BinLetter b, File file, boolean append) throws IOException {
        FileOutputStream f = new FileOutputStream(file, append);
        f.write(int2byte(b.width));
        f.write(int2byte(b.hight));
        f.write(int2byte(b.bytes));
        f.write(b.value);
        f.write(b.bytesArray);
        f.close();
    }

    public static void main(String[] args) {
        File file = new File("/home/dwd/.jd_home/jd/captcha/methods/nrdr/let.bin");
        JAntiCaptcha jac = new JAntiCaptcha("nrdr");

        if (false) {
            for (Letter let : jac.letterDB) {
                try {
                    write(let, file, true);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        } else {
            try {
                BinLetters bl = new BinLetters(file).open();
                LevenshteinLetter let = new LevenshteinLetter(jac.letterDB.get(2400));
                
                long time = System.currentTimeMillis();
                bl.contains(let);
                time = System.currentTimeMillis() - time;
                
                System.out.println(time);

//                boolean b = bl.contains(jac.letterDB.get(2400));


                // BasicWindow.showImage(new
                // BinLetters(file).open().readNext().toLetter().getImage());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
