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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.Timer;

import jd.Main;
import jd.config.Configuration;
import jd.event.JDBroadcaster;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageEvent;
import jd.plugins.FilePackageListener;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;
import jd.utils.JDUtilities;

class DownloadControllerBroadcaster extends JDBroadcaster<DownloadControllerListener, DownloadControllerEvent> {

    protected void fireEvent(final DownloadControllerListener listener, final DownloadControllerEvent event) {
        listener.onDownloadControllerEvent(event);
    }
}

class Optimizer {

    private final HashMap<String, ArrayList<DownloadLink>> url_links = new HashMap<String, ArrayList<DownloadLink>>();

    private final Object lock = new Object();

    private static Optimizer INSTANCE = null;

    private DownloadController INSTANCE2 = null;

    public synchronized static Optimizer getINSTANCE(final DownloadController INSTANCE2) {
        if (INSTANCE == null) {
            INSTANCE = new Optimizer(INSTANCE2);
        }
        return INSTANCE;
    }

    private Optimizer(final DownloadController INSTANCE2) {
        this.INSTANCE2 = INSTANCE2;
        init();
    }

    private void init() {
        final ArrayList<DownloadLink> links = INSTANCE2.getAllDownloadLinks();
        for (final DownloadLink link : links) {
            final String url = link.getDownloadURL().trim();
            if (url != null) {
                if (!url_links.containsKey(url)) {
                    url_links.put(url, new ArrayList<DownloadLink>());
                }
                final ArrayList<DownloadLink> tmp = url_links.get(url);
                if (!tmp.contains(link)) {
                    tmp.add(link);
                }
            }
        }
    }

    public ArrayList<DownloadLink> getLinkswithURL(final String url) {
        if (url == null || url.length() == 0) return null;
        synchronized (lock) {
            return url_links.get(url.trim());
        }
    }
}

public class DownloadController implements FilePackageListener, DownloadControllerListener, ActionListener {

    public static final byte MOVE_BEFORE = 1;
    public static final byte MOVE_AFTER = 2;
    public static final byte MOVE_BEGIN = 3;
    public static final byte MOVE_END = 4;
    public static final byte MOVE_TOP = 5;
    public static final byte MOVE_BOTTOM = 6;
    public static final byte MOVE_UP = 7;
    public static final byte MOVE_DOWN = 8;

    public final static Object ControllerLock = new Object();

    private static DownloadController INSTANCE = new DownloadController();

    private ArrayList<FilePackage> packages = new ArrayList<FilePackage>();

    private Logger logger = null;

    private JDController controller;

    // private Optimizer optimizer;

    private transient DownloadControllerBroadcaster broadcaster = new DownloadControllerBroadcaster();

    private Timer asyncSaveIntervalTimer; /*
                                           * Async-Save, Linkliste wird
                                           * verzögert gespeichert
                                           */

    private boolean saveinprogress;

    public static DownloadController getInstance() {
        /* darf erst nachdem der JDController init wurde, aufgerufen werden */
        return INSTANCE;
    }

    private DownloadController() {
        logger = jd.controlling.JDLogger.getLogger();
        controller = JDUtilities.getController();
        initDownloadLinks();
        asyncSaveIntervalTimer = new Timer(2000, this);
        asyncSaveIntervalTimer.setInitialDelay(2000);
        asyncSaveIntervalTimer.setRepeats(false);
        // optimizer = Optimizer.getINSTANCE(this);
        broadcaster.addListener(this);
    }

    public void addListener(final DownloadControllerListener l) {
        broadcaster.addListener(l);
    }

    public void removeListener(final DownloadControllerListener l) {
        broadcaster.removeListener(l);
    }

