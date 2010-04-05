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
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import jd.DecryptPluginWrapper;
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
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;
import jd.utils.locale.JDL;

import org.appwork.utils.Application;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * @author astaldo/JD-Team
 */
public class JDUtilities {

    private static final Logger LOGGER = JDLogger.getLogger();

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

    private static int REVISIONINT = -1;

    public static <K extends Comparable<K>, V> TreeMap<K, V> revSortByKey(final Map<K, V> map) {
        final TreeMap<K, V> a = new TreeMap<K, V>(new Comparator<K>() {

            public int compare(final K o1, final K o2) {
                return o2.compareTo(o1);
            }

        });
        a.putAll(map);
        return a;
    }

    public static <K extends Comparable<K>, V> TreeMap<K, V> sortByKey(final Map<K, V> map) {
        final TreeMap<K, V> a = new TreeMap<K, V>(new Comparator<K>() {

            public int compare(final K o1, final K o2) {
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
    public static void addToGridBag(final Container cont, final Component comp, final int x, final int y, final int width, final int height, final int weightX, final int weightY, final Insets insets, final int fill, final int anchor) {
        if (cont == null) {
            LOGGER.severe("Container ==null");
        } else if (comp == null) {
            LOGGER.severe("Componente ==null");
        } else {
            JDUtilities.addToGridBag(cont, comp, x, y, width, height, weightX, weightY, insets, 0, 0, fill, anchor);
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
    public static void addToGridBag(final Container cont, final Component comp, final int x, final int y, final int width, final int height, final int weightX, final int weightY, final Insets insets, final int iPadX, final int iPadY, final int fill, final int anchor) {
        final GridBagConstraints cons = new GridBagConstraints();
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

    public static String convertExceptionReadable(final Exception e) {
        String s = e.getClass().getName().replaceAll("Exception", "");
        s = s.substring(s.lastIndexOf('.') + 1);
        final StringBuilder ret = new StringBuilder();
        String letter = null;
        final int sLength = s.length();
        for (int i = 0; i < sLength; i++) {
            if ((letter = s.substring(i, i + 1)).equals(letter.toUpperCase())) {
                ret.append(' ');
                ret.append(letter);
            } else {
                ret.append(letter);
            }
        }
        final String message = e.getLocalizedMessage();
        final String rets = ret.toString().trim();
        return message != null ? rets + ": " + message : rets;
    }

    public static String createContainerString(final ArrayList<DownloadLink> downloadLinks, final String encryption) {
        final ArrayList<CPluginWrapper> pfc = CPluginWrapper.getCWrapper();
        final int size = pfc.size();
        for (int i = 0; i < size; i++) {
            final String pn = pfc.get(i).getHost();
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
    public static String[] encrypt(final String string, final String encryption) {
        final ArrayList<CPluginWrapper> pfc = CPluginWrapper.getCWrapper();
        final int size = pfc.size();
        for (int i = 0; i < size; i++) {
            if (pfc.get(i).getHost().equalsIgnoreCase(encryption)) { return pfc.get(i).getPlugin().encrypt(string); }
        }
        return null;
    }

    public static String getUserInput(final String message, final DownloadLink link) {
        return getUserInput(message, null, link);
    }

    public static String getUserInput(final String message, final String defaultmessage, final DownloadLink link) {
        try {
            link.getLinkStatus().addStatus(LinkStatus.WAITING_USERIO);
            link.requestGuiUpdate();
            final String code = getUserInput(message, defaultmessage);

            link.requestGuiUpdate();
            return code;
        } finally {
            link.getLinkStatus().removeStatus(LinkStatus.WAITING_USERIO);
        }
    }

    public static String getUserInput(final String message, final CryptedLink link) {
        return getUserInput(message, null, link);
    }

    public static String getUserInput(final String message, final String defaultmessage, final CryptedLink link) {
        link.getProgressController().setStatusText(JDL.L("gui.linkgrabber.waitinguserio", "Waiting for user input"));
        final String password = getUserInput(message, defaultmessage);
        link.getProgressController().setStatusText(null);
        return password;
    }

    public static String getUserInput(final String message, final String defaultmessage) {
        synchronized (USERIO_LOCK) {
            return UserIO.getInstance().requestInputDialog(0, message == null ? JDL.L("gui.linkgrabber.password", "Password?") : message, defaultmessage == null ? "" : defaultmessage);
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

    public static long getCRC(final File file) {
        try {
            // Computer CRC32 checksum
            final CheckedInputStream cis = new CheckedInputStream(new FileInputStream(file), new CRC32());
            // fileSize = file.length();

            final byte[] buf = new byte[128];
            while (cis.read(buf) >= 0) {
            }

            return cis.getChecksum().getValue();
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
    public static File getCurrentWorkingDirectory(final String id) {
        final String dlDir = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, null);
        final String lastDir = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_CURRENT_BROWSE_PATH + (id == null ? "" : id), null);
        if (lastDir == null) {
            return (dlDir == null) ? new File("") : new File(dlDir);
        } else {
            return new File(lastDir);
        }
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
        final String version = System.getProperty("java.version");
        final int dotIndex = version.indexOf('.');
        final int majorVersion = Formatter.filterInt(version.substring(0, dotIndex));
        final int subversion = Formatter.filterInt(version.substring(dotIndex + 1));
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
            final File homeDir = JDUtilities.getJDHomeDirectoryFromEnvironment();
            // String url = null;
            // Url Encode des pfads für den Classloader
            LOGGER.finest("Create Classloader: for: " + homeDir.getAbsolutePath());
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
        if (JD_HOME != null) {
            return JD_HOME;
        } else {
            Application.setApplication(".jd_home");
            URL ressource = Thread.currentThread().getContextClassLoader().getResource("jd/Main.class");
            /* we have 2 different Main classes */
            if (ressource != null) {
                JD_HOME = new File(Application.getRoot(jd.Main.class));
            } else {
                JD_HOME = new File(Application.getRoot(jd.update.Main.class));
            }
            if (!JD_HOME.exists()) {
                JD_HOME.mkdirs();
            }
            LOGGER.info("JD_HOME:" + JD_HOME);
            return JD_HOME;
        }
    }

    public static String getJDTitle() {
        final StringBuilder ret = new StringBuilder("JDownloader");

        final int i = WebUpdate.getWaitingUpdates();
        if (i > 0) {
            ret.append(new char[] { ' ', '(' });
            ret.append(JDL.LF("gui.mainframe.title.updatemessage2", "%s Updates available", i));
            ret.append(')');
        }

        return ret.toString();
    }

    public static String getPercent(final long downloadCurrent, final long downloadMax) {
        return (new DecimalFormat("0.00")).format(100.0 * downloadCurrent / downloadMax) + "%";
    }

    /**
     * Sucht ein passendes Plugin für ein Containerfile
     * 
     * @param container
     *            Der Host, von dem das Plugin runterladen kann
     * @param containerPath
     * @return Ein passendes Plugin oder null
     */
    public static PluginsC getPluginForContainer(final String container, final String containerPath) {
        if (containerPath != null && CONTAINER_PLUGINS.containsKey(containerPath)) { return CONTAINER_PLUGINS.get(containerPath); }
        PluginsC ret = null;
        for (final CPluginWrapper act : CPluginWrapper.getCWrapper()) {
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
    public static PluginForDecrypt getPluginForDecrypt(final String host) {
        for (final DecryptPluginWrapper pHost : DecryptPluginWrapper.getDecryptWrapper()) {
            if (pHost.getHost().equals(host.toLowerCase(Locale.getDefault()))) return pHost.getPlugin();
        }
        return null;
    }

    public static PluginForHost getPluginForHost(final String host) {
        try {
            HostPluginWrapper.readLock.lock();
            for (final HostPluginWrapper pHost : HostPluginWrapper.getHostWrapper()) {
                if (pHost.getHost().equals(host.toLowerCase(Locale.getDefault()))) return pHost.getPlugin();
            }
        } finally {
            HostPluginWrapper.readLock.unlock();
        }
        return null;
    }

    public static PluginForHost getNewPluginForHostInstance(final String host) {
        PluginForHost plugin = getPluginForHost(host);
        if (plugin != null) return (PluginForHost) plugin.getWrapper().getNewPluginInstance();
        return null;
    }

    public static OptionalPluginWrapper getOptionalPlugin(final String id) {
        for (final OptionalPluginWrapper wrapper : OptionalPluginWrapper.getOptionalWrapper()) {
            if (wrapper.getID() != null && wrapper.getID().equalsIgnoreCase(id)) return wrapper;
        }
        return null;
    }

    public static ArrayList<HostPluginWrapper> getPremiumPluginsForHost() {
        try {
            HostPluginWrapper.readLock.lock();
            final ArrayList<HostPluginWrapper> plugins = new ArrayList<HostPluginWrapper>(HostPluginWrapper.getHostWrapper());
            for (int i = plugins.size() - 1; i >= 0; --i) {
                if (!plugins.get(i).isPremiumEnabled()) {
                    plugins.remove(i);
                }
            }
            return plugins;
        } finally {
            HostPluginWrapper.readLock.unlock();
        }
    }

    /**
     * 
     * 
     * @return RevisionID
     */
    public static String getRevision() {
        return (REVISION != null) ? REVISION : (REVISION = getRevisionNumber() + "");
    }

    public static int getRevisionNumber() {
        if (REVISIONINT != -1) return REVISIONINT;
        int rev = -1;
        try {
            rev = Formatter.filterInt(JDIO.readFileToString(JDUtilities.getResourceFile("config/version.cfg")));
        } catch (Throwable t) {
            t.printStackTrace();
        }
        final int rev2 = Integer.parseInt(Formatter.getRevision("$Revision$"));
        return (REVISIONINT = Math.max(rev2, rev));
    }

    public static int getRunType() {
        final String caller = (Thread.currentThread().getContextClassLoader().getResource("jd") + "");
        return (caller.matches("jar\\:.*\\.jar\\!.*")) ? RUNTYPE_LOCAL_JARED : RUNTYPE_LOCAL;
    }

    public static void setJDargs(final String[] args) {
        JD_ARGUMENTS = args;
    }

    public static String[] getJDargs() {
        return JD_ARGUMENTS;
    }

    public static void restartJDandWait() {
        restartJD(false);
        while (true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }
    }

    public static void restartJD(final boolean tinybypass) {
        new Thread(new Runnable() {
            public void run() {
                if (JDUtilities.getController() != null) {
                    JDUtilities.getController().prepareShutdown(false);
                }

                final List<String> lst = ManagementFactory.getRuntimeMXBean().getInputArguments();
                final ArrayList<String> jargs = new ArrayList<String>();

                boolean xmxset = false;
                boolean xmsset = false;
                boolean useconc = false;
                boolean minheap = false;
                boolean maxheap = false;
                System.out.println("RESTART NOW");
                for (final String h : lst) {
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
                if (!xmxset) {
                    jargs.add("-Xmx512m");
                }
                if (OSDetector.isLinux()) {
                    if (!xmsset) jargs.add("-Xms64m");
                    if (!useconc) jargs.add("-XX:+UseConcMarkSweepGC");
                    if (!minheap) jargs.add("-XX:MinHeapFreeRatio=0");
                    if (!maxheap) jargs.add("-XX:MaxHeapFreeRatio=0");
                }
                jargs.add("-jar");
                jargs.add("JDownloader.jar");

                final String[] javaArgs = jargs.toArray(new String[jargs.size()]);
                final String[] finalArgs = new String[JD_ARGUMENTS.length + javaArgs.length];
                System.arraycopy(javaArgs, 0, finalArgs, 0, javaArgs.length);
                System.arraycopy(JD_ARGUMENTS, 0, finalArgs, javaArgs.length, JD_ARGUMENTS.length);

                final ArrayList<File> restartfiles = JDIO.listFiles(JDUtilities.getResourceFile("update"));
                final String javaPath = new File(new File(System.getProperty("sun.boot.library.path")), "javaw.exe").getAbsolutePath();

                if (restartfiles != null && restartfiles.size() > 0 || tinybypass) {
                    if (OSDetector.isMac()) {
                        LOGGER.info(JDUtilities.runCommand("java", new String[] { "-jar", "tools/tinyupdate.jar", "-restart" }, getResourceFile(".").getAbsolutePath(), 0));
                    } else {
                        if (new File(javaPath).exists()) {
                            LOGGER.info(JDUtilities.runCommand(javaPath, new String[] { "-jar", "tools/tinyupdate.jar", "-restart" }, getResourceFile(".").getAbsolutePath(), 0));
                        } else {
                            LOGGER.info(JDUtilities.runCommand("java", new String[] { "-jar", "tools/tinyupdate.jar", "-restart" }, getResourceFile(".").getAbsolutePath(), 0));
                        }
                    }
                } else {
                    if (OSDetector.isMac()) {
                        LOGGER.info(JDUtilities.runCommand("open", new String[] { "-n", "jDownloader.app" }, JDUtilities.getResourceFile(".").getParentFile().getParentFile().getParentFile().getParentFile().getAbsolutePath(), 0));
                    } else {
                        if (new File(javaPath).exists()) {
                            LOGGER.info(JDUtilities.runCommand(javaPath, finalArgs, getResourceFile(".").getAbsolutePath(), 0));
                        } else {
                            LOGGER.info(JDUtilities.runCommand("java", finalArgs, getResourceFile(".").getAbsolutePath(), 0));
                        }
                    }
                }
                System.out.println("EXIT NOW");
                System.exit(0);
            }
        }).start();
    }

    public static URL getResourceURL(final String resource) {
        final JDClassLoader cl = JDUtilities.getJDClassLoader();
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
    public static File getResourceFile(final String resource) {
        final URL clURL = getResourceURL(resource);
        if (clURL != null) {
            try {
                return new File(clURL.toURI());
            } catch (URISyntaxException e) {
            }
        }
        return null;
    }

    public static File getResourceFile(final String resource, final boolean mkdirs) {
        final URL clURL = getResourceURL(resource);
        if (clURL != null) {
            try {
                final File f = new File(clURL.toURI());
                if (mkdirs) {
                    final File f2 = f.getParentFile();
                    if (f2 != null && !f2.exists()) f2.mkdirs();
                }
                return f;
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
    public static String runCommand(final String command, final String[] parameter, final String runIn, final int waitForReturn) {
        final Executer exec = new Executer(command);
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

    public static String objectToXml(final Object obj) throws IOException {
        final ByteArrayOutputStream ba = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(ba);
        final XMLEncoder xmlEncoder = new XMLEncoder(out);
        xmlEncoder.writeObject(obj);
        xmlEncoder.close();
        out.close();
        return new String(ba.toByteArray());
    }

    public static Object xmlStringToObjekt(final String in) throws IOException {
        Object objectLoaded = null;
        final ByteArrayInputStream ba = new ByteArrayInputStream(in.getBytes());
        final XMLDecoder xmlDecoder = new XMLDecoder(ba);
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
    public static void setConfiguration(final Configuration configuration) {
        JDUtilities.CONFIGURATION = configuration;
    }

    /**
     * Setzt den Controller
     * 
     * @param con
     *            CONTROLLER
     */
    public static void setController(final JDController con) {
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
    public static void setCurrentWorkingDirectory(final File f, final String id) {
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_CURRENT_BROWSE_PATH + (id == null ? "" : id), f.getAbsolutePath());
        JDUtilities.getConfiguration().save();
    }

    public static String removeEndingPoints(final String name) {
        if (name == null) return null;
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
                JDLogger.exception(e);
                final String configpath = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/config/";
                if (e.getMessage().equals("Database broken!")) {
                    LOGGER.severe("Database broken! Creating fresh Database");
                    if (!new File(configpath + "database.script").delete() || !new File(configpath + "database.properties").delete()) {
                        LOGGER.severe("Could not delete broken Database");
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

    public static boolean openExplorer(final File path) {
        try {
            return GetExplorer.openExplorer(path);
        } catch (Exception e) {
            JDLogger.exception(e);
            return false;
        }
    }

    public static Document parseXmlString(final String xmlString, final boolean validating) {
        if (xmlString == null) return null;
        try {
            // Create a builder factory
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(validating);

            final InputSource inSource = new InputSource(new StringReader(xmlString));

            // Create the builder and parse the file
            final Document doc = factory.newDocumentBuilder().parse(inSource);

            return doc;
        } catch (Exception e) {
            LOGGER.severe(xmlString);
            JDLogger.exception(e);
        }
        return null;
    }

    public static String createXmlString(final Document doc) {
        try {
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            // initialize StreamResult with File object to save to file
            final StreamResult result = new StreamResult(new StringWriter());
            final DOMSource source = new DOMSource(doc);

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
    public static String getAttribute(final Node childNode, final String key) {
        final NamedNodeMap att = childNode.getAttributes();
        if (att == null || att.getNamedItem(key) == null) { return null; }
        return att.getNamedItem(key).getNodeValue();
    }

    public static String getDefaultDownloadDirectory() {
        return JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, JDUtilities.getResourceFile("downloads").getAbsolutePath());
    }

}
