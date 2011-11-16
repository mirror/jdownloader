package org.jdownloader.controlling.packagizer;

import org.appwork.storage.Storable;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.filter.FilterRule;
import org.jdownloader.translate._JDT;

public class PackagizerRule extends FilterRule implements Storable {
    public PackagizerRule() {
        // required by STorable
    }

    public PackagizerRule duplicate() {
        PackagizerRule ret = new PackagizerRule();

        ret.setEnabled(isEnabled());
        ret.setIconKey(getIconKey());
        ret.setFilenameFilter(getFilenameFilter());
        ret.setFilesizeFilter(getFilesizeFilter());
        ret.setFiletypeFilter(getFiletypeFilter());
        ret.setOnlineStatusFilter(getOnlineStatusFilter());
        ret.setHosterURLFilter(getHosterURLFilter());
        ret.setName(_JDT._.LinkgrabberFilterRule_duplicate(getName()));
        ret.setSourceURLFilter(getSourceURLFilter());
        ret.autoAddEnabled = autoAddEnabled;
        ret.autoExtractionEnabled = autoExtractionEnabled;
        ret.autoStartEnabled = autoStartEnabled;
        ret.chunks = chunks;
        ret.downloadDestination = downloadDestination;
        ret.packageName = packageName;
        ret.priority = priority;
        return ret;
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

    private Priority priority    = null;
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
    private String  filename;
    private int     order = 0;

    public void setOrder(int order) {
        this.order = order;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setDownloadDestination(String string) {
        downloadDestination = string;
    }

    public PackagizerRuleWrapper compile() {
        return new PackagizerRuleWrapper(this);
    }

    public String getFilename() {
        return filename;
    }

    public int getOrder() {
        return order;
    }

}
