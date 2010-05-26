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

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import jd.controlling.JDLogger;
import jd.nutils.JDImage;
import jd.nutils.encoding.Encoding;
import jd.nutils.io.JDFileFilter;
import jd.nutils.io.JDIO;
import jd.parser.Regex;

public final class JDTheme {

    /**
     * Don't let anyone instantiate this class.
     */
    private JDTheme() {
    }

    private static HashMap<String, String> data = new HashMap<String, String>();

    private static HashMap<String, String> defaultData;

    private static final Logger LOG = JDLogger.getLogger();

    public static final String THEME_DIR = "jd/themes/";

    private static String currentTheme;

    public static ArrayList<String> getThemeIDs() {
        final File dir = JDUtilities.getResourceFile(THEME_DIR);
        if (!dir.exists()) return null;

        final File[] files = dir.listFiles(new JDFileFilter(null, ".icl", false));
        final ArrayList<String> ret = new ArrayList<String>();

        for (File element : files) {
            ret.add(element.getName().split("\\.")[0]);
        }
        return ret;
    }

    public static String getThemeValue(final String key, String def) {
        if (data == null || defaultData == null) {
            LOG.severe("Use setTheme() first!");
            setTheme("default");
        }

        if (data.containsKey(key)) return Encoding.UTF8Decode(data.get(key));
        LOG.warning("Key not found: " + key + " (" + def + ")");

        if (defaultData.containsKey(key)) {
            def = Encoding.UTF8Decode(defaultData.get(key));
            LOG.finest("Use default Value: " + def);
        }
        if (def == null) {
            def = key;
        }
        data.put(key, def);

        return def;
    }

    /**
     * Gibt eine Farbe zum key zurück
     * 
     * @param key
     * @return
     */
    public static Color C(final String key, final String def) {
        return new Color(Integer.parseInt(JDTheme.V(key, def), 16));
    }

    public static Color C(final String key, final String def, final int alpha) {
        final String hex = JDTheme.V(key, def);
        return new Color(Integer.parseInt(hex.substring(0, 2), 16), Integer.parseInt(hex.substring(2, 4), 16), Integer.parseInt(hex.substring(4), 16), alpha);
    }

    /**
     * Gibt ein Image zum key zurück
     * 
     * @param key
     * @return
     */
    public static Image I(final String key) {
        return JDImage.getImage(JDTheme.V(key));
    }

    /**
     * Gibt ein skaliertes Image zurück
     * 
     * @param key
     * @param width
     * @param height
     * @return
     */
    public static Image I(final String key, final int width, final int height) {
        return JDImage.getImage(JDTheme.V(key)).getScaledInstance(width, height, Image.SCALE_SMOOTH);
    }

    /**
     * Gibt ein icon zum key zurück
     * 
     * @param key
     * @return
     */
    public static ImageIcon II(final String key) {
        return II(key, 32, 32);
    }

    /**
     * Gibt ein skaliertes ImageIcon zurück
     * 
     * @param key
     * @param width
     * @param height
     * @return
     */
    public static ImageIcon II(final String key, final int width, final int height) {
        if (key != null) {
            try {
                return new ImageIcon(getImage(V(key), width, height));
            } catch (Exception e) {
                LOG.severe("Could not find image: " + key + " (" + V(key) + "_" + width + "_" + height + ")");
                JDLogger.exception(e);
            }
        }
        return null;
    }

    public static Image getImage(final String string, final int width, final int height) {
        if (string != null) {
            final BufferedImage img = JDImage.getImage(string + "_" + width + "_" + height);
            if (img != null) return img;
            try {
                return JDImage.getScaledImage(JDImage.getImage(string), width, height);
            } catch (Exception e) {
                LOG.severe("Could not find image: " + string);
                JDLogger.exception(e);
            }
        }
        return null;
    }

    public static String getTheme() {
        return currentTheme == null ? "default" : currentTheme;
    }

    public static void setTheme(String themeID) {
        File file = JDUtilities.getResourceFile(THEME_DIR + themeID + ".icl");

        if (!file.exists()) {
            LOG.severe("Theme " + themeID + " not installed, switch to default theme");
            themeID = "default";
        }
        currentTheme = themeID;
        data = new HashMap<String, String>();
        String str = JDIO.readFileToString(file);
        String[] lines = Regex.getLines(str);
        for (final String element : lines) {
            int split = element.indexOf("=");
            if (split <= 0 || element.startsWith("#")) {
                continue;
            }
            final String key = element.substring(0, split).trim();
            final String value = element.substring(split + 1).trim();
            if (data.containsKey(key)) {
                LOG.severe("Dupe found: " + key);
            } else {
                data.put(key, value);
            }
        }
        if (themeID.equals("default")) {
            defaultData = data;
        }
        if (defaultData == null) {
            defaultData = new HashMap<String, String>();
            file = JDUtilities.getResourceFile(THEME_DIR + "default.icl");

            if (!file.exists()) {
                LOG.severe("Theme default not installed");
                return;
            }
            data = new HashMap<String, String>();
            str = JDIO.readFileToString(file);
            lines = Regex.getLines(str);
            for (String element : lines) {
                final int split = element.indexOf("=");
                if (split <= 0 || element.charAt(0) == '#') {
                    continue;
                }
                final String key = element.substring(0, split).trim();
                final String value = element.substring(split + 1).trim();
                if (data.containsKey(key)) {
                    LOG.finer("Dupe found: " + key);
                } else {
                    data.put(key, value);
                }
            }
        }
    }

    /**
     * Gibt einen Theme String zum Key zurück
     * 
     * @param key
     * @return
     */
    public static String V(final String key) {
        return JDTheme.getThemeValue(key, null);
    }

    /**
     * Gibt einen Theme String zum Key zurück
     * 
     * @param key
     * @return
     */
    public static String V(final String key, final String def) {
        return JDTheme.getThemeValue(key, def);
    }

}