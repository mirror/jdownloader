package org.jdownloader.updatev2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.parser.Regex;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.Storable;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.host.PluginFinder;

public class FilterList implements Storable {
    public enum Type {
        WHITELIST,
        BLACKLIST;
    }

    private Type      type            = Type.BLACKLIST;
    private Pattern[] domainPatterns  = new Pattern[0];
    private int       size            = 0;
    private Pattern[] accountPatterns = new Pattern[0];

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

    public synchronized void setEntries(String[] entries) {
        this.size = 0;
        if (entries == null) {
            this.entries = new String[0];
            this.domainPatterns = new Pattern[0];
            this.accountPatterns = new Pattern[0];
        } else {
            final PluginFinder pluginFinder = new PluginFinder();
            this.entries = entries;
            this.domainPatterns = new Pattern[entries.length];
            this.accountPatterns = new Pattern[entries.length];
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
                    final int index = entry.indexOf("@");
                    if (index >= 0) {
                        final String username = entry.substring(0, index);
                        final String host = entry.substring(index + 1);
                        String assignedHost = pluginFinder.assignHost(host);
                        if (assignedHost == null) {
                            assignedHost = host;
                        }
                        try {
                            accountPatterns[i] = Pattern.compile(username, Pattern.CASE_INSENSITIVE);
                        } catch (Throwable e) {
                            accountPatterns[i] = Pattern.compile(".*" + Pattern.quote(username) + ".*", Pattern.CASE_INSENSITIVE);
                        }
                        try {
                            domainPatterns[i] = Pattern.compile(assignedHost, Pattern.CASE_INSENSITIVE);
                        } catch (Throwable e) {
                            domainPatterns[i] = Pattern.compile(".*" + Pattern.quote(assignedHost) + ".*", Pattern.CASE_INSENSITIVE);
                        }
                        if (!StringUtils.equals(host, assignedHost)) {
                            entries[i] = username.concat("@").concat(assignedHost);
                        }
                    } else {
                        accountPatterns[i] = null;
                        String assignedHost = pluginFinder.assignHost(entry);
                        if (assignedHost == null) {
                            assignedHost = entry;
                        }
                        try {
                            domainPatterns[i] = Pattern.compile(assignedHost, Pattern.CASE_INSENSITIVE);
                        } catch (Throwable e) {
                            domainPatterns[i] = Pattern.compile(".*" + Pattern.quote(assignedHost) + ".*", Pattern.CASE_INSENSITIVE);
                        }
                        if (!StringUtils.equals(entry, assignedHost)) {
                            entries[i] = assignedHost;
                        }
                    }
                }
            }
        }
    }

    private String[] entries;

    public synchronized boolean validate(String host, String accUser) {
        switch (type) {
        case BLACKLIST:
            for (int i = 0; i < domainPatterns.length; i++) {
                final Pattern domain = domainPatterns[i];
                if (domain == null) {
                    continue;
                }
                final Pattern account = accountPatterns[i];
                if (account != null && accUser != null) {
                    if (domain.matcher(host).find() && account.matcher(accUser).find()) {
                        //
                        return false;
                    }
                } else {
                    if (domain.matcher(host).find()) {
                        //
                        return false;
                    }
                }
            }
            return true;
        case WHITELIST:
            for (int i = 0; i < domainPatterns.length; i++) {
                final Pattern domain = domainPatterns[i];
                if (domain == null) {
                    continue;
                }
                final Pattern account = accountPatterns[i];
                if (account != null && accUser != null) {
                    if (domain.matcher(host).find() && account.matcher(accUser).find()) {
                        //
                        return true;
                    }
                } else {
                    Matcher matcher = domain.matcher(host);
                    if (matcher.find()) {
                        //
                        // String g0 = matcher.group(0);
                        return true;
                    }
                }
            }
            return false;
        default:
            throw new WTFException("Unknown Type: " + type);
        }
    }

    public static void main(String[] args) {
        String[] matches = new Regex("premiumize.me", "^(?!premiumize).*$").getColumn(-1);
        System.out.println(1);
    }

    public int size() {
        return this.size;
    }
}
