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

package jd.utils.jobber;

import java.util.ArrayList;
import java.util.LinkedList;

public class Jobber {

    private int paralellWorkerNum;
    private LinkedList<JDRunnable> jobList;
    private Worker[] workerList;
    private ArrayList<WorkerListener> listener;
    private int currentlyRunningWorker;
    private boolean killWorkerAfterQueueFinished = true;
    private boolean running = false;
    private int jobsAdded = 0;
    boolean debug = false;

    public int getJobsAdded() {
        return jobsAdded;
    }

    private int jobsFinished = 0;

    public int getJobsFinished() {
        return jobsFinished;
    }

    private int jobsStarted = 0;

    public int getJobsStarted() {
        return jobsStarted;
    }

    /**
     * Jobber.class Diese Klasse ermöglichtda s paralelle ausführen mehrere
     * Jobs. Es ist möglich während der Ausführung neue Jobs hinzuzufügen.
     * 
     * @param i
     *            Anzahl der paralellen Jobs
     */
    public Jobber(int i) {
        this.paralellWorkerNum = i;
        this.currentlyRunningWorker = 0;
        this.jobList = new LinkedList<JDRunnable>();
        this.listener = new ArrayList<WorkerListener>();
    }

    /**
     * Gibt zurück ob der JObber noch am leben ist. Falls nicht kann er mit
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
        this.workerList = new Worker[paralellWorkerNum];

        currentlyRunningWorker = 0;
        for (int i = 0; i < paralellWorkerNum; i++) {
            increaseWorkingWorkers();
            workerList[i] = new Worker(i);
        }
        if (debug) System.out.println("created " + paralellWorkerNum + " worker");
    }

    private synchronized int increaseWorkingWorkers() {
        currentlyRunningWorker++;
        return currentlyRunningWorker;
    }

    private synchronized int decreaseWorkingWorkers() {
        currentlyRunningWorker--;
        if (currentlyRunningWorker <= 0) {
            synchronized (listener) {
                for (WorkerListener wl : listener)
                    wl.onJobListFinished(this);
            }
            if (killWorkerAfterQueueFinished) {
                stop();
            }
        }

        return currentlyRunningWorker;
    }

    /**
     * Bringt den Jobber um. Alle Workerthreads werden geschlossen. Es kann
     * nicht granatiert werden, dass laufende Jobs zuendegebracht werden. Die
     * Jobqueue geht nicht verloren. Der jobber kann mit start() neu gestartet
     * werden.
     */
    public void stop() {
        this.running = false;
        for (Worker w : workerList) {
            if (w != null) w.interrupt();
        }
    }

    private JDRunnable getNextJDRunnable() {
        synchronized (jobList) {
            if (jobList.size() == 0) return null;
            return jobList.removeFirst();
        }
    }

    /**
     * WorkingLIstener werden über den start und stop einzellner jobs informiert
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
            for (WorkerListener wl : listener)
                wl.onJobFinished(this, job);
        }
    }
    private void fireJobException(JDRunnable job,Exception e) {
        synchronized (listener) {
            for (WorkerListener wl : listener)
                wl.onJobException(this, job,e);
        }
    }
    private void fireJobStarted(JDRunnable job) {
        synchronized (listener) {
            for (WorkerListener wl : listener)
                wl.onJobStarted(this, job);
        }
    }

    /**
     * Fügt neue Jobs hinzu. Jobs können jedereit hinzugefügt werden. ein
     * anschließender jobber.start garantiert, dass der Job auch irgendwann mla
     * abgearbeitet wird.
     * 
     * @param runnable
     * @return
     */
    public int add(JDRunnable runnable) {
        synchronized (jobList) {
            jobList.add(runnable);
            this.jobsAdded++;
            if (debug) System.out.println(this + " RINGRING!!!!");
            // if a worker sleeps.... this should wake him up

            if (workerList != null) {
                synchronized (workerList) {
                    for (Worker w : workerList) {
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
     * 
     */
    public class Worker extends Thread {

        private int id;
        private boolean waitFlag = false;

        public int getWorkerID() {
            return id;
        }

        public Worker(int i) {
            super("JDWorkerThread" + i);
            this.id = i;
            this.start();
        }

        public String toString() {
            return "Worker no." + id;
        }

        public void run() {
            while (true) {
                JDRunnable ra = getNextJDRunnable();

                if (ra == null) {

                    if (debug) System.out.println(this + ": Work is done..I'll sleep now.");
                    decreaseWorkingWorkers();
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
                jobsStarted++;
                fireJobStarted(ra);
                try {
                    ra.go();
                } catch (Exception e) {            
                    fireJobException(ra,e);
                }
                jobsFinished++;
                fireJobFinished(ra);
            }
        }

    }

    public abstract class WorkerListener {

        public abstract void onJobFinished(Jobber jobber, JDRunnable job);
/**
 * Broadcastes occuring Exceptions
 * @param jobber
 * @param job
 * @param e
 */
        public abstract void onJobException(Jobber jobber, JDRunnable job, Exception e);

        public abstract void onJobListFinished(Jobber jobber);

        public abstract void onJobStarted(Jobber jobber, JDRunnable job);

    }

}
