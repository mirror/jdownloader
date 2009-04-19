package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.controlling.ProgressControllerEvent;
import jd.controlling.ProgressControllerListener;
import jd.event.JDBroadcaster;
import jd.gui.skins.simple.JDCollapser;
import jd.gui.skins.simple.JTabbedPanel;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.tasks.LinkGrabberTaskPane;
import jd.nutils.io.JDFileFilter;
import jd.nutils.jobber.Jobber;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

class LinkGrabberBroadcaster extends JDBroadcaster<LinkGrabberListener, LinkGrabberEvent> {

    @Override
    protected void fireEvent(LinkGrabberListener listener, LinkGrabberEvent event) {
        listener.onLinkgrabberEvent(event);

    }

}

public class LinkGrabberPanel extends JTabbedPanel implements ActionListener, LinkGrabberFilePackageListener, LinkCheckListener, ProgressControllerListener {

    private static final long serialVersionUID = 1607433619381447389L;
    protected static Vector<LinkGrabberFilePackage> packages = new Vector<LinkGrabberFilePackage>();
    public static final String PROPERTY_ONLINE_CHECK = "DO_ONLINE_CHECK_V2";
    public static final String PROPERTY_AUTOPACKAGE = "PROPERTY_AUTOPACKAGE";

    private Vector<DownloadLink> waitingList = new Vector<DownloadLink>();

    private LinkGrabberTreeTable internalTreeTable;

    protected Logger logger = jd.controlling.JDLogger.getLogger();

    private SubConfiguration guiConfig;
    private Thread gatherer;
    private boolean gatherer_running = false;
    private String PACKAGENAME_UNSORTED;
    private String PACKAGENAME_UNCHECKED;
    private ProgressController pc;

    private LinkGrabberFilePackageInfo FilePackageInfo;
    private Timer gathertimer;

    private Jobber checkJobbers = new Jobber(4);

    private boolean lastSort = false;
    private LinkCheck lc = LinkCheck.getLinkChecker();
    private Timer Update_Async;
    private static LinkGrabberPanel INSTANCE;

    private transient LinkGrabberBroadcaster broadcaster = new LinkGrabberBroadcaster();
    private boolean visible = true;

    public static synchronized LinkGrabberPanel getLinkGrabber() {
        if (INSTANCE == null) INSTANCE = new LinkGrabberPanel();
        return INSTANCE;
    }

    public boolean isRunning() {
        return gatherer_running;
    }

    private LinkGrabberPanel() {

        super(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[fill,grow]"));

        PACKAGENAME_UNSORTED = JDLocale.L("gui.linkgrabber.package.unsorted", "various");
        PACKAGENAME_UNCHECKED = JDLocale.L("gui.linkgrabber.package.unchecked", "unchecked");
        guiConfig = SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME);
        internalTreeTable = new LinkGrabberTreeTable(new LinkGrabberTreeTableModel(this), this);
        JScrollPane scrollPane = new JScrollPane(internalTreeTable);
        this.add(scrollPane, "cell 0 0");
        FilePackageInfo = new LinkGrabberFilePackageInfo();

        Update_Async = new Timer(250, this);
        Update_Async.setInitialDelay(250);
        Update_Async.setRepeats(false);
        gathertimer = new Timer(2000, LinkGrabberPanel.this);
        gathertimer.setInitialDelay(2000);
        gathertimer.setRepeats(false);
        INSTANCE = this;
    }

    public synchronized JDBroadcaster<LinkGrabberListener, LinkGrabberEvent> getBroadcaster() {
        if (broadcaster == null) broadcaster = new LinkGrabberBroadcaster();
        return this.broadcaster;
    }

    public void showFilePackageInfo(LinkGrabberFilePackage fp) {
        FilePackageInfo.setPackage(fp);
        JDCollapser.getInstance().setContentPanel(FilePackageInfo);
        JDCollapser.getInstance().getContentPane().add(FilePackageInfo);
        JDCollapser.getInstance().setTitle("FilePackage");
        JDCollapser.getInstance().setVisible(true);

        JDCollapser.getInstance().setCollapsed(false);
    }

    public void hideFilePackageInfo() {
        JDCollapser.getInstance().setCollapsed(true);
    }

    public Vector<LinkGrabberFilePackage> getPackages() {
        return packages;
    }

    public synchronized void fireTableChanged(final int id, final Object param) {
        synchronized (packages) {
            internalTreeTable.fireTableChanged(id, param);
        }
    }

