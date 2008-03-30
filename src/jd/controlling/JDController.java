//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.

package jd.controlling;

import java.io.File;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import jd.JDInit;
import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.interaction.BatchReconnect;
import jd.controlling.interaction.ExternReconnect;
import jd.controlling.interaction.HTTPLiveHeader;
import jd.controlling.interaction.InfoFileWriter;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.Unrar;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.event.UIEvent;
import jd.event.UIListener;
import jd.gui.UIInterface;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForContainer;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.event.PluginEvent;
import jd.plugins.event.PluginListener;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * Im Controller wird das ganze App gesteuert. Evebnts werden deligiert.
 * 
 * @author JD-Team/astaldo
 * 
 */
public class JDController implements PluginListener, ControlListener, UIListener {
    public static final int                   DOWNLOAD_TERMINATION_IN_PROGRESS = 0;

    public static final int                   DOWNLOAD_RUNNING                 = 2;

    public static final int                   DOWNLOAD_NOT_RUNNING             = 3;

    public static final int                   INIT_STATUS_COMPLETE             = 0;

    /**
     * Mit diesem Thread wird eingegebener Text auf Links untersucht
     */
    private DistributeData                    distributeData                   = null;

    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #firePluginEvent(PluginEvent)} ein Event losgeschickt wird.
     */
    private transient Vector<ControlListener> controlListener                  = null;

    /**
     * Die Konfiguration
     */
    protected Configuration                   config                           = JDUtilities.getConfiguration();

    /**
     * Schnittstelle zur Benutzeroberfläche
     */
    private UIInterface                       uiInterface;

    /**
     * Hier kann de Status des Downloads gespeichert werden.
     */
    private int                               downloadStatus;

    /**
     * Die DownloadLinks
     */
    private Vector<DownloadLink>              downloadLinks;

    /**
     * Der Logger
     */
    private Logger                            logger                           = JDUtilities.getLogger();

    private File                              lastCaptchaLoaded;

    private DownloadLink                      lastDownloadFinished;

    private ClipboardHandler                  clipboard;

    /**
     * Der Download Watchdog verwaltet die Downloads
     */
    private DownloadWatchDog                  watchdog;

    private Vector<DownloadLink>              finishedLinks                    = new Vector<DownloadLink>();

    private Unrar                             unrarModule;

    private InfoFileWriter                    infoFileWriterModule;

    public static Property                    FLAGS                            = new Property();

    private int                               initStatus                       = -1;

    private Vector<Vector<String>>            waitingUpdates;

    private boolean isReconnecting;

    private boolean lastReconnectSuccess;

