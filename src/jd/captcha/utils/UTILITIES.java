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

package jd.captcha.utils;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jd.captcha.JAntiCaptcha;

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

    /**
     * File Seperator
     */
    public static String FS = System.getProperty("file.separator");

    private static Logger logger = jd.controlling.JDLogger.getLogger();

    public static boolean checkJumper(int x, int from, int to) {
        return x >= from && x <= to;
    }

    /**
     * Zeigt einen Directory Chooser an
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
        if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) return fc.getSelectedFile();
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
    }

    /**
     * Gibt die File zurück der im array entries übergeben wurde. der passende
     * FS wird eingesetzt
     * 
     * @param entries
     * @return Pfad als File
     */
    public static File getFullFile(String[] entries) {
        return new File(UTILITIES.getFullPath(entries));
    }

    /**
     * Gibt den Pfad zurück der im array entries übergeben wurde. der passende
     * FS wird eingesetzt
     * 
     * @param entries
     * @return Pfad als String
     */
    public static String getFullPath(String[] entries) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < entries.length - 1; i++) {
            ret.append(entries[i]);
            ret.append(FS);
        }
        ret.append(entries[entries.length - 1]);
        return ret.toString();
    }

    /**
     * Gibt die default GridBagConstants zurück
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
     * @return logger
     */
    public static Logger getLogger() {
        return logger;
    }

    /**
     * Gibt alle treffer in source nach dem pattern zurück. Platzhalter ist nur
     * !! °
     * 
     * @param source
     * @param pattern
     * @return Alle TReffer
     */
    public static String[] getMatches(String source, String pattern) {
        // DEBUG.trace("pattern: "+STRING.getPattern(pattern));
        if (source == null || pattern == null) return null;
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

    /**
     * Gibt ein Regex pattern zurück. ° dient als Platzhalter!
     * 
     * @param str
     * @return RegEx Pattern
     */
    private static String getPattern(String str) {

        String allowed = "QWERTZUIOPÜASDFGHJKLÖÄYXCVBNMqwertzuiopasdfghjklyxcvbnm 1234567890";
        StringBuilder ret = new StringBuilder();
        int i;
        for (i = 0; i < str.length(); i++) {
            char letter = str.charAt(i);
            // 176 == °
            if (letter == 176) {
                ret.append("(.*?)");
            } else if (allowed.indexOf(letter) == -1) {
                ret.append('\\');
                ret.append(letter);
            } else {
                ret.append(letter);
            }
        }

        return ret.toString();
    }

    public static int getPercent(int a, int b) {
        if (b == 0) return 100;
        return a * 100 / b;
    }

    public static int[] getRGB(int a) {
        Color aa = new Color(a);
        return new int[] { aa.getRed(), aa.getGreen(), aa.getBlue() };
    }

    /**
     * @return Gibt die Millisekunen seit 1970 zurück
     */
    public static long getTimer() {
        return System.currentTimeMillis();
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

    /**
     * Lädt file als Bildatei und wartet bis file geladen wurde. gibt file als
     * Image zurück
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
        int R, G, B;

        R = (av[0] * ga + bv[0] * gb) / (ga + gb);
        G = (av[1] * ga + bv[1] * gb) / (ga + gb);
        B = (av[2] * ga + bv[2] * gb) / (ga + gb);

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
     * @param xmlString
     * @param validating
     * @return XML Dokument
     */
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
        } catch (SAXException e) {
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
        } catch (ParserConfigurationException e) {
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
        } catch (IOException e) {

            // DEBUG.error(e);
        }
        return null;
    }

    /**
     * Wandelt einen farbwert vom RGB Farbraum in HSB um (Hue, Saturation,
     * Brightness)
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

    public static float[] rgb2hsb(int pixelValue) {
        int[] rgbA = UTILITIES.hexToRgb(pixelValue);
        return rgb2hsb(rgbA[0], rgbA[1], rgbA[2]);
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
    private static int[] rgb2lab(int R, int G, int B) {
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
     * Dreht die Koordinaten x und y um den Mittelpunkt nullX und nullY umd en
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
     * Schreibt über einen BufferedWriter content in file. file wird gelöscht
     * falls sie schon existiert
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
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
            return false;
        }
    }

}