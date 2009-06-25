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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;

import javax.swing.JComponent;

import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.http.Browser;
import jd.http.Encoding;
import jd.nutils.io.JDFileFilter;

public class JDLocale {

    private static HashMap<Integer, String> data = new HashMap<Integer, String>();

    private static HashMap<Integer, String> defaultData = null;

    private static final String DEFAULTLANGUAGE = "en";

    public static final String CONFIG = "LOCALE";

    public static final String LOCALE_ID = "LOCALE3";

    private static final HashMap<String, JDLocale> CACHE = new HashMap<String, JDLocale>();

    private static String COUNTRY_CODE = null;

    public static boolean DEBUG = false;

    private static String LANGUAGES_DIR = "jd/languages/";

    private static String localeID;

    private static File localeFile;

    private static int key;

    private String lngGeoCode;

    private String[] codes;

    public String getLngGeoCode() {
        return lngGeoCode;
    }

    public JDLocale(String lngGeoCode) {
        this.lngGeoCode = lngGeoCode;
        codes = JDGeoCode.parseLanguageCode(lngGeoCode);
    }

    public static String getLocale() {
        return localeID;
    }

    public static File getLanguageFile() {
        return localeFile;
    }

    public static String getTranslater() {
        return JDLocale.L("$translater$", "JD-Team");
    }

    public static String getVersion() {
        String info = JDLocale.L("$version$", "0.0");
        String ret = JDUtilities.getVersion(info);
        return (ret.equals("0.0")) ? info : ret;
    }

    public static ArrayList<JDLocale> getLocaleIDs() {
        File dir = JDUtilities.getResourceFile(LANGUAGES_DIR);
        if (!dir.exists()) return null;
        File[] files = dir.listFiles(new JDFileFilter(null, ".loc", false));
        ArrayList<JDLocale> ret = new ArrayList<JDLocale>();
        for (File element : files) {
            if (JDGeoCode.parseLanguageCode(element.getName().split("\\.")[0]) == null) {
                element.renameTo(new File(element, ".outdated"));
            } else {

                ret.add(getInstance(element.getName().split("\\.")[0]));
            }
        }
        return ret;
    }

    /**
     * Creates a new JDLocale instance or uses a cached one
     * 
     * @param lngGeoCode
     * @return
     */
    public static JDLocale getInstance(String lngGeoCode) {
        JDLocale ret;
        if ((ret = CACHE.get(lngGeoCode)) != null) return ret;
        ret = new JDLocale(lngGeoCode);
        CACHE.put(lngGeoCode, ret);
        return ret;
    }

    public String toString() {
        return JDGeoCode.toLongerNative(lngGeoCode);
    }

    public static String getLocaleString(String key2, String def) {
        if (DEBUG) return key2;
        if (data == null || localeFile == null) {
            JDLocale.setLocale(SubConfiguration.getConfig(JDLocale.CONFIG).getStringProperty(JDLocale.LOCALE_ID, JDLocale.isGerman() ? "de" : "en"));
        }

        key = key2.toLowerCase().hashCode();
        if (data.containsKey(key)) return data.get(key);

        System.out.println("Key not found: " + key2 + " Defaultvalue: " + def);
        if (def == null) {
            // defaultData nur im absoluten Notfall laden
            loadDefault();
            if (defaultData.containsKey(key)) {
                def = defaultData.get(key);
            }
            if (def == null) def = key2;
        }

        data.put(key, def);

        return def;
    }

    public static boolean isGerman() {
        String country = System.getProperty("user.country");
        return country != null && country.equalsIgnoreCase("DE");
    }

    public static String L(String key, String def) {
        return JDLocale.getLocaleString(key, def);
    }

    /**
     * Wrapper für String.format(JDLocale.L(..),args)
     * 
     * @param key
     * @param def
     * @param args
     * @return
     */
    public static String LF(String key, String def, Object... args) {
        if (DEBUG) return key;
        if (args == null || args.length == 0) {
            JDLogger.getLogger().severe("FIXME: " + key);
        }
        try {
            return String.format(JDLocale.L(key, def), args);
        } catch (Exception e) {
            JDLogger.getLogger().severe("FIXME: " + key);
            return "FIXME: " + key;
        }
    }

