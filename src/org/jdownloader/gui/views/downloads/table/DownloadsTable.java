package org.jdownloader.gui.views.downloads.table;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.DropMode;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;

import jd.event.ControlEvent;
import jd.gui.swing.jdgui.BasicJDTable;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.CheckStatusAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.CopyPasswordAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.CopyURLAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.CreateDLCAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.DeleteAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.DeleteFromDiskAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.DisableAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.EnableAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.ForceDownloadAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.NewPackageAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.OpenDirectoryAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.OpenFileAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.OpenInBrowserAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.PackageDirectoryAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.PackageNameAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.PriorityAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.ResetAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.ResumeAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.SetPasswordAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.StopsignAction;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;
import jd.utils.JDUtilities;

import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.table.ExtColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.gui.views.downloads.context.RatedMenuController;
import org.jdownloader.gui.views.downloads.context.RatedMenuItem;
import org.jdownloader.images.NewTheme;

public class DownloadsTable extends BasicJDTable<PackageLinkNode> {

    private static final long   serialVersionUID = 8843600834248098174L;
    private DownloadsTableModel tableModel       = null;

    public DownloadsTable(final DownloadsTableModel tableModel) {
        super(tableModel);
        this.tableModel = tableModel;
        this.setShowVerticalLines(false);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.setDragEnabled(true);
        this.setDropMode(DropMode.INSERT_ROWS);

    }

    @Override
    protected void onDoubleClick(final MouseEvent e, final PackageLinkNode obj) {
        if (obj instanceof FilePackage) {
            final int column = this.getExtColumnIndexByPoint(e.getPoint());
            /* column 0 is filepackage/name column */
            if (FileColumn.class == this.getExtTableModel().getExtColumn(column).getClass()) {
                tableModel.toggleFilePackageExpand((FilePackage) obj, DownloadsTableModel.TOGGLEMODE.CURRENT);
            }
        }
    }

