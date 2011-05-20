//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.jdgui.views.downloads;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import jd.controlling.DownloadController;
import jd.event.ControlEvent;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.table.JDRowHighlighter;
import jd.gui.swing.components.table.JDTable;
import jd.gui.swing.components.table.JDTableColumn;
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
import jd.nutils.OSDetector;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLinkInfoCache;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageInfoCache;
import jd.plugins.LinkStatus;
import jd.utils.JDUtilities;

import org.appwork.utils.Application;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class DownloadTable extends JDTable implements MouseListener, KeyListener {

    public static final String       PROPERTY_EXPANDED = "expanded";
    public final static byte         EXPCOL_TOP        = 0;
    public final static byte         EXPCOL_CUR        = 1;
    public final static byte         EXPCOL_BOT        = 2;

    private static final long        serialVersionUID  = 1L;

    private final DownloadLinksPanel panel;

    public static String[]           PRIO_DESCS        = new String[] { _GUI._.gui_treetable_tooltip_priority_1(), _GUI._.gui_treetable_tooltip_priority0(), _GUI._.gui_treetable_tooltip_priority1(), _GUI._.gui_treetable_tooltip_priority2(), _GUI._.gui_treetable_tooltip_priority3() };

    private final PropMenuItem       propItem;

    public DownloadTable(final DownloadLinksPanel panel) {
        super(new DownloadJTableModel("gui2"));
        this.panel = panel;
        this.addMouseListener(this);
        this.addKeyListener(this);
        if (Application.getJavaVersion() >= 16000000) {
            this.setDropMode(DropMode.USE_SELECTION);
        }
        this.setDragEnabled(true);
        this.setTransferHandler(new TableTransferHandler(this));
        this.setColumnSelectionAllowed(false);
        this.setRowSelectionAllowed(true);

        this.addDisabledHighlighter();
        this.addPostErrorHighlighter();
        this.addWaitHighlighter();
        this.addPackageHighlighter();
        this.propItem = new PropMenuItem(panel);
    }

    private void addDisabledHighlighter() {
        this.addJDRowHighlighter(new JDRowHighlighter(new Color(173, 173, 173, 100)) {
            @Override
            public boolean doHighlight(final Object o) {
                return o != null && o instanceof DownloadLink && !((DownloadLink) o).isEnabled();
            }
        });
    }

    private void addPackageHighlighter() {
        this.addJDRowHighlighter(new JDRowHighlighter(UIManager.getColor("TableHeader.background")) {
            @Override
            public boolean doHighlight(final Object o) {
                return o != null && o instanceof FilePackage;
            }
        });
    }

    private void addPostErrorHighlighter() {
        this.addJDRowHighlighter(new JDRowHighlighter(new Color(255, 153, 54, 120)) {
            @Override
            public boolean doHighlight(final Object o) {
                return o != null && o instanceof DownloadLink && ((DownloadLink) o).getLinkStatus().hasStatus(LinkStatus.ERROR_POST_PROCESS);
            }
        });
    }

    private void addWaitHighlighter() {
        this.addJDRowHighlighter(new JDRowHighlighter(new Color(255, 153, 54, 100)) {
            @Override
            public boolean doHighlight(final Object o) {
                if (o == null || !(o instanceof DownloadLink)) { return false; }
                final DownloadLink dl = (DownloadLink) o;
                if (dl.getLinkStatus().hasStatus(LinkStatus.FINISHED) || !dl.isEnabled() || dl.getLinkStatus().isPluginActive()) { return false; }
                return false;
                /* TODO: PROXYTODO */
                // return
                // ProxyController.getInstance().hasRemainingIPBlockWaittime(dl.getHost())
                // ||
                // ProxyController.getInstance().hasRemainingIPBlockWaittime(dl.getHost());
            }
        });
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
        // pop.add(new StopAction(links));

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

            final JDTableColumn column = this.getJDTableModel().getJDTableColumn(col);
            if (column.isSortable(sfp)) {
                this.getDefaultSortMenuItem().set(column, sfp, _GUI._.gui_table_contextmenu_packagesort() + " (" + sfp.size() + "), (" + this.getJDTableModel().getColumnName(col) + ")");
                ret.add(new RatedMenuItem("SORTITEM", this.getDefaultSortMenuItem(), 0));
            }

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
            if (Application.getJavaVersion() >= 16000000) {
                // check if open a file is supported by this operating
                // system (on linux maybe wrong GNOME version)
                if (Desktop.isDesktopSupported()) {
                    // add the Open File entry
                    ret.add(new RatedMenuItem(new OpenFileAction(new File(((DownloadLink) obj).getFileOutput())), 0));
                }
            }
        }
        ret.add(RatedMenuItem.createSeparator());
        //
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
        ret.add(new RatedMenuItem("PRIORITY", this.createPrioMenu(alllinks), 0));

        return ret;
    }

    private JMenu createPrioMenu(final ArrayList<DownloadLink> links) {
        final JMenu prioPopup = new JMenu(_GUI._.gui_table_contextmenu_priority() + " (" + links.size() + ")");
        prioPopup.setIcon(NewTheme.I().getIcon("prio_0", 16));

        prioPopup.add(new PriorityAction(links, 3));
        prioPopup.add(new PriorityAction(links, 2));
        prioPopup.add(new PriorityAction(links, 1));
        prioPopup.add(new PriorityAction(links, 0));
        prioPopup.add(new PriorityAction(links, -1));

        return prioPopup;
    }

    public void fireTableChanged(final int id, final ArrayList<Object> objs) {
        new GuiRunnable<Object>() {
            // @Override
            @Override
            public Object runSave() {
                final Rectangle viewRect = DownloadTable.this.panel.getScrollPane().getViewport().getViewRect();
                final int first = DownloadTable.this.rowAtPoint(new Point(0, viewRect.y));
                final int last = DownloadTable.this.rowAtPoint(new Point(0, viewRect.y + viewRect.height - 1));
                switch (id) {
                case DownloadLinksPanel.REFRESH_SPECIFIED_LINKS:
                    for (final Object obj : objs) {
                        final int row = DownloadTable.this.getJDTableModel().getRowforObject(obj);
                        if (row != -1) {
                            if (last == -1) {
                                DownloadTable.this.getJDTableModel().fireTableRowsUpdated(row, row);
                            } else if (row >= first && row <= last) {
                                DownloadTable.this.getJDTableModel().fireTableRowsUpdated(row, row);
                            }
                        }
                    }
                    return null;
                case DownloadLinksPanel.REFRESH_ALL_DATA_CHANGED:
                    DownloadLinkInfoCache.reset();
                    FilePackageInfoCache.reset();
                    DownloadTable.this.getJDTableModel().fireTableDataChanged();
                    return null;
                case DownloadLinksPanel.REFRESH_DATA_AND_STRUCTURE_CHANGED:
                case DownloadLinksPanel.REFRESH_DATA_AND_STRUCTURE_CHANGED_FAST:
                    DownloadLinkInfoCache.reset();
                    FilePackageInfoCache.reset();
                    final int[] rows = DownloadTable.this.getSelectedRows();
                    final ArrayList<Object> selected = new ArrayList<Object>();
                    for (final int row : rows) {
                        final Object elem = DownloadTable.this.getValueAt(row, 0);
                        if (elem != null) {
                            selected.add(elem);
                        }
                    }
                    DownloadTable.this.getJDTableModel().refreshModel();
                    DownloadTable.this.getJDTableModel().fireTableStructureChanged();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            for (final Object obj : selected) {
                                final int row = DownloadTable.this.getJDTableModel().getRowforObject(obj);
                                if (row != -1) {
                                    DownloadTable.this.addRowSelectionInterval(row, row);
                                }
                            }
                            DownloadTable.this.scrollRectToVisible(viewRect);
                        }
                    });
                    return null;
                }
                return null;
            }
        }.start();
    }

    public ArrayList<DownloadLink> getAllSelectedDownloadLinks() {
        final ArrayList<DownloadLink> links = this.getSelectedDownloadLinks();
        final ArrayList<FilePackage> fps = this.getSelectedFilePackages();
        for (final FilePackage filePackage : fps) {
            for (final DownloadLink dl : filePackage.getDownloadLinkList()) {
                if (!links.contains(dl)) {
                    links.add(dl);
                }
            }
        }
        return links;
    }

    public ArrayList<DownloadLink> getSelectedDownloadLinks() {
        final int[] rows = this.getSelectedRows();
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (final int row : rows) {
            final Object element = this.getModel().getValueAt(row, 0);
            if (element != null && element instanceof DownloadLink) {
                ret.add((DownloadLink) element);
            }
        }
        return ret;
    }

    public ArrayList<FilePackage> getSelectedFilePackages() {
        final int[] rows = this.getSelectedRows();
        final ArrayList<FilePackage> ret = new ArrayList<FilePackage>();
        for (final int row : rows) {
            final Object element = this.getModel().getValueAt(row, 0);
            if (element != null && element instanceof FilePackage) {
                ret.add((FilePackage) element);
            }
        }
        return ret;
    }

    public void keyPressed(final KeyEvent e) {
    }

    public void keyReleased(final KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DELETE || OSDetector.isMac() && e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            final ArrayList<DownloadLink> alllinks = this.getAllSelectedDownloadLinks();
            if (alllinks.isEmpty()) { return; }
            new DeleteAction(alllinks).actionPerformed(null);
        }
        if ((e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) && this.panel.isFilePackageInfoVisible(null)) {
            int[] rows = getSelectedRows();

            Object element = getValueAt(rows[0], 0);

            if (element != null) {
                if (element instanceof FilePackage) {
                    this.panel.showFilePackageInfo((FilePackage) element);
                } else {
                    this.panel.showDownloadLinkInfo((DownloadLink) element);
                }
            }
        }
    }

    public void keyTyped(final KeyEvent e) {
    }

    public void mouseClicked(final MouseEvent e) {
    }

    public void mouseEntered(final MouseEvent e) {
    }

    public void mouseExited(final MouseEvent e) {
    }

    public void mousePressed(final MouseEvent e) {
        /* nicht auf headerclicks reagieren */
        if (e.getSource() != this) { return; }
        final Point point = e.getPoint();
        final int row = this.rowAtPoint(point);
        final int col = this.realColumnAtPoint(point);

        final Object obj = this.getModel().getValueAt(row, 0);

        if (obj == null) {
            this.clearSelection();
            return;
        }

        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            if (!this.isRowSelected(row)) {
                this.clearSelection();
                this.addRowSelectionInterval(row, row);
            }

            final ArrayList<DownloadLink> alllinks = this.getAllSelectedDownloadLinks();

            final ArrayList<FilePackage> sfp = this.getSelectedFilePackages();

            final JPopupMenu popup = new JPopupMenu();

            final RatedMenuController items = this.createMenuItems(obj, col, alllinks, sfp);

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

            // popup.add(this.createMoreMenu(alllinks));
            // popup.addSeparator();

            popup.addSeparator();

            // popup.add(this.createPrioMenu(alllinks));

            this.propItem.setObject(obj);
            popup.add(this.propItem);

            popup.show(this, point.x, point.y);
        }
    }

    public void mouseReleased(final MouseEvent e) {
        /* nicht auf headerclicks reagieren */
        if (e.getSource() != this) { return; }
        final int row = this.rowAtPoint(e.getPoint());
        if (row == -1) { return; }
        /* deselect package */
        if (row >= 0 && !this.isRowSelected(row)) {
            final Object obj = this.getValueAt(row, 0);
            if (obj != null && obj instanceof DownloadLink) {
                final int row2 = this.getJDTableModel().getRowforObject(((DownloadLink) obj).getFilePackage());
                if (row >= 0 && this.isRowSelected(row2)) {
                    this.removeRowSelectionInterval(row2, row2);
                }
            }
        }
        final int column = this.realColumnAtPoint(e.getPoint());
        if (column == 0 && e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1) {
            final Point p = this.getPointinCell(e.getPoint());
            if (p != null && p.getX() < 30) {
                final Object element = this.getValueAt(row, 0);
                if (element != null && element instanceof FilePackage) {
                    if (e.isControlDown() && !e.isShiftDown()) {
                        this.toggleFilePackageExpand((FilePackage) element, DownloadTable.EXPCOL_BOT);
                    } else if (e.isControlDown() && e.isShiftDown()) {
                        this.toggleFilePackageExpand((FilePackage) element, DownloadTable.EXPCOL_TOP);
                    } else {
                        this.toggleFilePackageExpand((FilePackage) element, DownloadTable.EXPCOL_CUR);
                    }
                    return;
                }
            }
        }
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (column == 0) {
                final Point p = this.getPointinCell(e.getPoint());
                /* dont react here on collapse/expand icon */
                if (p != null && p.getX() < 30) { return; }
            }
            if (e.getClickCount() == 1 && this.panel.isFilePackageInfoVisible(null) || e.getClickCount() == 2) {
                final Object element = this.getValueAt(row, 0);
                if (this.panel.isFilePackageInfoVisible(element) && e.getClickCount() == 2) {
                    this.panel.hideFilePackageInfo();
                } else if (element instanceof FilePackage) {
                    this.panel.showFilePackageInfo((FilePackage) element);
                } else {
                    this.panel.showDownloadLinkInfo((DownloadLink) element);
                }
            }
        }

    }

    @Override
    protected boolean processKeyBinding(final KeyStroke ks, final KeyEvent e, final int condition, final boolean pressed) {
        final boolean ret = super.processKeyBinding(ks, e, condition, pressed);
        if (this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(ks) != null) { return false; }
        if (this.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(ks) != null) { return false; }
        return ret;
    }

    public void toggleFilePackageExpand(final FilePackage fp, final byte mode) {
        final boolean cur = !fp.isExpanded();
        switch (mode) {
        case EXPCOL_CUR:
            fp.setExpanded(cur);
            break;
        case EXPCOL_TOP: {
            final ArrayList<FilePackage> packages = new ArrayList<FilePackage>(DownloadController.getInstance().getPackages());
            final int indexfp = DownloadController.getInstance().indexOf(fp);
            for (int index = 0; index <= indexfp; index++) {
                final FilePackage fp2 = packages.get(index);
                fp2.setExpanded(cur);
            }
        }
            break;
        case EXPCOL_BOT: {
            final ArrayList<FilePackage> packages = new ArrayList<FilePackage>(DownloadController.getInstance().getPackages());
            final int indexfp = DownloadController.getInstance().indexOf(fp);
            for (int index = indexfp; index < packages.size(); index++) {
                final FilePackage fp2 = packages.get(index);
                fp2.setExpanded(cur);
            }
        }
            break;
        default:
            return;
        }
        this.panel.updateTableTask(DownloadLinksPanel.REFRESH_DATA_AND_STRUCTURE_CHANGED_FAST, null);
    }
}

class PropMenuItem extends JMenuItem implements ActionListener {

    private static final long        serialVersionUID = 6328630034846759725L;
    private Object                   obj;
    private final DownloadLinksPanel panel;

    public PropMenuItem(final DownloadLinksPanel panel) {
        super(_GUI._.gui_table_contextmenu_prop());
        this.setIcon(NewTheme.I().getIcon("info", 16));
        this.panel = panel;
        this.addActionListener(this);
    }

    public void actionPerformed(final ActionEvent e) {
        if (this.obj == null) { return; }
        if (this.obj instanceof DownloadLink) {
            this.panel.showDownloadLinkInfo((DownloadLink) this.obj);
        } else if (this.obj instanceof FilePackage) {
            this.panel.showFilePackageInfo((FilePackage) this.obj);
        }
    }

    public void setObject(final Object obj) {
        this.obj = obj;
    }
}