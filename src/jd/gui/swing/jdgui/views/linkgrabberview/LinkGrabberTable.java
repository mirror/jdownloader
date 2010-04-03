//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.views.linkgrabberview;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DropMode;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import jd.config.Property;
import jd.controlling.LinkGrabberController;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.JExtCheckBoxMenuItem;
import jd.gui.swing.components.table.JDRowHighlighter;
import jd.gui.swing.components.table.JDTable;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.nutils.OSDetector;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.MenuScroller;
import jd.utils.locale.JDL;

class PropMenuItem extends JMenuItem implements ActionListener {

    private static final long serialVersionUID = 6328630034846759725L;
    private Object obj;
    private LinkGrabberPanel linkgrabber;

    public PropMenuItem(LinkGrabberPanel linkgrabber) {
        super(JDL.L("gui.table.contextmenu.prop", "Properties"));
        this.setIcon(JDTheme.II("gui.images.config.tip", 16, 16));
        this.linkgrabber = linkgrabber;
        this.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        if (obj != null && obj instanceof LinkGrabberFilePackage) linkgrabber.showFilePackageInfo((LinkGrabberFilePackage) obj);
    }

    public void setObject(Object obj) {
        this.obj = obj;
    }
}

public class LinkGrabberTable extends JDTable implements MouseListener, KeyListener {

    public final static byte EXPCOL_TOP = 0;
    public final static byte EXPCOL_CUR = 1;
    public final static byte EXPCOL_BOT = 2;
    private static final long serialVersionUID = 1L;

    protected LinkGrabberPanel linkgrabber;

    private String[] prioDescs;

    private PropMenuItem propItem;

    public static final String PROPERTY_EXPANDED = "lg_expanded";
    public static final String PROPERTY_USEREXPAND = "lg_userexpand";

    public LinkGrabberPanel getLinkGrabber() {
        return linkgrabber;
    }

    public LinkGrabberTable(LinkGrabberPanel linkgrabber) {
        super(new LinkGrabberJTableModel("linkgrabber"));
        this.linkgrabber = linkgrabber;
        addMouseListener(this);
        addKeyListener(this);
        if (JDUtilities.getJavaVersion() >= 1.6) {
            setDropMode(DropMode.USE_SELECTION);
        }
        setDragEnabled(true);
        setTransferHandler(new LinkGrabberTableTransferHandler(this));
        setColumnSelectionAllowed(false);
        setRowSelectionAllowed(true);
        addPackageHighlighter();
        addDisabledHighlighter();
        addExistsHighlighter();
        prioDescs = new String[] { JDL.L("gui.treetable.tooltip.priority-1", "Low Priority"), JDL.L("gui.treetable.tooltip.priority0", "Default Priority"), JDL.L("gui.treetable.tooltip.priority1", "High Priority"), JDL.L("gui.treetable.tooltip.priority2", "Higher Priority"), JDL.L("gui.treetable.tooltip.priority3", "Highest Priority") };
        propItem = new PropMenuItem(linkgrabber);
    }

