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

package jd.controlling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jd.config.ConfigPropertyListener;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.event.JDBroadcaster;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkGrabberFilePackage;
import jd.plugins.LinkGrabberFilePackageEvent;
import jd.plugins.LinkGrabberFilePackageListener;
import jd.utils.locale.JDL;

class LinkGrabberControllerBroadcaster extends JDBroadcaster<LinkGrabberControllerListener, LinkGrabberControllerEvent> {

    @Override
    protected void fireEvent(LinkGrabberControllerListener listener, LinkGrabberControllerEvent event) {
        listener.onLinkGrabberControllerEvent(event);
    }

}

public class LinkGrabberController implements LinkGrabberFilePackageListener, LinkGrabberControllerListener {

    public final static Object ControllerLock = new Object();

    public static final byte MOVE_BEFORE = 1;
    public static final byte MOVE_AFTER = 2;
    public static final byte MOVE_BEGIN = 3;
    public static final byte MOVE_END = 4;
    public static final byte MOVE_TOP = 5;
    public static final byte MOVE_BOTTOM = 6;
    public static final byte MOVE_UP = 7;
    public static final byte MOVE_DOWN = 8;

    public static final String PARAM_ONLINECHECK = "PARAM_ONLINECHECK";
    public static final String PARAM_REPLACECHARS = "PARAM_REPLACECHARS";
    public static final String PARAM_INFOPANEL_ONLINKGRAB = "PARAM_INFOPANEL_ONLINKGRAB";
    public static final String CONFIG = "LINKGRABBER";
    public static final String IGNORE_LIST = "IGNORE_LIST";
    public static final String DONTFORCEPACKAGENAME = "dontforcename";
    public static final String PARAM_CONTROLPOSITION = "PARAM_CONTROLPOSITION";
    public static final String PARAM_USE_CNL2 = "PARAM_USE_CNL2";
    public static final String PARAM_NEWPACKAGES = "PARAM_NEWPACKAGES";

    public static final String PROPERTY_EXPANDED = "lg_expanded";
    public static final String PROPERTY_USEREXPAND = "lg_userexpand";

    private static ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>();
    private static final HashSet<String> extensionFilter = new HashSet<String>();

    private static LinkGrabberController INSTANCE = null;

    private LinkGrabberControllerBroadcaster broadcaster;

    private static String[] filter;

    private ConfigPropertyListener cpl;
    private LinkGrabberFilePackage FP_UNSORTED;
    private LinkGrabberFilePackage FP_UNCHECKED;
    private LinkGrabberFilePackage FP_OFFLINE;
    private LinkGrabberFilePackage FP_FILTERED;
    private LinkGrabberDistributeEvent distributer = null;
    private LinkGrabberPackagingEvent customizedpackager = null;

    private Logger logger;

    public synchronized static LinkGrabberController getInstance() {
        if (INSTANCE == null) INSTANCE = new LinkGrabberController();
        return INSTANCE;
    }

    public LinkGrabberFilePackage getFilterPackage() {
        return this.FP_FILTERED;
    }

    public void setDistributer(LinkGrabberDistributeEvent dist) {
        this.distributer = dist;
    }

    public void setCustomizedPackager(LinkGrabberPackagingEvent pack) {
        customizedpackager = pack;
    }

