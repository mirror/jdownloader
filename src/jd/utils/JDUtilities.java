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
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
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

    private static final Logger      LOGGER              = JDLogger.getLogger();

    /**
     * Die Konfiguration
     */
    private static Configuration     CONFIGURATION       = null;

    private static DatabaseConnector DB_CONNECT          = null;

    /**
     * Ein URLClassLoader, um Dateien aus dem HomeVerzeichnis zu holen
     */
    private static JDClassLoader     JD_CLASSLOADER      = null;

    public static final int          RUNTYPE_LOCAL       = 1;

    public static final int          RUNTYPE_LOCAL_JARED = 2;

    private static File              JD_HOME             = null;

    /**
     * nur 1 UserIO Dialog gleichzeitig (z.b. PW, Captcha)
     */
    public static final Object       USERIO_LOCK         = new Object();

    private static String            REVISION;

    private static String[]          JD_ARGUMENTS        = new String[1];

    private static long              REVISIONINT         = -1;

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
     *            Abstaende der Komponente
     * @param fill
     *            Verteilung der Komponente innerhalb der zugewiesen Zelle/n
     * @param anchor
     *            Positionierung der Komponente innerhalb der zugewiesen Zelle/n
     */
    public static void addToGridBag(final Container cont, final Component comp, final int x, final int y, final int width, final int height, final int weightX, final int weightY, final Insets insets, final int fill, final int anchor) {
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
     * verschluesselt string mit der uebergebenen encryption
     * (Containerpluginname
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

    /**
     * @return Configuration instanz
     */
    public static Configuration getConfiguration() {
        if (CONFIGURATION == null) CONFIGURATION = new Configuration();
        return CONFIGURATION;
    }

    /**
     * Gibt den verwendeten Controller zurueck
     * 
     * @return gerade verwendete CONTROLLER-instanz
     */
    public static JDController getController() {
        return JDController.getInstance();
    }

    /**
     * Diese Funktion gibt den Pfad zum JAC-Methodenverzeichniss zurueck
     * 
     * @author JD-Team
     * @return gibt den Pfad zu den JAC Methoden zurueck
     */
    public static String getJACMethodsDirectory() {
        return "jd/captcha/methods/";
    }

    /**
     * @return Gibt die verwendete java Version als Double Value zurueck. z.B.
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
     * Liefert einen URLClassLoader zurueck, um Dateien aus dem Stammverzeichnis
     * zu laden
     * 
     * @return URLClassLoader
     */

    public static JDClassLoader getJDClassLoader() {
        if (JD_CLASSLOADER == null) {
            final File homeDir = JDUtilities.getJDHomeDirectoryFromEnvironment();
            // String url = null;
            // Url Encode des pfads fuer den Classloader
            LOGGER.finest("Create Classloader: for: " + homeDir.getAbsolutePath());
            JD_CLASSLOADER = new JDClassLoader(homeDir.getAbsolutePath(), Thread.currentThread().getContextClassLoader());

        }
        return JD_CLASSLOADER;
    }

    /**
     * Liefert das Basisverzeichnis fuer jD zurueck. Don't use a logger in this
     * method. It will cause a NullpointerException, because the logger need
     * this method for initialisation.
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
     * Sucht ein passendes Plugin fuer ein Containerfile
     * 
     * @param container
     *            Der Host, von dem das Plugin runterladen kann
     * @param containerPath
     * @return Ein passendes Plugin oder null
     */
    public static PluginsC getPluginForContainer(final String container) {
        if (container == null) return null;
        for (final CPluginWrapper act : CPluginWrapper.getCWrapper()) {
            if (act.getHost().equalsIgnoreCase(container)) return (PluginsC) act.getNewPluginInstance();
        }
        return null;
    }

    public static String getContainerExtensions(final String filter) {
        StringBuilder sb = new StringBuilder("");
        for (final CPluginWrapper act : CPluginWrapper.getCWrapper()) {
            if (filter != null && !new Regex(act.getHost(), filter).matches()) continue;
            String exs[] = new Regex(act.getPattern().pattern(), "\\.([a-zA-Z0-9]+)").getColumn(0);
            for (String ex : exs) {
                if (sb.length() > 0) sb.append("|");
                sb.append(".").append(ex);
            }
        }
        return sb.toString();
    }

    /**
     * Sucht ein passendes Plugin fuer einen Anbieter Please dont use the
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

    public static PluginForHost replacePluginForHost(final DownloadLink link) {
        try {
            HostPluginWrapper.readLock.lock();
            for (final HostPluginWrapper pHost : HostPluginWrapper.getHostWrapper()) {
                if (pHost.getPlugin().rewriteHost(link)) return pHost.getPlugin();
            }
        } finally {
            HostPluginWrapper.readLock.unlock();
        }
        return null;
    }

    public static PluginForHost getNewPluginForHostInstance(final String host) {
        PluginForHost plugin = getPluginForHost(host);
        if (plugin != null) return plugin.getWrapper().getNewPluginInstance();
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

    public static long getRevisionNumber() {
        if (REVISIONINT != -1) return REVISIONINT;
        int rev = -1;
        try {
            rev = Formatter.filterInt(JDIO.readFileToString(JDUtilities.getResourceFile("config/version.cfg")));
        } catch (Throwable t) {
            t.printStackTrace();
        }
        final long rev2 = Formatter.getRevision("$Revision$");
        return (REVISIONINT = Math.max(rev2, rev));
    }

    public static int getRunType() {
        final String caller = (Thread.currentThread().getContextClassLoader().getResource("jd") + "");
        return (caller.matches("jar\\:.*\\.jar\\!.*")) ? RUNTYPE_LOCAL_JARED : RUNTYPE_LOCAL;
    }

    public static void setJDargs(final String[] args) {
        JD_ARGUMENTS = args;
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

    private static URL getResourceURL(final String resource) {
        final JDClassLoader cl = JDUtilities.getJDClassLoader();
        if (cl == null) {
            System.err.println("Classloader == null");
            return null;
        }
        return cl.getResource(resource);
    }

    /**
     * Gibt ein FileOebject zu einem Resourcstring zurueck
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
     * Fuehrt einen Externen befehl aus.
     * 
     * @param command
     * @param parameter
     * @param runIn
     * @param waitForReturn
     * @return null oder die rueckgabe des befehls falls waitforreturn == true
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

    /**
     * Setzt die Konfigurations instanz
     * 
     * @param CONFIGURATION
     */
    public static void setConfiguration(final Configuration configuration) {
        JDUtilities.CONFIGURATION = configuration;
    }

    public static DownloadController getDownloadController() {
        return DownloadController.getInstance();
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
     * Gibt das Attribut zu key in childNode zurueck
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
