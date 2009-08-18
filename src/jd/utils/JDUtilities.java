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

package jd.utils;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jd.CPluginWrapper;
import jd.HostPluginWrapper;
import jd.JDClassLoader;
import jd.OptionalPluginWrapper;
import jd.config.Configuration;
import jd.config.DatabaseConnector;
import jd.controlling.DownloadController;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.nutils.Executer;
import jd.nutils.Formatter;
import jd.nutils.OSDetector;
import jd.nutils.io.JDIO;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;
import jd.update.WebUpdater;
import jd.utils.locale.JDL;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * @author astaldo/JD-Team
 */
public class JDUtilities {

    /**
     * Die Konfiguration
     */
    public static Configuration CONFIGURATION = null;

    private static DatabaseConnector DB_CONNECT = null;

    private static HashMap<String, PluginsC> CONTAINER_PLUGINS = new HashMap<String, PluginsC>();

    /**
     * Der DownloadController
     */
    private static JDController CONTROLLER = null;

    /**
     * Versionsstring der Applikation
     */
    public static final String JD_TITLE = "jDownloader";

    /**
     * Titel der Applikation
     */
    public static final String JD_VERSION = "0.";

    /**
     * Ein URLClassLoader, um Dateien aus dem HomeVerzeichnis zu holen
     */
    private static JDClassLoader JD_CLASSLOADER = null;

    public static final int RUNTYPE_LOCAL = 1;

    public static final int RUNTYPE_LOCAL_JARED = 2;

    private static File JD_HOME = null;

    /**
     * nur 1 UserIO Dialog gleichzeitig (z.b. PW, Captcha)
     */
    public static final Object USERIO_LOCK = new Object();

    private static String REVISION;

    private static String[] JD_ARGUMENTS = new String[1];

    public static <K extends Comparable<K>, V> TreeMap<K, V> revSortByKey(Map<K, V> map) {
        TreeMap<K, V> a = new TreeMap<K, V>(new Comparator<K>() {

            public int compare(K o1, K o2) {
                return o2.compareTo(o1);
            }

        });
        a.putAll(map);

        return a;
    }

    public static <K extends Comparable<K>, V> TreeMap<K, V> sortByKey(Map<K, V> map) {
        TreeMap<K, V> a = new TreeMap<K, V>(new Comparator<K>() {

            public int compare(K o1, K o2) {
                return o1.compareTo(o2);
            }

        });
        a.putAll(map);

        return a;
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
            JDLogger.getLogger().severe("Container ==null");
            return;
        }
        if (comp == null) {
            JDLogger.getLogger().severe("Componente ==null");
            return;
        }
        JDUtilities.addToGridBag(cont, comp, x, y, width, height, weightX, weightY, insets, 0, 0, fill, anchor);
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

    public static String convertExceptionReadable(Exception e) {
        String s = e.getClass().getName().replaceAll("Exception", "");
        s = s.substring(s.lastIndexOf(".") + 1);
        StringBuilder ret = new StringBuilder();
        String letter = null;
        for (int i = 0; i < s.length(); i++) {
            if ((letter = s.substring(i, i + 1)).equals(letter.toUpperCase())) {
                ret.append(' ');
                ret.append(letter);
            } else {
                ret.append(letter);
            }
        }
        String message = e.getLocalizedMessage();
        String rets = ret.toString();
        return message != null ? rets.trim() + ": " + message : rets.trim();

    }

    public static String createContainerString(ArrayList<DownloadLink> downloadLinks, String encryption) {
        ArrayList<CPluginWrapper> pfc = CPluginWrapper.getCWrapper();
        for (int i = 0; i < pfc.size(); i++) {
            String pn = pfc.get(i).getHost();
            if (pn.equalsIgnoreCase(encryption)) return pfc.get(i).getPlugin().createContainerString(downloadLinks);
        }
        return null;
    }

    /**
     * verschlüsselt string mit der übergebenen encryption (Containerpluginname
     * 
     * @param string
     * @param encryption
     * @return ciphertext
     */
    public static String[] encrypt(String string, String encryption) {
        ArrayList<CPluginWrapper> pfc = CPluginWrapper.getCWrapper();
        for (int i = 0; i < pfc.size(); i++) {
            if (pfc.get(i).getHost().equalsIgnoreCase(encryption)) { return pfc.get(i).getPlugin().encrypt(string); }
        }
        return null;

    }

