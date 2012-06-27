package jd.controlling.linkcollector;

import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.ChildComparator;

import org.appwork.storage.Storable;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.logging.Log;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;

public class CrawledPackageStorable implements Storable {

    public static enum TYPE {
        NORMAL,
        OFFLINE,
        POFFLINE,
        VARIOUS
    }

    private TYPE   type      = null;
    private String packageID = null;

    /* this one is correct during JSON serialization */
    public TYPE getType() {
        if (pkg instanceof OfflineCrawledPackage) return TYPE.OFFLINE;
        if (pkg instanceof PermanentOfflinePackage) return TYPE.POFFLINE;
        if (pkg instanceof VariousCrawledPackage) return TYPE.VARIOUS;
        return TYPE.NORMAL;
    }

    public void setType(TYPE type) {
        this.type = type;
    }

    /* this one was set by JSON on restore */
    public TYPE _getType() {
        return type;
    }

    private CrawledPackage                 pkg;
    private ArrayList<CrawledLinkStorable> links;

    public String getComment() {
        return pkg.getComment();
    }

    public String getSorterId() {
        if (pkg.getCurrentSorter() == null) return null;
        boolean asc = pkg.getCurrentSorter().isAsc();
        return ((asc ? "ASC" : "DSC") + "." + pkg.getCurrentSorter().getID());
    }

    public void setSorterId(String id) {
        try {
            /*
             * Ugly Restoremethod for the current package sorter
             */
            if (id == null) {
                pkg.setCurrentSorter(null);
                return;
            }
            boolean asc = id.startsWith("ASC.");
            int index = id.indexOf("Column.");
            if (id.endsWith("jd.controlling.linkcrawler.CrawledPackage")) {
                // default sorter
                pkg.setCurrentSorter(asc ? CrawledPackage.SORTER_ASC : CrawledPackage.SORTER_DESC);
                return;
            }

            // Column Sorter
            String colID = id.substring(index + 7);
            for (final ExtColumn<AbstractNode> c : LinkGrabberTableModel.getInstance().getColumns()) {
                if (colID.equals(c.getID())) {
                    if (asc) {
                        pkg.setCurrentSorter(new ChildComparator<CrawledLink>() {

                            public int compare(CrawledLink o1, CrawledLink o2) {

                                return c.getRowSorter().compare(o1, o2);

                            }

                            @Override
                            public String getID() {
                                return c.getModel().getModelID() + ".Column." + c.getID();
                            }

                            @Override
                            public boolean isAsc() {
                                return true;
                            }
                        });
                    } else {
                        pkg.setCurrentSorter(new ChildComparator<CrawledLink>() {

                            public int compare(CrawledLink o1, CrawledLink o2) {

                                return c.getRowSorter().compare(o2, o1);

                            }

                            @Override
                            public String getID() {
                                return c.getModel().getModelID() + ".Column." + c.getID();
                            }

                            @Override
                            public boolean isAsc() {
                                return false;
                            }
                        });
                    }
                    break;
                }
            }
        } catch (Throwable t) {
            Log.exception(t);
        }

    }

    public void setComment(String comment) {
        pkg.setComment(comment);
    }

    @SuppressWarnings("unused")
    private CrawledPackageStorable(/* storable */) {
        pkg = new CrawledPackage();
        links = new ArrayList<CrawledLinkStorable>();
    }

    public CrawledPackageStorable(CrawledPackage pkg) {
        this.pkg = pkg;
        links = new ArrayList<CrawledLinkStorable>(pkg.getChildren().size());
        synchronized (pkg) {
            for (CrawledLink link : pkg.getChildren()) {
                links.add(new CrawledLinkStorable(link));
            }
        }
    }

    public CrawledPackage _getCrawledPackage() {
        return pkg;
    }

    public long getCreated() {
        return pkg.getCreated();
    }

    public String getDownloadFolder() {
        if (!pkg.isDownloadFolderSet()) return null;
        return pkg.getRawDownloadFolder();
    }

    public ArrayList<CrawledLinkStorable> getLinks() {
        return links;
    }

    public boolean isExpanded() {
        return pkg.isExpanded();
    }

    public void setCreated(long created) {
        pkg.setCreated(created);
    }

    public void setDownloadFolder(String downloadFolder) {
        this.pkg.setDownloadFolder(downloadFolder);
    }

    public void setExpanded(boolean expanded) {
        this.pkg.setExpanded(expanded);
    }

    public void setLinks(ArrayList<CrawledLinkStorable> links) {
        if (links != null) {
            this.links = links;
            synchronized (pkg) {
                for (CrawledLinkStorable link : links) {
                    CrawledLink l = link._getCrawledLink();
                    pkg.getChildren().add(l);
                    l.setParentNode(pkg);
                }
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

    /**
     * @return the packageID
     */
    public String getPackageID() {
        return packageID;
    }

}
