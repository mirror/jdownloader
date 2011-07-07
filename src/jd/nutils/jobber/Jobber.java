//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.nutils.jobber;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;

import jd.controlling.JDLogger;

public class Jobber {

    private int                       paralellWorkerNum;
    private LinkedList<JDRunnable>    jobList;
    private Vector<Worker>            workerList                   = new Vector<Worker>();
    private ArrayList<WorkerListener> listener;
    private boolean                   killWorkerAfterQueueFinished = true;
    private boolean                   running                      = false;
    private Integer                   jobsAdded                    = new Integer(0);
    private boolean                   debug                        = false;

    public int getJobsAdded() {
        return jobsAdded;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    private Integer jobsFinished = new Integer(0);

    public int getJobsFinished() {
        synchronized (jobsFinished) {
            return jobsFinished;
        }
    }

    private Integer jobsStarted = new Integer(0);

    public int getJobsStarted() {
        return jobsStarted;
    }

    /**
     * Jobber.class Diese Klasse ermoeglicht das paralelle Ausfuehren mehrere
     * Jobs. Es ist moeglich waehrend der Ausfuehrung neue Jobs hinzuzufuegen.
     * 
     * @param paralellWorkerNum
     *            Anzahl der paralellen Jobs
     */
    public Jobber(int paralellWorkerNum) {
        this.paralellWorkerNum = paralellWorkerNum;
        this.jobList = new LinkedList<JDRunnable>();
        this.listener = new ArrayList<WorkerListener>();
    }

    /**
     * Gibt zurueck ob der JObber noch am leben ist. Falls nicht kann er mit
     * start() neu gestartet werden. Jobber ist kein Thread, und kann auch
     * wieder neu gestartet werden wenn er mal tot ist.
     */
    public boolean isAlive() {
        return running;
    }

    public void start() {
        if (running) return;
        this.running = true;
        this.createWorker();
    }

    private void createWorker() {
        synchronized (workerList) {
            workerList = new Vector<Worker>();
            for (int i = 0; i < paralellWorkerNum; i++) {
                workerList.add(new Worker(i, Jobber.this));
            }
        }
        if (debug) System.out.println("created " + paralellWorkerNum + " worker");
    }

    /**
     * Bringt den Jobber um. Alle Workerthreads werden geschlossen. Es kann
     * nicht granatiert werden, dass laufende Jobs zuendegebracht werden. Die
     * Jobqueue geht nicht verloren. Der jobber kann mit start() neu gestartet
     * werden.
     */
    public void stop() {
        this.running = false;
        if (workerList == null) return;
        synchronized (workerList) {
            Vector<Worker> tmp = new Vector<Worker>(workerList);
            for (Worker w : tmp) {
                if (w != null) {
                    w.interrupt();
                }
            }
        }
    }

    private synchronized void workerDone() {
        int count = 0;
        synchronized (workerList) {
            Vector<Worker> tmp = new Vector<Worker>(workerList);
            for (Worker w : tmp) {
                if (w.isAlive()) count++;
            }
            if (count <= 1) {
                running = false;
                if (debug) System.out.println(this + " All worker finished, this Jobber has done his job!");
            }
        }
    }

    private JDRunnable getNextJDRunnable() {
        if (!this.isAlive() || jobList == null) return null;
        synchronized (jobList) {
            if (jobList.isEmpty()) {
                synchronized (listener) {
                    for (WorkerListener wl : listener) {
                        wl.onJobListFinished(this);
                    }
                }
                return null;
            }
            return jobList.removeFirst();
        }
    }

    /**
     * WorkingLIstener werden ueber den start und stop einzellner jobs
     * informiert
     * 
     * @param wl
     */
    public void addWorkerListener(WorkerListener wl) {
        synchronized (listener) {
            listener.add(wl);
        }
    }

    public void removeWorkerListener(WorkerListener wl) {
        synchronized (listener) {
            listener.remove(wl);
        }
    }

    private void fireJobFinished(JDRunnable job) {
        synchronized (listener) {
            for (WorkerListener wl : listener) {
                wl.onJobFinished(this, job);
            }
        }
    }

    private void fireJobException(JDRunnable job, Exception e) {
        synchronized (listener) {
            for (WorkerListener wl : listener) {
                wl.onJobException(this, job, e);
            }
        }
    }

    private void fireJobStarted(JDRunnable job) {
        synchronized (listener) {
            for (WorkerListener wl : listener) {
                wl.onJobStarted(this, job);
            }
        }
    }

    /**
     * Fuegt neue Jobs hinzu. Jobs koennen jedereit hinzugefuegt werden. ein
     * anschliessender jobber.start garantiert, dass der Job auch irgendwann mla
     * abgearbeitet wird.
     * 
     * @param runnable
     * @return
     */
    public int add(JDRunnable runnable) {
        if (jobList == null) {
            System.out.println("Dhoo...No joblist available!?");
            return -1;
        }
        synchronized (jobList) {
            jobList.add(runnable);
            synchronized (this.jobsAdded) {
                this.jobsAdded++;
            }
            if (debug) System.out.println(this + " RINGRING!!!!");
            // if a worker sleeps.... this should wake him up

            if (workerList != null) {
                synchronized (workerList) {
                    Vector<Worker> tmp = new Vector<Worker>(workerList);
                    for (Worker w : tmp) {
                        if (w != null) {
                            synchronized (w) {
                                if (w.waitFlag) {
                                    if (debug) System.out.println("Dhoo...Hey " + w + "!! Time to wake up and do some work.");
                                    w.waitFlag = false;
                                    w.notify();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            return jobList.size();
        }
    }

    public void setKillWorkerAfterQueueFinished(boolean killWorkerAfterQueueFinished) {
        this.killWorkerAfterQueueFinished = killWorkerAfterQueueFinished;
    }

    public boolean isKillWorkerAfterQueueFinished() {
        return killWorkerAfterQueueFinished;
    }

    /**
     * Ein Worker arbeitet sequentiell jobs ab
     * 
     * @author coalado
     */
    // final, because the constructor calls Thread.start(),
    // see http://findbugs.sourceforge.net/bugDescriptions.html#SC_START_IN_CTOR
    public final class Worker extends Thread {

        private int     id;
        private boolean waitFlag = false;
        private Jobber  jobber;

        public int getWorkerID() {
            return id;
        }

        public Worker(int id, Jobber jobber) {
            super("JDWorkerThread" + id);
            this.jobber = jobber;
            this.id = id;
            this.start();
        }

        @Override
        public String toString() {
            return "Worker no." + id;
        }

        @Override
        public void run() {
            while (true) {
                JDRunnable ra = getNextJDRunnable();

                if (ra == null) {
                    if (killWorkerAfterQueueFinished) {
                        jobber.workerDone();
                        return;
                    }
                    if (debug) System.out.println(this + ": Work is done..I'll sleep now.");
                    waitFlag = true;
                    synchronized (this) {
                        while (waitFlag) {
                            try {
                                wait();
                            } catch (Exception e) {
                                return;
                            }
                            if (debug) System.out.println(this + " good morning...get up!");
                        }
                    }
                    if (debug) System.out.println(this + ": I'm up");
                    continue;
                }
                synchronized (jobsStarted) {
                    jobsStarted++;
                }
                fireJobStarted(ra);
                try {
                    ra.go();
                } catch (Exception e) {
                    JDLogger.exception(e);
                    fireJobException(ra, e);
                }
                synchronized (jobsFinished) {
                    jobsFinished++;
                }
                fireJobFinished(ra);
            }
        }

    }

    public static interface WorkerListener {

        public void onJobFinished(Jobber jobber, JDRunnable job);

        /**
         * Broadcastes occuring Exceptions
         * 
         * @param jobber
         * @param job
         * @param e
         */
        public void onJobException(Jobber jobber, JDRunnable job, Exception e);

        public void onJobListFinished(Jobber jobber);

        public void onJobStarted(Jobber jobber, JDRunnable job);

    }

}
