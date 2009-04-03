package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.Timer;

import jd.controlling.ProgressController;
import jd.controlling.ProgressControllerEvent;
import jd.event.JDBroadcaster;
import jd.event.JDEvent;
import jd.event.JDListener;
import jd.nutils.jobber.JDRunnable;
import jd.nutils.jobber.Jobber;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

public class LinkCheck extends JDBroadcaster implements ActionListener, JDListener {

    private static LinkCheck INSTANCE = getLinkChecker();
    private Timer checkTimer = null;
    private Thread checkThread = null;

    private Vector<DownloadLink> LinksToCheck = new Vector<DownloadLink>();
    private boolean check_running = false;
    protected ProgressController pc;
    protected Jobber checkJobbers;

    public synchronized static LinkCheck getLinkChecker() {
        if (INSTANCE == null) {
            INSTANCE = new LinkCheck();
        }
        return INSTANCE;
    }

    private LinkCheck() {
    }

    public boolean isRunning() {
        return check_running;
    }

    public synchronized void checkLinks(Vector<DownloadLink> links) {
        if (links == null || links.size() == 0) return;
        check_running = true;
        for (DownloadLink element : links) {
            synchronized (LinksToCheck) {
                if (!LinksToCheck.contains(element)) LinksToCheck.add(element);
            }
        }
        if (checkTimer != null) {
            checkTimer.stop();
            checkTimer.removeActionListener(this);
            checkTimer = null;

        }
        checkTimer = new Timer(2000, this);
        checkTimer.setInitialDelay(2000);
        checkTimer.setRepeats(false);
        checkTimer.start();
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
                }
            }
            this.fireJDEvent(new LinkCheckEvent(this, LinkCheckEvent.AFTER_CHECK, hosterList));
            pc.increase(hosterList.size());
        }
    }

    private void startLinkCheck() {
        if (checkThread != null && checkThread.isAlive()) { return; }
        checkThread = new Thread() {
            public void run() {
                setName("OnlineCheck");
                fireJDEvent(new LinkCheckEvent(this, LinkCheckEvent.START));
                pc = new ProgressController("OnlineCheck");
                pc.addJDListener(LinkCheck.getLinkChecker());
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
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    return;
                }
                pc.finalize();
                fireJDEvent(new LinkCheckEvent(this, LinkCheckEvent.STOP));
                check_running = false;
            }
        };
        checkThread.start();
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource() == checkTimer) {
            checkTimer.stop();
            checkTimer.removeActionListener(this);
            checkTimer = null;
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

    public void receiveJDEvent(JDEvent event) {
        if (event instanceof ProgressControllerEvent) {
            if (event.getSource() == this.pc) {
                this.abortLinkCheck();
                fireJDEvent(new LinkCheckEvent(this, LinkCheckEvent.ABORT));
                return;
            }
        }
    }

    public void abortLinkCheck() {
        check_running = false;
        if (checkTimer != null) {
            checkTimer.stop();
            checkTimer.removeActionListener(this);
        }
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

}
