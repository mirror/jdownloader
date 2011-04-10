//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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


 import org.jdownloader.translate.*;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.Timer;

import jd.http.Browser;
import jd.nutils.jobber.JDRunnable;
import jd.nutils.jobber.Jobber;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.event.Eventsender;

class LinkCheckBroadcaster extends Eventsender<LinkCheckListener, LinkCheckEvent> {

    // @Override
    protected void fireEvent(LinkCheckListener listener, LinkCheckEvent event) {
        listener.onLinkCheckEvent(event);
    }

}

public class LinkCheck implements ActionListener, ProgressControllerListener {

    private static LinkCheck               INSTANCE     = null;
    private Timer                          checkTimer   = null;
    private Thread                         checkThread  = null;

    private ArrayList<DownloadLink>        linksToCheck = new ArrayList<DownloadLink>();
    private boolean                        checkRunning = false;
    protected ProgressController           pc;
    protected Jobber                       checkJobbers;
    transient private LinkCheckBroadcaster broadcaster  = new LinkCheckBroadcaster();

    public synchronized static LinkCheck getLinkChecker() {
        if (INSTANCE == null) {
            INSTANCE = new LinkCheck();
        }
        return INSTANCE;
    }

    private LinkCheck() {
        checkTimer = new Timer(2000, this);
        checkTimer.setInitialDelay(2000);
        checkTimer.setRepeats(false);
    }

    public boolean isRunning() {
        return checkRunning;
    }

    public Eventsender<LinkCheckListener, LinkCheckEvent> getBroadcaster() {
        return broadcaster;
    }

    public void removefromWaitingList(ArrayList<DownloadLink> links) {
        synchronized (linksToCheck) {
            linksToCheck.removeAll(links);
        }
    }

    public synchronized void checkLinks(ArrayList<DownloadLink> links, boolean resetLinkCheck) {
        if (links == null || links.size() == 0) return;
        checkRunning = true;
        for (DownloadLink element : links) {
            synchronized (linksToCheck) {
                if (!linksToCheck.contains(element)) {
                    if (resetLinkCheck) element.setAvailableStatus(DownloadLink.AvailableStatus.UNCHECKED);
                    linksToCheck.add(element);
                }
            }
        }
        checkTimer.restart();
    }