    @Override
    protected void onSingleClick(MouseEvent e, final PackageLinkNode obj) {
        if (obj instanceof FilePackage) {
            final int column = this.getExtColumnIndexByPoint(e.getPoint());
            /* column 0 is filepackage/name column */
            if (FileColumn.class == this.getExtTableModel().getExtColumn(column).getClass()) {
                final Point p = this.getPointinCell(e.getPoint());
                if (p != null && p.getX() < 30) {
                    if (e.isControlDown() && !e.isShiftDown()) {
                        tableModel.toggleFilePackageExpand((FilePackage) obj, DownloadsTableModel.TOGGLEMODE.BOTTOM);
                    } else if (e.isControlDown() && e.isShiftDown()) {
                        tableModel.toggleFilePackageExpand((FilePackage) obj, DownloadsTableModel.TOGGLEMODE.TOP);
                    } else {
                        tableModel.toggleFilePackageExpand((FilePackage) obj, DownloadsTableModel.TOGGLEMODE.CURRENT);
                    }
                    return;
                }
            }
        }
        tableModel.getSelectedObjects();
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

    @Override
    protected JPopupMenu onContextMenu(final JPopupMenu popup, final PackageLinkNode contextObject, final ArrayList<PackageLinkNode> selection, ExtColumn<PackageLinkNode> column) {
        /* split selection into downloadlinks and filepackages */
        final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        final ArrayList<FilePackage> fps = new ArrayList<FilePackage>();
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
        return popup;
    }

    private ArrayList<DownloadLink> getAllDownloadLinks(ArrayList<PackageLinkNode> selectedObjects) {
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
    protected boolean onShortcutDelete(final ArrayList<PackageLinkNode> selectedObjects, final KeyEvent evt, final boolean direct) {
        new DeleteAction(getAllDownloadLinks(selectedObjects), direct).actionPerformed(null);
        return true;
    }

    /**
     * Creates all contextmenu items, and a initial rating for it.
     * 
     * @param obj
     * @param col
     * @param alllinks
     * @param sfp
     * @return
     */
    private RatedMenuController createMenuItems(final Object obj, final int col, final ArrayList<DownloadLink> alllinks, final ArrayList<FilePackage> sfp) {
        final RatedMenuController ret = new RatedMenuController();

        ret.add(new RatedMenuItem(new StopsignAction(obj), 10));
        ret.add(new RatedMenuItem(new EnableAction(alllinks), 10));
        ret.add(new RatedMenuItem(new DisableAction(alllinks), 10));
        ret.add(new RatedMenuItem(new ForceDownloadAction(alllinks), 10));
        ret.add(new RatedMenuItem(new ResumeAction(alllinks), 10));

        ret.add(new RatedMenuItem(new ResetAction(alllinks), 5));
        ret.add(RatedMenuItem.createSeparator());

        ret.add(new RatedMenuItem(new NewPackageAction(alllinks), 0));
        ret.add(new RatedMenuItem(new CheckStatusAction(alllinks), 0));
        ret.add(new RatedMenuItem(new CreateDLCAction(alllinks), 0));
        ret.add(new RatedMenuItem(new CopyURLAction(alllinks), 0));
        ret.add(RatedMenuItem.createSeparator());

        ret.add(new RatedMenuItem(new SetPasswordAction(alllinks), 0));
        ret.add(new RatedMenuItem(new CopyPasswordAction(alllinks), 0));
        ret.add(new RatedMenuItem(ActionController.getToolBarAction("action.passwordlist"), 0));
        ret.add(new RatedMenuItem(new DeleteAction(alllinks), 0));
        ret.add(new RatedMenuItem(new DeleteFromDiskAction(alllinks), 0));

        ret.add(RatedMenuItem.createSeparator());
        if (obj instanceof FilePackage) {
            ret.add(new RatedMenuItem(new OpenDirectoryAction(new File(((FilePackage) obj).getDownloadDirectory())), 0));

            /* TODO: sort action */
            // final JDTableColumn column =
            // this.getJDTableModel().getJDTableColumn(col);
            // if (column.isSortable(sfp)) {
            // this.getDefaultSortMenuItem().set(column, sfp,
            // _GUI._.gui_table_contextmenu_packagesort() + " (" + sfp.size() +
            // "), (" + this.getJDTableModel().getColumnName(col) + ")");
            // ret.add(new RatedMenuItem("SORTITEM",
            // this.getDefaultSortMenuItem(), 0));
            // }

            ret.add(new RatedMenuItem(new PackageNameAction(sfp), 0));
            ret.add(new RatedMenuItem(new PackageDirectoryAction(sfp), 0));
        } else if (obj instanceof DownloadLink) {
            ret.add(new RatedMenuItem(new OpenDirectoryAction(new File(((DownloadLink) obj).getFileOutput()).getParentFile()), 0));
            ret.add(new RatedMenuItem(new OpenInBrowserAction(alllinks), 0));
            /*
             * check if Java version 1.6 or higher is installed, because the
             * Desktop-Class (e.g. to open a file with correct application) is
             * only supported by v1.6 or higher
             */
            if (Application.getJavaVersion() >= 16000000 && CrossSystem.isOpenFileSupported()) {
                // add the Open File entry
                ret.add(new RatedMenuItem(new OpenFileAction(new File(((DownloadLink) obj).getFileOutput())), 0));
            }
        }
        ret.add(RatedMenuItem.createSeparator());
        final ArrayList<MenuAction> entries = new ArrayList<MenuAction>();
        JDUtilities.getController().fireControlEventDirect(new ControlEvent(obj, ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU, entries));
        if (entries != null && entries.size() > 0) {
            for (final MenuAction next : entries) {
                if (next.getType() == ToolBarAction.Types.SEPARATOR) {
                    ret.add(RatedMenuItem.createSeparator());
                } else {
                    ret.add(new RatedMenuItem(next, 0));
                }
            }
        }
        ret.add(RatedMenuItem.createSeparator());
        ret.add(new RatedMenuItem("PRIORITY", PriorityAction.createPrioMenu(alllinks), 0));
        return ret;
    }

}
