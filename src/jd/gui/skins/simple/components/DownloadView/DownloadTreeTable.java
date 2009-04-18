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

package jd.gui.skins.simple.components.DownloadView;

import java.awt.Color;
import java.awt.Component;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.DropMode;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreePath;

import jd.config.MenuItem;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.event.ControlEvent;
import jd.gui.skins.simple.JDMenu;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.nutils.Colors;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.PainterHighlighter;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.tree.TreeModelSupport;
import org.jvnet.substance.api.ComponentState;
import org.jvnet.substance.api.SubstanceColorScheme;
import org.jvnet.substance.utils.SubstanceColorSchemeUtilities;

public class DownloadTreeTable extends JXTreeTable implements TreeExpansionListener, MouseListener, MouseMotionListener, KeyListener {

    public static final String PROPERTY_EXPANDED = "expanded";

    public static final String PROPERTY_SELECTED = "selected";

    private static final long serialVersionUID = 1L;

    private TableCellRenderer cellRenderer;

    private Logger logger = jd.controlling.JDLogger.getLogger();

    private DownloadTreeTableModel model;

    public int mouseOverRow = -1;



    private Timer timer;

    private TableColumnExt[] cols;

    private DownloadLinksPanel panel;

    public DownloadTreeTable(DownloadTreeTableModel treeModel, DownloadLinksPanel panel) {
        super(treeModel);
        cellRenderer = new TreeTableRenderer(this);
        this.panel = panel;
        // setTreeCellRenderer(treeCellRenderer);
        // this.setHighlighters(new Highlighter[] { hl });
        // this.setModel(treeModel)
        model = treeModel;

        createColumns();
        // this.setUI(new TreeTablePaneUI());
        getTableHeader().setReorderingAllowed(false);
        getTableHeader().setResizingAllowed(true);
        // this.setExpandsSelectedPaths(true);
        
        if (JDUtilities.getJavaVersion() >= 1.6) {
            setDropMode(DropMode.ON_OR_INSERT_ROWS);
        }
       setDragEnabled(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setColumnControlVisible(true);
        this.setColumnControl(new JColumnControlButton(this));
        setEditable(false);
        setAutoscrolls(false);
        addTreeExpansionListener(this);
        addMouseListener(this);
        addKeyListener(this);
        addMouseMotionListener(this);
        this.getTableHeader().addMouseListener(this);
        UIManager.put("Table.focusCellHighlightBorder", null);
        setTransferHandler(new TreeTableTransferHandler(this));
        if (JDUtilities.getJavaVersion() > 1.6) {
            setDropMode(DropMode.USE_SELECTION);
        }

        this.setHighlighters(new Highlighter[] {});
        // setHighlighters(HighlighterFactory.createAlternateStriping(UIManager.
        // getColor("Panel.background").brighter(),
        // UIManager.getColor("Panel.background")));

        // addHighlighter(new ColorHighlighter(HighlightPredicate.ALWAYS,
        // JDTheme.C("gui.color.downloadlist.row_package", "fffa7c"),
        // Color.BLACK));

        // addFinishedHighlighter();
        addDisabledHighlighter();
        addPostErrorHighlighter();
        addWaitHighlighter();
        // addErrorHighlighter();

        // addHighlighter(new FilepackageRowHighlighter(this, Color.RED,
        // Color.BLUE, Color.RED, Color.BLUE) {
        // @Override
        // public boolean doHighlight(FilePackage fp) {
        // return true;
        // }
        // });
        addHighlighter(new PainterHighlighter(HighlightPredicate.IS_FOLDER, getFolderPainter(this)));

        // Highlighter extendPrefWidth = new AbstractHighlighter() {
        // @Override
        // protected Component doHighlight(Component component, ComponentAdapter
        // adapter) {
        // Dimension dim = component.getPreferredSize();
        // int width = 600;
        // dim.width = Math.max(dim.width, width);
        // component.setPreferredSize(dim);
        // return component;
        //
        // }
        // };
        //
        // addHighlighter(extendPrefWidth);
        // ATTENTION >=1.6
        /**
         * correct paint errors in JXTreeTable due to annimation over the first
         * row. The first row is a tree and thus does not implement
         * PainterHighlighter. Without modding the TreeTable code, it seems
         * unpossible to fix this.
         */

        /**
         * Set here colors if java version is below 1.6 and substance cannot be
         * used
         */
        // addHighlighter(new
        // ColorHighlighter(HighlightPredicate.ROLLOVER_ROW, Color.GRAY,
        // Color.BLACK));
    }

    public static Painter<Component> getFolderPainter(JXTreeTable table) {

        if (JDUtilities.getJavaVersion() >= 1.6 && SimpleGUI.isSubstance()) {

            int height = 20;
            // JProgressBar progressBar = new JProgressBar();
            SubstanceColorScheme colorScheme = SubstanceColorSchemeUtilities.getColorScheme(table, ComponentState.SELECTED);
            Color[] colors = new Color[] { Colors.getColor(colorScheme.getUltraLightColor(), 50), Colors.getColor(colorScheme.getLightColor(), 50), Colors.getColor(colorScheme.getMidColor(), 50), Colors.getColor(colorScheme.getUltraLightColor(), 50) };
            LinearGradientPaint paint = new LinearGradientPaint(0, 0, 0, height, new float[] { 0.0f, 0.4f, 0.5f, 1.0f }, colors, CycleMethod.REPEAT);
            return new MattePainter<Component>(paint);

        } else {
            return new MattePainter<Component>(JDTheme.C("gui.color.downloadlist.package", "4c4c4c", 150));
        }

    }

    /**
     * Link HIghlighters
     */
    private void addWaitHighlighter() {
        Color background = JDTheme.C("gui.color.downloadlist.error_post", "ff9936", 100);

        addHighlighter(new DownloadLinkRowHighlighter(this, background, background) {
            @Override
            public boolean doHighlight(DownloadLink dLink) {
                return dLink.getLinkStatus().getRemainingWaittime() > 0 || dLink.getPlugin() == null || dLink.getPlugin().getRemainingHosterWaittime() > 0;
            }
        });

    }

    private void addErrorHighlighter() {
        Color background = JDTheme.C("gui.color.downloadlist.error_post", "ff9936", 120);

        addHighlighter(new DownloadLinkRowHighlighter(this, background, background) {
            @Override
            public boolean doHighlight(DownloadLink link) {
                return link.getLinkStatus().isFailed();
            }
        });

    }

    private void addPostErrorHighlighter() {
        Color background = JDTheme.C("gui.color.downloadlist.error_post", "ff9936", 120);

        addHighlighter(new DownloadLinkRowHighlighter(this, background, background) {
            @Override
            public boolean doHighlight(DownloadLink link) {
                return link.getLinkStatus().hasStatus(LinkStatus.ERROR_POST_PROCESS);
            }
        });

    }

    private void addDisabledHighlighter() {
        Color background = JDTheme.C("gui.color.downloadlist.row_link_disabled", "adadad", 100);

        addHighlighter(new DownloadLinkRowHighlighter(this, background, background) {
            @Override
            public boolean doHighlight(DownloadLink link) {
                return !link.isEnabled();
            }
        });

    }

    private void addFinishedHighlighter() {
        Color background = JDTheme.C("gui.color.downloadlist.row_link_done", "c4ffd2", 10);
        Color backGroundPackage = JDTheme.C("gui.color.downloadlist.row_package_done", "339933", 10);

        addHighlighter(new FilepackageRowHighlighter(this, backGroundPackage, backGroundPackage) {
            @Override
            public boolean doHighlight(FilePackage fp) {
                return fp.isFinished();
            }
        });

        addHighlighter(new DownloadLinkRowHighlighter(this, background, background) {
            @Override
            public boolean doHighlight(DownloadLink link) {
                return link.getLinkStatus().hasStatus(LinkStatus.FINISHED);
            }
        });

    }

    public TableCellRenderer getCellRenderer(int row, int col) {
        return cellRenderer;
    }

    private void createColumns() {
        setAutoCreateColumnsFromModel(false);
        List<TableColumn> columns = getColumns(true);
        for (Iterator<TableColumn> iter = columns.iterator(); iter.hasNext();) {
            getColumnModel().removeColumn(iter.next());

        }

        final SubConfiguration config = SubConfiguration.getConfig("gui");
        cols = new TableColumnExt[getModel().getColumnCount()];
        for (int i = 0; i < getModel().getColumnCount(); i++) {
            TableColumnExt tableColumn = getColumnFactory().createAndConfigureTableColumn(getModel(), i);
            cols[i] = tableColumn;

            cols[i] = tableColumn;
            if (i > 0) {
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
                if (i == DownloadTreeTableModel.COL_STATUS_ICON) {
                    tableColumn.setMinWidth(20);
                    tableColumn.setMaxWidth(20);
                } else {
                    tableColumn.setPreferredWidth(config.getIntegerProperty("WIDTH_COL_" + i, tableColumn.getWidth()));
                }
                if (tableColumn != null) {
                    getColumnModel().addColumn(tableColumn);
                }
            } else {
                tableColumn.setVisible(false);
            }
        }

    }

    public TableColumnExt[] getCols() {
        return cols;
    }

    @SuppressWarnings("unchecked")
    public synchronized void fireTableChanged(int id, Object param) {
        TreeModelSupport supporter = getDownladTreeTableModel().getModelSupporter();
        switch (id) {
        case DownloadLinksPanel.REFRESH_SPECIFIED_LINKS:
            ArrayList<DownloadLink> links = ((ArrayList<DownloadLink>) param);
            HashMap<Object, TreePath> map = this.getPathMap();
            for (DownloadLink dl : links) {
                TreePath path = map.remove(dl);
                if (path == null) continue;
                supporter.firePathChanged(path);
                path = map.remove(dl.getFilePackage());
                if (path == null) continue;
                supporter.firePathChanged(path);
            }
            break;
        case DownloadLinksPanel.REFRESH_ALL_DATA_CHANGED:
            supporter.fireChildrenChanged(new TreePath(model.getRoot()), null, null);
            break;
        case DownloadLinksPanel.REFRESH_DATA_AND_STRUCTURE_CHANGED:
            supporter.fireTreeStructureChanged(new TreePath(model.getRoot()));
            updateSelectionAndExpandStatus();
            break;
        }
    }

    public DownloadTreeTableModel getDownladTreeTableModel() {
        return (DownloadTreeTableModel) getTreeTableModel();
    }

    public Vector<DownloadLink> getSelectedDownloadLinks() {
        int[] rows = getSelectedRows();
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        TreePath path;
        for (int element : rows) {
            path = getPathForRow(element);
            if (path != null && path.getLastPathComponent() instanceof DownloadLink) {
                ret.add((DownloadLink) path.getLastPathComponent());

            }
        }
        return ret;
    }

    public Vector<DownloadLink> getAllSelectedDownloadLinks() {
        Vector<DownloadLink> links = getSelectedDownloadLinks();
        Vector<FilePackage> fps = getSelectedFilePackages();
        for (FilePackage filePackage : fps) {
            for (DownloadLink dl : filePackage.getDownloadLinks()) {
                if (!links.contains(dl)) links.add(dl);
            }
        }
        return links;
    }

    public Vector<FilePackage> getSelectedFilePackages() {
        int[] rows = getSelectedRows();
        Vector<FilePackage> ret = new Vector<FilePackage>();
        TreePath path;
        for (int element : rows) {
            path = getPathForRow(element);
            if (path != null && path.getLastPathComponent() instanceof FilePackage) {
                ret.add((FilePackage) path.getLastPathComponent());

            }
        }
        return ret;
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            Vector<DownloadLink> alllinks = getAllSelectedDownloadLinks();
            TreeTableAction test = new TreeTableAction(panel, JDLocale.L("gui.table.contextmenu.delete", "entfernen") + " (" + alllinks.size() + ")", TreeTableAction.DELETE, new Property("links", alllinks));
            test.actionPerformed(new ActionEvent(test, 0, ""));
        } else if (e.getKeyCode() == KeyEvent.VK_UP && e.isControlDown()) {
            int cur = getSelectedRow();
            /**
             * TODO
             */
            if (e.isAltDown()) {
                // moveSelectedItems(MenuAction.ITEMS_MOVE_TOP);
                getSelectionModel().setSelectionInterval(0, 0);
            } else {
                // moveSelectedItems(MenuAction.ITEMS_MOVE_UP);
                cur = Math.max(0, cur - 1);
                getSelectionModel().setSelectionInterval(cur, cur);
            }
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN && e.isControlDown()) {
            int cur = getSelectedRow();
            int len = getVisibleRowCount();
            if (e.isAltDown()) {
                // moveSelectedItems(MenuAction.ITEMS_MOVE_BOTTOM);
                getSelectionModel().setSelectionInterval(len, len);
            } else {
                // moveSelectedItems(MenuAction.ITEMS_MOVE_DOWN);
                cur = Math.min(len, cur + 1);
                getSelectionModel().setSelectionInterval(cur, cur);
            }
        }

    }

