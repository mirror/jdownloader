package jd.controlling.packagecontroller;

import java.util.Comparator;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import jd.nutils.NaturalOrderComparator;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.logging.LogController;

public abstract class PackageControllerComparator<T extends AbstractNode> implements java.util.Comparator<T> {
    public static final PackageControllerComparator<AbstractNode> SORTER_ASC  = new PackageControllerComparator<AbstractNode>() {
                                                                                  private final Comparator<String> comp = new NaturalOrderComparator();

                                                                                  public int compare(AbstractNode o1, AbstractNode o2) {
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
                                                                                      return "jd.generic.Name";
                                                                                  }

                                                                                  @Override
                                                                                  public boolean isAsc() {
                                                                                      return true;
                                                                                  }
                                                                              };
    public static final PackageControllerComparator<AbstractNode> SORTER_DESC = new PackageControllerComparator<AbstractNode>() {
                                                                                  public int compare(AbstractNode o1, AbstractNode o2) {
                                                                                      return SORTER_ASC.compare(o2, o1);
                                                                                  }

                                                                                  @Override
                                                                                  public String getID() {
                                                                                      return SORTER_ASC.getID();
                                                                                  }

                                                                                  @Override
                                                                                  public boolean isAsc() {
                                                                                      return false;
                                                                                  }
                                                                              };

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

    public static PackageControllerComparator<AbstractNode> getComparator(String id) {
        if (id != null) {
            final boolean asc = id.startsWith(ExtColumn.SORT_ASC + ".");
            if (id.endsWith("jd.generic.Name") || id.endsWith("jd.controlling.linkcrawler.CrawledPackage")) {
                return asc ? SORTER_ASC : SORTER_DESC;
            } else {
                try {
                    synchronized (CACHE) {
                        for (Entry<PackageControllerComparator<AbstractNode>, String> entry : CACHE.entrySet()) {
                            if (id.equals(entry.getValue())) {
                                return entry.getKey();
                            }
                        }
                        PackageControllerComparator<AbstractNode> ret = null;
                        final int columnIndex = id.indexOf("Column.");
                        if (columnIndex != -1) {
                            final String colID = id.substring(columnIndex + 7);
                            if (id.contains(DownloadsTableModel.getInstance().getModelID())) {
                                for (final ExtColumn<AbstractNode> c : DownloadsTableModel.getInstance().getColumns()) {
                                    if (colID.equals(c.getID())) {
                                        ret = new PackageControllerComparator<AbstractNode>() {
                                            public int compare(AbstractNode o1, AbstractNode o2) {
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
                            if (ret == null && id.contains(LinkGrabberTableModel.getInstance().getModelID())) {
                                for (final ExtColumn<AbstractNode> c : LinkGrabberTableModel.getInstance().getColumns()) {
                                    if (colID.equals(c.getID())) {
                                        ret = new PackageControllerComparator<AbstractNode>() {
                                            public int compare(AbstractNode o1, AbstractNode o2) {
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
                        }
                        CACHE.put(ret, id);
                        return ret;
                    }
                } catch (Throwable t) {
                    LogController.CL(true).log(t);
                }
            }
        }
        return null;
    }

    private final static WeakHashMap<PackageControllerComparator<AbstractNode>, String> CACHE = new WeakHashMap<PackageControllerComparator<AbstractNode>, String>();
}
