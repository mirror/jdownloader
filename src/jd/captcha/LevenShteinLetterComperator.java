package jd.captcha;

import java.util.Arrays;
import java.util.List;

import jd.captcha.pixelgrid.Letter;

public class LevenShteinLetterComperator {
    private boolean[][][][] letterDB;
    private JAntiCaptcha jac;
    public boolean onlySameWidth=false;
    public int dimension = 1;
    public void run(Letter letter) {
        int bestdist = Integer.MAX_VALUE;
        boolean[][][] b = getBooleanArrays(letter);
        int best = 0;
        for (int i = 0; i < letterDB.length; i++) {
            if(onlySameWidth&&jac.letterDB.get(i).getWidth()!=letter.getWidth())continue;
            int dist = getLevenshteinDistance(b, letterDB[i], bestdist, dimension);
            if (bestdist > dist) {
                bestdist = dist;
                best = i;
            }
        }
        Letter bestLetter = jac.letterDB.get(best);
        letter.detected= new LetterComperator(letter, bestLetter);
        //75 weil zeilen und reihen gescannt werden
        letter.detected.setValityPercent(((double)75*bestdist)/((double )letter.getArea()));
        letter.setDecodedValue(bestLetter.getDecodedValue());
    }
    public void run(final Letter[] letters) {
        Thread[] ths = new Thread[letters.length];
        final LevenShteinLetterComperator lv = this;
        for (int i = 0; i < ths.length; i++) {
            final int j = i;
            ths[i]=new Thread(new Runnable() {
                public void run() {
                    lv.run(letters[j]);
                    synchronized(this)
                    {
                        notify();
                    }
                }

            });
            ths[i].start();
            
        }
        for (Thread thread : ths) {
            while(thread.isAlive())
            {
            synchronized(thread)
            {
                try {
                    thread.wait();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            }
        }
    }
    public void run(List<Letter> letters) {
        run(letters.toArray(new Letter[]{}));
    }
    public LevenShteinLetterComperator(JAntiCaptcha jac) {
        letterDB=new boolean[jac.letterDB.size()][][][];
        this.jac=jac;
        for (int i = 0; i < letterDB.length; i++) {
            letterDB[i]=getBooleanArrays(jac.letterDB.get(i));
        }
    }

    private static boolean[][][] getBooleanArrays(Letter letter) {
        boolean[][] leth1 = new boolean[letter.getWidth()][letter.getHeight()];
        int avg = letter.getAverage();
        for (int x = 0; x < leth1.length; x++) {
            for (int y = 0; y < leth1[0].length; y++) {
                int pix = letter.getPixelValue(x, y);
                if(pix==0)
                    leth1[x][y]=true;
                else if(pix==1)
                    leth1[x][y]=false;
                else
                leth1[x][y] =  letter.isElement(pix, avg);
            }
        }
        boolean[][] leth12 = new boolean[letter.getHeight()][letter.getWidth()];

        for (int y = 0; y < leth1[0].length; y++) {
            for (int x = 0; x < leth1.length; x++) {
                leth12[y][x] = leth1[x][y];
            }
        }
        return new boolean[][][] { leth1, leth12 };
    }

    public static int getLevenshteinDistance(Letter a, Letter b) {
        boolean[][][] ba = getBooleanArrays(a);
        boolean[][][] bb = getBooleanArrays(b);
        return getLevenshteinDistance(ba, bb, Integer.MAX_VALUE, 1);
    }

    private static int getLevenshteinDistance(boolean[][][] ba, boolean[][][] bb, int best,int dimension) {
        int res = 0;
        boolean[][] bba1 = ba[0];
        boolean[][] bbb1 = bb[0];
        boolean[][] bba2 = ba[1];
        boolean[][] bbb2 = bb[1];
        res += (((Math.abs(bba1.length - bbb1.length) * Math.max(bba2.length, bbb2.length))+(Math.abs(bba2.length - bbb2.length) * Math.max(bba1.length, bbb1.length)))/dimension);
        if(best<res)return Integer.MAX_VALUE;
        for (int c = 0; c < Math.min(bba1.length, bbb1.length); c++) {
            res += getLevenshteinDistance(bba1[c], bbb1[c]);
            if(best<res)return Integer.MAX_VALUE;
        }
        for (int c = 0; c < Math.min(bba2.length, bbb2.length); c++) {
            res += getLevenshteinDistance(bba2[c], bbb2[c]);
            if(best<res)return Integer.MAX_VALUE;
        }

        return res;
    }

    private static int getLevenshteinDistance(boolean[] l1, boolean[] l2) {
        if (l1 == null || l2 == null) { throw new IllegalArgumentException("Letter must not be null"); }

        int n = l1.length;
        int m = l2.length;

        if (n == 0) {
            return m;
        } else if (m == 0) { return n; }

        int p[] = new int[n + 1]; // 'previous' cost array, horizontally
        int d[] = new int[n + 1]; // cost array, horizontally
        int _d[] = null; // placeholder to assist in swapping p and d
        int c[] = new int[n + 1]; // cost array, horizontally

        // indexes into strings s and t
        int i; // iterates through s
        int j; // iterates through t

        boolean t_j;

        int cost; // cost

        for (i = 0; i <= n; i++) {
            p[i] = i;
        }
        for (j = 1; j <= m; j++) {
            t_j = l2[j - 1];

            d[0] = j;

            for (i = 1; i <= n; i++) {
                cost = l1[i - 1] == t_j ? 0 : 1;
                // minimum of cell to the left+1, to the top+1, diagonally left
                // and up +cost
                d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
                // Damerau
                if ((i > 1) && (j > 1) && (l1[i - 1] == l2[j - 2]) && (l1[i - 2] == l2[j - 1])) {
                    d[i] = Math.min(d[i], c[i - 2] + cost);
                }

            }
            // previous of previous for Damerau
            c = Arrays.copyOf(p, p.length);
            // copy current distance counts to 'previous row' distance counts
            _d = p;
            p = d;
            d = _d;
        }

        // our last action in the above loop was to switch d and p, so p now
        // actually has the most recent cost counts
        return p[n];

    }

}
