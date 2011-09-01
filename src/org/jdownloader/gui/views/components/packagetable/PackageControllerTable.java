package org.jdownloader.gui.views.components.packagetable;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.ListSelectionModel;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.exttable.DropHighlighter;
import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel.TOGGLEMODE;
import org.jdownloader.gui.views.downloads.columns.FileColumn;

public abstract class PackageControllerTable<E extends AbstractPackageNode<V, E>, V extends AbstractPackageChildrenNode<E>> extends BasicJDTable<AbstractNode> {

    /**
     * 
     */
    private static final long                 serialVersionUID = 3880570615872972276L;
    private PackageControllerTableModel<E, V> tableModel       = null;

    public PackageControllerTable(PackageControllerTableModel<E, V> pctm) {
        super(pctm);
        tableModel = pctm;
        this.setShowVerticalLines(false);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.addRowHighlighter(new DropHighlighter(null, new Color(27, 164, 191, 75)));
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
    }

}
