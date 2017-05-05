package org.jdownloader.updatev2;

import java.util.regex.Pattern;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.Storable;

public class FilterList implements Storable {
    public enum Type {
        WHITELIST,
        BLACKLIST;
    }

    private Type                 type     = Type.BLACKLIST;
    private volatile Pattern[][] patterns = new Pattern[2][0];
    private int                  size     = 0;

    public FilterList(/* Storable */) {
    }

    public FilterList(FilterList.Type selectedItem, String[] lines) {
        this.type = selectedItem;
        this.size = 0;
        setEntries(lines);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        if (type == null) {
            this.type = Type.BLACKLIST;
        } else {
            this.type = type;
        }
    }

    public String[] getEntries() {
        return entries;
    }

    public void setEntries(String[] entries) {
        this.size = 0;
        if (entries == null) {
            this.entries = new String[0];
            this.patterns = new Pattern[2][0];
        } else {
            this.entries = entries;
            final Pattern[][] lPatterns = new Pattern[2][entries.length];
            final Pattern[] accountPatterns = lPatterns[0];
            final Pattern[] domainPatterns = lPatterns[1];
            for (int i = 0; i < entries.length; i++) {
                final String entry = entries[i] == null ? "" : entries[i].trim();
                if (entry.length() == 0 || entry.startsWith("//") || entry.startsWith("#")) {
                    /**
                     * empty/comment lines
                     */
                    domainPatterns[i] = null;
                    accountPatterns[i] = null;
                } else {
                    size++;
                    final int index = entry.lastIndexOf("@");
                    if (index >= 0 && index + 1 < entry.length() && entry.matches("^[a-zA-Z0-9.-_@]+$")) {
                        final String username = entry.substring(0, index);
                        final String host = entry.substring(index + 1);
                        try {
                            accountPatterns[i] = Pattern.compile(username, Pattern.CASE_INSENSITIVE);
                        } catch (Throwable e) {
                            accountPatterns[i] = Pattern.compile(".*" + Pattern.quote(username) + ".*", Pattern.CASE_INSENSITIVE);
                        }
                        try {
                            domainPatterns[i] = Pattern.compile(host, Pattern.CASE_INSENSITIVE);
                        } catch (Throwable e) {
                            domainPatterns[i] = Pattern.compile(".*" + Pattern.quote(host) + ".*", Pattern.CASE_INSENSITIVE);
                        }
                    } else {
                        accountPatterns[i] = null;
                        try {
                            domainPatterns[i] = Pattern.compile(entry, Pattern.CASE_INSENSITIVE);
                        } catch (Throwable e) {
                            domainPatterns[i] = Pattern.compile(".*" + Pattern.quote(entry) + ".*", Pattern.CASE_INSENSITIVE);
                        }
                    }
                }
            }
            this.patterns = lPatterns;
        }
    }

    private volatile String[] entries;

    public boolean validate(String host, String accUser) {
        if (host == null) {
            host = "";
        }
        final Pattern[][] lPatterns = patterns;
        final int size = this.size;
        final Pattern[] accountPatterns = lPatterns[0];
        final Pattern[] domainPatterns = lPatterns[1];
        switch (type) {
        case BLACKLIST:
            for (int i = 0; i < domainPatterns.length; i++) {
                final Pattern domain = domainPatterns[i];
                if (domain != null) {
                    final Pattern account = accountPatterns[i];
                    if (account != null && accUser != null) {
                        if (domain.matcher(host).find() && account.matcher(accUser).find()) {
                            return false;
                        }
                    } else {
                        if (domain.matcher(host).find()) {
                            return false;
                        }
                    }
                }
            }
            return true;
        case WHITELIST:
            for (int i = 0; i < domainPatterns.length; i++) {
                final Pattern domain = domainPatterns[i];
                if (domain != null) {
                    final Pattern account = accountPatterns[i];
                    if (account != null && accUser != null) {
                        if (domain.matcher(host).find() && account.matcher(accUser).find()) {
                            return true;
                        }
                    } else {
                        if (domain.matcher(host).find()) {
                            return true;
                        }
                    }
                }
            }
            /**
             * whitelist is only active with at least one valid entry
             *
             * it is too easy to switch to whitelist without any entry -> blocks all connections
             */
            return size == 0;
        default:
            throw new WTFException("Unknown Type: " + type);
        }
    }

    public int size() {
        return this.size;
    }
}
