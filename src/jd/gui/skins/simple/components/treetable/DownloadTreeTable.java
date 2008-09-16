//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.gui.skins.simple.components.treetable;

import java.awt.Component;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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
import jd.gui.skins.simple.components.HTMLTooltip;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadInterface.Chunk;
import jd.utils.GetExplorer;
import jd.utils.JDLocale;
import jd.utils.JDSounds;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.tree.TreeModelSupport;

import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

public class DownloadTreeTable extends JXTreeTable implements WindowFocusListener, TreeExpansionListener, TreeSelectionListener, MouseListener, ActionListener, MouseMotionListener, KeyListener {

    abstract class Caller {
        abstract public void call();
    }

    class TooltipTimer extends Thread {
        private Caller cl = null;
        private int delay;
        private long timer;

        public TooltipTimer(int delay) {
            this.setName("GUITooltiptimer");
            this.delay = delay;
        }

        public void run() {
            while (true) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (cl != null && System.currentTimeMillis() - timer > delay) {
                    cl.call();
                    cl = null;
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void setCaller(Caller cl) {
            setCaller(cl, delay);
        }

        public void setCaller(Caller cl, int delay) {
            this.cl = cl;
            this.delay = delay;
            timer = System.currentTimeMillis();

        }

    }

    public static final String PROPERTY_EXPANDED = "expanded";

    public static final String PROPERTY_SELECTED = "selected";

    private static final long serialVersionUID = 1L;

    private static final long UPDATE_INTERVAL = 200;

    private TableCellRenderer cellRenderer;

    private DownloadLink currentLink;

    private long ignoreSelectionsAndExpansionsUntil;

    private Logger logger = JDUtilities.getLogger();

    private DownloadTreeTableModel model;

    private int mouseOverColumn = -1;

    public int mouseOverRow = -1;

    private Point mousePoint = null;

    private Timer timer;

    private HTMLTooltip tooltip = null;

    private TooltipTimer tooltipTimer;

    private TreeTableTransferHandler transferHandler;

    private HashMap<FilePackage, ArrayList<DownloadLink>> updatePackages;

    private long updateTimer = 0;

    public DownloadTreeTable(DownloadTreeTableModel treeModel) {
        super(treeModel);

        cellRenderer = new TreeTableRenderer(this);
        model = treeModel;
        this.setUI(new TreeTablePaneUI());
        getTableHeader().setReorderingAllowed(false);
        getTableHeader().setResizingAllowed(true);
        // this.setExpandsSelectedPaths(true);
        setToggleClickCount(1);
        if (JDUtilities.getJavaVersion() >= 1.6) {
            setDropMode(DropMode.ON_OR_INSERT_ROWS);
        }
        setDragEnabled(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setColumnControlVisible(true);
        SimpleGUI.CURRENTGUI.getFrame().addWindowFocusListener(this);

        setEditable(false);
        setAutoscrolls(false);
        addTreeExpansionListener(this);
        addTreeSelectionListener(this);
        addMouseListener(this);
        addKeyListener(this);
        addMouseMotionListener(this);
        setTransferHandler(transferHandler = new TreeTableTransferHandler(this));
        if (JDUtilities.getJavaVersion() > 1.6) {
            setDropMode(DropMode.USE_SELECTION);
        }
        tooltipTimer = new TooltipTimer(2000);
        tooltipTimer.start();
    }

    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {
        DownloadLink link;
        SubConfiguration guiConfig = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
        FilePackage fp;
        Vector<DownloadLink> links;
        Vector<FilePackage> fps;

        switch (e.getID()) {

        case TreeTableAction.DOWNLOAD_INFO:
            link = (DownloadLink) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlink");
            new DownloadInfo(SimpleGUI.CURRENTGUI.getFrame(), link);

            break;

        case TreeTableAction.DOWNLOAD_BROWSE_LINK:
            link = (DownloadLink) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlink");
            if (link.getLinkType() == DownloadLink.LINKTYPE_NORMAL) {

                try {
                    new BrowserLauncher().openURLinBrowser(link.getBrowserUrl());
                } catch (BrowserLaunchingInitializingException e1) {
                    e1.printStackTrace();
                } catch (UnsupportedOperatingSystemException e1) {
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
                if (SimpleGUI.CURRENTGUI.showConfirmDialog(JDLocale.L("gui.downloadlist.delete", "Ausgewählte Links wirklich entfernen?"))) {

                    JDUtilities.getController().removeDownloadLinks(links);
                    JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));

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
                links.elementAt(i).getLinkStatus().setStatus(LinkStatus.TODO);
                links.elementAt(i).getLinkStatus().setStatusText(JDLocale.L("gui.linklist.status.doresume", "Warte auf Fortsetzung"));
                // links.elementAt(i).getPlugin().resetSteps();
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
            if (ret == null) { return; }
            if (JDUtilities.getFileExtension(ret) == null || !JDUtilities.getFileExtension(ret).equalsIgnoreCase("dlc")) {

                ret = new File(ret.getAbsolutePath() + ".dlc");
            }

            JDUtilities.getController().saveDLC(ret, links);
            break;
        case TreeTableAction.DOWNLOAD_NEW_PACKAGE:
            links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlinks");
            FilePackage parentFP = links.get(0).getFilePackage();
            String name = SimpleGUI.CURRENTGUI.showUserInputDialog(JDLocale.L("gui.linklist.newpackage.message", "Name of the new package"), parentFP.getName());
            if (name != null) {
                JDUtilities.getController().removeDownloadLinks(links);
                FilePackage nfp = new FilePackage();
                nfp.setName(name);
                nfp.setDownloadDirectory(parentFP.getDownloadDirectory());
                nfp.setPassword(parentFP.getPassword());
                nfp.setComment(parentFP.getComment());

                for (int i = 0; i < links.size(); i++) {
                    links.elementAt(i).setFilePackage(nfp);
                }
                JDUtilities.getController().addAllLinks(links);
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));

            break;

        case TreeTableAction.DOWNLOAD_RESET:
            links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlinks");
            if (!guiConfig.getBooleanProperty(SimpleGUI.PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                if (SimpleGUI.CURRENTGUI.showConfirmDialog(JDLocale.L("gui.downloadlist.reset", "Reset selected downloads?"))) {
                    for (int i = 0; i < links.size(); i++) {
                        // if (!links.elementAt(i).isPluginActive()) {
                        links.elementAt(i).getLinkStatus().reset();
                        links.elementAt(i).reset();
                        links.elementAt(i).getPlugin().resetHosterWaitTime();
                    }
                    JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
                }
            } else {
                for (int i = 0; i < links.size(); i++) {
                    // if (!links.elementAt(i).isPluginActive()) {
                    links.elementAt(i).getLinkStatus().reset();
                    links.elementAt(i).reset();
                    links.elementAt(i).getPlugin().resetHosterWaitTime();
                }
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
            }

            break;

        case TreeTableAction.DOWNLOAD_COPY_PASSWORD:
            link = (DownloadLink) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlink");
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(link.getFilePackage().getPassword()), null);

            break;

        case TreeTableAction.PACKAGE_INFO:
            fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");
            new PackageInfo(SimpleGUI.CURRENTGUI.getFrame(), fp);

            break;

        case TreeTableAction.PACKAGE_EDIT_DIR:
            fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");

            fc = new JDFileChooser();
            fc.setApproveButtonText(JDLocale.L("gui.btn_ok", "OK"));
            fc.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);

            fc.setCurrentDirectory(fp.getDownloadDirectory() != null ? new File(fp.getDownloadDirectory()) : JDUtilities.getResourceFile("downloads"));
            fc.showOpenDialog(this);
            ret = fc.getSelectedFile();

            if (ret != null) {
                fp.setDownloadDirectory(ret.getAbsolutePath());
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));

            break;
        case TreeTableAction.PACKAGE_EDIT_NAME:
            fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");
            name = SimpleGUI.CURRENTGUI.showUserInputDialog(JDLocale.L("gui.linklist.editpackagename.message", "Neuer Paketname"), fp.getName());

            if (name != null) {
                fp.setName(name);
            }
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
            if (!guiConfig.getBooleanProperty(SimpleGUI.PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                if (SimpleGUI.CURRENTGUI.showConfirmDialog(JDLocale.L("gui.downloadlist.delete", "Ausgewählte Links wirklich entfernen?"))) {
                    for (FilePackage filePackage : fps) {
                        JDUtilities.getController().removePackage(filePackage);
                    }
                }
            } else {
                for (FilePackage filePackage : fps) {
                    JDUtilities.getController().removePackage(filePackage);
                }
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
            if (ret == null) { return; }
            if (JDUtilities.getFileExtension(ret) == null || !JDUtilities.getFileExtension(ret).equalsIgnoreCase("dlc")) {

                ret = new File(ret.getAbsolutePath() + ".dlc");
            }

            JDUtilities.getController().saveDLC(ret, links);
            break;

        case TreeTableAction.PACKAGE_RESET:
            if (!guiConfig.getBooleanProperty(SimpleGUI.PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                if (SimpleGUI.CURRENTGUI.showConfirmDialog(JDLocale.L("gui.downloadlist.reset", "Reset selected downloads?"))) {
                    fps = (Vector<FilePackage>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("packages");

                    for (Iterator<FilePackage> it = fps.iterator(); it.hasNext();) {
                        next = it.next();
                        for (int i = 0; i < next.size(); i++) {
                            if (!next.get(i).getLinkStatus().isPluginActive()) {
                                next.get(i).getLinkStatus().setStatus(LinkStatus.TODO);
                                next.get(i).getLinkStatus().setStatusText("");
                                next.get(i).getPlugin().resetHosterWaitTime();
                                next.get(i).reset();
                            }
                        }
                    }
                    JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
                }
            } else {
                fps = (Vector<FilePackage>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("packages");

                for (Iterator<FilePackage> it = fps.iterator(); it.hasNext();) {
                    next = it.next();
                    for (int i = 0; i < next.size(); i++) {
                        if (!next.get(i).getLinkStatus().isPluginActive()) {
                            next.get(i).getLinkStatus().setStatus(LinkStatus.TODO);
                            next.get(i).getLinkStatus().setStatusText("");
                            next.get(i).getPlugin().resetHosterWaitTime();
                            next.get(i).reset();
                        }
                    }
                }
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
            }
            break;
        case TreeTableAction.PACKAGE_SORT:
            fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");
            fp.sort(null);
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));

            break;

        case TreeTableAction.PACKAGE_COPY_PASSWORD:
            fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(fp.getPassword()), null);

            break;
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void fireTableChanged(int id, Object param) {
        TreeModelSupport supporter = getDownladTreeTableModel().getModelSupporter();
        if (updatePackages == null) {
            updatePackages = new HashMap<FilePackage, ArrayList<DownloadLink>>();
        }
        switch (id) {
        /*
         * Es werden nur die Ãœbergebenen LinkPfade aktualisiert.
         * REFRESH_SPECIFIED_LINKS kann als Parameter eine Arraylist oder einen
         * einzellnen DownloadLink haben. ArrayLists werden nicht ausgewertet.
         * in diesem Fall wird die komplette Tabelle neu gezeichnet.
         */
        case DownloadLinksView.REFRESH_SPECIFIED_LINKS:
            // logger.info("REFRESH SPECS COMPLETE");
            if (param instanceof DownloadLink) {
                currentLink = (DownloadLink) param;
                // logger.info("Updatesingle "+currentLink);
                if (updatePackages.containsKey(currentLink.getFilePackage())) {
                    if (!updatePackages.get(currentLink.getFilePackage()).contains(currentLink)) {
                        updatePackages.get(currentLink.getFilePackage()).add(currentLink);
                    }
                } else {
                    ArrayList<DownloadLink> ar = new ArrayList<DownloadLink>();
                    updatePackages.put(currentLink.getFilePackage(), ar);
                    ar.add(currentLink);
                }

            } else if (param instanceof ArrayList) {
                for (Iterator<DownloadLink> it = ((ArrayList<DownloadLink>) param).iterator(); it.hasNext();) {
                    currentLink = it.next();
                    if (updatePackages.containsKey(currentLink.getFilePackage())) {
                        if (!updatePackages.get(currentLink.getFilePackage()).contains(currentLink)) {
                            updatePackages.get(currentLink.getFilePackage()).add(currentLink);
                        }
                    } else {
                        ArrayList<DownloadLink> ar = new ArrayList<DownloadLink>();
                        updatePackages.put(currentLink.getFilePackage(), ar);
                        ar.add(currentLink);
                    }
                }
            }
            if (System.currentTimeMillis() - updateTimer > UPDATE_INTERVAL && updatePackages != null) {
                Entry<FilePackage, ArrayList<DownloadLink>> next;
                DownloadLink next3;

                for (Iterator<Entry<FilePackage, ArrayList<DownloadLink>>> it2 = updatePackages.entrySet().iterator(); it2.hasNext();) {
                    next = it2.next();
                    // logger.info("Refresh " + next.getKey() + " - " +
                    // next.getValue().size());

                    if (!model.containesPackage(next.getKey())) {
                        continue;
                    }
                    supporter.firePathChanged(new TreePath(new Object[] { model.getRoot(), next.getKey() }));

                    if (next.getKey().getBooleanProperty(PROPERTY_EXPANDED, false)) {

                        int[] ind = new int[next.getValue().size()];
                        Object[] objs = new Object[next.getValue().size()];
                        int i = 0;

                        for (Iterator<DownloadLink> it3 = next.getValue().iterator(); it3.hasNext();) {
                            next3 = it3.next();
                            if (!next.getKey().contains(next3)) {
                                logger.warning("Dauniel bug");
                                continue;
                            }
                            ind[i] = next.getKey().indexOf(next3);
                            objs[i] = next3;

                            i++;
                            // logger.info(" children: " + next3 + " - " +
                            // ind[i]);
                        }

                        if (i > 0) {
                            supporter.fireChildrenChanged(new TreePath(new Object[] { model.getRoot(), next.getKey() }), ind, objs);
                        }
                    }

                }
                updatePackages = null;
                updateTimer = System.currentTimeMillis();
            }

            break;
        case DownloadLinksView.REFRESH_ALL_DATA_CHANGED:
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

    public TableCellRenderer getCellRenderer(int row, int col) {
        if (col >= 1) { return cellRenderer; }
        return super.getCellRenderer(row, col);
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

    void ignoreSelectionsAndExpansions(int i) {
        ignoreSelectionsAndExpansionsUntil = System.currentTimeMillis() + i;
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_DELETE) {

            Vector<DownloadLink> links = getSelectedDownloadLinks();
            Vector<FilePackage> fps = getSelectedFilePackages();

            if (fps.size() == 0 && links.size() == 0) { return; }

            for (Iterator<DownloadLink> it = links.iterator(); it.hasNext();) {
                DownloadLink link = it.next();
                for (FilePackage filePackage : fps) {
                    if (filePackage.contains(link)) {
                        it.remove();
                        break;
                    }
                }

            }

            if (!JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getBooleanProperty(SimpleGUI.PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                if (SimpleGUI.CURRENTGUI.showConfirmDialog(JDLocale.L("gui.downloadlist.delete", "Ausgewählte Links wirklich entfernen?"))) {
                    // zuerst Pakete entfernen
                    for (FilePackage filePackage : fps) {
                        JDUtilities.getController().removePackage(filePackage);
                    }

                    JDUtilities.getController().removeDownloadLinks(links);
                    JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));
                }
            } else {
                for (FilePackage filePackage : fps) {
                    JDUtilities.getController().removePackage(filePackage);
                }

                JDUtilities.getController().removeDownloadLinks(links);
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));
            }

        } else if (e.getKeyCode() == KeyEvent.VK_UP && e.isControlDown()) {
            int cur = getSelectedRow();
            if (e.isAltDown()) {
                moveSelectedItems(JDAction.ITEMS_MOVE_TOP);
                getSelectionModel().setSelectionInterval(0, 0);
            } else {
                moveSelectedItems(JDAction.ITEMS_MOVE_UP);
                cur = Math.max(0, cur - 1);
                getSelectionModel().setSelectionInterval(cur, cur);
            }
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN && e.isControlDown()) {
            int cur = getSelectedRow();
            int len = getVisibleRowCount();
            if (e.isAltDown()) {
                moveSelectedItems(JDAction.ITEMS_MOVE_BOTTOM);
                getSelectionModel().setSelectionInterval(len, len);
            } else {
                moveSelectedItems(JDAction.ITEMS_MOVE_DOWN);
                cur = Math.min(len, cur + 1);
                getSelectionModel().setSelectionInterval(cur, cur);
            }
        }

    }

    public void keyTyped(KeyEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
        ignoreSelectionsAndExpansions(-100);
        if (tooltip != null) {
            tooltip.destroy();
            tooltip = null;
        }
        if (e.getButton() == MouseEvent.BUTTON1 && 2 == e.getClickCount()) {
            Point point = e.getPoint();
            int row = rowAtPoint(point);
            TreePath path = getPathForRow(row);
            if (path == null) { return; }
            Object obj = path.getLastPathComponent();
            if (obj instanceof DownloadLink) {
                new DownloadInfo(SimpleGUI.CURRENTGUI.getFrame(), (DownloadLink) getPathForRow(row).getLastPathComponent());
            }
        }
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {
        tooltipTimer.setCaller(null);
    }

    public void mouseMoved(MouseEvent e) {
        final int moRow = rowAtPoint(e.getPoint());
        final int moColumn = columnAtPoint(e.getPoint());
        mousePoint = e.getPoint();
        Point screen = getLocationOnScreen();
        mousePoint.x += screen.x;
        mousePoint.y += screen.y;
        if (getPathForRow(moRow) == null) {
            mouseOverRow = moRow;
            mouseOverColumn = moColumn;
            tooltipTimer.setCaller(null);
            if (tooltip != null) {
                tooltip.destroy();
                tooltip = null;
            }
            return;
        }

        if (moRow != mouseOverRow || moColumn != mouseOverColumn) {
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

        mouseOverRow = moRow;
        mouseOverColumn = moColumn;

    }

    public void mousePressed(MouseEvent e) {
        ignoreSelectionsAndExpansions(-100);
        // TODO: isPopupTrigger() funktioniert nicht
        // logger.info("Press"+e.isPopupTrigger() );
        Point point = e.getPoint();
        int row = rowAtPoint(point);

        if (!isRowSelected(row)) {
            getTreeSelectionModel().clearSelection();
            getTreeSelectionModel().addSelectionPath(getPathForRow(row));
        }
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {

            if (getPathForRow(row) == null) { return; }
            Object obj = getPathForRow(row).getLastPathComponent();
            JMenuItem tmp;
            JPopupMenu popup = new JPopupMenu();
            if (obj instanceof DownloadLink) {
                Vector<FilePackage> fps = new Vector<FilePackage>();
                int enabled = 0;
                int disabled = 0;
                int resumeable = 0;
                for (DownloadLink next : getSelectedDownloadLinks()) {
                    if (!fps.contains(next.getFilePackage())) {
                        fps.add(next.getFilePackage());
                    }
                    if (next.isEnabled()) {
                        enabled++;
                    } else {
                        disabled++;
                    }
                    if (!next.getLinkStatus().isPluginActive() && next.getLinkStatus().isFailed()) {
                        resumeable++;
                    }
                }

                Plugin plg = ((DownloadLink) obj).getPlugin();
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.info", "Detailansicht"), TreeTableAction.DOWNLOAD_INFO, new Property("downloadlink", obj))));

                JMenu packagePopup = new JMenu(JDLocale.L("gui.table.contextmenu.packagesubmenu", "Paket"));
                JMenu pluginPopup = new JMenu(JDLocale.L("gui.table.contextmenu.pluginsubmenu", "Plugin") + " (" + plg.getHost() + ")");

                ArrayList<MenuItem> pluginMenuEntries = plg.createMenuitems();
                if (pluginMenuEntries != null) {
                    for (MenuItem next : pluginMenuEntries) {
                        JMenuItem mi = SimpleGUI.getJMenuItem(next);

                        if (mi == null) {
                            pluginPopup.addSeparator();
                        } else {
                            pluginPopup.add(mi);
                        }
                    }
                }

                if (pluginMenuEntries != null && pluginMenuEntries.size() == 0) pluginPopup.setEnabled(false);

                popup.add(packagePopup);
                popup.add(pluginPopup);

                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.downloadDir", "Zielordner öffnen"), TreeTableAction.DOWNLOAD_DOWNLOAD_DIR, new Property("downloadlink", obj))));
                popup.add(tmp = new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.browseLink", "im Browser öffnen"), TreeTableAction.DOWNLOAD_BROWSE_LINK, new Property("downloadlink", obj))));
                if (((DownloadLink) obj).getLinkType() != DownloadLink.LINKTYPE_NORMAL) tmp.setEnabled(false);

                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.delete", "entfernen"), TreeTableAction.DOWNLOAD_DELETE, new Property("downloadlinks", getSelectedDownloadLinks()))));
                popup.add(tmp = new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.enable", "aktivieren") + " (" + disabled + ")", TreeTableAction.DOWNLOAD_ENABLE, new Property("downloadlinks", getSelectedDownloadLinks()))));
                if (disabled == 0) tmp.setEnabled(false);
                popup.add(tmp = new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.disable", "deaktivieren") + " (" + enabled + ")", TreeTableAction.DOWNLOAD_DISABLE, new Property("downloadlinks", getSelectedDownloadLinks()))));
                if (enabled == 0) tmp.setEnabled(false);
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.reset", "zurücksetzen"), TreeTableAction.DOWNLOAD_RESET, new Property("downloadlinks", getSelectedDownloadLinks()))));
                popup.add(tmp = new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.resume", "fortsetzen") + " (" + resumeable + ")", TreeTableAction.DOWNLOAD_RESUME, new Property("downloadlinks", getSelectedDownloadLinks()))));
                if (resumeable == 0) tmp.setEnabled(false);

                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.newpackage", "In neues Paket verschieben"), TreeTableAction.DOWNLOAD_NEW_PACKAGE, new Property("downloadlinks", getSelectedDownloadLinks()))));

                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.copyPassword", "Copy Password"), TreeTableAction.DOWNLOAD_COPY_PASSWORD, new Property("downloadlink", obj))));

                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.dlc", "DLC erstellen"), TreeTableAction.DOWNLOAD_DLC, new Property("downloadlinks", getSelectedDownloadLinks()))));

                for (Component comp : createPackageMenu(((DownloadLink) obj).getFilePackage(), fps)) {
                    packagePopup.add(comp);
                }
            } else {
                for (Component comp : createPackageMenu((FilePackage) obj, getSelectedFilePackages())) {
                    popup.add(comp);
                }
            }
            popup.show(this, point.x, point.y);

            logger.info(getSelectedFilePackages() + "");
            logger.info(getSelectedDownloadLinks() + "");
        }
    }

    private Vector<Component> createPackageMenu(FilePackage fp, Vector<FilePackage> fps) {
        Vector<Component> res = new Vector<Component>();
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.packageinfo", "Detailansicht"), TreeTableAction.PACKAGE_INFO, new Property("package", fp))));
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.packagesort", "Paket sortieren"), TreeTableAction.PACKAGE_SORT, new Property("package", fp))));

        res.add(new JSeparator());
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.editdownloadDir", "Zielordner ändern"), TreeTableAction.PACKAGE_EDIT_DIR, new Property("package", fp))));
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.packagedownloadDir", "Zielordner öffnen"), TreeTableAction.PACKAGE_DOWNLOAD_DIR, new Property("package", fp))));
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.editpackagename", "Paketname ändern"), TreeTableAction.PACKAGE_EDIT_NAME, new Property("package", fp))));

        res.add(new JSeparator());
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.deletepackage", "entfernen"), TreeTableAction.PACKAGE_DELETE, new Property("packages", fps))));
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.enablepackage", "aktivieren"), TreeTableAction.PACKAGE_ENABLE, new Property("packages", fps))));
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.disablepackage", "deaktivieren"), TreeTableAction.PACKAGE_DISABLE, new Property("packages", fps))));
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.resetpackage", "zurücksetzen"), TreeTableAction.PACKAGE_RESET, new Property("packages", fps))));

        res.add(new JSeparator());
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.copyPassword_package", "Copy Password"), TreeTableAction.PACKAGE_COPY_PASSWORD, new Property("package", fp))));

        res.add(new JSeparator());
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.dlc_package", "DLC erstellen"), TreeTableAction.PACKAGE_DLC, new Property("packages", fps))));

        return res;
    }

    public void mouseReleased(MouseEvent e) {
        TreePath path = getPathForLocation(e.getX(), e.getY());

        if (path != null && path.getLastPathComponent() instanceof FilePackage) {
            JDSounds.PT("sound.gui.selectPackage");
        } else if (path != null) {
            JDSounds.PT("sound.gui.selectLink");
        }

        tooltipTimer.setCaller(null);
        if (tooltip != null) {
            tooltip.destroy();
            tooltip = null;
        }
    }

    public void moveSelectedItems(int id) {
        Vector<DownloadLink> links = getSelectedDownloadLinks();
        Vector<FilePackage> fps = getSelectedFilePackages();

        ignoreSelectionsAndExpansions(2000);
        logger.info(links.size() + " - " + fps.size());
        if (links.size() >= fps.size()) {
            if (links.size() == 0) { return; }

            switch (id) {
            case JDAction.ITEMS_MOVE_BOTTOM:
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

    private void showToolTip(int mouseOverRow, int mouseOverColumn) {
        if (tooltip != null) {
            tooltip.destroy();
            tooltip = null;
        }

        if (!SimpleGUI.CURRENTGUI.getFrame().isActive() || transferHandler.isDragging) { return; }
        StringBuffer sb = new StringBuffer();
        sb.append("<div>");
        Object obj = getPathForRow(mouseOverRow).getLastPathComponent();

        if (obj instanceof DownloadLink) {
            DownloadLink link = (DownloadLink) obj;
            int column = this.getColumn(mouseOverColumn).getModelIndex();
            switch (column) {
            case DownloadTreeTableModel.COL_PART:
                sb.append("<h1>" + link.getFileOutput() + "</h1>");
                break;
            case DownloadTreeTableModel.COL_FILE:
                sb.append("<h1>" + link.getFileOutput() + "</h1><hr>");
                if (link.getLinkType() == DownloadLink.LINKTYPE_NORMAL) {
                    sb.append("<p>" + link.getBrowserUrl() + "</p>");
                }
                break;
            case DownloadTreeTableModel.COL_HOSTER:
                PluginForHost plg = (PluginForHost) link.getPlugin();

                sb.append("<h1>" + plg.getPluginID() + "</h1><hr>");
                sb.append("<table>");
                sb.append("<tr><td>" + JDLocale.L("gui.downloadlist.tooltip.connections", "Akt. Verbindungen:") + "</td><td>" + plg.getCurrentConnections() + "/" + plg.getMaxConnections() + "</td></tr>");
                sb.append("<tr><td>" + JDLocale.L("gui.downloadlist.tooltip.simultan_downloads", "Max. gleichzeitige Downloads:") + "</td><td>" + plg.getMaxSimultanDownloadNum(link) + "</td></tr>");
                sb.append("</table>");
                break;
            case DownloadTreeTableModel.COL_STATUS:
                sb.append("<p>" + link.getLinkStatus().getStatusString() + "</p>");
                break;
            case DownloadTreeTableModel.COL_PROGRESS:
                if (link.getDownloadInstance() == null) { return; }
                DownloadInterface dl = link.getDownloadInstance();
                sb.append("<h1>" + link.getFileOutput() + "</h1><hr>");
                sb.append("<table>");
                for (Chunk chunk : dl.getChunks()) {
                    long loaded = chunk.getBytesLoaded();
                    long total = chunk.getChunkSize();
                    long percent = chunk.getPercent() / 100;
                    sb.append("<tr>");
                    sb.append("<td> Verbindung " + chunk.getID() + "</td>");
                    sb.append("<td>" + JDUtilities.formatKbReadable(chunk.getBytesPerSecond() / 1024) + "/s</td>");
                    sb.append("<td>" + JDUtilities.formatKbReadable(loaded / 1024) + "/" + JDUtilities.formatKbReadable(total / 1024) + "</td>");
                    sb.append("<td><table width='100px' height='5px'  cellpadding='0' cellspacing='0' ><tr><td width='" + percent + "%' bgcolor='#000000'/><td width='" + (100 - percent) + "%' bgcolor='#cccccc'/></tr> </table></td>");
                    sb.append("</tr>");
                }
                sb.append("</table>");
            }
        } else {
            FilePackage fp = (FilePackage) obj;
            int column = this.getColumn(mouseOverColumn).getModelIndex();
            switch (column) {
            case DownloadTreeTableModel.COL_PART:
                sb.append("<h1>" + fp.getName() + "</h1><hr>");
                if (fp.hasComment()) sb.append("<p>" + fp.getComment() + "</p>");
                if (fp.hasPassword()) sb.append("<p>" + fp.getPassword() + "</p>");
                if (fp.hasDownloadDirectory()) sb.append("<p>" + fp.getDownloadDirectory() + "</p>");
                break;
            case DownloadTreeTableModel.COL_FILE:
                sb.append("<h1>" + fp.getName() + "</h1><hr>");
                sb.append("<table>");
                sb.append("<tr><td>" + JDLocale.L("gui.downloadlist.tooltip.partnum", "Teile:") + "</td><td>" + fp.size() + "</td></tr>");
                sb.append("<tr><td>" + JDLocale.L("gui.downloadlist.tooltip.partsfinished", "Davon fertig:") + "</td><td>" + fp.getLinksFinished() + "</td></tr>");
                sb.append("<tr><td>" + JDLocale.L("gui.downloadlist.tooltip.partsfailed", "Davon fehlerhaft:") + "</td><td>" + fp.getLinksFailed() + "</td></tr>");
                sb.append("<tr><td>" + JDLocale.L("gui.downloadlist.tooltip.partsactive", "Gerade aktiv:") + "</td><td>" + fp.getLinksInProgress() + "</td></tr>");
                sb.append("</table>");
                break;
            case DownloadTreeTableModel.COL_HOSTER:
                return;
            case DownloadTreeTableModel.COL_STATUS:
                return;
            case DownloadTreeTableModel.COL_PROGRESS:
                return;
            }
        }

        sb.append("</div>");
        if (sb.length() <= 22) { return; }
        tooltip = HTMLTooltip.show(sb.toString(), mousePoint);

    }

    /**
     * Die Listener speichern bei einer Selection oder beim aus/Einklappen von
     * Ästen deren Status
     */
    public void treeCollapsed(TreeExpansionEvent event) {
        if (ignoreSelectionsAndExpansionsUntil > System.currentTimeMillis()) { return; }
        FilePackage fp = (FilePackage) event.getPath().getLastPathComponent();
        fp.setProperty(DownloadTreeTable.PROPERTY_EXPANDED, false);
    }

    public void treeExpanded(TreeExpansionEvent event) {
        if (ignoreSelectionsAndExpansionsUntil > System.currentTimeMillis()) { return; }
        FilePackage fp = (FilePackage) event.getPath().getLastPathComponent();
        fp.setProperty(DownloadTreeTable.PROPERTY_EXPANDED, true);
    }

    /**
     * Diese Methode setzt die gespeicherten Werte für die Selection und
     * Expansion
     */
    public void updateSelectionAndExpandStatus() {
        // logger.info("UPD");

        int i = 0;
        while (getPathForRow(i) != null) {
            if (getPathForRow(i).getLastPathComponent() instanceof DownloadLink) {
                DownloadLink dl = (DownloadLink) getPathForRow(i).getLastPathComponent();
                if (dl.getBooleanProperty(PROPERTY_SELECTED, false)) {
                    getTreeSelectionModel().addSelectionPath(getPathForRow(i));
                }
            } else {
                FilePackage fp = (FilePackage) getPathForRow(i).getLastPathComponent();
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

    public void valueChanged(TreeSelectionEvent e) {

        TreePath[] paths = e.getPaths();
        // logger.info("" + e);
        if (ignoreSelectionsAndExpansionsUntil > System.currentTimeMillis()) { return; }
        for (TreePath path : paths) {
            if (e.isAddedPath(path)) {
                if (path.getLastPathComponent() instanceof DownloadLink) {
                    ((DownloadLink) path.getLastPathComponent()).setProperty(DownloadTreeTable.PROPERTY_SELECTED, true);

                } else {
                    ((FilePackage) path.getLastPathComponent()).setProperty(DownloadTreeTable.PROPERTY_SELECTED, true);

                }
            } else {

                if (path.getLastPathComponent() instanceof DownloadLink) {
                    ((DownloadLink) path.getLastPathComponent()).setProperty(DownloadTreeTable.PROPERTY_SELECTED, false);

                } else {
                    ((FilePackage) path.getLastPathComponent()).setProperty(DownloadTreeTable.PROPERTY_SELECTED, false);

                }
            }
        }

    }

    public void windowGainedFocus(WindowEvent e) {
    }

    public void windowLostFocus(WindowEvent e) {
        if (tooltip != null && e.getOppositeWindow() != tooltip) {
            tooltip.destroy();
            tooltip = null;
        }

    }

}