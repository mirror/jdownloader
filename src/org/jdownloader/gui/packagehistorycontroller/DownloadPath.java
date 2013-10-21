package org.jdownloader.gui.packagehistorycontroller;

import org.appwork.storage.Storable;

public class DownloadPath implements Storable, HistoryEntry {
    @SuppressWarnings("unused")
    private DownloadPath(/* Storable */) {

    }

    public DownloadPath(String myPath) {
        name = myPath;
        time = System.currentTimeMillis();
    }

    public String getName() {

        return name;
    }

    public boolean equals(Object obj) {
        if (obj == null) return false;
        return hashCode() == obj.hashCode();
    }

    public int hashCode() {
        return name.hashCode();
    }

    public void setName(String path) {
        this.name = path;
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
