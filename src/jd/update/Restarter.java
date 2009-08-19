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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import jd.controlling.JDLogger;
import jd.nutils.Executer;
import jd.nutils.OSDetector;

public class Restarter {

    private static boolean WAIT_FOR_JDOWNLOADER_TERM = false;
    private static boolean RESTART = false;
    private static Logger logger;

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

            while (new File("JDownloader.jar").exists() && !new File("JDownloader.jar").canWrite()) {
                logger.severe("Wait for jdownloader terminating");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            new File("update/tools/tinyupdate.jar").deleteOnExit();
            new File("update/tools/tinyupdate.jar").delete();

            move(new File("update"));
            int i = 0;
            while (!removeFiles() && i <= 3) {

                Thread.sleep(2000);
                i++;

            }
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

    public static String getLocalFile(File file) {
        if (file == null) return null;
        if (!file.exists()) { return ""; }
        BufferedReader f;
        try {
            f = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));

            String line;
            StringBuffer ret = new StringBuffer();
            String sep = System.getProperty("line.separator");
            while ((line = f.readLine()) != null) {
                ret.append(line + sep);
            }
            f.close();
            return ret.toString();
        } catch (IOException e) {

            JDLogger.exception(e);
        }
        return "";
    }

    public static String[] getLines(String arg) {
        if (arg == null) { return new String[] {}; }
        String[] temp = arg.split("[\r\n]{1,2}");
        String[] output = new String[temp.length];
        for (int i = 0; i < temp.length; i++) {
            output[i] = temp[i].trim();
        }
        return output;
    }

    public static boolean removeDirectoryOrFile(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String element : children) {
                boolean success = removeDirectoryOrFile(new File(dir, element));
                if (!success) return false;
            }
        }

        return dir.delete();
    }

    public static boolean removeFiles() {
        File outdated = new File("outdated.dat");
        String[] remove = getLines(getLocalFile(outdated));
        String homedir = outdated.getParent();
        boolean ret = true;
        if (remove != null) {
            for (String file : remove) {
                if (file.length() == 0) continue;
                if (!file.matches(".*?" + File.separator + "?\\.+" + File.separator + ".*?")) {
                    File delete = new File(homedir, file);
                    if (!delete.exists()) continue;
                    if (removeDirectoryOrFile(delete)) {
                        logger.info("Removed " + file);
                    } else {
                        ret = false;
                        logger.info(" FAILED to Removed " + file);
                    }
                }
            }
        }
        return ret;
    }

    public static void move(File dir) {
        if (!dir.isDirectory()) return;

        main: for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                move(f);

            } else {

                String n = new File("update").getAbsolutePath();
                File newFile = new File(f.getAbsolutePath().replace(n, "").substring(1)).getAbsoluteFile();

                int waittime = 15000;
                while (newFile.exists() && !newFile.delete() && !WAIT_FOR_JDOWNLOADER_TERM) {

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    waittime -= 1000;
                    if (waittime < 0) {
                        logger.severe("COULD NOT DELETE");
                        continue main;
                    }
                    logger.severe("WAIT FOR DELETE");

                }
                if (!newFile.exists()) {
                    WAIT_FOR_JDOWNLOADER_TERM = true;
                    logger.severe("DELETE OLD OK");
                } else {
                    logger.severe("DELETE OLD FAILED");
                }

                newFile.getParentFile().mkdirs();
                logger.severe(newFile + " exists: " + newFile.exists());
                logger.severe(f + " new exists: " + f.exists());
                logger.severe("RENAME :" + f.renameTo(newFile));
                if (f.getParentFile().list().length == 0) f.getParentFile().delete();
            }
        }
        if (dir.list() != null) {
            if (dir.list().length == 0) dir.delete();
        }

    }
}
