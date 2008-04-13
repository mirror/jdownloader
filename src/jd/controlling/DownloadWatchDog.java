//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.controlling;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.controlling.interaction.Interaction;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

/**
 * Dieser Controller verwaltet die downloads. Während StartDownloads.java für
 * die Steuerung eines einzelnen Downloads zuständig ist, ist DownloadWatchdog
 * für die Verwaltung und steuerung der ganzen Download Liste zuständig
 * 
 * @author JD-Team
 * 
 */
public class DownloadWatchDog extends Thread implements ControlListener {

    /**
     * Der Logger
     */
    private Logger logger = JDUtilities.getLogger();

    /**
     * Downloadlinks die gerade aktiv sind
     */
    private Vector<SingleDownloadController> activeDownloadControllers = new Vector<SingleDownloadController>();

    private boolean aborted = false;

    private JDController controller;

    private boolean pause = false;

    private int totalSpeed = 0;

    private boolean aborting;

    public DownloadWatchDog(JDController controller) {

        this.controller = controller;
       controller.addControlListener(this);
    }

    public void run() {
        // int started;
        Vector<DownloadLink> links;
        Vector<FilePackage> fps;
        DownloadLink link;
        boolean hasWaittimeLinks;
        boolean hasInProgressLinks;
        aborted = false;

       int currentTotalSpeed = 0;
        int inProgress = 0;
        while (aborted != true) {

            hasWaittimeLinks = false;
            hasInProgressLinks = false;

            //deligateFireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOADLINK_DATA_CHANGED, null));
            fps = controller.getPackages();
            currentTotalSpeed = 0;
            inProgress = 0;
            for (Iterator<FilePackage> fpsIt = fps.iterator(); fpsIt.hasNext();) {
                links = fpsIt.next().getDownloadLinks();

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
                    if (link.getStatus() == DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS) {
                        inProgress++;
                        currentTotalSpeed += link.getDownloadSpeed();
                    }

                }
            }
            if (inProgress > 0) {
                fps = controller.getPackages();

                for (Iterator<FilePackage> fpsIt = fps.iterator(); fpsIt.hasNext();) {

                    Iterator<DownloadLink> iter = fpsIt.next().getDownloadLinks().iterator();
                    int maxspeed = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0) * 1024;
                    boolean isLimited = (maxspeed != 0);
                    if (maxspeed == 0) maxspeed = Integer.MAX_VALUE;
                    int overhead = maxspeed - currentTotalSpeed;
                    // logger.info("cu speed= " + currentTotalSpeed + " overhead
                    // :;" + overhead);
                    this.totalSpeed = currentTotalSpeed;

                    DownloadLink element;
                    while (iter.hasNext()) {
                        element = (DownloadLink) iter.next();
                        element.setLimited(isLimited);
                        if (element.getStatus() == DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS) {

                            element.setMaximalSpeed(element.getDownloadSpeed() + overhead / inProgress);

                        }
                    }
                }
            } else {
                this.totalSpeed = 0;
            }
            if (Interaction.getInteractionsRunning() == 0) {
                if (activeDownloadControllers.size() < getSimultanDownloadNum() && !pause) {
                    setDownloadActive();
                    // logger.info("Started " + started + "Downloads");
                }
            }

