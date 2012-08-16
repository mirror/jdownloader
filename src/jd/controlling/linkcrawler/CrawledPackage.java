package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.ChildComparator;
import jd.controlling.packagecontroller.PackageController;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.settings.GeneralSettings;

public class CrawledPackage implements AbstractPackageNode<CrawledLink, CrawledPackage> {

    public static final String                             PACKAGETAG        = "<jd:" + PackagizerController.PACKAGENAME + ">";

    public static final ChildComparator<CrawledLink>       SORTER_ASC        = new ChildComparator<CrawledLink>() {

                                                                                 public int compare(CrawledLink o1, CrawledLink o2) {
                                                                                     String o1s = o1.getName().toLowerCase(Locale.ENGLISH);
                                                                                     String o2s = o2.getName().toLowerCase(Locale.ENGLISH);
                                                                                     if (o1s == null) {
                                                                                         o1s = "";
                                                                                     }
                                                                                     if (o2s == null) {
                                                                                         o2s = "";
                                                                                     }
                                                                                     return o1s.compareTo(o2s);

                                                                                 }

                                                                                 @Override
                                                                                 public String getID() {
                                                                                     return "jd.controlling.linkcrawler.CrawledPackage";
                                                                                 }

                                                                                 @Override
                                                                                 public boolean isAsc() {
                                                                                     return true;
                                                                                 }
                                                                             };
    public static final ChildComparator<CrawledLink>       SORTER_DESC       = new ChildComparator<CrawledLink>() {

                                                                                 public int compare(CrawledLink o1, CrawledLink o2) {
                                                                                     String o1s = o1.getName().toLowerCase(Locale.ENGLISH);
                                                                                     ;
                                                                                     String o2s = o2.getName().toLowerCase(Locale.ENGLISH);
                                                                                     ;
                                                                                     if (o1s == null) {
                                                                                         o1s = "";
                                                                                     }
                                                                                     if (o2s == null) {
                                                                                         o2s = "";
                                                                                     }
                                                                                     return o2s.compareTo(o1s);

                                                                                 }

                                                                                 @Override
                                                                                 public String getID() {
                                                                                     return "jd.controlling.linkcrawler.CrawledPackage";
                                                                                 }

                                                                                 @Override
                                                                                 public boolean isAsc() {
                                                                                     return false;
                                                                                 }
                                                                             };

    private ArrayList<CrawledLink>                         children;
    private String                                         comment           = null;
    private PackageController<CrawledPackage, CrawledLink> controller        = null;

    private long                                           created           = -1;

    private String                                         name              = null;

    private String                                         downloadFolder    = JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();

    private boolean                                        downloadFolderSet = false;

    private boolean                                        expanded          = false;
    private transient UniqueAlltimeID                      uniqueID          = new UniqueAlltimeID();
    protected CrawledPackageView                           view;

    private ChildComparator<CrawledLink>                   sorter;

    public UniqueAlltimeID getUniqueID() {
        return uniqueID;
    }

    public CrawledPackage() {
        children = new ArrayList<CrawledLink>();
        view = new CrawledPackageView();
        if (JsonConfig.create(GeneralSettings.class).isAutoSortChildrenEnabled()) {
            sorter = SORTER_ASC;
        }
    }

    public void copyPropertiesTo(CrawledPackage dest) {
        if (dest == null || dest == this) return;
        dest.name = name;
        dest.comment = comment;
        if (this.isDownloadFolderSet()) dest.setDownloadFolder(getRawDownloadFolder());

    }

    @Override
    public void sort() {
        synchronized (this) {
            if (sorter == null) return;
            Collections.sort(children, sorter);
        }
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
        int limit = JsonConfig.create(GeneralSettings.class).getSubfolderThreshold();
        if (limit > getChildren().size()) {
            if (downloadFolder.contains(CrawledPackage.PACKAGETAG)) {
                //
                return downloadFolder.replace(PACKAGETAG, "").replace("//", "/").replace("\\\\", "\\");
            }

        }
        return downloadFolder.replace(PACKAGETAG, CrossSystem.alleviatePathParts(getName()));

    }

    public long getFinishedDate() {
        return 0;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the raw Downloadfolder String. This link may contain wildcards like <jd:packagename>. Use {@link #getDownloadFolder()} to
     * return the actuall downloadloadfolder
     * 
     * @return
     */
    public String getRawDownloadFolder() {
        return downloadFolder;
    }

    public boolean isDownloadFolderSet() {
        return downloadFolderSet;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void notifyStructureChanges() {
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

    public CrawledPackageView getView() {
        return view;
    }

    public boolean isEnabled() {
        return getView().isEnabled();
    }

    public int indexOf(CrawledLink child) {
        synchronized (this) {
            return children.indexOf(child);
        }
    }

    @Override
    public ChildComparator<CrawledLink> getCurrentSorter() {
        return sorter;
    }

    @Override
    public void setCurrentSorter(ChildComparator<CrawledLink> comparator) {

        if (comparator != null) {
            if (comparator.isAsc()) {
                Log.L.info("Sort ASC " + comparator.getID());
            } else {
                Log.L.info("Sort DESC " + comparator.getID());
            }
        } else {
            Log.L.info("UNSORTED");
        }
        sorter = comparator;
    }

    @Override
    public void nodeUpdated(CrawledLink source, jd.controlling.packagecontroller.AbstractNodeNotifier.NOTIFY notify) {
        PackageController<CrawledPackage, CrawledLink> n = getControlledBy();
        if (n != null) n.nodeUpdated(this, notify);
    }

}