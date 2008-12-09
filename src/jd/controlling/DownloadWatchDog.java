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
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.Reconnecter;

/**
 * Dieser Controller verwaltet die downloads. Während StartDownloads.java für
 * die Steuerung eines einzelnen Downloads zuständig ist, ist DownloadWatchdog
 * für die Verwaltung und steuerung der ganzen Download Liste zuständig
 * 
 * @author JD-Team
 * 
 */
public class DownloadWatchDog extends Thread implements ControlListener {

    private boolean aborted = false;

    private boolean aborting;

    /**
     * Downloadlinks die gerade aktiv sind
     */
    private Vector<SingleDownloadController> activeDownloadControllers = new Vector<SingleDownloadController>();

    private JDController controller;

    /**
     * Der Logger
     */
    private Logger logger = JDUtilities.getLogger();

    private boolean pause = false;

    private int totalSpeed = 0;

    public DownloadWatchDog(JDController controller) {
        this.setName("Downloadwatchdog");
        this.controller = controller;
        controller.addControlListener(this);
    }

    /**
     * Bricht den Watchdog ab. Alle laufenden downloads werden beendet und die
     * downloadliste zurückgesetzt. Diese Funktion blockiert bis alle Downloads
     * erfolgreich abgeborhcen wurden.
     */
    void abort() {
        logger.finer("Breche alle actove links ab");
        aborting = true;
        aborted = true;

        ProgressController progress = new ProgressController("Termination", activeDownloadControllers.size());
        progress.setStatusText("Stopping all downloads " + activeDownloadControllers);
        progress.setRange(100);
        ArrayList<DownloadLink> al = new ArrayList<DownloadLink>();

        synchronized (activeDownloadControllers) {
            for (SingleDownloadController singleDownloadController : activeDownloadControllers) {
                al.add(singleDownloadController.abortDownload().getDownloadLink());
            }

            deligateFireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, al));
            boolean check = true;
            // Warteschleife bis alle activelinks abgebrochen wurden
            logger.finer("Warten bis alle activeLinks abgebrochen wurden.");

            while (true) {
                progress.increase(1);
                check = true;
                for (int i = 0; i < activeDownloadControllers.size(); i++) {
                    if (activeDownloadControllers.get(i).getDownloadLink().getLinkStatus().isPluginActive()) {
                        check = false;
                        break;
                    }
                }
                if (check) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }
        logger.info("Active links abgebrochen");

        deligateFireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, al));

        progress.finalize();
        logger.finer("Abbruch komplett");

        clearDownloadListStatus();
        aborting = false;
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

    /**
     * Setzt den Status der Downloadliste zurück. zB. bei einem Abbruch
     */
    private void clearDownloadListStatus() {

        activeDownloadControllers.removeAllElements();
        // logger.finer("TODO!!! HIER WERDEN MEINE FEHLERHAFTEN LINKS
        // ZURÜCKGESETZT!");
        PluginForHost.resetStatics();
        Vector<FilePackage> fps;
        Vector<DownloadLink> links;
        fps = controller.getPackages();
        for (FilePackage filePackage : fps) {
            links = filePackage.getDownloadLinks();
            for (int i = 0; i < links.size(); i++) {
                if (!links.elementAt(i).getLinkStatus().hasStatus(LinkStatus.FINISHED)) {

                    links.elementAt(i).getLinkStatus().setStatusText(null);
                    links.elementAt(i).setAborted(false);
                    links.elementAt(i).getLinkStatus().setStatus(LinkStatus.TODO);
                    links.elementAt(i).getLinkStatus().resetWaitTime();
                }

            }
        }
        deligateFireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, null));

    }

    public void controlEvent(ControlEvent event) {

        if (event.getID() == ControlEvent.CONTROL_PLUGIN_INACTIVE && event.getSource() instanceof PluginForHost) {
            removeDownloadLinkFromActiveList(((SingleDownloadController) event.getParameter()).getDownloadLink());
        }

    }

    /**
     * Delegiert den Aufruf des multicasters an den controller weiter
     * 
     * @param controlEvent
     */
    private void deligateFireControlEvent(ControlEvent controlEvent) {
        controller.fireControlEvent(controlEvent);

    }

    public Vector<SingleDownloadController> getActiveDownloadControllers() {
        return activeDownloadControllers;
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
            for (int i = 0; i < activeDownloadControllers.size(); i++) {
                if (activeDownloadControllers.get(i).getDownloadLink().getPlugin().getPluginID().equals(plugin.getPluginID())) {
                    num++;
                }

            }
        }
        return num;
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
     * Liefert den nächsten DownloadLink
     * 
     * @return Der nächste DownloadLink oder null
     */
    public DownloadLink getNextDownloadLink() {

        DownloadLink nextDownloadLink = null;
        try {
            for (FilePackage filePackage : controller.getPackages()) {
                for (Iterator<DownloadLink> it2 = filePackage.getDownloadLinks().iterator(); it2.hasNext();) {
                    nextDownloadLink = it2.next();
                    // Setzt die Wartezeit zurück
                    if (!nextDownloadLink.getLinkStatus().isPluginActive() && nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                        nextDownloadLink.reset();
                        nextDownloadLink.getLinkStatus().setStatus(LinkStatus.TODO);
                    }
                    if (nextDownloadLink.isEnabled()) {
                        if (nextDownloadLink.getPlugin().ignoreHosterWaittime(nextDownloadLink) || nextDownloadLink.getPlugin().getRemainingHosterWaittime() <= 0) {
                            if (!isDownloadLinkActive(nextDownloadLink)) {
                                // if (!nextDownloadLink.isAborted()) {
                                if (!nextDownloadLink.getLinkStatus().isPluginActive()) {

                                    if (nextDownloadLink.getLinkStatus().isStatus(LinkStatus.TODO)) {

                                        int maxPerHost = getSimultanDownloadNumPerHost();
                                        if (maxPerHost == 0) maxPerHost = Integer.MAX_VALUE;

                                        if (getDownloadNumByHost(nextDownloadLink.getPlugin()) < (nextDownloadLink.getPlugin()).getMaxSimultanDownloadNum(nextDownloadLink) && getDownloadNumByHost(nextDownloadLink.getPlugin()) < maxPerHost && nextDownloadLink.getPlugin().getWrapper().usePlugin()) {

                                        return nextDownloadLink; }

                                    }
                                }
                            }
                            // }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fängt concurrentmodification Exceptions ab
        }
        return null;
    }

    /**
     * Gibt die Configeinstellung zurück, wieviele simultane Downloads der user
     * erlaubt hat
     * 
     * @return
     */
    public int getSimultanDownloadNum() {
        return JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2);
    }

    /**
     * Gibt die Configeinstellung zurück, wieviele simultane Downloads der user
     * pro Hoster erlaubt hat
     * 
     * @return
     */
    public int getSimultanDownloadNumPerHost() {
        return JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN_PER_HOST, 0);
    }

    /**
     * @return the totalSpeed
     */
    public int getTotalSpeed() {
        return totalSpeed;
    }

    public boolean isAborted() {
        return !isAlive();
    }

    boolean isDownloadLinkActive(DownloadLink nextDownloadLink) {
        synchronized (activeDownloadControllers) {
            for (int i = 0; i < activeDownloadControllers.size(); i++) {
                if (activeDownloadControllers.get(i).getDownloadLink() == nextDownloadLink) { return true; }

            }
        }
        return false;
    }

    public void pause(boolean value) {
        pause = value;
        logger.info("KKK " + value);

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

    @Override
    public void run() {
        Vector<DownloadLink> links;
        ArrayList<DownloadLink> updates = new ArrayList<DownloadLink>();
        Vector<FilePackage> fps;
        DownloadLink link;
        LinkStatus linkStatus;
        boolean hasWaittimeLinks;
        boolean hasInProgressLinks;
        boolean hasTempDisabledLinks;
        aborted = false;
        int stopCounter = 5;
        int currentTotalSpeed = 0;
        int inProgress = 0;
        Vector<DownloadLink> removes = new Vector<DownloadLink>();
        while (aborted != true) {

            hasWaittimeLinks = false;
            hasInProgressLinks = false;
            hasTempDisabledLinks = false;

            fps = controller.getPackages();
            currentTotalSpeed = 0;
            inProgress = 0;
            updates.clear();
            try {

                for (FilePackage filePackage : fps) {
                    links = filePackage.getDownloadLinks();

                    for (int i = 0; i < links.size(); i++) {
                        link = links.elementAt(i);
                        linkStatus = link.getLinkStatus();
                        if (!link.isEnabled() && link.getLinkType() == DownloadLink.LINKTYPE_JDU && linkStatus.getTotalWaitTime() <= 0) {

                            removes.add(link);
                            continue;
                        }
                        if (!link.isEnabled() && linkStatus.getTotalWaitTime() > 0) {

                            if (linkStatus.getRemainingWaittime() == 0) {
                                link.setEnabled(true);
                                linkStatus.reset();
                                updates.add(link);
                            }
                            hasTempDisabledLinks = true;

                        }

                        // Link mit Wartezeit in der queue
                        if (link.isEnabled() && linkStatus.hasStatus(LinkStatus.ERROR_IP_BLOCKED) && !linkStatus.hasStatus(LinkStatus.PLUGIN_IN_PROGRESS)) {
                            if (linkStatus.getRemainingWaittime() == 0) {
                                // reaktiviere Downloadlink
                                linkStatus.reset();

                            }

                        }

                        if (linkStatus.getRemainingWaittime() > 0) {
                            hasWaittimeLinks = true;
                            updates.add(link);
                        }
                        if (link.isEnabled() && linkStatus.isPluginActive()) {
                            hasInProgressLinks = true;
                        }
                        if (link.isEnabled() && linkStatus.hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                            inProgress++;
                            currentTotalSpeed += link.getDownloadSpeed();
                        }

                    }
                }
                if (removes.size() > 0) {
                    JDUtilities.getController().removeDownloadLinks(removes);
                    removes.clear();
                    JDUtilities.getController().fireControlEvent(ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this);
                }

                Reconnecter.doReconnectIfRequested();
                if (inProgress > 0) {
                    fps = controller.getPackages();

                    for (FilePackage filePackage : fps) {

                        Iterator<DownloadLink> iter = filePackage.getDownloadLinks().iterator();
                        int maxspeed = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0) * 1024;
                        if (maxspeed == 0) {
                            maxspeed = Integer.MAX_VALUE;
                        }
                        int overhead = maxspeed - currentTotalSpeed;

                        totalSpeed = currentTotalSpeed;

                        DownloadLink element;
                        while (iter.hasNext()) {
                            element = iter.next();
                            if (element.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {

                                element.setSpeedLimit(element.getDownloadSpeed() + overhead / inProgress);

                            }
                        }
                    }
                } else {
                    totalSpeed = 0;
                }
                if (updates.size() > 0) {
                    deligateFireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, updates));
                }
                int ret = 0;
                if (Interaction.getInteractionsRunning() == 0 && activeDownloadControllers.size() < getSimultanDownloadNum() && !pause) {
                    ret = setDownloadActive();
                }
                if (ret == 0) {

                    if (pause && !hasInProgressLinks || !hasTempDisabledLinks && !hasInProgressLinks && !hasWaittimeLinks && getNextDownloadLink() == null && activeDownloadControllers != null && activeDownloadControllers.size() == 0) {
                        stopCounter--;
                        if (stopCounter == 0) {
                            totalSpeed = 0;
                            logger.info("Alle Downloads beendet");

                            break;
                        }

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        aborted = true;

        logger.info("Wait for termination");
        while (aborting) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        logger.info("RUN END");
        deligateFireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED, this));
        controller.removeControlListener(this);
        Interaction.handleInteraction(Interaction.INTERACTION_ALL_DOWNLOADS_FINISHED, this);

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
            dlink = getNextDownloadLink();
            if (dlink == null) {
                break;
            }
            if (dlink != getNextDownloadLink()) {
                break;
            }
            startDownloadThread(dlink);
            ret++;
        }
        return ret;
    }

    /**
     * @param totalSpeed
     *            the totalSpeed to set
     */
    public void setTotalSpeed(int totalSpeed) {
        this.totalSpeed = totalSpeed;
    }

    private void startDownloadThread(DownloadLink dlink) {
        synchronized (activeDownloadControllers) {
            Interaction.handleInteraction(Interaction.INTERACTION_BEFORE_DOWNLOAD, dlink);
            SingleDownloadController download = new SingleDownloadController(controller, dlink);
            logger.info("start download: " + dlink);
            dlink.getLinkStatus().setInProgress(true);

            download.start();
            activeDownloadControllers.add(download);
        }
    }

}
