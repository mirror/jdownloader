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
import jd.controlling.DownloadWatchDog;
import jd.event.ControlEvent;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.table.JDRowHighlighter;
import jd.gui.swing.components.table.JDTable;
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
import jd.gui.swing.jdgui.views.downloads.contextmenu.OpenInBrowserAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.PackageDirectoryAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.PackageNameAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.PasswordListAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.PriorityAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.ResetAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.ResumeAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.SetPasswordAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.StopsignAction;
import jd.nutils.OSDetector;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

class PropMenuItem extends JMenuItem implements ActionListener {

    private static final long serialVersionUID = 6328630034846759725L;
    private Object obj;
    private DownloadLinksPanel panel;

    public PropMenuItem(DownloadLinksPanel panel) {
        super(JDL.L("gui.table.contextmenu.prop", "Properties"));
        this.setIcon(JDTheme.II("gui.images.config.tip", 16, 16));
        this.panel = panel;
        this.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        if (obj == null) return;
        if (obj instanceof DownloadLink) {
            panel.showDownloadLinkInfo((DownloadLink) obj);
        } else if (obj instanceof FilePackage) {
            panel.showFilePackageInfo((FilePackage) obj);
        }
    }

    public void setObject(Object obj) {
        this.obj = obj;
    }
}

public class DownloadTable extends JDTable implements MouseListener, KeyListener {

    public static final String PROPERTY_EXPANDED = "expanded";
    public final static byte EXPCOL_TOP = 0;
    public final static byte EXPCOL_CUR = 1;
    public final static byte EXPCOL_BOT = 2;

    private static final long serialVersionUID = 1L;

    private DownloadLinksPanel panel;

    public static String[] prioDescs;

    private PropMenuItem propItem;

    public DownloadTable(DownloadLinksPanel panel) {
        super(new DownloadJTableModel("gui2"));
        this.panel = panel;
        addMouseListener(this);
        addKeyListener(this);
        if (JDUtilities.getJavaVersion() >= 1.6) {
            setDropMode(DropMode.USE_SELECTION);
        }
        setDragEnabled(true);
        setTransferHandler(new TableTransferHandler(this));
        setColumnSelectionAllowed(false);
        setRowSelectionAllowed(true);

        addDisabledHighlighter();
        addPostErrorHighlighter();
        addWaitHighlighter();
        addPackageHighlighter();
        prioDescs = new String[] { JDL.L("gui.treetable.tooltip.priority-1", "Low Priority"), JDL.L("gui.treetable.tooltip.priority0", "Default Priority"), JDL.L("gui.treetable.tooltip.priority1", "High Priority"), JDL.L("gui.treetable.tooltip.priority2", "Higher Priority"), JDL.L("gui.treetable.tooltip.priority3", "Highest Priority") };
        propItem = new PropMenuItem(panel);
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        boolean ret = super.processKeyBinding(ks, e, condition, pressed);
        if (getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(ks) != null) { return false; }
        if (getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(ks) != null) { return false; }
        return ret;
    }

    private void addPackageHighlighter() {
        this.addJDRowHighlighter(new JDRowHighlighter(UIManager.getColor("TableHeader.background")) {
            @Override
            public boolean doHighlight(Object o) {
                return (o != null && o instanceof FilePackage);
            }
        });
    }

    private void addWaitHighlighter() {
        this.addJDRowHighlighter(new JDRowHighlighter(JDTheme.C("gui.color.downloadlist.error_post", "ff9936", 100)) {
            @Override
            public boolean doHighlight(Object o) {
                if (o == null || !(o instanceof DownloadLink)) return false;
                DownloadLink dl = (DownloadLink) o;
                if (dl.getLinkStatus().hasStatus(LinkStatus.FINISHED) || !dl.isEnabled() || dl.getLinkStatus().isPluginActive()) return false;
                return (DownloadWatchDog.getInstance().getRemainingIPBlockWaittime(dl.getHost()) > 0) || (DownloadWatchDog.getInstance().getRemainingTempUnavailWaittime(dl.getHost()) > 0);
            }
        });
    }

