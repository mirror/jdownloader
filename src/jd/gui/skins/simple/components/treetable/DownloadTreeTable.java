/**
 * 
 */
package jd.gui.skins.simple.components.treetable;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.swing.DropMode;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreePath;

import jd.JDFileFilter;
import jd.config.MenuItem;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.event.ControlEvent;
import jd.gui.skins.simple.DownloadInfo;
import jd.gui.skins.simple.DownloadLinksView;
import jd.gui.skins.simple.JDAction;
import jd.gui.skins.simple.PackageInfo;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.BrowseFile;
import jd.gui.skins.simple.components.HTMLTooltip;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.config.GetExplorer;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadInterface.Chunk;
import jd.utils.JDLocale;
import jd.utils.JDSounds;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.tree.TreeModelSupport;

import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

public class DownloadTreeTable extends JXTreeTable implements WindowFocusListener, TreeExpansionListener, TreeSelectionListener, MouseListener, ActionListener, MouseMotionListener {
    private Logger logger = JDUtilities.getLogger();

    public static final String PROPERTY_EXPANDED = "expanded";

    public static final String PROPERTY_SELECTED = "selected";

    private static final long UPDATE_INTERVAL = 500;

    private DownloadTreeTableModel model;

    private TableCellRenderer cellRenderer;

    public int mouseOverRow = -1;

    private int mouseOverColumn = -1;

    private TooltipTimer tooltipTimer;

    private Point mousePoint = null;

    private HTMLTooltip tooltip = null;

    private HashMap<FilePackage, ArrayList<DownloadLink>> updatePackages;
    private DownloadLink currentLink;

    private long updateTimer = 0;

    private Point mousePosition;

    private TreeTableTransferHandler transferHandler;

    private Vector<DownloadLink> moved;

    private long ignoreSelectionsAndExpansionsUntil;

    private Timer timer;

