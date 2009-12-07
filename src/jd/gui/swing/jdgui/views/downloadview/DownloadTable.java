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

package jd.gui.swing.jdgui.views.downloadview;

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
import java.util.HashMap;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import jd.config.Property;
import jd.controlling.DownloadController;
import jd.controlling.DownloadWatchDog;
import jd.event.ControlEvent;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.table.JDRowHighlighter;
import jd.gui.swing.components.table.JDTable;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.gui.swing.menu.Menu;
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
        prioDescs = new String[] { JDL.L("gui.treetable.tooltip.priority-1", "Low Priority"), JDL.L("gui.treetable.tooltip.priority0", "No Priority"), JDL.L("gui.treetable.tooltip.priority1", "High Priority"), JDL.L("gui.treetable.tooltip.priority2", "Higher Priority"), JDL.L("gui.treetable.tooltip.priority3", "Highest Priority") };
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
            TableAction test = new TableAction(panel, JDTheme.II("gui.images.delete", 16, 16), JDL.L("gui.table.contextmenu.delete", "entfernen") + " (" + alllinks.size() + ")", TableAction.DELETE, new Property("links", alllinks));
            test.actionPerformed(new ActionEvent(test, 0, ""));
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
        JMenuItem tmp;

        if (getValueAt(row, 0) == null) {
            clearSelection();
        }

        if (!isRowSelected(row) && e.getButton() == MouseEvent.BUTTON3) {
            clearSelection();
            if (getValueAt(row, 0) != null) this.addRowSelectionInterval(row, row);
        }
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {

            if (getValueAt(row, 0) == null) { return; }
            ArrayList<DownloadLink> alllinks = getAllSelectedDownloadLinks();
            ArrayList<DownloadLink> resumlinks = new ArrayList<DownloadLink>();
            ArrayList<DownloadLink> allnoncon = new ArrayList<DownloadLink>();
            ArrayList<DownloadLink> notrunning = new ArrayList<DownloadLink>();
            int links_enabled = 0;
            for (DownloadLink next : alllinks) {
                if (!next.getLinkStatus().isPluginActive()) notrunning.add(next);
                if (next.getLinkType() == DownloadLink.LINKTYPE_NORMAL) {
                    allnoncon.add(next);
                }
                if (next.isEnabled()) {
                    links_enabled++;
                }
                if (!next.getLinkStatus().isPluginActive() && next.getLinkStatus().isFailed()) {
                    resumlinks.add(next);
                }
            }
            int links_disabled = alllinks.size() - links_enabled;
            ArrayList<FilePackage> sfp = getSelectedFilePackages();
            Object obj = this.getModel().getValueAt(row, 0);
            JPopupMenu popup = new JPopupMenu();

            if (obj instanceof FilePackage || obj instanceof DownloadLink) {
                popup.add(tmp = new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.stopsign", 16, 16), JDL.L("gui.table.contextmenu.stopmark", "Stop sign"), TableAction.STOP_MARK, new Property("item", obj))));
                if (DownloadWatchDog.getInstance().isStopMark(obj)) tmp.setIcon(tmp.getDisabledIcon());
                popup.add(builddeletemenu(alllinks));

                popup.addSeparator();
            }

            popup.add(createExtrasMenu(obj));
            if (obj instanceof FilePackage) {
                popup.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.package_opened", 16, 16), JDL.L("gui.table.contextmenu.downloadDir", "Zielordner öffnen"), TableAction.DOWNLOAD_DIR, new Property("folder", new File(((FilePackage) obj).getDownloadDirectory())))));
                addSortItem(popup, col, sfp, JDL.L("gui.table.contextmenu.packagesort", "Paket sortieren") + " (" + sfp.size() + "), (" + getJDTableModel().getColumnName(col) + ")");
                popup.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.edit", 16, 16), JDL.L("gui.table.contextmenu.editpackagename", "Paketname ändern") + " (" + sfp.size() + ")", TableAction.EDIT_NAME, new Property("packages", sfp))));
                popup.add(tmp = new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.save", 16, 16), JDL.L("gui.table.contextmenu.editdownloadDir", "Zielordner ändern") + " (" + sfp.size() + ")", TableAction.EDIT_DIR, new Property("packages", sfp))));

                popup.addSeparator();
            }
            if (obj instanceof DownloadLink) {
                popup.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.package_opened", 16, 16), JDL.L("gui.table.contextmenu.downloadDir", "Zielordner öffnen"), TableAction.DOWNLOAD_DIR, new Property("folder", new File(((DownloadLink) obj).getFileOutput()).getParentFile()))));
                popup.add(tmp = new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.browse", 16, 16), JDL.L("gui.table.contextmenu.browseLink", "im Browser öffnen"), TableAction.DOWNLOAD_BROWSE_LINK, new Property("downloadlink", obj))));
                if (((DownloadLink) obj).getLinkType() != DownloadLink.LINKTYPE_NORMAL) tmp.setEnabled(false);
            }
            if (obj instanceof FilePackage || obj instanceof DownloadLink) {
                popup.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.dlc", 16, 16), JDL.L("gui.table.contextmenu.dlc", "DLC erstellen") + " (" + alllinks.size() + ")", TableAction.DOWNLOAD_DLC, new Property("links", alllinks))));
                popup.add(buildpriomenu(alllinks));
                popup.add(tmp = new JMenuItem(new TableAction(panel, JDTheme.II("gui.icons.copy", 16, 16), JDL.L("gui.table.contextmenu.copyPassword", "Copy Password") + " (" + alllinks.size() + ")", TableAction.DOWNLOAD_COPY_PASSWORD, new Property("links", alllinks))));
                if (DownloadLinksPanel.getPasswordSelectedLinks(alllinks).length() == 0) tmp.setEnabled(false);
                popup.add(tmp = new JMenuItem(new TableAction(panel, JDTheme.II("gui.icons.cut", 16, 16), JDL.L("gui.table.contextmenu.copyLink", "Copy URL") + " (" + allnoncon.size() + ")", TableAction.DOWNLOAD_COPY_URL, new Property("links", allnoncon))));
                if (allnoncon.size() == 0) tmp.setEnabled(false);
                popup.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.config.network_local", 16, 16), JDL.L("gui.table.contextmenu.check", "Check OnlineStatus") + " (" + alllinks.size() + ")", TableAction.CHECK, new Property("links", alllinks))));
                popup.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.newpackage", 16, 16), JDL.L("gui.table.contextmenu.newpackage", "In neues Paket verschieben") + " (" + alllinks.size() + ")", TableAction.NEW_PACKAGE, new Property("links", alllinks))));
                popup.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.password", 16, 16), JDL.L("gui.table.contextmenu.setdlpw", "Set download password") + " (" + alllinks.size() + ")", TableAction.SET_PW, new Property("links", alllinks))));
                popup.addSeparator();
                HashMap<String, Object> prop = new HashMap<String, Object>();
                prop.put("links", alllinks);
                prop.put("boolean", true);
                popup.add(tmp = new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.ok", 16, 16), JDL.L("gui.table.contextmenu.enable", "aktivieren") + " (" + links_disabled + ")", TableAction.DE_ACTIVATE, new Property("infos", prop))));
                if (links_disabled == 0) tmp.setEnabled(false);
                prop = new HashMap<String, Object>();
                prop.put("links", alllinks);
                prop.put("boolean", false);
                popup.add(tmp = new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.bad", 16, 16), JDL.L("gui.table.contextmenu.disable", "deaktivieren") + " (" + links_enabled + ")", TableAction.DE_ACTIVATE, new Property("infos", prop))));
                if (links_enabled == 0) tmp.setEnabled(false);
                popup.add(tmp = new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.resume", 16, 16), JDL.L("gui.table.contextmenu.resume", "fortsetzen") + " (" + resumlinks.size() + ")", TableAction.DOWNLOAD_RESUME, new Property("links", resumlinks))));
                if (resumlinks.size() == 0) tmp.setEnabled(false);
                if (notrunning.size() > 0 && DownloadWatchDog.getInstance().getDownloadStatus() == DownloadWatchDog.STATE.RUNNING) popup.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.next", 16, 16), JDL.L("gui.table.contextmenu.tryforce", "Force download") + " (" + notrunning.size() + ")", TableAction.FORCE_DOWNLOAD, new Property("links", notrunning))));
                popup.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.undo", 16, 16), JDL.L("gui.table.contextmenu.reset", "zurücksetzen") + " (" + alllinks.size() + ")", TableAction.DOWNLOAD_RESET, new Property("links", alllinks))));
                popup.addSeparator();
                propItem.setObject(obj);
                popup.add(propItem);
            }
            if (popup.getComponentCount() != 0) popup.show(this, point.x, point.y);
        }
    }

    private JMenu builddeletemenu(ArrayList<DownloadLink> alllinks) {
        int counter = 0;
        for (DownloadLink link : alllinks) {
            if (link.existsFile() && link.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) counter++;
        }
        JMenu pop = new JMenu(JDL.L("gui.table.contextmenu.delete", "entfernen"));
        pop.setIcon(JDTheme.II("gui.images.delete", 16, 16));
        pop.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.delete", 16, 16), JDL.L("gui.table.contextmenu.deletelist", "from list") + " (" + alllinks.size() + ")", TableAction.DELETE, new Property("links", alllinks))));
        pop.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.delete", 16, 16), JDL.L("gui.table.contextmenu.deletelistdisk", "from list and disk") + " (" + alllinks.size() + "/" + counter + ")", TableAction.DELETEFILE, new Property("links", alllinks))));
        return pop;
    }

    private JMenu buildpriomenu(ArrayList<DownloadLink> links) {
        JMenuItem tmp;
        JMenu prioPopup = new JMenu(JDL.L("gui.table.contextmenu.priority", "Priority") + " (" + links.size() + ")");
        Integer prio = null;
        if (links.size() == 1) prio = links.get(0).getPriority();
        prioPopup.setIcon(JDTheme.II("gui.images.priority0", 16, 16));
        HashMap<String, Object> prop = null;
        for (int i = 3; i >= -1; i--) {
            prop = new HashMap<String, Object>();
            prop.put("links", links);
            prop.put("prio", Integer.valueOf(i));
            prioPopup.add(tmp = new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.priority" + i, 16, 16), prioDescs[i + 1], TableAction.DOWNLOAD_PRIO, new Property("infos", prop))));

            if (prio != null && i == prio) {
                tmp.setEnabled(false);
                tmp.setIcon(JDTheme.II("gui.images.priority" + i, 16, 16));
            } else
                tmp.setEnabled(true);
        }
        return prioPopup;
    }

    private JMenu createExtrasMenu(Object obj) {
        if (!(obj instanceof FilePackage || obj instanceof DownloadLink)) return null;
        JMenu pluginPopup = new JMenu(JDL.L("gui.table.contextmenu.extrasSubmenu", "Extras"));
        pluginPopup.setIcon(JDTheme.II("gui.images.config.packagemanager", 16, 16));
        ArrayList<MenuAction> entries = new ArrayList<MenuAction>();
        JDUtilities.getController().fireControlEventDirect(new ControlEvent(obj, ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU, entries));
        if (entries != null && entries.size() > 0) {
            for (MenuAction next : entries) {
                JMenuItem mi = Menu.getJMenuItem(next);
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