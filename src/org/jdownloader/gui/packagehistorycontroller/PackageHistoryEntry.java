package org.jdownloader.gui.packagehistorycontroller;

import org.appwork.storage.Storable;

public class PackageHistoryEntry implements Storable, HistoryEntry {
    @SuppressWarnings("unused")
    private PackageHistoryEntry(/* Storable */) {

    }

    public PackageHistoryEntry(String name) {
        this.name = name;
        time = System.currentTimeMillis();
    }

    public boolean equals(Object obj) {
        if (obj == null) return false;
        return hashCode() == obj.hashCode();
    }

    public int hashCode() {
        return name.hashCode();
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

    @Override
    public int compareTo(HistoryEntry o) {
        return new Long(o.getTime()).compareTo(new Long(getTime()));
    }
}
