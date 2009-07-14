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
import java.awt.Dimension;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.event.ActionEvent;
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

import javax.swing.DropMode;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import jd.config.MenuItem;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.DownloadWatchDog;
import jd.event.ControlEvent;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.JDMenu;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.JBlurMenu;
import jd.nutils.Colors;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.PainterHighlighter;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jvnet.substance.api.ComponentState;
import org.jvnet.substance.api.SubstanceColorScheme;
import org.jvnet.substance.utils.SubstanceColorSchemeUtilities;

public class DownloadTable extends JXTable implements MouseListener, MouseMotionListener, KeyListener {

    public static final String PROPERTY_EXPANDED = "expanded";

    private static final long serialVersionUID = 1L;

    private TableCellRenderer cellRenderer;

    private TableColumnExt[] cols;

    private DownloadLinksPanel panel;

    private String[] prioDescs;

    private DownloadJTableModel model;

    public DownloadTable(DownloadJTableModel Model, final DownloadLinksPanel panel) {
        super(Model);
        cellRenderer = new TableRenderer(this);
        this.panel = panel;
        this.model = Model;
        this.setShowGrid(false);
        this.setShowHorizontalLines(false);
        this.setShowVerticalLines(false);
        createColumns();
        getTableHeader().setReorderingAllowed(false);
        getTableHeader().setResizingAllowed(true);
        prioDescs = new String[] { JDL.L("gui.treetable.tooltip.priority-1", "Low Priority"), JDL.L("gui.treetable.tooltip.priority0", "No Priority"), JDL.L("gui.treetable.tooltip.priority1", "High Priority"), JDL.L("gui.treetable.tooltip.priority2", "Higher Priority"), JDL.L("gui.treetable.tooltip.priority3", "Highest Priority") };
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.setColumnSelectionAllowed(false);
        this.setRowSelectionAllowed(true);
        setColumnControlVisible(true);

        this.setColumnControl(new JColumnControlButton(this));
        setAutoscrolls(false);
        setEditable(false);
        addMouseListener(this);
        addKeyListener(this);
        addMouseMotionListener(this);

        getTableHeader().setPreferredSize(new Dimension(getColumnModel().getTotalColumnWidth(), 19));
        this.setSortable(false);
        this.getTableHeader().addMouseListener(this);
        UIManager.put("Table.focusCellHighlightBorder", null);

        if (JDUtilities.getJavaVersion() >= 1.6) {
            // setDropMode(DropMode.ON_OR_INSERT_ROWS); /*muss noch geschaut
            // werden wie man das genau macht*/
            setDropMode(DropMode.USE_SELECTION);
            // setDropMode(DropMode.ON_OR_INSERT_ROWS);
        }

        setDragEnabled(true);
        setTransferHandler(new TableTransferHandler(this));

        ToolTipManager.sharedInstance().unregisterComponent(this);
        ToolTipManager.sharedInstance().unregisterComponent(this.getTableHeader());

        this.setHighlighters(new Highlighter[] {});
        addDisabledHighlighter();
        addPostErrorHighlighter();
        addWaitHighlighter();
        addPackageHighlighter();
    }

    public static Painter<?> getFolderPainter(JXTable table) {

        if (JDUtilities.getJavaVersion() >= 1.6 && SimpleGUI.isSubstance()) {

            int height = 20;
            SubstanceColorScheme colorScheme = SubstanceColorSchemeUtilities.getColorScheme(table, ComponentState.SELECTED);
            Color[] colors = new Color[] { Colors.getColor(colorScheme.getUltraLightColor(), 30), Colors.getColor(colorScheme.getLightColor(), 30), Colors.getColor(colorScheme.getMidColor(), 30), Colors.getColor(colorScheme.getUltraLightColor(), 30) };

            // Color[] colors= new
            // Color[]{Color.RED,Color.BLUE,Color.BLUE,Color.RED};
            LinearGradientPaint paint = new LinearGradientPaint(0, 0, 0, height, new float[] { 0.0f, 0.4f, 0.5f, 1.0f }, colors, CycleMethod.REPEAT);
            return new MattePainter(paint);

        } else {
            return new MattePainter(JDTheme.C("gui.color.downloadlist.package", "4c4c4c", 150));
        }

    }

