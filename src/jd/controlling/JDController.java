package jd.controlling;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.JDInit;

import jd.config.Configuration;
import jd.config.SubConfiguration;
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

    public static final int INIT_STATUS_COMPLETE = 0;

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

    private int initStatus=-1;

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
    private void initInteractions() {
        Vector<Interaction> interactions = JDUtilities.getConfiguration().getInteractions();

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
                if (links != null && links instanceof Vector && ((Vector) links).size() > 0) {
                    // schickt die Links zuerst mal zum Linkgrabber
                    uiInterface.addLinksToGrabber((Vector<DownloadLink>) links);
                }
                break;
            case ControlEvent.CONTROL_PLUGIN_INTERACTION_INACTIVE:
          //      Interaction interaction = (Interaction) event.getParameter();
         
              

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
            case UIEvent.UI_SAVE_CONFIG:

                JDUtilities.saveObject(null, JDUtilities.getConfiguration(), JDUtilities.getJDHomeDirectoryFromEnvironment(), JDUtilities.CONFIG_PATH.split("\\.")[0], "." + JDUtilities.CONFIG_PATH.split("\\.")[1], Configuration.saveAsXML);
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

               
               // Interaction.handleInteraction(Interaction.INTERACTION_NEED_RECONNECT, this);
                if (reconnect()) {
                    uiInterface.showMessageDialog("Reconnect erfolgreich");

                }
                else {

                    uiInterface.showMessageDialog("Reconnect fehlgeschlagen");

                }
              
                uiInterface.setDownloadLinks(downloadLinks);
                break;
            case UIEvent.UI_INTERACT_UPDATE:
                new JDInit().doWebupdate(JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1),true);
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

    public void saveDLC(File file) {
        // JDUtilities.saveObject(null, downloadLinks.toArray(new
        // DownloadLink[]{}), file, "links", "dat", true);
SubConfiguration cfg = JDUtilities.getSubConfig("DLCCONFIG");
        String xml = "";
        xml += "<header>";
        xml += "<generator>";
        xml += "<app>jDownloader</app>";
        xml += "<version>" + JDUtilities.getRevision() + "</version>";
        xml += "<url>http://jdownloader.ath.cx</url>";
        xml += "</generator>";
        xml += "<tribute>";
        if(cfg.getBooleanProperty("ASK_ADD_INFOS", false)){
            if((cfg.getStringProperty("UPLOADERNAME", null)==null||cfg.getStringProperty("UPLOADERNAME", null).trim().length()==0)){
                
            }else{           xml += "<name>" +JDUtilities.Base64Encode(this.getUiInterface().showUserInputDialog("Uploader Name")) + "</name>";
                xml += "<name>" +cfg.getStringProperty("UPLOADERNAME", null) + "</name>";
                    
            }
        }else{
            xml += "<name>"+"unknown" + "</name>";
              
        }
        
        xml += "</tribute>";
        if(cfg.getBooleanProperty("ASK_ADD_INFOS", false)){
        xml += "<comment>" + JDUtilities.Base64Encode((this.getUiInterface().showUserInputDialog("Comment"))) + "</comment>";
        }
        if(cfg.getBooleanProperty("ASK_ADD_INFOS", false)){
        xml += "<category>" + JDUtilities.Base64Encode((this.getUiInterface().showUserInputDialog("Category"))) + "</category>";
        }else{
            xml += "<category>various</category>";
                
        }
        xml += "</header>";
        xml += "<content>";
        Vector<FilePackage> packages = new Vector<FilePackage>();
        Vector<DownloadLink> links = this.getDownloadLinks();

        for (int i = 0; i < links.size(); i++) {
            if (!packages.contains(links.get(i).getFilePackage())) {
                packages.add(links.get(i).getFilePackage());
            }
        }
        logger.info("Found " + packages.size());
        for (int i = 0; i < packages.size(); i++) {
            xml += "<package name=\"" + JDUtilities.Base64Encode(packages.get(i).getName()) + "\">";

            Vector<DownloadLink> tmpLinks = this.getPackageFiles(packages.get(i));
            for (int x = 0; x < tmpLinks.size(); x++) {
                xml += "<file>";
                xml += "<url>" + JDUtilities.Base64Encode(tmpLinks.get(x).getUrlDownloadDecrypted()) + "</url>";
                xml += "<password>" + JDUtilities.Base64Encode(packages.get(i).getPassword()) + "</password>";
                xml += "<comment>" + JDUtilities.Base64Encode(packages.get(i).getComment()) + "</comment>";
                //logger.info("Found pw" + packages.get(i).getPassword());
                //logger.info("Found comment" + packages.get(i).getComment());
                xml += "</file>";
            }
            xml += "</package>";
        }

        xml += "</content>";
        logger.info(xml);
       // logger.info(xml);
        String[] encrypt = JDUtilities.encrypt(xml, "DLC Parser");

       // logger.info(encrypt[1] + " - ");
        if (encrypt == null) {
            logger.severe("Container Encryption failed.");
            this.getUiInterface().showMessageDialog("JDTC Encryption failed.");
            return;

        }
        String key = encrypt[1];
        xml = encrypt[0];

        try {
            URL[] services = new URL[] { new URL("http://dlcrypt.ath.cx/service.php"), new URL("http://recrypt1.ath.cx/service.php") };
            int url = 0;
            while (url < services.length) {
                // Get redirect url
                logger.finer("Call " + services[url]);
                RequestInfo ri = Plugin.getRequestWithoutHtmlCode(services[url], null, null, false);
                if (!ri.isOK() || ri.getLocation() == null) {
                    url++;
                    continue;
                }
                
                http://web146.donau.serverway.de/jdownloader/recrypt/service.php?jd=1&srcType=jdtc&data=
                logger.finer("Call Redirect: " + ri.getLocation() + " - " + "jd=1&srcType=jdtc&data=" + key);

                ri = Plugin.postRequest(new URL(ri.getLocation()), null, null, null, "jd=1&srcType=jdtc&data=" + key, true);
                // CHeck ob der call erfolgreich war
logger.info("Call re: "+ri.getHtmlCode());
                if (!ri.isOK() || !ri.containsHTML("<rc>")) {
                    url++;
                    continue;
                }
                else {
                    String dlcKey = ri.getHtmlCode();

                    dlcKey = Plugin.getBetween(dlcKey, "<rc>", "</rc>");
                    if (dlcKey.trim().length() < 80) {
                        JDUtilities.getGUI().showMessageDialog(JDLocale.L("sys.dlc.error_version", "DLC Encryption fehlgeschlagen. Bitte stellen Sie sicher dass Sie die aktuellste JD-Version verwenden."));
                        return;
                    }
                    //logger.info("DLC KEy: " + dlcKey);
                    
                    JDUtilities.writeLocalFile(file, xml + dlcKey);
                   if(this.getUiInterface().showConfirmDialog(JDLocale.L("sys.dlc.success", "DLC encryption successfull. Run Testdecrypt now?"))){
                       loadContainerFile(file);
                   }

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
        logger.severe("Container creation failed");
        this.getUiInterface().showMessageDialog("Container encryption failed");
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
                                pluginForContainer = JDUtilities.getPluginForContainer(localLink.getContainer());
                                if (pluginForContainer != null) {
                                    pluginForContainer = pluginForContainer.getPlugin(localLink.getContainerFile());
                                    // pluginForContainer.
                                    pluginForContainer.initContainer(localLink.getContainerFile());
                                    pluginForContainer.getContainedDownloadlinks();
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
                    if(links==null||links.size()==0){
                        logger.severe("Container Decryption failed"); 
                    }else{
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
     * Gibt ale links zurück die im selben Package sind wie downloadLink
     * 
     * @param downloadLink
     * @return Alle DownloadLinks die zum selben Package gehören
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

    public Vector<DownloadLink> getPackageFiles(FilePackage filepackage) {
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        // ret.add(downloadLink);
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
    public int getSpeedMeter() {
    	Iterator<DownloadLink> iter = getDownloadLinks().iterator();
    	int ret=0;
    	while (iter.hasNext()) {
			DownloadLink element = (DownloadLink) iter.next();
			if(element.isInProgress())
			{
			ret+=element.getDownloadSpeed();
			}
		}
        return (int) ret;
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
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLinks.elementAt(i)));
            }

        }

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

    public boolean reconnect() {
        Interaction.handleInteraction(Interaction.INTERACTION_BEFORE_RECONNECT, this);
        
        logger.info("Reconnect: " + JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, true));
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false)) {
            logger.finer("Reconnect is disabled. Enable the CheckBox in the Toolbar to reactivate it");
            return false;
        }
        if(this.getRunningDownloadNum()>0){
            logger.finer("Downloads are running. reconnect is disabled");
            return false;
        }
        String type = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_RECONNECT_TYPE, null);
        if (type == null) {

            logger.severe("Reconnect is not configured. Config->Reconnect!");
            return false;
        }
        boolean ret = false;
        if (type.equals("Extern")) {
            ret = new ExternReconnect().interact(null);
        }else{
            ret = new HTTPLiveHeader().interact(null);
        }
      
        logger.info("Reconnect success: " + ret);
        if (ret) {
            Iterator<DownloadLink> iterator = downloadLinks.iterator();

            DownloadLink i;
            while (iterator.hasNext()) {
                i = iterator.next();
                if (i.getRemainingWaittime() > 0) {
                    i.setEndOfWaittime(0);
                    logger.finer("REset GLOBALS: "+((PluginForHost) i.getPlugin()));
                    ((PluginForHost) i.getPlugin()).resetPluginGlobals();
                    i.setStatus(DownloadLink.STATUS_TODO);

                }
            }

        }
        if(ret){
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
        this.initStatus= initStatus;
    }

    public int getInitStatus() {
        return initStatus;
    }

}
