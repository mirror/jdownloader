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

package jd.captcha.utils;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
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
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Properties;
import java.util.Scanner;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jd.captcha.JAntiCaptcha;
import jd.utils.JDUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Diese Klasse beinhaltet mehrere Hilfsfunktionen
 * 
 * @author JD-Team
 */

public class UTILITIES {
    public static String cookie = null;

    /**
     * File Seperator
     */
    public static String FS = System.getProperty("file.separator");

    private static Logger logger = JDUtilities.getLogger();

    /**
     * private static String PROPERTYFILE Pfad zur Gloabeln property file
     */
    public static String PROPERTYFILE = "globals.dat";
    /** ************************************Properties************************************ */
    /**
     * private static Properties PROPS Globale Property File
     */
    private static Properties PROPS;
    private static int READ_TIMEOUT = 10000;

    private static int REQUEST_TIMEOUT = 10000;

    /**
     * Application dir
     */
    public static String ROOTDIR = System.getProperty("user.dir") + FS;

    public static boolean useCookies = false;

    /** *********************DEBUG*************************** */

    private static String USER_AGENT = "WebUpdater";

    public static boolean checkJumper(int x, int from, int to) {
        return x >= from && x <= to;

    }

    /**
     * public static boolean confirm(String msg) Zeigt einen Bestätigungsdialog
     * an.
     * 
     * @param msg
     * @return true/false, Je nach Input
     */
    public static boolean confirm(String msg) {
        return JOptionPane.showConfirmDialog(new JFrame(), msg, "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == 0;
    }

    /**
     * public static File directoryChooser Zeigt einen Directory Chooser an
     * 
     * @param path
     * @return User Input /null
     */

    public static File directoryChooser(String path) {
        JFileChooser fc = new JFileChooser();

        fc.setApproveButtonText("OK");
        if (path != null) {
            fc.setCurrentDirectory(new File(path));
        }

        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.showOpenDialog(new JFrame());
        File ret = fc.getSelectedFile();

        return ret;

    }

    /**
     * Lädt fileurl nach filepath herunter
     * 
     * @param filepath
     * @param fileurl
     * @return true/False
     */
    public static boolean downloadBinary(String filepath, String fileurl) {
        filepath = filepath.replace("\\", FS);
        try {
            fileurl = UTILITIES.urlEncode(fileurl.replaceAll("\\\\", "/"));
            File file = new File(filepath);
            if (file.isFile()) {
                if (!file.delete()) {

                    UTILITIES.getLogger().severe("Konnte Datei nicht löschen " + file);
                    return false;
                }

            }

            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();

            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file, true));
            fileurl = URLDecoder.decode(fileurl, "UTF-8");

            URL url = new URL(fileurl);

            URLConnection con = url.openConnection();
            if (cookie != null && useCookies) {
                con.setRequestProperty("Cookie", cookie);
            }
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
     * public static File fileChooser(String path) zeigt einen filechooser an
     * 
     * @param path
     * @return User input/null
     */

    public static File fileChooser(String path) {
        JFileChooser fc = new JFileChooser();

        fc.setApproveButtonText("OK");
        if (path != null) {
            fc.setCurrentDirectory(new File(path));
        }

        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.showOpenDialog(new JFrame());
        File ret = fc.getSelectedFile();

        return ret;

    }

    /**
     * 
     * @param file
     * @return URI zur file
     */
    public static String fileToURI(File file) {
        return file.toURI().toString();
    }

    /**
     * Füllt string mit filler auf bis i Zeichen erreicht sind.
     * 
     * @param string
     * @param i
     * @param filler
     * @return neuer String
     */
    public static String fillInteger(String string, int i, String filler) {
        while (string.length() < i) {
            string = filler + string;
        }
        return string;
    }

    // /**
    // * @param path
    // * @return Gibt zum Pfad Path die URL zurück
    // */
    // public static URLClassLoader getURLClassLoader(String path) {
    // if(JAntiCaptcha.isLoggerActive())logger.fine("Loading: " + path);
    // if (path.startsWith("file:")) {
    // try {
    //
    // if (new File(new URI(path)).exists() != true) return null;
    //
    // return new URLClassLoader(new URL[] { new URL(path) });
    // }
    // catch (URISyntaxException e) {
    // e.printStackTrace();
    //
    // }
    // catch (MalformedURLException e) {
    //
    // e.printStackTrace();
    // }
    // }
    // else {
    // try {
    // if (!new File(path).exists()) return null;
    // return new URLClassLoader(new URL[] { new File(path).toURI().toURL() },
    // null);
    // }
    // catch (MalformedURLException e) {
    // e.printStackTrace();
    // return null;
    // }
    // }
    // return null;
    // }

    // /**
    // * @param file
    // * @return Gibt die URl zur File zurück
    // */
    // public static URLClassLoader getURLClassLoader(File file) {
    // return getURLClassLoader(file.getAbsolutePath());
    // }

    // /**
    // * @param url
    // * @param file
    // * @return Gibt die URl zur File zurück
    // */
    // public static URLClassLoader getURLClassLoader(URL url) {
    // return getURLClassLoader(url.toString());
    // }

    public static double getAbsolutLABValue(int[] rgb) {
        return UTILITIES.getColorDifference(rgb, new int[] { 0, 0, 0 });
    }

    /**
     * @param source
     * @param pattern
     * @return Alle treffer von pattern in sourde in einem vector
     */
    public static Vector<Vector<String>> getAllMatches(String source, String pattern) {
        return UTILITIES.getAllMatches(source, pattern, new Vector<Vector<String>>());
    }

    /**
     * Schreibt alle treffer von pattern in source in den übergebenen vector
     * 
     * @param source
     * @param pattern
     * @param container
     * @return Treffer
     */
    public static Vector<Vector<String>> getAllMatches(String source, String pattern, Vector<Vector<String>> container) {
        pattern = UTILITIES.getPattern(pattern);
        Vector<Vector<String>> ret = container;

        Vector<String> entry;
        String tmp;
        for (Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(source); r.find();) {
            entry = new Vector<String>();

            for (int x = 1; x <= r.groupCount(); x++) {
                if ((tmp = r.group(x).trim()).length() > 0) {
                    entry.add(UTILITIES.UTF8Decode(tmp));
                }
            }
            ret.add(entry);

        }

        return ret;
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
        if (att == null || att.getNamedItem(key) == null) {
            if (JAntiCaptcha.isLoggerActive()) {
                logger.info("ERROR: XML Attribute missing: " + key);
            }
            return null;
        }
        return att.getNamedItem(key).getNodeValue();
    }

    public static double getColorDifference(int a, int b) {
        return UTILITIES.getColorDifference(UTILITIES.getRGB(a), UTILITIES.getRGB(b));

    }

    public static double getColorDifference(int[] rgbA, int[] rgbB) {
        int[] labA = UTILITIES.rgb2lab(rgbA[0], rgbA[1], rgbA[2]);
        int[] labB = UTILITIES.rgb2lab(rgbB[0], rgbB[1], rgbB[2]);
        int dif0 = labA[0] - labB[0];
        int dif1 = labA[1] - labB[1];
        int dif2 = labA[2] - labB[2];
        return Math.sqrt(dif0 * dif0 + dif1 * dif1 + dif2 * dif2);
    };

    /**
     * @param arg
     * @return URL Connection
     * @throws IOException
     * @throws URISyntaxException
     */
    public static URLConnection getConnection(String arg) throws IOException, URISyntaxException {
        URL url = UTILITIES.getURL(arg);
        URLConnection con = url.openConnection();
        con.setConnectTimeout(REQUEST_TIMEOUT);
        con.setReadTimeout(READ_TIMEOUT);
        con.setRequestProperty("User-Agent", USER_AGENT);
        if (useCookies) {
            if (cookie != null) {
                con.setRequestProperty("Cookie", cookie);
            }
            cookie = con.getHeaderField("Set-Cookie");
        }
        return con;

    }

    /**
     * Liest einen String von einem Inputstring
     * 
     * @param is
     * @return String von is
     */
    public static String getFromInputStream(InputStream is) {
        BufferedReader f;
        try {
            f = new BufferedReader(new InputStreamReader(is));

            String line;
            String ret = "";

            while ((line = f.readLine()) != null) {
                ret += line + "\r\n";
            }
            f.close();
            return ret;
        } catch (IOException e) {

            e.printStackTrace();
        }
        return "";
    }

    /**
     * public static String getFullFile(String[] entries) Gibt die File zurück
     * der im array entries übergeben wurde. der passende FS wird eingesetzt
     * 
     * @param entries
     * @return Pfad als File
     */
    public static File getFullFile(String[] entries) {
        return new File(UTILITIES.getFullPath(entries));
    }

    /**
     * public static String getFullPath(String[] entries) Gibt den Pfad zurück
     * der im array entries übergeben wurde. der passende FS wird eingesetzt
     * 
     * @param entries
     * @return Pfad als String
     */
    public static String getFullPath(String[] entries) {
        String ret = "";
        for (int i = 0; i < entries.length - 1; i++) {
            ret += entries[i] + FS;
        }
        ret += entries[entries.length - 1];
        return ret;
    }

    /**
     * public static GridBagConstraints getGBC(int x, int y, int width, int
     * height) Gibt die default GridBAgConstants zurück
     * 
     * @param x
     * @param y
     * @param width
     * @param height
     * @return Default GridBagConstraints
     */
    public static GridBagConstraints getGBC(int x, int y, int width, int height) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.gridheight = height;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(1, 1, 1, 1);
        return gbc;
    }