    public JDController() {
        downloadLinks = new Vector<DownloadLink>();
        clipboard = new ClipboardHandler();
        downloadStatus = DOWNLOAD_NOT_RUNNING;

        JDUtilities.setController(this);
        initInteractions();
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
    public synchronized void startDownloads() {
        if (getDownloadStatus() == DOWNLOAD_NOT_RUNNING) {
            setDownloadStatus(DOWNLOAD_RUNNING);
            logger.info("StartDownloads");
            this.watchdog = new DownloadWatchDog(this);
            watchdog.start();
        }
    }

    /**
     * Beendet das Programm
     */
    private void exit() {
        saveDownloadLinks(JDUtilities.getResourceFile("links.dat"));

        System.exit(0);
    }

    /**
     * Eventfunktion für den Pluginlistener
     * 
     * @param event PluginEvent
     */
    public void pluginEvent(PluginEvent event) {
        uiInterface.delegatedPluginEvent(event);
        switch (event.getEventID()) {
        }
    }

    /**
     * Hier werden ControlEvent ausgewertet
     * 
     * @param event
     */

    @SuppressWarnings("unchecked")
    public void controlEvent(ControlEvent event) {
        switch (event.getID()) {
            case ControlEvent.CONTROL_SINGLE_DOWNLOAD_FINISHED:
                lastDownloadFinished = (DownloadLink) event.getParameter();
                saveDownloadLinks(JDUtilities.getResourceFile("links.dat"));

                this.addToFinished(lastDownloadFinished);
                if (this.getMissingPackageFiles(lastDownloadFinished) == 0) {
                    Interaction.handleInteraction(Interaction.INTERACTION_DOWNLOAD_PACKAGE_FINISHED, this);

                    this.getInfoFileWriterModule().interact(lastDownloadFinished);

                }

                if (lastDownloadFinished.getStatus() == DownloadLink.STATUS_DONE && Configuration.FINISHED_DOWNLOADS_REMOVE.equals(JDUtilities.getConfiguration().getProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION))) {
                    logger.info("REM1");
                    downloadLinks.remove(lastDownloadFinished);

                    saveDownloadLinks(JDUtilities.getResourceFile("links.dat"));
                    uiInterface.setDownloadLinks(downloadLinks);
                }

                break;
            case ControlEvent.CONTROL_CAPTCHA_LOADED:
                lastCaptchaLoaded = (File) event.getParameter();
                break;

            case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:

                saveDownloadLinks(JDUtilities.getResourceFile("links.dat"));
                break;
            case ControlEvent.CONTROL_DISTRIBUTE_FINISHED:
                Object links = event.getParameter();
                //logger.info("rvc event" + links);
                if (links != null && links instanceof Vector && ((Vector) links).size() > 0) {
                    // schickt die Links zuerst mal zum Linkgrabber
                    uiInterface.addLinksToGrabber((Vector<DownloadLink>) links);
                }
                break;
            case ControlEvent.CONTROL_PLUGIN_INTERACTION_INACTIVE:
                // Interaction interaction = (Interaction) event.getParameter();

                break;
            default:

                break;
        }
        if (uiInterface != null) uiInterface.delegatedControlEvent(event);
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
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOAD_TERMINATION_INACTIVE, this));
        }
    }

    /**
     * Hier werden die UIEvente ausgewertet
     * 
     * @param uiEvent UIEent
     */
    @SuppressWarnings("unchecked")
    public void uiEvent(UIEvent uiEvent) {
        Vector<DownloadLink> newLinks;

        switch (uiEvent.getActionID()) {
            case UIEvent.UI_PAUSE_DOWNLOADS:
                logger.info("KKKK");
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
//            case UIEvent.UI_SAVE_CONFIG:
//
//                JDUtilities.saveObject(null, JDUtilities.getConfiguration(), JDUtilities.getJDHomeDirectoryFromEnvironment(), JDUtilities.CONFIG_PATH.split("\\.")[0], "." + JDUtilities.CONFIG_PATH.split("\\.")[1], Configuration.saveAsXML);
//                break;
            case UIEvent.UI_LINKS_GRABBED:

                // Event wenn der Linkgrabber mit ok bestätigt wird. Die
                // ausgewählten Links werden als Eventparameter übergeben und
                // können nun der Downloadliste zugeführt werden
                Object links = uiEvent.getParameter();
                if (links != null && links instanceof Vector && ((Vector) links).size() > 0) {
                    downloadLinks.addAll((Vector<DownloadLink>) links);
                    saveDownloadLinks(JDUtilities.getResourceFile("links.dat"));
                    uiInterface.setDownloadLinks(downloadLinks);
                }

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

            case UIEvent.UI_LINKS_CHANGED:

                newLinks = uiInterface.getDownloadLinks();
                abortDeletedLink(downloadLinks, newLinks);
                // newLinks darf nicht einfach übernommen werden sonst
                // bearbeiten controller und gui den selben vector.
                downloadLinks.clear();
                downloadLinks.addAll(newLinks);
                saveDownloadLinks(JDUtilities.getResourceFile("links.dat"));
                break;
            case UIEvent.UI_INTERACT_RECONNECT:
                if (getRunningDownloadNum() > 0) {
                    logger.info("Es laufen noch Downloads. Breche zum reconnect Downloads ab!");
                    stopDownloads();
                }

                // Interaction.handleInteraction(Interaction.INTERACTION_NEED_RECONNECT,
                // this);
                if (requestReconnect()) {
                    uiInterface.showMessageDialog("Reconnect erfolgreich");

                }
                else {

                    uiInterface.showMessageDialog("Reconnect fehlgeschlagen");

                }

                uiInterface.setDownloadLinks(downloadLinks);
                break;
            case UIEvent.UI_INTERACT_UPDATE:
                new JDInit().doWebupdate(JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1), true);
                break;
        }
    }

    private void pauseDownloads(boolean value) {
        watchdog.pause(value);

    }

    /**
     * bricht downloads ab wenn diese entfernt wurden
     * 
     * @param oldLinks
     * @param newLinks
     */
    private void abortDeletedLink(Vector<DownloadLink> oldLinks, Vector<DownloadLink> newLinks) {
        logger.info("abort " + oldLinks.size() + " - " + newLinks.size());
        if (watchdog == null) return;
        for (int i = 0; i < oldLinks.size(); i++) {
            if (newLinks.indexOf(oldLinks.elementAt(i)) == -1) {
                // Link gefunden der entfernt wurde
                logger.finer("Found link that hast been removed: " + oldLinks.elementAt(i));
                // oldLinks.elementAt(i).setAborted(true);

                watchdog.abortDownloadLink(oldLinks.elementAt(i));
            }
        }

    }

    /**
     * Speichert die Linksliste ab
     * 
     * @param file Die Datei, in die die Links gespeichert werden sollen
     */
    public void saveDownloadLinks(File file) {
        // JDUtilities.saveObject(null, downloadLinks.toArray(new
        // DownloadLink[]{}), file, "links", "dat", true);

        JDUtilities.saveObject(null, downloadLinks, file, "links", "dat", Configuration.saveAsXML);
    }

    public String createDLCString(Vector<DownloadLink> links) {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        SubConfiguration cfg = JDUtilities.getSubConfig("DLCCONFIG");
        InputSource inSourceHeader = new InputSource(new StringReader("<header><generator><app></app><version/><url></url></generator><tribute></tribute><comment/><category/></header>"));
        InputSource inSourceContent = new InputSource(new StringReader("<content/>"));

        try {
            Document content = factory.newDocumentBuilder().parse(inSourceContent);
            Document header = factory.newDocumentBuilder().parse(inSourceHeader);
            Node header_generator_app = header.getFirstChild().getFirstChild().getChildNodes().item(0);
            Node header_generator_version = header.getFirstChild().getFirstChild().getChildNodes().item(1);
            Node header_generator_url = header.getFirstChild().getFirstChild().getChildNodes().item(2);
            header_generator_app.appendChild(header.createTextNode(JDUtilities.Base64Encode("JDownloader")));
            header_generator_version.appendChild(header.createTextNode(JDUtilities.Base64Encode(JDUtilities.getRevision())));
            header_generator_url.appendChild(header.createTextNode(JDUtilities.Base64Encode("http://jdownload.ath.cx")));

            Node header_tribute = header.getFirstChild().getChildNodes().item(1);

            if (cfg.getBooleanProperty("ASK_ADD_INFOS", false)) {
                Element element = header.createElement("name");
                header_tribute.getFirstChild().appendChild(element);
                element.appendChild(header.createTextNode(JDUtilities.Base64Encode(this.getUiInterface().showUserInputDialog("Uploader Name"))));

                if ((cfg.getStringProperty("UPLOADERNAME", null) != null && cfg.getStringProperty("UPLOADERNAME", null).trim().length() > 0)) {
                    element = header.createElement("name");
                    header_tribute.getFirstChild().appendChild(element);
                    element.appendChild(header.createTextNode(JDUtilities.Base64Encode(cfg.getStringProperty("UPLOADERNAME", null))));

                }

            }
            else {
                Element element = header.createElement("name");
                header_tribute.appendChild(element);
                element.appendChild(header.createTextNode(JDUtilities.Base64Encode("unknown")));

            }
            Node header_comment = header.getFirstChild().getChildNodes().item(2);
            Node header_category = header.getFirstChild().getChildNodes().item(3);
            if (cfg.getBooleanProperty("ASK_ADD_INFOS", false)) {

                header_comment.appendChild(header.createTextNode(JDUtilities.Base64Encode(this.getUiInterface().showUserInputDialog("Comment"))));

            }
            if (cfg.getBooleanProperty("ASK_ADD_INFOS", false)) {
                header_category.appendChild(header.createTextNode(JDUtilities.Base64Encode(this.getUiInterface().showUserInputDialog("Category"))));
            }
            else {
                header_category.appendChild(header.createTextNode(JDUtilities.Base64Encode("various")));

            }

            Vector<FilePackage> packages = new Vector<FilePackage>();

            for (int i = 0; i < links.size(); i++) {
                if (!packages.contains(links.get(i).getFilePackage())) {
                    packages.add(links.get(i).getFilePackage());
                }
            }

            for (int i = 0; i < packages.size(); i++) {
                Element filePackage = content.createElement("package");
                if (packages.get(i) == null) {
                    filePackage.setAttribute("name", JDUtilities.Base64Encode("various"));
                }
                else {
                    filePackage.setAttribute("name", JDUtilities.Base64Encode(packages.get(i).getName()));
                }

                content.getFirstChild().appendChild(filePackage);

                Vector<DownloadLink> tmpLinks = this.getPackageFiles(packages.get(i), links);

                for (int x = 0; x < tmpLinks.size(); x++) {
                    Element file = content.createElement("file");
                    filePackage.appendChild(file);
                    Element url = content.createElement("url");
                    Element pw = content.createElement("password");
                    Element comment = content.createElement("comment");
                    url.appendChild(content.createTextNode(JDUtilities.Base64Encode(tmpLinks.get(x).getDownloadURL())));
                    if (packages.get(i) != null) {
                        pw.appendChild(content.createTextNode(JDUtilities.Base64Encode(packages.get(i).getPassword())));
                        comment.appendChild(content.createTextNode(JDUtilities.Base64Encode(packages.get(i).getComment())));
                        filePackage.getLastChild().appendChild(pw);
                        filePackage.getLastChild().appendChild(comment);
                    }
                    filePackage.getLastChild().appendChild(url);

                }

            }

            int ind1 = JDUtilities.xmltoStr(header).indexOf("<header");
            int ind2 = JDUtilities.xmltoStr(content).indexOf("<content");
            String ret = JDUtilities.xmltoStr(header).substring(ind1) + JDUtilities.xmltoStr(content).substring(ind2);

            return ret;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
             services.add(new URL("http://dlcrypt1.ath.cx/service.php"));
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
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        catch (MalformedURLException e1) {
        }
        return null;

    }

    private String callService(URL service, String key) throws Exception {
        logger.finer("Call " + service);
        // int tc=Plugin.getConnectTimeoutFromConfiguration();
        // int tr=Plugin.getReadTimeoutFromConfiguration();

        RequestInfo ri = Plugin.getRequestWithoutHtmlCode(service, null, null, false, 2000, 2000);

        if (!ri.isOK() || ri.getLocation() == null) {

            return null;
        }

        logger.finer("Call Redirect: " + ri.getLocation());

        ri = Plugin.postRequest(new URL(ri.getLocation()), null, null, null, "jd=1&srcType=plain&data=" + key, true, 2000, 2000);

        logger.info("Call re: " + ri.getHtmlCode());
        if (!ri.isOK() || !ri.containsHTML("<rc>")) {

            return null;
        }
        else {
            String dlcKey = ri.getHtmlCode();

            dlcKey = Plugin.getBetween(dlcKey, "<rc>", "</rc>");
            if (dlcKey.trim().length() < 80) {

                return null;
            }

            return dlcKey;
        }

    }

    public void saveDLC(File file) {

        String xml = JDUtilities.createContainerString(this.getDownloadLinks(), "DLC Parser");
        // String[] encrypt = JDUtilities.encrypt(xml, "DLC Parser");
        String cipher = encryptDLC(xml);
        if (cipher != null) {

            JDUtilities.writeLocalFile(file, cipher);
            if (this.getUiInterface().showConfirmDialog(JDLocale.L("sys.dlc.success", "DLC encryption successfull. Run Testdecrypt now?"))) {
                loadContainerFile(file);
                return;
            }
            return;
        }

        logger.severe("Container creation failed");
        this.getUiInterface().showMessageDialog("Container encryption failed");
    }

    public boolean isLocalFileInProgress(DownloadLink link) {

        Iterator<DownloadLink> iterator = downloadLinks.iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();
            if (nextDownloadLink.getStatus() == DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS && nextDownloadLink.getFileOutput().equalsIgnoreCase(link.getFileOutput())) {
                logger.info("Link owner: " + nextDownloadLink.getHost() + nextDownloadLink);
                return true;

            }

        }
        return false;
    }

    /**
     * Lädt eine LinkListe
     * 
     * @param file Die Datei, aus der die Links gelesen werden
     * @return Ein neuer Vector mit den DownloadLinks
     */
    @SuppressWarnings("unchecked")
    private Vector<DownloadLink> loadDownloadLinks(File file) {
        try {
            if (file.exists()) {
                Object obj = JDUtilities.loadObject(null, file, Configuration.saveAsXML);
                if (obj != null && obj instanceof Vector) {
                    Vector<DownloadLink> links = (Vector<DownloadLink>) obj;
                    Iterator<DownloadLink> iterator = links.iterator();
                    DownloadLink localLink;
                    PluginForHost pluginForHost = null;
                    PluginForContainer pluginForContainer = null;
                    while (iterator.hasNext()) {
                        localLink = iterator.next();
                        if (localLink.getStatus() == DownloadLink.STATUS_DONE && Configuration.FINISHED_DOWNLOADS_REMOVE_AT_START.equals(JDUtilities.getConfiguration().getProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION))) {
                            iterator.remove();
                            continue;
                        }
                        // Anhand des Hostnamens aus dem DownloadLink wird ein
                        // passendes Plugin gesucht
                        try {
                            pluginForHost = JDUtilities.getPluginForHost(localLink.getHost()).getClass().newInstance();
                        }
                        catch (InstantiationException e) {
                            e.printStackTrace();
                        }
                        catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                        // Gibt es einen Names für ein Containerformat, wird ein
                        // passendes Plugin gesucht
                        try {
                            if (localLink.getContainer() != null) {
                                pluginForContainer = JDUtilities.getPluginForContainer(localLink.getContainer(), localLink.getContainerFile());
                                if (pluginForContainer != null) {
                                    // pluginForContainer =
                                    // pluginForContainer.getPlugin(localLink.getContainerFile());
                                    // pluginForContainer.
                                    // pluginForContainer.initContainer(localLink.getContainerFile());
                                    // pluginForContainer.getContainedDownloadlinks();
                                }
                                else
                                    localLink.setEnabled(false);
                            }
                        }

                        catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                        if (pluginForHost != null) {
                            localLink.setLoadedPlugin(pluginForHost);
                            pluginForHost.addPluginListener(this);
                        }
                        if (pluginForContainer != null) {
                            localLink.setLoadedPluginForContainer(pluginForContainer);
                            pluginForContainer.addPluginListener(this);
                        }
                        if (pluginForHost == null) {
                            logger.severe("couldn't find plugin(" + localLink.getHost() + ") for this DownloadLink." + localLink.getName());
                        }
                    }
                    return links;
                }
            }

            return null;
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.severe("Linklist Konflikt.");
            return null;
        }
    }

    /**
     * Hiermit wird eine Containerdatei geöffnet. Dazu wird zuerst ein passendes
     * Plugin gesucht und danach alle DownloadLinks interpretiert
     * 
     * @param file Die Containerdatei
     */
    public void loadContainerFile(File file) {

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
                // es muss jeweils eine neue plugininstanz erzeugt werden
                try {
                    pContainer = pContainer.getClass().newInstance();
                    progress.setSource(pContainer);
                    fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_DECRYPT_ACTIVE, pContainer));
                    pContainer.initContainer(file.getAbsolutePath());
                    Vector<DownloadLink> links = pContainer.getContainedDownloadlinks();
                    if (links == null || links.size() == 0) {
                        logger.severe("Container Decryption failed (1)");
                    }
                    else {
                        downloadLinks.addAll(links);
                    }
                    fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_DECRYPT_INACTIVE, pContainer));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            progress.increase(1);
        }
        progress.setStatusText(downloadLinks.size() + " links found");
        if (downloadLinks.size() > 0) {
            // schickt die Links zuerst mal zum Linkgrabber
            uiInterface.addLinksToGrabber((Vector<DownloadLink>) downloadLinks);
        }
        progress.finalize();
    }

    public boolean isContainerFile(File file) {

        Vector<PluginForContainer> pluginsForContainer = JDUtilities.getPluginsForContainer();
        // Vector<DownloadLink> downloadLinks = new Vector<DownloadLink>();
        PluginForContainer pContainer;

        for (int i = 0; i < pluginsForContainer.size(); i++) {
            pContainer = pluginsForContainer.get(i);

            if (pContainer.canHandle(file.getName())) {
                return true;
            }

        }

        return false;
    }

    /**
     * Liefert alle DownloadLinks zurück
     * 
     * @return Alle DownloadLinks zurück
     */
    public Vector<DownloadLink> getDownloadLinks() {
        return downloadLinks;
    }

    /**
     * Lädt zum Start das erste Mal alle Links aus einer Datei
     * 
     * @return true/False je nach Erfolg
     */
    public boolean initDownloadLinks() {

        downloadLinks = loadDownloadLinks(JDUtilities.getResourceFile("links.dat"));
        if (downloadLinks == null) {
            downloadLinks = new Vector<DownloadLink>();
            if (uiInterface != null) uiInterface.setDownloadLinks(downloadLinks);
            return false;
        }
        if (uiInterface != null) uiInterface.setDownloadLinks(downloadLinks);
        return true;

    }

    /**
     * Gibt ale links zurück die im selben package sind wie downloadLink
     * 
     * @param downloadLink
     * @return Alle DownloadLinks die zum selben package gehören
     */
    public Vector<DownloadLink> getPackageFiles(DownloadLink downloadLink) {
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        // ret.add(downloadLink);
        Iterator<DownloadLink> iterator = downloadLinks.iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();
            if (downloadLink.getFilePackage() == nextDownloadLink.getFilePackage()) ret.add(nextDownloadLink);
        }
        return ret;
    }

    public Vector<DownloadLink> getpackageFiles(FilePackage filePackage) {
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        // ret.add(downloadLink);

        Iterator<DownloadLink> iterator = downloadLinks.iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();

            if (filePackage == nextDownloadLink.getFilePackage()) ret.add(nextDownloadLink);
        }
        return ret;
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
        if (downloadLink.getStatus() == DownloadLink.STATUS_DONE) i++;
        Iterator<DownloadLink> iterator = downloadLinks.iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();
            if (downloadLink.getFilePackage() == nextDownloadLink.getFilePackage() && nextDownloadLink.getStatus() == DownloadLink.STATUS_DONE) i++;
        }
        return i;
    }

    /**
     * Gibt die Anzahl der fehlenden FIles zurück
     * 
     * @param downloadLink
     * @return Anzahl der fehlenden Files in diesem Paket
     */
    public int getMissingPackageFiles(DownloadLink downloadLink) {
        int i = 0;
        if (downloadLink.getStatus() != DownloadLink.STATUS_DONE) i++;
        Iterator<DownloadLink> iterator = downloadLinks.iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();
            if (downloadLink.getFilePackage() == nextDownloadLink.getFilePackage() && nextDownloadLink.getStatus() != DownloadLink.STATUS_DONE) i++;
        }
        return i;
    }

    /**
     * Liefert die Anzahl der gerade laufenden Downloads. (nur downloads die
     * sich wirklich in der downloadpahse befinden
     * 
     * @return Anzahld er laufenden Downloadsl
     */
    public int getRunningDownloadNum() {
        int ret = 0;
        DownloadLink nextDownloadLink = null;
        for (int i = 0; i < downloadLinks.size(); i++) {
            if (downloadLinks.size() <= i) continue;
            nextDownloadLink = downloadLinks.get(i);
            if (nextDownloadLink.getStatus() == DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS) ret++;
        }
        return ret;
    }

    /**
     * Der Benuter soll den Captcha Code erkennen
     * 
     * @param plugin Das Plugin, das den Code anfordert
     * @param captchaAddress Adresse des anzuzeigenden Bildes
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
        
       if(this.getWatchdog()==null||!getWatchdog().isAlive())return 0;
        return this.getWatchdog().getTotalSpeed();
    }

    /**
     * Fügt einen Listener hinzu
     * 
     * @param listener Ein neuer Listener
     */
    public void addControlListener(ControlListener listener) {
        if (controlListener == null) controlListener = new Vector<ControlListener>();
        if (controlListener.indexOf(listener) == -1) {
            controlListener.add(listener);
        }
    }

    /**
     * Emtfernt einen Listener
     * 
     * @param listener Der zu entfernende Listener
     */
    public void removeControlListener(ControlListener listener) {
        controlListener.remove(listener);
    }

    /**
     * Verteilt Ein Event an alle Listener
     * 
     * @param controlEvent ein abzuschickendes Event
     */
    public void fireControlEvent(ControlEvent controlEvent) {
        // logger.info(controlEvent.getID()+" controllistener "+controlEvent);
        if (uiInterface != null) uiInterface.delegatedControlEvent(controlEvent);
        if (controlListener == null) controlListener = new Vector<ControlListener>();
        Iterator<ControlListener> iterator = controlListener.iterator();
        while (iterator.hasNext()) {
            logger.info(" --> " + ((ControlListener) iterator.next()));
            ((ControlListener) iterator.next()).controlEvent(controlEvent);
        }
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
        for (int i = 0; i < downloadLinks.size(); i++) {
            if (!downloadLinks.elementAt(i).isInProgress()) {
                downloadLinks.elementAt(i).setStatus(DownloadLink.STATUS_TODO);
                downloadLinks.elementAt(i).setStatusText("");
                downloadLinks.elementAt(i).reset();
                requestDownloadLinkUpdate( downloadLinks.elementAt(i));
            }

        }

    }
