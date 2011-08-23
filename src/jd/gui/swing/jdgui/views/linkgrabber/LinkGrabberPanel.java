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

package jd.gui.swing.jdgui.views.linkgrabber;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.Timer;

import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.DownloadController;
import jd.controlling.DownloadWatchDog;
import jd.controlling.GarbageController;
import jd.controlling.IOEQ;
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
import jd.gui.swing.components.Balloon;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.ViewToolbar;
import jd.nutils.JDFlags;
import jd.nutils.io.JDIO;
import jd.nutils.jobber.Jobber;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkGrabberFilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;

public class LinkGrabberPanel extends SwitchPanel implements ActionListener, LinkCheckListener, ProgressControllerListener, LinkGrabberControllerListener {

    private static final long          serialVersionUID       = 1607433619381447389L;

    public static final String         JDL_PREFIX             = "jd.gui.swing.jdgui.views.linkgrabberview";

    private ArrayList<DownloadLink>    waitingList            = new ArrayList<DownloadLink>();

    private LinkGrabberTable           internalTable;

    protected Logger                   logger                 = jd.controlling.JDLogger.getLogger();

    private transient Thread           gatherer;
    private boolean                    gatherer_running       = false;
    private ProgressController         pc;

    private LinkGrabberFilePackageInfo filePackageInfo;
    private Timer                      gathertimer;

    private Jobber                     checkJobbers           = new Jobber(4);

    private LinkCheck                  lc                     = LinkCheck.getLinkChecker();
    private Timer                      updateAsync;
    private static LinkGrabberPanel    INSTANCE;

    private LinkGrabberController      LGINSTANCE             = null;

    protected boolean                  tablerefreshinprogress = false;
    protected boolean                  addinginprogress       = false;

    private JScrollPane                scrollPane;

    private ViewToolbar                toolbar;

    private boolean                    notvisible             = true;

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
        updateAsync = new Timer(250, this);
        updateAsync.setInitialDelay(250);
        updateAsync.setRepeats(false);
        gathertimer = new Timer(2000, LinkGrabberPanel.this);
        gathertimer.setInitialDelay(2000);
        gathertimer.setRepeats(false);
        INSTANCE = this;
        LGINSTANCE = LinkGrabberController.getInstance();
        LGINSTANCE.addListener(this);
        /* to init the DownloadAutostart Listener */
        DownloadAutostart.getInstance();

