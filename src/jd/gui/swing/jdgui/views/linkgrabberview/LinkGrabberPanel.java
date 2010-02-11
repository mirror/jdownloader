//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.views.linkgrabberview;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.ClipboardHandler;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDLogger;
import jd.controlling.LinkCheck;
import jd.controlling.LinkCheckEvent;
import jd.controlling.LinkCheckListener;
import jd.controlling.LinkGrabberController;
import jd.controlling.LinkGrabberControllerEvent;
import jd.controlling.LinkGrabberControllerListener;
import jd.controlling.ProgressController;
import jd.controlling.ProgressControllerEvent;
import jd.controlling.ProgressControllerListener;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.SwingGui;
import jd.gui.swing.components.Balloon;
import jd.gui.swing.components.JDFileChooser;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.gui.swing.jdgui.actions.ThreadedAction;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.LinkgrabberView;
import jd.gui.swing.jdgui.views.downloadview.DownloadTable;
import jd.gui.swing.jdgui.views.toolbar.ViewToolbar;
import jd.nutils.JDFlags;
import jd.nutils.io.JDFileFilter;
import jd.nutils.io.JDIO;
import jd.nutils.jobber.Jobber;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkGrabberFilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class LinkGrabberPanel extends SwitchPanel implements ActionListener, LinkCheckListener, ProgressControllerListener, LinkGrabberControllerListener {

    private static final long serialVersionUID = 1607433619381447389L;

    public static final String JDL_PREFIX = "jd.gui.swing.jdgui.views.linkgrabberview";

    private ArrayList<DownloadLink> waitingList = new ArrayList<DownloadLink>();

    private LinkGrabberTable internalTable;

    protected Logger logger = jd.controlling.JDLogger.getLogger();

    private transient Thread gatherer;
    private boolean gatherer_running = false;
    private ProgressController pc;

    private LinkGrabberFilePackageInfo filePackageInfo;
    private Timer gathertimer;

    private Jobber checkJobbers = new Jobber(4);

    private LinkCheck lc = LinkCheck.getLinkChecker();
    private Timer Update_Async;
    private static LinkGrabberPanel INSTANCE;

    private LinkGrabberController LGINSTANCE = null;

    protected boolean tablerefreshinprogress = false;
    protected boolean addinginprogress = false;

    private JScrollPane scrollPane;

    private ViewToolbar toolbar;

    private boolean notvisible = true;

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

    public boolean isNotVisible() {
        return notvisible;
    }

    private LinkGrabberPanel() {
        super();
        initActions();
        internalTable = new LinkGrabberTable(this);
        scrollPane = new JScrollPane(internalTable);
        toolbar = new LinkGrabberToolbar();
        filePackageInfo = new LinkGrabberFilePackageInfo();
        Update_Async = new Timer(250, this);
        Update_Async.setInitialDelay(250);
        Update_Async.setRepeats(false);
        gathertimer = new Timer(2000, LinkGrabberPanel.this);
        gathertimer.setInitialDelay(2000);
        gathertimer.setRepeats(false);
        INSTANCE = this;
        LGINSTANCE = LinkGrabberController.getInstance();
        LGINSTANCE.addListener(this);

        if (SubConfiguration.getConfig(LinkGrabberController.CONFIG).getBooleanProperty(LinkGrabberController.PARAM_CONTROLPOSITION, false)) {
            this.setLayout(new MigLayout("ins 0, wrap 1", "[fill,grow]", "[][fill,grow]"));
            this.add(toolbar, "gapleft 3, gaptop 3");
            this.add(scrollPane, "grow");
        } else {
            this.setLayout(new MigLayout("ins 0, wrap 1", "[fill,grow]", "[fill,grow][]"));
            this.add(scrollPane, "grow");
            this.add(toolbar, "gapleft 3, gapbottom 3");
        }
    }

    public void initActions() {
        new ThreadedAction("action.linkgrabber.clearlist", "gui.images.clear") {

            private static final long serialVersionUID = -4407938288408350792L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void init() {
            }

            @Override
            public void threadedActionPerformed(ActionEvent e) {
                if (LGINSTANCE.getPackages().isEmpty() && LGINSTANCE.getFilterPackage().isEmpty()) return;
                if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.NO_COUNTDOWN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, JDL.L("gui.linkgrabberv2.lg.clear.ask", "Clear linkgrabber list?")), UserIO.RETURN_OK)) {
                    synchronized (LinkGrabberController.ControllerLock) {
                        synchronized (LGINSTANCE.getPackages()) {
                            stopLinkGatherer();
                            lc.abortLinkCheck();
                            LGINSTANCE.getFilterPackage().clear();
                            ArrayList<LinkGrabberFilePackage> selected_packages = new ArrayList<LinkGrabberFilePackage>(LGINSTANCE.getPackages());
                            selected_packages.add(LGINSTANCE.getFilterPackage());
                            for (LinkGrabberFilePackage fp2 : selected_packages) {
                                fp2.setDownloadLinks(new ArrayList<DownloadLink>());
                            }
                        }
                    }
                }
            }
        };
        new ThreadedAction("action.linkgrabber.addall", "gui.images.add_all") {

            private static final long serialVersionUID = 6181260839200699153L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void init() {
            }

            @Override
            public void threadedActionPerformed(ActionEvent e) {
                synchronized (LinkGrabberController.ControllerLock) {
                    synchronized (LGINSTANCE.getPackages()) {
                        LGINSTANCE.getFilterPackage().clear();
                        ArrayList<LinkGrabberFilePackage> fps = new ArrayList<LinkGrabberFilePackage>(LGINSTANCE.getPackages());
                        confirmPackages(fps);
                    }
                }
            }
        };

    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public void showFilePackageInfo(LinkGrabberFilePackage fp) {
        filePackageInfo.setPackage(fp);
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                LinkgrabberView.getInstance().setInfoPanel(filePackageInfo);
                return null;
            }
        }.start();
    }

    public void hideFilePackageInfo() {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                LinkgrabberView.getInstance().setInfoPanel(null);
                return null;
            }
        }.start();
    }

    public void fireTableChanged() {
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (LGINSTANCE.getPackages()) {
                if (gatherer_running) {
                    ArrayList<LinkGrabberFilePackage> fps = LGINSTANCE.getPackages();
                    int count = 0;
                    for (LinkGrabberFilePackage fp : fps) {
                        count += 1 + fp.size();
                    }
                    if (count > (internalTable.getVisibleRect().getHeight() / 16.0)) {
                        for (LinkGrabberFilePackage fp : fps) {
                            if (!fp.getBooleanProperty(LinkGrabberTable.PROPERTY_USEREXPAND, false)) fp.setProperty(LinkGrabberTable.PROPERTY_EXPANDED, false);
                        }
                    } else {
                        for (LinkGrabberFilePackage fp : fps) {
                            if (!fp.getBooleanProperty(LinkGrabberTable.PROPERTY_USEREXPAND, false)) fp.setProperty(LinkGrabberTable.PROPERTY_EXPANDED, true);
                        }
                    }
                }
                try {
                    internalTable.fireTableChanged();
                } catch (Exception e) {
                    logger.severe("TreeTable Exception, complete refresh!");
                    Update_Async.restart();
                }
            }
        }
    }

    @Override
    public void onHide() {
        notvisible = true;
        LGINSTANCE.removeListener(this);
        Update_Async.stop();
    }

    public void move(byte mode) {
        ArrayList<LinkGrabberFilePackage> fps = internalTable.getSelectedFilePackages();
        ArrayList<DownloadLink> links = internalTable.getSelectedDownloadLinks();
        if (fps.size() > 0) LinkGrabberController.getInstance().move(fps, null, mode);
        if (links.size() > 0) LinkGrabberController.getInstance().move(links, null, mode);
    }

    public void addLinks(final ArrayList<DownloadLink> linkList) {
        addinginprogress = true;
        new Thread() {
            @Override
            public void run() {
                Balloon.showIfHidden(JDL.L("gui.config.gui.linkgrabber", "LinkGrabber"), JDTheme.II("gui.images.add", 32, 32), JDL.LF("gui.linkgrabber.adding", "Adding %s link(s) to LinkGrabber", "" + linkList.size()));
                for (DownloadLink element : linkList) {
                    if (LGINSTANCE.isDupe(element)) continue;
                    addToWaitingList(element);
                }
                Update_Async.restart();
                gathertimer.restart();
                addinginprogress = false;
            }
        }.start();
    }

    public boolean isAddinginProgress() {
        return addinginprogress;
    }

    public void addToWaitingList(DownloadLink element) {
        synchronized (waitingList) {
            waitingList.add(element);
        }
        checkAlreadyinList(element);
        LGINSTANCE.attachToPackagesFirstStage(element);
    }

    public void recheckLinks(final ArrayList<DownloadLink> links) {
        new Thread() {
            @Override
            public void run() {
                synchronized (waitingList) {
                    /* remove links from waitinglist, so we can check again */
                    waitingList.removeAll(links);
                }
                /* remove from waittinglist of linkchecker */
                lc.removefromWaitingList(links);
                synchronized (LinkGrabberController.ControllerLock) {
                    synchronized (LGINSTANCE.getPackages()) {
                        for (DownloadLink link : links) {
                            LinkGrabberFilePackage fp = LGINSTANCE.getFPwithLink(link);
                            if (fp != null) {
                                /*
                                 * remove link from linkgrabbercontroller and
                                 * reset availablestatus
                                 */
                                fp.remove(link);
                                link.setAvailableStatus(DownloadLink.AvailableStatus.UNCHECKED);
                            }
                            link.setProperty("forcecheck", 1);
                        }
                    }
                }
                addLinks(links);
            }
        }.start();
    }

    private void stopLinkGatherer() {
        lc.getBroadcaster().removeListener(this);
        if (gatherer != null && gatherer.isAlive()) {
            gatherer_running = false;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    pc.setStatusText(pc.getStatusText() + ": " + JDL.L("gui.linkgrabber.aborted", "Aborted"));
                    pc.doFinalize(5000l);
                }
            });
            checkJobbers.stop();
            gatherer.interrupt();
        }
    }

    public ArrayList<String> getExtensions() {
        ArrayList<String> extensions = new ArrayList<String>();
        String ext = null;
        ArrayList<LinkGrabberFilePackage> fps = new ArrayList<LinkGrabberFilePackage>(LGINSTANCE.getPackages());
        fps.add(LGINSTANCE.getFilterPackage());
        for (LinkGrabberFilePackage fp : fps) {
            for (DownloadLink l : new ArrayList<DownloadLink>(fp.getDownloadLinks())) {
                ext = JDIO.getFileExtension(l.getName());
                if (ext != null && ext.trim().length() > 1) {
                    if (!extensions.contains(ext.trim())) extensions.add(ext.trim());
                }
            }
        }
        Collections.sort(extensions);
        return extensions;
    }

    private void startLinkGatherer() {
        if (gatherer != null && gatherer.isAlive()) { return; }
        gatherer = new Thread() {
            @Override
            public void run() {
                setName("LinkGrabber");
                gatherer_running = true;
                pc = new ProgressController(JDL.L("gui.linkgrabber.pc.linkgrabber", "LinkGrabber operations pending..."), null);
                pc.getBroadcaster().addListener(INSTANCE);
                lc.getBroadcaster().addListener(INSTANCE);
                pc.setRange(0);
                while (waitingList.size() > 0 || lc.isRunning()) {
                    ArrayList<DownloadLink> currentList = new ArrayList<DownloadLink>();
                    synchronized (waitingList) {
                        currentList = new ArrayList<DownloadLink>(waitingList);
                        pc.addToMax(currentList.size());
                        waitingList.removeAll(currentList);
                    }
                    if (!LGINSTANCE.isLinkCheckEnabled()) {
                        /* kein online check, kein multithreaded nötig */
                        ArrayList<DownloadLink> currentList2 = new ArrayList<DownloadLink>();
                        /* check for forced checks */
                        for (DownloadLink link : currentList) {
                            if (link.getIntegerProperty("forcecheck", 0) == 1) {
                                currentList2.add(link);
                            }
                        }
                        if (currentList2.size() > 0) {
                            /* we have forced linkchecks */
                            currentList.clear();
                            lc.checkLinks(currentList2, false);
                        }
                        if (currentList.size() > 0) afterLinkGrabber(currentList);
                    } else {
                        lc.checkLinks(currentList, false);
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                lc.getBroadcaster().removeListener(INSTANCE);
                pc.doFinalize();
                pc.getBroadcaster().removeListener(INSTANCE);
                LGINSTANCE.postprocessing();
                LGINSTANCE.throwFinished();
                gatherer_running = false;
                ArrayList<LinkGrabberFilePackage> fps = new ArrayList<LinkGrabberFilePackage>(LGINSTANCE.getPackages());
                int links = 0;
                for (LinkGrabberFilePackage fp : fps) {
                    links += fp.getDownloadLinks().size();
                }
                Balloon.showIfHidden(JDL.L("gui.config.gui.linkgrabber", "LinkGrabber"), JDTheme.II("gui.images.add", 32, 32), JDL.LF("gui.linkgrabber.finished", "Grabbed %s link(s) in %s Package(s)", "" + links, "" + fps.size()));
                fps = null;
            }
        };
        gatherer.start();
    }

    private void afterLinkGrabber(ArrayList<DownloadLink> links) {
        for (DownloadLink link : links) {
            if (!gatherer_running) break;
            if (!link.getBooleanProperty("removed", false)) LGINSTANCE.attachToPackagesSecondStage(link);
        }
        pc.increase(links.size());
        Update_Async.restart();
    }

    @Override
    public void onShow() {
        notvisible = false;
        LGINSTANCE.addListener(this);
        fireTableChanged();
    }

    @SuppressWarnings("unchecked")
    public void actionPerformed(final ActionEvent arg0) {
        new Thread() {
            @Override
            public void run() {
                this.setName("LinkGrabberPanel: actionPerformed");
                if (arg0.getSource() == INSTANCE.Update_Async) {
                    fireTableChanged();
                    return;
                }

                if (arg0.getSource() == INSTANCE.gathertimer) {
                    gathertimer.stop();
                    if (waitingList.size() > 0) {
                        startLinkGatherer();
                    }
                    return;
                }
                ArrayList<LinkGrabberFilePackage> selected_packages = new ArrayList<LinkGrabberFilePackage>();
                ArrayList<DownloadLink> selected_links = new ArrayList<DownloadLink>();
                DownloadLink link = null;
                int prio = 0;
                String pw = "";
                HashMap<String, Object> prop = new HashMap<String, Object>();
                LinkGrabberFilePackage fp;
                String ext = null;
                Set<String> hoster = null;
                String name = null;
                boolean b = false;
                HashSet<String> List = new HashSet<String>();
                StringBuilder build = new StringBuilder();
                String string = null;
                synchronized (LinkGrabberController.ControllerLock) {
                    synchronized (LGINSTANCE.getPackages()) {
                        ArrayList<LinkGrabberFilePackage> fps = LGINSTANCE.getPackages();
                        if (arg0.getSource() instanceof JMenuItem) {
                            switch (arg0.getID()) {
                            case LinkGrabberTableAction.SELECT_HOSTER:
                                hoster = (Set<String>) ((LinkGrabberTableAction) ((JMenuItem) arg0.getSource()).getAction()).getProperty().getProperty("hoster");
                                selected_packages = new ArrayList<LinkGrabberFilePackage>(fps);
                                selected_packages.add(LGINSTANCE.getFilterPackage());
                                break;
                            case LinkGrabberTableAction.DELETE_OFFLINE:
                            case LinkGrabberTableAction.DELETE_DUPS:
                                selected_packages = new ArrayList<LinkGrabberFilePackage>(fps);
                                selected_packages.add(LGINSTANCE.getFilterPackage());
                                break;
                            case LinkGrabberTableAction.ADD_SELECTED_PACKAGES:
                            case LinkGrabberTableAction.EDIT_DIR:
                            case LinkGrabberTableAction.SPLIT_HOSTER:
                                selected_packages = new ArrayList<LinkGrabberFilePackage>(INSTANCE.internalTable.getSelectedFilePackages());
                                break;
                            case LinkGrabberTableAction.DOWNLOAD_PRIO:
                            case LinkGrabberTableAction.DE_ACTIVATE:
                                prop = (HashMap<String, Object>) ((LinkGrabberTableAction) ((JMenuItem) arg0.getSource()).getAction()).getProperty().getProperty("infos");
                                selected_links = (ArrayList<DownloadLink>) prop.get("links");
                                break;
                            case LinkGrabberTableAction.DELETE:
                            case LinkGrabberTableAction.SET_PW:
                            case LinkGrabberTableAction.NEW_PACKAGE:
                            case LinkGrabberTableAction.MERGE_PACKAGE:
                            case LinkGrabberTableAction.SAVE_DLC:
                            case LinkGrabberTableAction.ADD_SELECTED_LINKS:
                            case LinkGrabberTableAction.COPY_LINK:
                            case LinkGrabberTableAction.CHECK_LINK:
                                selected_links = (ArrayList<DownloadLink>) ((LinkGrabberTableAction) ((JMenuItem) arg0.getSource()).getAction()).getProperty().getProperty("links");
                                break;
                            case LinkGrabberTableAction.BROWSE_LINK:
                                link = (DownloadLink) ((LinkGrabberTableAction) ((JMenuItem) arg0.getSource()).getAction()).getProperty().getProperty("link");
                                break;
                            case LinkGrabberTableAction.EXT_FILTER:
                                ext = (String) ((LinkGrabberTableAction) ((JMenuItem) arg0.getSource()).getAction()).getProperty().getProperty("extension");
                                b = ((JCheckBoxMenuItem) arg0.getSource()).isSelected();
                                break;
                            }
                        } else if (arg0.getSource() instanceof LinkGrabberTableAction) {
                            switch (arg0.getID()) {
                            case LinkGrabberTableAction.DELETE:
                                selected_links = (ArrayList<DownloadLink>) ((LinkGrabberTableAction) arg0.getSource()).getProperty().getProperty("links");
                                break;
                            }
                        }
                        switch (arg0.getID()) {
                        case LinkGrabberTableAction.CHECK_LINK:
                            recheckLinks(selected_links);
                            break;
                        case LinkGrabberTableAction.ADD_SELECTED_LINKS:
                            ArrayList<LinkGrabberFilePackage> selected_packages2 = new ArrayList<LinkGrabberFilePackage>();
                            while (selected_links.size() > 0) {
                                ArrayList<DownloadLink> links2 = new ArrayList<DownloadLink>(selected_links);
                                LinkGrabberFilePackage fp3 = LGINSTANCE.getFPwithLink(selected_links.get(0));
                                if (fp3 == null) {
                                    logger.warning("DownloadLink not controlled by LinkGrabberController!");
                                    selected_links.remove(selected_links.get(0));
                                    continue;
                                }
                                LinkGrabberFilePackage fp4 = new LinkGrabberFilePackage(fp3.getName());
                                fp4.setDownloadDirectory(fp3.getDownloadDirectory());
                                fp4.setPassword(fp3.getPassword());
                                fp4.setExtractAfterDownload(fp3.isExtractAfterDownload());
                                fp4.setUseSubDir(fp3.useSubDir());
                                fp4.setComment(fp3.getComment());
                                for (DownloadLink dl : links2) {
                                    if (LGINSTANCE.getFPwithLink(dl) != null && LGINSTANCE.getFPwithLink(dl) == fp3) {
                                        fp4.add(dl);
                                        selected_links.remove(dl);
                                    }
                                }
                                selected_packages2.add(fp4);
                            }
                            confirmPackages(selected_packages2);
                            break;
                        case LinkGrabberTableAction.SPLIT_HOSTER:
                            for (LinkGrabberFilePackage fp2 : selected_packages) {
                                synchronized (fp2) {
                                    ArrayList<DownloadLink> links2 = new ArrayList<DownloadLink>(fp2.getDownloadLinks());
                                    Set<String> hosts = DownloadLink.getHosterList(links2);
                                    for (String host : hosts) {
                                        LinkGrabberFilePackage fp3 = new LinkGrabberFilePackage(fp2.getName());
                                        fp3.setDownloadDirectory(fp2.getDownloadDirectory());
                                        fp3.setPassword(fp2.getPassword());
                                        fp3.setExtractAfterDownload(fp2.isExtractAfterDownload());
                                        fp3.setUseSubDir(fp2.useSubDir());
                                        fp3.setComment(fp2.getComment());
                                        for (DownloadLink dl : links2) {
                                            if (dl.getPlugin().getHost().equalsIgnoreCase(host)) {
                                                fp3.add(dl);
                                            }
                                        }
                                        LGINSTANCE.addPackage(fp3);
                                    }
                                }
                            }
                            break;
                        case LinkGrabberTableAction.DELETE_OFFLINE:
                            for (LinkGrabberFilePackage fp2 : selected_packages) {
                                fp2.removeOffline();
                            }
                            break;
                        case LinkGrabberTableAction.DELETE_DUPS:
                            for (LinkGrabberFilePackage fp2 : selected_packages) {
                                selected_links = fp2.getLinksListbyStatus(LinkStatus.ERROR_ALREADYEXISTS);
                                fp2.remove(selected_links);
                            }
                            break;
                        case LinkGrabberTableAction.SELECT_HOSTER:
                            for (LinkGrabberFilePackage fp2 : selected_packages) {
                                fp2.keepHostersOnly(hoster);
                            }
                            break;
                        case LinkGrabberTableAction.EDIT_DIR:
                            final ArrayList<LinkGrabberFilePackage> selected_packages3 = new ArrayList<LinkGrabberFilePackage>(selected_packages);
                            new GuiRunnable<Object>() {
                                @Override
                                public Object runSave() {
                                    JDFileChooser fc = new JDFileChooser();
                                    fc.setApproveButtonText(JDL.L("gui.btn_ok", "OK"));
                                    fc.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
                                    fc.setCurrentDirectory(new File(selected_packages3.get(0).getDownloadDirectory()));
                                    if (fc.showOpenDialog(INSTANCE) == JDFileChooser.APPROVE_OPTION) {
                                        if (fc.getSelectedFile() != null) {
                                            for (LinkGrabberFilePackage fp2 : selected_packages3) {
                                                fp2.setDownloadDirectory(fc.getSelectedFile().getAbsolutePath());
                                            }
                                        }
                                    }
                                    return null;
                                }
                            }.start();
                            break;
                        case LinkGrabberTableAction.MERGE_PACKAGE:
                            fp = LGINSTANCE.getFPwithLink(selected_links.get(0));
                            name = fp.getName();
                        case LinkGrabberTableAction.NEW_PACKAGE:
                            fp = LGINSTANCE.getFPwithLink(selected_links.get(0));
                            LinkGrabberFilePackage nfp;
                            if (name == null) name = UserIO.getInstance().requestInputDialog(0, JDL.L("gui.linklist.newpackage.message", "Name of the new package"), fp.getName());
                            if (name != null) {
                                nfp = new LinkGrabberFilePackage(name, LGINSTANCE);
                                nfp.setDownloadDirectory(fp.getDownloadDirectory());
                                nfp.setExtractAfterDownload(fp.isExtractAfterDownload());
                                nfp.setUseSubDir(fp.useSubDir());
                                nfp.setComment(fp.getComment());
                                for (DownloadLink dlink : selected_links) {
                                    fp = LGINSTANCE.getFPwithLink(dlink);
                                    if (fp != null) nfp.setPassword(fp.getPassword());
                                }
                                nfp.addAll(selected_links);
                                if (GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_INSERT_NEW_LINKS_AT, false)) {
                                    LGINSTANCE.addPackageAt(nfp, 0, 0);
                                } else {
                                    LGINSTANCE.addPackage(nfp);
                                }
                            }
                            break;
                        case LinkGrabberTableAction.COPY_LINK:
                            for (int i = 0; i < selected_links.size(); i++) {
                                if (selected_links.get(i).getLinkType() == DownloadLink.LINKTYPE_NORMAL) {
                                    String url = selected_links.get(i).getBrowserUrl();
                                    if (!List.contains(url)) {
                                        if (List.size() > 0) build.append("\r\n");
                                        List.add(url);
                                        build.append(url);
                                    }
                                }
                            }
                            string = build.toString();
                            ClipboardHandler.getClipboard().copyTextToClipboard(string);
                            break;
                        case LinkGrabberTableAction.BROWSE_LINK:
                            if (link.getLinkType() == DownloadLink.LINKTYPE_NORMAL) {
                                try {
                                    JLink.openURL(link.getBrowserUrl());
                                } catch (Exception e1) {
                                    JDLogger.exception(e1);
                                }
                            }
                            break;
                        case LinkGrabberTableAction.SAVE_DLC: {
                            GuiRunnable<File> temp = new GuiRunnable<File>() {
                                @Override
                                public File runSave() {
                                    JDFileChooser fc = new JDFileChooser("_LOADSAVEDLC");
                                    fc.setFileFilter(new JDFileFilter(null, ".dlc", true));
                                    if (fc.showSaveDialog(SwingGui.getInstance().getMainFrame()) == JDFileChooser.APPROVE_OPTION) return fc.getSelectedFile();
                                    return null;
                                }
                            };
                            File ret = temp.getReturnValue();
                            if (ret == null) return;
                            if (JDIO.getFileExtension(ret) == null || !JDIO.getFileExtension(ret).equalsIgnoreCase("dlc")) {
                                ret = new File(ret.getAbsolutePath() + ".dlc");
                            }
                            JDUtilities.getController().saveDLC(ret, selected_links);
                            break;
                        }
                        case LinkGrabberTableAction.SET_PW:
                            pw = UserIO.getInstance().requestInputDialog(0, JDL.L("gui.linklist.setpw.message", "Set download password"), null);
                            for (int i = 0; i < selected_links.size(); i++) {
                                selected_links.get(i).setProperty("pass", pw);
                            }
                            break;
                        case LinkGrabberTableAction.DE_ACTIVATE:
                            b = (Boolean) prop.get("boolean");
                            for (int i = 0; i < selected_links.size(); i++) {
                                selected_links.get(i).setEnabled(b);
                            }
                            Update_Async.restart();
                            break;
                        case LinkGrabberTableAction.ADD_SELECTED_PACKAGES:
                            confirmPackages(selected_packages);
                            break;
                        case LinkGrabberTableAction.DELETE:
                            if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, JDL.L("gui.downloadlist.delete", "Ausgewählte Links wirklich entfernen?") + " (" + JDL.LF("gui.downloadlist.delete.size_packagev2", "%s links", selected_links.size()) + ")"), UserIO.RETURN_OK, UserIO.RETURN_DONT_SHOW_AGAIN)) {
                                for (DownloadLink dlink : selected_links) {
                                    dlink.setProperty("removed", true);
                                    fp = LGINSTANCE.getFPwithLink(dlink);
                                    if (fp == null) continue;
                                    fp.remove(dlink);
                                }
                            }
                            break;
                        case LinkGrabberTableAction.DOWNLOAD_PRIO:
                            prio = (Integer) prop.get("prio");
                            for (int i = 0; i < selected_links.size(); i++) {
                                selected_links.get(i).setPriority(prio);
                            }
                            break;
                        case LinkGrabberTableAction.EXT_FILTER:
                            LGINSTANCE.filterExtension(ext, b);
                            break;
                        }
                    }
                }
            }
        }.start();
    }

    private void confirmPackages(ArrayList<LinkGrabberFilePackage> all) {
        if (all.size() == 0) return;
        for (int i = 0; i < all.size(); i++) {
            confirmPackage(all.get(i), null, i);
        }
        LGINSTANCE.throwLinksAdded();
        if (GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_START_AFTER_ADDING_LINKS, true)) {
            DownloadWatchDog.getInstance().startDownloads();
        }
    }

    private void addToDownloadDirs(String downloadDirectory, String packageName) {
        if (packageName.length() < 5 || downloadDirectory.equalsIgnoreCase(JDUtilities.getDefaultDownloadDirectory())) return;
        getDownloadDirList().add(new String[] { downloadDirectory, packageName });
        GUIUtils.getConfig().save();
    }

    private ArrayList<String[]> getDownloadDirList() {
        return GUIUtils.getConfig().getGenericProperty("DOWNLOADDIR_LIST", new ArrayList<String[]>());
    }

    public void confirmPackage(LinkGrabberFilePackage fpv2, String host, int index) {
        if (fpv2 == null) return;
        if (filePackageInfo.getPackage() != null && filePackageInfo.getPackage() == fpv2) {
            filePackageInfo.onHideSave();
        }
        ArrayList<DownloadLink> linkList = fpv2.getDownloadLinks();
        if (linkList.isEmpty()) return;
        for (DownloadLink link : linkList) {
            /* remove forcecheck flag */
            link.setProperty("forcecheck", Property.NULL);
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setProperty(DownloadTable.PROPERTY_EXPANDED, fpv2.getBooleanProperty(LinkGrabberTable.PROPERTY_EXPANDED, false));
        fp.setName(fpv2.getName());
        fp.setComment(fpv2.getComment());
        fp.setPassword(fpv2.getPassword());
        fp.setExtractAfterDownload(fpv2.isExtractAfterDownload());
        addToDownloadDirs(fpv2.getDownloadDirectory(), fpv2.getName());

        fp.setDownloadDirectory(fpv2.getDownloadDirectory());
        if (fpv2.useSubDir()) {
            File file = new File(new File(fpv2.getDownloadDirectory()), fp.getName());
            fp.setDownloadDirectory(file.getAbsolutePath());
            if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CREATE_SUBFOLDER_BEFORE_DOWNLOAD, false)) {
                if (!file.exists()) {
                    if (!file.mkdirs()) {
                        logger.severe("could not create " + file.toString());
                        fp.setDownloadDirectory(fpv2.getDownloadDirectory());
                    }
                }
            }
        }
        if (host == null) {
            fp.addLinks(linkList);
            for (DownloadLink link : linkList) {
                boolean avail = true;
                if (link.isAvailabilityStatusChecked()) avail = link.isAvailable();
                link.getLinkStatus().reset();
                if (!avail) link.getLinkStatus().addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                link.setFilePackage(fp);
            }
            fpv2.setDownloadLinks(new ArrayList<DownloadLink>());
        } else {
            int files = 0;
            ArrayList<DownloadLink> linkListHost = new ArrayList<DownloadLink>();
            for (int i = fpv2.getDownloadLinks().size() - 1; i >= 0; --i) {
                if (linkList.get(i).getHost().compareTo(host) == 0) {
                    DownloadLink link = linkList.remove(i);
                    boolean avail = true;
                    if (link.isAvailabilityStatusChecked()) avail = link.isAvailable();
                    link.getLinkStatus().reset();
                    if (!avail) link.getLinkStatus().addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                    linkListHost.add(link);
                    link.setFilePackage(fp);
                    ++files;
                }
            }
            if (files == 0) return;
            fp.addLinks(linkListHost);
            fpv2.setDownloadLinks(linkList);
        }
        /* set same add date to package and files */
        long curtime = System.currentTimeMillis();
        fp.setCreated(curtime);
        for (DownloadLink link : fp.getDownloadLinkList()) {
            link.setCreated(curtime);
        }
        if (!fpv2.isIgnored()) {
            if (GUIUtils.getConfig() != null && GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_INSERT_NEW_LINKS_AT, false)) {
                JDUtilities.getDownloadController().addPackageAt(fp, index, 0);
            } else {
                JDUtilities.getDownloadController().addPackage(fp);
            }
        }
    }

    public void checkAlreadyinList(DownloadLink link) {
        if (JDUtilities.getDownloadController().hasDownloadLinkwithURL(link.getDownloadURL())) {
            link.getLinkStatus().setErrorMessage(JDL.L("gui.linkgrabber.alreadyindl", "Already on Download List"));
            link.getLinkStatus().addStatus(LinkStatus.ERROR_ALREADYEXISTS);
        }
    }

    @SuppressWarnings("unchecked")
    public void onLinkCheckEvent(LinkCheckEvent event) {
        switch (event.getID()) {
        case LinkCheckEvent.AFTER_CHECK:
            if (event.getParameter() instanceof ArrayList) {
                afterLinkGrabber((ArrayList<DownloadLink>) event.getParameter());
            } else if (event.getParameter() instanceof DownloadLink) {
                ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
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
        return this.addinginprogress || waitingList.size() > 0 || LGINSTANCE.size() > 0 || LGINSTANCE.getFilterPackage().size() > 0;
    }

    public void onLinkGrabberControllerEvent(LinkGrabberControllerEvent event) {
        switch (event.getID()) {
        case LinkGrabberControllerEvent.ADD_FILEPACKAGE:
            if (SubConfiguration.getConfig(LinkGrabberController.CONFIG).getBooleanProperty(LinkGrabberController.PARAM_INFOPANEL_ONLINKGRAB)) {
                showFilePackageInfo((LinkGrabberFilePackage) event.getParameter());
            }
            Update_Async.restart();
            break;
        case LinkGrabberControllerEvent.REMOVE_FILEPACKAGE:
            if (filePackageInfo.getPackage() != null && filePackageInfo.getPackage() == ((LinkGrabberFilePackage) event.getParameter())) {
                if (!LGINSTANCE.getPackages().isEmpty()) {
                    showFilePackageInfo(LGINSTANCE.getPackages().get(0));
                } else {
                    hideFilePackageInfo();
                }
            }
            Update_Async.restart();
            break;
        case LinkGrabberControllerEvent.REFRESH_STRUCTURE:
            Update_Async.restart();
            break;
        default:
            break;
        }
    }

    public boolean isFilePackageInfoVisible(Object obj) {
        boolean visible = LinkgrabberView.getInstance().getInfoPanel() == filePackageInfo;
        if (obj != null) {
            if (obj instanceof LinkGrabberFilePackage && filePackageInfo.getPackage() == obj && visible) return true;
            return false;
        }
        return visible;
    }

}
