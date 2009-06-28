package jd.update;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import jd.nutils.Executer;
import jd.nutils.OSDetector;

public class Restarter {

    private static boolean WAIT_FOR_JDOWNLOADER_TERM = false;
    private static boolean RESTART = false;
    private static Logger logger;

    /**
     * @param args
     */

    public static String getStackTrace(Throwable thrown) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        thrown.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    public static void main(String[] args) {

        try {
            // Create a file handler that write log record to a file called
            // my.log
            FileHandler handler = new FileHandler("restarter.log", true);

            // Add to the desired logger
            logger = Logger.getLogger("org.jdownloader");
            logger.addHandler(handler);
        } catch (IOException e) {
        }
        try {
            for (String arg : args) {
                if (arg.equalsIgnoreCase("-restart")) RESTART = true;
            }

            String master = new File("JDownloader.jar").getAbsolutePath();
            while (new File("JDownloader.jar").exists() && !new File("JDownloader.jar").canWrite()) {
                logger.severe("Wait for jdownloader terminating");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            move(new File("update"));

            if (RESTART) {
                if (OSDetector.isMac()) {
                    Executer exec = new Executer("open");
                    exec.addParameters(new String[] { "-n", "jDownloader.app" });
                    exec.setRunin(new File(".").getAbsolutePath());
                    exec.setWaitTimeout(0);
                    exec.start();

                } else {

                    Executer exec = new Executer("java");
                    exec.addParameters(new String[] { "-jar", "-Xmx512m", "JDownloader.jar", "-rtfu2" });
                    exec.setRunin(new File(".").getAbsolutePath());
                    exec.setWaitTimeout(0);
                    exec.start();

                }
            }
            System.exit(0);

        } catch (Throwable e) {
            logger.severe(getStackTrace(e));
        }

    }

    public static void move(File dir) {
        if (!dir.isDirectory()) return;

        main: for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                move(f);

            } else {

                String n = new File("update").getAbsolutePath();
                File newFile = new File(f.getAbsolutePath().replace(n, "").substring(1)).getAbsoluteFile();
                logger.severe(newFile.getAbsolutePath());
               
               
                int waittime = 10000;
                while (newFile.exists()&&! newFile.delete() && !WAIT_FOR_JDOWNLOADER_TERM) {
                    WAIT_FOR_JDOWNLOADER_TERM = true;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    waittime -= 1000;
                    if (waittime < 0) {
                        logger.severe("COULD NOT DELETE");
                       continue main;
                    }
                    logger.severe("WAIT FOR DELETE");

                }     logger.severe("DELETE OLD OK");
                newFile.getParentFile().mkdirs();
                
                logger.severe("RENAME :"+f.renameTo(newFile));
                if (f.getParentFile().list().length == 0) f.getParentFile().delete();
            }
        }
        if (dir.list() != null) {
            if (dir.list().length == 0) dir.delete();
        }

    }
}