        if (SubConfiguration.getConfig(LinkGrabberController.CONFIG).getBooleanProperty(LinkGrabberController.PARAM_CONTROLPOSITION, true)) {
            this.setLayout(new MigLayout("ins 0, wrap 1", "[fill,grow]", "[][fill,grow]"));
            this.add(toolbar, "gaptop 3");
            this.add(scrollPane, "grow");
        } else {
            this.setLayout(new MigLayout("ins 0, wrap 1", "[fill,grow]", "[fill,grow][]"));
            this.add(scrollPane, "grow");
            this.add(toolbar, "gapbottom 3");
        }
    }

    public void initActions() {
        new ToolBarAction(_GUI._.action_clearlinkgrabber(), "action.linkgrabber.clearlist", "clear") {

            private static final long serialVersionUID = -4407938288408350792L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void onAction(ActionEvent e) {
                IOEQ.add(new Runnable() {

                    public void run() {
                        if (LGINSTANCE.getPackages().isEmpty() && LGINSTANCE.getFilterPackage().isEmpty()) return;
                        if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.NO_COUNTDOWN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.gui_linkgrabberv2_lg_clear_ask()), UserIO.RETURN_OK)) {
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

                                    new GuiRunnable<Object>() {
                                        @Override
                                        public Object runSave() {
                                            LinkgrabberView.getInstance().setInfoPanel(null);
                                            return null;
                                        }
                                    }.start();
                                }
                            }
                        }
                    }
                });

            }

            @Override
            protected String createMnemonic() {
                return _GUI._.action_clearlinkgrabber_mnemonic();
            }

            @Override
            protected String createAccelerator() {
                return _GUI._.action_clearlinkgrabber_accelerator();
            }

            @Override
            protected String createTooltip() {
                return _GUI._.action_clearlinkgrabber_tooltip();
            }
        };
        new ToolBarAction(_GUI._.action_linkgrabber_addall(), "action.linkgrabber.addall", "download") {

            private static final long serialVersionUID = 6181260839200699153L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void onAction(ActionEvent e) {
                IOEQ.add(new Runnable() {

                    public void run() {
                        ArrayList<LinkGrabberFilePackage> fps = null;
                        synchronized (LinkGrabberController.ControllerLock) {
                            synchronized (LGINSTANCE.getPackages()) {
                                LGINSTANCE.getFilterPackage().clear();
                                fps = new ArrayList<LinkGrabberFilePackage>(LGINSTANCE.getPackages());
                            }
                        }
                        confirmPackages(fps);
                    }
                });
            }

            @Override
            protected String createMnemonic() {
                return _GUI._.action_linkgrabber_addall_mnemonic();
            }

            @Override
            protected String createAccelerator() {
                return _GUI._.action_linkgrabber_addall_accelerator();
            }

            @Override
            protected String createTooltip() {
                return _GUI._.action_linkgrabber_addall_tooltip();
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
                    if (SubConfiguration.getConfig(LinkGrabberController.CONFIG).getIntegerProperty(LinkGrabberController.PARAM_NEWPACKAGES, 2) == 1) {
                        ArrayList<LinkGrabberFilePackage> fps = LGINSTANCE.getPackages();
                        int count = 0;
                        for (LinkGrabberFilePackage fp : fps) {
                            count += 1 + fp.size();
                        }
                        if (count > (internalTable.getVisibleRect().getHeight() / 16.0)) {
                            for (LinkGrabberFilePackage fp : fps) {
                                if (!fp.getBooleanProperty(LinkGrabberController.PROPERTY_USEREXPAND, false)) fp.setProperty(LinkGrabberController.PROPERTY_EXPANDED, false);
                            }
                        } else {
                            for (LinkGrabberFilePackage fp : fps) {
                                if (!fp.getBooleanProperty(LinkGrabberController.PROPERTY_USEREXPAND, false)) fp.setProperty(LinkGrabberController.PROPERTY_EXPANDED, true);
                            }
                        }
                    }
                }
                try {
                    internalTable.fireTableChanged();
                } catch (Exception e) {
                    logger.severe("TreeTable Exception, complete refresh!");
                    updateAsync.restart();
                }
            }
        }
    }

    @Override
    public void onHide() {
        notvisible = true;
        LGINSTANCE.removeListener(this);
        updateAsync.stop();
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
                Balloon.showIfHidden(_GUI._.gui_config_gui_linkgrabber(), NewTheme.I().getIcon("add", 32), _GUI._.gui_linkgrabber_adding("" + linkList.size()));
                for (DownloadLink element : linkList) {
                    if (LGINSTANCE.isDupe(element)) continue;
                    addToWaitingList(element);
                }
                updateAsync.restart();
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
                    pc.setStatusText(pc.getStatusText() + ": " + _GUI._.gui_linkgrabber_aborted());
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
                pc = new ProgressController(_GUI._.gui_linkgrabber_pc_linkgrabber(), null);
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
                Balloon.showIfHidden(_GUI._.gui_config_gui_linkgrabber(), NewTheme.I().getIcon("add", 32), _GUI._.gui_linkgrabber_finished("" + links, "" + fps.size()));
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
        updateAsync.restart();
    }

    @Override
    public void onShow() {
        notvisible = false;
        LGINSTANCE.addListener(this);
        fireTableChanged();
    }

    public void actionPerformed(final ActionEvent e) {
        new Thread("LinkGrabberPanel: actionPerformed") {
            @Override
            public void run() {
                if (e.getSource() == updateAsync) {
                    fireTableChanged();
                } else if (e.getSource() == gathertimer) {
                    gathertimer.stop();
                    if (waitingList.size() > 0) {
                        startLinkGatherer();
                    }
                }
            }
        }.start();
    }

    public void confirmPackages(ArrayList<LinkGrabberFilePackage> all) {
        if (all.size() == 0) return;
        for (int i = 0; i < all.size(); i++) {
            confirmPackage(all.get(i), null, i);
        }
        LGINSTANCE.throwLinksAdded();
        if (JsonConfig.create(GeneralSettings.class).isAutoDownloadStartAfterAddingEnabled()) {
            DownloadWatchDog.getInstance().startDownloads();
        }
    }

    private void addToDownloadDirs(String downloadDirectory, String packageName) {
        if (packageName.length() < 5 || downloadDirectory.equalsIgnoreCase(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder())) return;
        GeneralSettings storage = JsonConfig.create(GeneralSettings.class);
        ArrayList<String[]> history = storage.getDownloadFolderHistory();

        history.add(new String[] { downloadDirectory, packageName });
        storage.setDownloadFolderHistory(history);
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
        fp.setExpanded(fpv2.getBooleanProperty(LinkGrabberController.PROPERTY_EXPANDED, false));
        fp.setName(fpv2.getName());
        fp.setComment(fpv2.getComment());
        fp.setPassword(fpv2.getPassword());
        fp.setPostProcessing(fpv2.isPostProcessing());
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
                fp.add(link);
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
                    fp.add(link);
                    ++files;
                }
            }
            if (files == 0) return;
            fp.addLinks(linkListHost);
            fpv2.setDownloadLinks(linkList);
        }
        /* set same add date to package and files */
        fp.setCreated(fpv2.getCreated());
        if (!fpv2.isIgnored()) {
            if (JsonConfig.create(GeneralSettings.class).isAddNewLinksOnTop()) {
                DownloadController.getInstance().addmovePackageAt(fp, index);
            } else {
                DownloadController.getInstance().addmovePackageAt(fp, -1);
            }
        }
        GarbageController.requestGC();
    }

    public void checkAlreadyinList(DownloadLink link) {
        if (JDUtilities.getDownloadController().hasDownloadLinkwithURL(link.getDownloadURL())) {
            link.getLinkStatus().setErrorMessage(_GUI._.gui_linkgrabber_alreadyindl());
            link.getLinkStatus().addStatus(LinkStatus.ERROR_ALREADYEXISTS);
        }
    }

    @SuppressWarnings("unchecked")
    public void onLinkCheckEvent(LinkCheckEvent event) {
        switch (event.getEventID()) {
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
        if (event.getCaller() == this.pc) {
            lc.abortLinkCheck();
            this.stopLinkGatherer();
            return;
        }
    }

    public boolean hasLinks() {
        return this.addinginprogress || waitingList.size() > 0 || LGINSTANCE.size() > 0 || LGINSTANCE.getFilterPackage().size() > 0;
    }

    public void onLinkGrabberControllerEvent(LinkGrabberControllerEvent event) {
        switch (event.getEventID()) {
        case LinkGrabberControllerEvent.ADD_FILEPACKAGE:
            if (SubConfiguration.getConfig(LinkGrabberController.CONFIG).getBooleanProperty(LinkGrabberController.PARAM_INFOPANEL_ONLINKGRAB)) {
                showFilePackageInfo((LinkGrabberFilePackage) event.getParameter());
            }
            updateAsync.restart();
            break;
        case LinkGrabberControllerEvent.REMOVE_FILEPACKAGE:
            if (filePackageInfo.getPackage() != null && filePackageInfo.getPackage() == ((LinkGrabberFilePackage) event.getParameter())) {
                if (!LGINSTANCE.getPackages().isEmpty()) {
                    showFilePackageInfo(LGINSTANCE.getPackages().get(0));
                } else {
                    hideFilePackageInfo();
                }
            }
            updateAsync.restart();
            break;
        case LinkGrabberControllerEvent.REFRESH_STRUCTURE:
            updateAsync.restart();
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