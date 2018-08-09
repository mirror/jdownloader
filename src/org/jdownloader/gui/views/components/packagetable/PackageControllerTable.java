package org.jdownloader.gui.views.components.packagetable;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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
import org.appwork.swing.exttable.ExtOverlayRowHighlighter;
import org.appwork.swing.exttable.ExtTable;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel.TOGGLEMODE;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelSelectionOnlySelectionInfo.SelectionOnlyPackageView;
import org.jdownloader.gui.views.components.packagetable.actions.SortPackagesDownloadOrdnerOnColumn;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

public abstract class PackageControllerTable<ParentType extends AbstractPackageNode<ChildrenType, ParentType>, ChildrenType extends AbstractPackageChildrenNode<ParentType>> extends BasicJDTable<AbstractNode> {
    protected class SelectionInfoCache {
        private final SelectionInfo<ParentType, ChildrenType> selectionInfo;
        private final long                                    selectionVersion;
        private final long                                    dataVersion;

        public final long getSelectionVersion() {
            return selectionVersion;
        }

        public final long getDataVersion() {
            return dataVersion;
        }

        public final SelectionInfo<ParentType, ChildrenType> getSelectionInfo() {
            return selectionInfo;
        }

        protected SelectionInfoCache(long selectionVersion, long dataVersion, SelectionInfo<ParentType, ChildrenType> selectionInfo) {
            this.selectionVersion = selectionVersion;
            this.dataVersion = dataVersion;
            this.selectionInfo = selectionInfo;
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
    private final AtomicLong                                      selectionVersion    = new AtomicLong(0);
    private final DelayedRunnable                                 selectionDelayedUpdate;
    private final boolean                                         wrapAroundEnabled;

    public ExtColumn<AbstractNode> getMouseOverColumn() {
        Point mp = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(mp, this);
        return getExtColumnAtPoint(mp);
    }

    @Override
    protected boolean isWrapAroundEnabled() {
        return wrapAroundEnabled;
    }

    public boolean isColumnLockingFeatureEnabled() {
        return getAutoResizeMode() != JTable.AUTO_RESIZE_OFF;
    }

    @Override
    public void setAutoResizeMode(int mode) {
        super.setAutoResizeMode(mode);
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

    public PackageControllerTable(final PackageControllerTableModel<ParentType, ChildrenType> pctm) {
        super(pctm);
        tableModel = pctm;
        this.setShowVerticalLines(false);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        sortNotifyColor = CFG_GUI.SORT_COLUMN_HIGHLIGHT_ENABLED.isEnabled() ? (LAFOptions.getInstance().getColorForTableSortedColumnView()) : null;
        filterNotifyColor = CFG_GUI.CFG.isFilterHighlightEnabled() ? (LAFOptions.getInstance().getColorForTableFilteredView()) : null;
        wrapAroundEnabled = CFG_GUI.CFG.isTableWrapAroundEnabled();
        initAppActions();
        selectionDelayedUpdate = new DelayedRunnable(500, 5000) {
            @Override
            public void delayedrun() {
                updateMoveActions();
            }
        };
        this.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                if (e == null || e.getValueIsAdjusting() || tableModel.isTableSelectionClearing()) {
                    return;
                }
                selectionVersion.incrementAndGet();
                final AbstractNode node = getModel().getObjectbyRow(getSelectionModel().getLeadSelectionIndex());
                final WeakReference<AbstractNode> lContextMenuTrigger = contextMenuTrigger;
                if (lContextMenuTrigger == null || lContextMenuTrigger.get() != node) {
                    contextMenuTrigger = new WeakReference<AbstractNode>(node);
                }
            }
        });
        final int horizontalLineWeight = LAFOptions.getInstance().getCfg().getLinkTableHorizontalRowLineWeight();
        if (horizontalLineWeight > 0) {
            this.setRowMargin(horizontalLineWeight);
            final Color color = LAFOptions.getInstance().getColorForTableRowGap();
            if (color != null) {
                addRowHighlighter(new ExtOverlayRowHighlighter(null, null) {
                    private BasicStroke stroke;
                    {
                        stroke = new BasicStroke(horizontalLineWeight);
                    }

                    @Override
                    public void paint(Graphics2D g, int x, int y, int width, int height) {
                        g.setColor(color);
                        g.setStroke(stroke);
                        g.drawLine(x, y + height, x + width, y + height);
                    }

                    @Override
                    public boolean doHighlight(ExtTable<?> extTable, int row) {
                        return true;
                    }
                });
            }
        }
    }

