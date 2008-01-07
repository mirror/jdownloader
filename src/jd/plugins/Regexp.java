package jd.plugins;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regexp {
    private Matcher matcher;
    public Regexp(Matcher matcher) {
        if(matcher == null)
            return;
        this.matcher = matcher;
    }
    public Regexp(String data, Pattern pattern) {
        if(data==null || pattern == null)
            return;
        this.matcher = pattern.matcher(data);
    }
    public Regexp(String data, String pattern) {
        if(data==null || pattern == null)
            return;
        this.matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(data);
    }
    public Regexp(String data, String pattern, int flags) {
        if(data==null || pattern == null)
            return;
        this.matcher = Pattern.compile(pattern, flags).matcher(data);
    }
    public String getFirstMatch() {
        if(matcher==null)
            return null;
        if (matcher.groupCount() == 0)
            return getFirstMatch(0);
        else
            return getFirstMatch(1);
    }
    public String getFirstMatch(int group) {
        if(matcher==null)
            return null;
        if (matcher.find())
            return matcher.group(group);
        return null;
    }
    public String[][] getMatches() {
        if(matcher==null)
            return null;
        ArrayList<String[]> ar = new ArrayList<String[]>();
        int c = matcher.groupCount();
        int d = 1;
        String[] group;
        if (c == 0) {
            group = new String[c+1];
            d = 0;
        }
        else
            group = new String[c];

        while (matcher.find()) {
            for (int i = d; i <= c; i++) {
                group[i - d] = matcher.group(i);
            }
            ar.add(group);
        }
        return ar.toArray(new String[ar.size()][group.length]);
    }
    public String[] getMatches(int group) {
        if(matcher==null)
            return null;
        ArrayList<String> ar = new ArrayList<String>();
        while (matcher.find()) {
            ar.add(matcher.group(group));
        }
        return ar.toArray(new String[ar.size()]);
    }
    public int count() {
        if(matcher==null)
            return 0;
        int c = 0;
        while (matcher.find())
            c++;
        return c;
    }
    public Matcher getMatcher() {
        return this.matcher;
    }
    public void setMatcher(Matcher matcher) {
        this.matcher = matcher;
    }
}
