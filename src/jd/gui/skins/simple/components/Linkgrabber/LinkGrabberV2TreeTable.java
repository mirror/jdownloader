package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreePath;

import jd.config.Property;
import jd.config.SubConfiguration;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.treetable.DownloadLinkRowHighlighter;
import jd.gui.skins.simple.components.treetable.DownloadTreeTable;
import jd.gui.skins.simple.components.treetable.JColumnControlButton;
import jd.gui.skins.simple.components.treetable.TreeTableRenderer;
import jd.gui.skins.simple.components.treetable.TreeTableTransferHandler;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDLocale;
import jd.utils.JDSounds;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jdesktop.swingx.decorator.PainterHighlighter;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.tree.TreeModelSupport;

public class LinkGrabberV2TreeTable extends JXTreeTable implements ActionListener, MouseListener, MouseMotionListener, TreeExpansionListener, TreeSelectionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private LinkGrabberV2TreeTableModel model;
    public int mouseOverRow;
    public int mouseOverColumn;
    private boolean update = true;
    private boolean update2 = true;
    private LinkGrabberV2 linkgrabber;
    private LinkGrabberV2TreeTableRenderer cellRenderer;
    private TableColumnExt[] cols;

    public static final String PROPERTY_EXPANDED = "lg_expanded";

    public static final String PROPERTY_SELECTED = "lg_selected";

    public LinkGrabberV2TreeTable(LinkGrabberV2TreeTableModel treeModel, LinkGrabberV2 linkgrabber) {
        super(treeModel);
        this.linkgrabber = linkgrabber;
        JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
        cellRenderer = new LinkGrabberV2TreeTableRenderer(this);
        model = treeModel;
        createColumns();

        getTableHeader().setReorderingAllowed(false);
        getTableHeader().setResizingAllowed(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setEditable(false);
        setAutoscrolls(false);
        setColumnControlVisible(true);
        this.setColumnControl(new JColumnControlButton(this));
        addTreeExpansionListener(this);
        addTreeSelectionListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        UIManager.put("Table.focusCellHighlightBorder", null);
        this.setHighlighters(new Highlighter[] {});
        setHighlighters(HighlighterFactory.createAlternateStriping(UIManager.getColor("Panel.background").brighter(), UIManager.getColor("Panel.background")));
        addOfflineHighlighter();
        addOnlineHighlighter();
        addExistsHighlighter();
        addUncheckedHighlighter();
        addHighlighter(new PainterHighlighter(HighlightPredicate.IS_FOLDER, getGradientPainter(JDTheme.C("gui.color.linkgrabber.row_package", "fffa7c"))));
    }

    public TableCellRenderer getCellRenderer(int row, int col) {
        return cellRenderer;
    }

    public synchronized void fireTableChanged(int id, Object param) {
        TreeModelSupport supporter = getLinkGrabberV2TreeTableModel().getModelSupporter();
        update = false;
        supporter.fireTreeStructureChanged(new TreePath(model.getRoot()));
        updateSelectionAndExpandStatus();
        update = true;
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
        // TODO Auto-generated method stub

    }

    public void mouseMoved(MouseEvent e) {
        mouseOverRow = rowAtPoint(e.getPoint());
        mouseOverColumn = columnAtPoint(e.getPoint());
    }

    public void updateSelectionAndExpandStatus() {
        System.out.println("update selecetion");
        update2 = false;
        int i = 0;
        while (getPathForRow(i) != null) {
            if (getPathForRow(i).getLastPathComponent() instanceof DownloadLink) {
                DownloadLink dl = (DownloadLink) getPathForRow(i).getLastPathComponent();
                if (dl.getBooleanProperty(PROPERTY_SELECTED, false)) {
                    getTreeSelectionModel().addSelectionPath(getPathForRow(i));
                }
            } else {
                LinkGrabberV2FilePackage fp = (LinkGrabberV2FilePackage) getPathForRow(i).getLastPathComponent();
                if (fp.getBooleanProperty(PROPERTY_EXPANDED, false)) {
                    expandPath(getPathForRow(i));
                }
                if (fp.getBooleanProperty(PROPERTY_SELECTED, false)) {
                    getTreeSelectionModel().addSelectionPath(getPathForRow(i));
                }
            }
            i++;
        }
        update2 = true;
    }

    public void valueChanged(TreeSelectionEvent e) {
        TreePath[] paths = e.getPaths();
        System.out.println("" + update);
        if (update == false || update2 == false) return;
        for (TreePath path : paths) {
            if (e.isAddedPath(path)) {
                if (path.getLastPathComponent() instanceof DownloadLink) {
                    ((DownloadLink) path.getLastPathComponent()).setProperty(PROPERTY_SELECTED, true);

                } else {
                    ((LinkGrabberV2FilePackage) path.getLastPathComponent()).setProperty(PROPERTY_SELECTED, true);

                }
            } else {

                if (path.getLastPathComponent() instanceof DownloadLink) {
                    ((DownloadLink) path.getLastPathComponent()).setProperty(PROPERTY_SELECTED, false);

                } else {
                    ((LinkGrabberV2FilePackage) path.getLastPathComponent()).setProperty(PROPERTY_SELECTED, false);

                }
            }
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

    public void mouseClicked(MouseEvent e) {
        Point point = e.getPoint();
        int row = rowAtPoint(point);
        if (getPathForRow(row) == null) { return; }
        Object obj = getPathForRow(row).getLastPathComponent();
        if (obj instanceof LinkGrabberV2FilePackage) {
            linkgrabber.showFilePackageInfo((LinkGrabberV2FilePackage) obj);
        } else {
            linkgrabber.hideFilePackageInfo();
        }
    }

    public void mouseEntered(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void mouseExited(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void mouseReleased(MouseEvent e) {
        TreePath path = getPathForLocation(e.getX(), e.getY());
        int column = this.columnAtPoint(e.getPoint());
        if (path != null && path.getLastPathComponent() instanceof LinkGrabberV2FilePackage) {
            if (column == 0) {
                LinkGrabberV2FilePackage fp = (LinkGrabberV2FilePackage) path.getLastPathComponent();
                if (fp.getBooleanProperty(PROPERTY_EXPANDED, false)) {
                    this.collapsePath(path);
                } else {
                    expandPath(path);
                }
            }
        }
    }

    public void mousePressed(MouseEvent e) {
        Point point = e.getPoint();
        int row = rowAtPoint(point);

        if (!isRowSelected(row) && e.getButton() == MouseEvent.BUTTON3) {
            getTreeSelectionModel().clearSelection();
            getTreeSelectionModel().addSelectionPath(getPathForRow(row));
        }
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {

            if (getPathForRow(row) == null) { return; }
            Object obj = getPathForRow(row).getLastPathComponent();
            JPopupMenu popup = new JPopupMenu();
            if (obj instanceof DownloadLink) {
                popup.add(buildpriomenuDownloadLink((DownloadLink) obj));
                popup.show(this, point.x, point.y);
            }
        }
    }

    private JMenu buildpriomenuDownloadLink(DownloadLink link) {
        JMenuItem tmp;
        JMenu prioPopup = new JMenu(JDLocale.L("gui.table.contextmenu.priority", "Priority"));
        int prio = link.getPriority();
        HashMap<String, Object> prop = null;
        Vector<DownloadLink> links = getAllSelectedDownloadLinks();
        for (int i = 4; i >= -4; i--) {
            prop = new HashMap<String, Object>();
            prop.put("downloadlinks", links);
            prop.put("prio", new Integer(i));
            prioPopup.add(tmp = new JMenuItem(new LinkGrabberV2TreeTableAction(this, Integer.toString(i), LinkGrabberV2TreeTableAction.DOWNLOAD_PRIO, new Property("infos", prop))));
            if (i == prio) {
                tmp.setEnabled(false);
            } else
                tmp.setEnabled(true);
        }
        return prioPopup;
    }

    private JMenu buildpriomenuFilePackage(Vector<FilePackage> fps) {
        JMenuItem tmp;
        JMenu prioPopup = new JMenu(JDLocale.L("gui.table.contextmenu.priority", "Priority"));
        HashMap<String, Object> prop = null;
        for (int i = 4; i >= -4; i--) {
            prop = new HashMap<String, Object>();
            prop.put("packages", fps);
            prop.put("prio", new Integer(i));
            prioPopup.add(tmp = new JMenuItem(new LinkGrabberV2TreeTableAction(this, Integer.toString(i), LinkGrabberV2TreeTableAction.PACKAGE_PRIO, new Property("infos", prop))));
            tmp.setEnabled(true);
        }
        return prioPopup;
    }

    public Painter getGradientPainter(Color color1) {
        int height = 20;
        Color color2;
        color1 = new Color(color1.getRed(), color1.getGreen(), color1.getBlue(), 40);
        color2 = new Color(color1.getRed(), color1.getGreen(), color1.getBlue(), 200);
        LinearGradientPaint gradientPaint = new LinearGradientPaint(1, 0, 1, height, new float[] { 0.0f, 1.0f }, new Color[] { color1, color2 });
        MattePainter mattePainter = new MattePainter(gradientPaint);
        return mattePainter;
    }

    private void addOnlineHighlighter() {
        Color background = JDTheme.C("gui.color.linkgrabber.online", "00ff00");
        Color foreground = Color.DARK_GRAY;
        Color selectedBackground = background.darker();
        Color selectedForground = foreground;

        addHighlighter(new LinkGrabberV2DownloadLinkRowHighlighter(this, background, foreground, selectedBackground, selectedForground) {
            @Override
            public boolean doHighlight(DownloadLink link) {
                if (link.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) return false;
                if (link.isAvailabilityChecked() && link.isAvailable()) return true;
                return false;
            }
        });

    }

    private void addOfflineHighlighter() {
        Color background = JDTheme.C("gui.color.downloadlist.error_post", "ff0000");
        Color foreground = Color.DARK_GRAY;
        Color selectedBackground = background.darker();
        Color selectedForground = foreground;

        addHighlighter(new LinkGrabberV2DownloadLinkRowHighlighter(this, background, foreground, selectedBackground, selectedForground) {
            @Override
            public boolean doHighlight(DownloadLink link) {
                if (link.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) return false;
                if (link.isAvailabilityChecked() && !link.isAvailable()) return true;
                return false;
            }
        });

    }

    private void addExistsHighlighter() {
        Color background = JDTheme.C("gui.color.downloadlist.error_post", "ff7f00");
        Color foreground = Color.DARK_GRAY;
        Color selectedBackground = background.darker();
        Color selectedForground = foreground;

        addHighlighter(new LinkGrabberV2DownloadLinkRowHighlighter(this, background, foreground, selectedBackground, selectedForground) {
            @Override
            public boolean doHighlight(DownloadLink link) {
                if (link.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) return true;
                return false;
            }
        });
    }

    private void addUncheckedHighlighter() {
        Color background = JDTheme.C("gui.color.downloadlist.error_post", "ff7f00");
        Color foreground = Color.DARK_GRAY;
        Color selectedBackground = background.darker();
        Color selectedForground = foreground;

        addHighlighter(new LinkGrabberV2DownloadLinkRowHighlighter(this, background, foreground, selectedBackground, selectedForground) {
            @Override
            public boolean doHighlight(DownloadLink link) {
                if (!link.isAvailabilityChecked()) return true;
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

    public void actionPerformed(ActionEvent arg0) {
        // TODO Auto-generated method stub

    }

}
