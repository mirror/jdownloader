package jd.controlling;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.JDUtilities;
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
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.event.PluginEvent;
import jd.plugins.event.PluginListener;

/**
 * Im Controller wird das ganze App gesteuert. Evebnts werden deligiert.
 * 
 * @author coalado/astaldo
 * 
 */
public class JDController implements PluginListener, ControlListener, UIListener {

    /**
     * Mit diesem Thread wird eingegebener Text auf Links untersucht
     */
    private DistributeData                    distributeData  = null;

    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #firePluginEvent(PluginEvent)} ein Event losgeschickt wird.
     */
    private transient Vector<ControlListener> controlListener = null;

    /**
     * Die Konfiguration
     */
    protected Configuration                   config          = JDUtilities.getConfiguration();

    /**
     * Schnittstelle zur Benutzeroberfläche
     */
    private UIInterface                       uiInterface;

    /**
     * Die DownloadLinks
     */
    private Vector<DownloadLink>              downloadLinks;

    /**
     * Der Logger
     */
    private Logger                            logger          = Plugin.getLogger();

    private File                              lastCaptchaLoaded;

    private SpeedMeter                        speedMeter;

    private Vector<StartDownloads>            activeLinks     = new Vector<StartDownloads>();

    private DownloadLink                      lastDownloadFinished;

    private ClipboardHandler                  clipboard;

    private boolean                           aborted         = false;

    private DownloadWatchDog                  watchdog;

    /**
     * 
     */
    public JDController() {
        downloadLinks = new Vector<DownloadLink>();
        speedMeter = new SpeedMeter(5000);
        clipboard = new ClipboardHandler(this);
        JDUtilities.setController(this);
    }

    /**
     * Startet den Downloadvorgang. Dies eFUnkton sendet das startdownload event
     * und aktiviert die ersten downloads
     */
    private void startDownloads() {
        if(watchdog==null){
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
     * Eventfunktion für den PLuginlistener
     * 
     * @param event PluginEvent
     */
    public void pluginEvent(PluginEvent event) {
        uiInterface.deligatedPluginEvent(event);
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
                
                if(Configuration.FINISHED_DOWNLOADS_REMOVE.equals(JDUtilities.getConfiguration().getProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION))){
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

                break;
            default:

                break;
        }
        uiInterface.deligatedControlEvent(event);
    }

    private void stopDownloads() {
        if (watchdog != null) {
            watchdog.abort();
            watchdog = null;
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
                saveDownloadLinks(file);
                break;
            case UIEvent.UI_LOAD_LINKS:
                file = (File) uiEvent.getParameter();
                loadDownloadLinks(file);
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

    /**
     * bricht downloads ab wenn diese entfernt wurden
     * 
     * @param oldLinks
     * @param newLinks
     */
    private void abortDeletedLink(Vector<DownloadLink> oldLinks, Vector<DownloadLink> newLinks) {
        if(watchdog==null)return;
        for (int i = 0; i < oldLinks.size(); i++) {
            if (newLinks.indexOf(oldLinks.elementAt(i)) == -1) {
                // Link gefunden der entfernt wurde
                
                oldLinks.elementAt(i).setAborted(true);
                watchdog.abortDownloadLink( oldLinks.elementAt(i));
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
                PluginForHost pluginForHost=null;
                while (iterator.hasNext()) {
                    localLink = iterator.next();
                  if(localLink.getStatus()==DownloadLink.STATUS_DONE&&Configuration.FINISHED_DOWNLOADS_REMOVE_AT_START.equals(JDUtilities.getConfiguration().getProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION))){
                      iterator.remove();
                      continue;
                  }
                    //Neue instanz. jeder Link braucht seine eigene Plugininstanz
                    try {
                        pluginForHost = JDUtilities.getPluginForHost(localLink.getHost()).getClass().newInstance();
                    }
                    catch (InstantiationException e) {
                       
                        e.printStackTrace();
                    }
                    catch (IllegalAccessException e) {
                        
                        e.printStackTrace();
                    }
                    if (pluginForHost != null) {
                        localLink.setLoadedPlugin(pluginForHost);
                        pluginForHost.addPluginListener(this);
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
     * Gibt ale links zurück die im selben Package sind wie downloadLink
     * 
     * @param downloadLink
     * @return Alle DownloadLinks die zum selben Package gehören
     */
    public Vector<DownloadLink> getPackageFiles(DownloadLink downloadLink) {
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        ret.add(downloadLink);
        Iterator<DownloadLink> iterator = downloadLinks.iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();
            if (downloadLink.getFilePackage() == nextDownloadLink.getFilePackage()) ret.add(nextDownloadLink);
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
        if (controlListener == null) controlListener = new Vector<ControlListener>();
        Iterator<ControlListener> iterator = controlListener.iterator();
        while (iterator.hasNext()) {
            ((ControlListener) iterator.next()).controlEvent(controlEvent);
        }
    }

}