    public static double getHsbColorDifference(int[] rgbA, int[] rgbB) {
        float hsbA = UTILITIES.rgb2hsb(rgbA[0], rgbA[1], rgbA[2])[0] * 100;
        float hsbB = UTILITIES.rgb2hsb(rgbB[0], rgbB[1], rgbB[2])[0] * 100;
        double dif = Math.abs((double) (hsbA - hsbB));
        return dif;
    }

    public static int getJumperStart(int from, int to) {

        return from + (to - from) / 2;

    }

    /**
     * public static String getLocalFile(File file) Liest file über einen
     * bufferdReader ein und gibt den Inhalt asl String zurück
     * 
     * @param file
     * @return File Content als String
     */
    public static String getLocalFile(File file) {
        BufferedReader f;
        try {
            f = new BufferedReader(new FileReader(file));

            String line;
            String ret = "";

            while ((line = f.readLine()) != null) {
                ret += line + "\r\n";
            }
            f.close();
            return ret;
        } catch (IOException e) {

            e.printStackTrace();
        }
        return "";
    }

    /**
     * public static String getLocalHash(File f) Gibt einen MD% Hash der file
     * zurück
     * 
     * @param f
     * @return Hashstring Md5
     */
    public static String getLocalHash(File f) {
        try {

            MessageDigest md;

            md = MessageDigest.getInstance("md5");

            byte[] b = new byte[1024];

            InputStream in = new FileInputStream(f);
            for (int n = 0; (n = in.read(b)) > -1;) {
                md.update(b, 0, n);

            }
            byte[] digest = md.digest();
            String ret = "";

            for (byte element : digest) {
                String tmp = Integer.toHexString(element & 0xFF);
                if (tmp.length() < 2) {
                    tmp = "0" + tmp;
                }
                ret += tmp;
            }
            in.close();
            return ret;

        } catch (Exception e) {

            // DEBUG.error(e);
        }
        return "";
    }