    public void addLinks(ArrayList<DownloadLink> links, boolean hidegrabber, boolean autostart) {
        if (links.size() > 0) broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.NEW_LINKS));
        if (distributer != null) {
            distributer.addLinks(links, hidegrabber, autostart);
        } else {
            addLinksInternal(links, hidegrabber, autostart);
        }
    }

    public void addLinksInternal(ArrayList<DownloadLink> links, boolean hidegrabber, boolean autostart) {
        /*
         * TODO: evtl autopackaging auch hier, aber eigentlich net nötig, da es
         * sache des coders ist was genau er machen soll
         */
        JDLogger.getLogger().info("No Distributer set, using minimal version");
        ArrayList<FilePackage> fps = new ArrayList<FilePackage>();
        FilePackage fp = FilePackage.getInstance();
        fp.setName("Added");
        for (DownloadLink link : links) {
            if (link.getFilePackage() == FilePackage.getDefaultFilePackage()) {
                fp.add(link);
                if (!fps.contains(fp)) fps.add(fp);
            } else {
                if (!fps.contains(link.getFilePackage())) fps.add(link.getFilePackage());
            }
        }
        broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.FINISHED, fps));
        DownloadController.getInstance().addAllAt(fps, 0);
        if (autostart) DownloadWatchDog.getInstance().startDownloads();
    }

    public void addListener(LinkGrabberControllerListener l) {
        broadcaster.addListener(l);
    }

    public void removeListener(LinkGrabberControllerListener l) {
        broadcaster.removeListener(l);
    }

    private LinkGrabberController() {
        logger = jd.controlling.JDLogger.getLogger();
        broadcaster = new LinkGrabberControllerBroadcaster();
        broadcaster.addListener(this);

        filter = getLinkFilterPattern();
        JDController.getInstance().addControlListener(this.cpl = new ConfigPropertyListener(IGNORE_LIST) {

            @Override
            public void onPropertyChanged(Property source, String propertyName) {
                filter = getLinkFilterPattern();
            }

        });

        FP_UNSORTED = new LinkGrabberFilePackage(JDL.L("gui.linkgrabber.package.unsorted", "various"), this);
        FP_UNCHECKED = new LinkGrabberFilePackage(JDL.L("gui.linkgrabber.package.unchecked", "unchecked"), this);
        FP_OFFLINE = new LinkGrabberFilePackage(JDL.L("gui.linkgrabber.package.offline", "offline"), this);
        FP_OFFLINE.setIgnore(true);
        FP_FILTERED = new LinkGrabberFilePackage(JDL.L("gui.linkgrabber.package.filtered", "filtered"));
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
        }
        this.FP_FILTERED.setDownloadLinks(new ArrayList<DownloadLink>());
    }

    public void filterExtension(String ext, boolean b) {
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
            try {
                Pattern.compile(line.trim());
                ret.add(line.trim());
            } catch (Exception e) {
                logger.severe("Filter " + line.trim() + " is invalid!");
            }
        }
        return ret.toArray(new String[] {});
    }

    protected void finalize() {
        JDController.getInstance().removeControlListener(cpl);
        System.out.println("REMOVED LISTENER " + cpl);
    }

    public ArrayList<LinkGrabberFilePackage> getPackages() {
        return packages;
    }

    public int indexOf(LinkGrabberFilePackage fp) {
        return packages.indexOf(fp);
    }

    public boolean isExtensionFiltered(DownloadLink link) {
        synchronized (extensionFilter) {
            for (String ext : extensionFilter) {
                if (link.getName().endsWith(ext)) { return true; }
            }
        }
        return false;
    }

    public LinkGrabberFilePackage getFPwithName(String name) {
        synchronized (packages) {
            if (name == null) return null;
            for (LinkGrabberFilePackage fp : packages) {
                if (fp.getName().equalsIgnoreCase(name)) return fp;
            }
            if (FP_FILTERED.getName().equalsIgnoreCase(name)) return FP_FILTERED;
            return null;
        }
    }

    public LinkGrabberFilePackage getFPwithLink(DownloadLink link) {
        synchronized (packages) {
            if (link == null) return null;
            for (LinkGrabberFilePackage fp : packages) {
                if (fp.contains(link)) return fp;
            }
            if (FP_FILTERED.contains(link)) return FP_FILTERED;
            return null;
        }
    }

    public void postprocessing() {
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (packages) {
                ArrayList<LinkGrabberFilePackage> fps = new ArrayList<LinkGrabberFilePackage>(packages);
                for (LinkGrabberFilePackage fp : fps) {
                    boolean remove = false;
                    if (fp.countFailedLinks(true) == fp.size()) remove = true;
                    ArrayList<DownloadLink> links = new ArrayList<DownloadLink>(fp.getDownloadLinks());
                    for (DownloadLink dl : links) {
                        if (dl.isAvailabilityStatusChecked() && !dl.isAvailable() && (links.size() == 1 || remove)) {
                            FP_OFFLINE.add(dl);
                        }
                    }
                    Collections.sort(fp.getDownloadLinks(), new Comparator<DownloadLink>() {
                        public int compare(DownloadLink a, DownloadLink b) {
                            return a.getName().compareToIgnoreCase(b.getName());
                        }
                    });
                }
            }
            throwRefresh();
        }
    }

    public boolean isDupe(DownloadLink link) {
        synchronized (packages) {
            if (link == null) return false;
            if (link.getBooleanProperty("ALLOW_DUPE", false)) return false;
            for (LinkGrabberFilePackage fp : packages) {
                for (DownloadLink dl : fp.getDownloadLinks()) {
                    if (dl.compareTo(link) == 0) return true;
                }
            }
            return false;
        }
    }

    public void addPackage(LinkGrabberFilePackage fp) {
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (packages) {
                if (!packages.contains(fp)) {
                    packages.add(fp);
                    fp.addListener(this);
                    broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.ADD_FILEPACKAGE, fp));
                }
            }
        }
    }

    public void addAllAt(ArrayList<LinkGrabberFilePackage> links, int index) {
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (packages) {
                int repos = 0;
                for (int i = 0; i < links.size(); i++) {
                    repos = addPackageAt(links.get(i), index + i, repos);
                }
            }
        }
    }

    public int addPackageAt(LinkGrabberFilePackage fp, int index, int repos) {
        if (fp == null) return repos;
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (packages) {
                if (packages.size() == 0) {
                    addPackage(fp);
                    return repos;
                }
                boolean newadded = false;
                if (packages.contains(fp)) {
                    int posa = this.indexOf(fp);
                    if (posa < index) {
                        index -= ++repos;
                    }
                    packages.remove(fp);
                    if (index > packages.size() - 1) {
                        packages.add(fp);
                    } else if (index < 0) {
                        packages.add(0, fp);
                    } else
                        packages.add(index, fp);
                } else {
                    if (index > packages.size() - 1) {
                        packages.add(fp);
                    } else if (index < 0) {
                        packages.add(0, fp);
                    } else
                        packages.add(index, fp);
                }
                if (newadded) {
                    fp.addListener(this);
                    broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.ADD_FILEPACKAGE, fp));
                } else {
                    broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.REFRESH_STRUCTURE, fp));
                }
            }
        }
        return repos;
    }

    public void removePackage(LinkGrabberFilePackage fp) {
        if (fp == null) return;
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (packages) {
                if (fp != this.FP_FILTERED && fp != this.FP_OFFLINE && fp != this.FP_UNCHECKED && fp != this.FP_UNSORTED) fp.removeListener(this);
                packages.remove(fp);
                broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.REMOVE_FILEPACKAGE, fp));
            }
        }
    }

    public void throwRefresh() {
        broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.REFRESH_STRUCTURE));
    }

    public void throwFinished() {
        broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.FINISHED));
        GarbageController.requestGC();
    }

    public void filterPackages() {
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (packages) {
                ArrayList<LinkGrabberFilePackage> fps = new ArrayList<LinkGrabberFilePackage>(packages);
                fps.add(this.FP_FILTERED);
                for (LinkGrabberFilePackage fp : fps) {
                    if (fp == this.FP_UNCHECKED || fp == this.FP_OFFLINE || fp == this.FP_UNSORTED) continue;
                    ArrayList<DownloadLink> links = new ArrayList<DownloadLink>(fp.getDownloadLinks());
                    for (DownloadLink dl : links) {
                        // save old filepackagename, so we can put it in package
                        // with same name again
                        LinkGrabberFilePackage oldfp = getFPwithLink(dl);
                        if (oldfp != null && oldfp != FP_FILTERED) {
                            dl.setProperty("oldfp", oldfp.getName());
                        }
                        if (this.isExtensionFiltered(dl)) {
                            // attach to filterd, if its not already in it
                            if (!FP_FILTERED.contains(dl)) {
                                FP_FILTERED.add(dl);
                            }
                        } else {
                            // reattach to visible list
                            attachToPackagesSecondStage(dl);
                        }
                    }
                }
            }
        }
    }

    public void attachToPackagesFirstStage(DownloadLink link) {
        if (customizedpackager != null) {
            customizedpackager.attachToPackagesFirstStage(link);
        } else {
            attachToPackagesFirstStageInternal(link);
        }
    }

    public void attachToPackagesFirstStageInternal(DownloadLink link) {
        synchronized (LinkGrabberController.ControllerLock) {
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
                    fp.setDownloadDirectory(link.getFilePackage().getDownloadDirectory());
                    fp.setPassword(link.getFilePackage().getPassword());
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

    public void attachToPackagesSecondStage(DownloadLink link) {
        if (customizedpackager != null) {
            customizedpackager.attachToPackagesSecondStage(link);
        } else {
            attachToPackagesSecondStageInternal(link);
        }
    }

    public void attachToPackagesSecondStageInternal(DownloadLink link) {
        synchronized (LinkGrabberController.ControllerLock) {
            if (this.isExtensionFiltered(link)) {
                this.FP_FILTERED.add(link);
                return;
            }

            LinkGrabberFilePackage fp = getGeneratedPackage(link);
            fp.add(link);
        }
    }

    public LinkGrabberFilePackage getGeneratedPackage(DownloadLink link) {
        synchronized (LinkGrabberController.ControllerLock) {
            String packageName;
            boolean autoPackage = false;
            if (link.hasProperty("oldfp")) {
                /* get old packagename */
                packageName = link.getStringProperty("oldfp");
                link.setProperty("oldfp", Property.NULL);
            } else if (link.getFilePackage() != FilePackage.getDefaultFilePackage()) {
                if (link.getFilePackage().getStringProperty(DONTFORCEPACKAGENAME, null) != null) {
                    /* enable autopackaging even if filepackage is set */
                    autoPackage = true;
                    packageName = LinkGrabberPackager.cleanFileName(link.getName());
                } else {
                    packageName = link.getFilePackage().getName();
                }
            } else {
                autoPackage = true;
                packageName = LinkGrabberPackager.cleanFileName(link.getName());
            }

            int bestSim = 0;
            LinkGrabberFilePackage bestp = null;
            synchronized (packages) {
                for (int i = 0; i < packages.size(); i++) {
                    int sim = LinkGrabberPackager.comparepackages(packages.get(i).getName(), packageName);
                    if (sim > bestSim) {
                        bestSim = sim;
                        bestp = packages.get(i);
                    }
                }
            }

            if (bestSim < 99) {
                bestp = new LinkGrabberFilePackage(packageName, this);
            } else {
                String newPackageName = autoPackage ? getSimString(bestp.getName(), packageName) : packageName;
                bestp.setName(newPackageName);
            }

            return bestp;
        }
    }

    private String getSimString(String a, String b) {
        String aa = a.toLowerCase();
        String bb = b.toLowerCase();
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < Math.min(aa.length(), bb.length()); i++) {
            if (aa.charAt(i) == bb.charAt(i)) {
                ret.append(a.charAt(i));
            }
        }
        return ret.toString();
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
            if (!packages.contains(event.getSource())) {
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

    public void throwLinksAdded() {
        broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.ADDED));
    }

    public void onLinkGrabberControllerEvent(LinkGrabberControllerEvent event) {
        switch (event.getID()) {
        case LinkGrabberControllerEvent.ADD_FILEPACKAGE:
        case LinkGrabberControllerEvent.REMOVE_FILEPACKAGE:
            broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.REFRESH_STRUCTURE));
            break;
        case LinkGrabberControllerEvent.FILTER_CHANGED:
            filterPackages();
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

    @SuppressWarnings("unchecked")
    public void move(Object src2, Object dst, byte mode) {
        boolean type = false; /* false=downloadLink,true=LinkGrabberFilePackage */
        Object src = null;
        LinkGrabberFilePackage fp = null;
        if (src2 instanceof ArrayList<?>) {
            Object check = ((ArrayList<?>) src2).get(0);
            if (check == null) {
                logger.warning("Null src, cannot move!");
                return;
            }
            if (check instanceof DownloadLink) {
                src = src2;
                type = false;
            } else if (check instanceof LinkGrabberFilePackage) {
                src = src2;
                type = true;
            }
        } else if (src2 instanceof DownloadLink) {
            type = false;
            src = new ArrayList<DownloadLink>();
            ((ArrayList<DownloadLink>) src).add((DownloadLink) src2);
        } else if (src2 instanceof LinkGrabberFilePackage) {
            type = true;
            src = new ArrayList<LinkGrabberFilePackage>();
            ((ArrayList<LinkGrabberFilePackage>) src).add((LinkGrabberFilePackage) src2);
        }
        if (src == null) {
            logger.warning("Unknown src, cannot move!");
            return;
        }
        synchronized (ControllerLock) {
            synchronized (packages) {
                if (dst != null) {
                    if (!type) {
                        if (dst instanceof LinkGrabberFilePackage) {
                            /* src:DownloadLinks dst:LinkGrabberFilePackage */
                            switch (mode) {
                            case MOVE_BEGIN:
                                fp = ((LinkGrabberFilePackage) dst);
                                fp.addAllAt((ArrayList<DownloadLink>) src, 0);
                                return;
                            case MOVE_END:
                                fp = ((LinkGrabberFilePackage) dst);
                                fp.addAllAt((ArrayList<DownloadLink>) src, fp.size());
                                return;
                            default:
                                logger.warning("Unsupported mode, cannot move!");
                                return;
                            }
                        } else if (dst instanceof DownloadLink) {
                            /* src:DownloadLinks dst:DownloadLinks */
                            switch (mode) {
                            case MOVE_BEFORE:
                                fp = getFPwithLink((DownloadLink) dst);
                                fp.addAllAt((ArrayList<DownloadLink>) src, fp.indexOf((DownloadLink) dst));
                                return;
                            case MOVE_AFTER:
                                fp = getFPwithLink((DownloadLink) dst);
                                fp.addAllAt((ArrayList<DownloadLink>) src, fp.indexOf((DownloadLink) dst) + 1);
                                return;
                            default:
                                logger.warning("Unsupported mode, cannot move!");
                                return;
                            }
                        } else {
                            logger.warning("Unsupported dst, cannot move!");
                            return;
                        }
                    } else {
                        if (dst instanceof LinkGrabberFilePackage) {
                            /*
                             * src:LinkGrabberFilePackage
                             * dst:LinkGrabberFilePackage
                             */
                            switch (mode) {
                            case MOVE_BEFORE:
                                addAllAt((ArrayList<LinkGrabberFilePackage>) src, indexOf((LinkGrabberFilePackage) dst));
                                return;
                            case MOVE_AFTER:
                                addAllAt((ArrayList<LinkGrabberFilePackage>) src, indexOf((LinkGrabberFilePackage) dst) + 1);
                                return;
                            default:
                                logger.warning("Unsupported mode, cannot move!");
                                return;
                            }
                        } else if (dst instanceof DownloadLink) {
                            /* src:LinkGrabberFilePackage dst:DownloadLinks */
                            logger.warning("Unsupported mode, cannot move!");
                            return;
                        }
                    }
                } else {
                    /* dst==null, global moving */
                    if (type) {
                        /* src:LinkGrabberFilePackage */
                        switch (mode) {
                        case MOVE_UP: {
                            int curpos = 0;
                            for (LinkGrabberFilePackage item : (ArrayList<LinkGrabberFilePackage>) src) {
                                curpos = indexOf(item);
                                addPackageAt(item, curpos - 1, 0);
                            }
                        }
                            return;
                        case MOVE_DOWN: {
                            int curpos = 0;
                            ArrayList<LinkGrabberFilePackage> fps = ((ArrayList<LinkGrabberFilePackage>) src);
                            for (int i = fps.size() - 1; i >= 0; i--) {
                                curpos = indexOf(fps.get(i));
                                addPackageAt(fps.get(i), curpos + 2, 0);
                            }
                        }
                            return;
                        case MOVE_TOP:
                            addAllAt((ArrayList<LinkGrabberFilePackage>) src, 0);
                            return;
                        case MOVE_BOTTOM:
                            addAllAt((ArrayList<LinkGrabberFilePackage>) src, size() + 1);
                            return;
                        default:
                            logger.warning("Unsupported mode, cannot move!");
                            return;
                        }
                    } else {
                        /* src:DownloadLinks */
                        switch (mode) {
                        case MOVE_UP: {
                            int curpos = 0;
                            for (DownloadLink item : (ArrayList<DownloadLink>) src) {
                                fp = getFPwithLink(item);
                                curpos = fp.indexOf(item);
                                fp.add(curpos - 1, item, 0);
                                if (curpos == 0) {
                                    curpos = indexOf(fp);
                                    addPackageAt(fp, curpos - 1, 0);
                                }
                            }
                        }
                            return;
                        case MOVE_DOWN: {
                            int curpos = 0;
                            ArrayList<DownloadLink> links = ((ArrayList<DownloadLink>) src);
                            for (int i = links.size() - 1; i >= 0; i--) {
                                fp = getFPwithLink(links.get(i));
                                curpos = fp.indexOf(links.get(i));
                                fp.add(curpos + 2, links.get(i), 0);
                                if (curpos == fp.size() - 1) {
                                    curpos = indexOf(fp);
                                    addPackageAt(fp, curpos + 2, 0);
                                }
                            }
                        }
                            return;
                        case MOVE_TOP: {
                            ArrayList<ArrayList<DownloadLink>> split = splitByFilePackage((ArrayList<DownloadLink>) src);
                            for (ArrayList<DownloadLink> links : split) {
                                fp = getFPwithLink(links.get(0));
                                if (fp.indexOf(links.get(0)) == 0) {
                                    addPackageAt(fp, 0, 0);
                                }
                                fp.addAllAt(links, 0);
                            }
                        }
                            return;
                        case MOVE_BOTTOM: {
                            ArrayList<ArrayList<DownloadLink>> split = splitByFilePackage((ArrayList<DownloadLink>) src);
                            for (ArrayList<DownloadLink> links : split) {
                                fp = getFPwithLink(links.get(0));
                                if (fp.indexOf(links.get(links.size() - 1)) == fp.size() - 1) {
                                    addPackageAt(fp, size() + 1, 0);
                                }
                                fp.addAllAt(links, fp.size() + 1);
                            }
                        }
                            return;
                        default:
                            logger.warning("Unsupported mode, cannot move!");
                            return;
                        }
                    }
                }
            }
        }
    }

    public static ArrayList<ArrayList<DownloadLink>> splitByFilePackage(ArrayList<DownloadLink> links) {
        ArrayList<ArrayList<DownloadLink>> ret = new ArrayList<ArrayList<DownloadLink>>();
        boolean added = false;
        for (DownloadLink link : links) {
            LinkGrabberFilePackage fp1 = INSTANCE.getFPwithLink(link);
            if (ret.size() == 0) {
                ArrayList<DownloadLink> tmp = new ArrayList<DownloadLink>();
                tmp.add(link);
                ret.add(tmp);
            } else {
                added = false;
                for (ArrayList<DownloadLink> check : ret) {
                    LinkGrabberFilePackage fp2 = INSTANCE.getFPwithLink(check.get(0));
                    if (fp1 == fp2) {
                        added = true;
                        check.add(link);
                    }
                }
                if (added == false) {
                    ArrayList<DownloadLink> tmp = new ArrayList<DownloadLink>();
                    tmp.add(link);
                    ret.add(tmp);
                }
            }
        }
        return ret;
    }
}
