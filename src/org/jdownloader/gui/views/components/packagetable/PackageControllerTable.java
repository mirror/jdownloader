package org.jdownloader.gui.views.components.packagetable;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.ListSelectionModel;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;
import jd.gui.swing.jdgui.BasicJDTable;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel.TOGGLEMODE;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public abstract class PackageControllerTable<ParentType extends AbstractPackageNode<ChildrenType, ParentType>, ChildrenType extends AbstractPackageChildrenNode<ParentType>> extends BasicJDTable<AbstractNode> {

    /**
     * 
     */
    private static final long                                     serialVersionUID = 3880570615872972276L;
    private PackageControllerTableModel<ParentType, ChildrenType> tableModel       = null;
    private Color                                                 sortNotifyColor;
    private Color                                                 filterNotifyColor;

    public PackageControllerTable(PackageControllerTableModel<ParentType, ChildrenType> pctm) {
        super(pctm);
        tableModel = pctm;
        this.setShowVerticalLines(false);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        sortNotifyColor = CFG_GUI.SORT_COLUMN_HIGHLIGHT_ENABLED.getValue() ? new Color(LookAndFeelController.getInstance().getLAFOptions().getHighlightColor1()) : null;
        filterNotifyColor = CFG_GUI.CFG.isFilterHighlightEnabled() ? new Color(LookAndFeelController.getInstance().getLAFOptions().getHighlightColor2()) : null;

    }

    public PackageControllerTableModel<ParentType, ChildrenType> getPackageControllerTableModel() {
        return tableModel;
    }

    public PackageController<ParentType, ChildrenType> getController() {
        return tableModel.getController();
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
