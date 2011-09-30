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
import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel.TOGGLEMODE;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public abstract class PackageControllerTable<E extends AbstractPackageNode<V, E>, V extends AbstractPackageChildrenNode<E>> extends BasicJDTable<AbstractNode> {

    /**
     * 
     */
    private static final long                 serialVersionUID = 3880570615872972276L;
    private PackageControllerTableModel<E, V> tableModel       = null;
    private Color                             sortNotifyColor;

    public PackageControllerTable(PackageControllerTableModel<E, V> pctm) {
        super(pctm);
        tableModel = pctm;
        this.setShowVerticalLines(false);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isSortColumnHighlightEnabled()) {
            sortNotifyColor = Color.ORANGE;
        }
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

        Graphics2D g2 = (Graphics2D) g;
        Composite comp = g2.getComposite();
        final Rectangle visibleRect = this.getVisibleRect();
        Rectangle first;
        ExtColumn<AbstractNode> sortColumn = getExtTableModel().getSortColumn();
        if (sortColumn == null) return;
        int index = sortColumn.getIndex();

        if (index < 0) return;

        if (sortNotifyColor != null) {

            first = this.getCellRect(0, index, true);

            g2.setColor(Color.ORANGE);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
            g2.fillRect(visibleRect.x + first.x, visibleRect.y, visibleRect.x + getExtTableModel().getSortColumn().getWidth(), visibleRect.y + visibleRect.height);
        }
        if (isOriginalOrder()) return;
        g2.setComposite(comp);
    }

    @SuppressWarnings("unchecked")
    public ArrayList<E> getSelectedPackages() {
        final ArrayList<E> ret = new ArrayList<E>();
        final int[] rows = this.getSelectedRows();
        for (final int row : rows) {
            final AbstractNode node = getExtTableModel().getObjectbyRow(row);
            if (node != null && node instanceof AbstractPackageNode<?, ?>) {
                ret.add((E) node);
            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<V> getSelectedChildren() {
        final ArrayList<V> ret = new ArrayList<V>();
        final int[] rows = this.getSelectedRows();
        for (final int row : rows) {
            final AbstractNode node = getExtTableModel().getObjectbyRow(row);
            if (node != null && node instanceof AbstractPackageChildrenNode<?>) {
                ret.add((V) node);
            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    protected ArrayList<V> getAllSelectedChildren(ArrayList<AbstractNode> selectedObjects) {
        final ArrayList<V> links = new ArrayList<V>();
        for (final AbstractNode node : selectedObjects) {
            if (node instanceof AbstractPackageChildrenNode<?>) {
                if (!links.contains(node)) links.add((V) node);
            } else {
                synchronized (node) {
                    for (final V dl : ((E) node).getChildren()) {
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
