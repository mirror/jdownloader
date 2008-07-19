//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;

public class Main {

    /**
     * @param args
     */
    public static int REL = GridBagConstraints.RELATIVE;
    public static int REM = GridBagConstraints.REMAINDER;
    public static int NORESIZE = GridBagConstraints.NONE;
    public static int HORRESIZE = GridBagConstraints.HORIZONTAL;
    public static int VERRESIZE = GridBagConstraints.VERTICAL;
    public static int BOTHRESIZE = GridBagConstraints.BOTH;
    public static int NORTHWEST = GridBagConstraints.NORTHWEST;
    public static Insets INSETS = new Insets(5, 5, 5, 5);

    public static void main(String args[]) {
        final StringBuffer log = new StringBuffer();

        UIManager.LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
        SubConfiguration guiConfig = SubConfiguration.getSubConfig("simpleGUI");
        String paf = guiConfig.getStringProperty("PLAF", null);
        boolean plafisSet = false;

        /* Http-Proxy einstellen */
        if (SubConfiguration.getSubConfig("DOWNLOAD").getBooleanProperty("USE_PROXY", false)) {
            System.setProperty("http.proxyHost", SubConfiguration.getSubConfig("DOWNLOAD").getStringProperty("PROXY_HOST", ""));
            System.setProperty("http.proxyPort", new Integer(SubConfiguration.getSubConfig("DOWNLOAD").getIntegerProperty("PROXY_PORT", 8080)).toString());
            log(log, "http-proxy: enabled" + System.getProperty("line.separator"));
        } else {
            System.setProperty("http.proxyHost", "");
            log(log, "http-proxy: disabled" + System.getProperty("line.separator"));
        }
        /* Socks-Proxy einstellen */
        if (SubConfiguration.getSubConfig("DOWNLOAD").getBooleanProperty("USE_SOCKS", false)) {
            System.setProperty("socksProxyHost", SubConfiguration.getSubConfig("DOWNLOAD").getStringProperty("SOCKS_HOST", ""));
            System.setProperty("socksProxyPort", new Integer(SubConfiguration.getSubConfig("DOWNLOAD").getIntegerProperty("SOCKS_PORT", 1080)).toString());
            log(log, "socks-proxy: enabled" + System.getProperty("line.separator"));
        } else {
            System.setProperty("socksProxyHost", "");
            log(log, "socks-proxy: disabled" + System.getProperty("line.separator"));
        }

        if (paf != null) {
            for (int i = 0; i < info.length; i++) {
                if (info[i].getName().equals(paf)) {
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
        File file = new File("webupdater.jar");
        if (file.exists()) {
            file.deleteOnExit();
        }
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("JD Update");
        frame.setLayout(new GridBagLayout());
        final JProgressBar progresslist = new JProgressBar();
        progresslist.setMaximum(100);
        progresslist.setStringPainted(true);
        final JProgressBar progressload = new JProgressBar();
        progressload.setMaximum(100);
        progressload.setStringPainted(true);
        final JTextArea logWindow = new JTextArea(30, 120);
        JScrollPane scrollPane = new JScrollPane(logWindow);
        logWindow.setEditable(true);

        addToGridBag(frame, new JLabel("Webupdate is running..."), REL, REL, REM, 1, 0, 0, INSETS, NORESIZE, NORTHWEST);
        addToGridBag(frame, new JLabel("List files: "), REL, REL, REL, 1, 0, 0, INSETS, NORESIZE, NORTHWEST);
        addToGridBag(frame, progresslist, REL, REL, REM, 1, 1, 0, INSETS, BOTHRESIZE, NORTHWEST);
        addToGridBag(frame, new JLabel("Download: "), REL, REL, REL, 1, 0, 0, INSETS, NORESIZE, NORTHWEST);
        addToGridBag(frame, progressload, REL, REL, REM, 1, 1, 0, INSETS, BOTHRESIZE, NORTHWEST);
        log(log, "Starting...");
        logWindow.setText(log.toString());
        addToGridBag(frame, scrollPane, REL, REL, REM, 1, 1, 1, INSETS, BOTHRESIZE, NORTHWEST);

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

        boolean restart = false;
        int runtype = 1;
        for (int i = 0; i < args.length; i++) {

            if (args[i].trim().equalsIgnoreCase("/all")) {
            }
            if (args[i].trim().equalsIgnoreCase("/restart")) restart = true;
            if (args[i].trim().equalsIgnoreCase("/rt0")) runtype = 0;
            if (args[i].trim().equalsIgnoreCase("/rt1")) runtype = 1;
            if (args[i].trim().equalsIgnoreCase("/rt2")) runtype = 2;
            log(log, "Parameter " + i + " " + args[i] + " " + System.getProperty("line.separator"));
            logWindow.setText(log.toString());
        }
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
        WebUpdater updater = new WebUpdater(null);
        updater.setLogger(log);
        updater.setListProgress(progresslist);
        updater.setDownloadProgress(progressload);
        trace("Start Webupdate");

        Vector<Vector<String>> files = updater.getAvailableFiles();

        // boolean success = false;
        if (files != null) {
            // int totalFiles = files.size();

            updater.filterAvailableUpdates(files);
            progresslist.setValue(100);
            updater.updateFiles(files);
        }
        String[] jdus = new File("packages").list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.endsWith(".jdu")) return true;
                return false;

            }

        });
        if(jdus==null)jdus= new String[0];
        log(log, "JD Packages to install: " + jdus.length + System.getProperty("line.separator"));
        ArrayList<File> readmes = new ArrayList<File>();
        ArrayList<File> failed = new ArrayList<File>();
        ArrayList<File> successfull = new ArrayList<File>();
        for (String jdu : jdus) {
            File zip = new File(new File("packages"), jdu);

            log(log, "Install: " + zip + System.getProperty("line.separator") + System.getProperty("line.separator"));

            UnZip u = new UnZip(zip, new File("."));
            File[] efiles;
            try {
                efiles = u.extract();
                if (files != null) {

                    for (int i = 0; i < efiles.length; i++) {
                        log(log, "       extracted: " + efiles[i] + System.getProperty("line.separator"));
                        if (efiles[i].getAbsolutePath().endsWith("readme.html")) {
                            readmes.add(efiles[i].getAbsoluteFile());
                        }
                    }
                    String comment = SubConfiguration.getSubConfig("PACKAGEMANAGER").getStringProperty("COMMENT_" + zip.getAbsolutePath());

                    if (comment != null) {

                        String[] dat = comment.split("_");

                        SubConfiguration.getSubConfig("PACKAGEMANAGER").setProperty("PACKAGE_INSTALLED_VERSION_" + dat[0], dat[1]);
                        SubConfiguration.getSubConfig("PACKAGEMANAGER").save();
                        log(log, "Installation successfull: " + zip + System.getProperty("line.separator"));
                        successfull.add(zip.getAbsoluteFile());

                    } else {
                        log(log, "Installation failed: " + zip + System.getProperty("line.separator"));
                        failed.add(zip.getAbsoluteFile());
                    }
                    zip.delete();
                    zip.deleteOnExit();

                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                zip.delete();
                zip.deleteOnExit();
            }

        }
        SubConfiguration.getSubConfig("PACKAGEMANAGER").setProperty("NEW_INSTALLED", readmes);
        SubConfiguration.getSubConfig("PACKAGEMANAGER").setProperty("NEW_INSTALLED_FAILED", failed);
        SubConfiguration.getSubConfig("PACKAGEMANAGER").setProperty("NEW_INSTALLED_SUCCESSFULL", successfull);
        SubConfiguration.getSubConfig("PACKAGEMANAGER").save();
        trace(updater.getLogger().toString());
        trace("End Webupdate");
        logWindow.setText(log.toString());
        trace(new File("updateLog.txt").getAbsoluteFile());

        if (restart) {
            if (new File("webcheck.tmp").exists()) {
                new File("webcheck.tmp").delete();
            }
            log(log, "Local: " + new File("").getAbsolutePath());
            if (runtype == 2) {
                log(log, "Start java -jar JDownloader.jar in " + new File("").getAbsolutePath());
                // if(SubConfiguration.getSubConfig("OS").getStringProperty(
                // "RESTART_COMMAND", null)!=null
                // &&SubConfiguration.getSubConfig
                // ("OS").getProperty("RESTART_PARAMETER",
                // null)!=null&&SubConfiguration
                // .getSubConfig("OS").getStringProperty("RESTART_DIRECTORY",
                // null)!=null){
                // runCommand(SubConfiguration.getSubConfig("OS").
                // getStringProperty("RESTART_COMMAND", null),
                // (String[])SubConfiguration
                // .getSubConfig("OS").getProperty("RESTART_PARAMETER", null),
                // SubConfiguration
                // .getSubConfig("OS").getStringProperty("RESTART_DIRECTORY",
                // null), 0);
                //                      
                // }else{
                runCommand("java", new String[] { "-jar", "JDownloader.jar" }, new File("").getAbsolutePath(), 0);
                // }
            } else if (runtype == 1 && new File("jd/Main.class").exists()) {
                log(log, "java Main.class in " + new File("jd/").getAbsolutePath());
                runCommand("java", new String[] { "Main.class" }, new File("jd/").getAbsolutePath(), 0);
            } else {
                log(log, "Start jDownloader.jnlp in " + new File("").getAbsolutePath());
                runCommand("javaws", new String[] { "jDownloader.jnlp" }, new File("").getAbsolutePath(), 0);

            }

        }
        logWindow.setText(log.toString());
        writeLocalFile(new File("updateLog.txt"), log.toString());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        System.exit(0);
    }

