package org.jdownloader.gui.views.components.packagetable;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;
import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.exceptions.WTFException;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtComponentRowHighlighter;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel.TOGGLEMODE;
import org.jdownloader.gui.views.components.packagetable.actions.SortPackagesDownloadOrdnerOnColumn;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

public abstract class PackageControllerTable<ParentType extends AbstractPackageNode<ChildrenType, ParentType>, ChildrenType extends AbstractPackageChildrenNode<ParentType>> extends BasicJDTable<AbstractNode> {

    private static final KeyStroke                                KEY_STROKE_ALT_END  = KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.ALT_MASK);
    private static final KeyStroke                                KEY_STROKE_ALT_HOME = KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.ALT_MASK);
    private static final KeyStroke                                KEY_STROKE_ALT_DOWN = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_MASK);
    private static final KeyStroke                                KEY_STROKE_ALT_UP   = KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_MASK);
    private static final KeyStroke                                KEY_STROKE_RIGHT    = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
    private static final KeyStroke                                KEY_STROKE_KP_RIGHT = KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, 0);
    private static final KeyStroke                                KEY_STROKE_LEFT     = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0);
    private static final KeyStroke                                KEY_STROKE_KP_LEFT  = KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0);
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

    public PackageControllerTable(PackageControllerTableModel<ParentType, ChildrenType> pctm) {
        super(pctm);
        tableModel = pctm;
        this.setShowVerticalLines(false);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        sortNotifyColor = CFG_GUI.SORT_COLUMN_HIGHLIGHT_ENABLED.getValue() ? (LAFOptions.getInstance().getColorForTableSortedColumnView()) : null;
        filterNotifyColor = CFG_GUI.CFG.isFilterHighlightEnabled() ? (LAFOptions.getInstance().getColorForTableFilteredView()) : null;
        initAppActions();
        if (CFG_GUI.PACKAGES_BACKGROUND_HIGHLIGHT_ENABLED.isEnabled()) {
            Color tableFG = (LAFOptions.getInstance().getColorForTablePackageRowForeground());
            Color tableBG = (LAFOptions.getInstance().getColorForTablePackageRowBackground());
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
        if (tableModel.countSelectedObjects() == 0 || !updateMoveButtonEnabledStatus()) {
            // disable move buttons
            moveDownAction.setEnabled(false);
            moveBottomAction.setEnabled(false);
            moveTopAction.setEnabled(false);
            moveUpAction.setEnabled(false);
        } else {
            java.util.List<ParentType> selectedPkgs = getSelectedPackages();
            java.util.List<ChildrenType> selectedChld = getSelectedChildren();
            boolean moveUpPossible = moveUpPossible(selectedPkgs, selectedChld);
            boolean moveDownPossibe = moveDownPossible(selectedPkgs, selectedChld);
            moveTopAction.setEnabled(moveUpPossible);
            moveUpAction.setEnabled(moveUpPossible);
            moveDownAction.setEnabled(moveDownPossibe);
            moveBottomAction.setEnabled(moveDownPossibe);
        }
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
                allInPackages.addAll(pkg.getChildren());
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
            index = 0;
            for (ChildrenType child : selectedChld) {
                if (sameParent.indexOf(child) != index++) return true;
            }
        }
        return false;
    }

    protected boolean moveDownPossible(java.util.List<ParentType> pkgs, java.util.List<ChildrenType> selectedChld) {
        if (getModel().isFilteredView()) return false;
        boolean onlyFullPackagesSelected = true;
        HashSet<ChildrenType> allInPackages = new HashSet<ChildrenType>();
        for (ParentType pkg : pkgs) {
            allInPackages.addAll(pkg.getChildren());
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
                tableModel.setFilePackageExpand((AbstractPackageNode<?, ?>) element, false);
                return true;
            }
        }
        if (stroke.equals(KEY_STROKE_KP_RIGHT) || stroke.equals(KEY_STROKE_RIGHT)) {
            AbstractNode element = this.getModel().getElementAt(this.getSelectedRow());
            if (element != null && element instanceof AbstractPackageNode) {
                tableModel.setFilePackageExpand((AbstractPackageNode<?, ?>) element, true);
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
        final int[] rows = this.getSelectedRows();
        for (final int row : rows) {
            final AbstractNode node = getModel().getObjectbyRow(row);
            if (node != null && node instanceof AbstractPackageNode<?, ?>) {
                ret.add((ParentType) node);
            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public java.util.List<ChildrenType> getSelectedChildren() {
        final java.util.List<ChildrenType> ret = new ArrayList<ChildrenType>();
        final int[] rows = this.getSelectedRows();
        for (final int row : rows) {
            final AbstractNode node = getModel().getObjectbyRow(row);
            if (node != null && node instanceof AbstractPackageChildrenNode<?>) {
                ret.add((ChildrenType) node);
            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public java.util.List<ChildrenType> getAllSelectedChildren(java.util.List<AbstractNode> selectedObjects) {
        final java.util.List<ChildrenType> links = new ArrayList<ChildrenType>();
        for (final AbstractNode node : selectedObjects) {
            if (node instanceof AbstractPackageChildrenNode<?>) {
                if (!links.contains(node)) links.add((ChildrenType) node);
            } else {
                boolean readL = ((AbstractPackageNode) node).getModifyLock().readLock();
                try {
                    for (final ChildrenType dl : ((ParentType) node).getChildren()) {
                        if (!links.contains(dl)) {
                            links.add(dl);
                        }
                    }
                } finally {
                    ((AbstractPackageNode) node).getModifyLock().readUnlock(readL);
                }
            }
        }
        return links;
    }

}