    @Override
    public void onHide() {
        visible = false;

    }

    public synchronized void addLinks(DownloadLink[] linkList) {
        for (DownloadLink element : linkList) {
            if (isDupe(element)) continue;
            addToWaitingList(element);
        }
        Update_Async.restart();
        gathertimer.restart();
    }

    public synchronized void addToWaitingList(DownloadLink element) {
        waitingList.add(element);
        checkAlreadyinList(element);
        attachToPackagesFirstStage(element);
        getBroadcaster().fireEvent(new LinkGrabberEvent(this, LinkGrabberEvent.UPDATE_EVENT));
    }

    private void attachToPackagesFirstStage(DownloadLink link) {
        synchronized (packages) {
            String packageName;
            LinkGrabberFilePackage fp = null;
            if (link.getFilePackage() != FilePackage.getDefaultFilePackage()) {
                packageName = link.getFilePackage().getName();
                fp = getFPwithName(packageName);
                if (fp == null) {
                    fp = new LinkGrabberFilePackage(packageName, this);
                }
            }
            if (fp == null) {
                if (guiConfig.getBooleanProperty(PROPERTY_ONLINE_CHECK, true)) {
                    fp = getFPwithName(PACKAGENAME_UNCHECKED);
                    if (fp == null) {
                        fp = new LinkGrabberFilePackage(PACKAGENAME_UNCHECKED, this);
                    }
                } else {
                    fp = getFPwithName(PACKAGENAME_UNSORTED);
                    if (fp == null) {
                        fp = new LinkGrabberFilePackage(PACKAGENAME_UNSORTED, this);
                    }
                }
            }
            addToPackages(fp, link);
        }
    }

    private LinkGrabberFilePackage getFPwithName(String name) {
        synchronized (packages) {
            if (name == null) return null;
            LinkGrabberFilePackage fp = null;
            for (Iterator<LinkGrabberFilePackage> it = packages.iterator(); it.hasNext();) {
                fp = it.next();
                if (fp.getName().equalsIgnoreCase(name)) return fp;
            }
            return null;
        }
    }

    public LinkGrabberFilePackage getFPwithLink(DownloadLink link) {
        synchronized (packages) {
            if (link == null) return null;
            LinkGrabberFilePackage fp = null;
            for (Iterator<LinkGrabberFilePackage> it = packages.iterator(); it.hasNext();) {
                fp = it.next();
                if (fp.contains(link)) return fp;
            }
            return null;
        }
    }

    private boolean isDupe(DownloadLink link) {
        synchronized (packages) {
            if (link == null) return false;
            if (link.getBooleanProperty("ALLOW_DUPE", false)) return false;
            LinkGrabberFilePackage fp = null;
            DownloadLink dl = null;
            for (Iterator<LinkGrabberFilePackage> it = packages.iterator(); it.hasNext();) {
                fp = it.next();
                for (Iterator<DownloadLink> it2 = fp.getDownloadLinks().iterator(); it2.hasNext();) {
                    dl = it2.next();
                    if (dl.getDownloadURL().trim().replaceAll("httpviajd", "http").equalsIgnoreCase(link.getDownloadURL().trim().replaceAll("httpviajd", "http"))) { return true; }
                }
            }
            return false;
        }
    }

    private void addToPackages(LinkGrabberFilePackage fp, DownloadLink link) {
        synchronized (packages) {
            LinkGrabberFilePackage fptmp = getFPwithLink(link);
            if (!packages.contains(fp)) packages.add(fp);
            fp.add(link);
            if (fptmp != null && fp != fptmp) fptmp.remove(link);
        }
    }

    private void removeFromPackages(DownloadLink link) {
        if (link == null) return;
        synchronized (packages) {
            LinkGrabberFilePackage fptmp = getFPwithLink(link);
            if (fptmp != null) fptmp.remove(link);
        }
    }

