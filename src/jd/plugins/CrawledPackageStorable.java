package jd.plugins;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.PackageControllerComparator;

import org.appwork.storage.Storable;
import org.appwork.storage.StorableAllowPrivateAccessModifier;
import org.appwork.storage.TypeRef;
import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.controlling.Priority;

public class CrawledPackageStorable implements Storable {
    public static final TypeRef<CrawledPackageStorable> TYPEREF = new TypeRef<CrawledPackageStorable>() {
                                                                };

    public static enum TYPE {
        NORMAL,
        OFFLINE,
        POFFLINE,
        VARIOUS
    }

    private TYPE   type      = TYPE.NORMAL;
    private String packageID = null;

    /* this one is correct during JSON serialization */
    public TYPE getType() {
        switch (pkg.getType()) {
        case NORMAL:
            return TYPE.NORMAL;
        case OFFLINE:
            return TYPE.OFFLINE;
        case POFFLINE:
            return TYPE.POFFLINE;
        case VARIOUS:
            return TYPE.VARIOUS;
        default:
            return TYPE.NORMAL;
        }
    }

    public void setType(TYPE type) {
        if (type == null) {
            type = TYPE.NORMAL;
        }
        this.type = type;
    }

    public long getUID() {
        return pkg.getUniqueID().getID();
    }

    public void setUID(long id) {
        pkg.getUniqueID().setID(id);
    }

    private CrawledPackage                      pkg;
    private java.util.List<CrawledLinkStorable> links;

    public String getComment() {
        return pkg.getComment();
    }

    public String getSorterId() {
        final PackageControllerComparator<AbstractNode> lSorter = pkg.getCurrentSorter();
        if (lSorter == null) {
            return null;
        } else {
            final boolean asc = lSorter.isAsc();
            return ((asc ? ExtColumn.SORT_ASC : ExtColumn.SORT_DESC) + "." + lSorter.getID());
        }
    }

    public void setSorterId(String id) {
        if (id == null) {
            pkg.setCurrentSorter(null);
        } else {
            pkg.setCurrentSorter(PackageControllerComparator.getComparator(id));
        }
    }

    public void setComment(String comment) {
        pkg.setComment(comment);
    }

    @SuppressWarnings("unused")
    @StorableAllowPrivateAccessModifier
    private CrawledPackageStorable(/* storable */) {
        this(new CrawledPackage(), false);
    }

    public CrawledPackageStorable(CrawledPackage pkg) {
        this(pkg, true);
    }

    public CrawledPackageStorable(CrawledPackage pkg, boolean includeChildren) {
        this.pkg = pkg;
        if (!includeChildren) {
            links = new ArrayList<CrawledLinkStorable>();
        } else {
            final boolean readL = pkg.getModifyLock().readLock();
            try {
                links = new ArrayList<CrawledLinkStorable>(pkg.getChildren().size());
                for (CrawledLink link : pkg.getChildren()) {
                    links.add(new CrawledLinkStorable(link));
                }
            } finally {
                pkg.getModifyLock().readUnlock(readL);
            }
        }
    }

    public CrawledPackage _getCrawledPackage() {
        switch (type) {
        case NORMAL:
            pkg.setType(jd.controlling.linkcrawler.CrawledPackage.TYPE.NORMAL);
            break;
        case OFFLINE:
            pkg.setType(jd.controlling.linkcrawler.CrawledPackage.TYPE.OFFLINE);
            break;
        case POFFLINE:
            pkg.setType(jd.controlling.linkcrawler.CrawledPackage.TYPE.POFFLINE);
            break;
        case VARIOUS:
            pkg.setType(jd.controlling.linkcrawler.CrawledPackage.TYPE.VARIOUS);
            break;
        }
        return pkg;
    }

    public long getCreated() {
        return pkg.getCreated();
    }

    public long getModified() {
        return pkg.getModified();
    }

    public String getDownloadFolder() {
        return pkg.getRawDownloadFolder();
    }

    public List<CrawledLinkStorable> getLinks() {
        return links;
    }

    public boolean isExpanded() {
        return pkg.isExpanded();
    }

    public void setCreated(long created) {
        pkg.setCreated(created);
    }

    public void setModified(long modified) {
        pkg.setModified(modified);
    }

    public void setDownloadFolder(String downloadFolder) {
        this.pkg.setDownloadFolder(downloadFolder);
    }

    public void setExpanded(boolean expanded) {
        this.pkg.setExpanded(expanded);
    }

    public void setLinks(List<CrawledLinkStorable> links) {
        if (links != null) {
            this.links = links;
            try {
                pkg.getModifyLock().writeLock();
                final List<CrawledLink> children = pkg.getChildren();
                for (final CrawledLinkStorable link : links) {
                    if (link != null) {
                        final CrawledLink crawledLink = link._getCrawledLink();
                        children.add(crawledLink);
                        crawledLink.setParentNode(pkg);
                        final DownloadLink downloadLink = crawledLink.getDownloadLink();
                        downloadLink.setNodeChangeListener(crawledLink);
                    }
                }
            } finally {
                pkg.getModifyLock().writeUnlock();
            }
        }
    }

    /**
     * @param customName
     *            the customName to set
     */
    public void setName(String name) {
        this.pkg.setName(name);
    }

    /**
     * @return the customName
     */
    public String getName() {
        return pkg.getName();
    }

    /**
     * @param packageID
     *            the packageID to set
     */
    public void setPackageID(String packageID) {
        this.packageID = packageID;
    }

    public String getPriority() {
        return pkg.getPriorityEnum().name();
    }

    public void setPriority(String priority) {
        try {
            if (priority != null) {
                pkg.setPriorityEnum(Priority.valueOf(priority));
            }
        } catch (final Throwable e) {
        }
    }

    /**
     * @return the packageID
     */
    public String getPackageID() {
        return packageID;
    }
}
