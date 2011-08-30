package jd.controlling.packagecontroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.IOEQ;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;

public abstract class PackageControllerTableModel<E extends AbstractPackageNode<V, E>, V extends AbstractPackageChildrenNode<E>> extends ExtTableModel<AbstractNode> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static enum TOGGLEMODE {
        CURRENT,
        TOP,
        BOTTOM
    }

    private DelayedRunnable         asyncRefresh;
    private PackageController<E, V> pc;

    public PackageControllerTableModel(PackageController<E, V> pc, String id) {
        super(id);
        this.pc = pc;
        asyncRefresh = new DelayedRunnable(IOEQ.TIMINGQUEUE, 150l, 250l) {
            @Override
            public void delayedrun() {
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        /* we just want to repaint */
                        getTable().repaint();
                    }
                };
            }

        };
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
                final ArrayList<AbstractNode> newtableData = refreshSort(tableData);
                _fireTableStructureChanged(newtableData, false);
            }

        }, true);
    }

    public void refreshModel() {
        asyncRefresh.run();
    }

    public void toggleFilePackageExpand(final E fp2, final TOGGLEMODE mode) {
        tableChangesReq.incrementAndGet();
        IOEQ.getQueue().addAsynch(new QueueAction<Void, RuntimeException>() {

            @Override
            protected boolean allowAsync() {
                return true;
            }

            @Override
            public QueuePriority getQueuePrio() {
                return QueuePriority.HIGH;
            }

            @Override
            protected Void run() throws RuntimeException {
                final boolean cur = !fp2.isExpanded();
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
                if (mode != TOGGLEMODE.CURRENT) {
                    final boolean readL = pc.readLock();
                    try {
                        for (E fp : pc.packages) {

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
                    } finally {
                        pc.readUnlock(readL);
                    }
                }
                if (tableChangesDone.incrementAndGet() != tableChangesReq.get()) {
                    System.out.println("skip tableMod_toggle");
                    return null;
                }
                final ArrayList<AbstractNode> newtableData = refreshSort(tableData);
                _fireTableStructureChanged(newtableData, false);
                return null;
            }
        });
    }

    /*
     * we override sort to have a better sorting of packages/files, to keep
     * their structure alive,data is only used to specify the size of the new
     * ArrayList
     */
    @SuppressWarnings("unchecked")
    @Override
    public ArrayList<AbstractNode> sort(final ArrayList<AbstractNode> data, ExtColumn<AbstractNode> column) {
        this.sortColumn = column;
        String id = null;
        if (column != null) id = column.getSortOrderIdentifier();
        try {
            if (column != null) {
                getStorage().put(ExtTableModel.SORT_ORDER_ID_KEY, id);
                getStorage().put(ExtTableModel.SORTCOLUMN_KEY, column.getID());
            } else {
                getStorage().put(ExtTableModel.SORT_ORDER_ID_KEY, (String) null);
                getStorage().put(ExtTableModel.SORTCOLUMN_KEY, (String) null);
            }
        } catch (final Exception e) {
            Log.exception(e);
        }
        ArrayList<AbstractNode> packages = null;
        final boolean readL = pc.readLock();
        try {
            /* get all packages from controller */
            packages = new ArrayList<AbstractNode>(pc.size());
            packages.addAll(pc.packages);
        } finally {
            pc.readUnlock(readL);
        }
        /* sort packages */
        if (column != null) Collections.sort(packages, column.getRowSorter());
        ArrayList<AbstractNode> newData = new ArrayList<AbstractNode>(Math.max(data.size(), packages.size()));
        for (AbstractNode node : packages) {
            newData.add(node);
            if (!((E) node).isExpanded()) continue;
            ArrayList<AbstractNode> files = null;
            synchronized (node) {
                files = new ArrayList<AbstractNode>(((E) node).getChildren());
                if (column != null) Collections.sort(files, column.getRowSorter());
            }
            newData.addAll(files);
        }
        return newData;

    }
}
