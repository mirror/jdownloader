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

package jd.update;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;

import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;

public class Main {

    public static int BOTHRESIZE = GridBagConstraints.BOTH;
    public static Insets INSETS = new Insets(5, 5, 5, 5);
    public static int NORESIZE = GridBagConstraints.NONE;
    public static int NORTHWEST = GridBagConstraints.NORTHWEST;
    public static int REL = GridBagConstraints.RELATIVE;
    public static int REM = GridBagConstraints.REMAINDER;
    private static SubConfiguration guiConfig;
    private static StringBuilder log;
    private static JFrame frame;
    private static JTextArea logWindow;

    private static JProgressBar progressload;

    /**
     * Genau wie add, aber mit den Standardwerten iPadX,iPadY=0
     * 
     * @param cont
     *            Der Container, dem eine Komponente hinzugefuegt werden soll
     * @param comp
     *            Die Komponente, die hinzugefuegt werden soll
     * @param x
     *            X-Position innerhalb des GriBagLayouts
     * @param y
     *            Y-Position innerhalb des GriBagLayouts
     * @param width
     *            Anzahl der Spalten, ueber die sich diese Komponente erstreckt
     * @param height
     *            Anzahl der Reihen, ueber die sich diese Komponente erstreckt
     * @param weightX
     *            Verteilung von zur Verfuegung stehendem Platz in X-Richtung
     * @param weightY
     *            Verteilung von zur Verfuegung stehendem Platz in Y-Richtung
     * @param insets
     *            Abstände der Komponente
     * @param fill
     *            Verteilung der Komponente innerhalb der zugewiesen Zelle/n
     * @param anchor
     *            Positionierung der Komponente innerhalb der zugewiesen Zelle/n
     */
    public static void addToGridBag(Container cont, Component comp, int x, int y, int width, int height, int weightX, int weightY, Insets insets, int fill, int anchor) {
        if (cont == null) return;
        if (comp == null) return;
        Main.addToGridBag(cont, comp, x, y, width, height, weightX, weightY, insets, 0, 0, fill, anchor);
    }

    /**
     * Diese Klasse fuegt eine Komponente einem Container hinzu
     * 
     * @param cont
     *            Der Container, dem eine Komponente hinzugefuegt werden soll
     * @param comp
     *            Die Komponente, die hinzugefuegt werden soll
     * @param x
     *            X-Position innerhalb des GriBagLayouts
     * @param y
     *            Y-Position innerhalb des GriBagLayouts
     * @param width
     *            Anzahl der Spalten, ueber die sich diese Komponente erstreckt
     * @param height
     *            Anzahl der Reihen, ueber die sich diese Komponente erstreckt
     * @param weightX
     *            Verteilung von zur Verfuegung stehendem Platz in X-Richtung
     * @param weightY
     *            Verteilung von zur Verfuegung stehendem Platz in Y-Richtung
     * @param insets
     *            Abständer der Komponente
     * @param iPadX
     *            Leerraum zwischen einer GridBagZelle und deren Inhalt
     *            (X-Richtung)
     * @param iPadY
     *            Leerraum zwischen einer GridBagZelle und deren Inhalt
     *            (Y-Richtung)
     * @param fill
     *            Verteilung der Komponente innerhalb der zugewiesen Zelle/n
     * @param anchor
     *            Positionierung der Komponente innerhalb der zugewiesen Zelle/n
     */
    public static void addToGridBag(Container cont, Component comp, int x, int y, int width, int height, int weightX, int weightY, Insets insets, int iPadX, int iPadY, int fill, int anchor) {
        GridBagConstraints cons = new GridBagConstraints();
        cons.gridx = x;
        cons.gridy = y;
        cons.gridwidth = width;
        cons.gridheight = height;
        cons.weightx = weightX;
        cons.weighty = weightY;
        cons.fill = fill;
        cons.anchor = anchor;
        if (insets != null) {
            cons.insets = insets;
        }
        cons.ipadx = iPadX;
        cons.ipady = iPadY;
        cont.add(comp, cons);
    }

    private static void log(StringBuilder log, String string) {
        log.append(string);
        System.out.println(string);

    }