    public void fireTableChanged() {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                final Rectangle viewRect = linkgrabber.getScrollPane().getViewport().getViewRect();
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
        }.start();
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        boolean ret = super.processKeyBinding(ks, e, condition, pressed);
        if (ks.getKeyCode() == KeyEvent.VK_ENTER && !ks.isOnKeyRelease()) {
            ActionController.getToolBarAction("action.linkgrabber.addall").actionPerformed(null);
        }
        return ret;
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
        ArrayList<LinkGrabberFilePackage> fps = getSelectedFilePackages();
        for (LinkGrabberFilePackage filePackage : fps) {
            for (DownloadLink dl : filePackage.getDownloadLinks()) {
                if (!links.contains(dl)) links.add(dl);
            }
        }
        return links;
    }

    public ArrayList<LinkGrabberFilePackage> getSelectedFilePackages() {
        int[] rows = getSelectedRows();
        ArrayList<LinkGrabberFilePackage> ret = new ArrayList<LinkGrabberFilePackage>();
        for (int row : rows) {
            Object element = this.getModel().getValueAt(row, 0);
            if (element != null && element instanceof LinkGrabberFilePackage) {
                ret.add((LinkGrabberFilePackage) element);
            }
        }
        return ret;
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent arg0) {
    }

    public void mouseExited(MouseEvent arg0) {
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
                int row2 = getJDTableModel().getRowforObject(LinkGrabberController.getInstance().getFPwithLink((DownloadLink) obj));
                if (row >= 0 && isRowSelected(row2)) removeRowSelectionInterval(row2, row2);
            }
        }
        int column = realColumnAtPoint(e.getPoint());
        if (column == 0 && e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1) {
            Point p = this.getPointinCell(e.getPoint());
            if (p != null && p.getX() < 30) {
                Object element = getValueAt(row, 0);
                if (element != null && element instanceof LinkGrabberFilePackage) {
                    if (e.isControlDown() && !e.isShiftDown()) {
                        toggleFilePackageExpand((LinkGrabberFilePackage) element, EXPCOL_BOT);
                    } else if (e.isControlDown() && e.isShiftDown()) {
                        toggleFilePackageExpand((LinkGrabberFilePackage) element, EXPCOL_TOP);
                    } else {
                        toggleFilePackageExpand((LinkGrabberFilePackage) element, EXPCOL_CUR);
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
            if ((e.getClickCount() == 1 && linkgrabber.isFilePackageInfoVisible(null)) || e.getClickCount() == 2) {
                Object element = getValueAt(row, 0);
                if (linkgrabber.isFilePackageInfoVisible(element) && e.getClickCount() == 2) {
                    linkgrabber.hideFilePackageInfo();
                } else if (element != null && element instanceof LinkGrabberFilePackage) {
                    linkgrabber.showFilePackageInfo((LinkGrabberFilePackage) element);
                } else {
                    linkgrabber.showFilePackageInfo(LinkGrabberController.getInstance().getFPwithLink((DownloadLink) element));
                }
            }
        }
    }

    public void toggleFilePackageExpand(LinkGrabberFilePackage fp, byte mode) {
        boolean cur = !fp.getBooleanProperty(PROPERTY_EXPANDED, false);
        switch (mode) {
        case EXPCOL_CUR:
            fp.setProperty(PROPERTY_EXPANDED, cur);
            fp.setProperty(PROPERTY_USEREXPAND, true);
            break;
        case EXPCOL_TOP: {
            ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>(LinkGrabberController.getInstance().getPackages());
            int indexfp = LinkGrabberController.getInstance().indexOf(fp);
            for (int index = 0; index <= indexfp; index++) {
                LinkGrabberFilePackage fp2 = packages.get(index);
                fp2.setProperty(PROPERTY_EXPANDED, cur);
                fp2.setProperty(PROPERTY_USEREXPAND, true);
            }
        }
            break;
        case EXPCOL_BOT: {
            ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>(LinkGrabberController.getInstance().getPackages());
            int indexfp = LinkGrabberController.getInstance().indexOf(fp);
            for (int index = indexfp; index < packages.size(); index++) {
                LinkGrabberFilePackage fp2 = packages.get(index);
                fp2.setProperty(PROPERTY_EXPANDED, cur);
                fp2.setProperty(PROPERTY_USEREXPAND, true);
            }
        }
            break;
        default:
            return;
        }
        linkgrabber.fireTableChanged();
    }

    public void mousePressed(MouseEvent e) {
        /* nicht auf headerclicks reagieren */
        if (e.getSource() != this) return;
        Point point = e.getPoint();
        int row = rowAtPoint(point);
        int col = realColumnAtPoint(point);

        if (getValueAt(row, 0) == null) {
            clearSelection();
            if (e.getButton() == MouseEvent.BUTTON3) {
                if (LinkGrabberController.getInstance().getFilterPackage().size() > 0 || LinkGrabberController.getInstance().size() > 0) {
                    JPopupMenu popup = new JPopupMenu();
                    popup.add(new JMenuItem(ActionController.getToolBarAction("action.linkgrabber.addall")));
                    popup.add(builddeletemenu(null));
                    addExtMenu(popup);
                    if (popup.getComponentCount() != 0) popup.show(this, point.x, point.y);
                }
            }
            return;
        }

        if (!isRowSelected(row) && e.getButton() == MouseEvent.BUTTON3) {
            clearSelection();
            if (getValueAt(row, 0) != null) this.addRowSelectionInterval(row, row);
        }
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            if (getValueAt(row, 0) == null) { return; }
            ArrayList<DownloadLink> alllinks = getAllSelectedDownloadLinks();
            int links_enabled = 0;
            for (DownloadLink next : alllinks) {
                if (next.isEnabled()) {
                    links_enabled++;
                }
            }
            int links_disabled = alllinks.size() - links_enabled;
            ArrayList<LinkGrabberFilePackage> sfp = getSelectedFilePackages();
            Object obj = this.getModel().getValueAt(row, 0);
            JPopupMenu popup = new JPopupMenu();

            if (obj instanceof LinkGrabberFilePackage || obj instanceof DownloadLink) {
                popup.add(new JMenuItem(ActionController.getToolBarAction("action.linkgrabber.addall")));
                if (sfp.size() > 0) {
                    popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.taskpanes.linkgrabber", 16, 16), JDL.L("gui.linkgrabberv2.lg.continueselected", "Continue with selected package(s)") + " (" + sfp.size() + ")", LinkGrabberTableAction.ADD_SELECTED_PACKAGES)));
                } else {
                    popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.taskpanes.linkgrabber", 16, 16), JDL.L("gui.linkgrabberv2.lg.continueselectedlinks", "Continue with selected link(s)") + " (" + alllinks.size() + ")", LinkGrabberTableAction.ADD_SELECTED_LINKS, new Property("links", alllinks))));
                }
                popup.add(builddeletemenu(alllinks));
                popup.addSeparator();
            }
            if (obj instanceof LinkGrabberFilePackage) {
                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.newpackage", 16, 16), JDL.L("gui.linkgrabberv2.splithoster", "Split by hoster") + " (" + sfp.size() + ")", LinkGrabberTableAction.SPLIT_HOSTER)));
                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.add_package", 16, 16), JDL.L("gui.table.contextmenu.editdownloadDir", "Edit Directory") + " (" + sfp.size() + ")", LinkGrabberTableAction.EDIT_DIR)));
            }
            if (obj instanceof LinkGrabberFilePackage || obj instanceof DownloadLink) {
                addSortItem(popup, col, sfp, JDL.L("gui.table.contextmenu.packagesort", "Sort Packages") + " (" + sfp.size() + "), (" + getJDTableModel().getColumnName(col) + ")");
                popup.addSeparator();
                if (obj instanceof DownloadLink) {
                    popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.icons.cut", 16, 16), JDL.L("gui.table.contextmenu.copyLink", "Copy URL") + " (" + alllinks.size() + ")", LinkGrabberTableAction.COPY_LINK, new Property("links", alllinks))));
                    JMenuItem tmp;
                    popup.add(tmp = new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.browse", 16, 16), JDL.L("gui.table.contextmenu.browselink", "Open in browser"), LinkGrabberTableAction.BROWSE_LINK, new Property("link", obj))));
                    if (((DownloadLink) obj).getLinkType() != DownloadLink.LINKTYPE_NORMAL) tmp.setEnabled(false);
                }
                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.config.network_local", 16, 16), JDL.L("gui.table.contextmenu.check", "Check Online Status") + " (" + alllinks.size() + ")", LinkGrabberTableAction.CHECK_LINK, new Property("links", alllinks))));
                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.dlc", 16, 16), JDL.L("gui.table.contextmenu.dlc", "Create DLC") + " (" + alllinks.size() + ")", LinkGrabberTableAction.SAVE_DLC, new Property("links", alllinks))));
                popup.add(buildpriomenu(alllinks));
                addExtMenu(popup);
                Set<String> hoster = DownloadLink.getHosterList(alllinks);
                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.addselected", 16, 16), JDL.L("gui.linkgrabberv2.onlyselectedhoster", "Keep only selected Hoster") + " (" + hoster.size() + ")", LinkGrabberTableAction.SELECT_HOSTER, new Property("hoster", hoster))));
                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.newpackage", 16, 16), JDL.L("gui.table.contextmenu.newpackage2", "Move to new package") + " (" + alllinks.size() + ")", LinkGrabberTableAction.NEW_PACKAGE, new Property("links", alllinks))));
                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.newpackage", 16, 16), JDL.L("gui.table.contextmenu.mergepackage2", "Merge to one package") + " (" + alllinks.size() + ")", LinkGrabberTableAction.MERGE_PACKAGE, new Property("links", alllinks))));
                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.password", 16, 16), JDL.L("gui.table.contextmenu.setdlpw", "Set download password") + " (" + alllinks.size() + ")", LinkGrabberTableAction.SET_PW, new Property("links", alllinks))));
                popup.addSeparator();
                HashMap<String, Object> prop = new HashMap<String, Object>();
                prop.put("links", alllinks);
                prop.put("boolean", true);
                if (links_disabled > 0) popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.ok", 16, 16), JDL.L("gui.table.contextmenu.enable", "Enable") + " (" + links_disabled + ")", LinkGrabberTableAction.DE_ACTIVATE, new Property("infos", prop))));
                prop = new HashMap<String, Object>();
                prop.put("links", alllinks);
                prop.put("boolean", false);
                if (links_enabled > 0) popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.bad", 16, 16), JDL.L("gui.table.contextmenu.disable", "Disable") + " (" + links_enabled + ")", LinkGrabberTableAction.DE_ACTIVATE, new Property("infos", prop))));
            }
            if (obj instanceof LinkGrabberFilePackage) {
                popup.addSeparator();
                propItem.setObject(obj);
                popup.add(propItem);
            }
            if (popup.getComponentCount() != 0) popup.show(this, point.x, point.y);
        }
    }

    private JMenu builddeletemenu(ArrayList<DownloadLink> alllinks) {
        JMenu popup = new JMenu(JDL.L("gui.table.contextmenu.remove", "Remove"));
        popup.setIcon(JDTheme.II("gui.images.delete", 16, 16));
        if (alllinks != null && alllinks.size() > 0) {
            popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.delete", 16, 16), JDL.L("gui.table.contextmenu.remove", "Remove") + " (" + alllinks.size() + ")", LinkGrabberTableAction.DELETE, new Property("links", alllinks))));
            popup.addSeparator();
        }
        popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.removefailed", 16, 16), JDL.L("gui.linkgrabberv2.lg.rmoffline", "Remove all Offline"), LinkGrabberTableAction.DELETE_OFFLINE)));
        popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.delete", 16, 16), JDL.L("gui.linkgrabberv2.lg.rmdups", "Remove all Duplicates"), LinkGrabberTableAction.DELETE_DUPS)));
        popup.addSeparator();
        popup.add(new JMenuItem(ActionController.getToolBarAction("action.linkgrabber.clearlist")));
        return popup;
    }

    private void addExtMenu(JPopupMenu popup) {
        JExtCheckBoxMenuItem tmp;
        JMenu men = new JMenu(JDL.L("gui.table.contextmenu.filetype", "Filter"));
        ArrayList<String> extensions = linkgrabber.getExtensions();
        if (extensions.size() == 0) return;
        HashSet<String> fl = LinkGrabberController.getInstance().getExtensionFilter();
        men.setIcon(JDTheme.II("gui.images.filter", 16, 16));
        synchronized (fl) {
            for (String e : extensions) {
                men.add(tmp = new JExtCheckBoxMenuItem(new LinkGrabberTableAction(linkgrabber, null, "*." + e, LinkGrabberTableAction.EXT_FILTER, new Property("extension", e))));
                tmp.setHideOnClick(false);
                tmp.setSelected(!fl.contains(e));
            }
        }
        MenuScroller.setScrollerFor(men);
        popup.add(men);
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
            prioPopup.add(tmp = new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.priority" + i, 16, 16), prioDescs[i + 1], LinkGrabberTableAction.DOWNLOAD_PRIO, new Property("infos", prop))));

            if (prio != null && i == prio) {
                tmp.setEnabled(false);
                tmp.setIcon(JDTheme.II("gui.images.priority" + i, 16, 16));
            } else
                tmp.setEnabled(true);
        }
        return prioPopup;
    }

    private void addDisabledHighlighter() {
        this.addJDRowHighlighter(new JDRowHighlighter(JDTheme.C("gui.color.downloadlist.row_link_disabled", "adadad", 100)) {
            @Override
            public boolean doHighlight(Object o) {
                return (o != null && o instanceof DownloadLink && !((DownloadLink) o).isEnabled());
            }
        });
    }

    private void addPackageHighlighter() {
        this.addJDRowHighlighter(new JDRowHighlighter(UIManager.getColor("TableHeader.background")) {
            @Override
            public boolean doHighlight(Object o) {
                return (o != null && o instanceof LinkGrabberFilePackage);
            }
        });
    }

    private void addExistsHighlighter() {
        this.addJDRowHighlighter(new JDRowHighlighter(JDTheme.C("gui.color.linkgrabber.error_exists", "ff7f00", 120)) {
            @Override
            public boolean doHighlight(Object o) {
                return (o != null && o instanceof DownloadLink && ((DownloadLink) o).getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS));
            }
        });
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DELETE || (OSDetector.isMac() && e.getKeyCode() == KeyEvent.VK_BACK_SPACE)) {
            ArrayList<DownloadLink> alllinks = getAllSelectedDownloadLinks();
            if (alllinks.size() == 0) return;
            LinkGrabberTableAction test = new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.delete", 16, 16), JDL.L("gui.table.contextmenu.delete", "Delete") + " (" + alllinks.size() + ")", LinkGrabberTableAction.DELETE, new Property("links", alllinks));
            test.actionPerformed(new ActionEvent(test, 0, ""));
        }
    }

    public void keyPressed(KeyEvent arg0) {
    }

    public void keyTyped(KeyEvent arg0) {
    }

}
