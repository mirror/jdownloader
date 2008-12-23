package jd.utils;

import java.io.IOException;


public class EditDistance {
    /**
     * Gibt den Prozentualen Unterschied zwischen zwei Strings zurück verwendet
     * die LevenshteinDistance
     * 
     * @param s
     * @param t
     * @return
     */
    public static int getLevenshteinDifference(String s, String t) {
        if (s == null) {if(t==null) return 0; return t.length();}
        if (t == null) {return s.length();}
        if(s.equals(t))return 0;
        return 100 * getLevenshteinDistance(s, t) / Math.max(s.length(), t.length());
    }

    /**
     * Gibt den Unterschied zwischen zwei Strings zurück gleicher Buchstabe +0
     * Ersetzung +1 Einfügen +1 Löschen +1
     * 
     * @param s
     * @param t
     * @return
     */
    public static int getLevenshteinDistance(String s, String t) {
        if (s == null) {if(t==null) return 0; return t.length();}
        if (t == null) {return s.length();}

        int n = s.length();
        int m = t.length();
        if (n == 0) {
            return m;
        } else if (m == 0) { return n; }
        int i;
        int n1 = n + 1;
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
    public static int damerauLevenshteinDistance(String src, String dest) {
        int[][] d = new int[src.length() + 1][dest.length() + 1];
        int i, j, cost;
        char[] str1 = src.toCharArray();
        char[] str2 = dest.toCharArray();

        for (i = 0; i <= str1.length; i++) {
            d[i][0] = i;
        }
        for (j = 0; j <= str2.length; j++) {
            d[0][j] = j;
        }
        for (i = 1; i <= str1.length; i++) {
            for (j = 1; j <= str2.length; j++) {

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
        return d[str1.length][str2.length];
    }

    public static void main(String[] args) throws IOException {
        String scr = "mama";
        String target = "papa";
        int dis = getLevenshteinDistance(scr, target);
        System.out.println(100 - (100 * dis / Math.max(scr.length(), target.length())));
    }
}