    /**
     * Lädt zum Start das erste Mal alle Links aus einer Datei
     * 
     * @return true/False je nach Erfolg
     */
    private void initDownloadLinks() {
        try {
            packages = loadDownloadLinks();
        } catch (Exception e) {
            packages = null;
        }
        if (packages == null) {
            packages = new ArrayList<FilePackage>();
            final File file = JDUtilities.getResourceFile("backup/links.linkbackup");
            if (file.exists()) {
                logger.warning("Strange: No Linklist,Try to restore from backup file");
                controller.loadContainerFile(file);
            }
            return;
        } else if (packages.size() == 0 && Main.returnedfromUpdate()) {
            final File file = JDUtilities.getResourceFile("backup/links.linkbackup");
            if (file.exists() && file.lastModified() >= System.currentTimeMillis() - 10 * 60 * 1000l) {
                logger.warning("Strange: Empty Linklist,Try to restore from backup file");
                controller.loadContainerFile(file);
            }
            return;
        }
        for (final FilePackage filePackage : packages) {
            filePackage.addListener(this);
            filePackage.update_linksDisabled();
            for (DownloadLink downloadLink : filePackage.getDownloadLinkList()) {
                downloadLink.getBroadcaster().addListener(filePackage);
            }
        }
        return;
    }

    public void saveDownloadLinksAsync() {
        if (saveinprogress) return;
        asyncSaveIntervalTimer.setDelay(2 * 1000);
        asyncSaveIntervalTimer.setInitialDelay(2 * 1000);
        asyncSaveIntervalTimer.restart();
    }

    /**
     * Speichert die Linksliste ab
     * 
     * @param file
     *            Die Datei, in die die Links gespeichert werden sollen
     */
    public void saveDownloadLinksSync() {
        if (saveinprogress) return;
        new Thread() {
            public void run() {
                this.setName("DownloadController: Saving");
                saveDownloadLinksSyncnonThread();
                asyncSaveIntervalTimer.setDelay(10 * 60 * 1000);
                asyncSaveIntervalTimer.setInitialDelay(10 * 60 * 1000);
                asyncSaveIntervalTimer.restart();
            }
        }.start();
    }

    public void saveDownloadLinksSyncnonThread() {
        asyncSaveIntervalTimer.stop();
        final String id = JDController.requestDelayExit("downloadcontroller");
        synchronized (packages) {
            saveinprogress = true;
            JDUtilities.getDatabaseConnector().saveLinks(packages);
            saveinprogress = false;
        }
        JDController.releaseDelayExit(id);
    }

    /**
     * Lädt eine LinkListe
     * 
     * @param file
     *            Die Datei, aus der die Links gelesen werden
     * @return Ein neuer ArrayList mit den DownloadLinks
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private ArrayList<FilePackage> loadDownloadLinks() throws Exception {
        final Object obj = JDUtilities.getDatabaseConnector().getLinks();
        if (obj != null && obj instanceof ArrayList && (((ArrayList) obj).size() == 0 || ((ArrayList) obj).size() > 0 && ((ArrayList) obj).get(0) instanceof FilePackage)) {
            final ArrayList<FilePackage> packages = (ArrayList<FilePackage>) obj;
            final Iterator<FilePackage> iterator = packages.iterator();
            DownloadLink localLink;
            PluginForHost pluginForHost = null;
            PluginsC pluginForContainer = null;
            // String tmp2 = null;
            Iterator<DownloadLink> it;
            FilePackage fp;
            while (iterator.hasNext()) {
                fp = iterator.next();
                if (fp.getDownloadLinkList().size() == 0) {
                    iterator.remove();
                    continue;
                }
                it = fp.getDownloadLinkList().iterator();
                while (it.hasNext()) {
                    localLink = it.next();
                    /*
                     * reset not if already exist, offline or finished. plugin
                     * errors will be reset here because plugin can be fixed
                     * again
                     */
                    localLink.getLinkStatus().resetStatus(LinkStatus.ERROR_ALREADYEXISTS, LinkStatus.ERROR_FILE_NOT_FOUND, LinkStatus.FINISHED, LinkStatus.ERROR_FATAL);

