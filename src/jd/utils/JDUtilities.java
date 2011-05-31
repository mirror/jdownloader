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
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
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
import jd.Main;
import jd.config.Configuration;
import jd.config.DatabaseConnector;
import jd.controlling.DownloadController;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.nutils.Executer;
import jd.nutils.Formatter;
import jd.nutils.io.JDIO;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;

import org.appwork.utils.Application;
import org.appwork.utils.Regex;
import org.jdownloader.translate._JDT;
import org.jdownloader.update.JDUpdater;
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

    public static final int          RUNTYPE_LOCAL       = 1;

    public static final int          RUNTYPE_LOCAL_JARED = 2;

    private static File              JD_HOME             = null;

    /**
     * nur 1 UserIO Dialog gleichzeitig (z.b. PW, Captcha)
     */
    public static final Object       USERIO_LOCK         = new Object();

    private static String            REVISION;

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

    public static String convertExceptionReadable(final Throwable e) {
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

    /* please keep this */
    public static void setDB_CONNECT(DatabaseConnector dB_CONNECT) {
        DB_CONNECT = dB_CONNECT;
    }

    /* please keep this */
    public static void setJDHomeDirectory(File home) {
        if (home == null) return;
        JD_HOME = home;
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
            // do not use hardcoded classpathes if possible
            URL ressource = Thread.currentThread().getContextClassLoader().getResource(Main.class.getName().replace(".", "/") + ".class");

            if (ressource != null) {
                JD_HOME = new File(Application.getRoot(jd.Main.class));
            } else {
                throw new NullPointerException("jd/Main.class not found");
            }
            if (!JD_HOME.exists()) {
                JD_HOME.mkdirs();
            }
            return JD_HOME;
        }
    }

    public static String getJDTitle() {
        final StringBuilder ret = new StringBuilder("JDownloader");

        if (JDUpdater.getInstance().getWaitingUpdates() > 0) {
            ret.append(new char[] { ' ', '(' });
            ret.append(_JDT._.gui_mainframe_title_updatemessage2(JDUpdater.getInstance().getWaitingUpdates()));
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
                if (pHost.getHost().equals(host.toLowerCase(Locale.getDefault()))) { return pHost.getPlugin(); }
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

    /**
     * Gibt ein FileOebject zu einem Resourcstring zurueck
     * 
     * @author JD-Team
     * @param resource
     *            Ressource, die geladen werden soll
     * @return File zu arg
     */
    public static File getResourceFile(final String resource) {
        return Application.getResource(resource);

    }

    public static File getResourceFile(final String resource, final boolean mkdirs) {
        final File f = getResourceFile(resource);
        if (f != null) {
            if (mkdirs) {
                final File f2 = f.getParentFile();
                if (f2 != null && !f2.exists()) f2.mkdirs();
            }
            return f;
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

}