package org.jdownloader.gui.views.components.linktable;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.PackageControllerTableModel;
import jd.controlling.packagecontroller.PackageControllerTableModel.TOGGLEMODE;
import jd.gui.swing.jdgui.BasicJDTable;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.exttable.DropHighlighter;
import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.gui.views.downloads.context.RatedMenuController;
import org.jdownloader.images.NewTheme;

public abstract class LinkTable extends BasicJDTable<AbstractNode> {

    /**
     * 
     */
    private static final long             serialVersionUID = -4691993525525519421L;
    protected PackageControllerTableModel tableModel;

    public LinkTable(PackageControllerTableModel tableModel) {
        super(tableModel);
        this.tableModel = tableModel;
        this.setShowVerticalLines(false);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        this.addRowHighlighter(new DropHighlighter(null, new Color(27, 164, 191, 75)));
    }

    public ArrayList<FilePackage> getSelectedFilePackages() {
        final ArrayList<FilePackage> ret = new ArrayList<FilePackage>();
        final ArrayList<AbstractNode> selected = this.getExtTableModel().getSelectedObjects();
        for (final AbstractNode node : selected) {
            if (node instanceof FilePackage) {
                ret.add((FilePackage) node);
            }
        }
        return ret;
    }

    public ArrayList<DownloadLink> getSelectedDownloadLinks() {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final ArrayList<AbstractNode> selected = this.getExtTableModel().getSelectedObjects();
        for (final AbstractNode node : selected) {
            if (node instanceof DownloadLink) {
                ret.add((DownloadLink) node);
            }
        }
        return ret;
    }

    @Override
    protected void onSingleClick(MouseEvent e, final AbstractNode obj) {
        if (obj instanceof FilePackage) {
            final ExtColumn<AbstractNode> column = this.getExtColumnAtPoint(e.getPoint());

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

    protected ArrayList<DownloadLink> getAllDownloadLinks(ArrayList<AbstractNode> selectedObjects) {
        final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        for (final AbstractNode node : selectedObjects) {
            if (node instanceof DownloadLink) {
                if (!links.contains(node)) links.add((DownloadLink) node);
            } else {
                synchronized (node) {
                    for (final DownloadLink dl : ((FilePackage) node).getChildren()) {
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
    protected JPopupMenu onContextMenu(final JPopupMenu popup, final AbstractNode contextObject, final ArrayList<AbstractNode> selection, ExtColumn<AbstractNode> column) {
        /* split selection into downloadlinks and filepackages */
        final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        final ArrayList<FilePackage> fps = new ArrayList<FilePackage>();
        if (selection != null) {
            for (final AbstractNode node : selection) {
                if (node instanceof DownloadLink) {
                    if (!links.contains(node)) links.add((DownloadLink) node);
                } else {
                    if (!fps.contains(node)) fps.add((FilePackage) node);
                    synchronized (node) {
                        for (final DownloadLink dl : ((FilePackage) node).getChildren()) {
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

    abstract protected RatedMenuController createMenuItems(AbstractNode obj, int col, ArrayList<DownloadLink> alllinks, ArrayList<FilePackage> sfp);

}
