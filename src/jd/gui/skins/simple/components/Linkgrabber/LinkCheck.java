package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.Timer;

import jd.controlling.DownloadController;
import jd.controlling.ProgressController;
import jd.controlling.ProgressControllerEvent;
import jd.controlling.ProgressControllerListener;
import jd.event.JDBroadcaster;
import jd.nutils.jobber.JDRunnable;
import jd.nutils.jobber.Jobber;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;

class LinkCheckBroadcaster extends JDBroadcaster<LinkCheckListener, LinkCheckEvent> {

    // @Override
    protected void fireEvent(LinkCheckListener listener, LinkCheckEvent event) {
        listener.onLinkCheckEvent(event);
    }

}

public class LinkCheck implements ActionListener, ProgressControllerListener {

    private static LinkCheck INSTANCE = null;
    private Timer checkTimer = null;
    private Thread checkThread = null;

    private Vector<DownloadLink> LinksToCheck = new Vector<DownloadLink>();
    private boolean check_running = false;
    protected ProgressController pc;
    protected Jobber checkJobbers;
    transient private LinkCheckBroadcaster broadcaster = new LinkCheckBroadcaster();

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
        return check_running;
    }

    public synchronized JDBroadcaster<LinkCheckListener, LinkCheckEvent> getBroadcaster() {
        if (broadcaster == null) broadcaster = new LinkCheckBroadcaster();
        return this.broadcaster;
    }

    public synchronized void checkLinks(Vector<DownloadLink> links) {
        if (links == null || links.size() == 0) return;
        check_running = true;
        for (DownloadLink element : links) {
            synchronized (LinksToCheck) {
                if (!LinksToCheck.contains(element)) LinksToCheck.add(element);
            }
        }
        checkTimer.restart();
    }

    private void checkHosterList(Vector<DownloadLink> hosterList) {
        if (hosterList.size() != 0) {
            DownloadLink link = hosterList.get(0);
            boolean ret = ((PluginForHost) link.getPlugin()).checkLinks(hosterList.toArray(new DownloadLink[] {}));
            if (!ret) {
                for (int i = 0; i < hosterList.size(); i++) {
                    link = hosterList.get(i);
                    if (!check_running) return;
                    link.isAvailable();
                    getBroadcaster().fireEvent(new LinkCheckEvent(this, LinkCheckEvent.AFTER_CHECK, link));
                    pc.increase(1);
                }
            } else {
                getBroadcaster().fireEvent(new LinkCheckEvent(this, LinkCheckEvent.AFTER_CHECK, hosterList));
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
                pc = new ProgressController(JDLocale.L("gui.linkgrabber.pc.onlinecheck", "Checking online availability..."));
                pc.getBroadcaster().addListener(LinkCheck.getLinkChecker());
                pc.setRange(0);
                while (LinksToCheck.size() != 0) {
                    Vector<DownloadLink> currentList;
                    synchronized (LinksToCheck) {
                        currentList = new Vector<DownloadLink>(LinksToCheck);
                        pc.addToMax(currentList.size());
                    }
                    /* onlinecheck, multithreaded damit schneller */
                    HashMap<String, Vector<DownloadLink>> map = new HashMap<String, Vector<DownloadLink>>();
                    for (DownloadLink dl : currentList) {
                        /*
                         * aufteilung in hosterlisten, um schnellere checks zu
                         * ermöglichen
                         */
                        Vector<DownloadLink> localList = map.get(dl.getPlugin().getHost());
                        if (localList == null) {
                            localList = new Vector<DownloadLink>();
                            map.put(dl.getPlugin().getHost(), localList);
                        }
                        localList.add(dl);
                    }
                    checkJobbers = new Jobber(4);
                    Vector<DownloadLink> hosterList;
                    for (Iterator<Vector<DownloadLink>> it = map.values().iterator(); it.hasNext();) {
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
                    synchronized (LinksToCheck) {
                        LinksToCheck.removeAll(currentList);
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                pc.finalize();
                pc.getBroadcaster().removeListener(LinkCheck.getLinkChecker());
                getBroadcaster().fireEvent(new LinkCheckEvent(this, LinkCheckEvent.STOP));
                check_running = false;
            }
        };
        checkThread.start();
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource() == checkTimer) {
            checkTimer.stop();
            if (LinksToCheck.size() > 0) {
                startLinkCheck();
            }
            return;
        }
    }

    class CheckThread implements JDRunnable {
        private Vector<DownloadLink> links = null;

        public CheckThread(Vector<DownloadLink> links) {
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
        check_running = false;
        checkTimer.stop();
        if (checkThread != null && checkThread.isAlive()) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    pc.setStatusText(pc.getStatusText() + ": Aborted");
                    pc.finalize(5000l);
                }
            });
            checkJobbers.stop();
            checkThread.interrupt();
        }
        synchronized (LinksToCheck) {
            LinksToCheck = new Vector<DownloadLink>();
        }
    }

    public void onProgressControllerEvent(ProgressControllerEvent event) {
        if (event.getSource() == this.pc) {
            this.abortLinkCheck();
            getBroadcaster().fireEvent(new LinkCheckEvent(this, LinkCheckEvent.ABORT));
            return;
        }
    }

}
