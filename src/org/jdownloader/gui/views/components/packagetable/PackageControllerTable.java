package org.jdownloader.gui.views.components.packagetable;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;
import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.exceptions.WTFException;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtComponentRowHighlighter;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel.TOGGLEMODE;
import org.jdownloader.gui.views.components.packagetable.actions.SortPackagesDownloadOrdnerOnColumn;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

public abstract class PackageControllerTable<ParentType extends AbstractPackageNode<ChildrenType, ParentType>, ChildrenType extends AbstractPackageChildrenNode<ParentType>> extends BasicJDTable<AbstractNode> {

    protected class SelectionInfoCache {
        private final long tableDataVersion;

        public long getTableDataVersion() {
            return tableDataVersion;
        }

        protected volatile SelectionInfo<ParentType, ChildrenType> all;
        protected volatile SelectionInfo<ParentType, ChildrenType> all_filtered;
        protected volatile SelectionInfo<ParentType, ChildrenType> selection;
        protected volatile SelectionInfo<ParentType, ChildrenType> selection_filtered;
        protected final AtomicLong                                 selectionVersion = new AtomicLong(-1);

        protected SelectionInfoCache(long tableDataVersion) {
            this.tableDataVersion = tableDataVersion;
        }

    }

    public static final KeyStroke                                 KEY_STROKE_ALT_END  = KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.ALT_MASK);
    public static final KeyStroke                                 KEY_STROKE_ALT_HOME = KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.ALT_MASK);
    public static final KeyStroke                                 KEY_STROKE_ALT_DOWN = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_MASK);
    public static final KeyStroke                                 KEY_STROKE_ALT_UP   = KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_MASK);
    public static final KeyStroke                                 KEY_STROKE_RIGHT    = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
    public static final KeyStroke                                 KEY_STROKE_KP_RIGHT = KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, 0);
    public static final KeyStroke                                 KEY_STROKE_LEFT     = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0);
    public static final KeyStroke                                 KEY_STROKE_KP_LEFT  = KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0);
    /**
     * 
     */
    private static final long                                     serialVersionUID    = 3880570615872972276L;
    private PackageControllerTableModel<ParentType, ChildrenType> tableModel          = null;
    private Color                                                 sortNotifyColor;
    private Color                                                 filterNotifyColor;
    private AppAction                                             moveTopAction       = null;
    private AppAction                                             moveUpAction        = null;
    private AppAction                                             moveDownAction      = null;
    private AppAction                                             moveBottomAction    = null;
    private NullsafeAtomicReference<SelectionInfoCache>           selectionInfoCache  = new NullsafeAtomicReference<SelectionInfoCache>(null);
    private AtomicLong                                            selectionVersion    = new AtomicLong(0);
    private final DelayedRunnable                                 selectionDelayedUpdate;

    public ExtColumn<AbstractNode> getMouseOverColumn() {
        Point mp = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(mp, this);
        return getExtColumnAtPoint(mp);
    }

    @Override
    protected void initAlternateRowHighlighter() {
        super.initAlternateRowHighlighter();
        if (CFG_GUI.PACKAGES_BACKGROUND_HIGHLIGHT_ENABLED.isEnabled()) {
            Color tableFG = (LAFOptions.getInstance().getColorForTablePackageRowForeground());
            Color tableBG = (LAFOptions.getInstance().getColorForTablePackageRowBackground());
            // tableBG = Color.red;
            this.getModel().addExtComponentRowHighlighter(new ExtComponentRowHighlighter<AbstractNode>(tableFG, tableBG, null) {
                public int getPriority() {
                    return 0;
                }

                @Override
                public boolean accept(ExtColumn<AbstractNode> column, AbstractNode value, boolean selected, boolean focus, int row) {

                    return value instanceof AbstractPackageNode;
                }

            });
        }

    }

    public PackageControllerTable(PackageControllerTableModel<ParentType, ChildrenType> pctm) {
        super(pctm);
        tableModel = pctm;
        this.setShowVerticalLines(false);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        sortNotifyColor = CFG_GUI.SORT_COLUMN_HIGHLIGHT_ENABLED.getValue() ? (LAFOptions.getInstance().getColorForTableSortedColumnView()) : null;
        filterNotifyColor = CFG_GUI.CFG.isFilterHighlightEnabled() ? (LAFOptions.getInstance().getColorForTableFilteredView()) : null;
        initAppActions();

        selectionDelayedUpdate = new DelayedRunnable(500, 5000) {

            @Override
            public void delayedrun() {
                updateMoveActions();
            }
        };
        tableModel.addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                getSelectionInfo(false, false);
            }
        });
    }

    public SelectionInfo<ParentType, ChildrenType> getSelectionInfo() {
        return getSelectionInfo(true, true);
    }

    public SelectionInfo<ParentType, ChildrenType> getSelectionInfo(final boolean selectionOnly, final boolean filtered) {
        return new EDTHelper<SelectionInfo<ParentType, ChildrenType>>() {

            @Override
            public SelectionInfo<ParentType, ChildrenType> edtRun() {
                SelectionInfoCache cachedSelectionInfo = selectionInfoCache.get();
                final long tableVersion = tableModel.getTableDataVersion();
                if (cachedSelectionInfo == null || cachedSelectionInfo.getTableDataVersion() != tableVersion) {
                    cachedSelectionInfo = new SelectionInfoCache(tableVersion);
                    selectionInfoCache.set(cachedSelectionInfo);
                }
                if (selectionOnly == false) {
                    if (filtered == false) {
                        if (cachedSelectionInfo.all == null) {
                            cachedSelectionInfo.all = new SelectionInfo<ParentType, ChildrenType>(null, getModel().getController().getAllChildren(), null, null, null, PackageControllerTable.this);
                        }
                        return cachedSelectionInfo.all;
                    } else {
                        if (cachedSelectionInfo.all_filtered == null) {
                            cachedSelectionInfo.all_filtered = new SelectionInfo<ParentType, ChildrenType>(null, getModel().getElements(), null, null, null, PackageControllerTable.this);
                        }
                        return cachedSelectionInfo.all_filtered;
                    }
                } else {
                    long selection = selectionVersion.get();
                    if (cachedSelectionInfo.selectionVersion.getAndSet(selection) != selection) {
                        cachedSelectionInfo.selection = null;
                        cachedSelectionInfo.selection_filtered = null;
                    }
                    ListSelectionModel sm = getSelectionModel();
                    if (filtered == false) {
                        if (cachedSelectionInfo.selection == null) {
                            if (sm.isSelectionEmpty()) {
                                cachedSelectionInfo.selection = new SelectionInfo<ParentType, ChildrenType>(null, null, null, null, null, PackageControllerTable.this);
                            } else {
                                throw new WTFException("You really want an unfiltered filtered view?!");
                            }
                        }
                        return cachedSelectionInfo.selection;
                    } else {
                        if (cachedSelectionInfo.selection_filtered == null) {
                            if (sm.isSelectionEmpty()) {
                                cachedSelectionInfo.selection_filtered = new SelectionInfo<ParentType, ChildrenType>(null, null, null, null, null, PackageControllerTable.this);
                            } else {
                                cachedSelectionInfo.selection_filtered = new SelectionInfo<ParentType, ChildrenType>(getModel().getObjectbyRow(sm.getLeadSelectionIndex()), getModel().getSelectedObjects(), null, null, null, PackageControllerTable.this);
                            }
                        }
                        return cachedSelectionInfo.selection_filtered;
                    }
                }
            }
        }.getReturnValue();
    }

    @SuppressWarnings("unchecked")
    public PackageControllerTableModel<ParentType, ChildrenType> getModel() {
        return (PackageControllerTableModel<ParentType, ChildrenType>) super.getModel();
    }

    protected void doSortOnColumn(ExtColumn<AbstractNode> column, MouseEvent e) {
        if (CrossSystem.isMac()) {
            if (e.isMetaDown()) {

                new SortPackagesDownloadOrdnerOnColumn(column).actionPerformed(new ActionEvent(e.getSource(), e.getID(), "sort", System.currentTimeMillis(), e.getModifiers()));
                return;
            }
        } else {
            if (e.isControlDown()) {
                new SortPackagesDownloadOrdnerOnColumn(column).actionPerformed(new ActionEvent(e.getSource(), e.getID(), "sort", System.currentTimeMillis(), e.getModifiers()));
                return;
            }
        }
        column.doSort();
    }

    public void setModel(TableModel dataModel) {
        if (dataModel instanceof PackageControllerTableModel) {
            super.setModel(dataModel);
        } else {
            throw new WTFException("The Model is not instanceof PackageControllerTableModel!");
        }
    }

    public PackageController<ParentType, ChildrenType> getController() {
        return tableModel.getController();
    }

    @Override
    protected void onSelectionChanged() {
        selectionVersion.incrementAndGet();
        super.onSelectionChanged();
        if (tableModel.hasSelectedObjects() == false || !updateMoveButtonEnabledStatus()) {
            // disable move buttons
            moveDownAction.setEnabled(false);
            moveBottomAction.setEnabled(false);
            moveTopAction.setEnabled(false);
            moveUpAction.setEnabled(false);
        } else {
            selectionDelayedUpdate.run();
        }
    }

    protected void updateMoveActions() {
        final java.util.List<ParentType> selectedPkgs = new ArrayList<ParentType>();
        final java.util.List<ChildrenType> selectedChld = new ArrayList<ChildrenType>();
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                getSelected(selectedPkgs, selectedChld);
            }
        }.waitForEDT();
        final boolean[] movePossible = movePossible(selectedPkgs, selectedChld);
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                moveTopAction.setEnabled(movePossible[0]);
                moveUpAction.setEnabled(movePossible[0]);
                moveDownAction.setEnabled(movePossible[1]);
                moveBottomAction.setEnabled(movePossible[1]);
            }
        };
    }

    public void getSelected(List<ParentType> selectedPkgs, List<ChildrenType> selectedChld) {
        final java.util.List<ChildrenType> ret = new ArrayList<ChildrenType>();
        int iMin = selectionModel.getMinSelectionIndex();
        int iMax = selectionModel.getMaxSelectionIndex();
        if ((iMin == -1) || (iMax == -1)) { return; }
        List<AbstractNode> tableData = getModel().getTableData();
        for (int i = iMin; i <= iMax; i++) {
            if (selectionModel.isSelectedIndex(i)) {
                final AbstractNode node = tableData.get(i);
                if (node != null) {
                    if (node instanceof AbstractPackageNode<?, ?> && selectedPkgs != null) {
                        selectedPkgs.add((ParentType) node);
                    } else if (node instanceof AbstractPackageChildrenNode<?> && selectedChld != null) {
                        selectedChld.add((ChildrenType) node);
                    }
                }
            }
        }
    }

    protected boolean[] movePossible(List<ParentType> pkgs, List<ChildrenType> selectedChld) {
        boolean onlyFullPackagesSelected = true;
        HashSet<ChildrenType> allInPackages = new HashSet<ChildrenType>();
        for (ParentType pkg : pkgs) {
            boolean lock = false;
            try {
                lock = pkg.getModifyLock().readLock();
                if (pkg.isExpanded()) allInPackages.addAll(pkg.getChildren());
            } finally {
                pkg.getModifyLock().readUnlock(lock);
            }
        }

        for (ChildrenType ch : selectedChld) {
            if (!allInPackages.remove(ch)) {
                onlyFullPackagesSelected = false;
                break;
            }
        }
        onlyFullPackagesSelected &= allInPackages.size() == 0;
        if (pkgs.size() > 0 && selectedChld.size() > 0 && !onlyFullPackagesSelected) {
            /* we don't allow moving of packages/children at the same time */
            return new boolean[] { false, false };
        }
        ParentType sameParent = null;
        for (ChildrenType child : selectedChld) {
            if (sameParent == null) {
                sameParent = child.getParentNode();
            } else if (sameParent != child.getParentNode()) { return new boolean[] { false, false }; }
        }
        /* move up check */
        boolean moveUp = false;
        PackageController<ParentType, ChildrenType> pc = this.getController();
        int index = 0;
        for (ParentType parent : pkgs) {
            if (pc.indexOf(parent) != index++) {
                moveUp = true;
                break;
            }
        }
        if (sameParent != null) {
            boolean readL = sameParent.getModifyLock().readLock();
            try {
                index = 0;
                for (ChildrenType child : selectedChld) {
                    if (sameParent.indexOf(child) != index++) {
                        moveUp = true;
                        break;
                    }
                }
            } finally {
                sameParent.getModifyLock().readUnlock(readL);
            }
        }
        /* move down check */
        boolean moveDown = false;
        index = pc.size() - 1;
        for (int i = pkgs.size() - 1; i >= 0; i--) {
            ParentType parent = pkgs.get(i);
            if (pc.indexOf(parent) != index--) {
                moveDown = true;
                break;
            }
        }
        if (sameParent != null) {
            boolean readL = sameParent.getModifyLock().readLock();
            try {
                index = sameParent.getChildren().size() - 1;
                for (int i = selectedChld.size() - 1; i >= 0; i--) {
                    ChildrenType child = selectedChld.get(i);
                    if (sameParent.indexOf(child) != index--) {
                        moveDown = true;
                        break;
                    }
                }
            } finally {
                sameParent.getModifyLock().readUnlock(readL);
            }
        }
        return new boolean[] { moveUp, moveDown };
    }

    protected boolean updateMoveButtonEnabledStatus() {
        if (getModel().isFilteredView()) return false;
        return true;
    }

    protected boolean moveUpPossible(java.util.List<ParentType> pkgs, java.util.List<ChildrenType> selectedChld) {
        if (getModel().isFilteredView()) return false;
        // let's check if we have only full packages selected. that means, that selectedChld contains all links in the packages
        boolean onlyFullPackagesSelected = true;
        HashSet<ChildrenType> allInPackages = new HashSet<ChildrenType>();
        for (ParentType pkg : pkgs) {
            boolean lock = false;
            try {
                lock = pkg.getModifyLock().readLock();
                if (pkg.isExpanded()) allInPackages.addAll(pkg.getChildren());
            } finally {
                pkg.getModifyLock().readUnlock(lock);
            }
        }

        for (ChildrenType ch : selectedChld) {
            if (!allInPackages.remove(ch)) {
                onlyFullPackagesSelected = false;
                break;
            }
        }
        onlyFullPackagesSelected &= allInPackages.size() == 0;
        if (pkgs.size() > 0 && selectedChld.size() > 0 && !onlyFullPackagesSelected) {
            /* we don't allow moving of packages/children at the same time */
            return false;
        }
        ParentType sameParent = null;
        for (ChildrenType child : selectedChld) {
            if (sameParent == null) {
                sameParent = child.getParentNode();
            } else if (sameParent != child.getParentNode()) { return false; }
        }
        PackageController<ParentType, ChildrenType> pc = this.getController();
        int index = 0;
        for (ParentType parent : pkgs) {
            if (pc.indexOf(parent) != index++) return true;
        }
        if (sameParent != null) {
            boolean readL = sameParent.getModifyLock().readLock();
            try {
                index = 0;
                for (ChildrenType child : selectedChld) {
                    if (sameParent.indexOf(child) != index++) return true;
                }
            } finally {
                sameParent.getModifyLock().readUnlock(readL);
            }
        }
        return false;
    }

    protected boolean moveDownPossible(java.util.List<ParentType> pkgs, java.util.List<ChildrenType> selectedChld) {
        if (getModel().isFilteredView()) return false;
        boolean onlyFullPackagesSelected = true;
        HashSet<ChildrenType> allInPackages = new HashSet<ChildrenType>();
        for (ParentType pkg : pkgs) {
            boolean lock = false;
            try {
                lock = pkg.getModifyLock().readLock();
                if (pkg.isExpanded()) allInPackages.addAll(pkg.getChildren());
            } finally {
                pkg.getModifyLock().readUnlock(lock);
            }
        }

        for (ChildrenType ch : selectedChld) {
            if (!allInPackages.remove(ch)) {
                onlyFullPackagesSelected = false;
                break;
            }
        }
        onlyFullPackagesSelected &= allInPackages.size() == 0;
        if (pkgs.size() > 0 && selectedChld.size() > 0 && !onlyFullPackagesSelected) {
            /* we don't allow moving of packages/children at the same time */
            return false;
        }
        ParentType sameParent = null;
        for (ChildrenType child : selectedChld) {
            if (sameParent == null) {
                sameParent = child.getParentNode();
            } else if (sameParent != child.getParentNode()) { return false; }
        }
        PackageController<ParentType, ChildrenType> pc = this.getController();
        int index = pc.size() - 1;
        for (int i = pkgs.size() - 1; i >= 0; i--) {
            ParentType parent = pkgs.get(i);
            if (pc.indexOf(parent) != index--) return true;
        }
        if (sameParent != null) {
            boolean readL = sameParent.getModifyLock().readLock();
            try {
                index = sameParent.getChildren().size() - 1;
                for (int i = selectedChld.size() - 1; i >= 0; i--) {
                    ChildrenType child = selectedChld.get(i);
                    if (sameParent.indexOf(child) != index--) return true;
                }
            } finally {
                sameParent.getModifyLock().readUnlock(readL);
            }
        }
        return false;
    }

    protected void initAppActions() {
        moveTopAction = new AppAction() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            {
                // setName(_GUI._.BottomBar_BottomBar_totop());
                this.setTooltipText(_GUI._.BottomBar_BottomBar_totop_tooltip());
                setSmallIcon(NewTheme.I().getIcon("go-top", 20));
            }

            public void actionPerformed(ActionEvent e) {
                final java.util.List<ParentType> selectedPkgs = getSelectedPackages();
                final java.util.List<ChildrenType> selectedChld = getSelectedChildren();
                getController().getQueue().add(new QueueAction<Void, RuntimeException>() {

                    @Override
                    protected Void run() throws RuntimeException {
                        boolean moveUpPossible = moveUpPossible(selectedPkgs, selectedChld);
                        if (moveUpPossible == false) return null;
                        PackageController<ParentType, ChildrenType> pc = getController();
                        if (selectedPkgs.size() > 0) {
                            /* move package to top of list */
                            pc.move(selectedPkgs, null);
                        }
                        if (selectedChld.size() > 0) {
                            /* move children to top of package */
                            pc.move(selectedChld, selectedChld.get(0).getParentNode(), null);
                        }
                        return null;
                    }

                });
            }

        };
        moveUpAction = new AppAction() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            {
                // setName(_GUI._.BottomBar_BottomBar_moveup());
                this.setTooltipText(_GUI._.BottomBar_BottomBar_moveup_tooltip());
                setSmallIcon(NewTheme.I().getIcon("go-up", 20));
            }

            public void actionPerformed(ActionEvent e) {
                final java.util.List<ParentType> selectedPkgs = getSelectedPackages();
                final java.util.List<ChildrenType> selectedChld = getSelectedChildren();
                getController().getQueue().add(new QueueAction<Void, RuntimeException>() {

                    @Override
                    protected Void run() throws RuntimeException {
                        boolean moveUpPossible = moveUpPossible(selectedPkgs, selectedChld);
                        if (moveUpPossible == false) return null;
                        PackageController<ParentType, ChildrenType> pc = getController();
                        if (selectedPkgs.size() > 0) {
                            ParentType after = null;
                            boolean readL = pc.readLock();
                            try {
                                try {
                                    int index = pc.indexOf(selectedPkgs.get(0)) - 2;
                                    if (index >= 0) {
                                        /* move after this element */
                                        after = pc.getPackages().get(index);
                                    }/* else move to top */
                                } catch (final Throwable e) {
                                    Log.exception(e);
                                }
                            } finally {
                                pc.readUnlock(readL);
                            }
                            pc.move(selectedPkgs, after);
                        }
                        if (selectedChld.size() > 0) {
                            ChildrenType after = null;
                            ParentType pkg = selectedChld.get(0).getParentNode();
                            boolean readL = pkg.getModifyLock().readLock();
                            try {
                                int index = pkg.indexOf(selectedChld.get(0)) - 2;
                                if (index >= 0) {
                                    /* move after this element */
                                    after = pkg.getChildren().get(index);
                                }/* else move to top */
                            } catch (final Throwable e) {
                                Log.exception(e);
                            } finally {
                                pkg.getModifyLock().readUnlock(readL);
                            }
                            pc.move(selectedChld, pkg, after);
                        }
                        return null;
                    }
                });
            };
        };
        moveDownAction = new AppAction() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            {
                // setName(_GUI._.BottomBar_BottomBar_movedown());
                this.setTooltipText(_GUI._.BottomBar_BottomBar_movedown_tooltip());
                setSmallIcon(NewTheme.I().getIcon("go-down", 20));
            }

            public void actionPerformed(ActionEvent e) {
                final java.util.List<ParentType> selectedPkgs = getSelectedPackages();
                final java.util.List<ChildrenType> selectedChld = getSelectedChildren();
                getController().getQueue().add(new QueueAction<Void, RuntimeException>() {

                    @Override
                    protected Void run() throws RuntimeException {
                        boolean moveDownPossible = moveDownPossible(selectedPkgs, selectedChld);
                        if (moveDownPossible == false) return null;
                        PackageController<ParentType, ChildrenType> pc = getController();
                        if (selectedPkgs.size() > 0) {
                            ParentType after = null;
                            boolean readL = pc.readLock();
                            try {
                                try {
                                    int index = Math.min(pc.getPackages().size() - 1, pc.indexOf(selectedPkgs.get(selectedPkgs.size() - 1)) + 1);
                                    after = pc.getPackages().get(index);
                                } catch (final Throwable e) {
                                    Log.exception(e);
                                }
                            } finally {
                                pc.readUnlock(readL);
                            }
                            pc.move(selectedPkgs, after);
                        }
                        if (selectedChld.size() > 0) {
                            ChildrenType after = null;
                            ParentType pkg = selectedChld.get(0).getParentNode();
                            boolean readL = pkg.getModifyLock().readLock();
                            try {
                                /* move after after element or max at bottom */
                                int index = Math.min(pkg.getChildren().size() - 1, pkg.indexOf(selectedChld.get(selectedChld.size() - 1)) + 1);
                                after = pkg.getChildren().get(index);
                            } catch (final Throwable e) {
                                Log.exception(e);
                            } finally {
                                pkg.getModifyLock().readUnlock(readL);
                            }
                            pc.move(selectedChld, pkg, after);
                        }
                        return null;
                    }
                });
            }
        };
        moveBottomAction = new AppAction() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            {
                // setName(_GUI._.BottomBar_BottomBar_tobottom());
                this.setTooltipText(_GUI._.BottomBar_BottomBar_tobottom_tooltip());
                setSmallIcon(NewTheme.I().getIcon("go-bottom", 20));
            }

            public void actionPerformed(ActionEvent e) {
                final java.util.List<ParentType> selectedPkgs = getSelectedPackages();
                final java.util.List<ChildrenType> selectedChld = getSelectedChildren();
                getController().getQueue().add(new QueueAction<Void, RuntimeException>() {

                    @Override
                    protected Void run() throws RuntimeException {
                        boolean moveDownPossible = moveDownPossible(selectedPkgs, selectedChld);
                        if (moveDownPossible == false) return null;
                        PackageController<ParentType, ChildrenType> pc = getController();
                        if (selectedPkgs.size() > 0) {
                            ParentType after = null;
                            boolean readL = pc.readLock();
                            try {
                                try {
                                    after = pc.getPackages().get(pc.getPackages().size() - 1);
                                } catch (final Throwable e) {
                                    LogController.CL().log(e);
                                }
                            } finally {
                                pc.readUnlock(readL);
                            }
                            pc.move(selectedPkgs, after);
                        }
                        if (selectedChld.size() > 0) {
                            ChildrenType after = null;
                            ParentType pkg = selectedChld.get(0).getParentNode();
                            boolean readL = pkg.getModifyLock().readLock();
                            try {
                                after = pkg.getChildren().get(pkg.getChildren().size() - 1);
                            } catch (final Throwable e) {
                                LogController.CL().log(e);
                            } finally {
                                pkg.getModifyLock().readUnlock(readL);
                            }
                            pc.move(selectedChld, pkg, after);
                        }
                        return null;
                    }
                });

            }

        };
        moveDownAction.setEnabled(false);
        moveBottomAction.setEnabled(false);
        moveTopAction.setEnabled(false);
        moveUpAction.setEnabled(false);
    }

    @Override
    protected boolean processKeyBinding(KeyStroke stroke, KeyEvent evt, int condition, boolean pressed) {
        if (!pressed) { return super.processKeyBinding(stroke, evt, condition, pressed); }

        if (stroke.equals(KEY_STROKE_KP_LEFT) || stroke.equals(KEY_STROKE_LEFT)) {
            AbstractNode element = this.getModel().getElementAt(this.getSelectedRow());
            if (element != null && element instanceof AbstractPackageNode) {
                tableModel.setFilePackageExpand(false, (AbstractPackageNode<?, ?>) element);
                return true;
            }
        }
        if (stroke.equals(KEY_STROKE_KP_RIGHT) || stroke.equals(KEY_STROKE_RIGHT)) {
            AbstractNode element = this.getModel().getElementAt(this.getSelectedRow());
            if (element != null && element instanceof AbstractPackageNode) {
                tableModel.setFilePackageExpand(true, (AbstractPackageNode<?, ?>) element);
                return true;
            }
        }
        if (stroke.equals(KEY_STROKE_ALT_UP)) {
            this.moveUpAction.actionPerformed(null);
            return true;
        }
        if (stroke.equals(KEY_STROKE_ALT_DOWN)) {
            this.moveDownAction.actionPerformed(null);
            return true;
        }

        if (stroke.equals(KEY_STROKE_ALT_HOME)) {
            moveTopAction.actionPerformed(null);
            return true;
        }
        if (stroke.equals(KEY_STROKE_ALT_END)) {
            moveBottomAction.actionPerformed(null);
            return true;
        }

        return super.processKeyBinding(stroke, evt, condition, pressed);
    }

    public AppAction getMoveDownAction() {
        return moveDownAction;
    }

    public AppAction getMoveTopAction() {
        return moveTopAction;
    }

    public AppAction getMoveToBottomAction() {
        return moveBottomAction;
    }

    public AppAction getMoveUpAction() {
        return moveUpAction;
    }

    public abstract ExtColumn<AbstractNode> getExpandCollapseColumn();

    @Override
    protected boolean onSingleClick(MouseEvent e, final AbstractNode obj) {
        if (obj instanceof AbstractPackageNode) {
            final ExtColumn<AbstractNode> column = this.getExtColumnAtPoint(e.getPoint());

            if (column == getExpandCollapseColumn()) {
                Rectangle bounds = column.getBounds();
                if (e.getPoint().x - bounds.x < 30) {
                    if (e.isControlDown() && !e.isShiftDown()) {
                        tableModel.toggleFilePackageExpand((AbstractPackageNode<?, ?>) obj, TOGGLEMODE.BOTTOM);
                    } else if (e.isControlDown() && e.isShiftDown()) {
                        tableModel.toggleFilePackageExpand((AbstractPackageNode<?, ?>) obj, TOGGLEMODE.TOP);
                    } else {
                        tableModel.toggleFilePackageExpand((AbstractPackageNode<?, ?>) obj, TOGGLEMODE.CURRENT);
                    }
                    return true;
                }
            }
        }
        return super.onSingleClick(e, obj);
    }

    public boolean isOriginalOrder() {
        return getModel().getSortColumn() == null;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        boolean filteredView = filterNotifyColor != null && tableModel.isFilteredView();
        ExtColumn<AbstractNode> sortColumn = getModel().getSortColumn();
        int filteredColumn = -1;
        if (sortNotifyColor != null && sortColumn != null) {
            filteredColumn = sortColumn.getIndex();
        }
        if (filteredView == false && filteredColumn < 0) return;
        Graphics2D g2 = (Graphics2D) g;
        Composite comp = g2.getComposite();
        final Rectangle visibleRect = this.getVisibleRect();

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
        if (filteredView) {
            g2.setColor(filterNotifyColor);
            g2.fillRect(visibleRect.x, visibleRect.y, visibleRect.x + visibleRect.width, visibleRect.y + visibleRect.height);
        }
        if (filteredColumn >= 0 && tableModel.isTristateSorterEnabled()) {
            Rectangle first = this.getCellRect(0, filteredColumn, true);

            int w = getModel().getSortColumn().getWidth() - Math.max(0, visibleRect.x - first.x);
            if (w > 0) {
                g2.setColor(sortNotifyColor);
                g2.fillRect(Math.max(first.x, visibleRect.x), visibleRect.y, w, visibleRect.height);
            }
        }
        g2.setComposite(comp);
    }

    @SuppressWarnings("unchecked")
    public java.util.List<ParentType> getSelectedPackages() {
        final java.util.List<ParentType> ret = new ArrayList<ParentType>();
        int iMin = selectionModel.getMinSelectionIndex();
        int iMax = selectionModel.getMaxSelectionIndex();
        if ((iMin == -1) || (iMax == -1)) { return ret; }
        List<AbstractNode> tableData = getModel().getTableData();
        for (int i = iMin; i <= iMax; i++) {
            if (selectionModel.isSelectedIndex(i)) {
                final AbstractNode node = tableData.get(i);
                if (node != null && node instanceof AbstractPackageNode<?, ?>) {
                    ret.add((ParentType) node);
                }
            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public java.util.List<ChildrenType> getSelectedChildren() {
        final java.util.List<ChildrenType> ret = new ArrayList<ChildrenType>();
        int iMin = selectionModel.getMinSelectionIndex();
        int iMax = selectionModel.getMaxSelectionIndex();
        if ((iMin == -1) || (iMax == -1)) { return ret; }
        List<AbstractNode> tableData = getModel().getTableData();
        for (int i = iMin; i <= iMax; i++) {
            if (selectionModel.isSelectedIndex(i)) {
                final AbstractNode node = tableData.get(i);
                if (node != null && node instanceof AbstractPackageChildrenNode<?>) {
                    ret.add((ChildrenType) node);
                }
            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public java.util.List<ChildrenType> getAllSelectedChildren(java.util.List<AbstractNode> selectedObjects) {
        LinkedHashSet<ChildrenType> list = new LinkedHashSet<ChildrenType>();
        for (final AbstractNode node : selectedObjects) {
            if (node instanceof AbstractPackageChildrenNode<?>) {
                list.add((ChildrenType) node);
            } else {
                boolean readL = ((AbstractPackageNode) node).getModifyLock().readLock();
                try {
                    for (final ChildrenType dl : ((ParentType) node).getChildren()) {
                        list.add(dl);
                    }
                } finally {
                    ((AbstractPackageNode) node).getModifyLock().readUnlock(readL);
                }
            }
        }
        return new ArrayList<ChildrenType>(list);
    }

}
