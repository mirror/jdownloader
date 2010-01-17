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

package jd.utils.locale;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.Map.Entry;

import javax.swing.JComponent;

import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.nutils.io.JDFileFilter;
import jd.utils.EditDistance;
import jd.utils.JDGeoCode;
import jd.utils.JDUtilities;

public final class JDL {
    /**
     * Don't let anyone instantiate this class.
     */
    private JDL() {
    }

    private static final HashMap<String, JDLocale> CACHE = new HashMap<String, JDLocale>();

    public static final String CONFIG = "LOCALE";

    private static String COUNTRY_CODE = null;

    private static final HashMap<Integer, String> DATA = new HashMap<Integer, String>();

    public static boolean DEBUG = false;

    private static HashMap<Integer, String> DEFAULT_DATA = null;

    private static int KEY;

    private static String LANGUAGES_DIR = "jd/languages/";

    public static final String LOCALE_PARAM_ID = "LOCALE4";

    public static final JDLocale DEFAULT_LOCALE = JDL.getInstance("en");

    private static File LOCALE_FILE;

    private static JDLocale LOCALE_ID;

    private static String STATIC_LOCALE;

    /**
     * returns the correct country code
     * 
     * @return
     */
    public static String getCountryCodeByIP() {
        if (COUNTRY_CODE != null) return COUNTRY_CODE;

        if ((COUNTRY_CODE = SubConfiguration.getConfig(JDL.CONFIG).getStringProperty("DEFAULTLANGUAGE", null)) != null) { return COUNTRY_CODE; }
        final Browser br = new Browser();
        br.setConnectTimeout(10000);
        br.setReadTimeout(10000);
        try {
            COUNTRY_CODE = br.getPage("http://www.jdownloader.org/advert/getLanguage.php?id=" + System.currentTimeMillis() + new Random(System.currentTimeMillis()).nextLong());
            if (!br.getRequest().getHttpConnection().isOK()) {
                COUNTRY_CODE = null;
            } else {
                COUNTRY_CODE = COUNTRY_CODE.trim().toUpperCase();

                SubConfiguration.getConfig(JDL.CONFIG).setProperty("DEFAULTLANGUAGE", COUNTRY_CODE);
                SubConfiguration.getConfig(JDL.CONFIG).save();
            }
        } catch (Exception e) {
            COUNTRY_CODE = null;
        }
        return COUNTRY_CODE;
    }

    /**
     * Creates a new JDLocale instance or uses a cached one
     * 
     * @param lngGeoCode
     * @return
     */
    public static JDLocale getInstance(final String lngGeoCode) {
        JDLocale ret;
        if ((ret = CACHE.get(lngGeoCode)) != null) return ret;
        ret = new JDLocale(lngGeoCode);
        CACHE.put(lngGeoCode, ret);
        return ret;
    }

    /**
     * Returns an array for the best matching KEY to text
     * 
     * @param text
     * @return
     */
    public static String[] getKeysFor(final String text) {
        final ArrayList<Integer> bestKeys = new ArrayList<Integer>();
        int bestValue = Integer.MAX_VALUE;
        for (Entry<Integer, String> next : DATA.entrySet()) {
            final int dist = EditDistance.getLevenshteinDistance(text, next.getValue());

            if (dist < bestValue) {
                bestKeys.clear();
                bestKeys.add(next.getKey());
                bestValue = dist;
            } else if (bestValue == dist) {
                bestKeys.add(next.getKey());
                bestValue = dist;
            }
        }
        final int size = bestKeys.size();
        if (size == 0) return null;

        final String[] ret = new String[size];
        final int length = ret.length;
        for (int i = 0; i < length; i++) {
            ret[i] = hashToKey(bestKeys.get(i));
        }
        return ret;
    }

    public static File getLanguageFile() {
        return LOCALE_FILE;
    }

    /**
     * Gibt den configwert für Locale zurück
     * 
     * @return
     */
    public static JDLocale getConfigLocale() {
        return SubConfiguration.getConfig(JDL.CONFIG).getGenericProperty(JDL.LOCALE_PARAM_ID, JDL.DEFAULT_LOCALE);
    }

    /**
     * saves defaultlocal
     */
    public static void setConfigLocale(final JDLocale l) {
        SubConfiguration.getConfig(JDL.CONFIG).setProperty(JDL.LOCALE_PARAM_ID, l);
        SubConfiguration.getConfig(JDL.CONFIG).save();
    }

    public static JDLocale getLocale() {
        if (DEBUG) return DEFAULT_LOCALE;
        return LOCALE_ID;
    }

    public static ArrayList<JDLocale> getLocaleIDs() {
        final File dir = JDUtilities.getResourceFile(LANGUAGES_DIR);
        if (!dir.exists()) return null;
        final File[] files = dir.listFiles(new JDFileFilter(null, ".loc", false));
        final ArrayList<JDLocale> ret = new ArrayList<JDLocale>();
        String name = null;
        for (File element : files) {
            name = element.getName().split("\\.")[0];
            if (JDGeoCode.parseLanguageCode(name) == null) {
                element.renameTo(new File(element, ".outdated"));
            } else {
                ret.add(getInstance(name));
            }
        }
        return ret;
    }

    public static String getLocaleString(final String key2, String def) {
        if (DEBUG) return key2;
        if (DATA == null || LOCALE_FILE == null) {
            JDL.setLocale(getConfigLocale());
            if (DATA == null) return "Error in JDL: DATA==null";
        }
        KEY = key2.toLowerCase().hashCode();
        if (DATA.containsKey(KEY)) return DATA.get(KEY);

        System.out.println("Key not found: " + key2 + " Defaultvalue: " + def);
        if (def == null) {

            def = getDefaultLocaleString(KEY);
            if (def == null) def = key2;
        }

        DATA.put(KEY, def);

        return def;
    }