    /**
     * public static String getLocalHash(String file) Gibt einen MD% Hash der
     * file zurück
     * 
     * @param file
     * @return Hash-string (MD5)
     */
    public static String getLocalHash(String file) {
        return UTILITIES.getLocalHash(new File(file));
    }

    /**
     * @return logger
     */
    public static Logger getLogger() {
        return logger;
    }

    /**
     * ***************************STRING &
     * PARSE***************************************
     */
    /**
     * public static String[] getMatches(String source, String pattern) Gibt
     * alle treffer in source nach dem pattern zurück. Platzhalter ist nur !! °
     * 
     * @param source
     * @param pattern
     * @return Alle TReffer
     */
    public static String[] getMatches(String source, String pattern) {
        // DEBUG.trace("pattern: "+STRING.getPattern(pattern));
        if (source == null || pattern == null) { return null; }
        Matcher rr = Pattern.compile(UTILITIES.getPattern(pattern), Pattern.DOTALL).matcher(source);
        if (!rr.find()) {
            // Keine treffer
        }
        try {
            String[] ret = new String[rr.groupCount()];
            for (int i = 1; i <= rr.groupCount(); i++) {
                ret[i - 1] = rr.group(i);
            }
            return ret;
        } catch (IllegalStateException e) {

            return null;
        }
    }

    /***************************************************************************
     * ******************************FILE-SYSTEM
     * 
     * @param myClass
     * @return Gibt den Pfad zur Klasse zurück
     */
    public static String getPackagePath(Object myClass) {
        String packagePath = myClass.getClass().getPackage().getName().replace(".", System.getProperty("file.separator"));
        return myClass.getClass().getClassLoader().getResource(packagePath).toString();
    }

