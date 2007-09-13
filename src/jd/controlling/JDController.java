package jd.controlling;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.Configuration;
import jd.JDUtilities;
import jd.controlling.interaction.HTTPReconnect;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.WebUpdate;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.event.UIEvent;
import jd.event.UIListener;
import jd.gui.UIInterface;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.event.PluginEvent;
import jd.plugins.event.PluginListener;

public class JDController implements PluginListener, ControlListener, UIListener {
    /**
     * Der Thread, der das Downloaden realisiert
     */
    private StartDownloads       download       = null;

    /**
     * Mit diesem Thread wird eingegebener Text auf Links untersucht
     */
    private DistributeData       distributeData = null;

    /**
     * Die Konfiguration
     */
    protected Configuration      config         = JDUtilities.getConfiguration();

    /**
     * Schnittstelle zur Benutzeroberfläche
     */
    private UIInterface          uiInterface;

    /**
     * Die DownloadLinks
     */
    private Vector<DownloadLink> downloadLinks;

    /**
     * Der Logger
     */
    private Logger               logger         = Plugin.getLogger();

    private File                 lastCaptchaLoaded;
    private SpeedMeter speedMeter;
    private DownloadLink         lastDownloadFinished;

    public JDController() {
        downloadLinks = new Vector<DownloadLink>();
        speedMeter= new SpeedMeter(5000);
    
        JDUtilities.setController(this);
    }

    /**
     * Startet den Downloadvorgang
     */
    private void startDownloads() {
        if (download == null) {
            download = new StartDownloads(this);
            download.addControlListener(this);
            download.start();
        }
        else {
            logger.warning("download still active");
        }
    }

    private void stopDownloads() {
        if (download != null) {
            download.abortDownload();
            // download.interrupt();
            download = null;
        }
        else {
            logger.warning("no active download");
        }
    }

    /**
     * Beendet das Programm
     */
    private void exit() {
        saveDownloadLinks(JDUtilities.getResourceFile("links.dat"));
        System.exit(0);
    }

    public void pluginEvent(PluginEvent event) {
        uiInterface.uiPluginEvent(event);
        switch (event.getEventID()){
            case PluginEvent.PLUGIN_DOWNLOAD_BYTES:
                speedMeter.addValue((Integer)event.getParameter1());
                break;
                
        }
    }

    /**
     * Hier werden ControlEvent ausgewertet
     */
    @SuppressWarnings("unchecked")
    public void controlEvent(ControlEvent event) {

        switch (event.getID()) {
     
            case ControlEvent.CONTROL_SINGLE_DOWNLOAD_FINISHED:
                lastDownloadFinished = (DownloadLink) event.getParameter();
                break;
            case ControlEvent.CONTROL_CAPTCHA_LOADED:
                lastCaptchaLoaded = (File) event.getParameter();
                break;

            case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                download = null;
                uiInterface.uiControlEvent(event);
                break;
            case ControlEvent.CONTROL_DISTRIBUTE_FINISHED:
                Object links = event.getParameter();
                if (links != null && links instanceof Vector && ((Vector) links).size() > 0) {
                    downloadLinks.addAll((Vector<DownloadLink>) links);
                }
                saveDownloadLinks(JDUtilities.getResourceFile("links.dat"));
                uiInterface.setDownloadLinks(downloadLinks);
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
                    Interaction.handleInteraction(Interaction.INTERACTION_AFTER_RECONNECT, this);
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
                uiInterface.uiControlEvent(event);
                break;
            default:
                uiInterface.uiControlEvent(event);
                break;
        }
    }

    public void uiEvent(UIEvent uiEvent) {
        switch (uiEvent.getActionID()) {
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
                JDUtilities.saveObject(null, JDUtilities.getConfiguration(), JDUtilities.getJDHomeDirectory(), "jdownloader", ".config", Configuration.saveAsXML);
                break;
            case UIEvent.UI_SAVE_LINKS:
                File file = (File) uiEvent.getParameter();
                saveDownloadLinks(file);
                break;
            case UIEvent.UI_LOAD_LINKS:
                file = (File) uiEvent.getParameter();
                loadDownloadLinks(file);
                break;
            case UIEvent.UI_EXIT:
                exit();
                break;
            case UIEvent.UI_LINKS_CHANGED:
                downloadLinks = uiInterface.getDownloadLinks();
                saveDownloadLinks(JDUtilities.getResourceFile("links.dat"));
                break;
            case UIEvent.UI_INTERACT_RECONNECT:
                if(getRunningDownloadNum()>0){
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

    /**
     * Lädt eine LinkListe
     * 
     * @param file Die Datei, aus der die Links gelesen werden
     * @return Ein neuer Vector mit den DownloadLinks
     */
    public Vector<DownloadLink> loadDownloadLinks(File file) {
        if (file.exists()) {
            Object obj = JDUtilities.loadObject(null, file, Configuration.saveAsXML);
            if (obj != null && obj instanceof Vector) {
                Vector<DownloadLink> links = (Vector<DownloadLink>) obj;
                Iterator<DownloadLink> iterator = links.iterator();
                DownloadLink localLink;
                PluginForHost pluginForHost;
                while (iterator.hasNext()) {
                    localLink = iterator.next();
                    pluginForHost = JDUtilities.getPluginForHost(localLink.getHost());
                    if (pluginForHost != null) {
                        localLink.setLoadedPlugin(pluginForHost);
                    }
                    else {
                        logger.severe("couldn't find plugin(" + localLink.getHost() + ") for this DownloadLink." + localLink.getName());
                    }
                }
                return links;
            }
        }
        return new Vector<DownloadLink>();
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
     */
    public void initDownloadLinks() {
        downloadLinks = loadDownloadLinks(JDUtilities.getResourceFile("links.dat"));
        if (uiInterface != null) uiInterface.setDownloadLinks(downloadLinks);
    }

    /**
     * Liefert den nächsten DownloadLink
     * 
     * @return Der nächste DownloadLink oder null
     */
    public DownloadLink getNextDownloadLink() {
        Iterator<DownloadLink> iterator = downloadLinks.iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();
            if (nextDownloadLink.isEnabled() && nextDownloadLink.getStatus() == DownloadLink.STATUS_TODO && nextDownloadLink.getRemainingWaittime() == 0) return nextDownloadLink;
        }
        return null;
    }

    /**
     * Liefert die Anzahl der gerade laufenden Downloads. (nur downloads die
     * sich wirklich in der downloadpahse befinden
     * 
     * @returnAnzahld er laufenden Downloadsl
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
        return uiInterface.getCaptchaCodeFromUser(plugin, captchaAddress);
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

    public String getLastFinishedFile() {
        if (this.lastDownloadFinished == null) return "";
        return this.lastDownloadFinished.getFileOutput();
    }

    public String getLastCaptchaImage() {
        if (this.lastCaptchaLoaded == null) return "";
        return this.lastCaptchaLoaded.getAbsolutePath();
    }

    public SpeedMeter getSpeedMeter() {
        return speedMeter;
    }



}
