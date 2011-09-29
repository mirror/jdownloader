package org.jdownloader.controlling.packagizer;

import org.appwork.storage.Storable;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.filter.FilterRule;

public class PackagizerRule extends FilterRule implements Storable {
    public PackagizerRule() {
        // required by STorable
    }

    private String downloadDestination;

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public boolean isAutoExtractionEnabled() {
        return autoExtractionEnabled;
    }

    public void setAutoExtractionEnabled(boolean autoExtractionEnabled) {
        this.autoExtractionEnabled = autoExtractionEnabled;
    }

    public int getChunks() {
        return chunks;
    }

    public void setChunks(int chunks) {
        this.chunks = chunks;
    }

    public String getDownloadDestination() {
        return downloadDestination;
    }

    private Priority priority    = Priority.DEFAULT;
    private String   packageName = null;
    private boolean  autoExtractionEnabled;
    private boolean  autoAddEnabled;

    public boolean isAutoAddEnabled() {
        return autoAddEnabled;
    }

    public void setAutoAddEnabled(boolean autoAddEnabled) {
        this.autoAddEnabled = autoAddEnabled;
    }

    public boolean isAutoStartEnabled() {
        return autoStartEnabled;
    }

    public void setAutoStartEnabled(boolean autoStartEnabled) {
        this.autoStartEnabled = autoStartEnabled;
    }

    private boolean autoStartEnabled;
    private int     chunks;

    public void setDownloadDestination(String string) {
        downloadDestination = string;
    }

}
