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

package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.DropMode;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.LinkGrabberController;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.gui.skins.simple.components.DownloadView.DownloadLinkRowHighlighter;
import jd.gui.skins.simple.components.DownloadView.DownloadTable;
import jd.gui.skins.simple.components.DownloadView.JColumnControlButton;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.PainterHighlighter;
import org.jdesktop.swingx.table.TableColumnExt;

public class LinkGrabberTable extends JXTable implements MouseListener, MouseMotionListener, KeyListener {

    private static final long serialVersionUID = 1L;
    private LinkGrabberJTableModel model;
    protected LinkGrabberPanel linkgrabber;
    private LinkGrabberTableRenderer cellRenderer;
    private TableColumnExt[] cols;
    private String[] prioDescs;

    public static final String PROPERTY_EXPANDED = "lg_expanded";
    public static final String PROPERTY_USEREXPAND = "lg_userexpand";

    // public static final String PROPERTY_SELECTED = "lg_selected";

    public LinkGrabberPanel getLinkGrabber() {
        return linkgrabber;
    }

    public LinkGrabberTable(LinkGrabberJTableModel treeModel, final LinkGrabberPanel linkgrabber) {
        super(treeModel);
        this.linkgrabber = linkgrabber;
        SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME);
        cellRenderer = new LinkGrabberTableRenderer(this);
        model = treeModel;
        createColumns();
        this.setShowGrid(false);
        this.setShowHorizontalLines(false);
        this.setShowVerticalLines(false);
        getTableHeader().setReorderingAllowed(false);
        getTableHeader().setResizingAllowed(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setEditable(false);
        this.setSortable(false);
        setAutoscrolls(false);
        setColumnControlVisible(true);
        setColumnControl(new JColumnControlButton(this));
        addMouseListener(this);
        addKeyListener(this);
        addMouseMotionListener(this);

        if (JDUtilities.getJavaVersion() >= 1.6) {
            // setDropMode(DropMode.ON_OR_INSERT_ROWS); /*muss noch geschaut
            // werden wie man das genau macht*/
            setDropMode(DropMode.USE_SELECTION);
        }
        setDragEnabled(true);
        setTransferHandler(new LinkGrabberTableTransferHandler(this));
        this.getTableHeader().addMouseListener(this);
        UIManager.put("Table.focusCellHighlightBorder", null);
        setHighlighters(new Highlighter[] {});
        addPackageHighlighter();

        addDisabledHighlighter();

        addExistsHighlighter();
        setTransferHandler(new LinkGrabberTableTransferHandler(this));

        prioDescs = new String[] { JDL.L("gui.treetable.tooltip.priority-1", "Low Priority"), JDL.L("gui.treetable.tooltip.priority0", "No Priority"), JDL.L("gui.treetable.tooltip.priority1", "High Priority"), JDL.L("gui.treetable.tooltip.priority2", "Higher Priority"), JDL.L("gui.treetable.tooltip.priority3", "Highest Priority") };
    }

    public TableCellRenderer getCellRenderer(int row, int col) {
        return cellRenderer;
    }

    public void fireTableChanged() {
        new GuiRunnable<Object>() {
            // @Override
            public Object runSave() {
                final Rectangle viewRect = linkgrabber.getScrollPane().getViewport().getViewRect();
                int[] rows = getSelectedRows();
                final ArrayList<Object> selected = new ArrayList<Object>();
                for (int row : rows) {
                    Object elem = model.getValueAt(row, 0);
                    if (elem != null) selected.add(elem);
                }
                model.fireTableStructureChanged();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        for (Object obj : selected) {
                            int row = model.getRowforObject(obj);
                            if (row != -1) addRowSelectionInterval(row, row);
                        }
                        scrollRectToVisible(viewRect);
                    }
                });
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

    public void mouseDragged(MouseEvent arg0) {
    }

    public void mouseMoved(MouseEvent e) {
    }

