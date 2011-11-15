package org.jdownloader.gui.views.linkgrabber;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.DropMode;
import javax.swing.JPopupMenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.exttable.DropHighlighter;
import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ContextMenuFactory;
import org.jdownloader.gui.views.linkgrabber.contextmenu.RemoveSelectionAction;

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

}