    /*
     * public static File getJDHomeDirectoryFromEnvironment() { String envDir =
     * null;// System.getenv("JD_HOME"); File currentDir = null;
     * 
     * String dir =Thread.currentThread().getContextClassLoader().getResource(
     * "jd/update/Main.class") + ""; dir = dir.split("\\.jar\\!")[0] + ".jar";
     * dir = dir.substring(Math.max(dir.indexOf("file:"), 0)); try { currentDir
     * = new File(new URI(dir));
     * 
     * // logger.info(" App dir: "+currentDir+" - //
     * "+System.getProperty("java.class.path")); if (currentDir.isFile()) {
     * currentDir = currentDir.getParentFile(); }
     * 
     * } catch (URISyntaxException e) {
     * 
     * e.printStackTrace(); }
     * 
     * // logger.info("RunDir: " + currentDir);
     * 
     * switch (getRunType()) { case RUNTYPE_LOCAL_JARED: envDir =
     * currentDir.getAbsolutePath(); //
     * logger.info("JD_HOME from current Path :" + envDir); break; case
     * RUNTYPE_LOCAL_ENV: envDir = System.getenv("JD_HOME"); //
     * logger.info("JD_HOME from environment:" + envDir); break; default: envDir
     * = System.getProperty("user.home") + System.getProperty("file.separator")
     * + ".jd_home/"; // logger.info("JD_HOME from user.home :" + envDir);
     * 
     * }
     * 
     * if (envDir == null) { envDir = "." + System.getProperty("file.separator")
     * + ".jd_home/"; System.out.println(envDir); } File jdHomeDir = new
     * File(envDir); if (!jdHomeDir.exists()) { jdHomeDir.mkdirs(); } return
     * jdHomeDir; }
     */
    @SuppressWarnings("unchecked")
    public static void main(String args[]){
        try{
        log = new StringBuilder();
      
        boolean OSFilter = true;
        boolean loadAllPlugins = false;
        boolean clone = false;
        String clonePrefix = null;
        System.out.println("Started"+new Exception().getStackTrace()[0].getLineNumber());
        File cfg;
        if ((cfg = JDUtilities.getResourceFile("jdownloader.config")).exists()) {
            
            JDUtilities.getResourceFile("backup/").mkdirs();
            
            cfg.renameTo(JDUtilities.getResourceFile("backup/jdownloader.config.outdated"));
            
        }
        
        
        for (String p : args) {
            if (p.trim().equalsIgnoreCase("-noosfilter")) {
                OSFilter = false;
            } else if (p.trim().equalsIgnoreCase("-allplugins")) {
                loadAllPlugins = true;
            } else if (p.trim().equalsIgnoreCase("-full")) {
                loadAllPlugins = true;
                OSFilter = false;
            } else if (p.trim().equalsIgnoreCase("-clone")) {
                loadAllPlugins = true;
                OSFilter = false;
                clone = true;
            } else if (clone && clonePrefix == null) {
                clonePrefix = p.trim();
            }
        }
        
        Browser.init();
     
        
        guiConfig = WebUpdater.getConfig("WEBUPDATE");
        
        
        log.append(WebUpdater.getConfig("WEBUPDATE").getProperties() + "\r\n");
        System.out.println(WebUpdater.getConfig("WEBUPDATE").getProperties() + "\r\n");
        System.out.println(WebUpdater.getConfig("PACKAGEMANAGER").getProperties() + "\r\n");
        log.append(WebUpdater.getConfig("PACKAGEMANAGER").getProperties() + "\r\n");
       
        
        initGUI();
        
        
        for (int i = 0; i < args.length; i++) {
            Main.log(log, "Parameter " + i + " " + args[i] + " " + System.getProperty("line.separator"));
            logWindow.setText(log.toString());
        }
        WebUpdater updater = new WebUpdater();
        updater.setOSFilter(OSFilter);
        Main.log(log, "Current Date:" + new Date() + "\r\n");
        checkBackup();
        updater.ignorePlugins(!WebUpdater.getConfig("WEBUPDATE").getBooleanProperty("WEBUPDATE_DISABLE", false));
        if (loadAllPlugins) updater.ignorePlugins(false);
        checkUpdateMessage();
        updater.setLogger(log);

        updater.setDownloadProgress(progressload);
        Main.trace("Start Webupdate");
        Vector<Vector<String>> files;
        try {
            files = updater.getAvailableFiles();
        } catch (Exception e) {
            Main.trace("Update failed");
            Main.log(log, "Update failed");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
            }
            files = new Vector<Vector<String>>();
        }

        if (files != null) {
         
            updater.filterAvailableUpdates(files);

            JDUpdateUtils.backupDataBase();
            updater.updateFiles(files);
        }

        installAddons();
        Main.trace(updater.getLogger().toString());
        Main.trace("End Webupdate");
        logWindow.setText(log.toString());
        Main.trace(JDUtilities.getResourceFile("updateLog.txt").getAbsoluteFile());

        if (JDUtilities.getResourceFile("webcheck.tmp").exists()) {
            JDUtilities.getResourceFile("webcheck.tmp").delete();
        }
        Main.log(log, "Local: " + JDUtilities.getResourceFile(".").getAbsolutePath());

        Main.log(log, "Start java -jar -Xmx512m JDownloader.jar in " + JDUtilities.getResourceFile(".").getAbsolutePath());
        Main.runCommand("java", new String[] { "-Xmx512m", "-jar", "JDownloader.jar", "-rfu" }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0);

        logWindow.setText(log.toString());
        Main.writeLocalFile(JDUtilities.getResourceFile("updateLog.txt"), log.toString());
        System.exit(0);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static void installAddons() {
        SubConfiguration jdus = WebUpdater.getConfig("JDU");
        ArrayList<PackageData> data = (ArrayList<PackageData>) jdus.getProperty("PACKAGEDATA", new ArrayList<PackageData>());

        for (PackageData pa : data) {
            if (!pa.isDownloaded()) continue;
            File zip = new File(pa.getStringProperty("LOCALPATH"));

            Main.log(log, "Install: " + zip + System.getProperty("line.separator") + System.getProperty("line.separator"));

            UnZip u = new UnZip(zip, JDUtilities.getResourceFile("."));
            File[] efiles;
            try {
                efiles = u.extract();
                if (efiles != null) {

                    for (File element : efiles) {
                        Main.log(log, "       extracted: " + element + System.getProperty("line.separator"));
                        if (element.getAbsolutePath().endsWith("readme.html")) {
                            pa.setProperty("README", element.getAbsolutePath());

                        }
                    }
                    pa.setInstalled(true);
                    pa.setUpdating(false);
                    pa.setDownloaded(false);
                    pa.setInstalledVersion(Integer.parseInt(pa.getStringProperty("version")));

                    Main.log(log, "Installation successfull: " + zip + System.getProperty("line.separator"));

                    zip.delete();
                    zip.deleteOnExit();

                }
            } catch (Exception e) {

                e.printStackTrace();

                StackTraceElement[] trace = e.getStackTrace();
                for (int i = 0; i < trace.length; i++)
                    Main.log(log, "\tat " + trace[i] + "\r\n");

                zip.delete();
                zip.deleteOnExit();

                pa.setInstalled(true);
                pa.setUpdating(false);
                pa.setDownloaded(false);

            }

        }
        jdus.save();
        File afile[] = (JDUtilities.getResourceFile("packages")).listFiles();
        if (afile != null) {
            for (int l = 0; l < afile.length; l++) {
                File jdu = afile[l];
                if (jdu.getName().toLowerCase().endsWith("jdu")) {
                    jdu.delete();
                    jdu.deleteOnExit();
                    log(log, (new StringBuilder("delete: ")).append(jdu).toString());
                }
            }
        }

    }

    private static void checkUpdateMessage() throws IOException {
        if (JDUtilities.getResourceFile("updateLog.txt").exists()) {
            String warnHash = JDHash.getMD5(JDUtilities.getResourceFile("updatewarnings.html"));

            Browser.download(JDUtilities.getResourceFile("updatewarnings.html"), "http://service.jdownloader.org/messages/updatewarning.html");
            String hash2 = JDHash.getMD5(JDUtilities.getResourceFile("updatewarnings.html"));
            if (hash2 != null && !hash2.equals(warnHash)) {
                String str;
                if (JOptionPane.showConfirmDialog(frame, str = JDIO.getLocalFile(new File("updatewarnings.html")), "UPDATE WARNINGS", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION) {
                    Main.log(log, "Abort due to warnings " + str);

                    JDUtilities.getResourceFile("updatewarnings.html").delete();
                    JDUtilities.getResourceFile("updatewarnings.html").deleteOnExit();
                    if (JDUtilities.getResourceFile("webcheck.tmp").exists()) {
                        JDUtilities.getResourceFile("webcheck.tmp").delete();
                    }
                    Main.log(log, "Local: " + new File("").getAbsolutePath());
                    Main.log(log, "Start java -jar -Xmx512m JDownloader.jar in " + JDUtilities.getResourceFile(".").getAbsolutePath());

                    Main.runCommand("java", new String[] { "-Xmx512m", "-jar", "JDownloader.jar", "-rfu" }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0);

                    logWindow.setText(log.toString());
                    Main.writeLocalFile(JDUtilities.getResourceFile("updateLog.txt"), log.toString());
                    System.exit(0);
                    return;
                }
            }
        }

    }

    private static void checkBackup() {
        if (JDUtilities.getResourceFile("updateLog.txt").exists()) {
            if (!JDUtilities.getResourceFile("/backup/").exists()) {
                JDUtilities.getResourceFile("/backup/").mkdirs();

                Main.log(log, "Not found: " + (JDUtilities.getResourceFile("/backup/").getAbsolutePath()) + "\r\n");
                JOptionPane.showMessageDialog(frame, "JDownloader could not create a backup. Please make sure that\r\n " + JDUtilities.getResourceFile("/backup/").getAbsolutePath() + " exists and is writable before starting the update");
                Main.runCommand("java", new String[] { "-Xmx512m", "-jar", "JDownloader.jar", "-rfu" }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0);
                System.exit(0);
                return;
            }

            File lastBackup = JDUtilities.getResourceFile("/backup/links.linkbackup");

            Main.log(log, "Backup found. date:" + new Date(lastBackup.lastModified()) + "\r\n");
            if (!lastBackup.exists() || (System.currentTimeMillis() - lastBackup.lastModified()) > 5 * 60 * 1000) {

                JDUtilities.getResourceFile("/backup/").mkdirs();
                String msg = "";

                if (!lastBackup.exists()) {

                    msg = "Do you want to continue without a backup? Your queue may get lost.\r\nLatest backup found: NONE!\r\nNote: You can ignore this message if this is a fresh JD-Installation and your linklist is empty anyway";
                } else {
                    msg = "Do you want to continue without a backup? Your queue may get lost.\r\nLatest backup found: " + new Date(lastBackup.lastModified()) + "\r\nin " + lastBackup.getAbsolutePath();
                }

                if (JOptionPane.showConfirmDialog(frame, msg, "There is no backup of your current Downloadqueue", JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
                    Main.runCommand("java", new String[] { "-Xmx512m", "-jar", "JDownloader.jar", "-rfu" }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0);
                    System.exit(0);
                    return;
                }

            }
        }

    }

    private static void initGUI() {
        UIManager.LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
        String paf = guiConfig.getStringProperty("PLAF", null);
        boolean plafisSet = false;

        if (paf != null) {
            for (LookAndFeelInfo element : info) {
                if (element.getName().equals(paf)) {
                    try {
                        UIManager.setLookAndFeel(element.getClassName());
                        plafisSet = true;
                        break;
                    } catch (UnsupportedLookAndFeelException e) {
                    } catch (ClassNotFoundException e) {
                    } catch (InstantiationException e) {
                    } catch (IllegalAccessException e) {
                    }
                }
            }
        } else {
            for (int i = 0; i < info.length; i++) {
                if (!info[i].getName().matches("(?is).*(metal|motif).*")) {
                    try {
                        UIManager.setLookAndFeel(info[i].getClassName());
                        plafisSet = true;
                        break;
                    } catch (UnsupportedLookAndFeelException e) {
                    } catch (ClassNotFoundException e) {
                    } catch (InstantiationException e) {
                    } catch (IllegalAccessException e) {
                    }
                }
            }
        }
        if (!plafisSet) {
            try {
                UIManager.setLookAndFeel(new WindowsLookAndFeel());
            } catch (UnsupportedLookAndFeelException e) {
            }
        }

        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("JD Update");
        frame.setLayout(new GridBagLayout());

        progressload = new JProgressBar();
        progressload.setMaximum(100);
        progressload.setStringPainted(true);
        logWindow = new JTextArea(30, 120);
        JScrollPane scrollPane = new JScrollPane(logWindow);
        scrollPane.setAutoscrolls(true);
        logWindow.setEditable(false);
        logWindow.setAutoscrolls(true);

        Main.addToGridBag(frame, new JLabel("Webupdate is running..."), REL, REL, REM, 1, 0, 0, INSETS, NORESIZE, NORTHWEST);
       
        Main.addToGridBag(frame, new JLabel("Download: "), REL, REL, REL, 1, 0, 0, INSETS, NORESIZE, NORTHWEST);
        Main.addToGridBag(frame, progressload, REL, REL, REM, 1, 1, 0, INSETS, BOTHRESIZE, NORTHWEST);
        Main.log(log, "Starting...");
        logWindow.setText(log.toString());
        Main.addToGridBag(frame, scrollPane, REL, REL, REM, 1, 1, 1, INSETS, BOTHRESIZE, NORTHWEST);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        int n = 5;
        ((JComponent) frame.getContentPane()).setBorder(new EmptyBorder(n, n, n, n));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        new Thread() {
            public void run() {
                while (true) {
                    logWindow.setText(log.toString());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }.start();

    }

    /**
     * Führt einen Externen befehl aus.
     * 
     * @param command
     * @param parameter
     * @param runIn
     * @param waitForReturn
     * @return null oder die rückgabe des befehls falls waitforreturn == true
     *         ist
     */
    public static String runCommand(String command, String[] parameter, String runIn, int waitForReturn) {

        if (parameter == null) {
            parameter = new String[] {};
        }
        String[] params = new String[parameter.length + 1];
        params[0] = command;
        System.arraycopy(parameter, 0, params, 1, parameter.length);
        Vector<String> tmp = new Vector<String>();
        String par = "";
        for (String element : params) {
            if (element != null && element.trim().length() > 0) {
                par += element + " ";
                tmp.add(element.trim());
            }
        }

        params = tmp.toArray(new String[] {});
        ProcessBuilder pb = new ProcessBuilder(params);

        if (runIn != null && runIn.length() > 0) {
            if (new File(runIn).exists()) {
                pb.directory(new File(runIn));
            } else {
                Main.trace("Working drectory " + runIn + " does not exist!");
            }
        }
        Process process;

        try {
            Main.trace("Start " + par + " in " + runIn + " wait " + waitForReturn);
            process = pb.start();
            if (waitForReturn > 0) {
                long t = System.currentTimeMillis();
                while (true) {
                    try {
                        process.exitValue();
                        break;
                    } catch (Exception e) {
                        if (System.currentTimeMillis() - t > waitForReturn * 1000) {
                            Main.trace(command + ": Prozess ist nach " + waitForReturn + " Sekunden nicht beendet worden. Breche ab.");
                            process.destroy();
                        }
                    }
                }
                Scanner s = new Scanner(process.getInputStream()).useDelimiter("\\Z");
                String ret = "";
                while (s.hasNext()) {
                    ret += s.next();
                }
                return ret;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            Main.trace("Error executing " + command + ": " + e.getLocalizedMessage());
            return null;
        }
    }

    public static void trace(Object arg) {
        try {
            System.out.println(arg.toString());
        } catch (Exception e) {
            System.out.println(arg);
        }
    }

    /**
     * Schreibt content in eine Lokale textdatei
     * 
     * @param file
     * @param content
     * @return true/False je nach Erfolg des Schreibvorgangs
     */
    public static boolean writeLocalFile(File file, String content) {
        try {
            if (file.isFile()) {
                if (!file.delete()) {

                return false; }
            }
            if (file.getParent() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
            BufferedWriter f = new BufferedWriter(new FileWriter(file));
            f.write(content);
            f.close();
            return true;
        } catch (Exception e) {
            // e.printStackTrace();
            return false;
        }
    }

}
