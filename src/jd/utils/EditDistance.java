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
     * vertauschte Zeichen zu identifizieren z.B. Pron<-->Porn
     * 
     * @param s
     * @param t
     * @return
     */
    public final static int damerauLevenshteinDistance(String l1, String l2) {
        if (l1 == null || l2 == null) { throw new IllegalArgumentException("Letter must not be null"); }

        int n = l1.length();
        int m = l2.length();

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
        char t_j;

        int cost = 0; // cost

        for (i = 1; i <= n; i++) {
            p[i] = i;
        }

        // c=p;
        for (j = 1; j <= m; j++) {
            j1 = j;
            j2 = --j1;
            j2--;
            t_j = l2.charAt(j1);

            d[0] = j;

            for (i = 1; i <= n; i++) {
                i1 = i - 1;
                cost = (l1.charAt(i1) == t_j) ? 0 : 1;
                // minimum of cell to the left+1, to the top+1, diagonally left
                // and up +cost
                d[i] = Math.min(d[i1] + 1, Math.min(p[i] + 1, p[i1] + cost));
                // Damerau
                if (i > 1 && j > 1 && l1.charAt(i1) == l2.charAt(j2) && l1.charAt(i2 = i1 - 1) == l2.charAt(j1)) {
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
