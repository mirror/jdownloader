//jDownloader - Downloadmanager
//Copyright (C) 2008JD-Team support@jdownloader.org
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jd.CPluginWrapper;
import jd.JDInit;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.interaction.Interaction;
import jd.controlling.reconnect.Reconnecter;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.event.UIEvent;
import jd.event.UIListener;
import jd.gui.UIInterface;
import jd.gui.skins.simple.LinkGrabber;
import jd.gui.skins.simple.SimpleGUI;
import jd.http.Browser;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;
import jd.update.PackageData;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Im Controller wird das ganze App gesteuert. Evebnts werden deligiert.
 * 
 * @author JD-Team/astaldo
 * 
 */
public class JDController implements ControlListener, UIListener {
    private class EventSender extends Thread {

        protected static final long MAX_EVENT_TIME = 10000;
        private ControlListener currentListener;
        private ControlEvent event;
        private long eventStart = 0;
        public boolean waitFlag = true;
        private Thread watchDog;

        public EventSender() {
            super("EventSender");

            watchDog = new Thread("EventSenderWatchDog") {
                public void run() {
                    while (true) {
                        if (eventStart > 0 && System.currentTimeMillis() - eventStart > MAX_EVENT_TIME) {
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
                        if (event == null) {
                            continue;
                        }
                        eventStart = System.currentTimeMillis();
                        currentListener = JDController.this;
                        controlEvent(event);
                        eventStart = 0;
                        if (controlListener == null) {
                            controlListener = new ArrayList<ControlListener>();
                        }
                        synchronized (controlListener) {
                            if (controlListener.size() > 0) {
                                for (int i = controlListener.size() - 1; i >= 0; i--) {
                                    eventStart = System.currentTimeMillis();
                                    currentListener = controlListener.get(i);
                                    currentListener.controlEvent(event);
                                    eventStart = 0;
                                }
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

    /**
     * Es läuft kein Download
     */
    public static final int DOWNLOAD_NOT_RUNNING = 3;
    /**
     * Der Download läuft
     */
    public static final int DOWNLOAD_RUNNING = 2;
    /**
     * Der Download wird gerade abgebrochen.
     */
    public static final int DOWNLOAD_TERMINATION_IN_PROGRESS = 0;
    /**
     * Der Controller wurd fertig initialisiert
     */
    public static final int INIT_STATUS_COMPLETE = 0;

    private static final String PROPERTY_SELECTED = "selected";

    /**
     * Die Konfiguration
     */
    protected Configuration config = JDUtilities.getConfiguration();

    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #fireControlEvent(ControlEvent)} ein Event losgeschickt wird.
     */
    private transient ArrayList<ControlListener> controlListener = null;

    /**
     * Mit diesem Thread wird eingegebener Text auf Links untersucht
     */
    private DistributeData distributeData = null;

    /**
     * Hier kann de Status des Downloads gespeichert werden.
     */
    private int downloadStatus;

    private ArrayList<ControlEvent> eventQueue;

    private EventSender eventSender;

    private BufferedWriter fileLogger = null;

    private Vector<DownloadLink> finishedLinks = new Vector<DownloadLink>();

    private int initStatus = -1;

    private DownloadLink lastDownloadFinished;

    /**
     * Der Logger
     */
    private Logger logger = JDUtilities.getLogger();

    private Vector<FilePackage> packages;
    /**
     * Schnittstelle zur Benutzeroberfläche
     */
    private UIInterface uiInterface;

    private Vector<Vector<String>> waitingUpdates = new Vector<Vector<String>>();

    /**
     * Der Download Watchdog verwaltet die Downloads
     */
    private DownloadWatchDog watchdog;

    private Integer StartStopSync = new Integer(0);

    public JDController() {

        packages = new Vector<FilePackage>();

        downloadStatus = DOWNLOAD_NOT_RUNNING;
        eventSender = getEventSender();

        JDUtilities.setController(this);
        // Kann weg, oder? Ist bei allen Interactions eine leere Methode ..
        initInteractions();

    }

    public void addAllLinks(Vector<DownloadLink> links) {
        Iterator<DownloadLink> it = links.iterator();
        while (it.hasNext()) {
            addLink(it.next());
        }

    }

    /**
     * Fügt einen Listener hinzu
     * 
     * @param listener
     *            Ein neuer Listener
     */
    public synchronized void addControlListener(ControlListener listener) {
        if (controlListener == null) {
            controlListener = new ArrayList<ControlListener>();
        }
        synchronized (controlListener) {
            if (controlListener.indexOf(listener) == -1) {
                controlListener.add(listener);
            }
        }
    }

    public void addLink(DownloadLink link) {
        int index;

        synchronized (packages) {
            if ((index = packages.indexOf(link.getFilePackage())) >= 0) {

                packages.get(index).add(link);
            } else {
                packages.add(link.getFilePackage());
                if (!link.getFilePackage().contains(link)) {
                    link.getFilePackage().add(link);
                }

            }
        }
        ArrayList<DownloadLink> dl = new ArrayList<DownloadLink>();
        dl.add(link);
        fireControlEvent(ControlEvent.CONTROL_DOWNLOADLIST_ADDED_LINKS, dl);

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
            FilePackage fp = new FilePackage();
            fp.setName(packages.get(i));
            String downloadDir = JDUtilities.getConfiguration().getDefaultDownloadDirectory();

            if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false)) {
                File file = new File(new File(downloadDir), packages.get(i));
                if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CREATE_SUBFOLDER_BEFORE_DOWNLOAD, false)) {
                    if (!file.exists()) file.mkdirs();
                }
                fp.setDownloadDirectory(file.getAbsolutePath());
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

    public void addPackage(FilePackage fp) {
        ArrayList<DownloadLink> dl = new ArrayList<DownloadLink>();
        synchronized (packages) {
            packages.add(fp);

            dl.addAll(fp.getDownloadLinks());
        }

        fireControlEvent(ControlEvent.CONTROL_DOWNLOADLIST_ADDED_LINKS, dl);

    }

    public void addPackageAt(FilePackage fp, int index) {
        ArrayList<DownloadLink> dl = new ArrayList<DownloadLink>();
        if (packages.size() == 0) {
            addPackage(fp);
            return;
        }
        if (index > packages.size() - 1) {
            index = packages.size() - 1;
        }
        if (index < 0) {
            index = 0;
        }
        synchronized (packages) {
            packages.add(index, fp);
            dl.addAll(fp.getDownloadLinks());
        }

        fireControlEvent(ControlEvent.CONTROL_DOWNLOADLIST_ADDED_LINKS, dl);

    }

    /**
     * Fügt einen Downloadlink der Finishedliste hinzu.
     * 
     * @param lastDownloadFinished
     */
    private void addToFinished(DownloadLink lastDownloadFinished) {
        finishedLinks.add(lastDownloadFinished);

    }

    private String callService(String service, String key) throws Exception {
        logger.finer("Call " + service);

        Browser br = new Browser();
        br.postPage(service, "jd=1&srcType=plain&data=" + key);

        logger.info("Call re: " + br.toString());
        if (!br.getHttpConnection().isOK() || !br.containsHTML("<rc>")) {
            return null;
        } else {
            String dlcKey = br.getRegex("<rc>(.*?)</rc>").getMatch(0);
            if (dlcKey.trim().length() < 80) return null;
            return dlcKey;
        }
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

                // if (link.isAvailable() || ((PluginForHost)
                // link.getPlugin()).isListOffline()) {
                finalLinks.add(link);
                // }

            }

        }

        return finalLinks;

    }

    private int comparePackages(String a, String b) {

        int c = 0;
        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            if (a.charAt(i) == b.charAt(i)) {
                c++;
            }
        }

        if (Math.min(a.length(), b.length()) == 0) { return 0; }
        // logger.info("comp: " + a + " <<->> " + b + "(" + (c * 100) /
        // (b.length()) + ")");
        return c * 100 / b.length();
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
        case ControlEvent.CONTROL_ON_FILEOUTPUT:
            File[] list = (File[]) event.getParameter();

            for (File file : list) {

                if (isContainerFile(file)) {
                    if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_RELOADCONTAINER, true)) {
                        loadContainerFile(file);
                    }
                }

            }

            break;

        case ControlEvent.CONTROL_DOWNLOADLIST_ADDED_LINKS:
            ArrayList<DownloadLink> linksAdded = (ArrayList<DownloadLink>) event.getParameter();

            for (DownloadLink link : linksAdded) {
                link.getLinkStatus().setStatusText(JDLocale.L("sys.linklist.addnew.prepare", "Preparing Downloadlink"));
                link.requestGuiUpdate();
                try {
                    link.getPlugin().prepareLink(link);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                link.getLinkStatus().setStatusText(null);
            }
            break;
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

            if (fileLogger != null) {
                try {
                    fileLogger.flush();
                    fileLogger.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            break;
        case ControlEvent.CONTROL_PLUGIN_INACTIVE:
            // Nur Hostpluginevents auswerten
            if (!(event.getSource() instanceof PluginForHost)) { return; }
            lastDownloadFinished = ((SingleDownloadController) event.getParameter()).getDownloadLink();
            addToFinished(lastDownloadFinished);

            // Prüfen ob das Paket fertig ist und entfernt werden soll
            if (lastDownloadFinished.getFilePackage().getRemainingLinks() == 0) {
                Interaction.handleInteraction(Interaction.INTERACTION_DOWNLOAD_PACKAGE_FINISHED, this);

                if (JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION) == 2) {
                    removePackage(lastDownloadFinished.getFilePackage());
                    this.fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));
                    saveDownloadLinks();
                    break;
                }
            }

            // Prüfen ob der Link entfernt werden soll
            if (lastDownloadFinished.getLinkStatus().hasStatus(LinkStatus.FINISHED) && JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION) == 0) {
                removeDownloadLink(lastDownloadFinished);
                this.fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));
            }

            saveDownloadLinks();

            break;
        case ControlEvent.CONTROL_DISTRIBUTE_FINISHED:
            if (uiInterface == null) return;

            if (event.getParameter() != null && event.getParameter() instanceof Vector && ((Vector) event.getParameter()).size() > 0) {
                Vector<DownloadLink> links = (Vector<DownloadLink>) event.getParameter();
                uiInterface.addLinksToGrabber(links);

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
                    toggleStartStop();
                }
            }

            break;
        case ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED:
            Interaction.handleInteraction(Interaction.INTERACTION_LINKLIST_STRUCTURE_CHANGED, null);
            saveDownloadLinks();
            break;
        }

    }

    public String encryptDLC(String xml) {
        String[] encrypt = JDUtilities.encrypt(xml, "dlc");
        if (encrypt == null) {
            logger.severe("Container Encryption failed.");
            return null;
        }

        String key = encrypt[1];
        xml = encrypt[0];

        String service = "http://service.jdownloader.org/dlcrypt/service.php";

        try {
            String dlcKey = callService(service, key);
            if (dlcKey == null) return null;
            return xml + dlcKey;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    /**
     * Beendet das Programm
     */
    public void exit() {
        prepareShutdown();
        System.exit(0);
    }

    public void prepareShutdown() {
        stopDownloads();
        saveDownloadLinks();
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SYSTEM_EXIT, this));
        Interaction.handleInteraction(Interaction.INTERACTION_EXIT, null);
        JDUtilities.getDatabaseConnector().shutdownDatabase();
    }

    /**
     * Startet das Programm neu
     */
    public void restart() {
        prepareShutdown();
        logger.info(JDUtilities.runCommand("java", new String[] { "-Xmx512m", "-jar", "JDownloader.jar", }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
        System.exit(0);
    }

    /**
     * Verteilt Ein Event an alle Listener
     * 
     * @param controlEvent
     *            ein abzuschickendes Event
     */
    public void fireControlEvent(ControlEvent controlEvent) {
        if (controlEvent == null) return;

        try {

            if (eventQueue == null) {
                eventQueue = new ArrayList<ControlEvent>();
            }
            eventQueue.add(controlEvent);

            synchronized (eventSender) {
                if (eventSender.waitFlag) {
                    eventSender.waitFlag = false;
                    eventSender.notify();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fireControlEventDirect(ControlEvent controlEvent) {
        synchronized (controlListener) {
            if (controlListener.size() > 0) {
                for (int i = controlListener.size() - 1; i >= 0; i--) {
                    ControlListener currentListener = controlListener.get(i);
                    currentListener.controlEvent(controlEvent);

                }
            }
        }

    }

    public void fireControlEvent(int controlID, Object param) {
        ControlEvent c = new ControlEvent(this, controlID, param);
        fireControlEvent(c);

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

    public DownloadLink getDownloadLinkAfter(DownloadLink lastElement) {
        synchronized (packages) {

            FilePackage fp = null;
            DownloadLink nextDownloadLink;
            DownloadLink lastDownloadLink = null;
            for (Iterator<FilePackage> packageIterator = packages.iterator(); packageIterator.hasNext();) {
                fp = packageIterator.next();

                for (Iterator<DownloadLink> linkIterator = fp.getDownloadLinks().iterator(); linkIterator.hasNext();) {

                    nextDownloadLink = linkIterator.next();
                    if (lastElement == lastDownloadLink) { return nextDownloadLink; }
                    lastDownloadLink = nextDownloadLink;

                }
            }
            return null;

        }
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
                    if (downloadLink == nextDownloadLink) { return lastDownloadLink; }
                    lastDownloadLink = nextDownloadLink;

                }
            }
            return null;

        }

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
        // logger.warning("DEPRECATED FUNCTION");
        return ret;
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
        return downloadStatus;

    }

    private EventSender getEventSender() {
        EventSender th = new EventSender();
        th.start();

        return th;
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
                    if (nextDownloadLink.getPlugin() != null && !nextDownloadLink.getPlugin().canResume(nextDownloadLink)) {
                        if (nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS) || nextDownloadLink.getLinkStatus().isPluginActive() && nextDownloadLink.getPlugin().getRemainingHosterWaittime() <= 0 && nextDownloadLink.isEnabled()) {

                            ret++;
                        }
                    }
                }
            }
        }

        if (watchdog != null && !watchdog.isAborted() && watchdog.isAlive()) { return Math.min(watchdog.getActiveDownloadControllers().size(), ret); }
        return ret;
    }

    public int getInitStatus() {
        return initStatus;
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
     * @return Die zuletzte fertiggestellte datei
     */
    public String getLastFinishedFile() {
        if (lastDownloadFinished == null) { return ""; }
        return lastDownloadFinished.getFileOutput();
    }

    public Vector<DownloadLink> getPackageFiles(FilePackage filePackage, Vector<DownloadLink> links) {
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        // ret.add(downloadLink);

        Iterator<DownloadLink> iterator = links.iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();

            if (filePackage == nextDownloadLink.getFilePackage()) {
                ret.add(nextDownloadLink);
            }
        }
        return ret;
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
                    if (nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                        ret++;
                    }
                }
            }
        }
        return ret;
    }

    public int getRunningDownloadNumByHost(PluginForHost pluginForHost) {
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
                    if (nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                        if (nextDownloadLink.getPlugin().getClass() == pluginForHost.getClass()) {
                            ret++;
                        }
                    }
                }
            }
        }
        return ret;
    }

    /**
     * @return gibt das globale speedmeter zurück
     */
    public int getSpeedMeter() {

        if (getWatchdog() == null || !getWatchdog().isAlive()) { return 0; }
        return getWatchdog().getTotalSpeed();
    }

    /**
     * Gibt das verwendete UIinterface zurpck
     * 
     * @return aktuelles uiInterface
     */
    public UIInterface getUiInterface() {
        return uiInterface;
    }

    public Vector<Vector<String>> getWaitingUpdates() {
        return waitingUpdates;

    }

    public DownloadWatchDog getWatchdog() {
        return watchdog;
    }

    public boolean hasDownloadLinkURL(String url) {
        if (url == null) return false;
        url = url.trim();
        try {
            Iterator<FilePackage> iterator = packages.iterator();
            FilePackage fp = null;
            DownloadLink nextDownloadLink;
            while (iterator.hasNext()) {
                fp = iterator.next();
                Iterator<DownloadLink> it2 = fp.getDownloadLinks().iterator();
                while (it2.hasNext()) {
                    nextDownloadLink = it2.next();
                    if (nextDownloadLink.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) continue;
                    if (nextDownloadLink.getDownloadURL() != null && nextDownloadLink.getDownloadURL().equalsIgnoreCase(url)) { return true; }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;

    }

    /**
     * Lädt zum Start das erste Mal alle Links aus einer Datei
     * 
     * @return true/False je nach Erfolg
     */
    public boolean initDownloadLinks() {
        try {
            packages = loadDownloadLinks();
        } catch (Exception e) {
            JDUtilities.getLogger().severe("" + e.getStackTrace());
            packages = null;
        }
        if (packages == null) {
            packages = new Vector<FilePackage>();
            this.fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));
            File file = JDUtilities.getResourceFile("backup/links.linkbackup");
            if (file.exists()) {
                JDUtilities.getLogger().severe("Strange: No Linklist,Try to restore from backup file");
                this.loadContainerFile(file);
            }
            return false;
        } else if (packages.size() == 0) {
            this.fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));
            File file = JDUtilities.getResourceFile("backup/links.linkbackup");
            if (file.exists() && file.lastModified() >= System.currentTimeMillis() - 10 * 60 * 1000l) {
                JDUtilities.getLogger().severe("Strange: Empty Linklist,Try to restore from backup file");
                this.loadContainerFile(file);
            }
            return false;
        }

        for (FilePackage filePackage : packages) {
            for (DownloadLink downloadLink : filePackage.getDownloadLinks()) {
                downloadLink.setProperty(PROPERTY_SELECTED, false);
            }
        }
        this.fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));

        return true;

    }

    /**
     * Initialisiert alle Interactions. <br>
     * Kann weg, oder? Ist bei allen Interactions eine leere Methode ..
     */
    @SuppressWarnings("unchecked")
    private void initInteractions() {
        Vector<Interaction> interactions = (Vector<Interaction>) JDUtilities.getSubConfig(Configuration.CONFIG_INTERACTIONS).getProperty(Configuration.PARAM_INTERACTIONS, new Vector<Interaction>());

        for (int i = 0; i < interactions.size(); i++) {
            interactions.get(i).initInteraction();
        }
    }

    public boolean isContainerFile(File file) {

        ArrayList<CPluginWrapper> pluginsForContainer = CPluginWrapper.getCWrapper();
        // Vector<DownloadLink> downloadLinks = new Vector<DownloadLink>();
        CPluginWrapper pContainer;

        for (int i = 0; i < pluginsForContainer.size(); i++) {
            pContainer = pluginsForContainer.get(i);

            if (pContainer.canHandle(file.getName())) { return true; }

        }

        return false;
    }

    public synchronized DownloadLink getLinkThatBlocks(DownloadLink link) {
        synchronized (packages) {
            Iterator<FilePackage> iterator = packages.iterator();
            FilePackage fp = null;
            DownloadLink nextDownloadLink;
            while (iterator.hasNext()) {
                fp = iterator.next();
                Iterator<DownloadLink> it2 = fp.getDownloadLinks().iterator();
                while (it2.hasNext()) {
                    nextDownloadLink = it2.next();
                    if (nextDownloadLink != link) {
                        if ((nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) && nextDownloadLink.getFileOutput().equalsIgnoreCase(link.getFileOutput())) {
                            if (new File(nextDownloadLink.getFileOutput()).exists()) {
                                /*
                                 * fertige datei sollte auch auf der platte sein
                                 * und nicht nur als fertig in der liste
                                 */
                                logger.info("Link owner: " + nextDownloadLink.getHost() + nextDownloadLink);
                                return nextDownloadLink;
                            }
                        }
                        if ((nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS) || nextDownloadLink.getLinkStatus().isPluginActive()) && nextDownloadLink.getFileOutput().equalsIgnoreCase(link.getFileOutput())) {
                            if (nextDownloadLink.getFinalFileName() != null) {
                                /* Dateiname muss fertig geholt sein */
                                logger.info("Link owner: " + nextDownloadLink.getHost() + nextDownloadLink);
                                return nextDownloadLink;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public void loadContainerFile(final File file) {
        loadContainerFile(file, false, false);
    }

    /**
     * Hiermit wird eine Containerdatei geöffnet. Dazu wird zuerst ein passendes
     * Plugin gesucht und danach alle DownloadLinks interpretiert
     * 
     * @param file
     *            Die Containerdatei
     */
    public void loadContainerFile(final File file, final boolean hideGrabber, final boolean startDownload) {
        new Thread() {
            @SuppressWarnings("unchecked")
            public void run() {

                ArrayList<CPluginWrapper> pluginsForContainer = CPluginWrapper.getCWrapper();
                Vector<DownloadLink> downloadLinks = new Vector<DownloadLink>();
                CPluginWrapper wrapper;
                ProgressController progress = new ProgressController("Containerloader", pluginsForContainer.size());
                logger.info("load Container: " + file);

                for (int i = 0; i < pluginsForContainer.size(); i++) {

                    wrapper = pluginsForContainer.get(i);
                    // logger.info(i + ". " + "Containerplugin: " +
                    // pContainer.getPluginName());
                    progress.setStatusText("Containerplugin: " + wrapper.getHost());
                    if (wrapper.canHandle(file.getName())) {
                        // es muss jeweils eine neue plugininstanz erzeugt
                        // werden
                        PluginsC pContainer = (PluginsC) wrapper.getNewPluginInstance();
                        try {

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
                        JDUtilities.getGUI().showHTMLDialog(JDLocale.L("container.message.title", "DownloadLinkContainer loaded"), String.format(html, JDIO.getFileExtension(file).toLowerCase(), JDLocale.L("container.message.title", "DownloadLinkContainer loaded"), JDLocale.L("container.message.uploaded", "Brought to you by"), uploader, JDLocale.L("container.message.created", "Created with"), app, JDLocale.L("container.message.comment", "Comment"), comment, JDLocale.L("container.message.password", "Password"), password));
                        // schickt die Links zuerst mal zum Linkgrabber
                    }
                    if (hideGrabber)
                        addLinksWithoutGrabber((Vector<DownloadLink>) downloadLinks);
                    else
                        uiInterface.addLinksToGrabber((Vector<DownloadLink>) downloadLinks);
                    if (startDownload && getDownloadStatus() == JDController.DOWNLOAD_NOT_RUNNING) {

                    }
                }
                progress.finalize();
            }
        }.start();
    }

    public Vector<DownloadLink> getContainerLinks(final File file) {

        ArrayList<CPluginWrapper> pluginsForContainer = CPluginWrapper.getCWrapper();
        Vector<DownloadLink> downloadLinks = new Vector<DownloadLink>();
        PluginsC pContainer;
        CPluginWrapper wrapper;
        ProgressController progress = new ProgressController("Containerloader", pluginsForContainer.size());
        logger.info("load Container: " + file);

        for (int i = 0; i < pluginsForContainer.size(); i++) {

            wrapper = pluginsForContainer.get(i);
            // logger.info(i + ". " + "Containerplugin: " +
            // pContainer.getPluginName());
            progress.setStatusText("Containerplugin: " + wrapper.getHost());
            if (wrapper.canHandle(file.getName())) {
                // es muss jeweils eine neue plugininstanz erzeugt
                // werden
                pContainer = (PluginsC) wrapper.getNewPluginInstance();
                try {

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
        progress.finalize();
        return downloadLinks;

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
            Iterator<DownloadLink> it;
            FilePackage fp;
            while (iterator.hasNext()) {
                fp = iterator.next();
                it = fp.getDownloadLinks().iterator();
                while (it.hasNext()) {
                    localLink = it.next();
                    if (localLink.getLinkType() == DownloadLink.LINKTYPE_JDU && (localLink.getProperty("JDU") == null || !(localLink.getProperty("JDU") instanceof PackageData))) {
                        iterator.remove();
                        continue;
                    }

                    if (!localLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                        localLink.getLinkStatus().reset();
                    }
                    if (localLink.getLinkStatus().hasStatus(LinkStatus.FINISHED) && JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION) == 1) {
                        it.remove();
                        if (fp.getDownloadLinks().size() == 0) {
                            iterator.remove();

                        }
                    } else {
                        // Anhand des Hostnamens aus dem DownloadLink
                        // wird ein passendes Plugin gesucht
                        try {
                            pluginForHost = JDUtilities.getNewPluginForHostInstanz(localLink.getHost());
                        } catch (Exception e) {
                            e.printStackTrace();
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

        throw new Exception("Linklist incompatible");

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
        if (links.contains(before) || links.contains(after)) { return false; }
        if (before != null && after != null && before.getFilePackage() != after.getFilePackage()) { return false; }
        if (before == null & after == null) { return false; }
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

                        if (fp.size() == 0) {
                            it.remove();
                        }
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

    public boolean movePackage(FilePackage fp, int index) {
        if (index < 0) {
            index = 0;
        }
        if (index > packages.size() - 1) {
            index = packages.size() - 1;
        }
        int i = packages.indexOf(fp);
        if (i == index) {
            return false;
        } else if (i > index) {
            index--;
            removePackage(fp);
        } else {
            removePackage(fp);
        }
        addPackageAt(fp, index);
        return true;
    }

    public boolean movePackages(Vector<FilePackage> fps, FilePackage before, FilePackage after) {
        if (after != null && fps.contains(after)) { return false; }
        if (before != null && fps.contains(before)) { return false; }
        if (before == null && after == null) { return false; }
        synchronized (packages) {
            packages.removeAll(fps);
            int pos = after == null ? packages.indexOf(before) + 1 : packages.indexOf(after);

            packages.addAll(pos, fps);

        }
        this.fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));

        return true;
    }

    public void pauseDownloads(boolean value) {
        if (watchdog == null) return;
        watchdog.pause(value);

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

    public void removeDownloadLink(DownloadLink link) {
        synchronized (packages) {
            link.setAborted(true);
            Iterator<FilePackage> it = packages.iterator();
            FilePackage fp;
            while (it.hasNext()) {
                fp = it.next();
                if (fp.remove(link)) {
                    if (fp.size() == 0) {
                        packages.remove(fp);
                    }
                    return;
                }

            }

        }
        // logger.severe("Link " + link + " does not belong to any Package");

    }

    public void removeDownloadLinks(Vector<DownloadLink> links) {
        if (links == null || links.size() == 0) { return; }
        Iterator<DownloadLink> iterator = links.iterator();
        while (iterator.hasNext()) {

            removeDownloadLink(iterator.next());
        }

    }

    private String removeExtension(String a) {
        // logger.finer("file " + a);
        if (a == null) { return a; }
        a = a.replaceAll("\\.part([0-9]+)", "");
        a = a.replaceAll("\\.html", "");
        a = a.replaceAll("\\.htm", "");
        int i = a.lastIndexOf(".");
        // logger.info("FOund . " + i);
        String ret;
        if (i <= 1 || a.length() - i > 5) {
            ret = a.toLowerCase().trim();
        } else {
            // logger.info("Remove ext");
            ret = a.substring(0, i).toLowerCase().trim();
        }

        if (a.equals(ret)) { return ret; }
        return ret;

    }

    public boolean removePackage(FilePackage fp2) {
        for (DownloadLink downloadLink : fp2.getDownloadLinks()) {
            downloadLink.setAborted(true);
        }

        synchronized (packages) {
            return packages.remove(fp2);
        }

    }

    public void removeCompletedPackages() {
        Vector<FilePackage> packagestodelete = new Vector<FilePackage>();
        synchronized (packages) {
            for (FilePackage fp : packages) {
                boolean remove = true;
                for (DownloadLink downloadLink : fp.getDownloadLinks()) {
                    if (!downloadLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                        remove = false;
                        break;
                    }
                }
                if (remove == true) {
                    packagestodelete.add(fp);
                }
            }
            if (packagestodelete.size() > 0) {
                for (int i = packagestodelete.size() - 1; i >= 0; i--) {
                    removePackage(packagestodelete.get(i));
                }
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));
            }
        }
    }

    public void removeCompletedDownloadLinks() {
        Vector<DownloadLink> downloadstodelete = new Vector<DownloadLink>();
        synchronized (packages) {
            for (FilePackage fp : packages) {
                for (DownloadLink downloadLink : fp.getDownloadLinks()) {
                    if (downloadLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                        downloadstodelete.add(downloadLink);
                    }
                }
            }

            if (downloadstodelete.size() > 0) {
                this.removeDownloadLinks(downloadstodelete);
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));
            }
        }
    }

    public void requestDownloadLinkUpdate(DownloadLink link) {

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, link));

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
                    if (!nextDownloadLink.getLinkStatus().isPluginActive()) {
                        nextDownloadLink.getLinkStatus().setStatus(LinkStatus.TODO);
                        nextDownloadLink.getLinkStatus().setStatusText("");
                        nextDownloadLink.getLinkStatus().reset();
                        // nextDownloadLink.setEndOfWaittime(0);
                        ((PluginForHost) nextDownloadLink.getPlugin()).resetPluginGlobals();
                        al.add(nextDownloadLink);
                    }

                }
            }
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, al));

        }

    }

    public void saveDLC(File file) {

        String xml = JDUtilities.createContainerString(getDownloadLinks(), "dlc");
        // String[] encrypt = JDUtilities.encrypt(xml, "DLCrypt");
        String cipher = encryptDLC(xml);
        if (cipher != null) {

            JDIO.writeLocalFile(file, cipher);
            SubConfiguration cfg = JDUtilities.getSubConfig("DLCrypt");
            if (cfg.getBooleanProperty("SHOW_INFO_AFTER_CREATE", false))

            {

                if (getUiInterface().showConfirmDialog(JDLocale.L("sys.dlc.success", "DLC encryption successfull. Run Testdecrypt now?"))) {
                    loadContainerFile(file);
                    return;
                }
            }
            return;
        }

        logger.severe("Container creation failed");
        getUiInterface().showMessageDialog("Container encryption failed");
    }

    public void saveDLC(File file, Vector<DownloadLink> links) {

        String xml = JDUtilities.createContainerString(links, "dlc");
        // String[] encrypt = JDUtilities.encrypt(xml, "DLCrypt");
        String cipher = encryptDLC(xml);
        if (cipher != null) {
            SubConfiguration cfg = JDUtilities.getSubConfig("DLCrypt");
            JDIO.writeLocalFile(file, cipher);
            if (cfg.getBooleanProperty("SHOW_INFO_AFTER_CREATE", false))
            // Nur Falls Die Meldung nicht deaktiviert wurde
            {

                if (getUiInterface().showConfirmDialog(JDLocale.L("sys.dlc.success", "DLC encryption successfull. Run Testdecrypt now?"))) {
                    loadContainerFile(file);
                    return;
                }
            }
            return;
        }

        logger.severe("Container creation failed");
        getUiInterface().showMessageDialog("Container encryption failed");
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
        // File file = JDUtilities.getResourceFile("links.dat");
        // JDUtilities.saveObject(null, packages, file, "links", "dat",
        // Configuration.saveAsXML);
        JDUtilities.getDatabaseConnector().saveLinks(packages);
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

    public void setInitStatus(int initStatus) {
        this.initStatus = initStatus;
    }

    public void setLogFileWriter(BufferedWriter bufferedWriter) {
        fileLogger = bufferedWriter;
    }

    /**
     * Setzt das UIINterface
     * 
     * @param uiInterface
     */
    public void setUiInterface(UIInterface uiInterface) {
        if (this.uiInterface != null) {
            this.uiInterface.removeUIListener(this);
        }
        this.uiInterface = uiInterface;
        uiInterface.addUIListener(this);
    }

    public void setWaitingUpdates(Vector<Vector<String>> files) {
        waitingUpdates = files;
    }

    /**
     * Startet den Downloadvorgang. Dies eFUnkton sendet das startdownload event
     * und aktiviert die ersten downloads.
     */
    public boolean startDownloads() {
        synchronized (StartStopSync) {
            if (getDownloadStatus() == DOWNLOAD_NOT_RUNNING) {
                setDownloadStatus(DOWNLOAD_RUNNING);
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOAD_START, this));
                logger.info("StartDownloads");
                watchdog = new DownloadWatchDog(this);
                watchdog.start();
                return true;
            }
            return false;
        }
    }

    /**
     * Bricht den Download ab und blockiert bis er abgebrochen wurde.
     */
    public boolean stopDownloads() {
        synchronized (StartStopSync) {
            if (getDownloadStatus() == DOWNLOAD_RUNNING) {
                setDownloadStatus(DOWNLOAD_TERMINATION_IN_PROGRESS);
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOAD_TERMINATION_ACTIVE, this));

                watchdog.abort();
                setDownloadStatus(DOWNLOAD_NOT_RUNNING);

                synchronized (packages) {
                    for (FilePackage fp : packages) {
                        for (DownloadLink link : fp.getDownloadLinks()) {
                            if (link.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE)) {
                                link.getLinkStatus().removeStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                                link.setEnabled(true);
                            }
                        }
                    }
                }
                logger.info("termination broadcast");
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOAD_TERMINATION_INACTIVE, this));
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOAD_STOP, this));
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Startet den download wenn er angehalten ist und hält ihn an wenn er läuft
     */
    public void toggleStartStop() {

        if (!startDownloads()) {
            stopDownloads();
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
            if (JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getIntegerProperty(LinkGrabber.PROPERTY_POSITION, 1) == 0 && uiEvent.getSource() instanceof LinkGrabber) {
                addPackageAt(fp, 0);
            } else {
                addPackage(fp);
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
        case UIEvent.UI_RESTART:
            restart();
            break;
        case UIEvent.UI_UPDATED_LINKLIST:
            break;
        case UIEvent.UI_INTERACT_RECONNECT:
            if (getRunningDownloadNum() > 0) {
                logger.info("Es laufen noch Downloads. Breche zum reconnect Downloads ab!");
                stopDownloads();
            }

            if (Reconnecter.waitForNewIP(1)) {
                uiInterface.showMessageDialog(JDLocale.L("gui.reconnect.success", "Reconnect erfolgreich"));
            } else {
                uiInterface.showMessageDialog(JDLocale.L("gui.reconnect.failed", "Reconnect fehlgeschlagen"));
            }

            break;
        case UIEvent.UI_INTERACT_UPDATE:
            new JDInit().doWebupdate(true);
            break;
        }
    }

    /**
     * Gibt alle Downloadlinks die zu dem übergebenem Hosterplugin gehören
     * zurück.
     * 
     * @param pluginForHost
     */
    public ArrayList<DownloadLink> getDownloadLinks(PluginForHost pluginForHost) {
        ArrayList<DownloadLink> al = new ArrayList<DownloadLink>();
        synchronized (packages) {

            Iterator<FilePackage> iterator = packages.iterator();
            FilePackage fp = null;
            DownloadLink nextDownloadLink;
            while (iterator.hasNext()) {
                fp = iterator.next();
                Iterator<DownloadLink> it2 = fp.getDownloadLinks().iterator();
                while (it2.hasNext()) {
                    nextDownloadLink = it2.next();
                    if (nextDownloadLink.getPlugin().getClass() == pluginForHost.getClass()) al.add(nextDownloadLink);

                }
            }

        }
        return al;

    }

    public DownloadLink getDownloadLinkByFileOutput(File file) {
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
                    if (new File(nextDownloadLink.getFileOutput()).getAbsoluteFile().equals(file.getAbsoluteFile())) return nextDownloadLink;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    public ArrayList<DownloadLink> getDownloadLinksByNamePattern(String matcher) {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        try {
            Iterator<FilePackage> iterator = packages.iterator();
            FilePackage fp = null;
            DownloadLink nextDownloadLink;
            while (iterator.hasNext()) {
                fp = iterator.next();
                Iterator<DownloadLink> it2 = fp.getDownloadLinks().iterator();
                while (it2.hasNext()) {
                    nextDownloadLink = it2.next();
                    String name = new File(nextDownloadLink.getFileOutput()).getName();
                    if (new Regex(name, matcher, Pattern.CASE_INSENSITIVE).matches()) {
                        ret.add(nextDownloadLink);
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public ArrayList<DownloadLink> getDownloadLinksByPathPattern(String matcher) {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        try {
            Iterator<FilePackage> iterator = packages.iterator();
            FilePackage fp = null;
            DownloadLink nextDownloadLink;
            while (iterator.hasNext()) {
                fp = iterator.next();
                Iterator<DownloadLink> it2 = fp.getDownloadLinks().iterator();
                while (it2.hasNext()) {
                    nextDownloadLink = it2.next();
                    String path = nextDownloadLink.getFileOutput();
                    if (new Regex(path, matcher, Pattern.CASE_INSENSITIVE).matches()) {
                        ret.add(nextDownloadLink);
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }
}
