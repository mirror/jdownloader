package jd.controlling;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

import jd.config.ConfigPropertyListener;
import jd.config.Property;
import jd.event.JDBroadcaster;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberConstants;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberFilePackage;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberFilePackageEvent;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberFilePackageListener;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

class LinkGrabberControllerBroadcaster extends JDBroadcaster<LinkGrabberControllerListener, LinkGrabberControllerEvent> {

    // @Override
    protected void fireEvent(LinkGrabberControllerListener listener, LinkGrabberControllerEvent event) {
        listener.onLinkGrabberControllerEvent(event);
    }

}

public class LinkGrabberController implements LinkGrabberFilePackageListener, LinkGrabberControllerListener {

    protected static Vector<LinkGrabberFilePackage> packages = new Vector<LinkGrabberFilePackage>();
    protected static Vector<LinkGrabberFilePackage> publicpackages = new Vector<LinkGrabberFilePackage>();

    private static LinkGrabberController INSTANCE = null;
    private boolean lastSort = true;

    private String PACKAGENAME_UNSORTED;
    private String PACKAGENAME_UNCHECKED;
    private String PACKAGENAME_OFFLINE;

    private LinkGrabberControllerBroadcaster broadcaster;

    private String[] filter;

    private ConfigPropertyListener cpl;

    public synchronized static LinkGrabberController getInstance() {
        if (INSTANCE == null) INSTANCE = new LinkGrabberController();
        return INSTANCE;
    }

    private LinkGrabberController() {
        PACKAGENAME_UNSORTED = JDLocale.L("gui.linkgrabber.package.unsorted", "various");
        PACKAGENAME_UNCHECKED = JDLocale.L("gui.linkgrabber.package.unchecked", "unchecked");
        PACKAGENAME_OFFLINE = JDLocale.L("gui.linkgrabber.package.offline", "offline");
        getBroadcaster().addListener(this);

        filter = LinkGrabberConstants.getLinkFilterPattern();
        JDController.getInstance().addControlListener(this.cpl = new ConfigPropertyListener(LinkGrabberConstants.IGNORE_LIST) {

            // @Override
            public void onPropertyChanged(Property source, String propertyName) {
                filter = LinkGrabberConstants.getLinkFilterPattern();
            }

        });
    }

    protected void finalize() {
        JDController.getInstance().removeControlListener(cpl);
        System.out.println("REMOVED LISTENER " + cpl);
    }

    public synchronized JDBroadcaster<LinkGrabberControllerListener, LinkGrabberControllerEvent> getBroadcaster() {
        if (broadcaster == null) broadcaster = new LinkGrabberControllerBroadcaster();
        return broadcaster;
    }

    public Vector<LinkGrabberFilePackage> getPackagesUnfiltered() {
        return packages;
    }

    public Vector<LinkGrabberFilePackage> getPackages() {
        synchronized (packages) {
            synchronized (publicpackages) {
                publicpackages.clear();
                for (LinkGrabberFilePackage fp : packages) {
                    if (fp.getDownloadLinks().size() > 0) publicpackages.add(fp);
                }
            }
        }
        return publicpackages;
    }

    public int indexOf(LinkGrabberFilePackage fp) {
        synchronized (packages) {
            return packages.indexOf(fp);
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
            LinkGrabberFilePackage fp = null;
            for (Iterator<LinkGrabberFilePackage> it = packages.iterator(); it.hasNext();) {
                fp = it.next();
                if (fp.contains(link)) return fp;
            }
            return null;
        }
    }