            if ((pause && !hasInProgressLinks) || (!hasInProgressLinks && !hasWaittimeLinks && this.getNextDownloadLink() == null && activeDownloadControllers != null && activeDownloadControllers.size() == 0)) {
                this.totalSpeed = 0;
                logger.info("Alle Downloads beendet");
                // fireControlEvent(new ControlEvent(this,
                // ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED, this));
                // Interaction.handleInteraction((Interaction.INTERACTION_ALL_DOWNLOADS_FINISHED),
                // this);
                break;

            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        aborted = true;
        logger.info("Wait for termination");
        while(aborting){
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
        }
        logger.info("RUN END");
        deligateFireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED, this));
        controller.removeControlListener(this);
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
        while (activeDownloadControllers.size() < getSimultanDownloadNum()) {
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
        synchronized (activeDownloadControllers) {
            for (int i = 0; i < this.activeDownloadControllers.size(); i++) {
                if (this.activeDownloadControllers.get(i).getDownloadLink().getPlugin().getPluginID().equals(plugin.getPluginID())) {
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
        synchronized (activeDownloadControllers) {
            Interaction.handleInteraction(Interaction.INTERACTION_BEFORE_DOWNLOAD, dlink);
            SingleDownloadController download = new SingleDownloadController(controller, dlink);
            logger.info("start download: " + dlink);
            dlink.setInProgress(true);
      
            download.start();
            activeDownloadControllers.add(download);
        }
    }

    /**
     * Bricht den Watchdog ab. Alle laufenden downloads werden beendet und die
     * downloadliste zurückgesetzt. Diese Funktion blockiert bis alle Downloads
     * erfolgreich abgeborhcen wurden.
     */
    void abort() {
        logger.finer("Breche alle actove links ab");
        this.aborting=true;
        this.aborted = true;
       
        ProgressController progress = new ProgressController("Termination", activeDownloadControllers.size());
        progress.setStatusText("Stopping all downloads "+activeDownloadControllers);
        progress.setRange(100);
        ArrayList<DownloadLink> al= new ArrayList<DownloadLink>();
        
        synchronized (activeDownloadControllers) {
            for (Iterator<SingleDownloadController> it=activeDownloadControllers.iterator();it.hasNext();) {
                al.add(it.next().abortDownload().getDownloadLink());
            }

            deligateFireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOADLINKS_CHANGED, al));
            boolean check = true;
            // Warteschleife bis alle activelinks abgebrochen wurden
            logger.finer("Warten bis alle activeLinks abgebrochen wurden.");

            while (true) {
                progress.increase(1);
                check = true;
                for (int i = 0; i < this.activeDownloadControllers.size(); i++) {
                    if (activeDownloadControllers.get(i).getDownloadLink().isInProgress()) {
                        check = false;
                        break;
                    }
                }
                if (check) break;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }
        logger.info("Active links abgebrochen");
        
        deligateFireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOADLINKS_CHANGED, al));
        
       
        progress.finalize();
        logger.finer("Abbruch komplett");

        this.clearDownloadListStatus();
        this.aborting=false;
    }

    /**
     * Setzt den Status der Downloadliste zurück. zB. bei einem Abbruch
     */
    private void clearDownloadListStatus() {

        activeDownloadControllers.removeAllElements();
        logger.finer("TODO!!! HIER WERDEN MEINE FEHLERHAFTEN LINKS ZURÜCKGESETZT!");
        Vector<FilePackage> fps;
        Vector<DownloadLink> links;
        fps = controller.getPackages();
        for (Iterator<FilePackage> fpsIt = fps.iterator(); fpsIt.hasNext();) {
            links = fpsIt.next().getDownloadLinks();
            for (int i = 0; i < links.size(); i++) {
                if (links.elementAt(i).getStatus() != DownloadLink.STATUS_DONE) {
                    links.elementAt(i).setInProgress(false);
                    links.elementAt(i).setStatusText("");
                    links.elementAt(i).setStatus(DownloadLink.STATUS_TODO);
                    links.elementAt(i).setEndOfWaittime(0);
                }

            }
        }
        deligateFireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOADLINK_DATA_CHANGED, null));

    }





    public void controlEvent(ControlEvent event) {

        switch (event.getID()) {

        case ControlEvent.CONTROL_PLUGIN_INACTIVE:
            if(!(event.getSource() instanceof PluginForHost))return;
            removeDownloadLinkFromActiveList(((SingleDownloadController) event.getParameter()).getDownloadLink());
            // Wenn ein Download beendet wurde wird überprüft ob gerade ein
            // Download in der Warteschleife steckt. Wenn ja wird ein
            // Reconnectversuch gemacht. Die handleInteraction - funktion
            // blockiert den Aufruf wenn es noch weitere Downloads gibt die
            // gerade laufen
            one: for (Iterator<FilePackage> it = controller.getPackages().iterator(); it.hasNext();) {
                for (Iterator<DownloadLink> it2 = it.next().getDownloadLinks().iterator(); it2.hasNext();) {

                    if (it2.next().waitsForReconnect()) {

                        controller.requestReconnect();

                        break one;

                    }

                }
            }

            break;
     
        case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:

            break;
        case ControlEvent.CONTROL_DISTRIBUTE_FINISHED:
            break;
   
        }

    }

    private boolean removeDownloadLinkFromActiveList(DownloadLink parameter) {
        synchronized (activeDownloadControllers) {
            for (int i = activeDownloadControllers.size() - 1; i >= 0; i--) {

                if (activeDownloadControllers.get(i).getDownloadLink() == parameter) {
                    activeDownloadControllers.remove(i);
                    return true;
                }

            }
        }
        return false;

    }

    /**
     * Liefert den nächsten DownloadLink
     * 
     * @return Der nächste DownloadLink oder null
     */
    public DownloadLink getNextDownloadLink() {

        DownloadLink nextDownloadLink = null;

        for (Iterator<FilePackage> it = controller.getPackages().iterator(); it.hasNext();) {
            for (Iterator<DownloadLink> it2 = it.next().getDownloadLinks().iterator(); it2.hasNext();) {
                nextDownloadLink = it2.next();
                if (!nextDownloadLink.isInProgress() && nextDownloadLink.getStatus() == DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS) {
                    nextDownloadLink.reset();
                    nextDownloadLink.setStatus(DownloadLink.STATUS_TODO);
                }

                if (!this.isDownloadLinkActive(nextDownloadLink)) {
                    if (!nextDownloadLink.isInProgress()) {
                        if (nextDownloadLink.isEnabled()) {
                            if (nextDownloadLink.getStatus() == DownloadLink.STATUS_TODO) {
                                if (nextDownloadLink.getRemainingWaittime() == 0) {
                                    if (getDownloadNumByHost((PluginForHost) nextDownloadLink.getPlugin()) < ((PluginForHost) nextDownloadLink.getPlugin()).getMaxSimultanDownloadNum()) {

                                    return nextDownloadLink; }
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
        synchronized (activeDownloadControllers) {
            for (int i = 0; i < this.activeDownloadControllers.size(); i++) {
                if (this.activeDownloadControllers.get(i).getDownloadLink() == nextDownloadLink) { return true; }

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
        synchronized (activeDownloadControllers) {
            for (int i = 0; i < activeDownloadControllers.size(); i++) {
                if (activeDownloadControllers.get(i).getDownloadLink() == link) { return activeDownloadControllers.get(i); }

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
        DownloadLink nextDownloadLink;
        for (Iterator<FilePackage> it = controller.getPackages().iterator(); it.hasNext();) {
            for (Iterator<DownloadLink> it2 = it.next().getDownloadLinks().iterator(); it2.hasNext();) {

                nextDownloadLink = it2.next();
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

    /**
     * @param totalSpeed
     *            the totalSpeed to set
     */
    public void setTotalSpeed(int totalSpeed) {
        this.totalSpeed = totalSpeed;
    }

    /**
     * @return the totalSpeed
     */
    public int getTotalSpeed() {
        return totalSpeed;
    }

}
