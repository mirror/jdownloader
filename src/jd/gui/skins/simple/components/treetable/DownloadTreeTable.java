/**
 * 
 */
package jd.gui.skins.simple.components.treetable;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.DropMode;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreePath;

import jd.config.Property;
import jd.event.ControlEvent;
import jd.gui.skins.simple.DownloadInfo;
import jd.gui.skins.simple.DownloadLinksView;
import jd.gui.skins.simple.PackageInfo;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.BrowseFile;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.config.GetExplorer;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.tree.TreeModelSupport;

import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

public class DownloadTreeTable extends JXTreeTable implements TreeExpansionListener, TreeSelectionListener, MouseListener, ActionListener {
    private Logger                 logger            = JDUtilities.getLogger();

    public static final String     PROPERTY_EXPANDED = "expanded";

    public static final String     PROPERTY_SELECTED = "selected";

    private DownloadTreeTableModel model;

    private TableCellRenderer      cellRenderer;

    public DownloadTreeTable(DownloadTreeTableModel treeModel) {
        super(treeModel);

        this.cellRenderer = new TreeTableRenderer();
        this.model = treeModel;
        this.setUI(new TreeTablePaneUI());
        this.getTableHeader().setReorderingAllowed(false);
        this.getTableHeader().setResizingAllowed(false);
        this.setDropMode(DropMode.INSERT_ROWS);
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        this.setColumnControlVisible(true);

        this.setEditable(false);

        setDragEnabled(true);
        setAutoscrolls(false);
        addTreeExpansionListener(this);
        addTreeSelectionListener(this);
        addMouseListener(this);
        this.setTransferHandler(new TreeTableTransferHandler(this));

    }

    /**
     * Diese Methode setzt die gespeicherten Werte für die Selection und
     * Expansion
     */
    public void updateSelectionAndExpandStatus() {
        // logger.info("UPD");
        int i = 0;
        FilePackage fp;
        DownloadLink dl;
        while (getPathForRow(i) != null) {
            if (getPathForRow(i).getLastPathComponent() instanceof DownloadLink) {
                dl = (DownloadLink) getPathForRow(i).getLastPathComponent();
                if (dl.getBooleanProperty(PROPERTY_SELECTED, false)) {
                    getTreeSelectionModel().addSelectionPath(getPathForRow(i));

                }
            }
            else {
                fp = (FilePackage) getPathForRow(i).getLastPathComponent();
                if (fp.getBooleanProperty(PROPERTY_EXPANDED, false)) {
                    expandPath(getPathForRow(i));
                }
                if (fp.getBooleanProperty(PROPERTY_SELECTED, false)) {
                    getTreeSelectionModel().addSelectionPath(getPathForRow(i));

                }
            }
            i++;
        }

    }

    public TableCellRenderer getCellRenderer(int row, int col) {
        if (col >= 1) return cellRenderer;
        return super.getCellRenderer(row, col);
    }

    public DownloadTreeTableModel getDownladTreeTableModel() {

        return (DownloadTreeTableModel) getTreeTableModel();
    }

    public void setColumnSizes() {
        TableColumn column = null;
        for (int c = 0; c < getColumnModel().getColumnCount(); c++) {
            column = getColumnModel().getColumn(c);

            switch (c) {

                case DownloadTreeTableModel.COL_PART:
                    column.setPreferredWidth(110);
                    column.setMinWidth(110);
                    // column.setMaxWidth(140);
                    break;
                case DownloadTreeTableModel.COL_FILE:
                    column.setPreferredWidth(160);
                    // column.setMinWidth(180);
                    break;
                case DownloadTreeTableModel.COL_HOSTER:
                    column.setPreferredWidth(100);
                    column.setMaxWidth(100);
                    break;
                case DownloadTreeTableModel.COL_STATUS:
                    column.setPreferredWidth(160);
                    column.setMaxWidth(160);
                    break;
                case DownloadTreeTableModel.COL_PROGRESS:
                    column.setPreferredWidth(160);
                     column.setMaxWidth(160);
                    break;
            }
        }

    }