                    if (localLink.getLinkStatus().isFinished() && JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION, 3) == 1) {
                        it.remove();
                        if (fp.getDownloadLinkList().size() == 0) {
                            iterator.remove();
                            continue;
                        }
                    } else {
                        // Anhand des Hostnamens aus dem DownloadLink
                        // wird ein passendes Plugin gesucht
                        try {
                            pluginForHost = JDUtilities.getNewPluginForHostInstance(localLink.getHost());
                        } catch (Exception e) {
                            JDLogger.exception(e);
                        }
                        // Gibt es einen Names für ein Containerformat,
                        // wird ein passendes Plugin gesucht
                        try {
                            if (localLink.getContainer() != null) {
                                pluginForContainer = JDUtilities.getPluginForContainer(localLink.getContainer(), localLink.getContainerFile());
                                if (pluginForContainer == null) {
                                    localLink.setEnabled(false);
                                }
                            }
                        } catch (NullPointerException e) {
                            JDLogger.exception(e);
                        }
                        if (pluginForHost != null) {
                            localLink.setLoadedPlugin(pluginForHost);
                        }
                        if (pluginForContainer != null) {
                            localLink.setLoadedPluginForContainer(pluginForContainer);
                        }
                        if (pluginForHost == null) {
                            logger.severe("couldn't find plugin(" + localLink.getHost() + ") for this DownloadLink." + localLink.getName());
                        }
                    }
                }
                fp.resetUpdateTimer();
            }
            return packages;
        }
        throw new Exception("Linklist incompatible");
    }

    public ArrayList<FilePackage> getPackages() {
        return packages;
    }

    public void addAll(final ArrayList<FilePackage> links) {
        synchronized (DownloadController.ControllerLock) {
            synchronized (packages) {
                final int linksSize = links.size();
                for (int i = 0; i < linksSize; i++) {
                    addPackage(links.get(i));
                }
            }
        }
    }

    public void addPackage(final FilePackage fp) {
        if (fp != null) {
            synchronized (DownloadController.ControllerLock) {
                synchronized (packages) {
                    if (!packages.contains(fp)) {
                        fp.addListener(this);
                        packages.add(fp);
                        broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.ADD_FILEPACKAGE, fp));
                    }
                }
            }
        }
    }

    public int indexOf(final FilePackage fp) {
        return packages.indexOf(fp);
    }

    public int addPackageAt(final FilePackage fp, int index, int repos) {
        if (fp != null) {
            synchronized (DownloadController.ControllerLock) {
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
                        newadded = true;
                    }
                    if (newadded) {
                        fp.addListener(this);
                        broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.ADD_FILEPACKAGE, fp));
                    } else {
                        broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.REFRESH_STRUCTURE));
                    }
                }
            }
        }
        return repos;
    }

    public void addAllAt(final ArrayList<FilePackage> links, final int index) {
        synchronized (DownloadController.ControllerLock) {
            synchronized (packages) {
                int repos = 0;
                final int linksSize = links.size();
                for (int i = 0; i < linksSize; i++) {
                    repos = addPackageAt(links.get(i), index + i, repos);
                }
            }
        }
    }

    public void removePackage(final FilePackage fp2) {
        if (fp2 != null) {
            synchronized (DownloadController.ControllerLock) {
                synchronized (packages) {
                    fp2.abortDownload();
                    fp2.removeListener(this);
                    if (packages.remove(fp2)) {
                        broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.REMOVE_FILPACKAGE, fp2));
                    }
                }
            }
        }
    }

    public int size() {
        return packages.size();
    }

    /**
     * Liefert alle DownloadLinks zurück
     * 
     * @return Alle DownloadLinks zurück
     */
    public ArrayList<DownloadLink> getAllDownloadLinks() {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        synchronized (packages) {
            for (final FilePackage fp : packages) {
                ret.addAll(fp.getDownloadLinkList());
            }
        }
        return ret;
    }

    /**
     * Returns all needful downloadinformation
     */
    public void getDownloadStatus(final DownloadInformations ds) {
        ds.reset();
        ds.addRunningDownloads(DownloadWatchDog.getInstance().getActiveDownloads());
        synchronized (packages) {
            LinkStatus linkStatus;
            boolean isEnabled;
            for (final FilePackage fp : packages) {
                ds.addPackages(1);
                ds.addDownloadLinks(fp.getDownloadLinkList().size());
                for (final DownloadLink l : fp.getDownloadLinkList()) {
                    linkStatus = l.getLinkStatus();
                    isEnabled = l.isEnabled();
                    if (!linkStatus.hasStatus(LinkStatus.ERROR_ALREADYEXISTS) && isEnabled) {
                        ds.addTotalDownloadSize(l.getDownloadSize());
                        ds.addCurrentDownloadSize(l.getDownloadCurrent());
                    }

                    if (linkStatus.hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) {
                        ds.addDuplicateDownloads(1);
                    } else if (!isEnabled) {
                        ds.addDisabledDownloads(1);
                    } else if (linkStatus.hasStatus(LinkStatus.FINISHED)) {
                        ds.addFinishedDownloads(1);
                    }
                }
            }
        }
    }

    // Den Optimizer muss ich noch fertig machen
    // public boolean hasDownloadLinkwithURL(String url) {
    // if (optimizer.getLinkswithURL(url) != null ||
    // optimizer.getLinkswithURL(url).size() == 0) { return true; }
    // return false;
    // }

    public boolean hasDownloadLinkwithURL(final String url) {
        if (url != null) {
            final String correctUrl = url.trim();
            for (final DownloadLink dl : getAllDownloadLinks()) {
                if (correctUrl.equalsIgnoreCase(dl.getDownloadURL())) return true;
            }
        }
        return false;
    }

    public DownloadLink getFirstLinkThatBlocks(final DownloadLink link) {
        for (final DownloadLink nextDownloadLink : getAllDownloadLinks()) {
            if (nextDownloadLink != link) {
                if ((nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) && nextDownloadLink.getFileOutput().equalsIgnoreCase(link.getFileOutput())) {
                    if (new File(nextDownloadLink.getFileOutput()).exists()) {
                        /*
                         * fertige datei sollte auch auf der platte sein und
                         * nicht nur als fertig in der liste
                         */
                        return nextDownloadLink;
                    }
                }
                if (nextDownloadLink.getLinkStatus().isPluginInProgress() && nextDownloadLink.getFileOutput().equalsIgnoreCase(link.getFileOutput())) return nextDownloadLink;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void move(Object src2, Object dst, final byte mode) {
        boolean type = false; /* false=downloadLink,true=filepackage */
        Object src = null;
        FilePackage fp = null;
        if (src2 instanceof ArrayList<?>) {
            if (((ArrayList<?>) src2).isEmpty()) return;
            Object check = ((ArrayList<?>) src2).get(0);
            if (check == null) {
                logger.warning("Null src, cannot move!");
                return;
            }
            if (check instanceof DownloadLink) {
                src = src2;
                type = false;
            } else if (check instanceof FilePackage) {
                src = src2;
                type = true;
            }
        } else if (src2 instanceof DownloadLink) {
            type = false;
            src = new ArrayList<DownloadLink>();
            ((ArrayList<DownloadLink>) src).add((DownloadLink) src2);
        } else if (src2 instanceof FilePackage) {
            type = true;
            src = new ArrayList<FilePackage>();
            ((ArrayList<FilePackage>) src).add((FilePackage) src2);
        }
        if (src == null) {
            logger.warning("Unknown src, cannot move!");
            return;
        }
        synchronized (DownloadController.ControllerLock) {
            synchronized (packages) {
                if (dst != null) {
                    if (!type) {
                        if (dst instanceof FilePackage) {
                            /* src:DownloadLinks dst:filepackage */
                            switch (mode) {
                            case MOVE_BEGIN:
                                fp = ((FilePackage) dst);
                                fp.addLinksAt((ArrayList<DownloadLink>) src, 0);
                                return;
                            case MOVE_END:
                                fp = ((FilePackage) dst);
                                fp.addLinksAt((ArrayList<DownloadLink>) src, fp.size());
                                return;
                            default:
                                logger.warning("Unsupported mode, cannot move!");
                                return;
                            }
                        } else if (dst instanceof DownloadLink) {
                            /* src:DownloadLinks dst:DownloadLinks */
                            switch (mode) {
                            case MOVE_BEFORE:
                                fp = ((DownloadLink) dst).getFilePackage();
                                fp.addLinksAt((ArrayList<DownloadLink>) src, fp.indexOf((DownloadLink) dst));
                                return;
                            case MOVE_AFTER:
                                fp = ((DownloadLink) dst).getFilePackage();
                                fp.addLinksAt((ArrayList<DownloadLink>) src, fp.indexOf((DownloadLink) dst) + 1);
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
                        if (dst instanceof FilePackage) {
                            /* src:FilePackages dst:filepackage */
                            switch (mode) {
                            case MOVE_BEFORE:
                                addAllAt((ArrayList<FilePackage>) src, indexOf((FilePackage) dst));
                                return;
                            case MOVE_AFTER:
                                addAllAt((ArrayList<FilePackage>) src, indexOf((FilePackage) dst) + 1);
                                return;
                            default:
                                logger.warning("Unsupported mode, cannot move!");
                                return;
                            }
                        } else if (dst instanceof DownloadLink) {
                            /* src:FilePackages dst:DownloadLinks */
                            logger.warning("Unsupported mode, cannot move!");
                            return;
                        }
                    }
                } else {
                    /* dst==null, global moving */
                    if (type) {
                        /* src:FilePackages */
                        switch (mode) {
                        case MOVE_UP: {
                            int curpos = 0;
                            for (final FilePackage item : (ArrayList<FilePackage>) src) {
                                curpos = indexOf(item);
                                addPackageAt(item, curpos - 1, 0);
                            }
                        }
                            return;
                        case MOVE_DOWN: {
                            int curpos = 0;
                            final ArrayList<FilePackage> fps = ((ArrayList<FilePackage>) src);
                            for (int i = fps.size() - 1; i >= 0; i--) {
                                curpos = indexOf(fps.get(i));
                                addPackageAt(fps.get(i), curpos + 2, 0);
                            }
                        }
                            return;
                        case MOVE_TOP:
                            addAllAt((ArrayList<FilePackage>) src, 0);
                            return;
                        case MOVE_BOTTOM:
                            addAllAt((ArrayList<FilePackage>) src, size() + 1);
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
                            for (final DownloadLink item : (ArrayList<DownloadLink>) src) {
                                curpos = item.getFilePackage().indexOf(item);
                                item.getFilePackage().add(curpos - 1, item, 0);
                                if (curpos == 0) {
                                    curpos = indexOf(item.getFilePackage());
                                    addPackageAt(item.getFilePackage(), curpos - 1, 0);
                                }
                            }
                        }
                            return;
                        case MOVE_DOWN: {
                            int curpos = 0;
                            final ArrayList<DownloadLink> links = ((ArrayList<DownloadLink>) src);
                            for (int i = links.size() - 1; i >= 0; i--) {
                                curpos = links.get(i).getFilePackage().indexOf(links.get(i));
                                links.get(i).getFilePackage().add(curpos + 2, links.get(i), 0);
                                if (curpos == links.get(i).getFilePackage().size() - 1) {
                                    curpos = indexOf(links.get(i).getFilePackage());
                                    addPackageAt(links.get(i).getFilePackage(), curpos + 2, 0);
                                }
                            }
                        }
                            return;
                        case MOVE_TOP: {
                            final ArrayList<ArrayList<DownloadLink>> split = splitByFilePackage((ArrayList<DownloadLink>) src);
                            for (final ArrayList<DownloadLink> links : split) {
                                if (links.get(0).getFilePackage().indexOf(links.get(0)) == 0) {
                                    addPackageAt(links.get(0).getFilePackage(), 0, 0);
                                }
                                links.get(0).getFilePackage().addLinksAt(links, 0);
                            }
                        }
                            return;
                        case MOVE_BOTTOM: {
                            final ArrayList<ArrayList<DownloadLink>> split = splitByFilePackage((ArrayList<DownloadLink>) src);
                            for (final ArrayList<DownloadLink> links : split) {
                                if (links.get(0).getFilePackage().indexOf(links.get(links.size() - 1)) == links.get(0).getFilePackage().size() - 1) {
                                    addPackageAt(links.get(0).getFilePackage(), size() + 1, 0);
                                }
                                links.get(0).getFilePackage().addLinksAt(links, links.get(0).getFilePackage().size() + 1);
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

    public static ArrayList<ArrayList<DownloadLink>> splitByFilePackage(final ArrayList<DownloadLink> links) {
        final ArrayList<ArrayList<DownloadLink>> ret = new ArrayList<ArrayList<DownloadLink>>();
        boolean added = false;
        for (final DownloadLink link : links) {
            if (ret.size() == 0) {
                final ArrayList<DownloadLink> tmp = new ArrayList<DownloadLink>();
                tmp.add(link);
                ret.add(tmp);
            } else {
                added = false;
                for (final ArrayList<DownloadLink> check : ret) {
                    if (link.getFilePackage() == check.get(0).getFilePackage()) {
                        added = true;
                        check.add(link);
                    }
                }
                if (added == false) {
                    final ArrayList<DownloadLink> tmp = new ArrayList<DownloadLink>();
                    tmp.add(link);
                    ret.add(tmp);
                }
            }
        }
        return ret;
    }

    public void actionPerformed(final ActionEvent arg0) {
        if (arg0.getSource() == asyncSaveIntervalTimer) {
            this.saveDownloadLinksSync();
        }
    }

    public void fireStructureUpdate() {
        /* speichern der downloadliste + aktuallisierung der gui */
        broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.REFRESH_STRUCTURE));
    }

    public void fireGlobalUpdate() {
        broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.REFRESH_ALL));
    }

    public void fireDownloadLinkUpdate(final Object param) {
        broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.REFRESH_SPECIFIC, param));
    }

    public void onDownloadControllerEvent(final DownloadControllerEvent event) {
        switch (event.getID()) {
        case DownloadControllerEvent.ADD_DOWNLOADLINK:
        case DownloadControllerEvent.REMOVE_DOWNLOADLINK:
        case DownloadControllerEvent.ADD_FILEPACKAGE:
        case DownloadControllerEvent.REMOVE_FILPACKAGE:
            fireStructureUpdate();
            break;
        case DownloadControllerEvent.REFRESH_STRUCTURE:
            this.saveDownloadLinksAsync();
            break;
        }
    }

    public void onFilePackageEvent(final FilePackageEvent event) {
        switch (event.getID()) {
        case FilePackageEvent.DOWNLOADLINK_ADDED:
            broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.ADD_DOWNLOADLINK, event.getParameter()));
            break;
        case FilePackageEvent.DOWNLOADLINK_REMOVED:
            broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.REMOVE_DOWNLOADLINK, event.getParameter()));
            break;
        case FilePackageEvent.FILEPACKAGE_UPDATE:
            this.fireStructureUpdate();
            break;
        case FilePackageEvent.FILEPACKAGE_EMPTY:
            this.removePackage((FilePackage) event.getSource());
            break;
        }
    }
}