    private static void log(StringBuffer log, String string) {
        log.append(string);
        System.out.println(string);

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

        if (parameter == null) parameter = new String[] {};
        String[] params = new String[parameter.length + 1];
        params[0] = command;
        System.arraycopy(parameter, 0, params, 1, parameter.length);
        Vector<String> tmp = new Vector<String>();
        String par = "";
        for (int i = 0; i < params.length; i++) {
            if (params[i] != null && params[i].trim().length() > 0) {
                par += params[i] + " ";
                tmp.add(params[i].trim());
            }
        }

        params = tmp.toArray(new String[] {});
        ProcessBuilder pb = new ProcessBuilder(params);

        if (runIn != null && runIn.length() > 0) {
            if (new File(runIn).exists()) {
                pb.directory(new File(runIn));
            } else {
                trace("Working drectory " + runIn + " does not exist!");
            }
        }
        Process process;

        try {
            trace("Start " + par + " in " + runIn + " wait " + waitForReturn);
            process = pb.start();
            if (waitForReturn > 0) {
                long t = System.currentTimeMillis();
                while (true) {
                    try {
                        process.exitValue();
                        break;
                    } catch (Exception e) {
                        if (System.currentTimeMillis() - t > waitForReturn * 1000) {
                            trace(command + ": Prozess ist nach " + waitForReturn + " Sekunden nicht beendet worden. Breche ab.");
                            process.destroy();
                        }
                    }
                }
                Scanner s = new Scanner(process.getInputStream()).useDelimiter("\\Z");
                String ret = "";
                while (s.hasNext())
                    ret += s.next();
                return ret;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            trace("Error executing " + command + ": " + e.getLocalizedMessage());
            return null;
        }
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
        if (insets != null) cons.insets = insets;
        cons.ipadx = iPadX;
        cons.ipady = iPadY;
        cont.add(comp, cons);
    }

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
        if (cont == null) {

        return; }
        if (comp == null) {

        return; }
        addToGridBag(cont, comp, x, y, width, height, weightX, weightY, insets, 0, 0, fill, anchor);
    }

}
