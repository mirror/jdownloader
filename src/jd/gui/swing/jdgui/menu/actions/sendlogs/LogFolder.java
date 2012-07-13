package jd.gui.swing.jdgui.menu.actions.sendlogs;

import java.io.File;

public class LogFolder {

    private long    created;
    private boolean selected;

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public File getFolder() {
        return folder;
    }

    public void setFolder(File folder) {
        this.folder = folder;
    }

    private long    lastModified;
    private File    folder;
    private boolean needsFlush = false;

    public LogFolder(File f, long timestamp) {
        created = timestamp;
        lastModified = f.lastModified();
        folder = f;
    }

    /**
     * @return the needsFlush
     */
    public boolean isNeedsFlush() {
        return needsFlush;
    }

    /**
     * @param needsFlush
     *            the needsFlush to set
     */
    public void setNeedsFlush(boolean needsFlush) {
        this.needsFlush = needsFlush;
    }

}
