package jd.captcha;

import java.util.Arrays;
import java.util.List;

import jd.captcha.pixelgrid.Letter;

public class LevenShteinLetterComperator {
    private boolean[][][][] letterDB;
    private JAntiCaptcha jac;
    public boolean onlySameWidth = false;
    public int costs = 6;
    public boolean detectVerticalOffset = false;
    public boolean detectHorizonalOffset = false;

    public void run(Letter letter) {
        if(letterDB.length==0)return;
        boolean[][][] b = getBooleanArrays(letter);

        // dimension/=b[0].length;
        // System.out.println(this.costs+":"+dimension);

        int best = 0;
        int bestdist = Integer.MAX_VALUE;
        int[] bestOffset = null;
        for (int i = 0; i < letterDB.length; i++) {
            if (onlySameWidth && jac.letterDB.get(i).getWidth() != letter.getWidth()) continue;
            int[] dist = getLevenshteinDistance(b, letterDB[i], bestdist);
            if (dist!=null&&bestdist > dist[0]) {
                bestOffset = dist;
                bestdist=dist[0];
                best = i;
            }
        }
        if(bestOffset==null)return;
        Letter bestLetter = jac.letterDB.get(best);
//        LetterComperator r = new LetterComperator(letter,bestBiggest.detected.getB() );

        letter.detected = new LetterComperator(letter, bestLetter);
        letter.detected.setOffset(new int[] { bestOffset[1], bestOffset[2] });

        // 75 weil zeilen und reihen gescannt werden
        letter.detected.setValityPercent(((double) 75 * bestdist / costs) / ((double) letter.getArea()));

        letter.setDecodedValue(bestLetter.getDecodedValue());
    }

    public void run(final Letter[] letters) {
        Thread[] ths = new Thread[letters.length];
        final LevenShteinLetterComperator lv = this;
        for (int i = 0; i < ths.length; i++) {
            final int j = i;
            ths[i] = new Thread(new Runnable() {
                public void run() {
                    lv.run(letters[j]);
                    synchronized (this) {
                        notify();
                    }
                }

            });
            ths[i].start();

        }
        for (Thread thread : ths) {
            while (thread.isAlive()) {
                synchronized (thread) {
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
        run(letters.toArray(new Letter[] {}));
    }

    public LevenShteinLetterComperator(JAntiCaptcha jac) {
        letterDB = new boolean[jac.letterDB.size()][][][];
        this.jac = jac;
        for (int i = 0; i < letterDB.length; i++) {
            letterDB[i] = getBooleanArrays(jac.letterDB.get(i));
        }
    }

    private boolean[][][] getBooleanArrays(Letter letter) {
        boolean[][] leth1 = new boolean[letter.getWidth()][letter.getHeight()];
        int avg = letter.getAverage();
        for (int x = 0; x < leth1.length; x++) {
            for (int y = 0; y < leth1[0].length; y++) {
                int pix = letter.getPixelValue(x, y);
                if (pix == 0)
                    leth1[x][y] = true;
                else if (pix == 1)
                    leth1[x][y] = false;
                else
                    leth1[x][y] = letter.isElement(pix, avg);
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

    public double getLevenshteinDistance(Letter a, Letter b) {
        boolean[][][] ba = getBooleanArrays(a);
        boolean[][][] bb = getBooleanArrays(b);
        int[] d = getLevenshteinDistance(ba, bb, Integer.MAX_VALUE);
        if (d != null) {
            a.detected = new LetterComperator(a, b);
            a.detected.setOffset(new int[] { d[1], d[2] });
            // 75 weil zeilen und reihen gescannt werden
            double ret = (double) (((double) 75 * d[0] / costs) / ((double) a.getArea()));
            a.detected.setValityPercent(ret);
            return ret;
        }
        return Double.MAX_VALUE;
    }

    private int getBounds(boolean[][] lengthLong, boolean[][] lengthShort, boolean detectOffset) {
        int d = lengthLong.length - lengthShort.length;

        if (!detectOffset) {
            return d / 2;
        } else {
            int bestDist = Integer.MAX_VALUE;
            int besti = 0;
            int lsm1 = lengthShort.length - 1;
            for (int i = 0; i < d; i++) {
                int dist = getLevenshteinDistance(lengthLong[i], lengthShort[0]);
                dist += getLevenshteinDistance(lengthLong[i + lsm1], lengthShort[lsm1]);

                if (dist < bestDist) {
                    bestDist = dist;
                    besti = i;
                }

            }
            return besti;

        }

    }

    private int getBoundDiff(boolean[][] bba1, int start, int end) {
        int res = 0;
        for (int i = 0; i < start; i++) {
            for (int c = 0; c < bba1[i].length; c++) {
                if (bba1[i][c]) res++;
            }
        }
        for (int i = end; i < bba1.length; i++) {
            for (int c = 0; c < bba1[i].length; c++) {
                if (bba1[i][c]) res++;
            }
        }
        return res;
    }

    private int[] getLevenshteinDistance(boolean[][][] ba, boolean[][][] bb, int best) {
        int res = 0;
        boolean[][] bba1 = ba[0];
        boolean[][] bbb1 = bb[0];
        boolean[][] bba2 = ba[1];
        boolean[][] bbb2 = bb[1];

        int bounds1 = 0;
        int diff1 = bba1.length - bbb1.length;
        boolean swV = false;
        boolean swH = false;

        if (diff1 > 0) {
            bounds1 = getBounds(bba1, bbb1, detectVerticalOffset);
        } else if (diff1 < 0) {
            boolean[][] bac = bbb1;
            bbb1 = bba1;
            bba1 = bac;
            swV = true;
            bounds1 = getBounds(bba1, bbb1, detectVerticalOffset);
        } else
            bounds1 = 0;
        res += getBoundDiff(bba1, bounds1, bbb1.length) * costs;
        if (best < res) return null;

        int bounds2 = 0;
        int diff2 = bba2.length - bbb2.length;

        if (diff2 > 0) {
            bounds2 = getBounds(bba2, bbb2, detectHorizonalOffset);
        } else if (diff2 < 0) {
            boolean[][] bac = bbb2;
            bbb2 = bba2;
            bba2 = bac;
            swH = true;
            bounds2 = getBounds(bba2, bbb2, detectHorizonalOffset);
        } else
            bounds2 = 0;
        res += getBoundDiff(bba2, bounds2, bbb2.length) * costs;

        if (best < res) return null;

        // res += (((Math.abs(bba1.length - bbb1.length) * Math.max(bba2.length,
        // bbb2.length))+(Math.abs(bba2.length - bbb2.length) *
        // Math.max(bba1.length, bbb1.length)))/dimension);

        // System.out.println(bba1.length+":"+bbb1.length+":"+bounds1[1]+":"+bounds1[0]);
        for (int c = 0; c < bbb1.length; c++) {
            // System.out.println(c-bounds1[0]);
            res += getLevenshteinDistance(bba1[c + bounds1], bbb1[c]);
            if (best < res) return null;
        }
        for (int c = 0; c < bbb2.length; c++) {
            res += getLevenshteinDistance(bba2[c + bounds2], bbb2[c]);
            if (best < res) return null;
        }
        return new int[] { res, swV ? -bounds1 : bounds1, swH ? -bounds2 : bounds2 };
    }

    private int getLevenshteinDistance(boolean[] l1, boolean[] l2) {
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
                d[i] = Math.min(Math.min(d[i - 1] + costs, p[i] + costs), p[i - 1] + cost * costs);
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
