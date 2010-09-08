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

import jd.nutils.jobber.JDRunnable;
import jd.nutils.jobber.Jobber;

/**
 * Dieser Klasse kann man beliebig viele Threads hinzufügen. mit der
 * startAndWait() kann anschliesend gewartet werden bis alle beendet sind.<br>
 * TODO: Isn't this the same as the {@link Jobber} ?
 * 
 * @author coalado
 */
public class Threader {

    private ArrayList<Worker>         workerList;
    private Integer                   returnedWorker = 0;
    private boolean                   waitFlag       = false;
    private ArrayList<WorkerListener> listener;
    private boolean                   hasDied        = false;
    private boolean                   hasStarted     = false;

    public Threader() {
        this.workerList = new ArrayList<Worker>();
        this.listener = new ArrayList<WorkerListener>();
    }

    public void add(JDRunnable runnable) {
        if (this.hasDied) throw new IllegalStateException("Threader already has died");
        Worker worker = new Worker(runnable);
        synchronized (workerList) {
            workerList.add(worker);
        }
        if (this.hasStarted) worker.start();
    }

    public boolean isStarted() {
        return hasStarted;
    }

    public boolean isDied() {
        return hasDied;
    }

    public synchronized void interrupt() {
        for (Worker w : workerList) {
            if (w.isRunnableAlive()) w.interrupt();
        }
    }

    private synchronized void onWorkerFinished(Worker w) {
        returnedWorker++;
        fireJobFinished(w.getRunnable());
        if (returnedWorker == workerList.size()) {
            this.waitFlag = false;
            this.notify();
        }
    }

    public void startWorkers() {
        this.hasStarted = true;
        for (Worker w : workerList) {
            w.start();
        }

        waitFlag = true;
    }

    public void waitOnWorkers() {
        synchronized (this) {
            while (waitFlag) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    for (Worker w : workerList)
                        w.interrupt();

                    return;
                }
            }
        }
        this.hasDied = true;
    }

    public void startAndWait() {
        this.hasStarted = true;
        for (Worker w : workerList) {
            w.start();
        }

        waitFlag = true;
        synchronized (this) {
            while (waitFlag) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    for (Worker w : workerList) {
                        w.interrupt();
                    }
                    return;
                }
            }
        }
        this.hasDied = true;
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
                wl.onThreadFinished(this, job);
            }
        }
    }

    private void fireJobException(JDRunnable job, Exception e) {
        synchronized (listener) {
            for (WorkerListener wl : listener) {
                wl.onThreadException(this, job, e);
            }
        }
    }

    private void fireJobStarted(JDRunnable job) {
        synchronized (listener) {
            for (WorkerListener wl : listener) {
                wl.onThreadStarts(this, job);
            }
        }
    }

    private class Worker extends Thread {

        private JDRunnable runnable;
        private boolean    runnableAlive = false;

        public Worker(JDRunnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public String toString() {
            return "Worker for " + runnable;
        }

        public JDRunnable getRunnable() {
            return runnable;
        }

        @Override
        public void run() {
            try {
                fireJobStarted(getRunnable());
                this.runnableAlive = true;
                runnable.go();
            } catch (Exception e) {
                fireJobException(getRunnable(), e);
            } finally {
                this.runnableAlive = false;
                onWorkerFinished(this);
            }
        }

        public boolean isRunnableAlive() {
            return runnableAlive;
        }

    }

    public static interface WorkerListener {

        public void onThreadFinished(Threader th, JDRunnable runnable);

        public void onThreadStarts(Threader threader, JDRunnable runnable);

        public void onThreadException(Threader th, JDRunnable job, Throwable e);

    }

    public int size() {
        return workerList.size();
    }

    public JDRunnable get(int i) {
        return workerList.get(i).getRunnable();
    }

    public void interrupt(JDRunnable slowest) {
        for (Worker w : workerList) {
            if (w.getRunnable() == slowest) {
                System.err.println("Interrupt: " + w + " - " + w.getRunnable());
                w.interrupt();
                return;
            }
        }
    }

    public void sort(Comparator<Worker> comparator) {
        Collections.sort(workerList, comparator);
    }

    public ArrayList<JDRunnable> getAlive() {
        ArrayList<JDRunnable> list = new ArrayList<JDRunnable>();
        for (Worker w : workerList) {
            if (w.isRunnableAlive()) list.add(w.getRunnable());
        }
        return list;
    }

}
