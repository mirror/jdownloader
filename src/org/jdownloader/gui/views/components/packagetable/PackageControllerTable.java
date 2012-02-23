package org.jdownloader.gui.views.components.packagetable;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

import jd.controlling.IOEQ;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;
import jd.gui.swing.jdgui.BasicJDTable;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.logging.Log;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel.TOGGLEMODE;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public abstract class PackageControllerTable<ParentType extends AbstractPackageNode<ChildrenType, ParentType>, ChildrenType extends AbstractPackageChildrenNode<ParentType>> extends BasicJDTable<AbstractNode> {

    /**
     * 
     */
    private static final long                                     serialVersionUID = 3880570615872972276L;
    private PackageControllerTableModel<ParentType, ChildrenType> tableModel       = null;
    private Color                                                 sortNotifyColor;
    private Color                                                 filterNotifyColor;
    private AppAction                                             moveTopAction    = null;
    private AppAction                                             moveUpAction     = null;
    private AppAction                                             moveDownAction   = null;
    private AppAction                                             moveBottomAction = null;

    public PackageControllerTable(PackageControllerTableModel<ParentType, ChildrenType> pctm) {
        super(pctm);
        tableModel = pctm;
        this.setShowVerticalLines(false);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        sortNotifyColor = CFG_GUI.SORT_COLUMN_HIGHLIGHT_ENABLED.getValue() ? new Color(LookAndFeelController.getInstance().getLAFOptions().getHighlightColor1()) : null;
        filterNotifyColor = CFG_GUI.CFG.isFilterHighlightEnabled() ? new Color(LookAndFeelController.getInstance().getLAFOptions().getHighlightColor2()) : null;
        initAppActions();
    }

    public PackageControllerTableModel<ParentType, ChildrenType> getPackageControllerTableModel() {
        return tableModel;
    }

    public PackageController<ParentType, ChildrenType> getController() {
        return tableModel.getController();
    }

    @Override
    protected void onSelectionChanged() {
        super.onSelectionChanged();
        if (!updateMoveButtonEnabledStatus()) return;
        if (tableModel.countSelectedObjects() == 0) {
            // disable move buttons
            moveDownAction.setEnabled(false);
            moveBottomAction.setEnabled(false);
            moveTopAction.setEnabled(false);
            moveUpAction.setEnabled(false);
        } else {
            ArrayList<ParentType> selectedPkgs = getSelectedPackages();
            ArrayList<ChildrenType> selectedChld = getSelectedChildren();
            boolean moveUpPossible = moveUpPossible(selectedPkgs, selectedChld);
            boolean moveDownPossibe = moveDownPossible(selectedPkgs, selectedChld);
            moveTopAction.setEnabled(moveUpPossible);
            moveUpAction.setEnabled(moveUpPossible);
            moveDownAction.setEnabled(moveDownPossibe);
            moveBottomAction.setEnabled(moveDownPossibe);
        }
    }

    protected boolean updateMoveButtonEnabledStatus() {
        return true;
    }

    protected boolean moveUpPossible(ArrayList<ParentType> pkgs, ArrayList<ChildrenType> selectedChld) {
        if (pkgs.size() > 0 && selectedChld.size() > 0) {
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

    protected boolean moveDownPossible(ArrayList<ParentType> pkgs, ArrayList<ChildrenType> selectedChld) {
        if (pkgs.size() > 0 && selectedChld.size() > 0) {
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
            index = sameParent.getChildren().size() - 1;
            for (int i = selectedChld.size() - 1; i >= 0; i--) {
                ChildrenType child = selectedChld.get(i);
                if (sameParent.indexOf(child) != index--) return true;
            }
        }
        return false;
    }

    protected void initAppActions() {
        moveTopAction = new AppAction() {
            {
                // setName(_GUI._.BottomBar_BottomBar_totop());
                setToolTipText(_GUI._.BottomBar_BottomBar_totop_tooltip());
                setSmallIcon(NewTheme.I().getIcon("go-top", 20));
            }

            public void actionPerformed(ActionEvent e) {
                IOEQ.add(new Runnable() {

                    public void run() {
                        ArrayList<ParentType> selectedPkgs = getSelectedPackages();
                        ArrayList<ChildrenType> selectedChld = getSelectedChildren();
                        boolean moveUpPossible = moveUpPossible(selectedPkgs, selectedChld);
                        if (moveUpPossible == false) return;
                        PackageController<ParentType, ChildrenType> pc = getController();
                        if (selectedPkgs.size() > 0) {
                            /* move package to top of list */
                            pc.move(selectedPkgs, null);
                        }
                        if (selectedChld.size() > 0) {
                            /* move children to top of package */
                            pc.move(selectedChld, selectedChld.get(0).getParentNode(), null);
                        }
                    }

                }, true);
            }

        };
        moveUpAction = new AppAction() {
            {
                // setName(_GUI._.BottomBar_BottomBar_moveup());
                setToolTipText(_GUI._.BottomBar_BottomBar_moveup_tooltip());
                setSmallIcon(NewTheme.I().getIcon("go-up", 20));
            }

            public void actionPerformed(ActionEvent e) {
                IOEQ.add(new Runnable() {

                    public void run() {
                        ArrayList<ParentType> selectedPkgs = getSelectedPackages();
                        ArrayList<ChildrenType> selectedChld = getSelectedChildren();
                        boolean moveUpPossible = moveUpPossible(selectedPkgs, selectedChld);
                        if (moveUpPossible == false) return;
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
                            synchronized (pkg) {
                                try {
                                    int index = pkg.indexOf(selectedChld.get(0)) - 2;
                                    if (index >= 0) {
                                        /* move after this element */
                                        after = pkg.getChildren().get(index);
                                    }/* else move to top */
                                } catch (final Throwable e) {
                                    Log.exception(e);
                                }
                            }
                            pc.move(selectedChld, pkg, after);
                        }
                    }

                }, true);
            }

        };
        moveDownAction = new AppAction() {
            {
                // setName(_GUI._.BottomBar_BottomBar_movedown());
                setToolTipText(_GUI._.BottomBar_BottomBar_movedown_tooltip());
                setSmallIcon(NewTheme.I().getIcon("go-down", 20));
            }

            public void actionPerformed(ActionEvent e) {
                IOEQ.add(new Runnable() {

                    public void run() {
                        ArrayList<ParentType> selectedPkgs = getSelectedPackages();
                        ArrayList<ChildrenType> selectedChld = getSelectedChildren();
                        boolean moveDownPossible = moveDownPossible(selectedPkgs, selectedChld);
                        if (moveDownPossible == false) return;
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
                            synchronized (pkg) {
                                try {
                                    /* move after after element or max at bottom */
                                    int index = Math.min(pkg.getChildren().size() - 1, pkg.indexOf(selectedChld.get(selectedChld.size() - 1)) + 1);
                                    after = pkg.getChildren().get(index);
                                } catch (final Throwable e) {
                                    Log.exception(e);
                                }
                            }
                            pc.move(selectedChld, pkg, after);
                        }
                    }

                }, true);
            }

        };
        moveBottomAction = new AppAction() {
            {
                // setName(_GUI._.BottomBar_BottomBar_tobottom());
                setToolTipText(_GUI._.BottomBar_BottomBar_tobottom_tooltip());
                setSmallIcon(NewTheme.I().getIcon("go-bottom", 20));
            }

            public void actionPerformed(ActionEvent e) {
                IOEQ.add(new Runnable() {

                    public void run() {
                        ArrayList<ParentType> selectedPkgs = getSelectedPackages();
                        ArrayList<ChildrenType> selectedChld = getSelectedChildren();
                        boolean moveDownPossible = moveDownPossible(selectedPkgs, selectedChld);
                        if (moveDownPossible == false) return;
                        PackageController<ParentType, ChildrenType> pc = getController();
                        if (selectedPkgs.size() > 0) {
                            ParentType after = null;
                            boolean readL = pc.readLock();
                            try {
                                try {
                                    after = pc.getPackages().get(pc.getPackages().size() - 1);
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
                            synchronized (pkg) {
                                try {
                                    after = pkg.getChildren().get(pkg.getChildren().size() - 1);
                                } catch (final Throwable e) {
                                    Log.exception(e);
                                }
                            }
                            pc.move(selectedChld, pkg, after);
                        }
                    }

                }, true);
            }

        };
    }

    @Override
    protected boolean processKeyBinding(KeyStroke stroke, KeyEvent evt, int condition, boolean pressed) {
        if (!pressed) { return super.processKeyBinding(stroke, evt, condition, pressed); }
        switch (evt.getKeyCode()) {
        case KeyEvent.VK_UP:
            if (evt.isAltDown()) {
                this.moveUpAction.actionPerformed(null);
                return true;
            }
            break;
        case KeyEvent.VK_DOWN:
            if (evt.isAltDown()) {
                this.moveDownAction.actionPerformed(null);
                return true;
            }
            break;
        case KeyEvent.VK_HOME:
            if (evt.isAltDown()) {
                moveTopAction.actionPerformed(null);
                return true;
            }
            break;
        case KeyEvent.VK_END:
            if (evt.isAltDown()) {
                moveBottomAction.actionPerformed(null);
                return true;
            }
            break;
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

    @Override
    protected void onSingleClick(MouseEvent e, final AbstractNode obj) {
        if (obj instanceof AbstractPackageNode) {
            final ExtColumn<AbstractNode> column = this.getExtColumnAtPoint(e.getPoint());

            if (FileColumn.class == column.getClass()) {
                Rectangle bounds = column.getBounds();
                if (e.getPoint().x - bounds.x < 30) {
                    if (e.isControlDown() && !e.isShiftDown()) {
                        tableModel.toggleFilePackageExpand((AbstractPackageNode<?, ?>) obj, TOGGLEMODE.BOTTOM);
                    } else if (e.isControlDown() && e.isShiftDown()) {
                        tableModel.toggleFilePackageExpand((AbstractPackageNode<?, ?>) obj, TOGGLEMODE.TOP);
                    } else {
                        tableModel.toggleFilePackageExpand((AbstractPackageNode<?, ?>) obj, TOGGLEMODE.CURRENT);
                    }
                    return;
                }
            }
        }
        super.onSingleClick(e, obj);
    }

    public boolean isOriginalOrder() {
        return getExtTableModel().getSortColumn() == null;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        boolean filteredView = filterNotifyColor != null && tableModel.isFilteredView();
        ExtColumn<AbstractNode> sortColumn = getExtTableModel().getSortColumn();
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
        if (filteredColumn >= 0) {
            Rectangle first = this.getCellRect(0, filteredColumn, true);
            g2.setColor(sortNotifyColor);
            g2.fillRect(visibleRect.x + first.x, visibleRect.y, visibleRect.x + getExtTableModel().getSortColumn().getWidth(), visibleRect.y + visibleRect.height);
        }
        g2.setComposite(comp);
    }

    @SuppressWarnings("unchecked")
    public ArrayList<ParentType> getSelectedPackages() {
        final ArrayList<ParentType> ret = new ArrayList<ParentType>();
        final int[] rows = this.getSelectedRows();
        for (final int row : rows) {
            final AbstractNode node = getExtTableModel().getObjectbyRow(row);
            if (node != null && node instanceof AbstractPackageNode<?, ?>) {
                ret.add((ParentType) node);
            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<ChildrenType> getSelectedChildren() {
        final ArrayList<ChildrenType> ret = new ArrayList<ChildrenType>();
        final int[] rows = this.getSelectedRows();
        for (final int row : rows) {
            final AbstractNode node = getExtTableModel().getObjectbyRow(row);
            if (node != null && node instanceof AbstractPackageChildrenNode<?>) {
                ret.add((ChildrenType) node);
            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<ChildrenType> getAllSelectedChildren(ArrayList<AbstractNode> selectedObjects) {
        final ArrayList<ChildrenType> links = new ArrayList<ChildrenType>();
        for (final AbstractNode node : selectedObjects) {
            if (node instanceof AbstractPackageChildrenNode<?>) {
                if (!links.contains(node)) links.add((ChildrenType) node);
            } else {
                synchronized (node) {
                    for (final ChildrenType dl : ((ParentType) node).getChildren()) {
                        if (!links.contains(dl)) {
                            links.add(dl);
                        }
                    }
                }
            }
        }
        return links;
    }

}
