package jd.controlling.packagecontroller;

import java.util.Map.Entry;
import java.util.WeakHashMap;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.logging.LogController;

public abstract class PackageControllerComparator<T> implements java.util.Comparator<T> {
    public abstract String getID();

    public abstract boolean isAsc();

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (obj instanceof PackageControllerComparator) {
            final PackageControllerComparator c = (PackageControllerComparator) obj;
            return c.isAsc() == isAsc() && StringUtils.equals(c.getID(), getID());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getID().hashCode();
    }

    public static PackageControllerComparator<DownloadLink> getDownloadLinkComparator(String id) {
        if (id != null) {
            final boolean asc = id.startsWith(ExtColumn.SORT_ASC + ".");
            if (id.endsWith("jd.plugins.FilePackage")) {
                return asc ? FilePackage.SORTER_ASC : FilePackage.SORTER_DESC;
            } else {
                try {
                    synchronized (DOWNLOADLIST_COLUMNS) {
                        for (Entry<PackageControllerComparator<DownloadLink>, String> entry : DOWNLOADLIST_COLUMNS.entrySet()) {
                            if (id.equals(entry.getValue())) {
                                return entry.getKey();
                            }
                        }
                        PackageControllerComparator<DownloadLink> ret = null;
                        final int columnIndex = id.indexOf("Column.");
                        if (columnIndex != -1) {
                            final String colID = id.substring(columnIndex + 7);
                            for (final ExtColumn<AbstractNode> c : DownloadsTableModel.getInstance().getColumns()) {
                                if (colID.equals(c.getID())) {
                                    ret = new PackageControllerComparator<DownloadLink>() {
                                        public int compare(DownloadLink o1, DownloadLink o2) {
                                            final ExtDefaultRowSorter<AbstractNode> sorter = c.getRowSorter();
                                            sorter.setThreadSortOrderIdentifier(ExtColumn.SORT_ASC);
                                            try {
                                                if (isAsc()) {
                                                    return sorter.compare(o1, o2);
                                                } else {
                                                    return sorter.compare(o2, o1);
                                                }
                                            } finally {
                                                sorter.setThreadSortOrderIdentifier(null);
                                            }
                                        }

                                        @Override
                                        public String getID() {
                                            return c.getModel().getModelID() + ".Column." + c.getID();
                                        }

                                        @Override
                                        public boolean isAsc() {
                                            return asc;
                                        }
                                    };
                                    break;
                                }
                            }
                        }
                        DOWNLOADLIST_COLUMNS.put(ret, id);
                        return ret;
                    }
                } catch (Throwable t) {
                    LogController.CL(true).log(t);
                }
            }
        }
        return null;
    }

    private final static WeakHashMap<PackageControllerComparator<DownloadLink>, String> DOWNLOADLIST_COLUMNS  = new WeakHashMap<PackageControllerComparator<DownloadLink>, String>();
    private final static WeakHashMap<PackageControllerComparator<CrawledLink>, String>  LINKCOLLECTOR_COLUMNS = new WeakHashMap<PackageControllerComparator<CrawledLink>, String>();

    public static PackageControllerComparator<CrawledLink> getCrawledLinkComparator(String id) {
        if (id != null) {
            final boolean asc = id.startsWith(ExtColumn.SORT_ASC + ".");
            if (id.endsWith("jd.controlling.linkcrawler.CrawledPackage")) {
                return asc ? CrawledPackage.SORTER_ASC : CrawledPackage.SORTER_DESC;
            } else {
                try {
                    synchronized (LINKCOLLECTOR_COLUMNS) {
                        for (Entry<PackageControllerComparator<CrawledLink>, String> entry : LINKCOLLECTOR_COLUMNS.entrySet()) {
                            if (id.equals(entry.getValue())) {
                                return entry.getKey();
                            }
                        }
                        PackageControllerComparator<CrawledLink> ret = null;
                        final int columnIndex = id.indexOf("Column.");
                        if (columnIndex != -1) {
                            final String colID = id.substring(columnIndex + 7);
                            for (final ExtColumn<AbstractNode> c : LinkGrabberTableModel.getInstance().getColumns()) {
                                if (colID.equals(c.getID())) {
                                    ret = new PackageControllerComparator<CrawledLink>() {
                                        public int compare(CrawledLink o1, CrawledLink o2) {
                                            final ExtDefaultRowSorter<AbstractNode> sorter = c.getRowSorter();
                                            sorter.setThreadSortOrderIdentifier(ExtColumn.SORT_ASC);
                                            try {
                                                if (isAsc()) {
                                                    return sorter.compare(o1, o2);
                                                } else {
                                                    return sorter.compare(o2, o1);
                                                }
                                            } finally {
                                                sorter.setThreadSortOrderIdentifier(null);
                                            }
                                        }

                                        @Override
                                        public String getID() {
                                            return c.getModel().getModelID() + ".Column." + c.getID();
                                        }

                                        @Override
                                        public boolean isAsc() {
                                            return asc;
                                        }
                                    };
                                    break;
                                }
                            }
                        }
                        LINKCOLLECTOR_COLUMNS.put(ret, id);
                        return ret;
                    }
                } catch (Throwable t) {
                    LogController.CL(true).log(t);
                }
            }
        }
        return null;
    }
}