    /**
     * loads the default translation(english) and returns the string for the
     * givven key
     * 
     * @param key2
     *            stringkey.toLowerCase().hashCode()
     * @return
     */
    public static String getDefaultLocaleString(final int key) {
        // DEFAULT_DATA nur im absoluten Notfall laden
        loadDefault();
        if (DEFAULT_DATA.containsKey(key)) { return DEFAULT_DATA.get(key); }
        return null;
    }

    /**
     * Searches the KEY to a given hashcode. only needed for debug issues
     * 
     * @param hash
     * @return
     */
    private static String hashToKey(final Integer hash) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(LOCALE_FILE), "UTF8"));
            String line;
            String key;
            while ((line = reader.readLine()) != null) {
                // if (line.startsWith("#"))
                if (line.charAt(0) == '#') continue;
                final int split = line.indexOf('=');
                if (split <= 0) continue;

                key = line.substring(0, split).trim().toLowerCase();
                if (hash == key.hashCode()) return key;
            }
        } catch (Exception e) {
            JDLogger.exception(e);
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (Exception e1) {
                JDLogger.exception(e1);
            }
        }
        return null;
    }

    public static void initLocalisation() {
        JComponent.setDefaultLocale(new Locale(JDL.getLocale().getLanguageCode()));
    }

    public static boolean isGerman() {
        final String country = System.getProperty("user.country");
        return country != null && country.equalsIgnoreCase("DE");
    }

    public static String L(final String key, final String def) {
        return JDL.getLocaleString(key, def);
    }

    /**
     * Wrapper für String.format(JDL.L(..),args)
     * 
     * @param KEY
     * @param def
     * @param args
     * @return
     */
    public static String LF(final String key, final String def, final Object... args) {
        if (DEBUG) return key;
        if (args == null || args.length == 0) {
            JDLogger.getLogger().severe("FIXME: " + key);
        }
        try {
            return String.format(JDL.L(key, def), args);
        } catch (Exception e) {
            JDLogger.getLogger().severe("FIXME: " + key);
            return "FIXME: " + key;
        }
    }

    private static void loadDefault() {
        if (DEFAULT_DATA == null) {
            System.err.println("JD have to load the default language, there is an missing entry");
            DEFAULT_DATA = new HashMap<Integer, String>();
            final File defaultFile = STATIC_LOCALE == null ? JDUtilities.getResourceFile(LANGUAGES_DIR + DEFAULT_LOCALE.getLngGeoCode() + ".loc") : new File(STATIC_LOCALE);
            if (defaultFile.exists()) {
                JDL.parseLanguageFile(defaultFile, DEFAULT_DATA);
            } else {
                System.out.println("Could not load the default languagefile: " + defaultFile);
            }
        }
    }

    public static void parseLanguageFile(final File file, final HashMap<Integer, String> data) {

        JDLogger.getLogger().info("parse lng file " + file);
        data.clear();

        if (file == null || !file.exists()) {
            System.out.println("JDLocale: " + file + " not found");
            return;
        }

        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));

            String line;
            String key;
            String value;
            while ((line = reader.readLine()) != null) {
                // if (line.startsWith("#")) continue;
                if (line.charAt(0) == '#') continue;

                // int split = line.indexOf("=");
                int split = line.indexOf('=');
                if (split <= 0) continue;

                key = line.substring(0, split).trim().toLowerCase();
                value = line.substring(split + 1).trim() + (line.endsWith(" ") ? " " : "");
                value = value.replace("\\r", "\r").replace("\\n", "\n");
                data.put(key.hashCode(), value);
            }
            reader.close();
        } catch (IOException e) {
            JDLogger.exception(e);
        }

        JDLogger.getLogger().info("parse lng file end " + file);
    }

    public static void setLocale(final JDLocale lID) {
        if (lID == null) return;
        LOCALE_ID = lID;
        System.out.println("Loaded language: " + lID);
        LOCALE_FILE = STATIC_LOCALE == null ? JDUtilities.getResourceFile(LANGUAGES_DIR + LOCALE_ID.getLngGeoCode() + ".loc") : new File(STATIC_LOCALE);
        if (LOCALE_FILE.exists()) {
            JDL.parseLanguageFile(LOCALE_FILE, DATA);
        } else {
            System.out.println("Language " + LOCALE_ID + " not installed");
            return;
        }
    }

    public static Translation translate(final String to, final String msg) {
        return JDL.translate("auto", to, msg);
    }

    public static Translation translate(final String from, final String to, final String msg) {
        try {

            Translation trans = new Translation(msg, to);
            final Browser br = new Browser();
            br.getPage("http://www.google.com/uds/Gtranslate?callback=google.language.callbacks.id101&context=22&q=" + Encoding.urlEncode(msg) + "&langpair=|en&key=notsupplied&v=1.0");
            String[] match = br.getRegex("\"translatedText\":\"(.*?)\",\"detectedSourceLanguage\":\"(.*?)\"").getRow(0);
            trans.setTranslated(Encoding.UTF8Decode(Encoding.htmlDecode(match[0])));
            trans.setSourceLanguage(Encoding.UTF8Decode(Encoding.htmlDecode(match[1])));
            return trans;
        } catch (Exception e) {
            JDLogger.exception(e);
            return null;
        }
    }

    /**
     * Use a absolute path to a locale
     * 
     * @param string
     */
    public static void setStaticLocale(final String string) {
        STATIC_LOCALE = string;
    }

}