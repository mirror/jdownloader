package org.jdownloader.gui.views.components.linktable;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;

import jd.gui.swing.jdgui.BasicJDTable;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;

import org.appwork.utils.swing.table.DropHighlighter;
import org.appwork.utils.swing.table.ExtColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.gui.views.downloads.context.RatedMenuController;
import org.jdownloader.gui.views.linkgrabber.LinkTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkTableModel.TOGGLEMODE;
import org.jdownloader.images.NewTheme;

public abstract class LinkTable extends BasicJDTable<PackageLinkNode> {

    protected LinkTableModel tableModel;

    public LinkTable(LinkTableModel tableModel) {
        super(tableModel);
        this.tableModel = tableModel;
        this.setShowVerticalLines(false);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        this.addRowHighlighter(new DropHighlighter(null, new Color(27, 164, 191, 75)));
    }

    public ArrayList<FilePackage> getSelectedFilePackages() {
        final ArrayList<FilePackage> ret = new ArrayList<FilePackage>();
        final ArrayList<PackageLinkNode> selected = this.getExtTableModel().getSelectedObjects();
        for (final PackageLinkNode node : selected) {
            if (node instanceof FilePackage) {
                ret.add((FilePackage) node);
            }
        }
        return ret;
    }

    public ArrayList<DownloadLink> getSelectedDownloadLinks() {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final ArrayList<PackageLinkNode> selected = this.getExtTableModel().getSelectedObjects();
        for (final PackageLinkNode node : selected) {
            if (node instanceof DownloadLink) {
                ret.add((DownloadLink) node);
            }
        }
        return ret;
    }

    @Override
    protected void onSingleClick(MouseEvent e, final PackageLinkNode obj) {
        if (obj instanceof FilePackage) {
            final ExtColumn<PackageLinkNode> column = this.getExtColumnAtPoint(e.getPoint());

            if (FileColumn.class == column.getClass()) {
                Rectangle bounds = column.getBounds();
                if (e.getPoint().x - bounds.x < 30) {
                    if (e.isControlDown() && !e.isShiftDown()) {
                        tableModel.toggleFilePackageExpand((FilePackage) obj, TOGGLEMODE.BOTTOM);
                    } else if (e.isControlDown() && e.isShiftDown()) {
                        tableModel.toggleFilePackageExpand((FilePackage) obj, TOGGLEMODE.TOP);
                    } else {
                        tableModel.toggleFilePackageExpand((FilePackage) obj, TOGGLEMODE.CURRENT);
                    }
                    return;
                }
            }
        }
    }

    /**
     * create new table model data
     */
    public void recreateModel() {
        tableModel.recreateModel();
    }

    /**
     * refresh only the table model data
     */
    public void refreshModel() {
        tableModel.refreshModel();
    }

    protected ArrayList<DownloadLink> getAllDownloadLinks(ArrayList<PackageLinkNode> selectedObjects) {
        final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        for (final PackageLinkNode node : selectedObjects) {
            if (node instanceof DownloadLink) {
                if (!links.contains(node)) links.add((DownloadLink) node);
            } else {
                synchronized (node) {
                    for (final DownloadLink dl : ((FilePackage) node).getControlledDownloadLinks()) {
                        if (!links.contains(dl)) {
                            links.add(dl);
                        }
                    }
                }
            }
        }
        return links;
    }

    @Override
    protected JPopupMenu onContextMenu(final JPopupMenu popup, final PackageLinkNode contextObject, final ArrayList<PackageLinkNode> selection, ExtColumn<PackageLinkNode> column) {
        /* split selection into downloadlinks and filepackages */
        final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        final ArrayList<FilePackage> fps = new ArrayList<FilePackage>();
        if (selection != null) {
            for (final PackageLinkNode node : selection) {
                if (node instanceof DownloadLink) {
                    if (!links.contains(node)) links.add((DownloadLink) node);
                } else {
                    if (!fps.contains(node)) fps.add((FilePackage) node);
                    synchronized (node) {
                        for (final DownloadLink dl : ((FilePackage) node).getControlledDownloadLinks()) {
                            if (!links.contains(dl)) {
                                links.add(dl);
                            }
                        }
                    }
                }
            }
        }
        final RatedMenuController items = this.createMenuItems(contextObject, 0, links, fps);
        items.init(10);
        while (items.getMain().size() > 0) {
            items.getMain().remove(0).addToPopup(popup);
        }
        final JMenu pop = new JMenu(_GUI._.gui_table_contextmenu_more());
        popup.add(pop);
        pop.setIcon(NewTheme.I().getIcon("settings", 16));
        while (items.getSub().size() > 0) {
            items.getSub().remove(0).addToPopup(pop);
        }
        popup.add(new JSeparator());
        // popup.add(new EditLinkOrPackageAction(this, contextObject));
        return popup;
    }

    abstract protected RatedMenuController createMenuItems(PackageLinkNode obj, int col, ArrayList<DownloadLink> alllinks, ArrayList<FilePackage> sfp);

}
