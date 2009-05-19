package jd.controlling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import jd.config.ConfigPropertyListener;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.event.JDBroadcaster;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberFilePackage;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberFilePackageEvent;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberFilePackageListener;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

class LinkGrabberControllerBroadcaster extends JDBroadcaster<LinkGrabberControllerListener, LinkGrabberControllerEvent> {

    // @Override
    protected void fireEvent(LinkGrabberControllerListener listener, LinkGrabberControllerEvent event) {
        listener.onLinkGrabberControllerEvent(event);
    }

}

public class LinkGrabberController implements LinkGrabberFilePackageListener, LinkGrabberControllerListener {

    public static final String PARAM_ONLINECHECK = "PARAM_ONLINECHECK";
    public static final String CONFIG = "LINKGRABBER";
    public static final String IGNORE_LIST = "IGNORE_LIST";

    private static Vector<LinkGrabberFilePackage> packages = new Vector<LinkGrabberFilePackage>();
    private static final HashSet<String> extensionFilter = new HashSet<String>();

    private static LinkGrabberController INSTANCE = null;
    private boolean lastSort = true;

    private LinkGrabberControllerBroadcaster broadcaster;

    private static String[] filter;

    private ConfigPropertyListener cpl;
    private LinkGrabberFilePackage FP_UNSORTED;
    private LinkGrabberFilePackage FP_UNCHECKED;
    private LinkGrabberFilePackage FP_UNCHECKABLE;
    private LinkGrabberFilePackage FP_OFFLINE;
    private LinkGrabberFilePackage FP_FILTERED;

    public synchronized static LinkGrabberController getInstance() {
        if (INSTANCE == null) INSTANCE = new LinkGrabberController();
        return INSTANCE;
    }

    public LinkGrabberFilePackage getFILTERPACKAGE() {
        return this.FP_FILTERED;
    }

    private LinkGrabberController() {
        getBroadcaster().addListener(this);

        filter = getLinkFilterPattern();
        JDController.getInstance().addControlListener(this.cpl = new ConfigPropertyListener(IGNORE_LIST) {

            // @Override
            public void onPropertyChanged(Property source, String propertyName) {
                filter = getLinkFilterPattern();
            }

        });

        FP_UNSORTED = new LinkGrabberFilePackage(JDLocale.L("gui.linkgrabber.package.unsorted", "various"), this);
        FP_UNCHECKED = new LinkGrabberFilePackage(JDLocale.L("gui.linkgrabber.package.unchecked", "unchecked"), this);
        FP_UNCHECKABLE = new LinkGrabberFilePackage(JDLocale.L("gui.linkgrabber.package.uncheckable", "uncheckable"), this);
        FP_UNCHECKABLE.setIgnore(true);
        FP_OFFLINE = new LinkGrabberFilePackage(JDLocale.L("gui.linkgrabber.package.offline", "offline"), this);
        FP_OFFLINE.setIgnore(true);
        FP_FILTERED = new LinkGrabberFilePackage(JDLocale.L("gui.linkgrabber.package.filtered", "filtered"));
        FP_FILTERED.setIgnore(true);
    }

    public HashSet<String> getExtensionFilter() {
        return extensionFilter;
    }

    public boolean isLinkCheckEnabled() {
        return SubConfiguration.getConfig(CONFIG).getBooleanProperty(PARAM_ONLINECHECK, true);
    }

    public void clearExtensionFilter() {
        synchronized (extensionFilter) {
            extensionFilter.clear();
            this.FP_FILTERED.setDownloadLinks(new Vector<DownloadLink>());
        }
    }

