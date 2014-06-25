package org.jdownloader.gui.packagehistorycontroller;

import org.appwork.storage.Storable;
import org.appwork.utils.StringUtils;

public class PackageHistoryEntry implements Storable, HistoryEntry {
    @SuppressWarnings("unused")
    private PackageHistoryEntry(/* Storable */) {

    }

    public PackageHistoryEntry(String name) {
        this.name = name;
        time = System.currentTimeMillis();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj != null && obj instanceof PackageHistoryEntry) {
            return StringUtils.equals(getName(), ((PackageHistoryEntry) obj).getName());
        }
        return false;
    }

    public int hashCode() {
        return name == null ? "".hashCode() : name.hashCode();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    private String name;
    private long   time;

    private static int compare(long x, long y) {
        return (x < y) ? 1 : ((x == y) ? 0 : -1);
    }

    @Override
    public int compareTo(HistoryEntry o) {
        return compare(o.getTime(), getTime());
    }
}
