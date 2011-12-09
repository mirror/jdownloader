package jd.controlling.linkcrawler;

import java.util.HashSet;
import java.util.List;

import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.settings.GeneralSettings;

public class CrawledPackage implements AbstractPackageNode<CrawledLink, CrawledPackage> {

    protected static final String                          PACKAGETAG            = "<jd:" + PackagizerController.PACKAGENAME + ">";
    private boolean                                        allowAutoPackage      = true;
    private boolean                                        autoAddEnabled;
    private boolean                                        autoExtractionEnabled = true;
    private String                                         autoPackageName       = null;
    private boolean                                        autoStartEnabled;
    private CrawledPackageView                             children;
    private String                                         comment               = null;
    private PackageController<CrawledPackage, CrawledLink> controller            = null;

    private long                                           created               = -1;

    private String                                         customName            = null;

    private String                                         downloadFolder        = JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();

    private boolean                                        downloadFolderSet     = false;

    private boolean                                        expanded              = false;

    private HashSet<String>                                extractionPasswords   = new HashSet<String>();
    private CrawledPackageView                             view;

    // private transient CrawledPackageInfo fpInfo = null;

    public CrawledPackage() {
        children = new ChildrenCollection(this);
        view = new CrawledPackageView();
    }

    /**
     * @return the autoPackageName
     */
    public String getAutoPackageName() {
        return autoPackageName;
    }

    public List<CrawledLink> getChildren() {
        return children;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    public PackageController<CrawledPackage, CrawledLink> getControlledBy() {
        return controller;
    }

    // /*
    // * fpInfo is null by default, will get created on first use
    // */
    // public CrawledPackageInfo getCrawledPackageInfo() {
    // if (fpInfo != null) return fpInfo;
    // synchronized (this) {
    // if (fpInfo == null) {
    // fpInfo = new CrawledPackageInfo(this);
    // }
    // }
    // return fpInfo;
    // }

    public long getCreated() {
        return created;
    }

    public String getCustomName() {
        return customName;
    }

    public String getDownloadFolder() {

        // replace variables in downloadfolder
        return downloadFolder.replace(PACKAGETAG, getName());
    }

    public HashSet<String> getExtractionPasswords() {
        return extractionPasswords;
    }

    public long getFinishedDate() {
        return 0;
    }

    public String getName() {
        if (customName != null) return customName;
        return autoPackageName;
    }

    /**
     * Returns the raw Downloadfolder String. This link may contain wildcards
     * like <jd:packagename>. Use {@link #getDownloadFolder()} to return the
     * actuall downloadloadfolder
     * 
     * @return
     */
    public String getRawDownloadFolder() {
        return downloadFolder;
    }

    /**
     * @return the allowAutoPackage
     */
    public boolean isAllowAutoPackage() {
        return allowAutoPackage;
    }

    public boolean isAutoAddEnabled() {
        return autoAddEnabled;
    }

    public boolean isAutoExtractionEnabled() {
        return autoExtractionEnabled;
    }

    public boolean isAutoStartEnabled() {
        return autoStartEnabled;
    }

    public boolean isDownloadFolderSet() {
        return downloadFolderSet;
    }

    public boolean isEnabled() {
        return children.isEnabled();
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void notifyStructureChanges() {

    }

    /**
     * @param allowAutoPackage
     *            the allowAutoPackage to set
     */
    public void setAllowAutoPackage(boolean allowAutoPackage) {
        this.allowAutoPackage = allowAutoPackage;
    }

    public void setAutoAddEnabled(boolean autoAddEnabled) {
        this.autoAddEnabled = autoAddEnabled;
    }

    public void setAutoExtractionEnabled(boolean autoExtractionEnabled) {
        this.autoExtractionEnabled = autoExtractionEnabled;
    }

    /**
     * @param autoPackageName
     *            the autoPackageName to set
     */
    public void setAutoPackageName(String autoPackageName) {
        this.autoPackageName = autoPackageName;
    }

    public void setAutoStartEnabled(boolean autoStartEnabled) {
        this.autoStartEnabled = autoStartEnabled;
    }

    /**
     * @param comment
     *            the comment to set
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setControlledBy(PackageController<CrawledPackage, CrawledLink> controller) {
        this.controller = controller;
    }

    /**
     * @param created
     *            the created to set
     */
    public void setCreated(long created) {
        this.created = created;
    }

    public void setCustomName(String name) {
        customName = name;
    }

    public void setDownloadFolder(String downloadFolder) {
        if (downloadFolder != null) {
            downloadFolderSet = true;
            this.downloadFolder = downloadFolder;
        }
    }

    public void setEnabled(boolean b) {
    }

    public void setExpanded(boolean b) {
        this.expanded = b;
    }

    public void onChildEnabledStateChanged(CrawledLink crawledLink) {
        children.updateInfo(crawledLink);
    }

    public DomainInfo[] getDomainInfos() {
        return children.getDomainInfos();
    }

    public long getSize() {
        return children.getFileSize();
    }

    public CrawledPackageView getView() {
        return view;
    }

}