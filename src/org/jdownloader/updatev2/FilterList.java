package org.jdownloader.updatev2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.Account;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.Storable;

public class FilterList implements Storable {
    public enum Type {
        WHITELIST,
        BLACKLIST;

    }

    private Type      type;
    private Pattern[] domainPatterns;
    private int       size;
    private Pattern[] accountPatterns;

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
        domainPatterns = new Pattern[entries.length];
        accountPatterns = new Pattern[entries.length];
        for (int i = 0; i < entries.length; i++) {
            if (entries[i] == null || entries[i].trim().length() == 0 || entries[i].trim().startsWith("//") || entries[i].trim().startsWith("#")) {
                domainPatterns[i] = null;
                accountPatterns[i] = null;
            } else {
                size++;
                int index = entries[i].indexOf("@");
                if (index >= 0) {
                    String username = entries[i].substring(0, index);
                    String host = entries[i].substring(index + 1);

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
                        domainPatterns[i] = Pattern.compile(entries[i], Pattern.CASE_INSENSITIVE);
                    } catch (Throwable e) {

                        domainPatterns[i] = Pattern.compile(".*" + Pattern.quote(entries[i]) + ".*", Pattern.CASE_INSENSITIVE);
                    }
                }
            }
        }
    }

    private String[] entries;

    public boolean validate(String host, Account acc) {
        switch (type) {
        case BLACKLIST:
            for (int i = 0; i < domainPatterns.length; i++) {
                Pattern domain = domainPatterns[i];
                Pattern account = accountPatterns[i];
                if (domain == null) {
                    continue;
                }
                if (account != null) {

                    if (domain.matcher(host).find() && acc != null && acc.getUser() != null && account.matcher(acc.getUser()).find()) {
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
                Pattern domain = domainPatterns[i];
                Pattern account = accountPatterns[i];
                if (domain == null) {
                    continue;
                }

                if (account != null) {
                    if (domain.matcher(host).find() && acc != null && acc.getUser() != null && account.matcher(acc.getUser()).find()) {
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
        return size;
    }
}
