//jDownloader - Downloadmanager
//Copyright (C) 2008JD-Team jdownloader@freenet.de
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This programis distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.If not, see <http://www.gnu.org/licenses/>.

package jd.controlling;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import jd.JDInit;
import jd.captcha.CES;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.interaction.InfoFileWriter;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.Unrar;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.event.UIEvent;
import jd.event.UIListener;
import jd.gui.UIInterface;
import jd.gui.skins.simple.LinkGrabber;
import jd.gui.skins.simple.SimpleGUI;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.HTTP;
import jd.plugins.Plugin;
import jd.plugins.PluginForContainer;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.event.PluginEvent;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.Reconnecter;

/**
 * Im Controller wird das ganze App gesteuert. Evebnts werden deligiert.
 * 
 * @author JD-Team/astaldo
 * 
 */
public class JDController implements ControlListener, UIListener {
    /**
     * Der Download wird gerade abgebrochen.
     */
    public static final int DOWNLOAD_TERMINATION_IN_PROGRESS = 0;
    /**
     * Der Download läuft
     */
    public static final int DOWNLOAD_RUNNING = 2;
    /**
     * Es läuft kein Download
     */
    public static final int DOWNLOAD_NOT_RUNNING = 3;
    /**
     * Der Controller wurd fertig initialisiert
     */
    public static final int INIT_STATUS_COMPLETE = 0;
    private static final String PROPERTY_SELECTED = "selected";

    /**
     * Mit diesem Thread wird eingegebener Text auf Links untersucht
     */
    private DistributeData distributeData = null;

    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #firePluginEvent(PluginEvent)} ein Event losgeschickt wird.
     */
    private transient ArrayList<ControlListener> controlListener = null;

    /**
     * Die Konfiguration
     */
    protected Configuration config = JDUtilities.getConfiguration();

    /**
     * Schnittstelle zur Benutzeroberfläche
     */
    private UIInterface uiInterface;

    /**
     * Hier kann de Status des Downloads gespeichert werden.
     */
    private int downloadStatus;

    // /**
    // * Die DownloadLinks
    // */
    // private Vector<DownloadLink> downloadLinks;
    private Vector<FilePackage> packages;

    /**
     * Der Logger
     */
    private Logger logger = JDUtilities.getLogger();

    private File lastCaptchaLoaded;

    private DownloadLink lastDownloadFinished;


    /**
     * Der Download Watchdog verwaltet die Downloads
     */
    private DownloadWatchDog watchdog;

    private Vector<DownloadLink> finishedLinks = new Vector<DownloadLink>();

    private Unrar unrarModule;

    private InfoFileWriter infoFileWriterModule;

    // public static Property FLAGS = new Property();

    private int initStatus = -1;

    private Vector<Vector<String>> waitingUpdates = new Vector<Vector<String>>();

    // private boolean isReconnecting;
    private int downloadListChangeID = 0;
    // private boolean lastReconnectSuccess;
    private FilePackage fp;
    private ArrayList<ControlEvent> eventQueue;
    private EventSender eventSender;
    private BufferedWriter fileLogger = null;

    // private long lastIPUpdate;
    // private static String CURRENT_IP;

    public JDController() {

        packages = new Vector<FilePackage>();
     
        downloadStatus = DOWNLOAD_NOT_RUNNING;
        this.eventSender = getEventSender();

        fp = new FilePackage();
        fp.setName(JDLocale.L("controller.packages.defaultname", "various"));

        JDUtilities.setController(this);
        initInteractions();
        // CURRENT_IP = JDUtilities.getIPAddress();
    }

    private EventSender getEventSender() {
        EventSender th = new EventSender();
        th.start();

        return th;
    }

    /**
     * Initialisiert alle Interactions
     */
    @SuppressWarnings("unchecked")
    private void initInteractions() {
        Vector<Interaction> interactions = (Vector<Interaction>) JDUtilities.getSubConfig(Configuration.CONFIG_INTERACTIONS).getProperty(Configuration.PARAM_INTERACTIONS, new Vector<Interaction>());

        for (int i = 0; i < interactions.size(); i++) {
            interactions.get(i).initInteraction();
        }
    }

    /**
     * Gibt den Status (ID) der downloads zurück
     * 
     * @return
     */
    public int getDownloadStatus() {
        if (watchdog == null || watchdog.isAborted() && downloadStatus == DOWNLOAD_RUNNING) {
            setDownloadStatus(DOWNLOAD_NOT_RUNNING);
        }
        return this.downloadStatus;

    }

