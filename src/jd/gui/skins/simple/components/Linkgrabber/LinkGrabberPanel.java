package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDController;
import jd.controlling.LinkGrabberController;
import jd.controlling.LinkGrabberControllerEvent;
import jd.controlling.LinkGrabberControllerListener;
import jd.controlling.ProgressController;
import jd.controlling.ProgressControllerEvent;
import jd.controlling.ProgressControllerListener;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.JDCollapser;
import jd.gui.skins.simple.JDToolBar;
import jd.gui.skins.simple.JTabbedPanel;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.tasks.LinkGrabberTaskPane;
import jd.nutils.io.JDFileFilter;
import jd.nutils.io.JDIO;
import jd.nutils.jobber.Jobber;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class LinkGrabberPanel extends JTabbedPanel implements ActionListener, LinkCheckListener, ProgressControllerListener, LinkGrabberControllerListener {

    private static final long serialVersionUID = 1607433619381447389L;

    private Vector<DownloadLink> waitingList = new Vector<DownloadLink>();

    private LinkGrabberTreeTable internalTreeTable;

    protected Logger logger = jd.controlling.JDLogger.getLogger();

    private Thread gatherer;
    private boolean gatherer_running = false;
    private ProgressController pc;

    private LinkGrabberFilePackageInfo filePackageInfo;
    private Timer gathertimer;

    private Jobber checkJobbers = new Jobber(4);

    private LinkCheck lc = LinkCheck.getLinkChecker();
    private Timer Update_Async;
    private static LinkGrabberPanel INSTANCE;
    private boolean visible = true;

    private LinkGrabberController LGINSTANCE = null;

    protected boolean tablerefreshinprogress = false;

    public static synchronized LinkGrabberPanel getLinkGrabber() {
        if (INSTANCE == null) INSTANCE = new LinkGrabberPanel();
        return INSTANCE;
    }

    public boolean isRunning() {
        return gatherer_running;
    }

    public boolean needsViewport() {

        return false;
    }

    private LinkGrabberPanel() {
        super(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[fill,grow]"));
        internalTreeTable = new LinkGrabberTreeTable(new LinkGrabberTreeTableModel(this), this);
        JScrollPane scrollPane = new JScrollPane(internalTreeTable);
        this.add(scrollPane, "cell 0 0");
        filePackageInfo = new LinkGrabberFilePackageInfo();
        Update_Async = new Timer(250, this);
        Update_Async.setInitialDelay(250);
        Update_Async.setRepeats(false);
        gathertimer = new Timer(2000, LinkGrabberPanel.this);
        gathertimer.setInitialDelay(2000);
        gathertimer.setRepeats(false);
        INSTANCE = this;
        LGINSTANCE = LinkGrabberController.getInstance();
        LGINSTANCE.getBroadcaster().addListener(this);
    }

    public void showFilePackageInfo(LinkGrabberFilePackage fp) {
        filePackageInfo.setPackage(fp);
        JDCollapser.getInstance().setContentPanel(filePackageInfo);
        JDCollapser.getInstance().setTitle(JDLocale.L("gui.linkgrabber.packagetab.title", "File package"));
        JDCollapser.getInstance().setVisible(true);
        JDCollapser.getInstance().setCollapsed(false);
    }

    public void hideFilePackageInfo() {
        JDCollapser.getInstance().setCollapsed(true);
    }

    public synchronized void fireTableChanged() {
        if (tablerefreshinprogress) return;
        new Thread() {
            public void run() {
                tablerefreshinprogress = true;
                this.setName("LinkGrabber: refresh Table");
                synchronized (LGINSTANCE.getPackages()) {
                    if (gatherer_running) {
                        Vector<LinkGrabberFilePackage> fps = LGINSTANCE.getPackages();
                        int count = 0;
                        for (LinkGrabberFilePackage fp : fps) {
                            count += 1 + fp.getDownloadLinks().size();
                        }
                        if (count > (internalTreeTable.getSize().getHeight() / 16.0)) {
                            for (LinkGrabberFilePackage fp : fps) {
                                if (!(Boolean) fp.getProperty(LinkGrabberTreeTable.PROPERTY_USEREXPAND, false)) fp.setProperty(LinkGrabberTreeTable.PROPERTY_EXPANDED, false);
                            }
                        } else {
                            for (LinkGrabberFilePackage fp : fps) {
                                if (!(Boolean) fp.getProperty(LinkGrabberTreeTable.PROPERTY_USEREXPAND, false)) fp.setProperty(LinkGrabberTreeTable.PROPERTY_EXPANDED, true);
                            }
                        }
                    }
                    internalTreeTable.fireTableChanged();
                    tablerefreshinprogress = false;
                }
            }
        }.start();
    }

    // @Override
    public void onHide() {
        LGINSTANCE.getBroadcaster().removeListener(this);
        Update_Async.stop();
        visible = false;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                SimpleGUI.CURRENTGUI.getToolBar().setEnabled(JDToolBar.ENTRY_ALL, true, JDLocale.L("gui.linkgrabber.toolbar.disabled", "Switch to downloadtask to enable buttons"));
            }
        });
    }

    public synchronized void addLinks(final DownloadLink[] linkList) {
        new Thread() {
            public void run() {
                for (DownloadLink element : linkList) {
                    if (LGINSTANCE.isDupe(element)) continue;
                    addToWaitingList(element);
                }
                Update_Async.restart();
                gathertimer.restart();
            }
        }.start();
    }

    public synchronized void addToWaitingList(DownloadLink element) {
        waitingList.add(element);
        checkAlreadyinList(element);
        LGINSTANCE.attachToPackagesFirstStage(element);
    }

    private void stopLinkGatherer() {
        lc.getBroadcaster().removeListener(this);
        if (gatherer != null && gatherer.isAlive()) {
            gatherer_running = false;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    pc.setStatusText(pc.getStatusText() + ": " + JDLocale.L("gui.linkgrabber.aborted", "Aborted"));
                    pc.finalize(5000l);
                }
            });
            checkJobbers.stop();
            gatherer.interrupt();
        }
    }

    public ArrayList<String> getExtensions() {
        ArrayList<String> extensions = new ArrayList<String>();
        String ext = null;
        synchronized (LGINSTANCE.getPackages()) {
            Vector<LinkGrabberFilePackage> fps = new Vector<LinkGrabberFilePackage>(LGINSTANCE.getPackages());
            fps.add(LGINSTANCE.getFILTERPACKAGE());
            for (LinkGrabberFilePackage fp : fps) {
                synchronized (fp.getDownloadLinks()) {
                    for (DownloadLink l : fp.getDownloadLinks()) {
                        ext = JDIO.getFileExtension(l.getName());
                        if (ext != null && ext.trim().length() > 1) {
                            if (!extensions.contains(ext.trim())) extensions.add(ext.trim());
                        }
                    }
                }
            }

        }
        Collections.sort(extensions);
        return extensions;
    }

    private void startLinkGatherer() {
        if (gatherer != null && gatherer.isAlive()) { return; }
        gatherer = new Thread() {
            public void run() {
                setName("LinkGrabber");
                gatherer_running = true;
                pc = new ProgressController(JDLocale.L("gui.linkgrabber.pc.linkgrabber", "LinkGrabber operations pending..."));
                pc.getBroadcaster().addListener(INSTANCE);
                lc.getBroadcaster().addListener(INSTANCE);
                pc.setRange(0);
                while (waitingList.size() > 0 || lc.isRunning()) {
                    Vector<DownloadLink> currentList = new Vector<DownloadLink>();
                    synchronized (waitingList) {
                        currentList = new Vector<DownloadLink>(waitingList);
                        pc.addToMax(currentList.size());
                        waitingList.removeAll(currentList);
                    }
                    if (!LGINSTANCE.isLinkCheckEnabled()) {
                        /* kein online check, kein multithreaded nötig */
                        afterLinkGrabber(currentList);
                    } else {
                        lc.checkLinks(currentList);
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                lc.getBroadcaster().removeListener(INSTANCE);
                pc.finalize();
                pc.getBroadcaster().removeListener(INSTANCE);
                LGINSTANCE.MergeSingleOffline();
                gatherer_running = false;
            }
        };
        gatherer.start();
    }

    private synchronized void afterLinkGrabber(Vector<DownloadLink> links) {
        for (DownloadLink link : links) {
            if (!gatherer_running) break;
            if (!link.getBooleanProperty("removed", false)) LGINSTANCE.attachToPackagesSecondStage(link);
        }
        pc.increase(links.size());
        Update_Async.restart();
    }

    // @Override
    public void onDisplay() {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                SimpleGUI.CURRENTGUI.getToolBar().setEnabled(JDToolBar.ENTRY_CONTROL | JDToolBar.ENTRY_INTERACTION, false, JDLocale.L("gui.linkgrabber.toolbar.disabled", "Switch to downloadtask to enable buttons"));
            }
        });
        fireTableChanged();
        LGINSTANCE.getBroadcaster().addListener(this);
        visible = true;
        Update_Async.restart();
    }

    public Set<String> getHosterList(Vector<DownloadLink> links) {
        HashMap<String, String> hosters = new HashMap<String, String>();
        for (DownloadLink dl : links) {
            if (!hosters.containsKey(dl.getPlugin().getHost())) {
                hosters.put(dl.getPlugin().getHost(), "");
            }
        }
        return hosters.keySet();
    }

    @SuppressWarnings("unchecked")
    public void actionPerformed(final ActionEvent arg0) {
        new Thread() {
            public void run() {
                this.setName("LinkGrabberPanel: actionPerformed");
                if (arg0.getSource() == INSTANCE.Update_Async) {
                    if (visible) fireTableChanged();
                    return;
                }

                if (arg0.getSource() == INSTANCE.gathertimer) {
                    gathertimer.stop();
                    if (waitingList.size() > 0) {
                        startLinkGatherer();
                    }
                    return;
                }
                Vector<LinkGrabberFilePackage> selected_packages = new Vector<LinkGrabberFilePackage>();
                Vector<DownloadLink> selected_links = new Vector<DownloadLink>();
                int prio = 0;
                String pw = "";
                HashMap<String, Object> prop = new HashMap<String, Object>();
                LinkGrabberFilePackage fp;
                String ext = null;
                Set<String> hoster = null;
                String name = null;
                int col = 0;
                boolean b = false;
                synchronized (LGINSTANCE.getPackages()) {
                    Vector<LinkGrabberFilePackage> fps = LGINSTANCE.getPackages();
                    if (arg0.getSource() instanceof LinkGrabberTaskPane) {
                        switch (arg0.getID()) {
                        case LinkGrabberTreeTableAction.ADD_ALL:
                            LGINSTANCE.getFILTERPACKAGE().clear();
                            selected_packages = new Vector<LinkGrabberFilePackage>(fps);
                            break;
                        case LinkGrabberTreeTableAction.CLEAR:
                            stopLinkGatherer();
                            lc.abortLinkCheck();
                            LGINSTANCE.getFILTERPACKAGE().clear();
                            selected_packages = new Vector<LinkGrabberFilePackage>(LGINSTANCE.getPackages());
                            selected_packages.add(LGINSTANCE.getFILTERPACKAGE());
                            break;
                        case LinkGrabberTreeTableAction.ADD_SELECTED:
                            selected_packages = new Vector<LinkGrabberFilePackage>(INSTANCE.internalTreeTable.getSelectedFilePackages());
                            break;
                        case LinkGrabberTreeTableAction.GUI_LOAD:
                            new GuiRunnable<Object>() {
                                // @Override
                                public Object runSave() {
                                    JDFileChooser fc = new JDFileChooser("_LOADSAVEDLC");
                                    fc.setDialogTitle(JDLocale.L("gui.filechooser.loaddlc", "Load DLC file"));
                                    fc.setFileFilter(new JDFileFilter(null, ".dlc|.rsdf|.ccf|.linkbackup", true));
                                    if (fc.showOpenDialog(null) == JDFileChooser.APPROVE_OPTION) {
                                        File ret2 = fc.getSelectedFile();
                                        if (ret2 != null) {
                                            JDUtilities.getController().loadContainerFile(ret2);
                                        }
                                    }
                                    return null;
                                }
                            }.waitForEDT();
                            return;
                        }
                    } else if (arg0.getSource() instanceof JMenuItem) {
                        switch (arg0.getID()) {
                        case LinkGrabberTreeTableAction.SELECT_HOSTER:
                            hoster = (Set<String>) ((LinkGrabberTreeTableAction) ((JMenuItem) arg0.getSource()).getAction()).getProperty().getProperty("hoster");
                            selected_packages = new Vector<LinkGrabberFilePackage>(fps);
                            selected_packages.add(LGINSTANCE.getFILTERPACKAGE());
                            break;
                        case LinkGrabberTreeTableAction.ADD_ALL:
                            LGINSTANCE.getFILTERPACKAGE().clear();
                        case LinkGrabberTreeTableAction.DELETE_OFFLINE:
                            selected_packages = new Vector<LinkGrabberFilePackage>(fps);
                            selected_packages.add(LGINSTANCE.getFILTERPACKAGE());
                            break;
                        case LinkGrabberTreeTableAction.ADD_SELECTED:
                        case LinkGrabberTreeTableAction.EDIT_DIR:
                            selected_packages = new Vector<LinkGrabberFilePackage>(INSTANCE.internalTreeTable.getSelectedFilePackages());
                            break;
                        case LinkGrabberTreeTableAction.SORT:
                            col = (Integer) ((LinkGrabberTreeTableAction) ((JMenuItem) arg0.getSource()).getAction()).getProperty().getProperty("col");
                            selected_packages = new Vector<LinkGrabberFilePackage>(INSTANCE.internalTreeTable.getSelectedFilePackages());
                            break;
                        case LinkGrabberTreeTableAction.DOWNLOAD_PRIO:
                        case LinkGrabberTreeTableAction.DE_ACTIVATE:
                            prop = (HashMap<String, Object>) ((LinkGrabberTreeTableAction) ((JMenuItem) arg0.getSource()).getAction()).getProperty().getProperty("infos");
                            selected_links = (Vector<DownloadLink>) prop.get("links");
                            break;
                        case LinkGrabberTreeTableAction.DELETE:
                        case LinkGrabberTreeTableAction.SET_PW:
                        case LinkGrabberTreeTableAction.NEW_PACKAGE:
                        case LinkGrabberTreeTableAction.MERGE_PACKAGE:
                            selected_links = (Vector<DownloadLink>) ((LinkGrabberTreeTableAction) ((JMenuItem) arg0.getSource()).getAction()).getProperty().getProperty("links");
                            break;
                        case LinkGrabberTreeTableAction.EXT_FILTER:
                            ext = (String) ((LinkGrabberTreeTableAction) ((JMenuItem) arg0.getSource()).getAction()).getProperty().getProperty("extension");
                            b = ((JCheckBoxMenuItem) arg0.getSource()).isSelected();
                            break;
                        }
                    } else if (arg0.getSource() instanceof LinkGrabberTreeTableAction) {
                        switch (arg0.getID()) {
                        case LinkGrabberTreeTableAction.DELETE:
                            selected_links = (Vector<DownloadLink>) ((LinkGrabberTreeTableAction) arg0.getSource()).getProperty().getProperty("links");
                            break;
                        case LinkGrabberTreeTableAction.SORT_ALL:
                            col = (Integer) ((LinkGrabberTreeTableAction) arg0.getSource()).getProperty().getProperty("col");
                            break;
                        }
                    }
                    switch (arg0.getID()) {
                    case LinkGrabberTreeTableAction.DELETE_OFFLINE:
                        for (LinkGrabberFilePackage fp2 : selected_packages) {
                            fp2.removeOffline();
                        }
                        break;
                    case LinkGrabberTreeTableAction.SORT:
                        for (LinkGrabberFilePackage fp2 : selected_packages) {
                            fp2.sort(col);
                        }
                        break;
                    case LinkGrabberTreeTableAction.SORT_ALL:
                        if (LGINSTANCE.size() == 1) {
                            LGINSTANCE.getPackages().get(0).sort(col);
                        } else
                            LGINSTANCE.sort(col);
                        break;
                    case LinkGrabberTreeTableAction.SELECT_HOSTER:
                        for (LinkGrabberFilePackage fp2 : selected_packages) {
                            fp2.keepHostersOnly(hoster);
                        }
                        break;
                    case LinkGrabberTreeTableAction.EDIT_DIR:
                        final Vector<LinkGrabberFilePackage> selected_packages2 = new Vector<LinkGrabberFilePackage>(selected_packages);
                        new GuiRunnable<Object>() {
                            // @Override
                            public Object runSave() {
                                JDFileChooser fc = new JDFileChooser();
                                fc.setApproveButtonText(JDLocale.L("gui.btn_ok", "OK"));
                                fc.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
                                fc.setCurrentDirectory(new File(selected_packages2.get(0).getDownloadDirectory()));
                                if (fc.showOpenDialog(INSTANCE) == JDFileChooser.APPROVE_OPTION) {
                                    if (fc.getSelectedFile() != null) {
                                        for (LinkGrabberFilePackage fp2 : selected_packages2) {
                                            fp2.setDownloadDirectory(fc.getSelectedFile().getAbsolutePath());
                                        }
                                    }
                                }
                                return null;
                            }
                        }.waitForEDT();
                        break;
                    case LinkGrabberTreeTableAction.MERGE_PACKAGE:
                        fp = LGINSTANCE.getFPwithLink(selected_links.get(0));
                        name = fp.getName();
                    case LinkGrabberTreeTableAction.NEW_PACKAGE:
                        fp = LGINSTANCE.getFPwithLink(selected_links.get(0));
                        if (name == null) name = SimpleGUI.CURRENTGUI.showUserInputDialog(JDLocale.L("gui.linklist.newpackage.message", "Name of the new package"), fp.getName());
                        if (name != null) {
                            LinkGrabberFilePackage nfp = new LinkGrabberFilePackage(name, LGINSTANCE);
                            for (DownloadLink link : selected_links) {
                                LGINSTANCE.AddorMoveDownloadLink(nfp, link);
                            }
                        }
                        return;
                    case LinkGrabberTreeTableAction.SET_PW:
                        pw = SimpleGUI.CURRENTGUI.showUserInputDialog(JDLocale.L("gui.linklist.setpw.message", "Set download password"), null);
                        for (int i = 0; i < selected_links.size(); i++) {
                            fp = LGINSTANCE.getFPwithLink(selected_links.elementAt(i));
                            selected_links.elementAt(i).setProperty("pass", pw);
                            if (fp != null) fp.getBroadcaster().fireEvent(new LinkGrabberFilePackageEvent(fp, LinkGrabberFilePackageEvent.UPDATE_EVENT));
                        }
                        return;
                    case LinkGrabberTreeTableAction.DE_ACTIVATE:
                        b = (Boolean) prop.get("boolean");
                        for (int i = 0; i < selected_links.size(); i++) {
                            selected_links.get(i).setEnabled(b);
                        }
                        Update_Async.restart();
                        return;
                    case LinkGrabberTreeTableAction.ADD_ALL:
                    case LinkGrabberTreeTableAction.ADD_SELECTED:
                        confirmPackages(selected_packages);
                        return;
                    case LinkGrabberTreeTableAction.DELETE:
                        for (DownloadLink link : selected_links) {
                            link.setProperty("removed", true);
                            fp = LGINSTANCE.getFPwithLink(link);
                            fp.remove(link);
                        }
                        return;
                    case LinkGrabberTreeTableAction.CLEAR:
                        for (LinkGrabberFilePackage fp2 : selected_packages) {
                            fp2.setDownloadLinks(new Vector<DownloadLink>());
                        }
                        return;
                    case LinkGrabberTreeTableAction.DOWNLOAD_PRIO:
                        prio = (Integer) prop.get("prio");
                        for (int i = 0; i < selected_links.size(); i++) {
                            selected_links.elementAt(i).setPriority(prio);
                        }
                        return;
                    case LinkGrabberTreeTableAction.EXT_FILTER:
                        LGINSTANCE.FilterExtension(ext, b);
                        return;
                    }
                }
            }
        }.start();
    }

    private void confirmPackages(Vector<LinkGrabberFilePackage> all) {
        for (int i = 0; i < all.size(); ++i) {
            confirmPackage(all.get(i), null);
        }
        if (all.size() == 0) return;
        LGINSTANCE.getBroadcaster().fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.ADDED));
        if (SimpleGuiConstants.GUI_CONFIG.getBooleanProperty(SimpleGuiConstants.PARAM_START_AFTER_ADDING_LINKS, true)) {
            JDController.getInstance().startDownloads();
        }
    }

    private void addToDownloadDirs(String downloadDirectory, String packageName) {
        if (packageName.length() < 5 || downloadDirectory.equalsIgnoreCase(JDUtilities.getConfiguration().getDefaultDownloadDirectory())) return;
        getDownloadDirList().add(new String[] { downloadDirectory, packageName });
        SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME).save();
    }

    @SuppressWarnings("unchecked")
    private ArrayList<String[]> getDownloadDirList() {
        return ((ArrayList<String[]>) SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME).getProperty("DOWNLOADDIR_LIST", new ArrayList<String[]>()));
    }

    public void confirmPackage(LinkGrabberFilePackage fpv2, String host) {
        if (fpv2 == null) return;
        Vector<DownloadLink> linkList = fpv2.getDownloadLinks();
        if (linkList.isEmpty()) return;

        FilePackage fp = FilePackage.getInstance();
        fp.setName(fpv2.getName());
        fp.setComment(fpv2.getComment());
        fp.setPassword(fpv2.getPassword());
        fp.setExtractAfterDownload(fpv2.isExtractAfterDownload());
        addToDownloadDirs(fpv2.getDownloadDirectory(), fpv2.getName());

        if (fpv2.useSubDir()) {
            File file = new File(new File(fpv2.getDownloadDirectory()), fp.getName());
            if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CREATE_SUBFOLDER_BEFORE_DOWNLOAD, false)) {
                if (!file.exists()) file.mkdirs();
            }
            fp.setDownloadDirectory(file.getAbsolutePath());
        } else {
            fp.setDownloadDirectory(fpv2.getDownloadDirectory());
        }
        int files = 0;
        if (host == null) {
            files = linkList.size();
            fp.addAll(linkList);
            for (DownloadLink link : linkList) {
                boolean avail = true;
                if (link.isAvailabilityChecked()) avail = link.isAvailable();
                link.getLinkStatus().reset();
                if (!avail) link.getLinkStatus().addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                link.setFilePackage(fp);
            }
            fpv2.setDownloadLinks(new Vector<DownloadLink>());
        } else {
            Vector<DownloadLink> linkListHost = new Vector<DownloadLink>();
            for (int i = fpv2.getDownloadLinks().size() - 1; i >= 0; --i) {
                if (linkList.elementAt(i).getHost().compareTo(host) == 0) {
                    DownloadLink link = linkList.remove(i);
                    boolean avail = true;
                    if (link.isAvailabilityChecked()) avail = link.isAvailable();
                    link.getLinkStatus().reset();
                    if (!avail) link.getLinkStatus().addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                    linkListHost.add(link);
                    link.setFilePackage(fp);
                    ++files;
                }
            }
            if (files == 0) return;
            fp.addAll(linkListHost);
            fpv2.setDownloadLinks(linkList);
        }
        if (!fpv2.isIgnored()) {
            if (SimpleGuiConstants.GUI_CONFIG.getBooleanProperty(SimpleGuiConstants.PARAM_INSERT_NEW_LINKS_AT, false)) {
                JDUtilities.getDownloadController().addPackageAt(fp, 0);
            } else {
                JDUtilities.getDownloadController().addPackage(fp);
            }

        }
    }

    public void checkAlreadyinList(DownloadLink link) {
        if (JDUtilities.getDownloadController().hasDownloadLinkwithURL(link.getDownloadURL())) {
            link.getLinkStatus().setErrorMessage(JDLocale.L("gui.linkgrabber.alreadyindl", "Already on Download List"));
            link.getLinkStatus().addStatus(LinkStatus.ERROR_ALREADYEXISTS);
        }
    }

    @SuppressWarnings("unchecked")
    public void onLinkCheckEvent(LinkCheckEvent event) {
        switch (event.getID()) {
        case LinkCheckEvent.AFTER_CHECK:
            if (event.getParameter() instanceof Vector) {
                afterLinkGrabber((Vector<DownloadLink>) event.getParameter());
            } else if (event.getParameter() instanceof DownloadLink) {
                Vector<DownloadLink> links = new Vector<DownloadLink>();
                links.add((DownloadLink) event.getParameter());
                afterLinkGrabber(links);
            }
            break;
        case LinkCheckEvent.ABORT:
            stopLinkGatherer();
            break;
        }
    }

    public void onProgressControllerEvent(ProgressControllerEvent event) {
        if (event.getSource() == this.pc) {
            lc.abortLinkCheck();
            this.stopLinkGatherer();
            return;
        }
    }

    public boolean hasLinks() {
        return waitingList.size() > 0 || LGINSTANCE.size() > 0 || LGINSTANCE.getFILTERPACKAGE().size() > 0;
    }

    public void onLinkGrabberControllerEvent(LinkGrabberControllerEvent event) {
        switch (event.getID()) {
        case LinkGrabberControllerEvent.REMOVE_FILPACKAGE:
            if (filePackageInfo.getPackage() != null && filePackageInfo.getPackage() == ((LinkGrabberFilePackage) event.getParameter())) {
                this.hideFilePackageInfo();
            }
        case LinkGrabberControllerEvent.REFRESH_STRUCTURE:
            if (event.getParameter() != null) {
                if (filePackageInfo.getPackage() != null && filePackageInfo.getPackage() == event.getParameter()) {
                    filePackageInfo.update();
                }
            }
            Update_Async.restart();
            break;
        default:
            break;
        }
    }
}
