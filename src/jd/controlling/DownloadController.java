package jd.controlling;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.Timer;

import jd.Main;
import jd.config.Configuration;
import jd.event.JDBroadcaster;
import jd.nutils.io.JDIO;
import jd.plugins.BackupLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageEvent;
import jd.plugins.FilePackageListener;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;
import jd.update.PackageData;
import jd.utils.JDUtilities;

class DownloadControllerBroadcaster extends JDBroadcaster<DownloadControllerListener, DownloadControllerEvent> {

    protected void fireEvent(DownloadControllerListener listener, DownloadControllerEvent event) {
        listener.onDownloadControllerEvent(event);
    }
}

class Optimizer {

    private HashMap<String, ArrayList<DownloadLink>> url_links = new HashMap<String, ArrayList<DownloadLink>>();

    private Object lock = new Object();

    private static Optimizer INSTANCE = null;

    private DownloadController INSTANCE2 = null;

    public synchronized static Optimizer getINSTANCE(DownloadController INSTANCE2) {
        if (INSTANCE == null) INSTANCE = new Optimizer(INSTANCE2);
        return INSTANCE;
    }

    private Optimizer(DownloadController INSTANCE2) {
        this.INSTANCE2 = INSTANCE2;
        init();
    }

    private void init() {
        Vector<DownloadLink> links = INSTANCE2.getAllDownloadLinks();
        for (DownloadLink link : links) {
            String url = link.getDownloadURL().trim();
            if (url != null) {
                if (!url_links.containsKey(url)) {
                    url_links.put(url, new ArrayList<DownloadLink>());
                }
                ArrayList<DownloadLink> tmp = url_links.get(url);
                if (!tmp.contains(link)) tmp.add(link);
            }
        }
    }

    public ArrayList<DownloadLink> getLinkswithURL(String url) {
        if (url == null || url.length() == 0) return null;
        synchronized (lock) {
            return url_links.get(url.trim());
        }
    }
}

public class DownloadController implements FilePackageListener, DownloadControllerListener, ActionListener {

    private static DownloadController INSTANCE = null;

    private Vector<FilePackage> packages = new Vector<FilePackage>();

    private Logger logger = null;

    private JDController controller;

    // private Optimizer optimizer;

    private transient DownloadControllerBroadcaster broadcaster = new DownloadControllerBroadcaster();

    private Timer asyncSaveIntervalTimer; /*
                                           * Async-Save, Linkliste wird
                                           * verzögert gespeichert
                                           */

    private boolean saveinprogress;

    public synchronized static DownloadController getInstance() {
        /* darf erst nachdem der JDController init wurde, aufgerufen werden */
        if (INSTANCE == null) {
            INSTANCE = new DownloadController();
        }
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

    public void addListener(DownloadControllerListener l) {
        broadcaster.addListener(l);
    }

    public void removeListener(DownloadControllerListener l) {
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
            jd.controlling.JDLogger.getLogger().severe("" + e.getStackTrace());
            packages = null;
        }
        if (packages == null) {
            packages = new Vector<FilePackage>();
            File file = JDUtilities.getResourceFile("backup/links.linkbackup");
            if (file.exists()) {
                logger.warning("Strange: No Linklist,Try to restore from backup file");
                controller.loadContainerFile(file);
            }
            return;
        } else if (packages.size() == 0 && Main.returnedfromUpdate()) {
            File file = JDUtilities.getResourceFile("backup/links.linkbackup");
            if (file.exists() && file.lastModified() >= System.currentTimeMillis() - 10 * 60 * 1000l) {
                logger.warning("Strange: Empty Linklist,Try to restore from backup file");
                controller.loadContainerFile(file);
            }
            return;
        }
        for (FilePackage filePackage : packages) {
            filePackage.getBroadcaster().addListener(this);
            filePackage.update_linksDisabled();
            for (DownloadLink downloadLink : filePackage.getDownloadLinks()) {
                // downloadLink.setProperty(DownloadTreeTable.PROPERTY_SELECTED,
                // false);
                downloadLink.getBroadcaster().addListener(filePackage);
            }
        }
        return;
    }

