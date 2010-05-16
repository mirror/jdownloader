//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.captcha;

import java.io.File;
import java.io.IOException;
import java.util.List;

import jd.captcha.pixelgrid.BinLetters;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.LevenshteinLetter;

public class LevenShteinLetterComperator {
    public LevenshteinLetter[] letterDB;
    public boolean onlySameWidth = false;
    public int costs = 6;
    public boolean detectVerticalOffset = false;
    public boolean detectHorizonalOffset = false;

    public void run(Letter letter) {
        if (letterDB.length == 0 || letter.getWidth() == 0 || letter.getHeight() == 0) return;
        LevenshteinLetter b = new LevenshteinLetter(letter);

        // dimension/=b[0].length;
        // System.out.println(this.costs+":"+dimension);

        int best = 0;
        int bestdist = Integer.MAX_VALUE;
        int[] bestOffset = null;
        for (int i = 0; i < letterDB.length; i++) {
            if (onlySameWidth && letterDB[i].getWidth() != b.getWidth()) continue;
            int[] dist = getLevenshteinDistance(b, letterDB[i], bestdist);
            if (dist != null && bestdist > dist[0]) {
                bestOffset = dist;
                bestdist = dist[0];
                best = i;
            }
        }
        if (bestOffset == null) return;
        // System.out.println(bestdist);
        Letter bestLetter = letterDB[best].toLetter();
        // LetterComperator r = new
        // LetterComperator(letter,bestBiggest.detected.getB() );

        letter.detected = new LetterComperator(letter, bestLetter);
        letter.detected.setOffset(new int[] { bestOffset[1], bestOffset[2] });

        // 75 weil zeilen und reihen gescannt werden
        letter.detected.setValityPercent(((double) 75 * bestdist / costs) / letter.getArea());

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
        File letterDBBin = jac.getResourceFile("letters.bin");
        if (letterDBBin.exists()) {
            try {
                letterDB = new BinLetters(letterDBBin).readAll().toArray(new LevenshteinLetter[] {});
            } catch (IOException e) {
            }
        }
        if (letterDB == null) {
            letterDB = new LevenshteinLetter[jac.letterDB.size()];
            for (int i = 0; i < letterDB.length; i++) {
                letterDB[i] = new LevenshteinLetter(jac.letterDB.get(i));
            }
        }
    }

    public double getLevenshteinDistance(Letter a, Letter b) {
        int[] d = getLevenshteinDistance(new LevenshteinLetter(a), new LevenshteinLetter(b), Integer.MAX_VALUE);
        if (d != null) {
            a.detected = new LetterComperator(a, b);
            a.detected.setOffset(new int[] { d[1], d[2] });
            // 75 weil zeilen und reihen gescannt werden
            double ret = (((double) 75 * d[0] / costs) / a.getArea());
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

    private int[] getLevenshteinDistance(LevenshteinLetter ba, LevenshteinLetter bb, int best) {
        int res = 0;
        if (ba == null || bb == null) return null;
        boolean[][] bba1 = ba.horizontal;
        boolean[][] bbb1 = bb.horizontal;
        boolean[][] bba2 = ba.vertical;
        boolean[][] bbb2 = bb.vertical;

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
        int p[], d[], c[];
        {
            int n1 = n + 1;
            p = new int[n1]; // 'previous' cost array, horizontally
            d = new int[n1]; // cost array, horizontally
            c = new int[n1]; // 'previous previous' cost array, horizontally
        }
        // indexes into strings s and t
        int i; // iterates through s
        int j; // iterates through t
        int j1, j2, i1, i2;
        boolean t_j;

        int cost = 0; // cost

        for (i = 1; i <= n; i++) {
            p[i] = i;
        }

        // c=p;
        for (j = 1; j <= m; j++) {
            j1 = j;
            j2 = --j1;
            j2--;
            t_j = l2[j1];

            d[0] = j;

            for (i = 1; i <= n; i++) {
                i1 = i - 1;
                cost = (l1[i1] == t_j) ? 0 : costs;
                // minimum of cell to the left+1, to the top+1, diagonally left
                // and up +cost
                d[i] = Math.min(d[i1] + costs, Math.min(p[i] + costs, p[i1] + cost));
                // Damerau
                if (i > 1 && j > 1 && l1[i1] == l2[j2] && l1[i2 = i1 - 1] == l2[j1]) {
                    d[i] = Math.min(d[i], c[i2] + (cost > 0 ? 1 : cost));
                }
            }
            // previous of previous for Damerau
            for (i = 0; i <= n; i++) {
                c[i] = p[i];
            }
            // copy current distance counts to 'previous row' distance counts
            int[] _d = p;
            p = d;
            d = _d;
        }

        // our last action in the above loop was to switch d and p, so p now
        // actually has the most recent cost counts
        return p[n];

    }

}
