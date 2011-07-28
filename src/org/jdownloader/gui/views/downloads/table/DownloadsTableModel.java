package org.jdownloader.gui.views.downloads.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.Icon;

import jd.controlling.DownloadController;
import jd.controlling.IOEQ;
import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;

import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.table.ExtColumn;
import org.appwork.utils.swing.table.ExtTableModel;
import org.jdownloader.gui.views.downloads.columns.AddedDateColumn;
import org.jdownloader.gui.views.downloads.columns.ConnectionColumn;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.gui.views.downloads.columns.FinishedDateColumn;
import org.jdownloader.gui.views.downloads.columns.HosterColumn;
import org.jdownloader.gui.views.downloads.columns.ListOrderIDColumn;
import org.jdownloader.gui.views.downloads.columns.LoadedColumn;
import org.jdownloader.gui.views.downloads.columns.PriorityColumn;
import org.jdownloader.gui.views.downloads.columns.ProgressColumn;
import org.jdownloader.gui.views.downloads.columns.RemainingColumn;
import org.jdownloader.gui.views.downloads.columns.SizeColumn;
import org.jdownloader.gui.views.downloads.columns.SpeedColumn;
import org.jdownloader.gui.views.downloads.columns.StopSignColumn;
import org.jdownloader.gui.views.downloads.columns.TaskColumn;

public class DownloadsTableModel extends ExtTableModel<PackageLinkNode> {

    public static enum TOGGLEMODE {
        CURRENT, TOP, BOTTOM
    }

    private static final long   serialVersionUID   = -198189279671615981L;

    private static final String SORT_DOWNLOADORDER = "DOWNLOAD";

    private AtomicLong          tableChangesDone   = new AtomicLong(0);
    private AtomicLong          tableChangesReq    = new AtomicLong(0);

    private ListOrderIDColumn   downloadOrder;

    public DownloadsTableModel() {
        super("downloadstable");

    }

    @Override
    protected void initColumns() {

        this.addColumn(new FileColumn());
        this.addColumn(new SizeColumn());
        this.addColumn(new HosterColumn());
        addColumn(new ConnectionColumn());
        this.addColumn(new TaskColumn());
        this.addColumn(new RemainingColumn());

        this.addColumn(new AddedDateColumn());
        this.addColumn(new FinishedDateColumn());

        addColumn(new SpeedColumn());
        addColumn(new ETAColumn());

        this.addColumn(new LoadedColumn());
        this.addColumn(new ProgressColumn());

        this.addColumn(new PriorityColumn());
        this.addColumn(new StopSignColumn());

        this.addColumn(downloadOrder = new ListOrderIDColumn());
        // reset sort

        try {
            getStorage().put(ExtTableModel.SORT_ORDER_ID_KEY, ExtColumn.SORT_ASC);
            getStorage().put(ExtTableModel.SORTCOLUMN_KEY, downloadOrder.getID());
        } catch (final Exception e) {
            Log.exception(e);
        }

    }

    /**
     * we want to return to default sort after each start
     */
    protected boolean isSortStateSaverEnabled() {
        return false;
    }

    /**
     * @return
     */
    protected ExtColumn<PackageLinkNode> getDefaultSortColumn() {
        downloadOrder.setSortOrderIdentifier(ExtColumn.SORT_DESC);
        return downloadOrder;
    }

    protected void recreateModel() {
        tableChangesReq.incrementAndGet();
        IOEQ.add(new Runnable() {

            public void run() {
                if (tableChangesDone.incrementAndGet() != tableChangesReq.get()) {
                    System.out.println("skip tableMod_recreate");
                    return;
                }
                final ArrayList<PackageLinkNode> newtableData = DownloadsTableModel.this.refreshSort(tableData);
                _fireTableStructureChanged(newtableData, false);
            }

        }, true);
    }

