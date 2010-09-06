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

package jd.gui.swing.jdgui.views.linkgrabber;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.DropMode;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import jd.controlling.LinkGrabberController;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.JExtCheckBoxMenuItem;
import jd.gui.swing.components.table.JDRowHighlighter;
import jd.gui.swing.components.table.JDTable;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.views.downloads.contextmenu.CopyURLAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.CreateDLCAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.DisableAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.EnableAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.OpenInBrowserAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.PriorityAction;
import jd.gui.swing.jdgui.views.downloads.contextmenu.SetPasswordAction;
import jd.gui.swing.jdgui.views.linkgrabber.contextmenu.CheckStatusAction;
import jd.gui.swing.jdgui.views.linkgrabber.contextmenu.ContinueLinksAction;
import jd.gui.swing.jdgui.views.linkgrabber.contextmenu.ContinuePackagesAction;
import jd.gui.swing.jdgui.views.linkgrabber.contextmenu.DeleteAction;
import jd.gui.swing.jdgui.views.linkgrabber.contextmenu.FilterAction;
import jd.gui.swing.jdgui.views.linkgrabber.contextmenu.NewPackageAction;
import jd.gui.swing.jdgui.views.linkgrabber.contextmenu.PackageDirectoryAction;
import jd.gui.swing.jdgui.views.linkgrabber.contextmenu.SelectHostAction;
import jd.gui.swing.jdgui.views.linkgrabber.contextmenu.SplitHosterAction;
import jd.nutils.OSDetector;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLinkInfoCache;
import jd.plugins.LinkGrabberFilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.MenuScroller;
import jd.utils.locale.JDL;

class PropMenuItem extends JMenuItem implements ActionListener {

    private static final long serialVersionUID = 6328630034846759725L;
    private Object            obj;
    private LinkGrabberPanel  linkgrabber;

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

    public final static byte   EXPCOL_TOP       = 0;
    public final static byte   EXPCOL_CUR       = 1;
    public final static byte   EXPCOL_BOT       = 2;
    private static final long  serialVersionUID = 1L;

    protected LinkGrabberPanel linkgrabber;

