package jd.controlling;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.SingleInstanceController;
import jd.config.Configuration;
import jd.controlling.interaction.HTTPReconnect;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.WebUpdate;
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
import jd.utils.JDUtilities;

/**
 * Im Controller wird das ganze App gesteuert. Evebnts werden deligiert.
 * 
 * @author coalado/astaldo
 * 
 */
public class JDController implements PluginListener, ControlListener, UIListener {
    public static final int                   DOWNLOAD_TERMINATION_IN_PROGRESS = 0;

    public static final int                   DOWNLOAD_RUNNING                 = 2;

    public static final int                   DOWNLOAD_NOT_RUNNING             = 3;

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
    private Logger                            logger                           = Plugin.getLogger();

    private File                              lastCaptchaLoaded;

    private SpeedMeter                        speedMeter;

    private DownloadLink                      lastDownloadFinished;

    private ClipboardHandler                  clipboard;

    /**
     * Der Download Watchdog verwaltet die Downloads
     */
    private DownloadWatchDog                  watchdog;

    public JDController() {
        downloadLinks = new Vector<DownloadLink>();
        speedMeter = new SpeedMeter(10000);
        clipboard = new ClipboardHandler(this);
        downloadStatus = DOWNLOAD_NOT_RUNNING;
        JDUtilities.setController(this);
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
        SingleInstanceController.unbindRMIObject();
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
            case PluginEvent.PLUGIN_DOWNLOAD_BYTES:
                speedMeter.addValue((Integer) event.getParameter1());
                break;

        }
    }

    /**
     * Hier werden ControlEvent ausgewertet
     * 
     * @param event
     */

    public void controlEvent(ControlEvent event) {
        switch (event.getID()) {
            case ControlEvent.CONTROL_SINGLE_DOWNLOAD_FINISHED:
                lastDownloadFinished = (DownloadLink) event.getParameter();
                saveDownloadLinks(JDUtilities.getResourceFile("links.dat"));
                if (this.getMissingPackageFiles(lastDownloadFinished) == 0) {
                    Interaction.handleInteraction(Interaction.INTERACTION_DOWNLOAD_PACKAGE_FINISHED, this);
                }

                if (lastDownloadFinished.getStatus() == DownloadLink.STATUS_DONE && Configuration.FINISHED_DOWNLOADS_REMOVE.equals(JDUtilities.getConfiguration().getProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION))) {
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
                if (links != null && links instanceof Vector && ((Vector) links).size() > 0) {
                    // schickt die Links zuerst mal zum Linkgrabber
                    uiInterface.addLinksToGrabber((Vector<DownloadLink>) links);
                }
                break;
            case ControlEvent.CONTROL_PLUGIN_INTERACTION_INACTIVE:
                Interaction interaction = (Interaction) event.getParameter();
                // Macht einen Wartezeit reset wenn die HTTPReconnect
                // Interaction eine neue IP gebracht hat
                if (interaction instanceof HTTPReconnect && interaction.getCallCode() == Interaction.INTERACTION_CALL_SUCCESS) {
                    Iterator<DownloadLink> iterator = downloadLinks.iterator();
                    // stellt die Wartezeiten zurück
                    DownloadLink i;
                    while (iterator.hasNext()) {
                        i = iterator.next();
                        if (i.getRemainingWaittime() > 0) {
                            i.setEndOfWaittime(0);
                            i.setStatus(DownloadLink.STATUS_TODO);
                        }
                    }
         
                }
                else if (interaction instanceof WebUpdate) {
                    if (interaction.getCallCode() == Interaction.INTERACTION_CALL_ERROR) {
                        // uiInterface.showMessageDialog("Keine Updates
                        // verfügbar");
                    }
                    else {
                        uiInterface.showMessageDialog("Aktualisierte Dateien: " + ((WebUpdate) interaction).getUpdater().getUpdatedFiles());
                    }
                }

                break;
            default:

                break;
        }
        uiInterface.delegatedControlEvent(event);
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
            case UIEvent.UI_SAVE_CONFIG:

                JDUtilities.saveObject(null, JDUtilities.getConfiguration(), JDUtilities.getJDHomeDirectory(), JDUtilities.CONFIG_PATH.split("\\.")[0], "." + JDUtilities.CONFIG_PATH.split("\\.")[1], Configuration.saveAsXML);
                break;
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
            case UIEvent.UI_LOAD_CONTAINER:
                File containerFile = (File) uiEvent.getParameter();
                loadContainerFile(containerFile);
                break;
            case UIEvent.UI_EXIT:
                exit();
                break;

            case UIEvent.UI_SET_CLIPBOARD:
                this.clipboard.setEnabled((Boolean) uiEvent.getParameter());
                break;
            case UIEvent.UI_LINKS_CHANGED:
                newLinks = uiInterface.getDownloadLinks();
                abortDeletedLink(downloadLinks, newLinks);
                downloadLinks = newLinks;
                saveDownloadLinks(JDUtilities.getResourceFile("links.dat"));
                break;
            case UIEvent.UI_INTERACT_RECONNECT:
                if (getRunningDownloadNum() > 0) {
                    logger.info("Es laufen noch Downloads. Breche zum reconnect Downloads ab!");
                    stopDownloads();
                }
                if (Interaction.handleInteraction(Interaction.INTERACTION_NEED_RECONNECT, this)) {
                    uiInterface.showMessageDialog("Reconnect erfolgreich");
                    Iterator<DownloadLink> iterator = downloadLinks.iterator();
                    // stellt die Wartezeiten zurück
                    DownloadLink i;
                    while (iterator.hasNext()) {
                        i = iterator.next();
                        if (i.getRemainingWaittime() > 0) {
                            i.setEndOfWaittime(0);
                            i.setStatus(DownloadLink.STATUS_TODO);

                        }
                    }

                    if (Interaction.getInteractions(Interaction.INTERACTION_NEED_RECONNECT).length != 1) {
                        uiInterface.showMessageDialog("Es sind " + Interaction.getInteractions(Interaction.INTERACTION_NEED_RECONNECT).length + " Interactionen für den Reconnect festgelegt. \r\nEventl. wurde der Reconnect mehrmals ausgeführt. \r\nBitte Event einstellen (Konfiguration->Eventmanager)");

                    }

                }
                else {

                    if (Interaction.getInteractions(Interaction.INTERACTION_NEED_RECONNECT).length != 1) {
                        uiInterface.showMessageDialog("Reconnect fehlgeschlagen\r\nEs ist kein Event(oder mehrere) für die Reconnect festgelegt. \r\nBitte Event einstellen (Konfiguration->Eventmanager)");
                    }
                    else {
                        uiInterface.showMessageDialog("Reconnect fehlgeschlagen");
                    }
                }
                uiInterface.setDownloadLinks(downloadLinks);
                break;
            case UIEvent.UI_INTERACT_UPDATE:
                WebUpdate wu = new WebUpdate();
                wu.addControlListener(this);
                wu.interact(this);
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
        if (watchdog == null) return;
        for (int i = 0; i < oldLinks.size(); i++) {
            if (newLinks.indexOf(oldLinks.elementAt(i)) == -1) {
                // Link gefunden der entfernt wurde

                oldLinks.elementAt(i).setAborted(true);
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
    
    public void saveDLC(File file){
        // JDUtilities.saveObject(null, downloadLinks.toArray(new
        // DownloadLink[]{}), file, "links", "dat", true);

        String xml = "<dlc>";
        xml += "<header>";
        xml += "<generator>";
        xml += "<app>jDownloader</app>";
        xml += "<version>" + JDUtilities.getRevision() + "</version>";
        xml += "<url>http://jdownloader.ath.cx</url>";
        xml += "</generator>";
        xml += "<tribute>";
        xml += "<name>" + this.getUiInterface().showUserInputDialog("Uploader Name") + "</name>";
        xml += "</tribute>";
        xml += "<comment>" + this.getUiInterface().showUserInputDialog("Comment") + "</comment>";
        xml += "<category>" + this.getUiInterface().showUserInputDialog("Category") + "</category>";
        xml += "</header>";
        xml += "<content>";
        Vector<FilePackage> packages = new Vector<FilePackage>();
        Vector<DownloadLink> links = this.getDownloadLinks();

        for (int i = 0; i < links.size(); i++) {
            if (!packages.contains(links.get(i).getFilePackage())) {
                packages.add(links.get(i).getFilePackage());
            }
        }
        for (int i = 0; i < packages.size(); i++) {
            xml += "<package name=\"" + packages.get(i).getName() + "\">";

            Vector<DownloadLink> tmpLinks = this.getPackageFiles(packages.get(i));
            for (int x = 0; x < tmpLinks.size(); x++) {
                xml += "<file>";
                xml += "<url>" + tmpLinks.get(x).getUrlDownloadDecrypted() + "</url>";
                xml += "<password>" + packages.get(i).getPassword() + "</password>";
                xml += "<comment>" + packages.get(i).getComment() + "</comment>";
                xml += "</file>";
            }
            xml += "</package>";
        }

        xml += "</content>";
        xml += "</dlc>";
        
        xml=JDUtilities.urlEncode(xml);
        try {
            URL[] services = new URL[] { new URL("http://recrypt1.ath.cx/service.php"), new URL("http://recrypt2.ath.cx/service.php"), new URL("http://recrypt3.ath.cx/service.php") };
            int url = 0;
            while (url < services.length) {
                //Get redirect url
                logger.finer("Call "+services[url]);
                RequestInfo ri = Plugin.getRequestWithoutHtmlCode(services[url], null, null, false);
                if (!ri.isOK() || ri.getLocation()==null) {
                    url++;
                    continue;
                }
                logger.finer("Call Redirect: "+ri.getLocation());
                ri = Plugin.postRequest(new URL(ri.getLocation()), null, null, null, "data="+xml, true);
                // CHeck ob der call erfolgreich war
                
             
                
                if (!ri.isOK() || !ri.containsHTML("dlc")) {
                    url++;
                    continue;
                }
                else {
                    String dlcString = ri.getHtmlCode();
                    JDUtilities.writeLocalFile(file, dlcString);
                    this.getUiInterface().showMessageDialog("DLC encryption successfull");
                    
                    return;
                }

            }
        }
        catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        logger.severe("DLC creation failed");
        this.getUiInterface().showMessageDialog("DLC encryption failed");
    }

    /**
     * Lädt eine LinkListe
     * 
     * @param file Die Datei, aus der die Links gelesen werden
     * @return Ein neuer Vector mit den DownloadLinks
     */
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
                        }
                        // Gibt es einen Names für ein Containerformat, wird ein
                        // passendes Plugin gesucht
                        try {
                            if (localLink.getContainer() != null) {
                                pluginForContainer = JDUtilities.getPluginForContainer(localLink.getContainer());
                                if (pluginForContainer != null) {
                                    pluginForContainer = pluginForContainer.getClass().newInstance();
                                    pluginForContainer.getAllDownloadLinks(localLink.getContainerFile());
                                }
                                else
                                    localLink.setEnabled(false);
                            }
                        }
                        catch (InstantiationException e) {
                            e.printStackTrace();
                        }
                        catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        catch (NullPointerException e) {
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
    private void loadContainerFile(File file) {
        Vector<PluginForContainer> pluginsForContainer = JDUtilities.getPluginsForContainer();
        Vector<DownloadLink> downloadLinks = new Vector<DownloadLink>();
        PluginForContainer pContainer;
        for (int i = 0; i < pluginsForContainer.size(); i++) {
            pContainer = pluginsForContainer.get(i);
            if (pContainer.canHandle(file.getName())) {
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_DECRYPT_ACTIVE, pContainer));
                downloadLinks.addAll(pContainer.getAllDownloadLinks(file.getAbsolutePath()));
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_DECRYPT_INACTIVE, pContainer));
            }
        }
        if (downloadLinks.size() > 0) {
            // schickt die Links zuerst mal zum Linkgrabber
            uiInterface.addLinksToGrabber((Vector<DownloadLink>) downloadLinks);
        }
    }

    /**
     * Liefert alle DownloadLinks zurück
     * 
     * @return Alle DownloadLinks zurück
     */
    protected Vector<DownloadLink> getDownloadLinks() {
        return downloadLinks;
    }

    /**
     * Setzt alle DownloadLinks neu
     * 
     * @param links Die neuen DownloadLinks
     */
    protected void setDownloadLinks(Vector<DownloadLink> links) {
        downloadLinks = links;

    }

    /**
     * Lädt zum Start das erste Mal alle Links aus einer Datei
     * 
     * @return true/False je nach Erfolg
     */
    public boolean initDownloadLinks() {
        Vector<DownloadLink> list = loadDownloadLinks(JDUtilities.getResourceFile("links.dat"));
        if (list != null) {
            downloadLinks = loadDownloadLinks(JDUtilities.getResourceFile("links.dat"));
            if (uiInterface != null) uiInterface.setDownloadLinks(downloadLinks);
            return true;
        }
        else {

            return false;
        }
    }

    /**
     * Gibt ale links zurück die im selben Package sind wie downloadLink
     * 
     * @param downloadLink
     * @return Alle DownloadLinks die zum selben Package gehören
     */
    public Vector<DownloadLink> getPackageFiles(DownloadLink downloadLink) {
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
//        ret.add(downloadLink);
        Iterator<DownloadLink> iterator = downloadLinks.iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();
            if (downloadLink.getFilePackage() == nextDownloadLink.getFilePackage()) ret.add(nextDownloadLink);
        }
        return ret;
    }
    public Vector<DownloadLink> getPackageFiles(FilePackage filepackage) {
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
//        ret.add(downloadLink);
        Iterator<DownloadLink> iterator = downloadLinks.iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();
            if (filepackage == nextDownloadLink.getFilePackage()) ret.add(nextDownloadLink);
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
        Iterator<DownloadLink> iterator = downloadLinks.iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();
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
    public String getCaptchaCodeFromUser(Plugin plugin, File captchaAddress) {
        String captchaCode = uiInterface.getCaptchaCodeFromUser(plugin, captchaAddress);
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
    public SpeedMeter getSpeedMeter() {
        return speedMeter;
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
        uiInterface.delegatedControlEvent(controlEvent);
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
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLinks.elementAt(i)));
            }
            
        }

    }

}