    public DownloadTreeTable(DownloadTreeTableModel treeModel) {
        super(treeModel);

        this.cellRenderer = new TreeTableRenderer(this);
        this.model = treeModel;
        this.setUI(new TreeTablePaneUI());
        this.getTableHeader().setReorderingAllowed(false);
        this.getTableHeader().setResizingAllowed(true);
        // this.setExpandsSelectedPaths(true);
        this.setToggleClickCount(1);
        if (JDUtilities.getJavaVersion() >= 1.6) this.setDropMode(DropMode.ON_OR_INSERT_ROWS);
        this.setDragEnabled(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.setColumnControlVisible(true);
        SimpleGUI.CURRENTGUI.getFrame().addWindowFocusListener(this);

        this.setEditable(false);

        setDragEnabled(true);
        setAutoscrolls(false);
        addTreeExpansionListener(this);
        addTreeSelectionListener(this);
        addMouseListener(this);
        this.addMouseMotionListener(this);
        this.setTransferHandler(transferHandler = new TreeTableTransferHandler(this));
        if (JDUtilities.getJavaVersion() > 1.6) this.setDropMode(DropMode.USE_SELECTION);
        this.tooltipTimer = new TooltipTimer(2000);
        tooltipTimer.start();
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
            } else {
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
        if (false) {
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
                    column.setPreferredWidth(190);
                    column.setMaxWidth(190);
                    break;
                }
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
        if (this.ignoreSelectionsAndExpansionsUntil > System.currentTimeMillis()) return;
        FilePackage fp = (FilePackage) event.getPath().getLastPathComponent();
        fp.setProperty(DownloadTreeTable.PROPERTY_EXPANDED, false);

    }

    public void treeExpanded(TreeExpansionEvent event) {
        if (this.ignoreSelectionsAndExpansionsUntil > System.currentTimeMillis()) return;
        FilePackage fp = (FilePackage) event.getPath().getLastPathComponent();
        fp.setProperty(DownloadTreeTable.PROPERTY_EXPANDED, true);
    }

    public void valueChanged(TreeSelectionEvent e) {

        TreePath[] paths = e.getPaths();
        // logger.info("" + e);
        if (this.ignoreSelectionsAndExpansionsUntil > System.currentTimeMillis()) return;
        for (TreePath path : paths) {
            if (e.isAddedPath(path)) {
                if (path.getLastPathComponent() instanceof DownloadLink) {
                    ((DownloadLink) path.getLastPathComponent()).setProperty(DownloadTreeTable.PROPERTY_SELECTED, true);

                    logger.info("SELECTED " + ((DownloadLink) path.getLastPathComponent()));
                } else {
                    ((FilePackage) path.getLastPathComponent()).setProperty(DownloadTreeTable.PROPERTY_SELECTED, true);

                    logger.info("SELECTED " + ((FilePackage) path.getLastPathComponent()));
                }
            } else {

                if (path.getLastPathComponent() instanceof DownloadLink) {
                    ((DownloadLink) path.getLastPathComponent()).setProperty(DownloadTreeTable.PROPERTY_SELECTED, false);
                    logger.info("NOT SELECTED " + ((DownloadLink) path.getLastPathComponent()));
                } else {
                    ((FilePackage) path.getLastPathComponent()).setProperty(DownloadTreeTable.PROPERTY_SELECTED, false);
                    logger.info("NOT SELECTED " + ((FilePackage) path.getLastPathComponent()));
                }
            }
        }

    }

    @SuppressWarnings("unchecked")
    public synchronized void fireTableChanged(int id, Object param) {
        TreeModelSupport supporter = getDownladTreeTableModel().getModelSupporter();
        if (updatePackages == null) updatePackages = new HashMap<FilePackage, ArrayList<DownloadLink>>();
        switch (id) {
        /*
         * Es werden nur die Übergebenen LinkPfade aktualisiert.
         * REFRESH_SPECIFIED_LINKS kann als Parameter eine Arraylist oder einen
         * einzellnen DownloadLink haben. ArrayLists werden nicht ausgewertet.
         * in diesem Fall wird die komplette Tabelle neu gezeichnet.
         */
        case DownloadLinksView.REFRESH_SPECIFIED_LINKS:
            // logger.info("REFRESH SPECS COMPLETE");
            if (param instanceof DownloadLink) {
                currentLink = ((DownloadLink) param);
                // supporter.firePathChanged(new TreePath(new Object[] {
                // model.getRoot(), ((DownloadLink) param).getFilePackage(),
                // ((DownloadLink) param) }));
                // logger.info("Updatesingle "+currentLink);
                if (updatePackages.containsKey(currentLink.getFilePackage())) {
                    if (!updatePackages.get(currentLink.getFilePackage()).contains(currentLink)) updatePackages.get(currentLink.getFilePackage()).add(currentLink);
                } else {
                    ArrayList<DownloadLink> ar = new ArrayList<DownloadLink>();
                    updatePackages.put(currentLink.getFilePackage(), ar);
                    ar.add(currentLink);
                }

            } else if (param instanceof ArrayList) {
                for (Iterator<DownloadLink> it = ((ArrayList<DownloadLink>) param).iterator(); it.hasNext();) {
                    currentLink = it.next();
                    if (updatePackages.containsKey(currentLink.getFilePackage())) {
                        if (!updatePackages.get(currentLink.getFilePackage()).contains(currentLink)) updatePackages.get(currentLink.getFilePackage()).add(currentLink);
                    } else {
                        ArrayList<DownloadLink> ar = new ArrayList<DownloadLink>();
                        updatePackages.put(currentLink.getFilePackage(), ar);
                        ar.add(currentLink);
                    }
                }
            }
            if ((System.currentTimeMillis() - updateTimer) > UPDATE_INTERVAL && updatePackages != null) {
                Entry<FilePackage, ArrayList<DownloadLink>> next;
                DownloadLink next3;

                for (Iterator<Entry<FilePackage, ArrayList<DownloadLink>>> it2 = updatePackages.entrySet().iterator(); it2.hasNext();) {
                    next = it2.next();
                    // logger.info("Refresh " + next.getKey() + " - " +
                    // next.getValue().size());

                    if (!model.containesPackage(next.getKey())) continue;
                    supporter.firePathChanged(new TreePath(new Object[] { model.getRoot(), next.getKey() }));

                    if (next.getKey().getBooleanProperty(PROPERTY_EXPANDED, false)) {

                        int[] ind = new int[next.getValue().size()];
                        Object[] objs = new Object[next.getValue().size()];
                        int i = 0;
                        int nulls = 0;
                        for (Iterator<DownloadLink> it3 = next.getValue().iterator(); it3.hasNext();) {
                            next3 = it3.next();
                            if (!next.getKey().contains(next3)) {
                                nulls++;
                                continue;
                            }
                            ind[i] = next.getKey().indexOf(next3);
                            objs[i] = next3;
                            
                            i++;
                            // logger.info(" children: " + next3 + " - " +
                            // ind[i]);
                        }
                        if (nulls > 0) {
                            logger.warning("GUI Update error.");
                            objs = new Object[next.getValue().size() - nulls];
                            if (next.getValue().size() - nulls == 0) return;
                            ind = new int[next.getValue().size() - nulls];
                            objs = new Object[next.getValue().size() - nulls];
                            i = 0;
                            nulls=0;
                            for (Iterator<DownloadLink> it3 = next.getValue().iterator(); it3.hasNext();) {
                                next3 = it3.next();
                                if (!next.getKey().contains(next3)) {
                                    nulls++;
                                    continue;
                                }
                                ind[i] = next.getKey().indexOf(next3)-nulls;
                                objs[i] = next3;

                                i++;

                            }
                        }
                        if (i > 0) {
                            // if(model.getRoot()==null || )
                            supporter.fireChildrenChanged(new TreePath(new Object[] { model.getRoot(), next.getKey() }), ind, objs);
                        }
                    }

                }
                updatePackages = null;
                updateTimer = System.currentTimeMillis();
            }

            break;
        case DownloadLinksView.REFRESH_ALL_DATA_CHANGED:
            // supporter.fireTreeStructureChanged(new
            // TreePath(model.getRoot()));
            // updateSelectionAndExpandStatus();
            logger.info("Updatecomplete");
            supporter.fireChildrenChanged(new TreePath(model.getRoot()), null, null);

            break;
        case DownloadLinksView.REFRESH_DATA_AND_STRUCTURE_CHANGED:
            logger.info("REFRESH GUI COMPLETE");

            supporter.fireTreeStructureChanged(new TreePath(model.getRoot()));

            ignoreSelectionsAndExpansions(500);
            updateSelectionAndExpandStatus();
            // logger.info("finished");

            break;

        }

    }

    void ignoreSelectionsAndExpansions(int i) {
        ignoreSelectionsAndExpansionsUntil = System.currentTimeMillis() + i;

    }

    public void mouseClicked(MouseEvent e) {
        ignoreSelectionsAndExpansions(-100);
        if (tooltip != null) {
            tooltip.destroy();
            tooltip = null;
        }
        if (e.getButton() == MouseEvent.BUTTON1 && 2 == e.getClickCount()) {
            Point point = e.getPoint();
            int row = this.rowAtPoint(point);
            TreePath path = getPathForRow(row);
            if (path == null) return;
            Object obj = path.getLastPathComponent();
            if (obj instanceof DownloadLink) {
                new DownloadInfo(SimpleGUI.CURRENTGUI.getFrame(), (DownloadLink) getPathForRow(row).getLastPathComponent());
            }
        }
    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {
        this.tooltipTimer.setCaller(null);
        // Point point = mousePoint;
        // this.mousePoint = null;
        // if(point!=null) {
        // getDownladTreeTableModel().getModelSupporter().fireNewRoot();
        // }

    }

    public void mouseReleased(MouseEvent e) {
        TreePath path = this.getPathForLocation(e.getX(), e.getY());

        if (path != null && path.getLastPathComponent() instanceof FilePackage) {
            JDSounds.PT("sound.gui.selectPackage");
        } else if (path != null) {
            JDSounds.PT("sound.gui.selectLink");
        }

        this.tooltipTimer.setCaller(null);
        if (tooltip != null) {
            tooltip.destroy();
            tooltip = null;
        }
    }

    public void mousePressed(MouseEvent e) {
        ignoreSelectionsAndExpansions(-100);
        // TODO: isPopupTrigger() funktioniert nicht
        // logger.info("Press"+e.isPopupTrigger() );
        Point point = e.getPoint();
        int row = this.rowAtPoint(point);

        if (!this.isRowSelected(row)) {
            getTreeSelectionModel().clearSelection();
            getTreeSelectionModel().addSelectionPath(getPathForRow(row));
        }
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {

            if (getPathForRow(row) == null) return;
            Object obj = getPathForRow(row).getLastPathComponent();
            JMenuItem tmp;
            if (obj instanceof DownloadLink) {
                Vector<FilePackage> fps = new Vector<FilePackage>();
                int disabled = 0;
                int enabled = 0;
                int progress = 0;
                int resumeable = 0;
                for (Iterator<DownloadLink> it = getSelectedDownloadLinks().iterator(); it.hasNext();) {
                    DownloadLink next = it.next();
                    if (!fps.contains(next.getFilePackage())) fps.add(next.getFilePackage());
                    if (!next.isEnabled()) disabled++;
                    if (next.isEnabled()) enabled++;
                    if (next.isInProgress()) progress++;
                    if (!next.isInProgress() && next.isFailed()) resumeable++;
                }
                JPopupMenu popup = new JPopupMenu();
                Plugin plg = ((DownloadLink) obj).getPlugin();
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.info", "Detailansicht"), TreeTableAction.DOWNLOAD_INFO, new Property("downloadlink", obj))));

                JMenu packagePopup = new JMenu(JDLocale.L("gui.table.contextmenu.packagesubmenu", "Paket"));
                JMenu pluginPopup = new JMenu(JDLocale.L("gui.table.contextmenu.pluginsubmenu", "Plugin") + " (" + plg.getPluginName() + ")");

                MenuItem next1;
                JMenuItem mi;
                ArrayList<MenuItem> pluginMenuEntries = plg.createMenuitems();
                if (pluginMenuEntries != null) for (Iterator<MenuItem> it1 = pluginMenuEntries.iterator(); it1.hasNext();) {
                    next1 = it1.next();
                    mi = SimpleGUI.getJMenuItem(next1);

                    if (mi == null) {
                        pluginPopup.addSeparator();
                    } else {
                        pluginPopup.add(mi);
                    }

                }

                if (pluginMenuEntries != null) if (pluginMenuEntries.size() == 0) pluginPopup.setEnabled(false);
                popup.add(packagePopup);
                popup.add(pluginPopup);
                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.downloadDir", "Zielordner öffnen"), TreeTableAction.DOWNLOAD_DOWNLOAD_DIR, new Property("downloadlink", obj))));
                popup.add(tmp = new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.browseLink", "im Browser öffnen"), TreeTableAction.DOWNLOAD_BROWSE_LINK, new Property("downloadlink", obj))));
                if (((DownloadLink) obj).getLinkType() != DownloadLink.LINKTYPE_NORMAL) tmp.setEnabled(false);
                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.delete", "entfernen"), TreeTableAction.DOWNLOAD_DELETE, new Property("downloadlinks", getSelectedDownloadLinks()))));

                tmp = new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.enable", "aktivieren") + " (" + disabled + ")", TreeTableAction.DOWNLOAD_ENABLE, new Property("downloadlinks", getSelectedDownloadLinks())));
                if (disabled == 0) tmp.setEnabled(false);
                popup.add(tmp);
                tmp = new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.disable", "deaktivieren") + " (" + enabled + ")", TreeTableAction.DOWNLOAD_DISABLE, new Property("downloadlinks", getSelectedDownloadLinks())));
                if (enabled == 0) tmp.setEnabled(false);
                popup.add(tmp);

                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.reset", "zurücksetzen"), TreeTableAction.DOWNLOAD_RESET, new Property("downloadlinks", getSelectedDownloadLinks()))));
                tmp = (new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.abort", "abbrechen") + " (" + progress + ")", TreeTableAction.DOWNLOAD_ABORT, new Property("downloadlinks", getSelectedDownloadLinks()))));
                popup.add(tmp);
                if (progress == 0) tmp.setEnabled(false);
                tmp = (new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.resume", "fortsetzen") + " (" + resumeable + ")", TreeTableAction.DOWNLOAD_RESUME, new Property("downloadlinks", getSelectedDownloadLinks()))));
                popup.add(tmp);
                if (resumeable == 0) tmp.setEnabled(false);
                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.newpackage", "In neues Paket verschieben"), TreeTableAction.DOWNLOAD_NEW_PACKAGE, new Property("downloadlinks", getSelectedDownloadLinks()))));

                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.dlc", "DLC erstellen"), TreeTableAction.DOWNLOAD_DLC, new Property("downloadlinks", getSelectedDownloadLinks()))));

                packagePopup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.packageinfo", "Detailansicht"), TreeTableAction.PACKAGE_INFO, new Property("package", ((DownloadLink) obj).getFilePackage()))));
                packagePopup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.packagesort", "Paket sortieren"), TreeTableAction.PACKAGE_SORT, new Property("package", ((DownloadLink) obj).getFilePackage()))));
                packagePopup.add(new JSeparator());
                packagePopup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.editdownloadDir", "Zielordner ändern"), TreeTableAction.PACKAGE_EDIT_DIR, new Property("package", ((DownloadLink) obj).getFilePackage()))));
                packagePopup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.packagedownloadDir", "Zielordner öffnen"), TreeTableAction.PACKAGE_DOWNLOAD_DIR, new Property("package", ((DownloadLink) obj).getFilePackage()))));
                packagePopup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.editpackagename", "Paketname ändern"), TreeTableAction.PACKAGE_EDIT_NAME, new Property("package", ((DownloadLink) obj).getFilePackage()))));
                packagePopup.add(new JSeparator());
                packagePopup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.deletepackage", "entfernen"), TreeTableAction.PACKAGE_DELETE, new Property("packages", fps))));
                tmp = new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.enablepackage", "aktivieren"), TreeTableAction.PACKAGE_ENABLE, new Property("packages", fps)));

                packagePopup.add(tmp);
                tmp = new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.disablepackage", "deaktivieren"), TreeTableAction.PACKAGE_DISABLE, new Property("packages", fps)));

                packagePopup.add(tmp);

                packagePopup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.resetpackage", "zurücksetzen"), TreeTableAction.PACKAGE_RESET, new Property("packages", fps))));
                packagePopup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.abortpackage", "abbrechen"), TreeTableAction.PACKAGE_ABORT, new Property("packages", fps))));
                packagePopup.add(new JSeparator());
                packagePopup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.dlc_package", "DLC erstellen"), TreeTableAction.PACKAGE_DLC, new Property("packages", fps))));

                // popup.add(new JMenuItem(new TreeTableAction(this,
                // "forcedownload", TreeTableAction.DOWNLOAD_FORCE,new
                // Property("downloadlinks",getSelectedDownloadLinks()))));
                popup.show(this, point.x, point.y);
            } else {
                TreeTableAction action;
                JPopupMenu popup = new JPopupMenu();
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.packageinfo", "Detailansicht"), TreeTableAction.PACKAGE_INFO, new Property("package", obj))));
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.packagesort", "Paket sortieren"), TreeTableAction.PACKAGE_SORT, new Property("package", obj))));

                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.editdownloadDir", "Zielordner ändern"), TreeTableAction.PACKAGE_EDIT_DIR, new Property("package", obj))));
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.packagedownloadDir", "Zielordner öffnen"), TreeTableAction.PACKAGE_DOWNLOAD_DIR, new Property("package", obj))));

                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.editpackagename", "Paketname ändern"), TreeTableAction.PACKAGE_EDIT_NAME, new Property("package", obj))));

                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.deletepackage", "entfernen"), TreeTableAction.PACKAGE_DELETE, new Property("packages", this.getSelectedFilePackages()))));
                popup.add(tmp = new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.enablepackage", "aktivieren"), TreeTableAction.PACKAGE_ENABLE, new Property("packages", getSelectedFilePackages()))));
                popup.add(tmp = new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.disablepackage", "deaktivieren"), TreeTableAction.PACKAGE_DISABLE, new Property("packages", getSelectedFilePackages()))));

                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.resetpackage", "zurücksetzen"), TreeTableAction.PACKAGE_RESET, new Property("packages", getSelectedFilePackages()))));
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.abortpackage", "abbrechen"), TreeTableAction.PACKAGE_ABORT, new Property("packages", getSelectedFilePackages()))));
                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.dlc_package", "DLC erstellen"), TreeTableAction.PACKAGE_DLC, new Property("packages", getSelectedFilePackages()))));

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
        SubConfiguration guiConfig = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
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
                } catch (BrowserLaunchingInitializingException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (UnsupportedOperatingSystemException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

            }

            break;

        case TreeTableAction.DOWNLOAD_DOWNLOAD_DIR:
            link = (DownloadLink) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlink");
            try {
                new GetExplorer().openExplorer(new File(link.getFileOutput()).getParentFile());
            } catch (Exception ec) {
            }
            break;

        case TreeTableAction.DOWNLOAD_DELETE:
            links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlinks");

            if (!guiConfig.getBooleanProperty(SimpleGUI.PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                if (SimpleGUI.CURRENTGUI.showConfirmDialog("Ausgewählte Links wirklich entfernen?")) {

                    JDUtilities.getController().removeDownloadLinks(links);
                    JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));

                    // fireUIEvent(new UIEvent(this,
                    // UIEvent.UI_UPDATED_LINKLIST, this.getDownloadLinks()));

                }
            } else {
                JDUtilities.getController().removeDownloadLinks(links);
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));

            }
            break;
        case TreeTableAction.DOWNLOAD_ENABLE:
            links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlinks");

            for (int i = 0; i < links.size(); i++) {
                links.elementAt(i).setEnabled(true);
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
            break;
        case TreeTableAction.DOWNLOAD_RESUME:
            links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlinks");

            for (int i = 0; i < links.size(); i++) {
                links.elementAt(i).setStatus(DownloadLink.STATUS_TODO);
                links.elementAt(i).setStatusText(JDLocale.L("gui.linklist.status.doresume", "Warte auf Fortsetzung"));
                links.elementAt(i).getPlugin().resetSteps();
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));

            break;
        case TreeTableAction.DOWNLOAD_DISABLE:
            links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlinks");

            for (int i = 0; i < links.size(); i++) {
                links.elementAt(i).setEnabled(false);
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));

            break;

        case TreeTableAction.DOWNLOAD_DLC:
            links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlinks");
            JDFileChooser fc = new JDFileChooser("_LOADSAVEDLC");
            fc.setFileFilter(new JDFileFilter(null, ".dlc", true));
            fc.showSaveDialog(SimpleGUI.CURRENTGUI.getFrame());
            File ret = fc.getSelectedFile();
            if (ret == null) return;
            if (JDUtilities.getFileExtension(ret) == null || !JDUtilities.getFileExtension(ret).equalsIgnoreCase("dlc")) {

                ret = new File(ret.getAbsolutePath() + ".dlc");
            }

            JDUtilities.getController().saveDLC(ret, links);
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
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));

            break;

        case TreeTableAction.DOWNLOAD_RESET:
            links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlinks");

            for (int i = 0; i < links.size(); i++) {
                // if (!links.elementAt(i).isInProgress()) {
                links.elementAt(i).setStatus(DownloadLink.STATUS_TODO);
                links.elementAt(i).setStatusText("");
                links.elementAt(i).reset();

            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));

            break;
        // case TreeTableAction.DOWNLOAD_FORCE:
        // break;
        case TreeTableAction.DOWNLOAD_ABORT:
            links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlinks");

            for (int i = 0; i < links.size(); i++) {
                // if (!links.elementAt(i).isInProgress()) {
                links.elementAt(i).setAborted(true);

            }
            // JDUtilities.getController().fireControlEvent(new
            // ControlEvent(this,
            // ControlEvent.CONTROL_DOWNLOADLINK_DATA_CHANGED, this));

            break;
        // case TreeTableAction.DOWNLOAD_FORCE:
        // break;

        case TreeTableAction.PACKAGE_INFO:
            fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");

            new PackageInfo(SimpleGUI.CURRENTGUI.getFrame(), fp);
            break;

        case TreeTableAction.PACKAGE_EDIT_DIR:
            fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");

            fc = new JDFileChooser();
            fc.setApproveButtonText("OK");
            fc.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);

            fc.setCurrentDirectory(fp.getDownloadDirectory() != null ? new File(fp.getDownloadDirectory()) : JDUtilities.getResourceFile("downloads"));
            fc.showOpenDialog(this);
            ret = fc.getSelectedFile();

            if (ret != null) fp.setDownloadDirectory(ret.getAbsolutePath());
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));

