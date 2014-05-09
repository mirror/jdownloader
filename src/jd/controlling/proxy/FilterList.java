package jd.controlling.proxy;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.Storable;
import org.appwork.utils.StringUtils;

public class FilterList implements Storable {
    public enum Type {
        WHITELIST,
        BLACKLIST;

    }

    private Type type;

    public FilterList(/* Storable */) {

    }

    public FilterList(FilterList.Type selectedItem, String[] lines) {
        this.type = selectedItem;
        this.entries = lines;
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
    }

    private String[] entries;

    public boolean validate(String host) {
        switch (type) {
        case BLACKLIST:
            for (String s : entries) {
                if (StringUtils.equalsIgnoreCase(host, s)) { return false; }
            }
            return true;

        case WHITELIST:

            for (String s : entries) {
                if (StringUtils.equalsIgnoreCase(host, s)) { return true; }
            }
            return false;

        default:
            throw new WTFException("Unknown Type: " + type);
        }

    }
}
