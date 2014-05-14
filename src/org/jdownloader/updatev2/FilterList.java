package org.jdownloader.updatev2;

import java.util.regex.Pattern;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.Storable;

public class FilterList implements Storable {
    public enum Type {
        WHITELIST,
        BLACKLIST;

    }

    private Type      type;
    private Pattern[] patterns;
    private int       size;

    public FilterList(/* Storable */) {

    }

    public FilterList(FilterList.Type selectedItem, String[] lines) {
        this.type = selectedItem;
        size = 0;
        setEntries(lines);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String[] getEntries() {
        return entries;
    }

    public void setEntries(String[] entries) {
        this.entries = entries;
        patterns = new Pattern[entries.length];
        for (int i = 0; i < entries.length; i++) {
            if (entries[i] == null || entries[i].trim().length() == 0 || entries[i].trim().startsWith("//") || entries[i].trim().startsWith("#")) {
                patterns[i] = null;
            } else {
                size++;
                try {
                    patterns[i] = Pattern.compile(".*" + entries[i] + ".*", Pattern.CASE_INSENSITIVE);
                } catch (Throwable e) {

                    patterns[i] = Pattern.compile(".*" + Pattern.quote(entries[i]) + ".*", Pattern.CASE_INSENSITIVE);
                }
            }
        }
    }

    private String[] entries;

    public boolean validate(String host) {
        switch (type) {
        case BLACKLIST:
            for (Pattern s : patterns) {
                if (s == null)
                    continue;
                if (s.matcher(host).find()) {
                    //
                    return false;
                }

            }
            return true;

        case WHITELIST:

            for (Pattern s : patterns) {
                if (s == null)
                    continue;
                if (s.matcher(host).find()) {
                    //
                    return true;
                }

            }

            return false;

        default:
            throw new WTFException("Unknown Type: " + type);
        }

    }

    public int size() {
        return size;
    }
}
