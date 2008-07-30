package jd.captcha.specials;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Letter;
import jd.utils.JDUtilities;

public class MultiThreadDetection {
    class DetectionThread extends Thread {
        public Letter letter;

        public DetectionThread(Letter l) {
            this.letter = l;
        }

        @Override
        public void run() {
            letter.detected = jac.getLetter(letter);
            startDetection(this);
        }
    }
    private static ArrayList<Letter> DETECTION_QUEUE = new ArrayList<Letter>();
    private static ArrayList<DetectionThread> DETECTION_THREADS = new ArrayList<DetectionThread>();
    public static Letter[] multiCore(Letter[] org, JAntiCaptcha jac) {
       
        int ths = Runtime.getRuntime().availableProcessors();
        MultiThreadDetection mtd = new MultiThreadDetection(ths, jac);
  

//        jac.getJas().getInteger("borderVarianceX");

        for (Letter l : org) {
            mtd.queueDetection(l);
        }
        mtd.waitFor(null);
        return org;
    }
    private JAntiCaptcha jac;

    private Logger logger = JDUtilities.getLogger();

    private int maxThreads;

    public MultiThreadDetection(int threads, JAntiCaptcha jac) {
        logger.info("Run Detection on " + threads + " CPU Cores");
        this.maxThreads = threads;
        this.jac = jac;
    }

    public synchronized void queueDetection(Letter ll) {

        DETECTION_QUEUE.add(ll);

        startDetection(null);

    }

    private synchronized void startDetection(Thread thread) {
        if (thread != null) {

            DETECTION_THREADS.remove(thread);
            logger.info("Thread finished. running: " + DETECTION_THREADS.size());
        }

        if (DETECTION_QUEUE.size() > 0 && DETECTION_THREADS.size() < maxThreads) {
            startThread(DETECTION_QUEUE.remove(0));
            logger.info("NO RUNNING " + DETECTION_THREADS.size() + " Threads. IN queue: " + DETECTION_QUEUE.size());
        }

    }

    private void startThread(Letter l) {
        DetectionThread th = new DetectionThread(l);
        th.start();
        DETECTION_THREADS.add(th);

    };
    
    
    public void waitFor(Vector<Letter> list) {

        while (true) {
            if (list == null) {
                if (DETECTION_THREADS.size() == 0 && DETECTION_QUEUE.size() == 0) return;
            } else {
                boolean found = false;
                for (Iterator<DetectionThread> it = DETECTION_THREADS.iterator(); it.hasNext();) {
                    if (list.contains(it.next().letter)) {
                        found = true;
                        break;
                    }
                }
                if (!found) return;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                
                e.printStackTrace();
            }
        }
    }

}