    public SelectionInfo<ParentType, ChildrenType> getSelectionInfo() {
        return getSelectionInfo(true, true);
    }

    protected volatile WeakReference<AbstractNode> contextMenuTrigger = new WeakReference<AbstractNode>(null);

    protected boolean isExpandToggleEvent(final MouseEvent e) {
        final ExtColumn<AbstractNode> column = this.getExtColumnAtPoint(e.getPoint());
        if (column == getExpandCollapseColumn()) {
            final Rectangle bounds = column.getBounds();
            if (e.getPoint().x - bounds.x < FileColumn.EXPAND_COLLAPSE_AREA) {
                final int row = this.rowAtPoint(e.getPoint());
                final AbstractNode node = this.getModel().getObjectbyRow(row);
                if (node instanceof AbstractPackageNode) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void processMouseEvent(final MouseEvent e) {
        final boolean isPressed = e.getID() == MouseEvent.MOUSE_PRESSED;
        if (isPressed) {
            final int row = this.rowAtPoint(e.getPoint());
            final AbstractNode node = this.getModel().getObjectbyRow(row);
            final WeakReference<AbstractNode> lContextMenuTrigger = contextMenuTrigger;
            if (lContextMenuTrigger == null || lContextMenuTrigger.get() != node) {
                contextMenuTrigger = new WeakReference<AbstractNode>(node);
            }
        }
        if ((isPressed || e.getID() == MouseEvent.MOUSE_RELEASED) && isExpandToggleEvent(e)) {
            /* avoid selection for expand/collapse packages */
            return;
        }
        super.processMouseEvent(e);
    }

    protected AbstractNode getContextMenuTrigger() {
        final WeakReference<AbstractNode> lContextMenuTrigger = contextMenuTrigger;
        if (lContextMenuTrigger != null) {
            return lContextMenuTrigger.get();
        }
        return null;
    }

    /** access within EDT only **/
    protected volatile SelectionInfoCache selectionOnly_TableData      = null;
    /** access within EDT only **/
    protected volatile SelectionInfoCache selectionOnly_ControllerData = null;
    /** access within EDT only **/
    protected volatile SelectionInfoCache all_TableData                = null;

    public SelectionInfo<ParentType, ChildrenType> getSelectionInfo(final boolean selectionOnly, final boolean useTableModelData) {
        if (selectionOnly) {
            return new EDTHelper<SelectionInfo<ParentType, ChildrenType>>() {
                @Override
                public SelectionInfo<ParentType, ChildrenType> edtRun() {
                    final long currentSelectionVersion = selectionVersion.get();
                    if (useTableModelData) {
                        SelectionInfoCache lselectionOnly_TableData = selectionOnly_TableData;
                        final long dataVersion = tableModel.getTableDataVersion();
                        final AbstractNode contextMenuTrigger = getContextMenuTrigger();
                        if (lselectionOnly_TableData != null && lselectionOnly_TableData.getSelectionVersion() == currentSelectionVersion && lselectionOnly_TableData.getDataVersion() == dataVersion) {
                            SelectionInfo<ParentType, ChildrenType> ret = lselectionOnly_TableData.getSelectionInfo();
                            if (ret.getRawContext() == contextMenuTrigger) {
                                return ret;
                            }
                            if (ret instanceof PackageControllerTableModelSelectionOnlySelectionInfo) {
                                ret = new PackageControllerTableModelSelectionOnlySelectionInfo(contextMenuTrigger, (PackageControllerTableModelSelectionOnlySelectionInfo<?, ?>) ret);
                                selectionOnly_TableData = new SelectionInfoCache(currentSelectionVersion, dataVersion, ret);
                                return ret;
                            }
                        }
                        final SelectionInfo<ParentType, ChildrenType> selectionInfo;
                        if (getSelectionModel().isSelectionEmpty()) {
                            selectionInfo = new EmptySelectionInfo<ParentType, ChildrenType>(getController());
                        } else {
                            selectionInfo = new PackageControllerTableModelSelectionOnlySelectionInfo<ParentType, ChildrenType>(contextMenuTrigger, getModel());
                        }
                        lselectionOnly_TableData = new SelectionInfoCache(currentSelectionVersion, dataVersion, selectionInfo);
                        selectionOnly_TableData = lselectionOnly_TableData;
                        return selectionInfo;
                    } else {
                        SelectionInfoCache lselectionOnly_ControllerData = selectionOnly_ControllerData;
                        if (lselectionOnly_ControllerData != null && lselectionOnly_ControllerData.getSelectionVersion() == currentSelectionVersion) {
                            return lselectionOnly_ControllerData.getSelectionInfo();
                        }
                        if (getSelectionModel().isSelectionEmpty()) {
                            final SelectionInfo<ParentType, ChildrenType> selectionInfo = new EmptySelectionInfo<ParentType, ChildrenType>(getController());
                            lselectionOnly_ControllerData = new SelectionInfoCache(currentSelectionVersion, -1, selectionInfo);
                            selectionOnly_ControllerData = lselectionOnly_ControllerData;
                            return selectionInfo;
                        } else {
                            throw new WTFException("You really want an unfiltered filtered view?!");
                        }
                    }
                }
            }.getReturnValue();
        } else {
            if (useTableModelData) {
                return new EDTHelper<SelectionInfo<ParentType, ChildrenType>>() {
                    @Override
                    public SelectionInfo<ParentType, ChildrenType> edtRun() {
                        final long dataVersion = tableModel.getTableDataVersion();
                        SelectionInfoCache lall_TableData = all_TableData;
                        if (lall_TableData != null && lall_TableData.getDataVersion() == dataVersion) {
                            return lall_TableData.getSelectionInfo();
                        }
                        final SelectionInfo<ParentType, ChildrenType> selectionInfo = new PackageControllerTableModelSelectionInfo<ParentType, ChildrenType>(null, getModel());
                        lall_TableData = new SelectionInfoCache(-1, dataVersion, selectionInfo);
                        all_TableData = lall_TableData;
                        return selectionInfo;
                    }
                }.getReturnValue();
            } else {
                return getModel().getController().getSelectionInfo();
            }
        }
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
        final SelectionInfo<ParentType, ChildrenType> selectionInfo = getSelectionInfo(true, true);
        final boolean moveUpPossible = moveUpPossible(selectionInfo);
        final boolean moveDownPossible = moveDownPossible(selectionInfo);
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                moveTopAction.setEnabled(moveUpPossible);
                moveUpAction.setEnabled(moveUpPossible);
                moveDownAction.setEnabled(moveDownPossible);
                moveBottomAction.setEnabled(moveDownPossible);
            }
        };
    }

    @Override
    public void onShortcutSelectAll() {
        if (!CFG_GUI.CFG.isTwoStepCtrlASelectionEnabled()) {
            super.onShortcutSelectAll();
        } else {
            final ArrayList<AbstractNode> toSelect = new ArrayList<AbstractNode>();
            final SelectionInfo<ParentType, ChildrenType> selection = getSelectionInfo(true, true);
            final PackageControllerTableModelData<ParentType, ChildrenType> tableData = tableModel.getTableData();
            for (final PackageView<ParentType, ChildrenType> packageView : selection.getPackageViews()) {
                List<AbstractNode> visibleChildren = null;
                if (packageView.isExpanded()) {
                    visibleChildren = ((SelectionOnlyPackageView) packageView).getVisibleChildren();
                }
                if (!tableData.isHideSingleChildPackages() || (visibleChildren != null && (visibleChildren.size() > 1 || (visibleChildren.size() == 1 && !tableData.isHiddenPackageSingleChildIndex(tableData.getRowforObject(packageView.getPackage(), tableModel.getController())))))) {
                    toSelect.add(packageView.getPackage());
                }
                if (visibleChildren != null) {
                    toSelect.addAll(visibleChildren);
                }
            }
            final boolean selectall = selection.getRawSelection().size() == toSelect.size();
            if (selectall) {
                super.onShortcutSelectAll();
            } else {
                getModel().setSelectedObjects(toSelect);
            }
        }
    }

    protected boolean updateMoveButtonEnabledStatus() {
        return true;
    }

    protected boolean moveUpPossible(SelectionInfo<ParentType, ChildrenType> selectionInfo) {
        if (selectionInfo.isEmpty()) {
            return false;
        } else {
            final PackageControllerTableModelData<ParentType, ChildrenType> tableData = tableModel.getTableData();
            if (selectionInfo.getPackageViews().size() > 1) {
                int index = 0;
                boolean ret = false;
                for (final PackageView<ParentType, ChildrenType> packageView : selectionInfo.getPackageViews()) {
                    if (packageView.getSelectedChildren().size() > 0 && packageView.isExpanded()) {
                        return false;
                    }
                    final int pcIndex = tableData.indexOf(packageView.getPackage());
                    if (pcIndex < 0) {
                        return false;
                    } else if (pcIndex != index++) {
                        ret = true;
                    }
                }
                return ret;
            } else if (selectionInfo.getPackageViews().size() == 1) {
                final PackageView<ParentType, ChildrenType> packageView = selectionInfo.getPackageViews().get(0);
                final List<ChildrenType> children = packageView.getSelectedChildren();
                if (!packageView.isExpanded() || children.size() == 0) {
                    return tableData.indexOf(packageView.getPackage()) != 0;
                } else {
                    final ParentType pkg = packageView.getPackage();
                    final boolean readL = pkg.getModifyLock().readLock();
                    try {
                        final List<ChildrenType> children2;
                        if (packageView instanceof SelectionOnlyPackageView) {
                            children2 = ((SelectionOnlyPackageView) packageView).getVisibleChildren();
                        } else {
                            children2 = pkg.getChildren();
                        }
                        int index = 0;
                        for (final ChildrenType child : children) {
                            final int cIndex = children2.indexOf(child);
                            if (cIndex < 0) {
                                return false;
                            } else if (cIndex != index++) {
                                return true;
                            }
                        }
                    } finally {
                        pkg.getModifyLock().readUnlock(readL);
                    }
                }
            }
            return false;
        }
    }

    protected boolean moveDownPossible(SelectionInfo<ParentType, ChildrenType> selectionInfo) {
        if (selectionInfo.isEmpty()) {
            return false;
        } else {
            final PackageControllerTableModelData<ParentType, ChildrenType> tableData = tableModel.getTableData();
            if (selectionInfo.getPackageViews().size() > 1) {
                boolean ret = false;
                int index = tableData.getModelDataPackages().size() - 1;
                for (int i = selectionInfo.getPackageViews().size() - 1; i >= 0; i--) {
                    final PackageView<ParentType, ChildrenType> packageView = selectionInfo.getPackageViews().get(i);
                    if (packageView.getSelectedChildren().size() > 0 && packageView.isExpanded()) {
                        return false;
                    }
                    final int pcIndex = tableData.lastIndexOf(packageView.getPackage());
                    if (pcIndex < 0) {
                        return false;
                    } else if (pcIndex != index--) {
                        ret = true;
                    }
                }
                return ret;
            } else if (selectionInfo.getPackageViews().size() == 1) {
                final PackageView<ParentType, ChildrenType> packageView = selectionInfo.getPackageViews().get(0);
                final List<ChildrenType> children = packageView.getSelectedChildren();
                if (!packageView.isExpanded() || children.size() == 0) {
                    return tableData.lastIndexOf(packageView.getPackage()) != tableData.getModelDataPackages().size() - 1;
                } else {
                    final ParentType pkg = packageView.getPackage();
                    final boolean readL = pkg.getModifyLock().readLock();
                    try {
                        final List<ChildrenType> children2;
                        if (packageView instanceof SelectionOnlyPackageView) {
                            children2 = ((SelectionOnlyPackageView) packageView).getVisibleChildren();
                        } else {
                            children2 = pkg.getChildren();
                        }
                        int index = children2.size() - 1;
                        for (int i = children.size() - 1; i >= 0; i--) {
                            final ChildrenType child = children.get(i);
                            final int cIndex = children2.lastIndexOf(child);
                            if (cIndex < 0) {
                                return false;
                            } else if (cIndex != index--) {
                                return true;
                            }
                        }
                    } finally {
                        pkg.getModifyLock().readUnlock(readL);
                    }
                }
            }
            return false;
        }
    }

    protected void initAppActions() {
        moveTopAction = new AppAction() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;
            {
                // setName(_GUI.T.BottomBar_BottomBar_totop());
                this.setTooltipText(_GUI.T.BottomBar_BottomBar_totop_tooltip());
                setSmallIcon(new AbstractIcon(IconKey.ICON_GO_TOP, 20));
            }

            public void actionPerformed(ActionEvent e) {
                final SelectionInfo<ParentType, ChildrenType> selectionInfo = getSelectionInfo(true, true);
                getController().getQueue().add(new QueueAction<Void, RuntimeException>() {
                    @Override
                    protected Void run() throws RuntimeException {
                        final boolean moveUpPossible = moveUpPossible(selectionInfo);
                        if (moveUpPossible) {
                            final PackageController<ParentType, ChildrenType> pc = getController();
                            final ArrayList<ParentType> selectedPackages = new ArrayList<ParentType>();
                            for (final PackageView<ParentType, ChildrenType> packageView : selectionInfo.getPackageViews()) {
                                if (packageView.isPackageSelected()) {
                                    selectedPackages.add(packageView.getPackage());
                                }
                            }
                            if (selectedPackages.size() > 0) {
                                /* move package to top of list */
                                pc.move(selectedPackages, null);
                            } else if (selectionInfo.getPackageViews().size() > 0) {
                                final PackageView<ParentType, ChildrenType> packageView = selectionInfo.getPackageViews().get(0);
                                /* move children to top of package */
                                pc.move(selectionInfo.getChildren(), packageView.getPackage(), null);
                            }
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
                // setName(_GUI.T.BottomBar_BottomBar_moveup());
                this.setTooltipText(_GUI.T.BottomBar_BottomBar_moveup_tooltip());
                setSmallIcon(new AbstractIcon(IconKey.ICON_GO_UP, 20));
            }

            public void actionPerformed(ActionEvent e) {
                final SelectionInfo<ParentType, ChildrenType> selectionInfo = getSelectionInfo(true, true);
                getController().getQueue().add(new QueueAction<Void, RuntimeException>() {
                    @Override
                    protected Void run() throws RuntimeException {
                        final boolean moveUpPossible = moveUpPossible(selectionInfo);
                        if (moveUpPossible) {
                            final PackageController<ParentType, ChildrenType> pc = getController();
                            final ArrayList<ParentType> selectedPackages = new ArrayList<ParentType>();
                            for (final PackageView<ParentType, ChildrenType> packageView : selectionInfo.getPackageViews()) {
                                if (packageView.isPackageSelected()) {
                                    selectedPackages.add(packageView.getPackage());
                                }
                            }
                            if (selectedPackages.size() > 0) {
                                ParentType after = null;
                                final boolean readL = pc.readLock();
                                try {
                                    try {
                                        final ParentType pkg = selectedPackages.get(0);
                                        final int index = pc.indexOf(pkg) - 2;
                                        if (index >= 0) {
                                            /* move after this element */
                                            after = pc.getPackages().get(index);
                                        } /* else move to top */
                                    } catch (final Throwable e) {
                                        LogController.CL().log(e);
                                    }
                                } finally {
                                    pc.readUnlock(readL);
                                }
                                pc.move(selectedPackages, after);
                            } else if (selectionInfo.getPackageViews().size() > 0) {
                                final PackageView<ParentType, ChildrenType> packageView = selectionInfo.getPackageViews().get(0);
                                ChildrenType after = null;
                                final ParentType pkg = packageView.getPackage();
                                final boolean readL = pkg.getModifyLock().readLock();
                                try {
                                    final ChildrenType child = packageView.getChildren().get(0);
                                    final int index = pkg.indexOf(child) - 2;
                                    if (index >= 0) {
                                        /* move after this element */
                                        after = pkg.getChildren().get(index);
                                    } /* else move to top */
                                } catch (final Throwable e) {
                                    LogController.CL().log(e);
                                } finally {
                                    pkg.getModifyLock().readUnlock(readL);
                                }
                                pc.move(selectionInfo.getChildren(), pkg, after);
                            }
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
                // setName(_GUI.T.BottomBar_BottomBar_movedown());
                this.setTooltipText(_GUI.T.BottomBar_BottomBar_movedown_tooltip());
                setSmallIcon(new AbstractIcon(IconKey.ICON_GO_DOWN, 20));
            }

            public void actionPerformed(ActionEvent e) {
                final SelectionInfo<ParentType, ChildrenType> selectionInfo = getSelectionInfo(true, true);
                getController().getQueue().add(new QueueAction<Void, RuntimeException>() {
                    @Override
                    protected Void run() throws RuntimeException {
                        final boolean moveDownPossible = moveDownPossible(selectionInfo);
                        if (moveDownPossible) {
                            final PackageController<ParentType, ChildrenType> pc = getController();
                            final ArrayList<ParentType> selectedPackages = new ArrayList<ParentType>();
                            for (final PackageView<ParentType, ChildrenType> packageView : selectionInfo.getPackageViews()) {
                                if (packageView.isPackageSelected()) {
                                    selectedPackages.add(packageView.getPackage());
                                }
                            }
                            if (selectedPackages.size() > 0) {
                                ParentType after = null;
                                final boolean readL = pc.readLock();
                                try {
                                    try {
                                        final ParentType pkg = selectedPackages.get(selectedPackages.size() - 1);
                                        final int index = Math.min(pc.getPackages().size() - 1, pc.indexOf(pkg) + 1);
                                        after = pc.getPackages().get(index);
                                    } catch (final Throwable e) {
                                        LogController.CL().log(e);
                                    }
                                } finally {
                                    pc.readUnlock(readL);
                                }
                                pc.move(selectedPackages, after);
                            } else if (selectionInfo.getPackageViews().size() > 0) {
                                final PackageView<ParentType, ChildrenType> packageView = selectionInfo.getPackageViews().get(0);
                                ChildrenType after = null;
                                final ParentType pkg = packageView.getPackage();
                                final boolean readL = pkg.getModifyLock().readLock();
                                try {
                                    /* move after after element or max at bottom */
                                    final ChildrenType child = selectionInfo.getChildren().get(selectionInfo.getChildren().size() - 1);
                                    final int index = Math.min(pkg.getChildren().size() - 1, pkg.indexOf(child) + 1);
                                    after = pkg.getChildren().get(index);
                                } catch (final Throwable e) {
                                    LogController.CL().log(e);
                                } finally {
                                    pkg.getModifyLock().readUnlock(readL);
                                }
                                pc.move(packageView.getChildren(), pkg, after);
                            }
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
                // setName(_GUI.T.BottomBar_BottomBar_tobottom());
                this.setTooltipText(_GUI.T.BottomBar_BottomBar_tobottom_tooltip());
                setSmallIcon(new AbstractIcon(IconKey.ICON_GO_BOTTOM, 20));
            }

            public void actionPerformed(ActionEvent e) {
                final SelectionInfo<ParentType, ChildrenType> selectionInfo = getSelectionInfo(true, true);
                getController().getQueue().add(new QueueAction<Void, RuntimeException>() {
                    @Override
                    protected Void run() throws RuntimeException {
                        if (moveDownPossible(selectionInfo)) {
                            final PackageController<ParentType, ChildrenType> pc = getController();
                            final ArrayList<ParentType> selectedPackages = new ArrayList<ParentType>();
                            for (final PackageView<ParentType, ChildrenType> packageView : selectionInfo.getPackageViews()) {
                                if (packageView.isPackageSelected()) {
                                    selectedPackages.add(packageView.getPackage());
                                }
                            }
                            if (selectedPackages.size() > 0) {
                                ParentType after = null;
                                final boolean readL = pc.readLock();
                                try {
                                    try {
                                        after = pc.getPackages().get(pc.getPackages().size() - 1);
                                    } catch (final Throwable e) {
                                        LogController.CL().log(e);
                                    }
                                } finally {
                                    pc.readUnlock(readL);
                                }
                                pc.move(selectedPackages, after);
                            } else if (selectionInfo.getPackageViews().size() > 0) {
                                final PackageView<ParentType, ChildrenType> packageView = selectionInfo.getPackageViews().get(0);
                                ChildrenType after = null;
                                final ParentType pkg = packageView.getPackage();
                                final boolean readL = pkg.getModifyLock().readLock();
                                try {
                                    after = pkg.getChildren().get(pkg.getChildren().size() - 1);
                                } catch (final Throwable e) {
                                    LogController.CL().log(e);
                                } finally {
                                    pkg.getModifyLock().readUnlock(readL);
                                }
                                pc.move(packageView.getChildren(), pkg, after);
                            }
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
        if (!pressed) {
            return super.processKeyBinding(stroke, evt, condition, pressed);
        }
        if (stroke.equals(KEY_STROKE_KP_LEFT) || stroke.equals(KEY_STROKE_LEFT)) {
            AbstractNode element = this.getModel().getElementAt(this.getSelectedRow());
            if (element != null) {
                if (element instanceof AbstractPackageNode) {
                    // collapse package
                    tableModel.setFilePackageExpand(false, (AbstractPackageNode<?, ?>) element);
                    return true;
                } else {
                    // select package containing selected file
                    for (int i = this.getSelectedRow() - 1; i >= 0; i--) {
                        if (this.getModel().getElementAt(i) instanceof AbstractPackageNode) {
                            this.changeSelection(i, 0, false, false);
                            return true;
                        }
                    }
                }
            }
        }
        if (stroke.equals(KEY_STROKE_KP_RIGHT) || stroke.equals(KEY_STROKE_RIGHT)) {
            AbstractNode element = this.getModel().getElementAt(this.getSelectedRow());
            if (element != null && element instanceof AbstractPackageNode) {
                // expand package
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
        if (obj instanceof AbstractPackageNode && isExpandToggleEvent(e)) {
            if (e.isControlDown() && !e.isShiftDown()) {
                tableModel.toggleFilePackageExpand((AbstractPackageNode<?, ?>) obj, TOGGLEMODE.BOTTOM);
            } else if (e.isControlDown() && e.isShiftDown()) {
                tableModel.toggleFilePackageExpand((AbstractPackageNode<?, ?>) obj, TOGGLEMODE.TOP);
            } else {
                tableModel.toggleFilePackageExpand((AbstractPackageNode<?, ?>) obj, TOGGLEMODE.CURRENT);
            }
            return true;
        }
        return super.onSingleClick(e, obj);
    }

    public boolean isOriginalOrder() {
        return getModel().getSortColumn() == null;
    }

    private final AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f);

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        boolean filteredView = filterNotifyColor != null && tableModel.isFilteredView();
        ExtColumn<AbstractNode> sortColumn = getModel().getSortColumn();
        int filteredColumn = -1;
        if (sortNotifyColor != null && sortColumn != null) {
            filteredColumn = sortColumn.getIndex();
        }
        if (filteredView == false && filteredColumn < 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        Composite comp = g2.getComposite();
        final Rectangle visibleRect = this.getVisibleRect();
        g2.setComposite(alpha);
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
}
