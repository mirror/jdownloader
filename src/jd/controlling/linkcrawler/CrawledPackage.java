package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractNodeNotifier;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.ModifyLock;
import jd.controlling.packagecontroller.PackageController;
import jd.controlling.packagecontroller.PackageControllerComparator;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.settings.GeneralSettings;

public class CrawledPackage implements AbstractPackageNode<CrawledLink, CrawledPackage> {

    public static final PackageControllerComparator<CrawledLink> SORTER_ASC  = new PackageControllerComparator<CrawledLink>() {

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
    public static final PackageControllerComparator<CrawledLink> SORTER_DESC = new PackageControllerComparator<CrawledLink>() {

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

    public static enum TYPE {
        NORMAL,
        OFFLINE,
        POFFLINE,
        VARIOUS
    }

    private TYPE type = TYPE.NORMAL;

    public void setType(TYPE type) {
        this.type = type;
    }

    public TYPE getType() {
        return type;
    }

    private java.util.List<CrawledLink>                    children;
    private String                                         comment                = null;
    private PackageController<CrawledPackage, CrawledLink> controller             = null;

    private long                                           created                = System.currentTimeMillis();

    private String                                         name                   = null;

    private String                                         downloadFolder         = JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();

    private boolean                                        downloadFolderSet      = false;

    private boolean                                        expanded               = false;
    private transient UniqueAlltimeID                      uniqueID               = new UniqueAlltimeID();
    protected CrawledPackageView                           view;
    private String                                         compiledDownloadFolder = null;
    private transient ModifyLock                           lock                   = new ModifyLock();
    private static GeneralSettings                         cfg                    = JsonConfig.create(GeneralSettings.class);

    private PackageControllerComparator<CrawledLink>       sorter;

    public UniqueAlltimeID getUniqueID() {
        return uniqueID;
    }

    public CrawledPackage() {
        children = new ArrayList<CrawledLink>();
        if (cfg.isAutoSortChildrenEnabled()) {
            sorter = SORTER_ASC;
        }
    }

    public void copyPropertiesTo(CrawledPackage dest) {
        if (dest == null || dest == this) return;
        dest.name = name;
        dest.comment = comment;
        if (this.isDownloadFolderSet()) dest.setDownloadFolder(getRawDownloadFolder());

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
        String ret = compiledDownloadFolder;
        if (ret != null) return ret;
        ret = downloadFolder;
        String packageName = getName();
        if (JsonConfig.create(GeneralSettings.class).getSubfolderThreshold() > getChildren().size()) {
            packageName = null;
        }
        ret = PackagizerController.replaceDynamicTags(ret, packageName);
        compiledDownloadFolder = ret;
        return ret;
    }

    public long getFinishedDate() {
        return 0;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the raw Downloadfolder String. This link may contain wildcards like <jd:packagename>. Use {@link #getDownloadFolder()} to return the actuall
     * downloadloadfolder
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
        if (StringUtils.equals(name, this.name)) return;
        if (name != null) {
            name = CrossSystem.alleviatePathParts(name);
            if (StringUtils.equals(name, this.name)) return;
        }
        setType(TYPE.NORMAL);
        this.name = name;
        compiledDownloadFolder = null;
        if (hasNotificationListener()) nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledPackageProperty(this, CrawledPackageProperty.Property.NAME, getName()));
    }

    public void setDownloadFolder(String downloadFolder) {
        if (!StringUtils.isEmpty(downloadFolder)) {
            downloadFolderSet = true;
            this.downloadFolder = downloadFolder;
        } else {
            this.downloadFolder = JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();
            this.downloadFolderSet = false;
        }
        compiledDownloadFolder = null;
        if (hasNotificationListener()) nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledPackageProperty(this, CrawledPackageProperty.Property.FOLDER, getDownloadFolder()));
    }

    public void setEnabled(boolean b) {
    }

    public void setExpanded(boolean b) {
        this.expanded = b;
    }

    public CrawledPackageView getView() {
        if (view != null) return view;
        synchronized (this) {
            if (view == null) {
                CrawledPackageView lfpInfo = new CrawledPackageView();
                view = lfpInfo;
            }
        }
        return view;
    }

    public boolean isEnabled() {
        return getView().isEnabled();
    }

    public int indexOf(CrawledLink child) {
        boolean readL = getModifyLock().readLock();
        try {
            return children.indexOf(child);
        } finally {
            if (readL) getModifyLock().readUnlock(readL);
        }
    }

    @Override
    public PackageControllerComparator<CrawledLink> getCurrentSorter() {
        return sorter;
    }

    @Override
    public void setCurrentSorter(PackageControllerComparator<CrawledLink> comparator) {
        sorter = comparator;
    }

    @Override
    public void nodeUpdated(AbstractNode source, NOTIFY notify, Object param) {
        PackageController<CrawledPackage, CrawledLink> n = getControlledBy();
        if (n == null) return;
        AbstractNode lsource = source;
        if (lsource == null) lsource = this;
        n.nodeUpdated(lsource, notify, param);
    }

    @Override
    public ModifyLock getModifyLock() {
        return lock;
    }

    @Override
    public boolean hasNotificationListener() {
        PackageController<CrawledPackage, CrawledLink> n = getControlledBy();
        if (n != null && n.hasNotificationListener()) return true;
        return false;
    }

}