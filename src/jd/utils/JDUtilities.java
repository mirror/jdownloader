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

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jd.Launcher;
import jd.config.Configuration;
import jd.config.DatabaseConnector;
import jd.config.NoOldJDDataBaseFoundException;
import jd.controlling.JDController;
import jd.nutils.Executer;
import jd.nutils.Formatter;
import jd.nutils.io.JDIO;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.utils.Application;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.translate._JDT;
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
    private static Configuration     CONFIGURATION = null;

    private static DatabaseConnector DB_CONNECT    = null;

    private static File              JD_HOME       = null;

    private static String            REVISION;

    private static long              REVISIONINT   = -1;
    private static Boolean           oldDBExists   = null;

    /**
     * returns old Configuration (if available) or creates dummy one (in new JD2) so we can import existing configs into current version
     * 
     * @return
     */
    @Deprecated
    public static synchronized Configuration getConfiguration() {
        if (CONFIGURATION == null) {
            try {
                Object obj = JDUtilities.getDatabaseConnector().getData(Configuration.NAME);
                if (obj != null) {
                    CONFIGURATION = (Configuration) obj;
                }
            } catch (NoOldJDDataBaseFoundException e) {
            } catch (Throwable e) {
                LogController.CL().log(e);
            }
        }
        if (CONFIGURATION == null) CONFIGURATION = new Configuration();
        return CONFIGURATION;
    }

    /**
     * Gibt den verwendeten Controller zurueck
     * 
     * @return gerade verwendete CONTROLLER-instanz
     */
    @Deprecated
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
     * Liefert das Basisverzeichnis fuer jD zurueck. Don't use a logger in this method. It will cause a NullpointerException, because the logger need this
     * method for initialisation.
     * 
     * @return ein File, dass das Basisverzeichnis angibt
     */
    public static File getJDHomeDirectoryFromEnvironment() {
        if (JD_HOME != null) {
            return JD_HOME;
        } else {
            // do not use hardcoded classpathes if possible
            URL ressource = Thread.currentThread().getContextClassLoader().getResource(Launcher.class.getName().replace(".", "/") + ".class");

            if (ressource != null) {
                JD_HOME = new File(Application.getRoot(jd.Launcher.class));
            } else {
                throw new NullPointerException("jd/Main.class not found");
            }
            if (!JD_HOME.exists()) {
                JD_HOME.mkdirs();
            }
            return JD_HOME;
        }
    }

    public static String getJDTitle(int waitingupdates) {
        final StringBuilder ret = new StringBuilder("JDownloader");

        if (waitingupdates > 0) {
            ret.append(new char[] { ' ', '(' });
            ret.append(_JDT._.gui_mainframe_title_updatemessage2(waitingupdates));
            ret.append(')');
        }

        return ret.toString();
    }

    /**
     * Sucht ein passendes Plugin fuer einen Anbieter Please dont use the returned Plugin to start any function
     * 
     * @param host
     *            Der Host, von dem das Plugin runterladen kann
     * @return Ein passendes Plugin oder null
     */
    public static PluginForDecrypt getPluginForDecrypt(final String host) {
        LazyCrawlerPlugin l = CrawlerPluginController.getInstance().get(host);
        if (l != null) try {
            return l.getPrototype();
        } catch (UpdateRequiredClassNotFoundException e) {
            LogController.CL().log(e);
            return null;
        }
        return null;
    }

    public static PluginForHost getPluginForHost(final String host) {
        LazyHostPlugin lplugin = HostPluginController.getInstance().get(host);
        if (lplugin != null) try {
            return lplugin.getPrototype();
        } catch (UpdateRequiredClassNotFoundException e) {
            LogController.CL().log(e);
            return null;
        }
        return null;
    }

    public static PluginForHost getNewPluginForHostInstance(final String host) {
        LazyHostPlugin lplugin = HostPluginController.getInstance().get(host);
        if (lplugin != null) try {
            return lplugin.newInstance();
        } catch (UpdateRequiredClassNotFoundException e) {
            LogController.CL().log(e);
            return null;
        }
        return null;
    }

    /**
     * 
     * 
     * @return RevisionID
     */
    public static String getRevision() {
        return (REVISION != null) ? REVISION : (REVISION = getRevisionNumber() + "");
    }

    /* DO NOT USE in old 09581 stable */
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

    /**
     * Gibt ein FileOebject zu einem Resourcstring zurueck
     * 
     * @author JD-Team
     * @param resource
     *            Ressource, die geladen werden soll
     * @return File zu arg
     */
    @Deprecated
    /* please use Application.getRessource where possible */
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
     * @return null oder die rueckgabe des befehls falls waitforreturn == true ist
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

    public synchronized static DatabaseConnector getDatabaseConnector() throws NoOldJDDataBaseFoundException {
        if (oldDBExists == null) oldDBExists = Application.getResource("/config/database.script").exists();
        if (Boolean.FALSE.equals(oldDBExists)) throw new NoOldJDDataBaseFoundException();
        if (DB_CONNECT != null) return DB_CONNECT;
        if (DB_CONNECT == null) {
            try {
                DB_CONNECT = new DatabaseConnector();
            } catch (Throwable e) {
                /* we no longer use old DataBase, so no need to retry or create fresh one */
                LogController.CL().log(e);
                LogController.CL().severe("Rename old Database to avoid loading it again!");
                Application.getResource("/config/database.script").renameTo(Application.getResource("/config/database.scriptBAK"));
                oldDBExists = false;
                throw new NoOldJDDataBaseFoundException();
            }
        }
        return DB_CONNECT;
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
            LogController.CL().severe(xmlString);
            LogController.CL().log(e);
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
        } catch (Throwable e) {
            LogController.CL().log(e);
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