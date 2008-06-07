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

package jd.utils;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Toolkit;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;
import jd.JDClassLoader;
import jd.JDFileFilter;
import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Captcha;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.gui.UIInterface;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.LogFormatter;
import jd.plugins.Plugin;
import jd.plugins.PluginForContainer;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
import jd.plugins.RequestInfo;
import jd.update.WebUpdater;

import org.w3c.dom.Document;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * @author astaldo/JD-Team
 */ 
public class JDUtilities {

    /** 
     * Parametername für den Konfigpath 
     */
    public static final String CONFIG_PATH = "jDownloader.config";

    private static HashMap<String, SubConfiguration> subConfigs = new HashMap<String, SubConfiguration>();

    /**
     * Name des Loggers 
     */
    public static String LOGGER_NAME = "java_downloader"; 

    /**
     * Titel der Applikation
     */
    public static final String JD_VERSION = "0.";

    public static final String JD_REVISION = "$Id$";

    /**
     * Versionsstring der Applikation
     */
    public static final String JD_TITLE = "jDownloader";

    private static final int RUNTYPE_WEBSTART = 0;

    public static final int RUNTYPE_LOCAL = 1;

    public static final int RUNTYPE_LOCAL_JARED = 2;

    public static final int RUNTYPE_LOCAL_ENV = 3;

    private static final int OS_TYPE = -1;

    // private static Vector<PluginForSearch> pluginsForSearch = null;

    private static Vector<PluginForContainer> pluginsForContainer = null;

    private static Vector<PluginForHost> pluginsForHost = null;

    private static HashMap<String, PluginOptional> pluginsOptional = null;

    private static Vector<PluginForDecrypt> pluginsForDecrypt;

    /**
     * Ein URLClassLoader, um Dateien aus dem HomeVerzeichnis zu holen
     */
    private static JDClassLoader jdClassLoader = null;

    /**
     * Das JD-Home Verzeichnis. Dieses wird nur gesetzt, wenn es aus dem
     * WebStart Cookie gelesen wurde. Diese Variable kann nämlich im
     * KonfigDialog geändert werden
     */
    private static String homeDirectory = null;

    /**
     * Das ist das File Objekt, daß das HomeDirectory darstellt
     */
    private static File homeDirectoryFile = null;

    /**
     * Der DownloadController
     */
    private static JDController controller = null;

    /**
     * RessourceBundle für Texte
     */
    private static ResourceBundle resourceBundle = null;

    /**
     * Angaben über Spracheinstellungen
     */
    private static Locale locale = null;

    /**
     * Alle verfügbaren Bilder werden hier gespeichert
     */
    private static HashMap<String, Image> images = new HashMap<String, Image>();
    private static HashMap<String, PluginForContainer> containerPlugins = new HashMap<String, PluginForContainer>();
    /**
     * Der Logger für Meldungen
     */
    private static Logger logger = JDUtilities.getLogger();

    /**
     * Damit werden die JARs rausgesucht
     */
    public static JDFileFilter filterJar = new JDFileFilter(null, ".jar", false);

    /**
     * Das aktuelle Verzeichnis (Laden/Speichern)
     */
    private static File currentDirectory;

    /**
     * Die Konfiguration
     */
    private static Configuration configuration = new Configuration();

