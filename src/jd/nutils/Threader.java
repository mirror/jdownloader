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

package jd.nutils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import jd.http.download.Broadcaster;
import jd.http.download.DownloadChunk;
import jd.nutils.jobber.JDRunnable;

/**
 * Dieser Klasse kann man beliebig viele Threads hinzufügen. mit der
 * startAndWait() kann anschliesend gewartet werden bis alle beendet sind
 * 
 * @author coalado
 * 
 */
public class Threader {

    public static void main(String[] args) throws Exception {

        Threader th = new Threader();

        for (int i = 0; i < 1000; i++) {
            th.add(new JDRunnable() {
                public void go() throws Exception {
                    System.out.println("DA");

                }

            });

        }

        th.startAndWait();

        System.out.println("ALLES OK");

    }

    private ArrayList<Worker> workerlist;
    private Integer returnedWorker = 0;
    private boolean waitFlag = false;
    private Broadcaster<WorkerListener> broadcaster;
    private boolean hasDied = false;
    private boolean hasStarted = false;

    public boolean isHasStarted() {
        return hasStarted;
    }

    public Threader() {
        broadcaster = new Broadcaster<WorkerListener>();
        workerlist = new ArrayList<Worker>();
    }

    public Broadcaster<WorkerListener> getBroadcaster() {
        return broadcaster;
    }

    public void add(JDRunnable runnable) {
        if (this.hasDied) throw new IllegalStateException("Threader already has died");
        Worker worker = new Worker(runnable);
        synchronized (workerlist) {
            workerlist.add(worker);
        }
        if (this.hasStarted) worker.start();
    }

    public boolean isHasDied() {
        return hasDied;
    }

    public synchronized void interrupt() {
        for (Worker w : workerlist) {
            if (w.isRunnableAlive()) w.interrupt();
        }
    }

    private synchronized void onWorkerFinished(Worker w) {
        returnedWorker++;
        for (int i = 0; i < broadcaster.size(); i++) {
            broadcaster.get(i).onThreadFinished(this, w.getRunnable());
        }
        if (returnedWorker == workerlist.size()) {
            this.waitFlag = false;
            this.notify();
        }
    }
    public void startWorkers()
    {
        this.hasStarted = true;
        for (Worker w : workerlist) {
            w.start();
        }

        waitFlag = true;
    }
    public void waitOnWorkers()
    {
        synchronized (this) {
            while (waitFlag) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    for (Worker w : workerlist)
                        w.interrupt();

                    return;
                }
            }
        }
        this.hasDied = true;
    }
    public void startAndWait() {
        this.hasStarted = true;
        for (Worker w : workerlist) {
            w.start();
        }

        waitFlag = true;
        synchronized (this) {
            while (waitFlag) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    for (Worker w : workerlist)
                        w.interrupt();

                    return;
                }
            }
        }
        this.hasDied = true;
    }

    public class Worker extends Thread {

        private JDRunnable runnable;
        private boolean runnableAlive = false;

        public Worker(JDRunnable runnable) {

            this.runnable = runnable;

        }

        public String toString() {
            return "Worker for " + runnable;
        }

        public JDRunnable getRunnable() {
            return runnable;
        }

        public void run() {
            try {
                for (int i = 0; i < broadcaster.size(); i++) {
                    broadcaster.get(i).onThreadStarts(Threader.this, getRunnable());
                }
                this.runnableAlive = true;
                runnable.go();
            } catch (Throwable e) {
                for (int i = 0; i < broadcaster.size(); i++) {
                    broadcaster.get(i).onThreadException(Threader.this, getRunnable(), e);
                }

                // JDLogger.exception(e);
            } finally {
                this.runnableAlive = false;
                onWorkerFinished(this);
            }
           
        }

        public boolean isRunnableAlive() {
            return runnableAlive;
        }

    }

    public abstract class WorkerListener {

        public abstract void onThreadFinished(Threader th, JDRunnable runnable);

        public abstract void onThreadStarts(Threader threader, JDRunnable runnable);

        public abstract void onThreadException(Threader th, JDRunnable job, Throwable e);

    }

    public int size() {

        return workerlist.size();
    }

    public JDRunnable get(int i) {

        return workerlist.get(i).getRunnable();
    }

    public void interrupt(DownloadChunk slowest) {
        for (Worker w : workerlist) {
            if (w.getRunnable() == slowest) {
                System.err.println("Interruot:, " + w + " - " + w.getRunnable() + "-" + slowest);
                w.interrupt();
                return;

            }
        }

    }

    public void sort(Comparator<Worker> comparator) {
        Collections.sort(workerlist, comparator);
    }

    public ArrayList<JDRunnable> getAlive() {
        ArrayList<JDRunnable> list = new ArrayList<JDRunnable>();
        for (Worker w : workerlist) {
            if (w.isRunnableAlive()) list.add(w.getRunnable());
        }
        return list;
    }

}
