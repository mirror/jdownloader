package org.jdownloader.gui.views.linkgrabber.addlinksdialog;

import org.appwork.storage.Storable;

public class DownloadPath implements Storable {
    @SuppressWarnings("unused")
    private DownloadPath(/* Storable */) {

    }

    public DownloadPath(String myPath) {
        path = myPath;
        time = System.currentTimeMillis();
    }

    public String getPath() {

        return path;
    }

    public boolean equals(Object obj) {
        if (obj == null) return false;
        return hashCode() == obj.hashCode();
    }

    public int hashCode() {
        return path.hashCode();
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    private String path;
    private long   time;
}