    /**
     * LIest eine webseite ein und gibt deren source zurück
     * 
     * @param urlStr
     * @return String inhalt von urlStr
     */
    public static String getPagewithScanner(String urlStr) {
        try {
            URLConnection con = UTILITIES.getConnection(urlStr);
            BufferedReader input = new BufferedReader(new InputStreamReader(con.getInputStream()));
            Scanner r = new Scanner(input).useDelimiter("\\Z");
            String ret = "";
            while (r.hasNext()) {
                ret += r.next();
            }
            return ret;
        } catch (FileNotFoundException e) {
            UTILITIES.getLogger().severe(urlStr + " nicht gefunden");
            // DEBUG.error(e);
        } catch (URISyntaxException e) {
            UTILITIES.getLogger().severe(urlStr + " URI yntac error");

        } catch (MalformedURLException e) {
            UTILITIES.getLogger().severe(urlStr + " Malformed URL");
        } catch (SocketTimeoutException e) {
            // getLogger().severe(urlStr + " Socket Timeout");
        } catch (IOException e) {
            if (JAntiCaptcha.isLoggerActive()) {
                logger.severe("IOException " + e);
            }
        }
        return null;
    }

    /**
     * public static String getPattern(String str) Gibt ein Regex pattern
     * zurück. ° dient als Platzhalter!
     * 
     * @param str
     * @return REgEx Pattern
     */
    public static String getPattern(String str) {

        String allowed = "QWERTZUIOPÜASDFGHJKLÖÄYXCVBNMqwertzuiopasdfghjklyxcvbnm 1234567890";
        String ret = "";
        int i;
        for (i = 0; i < str.length(); i++) {
            char letter = str.charAt(i);
            // 176 == °
            if (letter == 176) {
                ret += "(.*?)";
            } else if (allowed.indexOf(letter) == -1) {

                ret += "\\" + letter;
            } else {

                ret += letter;
            }
        }

        return ret;
    }

    public static int getPercent(int a, int b) {
        if (b == 0) { return 100; }
        return a * 100 / b;
    }

