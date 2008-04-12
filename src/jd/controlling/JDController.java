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
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.controlling;

import java.io.File;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import jd.JDInit;
import jd.config.Configuration;
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

    /**
     * Mit diesem Thread wird eingegebener Text auf Links untersucht
     */
    private DistributeData distributeData = null;

    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #firePluginEvent(PluginEvent)} ein Event losgeschickt wird.
     */
    private transient Vector<ControlListener> controlListener = null;

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

    private ClipboardHandler clipboard;

    /**
     * Der Download Watchdog verwaltet die Downloads
     */
    private DownloadWatchDog watchdog;

    private Vector<DownloadLink> finishedLinks = new Vector<DownloadLink>();

    private Unrar unrarModule;

    private InfoFileWriter infoFileWriterModule;

    // public static Property FLAGS = new Property();

    private int initStatus = -1;

    private Vector<Vector<String>> waitingUpdates;

    private boolean isReconnecting;

    private boolean lastReconnectSuccess;
    private FilePackage fp;

    public JDController() {

        packages = new Vector<FilePackage>();
        clipboard = new ClipboardHandler();
        downloadStatus = DOWNLOAD_NOT_RUNNING;
        fp = new FilePackage();
        fp.setName(JDLocale.L("controller.packages.defaultname", "various"));

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

        System.exit(0);
    }

    /**
     * Hier werden ControlEvent ausgewertet
     * 
     * @param event
     */

    @SuppressWarnings("unchecked")
    public void controlEvent(ControlEvent event) {
        switch (event.getID()) {
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

            } else {
                saveDownloadLinks();
            }

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

        case ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED:
            // Interaction interaction = (Interaction) event.getParameter();
            this.saveDownloadLinks();
            break;
        default:

            break;
        }
        // if (uiInterface != null) uiInterface.delegatedControlEvent(event);
    }

    private void removeDownloadLink(DownloadLink link) {
        synchronized (packages) {
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
        logger.severe("Link " + link + " does not belong to any Package");

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
    @SuppressWarnings("unchecked")
    public void uiEvent(UIEvent uiEvent) {
        Vector<DownloadLink> newLinks;

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
            this.addPackage(fp);
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

            // Interaction.handleInteraction(Interaction.INTERACTION_NEED_RECONNECT,
            // this);
            if (requestReconnect()) {
                uiInterface.showMessageDialog("Reconnect erfolgreich");

            } else {

                uiInterface.showMessageDialog("Reconnect fehlgeschlagen");

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
        int i=packages.indexOf(fp);
        if (i ==index) {
           return false;
        }else if(i>index){
            index--;
            this.removePackage(fp);
        }else{
            this.removePackage(fp);
        }
        this.addPackageAt(fp, index);
        return true;
    }

    public boolean removePackage(FilePackage fp2) {
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
        if (index < 0) index = 0;
        if (index > packages.size() - 1) index = packages.size() - 1;
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
     * @param file
     *            Die Datei, in die die Links gespeichert werden sollen
     */
    public void saveDownloadLinks() {
        // JDUtilities.saveObject(null, downloadLinks.toArray(new
        // DownloadLink[]{}), file, "links", "dat", true);
        File file = JDUtilities.getResourceFile("links.dat");
        JDUtilities.saveObject(null, packages, file, "links", "dat", Configuration.saveAsXML);
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

            } else {
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
            } else {
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
                } else {
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
        } catch (Exception e) {
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

        RequestInfo ri = Plugin.getRequestWithoutHtmlCode(service, null, null, false, 2000, 2000);

        if (!ri.isOK() || ri.getLocation() == null) {

        return null; }

        logger.finer("Call Redirect: " + ri.getLocation());

        ri = Plugin.postRequest(new URL(ri.getLocation()), null, null, null, "jd=1&srcType=plain&data=" + key, true, 2000, 2000);

        logger.info("Call re: " + ri.getHtmlCode());
        if (!ri.isOK() || !ri.containsHTML("<rc>")) {

            return null;
        } else {
            String dlcKey = ri.getHtmlCode();

            dlcKey = Plugin.getBetween(dlcKey, "<rc>", "</rc>");
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
                // if (obj != null && obj instanceof Vector
                // &&((Vector)obj).size()>0
                // &&((Vector)obj).get(0) instanceof DownloadLink) {
                // Vector<DownloadLink> links = (Vector<DownloadLink>) obj;
                // Iterator<DownloadLink> iterator = links.iterator();
                // DownloadLink localLink;
                // PluginForHost pluginForHost = null;
                // PluginForContainer pluginForContainer = null;
                // while (iterator.hasNext()) {
                // localLink = iterator.next();
                // if (localLink.getStatus() == DownloadLink.STATUS_DONE &&
                // Configuration.FINISHED_DOWNLOADS_REMOVE_AT_START.equals(JDUtilities.getConfiguration().getProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION)))
                // {
                // iterator.remove();
                // continue;
                // }
                // // Anhand des Hostnamens aus dem DownloadLink wird ein
                // // passendes Plugin gesucht
                // try {
                // pluginForHost =
                // JDUtilities.getPluginForHost(localLink.getHost()).getClass().newInstance();
                // }
                // catch (InstantiationException e) {
                // e.printStackTrace();
                // }
                // catch (IllegalAccessException e) {
                // e.printStackTrace();
                // }
                // catch (NullPointerException e) {
                // e.printStackTrace();
                // }
                // // Gibt es einen Names für ein Containerformat, wird ein
                // // passendes Plugin gesucht
                // try {
                // if (localLink.getContainer() != null) {
                // pluginForContainer =
                // JDUtilities.getPluginForContainer(localLink.getContainer(),
                // localLink.getContainerFile());
                // if (pluginForContainer != null) {
                // // pluginForContainer =
                // //
                // pluginForContainer.getPlugin(localLink.getContainerFile());
                // // pluginForContainer.
                // //
                // pluginForContainer.initContainer(localLink.getContainerFile());
                // // pluginForContainer.getContainedDownloadlinks();
                // }
                // else
                // localLink.setEnabled(false);
                // }
                // }
                //
                // catch (NullPointerException e) {
                // e.printStackTrace();
                // }
                // if (pluginForHost != null) {
                // localLink.setLoadedPlugin(pluginForHost);
                // pluginForHost.addPluginListener(this);
                // }
                // if (pluginForContainer != null) {
                // localLink.setLoadedPluginForContainer(pluginForContainer);
                // pluginForContainer.addPluginListener(this);
                // }
                // if (pluginForHost == null) {
                // logger.severe("couldn't find plugin(" + localLink.getHost() +
                // ") for this
                // DownloadLink." + localLink.getName());
                // }
                // }
                // return links;
                // }else
                if (obj != null && obj instanceof Vector && ((Vector) obj).size() > 0 && ((Vector) obj).get(0) instanceof FilePackage) {
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
                                iterator.remove();
                                if (fp.getDownloadLinks().size() == 0) {
                                    iterator.remove();

                                }
                                break;
                            }
                            // Anhand des Hostnamens aus dem DownloadLink wird
                            // ein
                            // passendes Plugin gesucht
                            try {
                                pluginForHost = JDUtilities.getPluginForHost(localLink.getHost()).getClass().newInstance();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            // Gibt es einen Names für ein Containerformat, wird
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
                        if (fp.getDownloadLinks().size() == 0) {
                            iterator.remove();

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

                    pContainer.initContainer(file.getAbsolutePath());
                    Vector<DownloadLink> links = pContainer.getContainedDownloadlinks();
                    if (links == null || links.size() == 0) {
                        logger.severe("Container Decryption failed (1)");
                    } else {
                        this.addAllLinks(links);
                    }

                } catch (Exception e) {
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
     * sich wirklich in der downloadpahse befinden
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

    public boolean hasDownloadLinkURL(String url) {
        synchronized (packages) {
            Iterator<FilePackage> iterator = packages.iterator();
            FilePackage fp = null;
            DownloadLink nextDownloadLink;
            while (iterator.hasNext()) {
                fp = iterator.next();
                Iterator<DownloadLink> it2 = fp.getDownloadLinks().iterator();
                while (it2.hasNext()) {
                    nextDownloadLink = it2.next();
                    if (nextDownloadLink.getDownloadURL().equalsIgnoreCase(url)) return true;
                }
            }
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
    public void addControlListener(ControlListener listener) {
        if (controlListener == null) controlListener = new Vector<ControlListener>();
        if (controlListener.indexOf(listener) == -1) {
            controlListener.add(listener);
        }
    }

    /**
     * Emtfernt einen Listener
     * 
     * @param listener
     *            Der zu entfernende Listener
     */
    public void removeControlListener(ControlListener listener) {
        controlListener.remove(listener);
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
        this.controlEvent(controlEvent);
        if (controlListener == null) controlListener = new Vector<ControlListener>();
        Iterator<ControlListener> iterator = controlListener.iterator();
        while (iterator.hasNext()) {

            ((ControlListener) iterator.next()).controlEvent(controlEvent);
        }
    }

    public void fireControlEvent(int controlID, Object param) {
        ControlEvent c = new ControlEvent(this, controlID, param);
        Iterator<ControlListener> iterator = controlListener.iterator();
        while (iterator.hasNext()) {
            ((ControlListener) iterator.next()).controlEvent(c);
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
                      
                        al.add(nextDownloadLink);
                    }

                }
            }
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOADLINKS_CHANGED, al));

        }

    }

    public void requestDownloadLinkUpdate(DownloadLink link) {
       
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOADLINKS_CHANGED, link));

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
        int wait = 0;
        while (isReconnecting) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            wait += 500;

        }
        if (wait > 0 && lastReconnectSuccess) return true;

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
        isReconnecting = true;
        boolean ret = false;
        if (type.equals(JDLocale.L("modules.reconnect.types.extern", "Extern"))) {
            ret = new ExternReconnect().interact(null);
        } else if (type.equals(JDLocale.L("modules.reconnect.types.batch", "Batch"))) {
            ret = new BatchReconnect().interact(null);
        } else {
            ret = new HTTPLiveHeader().interact(null);
        }
        isReconnecting = false;
        lastReconnectSuccess = ret;
        logger.info("Reconnect success: " + ret);
        if (ret) {
            synchronized (packages) {
                Iterator<FilePackage> iterator = packages.iterator();
                FilePackage fp = null;
                DownloadLink nextDownloadLink;
                while (iterator.hasNext()) {
                    fp = iterator.next();
                    Iterator<DownloadLink> it2 = fp.getDownloadLinks().iterator();
                    while (it2.hasNext()) {
                        nextDownloadLink = it2.next();
                        if (nextDownloadLink.getRemainingWaittime() > 0) {
                            nextDownloadLink.setEndOfWaittime(0);
                            logger.finer("REset GLOBALS: " + ((PluginForHost) nextDownloadLink.getPlugin()));
                            ((PluginForHost) nextDownloadLink.getPlugin()).resetPluginGlobals();
                            nextDownloadLink.setStatus(DownloadLink.STATUS_TODO);

                        }
                    }
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

    /**
     * Der Zurückgegeben Vector darf nur gelesen werden!!
     * 
     * @return
     */
    public Vector<FilePackage> getPackages() {
        // TODO Auto-generated method stub
        return packages;
    }

    public boolean moveLinks(Vector<DownloadLink> links, Object after) {
        this.removeDownloadLinks(links);
        if (after == null) {
            packages.lastElement().addAll(links);
        }
        // if (after instanceof FilePackage) {
        // ((FilePackage) after).addAllAt(links, 0);
        //
        // }
        else {
            DownloadLink pos = (DownloadLink) after;
            if (links.contains(pos)) return false;
            FilePackage dest = pos.getFilePackage();
            if (dest == null) {
                logger.severe(after + " +does not belong to a filepackage. Set default");
                pos.setFilePackage(fp);

                return false;
            }

            dest.addAllAt(links, dest.indexOf(pos));

        }
        // logger.info("II");
        this.fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));

        return true;
    }

    public boolean movePackages(Vector<FilePackage> fps, FilePackage after) {
        if (after != null && fps.contains(after)) return false;
        synchronized (packages) {
            if (after == null) {
                packages.removeAll(fps);
                packages.addAll(fps);

            } else {

                packages.removeAll(fps);

                packages.addAll(packages.indexOf(after), fps);
            }
        }
        this.fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));

        return true;
    }

    public FilePackage getDefaultFilePackage() {
        return fp;
    }

}
