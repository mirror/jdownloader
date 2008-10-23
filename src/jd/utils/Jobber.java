package jd.utils;

import java.util.ArrayList;
import java.util.LinkedList;

public class Jobber {

    private int paralellWorkerNum;
    private LinkedList<Runnable> jobList;
    private Worker[] workerList;
    private ArrayList<WorkerListener> listener;
    private int currentlyRunningWorker;
    private boolean killWorkerAfterQueueFinished = true;
    private boolean running=false;
/**
 * Jobber.class 
 * Diese Klasse ermöglichtda s paralelle ausführen mehrere Jobs.
 * Es ist möglich während der Ausführung neue Jobs hinzuzufügen.
 * @param i: Anzahl der paralellen Jobs
 */
    public Jobber(int i) {
        this.paralellWorkerNum = i;
        this.currentlyRunningWorker = 0;
        this.jobList = new LinkedList<Runnable>();
        this.listener = new ArrayList<WorkerListener>();

    }

//    /**
//     * @param args
//     */
//    public static void main(String[] args) {
//        // TODO Auto-generated method stub
//        Jobber w = new Jobber(2);
//        w.add(new Runnable() {
//            public void run() {
//                for (int i = 0; i < 20; i++) {
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                    System.out.println(this + " is working");
//                }
//
//            }
//
//        });
//        w.start();
//        w.add(new Runnable() {
//            public void run() {
//                for (int i = 0; i < 20; i++) {
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                    System.out.println(this + " is working");
//                }
//
//            }
//
//        });
//        w.add(new Runnable() {
//            public void run() {
//                for (int i = 0; i < 20; i++) {
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                    System.out.println(this + " is working");
//                }
//
//            }
//
//        });
//
//    }
    /**
     * Gibt zurück ob der JObber noch am leben ist. Falls nicht kann er mit start() neu gestartet werden.
     * Jobber ist kein Thread, und kann auch wieder neu gestartet werden wenn er mal tot ist.
     */
public boolean isAlive(){
    return running;
}
    public void start() {
        if(running)return;
        this.running=true;
        this.createWorker();

    }

    private void createWorker() {
        this.workerList = new Worker[paralellWorkerNum];
     
        currentlyRunningWorker = 0;
        for (int i = 0; i < paralellWorkerNum; i++) {
            increaseWorkingWorkers();
            workerList[i] = new Worker(i);
        }
        System.out.println("created " + paralellWorkerNum + " worker");

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
                    wl.onJobListFinished();
            }
            if (killWorkerAfterQueueFinished) {
                stop();
            }
        }

        return currentlyRunningWorker;
    }
/**
 * Bringt den Jobber um. Alle Workerthreads werden geschlossen. Es kann nicht granatiert werden, dass laufende Jobs zuendegebracht werden.
 * Die Jobqueue geht nicht verloren. Der jobber kann mit start() neu gestartet werden.
 */
    public void stop() {this.running=false;
        for (Worker w : workerList) {
            w.interrupt();
        }

    }

    private Runnable getNextRunnable() {
        synchronized (jobList) {
            if (jobList.size() == 0) return null;
            return jobList.removeFirst();
        }
    }
/**
 * WorkingLIstener werden über den start und stop einzellner jobs informiert
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

    private void fireJobFinished(Runnable job) {
        synchronized (listener) {
            for (WorkerListener wl : listener)
                wl.onJobFinished(job);
        }

    }

    private void fireJobStarted(Runnable job) {
        synchronized (listener) {
            for (WorkerListener wl : listener)
                wl.onJobStarted(job);
        }

    }
/**
 * Fügt neue Jobs hinzu. Jobs können jedereit hinzugefügt werden. ein anschließender jobber.start garantiert, dass der Job auch irgendwann mla abgearbeitet wird.
 * @param runnable
 * @return
 */
    public int add(Runnable runnable) {
        synchronized (jobList) {
            jobList.add(runnable);
            System.out.println(this + " RINGRING!!!!");
            // if a worker sleeps.... this should wake him up

            if (workerList != null) {
                synchronized (workerList) {
                    for (Worker w : workerList) {
                        synchronized (w) {
                            if (w.waitFlag) {
                                System.out.println("Dhoo...Hey " + w + "!! Time to wake up and do some work.");
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
                Runnable ra = getNextRunnable();

                if (ra == null) {

                    System.out.println(this + ": Work is done..I'll sleep now.");
                    decreaseWorkingWorkers();
                    waitFlag = true;
                    synchronized (this) {

                        while (waitFlag) {
                            try {
                                wait();
                            } catch (Exception e) {
                                return;
                            }
                            System.out.println(this + " good morning...get up!");
                        }
                    }
                    System.out.println(this + ": I'm up");
                    continue;
                }
                fireJobStarted(this);
                ra.run();
                fireJobFinished(this);
            }
        }

    }

    public abstract class WorkerListener {

        public abstract void onJobFinished(Runnable job);

        public abstract void onJobListFinished();

        public abstract void onJobStarted(Runnable job);
    }

}
