package org.jdownloader.logging;

import java.util.logging.Level;

import org.appwork.utils.logging.Log;

public class LogTester {
    public static void main(String[] args) throws InterruptedException {

        new LogTester().testBla();
    }

    public void testBla() throws InterruptedException {
        LogCollector log = new LogCollector("test", 1);
        log.setParent(Log.L);
        Log.L.setLevel(Level.ALL);
        for (int i = 1; i < 3; i++) {
            log.info(i + "");
            // log.log(Level.ALL, "Exception", new WTFException(i + ""));
        }
        Thread.sleep(100);
        log.flush();
    }
}