            break;
        case TreeTableAction.PACKAGE_EDIT_NAME:
            fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");
            name = JOptionPane.showInputDialog(SimpleGUI.CURRENTGUI.getFrame(), JDLocale.L("gui.linklist.editpackagename.message", "Neuer Paketname"), fp.getName());

            if (name != null) fp.setName(name);
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));

            break;

        case TreeTableAction.PACKAGE_DOWNLOAD_DIR:
            fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");
            try {
                new GetExplorer().openExplorer(new File(fp.getDownloadDirectory()));
            } catch (Exception ec) {
            }
            break;

        case TreeTableAction.PACKAGE_DELETE:
            fps = (Vector<FilePackage>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("packages");
            for (Iterator<FilePackage> it = fps.iterator(); it.hasNext();) {
                JDUtilities.getController().removePackage(it.next());
            }

            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));

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
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));

            break;
        case TreeTableAction.PACKAGE_DISABLE:

            fps = (Vector<FilePackage>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("packages");

            for (Iterator<FilePackage> it = fps.iterator(); it.hasNext();) {
                next = it.next();
                for (int i = 0; i < next.size(); i++) {
                    next.get(i).setEnabled(false);
                }

            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));

            break;

        case TreeTableAction.PACKAGE_DLC:
            links = new Vector<DownloadLink>();
            fps = (Vector<FilePackage>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("packages");

            for (Iterator<FilePackage> it = fps.iterator(); it.hasNext();) {
                next = it.next();
                for (int i = 0; i < next.size(); i++) {
                    links.add(next.get(i));
                }

            }
            fc = new JDFileChooser("_LOADSAVEDLC");
            fc.setFileFilter(new JDFileFilter(null, ".dlc", true));
            fc.showSaveDialog(SimpleGUI.CURRENTGUI.getFrame());
            ret = fc.getSelectedFile();
            if (ret == null) return;
            if (JDUtilities.getFileExtension(ret) == null || !JDUtilities.getFileExtension(ret).equalsIgnoreCase("dlc")) {

                ret = new File(ret.getAbsolutePath() + ".dlc");
            }

            JDUtilities.getController().saveDLC(ret, links);
            break;
        case TreeTableAction.PACKAGE_ABORT:

            fps = (Vector<FilePackage>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("packages");

            for (Iterator<FilePackage> it = fps.iterator(); it.hasNext();) {
                next = it.next();
                for (int i = 0; i < next.size(); i++) {
                    next.get(i).setAborted(true);
                }

            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));

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
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));

            break;
        case TreeTableAction.PACKAGE_SORT:
            fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");
            fp.sort(null);
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));

            break;
        }

    }

    public void mouseDragged(MouseEvent e) {
        this.mousePosition = e.getPoint();
    }

    public void mouseMoved(MouseEvent e) {
        final int moRow = this.rowAtPoint(e.getPoint());
        final int moColumn = this.columnAtPoint(e.getPoint());
        this.mousePosition = e.getPoint();
        this.mousePoint = e.getPoint();
        Point screen = this.getLocationOnScreen();
        mousePoint.x += screen.x;
        mousePoint.y += screen.y;
        if (this.getPathForRow(moRow) == null) {
            this.mouseOverRow = moRow;
            this.mouseOverColumn = moColumn;
            tooltipTimer.setCaller(null);
            if (tooltip != null) {
                tooltip.destroy();
                tooltip = null;
            }
            return;
        }

        if (moRow != this.mouseOverRow || moColumn != this.mouseOverColumn) {
            if (tooltip != null) {
                tooltip.destroy();
                tooltip = null;
            }
            tooltipTimer.setCaller(new Caller() {
                public void call() {
                    if (moColumn == mouseOverColumn && moRow == mouseOverRow) {
                        // logger.info(moColumn+"="+mouseOverColumn+" -
                        // "+moRow+" - "+mouseOverRow);
                        showToolTip(moRow, moColumn);
                    }
                    // logger.info(moColumn+"="+mouseOverColumn+" - "+moRow+" -
                    // "+mouseOverRow);
                }
            });
        }

        this.mouseOverRow = moRow;
        this.mouseOverColumn = moColumn;

    }

    private void showToolTip(int mouseOverRow, int mouseOverColumn) {
        if (tooltip != null) {
            tooltip.destroy();
            tooltip = null;
        }

        if (!SimpleGUI.CURRENTGUI.getFrame().isActive() || transferHandler.isDragging) return;
        StringBuffer sb = new StringBuffer();
        sb.append("<div>");
        Object obj = this.getPathForRow(mouseOverRow).getLastPathComponent();
        DownloadLink link;
        FilePackage fp;
        if (obj instanceof DownloadLink) {
            link = (DownloadLink) obj;
            switch (mouseOverColumn) {
            case 0:
                sb.append("<h1>" + link.getFileOutput() + "</h1>");
                break;
            case 1:
                sb.append("<h1>" + link.getFileOutput() + "</h1><hr>");
                if (link.getLinkType() == DownloadLink.LINKTYPE_NORMAL) sb.append("<p>" + link.getDownloadURL() + "</p>");

                break;
            case 2:
                PluginForHost plg = (PluginForHost) link.getPlugin();

                sb.append("<h1>" + plg.getPluginID() + "</h1><hr>");
                sb.append("<p>");
                sb.append(JDLocale.L("gui.downloadlist.tooltip.connections", "Akt. Verbindungen: ") + "" + plg.getCurrentConnections() + "/" + plg.getMaxConnections());
                sb.append("<br>");
                sb.append(JDLocale.L("gui.downloadlist.tooltip.simultan_downloads", "Max. gleichzeitige Downloads:") + " " + plg.getMaxSimultanDownloadNum());
                sb.append("</p>");
                break;
            case 3:
                sb.append("<p>" + link.getStatusText() + "</p>");
                break;
            case 4:
                if (link.getDownloadInstance() == null) return;
                DownloadInterface dl = link.getDownloadInstance();
                String table = "<p><table>";
                for (Chunk chunk : dl.getChunks()) {
                    int loaded = chunk.getBytesLoaded();
                    int total = chunk.getChunkSize();
                    table += "<tr>";
                    table += "<td> Verbindung " + chunk.getID() + "</td>";
                    table += "<td>" + JDUtilities.formatKbReadable((int) chunk.getBytesPerSecond() / 1024) + "/s" + "</td>";
                    table += "<td>" + JDUtilities.formatKbReadable(loaded / 1024) + "/" + JDUtilities.formatKbReadable(total / 1024) + "</td>";

                    int r = (loaded * 100) / total;
                    int m = 100 - r;

                    table += "<td>" + "<table width='100px' height='5px'  cellpadding='0' cellspacing='0' ><tr><td width='" + r + "%' bgcolor='#000000'/><td width='" + m + "%' bgcolor='#cccccc'/></tr> </table>" + "</td>";
                    table += "</tr>";
                }
                table += "</table></p>";

                sb.append("<h1>" + link.getFileOutput() + "</h1><hr>");
                sb.append(table);

                break;
            }
        } else {
            // FilePackage filePackage;
            // DownloadLink dLink;
            // for(Iterator<FilePackage> packageIterator =
            // JDUtilities.getController().getPackages().iterator();packageIterator.hasNext();){
            // filePackage = packageIterator.next();
            // for(Iterator<DownloadLink> linkIterator =
            // filePackage.getDownloadLinks().iterator();linkIterator.hasNext();){
            // dLink = linkIterator.next();
            // logger.info(dLink.getName());
            // logger.info(dLink.getHost());
            // logger.info(dLink.getStatusText());//Gibt einen Statustext aus
            // der den jeweiligen Status des Links beschreibt. inkl. Speed
            // logger.info("Link in Arbeit: "+dLink.isInProgress());
            // }
            //                
            // }

            fp = (FilePackage) obj;
            switch (mouseOverColumn) {
            case 0:

                sb.append("<h1>" + fp.getName() + "</h1><hr>");
                if (fp.hasComment()) sb.append("<p>" + fp.getComment() + "</p>");
                if (fp.hasPassword()) sb.append("<p>" + fp.getPassword() + "</p>");
                if (fp.hasDownloadDirectory()) sb.append("<p>" + fp.getDownloadDirectory() + "</p>");
                break;
            case 1:
                sb.append("<h1>" + fp.getName() + "</h1><hr>");
                sb.append("<p>" + JDLocale.L("gui.downloadlist.tooltip.partnum", "Teile: ") + fp.size() + "</p>");
                sb.append("<p>" + JDLocale.L("gui.downloadlist.tooltip.partsfinished", "Davon fertig: ") + fp.getLinksFinished() + "</p>");
                sb.append("<p>" + JDLocale.L("gui.downloadlist.tooltip.partsfailed", "Davon fehlerhaft: ") + fp.getLinksFailed() + "</p>");
                sb.append("<p>" + JDLocale.L("gui.downloadlist.tooltip.partsactive", "Gerade aktiv: ") + fp.getLinksInProgress() + "</p>");

                break;
            case 2:
                return;

            case 3:
                sb.append("<h1>" + fp.getName() + "</h1><hr>");
                sb.append("<p>" + this.getDownladTreeTableModel().getValueAt(obj, mouseOverColumn) + "</p>");
                break;
            case 4:
                sb.append("<h1>" + fp.getName() + "</h1><hr>");
                sb.append("<p>" + this.getDownladTreeTableModel().getValueAt(obj, mouseOverColumn - 1) + "</p>");

                break;
            }
        }

        sb.append("</div>");
        if (sb.length() <= 12) return;
        tooltip = HTMLTooltip.show(sb.toString(), this.mousePoint);

    }

    class TooltipTimer extends Thread {
        private int delay;
        private Caller cl = null;
        private long timer;

        public TooltipTimer(int delay) {
            this.delay = delay;
        }

        public void setCaller(Caller cl, int delay) {
            this.cl = cl;
            this.delay = delay;
            timer = System.currentTimeMillis();

        }

        public void setCaller(Caller cl) {
            setCaller(cl, delay);
        }

        public void run() {
            // try {
            // wait();
            // } catch (InterruptedException e1) { }
            while (true) {
                try {
                    sleep(50);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (cl != null && System.currentTimeMillis() - timer > delay) {
                    cl.call();
                    cl = null;
                    try {
                        sleep(200);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    // try {
                    // wait();
                    // } catch (InterruptedException e) {
                    // }
                }
            }
        }

    }

    abstract class Caller {
        abstract public void call();
    }

    public void windowGainedFocus(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    public void windowLostFocus(WindowEvent e) {
        if (tooltip != null && e.getOppositeWindow() != tooltip) {
            tooltip.destroy();
            tooltip = null;
        }

    }

    public Point getMousePosition2() {

        return mousePosition;
    }

    public void moveSelectedItems(int id) {
        Vector<DownloadLink> links = getSelectedDownloadLinks();
        Vector<FilePackage> fps = getSelectedFilePackages();
        this.moved = links;
        this.ignoreSelectionsAndExpansions(2000);
        logger.info(links.size() + " - " + fps.size());
        if (links.size() >= fps.size()) {
            if (links.size() == 0) return;
            // getDownladTreeTableModel().move(draggingPathes, preLink,
            // current.getLastPathComponent())
            switch (id) {
            case JDAction.ITEMS_MOVE_BOTTOM:
                // DownloadLink before =
                // JDUtilities.getController().getDownloadLinkBefore(links.get(0));
                DownloadLink lastLink = JDUtilities.getController().getPackages().lastElement().getDownloadLinks().lastElement();
                JDUtilities.getController().moveLinks(links, lastLink, null);
                break;
            case JDAction.ITEMS_MOVE_TOP:

                DownloadLink firstLink = JDUtilities.getController().getPackages().firstElement().getDownloadLinks().firstElement();
                JDUtilities.getController().moveLinks(links, null, firstLink);
                break;
            case JDAction.ITEMS_MOVE_UP:

                DownloadLink before = JDUtilities.getController().getDownloadLinkBefore(links.get(0));
                JDUtilities.getController().moveLinks(links, null, before);
                break;
            case JDAction.ITEMS_MOVE_DOWN:
                DownloadLink after = JDUtilities.getController().getDownloadLinkAfter(links.lastElement());
                JDUtilities.getController().moveLinks(links, after, null);

                break;
            }

        } else {

            switch (id) {
            case JDAction.ITEMS_MOVE_BOTTOM:
                // DownloadLink before =
                // JDUtilities.getController().getDownloadLinkBefore(links.get(0));
                FilePackage lastFilepackage = JDUtilities.getController().getPackages().lastElement();
                JDUtilities.getController().movePackages(fps, lastFilepackage, null);
                break;
            case JDAction.ITEMS_MOVE_TOP:
                FilePackage firstPackage = JDUtilities.getController().getPackages().firstElement();
                JDUtilities.getController().movePackages(fps, null, firstPackage);

                break;
            case JDAction.ITEMS_MOVE_UP:
                int i = JDUtilities.getController().getPackages().indexOf(fps.get(0));
                if (i <= 0) return;

                FilePackage before = JDUtilities.getController().getPackages().get(i - 1);
                JDUtilities.getController().movePackages(fps, null, before);

                break;
            case JDAction.ITEMS_MOVE_DOWN:
                i = JDUtilities.getController().getPackages().indexOf(fps.lastElement());
                if (i >= JDUtilities.getController().getPackages().size() - 1) return;
                FilePackage after = JDUtilities.getController().getPackages().get(i + 1);

                JDUtilities.getController().movePackages(fps, after, null);

                break;
            }

        }
        timer = new Timer(100, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateSelectionAndExpandStatus();
                logger.info("REFRESH");
            }

        });
        timer.setRepeats(false);
        timer.start();

    }

}