    /**
     * Gibt das aktuelle Working Directory zurück. Beim FilebRowser etc wird da
     * s gebraucht.
     * 
     * @return
     */
    public static File getCurrentWorkingDirectory(String id) {
        if (id == null) id = "";
        String dlDir = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, null);
        String lastDir = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_CURRENT_BROWSE_PATH + id, null);

        File dlDirectory;
        // File lastDirectory;
        if (dlDir == null) {
            dlDirectory = new File("");
        } else {
            dlDirectory = new File(dlDir);
        }

        if (lastDir == null) return dlDirectory;
        return new File(lastDir);

    }

    /**
     * Setztd as aktuelle woringdirectory für den filebrowser
     * 
     * @param f
     */
    public static void setCurrentWorkingDirectory(File f, String id) {
        if (id == null) id = "";
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_CURRENT_BROWSE_PATH + id, f.getAbsolutePath());
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
        if (comp == null) return null;
        while (comp != null && !(comp instanceof Frame))
            comp = comp.getParent();
        if (comp instanceof Frame)
            return (Frame) comp;
        else
            return null;
    }

    /**
     * parsed den JD_REVISION String auf
     * 
     * @return RevissionID
     */
    public static String getRevision() {
        String[] data = JD_REVISION.split(" ");
        if (data.length > 2) {
            int rev = JDUtilities.filterInt(data[2]);
            double r = (double) rev / 1000.0;
            return r + "";
        }
        return null;
    }

    /**
     * parsed den JD_REVISION String auf
     * 
     * @return Letztes Änderungs datum
     */
    public static String getLastChangeDate() {
        String[] data = JD_REVISION.split(" ");
        if (data.length > 3) {
            String[] date = data[3].split("-");
            if (date.length != 3) return null;
            return date[2] + "." + date[1] + "." + date[0];
        }
        return null;
    }

    /**
     * parsed den JD_REVISION String auf
     * 
     * @return Letzte änderungsuhrzeit
     */
    public static String getLastChangeTime() {
        String[] data = JD_REVISION.split(" ");
        if (data.length > 4) { return data[4].substring(0, data[4].length() - 1); }
        return null;
    }

    /**
     * parsed den JD_REVISION String auf
     * 
     * @return Name des programmierers der die letzten Änderungen durchgeführt
     *         hat
     */
    public static String getLastChangeAuthor() {
        String[] data = JD_REVISION.split(" ");
        if (data.length > 5) { return data[5]; }
        return null;
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
            logger.severe("Container ==null");
            return;
        }
        if (comp == null) {
            logger.severe("Componente ==null");
            return;
        }
        addToGridBag(cont, comp, x, y, width, height, weightX, weightY, insets, 0, 0, fill, anchor);
    }

    public static String sprintf(String pattern, String[] inset) {

        for (int i = 0; i < inset.length; i++) {
            int ind = pattern.indexOf("%s");
            pattern = pattern.substring(0, ind) + inset[i] + pattern.substring(ind + 2);

        }

        return pattern;
    }

    /**
     * Liefert einen Punkt zurück, mit dem eine Komponente auf eine andere
     * zentriert werden kann
     * 
     * @param parent
     *            Die Komponente, an der ausgerichtet wird
     * @param child
     *            Die Komponente die ausgerichtet werden soll
     * @return Ein Punkt, mit dem diese Komponente mit der setLocation Methode
     *         zentriert dargestellt werden kann
     */
    public static Point getCenterOfComponent(Component parent, Component child) {
        Point center;
        if (parent == null || !parent.isShowing()) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int width = screenSize.width;
            int height = screenSize.height;
            center = new Point(width / 2, height / 2);
        } else {
            center = parent.getLocationOnScreen();
            center.x += parent.getWidth() / 2;
            center.y += parent.getHeight() / 2;
        }
        // Dann Auszurichtende Komponente in die Berechnung einfließen lassen
        center.x -= child.getWidth() / 2;
        center.y -= child.getHeight() / 2;
        return center;
    }

    public static String[] splitByNewline(String arg) {
        if (arg == null) return new String[] {};
        return arg.split("[\r|\n|\r\n]{1,2}");
    }

    /**
     * Liefert eine Zeichenkette aus dem aktuellen ResourceBundle zurück
     * 
     * @param key
     *            Identifier der gewünschten Zeichenkette
     * @return Die gewünschte Zeichnenkette
     */
    public static String getResourceString(String key) {
        if (resourceBundle == null) {
            if (locale == null) {
                locale = Locale.getDefault();
            }
            resourceBundle = ResourceBundle.getBundle("LanguagePack", locale);
        }
        String result = key;
        try {
            result = resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            logger.warning("resource missing:" + e.getKey());
        }
        return result;
    }

    /**
     * Liefert einer char aus dem aktuellen ResourceBundle zurück
     * 
     * @param key
     *            Identifier des gewünschten chars
     * @return der gewünschte char
     */
    public static char getResourceChar(String key) {
        char result = 0;
        String s = getResourceString(key);
        if (s != null && s.length() > 0) {
            result = s.charAt(0);
        }
        return result;
    }

    /**
     * Liefert aus der Map der geladenen Bilder ein Element zurück
     * 
     * @param imageName
     *            Name des Bildes das zurückgeliefert werden soll
     * @return Das gewünschte Bild oder null, falls es nicht gefunden werden
     *         kann
     */
    public static Image getImage(String imageName) {

        if (images.get(imageName) == null) {
            ClassLoader cl = JDUtilities.getJDClassLoader();
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            return toolkit.getImage(cl.getResource("jd/img/" + imageName + ".png"));
        }
        return images.get(imageName);
    }

    /**
     * Fügt ein Bild zur Map hinzu
     * 
     * @param imageName
     *            Name des Bildes, daß hinzugefügt werden soll
     * @param image
     *            Das hinzuzufügende Bild
     */
    public static void addImage(String imageName, Image image) {
        Toolkit.getDefaultToolkit().prepareImage(image, -1, -1, null);
        images.put(imageName, image);
    }

    /**
     * Liefert das Basisverzeichnis für jD zurück.
     * 
     * @return ein File, daß das Basisverzeichnis angibt
     */
    public static File getJDHomeDirectoryFromEnvironment() {
        String envDir = null;// System.getenv("JD_HOME");
        File currentDir = null;

        String dir = Thread.currentThread().getContextClassLoader().getResource("jd/Main.class") + "";
        dir = dir.split("\\.jar\\!")[0] + ".jar";
        dir = dir.substring(Math.max(dir.indexOf("file:"), 0));
        try {
            currentDir = new File(new URI(dir));

            // logger.info(" App dir: "+currentDir+" -
            // "+System.getProperty("java.class.path"));
            if (currentDir.isFile()) currentDir = currentDir.getParentFile();

        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // logger.info("RunDir: " + currentDir);

        switch (getRunType()) {
        case RUNTYPE_LOCAL_JARED:
            envDir = currentDir.getAbsolutePath();
            // logger.info("JD_HOME from current Path :" + envDir);
            break;
        case RUNTYPE_LOCAL_ENV:
            envDir = System.getenv("JD_HOME");
            // logger.info("JD_HOME from environment:" + envDir);
            break;
        default:
            envDir = System.getProperty("user.home") + System.getProperty("file.separator") + ".jd_home/";
            // logger.info("JD_HOME from user.home :" + envDir);

        }

        if (envDir == null) {
            envDir = "." + System.getProperty("file.separator") + ".jd_home/";
            logger.info("JD_HOME from current directory:" + envDir);
        }
        File jdHomeDir = new File(envDir);
        if (!jdHomeDir.exists()) {
            jdHomeDir.mkdirs();
        }
        return jdHomeDir;
    }

    /**
     * Lädt eine Klasse aus dem homedir. UNd instanziert sie mit den gegebenen
     * arumenten
     * 
     * @param classPath
     * @param arguments
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Object getHomeDirInstance(String classPath, Object[] arguments) {
        classPath = classPath.replaceAll("\\.class", "");
        classPath = classPath.replaceAll("\\/", ".");
        classPath = classPath.replaceAll("\\\\", ".");
        logger.finer("Load Class form homedir: " + classPath);
        Class newClass = null;
        // Zuerst versuchen die klasse aus dem appdir zu laden( praktisch zum
        // entwicklen solcher klassen)
        try {
            newClass = Class.forName(classPath);
        } catch (ClassNotFoundException e1) {
        }
        // Falls das nicht geklappt hat wird die klasse im homedir gesucht
        if (newClass == null) {
            try {
                String url = urlEncode(new File((getJDHomeDirectoryFromEnvironment().getAbsolutePath())).toURI().toURL().toString());
                URLClassLoader cl = new URLClassLoader(new URL[] { new URL(url) }, Thread.currentThread().getContextClassLoader());
                newClass = Class.forName(classPath, true, cl);
            } catch (ClassNotFoundException e) {

                e.printStackTrace();
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            // newClass = Class.forName(classPath);
            Class[] classes = new Class[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                classes[i] = arguments[i].getClass();
            }
            Constructor con = newClass.getConstructor(classes);
            return con.newInstance(arguments);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {

            e.printStackTrace();
        } catch (IllegalArgumentException e) {

            e.printStackTrace();
        } catch (InstantiationException e) {

            e.printStackTrace();
        } catch (IllegalAccessException e) {

            e.printStackTrace();
        } catch (InvocationTargetException e) {

            e.printStackTrace();
        } catch (Exception e) {

            e.printStackTrace();
        }
        return null;
    }

    /**
     * Schreibt das Home Verzeichnis in den Webstart Cache
     * 
     * @param newHomeDir
     *            Das neue JD-HOME
     */
    @SuppressWarnings("unchecked")
    public static void writeJDHomeDirectoryToWebStartCookie(String newHomeDir) {
        try {
            Class webStartHelper = Class.forName("jd.JDWebStartHelper");
            Method method = webStartHelper.getDeclaredMethod("writeJDHomeDirectoryToWebStartCookie", new Class[] { String.class });
            String homeDir = (String) method.invoke(webStartHelper, newHomeDir);
            setHomeDirectory(homeDir);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static SubConfiguration getSubConfig(String name) {
        if (subConfigs.containsKey(name)) return subConfigs.get(name);

        SubConfiguration cfg = new SubConfiguration(name);
        subConfigs.put(name, cfg);
        cfg.save();
        return cfg;

    }

    public static String xmltoStr(Document header) {
        Transformer transformer;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            // initialize StreamResult with File object to save to file
            StreamResult result = new StreamResult(new StringWriter());

            DOMSource source = new DOMSource(header);

            transformer.transform(source, result);

            String xmlString = result.getWriter().toString();
            return xmlString;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Liefert einen URLClassLoader zurück, um Dateien aus dem Stammverzeichnis
     * zu laden
     * 
     * @return URLClassLoader
     */
    public static JDClassLoader getJDClassLoader() {
        if (jdClassLoader == null) {
            File homeDir = getJDHomeDirectoryFromEnvironment();
            // String url = null;
            // Url Encode des pfads für den Classloader
            logger.info("Create Classloader: for: " + homeDir.getAbsolutePath());
            jdClassLoader = new JDClassLoader(homeDir.getAbsolutePath(), Thread.currentThread().getContextClassLoader());

        }
        return jdClassLoader;
    }

    /**
     * Diese Methode erstellt einen neuen Captchadialog und liefert den
     * eingegebenen Text zurück.
     * 
     * @param controller
     *            Der Controller
     * @param plugin
     *            Das Plugin, das dieses Captcha fordert
     * @param host
     *            der Host von dem die Methode verwendet werden soll
     * @param file
     * @return Der vom Benutzer eingegebene Text
     */
    public static String getCaptcha(Plugin plugin, String method, File file, boolean forceJAC) {
        String host;
        if (method == null) {
            host = plugin.getHost();
        } else {
            host = method;
        }

        JDUtilities.getController().fireControlEvent(new ControlEvent(plugin, ControlEvent.CONTROL_CAPTCHA_LOADED, file));

        logger.info("JAC has Method for: " + host + ": " + JAntiCaptcha.hasMethod(getJACMethodsDirectory(), host));
        if (forceJAC || (JAntiCaptcha.hasMethod(getJACMethodsDirectory(), host) && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_JAC_METHODS + "_" + host, true) && !configuration.getBooleanProperty(Configuration.PARAM_CAPTCHA_JAC_DISABLE, false))) {
            if (!JAntiCaptcha.hasMethod(getJACMethodsDirectory(), host) || !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_JAC_METHODS + "_" + host, true)) { return null; }

            JFrame jf = new JFrame();
            Image captchaImage = new JFrame().getToolkit().getImage(file.getAbsolutePath());
            MediaTracker mediaTracker = new MediaTracker(jf);
            mediaTracker.addImage(captchaImage, 0);
            try {
                mediaTracker.waitForID(0);
            } catch (InterruptedException e) {
                return null;
            }
            mediaTracker.removeImage(captchaImage);
            JAntiCaptcha jac = new JAntiCaptcha(getJACMethodsDirectory(), host);
            Captcha captcha = jac.createCaptcha(captchaImage);
            String captchaCode = jac.checkCaptcha(captcha);
            logger.info("Code: " + captchaCode);
            logger.info("Vality: " + captcha.getValityPercent());
            logger.info("Object Detection: " + captcha.isPerfectObjectDetection());
            // ScrollPaneWindow window = new ScrollPaneWindow("Captcha");

            plugin.setLastCaptcha(captcha);
            String code = null;
            plugin.setCaptchaDetectID(Plugin.CAPTCHA_JAC);
            LetterComperator[] lcs = captcha.getLetterComperators();

            double vp = 0.0;
            if (lcs == null) {
                vp = 100.0;
            } else {
                for (int i = 0; i < lcs.length; i++) {
                    // window.setImage(i, 0, lcs[i].getB().getImage(3));
                    // window.setImage(i, 1, lcs[i].getA().getImage(3));
                    if (lcs[i] == null) {
                        vp = 100.0;
                        break;
                    }
                    vp = Math.max(vp, lcs[i].getValityPercent());
                    // window.setText(i, 2, lcs[i].getValityPercent());
                    // window.setText(i, 3, lcs[i].getDecodedValue());
                    // window.setText(i, 4, lcs[i].getB().getPixelString());
                }
            }
            // window.pack();
            logger.info("worst letter: " + vp);
            if (plugin.useUserinputIfCaptchaUnknown() && vp > (double) JDUtilities.getSubConfig("JAC").getIntegerProperty(Configuration.AUTOTRAIN_ERROR_LEVEL, 18)) {
                plugin.setCaptchaDetectID(Plugin.CAPTCHA_USER_INPUT);
                code = getController().getCaptchaCodeFromUser(plugin, file, captchaCode);
            } else {
                return captchaCode;
            }

            if (code != null && code.equals(captchaCode)) return captchaCode;

            return code;
        }

        else {
            return getController().getCaptchaCodeFromUser(plugin, file, null);
        }
    }

    // /**
    // * Fügt einen PluginListener hinzu
    // *
    // * @param listener
    // */
    // public static void registerListenerPluginsForDecrypt(PluginListener
    // listener) {
    // Iterator<PluginForDecrypt> iterator = pluginsForDecrypt.iterator();
    // while (iterator.hasNext()) {
    // iterator.next().addPluginListener(listener);
    // }
    // }
    // /**
    // * Fügt einen PluginListener hinzu
    // *
    // * @param listener
    // */
    // public static void registerListenerPluginsForHost(PluginListener
    // listener) {
    // Iterator<PluginForHost> iterator = pluginsForHost.iterator();
    // while (iterator.hasNext()) {
    // iterator.next().addPluginListener(listener);
    // }
    // }
    // /**
    // * Fügt einen PluginListener hinzu
    // *
    // * @param listener TODO: unused
    // */
    // public static void registerListenerPluginsForSearch(PluginListener
    // listener) {
    // Iterator<PluginForSearch> iterator = pluginsForSearch.iterator();
    // while (iterator.hasNext()) {
    // iterator.next().addPluginListener(listener);
    // }
    // }
    // /**
    // * Fügt einen PluginListener hinzu TODO: unused
    // *
    // * @param listener
    // */
    // public static void registerListenerPluginsForContainer(PluginListener
    // listener) {
    // Iterator<PluginForContainer> iterator = pluginsForContainer.iterator();
    // while (iterator.hasNext()) {
    // iterator.next().addPluginListener(listener);
    // }
    // }
    // /*
    // * TODO: unused
    // */
    // public static void registerListenerPluginsOptional(PluginListener
    // listener) {
    // Iterator<String> iterator = pluginsOptional.keySet().iterator();
    // while (iterator.hasNext()) {
    // pluginsOptional.get(iterator.next()).addPluginListener(listener);
    // }
    // }
    /**
     * Lädt ein Objekt aus einer Datei
     * 
     * @param frame
     *            Ein übergeordnetes Fenster
     * @param fileInput
     *            Falls das Objekt aus einer bekannten Datei geladen werden
     *            soll, wird hier die Datei angegeben. Falls nicht, kann der
     *            Benutzer über einen Dialog eine Datei aussuchen
     * @param asXML
     *            Soll das Objekt von einer XML Datei aus geladen werden?
     * @return Das geladene Objekt
     */
    public static Object loadObject(JFrame frame, File fileInput, boolean asXML) {
        // logger.info("load file: " + fileInput + " (xml:" + asXML + ")");
        Object objectLoaded = null;
        if (fileInput == null) {
            JFileChooser fileChooserLoad = new JFileChooser();
            if (currentDirectory != null) fileChooserLoad.setCurrentDirectory(currentDirectory);
            if (fileChooserLoad.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                fileInput = fileChooserLoad.getSelectedFile();
                currentDirectory = fileChooserLoad.getCurrentDirectory();
            }
        }
        if (fileInput != null) {
            // String hash = getLocalHash(fileInput);
            try {
                FileInputStream fis = new FileInputStream(fileInput);
                if (asXML) {
                    XMLDecoder xmlDecoder = new XMLDecoder(new BufferedInputStream(fis));
                    objectLoaded = xmlDecoder.readObject();
                    xmlDecoder.close();
                } else {
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    objectLoaded = ois.readObject();
                    ois.close();
                }
                // Object15475dea4e088fe0e9445da30604acd1
                // Object80d11614908074272d6b79abe91eeca1
                // logger.info("Loaded Object (" + hash + "): ");
                
            
                
                return objectLoaded;
            } catch (ClassNotFoundException e) {
                logger.severe(e.getMessage());
                // e.printStackTrace();
            } catch (FileNotFoundException e) {
                logger.severe(e.getMessage());
            } catch (IOException e) {
                logger.severe(e.getMessage());
            } catch (Exception e) {
                logger.severe(e.getMessage());
            }
        }
        return null;
    }

    /**
     * Speichert ein Objekt
     * 
     * @param frame
     *            ein Fenster
     * @param objectToSave
     *            Das zu speichernde Objekt
     * @param fileOutput
     *            Das File, in das geschrieben werden soll. Falls das File ein
     *            Verzeichnis ist, wird darunter eine Datei erstellt Falls keins
     *            angegeben wird, soll der Benutzer eine Datei auswählen
     * @param name
     *            Dateiname
     * @param extension
     *            Dateiendung (mit Punkt)
     * @param asXML
     *            Soll das Objekt in eine XML Datei gespeichert werden?
     */
    public static void saveObject(JFrame frame, Object objectToSave, File fileOutput, String name, String extension, boolean asXML) {
        String hashPre;
        if (fileOutput == null) {
            JDFileFilter fileFilter = new JDFileFilter(name, extension, true);
            JFileChooser fileChooserSave = new JFileChooser();
            fileChooserSave.setFileFilter(fileFilter);
            fileChooserSave.setSelectedFile(fileFilter.getFile());
            if (currentDirectory != null) fileChooserSave.setCurrentDirectory(currentDirectory);
            if (fileChooserSave.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                fileOutput = fileChooserSave.getSelectedFile();
                currentDirectory = fileChooserSave.getCurrentDirectory();
            }
        }
        // logger.info("save file: " + fileOutput + " object: " + objectToSave);
        if (fileOutput != null) {
            if (fileOutput.isDirectory()) {
                fileOutput = new File(fileOutput, name + extension);

            }
            hashPre = getLocalHash(fileOutput);
            if (fileOutput.exists()) fileOutput.delete();
            try {
                FileOutputStream fos = new FileOutputStream(fileOutput);
                if (asXML) {
                    XMLEncoder xmlEncoder = new XMLEncoder(new BufferedOutputStream(fos));
                    xmlEncoder.writeObject(objectToSave);
                    xmlEncoder.close();
                } else {
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    oos.writeObject(objectToSave);
                    oos.close();
                }
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String hashPost = getLocalHash(fileOutput);
            if (fileOutput.exists()) {
                // logger.info(fileOutput.delete()+"");
            }
            // logger.info(""+objectToSave);
            if (hashPost == null) {
                logger.severe("Schreibfehler: " + fileOutput + " Datei wurde nicht erstellt");
            } else if (hashPost.equals(hashPre)) {
                // logger.warning("Schreibvorgang: " + fileOutput + " Datei
                // wurde nicht überschrieben "+hashPost+" - "+hashPre);
            } else {
                // logger.finer("Schreibvorgang: " + fileOutput + " erfolgreich:
                // " + hashPost);
            }
          
            // logger.info(" -->"+JDUtilities.loadObject(null, fileOutput,
            // false));
        } else {
            logger.severe("Schreibfehler: Fileoutput: null");
        }
    } 

    /**
     * Formatiert Sekunden in das zeitformat stunden:minuten:sekunden
     * 
     * @param eta
     *            toURI().toURL();
     * @return formatierte Zeit
     */
    public static String formatSeconds(int eta) {
        int hours = eta / (60 * 60);
        eta -= hours * 60 * 60;
        int minutes = eta / 60;
        int seconds = eta - minutes * 60;
        if (hours == 0) { return fillInteger(minutes, 2, "0") + ":" + fillInteger(seconds, 2, "0"); }
        return fillInteger(hours, 2, "0") + ":" + fillInteger(minutes, 2, "0") + ":" + fillInteger(seconds, 2, "0");
    }

    /**
     * Hängt an i solange fill vorne an bis die zechenlänge von i gleich num ist
     * 
     * @param i
     * @param num
     * @param fill
     * @return aufgefüllte Zeichenkette
     */
    public static String fillInteger(int i, int num, String fill) {
        String ret = "" + i;
        while (ret.length() < num)
            ret = fill + ret;
        return ret;
    }

    /**
     * Liefert alle geladenen Plugins zum Entschlüsseln zurück
     * 
     * @return Plugins zum Entschlüsseln
     */
    public static Vector<PluginForDecrypt> getPluginsForDecrypt() {
        return pluginsForDecrypt;
    }

    /**
     * Liefert alle geladenen Plugins zum Laden von Containerdateien zurück
     * 
     * @return Plugins zum Laden von Containerdateien
     */
    public static Vector<PluginForContainer> getPluginsForContainer() {
        return pluginsForContainer;
    }

    /**
     * Liefert alle Plugins zum Downloaden von einem Anbieter zurück.
     * 
     * @return
     */
    public static Vector<PluginForHost> getUnsortedPluginsForHost() {
        return pluginsForHost;
    }

    /**
     * Liefert alle Plugins zum Downloaden von einem Anbieter zurück. Die liste
     * wird dabei sortiert zurückgegeben
     * 
     * @return Plugins zum Downloaden von einem Anbieter
     */
    @SuppressWarnings("unchecked")
    public static Vector<PluginForHost> getPluginsForHost() {
        // return pluginsForHost;

        Vector<PluginForHost> plgs = new Vector<PluginForHost>();
        if (pluginsForHost != null) plgs.addAll(pluginsForHost);
        Vector<PluginForHost> pfh = new Vector<PluginForHost>();
        Vector<String> priority = (Vector<String>) configuration.getProperty(Configuration.PARAM_HOST_PRIORITY, new Vector<String>());
        for (int i = 0; i < priority.size(); i++) {
            for (int b = plgs.size() - 1; b >= 0; b--) {
                if (plgs.get(b).getHost().equalsIgnoreCase(priority.get(i))) {
                    PluginForHost plg = plgs.remove(b);
                    pfh.add(plg);
                    break;
                }
            }
        }
        pfh.addAll(plgs);
        return pfh;
    }

    /**
     * Liefert alle optionalen Plugins zurücl
     * 
     * @return Alle optionalen Plugins
     */
    public static HashMap<String, PluginOptional> getPluginsOptional() {
        return pluginsOptional;
    }

    /**
     * Gibt den MD5 hash eines Strings zurück
     * 
     * @param arg
     * @return MD% hash von arg
     */
    public static String getMD5(String arg) {
        try {
            MessageDigest md = MessageDigest.getInstance("md5");
            byte[] digest = md.digest(arg.getBytes());
            String ret = "";
            String tmp;
            for (byte d : digest) {
                tmp = Integer.toHexString(d & 0xFF);
                ret += (tmp.length() < 2) ? "0" + tmp : tmp;
            }
            return ret;
        } catch (NoSuchAlgorithmException e) {
        }
        return "";
    }

    /**
     * Sucht ein passendes Plugin für einen Anbieter
     * 
     * @param host
     *            Der Host, von dem das Plugin runterladen kann
     * @return Ein passendes Plugin oder null
     */
    public static PluginForHost getPluginForHost(String host) {
        for (int i = 0; i < pluginsForHost.size(); i++) {
            if (pluginsForHost.get(i).getHost().equals(host)) return pluginsForHost.get(i);
        }
        return null;
    }

    /**
     * Sucht ein passendes Plugin für ein Containerfile
     * 
     * @param container
     *            Der Host, von dem das Plugin runterladen kann
     * @param containerPath
     * @return Ein passendes Plugin oder null
     */
    public static PluginForContainer getPluginForContainer(String container, String containerPath) {
        if (containerPath != null && containerPlugins.containsKey(containerPath)) return containerPlugins.get(containerPath);
        PluginForContainer ret = null;
        for (int i = 0; i < pluginsForContainer.size(); i++) {
            if (pluginsForContainer.get(i).getHost().equals(container)) {
                try {
                    ret = pluginsForContainer.get(i).getClass().newInstance();
                    if (containerPath != null) containerPlugins.put(containerPath, ret);
                    return ret;
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * @return Configuration instanz
     */
    public static Configuration getConfiguration() {
        return configuration;
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
     * @author astaldo
     * @return homeDirectory
     */
    public static String getHomeDirectory() {
        return homeDirectory;
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
     * Gibt ein FileOebject zu einem Resourcstring zurück
     * 
     * @author JD-Team
     * @param resource
     *            Ressource, die geladen werden soll
     * @return File zu arg
     */
    public static File getResourceFile(String resource) {
        JDClassLoader cl = getJDClassLoader();
        if (cl == null) {
            logger.severe("Classloader ==null: ");
            return null;
        }
        URL clURL = getJDClassLoader().getResource(resource);

        if (clURL != null) {
            try {
                return new File(clURL.toURI());
            } catch (URISyntaxException e) {
            }
        }
        return null;
    }

    /**
     * public static String getLocalHash(File f) Gibt einen MD% Hash der file
     * zurück
     * 
     * @author JD-Team
     * @param f
     * @return Hashstring Md5
     */
    public static String getLocalHash(File f) {
        try {
            if (!f.exists()) return null;
            MessageDigest md;
            md = MessageDigest.getInstance("md5");
            byte[] b = new byte[1024];
            InputStream in = new FileInputStream(f);
            for (int n = 0; (n = in.read(b)) > -1;) {
                md.update(b, 0, n);
            }
            byte[] digest = md.digest();
            String ret = "";
            for (int i = 0; i < digest.length; i++) {
                String tmp = Integer.toHexString(digest[i] & 0xFF);
                if (tmp.length() < 2) tmp = "0" + tmp;
                ret += tmp;
            }
            in.close();
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @author JD-Team Macht ein urlRawEncode und spart dabei die angegebenen
     *         Zeichen aus
     * @param str
     * @return str URLCodiert
     */
    @SuppressWarnings("deprecation")
    public static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str);
        } catch (Exception e) {
            // TODO: handle exception
        }
        return null;
        /*
         * 
         * 
         * if (str == null) return str; String allowed =
         * "1234567890QWERTZUIOPASDFGHJKLYXCVBNMqwertzuiopasdfghjklyxcvbnm-_.?/\\:&=;";
         * String ret = ""; String l; int i; for (i = 0; i < str.length(); i++) {
         * char letter = str.charAt(i); if (allowed.indexOf(letter) >= 0) { ret +=
         * letter; } else { l = Integer.toString(letter, 16); ret += "%" +
         * (l.length() == 1 ? "0" + l : l); } }
         */

    }

    /**
     * "http://rapidshare.com&#x2F;&#x66;&#x69;&#x6C;&#x65;&#x73;&#x2F;&#x35;&#x34;&#x35;&#x34;&#x31;&#x34;&#x38;&#x35;&#x2F;&#x63;&#x63;&#x66;&#x32;&#x72;&#x73;&#x64;&#x66;&#x2E;&#x72;&#x61;&#x72;";
     * Wandelt alle hexkodierten zeichen in diesem Format in normalen text um
     * 
     * @param str
     * @return decoded string
     */
    public static String htmlDecode(String str) {
        // http://rs218.rapidshare.com/files/&#0052;&#x0037;&#0052;&#x0034;&#0049;&#x0032;&#0057;&#x0031;/STE_S04E04.Borderland.German.dTV.XviD-2Br0th3rs.part1.rar
        if (str == null) return null;
        String pattern = "\\&\\#x(.*?)\\;";
        for (Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(str); r.find();) {
            if (r.group(1).length() > 0) {
                char c = (char) Integer.parseInt(r.group(1), 16);
                str = str.replaceFirst("\\&\\#x(.*?)\\;", c + "");
            }
        }
        pattern = "\\&\\#(.*?)\\;";
        for (Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(str); r.find();) {
            if (r.group(1).length() > 0) {
                char c = (char) Integer.parseInt(r.group(1), 10);
                str = str.replaceFirst("\\&\\#(.*?)\\;", c + "");
            }
        }
        try {
            str = URLDecoder.decode(str, "UTF-8");
        } catch (Exception e) {
        }
        return HTMLEntities.unhtmlentities(str);
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
                    logger.severe("Konnte Datei nicht löschen " + file);
                    return false;
                }
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
     * 
     */
    public static int getRunType() {

        try {

            Enumeration<URL> en = Thread.currentThread().getContextClassLoader().getResources("jd/Main.class");
            if (en.hasMoreElements()) {
                String root = en.nextElement().toString();
                // logger.info(root);
                if (root.indexOf("http://") >= 0) {
                    logger.info("Depr.: Webstart");
                    return RUNTYPE_WEBSTART;
                }
                if (root.indexOf("jar") >= 0) {
                    // logger.info("Default: Local jared");
                    return RUNTYPE_LOCAL_JARED;
                }
            }
            if (System.getenv("JD_HOME") != null) {
                if (new File(System.getenv("JD_HOME")).exists()) {
                    logger.info("Dev.: Local splitted from environment variable");
                    return RUNTYPE_LOCAL_ENV;
                }
            }
            // logger.info("Dev.: Local splitted");
            return RUNTYPE_LOCAL;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return 0;

    }

    /**
     * public static String getLocalFile(File file) Liest file über einen
     * bufferdReader ein und gibt den Inhalt asl String zurück
     * 
     * @param file
     * @return File Content als String
     */
    public static String getLocalFile(File file) {
        if (!file.exists()) return "";
        BufferedReader f;
        try {
            f = new BufferedReader(new FileReader(file));

            String line;
            StringBuffer ret = new StringBuffer();
            String sep = System.getProperty("line.separator");
            while ((line = f.readLine()) != null) {
                ret.append(line + sep);
            }
            f.close();
            return ret.toString();
        } catch (IOException e) {

            e.printStackTrace();
        }
        return "";
    }

    /**
     * Zum Kopieren von einem Ort zum anderen
     * 
     * @param in
     * @param out
     * @throws IOException
     */
    public static boolean copyFile(File in, File out) {
        FileChannel inChannel = null;

        FileChannel outChannel = null;
        try {
            if (!out.exists()) {
                out.getParentFile().mkdirs();
                out.createNewFile();
            }
            inChannel = new FileInputStream(in).getChannel();

            outChannel = new FileOutputStream(out).getChannel();

            inChannel.transferTo(0, inChannel.size(), outChannel);

            return true;
        } catch (FileNotFoundException e1) {

            e1.printStackTrace();
            if (inChannel != null) try {
                inChannel.close();

                if (outChannel != null) outChannel.close();
            } catch (IOException e) {

                e.printStackTrace();
                return false;
            }
            return false;
        } catch (IOException e) {

            e.printStackTrace();
        }
        try {
            if (inChannel != null) inChannel.close();

            if (outChannel != null) outChannel.close();
        } catch (IOException e) {

            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * @author JD-Team
     * @param str
     * @return str als UTF8Decodiert
     */
    public static String UTF8Decode(String str) {
        try {
            return new String(str.getBytes(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @author JD-Team
     * @param str
     * @return str als UTF8 Kodiert
     */
    public static String UTF8Encode(String str) {
        try {
            return new String(str.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Setzt das Homedirectory und erstellt es notfalls neu
     * 
     * @param homeDirectory
     */
    public static void setHomeDirectory(String homeDirectory) {
        JDUtilities.homeDirectory = homeDirectory;
        homeDirectoryFile = new File(homeDirectory);
        if (!homeDirectoryFile.exists()) homeDirectoryFile.mkdirs();
    }

    /**
     * Lädt eine url lokal herunter
     * 
     * @param file
     * @param urlString
     * @return Erfolg true/false
     */
    public static boolean download(File file, String urlString) {
        try {
            urlString = URLDecoder.decode(urlString, "UTF-8");
            URL url = new URL(urlString);
            HTTPConnection con = new HTTPConnection(url.openConnection());
            return download(file, con);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean downloadBinary(String filepath, String fileurl) {

        try {
            fileurl = urlEncode(fileurl.replaceAll("\\\\", "/"));
            File file = new File(filepath);
            if (file.isFile()) {
                if (!file.delete()) {
                    logger.info("Konnte Datei nicht löschen " + file);
                    return false;
                }

            }

            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();

            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file, true));
            fileurl = URLDecoder.decode(fileurl, "UTF-8");

            URL url = new URL(fileurl);
            HTTPConnection con = new HTTPConnection(url.openConnection());

            BufferedInputStream input = new BufferedInputStream(con.getInputStream());

            byte[] b = new byte[1024];
            int len;
            while ((len = input.read(b)) != -1) {
                output.write(b, 0, len);
            }
            output.close();
            input.close();

            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;

        }

    }

    /**
     * Lädt über eine URLConnection eine datei ehrunter. Zieldatei ist file.
     * 
     * @param file
     * @param con
     * @return Erfolg true/false
     */
    public static boolean download(File file, HTTPConnection con) {
        try {
            if (file.isFile()) {
                if (!file.delete()) {
                    logger.severe("Konnte Datei nicht überschreiben " + file);
                    return false;
                }
            }
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
            logger.info(" file" + file + " - " + con.getContentLength());
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file, true));
            BufferedInputStream input = new BufferedInputStream(con.getInputStream());
            byte[] b = new byte[1024];
            int len;
            while ((len = input.read(b)) != -1) {
                output.write(b, 0, len);
            }
            output.close();
            input.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * TODO: Serverpfad in de Config aufnehmen Gleicht das homedir mit dem
     * server ab. Der Serverpfad steht noch in WebUpdater.java
     * 
     * @author JD-Team
     * @return Anzahl der aktualisierten Files
     */
    public static int doWebupdate() {
        WebUpdater wu = new WebUpdater(null);
        wu.run();
        return wu.getUpdatedFiles();
    }
    /**
     * Überprüft ob eine IP gültig ist. das verwendete Pattern aknn in der config editiert werden.
     * @param ip
     * @return
     */
public static boolean validateIP(String ip){
    return Pattern.compile(getConfiguration().getStringProperty(Configuration.PARAM_GLOBAL_IP_MASK,"\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).)" + "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b")).matcher(ip).matches();
} 
    /**
     * Prüft anhand der Globalen IP Check einstellungen die IP
     * 
     * @return ip oder /offline
     */
    public static String getIPAddress() {
        if (getSubConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
            logger.finer("IP Check is disabled. return current Milliseconds");
            return System.currentTimeMillis() + "";
        }

        String site = getSubConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_CHECK_SITE, "http://checkip.dyndns.org");
        String patt = getSubConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_PATTERN, "Address\\: ([0-9.]*)\\<\\/body\\>");

        try {
            logger.finer("IP Check via " + site);
            RequestInfo requestInfo = Plugin.getRequest(new URL(site), null, null, true);
            Pattern pattern = Pattern.compile(patt);
            Matcher matcher = pattern.matcher(requestInfo.getHtmlCode());
            if (matcher.find()) {
                if (matcher.groupCount() > 0) {
                    return matcher.group(1);
                } else {
                    logger.severe("Primary bad Regex: " + patt);

                }
            }
            logger.info("Primary IP Check failed. Ip not found via regex: " + patt + " on " + site + " htmlcode: " + requestInfo.getHtmlCode());

        }

        catch (Exception e1) {
            logger.severe("url not found. " + e1.toString());

        }

        try {
            site = "http://jdownloaderipcheck.ath.cx";

            logger.finer("IP Check via jdownloaderipcheck.ath.cx");
            RequestInfo ri;

            ri = Plugin.getRequest(new URL(site), null, null, true);
            String ip = Plugin.getSimpleMatch(ri.getHtmlCode(), "<ip>°</ip>", 0);
            if (ip == null) {
                logger.info("Sec. IP Check failed.");
                return "offline";
            } else {
                logger.info("Sec. IP Check success. PLease Check Your IP Check settings");
                return ip;
            }
        }

        catch (Exception e1) {
            logger.severe("url not found. " + e1.toString());
            logger.info("Sec. IP Check failed.");

        }

        return "offline";
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
        if (command == null || command.trim().length() == 0) {
            logger.severe("Execute Parameter error: No Command");
            return "";
        }
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
        logger.info("RUN: " + tmp);
        ProcessBuilder pb = new ProcessBuilder(params);
        if (runIn != null && runIn.length() > 0) {
            if (new File(runIn).exists()) {
                pb.directory(new File(runIn));
            } else {
                logger.severe("Working drectory " + runIn + " does not exist!");
            }
        }

        Process process;

        try {
            logger.finer("Start " + par + " in " + runIn + " wait " + waitForReturn);
            process = pb.start();
            if (waitForReturn > 0 || waitForReturn < 0) {
                long t = System.currentTimeMillis();
                // while (true) {
                // try {
                // process.exitValue();
                // break;
                // } catch (Exception e) {
                // long dif=System.currentTimeMillis() - t;
                // if (waitForReturn > 0 && dif> waitForReturn * 1000) {
                // logger.severe(command + ": Prozess ist nach " + waitForReturn
                // + " Sekunden nicht beendet worden. Breche ab.");
                // process.destroy();
                // }
                // }
                // }
                // Scanner s = new
                // Scanner(process.getInputStream()).useDelimiter("\\Z");
                // String ret = "";
                // while (s.hasNext())
                // ret += s.next();

                BufferedReader f;
                StringBuffer s = new StringBuffer();
                f = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = f.readLine()) != null) {

                    s.append(line + "\r\n");
                    if ((System.currentTimeMillis() - t) > (1000l * waitForReturn)) {
                        logger.severe(command + ": Prozess ist nach " + waitForReturn + " Sekunden nicht beendet worden. Breche ab.");

                        break;
                    }
                }
                try {
                    int l = process.exitValue();
                } catch (Exception e) {
                    process.destroy();
                }

                return s.toString();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe("Error executing " + command + ": " + e.getLocalizedMessage());
            return null;
        }
    }

    public static String runTestCommand(String command, String[] parameter, String runIn, int waitForReturn, boolean returnValue) {
        if (command == null || command.trim().length() == 0) {
            logger.severe("Execute Parameter error: No Command");
            return "";
        }
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
        logger.info("RUN: " + tmp);
        ProcessBuilder pb = new ProcessBuilder(params);
        if (runIn != null && runIn.length() > 0) {
            if (new File(runIn).exists()) {
                pb.directory(new File(runIn));
            } else {
                logger.severe("Working drectory " + runIn + " does not exist!");
            }
        }

        Process process;

        try {
            logger.finer("Start " + par + " in " + runIn + " wait " + waitForReturn + " trace: " + returnValue);
            process = pb.start();
            if (waitForReturn > 0 || waitForReturn < 0) {
                long t = System.currentTimeMillis();
                while (true) {
                    try {
                        process.exitValue();
                        break;
                    } catch (Exception e) {
                        if (waitForReturn > 0 && System.currentTimeMillis() - t > waitForReturn * 1000) {
                            logger.severe(command + ": Prozess ist nach " + waitForReturn + " Sekunden nicht beendet worden. Breche ab.");
                            process.destroy();
                        }
                    }
                }
                String ret = "";
                if (returnValue) {
                    Scanner s = new Scanner(process.getInputStream()).useDelimiter("\\Z");

                    while (s.hasNext())
                        ret += s.next();
                }
                return ret;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe("Error executing " + command + ": " + e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Gibt den verwendeten Controller zurück
     * 
     * @return gerade verwendete controller-instanz
     */
    public static JDController getController() {
        return controller;
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

    /**
     * @return Gibt die verwendete java Version als Double Value zurück. z.B.
     *         1.603
     */
    public static Double getJavaVersion() {
        String version = System.getProperty("java.version");
        int majorVersion = JDUtilities.filterInt(version.substring(0, version.indexOf(".")));
        int subversion = JDUtilities.filterInt(version.substring(version.indexOf(".") + 1));
        return Double.parseDouble(majorVersion + "." + subversion);
    }

    /**
     * Ersetzt die Platzhalter in einem String
     * 
     * @param command
     * @return Neuer String mit ersetzen Platzhaltern
     */
    public static String replacePlaceHolder(String command) {
        if (controller == null) return command;
        command = command.replaceAll("\\%LASTFILE", controller.getLastFinishedFile());
        command = command.replaceAll("\\%CAPTCHAIMAGE", controller.getLastCaptchaImage());
        return command;
    }

    /**
     * Formatiert Byes in einen MB String [MM.MM MB]
     * 
     * @param downloadMax
     * @return MegaByte Formatierter String
     */
    public static String formatBytesToMB(long downloadMax) {
        DecimalFormat c = new DecimalFormat("0.00");
        return c.format(downloadMax / (1024.0 * 1024.0)) + " MB";
    }

    /**
     * GIbt den Integer der sich in src befindet zurück. alle nicht
     * integerzeichen werden ausgefiltert
     * 
     * @param src
     * @return Integer in src
     */
    public static int filterInt(String src) {
        try {
            return Integer.parseInt(filterString(src, "1234567890"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static long getCRC(File file) {

        try {

            CheckedInputStream cis = null;
            long fileSize = 0;
            try {
                // Computer CRC32 checksum
                cis = new CheckedInputStream(new FileInputStream(file), new CRC32());

                fileSize = file.length();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return 0;
            }

            byte[] buf = new byte[128];
            while (cis.read(buf) >= 0) {
            }

            long checksum = cis.getChecksum().getValue();
            return checksum;

        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

    }

    /**
     * Filtert alle nicht lesbaren zeichen aus str
     * 
     * @param str
     * @return
     */
    public static String filterString(String str) {
        String allowed = "QWERTZUIOPÜASDFGHJKLÖÄYXCVBNMqwertzuiopasdfghjklyxcvbnm;:,._-&%(){}#~+ 1234567890<>='\"/";
        return filterString(str, allowed);
    }

    /**
     * Filtert alle zeichen aus str die in filter nicht auftauchen
     * 
     * @param str
     * @param filter
     * @return
     */
    public static String filterString(String str, String filter) {
        if (str == null || filter == null) return "";

        byte[] org = str.getBytes();
        byte[] mask = filter.getBytes();
        byte[] ret = new byte[org.length];
        int count = 0;
        int i;
        for (i = 0; i < org.length; i++) {
            byte letter = org[i];
            for (int t = 0; t < mask.length; t++) {
                if (letter == mask[t]) {
                    ret[count] = letter;
                    count++;
                    break;
                }
            }
        }
        return new String(ret).trim();
    }

    /**
     * Untersucht zwei String, ob zwei String ähnlich anfangen. Der
     * übereinstimmende Text wird dann zurückgegeben
     * 
     * @param a
     *            Erster String, der vergleicht werden soll
     * @param b
     *            Zweiter String, der vergleicht werden soll
     * @return Übereinstimmender Text
     */
    public static String getEqualString(String a, String b) {
        String first, second;
        int index = 0;
        if (a.length() <= b.length()) {
            first = a.toLowerCase();
            second = b.toLowerCase();
        } else {
            first = b;
            second = a;
        }
        for (int i = 0; i < first.length(); i++) {
            if (first.charAt(i) == second.charAt(i))
                index = i;
            else
                break;
        }
        if (index > 0)
            return first.substring(0, index + 1);
        else
            return "";
    }

    public static String getJDTitle() {
        String ret = JDUtilities.JD_TITLE + " " + JDUtilities.JD_VERSION + JDUtilities.getRevision() + " (" + JDUtilities.getLastChangeDate() + " " + JDUtilities.getLastChangeTime() + ") - " + JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1);
        if (JDUtilities.getController() != null && JDUtilities.getController().getWaitingUpdates() != null && JDUtilities.getController().getWaitingUpdates().size() > 0) {
            ret += "  " + JDLocale.L("gui.mainframe.title.updatemessage", "-->UPDATES VERFÜGBAR: ") + JDUtilities.getController().getWaitingUpdates().size();

        }
        if (getSubConfig("WEBUPDATE").getBooleanProperty("WEBUPDATE_BETA", false)) { return "[BETA!] " + ret; }
        return ret;
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
    public static void appendInfoToFilename(final Plugin plugin, File file, String captchaCode, boolean isGood) {
        String dest = file.getAbsolutePath();
        String isGoodText;
        if (isGood)
            isGoodText = "_GOOD";
        else
            isGoodText = "_BAD";
        int idx = dest.lastIndexOf('.');
        dest = dest.substring(0, idx) + "_" + captchaCode.toUpperCase() + isGoodText + dest.substring(idx);
        final File file2 = new File(dest);
        file.renameTo(file2);
        /*
         * if(!isGood) { new Thread(new Runnable(){
         * 
         * public void run() { Upload.uploadToCollector(plugin, file2);
         * 
         * }}).start(); }
         */
    }

    public static Locale getLocale() {
        return locale;
    }

    public static void setLocale(Locale locale) {
        JDUtilities.locale = locale;
    }

    /***************************************************************************
     * Gibt die Endung einer FIle zurück oder null
     * 
     * @param ret
     * @return
     */
    public static String getFileExtension(File ret) {
        if (ret == null) return null;
        String str = ret.getAbsolutePath();

        int i3 = str.lastIndexOf(".");

        if (i3 > 0) { return str.substring(i3 + 1); }
        return null;
    }

    /**
     * Liefert die Klasse zurück, mit der Nachrichten ausgegeben werden können
     * Falls dieser Logger nicht existiert, wird ein neuer erstellt
     * 
     * @return LogKlasse
     */
    public static Logger getLogger() {
        if (logger == null) {

            logger = Logger.getLogger(LOGGER_NAME);
            Formatter formatter = new LogFormatter();
            logger.setUseParentHandlers(false);
            Handler console = new ConsoleHandler();

            console.setLevel(Level.ALL);
            console.setFormatter(formatter);
            logger.addHandler(console);

            logger.setLevel(Level.ALL);
            logger.addHandler(new Handler() {
                public void publish(LogRecord logRecord) {
                    // System.out.println(logRecord.getLevel() + ":");
                    // System.out.println(logRecord.getSourceClassName() + ":");
                    // System.out.println(logRecord.getSourceMethodName() +
                    // ":");
                    // System.out.println("<" + logRecord.getMessage() + ">");
                    // System.out.println("\n");
                    if (JDUtilities.getController() != null) JDUtilities.getController().fireControlEvent(ControlEvent.CONTROL_LOG_OCCURED, logRecord);
                }

                public void flush() {
                }

                public void close() {
                }
            });

            // logger.finer("Init Logger:" + LOGGER_NAME);
            // Leitet System.out zum Logger um.
            // final PrintStream err = System.err;
            OutputStream os = new OutputStream() {
                private StringBuffer buffer = new StringBuffer();

                @Override
                public void write(int b) throws IOException {
                    // err.write(b);
                    if ((b == 13 || b == 10)) {
                        if (buffer.length() > 0) {
                            JDUtilities.getLogger().severe(buffer.toString());
                            if (buffer.indexOf("OutOfMemoryError") >= 0) {
                                logger.finer("Restart");
                                boolean res;
                                res = getGUI().showConfirmDialog(JDLocale.L("gui.messages.outofmemoryerror", "An error ocured!\r\nJDownloader is out of memory. Restart recommended.\r\nPlease report this bug!"));

                                if (res) {

                                    JDUtilities.restartJD();
                                }
                            }

                        }
                        buffer = new StringBuffer();

                    } else {
                        buffer.append((char) b);

                    }

                }

            };
            System.setErr(new PrintStream(os));

        }
        return logger;
    }

    // public static boolean initFileLogger() {
    // try {
    // if (getConfiguration().getBooleanProperty(Configuration.PARAM_WRITE_LOG,
    // true)) {
    // Handler file_handler = new
    // FileHandler(getConfiguration().getStringProperty(Configuration.PARAM_WRITE_LOG_PATH,
    // getResourceFile("jd_log.txt").getAbsolutePath()));
    // logger.addHandler(file_handler);
    // logger.info("File Logger active: " +
    // getConfiguration().getStringProperty(Configuration.PARAM_WRITE_LOG_PATH,
    // getResourceFile("jd_log.txt").getAbsolutePath()));
    // return true;
    // }
    // }
    // catch (SecurityException e) {
    // e.printStackTrace();
    // }
    // catch (IOException e) {
    // e.printStackTrace();
    // }
    // return false;
    // }

    /**
     * Fügt dem Log eine Exception hinzu
     * 
     * @param e
     */
    public static void logException(Exception e) {
        getLogger().log(Level.SEVERE, "Exception", e);
        e.printStackTrace();
    }

    public static void logException(Error e) {
        getLogger().log(Level.SEVERE, "Error", e);
        e.printStackTrace();

    }

    /**
     * Gibt den Stacktrace einer exception zurück
     * 
     * @param e
     * @return
     */
    public static String getStackTraceForException(Exception e) {
        StringWriter sw = new StringWriter(2000);
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    /**
     * verschlüsselt string mit der übergebenen encryption (Containerpluginname
     * 
     * @param string
     * @param encryption
     * @return ciphertext
     */
    public static String[] encrypt(String string, String encryption) {
        Vector<PluginForContainer> pfc = JDUtilities.getPluginsForContainer();
        for (int i = 0; i < pfc.size(); i++) {
            if (pfc.get(i).getPluginName().equalsIgnoreCase(encryption)) { return pfc.get(i).encrypt(string); }
        }
        return null;

    }

    public static void setPluginForDecryptList(Vector<PluginForDecrypt> loadPlugins) {
        pluginsForDecrypt = loadPlugins;

    }

    public static void setPluginForHostList(Vector<PluginForHost> loadPlugins) {
        pluginsForHost = loadPlugins;

    }

    public static void setPluginForContainerList(Vector<PluginForContainer> loadPlugins) {
        pluginsForContainer = loadPlugins;

    }

    public static void setPluginOptionalList(HashMap<String, PluginOptional> loadPlugins) {
        pluginsOptional = loadPlugins;

    }

    public static void restartJD() {
        logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "-Xmx512m", "JDownloader.jar", }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
        System.exit(0);

    }

    public static void restartJD(String[] jdArgs) {

        String[] javaArgs = new String[] { "-jar", "-Xmx512m", "JDownloader.jar" };
        String[] finalArgs = new String[jdArgs.length + javaArgs.length];
        System.arraycopy(javaArgs, 0, finalArgs, 0, javaArgs.length);
        System.arraycopy(jdArgs, 0, finalArgs, javaArgs.length, jdArgs.length);

        logger.info(JDUtilities.runCommand("java", finalArgs, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
        System.exit(0);
    }

    public static void saveConfig() {
        JDUtilities.saveObject(null, JDUtilities.getConfiguration(), JDUtilities.getJDHomeDirectoryFromEnvironment(), JDUtilities.CONFIG_PATH.split("\\.")[0], "." + JDUtilities.CONFIG_PATH.split("\\.")[1], Configuration.saveAsXML);

    }

    public static UIInterface getGUI() {
        return JDUtilities.getController().getUiInterface();
    }

    public static String Base64Decode(String base64) {
        if (base64 == null) return null;
        try {
            byte[] plain = new BASE64Decoder().decodeBuffer(base64);
            if (JDUtilities.filterString(new String(plain)).length() < (plain.length / 1.5)) { return base64; }
            return new String(plain);
        } catch (IOException e) {
            return base64;
        }
    }

    public static void playMp3(File file) {
        AdvancedPlayer p;
        try {
            p = new AdvancedPlayer(new FileInputStream(file.getAbsolutePath()));

            p.play();
        } catch (FileNotFoundException e) {

            e.printStackTrace();
        } catch (JavaLayerException e) {

            e.printStackTrace();
        }
    }

    public static String Base64Encode(String plain) {

        if (plain == null) return null;
        String base64 = new BASE64Encoder().encode(plain.getBytes());
        base64 = JDUtilities.filterString(base64, "qwertzuiopasdfghjklyxcvbnmMNBVCXYASDFGHJKLPOIUZTREWQ1234567890=/");

        return base64;
    }

    public static String createContainerString(Vector<DownloadLink> downloadLinks, String encryption) {
        Vector<PluginForContainer> pfc = JDUtilities.getPluginsForContainer();
        for (int i = 0; i < pfc.size(); i++) {
            if (pfc.get(i).getPluginName().equalsIgnoreCase(encryption)) { return pfc.get(i).createContainerString(downloadLinks); }
        }
        return null;
    }

    public static String convertExceptionReadable(Exception e) {
        String s = e.getClass().getName().replaceAll("Exception", "");
        s = s.substring(s.lastIndexOf(".") + 1);
        String ret = "";
        String letter = null;
        for (int i = 0; i < s.length(); i++) {
            if ((letter = s.substring(i, i + 1)).equals(letter.toUpperCase())) {
                ret += " " + letter;
            } else {
                ret += letter;
            }
        }
        String message = e.getLocalizedMessage();

        return (message != null) ? (ret.trim() + ": " + message) : ret.trim();

    }

    public static String arrayToString(String[] a, String separator) {

        String result = "";

        if (a.length > 0) {

            result = a[0];

            for (int i = 1; i < a.length; i++) {
                result = result + separator + a[i];
            }

        }

        return result;

    }

    public static String formatKbReadable(int value) {

        DecimalFormat c = new DecimalFormat("0.00");
        ;
        if (value >= (1024 * 1024)) {

        return c.format(value / (1024 * 1024.0)) + " GB"; }
        if (value >= (1024)) {

        return c.format(value / 1024.0) + " MB"; }
        return value + " KB";

    }

    public static String getPercent(long downloadCurrent, long downloadMax) {
        DecimalFormat c = new DecimalFormat("0.00");
        ;

        return c.format(100.0 * downloadCurrent / (double) downloadMax) + "%";
    }

    /**
     * Sortiert einen Vector<HashMap<String, Comparable>>
     * 
     * @param packageData
     * @param key
     */
    @SuppressWarnings("unchecked")
    public static void sortHashVectorOn(Vector<HashMap<String, String>> packageData, final String key) {
        if (packageData.size() == 0 || !packageData.get(0).containsKey(key)) return;
        Collections.sort(packageData, new Comparator<HashMap<String, String>>() {
            public int compare(HashMap<String, String> a, HashMap<String, String> b) {
                return a.get(key).compareTo(b.get(key));
            }
        });

    }

    public static boolean removeDirectoryOrFile(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = removeDirectoryOrFile(new File(dir, children[i]));
                if (!success) { return false; }
            }
        }

        return dir.delete();
    }

    public static boolean sleep(int i) {
        try {
            Thread.sleep(i);
            return true;
        } catch (InterruptedException e) {
            return false;
        }

    }

    public static String validatePath(String fileOutput0) {
        if (OSDetector.isWindows()) {
            String hd="";
            if(new File(fileOutput0).isAbsolute()){
                hd=fileOutput0.substring(0,3);
                fileOutput0=fileOutput0.substring(3);
            }
            fileOutput0 = hd+fileOutput0.replaceAll("([<|>|\\||\"|:|\\*|\\?])+", "_");
        }

        return fileOutput0;
    }

    

}