    protected void refreshModel() {
        tableChangesReq.incrementAndGet();
        IOEQ.add(new Runnable() {

            public void run() {
                if (tableChangesDone.incrementAndGet() != tableChangesReq.get()) {
                    System.out.println("skip tableMod_refresh");
                    return;
                }
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        /* we just want to repaint */
                        DownloadsTableModel.this.getTable().repaint();
                    }
                };
            }

        }, true);
    }

    @Override
    public String getNextSortIdentifier(String sortOrderIdentifier) {
        if (sortOrderIdentifier == null || sortOrderIdentifier.equals(ExtColumn.SORT_ASC)) {
            return ExtColumn.SORT_DESC;
        } else if (sortOrderIdentifier.equals(ExtColumn.SORT_DESC)) {
            return SORT_DOWNLOADORDER;
        } else {
            return ExtColumn.SORT_ASC;
        }
    }

    public Icon getSortIcon(String sortOrderIdentifier) {
        if (SORT_DOWNLOADORDER.equals(sortOrderIdentifier)) { return null; }
        return super.getSortIcon(sortOrderIdentifier);
    }

    protected void toggleFilePackageExpand(final FilePackage fp2, final TOGGLEMODE mode) {
        tableChangesReq.incrementAndGet();
        final boolean cur = !fp2.isExpanded();
        IOEQ.add(new Runnable() {

            public void run() {
                boolean doToggle = true;
                switch (mode) {
                case CURRENT:
                    fp2.setExpanded(cur);
                    break;
                case TOP:
                    doToggle = true;
                    break;
                case BOTTOM:
                    doToggle = false;
                    break;
                }
                /*
                 * we use size of old table to minimize need to increase table
                 * size while adding nodes to it
                 */
                synchronized (DownloadController.ACCESSLOCK) {
                    for (FilePackage fp : DownloadController.getInstance().getPackages()) {
                        if (mode != TOGGLEMODE.CURRENT) {
                            if (doToggle) {
                                fp.setExpanded(cur);
                                if (fp == fp2) doToggle = false;
                            } else {
                                if (fp == fp2) {
                                    doToggle = true;
                                    fp.setExpanded(cur);
                                }
                            }
                        }
                    }
                }
                if (tableChangesDone.incrementAndGet() != tableChangesReq.get()) {
                    System.out.println("skip tableMod_toggle");
                    return;
                }
                final ArrayList<PackageLinkNode> newtableData = DownloadsTableModel.this.refreshSort(tableData);
                _fireTableStructureChanged(newtableData, false);
            }
        });
    }

    /*
     * we override sort to have a better sorting of packages/files, to keep
     * their structure alive,data is only used to specify the size of the new
     * ArrayList
     */
    @Override
    public ArrayList<PackageLinkNode> sort(final ArrayList<PackageLinkNode> data, ExtColumn<PackageLinkNode> column) {
        if (column.getSortOrderIdentifier() == SORT_DOWNLOADORDER) {
            column = downloadOrder;
            downloadOrder.setSortOrderIdentifier(ExtColumn.SORT_DESC);
        }
        this.sortColumn = column;
        String id = column.getSortOrderIdentifier();
        try {
            getStorage().put(ExtTableModel.SORT_ORDER_ID_KEY, id);
            getStorage().put(ExtTableModel.SORTCOLUMN_KEY, column.getID());
        } catch (final Exception e) {
            Log.exception(e);
        }
        synchronized (DownloadController.ACCESSLOCK) {
            /* get all packages from controller */
            ArrayList<PackageLinkNode> packages = new ArrayList<PackageLinkNode>(DownloadController.getInstance().size());
            packages.addAll(DownloadController.getInstance().getPackages());
            /* sort packages */
            Collections.sort(packages, column.getRowSorter());
            ArrayList<PackageLinkNode> newData = new ArrayList<PackageLinkNode>(Math.max(data.size(), packages.size()));
            for (PackageLinkNode node : packages) {
                newData.add(node);
                if (!((FilePackage) node).isExpanded()) continue;
                ArrayList<PackageLinkNode> files = null;
                synchronized (node) {
                    files = new ArrayList<PackageLinkNode>(((FilePackage) node).getControlledDownloadLinks());
                    Collections.sort(files, column.getRowSorter());
                }
                newData.addAll(files);
            }
            return newData;
        }
    }

    public boolean isDownloadOrder() {
        return getSortColumn() == downloadOrder;
    }
}
