package jd.controlling;

import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.controlling.interaction.Interaction;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.event.PluginEvent;
import jd.plugins.event.PluginListener;
import jd.utils.JDUtilities;

/**
 * Dieser Controller verwaltet die downloads. Während StartDownloads.java für
 * die Steuerung eines einzelnen Downloads zuständig ist, ist DownloadWatchdog
 * für die Verwaltung und steuerung der ganzen Download Liste zuständig
 * 
 * @author JD-Team
 * 
 */
public class DownloadWatchDog extends Thread implements PluginListener, ControlListener {

    /**
     * Der Logger
     */
    private Logger                           logger      = JDUtilities.getLogger();

    /**
     * Downloadlinks die gerade aktiv sind
     */
    private Vector<SingleDownloadController> activeLinks = new Vector<SingleDownloadController>();

    private boolean                          aborted     = false;

    private JDController                     controller;

    private boolean                          pause       = false;

    public DownloadWatchDog(JDController controller) {

        this.controller = controller;
    }

    public void run() {
        // int started;
        Vector<DownloadLink> links;
        DownloadLink link;
        boolean hasWaittimeLinks;
        boolean hasInProgressLinks;
        aborted = false;

        deligateFireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOAD_START, this));

        while (aborted != true) {

            if (Interaction.getInteractionsRunning() == 0) {
                if (activeLinks.size() < getSimultanDownloadNum() && !pause) {
                    setDownloadActive();
                    // logger.info("Started " + started + "Downloads");
                }

                hasWaittimeLinks = false;
                hasInProgressLinks = false;

                deligateFireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, null));

                links = getDownloadLinks();

                synchronized (links) {
                    for (int i = 0; i < links.size(); i++) {
                        link = links.elementAt(i);
                        if (!link.isEnabled()) continue;
                        // Link mit Wartezeit in der queue
                        if (link.getStatus() == DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT || link.getStatus() == DownloadLink.STATUS_ERROR_STATIC_WAITTIME) {
                            if (link.getRemainingWaittime() == 0) {
                                // reaktiviere Downloadlink
                                link.setStatus(DownloadLink.STATUS_TODO);
                                link.setEndOfWaittime(0);

                            }
                            hasWaittimeLinks = true;
                        }
                        if (link.isInProgress()) {
                            // logger.info("ip: "+link);
                            hasInProgressLinks = true;

                        }

                    }
                }
                if ((pause && !hasInProgressLinks) || (!hasInProgressLinks && !hasWaittimeLinks && this.getNextDownloadLink() == null && activeLinks != null && activeLinks.size() == 0)) {

                    logger.info("Alle Downloads beendet");
                    // fireControlEvent(new ControlEvent(this,
                    // ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED, this));
                    // Interaction.handleInteraction((Interaction.INTERACTION_ALL_DOWNLOADS_FINISHED),
                    // this);
                    break;

                }
            }
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
            }
        }
        aborted = true;
        logger.info("RUN END");
        deligateFireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED, this));
        // Interaction.handleInteraction((Interaction.INTERACTION_ALL_DOWNLOADS_FINISHED),
        // this);

    }

    /**
     * Delegiert den Aufruf des multicasters an den controller weiter
     * 
     * @param controlEvent
     */
    private void deligateFireControlEvent(ControlEvent controlEvent) {
        controller.fireControlEvent(controlEvent);

    }

    /**
     * Aktiviert solange neue Downloads, bis die Maxmalanzahl erreicht ist oder
     * die Liste zueende ist
     * 
     * @return
     */
    private int setDownloadActive() {
        DownloadLink dlink;
        int ret = 0;
        // logger.info("Gleichzeitige Downloads erlaubt: " +
        // getSimultanDownloadNum() + " aktiv: " + activeLinks.size());
        while (activeLinks.size() < getSimultanDownloadNum()) {
            dlink = this.getNextDownloadLink();
            if (dlink == null) break;
            this.startDownloadThread(dlink);
            ret++;
        }
        return ret;
    }

    /**
     * Gibt die Configeinstellung zurück, wieviele simultane Downloads der user
     * erlaubt hat
     * 
     * @return
     */
    public int getSimultanDownloadNum() {
        return JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 3);
    }

    /**
     * Zählt die Downloads die bereits über das Hostplugin laufen
     * 
     * @param plugin
     * @return Anzahl der downloads über das plugin
     */
    private int getDownloadNumByHost(PluginForHost plugin) {
        int num = 0;
        synchronized (activeLinks) {
            for (int i = 0; i < this.activeLinks.size(); i++) {
                if (this.activeLinks.get(i).getDownloadLink().getPlugin().getPluginID().equals(plugin.getPluginID())) {
                    num++;
                }

            }
        }
        return num;
    }

    // /**
    // * Entfernt
    // */
    // private void cleanActiveVector() {
    // int statusD;
    // logger.info("Clean Activevector");
    // for (int i = this.activeLinks.size() - 1; i >= 0; i--) {
    // statusD = this.activeLinks.get(i).getDownloadLink().getStatus();
    //
    // if (statusD == DownloadLink.STATUS_DONE ||
    // (this.activeLinks.get(i).getDownloadLink().getPlugin().getCurrentStep()
    // != null &&
    // this.activeLinks.get(i).getDownloadLink().getPlugin().getCurrentStep().getStatus()
    // == PluginStep.STATUS_ERROR)) {
    // activeLinks.remove(i);
    // }
    //
    // }
    //
    // logger.info("Clean ünrig_ " + activeLinks.size());
    //
    // }

    private void startDownloadThread(DownloadLink dlink) {
        synchronized (activeLinks) {
            Interaction.handleInteraction(Interaction.INTERACTION_BEFORE_DOWNLOAD, dlink);
            SingleDownloadController download = new SingleDownloadController(controller, dlink);
            logger.info("start download: " + dlink);
            dlink.setInProgress(true);
            download.addControlListener(this);
            download.addControlListener(this.controller);
            download.start();
            activeLinks.add(download);
        }
    }

    /**
     * Bricht den Watchdog ab. Alle laufenden downloads werden beendet und die
     * downloadliste zurückgesetzt. Diese Funktion blockiert bis alle Downloads
     * erfolgreich abgeborhcen wurden.
     */
    void abort() {
        logger.finer("Breche alle actove links ab");
        ProgressController progress = new ProgressController("Termination", activeLinks.size());
        progress.setStatusText("Stopping all downloads");
        synchronized (activeLinks) {
            for (int i = 0; i < this.activeLinks.size(); i++) {
                activeLinks.get(i).abortDownload();

            }

            deligateFireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, null));
            boolean check = true;
            // Warteschleife bis alle activelinks abgebrochen wurden
            logger.finer("Warten bis alle activeLinks abgebrochen wurden.");

            while (true) {
                progress.increase(1);
                check = true;
                for (int i = 0; i < this.activeLinks.size(); i++) {
                    if (activeLinks.get(i).isAlive()) {
                        check = false;
                        break;
                    }
                }
                if (check) break;
                try {
                    Thread.sleep(500);
                }
                catch (InterruptedException e) {
                }
            }
        }
        progress.finalize();
        logger.finer("Abbruch komplett");
        this.aborted = true;
        this.clearDownloadListStatus();

    }

    /**
     * Setzt den Status der Downloadliste zurück. zB. bei einem Abbruch
     */
    private void clearDownloadListStatus() {
        Vector<DownloadLink> links;
        activeLinks.removeAllElements();
        logger.finer("Clear");
        links = getDownloadLinks();

        synchronized (links) {
            for (int i = 0; i < links.size(); i++) {
                if (links.elementAt(i).getStatus() != DownloadLink.STATUS_DONE) {
                    links.elementAt(i).setInProgress(false);
                    links.elementAt(i).setStatusText("");
                    links.elementAt(i).setStatus(DownloadLink.STATUS_TODO);
                    links.elementAt(i).setEndOfWaittime(0);
                }

            }
        }
        deligateFireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, null));

    }

    /**
     * Eventfunktion für den Pluginlistener
     * 
     * @param event PluginEvent
     */
    public void pluginEvent(PluginEvent event) {

    }

    /**
     * Hier werden ControlEvent ausgewertet
     * 
     * @param event
     */

    public void controlEvent(ControlEvent event) {

        Vector<DownloadLink> links;
        switch (event.getID()) {

            case ControlEvent.CONTROL_SINGLE_DOWNLOAD_FINISHED:
                if (removeDownloadLinkFromActiveList((DownloadLink) event.getParameter())) {

                    logger.info("removed aktive download. left: " + this.activeLinks.size());
                }
                // Wenn ein Download beendet wurde wird überprüft ob gerade ein
                // Download in der Warteschleife steckt. Wenn ja wird ein
                // Reconnectversuch gemacht. Die handleInteraction - funktion
                // blockiert den Aufruf wenn es noch weitere Downloads gibt die
                // gerade laufen
                links = getDownloadLinks();

                synchronized (links) {
                    for (int i = 0; i < links.size(); i++) {
                        if (links.get(i).waitsForReconnect()) {

                            controller.reconnect();

                            break;

                        }

                    }
                }

                break;
            case ControlEvent.CONTROL_CAPTCHA_LOADED:
                break;

            case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:

                break;
            case ControlEvent.CONTROL_DISTRIBUTE_FINISHED:
                break;
            case ControlEvent.CONTROL_PLUGIN_INTERACTION_INACTIVE:

            case ControlEvent.CONTROL_PLUGIN_INTERACTION_RETURNED:

            default:

                break;
        }

    }

    private boolean removeDownloadLinkFromActiveList(DownloadLink parameter) {
        synchronized (activeLinks) {
            for (int i = activeLinks.size() - 1; i >= 0; i--) {

                if (activeLinks.get(i).getDownloadLink() == parameter) {
                    activeLinks.remove(i);
                    return true;
                }

            }
        }
        return false;

    }

    /**
     * Liefert alle DownloadLinks zurück
     * 
     * @return Alle DownloadLinks zurück
     */
    protected Vector<DownloadLink> getDownloadLinks() {
        return controller.getDownloadLinks();
    }

    /**
     * Liefert den nächsten DownloadLink
     * 
     * @return Der nächste DownloadLink oder null
     */
    public DownloadLink getNextDownloadLink() {
        Vector<DownloadLink> links = getDownloadLinks();
        synchronized (links) {
            Iterator<DownloadLink> iterator = links.iterator();
            DownloadLink nextDownloadLink = null;
            while (iterator.hasNext()) {
                nextDownloadLink = iterator.next();
                if (!nextDownloadLink.isInProgress() && nextDownloadLink.getStatus() == DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS) {
                    nextDownloadLink.reset();
                    nextDownloadLink.setStatus(DownloadLink.STATUS_TODO);
                }

                // logger.info(nextDownloadLink+""+!this.isDownloadLinkActive(nextDownloadLink)+"_"+!nextDownloadLink.isInProgress()+"_"+nextDownloadLink.isEnabled()+"-
                // "+(nextDownloadLink.getStatus() ==
                // DownloadLink.STATUS_TODO)+" -
                // "+(nextDownloadLink.getRemainingWaittime() == 0)+" -
                // "+(getDownloadNumByHost((PluginForHost)nextDownloadLink.getPlugin())
                // <
                // ((PluginForHost)nextDownloadLink.getPlugin()).getMaxSimultanDownloadNum())+"
                // :"+nextDownloadLink.getStatus());
                if (!this.isDownloadLinkActive(nextDownloadLink)) {
                    if (!nextDownloadLink.isInProgress()) {
                        if (nextDownloadLink.isEnabled()) {
                            if (nextDownloadLink.getStatus() == DownloadLink.STATUS_TODO) {
                                if (nextDownloadLink.getRemainingWaittime() == 0) {
                                    if (getDownloadNumByHost((PluginForHost) nextDownloadLink.getPlugin()) < ((PluginForHost) nextDownloadLink.getPlugin()).getMaxSimultanDownloadNum()) {

                                        return nextDownloadLink;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    boolean isDownloadLinkActive(DownloadLink nextDownloadLink) {
        synchronized (activeLinks) {
            for (int i = 0; i < this.activeLinks.size(); i++) {
                if (this.activeLinks.get(i).getDownloadLink() == nextDownloadLink) {
                    return true;
                }

            }
        }
        return false;
    }

    /**
     * Gibt den Downloadthread zu einem link zurück
     * 
     * @param link
     * @return
     */
    private SingleDownloadController getDownloadThread(DownloadLink link) {
        synchronized (activeLinks) {
            for (int i = 0; i < activeLinks.size(); i++) {
                if (activeLinks.get(i).getDownloadLink() == link) {
                    return activeLinks.get(i);
                }

            }
        }
        return null;
    }

    /**
     * Liefert die Anzahl der gerade laufenden Downloads. (nur downloads die
     * sich wirklich in der downloadpahse befinden
     * 
     * @return Anzahld er laufenden Downloadsl Sollte eventl mal umgeschrieben
     *         werden. ohne iteration
     */
    public int getRunningDownloadNum() {
        int ret = 0;
        Vector<DownloadLink> links = getDownloadLinks();
        synchronized (links) {
            Iterator<DownloadLink> iterator = links.iterator();
            DownloadLink nextDownloadLink = null;
            while (iterator.hasNext()) {
                nextDownloadLink = iterator.next();
                if (nextDownloadLink.getStatus() == DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS) ret++;

            }
        }
        return ret;
    }

    /**
     * bricht den dpwnloadlink ab.
     * 
     * @param link
     */
    public void abortDownloadLink(DownloadLink link) {
        SingleDownloadController dlThread = getDownloadThread(link);
        if (dlThread != null) {
            dlThread.abortDownload();
            removeDownloadLinkFromActiveList(link);
        }
        ;

    }

    public boolean isAborted() {
        return !isAlive();
    }

    public void pause(boolean value) {
        this.pause = value;
        logger.info("KKK " + value);

    }

}
