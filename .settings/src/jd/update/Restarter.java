//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.update;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import jd.nutils.Executer;
import jd.nutils.OutdatedParser;
import jd.nutils.zip.UnZip;

/**
 * This class is the mainclass of tinyupdate.jar. This tool is called by
 * JDownloader.jar and moves fresh files from ./update/ to ./ after an update.
 * Since jdownloader.jar is already closed when tinyupdate is running,
 * tinyupdate ca overwrite jdownloader.jar <br>
 * TINYUPDATE.jar must run in JD_HOME
 * 
 * @author coalado
 * 
 */
public class Restarter {
    /**
     * This variable is false and is set to true as soon as the first file could
     * be overwritten. It is used to wait until jdownlaoder.jar has really
     * terminated <br>
     * Take care that you do not use package-extern classes in here.
     */
    private static boolean WAIT_FOR_JDOWNLOADER_TERM = false;
    /**
     * is set by -restart parameter to true, and is used to do a restart aftrer
     * moving files
     */
    private static boolean RESTART = false;
    /**
     * package logger
     */
    private static Logger logger;
    /**
     * is set to true by parameter -nolog ,if the class should not write a log
     * file. Use this if you use this class in another contect than MAIN class
     */
    private static boolean NOLOG = false;

    /**
     * Returns the stacktrace of a Thorwable
     * 
     * @param thrown
     * @return
     */
    public static String getStackTrace(Throwable thrown) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        thrown.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-restart")) RESTART = true;
            if (arg.equalsIgnoreCase("-nolog")) NOLOG = true;
        }
        try {

            // Add to the desired logger
            logger = Logger.getLogger("org.jdownloader");
            if (!NOLOG) {
                // Create a file handler that write log record to a file called
                // my.log
                FileHandler handler = new FileHandler("restarter.log", false);
                logger.addHandler(handler);

            }

        } catch (IOException e) {

        }
        try {
            // waits while jdownloader.jar exixts and cannot be overwritten
            while (new File("JDownloader.jar").exists() && !new File("JDownloader.jar").canWrite()) {
                logger.severe("Wait for jdownloader terminating");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // delete tinyupdate.jar in updatefolder. tinyupdate.jar gets
            // updated by jdownloader.jar. REMEMBER: jar do not update
            // themselves. tinyupdate.jar updates jdownloader.jar and
            // jdownloader.jar updates tinyupdate.jar
            new File("update/tools/tinyupdate.jar").deleteOnExit();
            new File("update/tools/tinyupdate.jar").delete();

            extract(new File("update"));
            move(new File("update"));

            // gives 3 tries to delete all files in outdated.dat
            int i = 0;
            while (!removeFiles() && i <= 3) {

                Thread.sleep(2000);
                i++;

            }
            // use javaw if available. this helps to use a bundelt jre and
            // ensures that jd uses the smae jre over restarts
            String javaPath = new File(new File(System.getProperty("sun.boot.library.path")), "javaw.exe").getAbsolutePath();

            if (RESTART) {
                //Do not use Appwork utils here
                if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                    Executer exec = new Executer("open");
                    exec.setLogger(logger);
                    exec.addParameters(new String[] { "-n", "jDownloader.app" });
                    exec.setRunin(new File(".").getAbsolutePath());
                    exec.setWaitTimeout(0);
                    exec.start();

                } else {
                    Executer exec;
              
                    if (new File(javaPath).exists()) {
                        exec = new Executer(javaPath);
                    } else {
                        exec = new Executer("java");
                    }
                    exec.setLogger(logger);
                    exec.addParameters(new String[] { "-jar", "-Xmx512m", "JDownloader.jar", "-rfu" });
                    exec.setRunin(new File(".").getAbsolutePath());
                    exec.setWaitTimeout(0);
                    exec.start();

                }
                Thread.sleep(1000);
                System.exit(0);
            }

        } catch (Throwable e) {
            logger.severe(getStackTrace(e));
            JOptionPane.showMessageDialog(null, getStackTrace(e));
        }

    }

    /**
     * Extracts all .extract files
     * 
     * @param file
     *            folder which gets scanned recursive for .extract files
     */
    private static void extract(File file) {
        try {
            for (File f : file.listFiles()) {
                if (f.isDirectory()) {
                    extract(f);
                } else {
                    if (f.getName().endsWith(".extract")) {
                        logger.info("Extract: " + f);
                        UnZip u = new UnZip(f, f.getParentFile());
                        u.setOverwrite(false);
                        try {
                            File[] efiles = u.extract();
                            logger.info("-->: " + efiles.length + " files");
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
        }

    }

    /**
     * Removes all files in outdated.dat
     * 
     * @return
     */
    private static boolean removeFiles() {
        return OutdatedParser.parseFile(new File("outdated.dat"));
    }

    /**
     * moves all files from dir to ./ Waits a timeout for 15 ms if the files are
     * locked. (maybe the are blocked by old jvm)
     * 
     * @param dir
     */
    private static void move(File dir) {
        if (!dir.isDirectory()) return;

        main: for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                move(f);

            } else {

                // absolute path to update folder
                String n = new File("update").getAbsolutePath();
                // remove Updatefolder.
                File newFile = new File(f.getAbsolutePath().replace(n, "").substring(1)).getAbsoluteFile();
                logger.info("./update -> real  " + n + " ->" + newFile.getAbsolutePath());
                logger.info("Exists: " + newFile.exists());
                if (!newFile.getParentFile().exists()) {
                    logger.info("Parent Exists: false");

                    if (newFile.getParentFile().mkdirs()) {
                        logger.info("^^CREATED");
                    } else {
                        logger.info("^^CREATION FAILED");
                    }
                }
                int waittime = 15000;
                while (newFile.exists() && !newFile.delete() && !WAIT_FOR_JDOWNLOADER_TERM) {

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    waittime -= 1000;
                    if (waittime < 0) {
                        logger.severe("^^COULD NOT DELETE");
                        continue main;
                    }
                    logger.severe("^^WAIT FOR DELETE");

                }
                if (!newFile.exists()) {
                    WAIT_FOR_JDOWNLOADER_TERM = true;
                    logger.severe("^^DELETE OLD OK");
                } else {
                    logger.severe("^^DELETE OLD FAILED");
                }

                logger.severe("RENAME :" + f.renameTo(newFile));
                if (f.getParentFile().list().length == 0) {
                    logger.severe("^^REMOVED PARENT DIR");
                    f.getParentFile().delete();
                }
            }
        }
        if (dir.list() != null) {
            if (dir.list().length == 0) dir.delete();
        }

    }
}