    public void keyTyped(KeyEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getSource() == this.getTableHeader()) {
            int col = getRealcolumnAtPoint(e.getX());
            TreeTableAction test = new TreeTableAction(panel, JDLocale.L("gui.table.contextmenu.packagesort", "Paket sortieren"), TreeTableAction.SORT_ALL, new Property("col", col));
            test.actionPerformed(new ActionEvent(test, 0, ""));
            return;
        }
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {

    }

    public void mouseMoved(MouseEvent e) {
        mouseOverRow = rowAtPoint(e.getPoint());
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

    public void mousePressed(MouseEvent e) {
        /* nicht auf headerclicks reagieren */
        if (e.getSource() != this) return;
        Point point = e.getPoint();
        int row = rowAtPoint(point);
        int col = getRealcolumnAtPoint(e.getX());
        JMenuItem tmp;

        if (!isRowSelected(row) && e.getButton() == MouseEvent.BUTTON3) {
            getTreeSelectionModel().clearSelection();
            getTreeSelectionModel().addSelectionPath(getPathForRow(row));
        }
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {

            if (getPathForRow(row) == null) { return; }
            Vector<DownloadLink> alllinks = getAllSelectedDownloadLinks();
            Vector<DownloadLink> resumlinks = new Vector<DownloadLink>();
            Vector<DownloadLink> allnoncon = new Vector<DownloadLink>();
            int links_enabled = 0;
            for (DownloadLink next : alllinks) {
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
            Vector<FilePackage> sfp = getSelectedFilePackages();
            Object obj = getPathForRow(row).getLastPathComponent();
            JPopupMenu popup = new JPopupMenu();
            if (obj instanceof FilePackage || obj instanceof DownloadLink) {
                popup.add(new JMenuItem(new TreeTableAction(panel, JDLocale.L("gui.table.contextmenu.delete", "entfernen") + " (" + alllinks.size() + ")", TreeTableAction.DELETE, new Property("links", alllinks))));
                popup.add(new JSeparator());
            }
            popup.add(createExtrasMenu(obj));
            if (obj instanceof FilePackage) {
                popup.add(new JMenuItem(new TreeTableAction(panel, JDLocale.L("gui.table.contextmenu.downloadDir", "Zielordner öffnen"), TreeTableAction.DOWNLOAD_DIR, new Property("folder", new File(((FilePackage) obj).getDownloadDirectory())))));
                popup.add(new JMenuItem(new TreeTableAction(panel, JDLocale.L("gui.table.contextmenu.packagesort", "Paket sortieren") + " (" + sfp.size() + "), (" + this.getModel().getColumnName(col) + ")", TreeTableAction.SORT, new Property("col", col))));
                popup.add(new JMenuItem(new TreeTableAction(panel, JDLocale.L("gui.table.contextmenu.editpackagename", "Paketname ändern") + " (" + sfp.size() + ")", TreeTableAction.EDIT_NAME, new Property("packages", sfp))));
                popup.add(new JMenuItem(new TreeTableAction(panel, JDLocale.L("gui.table.contextmenu.editdownloadDir", "Zielordner ändern") + " (" + sfp.size() + ")", TreeTableAction.EDIT_DIR, new Property("packages", sfp))));
                popup.add(new JSeparator());
            }
            if (obj instanceof DownloadLink) {
                popup.add(new JMenuItem(new TreeTableAction(panel, JDLocale.L("gui.table.contextmenu.downloadDir", "Zielordner öffnen"), TreeTableAction.DOWNLOAD_DIR, new Property("folder", new File(((DownloadLink) obj).getFileOutput()).getParentFile()))));
                popup.add(tmp = new JMenuItem(new TreeTableAction(panel, JDLocale.L("gui.table.contextmenu.browseLink", "im Browser öffnen"), TreeTableAction.DOWNLOAD_BROWSE_LINK, new Property("downloadlink", obj))));
                if (((DownloadLink) obj).getLinkType() != DownloadLink.LINKTYPE_NORMAL) tmp.setEnabled(false);
            }
            if (obj instanceof FilePackage || obj instanceof DownloadLink) {
                popup.add(new JMenuItem(new TreeTableAction(panel, JDLocale.L("gui.table.contextmenu.dlc", "DLC erstellen") + " (" + alllinks.size() + ")", TreeTableAction.DOWNLOAD_DLC, new Property("links", alllinks))));
                popup.add(buildpriomenu(alllinks));
                popup.add(new JMenuItem(new TreeTableAction(panel, JDLocale.L("gui.table.contextmenu.copyPassword", "Copy Password") + " (" + alllinks.size() + ")", TreeTableAction.DOWNLOAD_COPY_PASSWORD, new Property("links", alllinks))));
                popup.add(tmp = new JMenuItem(new TreeTableAction(panel, JDLocale.L("gui.table.contextmenu.copyLink", "Copy URL") + " (" + allnoncon.size() + ")", TreeTableAction.DOWNLOAD_COPY_URL, new Property("links", allnoncon))));
                if (allnoncon.size() == 0) tmp.setEnabled(false);
                popup.add(new JMenuItem(new TreeTableAction(panel, JDLocale.L("gui.table.contextmenu.check", "Check OnlineStatus") + " (" + alllinks.size() + ")", TreeTableAction.CHECK, new Property("links", alllinks))));
                popup.add(new JMenuItem(new TreeTableAction(panel, JDLocale.L("gui.table.contextmenu.newpackage", "In neues Paket verschieben") + " (" + alllinks.size() + ")", TreeTableAction.NEW_PACKAGE, new Property("links", alllinks))));
                popup.add(new JMenuItem(new TreeTableAction(panel, JDLocale.L("gui.table.contextmenu.setdlpw", "Set download password") + " (" + alllinks.size() + ")", TreeTableAction.SET_PW, new Property("links", alllinks))));
                popup.add(new JSeparator());
                HashMap<String, Object> prop = new HashMap<String, Object>();
                prop.put("links", alllinks);
                prop.put("boolean", true);
                popup.add(tmp = new JMenuItem(new TreeTableAction(panel, JDLocale.L("gui.table.contextmenu.enable", "aktivieren") + " (" + links_disabled + ")", TreeTableAction.DE_ACTIVATE, new Property("infos", prop))));
                if (links_disabled == 0) tmp.setEnabled(false);
                prop = new HashMap<String, Object>();
                prop.put("links", alllinks);
                prop.put("boolean", false);
                popup.add(tmp = new JMenuItem(new TreeTableAction(panel, JDLocale.L("gui.table.contextmenu.disable", "deaktivieren") + " (" + links_enabled + ")", TreeTableAction.DE_ACTIVATE, new Property("infos", prop))));
                if (links_enabled == 0) tmp.setEnabled(false);
                popup.add(tmp = new JMenuItem(new TreeTableAction(panel, JDLocale.L("gui.table.contextmenu.resume", "fortsetzen") + " (" + resumlinks.size() + ")", TreeTableAction.DOWNLOAD_RESUME, new Property("links", resumlinks))));
                if (resumlinks.size() == 0) tmp.setEnabled(false);
                popup.add(new JMenuItem(new TreeTableAction(panel, JDLocale.L("gui.table.contextmenu.reset", "zurücksetzen") + " (" + alllinks.size() + ")", TreeTableAction.DOWNLOAD_RESET, new Property("links", alllinks))));
            }
            if (popup.getComponentCount() != 0) popup.show(this, point.x, point.y);
        }
    }

    private JMenu buildpriomenu(Vector<DownloadLink> links) {
        JMenuItem tmp;
        JMenu prioPopup = new JMenu(JDLocale.L("gui.table.contextmenu.priority", "Priority") + " (" + links.size() + ")");
        Integer prio = null;
        if (links.size() == 1) prio = links.get(0).getPriority();
        HashMap<String, Object> prop = null;
        for (int i = 4; i >= -4; i--) {
            prop = new HashMap<String, Object>();
            prop.put("links", links);
            prop.put("prio", new Integer(i));
            prioPopup.add(tmp = new JMenuItem(new TreeTableAction(panel, Integer.toString(i), TreeTableAction.DOWNLOAD_PRIO, new Property("infos", prop))));
            if (prio != null && i == prio) {
                tmp.setEnabled(false);
            } else
                tmp.setEnabled(true);
        }
        return prioPopup;
    }

    private JMenu createExtrasMenu(Object obj) {
        JMenu pluginPopup = new JMenu(JDLocale.L("gui.table.contextmenu.extrasSubmenu", "Extras"));
        ArrayList<MenuItem> entries = new ArrayList<MenuItem>();
        if (obj instanceof FilePackage) {
            JDUtilities.getController().fireControlEventDirect(new ControlEvent(((FilePackage) obj), ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU, entries));
        } else if (obj instanceof DownloadLink) {
            JDUtilities.getController().fireControlEventDirect(new ControlEvent((DownloadLink) obj, ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU, entries));
        } else {
            return null;
        }
        if (entries != null && entries.size() > 0) {
            for (MenuItem next : entries) {
                JMenuItem mi = JDMenu.getJMenuItem(next);
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
        TreePath path = getPathForLocation(e.getX(), e.getY());
        if (path == null) return;
        int column = getRealcolumnAtPoint(e.getX());
        if (path != null && path.getLastPathComponent() instanceof FilePackage) {
            if (column == 1 && e.getButton() == MouseEvent.BUTTON1) {
                if (e.getClickCount() == 1) {
                    FilePackage fp = (FilePackage) path.getLastPathComponent();
                    if (fp.getBooleanProperty(PROPERTY_EXPANDED, false)) {
                        collapsePath(path);
                    } else {
                        expandPath(path);
                    }
                }
            }
        }
    }

    public void moveSelectedItems(int id) {
        Vector<DownloadLink> links = getSelectedDownloadLinks();
        Vector<FilePackage> fps = getSelectedFilePackages();

        logger.finer("move " + links.size() + " - " + fps.size());
        if (links.size() >= fps.size()) {
            if (links.size() == 0) { return; }
            /**
             * TODO
             */
            switch (id) {
            // case MenuAction.ITEMS_MOVE_BOTTOM:
            // DownloadLink lastLink =
            // JDUtilities.getController().getPackages().
            // lastElement().getDownloadLinks().lastElement();
            // JDUtilities.getController().moveLinks(links, lastLink, null);
            // break;
            // case MenuAction.ITEMS_MOVE_TOP:
            // DownloadLink firstLink =
            // JDUtilities.getController().getPackages()
            // .firstElement().getDownloadLinks().firstElement();
            // JDUtilities.getController().moveLinks(links, null, firstLink);
            // break;
            // case MenuAction.ITEMS_MOVE_UP:
            // DownloadLink before =
            // JDUtilities.getController().getDownloadLinkBefore(links.get(0));
            // JDUtilities.getController().moveLinks(links, null, before);
            // break;
            // case MenuAction.ITEMS_MOVE_DOWN:
            // DownloadLink after =
            // JDUtilities.getController().getDownloadLinkAfter
            // (links.lastElement());
            // JDUtilities.getController().moveLinks(links, after, null);
            // break;
            }

        } else {

            switch (id) {
            // case MenuAction.ITEMS_MOVE_BOTTOM:
            // FilePackage lastFilepackage =
            // JDUtilities.getController().getPackages().lastElement();
            // JDUtilities.getController().movePackages(fps, lastFilepackage,
            // null);
            // break;
            // case MenuAction.ITEMS_MOVE_TOP:
            // FilePackage firstPackage =
            // JDUtilities.getController().getPackages().firstElement();
            // JDUtilities.getController().movePackages(fps, null,
            // firstPackage);
            // break;
            // case MenuAction.ITEMS_MOVE_UP:
            // int i =
            // JDUtilities.getController().getPackages().indexOf(fps.get(0));
            // if (i <= 0) return;
            //
            // FilePackage before =
            // JDUtilities.getController().getPackages().get(i - 1);
            // JDUtilities.getController().movePackages(fps, null, before);
            // break;
            // case MenuAction.ITEMS_MOVE_DOWN:
            // i =
            // JDUtilities.getController().getPackages().indexOf(fps.lastElement
            // ());
            // if (i >= JDUtilities.getController().getPackages().size() - 1)
            // return;
            //
            // FilePackage after =
            // JDUtilities.getController().getPackages().get(i + 1);
            // JDUtilities.getController().movePackages(fps, after, null);
            // break;
            }

        }
        timer = new Timer(100, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateSelectionAndExpandStatus();
                logger.finest("REFRESH");
            }

        });
        timer.setRepeats(false);
        timer.start();

    }

    /**
     * Die Listener speichern bei einer Selection oder beim aus/Einklappen von
     * Ästen deren Status
     */
    public void treeCollapsed(TreeExpansionEvent event) {
        FilePackage fp = (FilePackage) event.getPath().getLastPathComponent();
        fp.setProperty(DownloadTreeTable.PROPERTY_EXPANDED, false);
    }

    public void treeExpanded(TreeExpansionEvent event) {
        FilePackage fp = (FilePackage) event.getPath().getLastPathComponent();
        fp.setProperty(DownloadTreeTable.PROPERTY_EXPANDED, true);
    }

    /**
     * Diese Methode setzt die gespeicherten Werte für die Selection und
     * Expansion
     */
    public synchronized void updateSelectionAndExpandStatus() {
        int i = 0;
        while (getPathForRow(i) != null) {
            if (getPathForRow(i).getLastPathComponent() instanceof FilePackage) {
                FilePackage fp = (FilePackage) getPathForRow(i).getLastPathComponent();
                if (fp.getBooleanProperty(PROPERTY_EXPANDED, false)) {
                    expandPath(getPathForRow(i));
                }
            }
            i++;
        }
    }

    public HashMap<Object, TreePath> getPathMap() {
        HashMap<Object, TreePath> map = new HashMap<Object, TreePath>();
        int i = 0;
        while (getPathForRow(i) != null) {
            map.put(getPathForRow(i).getLastPathComponent(), new TreePath(this.getPathForRow(i).getPath()));
            i++;
        }
        return map;
    }
}