    private void addPackageHighlighter() {
        addHighlighter(new PainterHighlighter(new HighlightPredicate() {
            public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
                if (adapter.row == -1) return false;
                Object element = model.getValueAt(adapter.row, 0);
                if (element != null && element instanceof FilePackage) { return true; }
                return false;
            }
        }, DownloadTable.getFolderPainter(this)));
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

            getColumnModel().addColumn(tableColumn);
        }

    }

    public TableColumnExt[] getCols() {
        return cols;
    }

    private void addWaitHighlighter() {
        Color background = JDTheme.C("gui.color.downloadlist.error_post", "ff9936", 100);
        addHighlighter(new DownloadLinkRowHighlighter(this, background) {
            // @Override
            public boolean doHighlight(DownloadLink dLink) {
                if (dLink.getLinkStatus().hasStatus(LinkStatus.FINISHED) || !dLink.isEnabled() || dLink.getLinkStatus().isPluginActive()) return false;
                return dLink.getPlugin() == null || dLink.getPlugin().getRemainingHosterWaittime() > 0;
            }
        });

    }

    private void addPostErrorHighlighter() {
        Color background = JDTheme.C("gui.color.downloadlist.error_post", "ff9936", 120);
        addHighlighter(new DownloadLinkRowHighlighter(this, background) {
            // @Override
            public boolean doHighlight(DownloadLink link) {
                return link.getLinkStatus().hasStatus(LinkStatus.ERROR_POST_PROCESS);
            }
        });

    }

    private void addDisabledHighlighter() {
        Color background = JDTheme.C("gui.color.downloadlist.row_link_disabled", "adadad", 100);
        addHighlighter(new DownloadLinkRowHighlighter(this, background) {
            // @Override
            public boolean doHighlight(DownloadLink link) {
                return !link.isEnabled();
            }
        });

    }

    public TableCellRenderer getCellRenderer(int row, int col) {
        return cellRenderer;
    }

    public DownloadJTableModel getTableModel() {
        return model;
    }

    public void fireTableChanged(final int id, final ArrayList<Object> objs) {
        new GuiRunnable<Object>() {
            // @Override
            public Object runSave() {
                final Rectangle viewRect = panel.getScrollPane().getViewport().getViewRect();
                int first = rowAtPoint(new Point(0, viewRect.y));
                int last = rowAtPoint(new Point(0, viewRect.y + viewRect.height - 1));
                switch (id) {
                case DownloadLinksPanel.REFRESH_SPECIFIED_LINKS:
                    for (Object obj : objs) {
                        int row = model.getRowforObject(obj);
                        if (row != -1) {
                            if (last == -1) {
                                model.fireTableRowsUpdated(row, row);
                            } else if (row >= first && row <= last) {
                                model.fireTableRowsUpdated(row, row);
                            }
                        }
                    }
                    return null;
                case DownloadLinksPanel.REFRESH_ALL_DATA_CHANGED:
                    model.fireTableDataChanged();
                    return null;
                case DownloadLinksPanel.REFRESH_DATA_AND_STRUCTURE_CHANGED:
                case DownloadLinksPanel.REFRESH_DATA_AND_STRUCTURE_CHANGED_FAST:
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
        if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            ArrayList<DownloadLink> alllinks = getAllSelectedDownloadLinks();
            TableAction test = new TableAction(panel, JDTheme.II("gui.images.delete", 16, 16), JDL.L("gui.table.contextmenu.delete", "entfernen") + " (" + alllinks.size() + ")", TableAction.DELETE, new Property("links", alllinks));
            test.actionPerformed(new ActionEvent(test, 0, ""));
        }
    }

    public void keyTyped(KeyEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getSource() == this.getTableHeader()) {
            int col = getRealcolumnAtPoint(e.getX());
            TableAction test = new TableAction(panel, JDTheme.II("gui.images.sort", 16, 16), JDL.L("gui.table.contextmenu.packagesort", "Paket sortieren"), TableAction.SORT_ALL, new Property("col", col));
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
            clearSelection();
            if (model.getValueAt(row, 0) != null) this.addRowSelectionInterval(row, row);
        }
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {

            if (model.getValueAt(row, 0) == null) { return; }
            ArrayList<DownloadLink> alllinks = getAllSelectedDownloadLinks();
            ArrayList<DownloadLink> resumlinks = new ArrayList<DownloadLink>();
            ArrayList<DownloadLink> allnoncon = new ArrayList<DownloadLink>();
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
            ArrayList<FilePackage> sfp = getSelectedFilePackages();
            Object obj = this.getModel().getValueAt(row, 0);
            JPopupMenu popup = new JPopupMenu();

            if (obj instanceof FilePackage || obj instanceof DownloadLink) {
                popup.add(tmp = new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.stopsign", 16, 16), JDL.L("gui.table.contextmenu.stopmark", "Stop sign"), TableAction.STOP_MARK, new Property("item", obj))));
                if (DownloadWatchDog.getInstance().isStopMark(obj)) tmp.setIcon(tmp.getDisabledIcon());
                popup.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.delete", 16, 16), JDL.L("gui.table.contextmenu.delete", "entfernen") + " (" + alllinks.size() + ")", TableAction.DELETE, new Property("links", alllinks))));

                popup.add(new JSeparator());
            }

            popup.add(createExtrasMenu(obj));
            if (obj instanceof FilePackage) {
                popup.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.package_opened", 16, 16), JDL.L("gui.table.contextmenu.downloadDir", "Zielordner öffnen"), TableAction.DOWNLOAD_DIR, new Property("folder", new File(((FilePackage) obj).getDownloadDirectory())))));
                popup.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.sort", 16, 16), JDL.L("gui.table.contextmenu.packagesort", "Paket sortieren") + " (" + sfp.size() + "), (" + this.getModel().getColumnName(col) + ")", TableAction.SORT, new Property("col", col))));
                popup.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.edit", 16, 16), JDL.L("gui.table.contextmenu.editpackagename", "Paketname ändern") + " (" + sfp.size() + ")", TableAction.EDIT_NAME, new Property("packages", sfp))));
                popup.add(tmp = new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.save", 16, 16), JDL.L("gui.table.contextmenu.editdownloadDir", "Zielordner ändern") + " (" + sfp.size() + ")", TableAction.EDIT_DIR, new Property("packages", sfp))));

                popup.add(new JSeparator());
            }
            if (obj instanceof DownloadLink) {
                popup.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.package_opened", 16, 16), JDL.L("gui.table.contextmenu.downloadDir", "Zielordner öffnen"), TableAction.DOWNLOAD_DIR, new Property("folder", new File(((DownloadLink) obj).getFileOutput()).getParentFile()))));
                popup.add(tmp = new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.browse", 16, 16), JDL.L("gui.table.contextmenu.browseLink", "im Browser öffnen"), TableAction.DOWNLOAD_BROWSE_LINK, new Property("downloadlink", obj))));
                if (((DownloadLink) obj).getLinkType() != DownloadLink.LINKTYPE_NORMAL) tmp.setEnabled(false);
            }
            if (obj instanceof FilePackage || obj instanceof DownloadLink) {
                popup.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.dlc", 16, 16), JDL.L("gui.table.contextmenu.dlc", "DLC erstellen") + " (" + alllinks.size() + ")", TableAction.DOWNLOAD_DLC, new Property("links", alllinks))));
                popup.add(buildpriomenu(alllinks));
                popup.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.icons.copy", 16, 16), JDL.L("gui.table.contextmenu.copyPassword", "Copy Password") + " (" + alllinks.size() + ")", TableAction.DOWNLOAD_COPY_PASSWORD, new Property("links", alllinks))));
                popup.add(tmp = new JMenuItem(new TableAction(panel, JDTheme.II("gui.icons.cut", 16, 16), JDL.L("gui.table.contextmenu.copyLink", "Copy URL") + " (" + allnoncon.size() + ")", TableAction.DOWNLOAD_COPY_URL, new Property("links", allnoncon))));
                if (allnoncon.size() == 0) tmp.setEnabled(false);
                popup.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.config.network_local", 16, 16), JDL.L("gui.table.contextmenu.check", "Check OnlineStatus") + " (" + alllinks.size() + ")", TableAction.CHECK, new Property("links", alllinks))));
                popup.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.newpackage", 16, 16), JDL.L("gui.table.contextmenu.newpackage", "In neues Paket verschieben") + " (" + alllinks.size() + ")", TableAction.NEW_PACKAGE, new Property("links", alllinks))));
                popup.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.password", 16, 16), JDL.L("gui.table.contextmenu.setdlpw", "Set download password") + " (" + alllinks.size() + ")", TableAction.SET_PW, new Property("links", alllinks))));
                popup.add(new JSeparator());
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
                popup.add(new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.reset", 16, 16), JDL.L("gui.table.contextmenu.reset", "zurücksetzen") + " (" + alllinks.size() + ")", TableAction.DOWNLOAD_RESET, new Property("links", alllinks))));
            }
            if (popup.getComponentCount() != 0) popup.show(this, point.x, point.y);
        }
    }

    private JBlurMenu buildpriomenu(ArrayList<DownloadLink> links) {
        JMenuItem tmp;
        JBlurMenu prioPopup = new JBlurMenu(JDL.L("gui.table.contextmenu.priority", "Priority") + " (" + links.size() + ")");
        Integer prio = null;
        if (links.size() == 1) prio = links.get(0).getPriority();
        prioPopup.setIcon(JDTheme.II("gui.images.priority0", 16, 16));
        HashMap<String, Object> prop = null;
        for (int i = 3; i >= -1; i--) {
            prop = new HashMap<String, Object>();
            prop.put("links", links);
            prop.put("prio", new Integer(i));
            prioPopup.add(tmp = new JMenuItem(new TableAction(panel, JDTheme.II("gui.images.priority" + i, 16, 16), prioDescs[i + 1], TableAction.DOWNLOAD_PRIO, new Property("infos", prop))));

            if (prio != null && i == prio) {
                tmp.setEnabled(false);
                tmp.setIcon(JDTheme.II("gui.images.priority" + i, 16, 16));
            } else
                tmp.setEnabled(true);
        }
        return prioPopup;
    }

    private JBlurMenu createExtrasMenu(Object obj) {
        JBlurMenu pluginPopup = new JBlurMenu(JDL.L("gui.table.contextmenu.extrasSubmenu", "Extras"));
        ArrayList<MenuItem> entries = new ArrayList<MenuItem>();
        if (obj instanceof FilePackage) {
            JDUtilities.getController().fireControlEventDirect(new ControlEvent((FilePackage) obj, ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU, entries));
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
        int row = rowAtPoint(e.getPoint());
        if (row == -1) return;
        int column = getRealcolumnAtPoint(e.getX());
        if (column == 0 && e.getButton() == MouseEvent.BUTTON1 && e.getX() < 20 && e.getClickCount() == 1) {
            Object element = this.getModel().getValueAt(row, 0);
            if (element != null && element instanceof FilePackage) {
                toggleFilePackageExpand((FilePackage) element);
            }
        } else if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
            Object element = this.getModel().getValueAt(row, 0);
            if (element == null) return;
            if (element instanceof FilePackage) {
                panel.showFilePackageInfo((FilePackage) element);
            } else {
                panel.showDownloadLinkInfo((DownloadLink) element);
            }
        }

    }

    public void toggleFilePackageExpand(FilePackage fp) {
        fp.setProperty(DownloadTable.PROPERTY_EXPANDED, !fp.getBooleanProperty(DownloadTable.PROPERTY_EXPANDED, false));
        panel.updateTableTask(DownloadLinksPanel.REFRESH_DATA_AND_STRUCTURE_CHANGED_FAST, null);
    }

}