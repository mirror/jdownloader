package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreePath;

import jd.config.MenuItem;
import jd.config.Property;
import jd.event.ControlEvent;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.ComboBrowseFile;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.JXTreeTable;
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

    public static final String PROPERTY_EXPANDED = "expanded";

    public static final String PROPERTY_SELECTED = "selected";

    public LinkGrabberV2TreeTable(LinkGrabberV2TreeTableModel treeModel, LinkGrabberV2 linkgrabber) {
        super(treeModel);
        model = treeModel;
        this.linkgrabber = linkgrabber;
        this.setUI(new LinkGrabberV2TreeTablePaneUI());
        getTableHeader().setReorderingAllowed(false);
        getTableHeader().setResizingAllowed(true);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        setEditable(true);
        setAutoscrolls(false);
        addTreeExpansionListener(this);
        addTreeSelectionListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);
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

    public void mouseReleased(MouseEvent arg0) {
        // TODO Auto-generated method stub

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

    public void actionPerformed(ActionEvent arg0) {
        // TODO Auto-generated method stub

    }

}