    public static void parseLanguageFile(File file, HashMap<Integer, String> data) {
        data.clear();

        if (file == null || !file.exists()) {
            System.out.println("JDLocale: " + file + " not found");
            return;
        }

        BufferedReader f;
        try {
            f = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));

            String line;
            String key;
            String value;
            while ((line = f.readLine()) != null) {
                if (line.startsWith("#")) continue;
                int split = line.indexOf("=");
                if (split <= 0) continue;

                key = line.substring(0, split).trim().toLowerCase();
                value = line.substring(split + 1).trim() + (line.endsWith(" ") ? " " : "");
                value = value.replace("\\r", "\r").replace("\\n", "\n");

                data.put(key.hashCode(), value);
            }
            f.close();
        } catch (IOException e) {
            JDLogger.exception(e);
        }

    }

    /**
     * Searches the key to a given hashcode. only needed for debug issues
     * 
     * @param hash
     * @return
     */
    private static String hashToKey(Integer hash) {
        BufferedReader f;
        try {
            f = new BufferedReader(new InputStreamReader(new FileInputStream(localeFile), "UTF8"));

            String line;
            String key;
            while ((line = f.readLine()) != null) {
                if (line.startsWith("#")) continue;
                int split = line.indexOf("=");
                if (split <= 0) continue;

                key = line.substring(0, split).trim().toLowerCase();
                if (hash == key.hashCode()) return key;

            }
            f.close();
        } catch (IOException e) {
            JDLogger.exception(e);
        }
        return null;
    }

    private static void loadDefault() {
        if (defaultData == null) {
            System.err.println("JD have to load the default language, there is an missing entry");
            defaultData = new HashMap<Integer, String>();
            File defaultFile = JDUtilities.getResourceFile(LANGUAGES_DIR + DEFAULTLANGUAGE + ".loc");
            if (defaultFile.exists()) {
                JDLocale.parseLanguageFile(defaultFile, defaultData);
            } else {
                System.out.println("Could not load the default languagefile: " + defaultFile);
            }
        }
    }

    public static void setLocale(String lID) {
        lID = correctLID(lID);
        localeID = lID;
        System.out.println("Loaded language: " + lID);
        localeFile = JDUtilities.getResourceFile(LANGUAGES_DIR + localeID + ".loc");
        if (localeFile.exists()) {
            JDLocale.parseLanguageFile(localeFile, data);
        } else {
            System.out.println("Language " + localeID + " not installed");
            return;
        }
    }

    /**
     * Due to languagefile correction, it may be possible that user have
     * language IDs stored that are not available any more.
     * 
     * @param lID
     * @return
     */
    private static String correctLID(String lID) {
        String ret = JDGeoCode.longToShort(lID);
        if (ret != null) return ret;

        if (lID.equalsIgnoreCase("Chinese(traditionalbig5)")) {
            lID = "zh_hant";
            SubConfiguration.getConfig(JDLocale.CONFIG).setProperty(JDLocale.LOCALE_ID, lID);
        } else if (lID.equalsIgnoreCase("Italiano")) {
            lID = "it";
            SubConfiguration.getConfig(JDLocale.CONFIG).setProperty(JDLocale.LOCALE_ID, lID);
        } else if (lID.equalsIgnoreCase("Nederlands")) {
            lID = "nl";
            SubConfiguration.getConfig(JDLocale.CONFIG).setProperty(JDLocale.LOCALE_ID, lID);
        } else if (lID.equalsIgnoreCase("Polski")) {
            lID = "pl";
            SubConfiguration.getConfig(JDLocale.CONFIG).setProperty(JDLocale.LOCALE_ID, lID);
        } else if (lID.equalsIgnoreCase("Portugues(brazil)")) {
            lID = "Pt_BR";
            SubConfiguration.getConfig(JDLocale.CONFIG).setProperty(JDLocale.LOCALE_ID, lID);
        } else if (lID.equalsIgnoreCase("arabic")) {
            lID = "ar";
            SubConfiguration.getConfig(JDLocale.CONFIG).setProperty(JDLocale.LOCALE_ID, lID);
        } else if (lID.equalsIgnoreCase("Spanish(Argentina)")) {
            lID = "es";
            SubConfiguration.getConfig(JDLocale.CONFIG).setProperty(JDLocale.LOCALE_ID, lID);
        } else if (lID.equalsIgnoreCase("German")) {
            lID = "de";
            SubConfiguration.getConfig(JDLocale.CONFIG).setProperty(JDLocale.LOCALE_ID, lID);
        }
        return lID;

    }

    public static void initLocalisation() {
        JComponent.setDefaultLocale(new Locale(JDLocale.getInstance(JDLocale.getLocale()).getLanguageCode()));
    }

    public static String translate(String to, String msg) {
        return JDLocale.translate("auto", to, msg);
    }

    public static String translate(String from, String to, String msg) {
        try {
            HashMap<String, String> postData = new HashMap<String, String>();
            postData.put("hl", "de");
            postData.put("text", msg);
            postData.put("sl", from);
            postData.put("tl", to);
            postData.put("ie", "UTF8");

            Browser br = new Browser();
            br.postPage("http://translate.google.com/translate_t", postData);

            return Encoding.UTF8Decode(Encoding.htmlDecode(br.getRegex("<div id\\=result_box dir\\=\"ltr\">(.*?)</div>").getMatch(0)));
        } catch (IOException e) {
            JDLogger.exception(e);
            return null;
        }
    }

    /**
     * Returns an array for the best matching key to text
     * 
     * @param text
     * @return
     */
    public static String[] getKeysFor(String text) {
        ArrayList<Integer> bestKeys = new ArrayList<Integer>();
        int bestValue = Integer.MAX_VALUE;
        for (Iterator<Entry<Integer, String>> it = data.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, String> next = it.next();
            int dist = EditDistance.getLevenshteinDistance(text, next.getValue());

            if (dist < bestValue) {
                bestKeys.clear();
                bestKeys.add(next.getKey());
                bestValue = dist;
            } else if (bestValue == dist) {
                bestKeys.add(next.getKey());
                bestValue = dist;
            }

        }
        if (bestKeys.size() == 0) return null;
        String[] ret = new String[bestKeys.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = hashToKey(bestKeys.get(i));
        }
        return ret;
    }

    /**
     * returns the correct country code
     * 
     * @return
     */
    public static String getCountryCodeByIP() {

        if (COUNTRY_CODE != null) return COUNTRY_CODE;

        if ((COUNTRY_CODE = SubConfiguration.getConfig(JDLocale.CONFIG).getStringProperty("DEFAULTLANGUAGE", null)) != null) { return COUNTRY_CODE; }
        Browser br = new Browser();
        br.setConnectTimeout(10000);
        br.setReadTimeout(10000);
        try {
            COUNTRY_CODE = br.getPage("http://jdownloader.net:8081/advert/getLanguage.php");
            if (!br.getRequest().getHttpConnection().isOK()) {
                COUNTRY_CODE = null;
            } else {
                COUNTRY_CODE = COUNTRY_CODE.trim().toUpperCase();

                SubConfiguration.getConfig(JDLocale.CONFIG).setProperty("DEFAULTLANGUAGE", COUNTRY_CODE);
                SubConfiguration.getConfig(JDLocale.CONFIG).save();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return COUNTRY_CODE;
    }

    public String getCountryCode() {
        // TODO Auto-generated method stub
        return codes[1];
    }

    public String getExtensionCode() {
        // TODO Auto-generated method stub
        return codes[1];
    }

    public String getLanguageCode() {
        // TODO Auto-generated method stub
        return codes[0];
    }

}