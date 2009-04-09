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

    /**
     * @return Gibt die Millisekunen seit 1970 zurück
     */
    public static long getTimer() {
        return System.currentTimeMillis();
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