package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractNodeNotifier;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;
import jd.controlling.packagecontroller.PackageControllerComparator;
import jd.nutils.NaturalOrderComparator;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.ModifyLock;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.controlling.packagizer.SubFolderByPackageRule;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.staticreferences.CFG_LINKCOLLECTOR;

public class CrawledPackage implements AbstractPackageNode<CrawledLink, CrawledPackage> {
    public static final PackageControllerComparator<CrawledLink> SORTER_ASC  = new PackageControllerComparator<CrawledLink>() {
        private final Comparator<String> comp = new NaturalOrderComparator();

        public int compare(CrawledLink o1, CrawledLink o2) {
            String o1s = o1.getName();
            String o2s = o2.getName();
            if (o1s == null) {
                o1s = "";
            }
            if (o2s == null) {
                o2s = "";
            }
            return comp.compare(o1s, o2s);
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
        private final Comparator<String> comp = new NaturalOrderComparator();

        public int compare(CrawledLink o1, CrawledLink o2) {
            String o1s = o1.getName();
            String o2s = o2.getName();
            if (o1s == null) {
                o1s = "";
            }
            if (o2s == null) {
                o2s = "";
            }
            return comp.compare(o2s, o1s);
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

    private static final int                               SUBFOLDER_THRESHOLD        = JsonConfig.create(LinkgrabberSettings.class).getSubfolderThreshold();
    private static final SubFolderByPackageRule.COUNT      SUBFOLDER_COUNT            = JsonConfig.create(LinkgrabberSettings.class).getSubfolderCount();
    private static final GeneralSettings                   GENERALSETTINGS            = JsonConfig.create(GeneralSettings.class);
    private List<CrawledLink>                              children;
    private String                                         comment                    = null;
    private PackageController<CrawledPackage, CrawledLink> controller                 = null;
    private long                                           created                    = System.currentTimeMillis();
    private String                                         name                       = null;
    private String                                         downloadFolder             = null;
    private boolean                                        downloadFolderContainsTags = false;
    private boolean                                        expanded                   = CFG_LINKCOLLECTOR.CFG.isPackageAutoExpanded();
    private transient volatile UniqueAlltimeID             uniqueID                   = null;
    protected CrawledPackageView                           view;
    private String                                         compiledDownloadFolder     = null;
    private transient volatile ModifyLock                  lock                       = null;
    private PackageControllerComparator<CrawledLink>       sorter;
    private Priority                                       priority                   = Priority.DEFAULT;

    public UniqueAlltimeID getUniqueID() {
        if (uniqueID != null) {
            return uniqueID;
        }
        synchronized (this) {
            if (uniqueID != null) {
                return uniqueID;
            }
            uniqueID = new UniqueAlltimeID();
        }
        return uniqueID;
    }

    public CrawledPackage() {
        children = new ArrayList<CrawledLink>();
        if (GENERALSETTINGS.isAutoSortChildrenEnabled()) {
            sorter = SORTER_ASC;
        }
        setDownloadFolder(null);
    }

    public void copyPropertiesTo(CrawledPackage dest) {
        if (dest == null || dest == this) {
            return;
        }
        dest.name = name;
        dest.comment = comment;
        dest.setDownloadFolder(getRawDownloadFolder());
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
        if (ret == null) {
            ret = getRawDownloadFolder();
            if (ret != null && downloadFolderContainsTags) {
                if (SUBFOLDER_THRESHOLD == 0) {
                    ret = PackagizerController.replaceDynamicTags(ret, getName(), this);
                } else {
                    if (getChildren().size() <= SUBFOLDER_THRESHOLD) {
                        ret = PackagizerController.replaceDynamicTags(ret, null, this);
                    } else {
                        switch (SUBFOLDER_COUNT) {
                        case ITEMS:
                            ret = PackagizerController.replaceDynamicTags(ret, getName(), this);
                            break;
                        case NAMES:
                            final boolean readL = getModifyLock().readLock();
                            try {
                                final HashSet<String> names = new HashSet<String>();
                                for (final CrawledLink link : getChildren()) {
                                    names.add(link.getName());
                                    if (names.size() > SUBFOLDER_THRESHOLD) {
                                        break;
                                    }
                                }
                                if (names.size() > SUBFOLDER_THRESHOLD) {
                                    ret = PackagizerController.replaceDynamicTags(ret, getName(), this);
                                } else {
                                    ret = PackagizerController.replaceDynamicTags(ret, null, this);
                                }
                            } finally {
                                getModifyLock().readUnlock(readL);
                            }
                            break;
                        }
                    }
                }
            }
            compiledDownloadFolder = ret;
        }
        return ret;
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
        if (!StringUtils.equals(comment, this.comment)) {
            this.comment = comment;
            setType(TYPE.NORMAL);
            if (hasNotificationListener()) {
                nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledPackageProperty(this, CrawledPackageProperty.Property.COMMENT, getComment()));
            }
        }
    }

    public void setControlledBy(PackageController<CrawledPackage, CrawledLink> controller) {
        this.controller = controller;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public void setName(String name) {
        if (!StringUtils.equals(name, this.name)) {
            if (name != null) {
                name = CrossSystem.alleviatePathParts(name);
                if (StringUtils.equals(name, this.name)) {
                    return;
                }
            }
            setType(TYPE.NORMAL);
            this.name = name;
            compiledDownloadFolder = null;
            if (hasNotificationListener()) {
                nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledPackageProperty(this, CrawledPackageProperty.Property.NAME, getName()));
            }
        }
    }

    public void setDownloadFolder(final String downloadFolder) {
        if (!StringUtils.equals(downloadFolder, getDownloadFolder())) {
            if (!StringUtils.isEmpty(downloadFolder)) {
                downloadFolderContainsTags = downloadFolder.contains("<jd:");
                this.downloadFolder = downloadFolder.trim();
            } else {
                downloadFolderContainsTags = false;
                this.downloadFolder = null;
            }
            compiledDownloadFolder = null;
            if (hasNotificationListener()) {
                nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledPackageProperty(this, CrawledPackageProperty.Property.FOLDER, getDownloadFolder()));
            }
        }
    }

    public void setEnabled(boolean b) {
        final ArrayList<CrawledLink> links;
        final boolean readL = getModifyLock().readLock();
        try {
            links = new ArrayList<CrawledLink>(getChildren());
        } finally {
            if (readL) {
                getModifyLock().readUnlock(readL);
            }
        }
        for (final CrawledLink link : links) {
            link.setEnabled(b);
        }
    }

    public void setExpanded(boolean b) {
        this.expanded = b;
    }

    public CrawledPackageView getView() {
        if (view != null) {
            return view;
        }
        synchronized (this) {
            if (view == null) {
                final CrawledPackageView lfpInfo = new CrawledPackageView();
                view = lfpInfo;
            }
        }
        return view;
    }

    public boolean isEnabled() {
        return getView().isEnabled();
    }

    public int indexOf(CrawledLink child) {
        final boolean readL = getModifyLock().readLock();
        try {
            return children.indexOf(child);
        } finally {
            if (readL) {
                getModifyLock().readUnlock(readL);
            }
        }
    }

    public void setPriorityEnum(Priority priority) {
        if (priority == null) {
            priority = Priority.DEFAULT;
        }
        if (getPriorityEnum() != priority) {
            this.priority = priority;
            if (hasNotificationListener()) {
                nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledPackageProperty(this, CrawledPackageProperty.Property.PRIORITY, priority));
            }
        }
    }