    private int getRealcolumnAtPoint(int x) {
        /*
         * diese funktion gibt den echten columnindex zurück, da durch
         * an/ausschalten dieser anders kann
         */
        int c = getColumnModel().getColumnIndexAtX(x);
        if (c == -1) return -1;
        return getColumnModel().getColumn(c).getModelIndex();
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getSource() == this.getTableHeader()) {
            int col = getRealcolumnAtPoint(e.getX());
            LinkGrabberTableAction test = new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.sort", 16, 16), JDL.L("gui.table.contextmenu.packagesort", "Paket sortieren"), LinkGrabberTableAction.SORT_ALL, new Property("col", col));
            test.actionPerformed(new ActionEvent(test, 0, ""));
            return;
        }

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
        int column = getRealcolumnAtPoint(e.getX());
        if (column == 0 && e.getButton() == MouseEvent.BUTTON1 && e.getX() < 20 && e.getClickCount() == 1) {
            Object element = this.getModel().getValueAt(row, 0);
            if (element != null && element instanceof LinkGrabberFilePackage) {
                toggleFilePackageExpand((LinkGrabberFilePackage) element);
            }
        } else if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
            Object element = this.getModel().getValueAt(row, 0);
            if (element != null && element instanceof LinkGrabberFilePackage) {
                linkgrabber.showFilePackageInfo((LinkGrabberFilePackage) element);
            } else {
                linkgrabber.showFilePackageInfo(LinkGrabberController.getInstance().getFPwithLink((DownloadLink) element));
            }
        }
    }

    public void toggleFilePackageExpand(LinkGrabberFilePackage fp) {
        fp.setProperty(PROPERTY_EXPANDED, !fp.getBooleanProperty(PROPERTY_EXPANDED, false));
        fp.setProperty(PROPERTY_USEREXPAND, true);
        linkgrabber.fireTableChanged(true);
    }

    public void mousePressed(MouseEvent e) {
        /* nicht auf headerclicks reagieren */
        if (e.getSource() != this) return;
        Point point = e.getPoint();
        int row = rowAtPoint(point);
        int col = getRealcolumnAtPoint(e.getX());

        if (model.getValueAt(row, 0) == null) {
            clearSelection();
            if (e.getButton() == MouseEvent.BUTTON3) {
                JPopupMenu popup = new JPopupMenu();
                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.add_all", 16, 16), JDL.L("gui.linkgrabberv2.lg.addall", "Add all packages"), LinkGrabberTableAction.ADD_ALL)));
                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.removefailed", 16, 16), JDL.L("gui.linkgrabberv2.lg.rmoffline", "Remove all Offline"), LinkGrabberTableAction.DELETE_OFFLINE)));
                popup.add(buildExtMenu());
                if (popup.getComponentCount() != 0) popup.show(this, point.x, point.y);
            }
            return;
        }

        if (!isRowSelected(row) && e.getButton() == MouseEvent.BUTTON3) {
            clearSelection();
            if (model.getValueAt(row, 0) != null) this.addRowSelectionInterval(row, row);
        }
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            if (model.getValueAt(row, 0) == null) { return; }
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
                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.add_all", 16, 16), JDL.L("gui.linkgrabberv2.lg.addall", "Add all packages"), LinkGrabberTableAction.ADD_ALL)));
                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.removefailed", 16, 16), JDL.L("gui.linkgrabberv2.lg.rmoffline", "Remove all Offline"), LinkGrabberTableAction.DELETE_OFFLINE)));

                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.delete", 16, 16), JDL.L("gui.table.contextmenu.delete", "entfernen") + " (" + alllinks.size() + ")", LinkGrabberTableAction.DELETE, new Property("links", alllinks))));
                if (sfp.size() > 0) {
                    popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.taskpanes.linkgrabber", 16, 16), JDL.L("gui.linkgrabberv2.lg.addselected", "Add selected package(s)") + " (" + sfp.size() + ")", LinkGrabberTableAction.ADD_SELECTED_PACKAGES)));
                } else {
                    popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.taskpanes.linkgrabber", 16, 16), JDL.L("gui.linkgrabberv2.lg.addselectedlinks", "Add selected link(s)") + " (" + alllinks.size() + ")", LinkGrabberTableAction.ADD_SELECTED_LINKS, new Property("links", alllinks))));
                }
                popup.add(new JSeparator());
            }
            if (obj instanceof LinkGrabberFilePackage) {
                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.newpackage", 16, 16), JDL.L("gui.linkgrabberv2.splithoster", "Split by hoster") + " (" + sfp.size() + ")", LinkGrabberTableAction.SPLIT_HOSTER)));
                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.add_package", 16, 16), JDL.L("gui.table.contextmenu.editdownloadDir", "Zielordner ändern") + " (" + sfp.size() + ")", LinkGrabberTableAction.EDIT_DIR)));
                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.sort", 16, 16), JDL.L("gui.table.contextmenu.packagesort", "Paket sortieren") + " (" + sfp.size() + "), (" + this.getModel().getColumnName(col) + ")", LinkGrabberTableAction.SORT, new Property("col", col))));
                popup.add(new JSeparator());
            }
            if (obj instanceof LinkGrabberFilePackage || obj instanceof DownloadLink) {
                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.dlc", 16, 16), JDL.L("gui.table.contextmenu.dlc", "DLC erstellen") + " (" + alllinks.size() + ")", LinkGrabberTableAction.SAVE_DLC, new Property("links", alllinks))));
                popup.add(buildpriomenu(alllinks));
                popup.add(buildExtMenu());
                Set<String> hoster = linkgrabber.getHosterList(alllinks);
                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.addselected", 16, 16), JDL.L("gui.linkgrabberv2.onlyselectedhoster", "Keep only selected Hoster") + " (" + hoster.size() + ")", LinkGrabberTableAction.SELECT_HOSTER, new Property("hoster", hoster))));
                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.newpackage", 16, 16), JDL.L("gui.table.contextmenu.newpackage2", "Move to new package") + " (" + alllinks.size() + ")", LinkGrabberTableAction.NEW_PACKAGE, new Property("links", alllinks))));
                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.newpackage", 16, 16), JDL.L("gui.table.contextmenu.mergepackage2", "Merge to one package") + " (" + alllinks.size() + ")", LinkGrabberTableAction.MERGE_PACKAGE, new Property("links", alllinks))));
                popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.password", 16, 16), JDL.L("gui.table.contextmenu.setdlpw", "Set download password") + " (" + alllinks.size() + ")", LinkGrabberTableAction.SET_PW, new Property("links", alllinks))));
                popup.add(new JSeparator());
                HashMap<String, Object> prop = new HashMap<String, Object>();
                prop.put("links", alllinks);
                prop.put("boolean", true);
                if (links_disabled > 0) popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.ok", 16, 16), JDL.L("gui.table.contextmenu.enable", "aktivieren") + " (" + links_disabled + ")", LinkGrabberTableAction.DE_ACTIVATE, new Property("infos", prop))));
                prop = new HashMap<String, Object>();
                prop.put("links", alllinks);
                prop.put("boolean", false);
                if (links_enabled > 0) popup.add(new JMenuItem(new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.bad", 16, 16), JDL.L("gui.table.contextmenu.disable", "deaktivieren") + " (" + links_enabled + ")", LinkGrabberTableAction.DE_ACTIVATE, new Property("infos", prop))));
            }
            if (popup.getComponentCount() != 0) popup.show(this, point.x, point.y);
        }
    }

    private JMenuItem buildExtMenu() {
        JMenuItem tmp;
        JMenu men = new JMenu(JDL.L("gui.table.contextmenu.filetype", "Filter"));
        ArrayList<String> extensions = linkgrabber.getExtensions();
        HashSet<String> fl = LinkGrabberController.getInstance().getExtensionFilter();
        men.setIcon(JDTheme.II("gui.images.filter", 16, 16));
        synchronized (fl) {
            for (String e : extensions) {
                men.add(tmp = new JCheckBoxMenuItem(new LinkGrabberTableAction(linkgrabber, null, "*." + e, LinkGrabberTableAction.EXT_FILTER, new Property("extension", e))));
                tmp.setSelected(!fl.contains(e));
            }
        }
        return men;
    }

    private JMenu buildpriomenu(ArrayList<DownloadLink> links) {
        JMenuItem tmp;
        JMenu prioPopup = new JMenu(JDL.L("gui.table.contextmenu.priority", "Priority") + " (" + links.size() + ")");
        Integer prio = null;
        if (links.size() == 1) prio = links.get(0).getPriority();
        prioPopup.setIcon(JDTheme.II("gui.images.priority0", 16, 16));
        HashMap<String, Object> prop = null;
        for (int i = 3; i >= 1; i--) {
            prop = new HashMap<String, Object>();
            prop.put("links", links);
            prop.put("prio", new Integer(i));
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
        Color background = JDTheme.C("gui.color.downloadlist.row_link_disabled", "adadad", 100);

        addHighlighter(new DownloadLinkRowHighlighter(this, background, background) {
            // @Override
            public boolean doHighlight(DownloadLink link) {
                return !link.isEnabled();
            }
        });

    }

    private void addPackageHighlighter() {
        addHighlighter(new PainterHighlighter(new HighlightPredicate() {
            public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
                if (adapter.row == -1) return false;
                Object element = model.getValueAt(adapter.row, 0);
                if (element != null && element instanceof LinkGrabberFilePackage) { return true; }
                return false;
            }
        }, DownloadTable.getFolderPainter(this)));
    }

    private void addExistsHighlighter() {
        /* TODO: andre farbe auswählen */
        Color background = JDTheme.C("gui.color.linkgrabber.error_exists", "ff7f00", 120);
        addHighlighter(new DownloadLinkRowHighlighter(this, background, background) {
            // @Override
            public boolean doHighlight(DownloadLink link) {
                if (link.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) return true;
                return false;
            }
        });
    }

    private void createColumns() {
        // TODO Auto-generated method stub
        setAutoCreateColumnsFromModel(false);
        List<TableColumn> columns = getColumns(true);
        for (Iterator<TableColumn> iter = columns.iterator(); iter.hasNext();) {
            getColumnModel().removeColumn(iter.next());
        }
        final SubConfiguration config = SubConfiguration.getConfig("linkgrabber");
        cols = new TableColumnExt[getModel().getColumnCount()];
        for (int i = 0; i < getModel().getColumnCount(); i++) {

            TableColumnExt tableColumn = getColumnFactory().createAndConfigureTableColumn(getModel(), i);
            cols[i] = tableColumn;
            tableColumn.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    TableColumnExt column = (TableColumnExt) evt.getSource();
                    if (evt.getPropertyName().equals("width")) {
                        config.setProperty("WIDTH_COL_" + column.getModelIndex(), evt.getNewValue());
                        config.save();
                    } else if (evt.getPropertyName().equals("visible")) {
                        config.setProperty("VISABLE_COL_" + column.getModelIndex(), evt.getNewValue());
                        config.save();
                    }
                }
            });
            tableColumn.setVisible(config.getBooleanProperty("VISABLE_COL_" + i, true));
            tableColumn.setPreferredWidth(config.getIntegerProperty("WIDTH_COL_" + i, tableColumn.getWidth()));
            if (tableColumn != null) {
                getColumnModel().addColumn(tableColumn);
            }
        }
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            ArrayList<DownloadLink> alllinks = getAllSelectedDownloadLinks();
            LinkGrabberTableAction test = new LinkGrabberTableAction(linkgrabber, JDTheme.II("gui.images.delete", 16, 16), JDL.L("gui.table.contextmenu.delete", "entfernen") + " (" + alllinks.size() + ")", LinkGrabberTableAction.DELETE, new Property("links", alllinks));
            test.actionPerformed(new ActionEvent(test, 0, ""));
        }
    }

    public void keyPressed(KeyEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void keyTyped(KeyEvent arg0) {
        // TODO Auto-generated method stub

    }

    public LinkGrabberJTableModel getTableModel() {
        return model;
    }

}