    public void FilterExtension(String ext, boolean b) {
        boolean c = false;
        synchronized (extensionFilter) {
            if (!b) {
                if (!extensionFilter.contains(ext)) {
                    extensionFilter.add(ext);
                    c = true;
                } else {
                    return;
                }
            } else {
                if (extensionFilter.contains(ext)) {
                    extensionFilter.remove(ext);
                    c = true;
                } else {
                    return;
                }
            }
        }
        if (c) broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.FILTER_CHANGED));
    }

    public String[] getLinkFilterPattern() {
        String filter = SubConfiguration.getConfig(CONFIG).getStringProperty(IGNORE_LIST, null);
        if (filter == null || filter.length() == 0) return null;
        String[] lines = Regex.getLines(filter);
        ArrayList<String> ret = new ArrayList<String>();
        for (String line : lines) {
            if (line.trim().startsWith("#") || line.trim().length() == 0) continue;
            ret.add(line.trim());
        }
        return ret.toArray(new String[] {});
    }

    protected void finalize() {
        JDController.getInstance().removeControlListener(cpl);
        System.out.println("REMOVED LISTENER " + cpl);
    }

    public synchronized JDBroadcaster<LinkGrabberControllerListener, LinkGrabberControllerEvent> getBroadcaster() {
        if (broadcaster == null) broadcaster = new LinkGrabberControllerBroadcaster();
        return broadcaster;
    }

    public Vector<LinkGrabberFilePackage> getPackages() {
        return packages;
    }

    public int indexOf(LinkGrabberFilePackage fp) {
        synchronized (packages) {
            return packages.indexOf(fp);
        }
    }

    public boolean isExtensionFiltered(DownloadLink link) {
        synchronized (extensionFilter) {
            for (String ext : extensionFilter) {
                if (link.getName().endsWith(ext)) { return true; }
            }
            return false;
        }
    }

    public LinkGrabberFilePackage getFPwithName(String name) {
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
            Vector<LinkGrabberFilePackage> fps = new Vector<LinkGrabberFilePackage>(packages);
            fps.add(this.FP_FILTERED);
            for (LinkGrabberFilePackage fp : fps) {
                if (fp.contains(link)) return fp;
            }
            return null;
        }
    }

    public void mergeOfflineandUncheckable() {
        synchronized (packages) {
            Vector<LinkGrabberFilePackage> fps = new Vector<LinkGrabberFilePackage>(packages);
            for (LinkGrabberFilePackage fp : fps) {
                synchronized (fp.getDownloadLinks()) {
                    boolean remove = false;
                    if (fp.countFailedLinks(true) == fp.size()) remove = true;
                    Vector<DownloadLink> links = new Vector<DownloadLink>(fp.getDownloadLinks());
                    for (DownloadLink dl : links) {
                        if (dl.isAvailabilityStatusChecked() && dl.getAvailableStatus() == AvailableStatus.UNCHECKABLE && links.size() == 1) {
                            FP_UNCHECKABLE.add(dl);
                        } else if (dl.isAvailabilityStatusChecked() && !dl.isAvailable() && (links.size() == 1 || remove)) {
                            FP_OFFLINE.add(dl);
                        }
                    }
                }
            }
        }
    }

    public boolean isDupe(DownloadLink link) {
        synchronized (packages) {
            if (link == null) return false;
            if (link.getBooleanProperty("ALLOW_DUPE", false)) return false;
            LinkGrabberFilePackage fp = null;
            DownloadLink dl = null;
            for (Iterator<LinkGrabberFilePackage> it = packages.iterator(); it.hasNext();) {
                fp = it.next();
                for (Iterator<DownloadLink> it2 = fp.getDownloadLinks().iterator(); it2.hasNext();) {
                    dl = it2.next();
                    if (dl.compareTo(link) == 0) return true;
                }
            }
            return false;
        }
    }

    public void addPackage(LinkGrabberFilePackage fp) {
        synchronized (packages) {
            if (!packages.contains(fp)) {
                packages.add(fp);
                fp.addListener(this);
                broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.ADD_FILEPACKAGE, fp));
            }
        }
    }

    public void addAllAt(Vector<LinkGrabberFilePackage> links, int index) {
        synchronized (packages) {
            for (int i = 0; i < links.size(); i++) {
                addPackageAt(links.get(i), index + i);
            }
        }
    }

    public void addPackageAt(LinkGrabberFilePackage fp, int index) {
        if (fp == null) return;
        synchronized (packages) {
            if (packages.size() == 0) {
                addPackage(fp);
                return;
            }
            if (packages.contains(fp)) {
                packages.remove(fp);
                if (index > packages.size() - 1) {
                    packages.add(fp);
                } else if (index < 0) {
                    packages.add(0, fp);
                } else
                    packages.add(index, fp);
                broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.REFRESH_STRUCTURE, fp));
            } else {
                if (index > packages.size() - 1) {
                    packages.add(fp);
                } else if (index < 0) {
                    packages.add(0, fp);
                } else
                    packages.add(index, fp);
                fp.addListener(this);
                broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.ADD_FILEPACKAGE, fp));
            }
        }
    }

    public void AddorMoveDownloadLink(LinkGrabberFilePackage fp, DownloadLink link) {
        synchronized (packages) {
            fp.add(link);
        }
    }

    public void removePackage(LinkGrabberFilePackage fp) {
        if (fp == null) return;
        synchronized (packages) {
            if (fp != this.FP_FILTERED && fp != this.FP_OFFLINE && fp != this.FP_UNCHECKED && fp != this.FP_UNSORTED) fp.removeListener(this);
            packages.remove(fp);
            broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.REMOVE_FILPACKAGE, fp));
        }
    }

    public void sort(final int col) {
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
        broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.REFRESH_STRUCTURE));
    }

    public void FilterPackages() {
        synchronized (packages) {
            synchronized (extensionFilter) {
                Vector<LinkGrabberFilePackage> fps = new Vector<LinkGrabberFilePackage>(packages);
                fps.add(this.FP_FILTERED);
                for (LinkGrabberFilePackage fp : fps) {
                    if (fp == this.FP_UNCHECKED || fp == this.FP_OFFLINE || fp == this.FP_UNSORTED) continue;
                    synchronized (fp.getDownloadLinks()) {
                        Vector<DownloadLink> links = new Vector<DownloadLink>(fp.getDownloadLinks());
                        for (DownloadLink dl : links) {
                            if (this.isExtensionFiltered(dl)) {
                                FP_FILTERED.add(dl);
                            } else {
                                attachToPackagesSecondStage(dl);
                            }
                        }
                    }
                }
            }
        }
    }

    public void attachToPackagesFirstStage(DownloadLink link) {
        synchronized (packages) {
            String packageName;
            LinkGrabberFilePackage fp = null;
            if (this.isExtensionFiltered(link)) {
                fp = this.FP_FILTERED;
            } else {
                if (link.getFilePackage() != FilePackage.getDefaultFilePackage()) {
                    packageName = link.getFilePackage().getName();
                    fp = getFPwithName(packageName);
                    if (fp == null) {
                        fp = new LinkGrabberFilePackage(packageName, this);
                    }
                }
            }
            if (fp == null) {
                if (isLinkCheckEnabled()) {
                    fp = this.FP_UNCHECKED;
                } else {
                    fp = this.FP_UNSORTED;
                }
            }
            fp.add(link);
        }
    }

    public int size() {
        return packages.size();
    }

    public synchronized void attachToPackagesSecondStage(DownloadLink link) {
        String packageName;
        boolean autoPackage = false;
        if (this.isExtensionFiltered(link)) {
            AddorMoveDownloadLink(this.FP_FILTERED, link);
            return;
        } else if (link.getFilePackage() != FilePackage.getDefaultFilePackage()) {
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
                fp.add(link);
            } else {
                String newPackageName = autoPackage ? JDUtilities.getSimString(packages.get(bestIndex).getName(), packageName) : packageName;
                packages.get(bestIndex).setName(newPackageName);
                packages.get(bestIndex).add(link);
            }
        }
    }

    private String cleanFileName(String name) {
        /** remove rar extensions */
        name = getNameMatch(name, "(.*?)\\d+$");
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

    private String getNameMatch(String name, String pattern) {
        String match = new Regex(name, pattern).getMatch(0);
        if (match != null) return match;
        return name;
    }

    private int comparepackages(String a, String b) {
        int c = 0;
        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            if (a.charAt(i) == b.charAt(i)) {
                c++;
            }
        }
        if (Math.min(a.length(), b.length()) == 0) { return 0; }
        return c * 100 / Math.max(a.length(), b.length());
    }

    public void handle_LinkGrabberFilePackageEvent(LinkGrabberFilePackageEvent event) {
        switch (event.getID()) {
        case LinkGrabberFilePackageEvent.EMPTY_EVENT:
            removePackage(((LinkGrabberFilePackage) event.getSource()));
            if (packages.size() == 0 && this.FP_FILTERED.size() == 0) {
                clearExtensionFilter();
                broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.EMPTY));
            }
            break;
        case LinkGrabberFilePackageEvent.ADD_LINK:
        case LinkGrabberFilePackageEvent.REMOVE_LINK:
            if (!packages.contains(((LinkGrabberFilePackage) event.getSource()))) {
                addPackage(((LinkGrabberFilePackage) event.getSource()));
            } else {
                broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.REFRESH_STRUCTURE, event.getSource()));
            }
            break;
        case LinkGrabberFilePackageEvent.UPDATE_EVENT:
            broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.REFRESH_STRUCTURE, event.getSource()));
            break;
        default:
            break;
        }
    }

    public void onLinkGrabberControllerEvent(LinkGrabberControllerEvent event) {
        switch (event.getID()) {
        case LinkGrabberControllerEvent.ADD_FILEPACKAGE:
        case LinkGrabberControllerEvent.REMOVE_FILPACKAGE:
            broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.REFRESH_STRUCTURE));
            break;
        case LinkGrabberControllerEvent.FILTER_CHANGED:
            FilterPackages();
            break;
        default:
            break;
        }
    }

    public static boolean isFiltered(DownloadLink element) {
        if (filter == null || filter.length == 0) return false;
        synchronized (filter) {
            for (String f : filter) {
                if (element.getDownloadURL().matches(f) || element.getName().matches(f)) {
                    JDLogger.getLogger().finer("Filtered link: " + element.getName() + " due to filter entry " + f);
                    return true;
                }
            }
            return false;
        }
    }

    public static boolean isFiltered(CryptedLink element) {
        if (filter == null || filter.length == 0) return false;
        synchronized (filter) {
            for (String f : filter) {
                String t = element.getCryptedUrl().replaceAll("httpviajd://", "http://").replaceAll("httpsviajd://", "https://");
                if (t.matches(f)) {
                    JDLogger.getLogger().finer("Filtered link: due to filter entry " + f);
                    return true;
                }
            }
            return false;
        }
    }

}