    public void saveDownloadLinksAsync() {
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
                saveinprogress = true;
                synchronized (packages) {
                    JDUtilities.getDatabaseConnector().saveLinks(packages);
                }
                saveinprogress = false;
            }
        }.start();
    }

    public void saveDownloadLinksSyncnonThread() {
        synchronized (packages) {
            JDUtilities.getDatabaseConnector().saveLinks(packages);
        }
    }

    public void backupDownloadLinks() {
        synchronized (packages) {
            Vector<DownloadLink> links = getAllDownloadLinks();
            Iterator<DownloadLink> it = links.iterator();
            ArrayList<BackupLink> ret = new ArrayList<BackupLink>();
            while (it.hasNext()) {
                DownloadLink next = it.next();
                BackupLink bl;
                if (next.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) {
                    bl = (new BackupLink(JDUtilities.getResourceFile(next.getContainerFile()), next.getContainerIndex(), next.getContainer()));

                } else {
                    bl = (new BackupLink(next.getDownloadURL()));
                }
                bl.setProperty("downloaddirectory", next.getFilePackage().getDownloadDirectory());
                bl.setProperty("packagename", next.getFilePackage().getName());
                bl.setProperty("plugin", next.getPlugin().getClass().getSimpleName());
                bl.setProperty("name", new File(next.getFileOutput()).getName());
                bl.setProperty("properties", next.getProperties());
                bl.setProperty("enabled", next.isEnabled());

                ret.add(bl);
            }
            if (ret.size() == 0) return;
            File file = JDUtilities.getResourceFile("backup/links.linkbackup");
            if (file.exists()) {
                File old = JDUtilities.getResourceFile("backup/links_" + file.lastModified() + ".linkbackup");

                file.getParentFile().mkdirs();
                if (file.exists()) {
                    file.renameTo(old);
                }
                file.delete();
            } else {
                file.getParentFile().mkdirs();
            }
            JDIO.saveObject(null, ret, file, "links.linkbackup", "linkbackup", false);
        }
    }

    /**
     * Lädt eine LinkListe
     * 
     * @param file
     *            Die Datei, aus der die Links gelesen werden
     * @return Ein neuer Vector mit den DownloadLinks
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private Vector<FilePackage> loadDownloadLinks() throws Exception {
        Object obj = JDUtilities.getDatabaseConnector().getLinks();
        if (obj != null && obj instanceof Vector && (((Vector) obj).size() == 0 || ((Vector) obj).size() > 0 && ((Vector) obj).get(0) instanceof FilePackage)) {
            Vector<FilePackage> packages = (Vector<FilePackage>) obj;
            Iterator<FilePackage> iterator = packages.iterator();
            DownloadLink localLink;
            PluginForHost pluginForHost = null;
            PluginsC pluginForContainer = null;
            String tmp1 = null;
            String tmp2 = null;
            Iterator<DownloadLink> it;
            FilePackage fp;
            while (iterator.hasNext()) {
                fp = iterator.next();
                if (fp.getDownloadLinks().size() == 0) {
                    iterator.remove();
                    continue;
                }
                it = fp.getDownloadLinks().iterator();
                while (it.hasNext()) {
                    localLink = it.next();
                    if (localLink.getLinkType() == DownloadLink.LINKTYPE_JDU && (localLink.getProperty("JDU") == null || !(localLink.getProperty("JDU") instanceof PackageData))) {
                        iterator.remove();
                        continue;
                    }
                    if (!localLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                        tmp1 = localLink.getLinkStatus().getStatusText();
                        tmp2 = localLink.getLinkStatus().getErrorMessage();
                        localLink.getLinkStatus().reset();
                        localLink.getLinkStatus().setErrorMessage(tmp2);
                        localLink.getLinkStatus().setStatusText(tmp1);
                    }
                    if (localLink.getLinkStatus().hasStatus(LinkStatus.FINISHED) && JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION) == 1) {
                        it.remove();
                        if (fp.getDownloadLinks().size() == 0) {
                            iterator.remove();
                            continue;
                        }
                    } else {
                        // Anhand des Hostnamens aus dem DownloadLink
                        // wird ein passendes Plugin gesucht
                        try {
                            pluginForHost = JDUtilities.getNewPluginForHostInstanz(localLink.getHost());
                        } catch (Exception e) {
                            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
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
                            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
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
            }
            return packages;
        }
        throw new Exception("Linklist incompatible");
    }

    public Vector<FilePackage> getPackages() {
        return packages;
    }

    public void addPackage(FilePackage fp) {
        if (fp == null) return;
        synchronized (packages) {
            if (!packages.contains(fp)) {
                fp.getBroadcaster().addListener(this);
                packages.add(fp);
                broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.ADD_FILEPACKAGE, fp));
            }
        }
    }

    public int indexOf(FilePackage fp) {
        synchronized (packages) {
            return packages.indexOf(fp);
        }
    }

    public void addPackageAt(FilePackage fp, int index) {
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
                broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.REFRESH_STRUCTURE));
            } else {
                if (index > packages.size() - 1) {
                    packages.add(fp);
                } else if (index < 0) {
                    packages.add(0, fp);
                } else
                    packages.add(index, fp);
                fp.getBroadcaster().addListener(this);
                broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.ADD_FILEPACKAGE, fp));
            }
        }
    }

    public void addAllAt(Vector<FilePackage> links, int index) {
        synchronized (packages) {
            for (int i = 0; i < links.size(); i++) {
                addPackageAt(links.get(i), index + i);
            }
        }
    }

    public void removePackage(FilePackage fp2) {
        if (fp2 == null) return;
        synchronized (packages) {
            fp2.abortDownload();
            fp2.getBroadcaster().removeListener(this);
            packages.remove(fp2);
            broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.REMOVE_FILPACKAGE, fp2));
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
    public Vector<DownloadLink> getAllDownloadLinks() {
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        synchronized (packages) {
            for (FilePackage fp : packages) {
                ret.addAll(fp.getDownloadLinks());
            }
        }
        return ret;
    }

    // Den Optimizer muss ich noch fertig machen
    // public boolean hasDownloadLinkwithURL(String url) {
    // if (optimizer.getLinkswithURL(url) != null ||
    // optimizer.getLinkswithURL(url).size() == 0) { return true; }
    // return false;
    // }

    public boolean hasDownloadLinkwithURL(String url) {
        if (url == null) return false;
        url = url.trim();
        synchronized (packages) {
            for (DownloadLink dl : getAllDownloadLinks()) {
                if (dl.getDownloadURL() != null && dl.getDownloadURL().equalsIgnoreCase(url)) return true;
            }
        }
        return false;
    }

    public DownloadLink getFirstLinkThatBlocks(DownloadLink link) {
        synchronized (packages) {
            for (DownloadLink nextDownloadLink : getAllDownloadLinks()) {
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
                    if ((nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS) || nextDownloadLink.getLinkStatus().isPluginActive()) && nextDownloadLink.getFileOutput().equalsIgnoreCase(link.getFileOutput())) return nextDownloadLink;
                }
            }
        }
        return null;
    }

    public void actionPerformed(ActionEvent arg0) {
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

    public void fireDownloadLinkUpdate(Object param) {
        broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.REFRESH_SPECIFIC, param));
    }

    public void onDownloadControllerEvent(DownloadControllerEvent event) {
        switch (event.getID()) {
        case DownloadControllerEvent.ADD_DOWNLOADLINK:
        case DownloadControllerEvent.REMOVE_DOWNLOADLINK:
        case DownloadControllerEvent.ADD_FILEPACKAGE:
        case DownloadControllerEvent.REMOVE_FILPACKAGE:
            fireStructureUpdate();
        case DownloadControllerEvent.REFRESH_STRUCTURE:
            this.saveDownloadLinksAsync();
            break;
        }
    }

    public void onFilePackageEvent(FilePackageEvent event) {
        switch (event.getID()) {
        case FilePackageEvent.DOWNLOADLINK_ADDED:
            broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.ADD_DOWNLOADLINK, event.getParameter()));
            this.fireStructureUpdate();
            break;
        case FilePackageEvent.DOWNLOADLINK_REMOVED:
            broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.REMOVE_DOWNLOADLINK, event.getParameter()));
            break;
        case FilePackageEvent.FILEPACKAGE_UPDATE:
            this.fireStructureUpdate();
            break;
        case FilePackageEvent.FILEPACKAGE_EMPTY:
            this.removePackage((FilePackage) event.getSource());
            this.fireStructureUpdate();
            break;

        }
    }
}
