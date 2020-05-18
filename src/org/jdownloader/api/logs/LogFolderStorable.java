package org.jdownloader.api.logs;

import org.appwork.storage.Storable;
import org.appwork.utils.logging2.sendlogs.LogFolder;

public class LogFolderStorable implements Storable {
    private long    created      = -1l;
    private boolean current      = false;
    private long    lastModified = -1l;

    public LogFolderStorable(/* Storable */) {
    }

    public LogFolderStorable(LogFolder folder) {
        setCreated(folder.getCreated());
        setCurrent(folder.isCurrent());
        setLastModified(folder.getLastModified());
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
}