public void requestDownloadLinkUpdate(DownloadLink link){
    fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, link));
    
}
    public void setUnrarModule(Unrar instance) {
        this.unrarModule = instance;

    }

    public void setInfoFileWriterModule(InfoFileWriter instance) {
        // TODO Auto-generated method stub
        this.infoFileWriterModule = instance;

    }

    public Unrar getUnrarModule() {
        return unrarModule;
    }

    public InfoFileWriter getInfoFileWriterModule() {
        return infoFileWriterModule;
    }

    /**
     * Führt über die in der cnfig gegebenen daten einen reconnect durch.
     * 
     * @return
     */

    public boolean requestReconnect() {
       int wait=0;
        while(isReconnecting){
            try {
                Thread.sleep(500);
            }
            catch (InterruptedException e) {}
            wait+=500;
            
            
        }
        if(wait>0&&lastReconnectSuccess)return true;
        
        Interaction.handleInteraction(Interaction.INTERACTION_BEFORE_RECONNECT, this);

        logger.info("Reconnect: " + JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, true));
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false)) {
            logger.finer("Reconnect is disabled. Enable the CheckBox in the Toolbar to reactivate it");
         
            return false;
        }
        if (this.getRunningDownloadNum() > 0) {
            logger.finer("Downloads are running. reconnect is disabled");
            return false;
        }
        String type = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_RECONNECT_TYPE, null);
        if (type == null) {

            logger.severe("Reconnect is not configured. Config->Reconnect!");
            return false;
        }
      isReconnecting=true;
        boolean ret = false;
        if (type.equals(JDLocale.L("modules.reconnect.types.extern", "Extern"))) {
            ret = new ExternReconnect().interact(null);
        }
        else if (type.equals(JDLocale.L("modules.reconnect.types.batch", "Batch"))) {
            ret = new BatchReconnect().interact(null);
        }
        else {
            ret = new HTTPLiveHeader().interact(null);
        }
        isReconnecting=false;
        lastReconnectSuccess=ret;
        logger.info("Reconnect success: " + ret);
        if (ret) {
            Iterator<DownloadLink> iterator = downloadLinks.iterator();

            DownloadLink i;
            while (iterator.hasNext()) {
                i = iterator.next();
                if (i.getRemainingWaittime() > 0) {
                    i.setEndOfWaittime(0);
                    logger.finer("REset GLOBALS: " + ((PluginForHost) i.getPlugin()));
                    ((PluginForHost) i.getPlugin()).resetPluginGlobals();
                    i.setStatus(DownloadLink.STATUS_TODO);

                }
            }

        }
        if (ret) {
            Interaction.handleInteraction(Interaction.INTERACTION_AFTER_RECONNECT, this);
        }
        return ret;
    }

    public ClipboardHandler getClipboard() {
        if (clipboard == null) {
            clipboard = new ClipboardHandler();
        }
        return clipboard;
    }

    public void setClipboard(ClipboardHandler clipboard) {
        this.clipboard = clipboard;
    }

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

}
