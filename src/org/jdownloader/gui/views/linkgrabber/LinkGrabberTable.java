package org.jdownloader.gui.views.linkgrabber;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EventObject;

import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.TransferHandler;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.JDGui;

import org.appwork.swing.exttable.DropHighlighter;
import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ContextMenuFactory;
import org.jdownloader.gui.views.linkgrabber.contextmenu.RemoveSelectionAction;
import org.jdownloader.images.NewTheme;

public class LinkGrabberTable extends PackageControllerTable<CrawledPackage, CrawledLink> {

    private static final long  serialVersionUID = 8843600834248098174L;
    private ContextMenuFactory contextMenuFactory;

    public LinkGrabberTable(final LinkGrabberTableModel tableModel) {
        super(tableModel);

        this.addRowHighlighter(new DropHighlighter(null, new Color(27, 164, 191, 75)));
        this.setTransferHandler(new LinkGrabberTableTransferHandler(this));
        this.setDragEnabled(true);
        this.setDropMode(DropMode.ON_OR_INSERT_ROWS);
        contextMenuFactory = new ContextMenuFactory(this);

    }

    @Override
    public boolean editCellAt(int row, int column, EventObject e) {
        boolean ret = super.editCellAt(row, column, e);
        if (ret) {

            AbstractNode object = getExtTableModel().getObjectbyRow(row);
            if (object instanceof CrawledPackage) {
                String title = _GUI._.LinkGrabberTable_editCellAt_filepackage_title();
                String msg = _GUI._.LinkGrabberTable_editCellAt_filepackage_msg();
                ImageIcon icon = NewTheme.I().getIcon("edit", 32);
                JDGui.help(title, msg, icon);

            } else if (object instanceof CrawledLink) {
                String title = _GUI._.LinkGrabberTable_editCellAt_link_title();
                String msg = _GUI._.LinkGrabberTable_editCellAt_link_msg();
                ImageIcon icon = NewTheme.I().getIcon("edit", 32);
                JDGui.help(title, msg, icon);
            }

        }
        return ret;
    }

    @Override
    public boolean isSearchEnabled() {
        return true;
    }

    @Override
    protected void onDoubleClick(final MouseEvent e, final AbstractNode obj) {

    }

    @Override
    protected JPopupMenu onContextMenu(final JPopupMenu popup, final AbstractNode contextObject, final ArrayList<AbstractNode> selection, final ExtColumn<AbstractNode> column, MouseEvent event) {
        return contextMenuFactory.createPopup(contextObject, selection, column, event);
    }

    @Override
    protected boolean onShortcutDelete(final ArrayList<AbstractNode> selectedObjects, final KeyEvent evt, final boolean direct) {
        new RemoveSelectionAction(this, selectedObjects).actionPerformed(null);
        return true;
    }

    @Override
    protected boolean onShortcutCopy(ArrayList<AbstractNode> selectedObjects, KeyEvent evt) {
        TransferHandler.getCopyAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "copy"));
        return true;
    }

    @Override
    protected boolean onShortcutCut(ArrayList<AbstractNode> selectedObjects, KeyEvent evt) {
        TransferHandler.getCutAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "cut"));
        return true;
    }

    @Override
    protected boolean onShortcutPaste(ArrayList<AbstractNode> selectedObjects, KeyEvent evt) {
        TransferHandler.getPasteAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "paste"));
        return true;
    }

}
