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
import java.awt.Frame;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilderFactory;

import jd.CPluginWrapper;
import jd.HostPluginWrapper;
import jd.JDClassLoader;
import jd.Main;
import jd.config.Configuration;
import jd.config.DatabaseConnector;
import jd.config.SubConfiguration;
import jd.controlling.DownloadController;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.gui.UIInterface;
import jd.http.Browser;
import jd.http.JDProxy;
import jd.nutils.Executer;
import jd.nutils.Formatter;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;

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
    public static Configuration configuration = null;

    private static DatabaseConnector dbconnect = null;

    private static HashMap<String, PluginsC> containerPlugins = new HashMap<String, PluginsC>();

    /**
     * Der DownloadController
     */
    private static JDController controller = null;

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
    private static JDClassLoader jdClassLoader = null;

    public static final int RUNTYPE_LOCAL = 1;

    public static final int RUNTYPE_LOCAL_JARED = 2;

    private static File JD_HOME = null;

    /**
     * nur 1 UserIO Dialog gleichzeitig (z.b.PW,Captcha)
     */
    public static Integer userio_lock = new Integer(0);

    private static String LATEST_IP = null;

    private static String REVISION;

    public static String getSimString(String a, String b) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            if (a.charAt(i) == b.charAt(i)) {
                ret.append(a.charAt(i));
            }
        }
        return ret.toString();
    }

    @SuppressWarnings("unchecked")
    public static Map revSortByKey(Map map) {
        List list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getKey()).compareTo(((Map.Entry) (o2)).getKey());
            }
        });
        // JDUtilities.getLogger().info(list);
        Map result = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static Map sortByKey(Map map) {
        List list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o2)).getKey()).compareTo(((Map.Entry) (o1)).getKey());
            }
        });
        // JDUtilities.getLogger().info(list);
        Map result = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
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

    /**
     * Fügt dem Dateinamen den erkannten Code noch hinzu
     * 
     * @param file
     *            Die Datei, der der Captchacode angefügt werden soll
     * @param captchaCode
     *            Der erkannte Captchacode
     * @param isGood
     *            Zeigt, ob der erkannte Captchacode korrekt ist
     */
    public static void appendInfoToFilename(File file, String captchaCode, boolean isGood) {
        String dest = file.getAbsolutePath();
        if (captchaCode == null) {
            captchaCode = "null";
        }
        int idx = dest.lastIndexOf('.');
        dest = dest.substring(0, idx) + "_" + captchaCode.toUpperCase() + (isGood ? "_GOOD" : "_BAD") + dest.substring(idx);
        file.renameTo(new File(dest));
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

    public static String createContainerString(Vector<DownloadLink> downloadLinks, String encryption) {
        ArrayList<CPluginWrapper> pfc = CPluginWrapper.getCWrapper();
        for (int i = 0; i < pfc.size(); i++) {
            String pn = pfc.get(i).getHost();
            if (pn.equalsIgnoreCase(encryption)) return pfc.get(i).getPlugin().createContainerString(downloadLinks);
        }
        return null;
    }

    /**
     * verschlüsselt string mit der übergebenen encryption
     * (Containerpluginname
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

        try {
            link.getLinkStatus().addStatus(LinkStatus.WAITING_USERIO);
            link.requestGuiUpdate();
            String code = getUserInput(message, null, link);

            link.requestGuiUpdate();
            return code;

        } finally {
            link.getLinkStatus().removeStatus(LinkStatus.WAITING_USERIO);
        }
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
        link.getProgressController().setStatusText(JDLocale.L("gui.linkgrabber.waitinguserio", "Waiting for user input"));
        String password = getUserInput(message, defaultmessage);
        link.getProgressController().setStatusText(null);
        return password;
    }

    public static String getUserInput(String message, String defaultmessage) throws InterruptedException {
        synchronized (userio_lock) {
            if (message == null) message = JDLocale.L("gui.linkgrabber.password", "Password?");
            if (defaultmessage == null) defaultmessage = "";
            String password = JDUtilities.getGUI().showCountdownUserInputDialog(message, defaultmessage);
            return password;
        }
    }

    /**
     * @return Configuration instanz
     */
    public static Configuration getConfiguration() {
        if (configuration == null) configuration = new Configuration();
        return configuration;
    }

    /**
     * Gibt den verwendeten Controller zurück
     * 
     * @return gerade verwendete controller-instanz
     */
    public static JDController getController() {
        return controller;
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
                JDLogger.getLogger().log(Level.SEVERE, "Exception occured", e);
                return 0;
            }

            byte[] buf = new byte[128];
            while (cis.read(buf) >= 0) {
            }

            long checksum = cis.getChecksum().getValue();
            return checksum;

        } catch (IOException e) {
            JDLogger.getLogger().log(Level.SEVERE, "Exception occured", e);
            return 0;
        }

    }

    /**
     * Gibt das aktuelle Working Directory zurück. Beim FileBrowser etc wird
     * das gebraucht.
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

    public static UIInterface getGUI() {
        if (JDUtilities.getController() == null) return null;
        return JDUtilities.getController().getUiInterface();
    }

    /**
     * Prüft anhand der Globalen IP Check einstellungen die IP
     * 
     * @param br
     *            TODO
     * 
     * @return ip oder /offline
     */
    public static String getIPAddress(Browser br) {

        if (br == null) {
            br = new Browser();
            br.setProxy(JDProxy.NO_PROXY);

            br.setConnectTimeout(5000);
            br.setReadTimeout(5000);
        }
        if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
            JDLogger.getLogger().finer("IP Check is disabled. return current Milliseconds");
            return System.currentTimeMillis() + "";
        }

        String site = SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_CHECK_SITE, "http://checkip.dyndns.org");
        String patt = SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_PATTERN, "Address\\: ([0-9.]*)\\<\\/body\\>");

        try {
            JDLogger.getLogger().finer("IP Check via " + site);
            Pattern pattern = Pattern.compile(patt);
            Matcher matcher = pattern.matcher(br.getPage(site));
            if (matcher.find()) {
                if (matcher.groupCount() > 0) {
                    return LATEST_IP = matcher.group(1);
                } else {
                    JDLogger.getLogger().severe("Primary bad Regex: " + patt);

                }
            }
            JDLogger.getLogger().info("Primary IP Check failed. Ip not found via regex: " + patt + " on " + site + " htmlcode: " + br.toString());

        }

        catch (Exception e1) {
            JDLogger.getLogger().severe("url not found. " + e1.toString());

        }

        try {

            JDLogger.getLogger().finer("http://service.jdownloader.org/tools/getip.php");

            Pattern pattern = Pattern.compile(patt);
            Matcher matcher = pattern.matcher(br.getPage("http://service.jdownloader.org/tools/getip.php"));
            if (matcher.find()) {
                if (matcher.groupCount() > 0) {
                    return LATEST_IP = matcher.group(1);
                } else {
                    JDLogger.getLogger().severe("Primary bad Regex: " + patt);
                }
            }
            LATEST_IP = null;
            return "offline";
        }

        catch (Exception e1) {
            JDLogger.getLogger().severe("url not found. " + e1.toString());
            JDLogger.getLogger().info("Sec. IP Check failed.");

        }
        LATEST_IP = null;
        return "offline";
    }

    public static String getLatestIP() {
        if (LATEST_IP == null) getIPAddress(null);
        return LATEST_IP;
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
        if (jdClassLoader == null) {
            File homeDir = JDUtilities.getJDHomeDirectoryFromEnvironment();
            // String url = null;
            // Url Encode des pfads für den Classloader
            JDLogger.getLogger().finest("Create Classloader: for: " + homeDir.getAbsolutePath());
            jdClassLoader = new JDClassLoader(homeDir.getAbsolutePath(), Thread.currentThread().getContextClassLoader());

        }
        return jdClassLoader;
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
            JDLogger.getLogger().log(Level.SEVERE, "Exception occured", e);
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
        ret.append(' ');
        ret.append(JDUtilities.JD_VERSION);
        ret.append(JDUtilities.getRevision());
        if (Main.isBeta()) {
            ret.append(' ');
            ret.append(JDLocale.L("gui.mainframe.title.beta", "-->BETA Version<--"));
        }
        // if (JDUtilities.getController() != null &&
        // JDUtilities.getController().getWaitingUpdates() != null &&
        // JDUtilities.getController().getWaitingUpdates().size() > 0) {
        // ret.append(' ');
        // ret.append(JDLocale.L("gui.mainframe.title.updatemessage",
        // "-->UPDATES VERFÜGBAR:"));
        // ret.append(' ');
        // ret.append(JDUtilities.getController().getWaitingUpdates().size());
        // }
        return ret.toString();
    }

    /**
     * Geht eine Komponente so lange durch (getParent), bis ein Objekt vom Typ
     * Frame gefunden wird, oder es keine übergeordnete Komponente gibt
     * 
     * @param comp
     *            Komponente, dessen Frame Objekt gesucht wird
     * @return Ein Frame Objekt, das die Komponente beinhält oder null, falls
     *         keins gefunden wird
     */
    public static Frame getParentFrame(Component comp) {
        if (comp == null) { return null; }
        while (comp != null && !(comp instanceof Frame)) {
            comp = comp.getParent();
        }
        if (comp instanceof Frame) {
            return (Frame) comp;
        } else {
            return null;
        }
    }

    public static String getPercent(long downloadCurrent, long downloadMax) {
        DecimalFormat c = new DecimalFormat("0.00");
        return c.format(100.0 * downloadCurrent / (double) downloadMax) + "%";
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
        if (containerPath != null && containerPlugins.containsKey(containerPath)) { return containerPlugins.get(containerPath); }
        PluginsC ret = null;
        for (CPluginWrapper act : CPluginWrapper.getCWrapper()) {
            if (act.getHost().equalsIgnoreCase(container)) {

                ret = (PluginsC) act.getNewPluginInstance();
                if (containerPath != null) {
                    containerPlugins.put(containerPath, ret);
                }
                return ret;

            }
        }
        return null;
    }

    /**
     * Sucht ein passendes Plugin für einen Anbieter
     * 
     * @param host
     *            Der Host, von dem das Plugin runterladen kann
     * @return Ein passendes Plugin oder null
     */
    public static PluginForHost getPluginForHost(String host) {
        for (int i = 0; i < HostPluginWrapper.getHostWrapper().size(); i++) {
            if (HostPluginWrapper.getHostWrapper().get(i).getHost().equals(host.toLowerCase())) { return (PluginForHost) HostPluginWrapper.getHostWrapper().get(i).getPlugin(); }
        }
        return null;
    }

    public static PluginForHost getNewPluginForHostInstanz(String host) {
        for (int i = 0; i < HostPluginWrapper.getHostWrapper().size(); i++) {
            if (HostPluginWrapper.getHostWrapper().get(i).getHost().equals(host.toLowerCase())) { return (PluginForHost) HostPluginWrapper.getHostWrapper().get(i).getNewPluginInstance(); }
        }
        return null;
    }

    public static ArrayList<Account> getAccountsForHost(String host) {
        PluginForHost plugin = JDUtilities.getPluginForHost(host);
        if (plugin != null) {
            return plugin.getPremiumAccounts();
        } else {
            return null;
        }
    }

    /**
     * Liefert alle Plugins zum Downloaden von einem Anbieter zurück. Die liste
     * wird dabei sortiert zurückgegeben
     * 
     * @return Plugins zum Downloaden von einem Anbieter
     */
    @SuppressWarnings("unchecked")
    public static ArrayList<HostPluginWrapper> getPluginsForHost() {

        ArrayList<HostPluginWrapper> plgs = new ArrayList<HostPluginWrapper>();

        plgs.addAll(HostPluginWrapper.getHostWrapper());

        ArrayList<HostPluginWrapper> pfh = new ArrayList<HostPluginWrapper>();
        Vector<String> priority = (Vector<String>) JDUtilities.getConfiguration().getProperty(Configuration.PARAM_HOST_PRIORITY, new Vector<String>());
        for (int i = 0; i < priority.size(); i++) {
            for (int b = plgs.size() - 1; b >= 0; b--) {
                if (plgs.get(b).getHost() == null) {
                    JDLogger.getLogger().info("OO");
                }
                if (plgs.get(b).getHost().equalsIgnoreCase(priority.get(i))) {
                    HostPluginWrapper plg = plgs.remove(b);
                    pfh.add(plg);
                    break;
                }
            }
        }
        pfh.addAll(plgs);
        return pfh;
    }

    /**
     * Parsed den Revision-String ins Format 0.000
     * 
     * @return RevisionID
     */
    public static String getRevision() {
        if (REVISION != null) return REVISION;
        double r = Double.parseDouble(getVersion("$Revision$")) / 1000.0;
        return REVISION = new DecimalFormat("0.000").format(r).replace(",", ".");
    }

    /**
     * Parsed den String revision und gibt die RevisionsNummer zurück
     * 
     * @param revision
     * @return RevisionsNummer
     */
    public static String getVersion(String revision) {

        String ret = new Regex(revision, "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    public static int getRunType() {
        String caller = (Thread.currentThread().getContextClassLoader().getResource("jd") + "");

        if (caller.matches("jar\\:.*\\.jar\\!.*")) {
            return RUNTYPE_LOCAL_JARED;
        } else {
            return RUNTYPE_LOCAL;
        }

    }

    public static void restartJD() {
        if (JDUtilities.getController() != null) JDUtilities.getController().prepareShutdown();
        JDLogger.getLogger().info(JDUtilities.runCommand("java", new String[] { "-Xmx512m", "-jar", "JDownloader.jar", }, getResourceFile(".").getAbsolutePath(), 0));
        System.exit(0);

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

    public static void restartJD(String[] jdArgs) {
        if (JDUtilities.getController() != null) JDUtilities.getController().prepareShutdown();
        String[] javaArgs = new String[] { "-Xmx512m", "-jar", "JDownloader.jar" };
        String[] finalArgs = new String[jdArgs.length + javaArgs.length];
        System.arraycopy(javaArgs, 0, finalArgs, 0, javaArgs.length);
        System.arraycopy(jdArgs, 0, finalArgs, javaArgs.length, jdArgs.length);

        JDLogger.getLogger().info(JDUtilities.runCommand("java", finalArgs, getResourceFile(".").getAbsolutePath(), 0));
        System.exit(0);
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
     * @param configuration
     */
    public static void setConfiguration(Configuration configuration) {
        JDUtilities.configuration = configuration;
    }

    /**
     * Setzt den Controller
     * 
     * @param con
     *            controller
     */
    public static void setController(JDController con) {
        controller = con;
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

    /**
     * Überprüft ob eine IP gültig ist. das verwendete Pattern aknn in der
     * config editiert werden.
     * 
     * @param ip
     * @return
     */
    public static boolean validateIP(String ip) {
        return Pattern.compile(SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_MASK, "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).)" + "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b")).matcher(ip).matches();
    }

    public static String removeEndingPoints(String name) {
        if (name == null) { return null; }
        String ret = name;
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

        if (dbconnect == null) {

            try {
                dbconnect = new DatabaseConnector();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                JDLogger.exception(e);
                String configpath = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/config/";
                Logger logger = JDLogger.getLogger();
                if (e.getMessage().equals("Database broken!")) {
                    logger.severe("Database broken! Creating fresh Database");

                    if (!new File(configpath + "database.script").delete()||!new File(configpath + "database.properties").delete()) {
                        logger.severe("Could not delete broken Database");
                        JOptionPane.showMessageDialog(null, "Could not delete broken database. Please remove the JD_HOME/config directory and restart JD");

                    }
                }

                try {
                    dbconnect = new DatabaseConnector();
                } catch (Exception e1) {
                    JDLogger.exception(e1);
                    JOptionPane.showMessageDialog(null, "Could not create database. Please remove the JD_HOME/config directory and restart JD");
                   
                    System.exit(1);
                }
            }

        }
        return dbconnect;

    }

    public static boolean openExplorer(File path) {
        try {
            return new GetExplorer().openExplorer(path);
        } catch (Exception e) {
            JDLogger.getLogger().log(Level.SEVERE, "Exception occured", e);
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