    public void checkLinksandWait(ArrayList<DownloadLink> links) {
        final Object lock = new Object();
        final ArrayList<DownloadLink> check = new ArrayList<DownloadLink>(links);
        this.checkLinks(links, false);
        getBroadcaster().addListener(new LinkCheckListener() {

            @SuppressWarnings("unchecked")
            public void onLinkCheckEvent(LinkCheckEvent event) {
                synchronized (check) {
                    if (event.getEventID() == LinkCheckEvent.AFTER_CHECK) {
                        if (event.getParameter() instanceof ArrayList<?>) {
                            ArrayList<DownloadLink> arrayList = (ArrayList<DownloadLink>) event.getParameter();
                            for (DownloadLink k : arrayList) {
                                check.remove(k);
                            }
                        } else if (event.getParameter() instanceof DownloadLink) {
                            check.remove(event.getParameter());
                        }
                        synchronized (lock) {
                            if (check.size() == 0) {
                                lock.notify();
                            }
                        }
                    }
                    if (event.getEventID() == LinkCheckEvent.STOP || event.getEventID() == LinkCheckEvent.ABORT) {
                        synchronized (lock) {
                            if (check.size() == 0) {
                                lock.notify();
                            }
                        }
                    }
                }
            }
        });
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    private void checkHosterList(ArrayList<DownloadLink> hosterList) {
        if (hosterList.size() != 0) {
            DownloadLink link = hosterList.get(0);
            PluginForHost plg = link.getDefaultPlugin().getWrapper().getNewPluginInstance();
            plg.setBrowser(new Browser());
            plg.init();
            try {
                boolean ret = plg.checkLinks(hosterList.toArray(new DownloadLink[] {}));
                if (!ret) {
                    for (int i = 0; i < hosterList.size(); i++) {
                        link = hosterList.get(i);
                        if (!checkRunning) return;
                        if (!link.getBooleanProperty("removed", false)) {
                            link.getAvailableStatus(plg);
                            getBroadcaster().fireEvent(new LinkCheckEvent(this, LinkCheckEvent.AFTER_CHECK, link));
                        }
                        pc.increase(1);
                    }
                } else {
                    getBroadcaster().fireEvent(new LinkCheckEvent(this, LinkCheckEvent.AFTER_CHECK, hosterList));
                    pc.increase(hosterList.size());
                }
            } catch (Throwable e) {
                JDLogger.exception(e);
                pc.increase(hosterList.size());
            }
        }
        DownloadController.getInstance().fireDownloadLinkUpdate(hosterList);
    }

    private void startLinkCheck() {
        if (checkThread != null && checkThread.isAlive()) { return; }
        checkThread = new Thread() {
            public void run() {
                setName("OnlineCheck");
                getBroadcaster().fireEvent(new LinkCheckEvent(this, LinkCheckEvent.START));
                pc = new ProgressController(T._.gui_linkgrabber_pc_onlinecheck(), null);
                pc.getBroadcaster().addListener(LinkCheck.getLinkChecker());
                pc.setRange(0);
                while (linksToCheck.size() != 0) {
                    ArrayList<DownloadLink> currentList;
                    synchronized (linksToCheck) {
                        currentList = new ArrayList<DownloadLink>(linksToCheck);
                        pc.addToMax(currentList.size());
                    }
                    /* onlinecheck, multithreaded damit schneller */
                    HashMap<String, ArrayList<DownloadLink>> map = new HashMap<String, ArrayList<DownloadLink>>();
                    for (DownloadLink dl : currentList) {
                        /* no defaultPlugin available */
                        if (dl.getDefaultPlugin() == null) continue;
                        /*
                         * aufteilung in hosterlisten, um schnellere checks zu
                         * ermöglichen
                         */
                        ArrayList<DownloadLink> localList = map.get(dl.getHost());
                        if (localList == null) {
                            localList = new ArrayList<DownloadLink>();
                            map.put(dl.getHost(), localList);
                        }
                        localList.add(dl);
                    }
                    checkJobbers = new Jobber(4);
                    ArrayList<DownloadLink> hosterList;
                    for (Iterator<ArrayList<DownloadLink>> it = map.values().iterator(); it.hasNext();) {
                        hosterList = it.next();
                        CheckThread cthread = new CheckThread(hosterList);
                        checkJobbers.add(cthread);
                    }
                    int todo = checkJobbers.getJobsAdded();
                    checkJobbers.start();
                    while (checkJobbers.getJobsFinished() != todo) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    checkJobbers.stop();
                    synchronized (linksToCheck) {
                        linksToCheck.removeAll(currentList);
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                pc.doFinalize();
                pc.getBroadcaster().removeListener(LinkCheck.getLinkChecker());
                getBroadcaster().fireEvent(new LinkCheckEvent(this, LinkCheckEvent.STOP));
                checkRunning = false;
            }
        };
        checkThread.start();
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource() == checkTimer) {
            checkTimer.stop();
            if (linksToCheck.size() > 0) {
                startLinkCheck();
            }
            return;
        }
    }

    class CheckThread implements JDRunnable {
        private ArrayList<DownloadLink> links = null;

        public CheckThread(ArrayList<DownloadLink> links) {
            this.links = links;
        }

        public void run() {
            if (links == null || links.size() == 0) return;
            checkHosterList(links);
        }

        public void go() throws Exception {
            run();
        }
    }

    public void abortLinkCheck() {
        checkRunning = false;
        checkTimer.stop();
        if (checkThread != null && checkThread.isAlive()) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    pc.setStatusText(pc.getStatusText() + ": " + T._.gui_linkgrabber_aborted());
                    pc.doFinalize(5000l);
                }
            });
            checkJobbers.stop();
            checkThread.interrupt();
        }
        synchronized (linksToCheck) {
            linksToCheck.clear();
        }
    }

    public void onProgressControllerEvent(ProgressControllerEvent event) {
        if (event.getCaller() == this.pc) {
            this.abortLinkCheck();
            getBroadcaster().fireEvent(new LinkCheckEvent(this, LinkCheckEvent.ABORT));
            return;
        }
    }

}