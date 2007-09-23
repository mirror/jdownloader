package jd.controlling;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.controlling.interaction.HTTPReconnect;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.WebUpdate;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.event.UIEvent;
import jd.gui.UIInterface;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.event.PluginEvent;
import jd.plugins.event.PluginListener;
import jd.utils.JDUtilities;

/**
 * Dieser COntroller verwaltet die downloads.WÄhrend StartDownloads.java für die
 * Steuerung eines einzelnen Downloads zuständig ist, ist DownloadWatchdog für
 * die Verwaltung und steuerung der ganzen Download Liste zuständig
 * 
 * @author coalado
 * 
 */
public class DownloadWatchDog extends Thread implements PluginListener, ControlListener {

    /**
     * Der Logger
     */
    private Logger                           logger      = Plugin.getLogger();

    /**
     * Downloadlinks die gerade aktiv sind
     */
    private Vector<SingleDownloadController> activeLinks = new Vector<SingleDownloadController>();

    private boolean                          aborted     = false;

    private JDController                     controller;

    private int                              interactions;

    public DownloadWatchDog(JDController controller) {

        this.controller = controller;
    }

    public void run() {
        int started;
        Vector<DownloadLink> links;
        DownloadLink link;
        boolean hasWaittimeLinks;
        boolean hasInProgressLinks;
        aborted = false;
        interactions = 0;
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOAD_START, this));

        while (aborted != true) {
            if (interactions == 0) {
                if (activeLinks.size() < getSimultanDownloadNum()) {
                    started = setDownloadActive();
                    // logger.info("Started " + started + "Downloads");
                }

                hasWaittimeLinks = false;
                hasInProgressLinks = false;

                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, null));

                links = getDownloadLinks();

                for (int i = 0; i < links.size(); i++) {
                    link = links.elementAt(i);
                    if (!link.isEnabled()) continue;
                    // Link mit Wartezeit in der queue
                    if (link.getStatus() == DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT||link.getStatus() == DownloadLink.STATUS_ERROR_STATIC_WAITTIME) {
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

                if (!hasInProgressLinks && !hasWaittimeLinks && this.getNextDownloadLink() == null) {

                    logger.info("Alle Downloads beendet");
                    fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED, this));
                    Interaction.handleInteraction((Interaction.INTERACTION_ALL_DOWNLOADS_FINISHED), this);
                    break;

                }
            }
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
            }
        }
        logger.info("RUN END");
    }

    /**
     * Deligiert den aufruifd es multicasters an den controller weiter
     * 
     * @param controlEvent
     */
    private void fireControlEvent(ControlEvent controlEvent) {
        controller.fireControlEvent(controlEvent);

    }

    /**
     * Aktiviert Solange neue Downloads, bis die Maxmalanzahl erreicht ist oder
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
     * Gibt die Configeinstellung zurück. WErt vieviele simulaten ndownloads der
     * user erlaubt hat
     * 
     * @return
     */
    public int getSimultanDownloadNum() {
        return (Integer) JDUtilities.getConfiguration().getProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 3);
    }

    /**
     * Zählt die Downloads die bereits über das Hostplugin laufen
     * 
     * @param plugin
     * @return Anzahl der downloads über das plugin
     */
    private int getDownloadNumByHost(PluginForHost plugin) {
        int num = 0;
        for (int i = 0; i < this.activeLinks.size(); i++) {
            if (this.activeLinks.get(i).getDownloadLink().getPlugin().getPluginID().equals(plugin.getPluginID())) {
                num++;
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
        SingleDownloadController download = new SingleDownloadController(controller, dlink);
        logger.info("start download: " + dlink);
        dlink.setInProgress(true);
        download.addControlListener(this);
        download.addControlListener(this.controller);
        download.start();
        activeLinks.add(download);
    }

    //
    // /**
    // * Diese Methode prüft wiederholt die Downloadlinks solange welche dabei
    // * sind die Wartezeit haben. Läuft die Wartezeit ab, oder findet ein
    // * reconnect statt, wird wieder die Run methode aufgerifen
    // */
    // private void waitForDownloadLinks() {
    //
    // logger.info("wait");
    // Vector<DownloadLink> links;
    // DownloadLink link;
    // boolean hasWaittimeLinks = false;
    //
    // boolean returnToRun = false;
    //
    // try {
    // Thread.sleep(1000);
    // }
    // catch (InterruptedException e) {
    //
    // e.printStackTrace();
    // }
    //
    // fireControlEvent(new ControlEvent(this,
    // ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, null));
    //
    // links = getDownloadLinks();
    //
    // for (int i = 0; i < links.size(); i++) {
    // link = links.elementAt(i);
    // if (!link.isEnabled()) continue;
    // // Link mit Wartezeit in der queue
    // if (link.getStatus() == DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT) {
    // if (link.getRemainingWaittime() == 0) {
    //
    // link.setStatus(DownloadLink.STATUS_TODO);
    // link.setEndOfWaittime(0);
    // returnToRun = true;
    //
    // }
    //
    // hasWaittimeLinks = true;
    // // Neuer Link hinzugefügt
    // }
    // else if (link.getStatus() == DownloadLink.STATUS_TODO) {
    // returnToRun = true;
    // }
    //
    // }
    //
    // if (aborted) {
    //
    // logger.warning("Download aborted");
    // // fireControlEvent(new ControlEvent(this,
    // // ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED, this));
    // //
    // Interaction.handleInteraction((Interaction.INTERACTION_ALL_DOWNLOADS_FINISHED),
    // // this);
    //
    // return;
    //
    // }
    // else if (returnToRun) {
    // logger.info("return. there are downloads waiting");
    // this.setDownloadActive();
    // return;
    // }
    //
    // if (!hasWaittimeLinks) {
    //
    // logger.info("Alle Downloads beendet");
    // fireControlEvent(new ControlEvent(this,
    // ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED, this));
    // Interaction.handleInteraction((Interaction.INTERACTION_ALL_DOWNLOADS_FINISHED),
    // this);
    //
    // }
    // else {
    // waitForDownloadLinks();
    // }
    //
    // }
    /**
     * Bricht den Watchdog ab. Alle alufenden downloads werden beendet und die
     * downloadliste zurückgesetzt
     */
    void abort() {
        this.aborted = true;
        for (int i = 0; i < this.activeLinks.size(); i++) {
            activeLinks.get(i).abortDownload();
        }

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
        for (int i = 0; i < links.size(); i++) {
            if (links.elementAt(i).getStatus() != DownloadLink.STATUS_DONE) {
                links.elementAt(i).setInProgress(false);
                links.elementAt(i).setStatusText("");
                links.elementAt(i).setStatus(DownloadLink.STATUS_TODO);
            }

        }
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, null));

    }

    /**
     * Eventfunktion für den PLuginlistener
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

        switch (event.getID()) {

            case ControlEvent.CONTROL_SINGLE_DOWNLOAD_FINISHED:
                if (removeDownloadLinkFromActiveList((DownloadLink) event.getParameter())) {

                    logger.info("removed aktive download. left: " + this.activeLinks.size());
                }

                break;
            case ControlEvent.CONTROL_CAPTCHA_LOADED:
                break;

            case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:

                break;
            case ControlEvent.CONTROL_DISTRIBUTE_FINISHED:
                break;
            case ControlEvent.CONTROL_PLUGIN_INTERACTION_INACTIVE:
                this.interactions++;

            case ControlEvent.CONTROL_PLUGIN_INTERACTION_RETURNED:
                this.interactions--;
            default:

                break;
        }

    }

    private boolean removeDownloadLinkFromActiveList(DownloadLink parameter) {

        for (int i = activeLinks.size() - 1; i >= 0; i--) {

            if (activeLinks.get(i).getDownloadLink() == parameter) {
                activeLinks.remove(i);
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
    protected Vector<DownloadLink> getDownloadLinks() {
        return controller.getDownloadLinks();
    }

    /**
     * Liefert den nächsten DownloadLink
     * 
     * @return Der nächste DownloadLink oder null
     */
    public DownloadLink getNextDownloadLink() {
        Iterator<DownloadLink> iterator = getDownloadLinks().iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();
            logger.info(""+this.isDownloadLinkActive(nextDownloadLink)+"_"+nextDownloadLink.isInProgress()+"_"+nextDownloadLink.isEnabled()+" - "+nextDownloadLink.getStatus()+" - "+nextDownloadLink.getRemainingWaittime()+" - "+getDownloadNumByHost((PluginForHost) nextDownloadLink.getPlugin()));
            if (!this.isDownloadLinkActive(nextDownloadLink) ){
                if( !nextDownloadLink.isInProgress() ){
                    if(nextDownloadLink.isEnabled() ){
                        if(nextDownloadLink.getStatus() == DownloadLink.STATUS_TODO ){
                            if( nextDownloadLink.getRemainingWaittime() == 0 ){
                                if( getDownloadNumByHost((PluginForHost) nextDownloadLink.getPlugin()) < ((PluginForHost) nextDownloadLink.getPlugin()).getMaxSimultanDownloadNum()){
                                    return nextDownloadLink;
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
        for (int i = 0; i < this.activeLinks.size(); i++) {
            if (this.activeLinks.get(i).getDownloadLink() == nextDownloadLink) {
                return true;
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
        for (int i = 0; i < activeLinks.size(); i++) {
            if (activeLinks.get(i).getDownloadLink() == link) {
                return activeLinks.get(i);
            }

        }
        return null;
    }

    /**
     * Liefert die Anzahl der gerade laufenden Downloads. (nur downloads die
     * sich wirklich in der downloadpahse befinden
     * 
     * @return Anzahld er laufenden Downloadsl
     */
    public int getRunningDownloadNum() {
        int ret = 0;
        Iterator<DownloadLink> iterator = getDownloadLinks().iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();
            if (nextDownloadLink.getStatus() == DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS) ret++;

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
        if (dlThread != null) dlThread.abortDownload();

    }

}
