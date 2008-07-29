//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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


package jd.update;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regex {
    private Matcher matcher;
    public Regex(Matcher matcher) {
        if (matcher == null) return;
        this.matcher = matcher;
    }
    /**
     * Regexp auf ein Objekt (Beim Objekt wird toString aufgerufen)
     * @param data
     * @param pattern
     */
    public Regex(Object data, Pattern pattern) {
        this(data.toString(), pattern);
    }
    public Regex(String data, Pattern pattern) {
        if (data == null || pattern == null) return;
        this.matcher = pattern.matcher(data);
    }
    /**
     * Regexp auf ein Objekt (Beim Objekt wird toString aufgerufen)
     * @param data
     * @param pattern
     */
    public Regex(Object data, String pattern) {
        this(data.toString(), pattern);
    }
    /**
     * Regexp auf ein Objekt (Beim Objekt wird toString aufgerufen)
     * @param data
     * @param pattern
     * @param flags flags für den Pattern z.B. Pattern.CASE_INSENSITIVE
     */
    public Regex(Object data, String pattern, int flags) {
        this(data.toString(), pattern, flags);
    }
    public Regex(String data, String pattern) {
        if (data == null || pattern == null) return;
        this.matcher = Pattern.compile(pattern,
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(data);
    }
    public Regex(String data, String pattern, int flags) {
        if (data == null || pattern == null) return;
        this.matcher = Pattern.compile(pattern, flags).matcher(data);
    }
    /**
     * 
     * gibt den ersten Treffer aus fals groups existieren von
     * group 1 sonst von group 0
     */
    public String getFirstMatch() {
        if (matcher == null) return null;
        if (matcher.groupCount() == 0)
            return getFirstMatch(0);
        else return getFirstMatch(1);
    }
    /**
     * gibt den ersten Treffer einer group aus
     */
    public String getFirstMatch(int group) {
        if (matcher == null) return null;
        Matcher matchertmp = matcher;
        if (matchertmp.find()) return matchertmp.group(group);
        return null;
    }
    /**
     * Gibt alle Treffer eines Matches in einem 2D array aus
     */
    public String[][] getMatches() {
        if(matcher==null)
            return null;
        Matcher matchertmp = matcher;
       
        ArrayList<String[]> ar = new ArrayList<String[]>();
        while (matchertmp.find()) {
        int c = matchertmp.groupCount();
        int d = 1;
        String[] group;
        if (c == 0) {
            group = new String[c+1];
            d = 0;
        }
        else
            group = new String[c];

       
            for (int i = d; i <= c; i++) {
                group[i - d] = matchertmp.group(i);
            }
            ar.add(group);
        }
        return ar.toArray(new String[][]{});
    }
    
   /*
    public static void main(String args[]) {
       String txt="http://oxygen-warez.com/category/XVID/10'000_BC_DVD_Rip_AC3_104902.html";
//        //
   
       new Regex(txt,"(http://.*filefox.in/\\?id=.+)|(http://.*alphawarez.us/\\?id=.+)|(http://.*pirate-loads.com/\\?id=.+)|(http://.*fettrap.com/\\?id=.+)|(http://.*omega-music.com(/\\?id=.+|/download/.+/.+.html))|(http://.*hardcoremetal.biz/\\?id=.+)|(http://.*flashload.org/\\?id=.+)|(http://.*twin-warez.com/\\?id=.+)|(http://.*oneload.org/\\?id=.+)|(http://.*steelwarez.com/\\?id=.+)|(http://.*fullstreams.info/\\?id=.+)|(http://.*lionwarez.com/\\?id=.+)|(http://.*1dl.in/\\?id=.+)|(http://.*chrome-database.com/\\?id=.+)|(http://.*oneload.org/\\?id=.+)|(http://.*youwarez.biz/\\?id=.+)|(http://.*saugking.net/\\?id=.+)|(http://.*leetpornz.com/\\?id=.+)|(http://.*freefiles4u.com/\\?id=.+)|(http://.*dark-load.net/\\?id=.+)|(http://.*wrzunlimited.1gb.in/\\?id=.+)|(http://.*crimeland.de/\\?id=.+)|(http://.*get-warez.in/\\?id=.+)|(http://.*meinsound.com/\\?id=.+)|(http://.*projekt-tempel-news.de.vu/\\?id=.+)|(http://.*datensau.org/\\?id=.+)|(http://.*musik.am(/\\?id=.+|/download/.+/.+.html))|(http://.*spreaded.net(/\\?id=.+|/download/.+/.+.html))|(http://.*relfreaks.com(/\\?id=.+|/download/.+/.+.html))|(http://.*babevidz.com(/\\?id=.+|/category/.+/.+.html))|(http://.*serien24.com(/\\?id=.+|/download/.+/.+.html))|(http://.*porn-freaks.net(/\\?id=.+|/download/.+/.+.html))|(http://.*xxx-4-free.net(/\\?id=.+|/download/.+/.+.html))|(http://.*xxx-reactor.net(/\\?id=.+|/download/.+/.+.html))|(http://.*porn-traffic.net(/\\?id=.+|/category/.+/.+.html))|(http://.*chili-warez.net(/\\?id=.+|/.+/.+.html))|(http://.*game-freaks.net(/\\?id=.+|/download/.+/.+.html))|(http://.*isos.at(/\\?id=.+|/download/.+/.+.html))|(http://.*your-load.com(/\\?id=.+|/download/.+/.+.html))|(http://.*mov-world.net(/\\?id=.+|/category/.+/.+.html))|(http://.*xtreme-warez.net(/\\?id=.+|/category/.+/.+.html))|(http://.*sceneload.to(/\\?id=.+|/download/.+/.+.html))|(http://.*oxygen-warez.com(/\\?id=.+|/category/.+/.+.html))|(http://.*serienfreaks.to(/\\?id=.+|/category/.+/.+.html))|(http://.*serienfreaks.in(/\\?id=.+|/category/.+/.+.html))|(http://.*warez-load.com(/\\?id=.+|/category/.+/.+.html))|(http://.*ddl-scene.com(/\\?id=.+|/category/.+/.+.html))|(http://.*mp3king.cinipac-hosting.biz/\\?id=.+)").matches();
//        String[] matchs2 = new Regex(txt,"ich .*? (.*?) und").getMatches(0);
        System.out.println("II");
        
        
    }
     */

    /**
     * gibt alle Treffer in einer group als Array aus
     */
    public String[] getMatches(int group) {
        if(matcher==null)
            return null;
        Matcher matchertmp = matcher;
        ArrayList<String> ar = new ArrayList<String>();
        while (matchertmp.find()) {
            ar.add(matchertmp.group(group));
        }
        return ar.toArray(new String[ar.size()]);
    }
    /**
     * gibt die Anzahl der Treffer zurück
     */
    public int count() {
        if (matcher == null) return 0;
        int c = 0;
        Matcher matchertmp = matcher;
        while (matchertmp.find())
            c++;
        return c;
    }
    public boolean matches()
    {
        try {
            if (matcher.find()) { return true; }
        } catch (Exception e) {
            // TODO: handle exception
        }

        return false;
    }
    /**
     * gibt den matcher aus
     */
    public Matcher getMatcher() {
        return this.matcher;
    }
    /**
     * setzt den matcher
     */
    public void setMatcher(Matcher matcher) {
        this.matcher = matcher;
    }
    public String toString() {
    	String ret = "";
    	String[][] match = getMatches();
    	for (int i = 0; i < match.length; i++) {
			for (int j = 0; j < match.length; j++) {
				ret+="match["+i+"]["+j+"]="+match[i][j]+System.getProperty("line.separator");
			}
		}
    	 matcher.reset();
    	return ret;
    }
    public static String[] getLines(String arg) {
        if (arg == null) return new String[] {};
        return arg.split("[\r|\n]{1,2}");
    }
    public static boolean matches(Object str, Pattern pat) {
       
        return new Regex(str,pat).matches();
    }
    public static boolean matches(Object page, String string) {
       
        return new Regex(page,string).matches();
    }
    /**
     * Gibt zu einem typischem Sizestring (12,34kb , 45 mb etc) die größe in bytes zurück.
     * @param sizestring
     * @return
     */
    public static long getSize(String string) {

   
        String[][] matches = new Regex(string, Pattern.compile("([\\d]+)[\\.|\\,|\\:]([\\d]+)", Pattern.CASE_INSENSITIVE)).getMatches();

        if (matches == null || matches.length == 0) {
            matches = new Regex(string, Pattern.compile("([\\d]+)", Pattern.CASE_INSENSITIVE)).getMatches();

        }
        if (matches == null || matches.length == 0) return -1;

        double res = 0;
        if (matches[0].length == 1) res = Double.parseDouble(matches[0][0]);
        if (matches[0].length == 2) res = Double.parseDouble(matches[0][0] + "." + matches[0][1]);

        if (Regex.matches(string, Pattern.compile("(mb|mbyte|megabyte)", Pattern.CASE_INSENSITIVE))) {
            res *= 1024 * 1024;
        } else if (Regex.matches(string, Pattern.compile("(kb|kbyte|kilobyte)", Pattern.CASE_INSENSITIVE))) {
            res *= 1024;
        }

        return Math.round(res);
    }
}
