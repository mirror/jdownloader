package jd.http.download;

import java.util.ArrayList;

import jd.utils.jobber.JDRunnable;

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
    private int returnedWorker = 0;
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
                } catch (Exception e) {
                    return;
                }
            }
        }
        this.hasDied = true;
    }

    public class Worker extends Thread {

        private JDRunnable runnable;

        public Worker(JDRunnable runnable) {
            this.runnable = runnable;
        }

        public JDRunnable getRunnable() {
            return runnable;
        }

        public void run() {
            try {
                runnable.go();
            } catch (Exception e) {
                for (int i = 0; i < broadcaster.size(); i++) {
                    broadcaster.get(i).onThreadException(Threader.this, getRunnable(), e);
                }
                e.printStackTrace();
            }
            onWorkerFinished(this);
        }

    }

    public abstract class WorkerListener {

        public abstract void onThreadFinished(Threader th, JDRunnable job);

        public abstract void onThreadException(Threader th, JDRunnable job, Exception e);

    }

    public int size() {
    
        return workerlist.size();
    }

    public JDRunnable get(int i) {
      
        return workerlist.get(i).getRunnable();
    }


}