    public static String getUserInput(String message, DownloadLink link) throws InterruptedException {
        return getUserInput(message, null, link);
    }

    public static String getUserInput(String message, String defaultmessage, DownloadLink link) throws InterruptedException {
        try {
            link.getLinkStatus().addStatus(LinkStatus.WAITING_USERIO);
            link.requestGuiUpdate();
            String code = getUserInput(message, defaultmessage);

            link.requestGuiUpdate();
            return code;
        } finally {
            link.getLinkStatus().removeStatus(LinkStatus.WAITING_USERIO);
        }
    }

    public static String getUserInput(String message, CryptedLink link) throws InterruptedException {
        return getUserInput(message, null, link);
    }

    public static String getUserInput(String message, String defaultmessage, CryptedLink link) throws InterruptedException {
        link.getProgressController().setStatusText(JDL.L("gui.linkgrabber.waitinguserio", "Waiting for user input"));
        String password = getUserInput(message, defaultmessage);
        link.getProgressController().setStatusText(null);
        return password;
    }

    public static String getUserInput(String message, String defaultmessage) throws InterruptedException {
        synchronized (USERIO_LOCK) {
            if (message == null) message = JDL.L("gui.linkgrabber.password", "Password?");
            if (defaultmessage == null) defaultmessage = "";
            String password = UserIO.getInstance().requestInputDialog(0, message, defaultmessage);
            return password;
        }
    }

    /**
     * @return Configuration instanz
     */
    public static Configuration getConfiguration() {
        if (CONFIGURATION == null) CONFIGURATION = new Configuration();
        return CONFIGURATION;
    }

    /**
     * Gibt den verwendeten Controller zurück
     * 
     * @return gerade verwendete CONTROLLER-instanz
     */
    public static JDController getController() {
        return CONTROLLER;
    }

    public static long getCRC(File file) {

        try {

            CheckedInputStream cis = null;
            // long fileSize = 0;
            try {
                // Computer CRC32 checksum
                cis = new CheckedInputStream(new FileInputStream(file), new CRC32());

                // fileSize = file.length();

            } catch (FileNotFoundException e) {
                JDLogger.exception(e);
                return 0;
            }

            byte[] buf = new byte[128];
            while (cis.read(buf) >= 0) {
            }

            long checksum = cis.getChecksum().getValue();
            return checksum;

        } catch (IOException e) {
            JDLogger.exception(e);
            return 0;
        }

    }

