package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreePath;

import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.LinkGrabberController;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.gui.skins.simple.components.DownloadView.DownloadLinkRowHighlighter;
import jd.gui.skins.simple.components.DownloadView.DownloadTreeTable;
import jd.gui.skins.simple.components.DownloadView.JColumnControlButton;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.utils.JDLocale;
import jd.utils.JDTheme;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.PainterHighlighter;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.tree.TreeModelSupport;

public class LinkGrabberTreeTable extends JXTreeTable implements MouseListener, MouseMotionListener, TreeExpansionListener, KeyListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private LinkGrabberTreeTableModel model;
    protected LinkGrabberPanel linkgrabber;
    private LinkGrabberTreeTableRenderer cellRenderer;
    private TableColumnExt[] cols;

    public static final String PROPERTY_EXPANDED = "lg_expanded";
    public static final String PROPERTY_SELECTED = "lg_selected";

    public LinkGrabberPanel getLinkGrabber() {
        return linkgrabber;
    }

    public LinkGrabberTreeTable(LinkGrabberTreeTableModel treeModel, final LinkGrabberPanel linkgrabber) {
        super(treeModel);
        this.linkgrabber = linkgrabber;
        SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME);
        cellRenderer = new LinkGrabberTreeTableRenderer(this);
        model = treeModel;
        createColumns();

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
        addKeyListener(this);
        addMouseMotionListener(this);
        this.getTableHeader().addMouseListener(this);
        UIManager.put("Table.focusCellHighlightBorder", null);
        setHighlighters(new Highlighter[] {});
        addPackageHighlighter();

        addDisabledHighlighter();

        
        addExistsHighlighter();        
        setTransferHandler(new LinkGrabberTreeTableTransferHandler(this));
        getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                if (getSelectedRow() < 0) return;
                if (getPathForRow(getSelectedRow()) == null) return;
                Object obj = getPathForRow(getSelectedRow()).getLastPathComponent();

                if (obj == null) {
                    linkgrabber.hideFilePackageInfo();
                }

                LinkGrabberFilePackage pkg = null;
                if (obj instanceof LinkGrabberFilePackage) {
                    pkg = (LinkGrabberFilePackage) obj;

                } else {
                    pkg = LinkGrabberController.getInstance().getFPwithLink((DownloadLink) obj);
                }
                linkgrabber.showFilePackageInfo(pkg);

            }
        });
    }

    public TableCellRenderer getCellRenderer(int row, int col) {
        return cellRenderer;
    }

    public synchronized void fireTableChanged(int id, Object param) {
        TreeModelSupport supporter = getLinkGrabberV2TreeTableModel().getModelSupporter();
        supporter.fireTreeStructureChanged(new TreePath(model.getRoot()));
        updateSelectionAndExpandStatus();
    }

    public LinkGrabberTreeTableModel getLinkGrabberV2TreeTableModel() {
        return (LinkGrabberTreeTableModel) getTreeTableModel();
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
        Vector<LinkGrabberFilePackage> fps = getSelectedFilePackages();
        for (LinkGrabberFilePackage filePackage : fps) {
            for (DownloadLink dl : filePackage.getDownloadLinks()) {
                if (!links.contains(dl)) links.add(dl);
            }
        }
        return links;
    }

    public Vector<LinkGrabberFilePackage> getSelectedFilePackages() {
        int[] rows = getSelectedRows();
        Vector<LinkGrabberFilePackage> ret = new Vector<LinkGrabberFilePackage>();
        TreePath path;
        for (int element : rows) {
            path = getPathForRow(element);
            if (path != null && path.getLastPathComponent() instanceof LinkGrabberFilePackage) {
                ret.add((LinkGrabberFilePackage) path.getLastPathComponent());
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
            if (getPathForRow(i).getLastPathComponent() instanceof LinkGrabberFilePackage) {
                LinkGrabberFilePackage fp = (LinkGrabberFilePackage) getPathForRow(i).getLastPathComponent();
                if (fp.getBooleanProperty(PROPERTY_EXPANDED, false)) {
                    expandPath(getPathForRow(i));
                }
            }
            i++;
        }
    }

    public void treeCollapsed(TreeExpansionEvent event) {
        LinkGrabberFilePackage fp = (LinkGrabberFilePackage) event.getPath().getLastPathComponent();
        fp.setProperty(PROPERTY_EXPANDED, false);
    }

    public void treeExpanded(TreeExpansionEvent event) {
        LinkGrabberFilePackage fp = (LinkGrabberFilePackage) event.getPath().getLastPathComponent();
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
            LinkGrabberTreeTableAction test = new LinkGrabberTreeTableAction(linkgrabber, JDLocale.L("gui.table.contextmenu.packagesort", "Paket sortieren"), LinkGrabberTreeTableAction.SORT_ALL, new Property("col", col));
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
        if (e.getClickCount() == 1) {

            if (e.getSource() != this) return;
            TreePath path = getPathForLocation(e.getX(), e.getY());
            if (path == null) return;
            int column = getRealcolumnAtPoint(e.getX());
            if (path != null && path.getLastPathComponent() instanceof LinkGrabberFilePackage) {
                if (column == 1 && e.getButton() == MouseEvent.BUTTON1) {
                    System.out.println(e.getClickCount());

                    LinkGrabberFilePackage fp = (LinkGrabberFilePackage) path.getLastPathComponent();
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
            Vector<LinkGrabberFilePackage> sfp = getSelectedFilePackages();
            Object obj = getPathForRow(row).getLastPathComponent();
            JPopupMenu popup = new JPopupMenu();
            if (obj instanceof LinkGrabberFilePackage || obj instanceof DownloadLink) {
                popup.add(new JMenuItem(new LinkGrabberTreeTableAction(linkgrabber, JDLocale.L("gui.linkgrabberv2.lg.addall", "Add all packages"), LinkGrabberTreeTableAction.ADD_ALL)));
                popup.add(new JMenuItem(new LinkGrabberTreeTableAction(linkgrabber, JDLocale.L("gui.linkgrabberv2.lg.rmoffline", "Remove all Offline"), LinkGrabberTreeTableAction.DELETE_OFFLINE)));
                popup.add(new JMenuItem(new LinkGrabberTreeTableAction(linkgrabber, JDLocale.L("gui.table.contextmenu.delete", "entfernen") + " (" + alllinks.size() + ")", LinkGrabberTreeTableAction.DELETE, new Property("links", alllinks))));
                if (sfp.size() > 0) popup.add(new JMenuItem(new LinkGrabberTreeTableAction(linkgrabber, JDLocale.L("gui.linkgrabberv2.lg.addselected", "Add selected package(s)") + " (" + sfp.size() + ")", LinkGrabberTreeTableAction.ADD_SELECTED)));
                popup.add(new JSeparator());
            }
            if (obj instanceof LinkGrabberFilePackage) {
                popup.add(new JMenuItem(new LinkGrabberTreeTableAction(linkgrabber, JDLocale.L("gui.table.contextmenu.editdownloadDir", "Zielordner ändern") + " (" + sfp.size() + ")", LinkGrabberTreeTableAction.EDIT_DIR)));
                popup.add(new JMenuItem(new LinkGrabberTreeTableAction(linkgrabber, JDLocale.L("gui.table.contextmenu.packagesort", "Paket sortieren") + " (" + sfp.size() + "), (" + this.getModel().getColumnName(col) + ")", LinkGrabberTreeTableAction.SORT, new Property("col", col))));
                popup.add(new JSeparator());
            }
            if (obj instanceof LinkGrabberFilePackage || obj instanceof DownloadLink) {
                popup.add(buildpriomenu(alllinks));
                Set<String> hoster = linkgrabber.getHosterList(alllinks);
                popup.add(new JMenuItem(new LinkGrabberTreeTableAction(linkgrabber, JDLocale.L("gui.linkgrabberv2.onlyselectedhoster", "Keep only selected Hoster") + " (" + hoster.size() + ")", LinkGrabberTreeTableAction.SELECT_HOSTER, new Property("hoster", hoster))));
                popup.add(new JMenuItem(new LinkGrabberTreeTableAction(linkgrabber, JDLocale.L("gui.table.contextmenu.newpackage", "In neues Paket verschieben") + " (" + alllinks.size() + ")", LinkGrabberTreeTableAction.NEW_PACKAGE, new Property("links", alllinks))));
                popup.add(new JMenuItem(new LinkGrabberTreeTableAction(linkgrabber, JDLocale.L("gui.table.contextmenu.setdlpw", "Set download password") + " (" + alllinks.size() + ")", LinkGrabberTreeTableAction.SET_PW, new Property("links", alllinks))));
                popup.add(new JSeparator());
                HashMap<String, Object> prop = new HashMap<String, Object>();
                prop.put("links", alllinks);
                prop.put("boolean", true);
                if (links_disabled > 0) popup.add(new JMenuItem(new LinkGrabberTreeTableAction(linkgrabber, JDLocale.L("gui.table.contextmenu.enable", "aktivieren") + " (" + links_disabled + ")", LinkGrabberTreeTableAction.DE_ACTIVATE, new Property("infos", prop))));
                prop = new HashMap<String, Object>();
                prop.put("links", alllinks);
                prop.put("boolean", false);
                if (links_enabled > 0) popup.add(new JMenuItem(new LinkGrabberTreeTableAction(linkgrabber, JDLocale.L("gui.table.contextmenu.disable", "deaktivieren") + " (" + links_enabled + ")", LinkGrabberTreeTableAction.DE_ACTIVATE, new Property("infos", prop))));
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
            prioPopup.add(tmp = new JMenuItem(new LinkGrabberTreeTableAction(linkgrabber, Integer.toString(i), LinkGrabberTreeTableAction.DOWNLOAD_PRIO, new Property("infos", prop))));
            if (prio != null && i == prio) {
                tmp.setEnabled(false);
            } else
                tmp.setEnabled(true);
        }
        return prioPopup;
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
                TreePath path = LinkGrabberTreeTable.this.getPathForRow(adapter.row);
                Object element;
                if (path != null) {
                    element = path.getLastPathComponent();
                    if (element instanceof LinkGrabberFilePackage) { return true; }
                }
                return false;
            }

        }, DownloadTreeTable.getFolderPainter(this)));

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

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            Vector<DownloadLink> alllinks = getAllSelectedDownloadLinks();
            LinkGrabberTreeTableAction test = new LinkGrabberTreeTableAction(linkgrabber, JDLocale.L("gui.table.contextmenu.delete", "entfernen") + " (" + alllinks.size() + ")", LinkGrabberTreeTableAction.DELETE, new Property("links", alllinks));
            test.actionPerformed(new ActionEvent(test, 0, ""));
        }
    }

    public void keyPressed(KeyEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void keyTyped(KeyEvent arg0) {
        // TODO Auto-generated method stub

    }

}
