package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.event.UIEvent;
import jd.gui.skins.simple.JTabbedPanel;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.tasks.LinkGrabberTaskPane;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.JXCollapsiblePane;

public class LinkGrabberV2Panel extends JTabbedPanel implements ActionListener, UpdateListener {

    private static final long serialVersionUID = 1607433619381447389L;
    protected static Vector<LinkGrabberV2FilePackage> packages = new Vector<LinkGrabberV2FilePackage>();
    public static final String PROPERTY_ONLINE_CHECK = "DO_ONLINE_CHECK_V2";
    public static final String PROPERTY_AUTOPACKAGE = "PROPERTY_AUTOPACKAGE";

    private Vector<DownloadLink> totalLinkList = new Vector<DownloadLink>();
    private Vector<DownloadLink> waitingList = new Vector<DownloadLink>();

    private LinkGrabberV2TreeTable internalTreeTable;

    protected Logger logger = JDUtilities.getLogger();

    private SubConfiguration guiConfig;
    private Thread gatherer;
    private String PACKAGENAME_UNSORTED;
    private String PACKAGENAME_UNCHECKED;
    private ProgressController pc;
    private JXCollapsiblePane collapsepane;
    private LinkGrabberV2FilePackageInfo FilePackageInfo;

    public LinkGrabberV2Panel(SimpleGUI parent) {
        super(new BorderLayout());
        PACKAGENAME_UNSORTED = JDLocale.L("gui.linkgrabber.package.unsorted", "various");
        PACKAGENAME_UNCHECKED = JDLocale.L("gui.linkgrabber.package.unchecked", "unchecked");
        guiConfig = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
        internalTreeTable = new LinkGrabberV2TreeTable(new LinkGrabberV2TreeTableModel(this), this);
        JScrollPane scrollPane = new JScrollPane(internalTreeTable);
        this.add(scrollPane);
        FilePackageInfo = new LinkGrabberV2FilePackageInfo();
        collapsepane = new JXCollapsiblePane();
        collapsepane.setCollapsed(true);
        collapsepane.add(FilePackageInfo);
        this.add(collapsepane, BorderLayout.SOUTH);
    }

    public void showFilePackageInfo(LinkGrabberV2FilePackage fp) {
        FilePackageInfo.setPackage(fp);
        collapsepane.setCollapsed(false);
    }

    public void hideFilePackageInfo() {
        collapsepane.setCollapsed(true);
    }

    public Vector<LinkGrabberV2FilePackage> getPackages() {
        return packages;
    }

