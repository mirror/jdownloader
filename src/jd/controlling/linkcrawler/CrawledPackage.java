package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.settings.GeneralSettings;

public class CrawledPackage implements AbstractPackageNode<CrawledLink, CrawledPackage> {

    private ArrayList<CrawledLink>                         children         = new ArrayList<CrawledLink>();
    private PackageController<CrawledPackage, CrawledLink> controller       = null;
    private boolean                                        expanded         = false;
    private String                                         autoPackageName  = null;
    private boolean                                        allowAutoPackage = true;
    private transient CrawledPackageInfo                   fpInfo           = null;
    private String                                         comment          = null;
    private String                                         downloadFolder   = JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();
    protected static final String                          PACKAGETAG       = "<jd:" + PackagizerController.PACKAGENAME + ">";

    public String getDownloadFolder() {
        // replace variables in downloadfolder
        return downloadFolder.replace(PACKAGETAG, getName());
    }

    public HashSet<String> getExtractionPasswords() {
        return extractionPasswords;
    }

    private boolean autoAddEnabled;

    public boolean isAutoAddEnabled() {
        return autoAddEnabled;
    }

    private boolean autoExtractionEnabled = true;

    public boolean isAutoExtractionEnabled() {
        return autoExtractionEnabled;
    }

    public void setAutoExtractionEnabled(boolean autoExtractionEnabled) {
        this.autoExtractionEnabled = autoExtractionEnabled;
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

    private boolean         autoStartEnabled;
    private HashSet<String> extractionPasswords = new HashSet<String>();

    public void setDownloadFolder(String downloadFolder) {
        if (downloadFolder != null) {
            this.downloadFolder = downloadFolder;
        }
    }

    /**
     * @return the allowAutoPackage
     */
    public boolean isAllowAutoPackage() {
        return allowAutoPackage;
    }

    /**
     * @param allowAutoPackage
     *            the allowAutoPackage to set
     */
    public void setAllowAutoPackage(boolean allowAutoPackage) {
        this.allowAutoPackage = allowAutoPackage;
    }

    public CrawledPackage() {

    }

    /**
     * @return the autoPackageName
     */
    public String getAutoPackageName() {
        return autoPackageName;
    }

    /**
     * @param autoPackageName
     *            the autoPackageName to set
     */
    public void setAutoPackageName(String autoPackageName) {
        this.autoPackageName = autoPackageName;
    }

    private String customName = null;
    private long   created    = -1;

    /**
     * @param created
     *            the created to set
     */
    public void setCreated(long created) {
        this.created = created;
    }

    public PackageController<CrawledPackage, CrawledLink> getControlledBy() {
        return controller;
    }

    public void setControlledBy(PackageController<CrawledPackage, CrawledLink> controller) {
        this.controller = controller;
    }

    public List<CrawledLink> getChildren() {
        return children;
    }

    public void notifyStructureChanges() {
        if (fpInfo != null) {
            synchronized (fpInfo) {
                fpInfo.structureVersion++;
            }
        }
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean b) {
        this.expanded = b;
    }

    public String getName() {
        if (customName != null) return customName;
        return autoPackageName;
    }

    public void setName(String name) {
        customName = name;
    }

    public boolean isEnabled() {
        return true;
    }

    public long getCreated() {
        return created;
    }

    public long getFinishedDate() {
        return 0;
    }

    /*
     * fpInfo is null by default, will get created on first use
     */
    public CrawledPackageInfo getCrawledPackageInfo() {
        if (fpInfo != null) return fpInfo;
        synchronized (this) {
            if (fpInfo == null) {
                fpInfo = new CrawledPackageInfo(this);
            }
        }
        return fpInfo;
    }

    public void setEnabled(boolean b) {
    }

    public void notifyPropertyChanges() {
    }

    /**
     * @param comment
     *            the comment to set
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

}