    /**
     * Wird von der UI aufgerufen sobald die table komplett refreshed wurde
     */
    public void onRefresh() {

        setColumnSizes();
        // updateSelectionAndExpandStatus();

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

    public void valueChanged(TreeSelectionEvent e) {

        TreePath[] paths = e.getPaths();
        // logger.info("" + e);

        for (TreePath path : paths) {
            if (e.isAddedPath(path)) {
                if (path.getLastPathComponent() instanceof DownloadLink) {
                    ((DownloadLink) path.getLastPathComponent()).setProperty(DownloadTreeTable.PROPERTY_SELECTED, true);
                    // logger.info("SELECTED " + ((DownloadLink)
                    // path.getLastPathComponent()));
                }
                else {
                    ((FilePackage) path.getLastPathComponent()).setProperty(DownloadTreeTable.PROPERTY_SELECTED, true);
                    // logger.info("SELECTED " + ((FilePackage)
                    // path.getLastPathComponent()));
                }
            }
            else {

                if (path.getLastPathComponent() instanceof DownloadLink) {
                    ((DownloadLink) path.getLastPathComponent()).setProperty(DownloadTreeTable.PROPERTY_SELECTED, false);
                    // logger.info("NOT SELECTED " + ((DownloadLink)
                    // path.getLastPathComponent()));
                }
                else {
                    ((FilePackage) path.getLastPathComponent()).setProperty(DownloadTreeTable.PROPERTY_SELECTED, false);
                    // logger.info("NOT SELECTED " + ((FilePackage)
                    // path.getLastPathComponent()));
                }
            }
        }

    }

    public void fireTableChanged(int id) {
        TreeModelSupport supporter = getDownladTreeTableModel().getModelSupporter();
        switch (id) {
            case DownloadLinksView.REFRESH_DATA_AND_STRUCTURE_CHANGED:
                logger.info("REFRESH GUI COMPLETE");

                supporter.fireTreeStructureChanged(new TreePath(model.getRoot()));

                // ignoreSelectionsAndExpansions(200);
                updateSelectionAndExpandStatus();
                logger.info("finished");

                break;
            case DownloadLinksView.REFRESH_ONLY_DATA_CHANGED:
                // supporter.fireTreeStructureChanged(new
                // TreePath(model.getRoot()));
                // updateSelectionAndExpandStatus();
                supporter.fireChildrenChanged(new TreePath(model.getRoot()), null, null);
                break;
        }

    }

    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && 2 == e.getClickCount()) {
            Point point = e.getPoint();
            int row = this.rowAtPoint(point);
            Object obj = getPathForRow(row).getLastPathComponent();
            if (obj instanceof DownloadLink) {
                new DownloadInfo(SimpleGUI.CURRENTGUI.getFrame(), (DownloadLink) getPathForRow(row).getLastPathComponent());
            }
        }
    }

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {
        // TODO: isPopupTrigger() funktioniert nicht
        // logger.info("Press"+e.isPopupTrigger() );
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            Point point = e.getPoint();
            int row = this.rowAtPoint(point);
            int column = this.columnAtPoint(point);
            if(getPathForRow(row)==null)return;
            Object obj = getPathForRow(row).getLastPathComponent();

            if (obj instanceof DownloadLink) {
                TreeTableAction action;
                JPopupMenu popup = new JPopupMenu();
                popup.add(new JMenuItem(new TreeTableAction(this, "info", TreeTableAction.DOWNLOAD_INFO, new Property("downloadlink", obj))));
                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, "downloadDir", TreeTableAction.DOWNLOAD_DOWNLOAD_DIR, new Property("downloadlink", obj))));
                popup.add(new JMenuItem(new TreeTableAction(this, "browseLink", TreeTableAction.DOWNLOAD_BROWSE_LINK, new Property("downloadlink", obj))));

                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, "delete", TreeTableAction.DOWNLOAD_DELETE, new Property("downloadlinks", getSelectedDownloadLinks()))));
                popup.add(new JMenuItem(new TreeTableAction(this, "enable", TreeTableAction.DOWNLOAD_ENABLE, new Property("downloadlinks", getSelectedDownloadLinks()))));
                popup.add(new JMenuItem(new TreeTableAction(this, "disable", TreeTableAction.DOWNLOAD_DISABLE, new Property("downloadlinks", getSelectedDownloadLinks()))));
                popup.add(new JMenuItem(new TreeTableAction(this, "newpackage", TreeTableAction.DOWNLOAD_NEW_PACKAGE, new Property("downloadlinks", getSelectedDownloadLinks()))));

                popup.add(new JMenuItem(new TreeTableAction(this, "reset", TreeTableAction.DOWNLOAD_RESET, new Property("downloadlinks", getSelectedDownloadLinks()))));
                // popup.add(new JMenuItem(new TreeTableAction(this,
                // "forcedownload", TreeTableAction.DOWNLOAD_FORCE,new
                // Property("downloadlinks",getSelectedDownloadLinks()))));
                popup.show(this, point.x, point.y);
            }
            else {
                TreeTableAction action;
                JPopupMenu popup = new JPopupMenu();
                popup.add(new JMenuItem(new TreeTableAction(this, "packageinfo", TreeTableAction.PACKAGE_INFO, new Property("package", obj))));
                popup.add(new JMenuItem(new TreeTableAction(this, "packagesort", TreeTableAction.PACKAGE_SORT, new Property("package", obj))));

                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, "editdownloadDir", TreeTableAction.PACKAGE_EDIT_DIR, new Property("package", obj))));
                popup.add(new JMenuItem(new TreeTableAction(this, "editpackagename", TreeTableAction.PACKAGE_EDIT_NAME, new Property("package", obj))));

                popup.add(new JMenuItem(new TreeTableAction(this, "packagedownloadDir", TreeTableAction.PACKAGE_DOWNLOAD_DIR, new Property("package", obj))));

                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, "deletepackage", TreeTableAction.PACKAGE_DELETE, new Property("packages", this.getSelectedFilePackages()))));
                popup.add(new JMenuItem(new TreeTableAction(this, "enablepackage", TreeTableAction.PACKAGE_ENABLE, new Property("packages", getSelectedFilePackages()))));
                popup.add(new JMenuItem(new TreeTableAction(this, "disablepackage", TreeTableAction.PACKAGE_DISABLE, new Property("packages", getSelectedFilePackages()))));

                popup.add(new JMenuItem(new TreeTableAction(this, "resetpackage", TreeTableAction.PACKAGE_RESET, new Property("packages", getSelectedFilePackages()))));
                // popup.add(new JMenuItem(new TreeTableAction(this,
                // "forcedownload", TreeTableAction.DOWNLOAD_FORCE,new
                // Property("downloadlinks",getSelectedDownloadLinks()))));
                popup.show(this, point.x, point.y);
            }
            logger.info(getSelectedFilePackages() + "");
            logger.info(getSelectedDownloadLinks() + "");
        }
    }

    public Vector<FilePackage> getSelectedFilePackages() {
        int[] rows = this.getSelectedRows();
        Vector<FilePackage> ret = new Vector<FilePackage>();
        TreePath path;
        for (int i = 0; i < rows.length; i++) {
            path = this.getPathForRow(rows[i]);
            if (path != null && path.getLastPathComponent() instanceof FilePackage) {
                ret.add((FilePackage) path.getLastPathComponent());

            }
        }
        return ret;
    }

    public Vector<DownloadLink> getSelectedDownloadLinks() {
        int[] rows = this.getSelectedRows();
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        TreePath path;
        for (int i = 0; i < rows.length; i++) {
            path = this.getPathForRow(rows[i]);
            if (path != null && path.getLastPathComponent() instanceof DownloadLink) {
                ret.add((DownloadLink) path.getLastPathComponent());

            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {
        DownloadLink link;
        FilePackage fp;
        Vector<DownloadLink> links;
        Vector<FilePackage> fps;
        BrowseFile homeDir;
        switch (e.getID()) {

            case TreeTableAction.DOWNLOAD_INFO:
                link = (DownloadLink) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlink");
                // ((TreeTableAction)e);
                new DownloadInfo(SimpleGUI.CURRENTGUI.getFrame(), link);

                break;

            case TreeTableAction.DOWNLOAD_BROWSE_LINK:
                link = (DownloadLink) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlink");
                // ((TreeTableAction)e);
                if (link.getLinkType() == DownloadLink.LINKTYPE_NORMAL) {

                    try {
                        new BrowserLauncher().openURLinBrowser(link.getDownloadURL());
                    }
                    catch (BrowserLaunchingInitializingException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    catch (UnsupportedOperatingSystemException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }

                }

                break;

            case TreeTableAction.DOWNLOAD_DOWNLOAD_DIR:
                link = (DownloadLink) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlink");
                try {
                    new GetExplorer().openExplorer(new File(link.getFileOutput()).getParentFile());
                }
                catch (Exception ec) {
                }
                break;

            case TreeTableAction.DOWNLOAD_DELETE:
                links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlinks");
                JDUtilities.getController().removeDownloadLinks(links);
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_CHANGED, this));

                break;
            case TreeTableAction.DOWNLOAD_ENABLE:
                links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlinks");

                for (int i = 0; i < links.size(); i++) {
                    links.elementAt(i).setEnabled(true);
                }
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, this));
                break;
            case TreeTableAction.DOWNLOAD_DISABLE:
                links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlinks");

                for (int i = 0; i < links.size(); i++) {
                    links.elementAt(i).setEnabled(false);
                }
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, this));

                break;
            case TreeTableAction.DOWNLOAD_NEW_PACKAGE:
                links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlinks");
                JDUtilities.getController().removeDownloadLinks(links);
                FilePackage parentFP = links.get(0).getFilePackage();
                String name = JDUtilities.getGUI().showTextAreaDialog(JDLocale.L("gui.linklist.newpackage.title", "Neues Paket erstellen"), JDLocale.L("gui.linklist.newpackage.message", "Name des neuen Pakets"), parentFP.getName());
                FilePackage nfp = new FilePackage();
                nfp.setName(name);
                nfp.setDownloadDirectory(parentFP.getDownloadDirectory());
                nfp.setPassword(parentFP.getPassword());
                nfp.setComment(parentFP.getComment());

                if (name == null) return;
                for (int i = 0; i < links.size(); i++) {
                    links.elementAt(i).setFilePackage(nfp);
                }
                JDUtilities.getController().addAllLinks(links);
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_CHANGED, this));

                break;

            case TreeTableAction.DOWNLOAD_RESET:
                links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlinks");

                for (int i = 0; i < links.size(); i++) {
                    if (!links.elementAt(i).isInProgress()) {
                        links.elementAt(i).setStatus(DownloadLink.STATUS_TODO);
                        links.elementAt(i).setStatusText("");
                        links.elementAt(i).reset();
                    }
                }
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, this));

                break;
            // case TreeTableAction.DOWNLOAD_FORCE:
            // break;

            case TreeTableAction.PACKAGE_INFO:
                fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");

                new PackageInfo(SimpleGUI.CURRENTGUI.getFrame(), fp);
                break;

            case TreeTableAction.PACKAGE_EDIT_DIR:
                fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");

                JDFileChooser fc = new JDFileChooser();
                fc.setApproveButtonText("OK");
                fc.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
                fc.setCurrentDirectory(new File(fp.getDownloadDirectory()));
                fc.showOpenDialog(this);
                File ret = fc.getSelectedFile();

                if (ret != null) fp.setDownloadDirectory(ret.getAbsolutePath());
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, this));

                break;
            case TreeTableAction.PACKAGE_EDIT_NAME:
                fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");

                name = JDUtilities.getGUI().showTextAreaDialog(JDLocale.L("gui.linklist.editpackagename.title", "Paketname ändern"), JDLocale.L("gui.linklist.editpackagename.message", "Neuer Paketname"), fp.getName());
                if (name != null) fp.setName(name);
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, this));

                break;

            case TreeTableAction.PACKAGE_DOWNLOAD_DIR:
                fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");
                try {
                    new GetExplorer().openExplorer(new File(fp.getDownloadDirectory()));
                }
                catch (Exception ec) {
                }
                break;

            case TreeTableAction.PACKAGE_DELETE:
                fps = (Vector<FilePackage>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("packages");
                for (Iterator<FilePackage> it = fps.iterator(); it.hasNext();) {
                    JDUtilities.getController().removeDownloadLinks(new Vector<DownloadLink>(it.next().getDownloadLinks()));
                }

                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_CHANGED, this));

                break;
            case TreeTableAction.PACKAGE_ENABLE:
                fps = (Vector<FilePackage>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("packages");
                FilePackage next;
                for (Iterator<FilePackage> it = fps.iterator(); it.hasNext();) {
                    next = it.next();
                    for (int i = 0; i < next.size(); i++) {
                        next.get(i).setEnabled(true);
                    }

                }
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, this));

                break;
            case TreeTableAction.PACKAGE_DISABLE:

                fps = (Vector<FilePackage>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("packages");

                for (Iterator<FilePackage> it = fps.iterator(); it.hasNext();) {
                    next = it.next();
                    for (int i = 0; i < next.size(); i++) {
                        next.get(i).setEnabled(false);
                    }

                }
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, this));

                break;

            case TreeTableAction.PACKAGE_RESET:

                fps = (Vector<FilePackage>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("packages");

                for (Iterator<FilePackage> it = fps.iterator(); it.hasNext();) {
                    next = it.next();
                    for (int i = 0; i < next.size(); i++) {
                        if (!next.get(i).isInProgress()) {
                            next.get(i).setStatus(DownloadLink.STATUS_TODO);
                            next.get(i).setStatusText("");
                            next.get(i).reset();
                        }
                    }

                }
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, this));

                break;
            case TreeTableAction.PACKAGE_SORT:
                fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");
                fp.sort("ASC");
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_CHANGED, this));

                break;
        }

    }

}