    public void MergeSingleOffline() {
        synchronized (packages) {
            LinkGrabberFilePackage fp2 = getFPwithName(PACKAGENAME_OFFLINE);
            if (fp2 == null) {
                fp2 = new LinkGrabberFilePackage(PACKAGENAME_OFFLINE, this);
            }
            Vector<LinkGrabberFilePackage> fps = new Vector<LinkGrabberFilePackage>(packages);
            for (LinkGrabberFilePackage fp : fps) {
                synchronized (fp.getDownloadLinksUnFiltered()) {
                    Vector<DownloadLink> links = new Vector<DownloadLink>(fp.getDownloadLinksUnFiltered());
                    for (DownloadLink dl : links) {
                        if (dl.isAvailabilityChecked() && !dl.isAvailable() && links.size() == 1) {
                            this.AddorMoveDownloadLink(fp2, dl);
                            System.out.println("move to offline");
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
                for (Iterator<DownloadLink> it2 = fp.getDownloadLinksUnFiltered().iterator(); it2.hasNext();) {
                    dl = it2.next();
                    if (dl.getDownloadURL().trim().replaceAll("httpviajd", "http").equalsIgnoreCase(link.getDownloadURL().trim().replaceAll("httpviajd", "http"))) { return true; }
                }
            }
            return false;
        }
    }

    public void addPackage(LinkGrabberFilePackage fp) {
        synchronized (packages) {
            if (!packages.contains(fp)) {
                packages.add(fp);
                fp.getBroadcaster().addListener(this);
                broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.ADD_FILEPACKAGE));
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
                broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.ADD_FILEPACKAGE));
            } else {
                if (index > packages.size() - 1) {
                    packages.add(fp);
                } else if (index < 0) {
                    packages.add(0, fp);
                } else
                    packages.add(index, fp);
                fp.getBroadcaster().addListener(this);
                broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.ADD_FILEPACKAGE));
            }
        }
    }

    public void AddorMoveDownloadLink(LinkGrabberFilePackage fp, DownloadLink link) {
        synchronized (packages) {
            LinkGrabberFilePackage fptmp = getFPwithLink(link);
            addPackage(fp);
            fp.add(link);
            if (fptmp != null && fp != fptmp) fptmp.remove(link);
            broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.REFRESH_STRUCTURE));
        }
    }

    public void removePackage(LinkGrabberFilePackage fp) {
        if (fp == null) return;
        synchronized (packages) {
            synchronized (publicpackages) {
                fp.getBroadcaster().removeListener(this);
                packages.remove(fp);
                publicpackages.remove(fp);
                broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.REMOVE_FILPACKAGE, fp));
            }
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

    public void attachToPackagesFirstStage(DownloadLink link) {
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
                if (LinkGrabberConstants.isLinkCheckEnabled()) {
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
            AddorMoveDownloadLink(fp, link);
        }
    }

    public int size() {
        return packages.size();
    }

    public synchronized void attachToPackagesSecondStage(DownloadLink link) {
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
                AddorMoveDownloadLink(fp, link);
            } else {
                String newPackageName = autoPackage ? JDUtilities.getSimString(packages.get(bestIndex).getName(), packageName) : packageName;
                packages.get(bestIndex).setName(newPackageName);
                AddorMoveDownloadLink(packages.get(bestIndex), link);
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
        return c * 100 / Math.min(a.length(), b.length());
    }

    public void handle_LinkGrabberFilePackageEvent(LinkGrabberFilePackageEvent event) {
        switch (event.getID()) {
        case LinkGrabberFilePackageEvent.EMPTY_EVENT:
            removePackage(((LinkGrabberFilePackage) event.getSource()));
            if (packages.size() == 0) broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.EMPTY));
            break;
        case LinkGrabberFilePackageEvent.ADD_LINK:
        case LinkGrabberFilePackageEvent.REMOVE_LINK:
            broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.REFRESH_STRUCTURE));
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
        default:
            break;
        }
    }

    public boolean isFiltered(DownloadLink element) {
        if (filter == null) return false;
        for (String f : filter) {
            if (element.getDownloadURL().matches(f) || element.getName().matches(f)) {
                JDLogger.getLogger().finer("Filtered link: " + element.getName() + " due to filter entry " + f);
                return true;
            }
        }
        return false;
    }

}