    public Priority getPriorityEnum() {
        return priority;
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
        if (source == this && NOTIFY.STRUCTURE_CHANCE.equals(notify)) {
            compiledDownloadFolder = null;
        } else {
            if (source instanceof AbstractPackageChildrenNode && NOTIFY.PROPERTY_CHANCE.equals(notify) && param instanceof CrawledLinkProperty && CrawledLinkProperty.Property.NAME.equals(((CrawledLinkProperty) param).getProperty())) {
                compiledDownloadFolder = null;
            }
            final PackageController<CrawledPackage, CrawledLink> n = getControlledBy();
            if (n == null) {
                return;
            }
            final AbstractNode lsource;
            if (source == null) {
                lsource = this;
            } else {
                lsource = source;
            }
            if (lsource instanceof AbstractPackageChildrenNode) {
                final CrawledPackageView lView = view;
                if (lView != null) {
                    lView.requestUpdate();
                }
            }
            n.nodeUpdated(lsource, notify, param);
        }
    }

    @Override
    public ModifyLock getModifyLock() {
        if (lock != null) {
            return lock;
        }
        synchronized (this) {
            if (lock == null) {
                lock = new ModifyLock();
            }
        }
        return lock;
    }

    @Override
    public boolean hasNotificationListener() {
        final PackageController<CrawledPackage, CrawledLink> n = getControlledBy();
        if (n != null && n.hasNotificationListener()) {
            return true;
        }
        return false;
    }
}