    private void addPostErrorHighlighter() {
        this.addJDRowHighlighter(new JDRowHighlighter(JDTheme.C("gui.color.downloadlist.error_post", "ff9936", 120)) {
            @Override
            public boolean doHighlight(Object o) {
                return (o != null && o instanceof DownloadLink && ((DownloadLink) o).getLinkStatus().hasStatus(LinkStatus.ERROR_POST_PROCESS));
            }
        });
    }

    private void addDisabledHighlighter() {
        this.addJDRowHighlighter(new JDRowHighlighter(JDTheme.C("gui.color.downloadlist.row_link_disabled", "adadad", 100)) {
            @Override
            public boolean doHighlight(Object o) {
                return (o != null && o instanceof DownloadLink && !((DownloadLink) o).isEnabled());
            }
        });
    }

    public void fireTableChanged(final int id, final ArrayList<Object> objs) {
        new GuiRunnable<Object>() {
            // @Override
            @Override
            public Object runSave() {
                final Rectangle viewRect = panel.getScrollPane().getViewport().getViewRect();
                int first = rowAtPoint(new Point(0, viewRect.y));
                int last = rowAtPoint(new Point(0, viewRect.y + viewRect.height - 1));
                switch (id) {
                case DownloadLinksPanel.REFRESH_SPECIFIED_LINKS:
                    for (Object obj : objs) {
                        int row = getJDTableModel().getRowforObject(obj);
                        if (row != -1) {
                            if (last == -1) {
                                getJDTableModel().fireTableRowsUpdated(row, row);
                            } else if (row >= first && row <= last) {
                                getJDTableModel().fireTableRowsUpdated(row, row);
                            }
                        }
                    }
                    return null;
                case DownloadLinksPanel.REFRESH_ALL_DATA_CHANGED:
                    getJDTableModel().fireTableDataChanged();
                    return null;
                case DownloadLinksPanel.REFRESH_DATA_AND_STRUCTURE_CHANGED:
                case DownloadLinksPanel.REFRESH_DATA_AND_STRUCTURE_CHANGED_FAST:
                    int[] rows = getSelectedRows();
                    final ArrayList<Object> selected = new ArrayList<Object>();
                    for (int row : rows) {
                        Object elem = getValueAt(row, 0);
                        if (elem != null) selected.add(elem);
                    }
                    getJDTableModel().refreshModel();
                    getJDTableModel().fireTableStructureChanged();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            for (Object obj : selected) {
                                int row = getJDTableModel().getRowforObject(obj);
                                if (row != -1) addRowSelectionInterval(row, row);
                            }
                            scrollRectToVisible(viewRect);
                        }
                    });
                    return null;
                }
                return null;
            }
        }.start();
    }

    public ArrayList<DownloadLink> getSelectedDownloadLinks() {
        int[] rows = getSelectedRows();
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (int row : rows) {
            Object element = this.getModel().getValueAt(row, 0);
            if (element != null && element instanceof DownloadLink) {
                ret.add((DownloadLink) element);
            }
        }
        return ret;
    }

    public ArrayList<DownloadLink> getAllSelectedDownloadLinks() {
        ArrayList<DownloadLink> links = getSelectedDownloadLinks();
        ArrayList<FilePackage> fps = getSelectedFilePackages();
        for (FilePackage filePackage : fps) {
            for (DownloadLink dl : filePackage.getDownloadLinkList()) {
                if (!links.contains(dl)) links.add(dl);
            }
        }
        return links;
    }

    public ArrayList<FilePackage> getSelectedFilePackages() {
        int[] rows = getSelectedRows();
        ArrayList<FilePackage> ret = new ArrayList<FilePackage>();
        for (int row : rows) {
            Object element = this.getModel().getValueAt(row, 0);
            if (element != null && element instanceof FilePackage) {
                ret.add((FilePackage) element);
            }
        }
        return ret;
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DELETE || (OSDetector.isMac() && e.getKeyCode() == KeyEvent.VK_BACK_SPACE)) {
            ArrayList<DownloadLink> alllinks = getAllSelectedDownloadLinks();
            if (alllinks.isEmpty()) return;
            new DeleteAction(alllinks).actionPerformed(null);
        }
    }

    public void keyTyped(KeyEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        /* nicht auf headerclicks reagieren */
        if (e.getSource() != this) return;
        Point point = e.getPoint();
        int row = rowAtPoint(point);
        int col = realColumnAtPoint(point);

        Object obj = this.getModel().getValueAt(row, 0);

        if (obj == null) {
            clearSelection();
            return;
        }

        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            if (!isRowSelected(row)) {
                clearSelection();
                addRowSelectionInterval(row, row);
            }

            ArrayList<DownloadLink> alllinks = getAllSelectedDownloadLinks();

            ArrayList<FilePackage> sfp = getSelectedFilePackages();

            JPopupMenu popup = new JPopupMenu();

            popup.add(new StopsignAction(obj));
            popup.add(new EnableAction(alllinks));
            popup.add(new DisableAction(alllinks));
            popup.add(createMoreMenu(alllinks));
            popup.addSeparator();

            if (obj instanceof FilePackage) {
                popup.add(new OpenDirectoryAction(new File(((FilePackage) obj).getDownloadDirectory())));
                addSortItem(popup, col, sfp, JDL.L("gui.table.contextmenu.packagesort", "Sort Packages") + " (" + sfp.size() + "), (" + getJDTableModel().getColumnName(col) + ")");
                popup.add(new PackageNameAction(sfp));
                popup.add(new PackageDirectoryAction(sfp));
            } else if (obj instanceof DownloadLink) {
                popup.add(new OpenDirectoryAction(new File(((DownloadLink) obj).getFileOutput()).getParentFile()));
                popup.add(new OpenInBrowserAction((DownloadLink) obj));
            }
            popup.addSeparator();

            popup.add(createOtherMenu(alllinks));
            popup.add(createPrioMenu(alllinks));
            popup.add(createExtrasMenu(obj));
            popup.addSeparator();

            propItem.setObject(obj);
            popup.add(propItem);

            popup.show(this, point.x, point.y);
        }
    }

    private JMenu createOtherMenu(ArrayList<DownloadLink> links) {
        JMenu pop = new JMenu(JDL.L("gui.table.contextmenu.other", "Other"));
        pop.setIcon(JDTheme.II("gui.images.package", 16, 16));

        pop.add(new NewPackageAction(links));
        pop.add(new CheckStatusAction(links));
        pop.add(new CreateDLCAction(links));
        pop.add(new CopyURLAction(links));
        pop.addSeparator();

        pop.add(new SetPasswordAction(links));
        pop.add(new CopyPasswordAction(links));
        pop.add(new PasswordListAction());

        return pop;
    }

    private JMenu createMoreMenu(ArrayList<DownloadLink> links) {
        JMenu pop = new JMenu(JDL.L("gui.table.contextmenu.more", "More"));
        pop.setIcon(JDTheme.II("gui.images.configuration", 16, 16));
        pop.add(new ForceDownloadAction(links));
        pop.add(new ResumeAction(links));
        // pop.add(new StopAction(links));
        pop.add(new ResetAction(links));
        pop.addSeparator();

        pop.add(new DeleteAction(links));
        pop.add(new DeleteFromDiskAction(links));
        return pop;
    }

    private JMenu createPrioMenu(ArrayList<DownloadLink> links) {
        JMenu prioPopup = new JMenu(JDL.L("gui.table.contextmenu.priority", "Priority") + " (" + links.size() + ")");
        prioPopup.setIcon(JDTheme.II("gui.images.priority0", 16, 16));

        prioPopup.add(new PriorityAction(links, 3));
        prioPopup.add(new PriorityAction(links, 2));
        prioPopup.add(new PriorityAction(links, 1));
        prioPopup.add(new PriorityAction(links, 0));
        prioPopup.add(new PriorityAction(links, -1));

        return prioPopup;
    }

    private JMenu createExtrasMenu(Object obj) {
        JMenu pluginPopup = new JMenu(JDL.L("gui.table.contextmenu.extrasSubmenu", "Extras"));
        pluginPopup.setIcon(JDTheme.II("gui.images.config.packagemanager", 16, 16));
        ArrayList<MenuAction> entries = new ArrayList<MenuAction>();
        JDUtilities.getController().fireControlEventDirect(new ControlEvent(obj, ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU, entries));
        if (entries != null && entries.size() > 0) {
            for (MenuAction next : entries) {
                JMenuItem mi = next.toJMenuItem();
                if (mi == null) {
                    pluginPopup.addSeparator();
                } else {
                    pluginPopup.add(mi);
                }
            }
        } else {
            pluginPopup.setEnabled(false);
        }
        return pluginPopup;
    }

    public void mouseReleased(MouseEvent e) {
        /* nicht auf headerclicks reagieren */
        if (e.getSource() != this) return;
        int row = rowAtPoint(e.getPoint());
        if (row == -1) return;
        /* deselect package */
        if (row >= 0 && !isRowSelected(row)) {
            Object obj = getValueAt(row, 0);
            if (obj != null && obj instanceof DownloadLink) {
                int row2 = getJDTableModel().getRowforObject(((DownloadLink) obj).getFilePackage());
                if (row >= 0 && isRowSelected(row2)) removeRowSelectionInterval(row2, row2);
            }
        }
        int column = realColumnAtPoint(e.getPoint());
        if (column == 0 && e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1) {
            Point p = this.getPointinCell(e.getPoint());
            if (p != null && p.getX() < 30) {
                Object element = getValueAt(row, 0);
                if (element != null && element instanceof FilePackage) {
                    if (e.isControlDown() && !e.isShiftDown()) {
                        toggleFilePackageExpand((FilePackage) element, EXPCOL_BOT);
                    } else if (e.isControlDown() && e.isShiftDown()) {
                        toggleFilePackageExpand((FilePackage) element, EXPCOL_TOP);
                    } else {
                        toggleFilePackageExpand((FilePackage) element, EXPCOL_CUR);
                    }
                    return;
                }
            }
        }
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (column == 0) {
                Point p = this.getPointinCell(e.getPoint());
                /* dont react here on collapse/expand icon */
                if (p != null && p.getX() < 30) return;
            }
            if ((e.getClickCount() == 1 && panel.isFilePackageInfoVisible(null)) || e.getClickCount() == 2) {
                Object element = getValueAt(row, 0);
                if (panel.isFilePackageInfoVisible(element) && e.getClickCount() == 2) {
                    panel.hideFilePackageInfo();
                } else if (element instanceof FilePackage) {
                    panel.showFilePackageInfo((FilePackage) element);
                } else {
                    panel.showDownloadLinkInfo((DownloadLink) element);
                }
            }
        }

    }

    public void toggleFilePackageExpand(FilePackage fp, byte mode) {
        boolean cur = !fp.getBooleanProperty(DownloadTable.PROPERTY_EXPANDED, false);
        switch (mode) {
        case EXPCOL_CUR:
            fp.setProperty(DownloadTable.PROPERTY_EXPANDED, cur);
            break;
        case EXPCOL_TOP: {
            ArrayList<FilePackage> packages = new ArrayList<FilePackage>(DownloadController.getInstance().getPackages());
            int indexfp = DownloadController.getInstance().indexOf(fp);
            for (int index = 0; index <= indexfp; index++) {
                FilePackage fp2 = packages.get(index);
                fp2.setProperty(DownloadTable.PROPERTY_EXPANDED, cur);
            }
        }
            break;
        case EXPCOL_BOT: {
            ArrayList<FilePackage> packages = new ArrayList<FilePackage>(DownloadController.getInstance().getPackages());
            int indexfp = DownloadController.getInstance().indexOf(fp);
            for (int index = indexfp; index < packages.size(); index++) {
                FilePackage fp2 = packages.get(index);
                fp2.setProperty(DownloadTable.PROPERTY_EXPANDED, cur);
            }
        }
            break;
        default:
            return;
        }
        panel.updateTableTask(DownloadLinksPanel.REFRESH_DATA_AND_STRUCTURE_CHANGED_FAST, null);
    }
}