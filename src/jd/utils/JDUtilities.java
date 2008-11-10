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
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.Vector;
import java.util.concurrent.Semaphore;
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

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import jd.CPluginWrapper;
import jd.HostPluginWrapper;
import jd.JDClassLoader;
import jd.JDFileFilter;
import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Captcha;
import jd.config.Configuration;
import jd.config.DatabaseConnector;
import jd.config.SubConfiguration;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;

/**
 * @author astaldo/JD-Team
 */
public class JDUtilities {
    public static String LOGGER_NAME = "java_downloader";
    /**
     * Parametername für den Konfigpath
     */
    public static final String CONFIG_PATH = "jDownloader.config";

    /**
     * Die Konfiguration
     */
    private static Configuration configuration = new Configuration();

    private static DatabaseConnector dbconnect = null;

    private static HashMap<String, PluginsC> containerPlugins = new HashMap<String, PluginsC>();

    /**
     * Der DownloadController
     */
    private static JDController controller = null;

    /**
     * Das aktuelle Verzeichnis (Laden/Speichern)
     */
    private static File currentDirectory;

    /**
     * Damit werden die JARs rausgesucht
     */
    public static JDFileFilter filterJar = new JDFileFilter(null, ".jar", false);

    /**
     * Alle verfügbaren Bilder werden hier gespeichert
     */
    private static HashMap<String, Image> images = new HashMap<String, Image>();

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

    /**
     * Angaben über Spracheinstellungen
     */
    private static Locale locale = null;
    /**
     * Der Logger für Meldungen
     */
    public static Logger logger = JDUtilities.getLogger();

    public static final int RUNTYPE_LOCAL = 1;

    public static final int RUNTYPE_LOCAL_ENV = 3;

    public static final int RUNTYPE_LOCAL_JARED = 2;

    private static final int RUNTYPE_WEBSTART = 0;

    private static Vector<File> saveReadObject = new Vector<File>();

    private static HashMap<String, SubConfiguration> subConfigs = new HashMap<String, SubConfiguration>();

    /*
     * nur 1 UserIO Dialog gleichzeitig (z.b.PW,Captcha)
     */
    private static Semaphore userio_sem = new Semaphore(1);

