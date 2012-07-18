package org.jdownloader.gui.views.linkgrabber;

import java.awt.Color;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EventObject;

import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.swing.exttable.DropHighlighter;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
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

        return ret;
    }

    @Override
    public boolean isSearchEnabled() {
        return false;
    }

    @Override
    protected boolean onDoubleClick(final MouseEvent e, final AbstractNode obj) {
        JPopupMenu m = new JPopupMenu();

        if (obj instanceof AbstractPackageNode) {

            Image back = (((AbstractPackageNode<?, ?>) obj).isExpanded() ? NewTheme.I().getImage("tree_package_open", 32) : NewTheme.I().getImage("tree_package_closed", 32));

            m.add(SwingUtils.toBold(new JLabel(_GUI._.ContextMenuFactory_createPopup_properties(obj.getName()), new ImageIcon(ImageProvider.merge(back, NewTheme.I().getImage("settings", 14), -16, 0, 6, 6)), SwingConstants.LEFT)));
            m.add(new JSeparator());
        } else if (obj instanceof CrawledLink) {

            Image back = (((CrawledLink) obj).getDownloadLink().getIcon().getImage());

            m.add(SwingUtils.toBold(new JLabel(_GUI._.ContextMenuFactory_createPopup_properties(obj.getName()), new ImageIcon(ImageProvider.merge(back, NewTheme.I().getImage("settings", 14), 0, 0, 6, 6)), SwingConstants.LEFT)));
            m.add(new JSeparator());
        }

        final ExtColumn<AbstractNode> col = this.getExtColumnAtPoint(e.getPoint());

        for (JMenuItem mm : ContextMenuFactory.fillPropertiesMenu(new SelectionInfo<CrawledPackage, CrawledLink>(obj, getExtTableModel().getSelectedObjects()), col)) {
            m.add(mm);
        }
        m.show(this, e.getPoint().x, e.getPoint().y);
        return false;
    }

    @Override
    protected JPopupMenu onContextMenu(final JPopupMenu popup, final AbstractNode contextObject, final ArrayList<AbstractNode> selection, final ExtColumn<AbstractNode> column, MouseEvent event) {
        return contextMenuFactory.createPopup(contextObject, selection, column, event);
    }

    @Override
    protected boolean onShortcutDelete(final ArrayList<AbstractNode> selectedObjects, final KeyEvent evt, final boolean direct) {
        new RemoveSelectionAction(new SelectionInfo<CrawledPackage, CrawledLink>(selectedObjects)).actionPerformed(null);
        return true;
    }

    @Override
    protected boolean updateMoveButtonEnabledStatus() {
        return false;
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

    @Override
    public ExtColumn<AbstractNode> getExpandCollapseColumn() {
        return LinkGrabberTableModel.getInstance().expandCollapse;
    }
}