    /**
     * public static String getPreString() Diese Funktion gibt einen Zeitstring
     * zur anzeige aufd er Konsole aus
     * 
     * @return TimeString
     */
    public static String getPreString() {
        Calendar c = Calendar.getInstance();

        return c.get(Calendar.DAY_OF_MONTH) + "." + c.get(Calendar.MONTH) + "." + c.get(Calendar.YEAR) + " - " + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND) + " (" + c.get(Calendar.MILLISECOND) + ") : ";
    }

    /**
     * public static String getProperty(String key) Gibt eine Property aus der
     * globalen propertyfile zurück.
     * 
     * @param key
     * @return passender Eintrag in die globals.dat/null
     */
    public static String getProperty(String key) {
        if (PROPS == null) {
            FileInputStream input;
            PROPS = new Properties();
            try {
                input = new FileInputStream(PROPERTYFILE);
                if (JAntiCaptcha.isLoggerActive()) {
                    logger.info(PROPERTYFILE);
                }
                PROPS.load(input);
                return PROPS.getProperty(key);

            } catch (Exception e) {

                e.printStackTrace();
                return null;
            }
        } else {
            return PROPS.getProperty(key);
        }

    }

    /**
     * public static String getProperty(String key,String defaultValue) Gibt
     * eine Eigenschaft aus der globalen Propertyfile zurück. Falls zu dem key
     * kein Wert existiert wird der defaultwert angelegt und zurückgegeben
     * 
     * @param key
     * @param defaultValue
     * @return Passender Eintrag oder defaultValue
     */
    public static String getProperty(String key, String defaultValue) {
        String ret = UTILITIES.getProperty(key);
        if (ret == null) {
            UTILITIES.setProperty(key, defaultValue);
        }
        return PROPS.getProperty(key);
    }

    public static int[] getRGB(int a) {
        java.awt.Color aa = new java.awt.Color(a);
        return new int[] { aa.getRed(), aa.getGreen(), aa.getBlue() };

    }

    /**
     * @return Gibt die Millisekunen seit 1970 zurück
     */
    public static long getTimer() {
        return System.currentTimeMillis();
    }

    public static long getTimer(long timer) {
        return System.currentTimeMillis() - timer;
    }

    /**
     * @param arg
     * @return gibr url von arg zurück
     */
    public static URL getURL(String arg) {
        URL url = null;
        try {
            url = new URL(arg);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }

    /**
     * Wandelt eine decimale hexzahl(0-256^3) in die 3 RGB Farbkomponenten um.
     * 
     * @param value
     * @return RGB Werte
     */
    public static int[] hexToRgb(int value) {
        int[] v = { (value / 65536), ((value - value / 65536 * 65536) / 256), (value - value / 65536 * 65536 - (value - value / 65536 * 65536) / 256 * 256), 0 };
        return v;
    }

    /** **********************************GUI************************************** */
    /**
     * public static Image loadImage(File file) Lädt file als Bildatei und
     * wartet bis file geladen wurde. gibt file als Image zurück
     * 
     * @param file
     * @return Neues Bild
     */
    public static Image loadImage(File file) {
        JFrame jf = new JFrame();
        Image img = jf.getToolkit().getImage(file.getAbsolutePath());
        MediaTracker mediaTracker = new MediaTracker(jf);
        mediaTracker.addImage(img, 0);
        try {
            mediaTracker.waitForID(0);
        } catch (InterruptedException e) {
            return null;
        }

        mediaTracker.removeImage(img);
        return img;
    }

    /**
     * Lädt ein Bild von einer URL. kehrt nach dem laden zurück.
     * 
     * @param url
     * @return neue Bild
     */
    public static Image loadImage(URL url) {
        JFrame jf = new JFrame();
        Image img = jf.getToolkit().getImage(url);
        MediaTracker mediaTracker = new MediaTracker(jf);
        mediaTracker.addImage(img, 0);
        try {
            mediaTracker.waitForID(0);
        } catch (InterruptedException e) {
            return null;
        }

        mediaTracker.removeImage(img);
        return img;
    }

    /**
     * Mischt zwei Fraben 1:1
     * 
     * @param a
     * @param b
     * @return 32 Bit Farbwert
     */
    public static int mixColors(int a, int b) {
        int[] av = UTILITIES.hexToRgb(a);
        int[] bv = UTILITIES.hexToRgb(b);
        int[] ret = { (av[0] + bv[0]) / 2, (av[1] + bv[1]) / 2, (av[2] + bv[2]) / 2 };
        return UTILITIES.rgbToHex(ret);
    }

    /**
     * Mischt zwei decimal zahlen. dabei werden ga und gb als
     * gewichtungsfaktoren verwendet mixColors(0xff0000,0x00ff00,3,1) mischt 3
     * Teile Rot und einen teil grün
     * 
     * @param a
     * @param b
     * @param ga
     * @param gb
     * @return 32 Bit Farbwert
     */
    public static int mixColors(int a, int b, int ga, int gb) {

        int[] av = UTILITIES.hexToRgb(a);
        int[] bv = UTILITIES.hexToRgb(b);
        int R = (av[0] * ga + bv[0] * gb) / (ga + gb);
        int G = (av[1] * ga + bv[1] * gb) / (ga + gb);
        int B = (av[2] * ga + bv[2] * gb) / (ga + gb);

        return UTILITIES.rgbToHex(new int[] { R, G, B });

    }

    public static int nextJump(int x, int from, int to, int step) {
        int start = UTILITIES.getJumperStart(from, to);
        int ret;
        if (x == start) {
            ret = start + step;
            if (ret > to) {
                ret = start - step;
            }
        } else if (x > start) {
            int dif = x - start;
            ret = start - dif;

        } else {
            int dif = start - x + step;
            ret = start + dif;
            if (ret > to) {
                ret = start - dif;
            }
        }

        return ret;

    }

    /**
     * @param path
     * @return Pfad als array{hd,path,filename,extension
     */
    public static String[] parsePath(String path) {
        String[] ret = new String[4];
        File file = new File(path);

        path = file.getAbsolutePath();
        int first = path.indexOf(FS);
        int last = path.lastIndexOf(FS);
        ret[0] = path.substring(0, first + 1);
        ret[1] = path.substring(first + 1, last + 1);
        int lastPoint = file.getName().lastIndexOf(".");
        ret[2] = file.getName().substring(0, lastPoint);
        ret[3] = file.getName().substring(lastPoint + 1);
        return ret;

    }

    /** ************************************XML**************************************** */
    /**
     * public static Document parseXmlFile(String filename, boolean
     * validating) liest filename als XML ein und gibt ein XML Document
     * zurück. Parameter validating: Macht einen validt check
     * 
     * @param is
     *            InputStream
     * @param validating
     * @return XML Document
     */
    public static Document parseXmlFile(InputStream is, boolean validating) {
        try {
            // Create a builder factory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(validating);

            // Create the builder and parse the file
            Document doc = factory.newDocumentBuilder().parse(is);
            return doc;
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {

            // DEBUG.error(e);
        }
        return null;
    }

    /**
     * @param xmlString
     * @param validating
     * @return XML Dokument
     */
    public static Document parseXmlString(String xmlString, boolean validating) {
        if (xmlString == null) { return null; }
        try {
            // Create a builder factory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(validating);

            InputSource inSource = new InputSource(new StringReader(xmlString));

            // Create the builder and parse the file
            Document doc = factory.newDocumentBuilder().parse(inSource);

            return doc;
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {

            // DEBUG.error(e);
        }
        return null;
    }

    /***************************************************************************
     * ******************************SYSTEM
     * 
     * @param path
     * @return URI zu path
     */
    public static String pathToURI(String path) {
        return UTILITIES.fileToURI(new File(path));

    }

    /**
     * public static String prompt(String msg) Zeigt eine Text-Input Box an
     * 
     * @param msg
     * @return User Input /null
     */
    public static String prompt(String msg) {
        return JOptionPane.showInputDialog(msg);
    }

    /**
     * public static String prompt(String msg, String defaultStr) Zeigt eine
     * Text-Input Box an. inkl Default Eingabe
     * 
     * @param msg
     * @param defaultStr
     * @return User Input /null
     */
    public static String prompt(String msg, String defaultStr) {
        return JOptionPane.showInputDialog(msg, defaultStr);
    }

    /***************************************************************************
     * ***********************************IMAGE
     * 
     * @param file
     * @return neues BufferedImage
     * @throws IOException
     */
    public static BufferedImage readImageFromFile(File file) throws IOException {
        return ImageIO.read(file);
    }

    /** ***************************************COLOR************************************* */
    // RGB to HSB
    /**
     * public static float[] rgb2hsb(int r, int g, int b) Wandelt einen farbwert
     * vom RGB Farbraum in HSB um (Hue, Saturation, Brightness)
     * 
     * @param r
     * @param g
     * @param b
     * @return hsb Farbwerte
     */
    public static float[] rgb2hsb(int r, int g, int b) {

        float[] hsbvals = new float[3];
        Color.RGBtoHSB(r, g, b, hsbvals);
        return hsbvals;
    }

    /**
     * Rechnet RGB werte in den LAB Farbraum um. Der LAB Farbraum wird vor allem
     * zu Farbabstandsberechnungen verwendet Wert für L* enthält die
     * Helligkeitsinformation, und hat einen Wertebereich von 0 bis 100. Die
     * Werte a* und b* enthalten die Farbinformation. a* steht für die
     * Farbbalance zwischen Grün und Rot, b* steht für die Farbbalance zwischen
     * Blau und Gelb. a* und b* haben einen Wertebereich von -120 bis + 120.
     * 
     * @param R
     * @param G
     * @param B
     * @return int[] LAB Farbwerte
     */
    public static int[] rgb2lab(int R, int G, int B) {
        // http://www.brucelindbloom.com
        int[] lab = new int[3];
        float r, g, b, X, Y, Z, fx, fy, fz, xr, yr, zr;
        float Ls, as, bs;
        float eps = 216.f / 24389.f;
        float k = 24389.f / 27.f;

        float Xr = 0.964221f; // reference white D50
        float Yr = 1.0f;
        float Zr = 0.825211f;

        // RGB to XYZ
        r = R / 255.f; // R 0..1
        g = G / 255.f; // G 0..1
        b = B / 255.f; // B 0..1

        // assuming sRGB (D65)
        if (r <= 0.04045) {
            r = r / 12;
        } else {
            r = (float) Math.pow((r + 0.055) / 1.055, 2.4);
        }

        if (g <= 0.04045) {
            g = g / 12;
        } else {
            g = (float) Math.pow((g + 0.055) / 1.055, 2.4);
        }

        if (b <= 0.04045) {
            b = b / 12;
        } else {
            b = (float) Math.pow((b + 0.055) / 1.055, 2.4);
        }

        X = 0.436052025f * r + 0.385081593f * g + 0.143087414f * b;
        Y = 0.222491598f * r + 0.71688606f * g + 0.060621486f * b;
        Z = 0.013929122f * r + 0.097097002f * g + 0.71418547f * b;

        // XYZ to Lab
        xr = X / Xr;
        yr = Y / Yr;
        zr = Z / Zr;

        if (xr > eps) {
            fx = (float) Math.pow(xr, 1 / 3.);
        } else {
            fx = (float) ((k * xr + 16.) / 116.);
        }

        if (yr > eps) {
            fy = (float) Math.pow(yr, 1 / 3.);
        } else {
            fy = (float) ((k * yr + 16.) / 116.);
        }

        if (zr > eps) {
            fz = (float) Math.pow(zr, 1 / 3.);
        } else {
            fz = (float) ((k * zr + 16.) / 116);
        }

        Ls = 116 * fy - 16;
        as = 500 * (fx - fy);
        bs = 200 * (fy - fz);

        lab[0] = (int) (2.55 * Ls + .5);
        lab[1] = (int) (as + .5);
        lab[2] = (int) (bs + .5);
        return lab;
    }

    /**
     * Wandelt ein RGB Array in die zugehöroge decimale hexzahl um.
     * 
     * @param value
     * @return 32 BIt Farbwert
     */
    public static int rgbToHex(int[] value) {
        return value[0] * 65536 + value[1] * 256 + value[2];
    }

    /**
     * public static void runCommand(String command) Führt einen Shell Befehl
     * auf. Wartet nicht!
     * 
     * @param command
     */
    public static void runCommand(String command) {
        if (command == null) { return; }
        try {
            Runtime rt = Runtime.getRuntime();
            rt.exec(command);

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    /**
     * public static void runCommandAndWait(String command) Führt einen
     * Shellbefehl aus und wartet bis der Befehl abgearbeitet wurde. Die
     * rückgaben werden auf der Konsole ausgegeben
     * 
     * @param command
     */

    public static void runCommandAndWait(String command) {
        try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(command);

            BufferedReader br = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                if (JAntiCaptcha.isLoggerActive()) {
                    logger.fine(line);
                }

            }
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    /**
     * public static String runCommandWaitAndReturn(String command) Ruft eine
     * Shell-Befehl, wartet bis der Befehl beendet wurde und gibt die Rückgaben
     * als String zurück
     * 
     * @param command
     * @return Rückgabestring
     */
    public static String runCommandWaitAndReturn(String command) {
        String ret = "";
        try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(command);

            BufferedReader br = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line;

            while ((line = br.readLine()) != null) {
                ret += line;

            }
        } catch (IOException e) {

            e.printStackTrace();
        }
        return ret;
    }

    /**
     * @return true/false je nach Erfolg
     */
    public static boolean savePropertyFile() {
        if (PROPS == null) { return false; }
        try {
            FileOutputStream output = new FileOutputStream(PROPERTYFILE);

            try {
                PROPS.store(output, "autosave");
                return true;
            } catch (IOException e) {

                e.printStackTrace();
                return false;
            }
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    /**
     * public static boolean setProperty(String key, String value) Setzt den
     * Wert von key in der Globalen propertyfile
     * 
     * @param key
     * @param value
     * @return true/false je nach Erfolg
     */
    public static boolean setProperty(String key, String value) {
        if (value == null) {
            value = "";
        }
        if (PROPS == null) {
            FileInputStream input;
            PROPS = new Properties();
            try {
                input = new FileInputStream(PROPERTYFILE);
                if (JAntiCaptcha.isLoggerActive()) {
                    logger.info("create " + PROPERTYFILE);
                }
                PROPS.load(input);

                PROPS.setProperty(key, value);

            } catch (Exception e) {

                e.printStackTrace();
                return false;
            }
        } else {
            PROPS.setProperty(key, value);
        }

        return UTILITIES.savePropertyFile();
    }

    /**
     * public static void showMessage(String msg) Zeigt msg als MessageBox an
     * 
     * @param msg
     */
    public static void showMessage(String msg) {
        JOptionPane.showMessageDialog(new JFrame(), msg);
    }

    /**
     * Wandelt file in ein JPG um
     * 
     * @param file
     * @return Neue datei
     */
    public static File toJPG(File file) {
        try {
            String[] name = file.getName().split("\\.");
            String newName = file.getName().substring(0, file.getName().length() - name[name.length - 1].length()) + "jpg";
            newName = file.getParent() + FS + newName;

            File newFile = new File(newName);
            if (newFile.exists()) { return newFile; }
            UTILITIES.writeImageToJPG(newFile, UTILITIES.readImageFromFile(file));
            return newFile;
        } catch (IOException e) {

            e.printStackTrace();
            return null;
        }

    }

    /** *************************************MATH**************************************** */
    /**
     * DReht die Koordinaten x und y um den Mittelpunkt nullX und nullY umd en
     * Winkel winkel
     * 
     * @param x
     * @param y
     * @param nullX
     * @param nullY
     * @param winkel
     * @return neue Koordinaten
     */
    public static int[] turnCoordinates(int x, int y, int nullX, int nullY, double winkel) {
        winkel /= 180.0;
        int newX = x - nullX;
        int newY = y - nullY;
        double aktAngle = Math.atan2(newY, newX);

        int[] ret = new int[2];
        double radius = Math.sqrt(newX * newX + newY * newY);
        int yTrans = (int) Math.round(radius * Math.sin((aktAngle + winkel * Math.PI)));
        int xTrans = (int) Math.round(radius * Math.cos((aktAngle + winkel * Math.PI)));
        ret[0] = xTrans + nullX;
        ret[1] = yTrans + nullY;
        return ret;
    }

    /**
     * @param str
     * @return str URLCodiert
     */
    public static String urlEncode(String str) {
        try {
            str = URLDecoder.decode(str, "UTF-8");

            String allowed = "1234567890QWERTZUIOPASDFGHJKLYXCVBNMqwertzuiopasdfghjklyxcvbnm-_.?/:";
            String ret = "";
            int i;
            for (i = 0; i < str.length(); i++) {
                char letter = str.charAt(i);
                if (allowed.indexOf(letter) >= 0) {
                    ret += letter;
                } else {
                    ret += "%" + Integer.toString(letter, 16);
                }
            }

            return ret;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * TODO: substring(6) ist nicht richtig unter linux!!!! Wandelt einen URL in
     * den zugehörigen Local Pfad um
     * 
     * @param path
     * @return pfad
     */
    public static String URLtoPath(String path) {
        return path.substring(6);

    }

    /**
     * * TODO: substring(6) ist nicht richtig unter linux!!!! Wandelt eine URL
     * in den zugehörigen Path um
     * 
     * @param path
     * @return Pfad
     */

    public static String URLtoPath(URL path) {

        return path.toString().substring(6);

    }

    /** *************************************NET**************************************** */

    /**
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
     * public static void wait(int ms) Hält den aktuellen Thread um ms
     * Millisekunden auf pause
     * 
     * @param ms
     */
    public static void wait(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param file
     * @param bufferedImage
     * @throws IOException
     */
    public static void writeImageToJPG(File file, BufferedImage bufferedImage) throws IOException {

        ImageIO.write(bufferedImage, "jpg", file);
    }

    /**
     * public static boolean writeLocalFile(File file, String content) Schreibt
     * über einen BufferedWriter content in file. file wird gelöscht falls sie
     * schon existiert
     * 
     * @param file
     * @param content
     * @return true/false je Nach Erfolg
     */
    public static boolean writeLocalFile(File file, String content) {

        try {

            if (file.isFile()) {
                if (!file.delete()) {
                    if (JAntiCaptcha.isLoggerActive()) {
                        logger.warning("Konnte Datei nicht löschen " + file);
                    }
                    return false;
                }

            }
            if (JAntiCaptcha.isLoggerActive()) {
                logger.info("DIR :" + file.getParent());
            }
            if (file.getParent() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();

            // FileWriter f = new FileWriter(file);
            BufferedWriter f = new BufferedWriter(new FileWriter(file));
            // DEBUG.trace(file+" - "+content);
            f.write(content);
            f.close();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;

        }

    }

    /**
     * public static boolean writeLocalFileBytes(File file, String content)
     * schreibt content in file. file wird gelöscht fals sie schon existiert
     * /byteweise
     * 
     * @param file
     * @param content
     * @return true/false je nach Erfolg
     */
    public static boolean writeLocalFileBytes(File file, String content) {

        FileOutputStream f;
        try {

            if (file.isFile()) {
                if (!file.delete()) {
                    if (JAntiCaptcha.isLoggerActive()) {
                        logger.warning("Konnte Datei nicht löschen " + file);
                    }
                    return false;
                }

            }
            if (JAntiCaptcha.isLoggerActive()) {
                logger.info("DIR :" + file.getParent());
            }
            if (file.getParent() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();

            f = new FileOutputStream(file);

            for (int i = 0; i < content.length(); i++) {
                f.write((byte) content.charAt(i));
            }
            f.close();
        } catch (FileNotFoundException e) {

            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }
}