    private PropMenuItem       propItem;

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
        propItem = new PropMenuItem(linkgrabber);
    }

    public void fireTableChanged() {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                DownloadLinkInfoCache.reset();
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
        boolean cur = !fp.getBooleanProperty(LinkGrabberController.PROPERTY_EXPANDED, false);
        switch (mode) {
        case EXPCOL_CUR:
            fp.setProperty(LinkGrabberController.PROPERTY_EXPANDED, cur);
            fp.setProperty(LinkGrabberController.PROPERTY_USEREXPAND, true);
            break;
        case EXPCOL_TOP: {
            ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>(LinkGrabberController.getInstance().getPackages());
            int indexfp = LinkGrabberController.getInstance().indexOf(fp);
            for (int index = 0; index <= indexfp; index++) {
                LinkGrabberFilePackage fp2 = packages.get(index);
                fp2.setProperty(LinkGrabberController.PROPERTY_EXPANDED, cur);
                fp2.setProperty(LinkGrabberController.PROPERTY_USEREXPAND, true);
            }
        }
            break;
        case EXPCOL_BOT: {
            ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>(LinkGrabberController.getInstance().getPackages());
            int indexfp = LinkGrabberController.getInstance().indexOf(fp);
            for (int index = indexfp; index < packages.size(); index++) {
                LinkGrabberFilePackage fp2 = packages.get(index);
                fp2.setProperty(LinkGrabberController.PROPERTY_EXPANDED, cur);
                fp2.setProperty(LinkGrabberController.PROPERTY_USEREXPAND, true);
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

        Object obj = this.getModel().getValueAt(row, 0);

        if (obj == null) {
            clearSelection();
            if (e.getButton() == MouseEvent.BUTTON3) {
                if (LinkGrabberController.getInstance().getFilterPackage().size() > 0 || LinkGrabberController.getInstance().size() > 0) {
                    JPopupMenu popup = new JPopupMenu();

                    popup.add(new JMenuItem(ActionController.getToolBarAction("action.linkgrabber.addall")));
                    popup.add(createDeleteMenu(null));
                    popup.add(createFilterMenu());

                    popup.show(this, point.x, point.y);
                }
            }
            return;
        }

        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            if (!isRowSelected(row)) {
                clearSelection();
                addRowSelectionInterval(row, row);
            }

            ArrayList<DownloadLink> alllinks = getAllSelectedDownloadLinks();
            ArrayList<LinkGrabberFilePackage> sfp = getSelectedFilePackages();

            JPopupMenu popup = new JPopupMenu();

            popup.add(ActionController.getToolBarAction("action.linkgrabber.addall"));
            if (sfp.size() > 0) {
                popup.add(new ContinuePackagesAction(sfp));
            } else {
                popup.add(new ContinueLinksAction(alllinks));
            }
            popup.add(createDeleteMenu(alllinks));
            popup.addSeparator();

            if (obj instanceof LinkGrabberFilePackage) {
                popup.add(new SplitHosterAction(sfp));

                final JDTableColumn column = this.getJDTableModel().getJDTableColumn(col);
                if (column.isSortable(obj)) {
                    this.getDefaultSortMenuItem().set(column, obj, JDL.L("gui.table.contextmenu.packagesort", "Sort Packages") + " (" + sfp.size() + "), (" + getJDTableModel().getColumnName(col) + ")");
                    popup.add(this.getDefaultSortMenuItem());
                }

                popup.add(new PackageDirectoryAction(sfp));
            } else if (obj instanceof DownloadLink) {
                popup.add(new CopyURLAction(alllinks));
                popup.add(new OpenInBrowserAction(alllinks));
            }
            popup.addSeparator();

            popup.add(new CheckStatusAction(alllinks));
            popup.add(new CreateDLCAction(alllinks));
            popup.add(createPrioMenu(alllinks));
            popup.add(createFilterMenu());
            popup.add(new SelectHostAction(DownloadLink.getHosterList(alllinks)));
            popup.add(new NewPackageAction(alllinks));
            popup.add(new SetPasswordAction(alllinks));
            popup.addSeparator();

            popup.add(new EnableAction(alllinks));
            popup.add(new DisableAction(alllinks));

            if (obj instanceof LinkGrabberFilePackage) {
                popup.addSeparator();
                propItem.setObject(obj);
                popup.add(propItem);
            }

            popup.show(this, point.x, point.y);
        }
    }

    private JMenu createDeleteMenu(ArrayList<DownloadLink> alllinks) {
        JMenu popup = new JMenu(JDL.L("gui.table.contextmenu.remove", "Remove"));
        popup.setIcon(JDTheme.II("gui.images.delete", 16, 16));
        if (alllinks != null && alllinks.size() > 0) {
            popup.add(new DeleteAction(alllinks));
            popup.addSeparator();
        }
        popup.add(ActionController.getToolBarAction("action.remove_offline"));
        popup.add(ActionController.getToolBarAction("action.remove_dupes"));
        popup.addSeparator();
        popup.add(ActionController.getToolBarAction("action.linkgrabber.clearlist"));
        return popup;
    }

    private JMenu createFilterMenu() {
        JExtCheckBoxMenuItem tmp;

        JMenu men = new JMenu(JDL.L("gui.table.contextmenu.filetype", "Filter"));
        men.setIcon(JDTheme.II("gui.images.filter", 16, 16));

        ArrayList<String> extensions = linkgrabber.getExtensions();
        if (extensions.isEmpty()) {
            men.setEnabled(false);
        } else {
            HashSet<String> fl = LinkGrabberController.getInstance().getExtensionFilter();
            synchronized (fl) {
                for (String extension : extensions) {
                    men.add(tmp = new JExtCheckBoxMenuItem(new FilterAction(extension)));
                    tmp.setHideOnClick(false);
                    tmp.setSelected(!fl.contains(extension));
                }
            }
            MenuScroller.setScrollerFor(men);
        }
        return men;
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
            if (alllinks.isEmpty()) return;
            new DeleteAction(alllinks).actionPerformed(null);
        }
    }

    public void keyPressed(KeyEvent arg0) {
    }

    public void keyTyped(KeyEvent arg0) {
    }

}