    /**
     * Startet den Downloadvorgang. Dies eFUnkton sendet das startdownload event
     * und aktiviert die ersten downloads.
     */
    public boolean startDownloads() {
        if (getDownloadStatus() == DOWNLOAD_NOT_RUNNING) {
            setDownloadStatus(DOWNLOAD_RUNNING);
            logger.info("StartDownloads");
            this.watchdog = new DownloadWatchDog(this);
            watchdog.start();
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOAD_START, this));
            return true;
        }
        return false;
    }

    /**
     * Startet den download wenn er angehalten ist und hält ihn an wenn er läuft
     */
    public void toggleStartStop() {

        if (!startDownloads()) {
            this.stopDownloads();
        }

    }

    /**
     * Beendet das Programm
     */
    public void exit() {
        saveDownloadLinks();
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SYSTEM_EXIT, this));
        Interaction.handleInteraction(Interaction.INTERACTION_EXIT, null);
        System.exit(0);
    }

    /**
     * Hier werden ControlEvent ausgewertet
     * 
     * @param event
     */

    @SuppressWarnings("unchecked")
    public void controlEvent(ControlEvent event) {
        if (event == null) {
            logger.warning("event= NULL");
            return;
        }
        switch (event.getID()) {

        case ControlEvent.CONTROL_LOG_OCCURED:
            if (fileLogger != null) {
                LogRecord l = (LogRecord) event.getParameter();
                try {
                    fileLogger.write(l.getMillis() + " : " + l.getSourceClassName() + "(" + l.getSourceMethodName() + ") " + "[" + l.getLevel() + "] -> " + l.getMessage() + "\r\n");
                    fileLogger.flush();
                } catch (IOException e) {
                }

            }

            break;
        case ControlEvent.CONTROL_SYSTEM_EXIT:

            CES.setEnabled(false);
            if (fileLogger != null) {
                try {
                    fileLogger.flush();

                    fileLogger.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            break;
        case ControlEvent.CONTROL_PLUGIN_INACTIVE:
            // Nur Hostpluginevents auswerten
            if (!(event.getSource() instanceof PluginForHost)) return;
            lastDownloadFinished = ((SingleDownloadController) event.getParameter()).getDownloadLink();
            this.addToFinished(lastDownloadFinished);

            // Prüfen ob das Paket fertig ist
            if (lastDownloadFinished.getFilePackage().getRemainingLinks() == 0) {
                Interaction.handleInteraction(Interaction.INTERACTION_DOWNLOAD_PACKAGE_FINISHED, this);

                this.getInfoFileWriterModule().interact(lastDownloadFinished);

            }
            // Prüfen obd er Link entfernt werden soll.
            if (lastDownloadFinished.getStatus() == DownloadLink.STATUS_DONE && Configuration.FINISHED_DOWNLOADS_REMOVE.equals(JDUtilities.getConfiguration().getProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION))) {

                this.removeDownloadLink(lastDownloadFinished);
                this.fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));

            }
            saveDownloadLinks();

            break;
        case ControlEvent.CONTROL_CAPTCHA_LOADED:
            lastCaptchaLoaded = (File) event.getParameter();
            break;

        case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:

            break;
        case ControlEvent.CONTROL_DISTRIBUTE_FINISHED:

            // logger.info("rvc event" + links);

            if (event.getParameter() != null && event.getParameter() instanceof Vector && ((Vector) event.getParameter()).size() > 0) {
                Vector links = (Vector) event.getParameter();
                uiInterface.addLinksToGrabber((Vector<DownloadLink>) links);

            }

            break;

        case ControlEvent.CONTROL_DISTRIBUTE_FINISHED_HIDEGRABBER:

            if (event.getParameter() != null && event.getParameter() instanceof Vector && ((Vector) event.getParameter()).size() > 0) {
                Vector<DownloadLink> links = (Vector<DownloadLink>) event.getParameter();
                addLinksWithoutGrabber(links);
            }

            break;

        case ControlEvent.CONTROL_DISTRIBUTE_FINISHED_HIDEGRABBER_START:

            if (event.getParameter() != null && event.getParameter() instanceof Vector && ((Vector) event.getParameter()).size() > 0) {
                Vector<DownloadLink> links = (Vector<DownloadLink>) event.getParameter();
                addLinksWithoutGrabber(links);
                if (getDownloadStatus() == JDController.DOWNLOAD_NOT_RUNNING) {
                    fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_STARTSTOP_DOWNLOAD, this));
                }
            }

            break;

        case ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED:
            // Interaction interaction = (Interaction) event.getParameter();
            this.saveDownloadLinks();
            break;

        default:

            break;
        }
        // if (uiInterface != null) uiInterface.delegatedControlEvent(event);
    }

    public void removeDownloadLink(DownloadLink link) {
        synchronized (packages) {
            link.setAborted(true);
            Iterator<FilePackage> it = packages.iterator();
            FilePackage fp;
            while (it.hasNext()) {
                fp = it.next();
                if (fp.remove(link)) {
                    if (fp.size() == 0) packages.remove(fp);
                    return;
                }

            }

        }
        // logger.severe("Link " + link + " does not belong to any Package");

    }

    public void removeDownloadLinks(Vector<DownloadLink> links) {
        if (links == null || links.size() == 0) return;
        Iterator<DownloadLink> iterator = links.iterator();
        while (iterator.hasNext()) {

            this.removeDownloadLink(iterator.next());
        }

    }

    public FilePackage getFilePackage(DownloadLink link) {
        synchronized (packages) {
            Iterator<FilePackage> it = packages.iterator();
            FilePackage fp;
            while (it.hasNext()) {
                fp = it.next();
                if (fp.contains(link)) return fp;
            }
        }
        logger.severe("Link " + link + " does not belong to any Package");
        return null;
    }

    /**
     * Fügt einen Downloadlink der Finishedliste hinzu.
     * 
     * @param lastDownloadFinished
     */
    private void addToFinished(DownloadLink lastDownloadFinished) {
        this.finishedLinks.add(lastDownloadFinished);

    }

    /**
     * Gibt alle in dieser Session beendeten Downloadlinks zurück. unabhängig
     * davon ob sie noch in der dl liste stehen oder nicht
     * 
     * @return
     */
    public Vector<DownloadLink> getFinishedLinks() {
        return finishedLinks;

    }

    /**
     * Bricht den Download ab und blockiert bis er abgebrochen wurde.
     */
    public void stopDownloads() {
        if (getDownloadStatus() == DOWNLOAD_RUNNING) {
            setDownloadStatus(DOWNLOAD_TERMINATION_IN_PROGRESS);
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOAD_TERMINATION_ACTIVE, this));

            watchdog.abort();
            setDownloadStatus(DOWNLOAD_NOT_RUNNING);
            logger.info("termination broadcast");
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOAD_TERMINATION_INACTIVE, this));
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOAD_STOP, this));
        }
    }

    /**
     * Hier werden die UIEvente ausgewertet
     * 
     * @param uiEvent
     *            UIEent
     */
    public void uiEvent(UIEvent uiEvent) {

        switch (uiEvent.getActionID()) {
        case UIEvent.UI_PAUSE_DOWNLOADS:

            pauseDownloads((Boolean) uiEvent.getParameter());
            break;
        case UIEvent.UI_START_DOWNLOADS:
            startDownloads();
            break;
        case UIEvent.UI_STOP_DOWNLOADS:
            stopDownloads();
            break;
        case UIEvent.UI_LINKS_TO_PROCESS:
            String data = (String) uiEvent.getParameter();
            distributeData = new DistributeData(data);
            distributeData.addControlListener(this);
            distributeData.start();
            break;
        case UIEvent.UI_PACKAGE_GRABBED:
            FilePackage fp;
            try {
                fp = (FilePackage) uiEvent.getParameter();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            fp.sort("asc");
            if ((JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getIntegerProperty("PROPERTY_POSITION", 1) == 0) && 
                    (uiEvent.getSource() instanceof LinkGrabber)) {
                this.addPackageAt(fp, 0);
            } else {
                this.addPackage(fp);
            }
            this.fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));

            break;
        case UIEvent.UI_SAVE_LINKS:
            File file = (File) uiEvent.getParameter();
            saveDLC(file);
            break;
        case UIEvent.UI_LOAD_LINKS:
            file = (File) uiEvent.getParameter();
            loadContainerFile(file);
            break;

        case UIEvent.UI_EXIT:
            exit();
            break;

        case UIEvent.UI_UPDATED_LINKLIST:

            // newLinks = uiInterface.getP
            // abortDeletedLink(downloadLinks, newLinks);
            // // newLinks darf nicht einfach übernommen werden sonst
            // // bearbeiten controller und gui den selben vector.
            // downloadLinks.clear();
            // downloadLinks.addAll(newLinks);
            // saveDownloadLinks(JDUtilities.getResourceFile("links.dat"));
            break;
        case UIEvent.UI_INTERACT_RECONNECT:
            if (getRunningDownloadNum() > 0) {
                logger.info("Es laufen noch Downloads. Breche zum reconnect Downloads ab!");
                stopDownloads();
            }

            // Interaction.handleInteraction(Interaction.
            // INTERACTION_NEED_RECONNECT,
            // this);
            if (Reconnecter.waitForNewIP(1)) {
                uiInterface.showMessageDialog(JDLocale.L("gui.reconnect.success", "Reconnect erfolgreich"));

            } else {

                uiInterface.showMessageDialog(JDLocale.L("gui.reconnect.failed", "Reconnect fehlgeschlagen"));

            }

            // uiInterface.setDownloadLinks(downloadLinks);
            break;
        case UIEvent.UI_INTERACT_UPDATE:
            new JDInit().doWebupdate(JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1), true);
            break;
        }
    }

    public void addPackage(FilePackage fp) {
        synchronized (packages) {
            packages.add(fp);
        }

    }

    public void addLink(DownloadLink link) {
        int index;
        if (link.getFilePackage() == null) link.setFilePackage(fp);
        synchronized (packages) {
            if ((index = packages.indexOf(link.getFilePackage())) >= 0) {

                packages.get(index).add(link);
            } else {
                packages.add(link.getFilePackage());
                if (!link.getFilePackage().contains(link)) link.getFilePackage().add(link);

            }
        }

    }

    public boolean movePackage(FilePackage fp, int index) {
        if (index < 0) index = 0;
        if (index > packages.size() - 1) index = packages.size() - 1;
        int i = packages.indexOf(fp);
        if (i == index) {
            return false;
        } else if (i > index) {
            index--;
            this.removePackage(fp);
        } else {
            this.removePackage(fp);
        }
        this.addPackageAt(fp, index);
        return true;
    }

    public boolean removePackage(FilePackage fp2) {
        for (Iterator<DownloadLink> it = fp2.getDownloadLinks().iterator(); it.hasNext();) {
            it.next().setAborted(true);
        }

        synchronized (packages) {
            return packages.remove(fp2);
        }

    }

    // Dies Funktion macht um package basiertem design wenig Sinn
    // public void addLinkAt(DownloadLink link, int i) {
    // if (link.getFilePackage() == null) link.setFilePackage(fp);
    // if(packages.contains(link.getFilePackage())){
    // if(link.getFilePackage().contains(link))
    // }
    // Vector<DownloadLink> pfs = this.getPackageFiles(link);
    // if (pfs != null) this.removeDownloadLinks(new Vector<DownloadLink>(pfs));
    // synchronized (packages) {
    // packages.add(0, link.getFilePackage());
    // }
    //
    // }

    // public void addAllLinksAt(Vector<DownloadLink> links, int i) {
    // synchronized (packages) {
    // for (int t = 0; t < links.size(); t++) {
    // Vector<DownloadLink> pfs = this.getPackageFiles(links.get(t));
    // this.removeDownloadLinks(pfs);
    // packages.add(0, links.get(t).getFilePackage());
    // }
    // }
    //
    // }

    public void addPackageAt(FilePackage fp, int index) {
        if (packages.size() == 0) {
            this.addPackage(fp);
            return;
        }
        if (index > packages.size() - 1) index = packages.size() - 1;
        if (index < 0) index = 0;
        synchronized (packages) {
            packages.add(index, fp);
        }

    }

    public void addAllLinks(Vector<DownloadLink> links) {
        Iterator<DownloadLink> it = links.iterator();
        while (it.hasNext())
            this.addLink(it.next());

    }

    // Dies Funktion macht wenig sinn im package basierten Aufbau
    // public void addAllLinks(int index, Vector<DownloadLink> links) {
    // Iterator<DownloadLink> it = links.iterator();
    // int i = 0;
    // while (it.hasNext()) {
    // this.addLinkAt(it.next(), i + index);
    // i++;
    // }
    // }

    public void pauseDownloads(boolean value) {
        watchdog.pause(value);

    }

    /**
     * bricht downloads ab wenn diese entfernt wurden
     * 
     * @param oldLinks
     * @param newLinks
     */
    /*
     * private void abortDeletedLink(Vector<DownloadLink> oldLinks,
     * Vector<DownloadLink> newLinks) { logger.info("abort " + oldLinks.size() +
     * " - " + newLinks.size()); if (watchdog == null) return; for (int i = 0; i
     * < oldLinks.size(); i++) { if (newLinks.indexOf(oldLinks.elementAt(i)) ==
     * -1) { // Link gefunden der entfernt wurde
     * logger.finer("Found link that hast been removed: " +
     * oldLinks.elementAt(i)); // oldLinks.elementAt(i).setAborted(true);
     * 
     * watchdog.abortDownloadLink(oldLinks.elementAt(i)); } }
     * 
     * }
     */
    /**
     * Speichert die Linksliste ab
     * 
     * @param file
     *            Die Datei, in die die Links gespeichert werden sollen
     */
    public void saveDownloadLinks() {
        // JDUtilities.saveObject(null, downloadLinks.toArray(new
        // DownloadLink[]{}), file, "links", "dat", true);
        File file = JDUtilities.getResourceFile("links.dat");
        JDUtilities.saveObject(null, packages, file, "links", "dat", Configuration.saveAsXML);
    }

    public String encryptDLC(String xml) {
        // if(true)return xml;
        String[] encrypt = JDUtilities.encrypt(xml, "DLC Parser");

        // logger.info(encrypt[1] + " - ");
        if (encrypt == null) {
            logger.severe("Container Encryption failed.");

            return null;

        }
        String key = encrypt[1];
        xml = encrypt[0];

        Vector<URL> services;
        try {
            services = new Vector<URL>();
            // services.add(new URL("http://dlcrypt1.ath.cx/service.php"));
            // services.add(new URL("http://dlcrypt2.ath.cx/service.php"));
            // services.add(new URL("http://dlcrypt3.ath.cx/service.php"));
            services.add(new URL("http://dlcrypt4.ath.cx/service.php"));
            // services.add(new URL("http://dlcrypt5.ath.cx/service.php"));
            Collections.sort(services, new Comparator<Object>() {
                public int compare(Object a, Object b) {
                    return (int) ((Math.random() * 4.0) - 2.0);

                }

            });
            services.add(0, new URL("http://dlcrypt.ath.cx/service.php"));
            Iterator<URL> it = services.iterator();
            // int url = 0;
            while (it.hasNext()) {
                URL service = it.next();
                try {
                    String dlcKey = callService(service, key);
                    if (dlcKey == null) {
                        continue;
                    }
                    return xml + dlcKey;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (MalformedURLException e1) {
        }
        return null;

    }

    private String callService(URL service, String key) throws Exception {
        logger.finer("Call " + service);
        // int tc=Plugin.getConnectTimeoutFromConfiguration();
        // int tr=Plugin.getReadTimeoutFromConfiguration();

        RequestInfo ri = HTTP.getRequestWithoutHtmlCode(service, null, null, false, 2000, 2000);

        if (!ri.isOK() || ri.getLocation() == null) {

        return null; }

        logger.finer("Call Redirect: " + ri.getLocation());

        ri = HTTP.postRequest(new URL(ri.getLocation()), null, null, null, "jd=1&srcType=plain&data=" + key, true, 2000, 2000);

        logger.info("Call re: " + ri.getHtmlCode());
        if (!ri.isOK() || !ri.containsHTML("<rc>")) {

            return null;
        } else {
            String dlcKey = ri.getHtmlCode();

            dlcKey = new Regex(dlcKey, "<rc>(.*?)</rc>").getFirstMatch();
            if (dlcKey.trim().length() < 80) {

            return null; }

            return dlcKey;
        }

    }

    public void saveDLC(File file) {

        String xml = JDUtilities.createContainerString(this.getDownloadLinks(), "DLC Parser");
        // String[] encrypt = JDUtilities.encrypt(xml, "DLC Parser");
        String cipher = encryptDLC(xml);
        if (cipher != null) {

            JDUtilities.writeLocalFile(file, cipher);
            SubConfiguration cfg = JDUtilities.getSubConfig("DLC Parser");
            if (cfg.getBooleanProperty("SHOW_INFO_AFTER_CREATE", false))

            {

                if (this.getUiInterface().showConfirmDialog(JDLocale.L("sys.dlc.success", "DLC encryption successfull. Run Testdecrypt now?"))) {
                    loadContainerFile(file);
                    return;
                }
            }
            return;
        }

        logger.severe("Container creation failed");
        this.getUiInterface().showMessageDialog("Container encryption failed");
    }

    public void saveDLC(File file, Vector<DownloadLink> links) {

        String xml = JDUtilities.createContainerString(links, "DLC Parser");
        // String[] encrypt = JDUtilities.encrypt(xml, "DLC Parser");
        String cipher = encryptDLC(xml);
        if (cipher != null) {
            SubConfiguration cfg = JDUtilities.getSubConfig("DLC Parser");
            JDUtilities.writeLocalFile(file, cipher);
            if (cfg.getBooleanProperty("SHOW_INFO_AFTER_CREATE", false))
            // Nur Falls Die Meldung nicht deaktiviert wurde
            {

                if (this.getUiInterface().showConfirmDialog(JDLocale.L("sys.dlc.success", "DLC encryption successfull. Run Testdecrypt now?"))) {
                    loadContainerFile(file);
                    return;
                }
            }
            return;
        }

        logger.severe("Container creation failed");
        this.getUiInterface().showMessageDialog("Container encryption failed");
    }

    public boolean isLocalFileInProgress(DownloadLink link) {
        synchronized (packages) {
            Iterator<FilePackage> iterator = packages.iterator();
            FilePackage fp = null;
            DownloadLink nextDownloadLink;
            while (iterator.hasNext()) {
                fp = iterator.next();
                Iterator<DownloadLink> it2 = fp.getDownloadLinks().iterator();
                while (it2.hasNext()) {
                    nextDownloadLink = it2.next();

                    if (nextDownloadLink.getStatus() == DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS && nextDownloadLink.getFileOutput().equalsIgnoreCase(link.getFileOutput())) {
                        logger.info("Link owner: " + nextDownloadLink.getHost() + nextDownloadLink);
                        return true;

                    }
                }

            }
        }
        return false;
    }

    /**
     * Lädt eine LinkListe
     * 
     * @param file
     *            Die Datei, aus der die Links gelesen werden
     * @return Ein neuer Vector mit den DownloadLinks
     */
    @SuppressWarnings("unchecked")
    private Vector<FilePackage> loadDownloadLinks(File file) {
        try {
            if (file.exists()) {
                Object obj = JDUtilities.loadObject(null, file, Configuration.saveAsXML);

                if (obj != null && obj instanceof Vector && (((Vector) obj).size() == 0 || (((Vector) obj).size() > 0 && ((Vector) obj).get(0) instanceof FilePackage))) {
                    Vector<FilePackage> packages = (Vector<FilePackage>) obj;
                    Iterator<FilePackage> iterator = packages.iterator();
                    DownloadLink localLink;
                    PluginForHost pluginForHost = null;
                    PluginForContainer pluginForContainer = null;
                    Iterator<DownloadLink> it;
                    FilePackage fp;
                    while (iterator.hasNext()) {
                        fp = iterator.next();
                        it = fp.getDownloadLinks().iterator();
                        while (it.hasNext()) {

                            localLink = it.next();
                            if (localLink.getStatus() == DownloadLink.STATUS_DONE && Configuration.FINISHED_DOWNLOADS_REMOVE_AT_START.equals(JDUtilities.getConfiguration().getProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION))) {
                                it.remove();
                                if (fp.getDownloadLinks().size() == 0) {
                                    iterator.remove();

                                }
                            } else {
                                // Anhand des Hostnamens aus dem DownloadLink
                                // wird
                                // ein
                                // passendes Plugin gesucht
                                try {
                                    pluginForHost = JDUtilities.getPluginForHost(localLink.getHost()).getClass().newInstance();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                // Gibt es einen Names für ein Containerformat,
                                // wird
                                // ein
                                // passendes Plugin gesucht
                                try {
                                    if (localLink.getContainer() != null) {
                                        pluginForContainer = JDUtilities.getPluginForContainer(localLink.getContainer(), localLink.getContainerFile());
                                        if (pluginForContainer != null) {

                                        } else
                                            localLink.setEnabled(false);
                                    }
                                }

                                catch (NullPointerException e) {
                                    e.printStackTrace();
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
            }

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe("Linklist Konflikt.");
            return null;
        }
    }

    /**
     * Hiermit wird eine Containerdatei geöffnet. Dazu wird zuerst ein passendes
     * Plugin gesucht und danach alle DownloadLinks interpretiert
     * 
     * @param file
     *            Die Containerdatei
     */
    public void loadContainerFile(final File file) {
        new Thread() {
            @SuppressWarnings("unchecked")
            public void run() {

                Vector<PluginForContainer> pluginsForContainer = JDUtilities.getPluginsForContainer();
                Vector<DownloadLink> downloadLinks = new Vector<DownloadLink>();
                PluginForContainer pContainer;
                ProgressController progress = new ProgressController("Containerloader", pluginsForContainer.size());
                logger.info("load Container: " + file);

                for (int i = 0; i < pluginsForContainer.size(); i++) {

                    pContainer = pluginsForContainer.get(i);
                    // logger.info(i + ". " + "Containerplugin: " +
                    // pContainer.getPluginName());
                    progress.setStatusText("Containerplugin: " + pContainer.getPluginName());
                    if (pContainer.canHandle(file.getName())) {
                        // es muss jeweils eine neue plugininstanz erzeugt
                        // werden
                        try {
                            pContainer = pContainer.getClass().newInstance();
                            progress.setSource(pContainer);

                            pContainer.initContainer(file.getAbsolutePath());
                            Vector<DownloadLink> links = pContainer.getContainedDownloadlinks();
                            if (links == null || links.size() == 0) {
                                logger.severe("Container Decryption failed (1)");
                            } else {
                                downloadLinks = links;
                                break;
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    progress.increase(1);
                }
                progress.setStatusText(downloadLinks.size() + " links found");
                if (downloadLinks.size() > 0) {
                    if (JDUtilities.getSubConfig("GUI").getBooleanProperty(Configuration.PARAM_SHOW_CONTAINER_ONLOAD_OVERVIEW, false)) {
                        String html = "<style>p { font-size:9px;margin:1px; padding:0px;}div {font-family:Geneva, Arial, Helvetica, sans-serif; width:400px;background-color:#ffffff; padding:2px;}h1 { vertical-align:top; text-align:left;font-size:10px; margin:0px; display:block;font-weight:bold; padding:0px;}</style><div> <div align='center'> <p><img src='http://jdownloader.org/img/%s.gif'> </p> </div> <h1>%s</h1><hr> <table width='100%%' border='0' cellspacing='5'> <tr> <td><p>%s</p></td> <td style='width:100%%'><p>%s</p></td> </tr> <tr> <td><p>%s</p></td> <td style='width:100%%'><p>%s</p></td> </tr> <tr> <td><p>%s</p></td> <td style='width:100%%'><p>%s</p></td> </tr> <tr> <td><p>%s</p></td> <td style='width:100%%'><p>%s</p></td> </tr> </table> </div>";
                        String app;
                        String uploader;
                        if (downloadLinks.get(0).getFilePackage().getProperty("header", null) != null) {
                            HashMap<String, String> header = (HashMap<String, String>) downloadLinks.get(0).getFilePackage().getProperty("header", null);
                            uploader = header.get("tribute");
                            app = header.get("generator.app") + " v." + header.get("generator.version") + " (" + header.get("generator.url") + ")";
                        } else {
                            app = "n.A.";
                            uploader = "n.A";
                        }
                        String comment = downloadLinks.get(0).getFilePackage().getComment();
                        String password = downloadLinks.get(0).getFilePackage().getPassword();
                        JDUtilities.getGUI().showHTMLDialog(JDLocale.L("container.message.title", "DownloadLinkContainer loaded"), String.format(html, JDUtilities.getFileExtension(file).toLowerCase(), JDLocale.L("container.message.title", "DownloadLinkContainer loaded"), JDLocale.L("container.message.uploaded", "Brought to you by"), uploader, JDLocale.L("container.message.created", "Created with"), app, JDLocale.L("container.message.comment", "Comment"), comment, JDLocale.L("container.message.password", "Password"), password));
                        // schickt die Links zuerst mal zum Linkgrabber
                    }
                    uiInterface.addLinksToGrabber((Vector<DownloadLink>) downloadLinks);
                }
                progress.finalize();
            }
        }.start();
    }

    public boolean isContainerFile(File file) {

        Vector<PluginForContainer> pluginsForContainer = JDUtilities.getPluginsForContainer();
        // Vector<DownloadLink> downloadLinks = new Vector<DownloadLink>();
        PluginForContainer pContainer;

        for (int i = 0; i < pluginsForContainer.size(); i++) {
            pContainer = pluginsForContainer.get(i);

            if (pContainer.canHandle(file.getName())) { return true; }

        }

        return false;
    }

    /**
     * Liefert alle DownloadLinks zurück
     * 
     * @return Alle DownloadLinks zurück
     */
    public Vector<DownloadLink> getDownloadLinks() {
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        synchronized (packages) {
            Iterator<FilePackage> iterator = packages.iterator();
            FilePackage fp = null;

            while (iterator.hasNext()) {
                fp = iterator.next();
                Iterator<DownloadLink> it2 = fp.getDownloadLinks().iterator();
                while (it2.hasNext()) {
                    ret.add(it2.next());
                }
            }
        }
        logger.warning("DEPRECATED FUNCTION");
        return ret;
    }

    /**
     * Lädt zum Start das erste Mal alle Links aus einer Datei
     * 
     * @return true/False je nach Erfolg
     */
    public boolean initDownloadLinks() {

        packages = loadDownloadLinks(JDUtilities.getResourceFile("links.dat"));
        if (packages == null) {
            packages = new Vector<FilePackage>();
            this.fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));

            return false;
        }

        for (Iterator<FilePackage> packageIterator = packages.iterator(); packageIterator.hasNext();) {

            for (Iterator<DownloadLink> linkIterator = packageIterator.next().getDownloadLinks().iterator(); linkIterator.hasNext();) {
                linkIterator.next().setProperty(PROPERTY_SELECTED, false);
            }
        }
        this.fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));

        return true;

    }

    /**
     * Gibt ale links zurück die im selben package sind wie downloadLink
     * 
     * @param downloadLink
     * @return Alle DownloadLinks die zum selben package gehören
     */
    public Vector<DownloadLink> getPackageFiles(DownloadLink downloadLink) {
        synchronized (packages) {
            Iterator<FilePackage> iterator = packages.iterator();
            FilePackage fp = null;

            while (iterator.hasNext()) {
                fp = iterator.next();
                if (fp.contains(downloadLink)) return fp.getDownloadLinks();

            }
        }
        return null;
    }

    public Vector<DownloadLink> getPackageFiles(FilePackage filePackage) {
        return filePackage.getDownloadLinks();
    }

    public Vector<DownloadLink> getPackageFiles(FilePackage filePackage, Vector<DownloadLink> links) {
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        // ret.add(downloadLink);

        Iterator<DownloadLink> iterator = links.iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();

            if (filePackage == nextDownloadLink.getFilePackage()) ret.add(nextDownloadLink);
        }
        return ret;
    }

    /**
     * Gibt die Anzahl der fertigen Downloads im package zurück
     * 
     * @param downloadLink
     * @return Anzahl der fertigen Files in diesem paket
     */
    public int getPackageReadyNum(DownloadLink downloadLink) {
        int i = 0;

        Vector<DownloadLink> downloadLinks = getPackageFiles(downloadLink);
        Iterator<DownloadLink> iterator = downloadLinks.iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();
            if (nextDownloadLink.getStatus() == DownloadLink.STATUS_DONE) i++;
        }
        return i;
    }

    public int getPackageReadyNum(FilePackage filePackage) {
        int i = 0;

        Vector<DownloadLink> downloadLinks = filePackage.getDownloadLinks();
        Iterator<DownloadLink> iterator = downloadLinks.iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();
            if (nextDownloadLink.getStatus() == DownloadLink.STATUS_DONE) i++;
        }
        return i;
    }

    // /**
    // * Gibt die Anzahl der fehlenden FIles zurück
    // *
    // * @param downloadLink
    // * @return Anzahl der fehlenden Files in diesem Paket
    // */
    // public int getMissingPackageFiles(DownloadLink downloadLink) {
    // int i = 0;
    // Vector<DownloadLink> downloadLinks = getPackageFiles(downloadLink);
    // Iterator<DownloadLink> iterator = downloadLinks.iterator();
    // DownloadLink nextDownloadLink = null;
    // while (iterator.hasNext()) {
    // nextDownloadLink = iterator.next();
    // if (nextDownloadLink.getStatus() != DownloadLink.STATUS_DONE) i++;
    // }
    // return i;
    // }

    /**
     * Liefert die Anzahl der gerade laufenden Downloads. (nur downloads die
     * sich wirklich in der downloadphase befinden
     * 
     * @return Anzahld er laufenden Downloadsl
     */
    public int getRunningDownloadNum() {
        int ret = 0;
        synchronized (packages) {
            Iterator<FilePackage> iterator = packages.iterator();
            FilePackage fp = null;
            DownloadLink nextDownloadLink;
            while (iterator.hasNext()) {
                fp = iterator.next();
                Iterator<DownloadLink> it2 = fp.getDownloadLinks().iterator();
                while (it2.hasNext()) {
                    nextDownloadLink = it2.next();
                    if (nextDownloadLink.getStatus() == DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS) ret++;
                }
            }
        }
        return ret;
    }

    public int getForbiddenReconnectDownloadNum() {
        int ret = 0;
        synchronized (packages) {
            Iterator<FilePackage> iterator = packages.iterator();
            FilePackage fp = null;
            DownloadLink nextDownloadLink;
            while (iterator.hasNext()) {
                fp = iterator.next();
                Iterator<DownloadLink> it2 = fp.getDownloadLinks().iterator();
                while (it2.hasNext()) {
                    nextDownloadLink = it2.next();
                    if (nextDownloadLink.getStatus() == DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS || (nextDownloadLink.isInProgress() && !nextDownloadLink.isWaitingForReconnect() && nextDownloadLink.isEnabled())) {

                        ret++;
                    }
                }
            }
        }

        if (watchdog != null && !this.watchdog.isAborted() && watchdog.isAlive()) { return Math.max(watchdog.getActiveDownloadControllers().size(), ret); }
        return ret;
    }

    public boolean hasDownloadLinkURL(String url) {
        // synchronized (packages) {
        try {
            Iterator<FilePackage> iterator = packages.iterator();
            FilePackage fp = null;
            DownloadLink nextDownloadLink;
            while (iterator.hasNext()) {
                fp = iterator.next();
                Iterator<DownloadLink> it2 = fp.getDownloadLinks().iterator();
                while (it2.hasNext()) {
                    nextDownloadLink = it2.next();
                    if (nextDownloadLink.getDownloadURL() != null && nextDownloadLink.getDownloadURL().equalsIgnoreCase(url)) return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;

    }

    /**
     * Der Benuter soll den Captcha Code erkennen
     * 
     * @param plugin
     *            Das Plugin, das den Code anfordert
     * @param captchaAddress
     *            Adresse des anzuzeigenden Bildes
     * @return Text des Captchas
     */
    public String getCaptchaCodeFromUser(Plugin plugin, File captchaAddress, String def) {
        String captchaCode = uiInterface.getCaptchaCodeFromUser(plugin, captchaAddress, def);
        return captchaCode;
    }

    /**
     * Setzt das UIINterface
     * 
     * @param uiInterface
     */
    public void setUiInterface(UIInterface uiInterface) {
        if (this.uiInterface != null) this.uiInterface.removeUIListener(this);
        this.uiInterface = uiInterface;
        uiInterface.addUIListener(this);

    }

    /**
     * Gibt das verwendete UIinterface zurpck
     * 
     * @return aktuelles uiInterface
     */
    public UIInterface getUiInterface() {
        return uiInterface;
    }

    /**
     * 
     * @return Die zuletzte fertiggestellte datei
     */
    public String getLastFinishedFile() {
        if (this.lastDownloadFinished == null) return "";
        return this.lastDownloadFinished.getFileOutput();
    }

    /**
     * 
     * @return Die zuletzte fertiggestellte datei
     */
    public DownloadLink getLastFinishedDownloadLink() {

        return lastDownloadFinished;
    }

    /**
     * 
     * @return ZUletzt bearbeiteter Captcha
     */
    public String getLastCaptchaImage() {
        if (this.lastCaptchaLoaded == null) return "";
        return this.lastCaptchaLoaded.getAbsolutePath();
    }

    /**
     * @return gibt das globale speedmeter zurück
     */
    public int getSpeedMeter() {

        if (this.getWatchdog() == null || !getWatchdog().isAlive()) return 0;
        return this.getWatchdog().getTotalSpeed();
    }

    /**
     * Fügt einen Listener hinzu
     * 
     * @param listener
     *            Ein neuer Listener
     */
    public synchronized void addControlListener(ControlListener listener) {
        if (controlListener == null) controlListener = new ArrayList<ControlListener>();
        synchronized (controlListener) {
            if (controlListener.indexOf(listener) == -1) {
                controlListener.add(listener);
            }
        }
    }

    /**
     * Emtfernt einen Listener
     * 
     * @param listener
     *            Der zu entfernende Listener
     */
    public synchronized void removeControlListener(ControlListener listener) {
        synchronized (controlListener) {
            controlListener.remove(listener);
        }
    }

    /**
     * Verteilt Ein Event an alle Listener
     * 
     * @param controlEvent
     *            ein abzuschickendes Event
     */
    public void fireControlEvent(ControlEvent controlEvent) {
        // logger.info(controlEvent.getID()+" controllistener "+controlEvent);
        // if (uiInterface != null)
        // uiInterface.delegatedControlEvent(controlEvent);
        if (controlEvent == null) return;
        if (controlEvent.getID() == ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED) {
            this.increaseChangeID();
        }
        try {
            // this.controlEvent(controlEvent);
            // if (controlListener == null) controlListener = new
            // ArrayList<ControlListener>();
            if (this.eventQueue == null) this.eventQueue = new ArrayList<ControlEvent>();
            this.eventQueue.add(controlEvent);

            synchronized (eventSender) {
                if (eventSender.waitFlag) {
                    eventSender.waitFlag = false;
                    eventSender.notify();
                }

            }
            // Iterator<ControlListener> iterator = controlListener.iterator();
            // while (iterator.hasNext()) {

            // ((ControlListener) iterator.next()).controlEvent(controlEvent);
            // }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fireControlEvent(int controlID, Object param) {
        ControlEvent c = new ControlEvent(this, controlID, param);
        fireControlEvent(c);

    }

    /**
     * Setzt den Downloadstatus. Status Ids aus JDController.** sollten
     * verwendet werden
     * 
     * @param downloadStatus
     */
    public void setDownloadStatus(int downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    /**
     * Setzt de Status aller Links zurück die nicht gerade geladen werden.
     */
    public void resetAllLinks() {
        synchronized (packages) {
            ArrayList<DownloadLink> al = new ArrayList<DownloadLink>();
            Iterator<FilePackage> iterator = packages.iterator();
            FilePackage fp = null;
            DownloadLink nextDownloadLink;
            while (iterator.hasNext()) {
                fp = iterator.next();
                Iterator<DownloadLink> it2 = fp.getDownloadLinks().iterator();
                while (it2.hasNext()) {
                    nextDownloadLink = it2.next();
                    if (!nextDownloadLink.isInProgress()) {
                        nextDownloadLink.setStatus(DownloadLink.STATUS_TODO);
                        nextDownloadLink.setStatusText("");
                        nextDownloadLink.reset();
                        nextDownloadLink.setEndOfWaittime(0);
                        ((PluginForHost) nextDownloadLink.getPlugin()).resetPluginGlobals();
                        al.add(nextDownloadLink);
                    }

                }
            }
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, al));

        }

    }

    public void requestDownloadLinkUpdate(DownloadLink link) {

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, link));

    }

    public void setUnrarModule(Unrar instance) {
        this.unrarModule = instance;

    }

    public void setInfoFileWriterModule(InfoFileWriter instance) {
        this.infoFileWriterModule = instance;
    }

    public Unrar getUnrarModule() {
        return unrarModule;
    }

    public InfoFileWriter getInfoFileWriterModule() {
        return infoFileWriterModule;
    }

    // /**
    // * Führt über die in der cnfig gegebenen daten einen reconnect durch.
    // *
    // * @return
    // */
    //
    // public boolean requestReconnect() {
    // int wait = 0;
    // while (isReconnecting) {
    // try {
    // Thread.sleep(500);
    // } catch (InterruptedException e) {
    // }
    // wait += 500;
    //
    // }
    // if (wait > 0 && lastReconnectSuccess) return true;
    // boolean ipChangeSuccess = false;
    // isReconnecting=true;
    // if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.
    // PARAM_DISABLE_RECONNECT, false)) {
    // //logger.finer(
    // "Reconnect is disabled. Enable the CheckBox in the Toolbar to reactivate it"
    // );
    // if ((System.currentTimeMillis() - lastIPUpdate) > (1000 * 60 * 10)) {
    // this.lastIPUpdate = System.currentTimeMillis();
    // String tmp = CURRENT_IP;
    // CURRENT_IP = JDUtilities.getIPAddress();
    // if (CURRENT_IP!=null&&!tmp.equals(CURRENT_IP)) {
    // logger.info("Detected external IP Change.");
    // ipChangeSuccess = true;
    // }
    // JDUtilities.getGUI().displayMiniWarning(JDLocale.L(
    // "gui.warning.reconnect.hasbeendisabled", "Reconnect deaktiviert!"),
    // JDLocale.L("gui.warning.reconnect.hasbeendisabled.tooltip",
    // "Um erfolgreich einen Reconnect durchführen zu können muss diese Funktion wieder aktiviert werden."
    // ), 60000);
    //                
    // }
    //
    // if (!ipChangeSuccess) {
    // isReconnecting=false;
    // return false;
    // }
    // }
    // if (!ipChangeSuccess) {
    // if (this.getForbiddenReconnectDownloadNum() > 0) {
    // logger.finer("Downloads are running. reconnect is disabled");
    // isReconnecting=false;
    // return false;
    // }
    // Interaction.handleInteraction(Interaction.INTERACTION_BEFORE_RECONNECT,
    // this);
    // String type =
    // JDUtilities.getConfiguration().getStringProperty(Configuration
    // .PARAM_RECONNECT_TYPE, null);
    // if (type == null) {
    // isReconnecting=false;
    // logger.severe("Reconnect is not configured. Config->Reconnect!");
    // return false;
    // }
    // isReconnecting = true;
    //
    // if (type.equals(JDLocale.L("modules.reconnect.types.extern", "Extern")))
    // {
    // ipChangeSuccess = new ExternReconnect().interact(null);
    // } else if (type.equals(JDLocale.L("modules.reconnect.types.batch",
    // "Batch"))) {
    // ipChangeSuccess = new BatchReconnect().interact(null);
    // } else {
    // ipChangeSuccess = new HTTPLiveHeader().interact(null);
    // }
    //
    // lastReconnectSuccess = ipChangeSuccess;
    // logger.info("Reconnect success: " + ipChangeSuccess);
    // }
    // if (ipChangeSuccess) {
    // synchronized (packages) {
    // Iterator<FilePackage> iterator = packages.iterator();
    // FilePackage fp = null;
    // DownloadLink nextDownloadLink;
    // while (iterator.hasNext()) {
    // fp = iterator.next();
    // Iterator<DownloadLink> it2 = fp.getDownloadLinks().iterator();
    // while (it2.hasNext()) {
    // nextDownloadLink = it2.next();
    // if (nextDownloadLink.getRemainingWaittime() > 0) {
    // nextDownloadLink.setEndOfWaittime(0);
    // logger.finer("REset GLOBALS: " + ((PluginForHost)
    // nextDownloadLink.getPlugin()));
    // ((PluginForHost) nextDownloadLink.getPlugin()).resetPluginGlobals();
    // nextDownloadLink.setStatus(DownloadLink.STATUS_TODO);
    //
    // }
    // }
    // }
    // }
    //
    // }
    // if (ipChangeSuccess) {
    // Interaction.handleInteraction(Interaction.INTERACTION_AFTER_RECONNECT,
    // this);
    // }
    // isReconnecting = false;
    // this.lastIPUpdate = System.currentTimeMillis();
    // CURRENT_IP = JDUtilities.getIPAddress();
    // return ipChangeSuccess;
    // }



    public void setInitStatus(int initStatus) {
        this.initStatus = initStatus;
    }

    public int getInitStatus() {
        return initStatus;
    }

    public void setWaitingUpdates(Vector<Vector<String>> files) {
        this.waitingUpdates = files;

    }

    public Vector<Vector<String>> getWaitingUpdates() {
        return this.waitingUpdates;

    }

    public DownloadWatchDog getWatchdog() {
        return watchdog;
    }

    /**
     * Der Zurückgegeben Vector darf nur gelesen werden!!
     * 
     * @return
     */
    public Vector<FilePackage> getPackages() {
        return packages;
    }

    /**
     * Schneidet alle Links aus und fügt sie zwischen before unc after ein. Alle
     * 
     * @param links
     * @param before
     * @param after
     * @return
     */
    public boolean moveLinks(Vector<DownloadLink> links, DownloadLink before, DownloadLink after) {
        if (links.contains(before) || links.contains(after)) return false;
        if (before != null && after != null && before.getFilePackage() != after.getFilePackage()) return false;
        if (before == null & after == null) return false;
        DownloadLink link;

        Iterator<DownloadLink> iterator = links.iterator();
        synchronized (packages) {
            while (iterator.hasNext()) {
                link = iterator.next();
                Iterator<FilePackage> it = packages.iterator();
                FilePackage fp;
                while (it.hasNext()) {
                    fp = it.next();
                    if (fp.remove(link)) {

                        if (fp.size() == 0) it.remove();
                        continue;
                    }

                }
            }
        }

        FilePackage dest = before == null ? after.getFilePackage() : before.getFilePackage();
        if (dest == null) { return false; }
        int pos = 0;
        if (before != null) {
            pos = dest.indexOf(before) + 1;
        } else {
            pos = dest.indexOf(after);
        }

        dest.addAllAt(links, pos);

        // logger.info("II");
        this.fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));

        return true;
    }

    public boolean movePackages(Vector<FilePackage> fps, FilePackage before, FilePackage after) {
        if (after != null && fps.contains(after)) return false;
        if (before != null && fps.contains(before)) return false;
        if (before == null && after == null) return false;
        synchronized (packages) {
            packages.removeAll(fps);
            int pos = after == null ? packages.indexOf(before) + 1 : packages.indexOf(after);

            packages.addAll(pos, fps);

        }
        this.fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));

        return true;
    }

    public FilePackage getDefaultFilePackage() {
        return fp;
    }

    public void addLinksWithoutGrabber(final Vector<DownloadLink> parameter) {

        if (parameter == null || parameter.size() == 0) { return; }

        Vector<DownloadLink> linkList = checkLinks(parameter);

        Vector<Vector<DownloadLink>> links = new Vector<Vector<DownloadLink>>();
        Vector<String> packages = new Vector<String>();
        SubConfiguration guiConfig = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);

        for (int i = 0; i < linkList.size(); i++) {

            if (!guiConfig.getBooleanProperty("PROPERTY_AUTOPACKAGE", true)) {

                packages.add(removeExtension(linkList.get(i).getName()));
                links.get(0).add(linkList.get(i));

            } else {

                int bestSim = 0;
                int bestIndex = -1;

                for (int j = 0; j < packages.size(); j++) {

                    int sim = comparePackages(packages.get(j), removeExtension(linkList.get(i).getName()));
                    if (sim > bestSim) {
                        bestSim = sim;
                        bestIndex = j;
                    }

                }

                if (bestSim > guiConfig.getIntegerProperty("AUTOPACKAGE_LIMIT", 98) && bestIndex != -1) {

                    links.get(bestIndex).add(linkList.get(i));

                } else {

                    packages.add(removeExtension(linkList.get(i).getName()));
                    Vector<DownloadLink> temp = new Vector<DownloadLink>();
                    temp.add(linkList.get(i));
                    links.add(temp);

                }

            }

        }

        for (int i = 0; i < packages.size(); i++) {

            int rand = (int) (Math.random() * 0xffffff);
            Color c = new Color(rand);
            c = c.brighter();

            FilePackage fp = new FilePackage();
            fp.setProperty("color", c);
            fp.setName(packages.get(i));
            String downloadDir = JDUtilities.getConfiguration().getDefaultDownloadDirectory();

            if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false)) {

                File file = new File(new File(downloadDir), packages.get(i));
                if (!file.exists()) file.mkdirs();

                if (file.exists()) {
                    fp.setDownloadDirectory(file.getAbsolutePath());
                } else {
                    fp.setDownloadDirectory(downloadDir);
                }

            } else {
                fp.setDownloadDirectory(downloadDir);
            }

            fp.setDownloadLinks(links.get(i));

            for (int j = 0; j < links.get(i).size(); j++) {
                links.get(i).get(j).setFilePackage(fp);
            }

            JDUtilities.getGUI().fireUIEvent(new UIEvent(this, UIEvent.UI_PACKAGE_GRABBED, fp));

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

    private String removeExtension(String a) {
        // logger.finer("file " + a);
        if (a == null) return a;
        a = a.replaceAll("\\.part([0-9]+)", "");
        a = a.replaceAll("\\.html", "");
        a = a.replaceAll("\\.htm", "");
        int i = a.lastIndexOf(".");
        // logger.info("FOund . " + i);
        String ret;
        if (i <= 1 || (a.length() - i) > 5) {
            ret = a.toLowerCase().trim();
        } else {
            // logger.info("Remove ext");
            ret = a.substring(0, i).toLowerCase().trim();
        }

        if (a.equals(ret)) return ret;
        return (ret);

    }

    private int comparePackages(String a, String b) {

        int c = 0;
        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            if (a.charAt(i) == b.charAt(i)) c++;
        }

        if (Math.min(a.length(), b.length()) == 0) return 0;
        // logger.info("comp: " + a + " <<->> " + b + "(" + (c * 100) /
        // (b.length()) + ")");
        return (c * 100) / (b.length());
    }

    private Vector<DownloadLink> checkLinks(Vector<DownloadLink> linksQueue) {

        SubConfiguration guiConfig = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
        Vector<DownloadLink> finalLinks = new Vector<DownloadLink>();
        DownloadLink link;
        DownloadLink next;

        while (linksQueue.size() > 0) {

            link = linksQueue.remove(0);

            if (!guiConfig.getBooleanProperty("DO_ONLINE_CHECK", false)) {

                finalLinks.add(link);
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                }

            } else {

                Iterator<DownloadLink> it = linksQueue.iterator();
                Vector<DownloadLink> links = new Vector<DownloadLink>();
                Vector<DownloadLink> dlLinks = new Vector<DownloadLink>();
                links.add(link);
                dlLinks.add(link);

                while (it.hasNext()) {
                    next = it.next();
                    if (next.getPlugin().getClass() == link.getPlugin().getClass()) {
                        dlLinks.add(next);
                        links.add(next);
                    }
                }

                if (links.size() > 1) {
                    boolean[] ret = ((PluginForHost) link.getPlugin()).checkLinks(links.toArray(new DownloadLink[] {}));
                    if (ret != null) {
                        for (int i = 0; i < links.size(); i++) {
                            dlLinks.get(i).setAvailable(ret[i]);
                        }
                    }
                }

                if (link.isAvailable() || ((PluginForHost) link.getPlugin()).isListOffline()) {
                    finalLinks.add(link);
                }

            }

        }

        return finalLinks;

    }

    private class EventSender extends Thread {

        protected static final long MAX_EVENT_TIME = 10000;
        public boolean waitFlag = true;
        private Thread watchDog;
        private long eventStart = 0;
        private ControlListener currentListener;
        private ControlEvent event;

        public EventSender() {
            super("EventSender");

            this.watchDog = new Thread("EventSenderWatchDog") {
                public void run() {
                    while (true) {
                        if (eventStart > 0 && (System.currentTimeMillis() - eventStart) > MAX_EVENT_TIME) {
                            JDUtilities.getLogger().finer("WATCHDOG: Execution Limit reached");
                            JDUtilities.getLogger().finer("ControlListener: " + currentListener);
                            JDUtilities.getLogger().finer("Event: " + event);

                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                }

            };
            watchDog.start();

        }

        public void run() {
            while (true) {

                synchronized (this) {

                    while (waitFlag) {
                        try {
                            wait();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    // JDUtilities.getLogger().severe("THREAD");
                    if (eventQueue != null && eventQueue.size() > 0) {
                        event = eventQueue.remove(0);
                        if (event == null) continue;
                        eventStart = System.currentTimeMillis();
                        currentListener = JDController.this;
                        controlEvent(event);
                        eventStart = 0;
                        if (controlListener == null) controlListener = new ArrayList<ControlListener>();
                        synchronized (controlListener) {
                            Iterator<ControlListener> iterator = controlListener.iterator();
                            while (iterator.hasNext()) {
                                eventStart = System.currentTimeMillis();
                                currentListener = ((ControlListener) iterator.next());
                                currentListener.controlEvent(event);
                                eventStart = 0;
                            }
                        }
                        // JDUtilities.getLogger().severe("THREAD2");
                    } else {
                        eventStart = 0;
                        waitFlag = true;
                        // JDUtilities.getLogger().severe("PAUSE");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    eventStart = 0;
                }
            }

        }

    }

    public void setLogFileWriter(BufferedWriter bufferedWriter) {
        this.fileLogger = bufferedWriter;

    }

    public DownloadLink getDownloadLinkBefore(DownloadLink downloadLink) {
        synchronized (packages) {

            FilePackage fp = null;
            DownloadLink nextDownloadLink;
            DownloadLink lastDownloadLink = null;
            for (Iterator<FilePackage> packageIterator = packages.iterator(); packageIterator.hasNext();) {
                fp = packageIterator.next();

                for (Iterator<DownloadLink> linkIterator = fp.getDownloadLinks().iterator(); linkIterator.hasNext();) {

                    nextDownloadLink = linkIterator.next();
                    if (downloadLink == nextDownloadLink) return lastDownloadLink;
                    lastDownloadLink = nextDownloadLink;

                }
            }
            return null;

        }

    }

    public DownloadLink getDownloadLinkAfter(DownloadLink lastElement) {
        synchronized (packages) {

            FilePackage fp = null;
            DownloadLink nextDownloadLink;
            DownloadLink lastDownloadLink = null;
            for (Iterator<FilePackage> packageIterator = packages.iterator(); packageIterator.hasNext();) {
                fp = packageIterator.next();

                for (Iterator<DownloadLink> linkIterator = fp.getDownloadLinks().iterator(); linkIterator.hasNext();) {

                    nextDownloadLink = linkIterator.next();
                    if (lastElement == lastDownloadLink) return nextDownloadLink;
                    lastDownloadLink = nextDownloadLink;

                }
            }
            return null;

        }
    }

    public int getDownloadListChangeID() {
        return downloadListChangeID;
    }

    private synchronized void increaseChangeID() {
        downloadListChangeID++;
    }
}
