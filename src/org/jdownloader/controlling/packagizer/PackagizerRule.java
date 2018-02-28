package org.jdownloader.controlling.packagizer;

import org.appwork.storage.Storable;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.filter.FilterRule;
import org.jdownloader.translate._JDT;

public class PackagizerRule extends FilterRule implements Storable {
    public PackagizerRule() {
        // required by Storable
    }

    public PackagizerRule duplicate() {
        final PackagizerRule ret = new PackagizerRule();
        ret.setEnabled(isEnabled());
        ret.setCreated(System.currentTimeMillis());
        ret.setIconKey(getIconKey());
        ret.setName(_JDT.T.LinkgrabberFilterRule_duplicate(getName()));
        ret.setMatchAlwaysFilter(getMatchAlwaysFilter());
        ret.setFilenameFilter(getFilenameFilter());
        ret.setPackagenameFilter(getPackagenameFilter());
        ret.setFilesizeFilter(getFilesizeFilter());
        ret.setFiletypeFilter(getFiletypeFilter());
        ret.setHosterURLFilter(getHosterURLFilter());
        ret.setSourceURLFilter(getSourceURLFilter());
        ret.setOriginFilter(getOriginFilter());
        ret.setConditionFilter(getConditionFilter());
        ret.setOnlineStatusFilter(getOnlineStatusFilter());
        ret.setPluginStatusFilter(getPluginStatusFilter());
        ret.setDownloadDestination(getDownloadDestination());
        ret.setPriority(getPriority());
        ret.setPackageName(getPackageName());
        ret.setFilename(getFilename());
        ret.setComment(getComment());
        ret.setChunks(getChunks());
        ret.setAutoExtractionEnabled(isAutoExtractionEnabled());
        ret.setAutoAddEnabled(isAutoAddEnabled());
        ret.setAutoStartEnabled(isAutoStartEnabled());
        ret.setAutoForcedStartEnabled(isAutoForcedStartEnabled());
        ret.setLinkEnabled(getLinkEnabled());
        ret.setMoveto(getMoveto());
        ret.setRename(getRename());
        ret.setTestUrl(getTestUrl());
        ret.setPackageKey(getPackageKey());
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

    private String  moveto     = null;
    private Boolean autoExtractionEnabled;
    private Boolean autoAddEnabled;
    private Boolean linkEnabled;
    private String  packageKey = null;

    public String getPackageKey() {
        return packageKey;
    }

    public void setPackageKey(String packageKey) {
        this.packageKey = packageKey;
    }

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
    private String  comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    private int     order = 0;
    private Boolean autoForcedStartEnabled;

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

    public void setAutoForcedStartEnabled(Boolean autoForcedStartEnabled) {
        this.autoForcedStartEnabled = autoForcedStartEnabled;
    }

    public Boolean isAutoForcedStartEnabled() {
        return autoForcedStartEnabled;
    }
}
