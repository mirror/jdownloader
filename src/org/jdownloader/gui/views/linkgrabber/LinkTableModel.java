package org.jdownloader.gui.views.linkgrabber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.DownloadController;
import jd.controlling.IOEQ;
import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;

import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.table.ExtColumn;
import org.appwork.utils.swing.table.ExtTableModel;

public abstract class LinkTableModel extends ExtTableModel<PackageLinkNode> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static enum TOGGLEMODE {
        CURRENT,
        TOP,
        BOTTOM
    }

    public LinkTableModel(String id) {
        super(id);
    }

    private AtomicLong tableChangesDone = new AtomicLong(0);
    private AtomicLong tableChangesReq  = new AtomicLong(0);

    public void recreateModel() {
        tableChangesReq.incrementAndGet();
        IOEQ.add(new Runnable() {

            public void run() {
                if (tableChangesDone.incrementAndGet() != tableChangesReq.get()) {
                    System.out.println("skip tableMod_recreate");
                    return;
                }
                final ArrayList<PackageLinkNode> newtableData = refreshSort(tableData);
                _fireTableStructureChanged(newtableData, false);
            }

        }, true);
    }

    public void refreshModel() {
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
                        getTable().repaint();
                    }
                };
            }

        }, true);
    }

    public void toggleFilePackageExpand(final FilePackage fp2, final TOGGLEMODE mode) {
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
                final ArrayList<PackageLinkNode> newtableData = refreshSort(tableData);
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
}
