package myIrcBot;


import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * GNU GPL Lizenz Den offiziellen englischen Originaltext finden Sie unter http://www.gnu.org/licenses/gpl.html.
 * 
 *
 */
public class Regexp {
    private Matcher matcher;
    public Regexp(Matcher matcher) {
        if (matcher == null) return;
        this.matcher = matcher;
    }
    /**
     * Regexp auf ein Objekt (Beim Objekt wird toString aufgerufen)
     * @param data
     * @param pattern
     */
    public Regexp(Object data, Pattern pattern) {
        this(data.toString(), pattern);
    }
    public Regexp(String data, Pattern pattern) {
        if (data == null || pattern == null) return;
        this.matcher = pattern.matcher(data);
    }
    /**
     * Regexp auf ein Objekt (Beim Objekt wird toString aufgerufen)
     * @param data
     * @param pattern
     */
    public Regexp(Object data, String pattern) {
        this(data.toString(), pattern);
    }
    /**
     * Regexp auf ein Objekt (Beim Objekt wird toString aufgerufen)
     * @param data
     * @param pattern
     * @param flags flags für den Pattern z.B. Pattern.CASE_INSENSITIVE
     */
    public Regexp(Object data, String pattern, int flags) {
        this(data.toString(), pattern, flags);
    }
    public Regexp(String data, String pattern) {
        if (data == null || pattern == null) return;
        this.matcher = Pattern.compile(pattern,
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(data);
    }
    public Regexp(String data, String pattern, int flags) {
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
}