    public synchronized void fireTableChanged(final int id, final Object param) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                synchronized (packages) {
                    internalTreeTable.fireTableChanged(id, param);
                }
            }
        });
    }

    @Override
    public void onHide() {
        // TODO Auto-generated method stub

    }

    public synchronized void addLinks(DownloadLink[] linkList) {
        for (DownloadLink element : linkList) {
            if (isDupe(element)) continue;
            addToWaitingList(element);
        }
        fireTableChanged(1, null);
        startLinkGatherer();
    }

    public synchronized void addToWaitingList(DownloadLink element) {
        totalLinkList.add(element);
        waitingList.add(element);
        checkAlreadyinList(element);
        attachToPackagesFirstStage(element);
    }

    private void attachToPackagesFirstStage(DownloadLink link) {
        String packageName;
        LinkGrabberV2FilePackage fp = null;
        if (link.getFilePackage() != FilePackage.getDefaultFilePackage()) {
            packageName = link.getFilePackage().getName();
            fp = getFPwithName(packageName);
            if (fp == null) {
                fp = new LinkGrabberV2FilePackage(packageName, this);
            }
        }
        if (fp == null) {
            if (guiConfig.getBooleanProperty(PROPERTY_ONLINE_CHECK, true)) {
                fp = getFPwithName(PACKAGENAME_UNCHECKED);
                if (fp == null) {
                    fp = new LinkGrabberV2FilePackage(PACKAGENAME_UNCHECKED, this);
                }
            } else {
                fp = getFPwithName(PACKAGENAME_UNSORTED);
                if (fp == null) {
                    fp = new LinkGrabberV2FilePackage(PACKAGENAME_UNSORTED, this);
                }
            }
        }
        addToPackages(fp, link);
    }

    private LinkGrabberV2FilePackage getFPwithName(String name) {
        synchronized (packages) {
            if (name == null) return null;
            LinkGrabberV2FilePackage fp = null;
            for (Iterator<LinkGrabberV2FilePackage> it = packages.iterator(); it.hasNext();) {
                fp = it.next();
                if (fp.getName().equalsIgnoreCase(name)) return fp;
            }
            return null;
        }
    }

    private LinkGrabberV2FilePackage getFPwithLink(DownloadLink link) {
        synchronized (packages) {
            if (link == null) return null;
            LinkGrabberV2FilePackage fp = null;
            for (Iterator<LinkGrabberV2FilePackage> it = packages.iterator(); it.hasNext();) {
                fp = it.next();
                if (fp.contains(link)) return fp;
            }
            return null;
        }
    }

    private void addToPackages(LinkGrabberV2FilePackage fp, DownloadLink link) {
        synchronized (packages) {
            LinkGrabberV2FilePackage fptmp = getFPwithLink(link);
            if (fptmp != null) fptmp.remove(link);
            if (!packages.contains(fp)) packages.add(fp);
            fp.add(link);
        }
    }

    private boolean isDupe(DownloadLink link) {
        if (link.getBooleanProperty("ALLOW_DUPE", false)) return false;
        for (DownloadLink l : totalLinkList) {
            if (l.getDownloadURL().trim().equalsIgnoreCase(link.getDownloadURL().trim())) { return true; }
        }
        return false;
    }

    private void startLinkGatherer() {
        if (gatherer != null && gatherer.isAlive()) { return; }
        gatherer = new Thread() {
            public void run() {
                if (!guiConfig.getBooleanProperty(PROPERTY_ONLINE_CHECK, true)) return;
                pc = new ProgressController("onlinecheck");
                pc.setRange(0);
                while (waitingList.size() != 0) {
                    while (waitingList.size() > 0) {
                        HashMap<String, ArrayList<DownloadLink>> map = new HashMap<String, ArrayList<DownloadLink>>();
                        for (Iterator<DownloadLink> it = waitingList.iterator(); it.hasNext();) {
                            pc.addToMax(1);
                            DownloadLink l = it.next();
                            it.remove();
                            ArrayList<DownloadLink> localList = map.get(l.getPlugin().getHost());
                            if (localList == null) {
                                localList = new ArrayList<DownloadLink>();
                                map.put(l.getPlugin().getHost(), localList);
                            }
                            localList.add(l);
                        }
                        ArrayList<DownloadLink> hosterList;
                        for (Iterator<ArrayList<DownloadLink>> it = map.values().iterator(); it.hasNext();) {
                            hosterList = it.next();
                            DownloadLink link = hosterList.get(0);
                            boolean ret = ((PluginForHost) link.getPlugin()).checkLinks(hosterList.toArray(new DownloadLink[] {}));
                            if (!ret) {
                                for (int i = 0; i < hosterList.size(); i++) {
                                    link = hosterList.get(i);
                                    link.isAvailable();
                                    pc.increase(1);
                                    attachToPackagesSecondStage(link);
                                    fireTableChanged(1, null);
                                }
                            } else {
                                for (int i = 0; i < hosterList.size(); i++) {
                                    link = hosterList.get(i);
                                    pc.increase(1);
                                    attachToPackagesSecondStage(link);
                                    fireTableChanged(1, null);
                                }
                            }
                        }
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }
                }
                pc.finalize();
            }
        };
        gatherer.start();
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
                LinkGrabberV2FilePackage fp = new LinkGrabberV2FilePackage(packageName, this);
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
        // TODO Auto-generated method stub

    }

    public void UpdateEvent(UpdateEvent event) {
        synchronized (packages) {
            if (event.getSource() instanceof LinkGrabberV2FilePackage && event.getID() == UpdateEvent.EMPTY_EVENT) {
                ((LinkGrabberV2FilePackage) event.getSource()).getUpdateBroadcaster().removeUpdateListener(this);
                if (FilePackageInfo.getPackage() == ((LinkGrabberV2FilePackage) event.getSource()) || FilePackageInfo.getPackage() == null) {
                    this.hideFilePackageInfo();
                }
                packages.remove(event.getSource());
            }
        }

    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource() instanceof LinkGrabberTaskPane) {
            if (arg0.getID() == LinkGrabberV2TreeTableAction.ADD_ALL) {
                Vector<LinkGrabberV2FilePackage> all = new Vector<LinkGrabberV2FilePackage>(packages);
                confirmPackages(all);
                fireTableChanged(1, null);
                return;
            }
            if (arg0.getID() == LinkGrabberV2TreeTableAction.ADD_SELECTED) {
                Vector<LinkGrabberV2FilePackage> selected = new Vector<LinkGrabberV2FilePackage>(this.internalTreeTable.getSelectedFilePackages());
                confirmPackages(selected);
                fireTableChanged(1, null);
                return;
            }
        }
    }

    private void confirmPackages(Vector<LinkGrabberV2FilePackage> all) {
        for (int i = 0; i < all.size(); ++i) {
            confirmPackage(all.get(i), null);
        }
    }

    private void confirmPackage(LinkGrabberV2FilePackage fpv2, String host) {
        if (fpv2 == null) return;
        Vector<DownloadLink> linkList = fpv2.getDownloadLinks();
        if (linkList.isEmpty()) return;

        FilePackage fp = new FilePackage();
        fp.setName(fpv2.getName());
        fp.setComment(fpv2.getComment());
        fp.setPassword(fpv2.getPassword());
        // fp.setExtractAfterDownload(fpv2.isExtract());
        // addToDownloadDirs(fpv2.getDownloadDirectory(), fpv2.getName());

        // if (fpv2.useSubdirectory()) {
        if (true) {
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
            fp.setDownloadLinks(linkList);
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
                    totalLinkList.remove(link);
                    linkListHost.add(link);
                    link.setFilePackage(fp);
                    ++files;
                }
            }
            if (files == 0) return;
            fp.setDownloadLinks(linkListHost);
            fpv2.setDownloadLinks(linkList);
        }
        JDUtilities.getGUI().fireUIEvent(new UIEvent(this, UIEvent.UI_PACKAGE_GRABBED, fp));
    }

    public void checkAlreadyinList(DownloadLink link) {
        if (JDUtilities.getController().hasDownloadLinkURL(link.getDownloadURL())) {
            link.getLinkStatus().setErrorMessage("Already in Downloadlist");
            link.getLinkStatus().addStatus(LinkStatus.ERROR_ALREADYEXISTS);
        }
    }
}
