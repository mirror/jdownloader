package org.jdownloader.logging;

public class LogTester {
    public static void main(String[] args) throws InterruptedException {

        new LogTester().testBla();
    }

    public void testBla() throws InterruptedException {
        LogController.CL().info("df");
    }
}