    public static String getSimString(String a, String b) {
        String ret = "";
        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            if (a.charAt(i) == b.charAt(i)) {
                ret += a.charAt(i);
            }
        }
        return ret;
    }

    public static void acquireUserIO_Semaphore() throws InterruptedException {
        try {
            userio_sem.acquire();
        } catch (InterruptedException e) {
            userio_sem.drainPermits();
            userio_sem.release(1);
            throw e;
        }
    }

    public static void releaseUserIO_Semaphore() {
        userio_sem.drainPermits();
        userio_sem.release(1);
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
    public static void appendInfoToFilename(final Plugin plugin, File file, String captchaCode, boolean isGood) {
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

        return message != null ? ret.trim() + ": " + message : ret.trim();

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
            if (inChannel != null) {
                try {
                    inChannel.close();

                    if (outChannel != null) {
                        outChannel.close();
                    }
                } catch (IOException e) {

                    e.printStackTrace();
                    return false;
                }
            }
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (inChannel != null) {
                inChannel.close();
            }

            if (outChannel != null) {
                outChannel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
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

    /**
     * Hängt an i solange fill vorne an bis die zechenlänge von i gleich num ist
     * 
     * @param i
     * @param num
     * @param fill
     * @return aufgefüllte Zeichenkette
     */
    public static String fillInteger(long i, int num, String fill) {
        String ret = "" + i;
        while (ret.length() < num) {
            ret = fill + ret;
        }
        return ret;
    }

    public static String fillString(String binaryString, String pre, String post, int length) {
        while (binaryString.length() < length) {
            if (binaryString.length() < length) {
                binaryString = pre + binaryString;
            }
            if (binaryString.length() < length) {
                binaryString = binaryString + post;
            }
        }
        return binaryString;
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
            return Integer.parseInt(Encoding.filterString(src, "1234567890"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static long filterLong(String src) {
        try {
            return Long.parseLong(Encoding.filterString(src, "1234567890"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Formatiert Byes in einen MB String [MM.MM MB]
     * 
     * @param downloadMax
     * @return MegaByte Formatierter String
     */
    public static String formatBytesToMB(long downloadMax) {
        if (downloadMax < 0) return null;
        DecimalFormat c = new DecimalFormat("0.00");
        return c.format(downloadMax / (1024.0 * 1024.0)) + " MB";
    }

    public static String formatKbReadable(int value) {
        DecimalFormat c = new DecimalFormat("0.00");
        if (value >= 1024 * 1024) return c.format(value / (1024 * 1024.0)) + " GB";
        if (value >= 1024) return c.format(value / 1024.0) + " MB";
        return value + " KB";
    }

    public static String formatKbReadable(long value) {
        DecimalFormat c = new DecimalFormat("0.00");
        if (value >= 1024 * 1024) return c.format(value / (1024 * 1024.0)) + " GB";
        if (value >= 1024) return c.format(value / 1024.0) + " MB";
        return value + " KB";
    }

    /**
     * Formatiert Sekunden in das zeitformat stunden:minuten:sekunden
     * 
     * @param eta
     *            toURI().toURL();
     * @return formatierte Zeit
     */
    public static String formatSeconds(long eta) {
        long hours = eta / (60 * 60);
        eta -= hours * 60 * 60;
        long minutes = eta / 60;
        long seconds = eta - minutes * 60;
        if (hours == 0) { return JDUtilities.fillInteger(minutes, 2, "0") + ":" + JDUtilities.fillInteger(seconds, 2, "0"); }
        return JDUtilities.fillInteger(hours, 2, "0") + ":" + JDUtilities.fillInteger(minutes, 2, "0") + ":" + JDUtilities.fillInteger(seconds, 2, "0");
    }

    public static String getCaptcha(Plugin plugin, String method, File file, boolean forceJAC, CryptedLink link) throws InterruptedException {
        link.getProgressController().setProgressText(SimpleGUI.WAITING_USER_IO);
        String code = getCaptcha(plugin, method, file, forceJAC);
        link.getProgressController().setProgressText(null);
        return code;
    }

    public static String getCaptcha(Plugin plugin, String method, File file, boolean forceJAC, DownloadLink link) throws InterruptedException {
        link.getLinkStatus().addStatus(LinkStatus.WAITING_USERIO);
        link.requestGuiUpdate();
        String code = getCaptcha(plugin, method, file, forceJAC);
        link.getLinkStatus().removeStatus(LinkStatus.WAITING_USERIO);
        link.requestGuiUpdate();
        return code;
    }

    public static String getUserInput(String message, DownloadLink link) throws InterruptedException {
        link.getLinkStatus().addStatus(LinkStatus.WAITING_USERIO);
        link.requestGuiUpdate();
        String code = getUserInput(message);
        link.getLinkStatus().removeStatus(LinkStatus.WAITING_USERIO);
        link.requestGuiUpdate();
        return code;
    }

    public static String getUserInput(String message, String defaultmessage, DownloadLink link) throws InterruptedException {
        link.getLinkStatus().addStatus(LinkStatus.WAITING_USERIO);
        link.requestGuiUpdate();
        String code = getUserInput(message, defaultmessage);
        link.getLinkStatus().removeStatus(LinkStatus.WAITING_USERIO);
        link.requestGuiUpdate();
        return code;
    }

    public static String getUserInput(String message, CryptedLink link) throws InterruptedException {
        link.getProgressController().setProgressText(SimpleGUI.WAITING_USER_IO);
        String password = getUserInput(message);
        link.getProgressController().setProgressText(null);
        return password;
    }

    public static String getUserInput(String message, String defaultmessage, CryptedLink link) throws InterruptedException {
        link.getProgressController().setProgressText(SimpleGUI.WAITING_USER_IO);
        String password = getUserInput(message, defaultmessage);
        link.getProgressController().setProgressText(null);
        return password;
    }

    public static String getUserInput(String message) throws InterruptedException {
        acquireUserIO_Semaphore();
        if (message == null) message = JDLocale.L("gui.linkgrabber.password", "Password?");
        String password = JDUtilities.getGUI().showUserInputDialog(message);
        releaseUserIO_Semaphore();
        return password;
    }

    public static String getUserInput(String message, String defaultmessage) throws InterruptedException {
        acquireUserIO_Semaphore();
        if (message == null) message = JDLocale.L("gui.linkgrabber.password", "Password?");
        if (defaultmessage == null) defaultmessage = "";
        String password = JDUtilities.getGUI().showUserInputDialog(message, defaultmessage);
        releaseUserIO_Semaphore();
        return password;
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
     * @throws InterruptedException
     */
    public static String getCaptcha(Plugin plugin, String method, File file, boolean forceJAC) throws InterruptedException {
        String host;
        if (method == null) {
            host = plugin.getHost();
        } else {
            host = method.toLowerCase();
        }
        JDUtilities.getController().fireControlEvent(new ControlEvent(plugin, ControlEvent.CONTROL_CAPTCHA_LOADED, file));

        logger.info("JAC has Method for: " + host + ": " + JAntiCaptcha.hasMethod(JDUtilities.getJACMethodsDirectory(), host));
        if (forceJAC || JAntiCaptcha.hasMethod(JDUtilities.getJACMethodsDirectory(), host) && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_JAC_METHODS + "_" + host, true) && !configuration.getBooleanProperty(Configuration.PARAM_CAPTCHA_JAC_DISABLE, false)) {
            if (!JAntiCaptcha.hasMethod(JDUtilities.getJACMethodsDirectory(), host) || !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_JAC_METHODS + "_" + host, true)) { return null; }

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
            JAntiCaptcha jac = new JAntiCaptcha(JDUtilities.getJACMethodsDirectory(), host);
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
                for (LetterComperator element : lcs) {
                    // window.setImage(i, 0, lcs[i].getB().getImage(3));
                    // window.setImage(i, 1, lcs[i].getA().getImage(3));
                    if (element == null) {
                        vp = 100.0;
                        break;
                    }
                    vp = Math.max(vp, element.getValityPercent());
                    // window.setText(i, 2, lcs[i].getValityPercent());
                    // window.setText(i, 3, lcs[i].getDecodedValue());
                    // window.setText(i, 4, lcs[i].getB().getPixelString());
                }
            }
            // window.pack();
            logger.info("worst letter: " + vp);
            if (plugin.useUserinputIfCaptchaUnknown() && vp > (double) JDUtilities.getSubConfig("JAC").getIntegerProperty(Configuration.AUTOTRAIN_ERROR_LEVEL, 18)) {
                plugin.setCaptchaDetectID(Plugin.CAPTCHA_USER_INPUT);
                acquireUserIO_Semaphore();
                code = JDUtilities.getController().getCaptchaCodeFromUser(plugin, file, captchaCode);
                releaseUserIO_Semaphore();
            } else {
                return captchaCode;
            }

            if (code != null && code.equals(captchaCode)) { return captchaCode; }
            return code;
        }

        else {
            acquireUserIO_Semaphore();
            plugin.setCaptchaDetectID(Plugin.CAPTCHA_USER_INPUT);
            String code = JDUtilities.getController().getCaptchaCodeFromUser(plugin, file, null);
            releaseUserIO_Semaphore();
            return code;
        }
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

    /**
     * @return Configuration instanz
     */
    public static Configuration getConfiguration() {
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
        if (dlDir == null) {
            dlDirectory = new File("");
        } else {
            dlDirectory = new File(dlDir);
        }

        if (lastDir == null) return dlDirectory;
        return new File(lastDir);

    }

    /**
     * Gibt die Endung einer FIle zurück oder null
     * 
     * @param ret
     * @return
     */
    public static String getFileExtension(File ret) {
        if (ret == null) { return null; }
        String str = ret.getAbsolutePath();

        int i3 = str.lastIndexOf(".");

        if (i3 > 0) { return str.substring(i3 + 1); }
        return null;
    }

    public static UIInterface getGUI() {
        if (JDUtilities.getController() == null) { return null; }
        return JDUtilities.getController().getUiInterface();
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

    public static ImageIcon getImageIcon(String imageName) {
        return new ImageIcon(imageName);
    }

    /**
     * Prüft anhand der Globalen IP Check einstellungen die IP
     * 
     * @return ip oder /offline
     */
    public static String getIPAddress() {
        Browser br = new Browser();
        br.setConnectTimeout(5000);
        br.setReadTimeout(5000);
        if (JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
            logger.finer("IP Check is disabled. return current Milliseconds");
            return System.currentTimeMillis() + "";
        }

        String site = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_CHECK_SITE, "http://checkip.dyndns.org");
        String patt = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_PATTERN, "Address\\: ([0-9.]*)\\<\\/body\\>");

        try {
            logger.finer("IP Check via " + site);
            Pattern pattern = Pattern.compile(patt);
            Matcher matcher = pattern.matcher(br.getPage(site));
            if (matcher.find()) {
                if (matcher.groupCount() > 0) {
                    return matcher.group(1);
                } else {
                    logger.severe("Primary bad Regex: " + patt);

                }
            }
            logger.info("Primary IP Check failed. Ip not found via regex: " + patt + " on " + site + " htmlcode: " + br.toString());

        }

        catch (Exception e1) {
            logger.severe("url not found. " + e1.toString());

        }

        try {

            logger.finer("http://service.jdownloader.org/tools/getip.php");

            Pattern pattern = Pattern.compile(patt);
            Matcher matcher = pattern.matcher(br.getPage("http://service.jdownloader.org/tools/getip.php"));
            if (matcher.find()) {
                if (matcher.groupCount() > 0) {
                    return matcher.group(1);
                } else {
                    logger.severe("Primary bad Regex: " + patt);
                }
            }
            return "offline";
        }

        catch (Exception e1) {
            logger.severe("url not found. " + e1.toString());
            logger.info("Sec. IP Check failed.");

        }

        return "offline";
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
        int majorVersion = JDUtilities.filterInt(version.substring(0, version.indexOf(".")));
        int subversion = JDUtilities.filterInt(version.substring(version.indexOf(".") + 1));
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
            logger.info("Create Classloader: for: " + homeDir.getAbsolutePath());
            jdClassLoader = new JDClassLoader(homeDir.getAbsolutePath(), Thread.currentThread().getContextClassLoader());

        }
        return jdClassLoader;
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
            if (currentDir.isFile()) {
                currentDir = currentDir.getParentFile();
            }

        } catch (URISyntaxException e) {

            e.printStackTrace();
        }

        // logger.info("RunDir: " + currentDir);

        switch (JDUtilities.getRunType()) {
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

    public static String getJDTitle() {
        String ret = JDUtilities.JD_TITLE + " " + JDUtilities.JD_VERSION + JDUtilities.getRevision();
        if (JDUtilities.getController() != null && JDUtilities.getController().getWaitingUpdates() != null && JDUtilities.getController().getWaitingUpdates().size() > 0) {
            ret += " " + JDLocale.L("gui.mainframe.title.updatemessage", "-->UPDATES VERFÜGBAR:") + " " + JDUtilities.getController().getWaitingUpdates().size();
        }
        return ret;
    }

    public static int getLevenshteinDistance(String s, String t) {
        if (s == null || t == null) { throw new IllegalArgumentException("Strings must not be null"); }
        int n = s.length();
        int m = t.length();
        if (n == 0) {
            return m;
        } else if (m == 0) { return n; }
        int p[] = new int[n + 1];
        int d[] = new int[n + 1];
        int _d[];
        int i;
        int j;
        char t_j;
        int cost;
        for (i = 0; i <= n; i++) {
            p[i] = i;
        }
        for (j = 1; j <= m; j++) {
            t_j = t.charAt(j - 1);
            d[0] = j;
            for (i = 1; i <= n; i++) {
                cost = s.charAt(i - 1) == t_j ? 0 : 1;
                d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
            }
            _d = p;
            p = d;
            d = _d;
        }

        return p[n];
    }

    public static Locale getLocale() {
        return locale;
    }

    /**
     * public static String getLocalFile(File file) Liest file über einen
     * bufferdReader ein und gibt den Inhalt asl String zurück
     * 
     * @param file
     * @return File Content als String
     */
    public static String getLocalFile(File file) {
        if (!file.exists()) { return ""; }
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
                public void close() {
                }

                public void flush() {
                }

                public void publish(LogRecord logRecord) {
                    if (JDUtilities.getController() != null) {
                        JDUtilities.getController().fireControlEvent(ControlEvent.CONTROL_LOG_OCCURED, logRecord);
                    }
                }
            });

            OutputStream os = new OutputStream() {
                private StringBuffer buffer = new StringBuffer();

                public void write(int b) throws IOException {
                    if (b == 13 || b == 10) {
                        if (buffer.length() > 0) {
                            JDUtilities.getLogger().severe(buffer.toString());
                            if (buffer.indexOf("OutOfMemoryError") >= 0) {
                                logger.finer("Restart");
                                if (JDUtilities.getGUI().showConfirmDialog(JDLocale.L("gui.messages.outofmemoryerror", "An error ocured!\r\nJDownloader is out of memory. Restart recommended.\r\nPlease report this bug!"))) {
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
        Vector<String> priority = (Vector<String>) configuration.getProperty(Configuration.PARAM_HOST_PRIORITY, new Vector<String>());
        for (int i = 0; i < priority.size(); i++) {
            for (int b = plgs.size() - 1; b >= 0; b--) {
                if (plgs.get(b).getHost() == null) {
                    logger.info("OO");
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
     * Gibt ein FileOebject zu einem Resourcstring zurück
     * 
     * @author JD-Team
     * @param resource
     *            Ressource, die geladen werden soll
     * @return File zu arg
     */
    public static File getResourceFile(String resource) {
        JDClassLoader cl = JDUtilities.getJDClassLoader();
        if (cl == null) {
            logger.severe("Classloader ==null: ");
            return null;
        }
        URL clURL = JDUtilities.getJDClassLoader().getResource(resource);

        if (clURL != null) {
            try {
                return new File(clURL.toURI());
            } catch (URISyntaxException e) {
            }
        }
        return null;
    }

    /**
     * Parsed den Revision-String ins Format 0.000
     * 
     * @return RevisionID
     */
    public static String getRevision() {
        double r = Double.parseDouble(getVersion("$Revision$")) / 1000.0;
        return new DecimalFormat("0.000").format(r).replace(",", ".");
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

            e.printStackTrace();
        }
        return 0;

    }

    public static ImageIcon getScaledImageIcon(Image image, int width, int height) {
        return new ImageIcon(image.getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    public static ImageIcon getScaledImageIcon(ImageIcon icon, int width, int height) {
        return JDUtilities.getScaledImageIcon(icon.getImage(), width, height);
    }

    public static ImageIcon getScaledImageIcon(String imageName, int width, int height) {
        return JDUtilities.getScaledImageIcon(JDUtilities.getImage(imageName), width, height);
    }

    public static SubConfiguration getSubConfig(String name) {
        if (subConfigs.containsKey(name)) { return subConfigs.get(name); }

        SubConfiguration cfg = new SubConfiguration(name);
        subConfigs.put(name, cfg);
        cfg.save();
        return cfg;

    }

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
        Object objectLoaded = null;
        if (fileInput == null) {
            JFileChooser fileChooserLoad = new JFileChooser();
            if (currentDirectory != null) {
                fileChooserLoad.setCurrentDirectory(currentDirectory);
            }
            if (fileChooserLoad.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                fileInput = fileChooserLoad.getSelectedFile();
                currentDirectory = fileChooserLoad.getCurrentDirectory();
            }
        }
        if (fileInput != null) {

            JDUtilities.waitOnObject(fileInput);
            saveReadObject.add(fileInput);

            try {
                FileInputStream fis = new FileInputStream(fileInput);
                BufferedInputStream buff = new BufferedInputStream(fis);
                if (asXML) {
                    XMLDecoder xmlDecoder = new XMLDecoder(new BufferedInputStream(buff));
                    objectLoaded = xmlDecoder.readObject();
                    xmlDecoder.close();
                } else {
                    ObjectInputStream ois = new ObjectInputStream(buff);
                    objectLoaded = ois.readObject();
                    ois.close();
                }
                fis.close();
                buff.close();

                saveReadObject.remove(fileInput);
                return objectLoaded;
            } catch (Exception e) {
                logger.severe(e.getMessage());
            }
            saveReadObject.remove(fileInput);
        }
        return null;
    }

    public static boolean removeDirectoryOrFile(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String element : children) {
                boolean success = JDUtilities.removeDirectoryOrFile(new File(dir, element));
                if (!success) { return false; }
            }
        }

        return dir.delete();
    }

    public static void restartJD() {
        if (JDUtilities.getController() != null) JDUtilities.getController().prepareShutdown();
        logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "-Xmx512m", "JDownloader.jar", }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
        System.exit(0);

    }

    public static void restartJD(String[] jdArgs) {
        if (JDUtilities.getController() != null) JDUtilities.getController().prepareShutdown();
        String[] javaArgs = new String[] { "-jar", "-Xmx512m", "JDownloader.jar" };
        String[] finalArgs = new String[jdArgs.length + javaArgs.length];
        System.arraycopy(javaArgs, 0, finalArgs, 0, javaArgs.length);
        System.arraycopy(jdArgs, 0, finalArgs, javaArgs.length, jdArgs.length);

        logger.info(JDUtilities.runCommand("java", finalArgs, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
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
        JDUtilities.getDatabaseConnector().saveConfiguration("jdownloaderconfig", JDUtilities.getConfiguration());
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
        if (fileOutput == null) {
            JDFileFilter fileFilter = new JDFileFilter(extension, extension, true);
            JFileChooser fileChooserSave = new JFileChooser();
            fileChooserSave.setFileFilter(fileFilter);
            fileChooserSave.setSelectedFile(new File(((name != null) ? name : "*") + ((extension != null) ? extension : ".*")));
            if (currentDirectory != null) {
                fileChooserSave.setCurrentDirectory(currentDirectory);
            }
            if (fileChooserSave.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                fileOutput = fileChooserSave.getSelectedFile();
                currentDirectory = fileChooserSave.getCurrentDirectory();
            }
        }

        if (fileOutput != null) {
            if (fileOutput.isDirectory()) {
                fileOutput = new File(fileOutput, name + extension);

            }

            JDUtilities.waitOnObject(fileOutput);
            saveReadObject.add(fileOutput);

            if (fileOutput.exists()) {
                fileOutput.delete();
            }
            try {
                FileOutputStream fos = new FileOutputStream(fileOutput);
                BufferedOutputStream buff = new BufferedOutputStream(fos);
                if (asXML) {
                    XMLEncoder xmlEncoder = new XMLEncoder(buff);
                    xmlEncoder.writeObject(objectToSave);
                    xmlEncoder.close();
                } else {
                    ObjectOutputStream oos = new ObjectOutputStream(buff);
                    oos.writeObject(objectToSave);
                    oos.close();
                }
                buff.close();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String hashPost = JDHash.getMD5(fileOutput);
            if (hashPost == null) {
                logger.severe("Schreibfehler: " + fileOutput + " Datei wurde nicht erstellt");
            }
            saveReadObject.remove(fileOutput);

        } else {
            logger.severe("Schreibfehler: Fileoutput: null");
        }
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

    /**
     * Speichert ein byteArray in ein file.
     * 
     * @param file
     * @param bytearray
     * @return Erfolg true/false
     */
    public static boolean saveToFile(File file, byte[] b) {
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
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file, true));
            output.write(b, 0, b.length);
            output.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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

    /**
     * Setztd as aktuelle woringdirectory für den filebrowser
     * 
     * @param f
     * @param id
     */
    public static void setCurrentWorkingDirectory(File f, String id) {
        if (id == null) id = "";
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_CURRENT_BROWSE_PATH + id, f.getAbsolutePath());
        JDUtilities.saveConfig();
    }

    public static void setLocale(Locale locale) {
        JDUtilities.locale = locale;
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
        return Pattern.compile(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_GLOBAL_IP_MASK, "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).)" + "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b")).matcher(ip).matches();
    }

    public static String validatePath(String fileOutput0) {
        if (OSDetector.isWindows()) {
            String hd = "";
            if (new File(fileOutput0).isAbsolute()) {
                hd = fileOutput0.substring(0, 3);
                fileOutput0 = fileOutput0.substring(3);
            }
            fileOutput0 = hd + fileOutput0.replaceAll("([<|>|\\||\"|:|\\*|\\?|\\x00])+", "_");
        }

        return fileOutput0;
    }

    public static String validateFileandPathName(String name) {
        if (name == null) { return null; }
        return name.replaceAll("([<|>|\\||\"|:|\\*|\\?|/|\\x00])+", "_");
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

    public static void waitOnObject(File file) {
        int c = 0;
        while (saveReadObject.contains(file)) {
            if (c++ > 1000) { return; }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {

                e.printStackTrace();
            }
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
            e.printStackTrace();
            return false;
        }
    }

    public static DatabaseConnector getDatabaseConnector() {
        if (dbconnect == null) {
            dbconnect = new DatabaseConnector();
        }
        return dbconnect;
    }

    /**
     * The format describing an http date.
     */
    private static SimpleDateFormat dateFormat;
    static {
        dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(new SimpleTimeZone(0, "GMT"));
        dateFormat.setLenient(true);
    }

    /**
     * Returns a string containing an HTTP-formatted date.
     * 
     * @param time
     *            The date to format (current time in msec).
     * 
     * @return HTTP date string representing the given time.
     */
    public static String formatTime(long time) {
        return dateFormat.format(new Date(time)).substring(0, 29);
    }

    public static boolean openExplorer(File path) {
        try {
            return new GetExplorer().openExplorer(path);
        } catch (Exception e) {
            e.printStackTrace();
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

        String ret = "{\"";
        for (int i = 0; i < passwords.length; i++) {
            if (!passwords[i].matches("[\\s]*")) {
                ret += passwords[i] + (i == l ? "\"}" : "\",\"");
            }
        }
        return ret;

    }

}
