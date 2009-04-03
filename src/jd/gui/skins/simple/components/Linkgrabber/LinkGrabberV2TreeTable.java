package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreePath;

import jd.config.Property;
import jd.config.SubConfiguration;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.gui.skins.simple.components.treetable.DownloadLinkRowHighlighter;
import jd.gui.skins.simple.components.treetable.DownloadTreeTable;
import jd.gui.skins.simple.components.treetable.JColumnControlButton;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.PainterHighlighter;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.tree.TreeModelSupport;

public class LinkGrabberV2TreeTable extends JXTreeTable implements MouseListener, MouseMotionListener, TreeExpansionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private LinkGrabberV2TreeTableModel model;
    private LinkGrabberV2Panel linkgrabber;
    private LinkGrabberV2TreeTableRenderer cellRenderer;
    private TableColumnExt[] cols;
    private int neededclicks = 1;

    public static final String PROPERTY_EXPANDED = "lg_expanded";
    public static final String PROPERTY_SELECTED = "lg_selected";

    public LinkGrabberV2TreeTable(LinkGrabberV2TreeTableModel treeModel, LinkGrabberV2Panel linkgrabber) {
        super(treeModel);
        this.linkgrabber = linkgrabber;
        JDUtilities.getSubConfig(SimpleGuiConstants.GUICONFIGNAME);
        cellRenderer = new LinkGrabberV2TreeTableRenderer(this);
        model = treeModel;
        createColumns();
        if (JDUtilities.getSubConfig(SimpleGuiConstants.GUICONFIGNAME).getBooleanProperty(SimpleGuiConstants.PARAM_DCLICKPACKAGE, false)) neededclicks = 2;
        getTableHeader().setReorderingAllowed(false);
        getTableHeader().setResizingAllowed(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setEditable(false);
        setAutoscrolls(false);
        setColumnControlVisible(true);
        setColumnControl(new JColumnControlButton(this));
        addTreeExpansionListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        this.getTableHeader().addMouseListener(this);
        UIManager.put("Table.focusCellHighlightBorder", null);
        setHighlighters(new Highlighter[] {});
        // setHighlighters(HighlighterFactory.createAlternateStriping(UIManager.
        // getColor("Panel.background").brighter(),
        // UIManager.getColor("Panel.background").darker()));
        // new PainterHighlighter(HighlightPredicate.IS_FOLDER,
        // DownloadTreeTable.getFolderPainter());
        addPackageHighlighter();
        addOfflineHighlighter();
        // addOnlineHighlighter();
        addDisabledHighlighter();

        // addPackageOfflineHighlighter();
        addExistsHighlighter();
        addUncheckedHighlighter();

    }

    public TableCellRenderer getCellRenderer(int row, int col) {
        return cellRenderer;
    }

    public synchronized void fireTableChanged(int id, Object param) {
        TreeModelSupport supporter = getLinkGrabberV2TreeTableModel().getModelSupporter();
        supporter.fireTreeStructureChanged(new TreePath(model.getRoot()));
        updateSelectionAndExpandStatus();
    }

    public LinkGrabberV2TreeTableModel getLinkGrabberV2TreeTableModel() {
        return (LinkGrabberV2TreeTableModel) getTreeTableModel();
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
        Vector<LinkGrabberV2FilePackage> fps = getSelectedFilePackages();
        for (LinkGrabberV2FilePackage filePackage : fps) {
            links.addAll(filePackage.getDownloadLinks());
        }
        return links;
    }

    public Vector<LinkGrabberV2FilePackage> getSelectedFilePackages() {
        int[] rows = getSelectedRows();
        Vector<LinkGrabberV2FilePackage> ret = new Vector<LinkGrabberV2FilePackage>();
        TreePath path;
        for (int element : rows) {
            path = getPathForRow(element);
            if (path != null && path.getLastPathComponent() instanceof LinkGrabberV2FilePackage) {
                ret.add((LinkGrabberV2FilePackage) path.getLastPathComponent());
            }
        }
        return ret;
    }

    public void mouseDragged(MouseEvent arg0) {
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void updateSelectionAndExpandStatus() {
        int i = 0;
        while (getPathForRow(i) != null) {
            if (getPathForRow(i).getLastPathComponent() instanceof LinkGrabberV2FilePackage) {
                LinkGrabberV2FilePackage fp = (LinkGrabberV2FilePackage) getPathForRow(i).getLastPathComponent();
                if (fp.getBooleanProperty(PROPERTY_EXPANDED, false)) {
                    expandPath(getPathForRow(i));
                }
            }
            i++;
        }
    }

    public void treeCollapsed(TreeExpansionEvent event) {
        LinkGrabberV2FilePackage fp = (LinkGrabberV2FilePackage) event.getPath().getLastPathComponent();
        fp.setProperty(PROPERTY_EXPANDED, false);
    }

    public void treeExpanded(TreeExpansionEvent event) {
        LinkGrabberV2FilePackage fp = (LinkGrabberV2FilePackage) event.getPath().getLastPathComponent();
        fp.setProperty(PROPERTY_EXPANDED, true);
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
            LinkGrabberV2TreeTableAction test = new LinkGrabberV2TreeTableAction(linkgrabber, JDLocale.L("gui.table.contextmenu.packagesort", "Paket sortieren"), LinkGrabberV2TreeTableAction.SORT_ALL, new Property("col", col));
            test.actionPerformed(new ActionEvent(test, 0, ""));
            return;
        }
        Point point = e.getPoint();
        int row = rowAtPoint(point);
        int col = getRealcolumnAtPoint(e.getX());
        if (getPathForRow(row) == null) { return; }
        Object obj = getPathForRow(row).getLastPathComponent();
        if (col > 1) {
            if (obj instanceof LinkGrabberV2FilePackage) {
                linkgrabber.showFilePackageInfo((LinkGrabberV2FilePackage) obj);
            } else {
                linkgrabber.hideFilePackageInfo();
            }
        }
    }

    public void mouseEntered(MouseEvent arg0) {
    }

    public void mouseExited(MouseEvent arg0) {
    }

    public void mouseReleased(MouseEvent e) {
        /* nicht auf headerclicks reagieren */
        if (e.getSource() != this) return;
        TreePath path = getPathForLocation(e.getX(), e.getY());
        if (path == null) return;
        int column = getRealcolumnAtPoint(e.getX());
        if (path != null && path.getLastPathComponent() instanceof LinkGrabberV2FilePackage) {
            if (column == 1) {
                if (e.getClickCount() >= neededclicks) {
                    LinkGrabberV2FilePackage fp = (LinkGrabberV2FilePackage) path.getLastPathComponent();
                    if (fp.getBooleanProperty(PROPERTY_EXPANDED, false)) {
                        collapsePath(path);
                    } else {
                        expandPath(path);
                    }
                }
            }
        }
    }

    public void mousePressed(MouseEvent e) {
        /* nicht auf headerclicks reagieren */
        if (e.getSource() != this) return;
        Point point = e.getPoint();
        int row = rowAtPoint(point);
        int col = getRealcolumnAtPoint(e.getX());

        if (!isRowSelected(row) && e.getButton() == MouseEvent.BUTTON3) {
            getTreeSelectionModel().clearSelection();
            getTreeSelectionModel().addSelectionPath(getPathForRow(row));
        }
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {

            if (getPathForRow(row) == null) { return; }
            Vector<DownloadLink> alllinks = getAllSelectedDownloadLinks();
            int links_enabled = 0;
            for (DownloadLink next : alllinks) {
                if (next.isEnabled()) {
                    links_enabled++;
                }
            }
            int links_disabled = alllinks.size() - links_enabled;
            Vector<LinkGrabberV2FilePackage> sfp = getSelectedFilePackages();
            Object obj = getPathForRow(row).getLastPathComponent();
            JPopupMenu popup = new JPopupMenu();
            if (obj instanceof LinkGrabberV2FilePackage || obj instanceof DownloadLink) {
                popup.add(new JMenuItem(new LinkGrabberV2TreeTableAction(linkgrabber, JDLocale.L("gui.linkgrabberv2.lg.addall", "Add all packages"), LinkGrabberV2TreeTableAction.ADD_ALL)));
                popup.add(new JMenuItem(new LinkGrabberV2TreeTableAction(linkgrabber, JDLocale.L("gui.linkgrabberv2.lg.rmoffline", "Remove all Offline"), LinkGrabberV2TreeTableAction.DELETE_OFFLINE)));
                popup.add(new JMenuItem(new LinkGrabberV2TreeTableAction(linkgrabber, JDLocale.L("gui.table.contextmenu.delete", "entfernen") + " (" + alllinks.size() + ")", LinkGrabberV2TreeTableAction.DELETE, new Property("links", alllinks))));
                if (sfp.size() > 0) popup.add(new JMenuItem(new LinkGrabberV2TreeTableAction(linkgrabber, JDLocale.L("gui.linkgrabberv2.lg.addselected", "Add selected package(s)") + " (" + sfp.size() + ")", LinkGrabberV2TreeTableAction.ADD_SELECTED)));
                popup.add(new JSeparator());
            }
            if (obj instanceof LinkGrabberV2FilePackage) {
                popup.add(new JMenuItem(new LinkGrabberV2TreeTableAction(linkgrabber, JDLocale.L("gui.table.contextmenu.editdownloadDir", "Zielordner ändern") + " (" + sfp.size() + ")", LinkGrabberV2TreeTableAction.EDIT_DIR)));
                popup.add(new JMenuItem(new LinkGrabberV2TreeTableAction(linkgrabber, JDLocale.L("gui.table.contextmenu.packagesort", "Paket sortieren") + " (" + sfp.size() + "), (" + this.getModel().getColumnName(col) + ")", LinkGrabberV2TreeTableAction.SORT, new Property("col", col))));
                popup.add(new JSeparator());
            }
            if (obj instanceof LinkGrabberV2FilePackage || obj instanceof DownloadLink) {
                popup.add(buildpriomenu(alllinks));
                Set<String> hoster = linkgrabber.getHosterList(alllinks);
                popup.add(new JMenuItem(new LinkGrabberV2TreeTableAction(linkgrabber, JDLocale.L("gui.linkgrabberv2.onlyselectedhoster", "Keep only selected Hoster") + " (" + hoster.size() + ")", LinkGrabberV2TreeTableAction.SELECT_HOSTER, new Property("hoster", hoster))));
                popup.add(new JMenuItem(new LinkGrabberV2TreeTableAction(linkgrabber, JDLocale.L("gui.table.contextmenu.newpackage", "In neues Paket verschieben") + " (" + alllinks.size() + ")", LinkGrabberV2TreeTableAction.NEW_PACKAGE, new Property("links", alllinks))));
                popup.add(new JMenuItem(new LinkGrabberV2TreeTableAction(linkgrabber, JDLocale.L("gui.table.contextmenu.setdlpw", "Set download password") + " (" + alllinks.size() + ")", LinkGrabberV2TreeTableAction.SET_PW, new Property("links", alllinks))));
                popup.add(new JSeparator());
                HashMap<String, Object> prop = new HashMap<String, Object>();
                prop.put("links", alllinks);
                prop.put("boolean", true);
                if (links_disabled > 0) popup.add(new JMenuItem(new LinkGrabberV2TreeTableAction(linkgrabber, JDLocale.L("gui.table.contextmenu.enable", "aktivieren") + " (" + links_disabled + ")", LinkGrabberV2TreeTableAction.DE_ACTIVATE, new Property("infos", prop))));
                prop = new HashMap<String, Object>();
                prop.put("links", alllinks);
                prop.put("boolean", false);
                if (links_enabled > 0) popup.add(new JMenuItem(new LinkGrabberV2TreeTableAction(linkgrabber, JDLocale.L("gui.table.contextmenu.disable", "deaktivieren") + " (" + links_enabled + ")", LinkGrabberV2TreeTableAction.DE_ACTIVATE, new Property("infos", prop))));
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
            prioPopup.add(tmp = new JMenuItem(new LinkGrabberV2TreeTableAction(linkgrabber, Integer.toString(i), LinkGrabberV2TreeTableAction.DOWNLOAD_PRIO, new Property("infos", prop))));
            if (prio != null && i == prio) {
                tmp.setEnabled(false);
            } else
                tmp.setEnabled(true);
        }
        return prioPopup;
    }

    // @SuppressWarnings("unchecked")
    // public Painter getGradientPainter() {
    // // int height = 20;
    // // Color color2;
    // // color1 = new Color(color1.getRed(), color1.getGreen(),
    // color1.getBlue(), 40);
    // // color2 = new Color(color1.getRed(), color1.getGreen(),
    // color1.getBlue(), 200);
    // // LinearGradientPaint gradientPaint = new LinearGradientPaint(1, 0, 1,
    // height, new float[] { 0.0f, 1.0f }, new Color[] { color1, color2 });
    // // MattePainter mattePainter = new MattePainter(gradientPaint);
    // return new MattePainter(new Color(0xff, 0xfa, 0x7c, 255));
    // }

    private void addOnlineHighlighter() {
        Color background = JDTheme.C("gui.color.linkgrabber.online", "c4ffd2", 120);

        addHighlighter(new DownloadLinkRowHighlighter(this, background, background) {
            @Override
            public boolean doHighlight(DownloadLink link) {
                if (link.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) return false;
                if (link.isAvailabilityChecked() && link.isAvailable()) return true;
                return false;
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

    private void addPackageHighlighter() {

        addHighlighter(new PainterHighlighter(new HighlightPredicate() {

            public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
                TreePath path = LinkGrabberV2TreeTable.this.getPathForRow(adapter.row);
                Object element;
                if (path != null) {
                    element = path.getLastPathComponent();
                    if (element instanceof LinkGrabberV2FilePackage) { return true; }
                }
                return false;
            }

        }, DownloadTreeTable.getFolderPainter(this)));

    }

    private void addOfflineHighlighter() {
        /* TODO: andre farbe auswählen */
        Color background = JDTheme.C("gui.color.linkgrabber.error_post", "ff0000", 120);

        addHighlighter(new DownloadLinkRowHighlighter(this, background, background) {
            @Override
            public boolean doHighlight(DownloadLink link) {
                if (link.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) return false;
                if (link.isAvailabilityChecked() && !link.isAvailable()) return true;
                return false;
            }
        });

    }

    private void addExistsHighlighter() {
        /* TODO: andre farbe auswählen */

        Color background = JDTheme.C("gui.color.linkgrabber.error_exists", "ff7f00", 120);

        addHighlighter(new DownloadLinkRowHighlighter(this, background, background) {
            @Override
            public boolean doHighlight(DownloadLink link) {
                if (link.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) return true;
                return false;
            }
        });

    }

    private void addUncheckedHighlighter() {
        // /* TODO: andre farbe auswählen */
        // Color background = JDTheme.C("gui.color.linkgrabber.error_post",
        // "ff7f00");
        // Color foreground = Color.DARK_GRAY;
        // Color selectedBackground = background.darker();
        // Color selectedForground = foreground;
        //
        // addHighlighter(new LinkGrabberV2DownloadLinkRowHighlighter(this,
        // background, foreground, selectedBackground, selectedForground) {
        // @Override
        // public boolean doHighlight(DownloadLink link) {
        // if (!link.isAvailabilityChecked()) return true;
        // return false;
        // }
        // });

    }

    private void createColumns() {
        // TODO Auto-generated method stub
        setAutoCreateColumnsFromModel(false);
        List<TableColumn> columns = getColumns(true);
        for (Iterator<TableColumn> iter = columns.iterator(); iter.hasNext();) {
            getColumnModel().removeColumn(iter.next());
        }
        final SubConfiguration config = JDUtilities.getSubConfig("linkgrabber");
        cols = new TableColumnExt[getModel().getColumnCount()];
        for (int i = 0; i < getModel().getColumnCount(); i++) {

            TableColumnExt tableColumn = getColumnFactory().createAndConfigureTableColumn(getModel(), i);
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
                tableColumn.setPreferredWidth(config.getIntegerProperty("WIDTH_COL_" + i, tableColumn.getWidth()));
                if (tableColumn != null) {
                    getColumnModel().addColumn(tableColumn);
                }
            } else {
                tableColumn.setVisible(false);
            }
        }
    }

}