    private void stopLinkGatherer() {
        lc.getBroadcaster().removeListener(this);
        if (gatherer != null && gatherer.isAlive()) {
            gatherer_running = false;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    pc.setStatusText(pc.getStatusText() + ": Aborted");
                    pc.finalize(5000l);
                }
            });
            checkJobbers.stop();
            gatherer.interrupt();
        }
    }

    private void startLinkGatherer() {
        if (gatherer != null && gatherer.isAlive()) { return; }
        gatherer = new Thread() {
            public void run() {
                setName("LinkGrabber");
                gatherer_running = true;
                pc = new ProgressController("LinkGrabber");
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
                    if (!guiConfig.getBooleanProperty(PROPERTY_ONLINE_CHECK, true)) {
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
                gatherer_running = false;
            }
        };
        gatherer.start();
    }

    private synchronized void afterLinkGrabber(Vector<DownloadLink> links) {
        for (DownloadLink link : links) {
            if (!gatherer_running) break;
            if (!link.getBooleanProperty("removed", false)) attachToPackagesSecondStage(link);
        }
        pc.increase(links.size());
        Update_Async.restart();
    }

    private String getNameMatch(String name, String pattern) {
        String match = new Regex(name, pattern).getMatch(0);
        if (match != null) return match;
        return name;
    }

    private String cleanFileName(String name) {
        /** remove rar extensions */

        name = getNameMatch(name, "(.*)\\.part[0]*[1].rar$");
        name = getNameMatch(name, "(.*)\\.part[0-9]+.rar$");
        name = getNameMatch(name, "(.*)\\.rar$");

        name = getNameMatch(name, "(.*)\\.r\\d+$");

        /**
         * remove 7zip and hjmerge extensions
         */

        name = getNameMatch(name, "(?is).*\\.7z\\.[\\d]+$");
        name = getNameMatch(name, "(.*)\\.a.$");

        name = getNameMatch(name, "(.*)\\.[\\d]+($|\\.(7z|rar|divx|avi|xvid|bz2|doc|gz|jpg|jpeg|m4a|mdf|mkv|mp3|mp4|mpg|mpeg|pdf|wma|wmv|xcf|zip|jar|swf|class|bmp|cue|bin|dll|cab|png|ico|exe|gif|iso|flv|cso)$)");

        int lastPoint = name.lastIndexOf(".");
        if (lastPoint <= 0) return name;
        String extension = name.substring(name.length() - lastPoint + 1);
        if (extension.length() > 0 && extension.length() < 6) {
            name = name.substring(0, lastPoint);
        }
        return JDUtilities.removeEndingPoints(name);
    }

    private synchronized void attachToPackagesSecondStage(DownloadLink link) {
        String packageName;
        boolean autoPackage = false;
        if (link.getFilePackage() != FilePackage.getDefaultFilePackage()) {
            packageName = link.getFilePackage().getName();
        } else {
            autoPackage = true;
            packageName = cleanFileName(link.getName());
        }
        synchronized (packages) {
            int bestSim = 0;
            int bestIndex = -1;
            for (int i = 0; i < packages.size(); i++) {
                int sim = comparepackages(packages.get(i).getName(), packageName);
                if (sim > bestSim) {
                    bestSim = sim;
                    bestIndex = i;
                }
            }
            if (bestSim < 99) {
                LinkGrabberFilePackage fp = new LinkGrabberFilePackage(packageName, this);
                addToPackages(fp, link);
            } else {
                String newPackageName = autoPackage ? JDUtilities.getSimString(packages.get(bestIndex).getName(), packageName) : packageName;
                packages.get(bestIndex).setName(newPackageName);
                addToPackages(packages.get(bestIndex), link);
            }
        }
    }

    private int comparepackages(String a, String b) {

        int c = 0;
        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            if (a.charAt(i) == b.charAt(i)) {
                c++;
            }
        }
        if (Math.min(a.length(), b.length()) == 0) { return 0; }
        return c * 100 / Math.min(a.length(), b.length());
    }

    @Override
    public void onDisplay() {
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
    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource() == this.Update_Async) {
            if (visible) fireTableChanged(1, null);
            return;
        }

        if (arg0.getSource() == this.gathertimer) {
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
        Set<String> hoster = null;
        JDFileChooser fc;
        int col = 0;
        boolean b = false;
        synchronized (packages) {
            if (arg0.getSource() instanceof LinkGrabberTaskPane) {
                switch (arg0.getID()) {
                case LinkGrabberTreeTableAction.ADD_ALL:
                    selected_packages = new Vector<LinkGrabberFilePackage>(packages);
                    break;
                case LinkGrabberTreeTableAction.CLEAR:
                    stopLinkGatherer();
                    lc.abortLinkCheck();
                    selected_packages = new Vector<LinkGrabberFilePackage>(packages);
                    break;
                case LinkGrabberTreeTableAction.ADD_SELECTED:
                    selected_packages = new Vector<LinkGrabberFilePackage>(this.internalTreeTable.getSelectedFilePackages());
                    break;

                case LinkGrabberTreeTableAction.GUI_LOAD:
                    fc = new JDFileChooser("_LOADSAVEDLC");
                    fc.setDialogTitle(JDLocale.L("gui.filechooser.loaddlc", "Load DLC file"));
                    fc.setFileFilter(new JDFileFilter(null, ".dlc|.rsdf|.ccf|.linkbackup", true));
                    if (fc.showOpenDialog(null) == JDFileChooser.APPROVE_OPTION) {
                        File ret2 = fc.getSelectedFile();
                        if (ret2 != null) {
                            JDUtilities.getController().loadContainerFile(ret2);
                        }
                    }
                    return;
                }
            } else if (arg0.getSource() instanceof JMenuItem) {
                switch (arg0.getID()) {
                case LinkGrabberTreeTableAction.SELECT_HOSTER:
                    hoster = (Set<String>) ((LinkGrabberTreeTableAction) ((JMenuItem) arg0.getSource()).getAction()).getProperty().getProperty("hoster");
                    selected_packages = new Vector<LinkGrabberFilePackage>(packages);
                    break;
                case LinkGrabberTreeTableAction.ADD_ALL:
                case LinkGrabberTreeTableAction.DELETE_OFFLINE:
                    selected_packages = new Vector<LinkGrabberFilePackage>(packages);
                    break;
                case LinkGrabberTreeTableAction.ADD_SELECTED:
                case LinkGrabberTreeTableAction.EDIT_DIR:
                    selected_packages = new Vector<LinkGrabberFilePackage>(this.internalTreeTable.getSelectedFilePackages());
                    break;
                case LinkGrabberTreeTableAction.SORT:
                    col = (Integer) ((LinkGrabberTreeTableAction) ((JMenuItem) arg0.getSource()).getAction()).getProperty().getProperty("col");
                    selected_packages = new Vector<LinkGrabberFilePackage>(this.internalTreeTable.getSelectedFilePackages());
                    break;
                case LinkGrabberTreeTableAction.DOWNLOAD_PRIO:
                case LinkGrabberTreeTableAction.DE_ACTIVATE:
                    prop = (HashMap<String, Object>) ((LinkGrabberTreeTableAction) ((JMenuItem) arg0.getSource()).getAction()).getProperty().getProperty("infos");
                    selected_links = (Vector<DownloadLink>) prop.get("links");
                    break;
                case LinkGrabberTreeTableAction.DELETE:
                case LinkGrabberTreeTableAction.SET_PW:
                case LinkGrabberTreeTableAction.NEW_PACKAGE:
                    selected_links = (Vector<DownloadLink>) ((LinkGrabberTreeTableAction) ((JMenuItem) arg0.getSource()).getAction()).getProperty().getProperty("links");
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
                Update_Async.restart();
                break;
            case LinkGrabberTreeTableAction.SORT:
                for (LinkGrabberFilePackage fp2 : selected_packages) {
                    fp2.sort(col);
                }
                Update_Async.restart();
                break;
            case LinkGrabberTreeTableAction.SORT_ALL:
                sort(col);
                Update_Async.restart();
                break;
            case LinkGrabberTreeTableAction.SELECT_HOSTER:
                for (LinkGrabberFilePackage fp2 : selected_packages) {
                    fp2.keepHostersOnly(hoster);
                }
                Update_Async.restart();
                break;
            case LinkGrabberTreeTableAction.EDIT_DIR:
                fc = new JDFileChooser();
                fc.setApproveButtonText(JDLocale.L("gui.btn_ok", "OK"));
                fc.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
                fc.setCurrentDirectory(new File(selected_packages.get(0).getDownloadDirectory()));
                if (fc.showOpenDialog(this) == JDFileChooser.APPROVE_OPTION) {
                    if (fc.getSelectedFile() != null) {
                        for (LinkGrabberFilePackage fp2 : selected_packages) {
                            fp2.setDownloadDirectory(fc.getSelectedFile().getAbsolutePath());
                        }
                    }
                }
                break;
            case LinkGrabberTreeTableAction.NEW_PACKAGE:
                fp = this.getFPwithLink(selected_links.get(0));
                String name = SimpleGUI.CURRENTGUI.showUserInputDialog(JDLocale.L("gui.linklist.newpackage.message", "Name of the new package"), fp.getName());
                if (name != null) {
                    LinkGrabberFilePackage nfp = new LinkGrabberFilePackage(name, this);
                    for (DownloadLink link : selected_links) {
                        removeFromPackages(link);
                        addToPackages(nfp, link);
                    }
                    Update_Async.restart();
                }
                return;
            case LinkGrabberTreeTableAction.SET_PW:
                pw = SimpleGUI.CURRENTGUI.showUserInputDialog(JDLocale.L("gui.linklist.setpw.message", "Set download password"), null);
                for (int i = 0; i < selected_links.size(); i++) {
                    fp = this.getFPwithLink(selected_links.elementAt(i));
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
                Update_Async.restart();
                return;
            case LinkGrabberTreeTableAction.DELETE:
                for (DownloadLink link : selected_links) {
                    link.setProperty("removed", true);
                    removeFromPackages(link);
                }
                Update_Async.restart();
                return;
            case LinkGrabberTreeTableAction.CLEAR:
                for (LinkGrabberFilePackage fp2 : selected_packages) {
                    fp2.setDownloadLinks(new Vector<DownloadLink>());
                }
                Update_Async.restart();
                return;
            case LinkGrabberTreeTableAction.DOWNLOAD_PRIO:
                prio = (Integer) prop.get("prio");
                for (int i = 0; i < selected_links.size(); i++) {
                    selected_links.elementAt(i).setPriority(prio);
                }
                return;
            }
        }
    }

    private void confirmPackages(Vector<LinkGrabberFilePackage> all) {
        for (int i = 0; i < all.size(); ++i) {
            confirmPackage(all.get(i), null);
        }
    }

    private void addToDownloadDirs(String downloadDirectory, String packageName) {
        if (packageName.length() < 5 || downloadDirectory.equalsIgnoreCase(JDUtilities.getConfiguration().getDefaultDownloadDirectory())) return;
        getDownloadDirList().add(new String[] { downloadDirectory, packageName });
        guiConfig.save();
    }

    @SuppressWarnings("unchecked")
    private ArrayList<String[]> getDownloadDirList() {
        return ((ArrayList<String[]>) guiConfig.getProperty("DOWNLOADDIR_LIST", new ArrayList<String[]>()));
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
        JDUtilities.getDownloadController().addPackage(fp);
        Update_Async.restart();
    }

    public void checkAlreadyinList(DownloadLink link) {
        if (JDUtilities.getDownloadController().getFirstDownloadLinkwithURL(link.getDownloadURL()) != null) {
            link.getLinkStatus().setErrorMessage("Already in Downloadlist");
            link.getLinkStatus().addStatus(LinkStatus.ERROR_ALREADYEXISTS);
        }
    }

    private void sort(final int col) {
        lastSort = !lastSort;
        synchronized (packages) {

            Collections.sort(packages, new Comparator<LinkGrabberFilePackage>() {

                public int compare(LinkGrabberFilePackage a, LinkGrabberFilePackage b) {
                    LinkGrabberFilePackage aa = a;
                    LinkGrabberFilePackage bb = b;
                    if (lastSort) {
                        aa = b;
                        bb = a;
                    }
                    switch (col) {
                    case 1:
                        return aa.getName().compareToIgnoreCase(bb.getName());
                    case 2:
                        return aa.getDownloadSize(false) > bb.getDownloadSize(false) ? 1 : -1;
                    case 3:
                        return aa.getHoster().compareToIgnoreCase(bb.getHoster());
                    case 4:
                        return aa.countFailedLinks(false) > bb.countFailedLinks(false) ? 1 : -1;
                    default:
                        return -1;
                    }

                }

            });
        }
    }

    public void handle_LinkGrabberFilePackageEvent(LinkGrabberFilePackageEvent event) {
        if (event.getID() == LinkGrabberFilePackageEvent.EMPTY_EVENT) {
            synchronized (packages) {
                ((LinkGrabberFilePackage) event.getSource()).getBroadcaster().removeListener(this);
                if (FilePackageInfo.getPackage() == ((LinkGrabberFilePackage) event.getSource()) || FilePackageInfo.getPackage() == null) {
                    this.hideFilePackageInfo();
                }
                packages.remove(event.getSource());
                Update_Async.restart();
                if (packages.size() == 0) getBroadcaster().fireEvent(new LinkGrabberEvent(this, LinkGrabberEvent.EMPTY_EVENT));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void handle_LinkCheckEvent(LinkCheckEvent event) {
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

    public void handle_ProgressControllerEvent(ProgressControllerEvent event) {
        if (event.getSource() == this.pc) {
            lc.abortLinkCheck();
            this.stopLinkGatherer();
            return;
        }
    }

    public boolean hasLinks() {
        return waitingList.size() > 0 || packages.size() > 0;

    }
}
