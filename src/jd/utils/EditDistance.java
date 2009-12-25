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

package jd.utils;

public final class EditDistance {
    /**
     * Don't let anyone instantiate this class.
     */
    private EditDistance() {
    }

    /**
     * Gibt den Prozentualen Unterschied zwischen zwei Strings zurück verwendet
     * die LevenshteinDistance
     * 
     * @param s
     * @param t
     * @return
     */
    public static int getLevenshteinDifference(final String s, final String t) {
        if (s == null) {
            if (t == null) return 0;
            return t.length();
        }
        final int sLength = s.length();
        if (t == null) return sLength;
        if (s.equals(t)) return 0;
        return 100 * getLevenshteinDistance(s, t) / Math.max(sLength, t.length());
    }

    /**
     * Gibt den Unterschied zwischen zwei Strings zurück gleicher Buchstabe +0
     * Ersetzung +1 Einfügen +1 Löschen +1
     * 
     * @param s
     * @param t
     * @return
     */
    public static int getLevenshteinDistance(final String s, final String t) {
        if (s == null) {
            if (t == null) return 0;
            return t.length();
        }
        if (t == null) return s.length();

        final int n = s.length();
        final int m = t.length();
        if (n == 0) {
            return m;
        } else if (m == 0) { return n; }
        int i;
        final int n1 = n + 1;
        int p[] = new int[n1];
        int d[] = new int[n1];
        int _d[];

        int j;
        char t_j;
        for (i = 0; i <= n; i++) {
            p[i] = i;
        }
        for (j = 1; j <= m; j++) {
            t_j = t.charAt(j - 1);
            d[0] = j;
            for (i = 1; i <= n; i++) {
                int i1 = i - 1;
                d[i] = Math.min(Math.min(d[i1] + 1, p[i] + 1), p[i1] + (s.charAt(i1) == t_j ? 0 : 1));
            }
            _d = p;
            p = d;
            d = _d;
        }

        return p[n];
    }

    /**
     * erweitert die Funktionalität von Levenshtein um die Fähigkeit, zwei
     * vertauschte Zeichen zu identifizieren z.B. Pron<-->Porn ist aber
     * wesentlich langsamer als Levenshtein
     * 
     * @param s
     * @param t
     * @return
     */
    public static int damerauLevenshteinDistance(final String src, final String dest) {
        final int str1Length = src.length();
        final int str2Length = dest.length();
        final int[][] d = new int[str1Length + 1][str2Length + 1];
        int i, j, cost;
        final char[] str1 = src.toCharArray();
        final char[] str2 = dest.toCharArray();

        for (i = 0; i <= str1Length; i++) {
            d[i][0] = i;
        }
        for (j = 0; j <= str2Length; j++) {
            d[0][j] = j;
        }
        for (i = 1; i <= str1Length; i++) {
            for (j = 1; j <= str2Length; j++) {

                if (str1[i - 1] == str2[j - 1])
                    cost = 0;
                else
                    cost = 1;

                d[i][j] = Math.min(d[i - 1][j] + 1, // Deletion
                        Math.min(d[i][j - 1] + 1, // Insertion
                                d[i - 1][j - 1] + cost)); // Substitution

                if ((i > 1) && (j > 1) && (str1[i - 1] == str2[j - 2]) && (str1[i - 2] == str2[j - 1])) {
                    // System.out.println(d[i][j]+":"+Math.min(d[i][j], d[i -
                    // 2][j - 2] + cost));
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + cost);
                    System.out.println("d:" + d[i - 2][j - 2]);
                }
            }
        }
        return d[str1Length][str2Length];
    }

}
