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
        ret.setMatchAlwaysFilter(getMatchAlwaysFilter());
        ret.setFiletypeFilter(getFiletypeFilter());
        ret.setOnlineStatusFilter(getOnlineStatusFilter());
        ret.setPluginStatusFilter(getPluginStatusFilter());
        ret.setHosterURLFilter(getHosterURLFilter());
        ret.setName(_JDT._.LinkgrabberFilterRule_duplicate(getName()));
        ret.setSourceURLFilter(getSourceURLFilter());
        ret.setLinkEnabled(getLinkEnabled());
        ret.setRename(getRename());
        ret.setMoveto(getMoveto());
        ret.autoAddEnabled = autoAddEnabled;
        ret.autoExtractionEnabled = autoExtractionEnabled;
        ret.autoStartEnabled = autoStartEnabled;
        ret.chunks = chunks;
        ret.downloadDestination = downloadDestination;
        ret.packageName = packageName;
        ret.priority = priority;
        ret.setCreated(System.currentTimeMillis());
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

    public Boolean isAutoExtractionEnabled() {
        return autoExtractionEnabled;
    }

    public void setAutoExtractionEnabled(Boolean autoExtractionEnabled) {
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
    private String   rename      = null;

    public String getRename() {
        return rename;
    }

    public void setRename(String rename) {
        this.rename = rename;
    }

    public String getMoveto() {
        return moveto;
    }

    public void setMoveto(String moveto) {
        this.moveto = moveto;
    }

    private String  moveto = null;
    private Boolean autoExtractionEnabled;
    private Boolean autoAddEnabled;
    private Boolean linkEnabled;

    public Boolean getLinkEnabled() {
        return linkEnabled;
    }

    public void setLinkEnabled(Boolean linkEnabled) {
        this.linkEnabled = linkEnabled;
    }

    public Boolean isAutoAddEnabled() {
        return autoAddEnabled;
    }

    public void setAutoAddEnabled(Boolean autoAddEnabled) {
        this.autoAddEnabled = autoAddEnabled;
    }

    public Boolean isAutoStartEnabled() {
        return autoStartEnabled;
    }

    public void setAutoStartEnabled(Boolean autoStartEnabled) {
        this.autoStartEnabled = autoStartEnabled;
    }

    private Boolean autoStartEnabled;
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