    /**
     * Gibt das aktuelle Working Directory zurück. Beim FileBrowser etc wird das
     * gebraucht.
     * 
     * @return
     */
    public static File getCurrentWorkingDirectory(String id) {
        if (id == null) id = "";

        String dlDir = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, null);
        String lastDir = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_CURRENT_BROWSE_PATH + id, null);

        File dlDirectory;
        if (dlDir == null) {
            dlDirectory = new File("");
        } else {
            dlDirectory = new File(dlDir);
        }

        if (lastDir == null) return dlDirectory;
        return new File(lastDir);

    }

    /**
     * Diese Funktion gibt den Pfad zum JAC-Methodenverzeichniss zurück
     * 
     * @author JD-Team
     * @return gibt den Pfad zu den JAC Methoden zurück
     */
    public static String getJACMethodsDirectory() {
        return "jd/captcha/methods/";
    }

    /**
     * @return Gibt die verwendete java Version als Double Value zurück. z.B.
     *         1.603
     */
    public static Double getJavaVersion() {
        String version = System.getProperty("java.version");
        int majorVersion = Formatter.filterInt(version.substring(0, version.indexOf(".")));
        int subversion = Formatter.filterInt(version.substring(version.indexOf(".") + 1));
        return Double.parseDouble(majorVersion + "." + subversion);
    }

    /**
     * Liefert einen URLClassLoader zurück, um Dateien aus dem Stammverzeichnis
     * zu laden
     * 
     * @return URLClassLoader
     */

    public static JDClassLoader getJDClassLoader() {
        if (JD_CLASSLOADER == null) {
            File homeDir = JDUtilities.getJDHomeDirectoryFromEnvironment();
            // String url = null;
            // Url Encode des pfads für den Classloader
            JDLogger.getLogger().finest("Create Classloader: for: " + homeDir.getAbsolutePath());
            JD_CLASSLOADER = new JDClassLoader(homeDir.getAbsolutePath(), Thread.currentThread().getContextClassLoader());

        }
        return JD_CLASSLOADER;
    }

    /**
     * Liefert das Basisverzeichnis für jD zurück.
     * 
     * @return ein File, dass das Basisverzeichnis angibt
     */
    public static File getJDHomeDirectoryFromEnvironment() {
        if (JD_HOME != null) return JD_HOME;
        String envDir = null;// System.getenv("JD_HOME");
        File currentDir = null;

        URL ressource = Thread.currentThread().getContextClassLoader().getResource("jd/Main.class");
        // System.out.println("Ressource: " + ressource);
        if (ressource == null) {
            ressource = Thread.currentThread().getContextClassLoader().getResource("jd/update/Main.class");

        }
        String dir = ressource + "";
        // System.out.println(dir);
        dir = dir.split("\\.jar\\!")[0] + ".jar";
        // System.out.println(dir);
        dir = dir.substring(Math.max(dir.indexOf("file:"), 0));
        try {
            // System.out.println(dir);
            currentDir = new File(new URI(dir));
            // System.out.println(currentDir);
            // JDUtilities.getLogger().info(" App dir: "+currentDir+" -
            // "+System.getProperty("java.class.path"));
            if (currentDir.isFile()) {
                currentDir = currentDir.getParentFile();
            }
        } catch (URISyntaxException e) {
            JDLogger.exception(e);
        }

        // JDUtilities.getLogger().info("RunDir: " + currentDir);

        switch (JDUtilities.getRunType()) {
        case RUNTYPE_LOCAL_JARED:
            System.out.println("JAR");
            envDir = currentDir.getAbsolutePath();
            break;

        default:
            System.out.println("USER");
            envDir = System.getProperty("user.home") + System.getProperty("file.separator") + ".jd_home/";

        }
        // System.out.println("ENV " + envDir);
        if (envDir == null) {
            envDir = "." + System.getProperty("file.separator") + ".jd_home/";
            JDLogger.getLogger().info("JD_HOME from current directory:" + envDir);
        }
        // System.out.println("ENV " + envDir);
        File jdHomeDir = new File(envDir);
        if (!jdHomeDir.exists()) {
            jdHomeDir.mkdirs();
        }
        JD_HOME = jdHomeDir;
        return jdHomeDir;
    }

    public static String getJDTitle() {
        StringBuilder ret = new StringBuilder(JDUtilities.JD_TITLE);

        if (WebUpdater.getConfig("WEBUPDATE").getStringProperty("BRANCHINUSE", null) != null) {
            ret.append(' ');
            ret.append('(');
            ret.append(WebUpdater.getConfig("WEBUPDATE").getStringProperty("BRANCHINUSE", null));
            ret.append(')');
        }
        ret.append(' ');
        ret.append(JDUtilities.JD_VERSION);
        ret.append(JDUtilities.getRevision());
        if (JDUtilities.getController() != null && JDUtilities.getController().getWaitingUpdates() != null && JDUtilities.getController().getWaitingUpdates().size() > 0) {
            ret.append(' ');
            ret.append(JDL.L("gui.mainframe.title.updatemessage", "-->UPDATES VERFÜGBAR:"));
            ret.append(' ');
            ret.append(JDUtilities.getController().getWaitingUpdates().size());
        }
        return ret.toString();
    }

    public static String getPercent(long downloadCurrent, long downloadMax) {
        DecimalFormat c = new DecimalFormat("0.00");
        return c.format(100.0 * downloadCurrent / downloadMax) + "%";
    }

    /**
     * Sucht ein passendes Plugin für ein Containerfile
     * 
     * @param container
     *            Der Host, von dem das Plugin runterladen kann
     * @param containerPath
     * @return Ein passendes Plugin oder null
     */
    public static PluginsC getPluginForContainer(String container, String containerPath) {
        if (containerPath != null && CONTAINER_PLUGINS.containsKey(containerPath)) { return CONTAINER_PLUGINS.get(containerPath); }
        PluginsC ret = null;
        for (CPluginWrapper act : CPluginWrapper.getCWrapper()) {
            if (act.getHost().equalsIgnoreCase(container)) {

                ret = (PluginsC) act.getNewPluginInstance();
                if (containerPath != null) {
                    CONTAINER_PLUGINS.put(containerPath, ret);
                }
                return ret;

            }
        }
        return null;
    }

    /**
     * Sucht ein passendes Plugin für einen Anbieter Please dont use the
     * returned Plugin to start any function
     * 
     * @param host
     *            Der Host, von dem das Plugin runterladen kann
     * @return Ein passendes Plugin oder null
     */
    public static PluginForHost getPluginForHost(String host) {
        for (HostPluginWrapper pHost : JDUtilities.getPluginsForHost()) {
            if (pHost.getHost().equals(host.toLowerCase())) return pHost.getPlugin();
        }
        return null;
    }

    public static PluginForHost getNewPluginForHostInstance(String host) {
        for (HostPluginWrapper pHost : JDUtilities.getPluginsForHost()) {
            if (pHost.getHost().equals(host.toLowerCase())) return (PluginForHost) pHost.getNewPluginInstance();
        }
        return null;
    }

    public static OptionalPluginWrapper getOptionalPlugin(String id) {
        for (OptionalPluginWrapper wrapper : OptionalPluginWrapper.getOptionalWrapper()) {
            if (wrapper.getID() != null && wrapper.getID().equalsIgnoreCase(id)) return wrapper;
        }
        return null;
    }

    /**
     * Liefert alle Plugins zum Downloaden von einem Anbieter zurück.
     * 
     * @return Plugins zum Downloaden von einem Anbieter
     */
    public static ArrayList<HostPluginWrapper> getPluginsForHost() {
        return new ArrayList<HostPluginWrapper>(HostPluginWrapper.getHostWrapper());
    }

    public static ArrayList<HostPluginWrapper> getPremiumPluginsForHost() {
        ArrayList<HostPluginWrapper> plugins = getPluginsForHost();
        for (int i = plugins.size() - 1; i >= 0; --i) {
            if (!plugins.get(i).isPremiumEnabled()) plugins.remove(i);
        }
        return plugins;
    }

    /**
     * Parsed den Revision-String ins Format 0.000
     * 
     * @return RevisionID
     */
    public static String getRevision() {
        if (REVISION != null) return REVISION;
        int rev = -1;
        try {
            rev = Formatter.filterInt(JDIO.getLocalFile(JDUtilities.getResourceFile("config/version.cfg")));
        } catch (Throwable t) {
            t.printStackTrace();
        }

        int rev2 = Integer.parseInt(Formatter.getRevision("$Revision$"));

        double r = Math.max(rev2, rev) / 1000.0;
        return REVISION = new DecimalFormat("0.000").format(r).replace(",", ".");
    }

    public static int getRunType() {
        String caller = (Thread.currentThread().getContextClassLoader().getResource("jd") + "");

        if (caller.matches("jar\\:.*\\.jar\\!.*")) {
            return RUNTYPE_LOCAL_JARED;
        } else {
            return RUNTYPE_LOCAL;
        }

    }

    public static void setJDargs(String[] args) {
        JD_ARGUMENTS = args;
    }

    public static void restartJDandWait() {
        restartJD();
        while (true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }
    }

    public static void restartJD() {
        new Thread(new Runnable() {
            public void run() {
                if (JDUtilities.getController() != null) JDUtilities.getController().prepareShutdown();

                List<String> lst = ManagementFactory.getRuntimeMXBean().getInputArguments();
                ArrayList<String> jargs = new ArrayList<String>();

                boolean xmxset = false;
                boolean xmsset = false;
                boolean useconc = false;
                boolean minheap = false;
                boolean maxheap = false;
                System.out.println("RESTART NOW");
                for (String h : lst) {
                    if (h.contains("Xmx")) {
                        xmxset = true;
                        if (Runtime.getRuntime().maxMemory() < 533000000) {
                            jargs.add("-Xmx512m");
                            continue;
                        }
                    } else if (h.contains("xms")) {
                        xmsset = true;
                    } else if (h.contains("XX:+useconc")) {
                        useconc = true;
                    } else if (h.contains("minheapfree")) {
                        minheap = true;
                    } else if (h.contains("maxheapfree")) {
                        maxheap = true;
                    }
                    jargs.add(h);
                }
                if (!xmxset) jargs.add("-Xmx512m");
                if (OSDetector.isLinux()) {
                    if (!xmsset) jargs.add("-Xms64m");
                    if (!useconc) jargs.add("-XX:+UseConcMarkSweepGC");
                    if (!minheap) jargs.add("-XX:MinHeapFreeRatio=0");
                    if (!maxheap) jargs.add("-XX:MaxHeapFreeRatio=0");
                }
                jargs.add("-jar");
                jargs.add("JDownloader.jar");

                String[] javaArgs = jargs.toArray(new String[jargs.size()]);
                String[] finalArgs = new String[JD_ARGUMENTS.length + javaArgs.length];
                System.arraycopy(javaArgs, 0, finalArgs, 0, javaArgs.length);
                System.arraycopy(JD_ARGUMENTS, 0, finalArgs, javaArgs.length, JD_ARGUMENTS.length);

                ArrayList<File> restartfiles = JDIO.listFiles(JDUtilities.getResourceFile("update"));
                if (restartfiles != null && restartfiles.size() > 0) {
                    if (OSDetector.isMac()) {
                        JDLogger.getLogger().info(JDUtilities.runCommand("java", new String[] { "-jar", "tools/tinyupdate.jar", "-restart" }, getResourceFile(".").getAbsolutePath(), 0));
                    } else {
                        JDLogger.getLogger().info(JDUtilities.runCommand("java", new String[] { "-jar", "tools/tinyupdate.jar", "-restart" }, getResourceFile(".").getAbsolutePath(), 0));
                    }

                } else {

                    if (OSDetector.isMac()) {
                        JDLogger.getLogger().info(JDUtilities.runCommand("open", new String[] { "-n", "jDownloader.app" }, JDUtilities.getResourceFile(".").getParentFile().getParentFile().getParentFile().getParentFile().getAbsolutePath(), 0));
                    } else {
                        JDLogger.getLogger().info(JDUtilities.runCommand("java", finalArgs, getResourceFile(".").getAbsolutePath(), 0));
                    }
                }
                System.out.println("EXIT NOW");
                System.exit(0);
            }
        }).start();
    }

    public static URL getResourceURL(String resource) {
        JDClassLoader cl = JDUtilities.getJDClassLoader();
        if (cl == null) {
            System.err.println("Classloader == null");
            return null;
        }
        return cl.getResource(resource);
    }

    /**
     * Gibt ein FileOebject zu einem Resourcstring zurück
     * 
     * @author JD-Team
     * @param resource
     *            Ressource, die geladen werden soll
     * @return File zu arg
     */
    public static File getResourceFile(String resource) {
        URL clURL = getResourceURL(resource);
        if (clURL != null) {
            try {
                return new File(clURL.toURI());
            } catch (URISyntaxException e) {
            }
        }
        return null;
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
        Executer exec = new Executer(command);
        exec.addParameters(parameter);
        exec.setRunin(runIn);
        exec.setWaitTimeout(waitForReturn);
        exec.start();
        exec.waitTimeout();
        return exec.getOutputStream() + " \r\n " + exec.getErrorStream();
    }

    public static void saveConfig() {
        JDUtilities.getConfiguration().save();
    }

    public static String objectToXml(Object obj) throws IOException {

        ByteArrayOutputStream ba;
        DataOutputStream out = new DataOutputStream(ba = new ByteArrayOutputStream());
        XMLEncoder xmlEncoder = new XMLEncoder(out);
        xmlEncoder.writeObject(obj);
        xmlEncoder.close();

        out.close();
        return new String(ba.toByteArray());
    }

    public static Object xmlStringToObjekt(String in) throws IOException {
        Object objectLoaded = null;
        ByteArrayInputStream ba = new ByteArrayInputStream(in.getBytes());
        XMLDecoder xmlDecoder = new XMLDecoder(ba);
        objectLoaded = xmlDecoder.readObject();
        xmlDecoder.close();
        ba.close();
        return objectLoaded;
    }

    /**
     * Setzt die Konfigurations instanz
     * 
     * @param CONFIGURATION
     */
    public static void setConfiguration(Configuration configuration) {
        JDUtilities.CONFIGURATION = configuration;
    }

    /**
     * Setzt den Controller
     * 
     * @param con
     *            CONTROLLER
     */
    public static void setController(JDController con) {
        CONTROLLER = con;
    }

    public static DownloadController getDownloadController() {
        return DownloadController.getInstance();
    }

    /**
     * Setzt das aktuelle woringdirectory für den filebrowser
     * 
     * @param f
     * @param id
     */
    public static void setCurrentWorkingDirectory(File f, String id) {
        if (id == null) id = "";
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_CURRENT_BROWSE_PATH + id, f.getAbsolutePath());
        JDUtilities.getConfiguration().save();
    }

    public static boolean sleep(int i) {
        try {
            Thread.sleep(i);
            return true;
        } catch (InterruptedException e) {
            return false;
        }

    }

    public static String removeEndingPoints(String name) {
        if (name == null) { return null; }
        String ret = name.trim();
        while (true) {
            if (ret.endsWith(".")) {
                ret = ret.substring(0, ret.length() - 1);
            } else {
                break;
            }
        }
        return ret;
    }

    public synchronized static DatabaseConnector getDatabaseConnector() {

        if (DB_CONNECT == null) {

            try {
                DB_CONNECT = new DatabaseConnector();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                JDLogger.exception(e);
                String configpath = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/config/";
                Logger logger = JDLogger.getLogger();
                if (e.getMessage().equals("Database broken!")) {
                    logger.severe("Database broken! Creating fresh Database");

                    if (!new File(configpath + "database.script").delete() || !new File(configpath + "database.properties").delete()) {
                        logger.severe("Could not delete broken Database");
                        UserIO.getInstance().requestMessageDialog("Could not delete broken database. Please remove the JD_HOME/config directory and restart JD");

                    }
                }

                try {
                    DB_CONNECT = new DatabaseConnector();
                } catch (Exception e1) {
                    JDLogger.exception(e1);
                    UserIO.getInstance().requestMessageDialog("Could not create database. Please remove the JD_HOME/config directory and restart JD");

                    System.exit(1);
                }
            }

        }
        return DB_CONNECT;

    }

    public static boolean openExplorer(File path) {
        try {
            return new GetExplorer().openExplorer(path);
        } catch (Exception e) {
            JDLogger.exception(e);
            return false;
        }
    }

    public static String[] passwordStringToArray(String password) {
        if (password == null || password.matches("[\\s]*")) { return new String[] {}; }
        if (password.matches("[\\s]*\\{[\\s]*\".*\"[\\s]*\\}[\\s]*$")) {
            password = password.replaceFirst("[\\s]*\\{[\\s]*\"", "").replaceFirst("\"[\\s]*\\}[\\s]*$", "");
            return password.split("\"[\\s]*\\,[\\s]*\"");
        }
        return new String[] { password };
    }

    public static ArrayList<String> mergePasswords(ArrayList<String> passwords, String newpassword) {
        String[] newpws = passwordStringToArray(newpassword);
        if (newpws.length == 0) return passwords;
        if (passwords == null) passwords = new ArrayList<String>();
        for (String newpw : newpws) {
            if (!passwords.contains(newpw)) passwords.add(newpw);
        }
        return passwords;
    }

    public static String passwordArrayToString(String[] passwords) {
        LinkedList<String> pws = new LinkedList<String>();
        for (int i = 0; i < passwords.length; i++) {
            if (!passwords[i].matches("[\\s]*") && !pws.contains(passwords[i])) {
                pws.add(passwords[i]);
            }
        }
        passwords = pws.toArray(new String[pws.size()]);
        if (passwords.length == 0) { return ""; }
        if (passwords.length == 1) { return passwords[0]; }

        int l = passwords.length - 1;
        StringBuilder ret = new StringBuilder();
        ret.append(new char[] { '{', '"' });
        for (int i = 0; i < passwords.length; i++) {
            if (!passwords[i].matches("[\\s]*")) {
                ret.append(passwords[i]);
                if (i == l)
                    ret.append(new char[] { '"', '}' });
                else
                    ret.append(new char[] { '"', ',', '"' });
            }
        }
        return ret.toString();

    }

    public static Document parseXmlString(String xmlString, boolean validating) {
        if (xmlString == null) return null;
        try {
            // Create a builder factory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(validating);

            InputSource inSource = new InputSource(new StringReader(xmlString));

            // Create the builder and parse the file
            Document doc = factory.newDocumentBuilder().parse(inSource);

            return doc;
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        return null;
    }

    public static String createXmlString(Document doc) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            // initialize StreamResult with File object to save to file
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(doc);

            transformer.transform(source, result);

            return result.getWriter().toString();

        } catch (TransformerException e) {
            JDLogger.exception(e);
        }
        return null;
    }

    /**
     * Gibt das Attribut zu key in childNode zurück
     * 
     * @param childNode
     * @param key
     * @return String Atribut
     */
    public static String getAttribute(Node childNode, String key) {
        NamedNodeMap att = childNode.getAttributes();
        if (att == null || att.getNamedItem(key) == null) { return null; }
        return att.getNamedItem(key).getNodeValue();
    }

}
