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

package jd.captcha.specials;

import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Logger;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Letter;
import jd.controlling.JDLogger;

public class MultiThreadDetection {
    class DetectionThread extends Thread {
        public Letter letter;

        public DetectionThread(Letter l) {
            letter = l;
        }

        // @Override
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

        // jac.getJas().getInteger("borderVarianceX");

        for (Letter l : org) {
            mtd.queueDetection(l);
        }
        mtd.waitFor(null);
        return org;
    }

    private JAntiCaptcha jac;

    private Logger logger = jd.controlling.JDLogger.getLogger();

    private int maxThreads;

    public MultiThreadDetection(int threads, JAntiCaptcha jac) {
        logger.info("Run Detection on " + threads + " CPU Cores");
        maxThreads = threads;
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

    }

    public void waitFor(Vector<Letter> list) {

        while (true) {
            if (list == null) {
                if (DETECTION_THREADS.size() == 0 && DETECTION_QUEUE.size() == 0) { return; }
            } else {
                boolean found = false;
                for (DetectionThread detectionThread : DETECTION_THREADS) {
                    if (list.contains(detectionThread.letter)) {
                        found = true;
                        break;
                    }
                }
                if (!found) { return; }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {

                JDLogger.exception(e);
                return;
            }
        }
    }

}