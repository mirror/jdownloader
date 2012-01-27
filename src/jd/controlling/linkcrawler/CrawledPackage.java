package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.settings.GeneralSettings;

public class CrawledPackage implements AbstractPackageNode<CrawledLink, CrawledPackage> {

    protected static final String                          PACKAGETAG            = "<jd:" + PackagizerController.PACKAGENAME + ">";

    private boolean                                        autoExtractionEnabled = true;

    private ArrayList<CrawledLink>                         children;
    private String                                         comment               = null;
    private PackageController<CrawledPackage, CrawledLink> controller            = null;

    private long                                           created               = -1;

    private String                                         name                  = null;

    private String                                         downloadFolder        = JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();

    private boolean                                        downloadFolderSet     = false;

    private boolean                                        expanded              = false;

    private HashSet<String>                                extractionPasswords   = new HashSet<String>();
    protected CrawledPackageView                           view;

    public CrawledPackage() {
        children = new ArrayList<CrawledLink>();
        view = new CrawledPackageView();
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

    public long getCreated() {
        return created;
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
        return name;
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

    public boolean isAutoExtractionEnabled() {
        return autoExtractionEnabled;
    }

    public boolean isDownloadFolderSet() {
        return downloadFolderSet;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void notifyStructureChanges() {

    }

    public void setAutoExtractionEnabled(boolean autoExtractionEnabled) {
        this.autoExtractionEnabled = autoExtractionEnabled;
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

    public void setName(String name) {
        this.name = name;
    }

    public void setDownloadFolder(String downloadFolder) {
        if (!StringUtils.isEmpty(downloadFolder)) {
            downloadFolderSet = true;
            this.downloadFolder = downloadFolder;
        } else {
            this.downloadFolder = JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();
            this.downloadFolderSet = false;
        }
    }

    public void setEnabled(boolean b) {
    }

    public void setExpanded(boolean b) {
        this.expanded = b;
    }

    public void onChildEnabledStateChanged(CrawledLink crawledLink) {
        getView().updateInfo(crawledLink);
    }

    public CrawledPackageView getView() {
        return view;
    }

    public boolean isEnabled() {
        return getView().isEnabled();
    }

}